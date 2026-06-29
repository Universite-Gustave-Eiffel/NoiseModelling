/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.webserver;

import net.opengis.wps10.ExecuteResponseType;
import org.apache.log4j.PropertyConfigurator;
import org.h2.value.ValueBoolean;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.geojson.GeoJsonWrite;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.VersionUtils;
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_source;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.script.JobStates;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NoiseModellingServerHttpTest {

    /**
     * A Javalin instance used to manage the HTTP server lifecycle and handle HTTP routes
     * for the web application during testing.
     *
     * This static variable is initialized and configured in the {@code setUp} method,
     * and is responsible for serving HTTP routes used by the test cases defined in the
     * {@link NoiseModellingServerHttpTest} class.
     *
     * It supports the execution of various HTTP-based operations such as handling requests
     * for WPS capabilities, process descriptions, and WPS execution, as verified in the test methods.
     */
    private static NoiseModellingServer app;

    private Logger logger = LoggerFactory.getLogger(NoiseModellingServerHttpTest.class);

    /**
     * The default port number on which the HTTP server will listen.
     *
     * This constant defines the port number used to establish server connections.
     * It is primarily used during the setup phase of the server and
     * in test cases to ensure proper server communication and resource access.
     *
     * Modifying this value may require corresponding updates in client-side
     * configurations and resource endpoints to maintain compatibility.
     */
    private static final int PORT = 8000;
    /**
     * The base URL for the OWS (OGC Web Services) endpoints used in the test cases.
     * It dynamically constructs the URL using the `localhost` domain and the value
     * of the `PORT` variable defined in the class.
     *
     * This URL serves as the base endpoint for various HTTP requests made during
     * the execution of the test suite and is primarily used for testing capabilities,
     * descriptions, and process execution of the WPS (Web Processing Service).
     */
    private static final String BASE_URL = "http://localhost:" + PORT + "/"+Configuration.DEFAULT_APPLICATION_URL+"/builder/ows";

    /**
     * Sets up the test environment for the HTTP-based tests.
     * This method is executed once before all tests in the test class.
     *
     * During the setup, a Javalin server instance is initialized by invoking the
     * {@code Main.startServer} method with the browser opening disabled. The server
     * instance is assigned to the static field {@code app}.
     *
     * @throws IOException if an I/O error occurs while starting the server.
     */
    @BeforeAll
    public static void setUp(@TempDir Path temporaryDirectory) throws IOException, SQLException, URISyntaxException {
        PropertyConfigurator.configure(
                Objects.requireNonNull(VersionUtils.class.getResource("log4j_tests.properties")));
        Configuration configuration = new Configuration(true);
        configuration.setWorkingDirectory(temporaryDirectory.toString());
        // Copy unit test scripts and standard scripts to temporary directory
        // Using recursive path copy to ensure that all scripts and subdirectories are included
        // standard scripts from src/main/groovy/org/noise_planet/noisemodelling/scripts
        // unit tests scripts in src/test/resources/org/noise_planet/noisemodelling/webserver/wps_scripts
        copyScriptsFromResource(Path.of("src/main/groovy/org/noise_planet/noisemodelling/scripts").toAbsolutePath(), temporaryDirectory);
        copyScriptsFromResource(Path.of("src/test/resources/org/noise_planet/noisemodelling/webserver/wps_scripts").toAbsolutePath(), temporaryDirectory);
        configuration.setScriptPath(temporaryDirectory.resolve("scripts/").toString());
        app = new NoiseModellingServer(configuration);
        app.startServer(false);
    }

    private static void copyScriptsFromResource(Path resourcePath, Path temporaryDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(resourcePath)) {
            paths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    try {
                        // Get the folders after "scripts" in the path and create the same structure in the temporary directory
                        Path relativePath = resourcePath.relativize(path);
                        Path parentPath = relativePath.getParent();
                        if(parentPath != null) {
                            Files.createDirectories(temporaryDirectory.resolve("scripts").resolve(parentPath));
                        }
                        Path targetPath = temporaryDirectory.resolve("scripts").resolve(relativePath);
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }
    }

    /**
     * Tears down the testing environment after all tests have been executed.
     *
     * This method is annotated with {@code @AfterAll}, meaning it will be executed
     * once after all test cases in the test class have been run. It is responsible
     * for performing cleanup operations such as stopping the application instance
     * if it has been initialized during the test setup.
     *
     * If the application instance {@code app} is not null, this method will invoke
     * the {@code stop()} method to cease its operations and release any resources
     * associated with it. This ensures a proper shutdown and prevents resource leaks.
     */
    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.getJavalinInstance().stop();
        }
    }

    @BeforeEach
    public void clearInstance() throws SQLException {
        if (app != null) {
            try(Connection connection = app.getServerDataSource().getConnection()) {
                connection.createStatement().execute("TRUNCATE TABLE JOBS");
            }
        }
    }

    @Test
    @Order(1)
    void testGetWPSCapabilities() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String serviceParam = URLEncoder.encode("WPS", StandardCharsets.UTF_8);
        String requestParam = URLEncoder.encode("GetCapabilities", StandardCharsets.UTF_8);
        URI uri = URI.create(BASE_URL + "?service=" + serviceParam + "&VERSION=1.0.0&request=" + requestParam);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertNotNull(body);
        // Check if XML is valid - will throw an exception if not valid
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(new InputSource(new StringReader(body)));
        
        assertTrue(body.contains("<wps:Capabilities "));
        assertTrue(body.contains("Database_Manager:Display_Database"));
    }

    /**
     * Tests the DescribeProcess operation of the Web Processing Service (WPS).
     *
     * This method performs the following steps:
     * - Creates an HTTP GET request for the WPS DescribeProcess operation by specifying
     *   the service as "WPS", the request type as "DescribeProcess", and an identifier
     *   representing the process "Geometric_Tools:Screen_to_building".
     * - Sends the request using {@link HttpClient} and retrieves the response.
     * - Validates that the HTTP response status code is 200 (OK).
     * - Ensures that the response body is not null.
     * - Checks that the response body contains:
     *   - The XML element `<wps:ProcessDescriptions>`.
     *   - A description for the process, mentioning "Convert screens to building format."
     *   - Detailed information about the process functionality, including conversions and
     *     optional merging with a building table layer.
     *
     * @throws Exception if an error occurs during the HTTP request, response handling, or validation steps.
     */
    @Test
    @Order(2)
    void testGetWPSDescribeProcess() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String serviceParam = URLEncoder.encode("WPS", StandardCharsets.UTF_8);
        String requestParam = URLEncoder.encode("DescribeProcess", StandardCharsets.UTF_8);
        URI uri = URI.create(BASE_URL + "?service=" + serviceParam + "&VERSION=1.0.0&request=" + requestParam + "&identifier=Receivers:Delaunay_Grid");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertNotNull(body);
        // Check if XML is valid - will throw an exception if not valid
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(new InputSource(new StringReader(body)));
        // Check content
        assertTrue(body.contains("<wps:ProcessDescriptions "));
        assertTrue(body.contains("Receivers:Delaunay_Grid"));
    }

    /**
     * Tests the Execute operation of the Web Processing Service (WPS) using a POST request.
     *
     * This method performs the following actions:
     * - Constructs an HTTP POST request with an XML payload for executing the
     *   "Database_Manager:Clean_Database" process.
     * - Sends the request to the WPS server using {@link HttpClient}.
     * - Validates that the HTTP response status code is 200 (OK).
     * - Ensures that the response body is not null.
     * - Verifies that the response body contains the expected "result" element.
     *
     * The XML payload specifies the WPS service, version, process identifier, input
     * parameters, and raw data output format for the Execute operation.
     *
     * @throws Exception if an error occurs during the HTTP request, response handling, or validation steps.
     */
    @Test
    @Order(3)
    void testPostWPSExecute() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String requestBody ="<p0:Execute xmlns:p0=\"http://www.opengis.net/wps/1.0.0\" " +
                "service=\"WPS\" version=\"1.0.0\"><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">Database_Manager:Clean_Database</p1:Identifier><p0:DataInputs><p0:Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">areYouSure</p1:Identifier><p0:Data><p0:LiteralData>true</p0:LiteralData></p0:Data></p0:Input></p0:DataInputs><p0:ResponseForm><p0:RawDataOutput><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">result</p1:Identifier></p0:RawDataOutput></p0:ResponseForm></p0:Execute>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "text/xml")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        assertTrue(response.body().contains("dropped"));

        try(Connection connection = app.getServerDataSource().getConnection()) {
            List<Map<String, Object>> jobs = DatabaseManagement.getJobs(connection, -1);
            assertEquals(1, jobs.size());
            assertEquals("Database_Manager:Clean_Database", jobs.get(0).get("script").toString());
            assertEquals(JobStates.COMPLETED.name(), jobs.get(0).get("status").toString());
        }
    }



    /**
     * Test Delaunay script
     *
     * @throws Exception if an error occurs during the HTTP request, response handling, or validation steps.
     */
    @Test
    @Order(4)
    void testPostWPSDelaunayExecute() throws Exception {
        // Insert Data
        try(Connection connection = app.getUserDataSource(1).getConnection()) {
            GeoJsonRead.importTable(connection,
                    NoiseModellingServerHttpTest.class.getResource("wpsinput/BUILDINGS_LOW_HEIGHT.geojson").getFile(),
                    ValueBoolean.TRUE);
            try(Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE ROADS(id integer primary key, geom geometry(LineStringZ, 2154))");
                statement.execute("INSERT INTO ROADS VALUES(1, ST_GeomFromText('LINESTRING Z (491283" +
                        ".47973571467446163 6772700.14766194019466639 0, 491298.31839100952493027 6772724" +
                        ".17215146496891975 0, 491352.2851671117823571 6772724.08382613584399223 0, 491352" +
                        ".2851671117823571 6772724.08382613584399223 0)', 2154))");
            }
        }
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = "<p0:Execute xmlns:p0=\"http://www.opengis.net/wps/1.0.0\" service=\"WPS\" version=\"1.0" +
                ".0\"><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">Receivers:Delaunay_Grid</p1:Identifier><p0:DataInputs><p0:Input><p1:Identifier " +
                "xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">tableBuilding</p1:Identifier><p0:Data><p0:LiteralData>BUILDINGS_LOW_HEIGHT</p0:LiteralData></p0:Data></p0" +
                ":Input><p0:Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">sourcesTableName</p1:Identifier><p0:Data><p0:LiteralData>ROADS</p0:LiteralData></p0:Data></p0" +
                ":Input><p0:Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">exportTrianglesGeometries</p1:Identifier><p0:Data><p0:LiteralData>true</p0:LiteralData></p0" +
                ":Data></p0:Input><p0:Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">isoSurfaceInBuildings</p1:Identifier><p0:Data><p0:LiteralData>false</p0:LiteralData></p0" +
                ":Data></p0:Input></p0:DataInputs><p0:ResponseForm><p0:RawDataOutput><p1:Identifier " +
                "xmlns:p1=\"http://www.opengis.net/ows/1" +
                ".1\">result</p1:Identifier></p0:RawDataOutput></p0:ResponseForm></p0:Execute>";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "text/xml")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        try(Connection connection = app.getUserDataSource(1).getConnection()) {
            // debug export table triangles as geojson
            // GeoJsonWrite.exportTable(connection, "target/TRIANGLES.geojson", "TRIANGLES");
            // Check if there is a triangle at the location of the building in x,y location 491303.97 6772708.80
            // No triangle should be under the buildings
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM TRIANGLES WHERE " +
                    "ST_Contains(the_geom, ST_GeomFromText('POINT(491303.97 6772708.80)', 2154))")) {
                try(ResultSet rs = preparedStatement.executeQuery()) {
                    assertFalse(rs.next());
                }
            }
            // An area with a triangle at 491308.588, 6772710.399
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM TRIANGLES WHERE " +
                    "ST_Contains(the_geom, ST_GeomFromText('POINT(491325.310 6772704.089)', 2154))")) {
                try(ResultSet rs = preparedStatement.executeQuery()) {
                    assertTrue(rs.next());
                }
            }
        }
    }

    @Test
    void testPostWPSChainedExecution() throws Exception {
        try(InputStream inputStream = TestParseWPSQueries.class.getResourceAsStream("wps_parse/chainedExecute1.xml")) {
            assertNotNull(inputStream);
            String requestBody = new String(inputStream.readAllBytes());
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "text/xml")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            // Check if BUILDINGS_LOW_HEIGHT table exists

            try(Connection connection = app.getUserDataSource(1).getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM BUILDINGS_LOW_HEIGHT")) {
                    try(ResultSet rs = preparedStatement.executeQuery()) {
                        assertTrue(rs.next());
                    }
                }
            }
            // Check if the content of the buildings_low_height table is printed as html in response body
            assertTrue(response.body().contains("The total number of rows is 9"));
        }
    }

    @Test
    void testAsynchronousWPSExecute() throws Exception {
        try(InputStream inputStream = TestParseWPSQueries.class.getResourceAsStream("wps_parse/asynchronousExecute.xml")) {
                assertNotNull(inputStream);
                String requestBody = new String(inputStream.readAllBytes());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "text/xml")
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());

            ExecuteResponseType executeResponseType = OwsController.parseExecuteResponse(new ByteArrayInputStream(response.body().getBytes()));
            String requestResponseUrl = executeResponseType.getStatusLocation();

            // Polls status endpoint until process completes or fails
            while (!(executeResponseType.getStatus().getProcessFailed() != null ||
                    executeResponseType.getStatus().getProcessSucceeded() != null)) {
                Thread.sleep(500);
                request = HttpRequest.newBuilder()
                        .uri(URI.create(requestResponseUrl))
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                executeResponseType = OwsController.parseExecuteResponse(new ByteArrayInputStream(response.body().getBytes()));
                // log status and progression
                if(executeResponseType.getStatus().getProcessStarted() != null) {
                    logger.info("Progress: {} %",executeResponseType.getStatus().getProcessStarted().getPercentCompleted());
                }
            }
            assertNull(executeResponseType.getStatus().getProcessFailed());
        }
    }

}
