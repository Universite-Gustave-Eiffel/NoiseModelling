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
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Data_Assimilation.*
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestDataAssimilation extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestDataAssimilation.class)

    @Test
    void testSimulation() {
        logger.info('Start Test for Data Assimilation')

        // Path folder containing all the necessary data for this test
        String workingFolder = TestDataAssimilation.class.getResource("dataAssimilation/").getPath()

        new All_Possible_Configuration().exec(connection, [
                "trafficValues"    : "0.01, 1.0, 2.0",
                "temperatureValues": "10, 15, 20"
        ])
        new Export_Table().exec(connection, [
                "exportPath":"./target/ALL_CONFIGURATIONS.csv", // receivers
                "tableToExport":"ALL_CONFIGURATIONS"
        ])

        new Import_File().exec(connection,[
                "pathFile" : workingFolder+"device_mapping_sf.geojson",
                "inputSRID" : 2056,
                "tableName": "SENSORS_LOCATION"

        ])

        new Prepare_Sensors().exec(connection, [
                "startDate"    : "2024-08-25 06:30:00",
                "endDate"      : "2024-08-25 07:30:00",
                "trainingRatio": 0.8,
                "workingFolder": workingFolder,
                "targetSRID"   : 2056
        ])
        new Import_OSM().exec(connection, [
                "pathFile"      : workingFolder + "geneva.osm.pbf",
                "targetSRID"    : 2056,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ])

        // Method to execute a series of operations for generate noise maps
        new Data_Simulation().exec(connection,["noiseMapLimit": 5])

       new Noise_level_from_source().exec(connection, [
                "tableSources": "ROADS_GEOM",
                "tableSourcesEmission" : "LW_ROADS",
                "tableBuilding": "BUILDINGS",
                "tableReceivers": "SENSORS_LOCATION",
                "confExportSourceId": false,
                "confMaxSrcDist": 500,
                "confDiffVertical": false,
                "confDiffHorizontal": true
        ])

        // Extraction of the best maps
        new Extract_Best_Configuration().exec(connection,[
              "observationTable": "SENSORS_MEASUREMENTS_TRAINING",
              "noiseMapTable": "RECEIVERS_LEVEL",
              "tempToleranceThreshold"  : 5
        ])

      // Create a regular grid of receivers.
        new Regular_Grid().exec(connection,[
              "fenceTableName": "BUILDINGS",
              "buildingTableName": "BUILDINGS",
              "sourcesTableName":"ROADS",
              "delta": 20
        ])

        new Merged_Sensors_Receivers().exec(connection,[
                "tableReceivers": "RECEIVERS",
                "tableSensors" : "SENSORS_LOCATION"
        ])

         // Creation of the dynamic road using best configurations
        new NMs_4_BestConfigs().exec(connection,[
                "bestConfig": "BEST_CONFIGURATION_FULL",
                "roadEmission" : "LW_ROADS"
        ])

        // Create NoiseMaps only for best configurations
        new Noise_level_from_source().exec(connection, [
                      "tableSources": "ROADS_GEOM",
                      "tableSourcesEmission" : "LW_ROADS_best",
                      "tableBuilding": "BUILDINGS",
                      "tableReceivers": "RECEIVERS",
                      "confExportSourceId": false,
                      "confMaxSrcDist": 200,
                      "confDiffVertical": false,
                      "confDiffHorizontal": true
        ])

        new Create_Assimilated_Maps().exec(connection,[
                "bestConfigTable" : "BEST_CONFIGURATION_FULL",
                "receiverLevel" : "RECEIVERS_LEVEL",
                "outputTable": "ASSIMILATED_MAPS"
        ])

        logger.info('End of Test for Data Assimilation.')

    }
}
