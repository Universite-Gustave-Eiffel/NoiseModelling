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
import org.h2.value.ValueBoolean
import org.h2gis.functions.io.dbf.DBFRead
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.utilities.JDBCUtilities
import org.junit.Test
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.GenerateAtmosphericSettingsTemplate
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Railway_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Test parsing of zip file using H2GIS database
 */
class TestNoiseModelling extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestNoiseModelling.class)


    void testRoadEmissionFromDEN() {

        SHPRead.importTable(connection, TestDatabaseManager.getResource("ROADS2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])


        assertEquals("Calculation Done ! The table LW_ROADS has been created.", res)
    }

    void testRailWayEmissionFromDEN() {

        def sql = new Sql(connection)

        sql.execute("DROP TABLE IF EXISTS LW_RAILWAY")


        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("Train/RAIL_SECTIONS.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("Train/RAIL_TRAFFIC.dbf").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("Train/receivers_Railway_.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName" : "RECEIVERS"])

        new Railway_Emission_from_Traffic().exec(connection,
                ["tableRailwayTraffic": "RAIL_TRAFFIC",
                 "tableRailwayTrack": "RAIL_SECTIONS"
                ])

        def fieldNames = JDBCUtilities.getColumnNames(connection, "LW_RAILWAY")

        def expected = ["PK_SECTION","THE_GEOM","DIR_ID","GS","HZD50","HZD63","HZD80","HZD100","HZD125",
                        "HZD160","HZD200","HZD250","HZD315","HZD400","HZD500","HZD630","HZD800","HZD1000","HZD1250",
                        "HZD1600","HZD2000","HZD2500","HZD3150","HZD4000","HZD5000","HZD6300","HZD8000","HZD10000",
                        "HZE50","HZE63","HZE80","HZE100","HZE125","HZE160","HZE200","HZE250","HZE315","HZE400",
                        "HZE500","HZE630","HZE800","HZE1000","HZE1250","HZE1600","HZE2000","HZE2500","HZE3150",
                        "HZE4000","HZE5000","HZE6300","HZE8000","HZE10000","HZN50","HZN63","HZN80","HZN100","HZN125",
                        "HZN160","HZN200","HZN250","HZN315","HZN400","HZN500","HZN630","HZN800","HZN1000","HZN1250",
                        "HZN1600","HZN2000","HZN2500","HZN3150","HZN4000","HZN5000","HZN6300","HZN8000","HZN10000","PK"]

        assertArrayEquals(expected.toArray(new String[expected.size()]), fieldNames.toArray(new String[fieldNames.size()]))


        SHPRead.importTable(connection, TestDatabaseManager.getResource("Train/buildings2.shp").getPath(),
                "BUILDINGS", ValueBoolean.TRUE)

        sql.execute("DROP TABLE IF EXISTS LDAY_GEOM")

        new Noise_level_from_source().exec(connection,
                ["tableBuilding" : "BUILDINGS",
                 "tableSources"  : "LW_RAILWAY",
                 "tableReceivers": "RECEIVERS",
                 "confMaxSrcDist": 500,
                 "confMaxError"  : 5.0])

        assertTrue(JDBCUtilities.tableExists(connection, NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))

        def receiversCount = sql.rows("SELECT COUNT(*) CPT FROM "+
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME+" WHERE PERIOD = 'D'")

        new Export_Table().exec(connection,
                ["exportPath"   : "build/tmp/RECEIVERS_LEVEL.geojson",
                 "tableToExport": NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME])

        assertEquals(688, receiversCount[0]["CPT"] as Integer)
    }

    void testLdayFromTraffic() {

        SHPRead.importTable(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        String res = new Noise_level_from_traffic().exec(connection,
                ["tableBuilding" : "BUILDINGS",
                 "tableRoads"    : "ROADS2",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains(NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))

        def sql = new Sql(connection)


        def leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'D'")

        assertEquals(87, leqs[0] as Double, 2.0)
        assertEquals(78, leqs[1] as Double, 2.0)
        assertEquals(78, leqs[2] as Double, 2.0)
        assertEquals(79, leqs[3] as Double, 2.0)
        assertEquals(82, leqs[4] as Double, 2.0)
        assertEquals(80, leqs[5] as Double, 2.0)
        assertEquals(71, leqs[6] as Double, 2.0)
        assertEquals(62, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'E'")

        assertEquals(81, leqs[0] as Double, 2.0)
        assertEquals(74, leqs[1] as Double, 2.0)
        assertEquals(73, leqs[2] as Double, 2.0)
        assertEquals(75, leqs[3] as Double, 2.0)
        assertEquals(77, leqs[4] as Double, 2.0)
        assertEquals(75, leqs[5] as Double, 2.0)
        assertEquals(66, leqs[6] as Double, 2.0)
        assertEquals(57, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'N'")

        assertEquals(78, leqs[0] as Double, 2.0)
        assertEquals(71, leqs[1] as Double, 2.0)
        assertEquals(70, leqs[2] as Double, 2.0)
        assertEquals(72, leqs[3] as Double, 2.0)
        assertEquals(74, leqs[4] as Double, 2.0)
        assertEquals(72, leqs[5] as Double, 2.0)
        assertEquals(63, leqs[6] as Double, 2.0)
        assertEquals(54, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'DEN'")

        assertEquals(87, leqs[0] as Double, 2.0)
        assertEquals(79, leqs[1] as Double, 2.0)
        assertEquals(79, leqs[2] as Double, 2.0)
        assertEquals(80, leqs[3] as Double, 2.0)
        assertEquals(83, leqs[4] as Double, 2.0)
        assertEquals(81, leqs[5] as Double, 2.0)
        assertEquals(72, leqs[6] as Double, 2.0)
        assertEquals(63, leqs[7] as Double, 2.0)
    }


    void testLdayFromTrafficWithBuildingsZ() {

        def sql = new Sql(connection)

        SHPRead.importTable(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

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

        assertTrue(res.contains(NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))



        def leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'D'")

        assertEquals(87, leqs[0] as Double, 2.0)
        assertEquals(78, leqs[1] as Double, 2.0)
        assertEquals(78, leqs[2] as Double, 2.0)
        assertEquals(79, leqs[3] as Double, 2.0)
        assertEquals(82, leqs[4] as Double, 2.0)
        assertEquals(80, leqs[5] as Double, 2.0)
        assertEquals(71, leqs[6] as Double, 2.0)
        assertEquals(62, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'E'")

        assertEquals(81, leqs[0] as Double, 2.0)
        assertEquals(74, leqs[1] as Double, 2.0)
        assertEquals(73, leqs[2] as Double, 2.0)
        assertEquals(75, leqs[3] as Double, 2.0)
        assertEquals(77, leqs[4] as Double, 2.0)
        assertEquals(75, leqs[5] as Double, 2.0)
        assertEquals(66, leqs[6] as Double, 2.0)
        assertEquals(57, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'N'")

        assertEquals(78, leqs[0] as Double, 2.0)
        assertEquals(71, leqs[1] as Double, 2.0)
        assertEquals(70, leqs[2] as Double, 2.0)
        assertEquals(72, leqs[3] as Double, 2.0)
        assertEquals(74, leqs[4] as Double, 2.0)
        assertEquals(72, leqs[5] as Double, 2.0)
        assertEquals(63, leqs[6] as Double, 2.0)
        assertEquals(54, leqs[7] as Double, 2.0)

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000)," +
                " MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'DEN'")

        assertEquals(87, leqs[0] as Double, 2.0)
        assertEquals(79, leqs[1] as Double, 2.0)
        assertEquals(79, leqs[2] as Double, 2.0)
        assertEquals(80, leqs[3] as Double, 2.0)
        assertEquals(83, leqs[4] as Double, 2.0)
        assertEquals(81, leqs[5] as Double, 2.0)
        assertEquals(72, leqs[6] as Double, 2.0)
        assertEquals(63, leqs[7] as Double, 2.0)
    }


    void testLdenFromEmission() {

        SHPRead.importTable(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        String res = new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains(NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))
    }

    void testLdenFromEmission1khz() {

        SHPRead.importTable(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        // select only 1khz band
        Sql sql = new Sql(connection)

        sql.execute("CREATE TABLE LW_ROADS2(pk serial primary key, the_geom geometry, HZD1000 double) as select pk, the_geom, HZd1000 from LW_ROADS")

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        String res = new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS2",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains(NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME))

        // fetch columns
        def fields = JDBCUtilities.getColumnNames(connection, NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)

        assertArrayEquals(["IDRECEIVER","PERIOD","THE_GEOM", "HZ1000", "LAEQ", "LEQ"].toArray(), fields.toArray())
    }

    void testAtmosphericSettings() {

        Sql sql = new Sql(connection)

        sql.execute(
                $/CREATE TABLE SOURCES_EMISSION(
                      IDSOURCE INTEGER NOT NULL,
                      PERIOD VARCHAR,
                      HZ500 DOUBLE);    
        /$)

        sql.executeInsert("INSERT INTO SOURCES_EMISSION VALUES (1, 'D', 90.0), (1, 'E', 92.0), (1, 'N', 93.0);");

        new GenerateAtmosphericSettingsTemplate().exec(connection, ["tableSourcesEmission" : "SOURCES_EMISSION"])

        assertTrue(JDBCUtilities.tableExists(connection, "SOURCES_ATMOSPHERIC"))


        List<String> periods = JDBCUtilities.getUniqueFieldValues(connection, "SOURCES_ATMOSPHERIC", "PERIOD")

        ["D", "E", "N"].forEach {
            assertTrue(periods.contains(it))
        }
    }
}
