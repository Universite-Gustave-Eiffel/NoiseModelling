/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 */


package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lday_from_Traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lden_from_Road_Emission
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestNoiseModelling extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestNoiseModelling.class)

    void testRoadEmissionFromDEN() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("ROADS2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])


        assertEquals("Calculation Done ! The table LW_ROADS has been created.", res)
    }

    void testLdayFromTraffic() {

        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("buildings.shp").getPath())
        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


       String res = new Lday_from_Traffic().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableRoads"   : "ROADS2",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains("LDAY_GEOM"))

        def sql = new Sql(connection)

        def leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDAY_GEOM")

        assertEquals(83, leqs[0] as Double, 2.0)
        assertEquals(74, leqs[1] as Double, 2.0)
        assertEquals(73, leqs[2] as Double, 2.0)
        assertEquals(75, leqs[3] as Double, 2.0)
        assertEquals(79, leqs[4] as Double, 2.0)
        assertEquals(77, leqs[5] as Double, 2.0)
        assertEquals(68, leqs[6] as Double, 2.0)
        assertEquals(59, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LEVENING_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LEVENING_GEOM")

        assertEquals(76.0, leqs[0] as Double, 2.0)
        assertEquals(69.0, leqs[1] as Double, 2.0)
        assertEquals(68.0, leqs[2] as Double, 2.0)
        assertEquals(70.0, leqs[3] as Double, 2.0)
        assertEquals(74.0, leqs[4] as Double, 2.0)
        assertEquals(71.0, leqs[5] as Double, 2.0)
        assertEquals(62.0, leqs[6] as Double, 2.0)
        assertEquals(53.0, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LNIGHT_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LNIGHT_GEOM")

        assertEquals(83.0, leqs[0] as Double, 2.0)
        assertEquals(74.0, leqs[1] as Double, 2.0)
        assertEquals(73.0, leqs[2] as Double, 2.0)
        assertEquals(75.0, leqs[3] as Double, 2.0)
        assertEquals(79.0, leqs[4] as Double, 2.0)
        assertEquals(76.0, leqs[5] as Double, 2.0)
        assertEquals(68.0, leqs[6] as Double, 2.0)
        assertEquals(58.0, leqs[7] as Double, 2.0)

        assertTrue(res.contains("LDEN_GEOM"))

        leqs = sql.firstRow("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM LDEN_GEOM")

        assertEquals(82.0, leqs[0] as Double, 2.0)
        assertEquals(75.0, leqs[1] as Double, 2.0)
        assertEquals(74.0, leqs[2] as Double, 2.0)
        assertEquals(76.0, leqs[3] as Double, 2.0)
        assertEquals(80.0, leqs[4] as Double, 2.0)
        assertEquals(77.0, leqs[5] as Double, 2.0)
        assertEquals(68.0, leqs[6] as Double, 2.0)
        assertEquals(59.0, leqs[7] as Double, 2.0)
    }

//    void testLdayFromTrafficLongRun() {
//
//        SHPRead.readShape(connection, TestNoiseModelling.getResource("ROADS2.shp").getPath())
//
//        //SHPRead.readShape(connection, TestDatabaseManager.getResource("buildings.shp").getPath())
//        new Import_File().exec(connection,
//                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
//                 "inputSRID": "2154",
//                 "tableName": "buildings"])
//
//        new Building_Grid().exec(connection,
//                ["tableBuilding" : "BUILDINGS",
//                 "delta" : 5,
//                 "sourcesTableName": "ROADS2"])
//
//
//        String res = new Lday_from_Traffic().exec(connection,
//                ["tableBuilding"   : "BUILDINGS",
//                 "tableRoads"   : "ROADS2",
//                 "tableReceivers": "RECEIVERS",
//                "confThreadNumber" : 0,
//                "confMaxSrcDist": 400,
//                "confDiffHorizontal": true])
//
//        assertTrue(res.contains("LDAY_GEOM"))
//        assertTrue(res.contains("LEVENING_GEOM"))
//        assertTrue(res.contains("LNIGHT_GEOM"))
//        assertTrue(res.contains("LDEN_GEOM"))
//    }
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


        res = new Lden_from_Road_Emission().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS"])

        assertTrue(res.contains("LDAY_GEOM"))
        assertTrue(res.contains("LEVENING_GEOM"))
        assertTrue(res.contains("LNIGHT_GEOM"))
        assertTrue(res.contains("LDEN_GEOM"))
    }
}
