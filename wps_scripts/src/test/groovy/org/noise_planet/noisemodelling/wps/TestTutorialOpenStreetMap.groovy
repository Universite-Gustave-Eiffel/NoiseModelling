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
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Create_Isosurface
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestTutorialOpenStreetMap extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTutorialOpenStreetMap.class)

    void testTutorial() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)
        // Import OSM file
        res = new Import_OSM().exec(connection,
                ["pathFile": TestTutorialOpenStreetMap.getResource("map.osm.gz").getPath(),
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
        assertEquals(90, rowResult[0] as Double, 1.0)
        assertEquals(81, rowResult[1] as Double, 1.0)
        assertEquals(78, rowResult[2] as Double, 1.0)

        res = new Noise_level_from_source().exec(connection, ["tableSources"  : "LW_ROADS",
                                                              "tableBuilding" : "BUILDINGS",
                                                              "tableGroundAbs": "GROUND",
                                                              "tableReceivers": "RECEIVERS"])

        res =  new Display_Database().exec(connection, [])

        // Check database
        new Table_Visualization_Data().exec(connection, ["tableName": "LDEN_GEOM"])

        assertTrue(res.contains("LDEN_GEOM"))



        rowResult = sql.firstRow("SELECT MAX(LEQ), MAX(LAEQ) FROM LDEN_GEOM")
        assertEquals(88, rowResult[0] as Double, 5.0)
        assertEquals(87, rowResult[1] as Double, 5.0)

        res = new Create_Isosurface().exec(connection, [resultTable: "LDEN_GEOM"]);

        assertTrue(JDBCUtilities.tableExists(connection, "CONTOURING_NOISE_MAP"))
    }

}
