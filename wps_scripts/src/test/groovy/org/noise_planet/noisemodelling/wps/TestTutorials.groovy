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
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Folder
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Create_Isosurface
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestTutorials extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTutorials.class)

    @Test
    void testTutorialOSM() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)
        // Import OSM file
        res = new Import_OSM().exec(connection,
                ["pathFile": TestTutorials.getResource("map.osm.gz").getPath(),
                 "targetSRID" : 2154,
                 "convert2Building" : false,
                 "convert2Ground" : false,
                 "convert2Roads" : false])

        // Check SRID
        assertEquals(2154, SFSUtilities.getSRID(connection, TableLocation.parse("BUILDINGS")))

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("GROUND"))
        assertTrue(res.contains("BUILDINGS"))
        assertTrue(res.contains("ROADS"))

        // Check export geojson
        File testPath = new File("target/test.geojson")

        if(testPath.exists()) {
            testPath.delete()
        }

        res = new Export_Table().exec(connection, ["exportPath" : "target/test.geojson",
                                                   "tableToExport": "BUILDINGS"])
        assertTrue(testPath.exists())

        // Check regular grid

        res = new Delaunay_Grid().exec(connection, ["sourcesTableName" : "ROADS",
                                                    "tableBuilding": "BUILDINGS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("RECEIVERS"))
        assertTrue(res.contains("TRIANGLES"))

        new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("LW_ROADS"))

        def rowResult = sql.firstRow("SELECT MAX(LWD500), MAX(LWE500), MAX(LWN500) FROM LW_ROADS")
        assertEquals(94, rowResult[0] as Double, 1.0)
        assertEquals(84, rowResult[1] as Double, 1.0)
        assertEquals(80, rowResult[2] as Double, 1.0)

        res = new Noise_level_from_source().exec(connection, ["tableSources"  : "LW_ROADS",
                                                              "tableBuilding" : "BUILDINGS",
                                                              "tableGroundAbs": "GROUND",
                                                              "tableReceivers": "RECEIVERS"])

        res =  new Display_Database().exec(connection, [])

        // Check database
        new Table_Visualization_Data().exec(connection, ["tableName": "LDEN_GEOM"])

        assertTrue(res.contains("LDEN_GEOM"))



        rowResult = sql.firstRow("SELECT MAX(LEQ), MAX(LAEQ) FROM LDEN_GEOM")
        assertEquals(92, rowResult[0] as Double, 5.0)
        assertEquals(90, rowResult[1] as Double, 5.0)

        res = new Create_Isosurface().exec(connection, [resultTable: "LDEN_GEOM"]);

        assertTrue(JDBCUtilities.tableExists(connection, "CONTOURING_NOISE_MAP"))
    }

    @Test
    void testTutorialPointSource() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)


      /*  String createBuildings = '''-- make sources table
drop table if exists buildings;
create table buildings (PK INTEGER ,  the_geom GEOMETRY, height double );
INSERT INTO buildings (PK, the_geom, height) VALUES (1, ST_GeomFromText('MULTIPOLYGON (((2.5 2.5,2.5 7.5,7.5 7.5,7.5 2.5,2.5 2.5)))'), 5);
INSERT INTO buildings (PK, the_geom, height) VALUES (2, ST_GeomFromText('MULTIPOLYGON (((10 8 ,13.5 8, 13.5 13,10 13, 10 8 )))'), 10);
        '''
        String createSources = '''-- make sources table
        drop table if exists sources;
        create table sources (PK INTEGER ,  the_geom GEOMETRY, LW500 double, LW1000 double );
        INSERT INTO sources (PK, the_geom, LW500,LW1000) VALUES (1, ST_GeomFromText('POINT (5 5 7)'), 90.2,56.4);
        INSERT INTO sources (PK, the_geom, LW500,LW1000) VALUES (2, ST_GeomFromText('POINT (10 5 2)'), 80.2,66.4);
'''*/

        new Import_Folder().exec(connection,
                ["pathFolder": TestImportExport.class.getResource("TutoPointSource/").getPath(),
                 "inputSRID" : "2154",
                 "importExt" : "geojson"])

        // Check SRID
        assertEquals(2154, SFSUtilities.getSRID(connection, TableLocation.parse("BUILDINGS")))

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("SOURCES"))
        assertTrue(res.contains("BUILDINGS"))

        new Regular_Grid().exec(connection, ["sourcesTableName": "SOURCES",
                                             delta             : 0.2,
                                               "buildingTableName"   : "BUILDINGS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("RECEIVERS"))


        res = new Noise_level_from_source().exec(connection, ["tableSources"  : "SOURCES",
                                                              "tableBuilding" : "BUILDINGS",
                                                              "tableReceivers": "RECEIVERS",
                                                              "confReflOrder" : 1,
                                                              "confDiffVertical" : true,
                                                              "confDiffHorizontal" : true,
                                                              "confSkipLevening" : true,
                                                              "confSkipLnight" : true,
                                                              "confSkipLden" : true])

        res =  new Display_Database().exec(connection, [])

        // Check database
        new Table_Visualization_Data().exec(connection, ["tableName": "LDAY_GEOM"])

        assertTrue(res.contains("LDAY_GEOM"))

        def rowResult = sql.firstRow("SELECT MAX(LEQ), MAX(LAEQ) FROM LDAY_GEOM")
        assertEquals(72, rowResult[0] as Double, 5.0)
        assertEquals(69, rowResult[1] as Double, 5.0)

        // Check export geojson
        File testPath = new File("target/tutoPointSource.geojson")

        if(testPath.exists()) {
            testPath.delete()
        }

        new Export_Table().exec(connection,
                ["exportPath"   : "target/tutoPointSource.geojson",
                 "tableToExport": "LDAY_GEOM"])


    }

}
