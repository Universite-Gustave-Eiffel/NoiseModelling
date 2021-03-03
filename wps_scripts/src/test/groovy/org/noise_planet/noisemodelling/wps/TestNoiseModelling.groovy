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
import org.junit.Test
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Test parsing of zip file using H2GIS database
 */
class TestNoiseModelling extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestNoiseModelling.class)

    @Test
    void testRoadEmissionFromDEN() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("ROADS2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])


        assertEquals("Calculation Done ! The table LW_ROADS has been created.", res)
    }

    @Test
    void testLdayFromTraffic() {

        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


       String res = new Noise_level_from_traffic().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableRoads"   : "ROADS2",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains("LDAY_GEOM"))

        def sql = new Sql(connection)

        def leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDAY_GEOM")

        assertEquals(83, leqs[0] as Double, 2.0)
        assertEquals(77, leqs[1] as Double, 2.0)
        assertEquals(75, leqs[2] as Double, 2.0)
        assertEquals(76, leqs[3] as Double, 2.0)
        assertEquals(79, leqs[4] as Double, 2.0)
        assertEquals(77, leqs[5] as Double, 2.0)
        assertEquals(68, leqs[6] as Double, 2.0)
        assertEquals(59, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LEVENING_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LEVENING_GEOM")

        assertEquals(78.0, leqs[0] as Double, 2.0)
        assertEquals(72.0, leqs[1] as Double, 2.0)
        assertEquals(70.0, leqs[2] as Double, 2.0)
        assertEquals(72.0, leqs[3] as Double, 2.0)
        assertEquals(74.0, leqs[4] as Double, 2.0)
        assertEquals(72.0, leqs[5] as Double, 2.0)
        assertEquals(63.0, leqs[6] as Double, 2.0)
        assertEquals(54.0, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LNIGHT_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LNIGHT_GEOM")

        assertEquals(75, leqs[0] as Double, 2.0)
        assertEquals(69, leqs[1] as Double, 2.0)
        assertEquals(68, leqs[2] as Double, 2.0)
        assertEquals(69, leqs[3] as Double, 2.0)
        assertEquals(71, leqs[4] as Double, 2.0)
        assertEquals(69, leqs[5] as Double, 2.0)
        assertEquals(60, leqs[6] as Double, 2.0)
        assertEquals(51, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LDEN_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDEN_GEOM")

        assertEquals(84, leqs[0] as Double, 2.0)
        assertEquals(78, leqs[1] as Double, 2.0)
        assertEquals(76, leqs[2] as Double, 2.0)
        assertEquals(77, leqs[3] as Double, 2.0)
        assertEquals(80, leqs[4] as Double, 2.0)
        assertEquals(78, leqs[5] as Double, 2.0)
        assertEquals(69, leqs[6] as Double, 2.0)
        assertEquals(60, leqs[7] as Double, 2.0)
    }

    @Test
    void testLdayFromTrafficWithBuildingsZ() {

        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        new Set_Height().exec(connection,
                ["height": -50,
                 "tableName": "buildings"])

        sql.firstRow("SELECT THE_GEOM FROM buildings")[0]

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        String res = new Noise_level_from_traffic().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableRoads"   : "ROADS2",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains("LDAY_GEOM"))



        def leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDAY_GEOM")

        assertEquals(84, leqs[0] as Double, 2.0)
        assertEquals(77, leqs[1] as Double, 2.0)
        assertEquals(75, leqs[2] as Double, 2.0)
        assertEquals(76, leqs[3] as Double, 2.0)
        assertEquals(79, leqs[4] as Double, 2.0)
        assertEquals(77, leqs[5] as Double, 2.0)
        assertEquals(68, leqs[6] as Double, 2.0)
        assertEquals(59, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LEVENING_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LEVENING_GEOM")

        assertEquals(79, leqs[0] as Double, 2.0)
        assertEquals(72, leqs[1] as Double, 2.0)
        assertEquals(70, leqs[2] as Double, 2.0)
        assertEquals(72, leqs[3] as Double, 2.0)
        assertEquals(74, leqs[4] as Double, 2.0)
        assertEquals(72, leqs[5] as Double, 2.0)
        assertEquals(63, leqs[6] as Double, 2.0)
        assertEquals(54, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LNIGHT_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LNIGHT_GEOM")

        assertEquals(76, leqs[0] as Double, 2.0)
        assertEquals(69, leqs[1] as Double, 2.0)
        assertEquals(68, leqs[2] as Double, 2.0)
        assertEquals(69, leqs[3] as Double, 2.0)
        assertEquals(71, leqs[4] as Double, 2.0)
        assertEquals(69, leqs[5] as Double, 2.0)
        assertEquals(60, leqs[6] as Double, 2.0)
        assertEquals(51, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LDEN_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDEN_GEOM")

        assertEquals(84, leqs[0] as Double, 2.0)
        assertEquals(78, leqs[1] as Double, 2.0)
        assertEquals(76, leqs[2] as Double, 2.0)
        assertEquals(77, leqs[3] as Double, 2.0)
        assertEquals(80, leqs[4] as Double, 2.0)
        assertEquals(78, leqs[5] as Double, 2.0)
        assertEquals(69, leqs[6] as Double, 2.0)
        assertEquals(60, leqs[7] as Double, 2.0)
    }

    @Test
    void testLdenFromEmission() {

        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        res = new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        res = new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        res = new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains("LDAY_GEOM"))
        assertTrue(res.contains("LEVENING_GEOM"))
        assertTrue(res.contains("LNIGHT_GEOM"))
        assertTrue(res.contains("LDEN_GEOM"))
    }

    void testLdenFromEmission1khz() {

        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        // select only 1khz band
        Sql sql = new Sql(connection)

        sql.execute("CREATE TABLE LW_ROADS2(pk serial primary key, the_geom geometry, LWD1000 double) as select pk, the_geom, lwd1000 from LW_ROADS")

        res = new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        res = new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        res = new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS2",
                 "tableReceivers": "RECEIVERS",
                "confSkipLevening": true,
                "confSkipLnight": true,
                "confSkipLden": true])

        assertTrue(res.contains("LDAY_GEOM"))

        // fetch columns
        def fields = JDBCUtilities.getFieldNames(connection.getMetaData(), "LDAY_GEOM")

        assertArrayEquals(["IDRECEIVER","THE_GEOM", "HZ1000", "LAEQ", "LEQ"].toArray(), fields.toArray())
    }
}
