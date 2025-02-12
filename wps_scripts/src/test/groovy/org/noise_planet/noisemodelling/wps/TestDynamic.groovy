package org.noise_planet.noisemodelling.wps;

import org.noise_planet.noisemodelling.wps.Acoustic_Tools.DynamicIndicators;
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key;
import org.noise_planet.noisemodelling.wps.Dynamic.Flow_2_Noisy_Vehicles;
import org.noise_planet.noisemodelling.wps.Dynamic.Ind_Vehicles_2_Noisy_Vehicles;
import org.noise_planet.noisemodelling.wps.Dynamic.Noise_From_Attenuation_Matrix;
import org.noise_planet.noisemodelling.wps.Dynamic.Point_Source_0dB_From_Network;
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM;
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source;
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic;
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid;

class TestDynamic extends JdbcTestCase {

    /**
     * as SUMO or SYMUVIA or Drone input
     */
    void testDynamicIndividualVehiclesTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/buildings_nm_ready_pop_heights.shp").getPath() ,
                "inputSRID": "32635",
                "tableName": "buildings"])

        // Import the receivers (or generate your set of receivers using Regular_Grid script for example)
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/receivers_python_method0_50m_pop.shp").getPath() ,
                "inputSRID": "32635",
                "tableName": "receivers"])

        // Set the height of the receivers
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                "height": 1.5
                ])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/network_tartu_32635_.geojson").getPath() ,
                "inputSRID": "32635",
                "tableName": "network_tartu"])

        // (optional) Add a primary key to the road network
        new Add_Primary_Key().exec(connection,
                ["pkName" :"PK",
                "tableName": "network_tartu"])

        // Import the vehicles trajectories
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/SUMO.geojson").getPath() ,
                "inputSRID": "32635",
                "tableName": "vehicle"])

        // Create point sources from the network every 10 meters. This point source will be used to compute the noise attenuation level from them to each receiver.
        // The created table will be named SOURCES_0DB
        new Point_Source_0dB_From_Network().exec(connection,
                ["tableNetwork": "network_tartu",
                "gridStep" : 10
                ])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                "tableSources"   : "SOURCES_0DB",
                "tableReceivers": "RECEIVERS",
                "maxError" : 0.0,
                "confMaxSrcDist" : 150,
                "confDiffHorizontal" : false,
                "confExportSourceId": true,
                "confSkipLday":true,
                "confSkipLevening":true,
                "confSkipLnight":true,
                "confSkipLden":true
                ])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new Ind_Vehicles_2_Noisy_Vehicles().exec(connection,
                ["tableVehicles": "vehicle",
                "distance2snap" : 30,
                "tableFormat" : "SUMO"])

        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the time series of the noise level at each receiver
        new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "LW_DYNAMIC_GEOM",
                "attenuationTable"   : "LDAY_GEOM",
                "outputTable"   : "LT_GEOM"
                ])

        // This step is optional, it compute the LEQA, LEQ, L10, L50 and L90 at each receiver from the table LT_GEOM
        String res = new DynamicIndicators().exec(connection,
                ["tableName"   : "LT_GEOM",
                "columnName"   : "LEQA"
                ])

        assertEquals("The columns LEQA and LEQ have been added to the table: LT_GEOM.", res)
    }

    /**
     * as OSM input
     */
    void testDynamicFlowTutorial() {

        // Import the road network (with predicted traffic flows) and buildings from an OSM file
        new Import_OSM().exec(connection, [
                "pathFile"      : TestImportExport.getResource("map.osm.gz").getPath(),
                "targetSRID"    : 2154,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ]);

        // Create a receiver grid
        new Regular_Grid().exec(connection,  ["buildingTableName": "BUILDINGS",
                "sourcesTableName" : "ROADS",
                "delta"            : 25])

        // Set a height to the receivers at 1.5 m
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                "height": 1.5
                ])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                "method": "PROBA",
                "timestep": 1,
                "gridStep" : 10,
                "duration" : 60])

        // From the network with traffic flow to individual trajectories with associated Lw using the Poisson method
        // This method place the vehicles on the network according to the traffic flow following a poisson law
        // It keeps a coherence in the time series of the noise level
        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                "method": "POISSON",
                "timestep": 1,
                "gridStep" : 10,
                "duration" : 60])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                "tableSources"   : "SOURCES_GEOM",
                "tableSourcesEmission" : "SOURCES_EMISSION",
                "tableReceivers": "RECEIVERS",
                "maxError" : 2.0,
                "confMaxSrcDist" : 800,
                "confDiffHorizontal" : false
                ])

        // This step is optional, it compute the LEQA, LEQ, L10, L50 and L90 at each receiver from the table LT_GEOM
        String res =new DynamicIndicators().exec(connection,
                ["tableName"   : "LT_GEOM",
                "columnName"   : "LEQA"
                ])

        assertEquals("The columns LEQA and LEQ have been added to the table: LT_GEOM.", res)
    }

    /**
     * as MATSIM input
     */
    void testDynamicFluctuatingFlowTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_BUILDINGS.geojson").getPath() ,
                "inputSRID": "2154",
                "tableName": "buildings"])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_TRAFFIC.geojson").getPath() ,
                "inputSRID": "2154",
                "tableName": "ROADS"])

        // Create a receiver grid
        new Regular_Grid().exec(connection,  ["buildingTableName": "BUILDINGS",
                "sourcesTableName" : "ROADS",
                "delta"            : 25])

        // Set a height to the receivers at 1.5 m
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                "height": 1.5
                ])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS",
                "mode" : "dynamic"])


        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                "tableSources"   : "SOURCES_0DB",
                "tableReceivers": "RECEIVERS",
                "maxError" : 0.0,
                "confMaxSrcDist" : 150,
                "confDiffHorizontal" : false,
                "confExportSourceId": true,
                "confSkipLday":true,
                "confSkipLevening":true,
                "confSkipLnight":true,
                "confSkipLden":true
                ])

        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the noise level at each receiver for the whole timesteps
        String res = new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "LW_ROADS",
                "lwTable_sourceId" : "LINK_ID",
                "attenuationTable"   : "LDAY_GEOM",
                "outputTable"   : "LT_GEOM"
                ])

        assertEquals("Process done. Table of receivers LT_GEOM created !", res)
    }


}
