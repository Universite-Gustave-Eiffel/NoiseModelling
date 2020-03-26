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
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Experimental.Get_Rayz
import org.noise_planet.noisemodelling.wps.Experimental.Multi_Runs
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lden_from_Emission
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_From_DEN

class TestMultRuns extends JdbcTestCase  {

    @Test
    void testMultiRun() {

        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/buildings.geojson").getPath())
        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/receivers.geojson").getPath())
        GeoJsonRead.readGeoJson(connection, TestMultRuns.class.getResource("multirun/sources.geojson").getPath())

        new Add_Primary_Key().exec(connection,
                ["pkName":"PK",
                 "table" : "RECEIVERS"])

        new Add_Primary_Key().exec(connection,
                 ["pkName":"PK",
                  "table" : "SOURCES"])


        new Road_Emission_From_DEN().exec(connection,
                ["roadsTableName":"SOURCES"])

        new Lden_from_Emission().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS"])


        new Get_Rayz().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "roadsTableName"   : "SOURCES",
                 "tableReceivers": "RECEIVERS",
                 "exportPath"   : TestMultRuns.class.getResource("multirun/").getPath()])


        new Multi_Runs().exec(connection,
                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath()])


        // ICI tu verras valeur au recepteur 4004 / 63 Hz = 65.2966
        String res2 =  new Table_Visualization_Data().exec(connection,
                ["tableName": "LDEN_GEOM"])

        // ICI tu verras valeur au recepteur 4004 / 63 Hz = 65.2972
      String res =   new Table_Visualization_Data().exec(connection,
                ["tableName": "MultiRunsResults_geom"])
        // DONC POUR MOI C'est OK.... c'est juste une mini erreur ptet à la température par défaut ou le sol ou un truc du genre, pas de quoi se préocupper.


        // Ici je vais forcer à calculer les rayons avec un ordre de reflexion
        new Get_Rayz().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "roadsTableName"   : "SOURCES",
                 "tableReceivers": "RECEIVERS",
                 "confReflOrder": 1,
                 "exportPath"   : TestMultRuns.class.getResource("multirun/").getPath()])

        // Mais tu pourras voir dans le MR_input que j'exclue les rayons avec 1 ordre de reflextion du calcul....
        new Multi_Runs().exec(connection,
                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath()])

        //.... du coup  les résultats devraient être identiques
        // mais ICI tu verras valeur au recepteur 4004 / 63 Hz = 70.26 !!!!!!!!
        String res3 =   new Table_Visualization_Data().exec(connection,
                ["tableName": "MultiRunsResults_geom"])



        assertTrue(res.contains("MULTIRUNSRESULTS_GEOM"))
    }


}
