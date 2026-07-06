/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 * <p>
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.scripts;

import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.AssumptionLoggerExtension;
import org.noise_planet.noisemodelling.VersionUtils;
import org.noise_planet.noisemodelling.runner.Main;
import org.noise_planet.noisemodelling.scripts.Database_Manager.Clean_Database;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AssumptionLoggerExtension.class)
public class MainTest {
    String dbName = UUID.randomUUID().toString().replace("-", "");

    @BeforeEach
    public void initLogger() {
        PropertyConfigurator.configure(
                Objects.requireNonNull(VersionUtils.class.getResource("log4j_tests.properties")));
    }

    @AfterEach
    public void tearDown() throws Exception {
        Logging.clearAppenders();
    }

    @Test
    public void testSetHeight(@TempDir File temp) throws Exception {
        String receiverPath = MainTest.class.getResource("receivers.shp").getPath();
        Main.parseArgsAndRun("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--pathFile", receiverPath);

        Main.parseArgsAndRun("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Geometric_Tools/Set_Height.groovy",
                "--tableName", "RECEIVERS", "--height" , "1.5");
    }

    @Test
    public void testConnectionToPostGIS(@TempDir File temp) throws Exception {
        String pgHost = System.getenv("POSTGRES_HOST");
        Assumptions.assumeTrue(pgHost != null && !pgHost.isEmpty(), "POSTGRES_HOST is not defined, skipping PostGIS test");

        String pgUser = Optional.ofNullable(System.getenv("POSTGRES_USER")).orElse("noisemodelling");
        String pgPass = Optional.ofNullable(System.getenv("POSTGRES_PASSWORD")).orElse("noisemodelling");
        String pgPort = Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432");
        String pgDb = Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db");

        String buildingsPath = MainTest.class.getResource("buildings.shp").getPath();

        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--pathFile", buildingsPath,
                "--tableName", "BUILDINGS");

        try (Connection connection = JdbcTestCase.createPostgisDataSourceFromEnv().getConnection()) {
            assertTrue(JDBCUtilities.tableExists(connection, "BUILDINGS"), "Table BUILDINGS should exist in PostGIS");
        }
    }

    @Test
    public void testPostGISTutorialGroovy(@TempDir File temp) throws Exception {
        String pgHost = System.getenv("POSTGRES_HOST");
        Assumptions.assumeTrue(pgHost != null && !pgHost.isEmpty(), "POSTGRES_HOST is not defined, skipping PostGIS test");

        String pgUser = Optional.ofNullable(System.getenv("POSTGRES_USER")).orElse("noisemodelling");
        String pgPass = Optional.ofNullable(System.getenv("POSTGRES_PASSWORD")).orElse("noisemodelling");
        String pgPort = Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432");
        String pgDb = Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db");

        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/test/groovy/org/noise_planet/noisemodelling/scripts/get_started_tutorial_complex.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--resourcesFolder", "src/test/resources/org/noise_planet/noisemodelling/scripts");

        try (Connection connection = JdbcTestCase.createPostgisDataSourceFromEnv().getConnection()) {
            assertTrue(JDBCUtilities.tableExists(connection, "RECEIVERS_LEVEL"), "Table RECEIVERS_LEVEL should exist in PostGIS");
        }
    }

    @Test
    public void testPostGISDelaunayGrid(@TempDir File temp) throws Exception {
        String pgHost = System.getenv("POSTGRES_HOST");
        Assumptions.assumeTrue(pgHost != null && !pgHost.isEmpty(), "POSTGRES_HOST is not defined, skipping PostGIS test");

        String pgUser = Optional.ofNullable(System.getenv("POSTGRES_USER")).orElse("noisemodelling");
        String pgPass = Optional.ofNullable(System.getenv("POSTGRES_PASSWORD")).orElse("noisemodelling");
        String pgPort = Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432");
        String pgDb = Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db");

        String buildingsPath = MainTest.class.getResource("buildings.shp").getPath();
        String roadsPath = MainTest.class.getResource("ROADS2.shp").getPath();

        DataSource postgisDataSource = JdbcTestCase.createPostgisDataSourceFromEnv();

        try (Connection connection = postgisDataSource.getConnection()) {
            connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS TESTDELAUNAY");
            new Clean_Database().exec(connection, Map.of("areYouSure", true, "schema", "testdelaunay"));
            assertFalse(JDBCUtilities.tableExists(connection, "testdelaunay.receivers"), "Table RECEIVERS should not exist in PostGIS");
        }

        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--pathFile", buildingsPath,
                "--tableName", "testdelaunay.buildings");

        try(Connection connection = postgisDataSource.getConnection()) {
            // Remove height field in BUILDINGS table (to check if it is working without this field)
            connection.createStatement().execute("ALTER TABLE testdelaunay.buildings DROP COLUMN HEIGHT");
        }


        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--pathFile", roadsPath,
                "--tableName", "testdelaunay.roads");

        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Receivers/Delaunay_Grid.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--tableBuilding", "testdelaunay.buildings",
                "--sourcesTableName", "testdelaunay.roads",
                "--outputTableName", "testdelaunay.receivers"
                );

        try (Connection connection = postgisDataSource.getConnection()) {
            assertTrue(JDBCUtilities.tableExists(connection, "testdelaunay.receivers"), "Table RECEIVERS should exist in PostGIS");
        }
    }
}
