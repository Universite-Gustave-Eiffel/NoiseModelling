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
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.DynamicIndicators
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.Dynamic.Point_Source_0dB_From_Network
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.Dynamic.Ind_Vehicles_2_Noisy_Vehicles
import org.noise_planet.noisemodelling.wps.Dynamic.Noise_From_Attenuation_Matrix
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Dynamic.Flow_2_Noisy_Vehicles
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Test parsing of zip file using H2GIS database
 */
class TestSUMO extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestNoiseModelling.class)


    @Test
    /**
     * as SUMO or SYMUVIA or Drone input
     */
    void testDynamicIndividualVehiclesTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  "/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/buildings_nm_ready.shp",
                 "inputSRID": "32633",
                 "tableName": "buildings"])

        // Import the receivers (or generate your set of receivers using Regular_Grid script for example)
        new Import_File().exec(connection,
                ["pathFile" : "/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/receivers_python_method1_5m.shp",
                 "inputSRID": "32633",
                 "tableName": "receivers"])

        // Set the height of the receivers
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                  "height": 1.5
                ])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :"/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/roads_merged.shp",
                 "inputSRID": "32633",
                 "tableName": "network_stockholm"])

        // (optional) Add a primary key to the road network
        new Add_Primary_Key().exec(connection,
                ["pkName" :"PK",
                 "tableName": "network_stockholm"])

        // Import the vehicles trajectories
        new Import_File().exec(connection,
                ["pathFile" : "/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/Hornsgatan/synthetic_traffic_SUMO/syntatic/high/fcd_output_32633_selected.geojson" ,
                 "inputSRID": "32633",
                 "tableName": "vehicle"])

        // Create point sources from the network every 10 meters. This point source will be used to compute the noise attenuation level from them to each receiver.
        // The created table will be named SOURCES_0DB
        new Point_Source_0dB_From_Network().exec(connection,
                ["tableNetwork": "network_stockholm",
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

}
