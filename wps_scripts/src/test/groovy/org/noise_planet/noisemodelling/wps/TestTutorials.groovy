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
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Folder
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
}
