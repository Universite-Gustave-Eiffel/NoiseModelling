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
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Folder
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestTutorials extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTutorials.class)

    @Test
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

    @Test
    public void testTutorialPointSourceDirectivity() {
        Sql sql = new Sql(connection)

        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)



    }
}
