/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Create_Isosurface
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Agent_Exposure
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Import_Activities
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Receivers_From_Activities_Closest
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Traffic_From_Events
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Folder
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Test parsing of zip file using H2GIS database
 */
class TestTutorials extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTutorials.class)


    void testTutorialGetStarted() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)


        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("ground_type.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("ROADS2.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("dem.geojson").getPath(),
                 "inputSRID": "2154"])


        new Noise_level_from_traffic().exec(connection,
                ["tableBuilding"        : "BUILDINGS",
                 "tableRoads"           : "ROADS2",
                 "tableReceivers"       : "RECEIVERS",
                 "tableGroundAbs"       : "ground_type",
                 "tableDEM"             : "dem",
                 "confDiffHorizontal"   : true,
                 "confMaxSrcDist"       : 2000.0,
                 "confReflOrder"        : 0,
                 "confMaxError"         : 3.0,
                 "frequencyFieldPrepend": "LW"])

        def countReceivers = sql.firstRow("SELECT COUNT(*) FROM RECEIVERS")[0] as Integer
        def countResult = sql.firstRow("SELECT COUNT(*) FROM $NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME".toString())[0] as Integer

        assertEquals(4*countReceivers, countResult)

        def minLevel = sql.firstRow("SELECT MIN(LW1000) FROM $NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME".toString())[0] as Double

        assertNotSame(-99.0, minLevel)
    }



    void testTutorialPointSource() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)

        new Import_Folder().exec(connection,
                ["pathFolder": TestImportExport.class.getResource("TutoPointSource/").getPath(),
                 "inputSRID" : "2154",
                 "importExt" : "geojson"])

        // Check SRID
        assertEquals(2154, GeometryTableUtilities.getSRID(connection, TableLocation.parse("BUILDINGS")))

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("SOURCES"))
        assertTrue(res.contains("BUILDINGS"))

        // generate a grid of receivers using the buildings as envelope
        new Delaunay_Grid().exec(connection, [maxArea: 600, tableBuilding: "BUILDINGS",
                                                          sourcesTableName : "SOURCES" , height: 1.6])


        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("RECEIVERS"))


        res = new Noise_level_from_source().exec(connection, ["tableSources"         : "SOURCES",
                                                              "tableBuilding"        : "BUILDINGS",
                                                              "tableReceivers"       : "RECEIVERS",
                                                              "confReflOrder"        : 1,
                                                              "confDiffVertical"     : true,
                                                              "confDiffHorizontal"   : true,
                                                              "frequencyFieldPrepend": "LW"])

        res =  new Display_Database().exec(connection, [])

        // Check database
        def output = new Table_Visualization_Data().exec(connection, ["tableName": NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME])

        assertTrue(res.contains(NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))

        assertTrue(output.contains("PERIOD"))

        // Check export geojson
        File testPath = new File("build/tmp/tutoPointSource.geojson")

        if(testPath.exists()) {
            testPath.delete()
        }

        new Export_Table().exec(connection,
                ["exportPath"   : "build/tmp/tutoPointSource.geojson",
                 "tableToExport": NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME])


    }


    void testTutorialPointSourceDirectivity() {
        Logger logger = LoggerFactory.getLogger(TestTutorials.class)

        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)

        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("buildings.shp").getPath(),
                tableName : "BUILDINGS"])

        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("TutoPointSourceDirectivity/Point_Source.geojson").getPath(),
                tableName : "Point_Source"])

        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("ground_type.shp").getPath(),
                tableName : "ground_type"])

        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("TutoPointSourceDirectivity/Directivity.csv").getPath(),
                tableName : "Directivity"])

        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("dem.geojson").getPath(),
                tableName : "DEM"])



        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("BUILDINGS"))
        assertTrue(res.contains("DIRECTIVITY"))
        assertTrue(res.contains("GROUND"))
        assertTrue(res.contains("POINT_SOURCE"))
        assertTrue(res.contains("DEM"))

        // generate a grid of receivers using the buildings as envelope
        logger.info(new Delaunay_Grid().exec(connection, [maxArea: 60, tableBuilding: "BUILDINGS",
                                                          sourcesTableName : "POINT_SOURCE" , height: 1.6]));


        new Export_Table().exec(connection, [exportPath:"build/tmp/receivers.shp", tableToExport: "RECEIVERS"])
        new Export_Table().exec(connection, [exportPath:"build/tmp/TRIANGLES.shp", tableToExport: "TRIANGLES"])

        new Noise_level_from_source().exec(connection, [tableBuilding: "BUILDINGS", tableSources:"POINT_SOURCE",
                                                        tableReceivers : "RECEIVERS",
                                                        tableGroundAbs: "GROUND_TYPE",
                                                        tableSourceDirectivity: "DIRECTIVITY",
                                                        confMaxSrcDist : 800,
                                                        tableDEM: "DEM",
                                                        "frequencyFieldPrepend": "LW"
                                                        ])

        new Create_Isosurface().exec(connection,
                [resultTable: NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME,
                 smoothCoefficient : 0.4])

        new Export_Table().exec(connection, [exportPath:"build/tmp/CONTOURING_NOISE_MAP.shp", tableToExport: "CONTOURING_NOISE_MAP"])

        new Export_Table().exec(connection,
                [exportPath:"build/tmp/TUTO_DIR_RECEIVERS_LEVEL.shp",
                 tableToExport: NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME])

        def columnNames = JDBCUtilities.getColumnNames(connection, NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)

        assertTrue(columnNames.contains("IDRECEIVER"))
        assertTrue(columnNames.contains("PERIOD"))
        assertTrue(columnNames.contains("LW500"))
        assertTrue(columnNames.contains("LAEQ"))
        assertTrue(columnNames.contains("LEQ"))
    }


    void testTutorialMatsim() {
        Logger logger = LoggerFactory.getLogger(TestTutorials.class)
        Sql sql = new Sql(connection)

        Path tempDataDir = new File("build/tmp/matsim/").toPath();

        Files.createDirectories(tempDataDir);

        // URL of the file to download
        String fileUrl = "https://github.com/Symexpo/matsim-noisemodelling/releases/download/v5.0.0/scenario_matsim.zip";

        // Create a temporary directory
        Path zipFilePath = tempDataDir.resolve("scenario_matsim.zip");

        String osmFile = tempDataDir.toString() + "/nantes_mini.osm.pbf";
        String matsimFolder = tempDataDir.toString();
        String resultsFolder = tempDataDir.toString() + "/results/";
        Files.createDirectories(Path.of(resultsFolder));
        String populationFactor = "0.01"; // 0.001 for 1/1000 of the population

        int timeBinSize = 900;
        int timeBinMin = 0;
        int timeBinMax = 86400;

        Path buildingsPath = Path.of(tempDataDir.toString() + "/results/BUILDINGS.geojson");
        Path roadsPath = Path.of(tempDataDir.toString() + "/results/MATSIM_ROADS.geojson");

        int srid = 2154

        if (!zipFilePath.toFile().exists()) {
            // Download the file
            InputStream ins = new URL(fileUrl).openStream();
            Files.copy(ins, zipFilePath);

            // Unzip the file
            ZipFile zipFile = new ZipFile(zipFilePath.toFile());
            zipFile.stream().forEach({ entry ->
                try {
                    Path entryPath = tempDataDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        InputStream insz = zipFile.getInputStream(entry);
                        Files.copy( insz, entryPath );
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Clean up
            zipFile.close();
        }
        new Import_OSM().exec(connection, Map.of(
                "pathFile", osmFile,
                "targetSRID", srid,
                "ignoreGround", true,
                "ignoreBuilding", false,
                "ignoreRoads", true,
                "removeTunnels", false
        ));

        sql.execute("DELETE FROM BUILDINGS WHERE ST_IsEmpty(THE_GEOM);");

        new Export_Table().exec(connection, Map.of(
                "tableToExport", "BUILDINGS",
                "exportPath", Paths.get(resultsFolder, "BUILDINGS.geojson")
        ));

        Map<String, Object> params = new HashMap<>();
        params.put("folder", matsimFolder);
        params.put("outTableName", "MATSIM_ROADS");
        params.put("link2GeometryFile", Paths.get(matsimFolder, "detailed_network.csv")); // absolute path
        params.put("timeBinSize", timeBinSize);
        params.put("timeBinMin", timeBinMin);
        params.put("timeBinMax", timeBinMax);
        params.put("skipUnused", true);
        params.put("exportTraffic", true);
        params.put("SRID", srid);
        params.put("perVehicleLevel", true);
        params.put("populationFactor", populationFactor);

        new Traffic_From_Events().exec(connection, params);

        new Building_Grid().exec(connection, Map.of(
                "delta",  5.0,
                "tableBuilding", "BUILDINGS",
                "receiversTableName", "RECEIVERS",
                "height", 4.0,
                "fenceTableName", "BUILDINGS"
        ));

        new Import_Activities().exec(connection, Map.of(
                "facilitiesPath", Paths.get(matsimFolder, "output_facilities.xml.gz"),
                "SRID", srid,
                "outTableName", "ACTIVITIES"
        ));

        new Receivers_From_Activities_Closest().exec(connection, Map.of(
                "activitiesTable", "ACTIVITIES",
                "receiversTable", "RECEIVERS",
                "outTableName", "ACTIVITIES_RECEIVERS"
        ));

        params = new HashMap<>();
        params.put("tableBuilding", "BUILDINGS");
        params.put("tableReceivers", "ACTIVITIES_RECEIVERS");
        params.put("tableSources", "MATSIM_ROADS");
        params.put("tableSourcesEmission", "MATSIM_ROADS_LW");
        params.put("confMaxSrcDist", 50);
        params.put("confMaxReflDist", 10);
        params.put("confReflOrder", 0);
        params.put("confSkipLevening", true);
        params.put("confSkipLnight", true);
        params.put("confSkipLden", true);
        params.put("confExportSourceId", false);
        params.put("confDiffVertical", false);
        params.put("confDiffHorizontal", false);

        new Noise_level_from_source().exec(connection, params);

        sql.execute("DROP TABLE IF EXISTS ACTIVITIES_RECEIVERS_LEVEL");
        sql.execute("ALTER TABLE RECEIVERS_LEVEL RENAME TO ACTIVITIES_RECEIVERS_LEVEL");

        params = new HashMap<>();
        params.put("experiencedPlansFile", Paths.get(matsimFolder, "output_experienced_plans.xml.gz"));
        params.put("plansFile", Paths.get(matsimFolder, "output_plans.xml.gz"));
        params.put("personsCsvFile", Paths.get(matsimFolder, "output_persons.csv.gz"));
        params.put("SRID", srid);
        params.put("receiversTable", "ACTIVITIES_RECEIVERS");
        params.put("outTableName", "EXPOSURES");
        params.put("dataTable", "ACTIVITIES_RECEIVERS_LEVEL");
        params.put("timeBinSize", timeBinSize);
        params.put("timeBinMin", timeBinMin);
        params.put("timeBinMax", timeBinMax);

        new Agent_Exposure().exec(connection, params);

        new Export_Table().exec(connection, Map.of(
                "tableToExport", "MATSIM_ROADS",
                "exportPath", Paths.get(resultsFolder, "MATSIM_ROADS.geojson")
        ));

        new Export_Table().exec(connection, Map.of(
                "tableToExport", "ACTIVITIES_RECEIVERS_LEVEL",
                "exportPath", Paths.get(resultsFolder, "ACTIVITIES_RECEIVERS_LEVEL.shp")
        ));

        new Export_Table().exec(connection, Map.of(
                "tableToExport", "EXPOSURES",
                "exportPath", Paths.get(resultsFolder, "EXPOSURES.shp")
        ));

        assertTrue(Paths.get(resultsFolder, "ACTIVITIES_RECEIVERS_LEVEL.shp").toFile().exists());
        assertTrue(Paths.get(resultsFolder, "EXPOSURES.shp").toFile().exists());
        assertTrue(buildingsPath.toFile().exists());
        assertTrue(roadsPath.toFile().exists());
    }
}
