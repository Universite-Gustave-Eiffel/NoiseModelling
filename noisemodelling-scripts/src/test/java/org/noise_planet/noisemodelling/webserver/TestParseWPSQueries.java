package org.noise_planet.noisemodelling.webserver;

import net.opengis.wps10.ExecuteType;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.log4j.PropertyConfigurator;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.webserver.script.ExecutionPlan;
import org.noise_planet.noisemodelling.webserver.script.WpsXmlDocumentGenerator;
import org.noise_planet.noisemodelling.webserver.script.ScriptMetadata;
import org.noise_planet.noisemodelling.webserver.script.WpsScriptWrapper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseWPSQueries {

    @BeforeAll
    public static void init() {
        PropertyConfigurator.configure(
                Objects.requireNonNull(NoiseModellingServerHttpTest.class.getResource("log4j.properties")));
    }

    @Test
    public void testDelaunayParse() throws IOException, ParserConfigurationException, SAXException {

        // Build ScriptWrapper
        Map<String, ScriptMetadata> wrappers = getWrappers();
        assertNotEquals(0, wrappers.size());
        // look for the script named Delaunay_Grid
        ScriptMetadata scriptMetadata = wrappers.get("Receivers:Delaunay_Grid");
        assertNotNull(scriptMetadata);
        assertEquals("Receivers:Delaunay_Grid", scriptMetadata.id);

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

        // Provide request body as an input stream
        ExecutionPlan executionPlan = OwsController.generateExecutionPlanFromWPS(new ByteArrayInputStream(requestBody.getBytes()), wrappers);
        assertNotNull(executionPlan);
        // Check inputs values and type
        assertEquals("BUILDINGS_LOW_HEIGHT", executionPlan.getInputs().get("tableBuilding"));
        assertEquals(Boolean.class, executionPlan.getInputs().get("exportTrianglesGeometries").getClass());
        assertEquals(true, executionPlan.getInputs().get("exportTrianglesGeometries"));
        assertEquals(Boolean.class, executionPlan.getInputs().get("isoSurfaceInBuildings").getClass());
        assertEquals(false, executionPlan.getInputs().get("isoSurfaceInBuildings"));

    }

    private static @NonNull Map<String, ScriptMetadata> getWrappers() throws IOException {
        Map<String, ScriptMetadata> wrappers = WpsScriptWrapper.buildScriptWrappers(WpsScriptWrapper.scanScriptsGrouped(ClassLoader.getSystemClassLoader(),
                Path.of("scripts")));
        return wrappers;
    }


    @Test
    public void testGeometryReturnParse() throws IOException, ParserConfigurationException, SAXException {

        // Build ScriptWrapper
        Map<String, ScriptMetadata> wrappers = getWrappers();
        assertNotEquals(0, wrappers.size());
        // look for the script named Delaunay_Grid
        ScriptMetadata scriptMetadata = wrappers.get("Database_Manager:Table_Visualization_Map");
        assertNotNull(scriptMetadata);
        assertEquals("Database_Manager:Table_Visualization_Map", scriptMetadata.id);

        assertTrue(scriptMetadata.outputs.containsKey("result"));
        assertEquals(Geometry.class, scriptMetadata.outputs.get("result").type);

        String describeProcessXML = WpsXmlDocumentGenerator.generateDescribeProcessXML(scriptMetadata);

        // Expect XML output with WKT Geometry type
        assertTrue(describeProcessXML.contains("application/wkt"));
        assertTrue(describeProcessXML.contains("<ows:Identifier>Database_Manager:Table_Visualization_Map</ows:Identifier>"));
    }

    @Test
    public void testGenerateCapabilitiesXML() throws IOException {
        // Build ScriptWrapper
        Map<String, ScriptMetadata> wrappers = getWrappers();
        assertNotEquals(0, wrappers.size());

        String capabilitiesXML = WpsXmlDocumentGenerator.generateCapabilitiesXML(wrappers);

        // Check that capabilities XML is not empty
        assertNotNull(capabilitiesXML);
        assertFalse(capabilitiesXML.isEmpty());

        // Expect XML to contain WPS capabilities elements
        assertTrue(capabilitiesXML.contains("Capabilities"));
        assertTrue(capabilitiesXML.contains("ProcessOfferings"));
        assertTrue(capabilitiesXML.contains("ows:Identifier"));
        assertTrue(capabilitiesXML.contains("Receivers:Building_Grid"));
    }

    @Test
    public void testParseChainedExecuteQuery() throws IOException, ParserConfigurationException, SAXException {
        // Build ScriptWrapper
        Map<String, ScriptMetadata> wrappers = getWrappers();
        assertNotEquals(0, wrappers.size());

        try(InputStream inputStream = TestParseWPSQueries.class.getResourceAsStream("wps_parse/chainedExecute1.xml")) {
            assertNotNull(inputStream);
            ExecutionPlan executionPlan = OwsController.generateExecutionPlanFromWPS(inputStream, wrappers);
            assertNotNull(executionPlan);
            assertEquals("Database_Manager:Table_Visualization_Data", executionPlan.getScriptMetadata().id);
            assertTrue(executionPlan.getInputs().containsKey("tableName"));
            assertInstanceOf(ExecutionPlan.class, executionPlan.getInputs().get("tableName"));
            ExecutionPlan chainedExecutionPlan = (ExecutionPlan) executionPlan.getInputs().get("tableName");
            assertEquals("Import_and_Export:Import_File", chainedExecutionPlan.getScriptMetadata().id);
            assertEquals("src/test/resources/org/noise_planet/noisemodelling/webserver/wpsinput/BUILDINGS_LOW_HEIGHT.geojson", chainedExecutionPlan.getInputs().get("pathFile"));
            assertEquals("outputTable", chainedExecutionPlan.getChainedOutputKey());
        }

    }

    @Test
    public void testParseChainedExecuteQuery2() throws IOException, ParserConfigurationException, SAXException {
        // Build ScriptWrapper
        Map<String, ScriptMetadata> wrappers = getWrappers();
        assertNotEquals(0, wrappers.size());

        try(InputStream inputStream = TestParseWPSQueries.class.getResourceAsStream("wps_parse/chainedExecute2.xml")) {
            assertNotNull(inputStream);
            ExecutionPlan executionPlan = OwsController.generateExecutionPlanFromWPS(inputStream, wrappers);
            // Last process
            assertNotNull(executionPlan);
            assertEquals("Import_and_Export:Export_Table", executionPlan.getScriptMetadata().id);
            assertTrue(executionPlan.getInputs().containsKey("tableToExport"));
            assertInstanceOf(ExecutionPlan.class, executionPlan.getInputs().get("tableToExport"));
            // Linked previous process
            ExecutionPlan chainedExecutionPlan = (ExecutionPlan) executionPlan.getInputs().get("tableToExport");
            assertEquals("Acoustic_Tools:Create_Isosurface", chainedExecutionPlan.getScriptMetadata().id);
            assertTrue(chainedExecutionPlan.getInputs().containsKey("resultTable"));
            assertInstanceOf(ExecutionPlan.class, chainedExecutionPlan.getInputs().get("resultTable"));
            // Linked previous process
            ExecutionPlan chainedExecutionPlan2 = (ExecutionPlan) chainedExecutionPlan.getInputs().get("resultTable");
            assertEquals("NoiseModelling:Noise_level_from_source", chainedExecutionPlan2.getScriptMetadata().id);
            assertTrue(chainedExecutionPlan2.getInputs().containsKey("tableReceivers"));
            assertInstanceOf(ExecutionPlan.class, chainedExecutionPlan2.getInputs().get("tableReceivers"));
            // Linked previous process
            ExecutionPlan chainedExecutionPlan3 = (ExecutionPlan) chainedExecutionPlan2.getInputs().get("tableReceivers");
            assertEquals("Receivers:Delaunay_Grid", chainedExecutionPlan3.getScriptMetadata().id);
            assertTrue(chainedExecutionPlan2.getInputs().containsKey("tableBuilding"));
            assertInstanceOf(ExecutionPlan.class, chainedExecutionPlan2.getInputs().get("tableBuilding"));
            // Linked previous process
            ExecutionPlan chainedExecutionPlan4 = (ExecutionPlan) chainedExecutionPlan3.getInputs().get("tableBuilding");
            assertEquals("Import_and_Export:Import_File", chainedExecutionPlan4.getScriptMetadata().id);
        }
    }
}
