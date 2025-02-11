package org.noise_planet.noisemodelling.work

import groovy.sql.Sql
import org.apache.commons.cli.*
import org.h2.Driver
import org.h2gis.functions.factory.H2GISFunctions
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.DynamicIndicators
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.Dynamic.Point_Source_0dB_From_Network
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Symuvia
import org.noise_planet.noisemodelling.wps.Dynamic.Ind_Vehicles_2_Noisy_Vehicles
import org.noise_planet.noisemodelling.wps.Dynamic.Noise_From_Attenuation_Matrix
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source

import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.ResultSet

class Run {

    public static void main(String[] args) {
        //RunSUMO("fcd_output_32633",20)
        RunFlow("roads_merged_traffic_0","Speed")
        //export_table("Remove", 5800, 5900)
    }

    static void RunSUMO(String File_name, int gridStep){
        String dbName = "file:///home/gao/noise_modeling_database"
        Connection connection;
        File dbFile = new File(URI.create(dbName));
        String databasePath = "jdbc:h2:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
        Driver.load();
        connection = DriverManager.getConnection(databasePath, "", "");
        H2GISFunctions.load(connection);

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
                ["pathFile" : String.format("/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/Hornsgatan/synthetic_traffic_SUMO/syntatic/high/%s.geojson", File_name) ,
                 "inputSRID": "32633",
                 "tableName": "vehicle"])

        // Create point sources from the network every 10 meters. This point source will be used to compute the noise attenuation level from them to each receiver.
        // The created table will be named SOURCES_0DB
        new Point_Source_0dB_From_Network().exec(connection,
                ["tableNetwork": "network_stockholm",
                 "gridStep" : gridStep
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

        new Export_Table().exec(connection, [
                "exportPath"    : String.format('/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/Hornsgatan/synthetic_traffic_SUMO/syntatic/high/output/%s_%d.csv',File_name, gridStep),
                "tableToExport" : "LT_GEOM"
        ])

        connection.close();
    }

    static void RunFlow(String File_name, String Speed){
        String dbName = "file:///home/gao/noise_modeling_database"
        Connection connection;
        File dbFile = new File(URI.create(dbName));
        String databasePath = "jdbc:h2:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
        Driver.load();
        connection = DriverManager.getConnection(databasePath, "", "");
        H2GISFunctions.load(connection);

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

        new Import_File().exec(connection,
                ["pathFile" : String.format('/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/%s.shp',File_name),
                 "inputSRID": "32633",
                 "tableName" : "traffic_flow"])

        connection.close();
    }

    static void export_table(String File_name, int start_time, int end_time){

        String dbName = "file:///home/gao/Michele"
        Connection connection;
        File dbFile = new File(URI.create(dbName));
        String databasePath = "jdbc:h2:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";
        Driver.load();
        connection = DriverManager.getConnection(databasePath, "", "");
        H2GISFunctions.load(connection);

        new Export_Table().exec(connection, [
                "exportPath"    : String.format('/home/gao/Downloads/Noise/SUMO/Files_for_Yu/Sodermalm/Hornsgatan/synthetic_traffic_SUMO/syntatic/high/output/%s_%d_%d.csv',File_name, start_time, end_time),
                "tableToExport" : "LT_GEOM_PROBA"
        ])
    }

}
