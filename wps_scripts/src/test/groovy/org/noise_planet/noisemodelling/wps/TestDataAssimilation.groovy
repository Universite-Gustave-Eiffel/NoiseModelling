package org.noise_planet.noisemodelling.wps


import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Add_Laeq_Leq_columns
import org.noise_planet.noisemodelling.wps.DataAssimilation.All_Possible_Configuration
import org.noise_planet.noisemodelling.wps.DataAssimilation.Data_Simulation
import org.noise_planet.noisemodelling.wps.DataAssimilation.Dynamic_Road_Traffic_Emission
import org.noise_planet.noisemodelling.wps.DataAssimilation.Extract_Best_Configuration
import org.noise_planet.noisemodelling.wps.DataAssimilation.Prepare_Sensors
import org.noise_planet.noisemodelling.wps.Dynamic.Noise_From_Attenuation_Matrix
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

class TestDataAssimilation {
    Logger logger = LoggerFactory.getLogger(TestDataAssimilation.class)

    @Test
    void TestSimulation(){

        logger.info('Start Test for Data Assimilation')

        // Path folder containing all the necessary data for this test
        String workingFolder = TestDataAssimilation.class.getResource("dataAssimilation/").getPath()

        // Establish a connection to the spatial database.
        Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase("mem:assimilationDatabase", true)
        )


       new All_Possible_Configuration().exec(connection,[
               "trafficValues": [0.01,1.0, 2.0,3],
               "temperatureValues": [10,15,20]
       ])

        new Prepare_Sensors().exec(connection,[
                "startDate":"2024-08-25 06:30:00",
                "endDate": "2024-08-25 07:30:00",
                "trainingRatio": 0.8,
                "workingFolder": workingFolder,
                "targetSRID": 2056
        ])

        new Import_OSM().exec(connection, [
                "pathFile"      : workingFolder+"geneva.osm.pbf",
                "targetSRID"    : 2056,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ])

        new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"])

        new Noise_level_from_source().exec(connection, [
                "tableSources": "LW_ROADS_0DB",
                "tableBuilding": "BUILDINGS",
                "tableReceivers": "RECEIVERS",
                "confExportSourceId": true,
                "confMaxSrcDist": 500,
                "confDiffVertical": true,
                "confDiffHorizontal": true,
                "confSkipLevening": true,
                "confSkipLnight": true,
                "confSkipLden": true
        ])

        // Method to execute a series of operations for generate noise maps
        new Data_Simulation().exec(connection,[
                "noiseMapLimit": 80

        ])

        new Add_Laeq_Leq_columns().exec(connection, [
                "prefix": "HZ",
                "tableName": "NOISE_MAPS"
        ])

        // Extraction of the best maps
        new Extract_Best_Configuration().exec(connection,[
                "observationTable": "OBSERVATION",
                "noiseMapTable": "NOISE_MAPS"
        ])


        // Create a regular grid of receivers.
        new Regular_Grid().exec(connection,[
                "buildingTableName": "BUILDINGS",
                "sourcesTableName":"ROADS",
                "delta": 200
        ])

        // Creation of the dynamic road using best configurations
        new Dynamic_Road_Traffic_Emission().exec(connection)

        // Execute road emission and noise level calculations with dynamic mode.
        new Road_Emission_from_Traffic().exec(connection, [
                "tableRoads": "DYNAMIC_ROADS",
                "mode": "dynamic"
        ])
        new Noise_level_from_source().exec(connection, [
                "tableBuilding": "BUILDINGS",
                "tableSources": "SOURCES_0DB",
                "tableReceivers": "RECEIVERS",
                "maxError": 0.0,
                "confMaxSrcDist": 250,
                "confDiffHorizontal": false,
                "confExportSourceId": true,
                "confSkipLday": true,
                "confSkipLevening": true,
                "confSkipLnight": true,
                "confSkipLden": true
        ])

        // Compute the noise level from the moving vehicles to the receivers
        new Noise_From_Attenuation_Matrix().exec(connection, [
                "lwTable": "LW_ROADS",
                "lwTable_sourceId": "LINK_ID",
                "attenuationTable": "LDAY_GEOM",
                "sources0DBTable": "SOURCES_0DB",
                "outputTable": "LT_GEOM"
        ])

        // Export the LT_GEOM table to a shapefile.
        new Export_Table().exec(connection,
                ["exportPath": workingFolder+"results/LT_GEOM.shp",
                 "tableToExport": "LT_GEOM"])


        connection.close()
        logger.info('End of Test for Data Assimilation:  The dynamic noise map LT_GEOM is save on '+ workingFolder+"results")
    }
}
