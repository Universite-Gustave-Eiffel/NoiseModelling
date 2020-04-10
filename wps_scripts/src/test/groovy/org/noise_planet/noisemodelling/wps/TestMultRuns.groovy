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

import org.h2gis.functions.io.geojson.GeoJsonRead

import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Experimental.Get_Rayz
import org.noise_planet.noisemodelling.wps.Experimental.Multi_Runs

class TestMultRuns extends JdbcTestCase  {


//
//    void testMultiRun() {
//
//        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/buildings.geojson").getPath())
//        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/receivers.geojson").getPath())
//        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/sources.geojson").getPath())
//
//        new Add_Primary_Key().exec(connection,
//                ["pkName":"PK",
//                 "tableName" : "RECEIVERS"])
//
//        new Add_Primary_Key().exec(connection,
//                 ["pkName":"PK",
//                  "tableName" : "SOURCES"])
//
//
//        new Get_Rayz().exec(connection,
//                ["tableBuilding"   : "BUILDINGS",
//                 "roadsTableName"   : "SOURCES",
//                 "tableReceivers": "RECEIVERS",
//                 "exportPath"   : TestMultRuns.class.getResource("multirun/").getPath()])
//
//
//        new Multi_Runs().exec(connection,
//                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath()])
//
//
//      String res =   new Table_Visualization_Data().exec(connection,
//                ["tableName": "MultiRunsResults_geom"])
//
//        new Get_Rayz().exec(connection,
//                ["tableBuilding"   : "BUILDINGS",
//                 "roadsTableName"   : "SOURCES",
//                 "tableReceivers": "RECEIVERS",
//                 "confReflOrder": 1,
//                 "exportPath"   : TestMultRuns.class.getResource("multirun/").getPath()])
//
//        new Multi_Runs().exec(connection,
//                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath()])
//
//        String res3 =   new Table_Visualization_Data().exec(connection,
//                ["tableName": "MultiRunsResults_geom"])
//
//
//
//        assertEquals(res, res3)
//    }


}
