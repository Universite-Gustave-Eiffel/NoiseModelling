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
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.DataAssimilation.*
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestDataAssimilation extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestDataAssimilation.class)

    void testSimulation() {

        logger.info('Start Test for Data Assimilation')
        Sql sql = new Sql(connection)

        // Path folder containing all the necessary data for this test
        String workingFolder = TestDataAssimilation.class.getResource("dataAssimilation/").getPath()

        new All_Possible_Configuration().exec(connection, [
                "trafficValues"    : [0.01, 1.0, 2.0, 3],
                "temperatureValues": [10, 15, 20]
        ])

        new Import_OSM().exec(connection, [
                "pathFile"      : workingFolder + "geneva.osm.pbf",
                "targetSRID"    : 2056,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ])

        // use import file au maximum
        // new import_file csv donnees
        // new import_file _sf.csv
        //todo new import_csv mieux, car toujours mieux de mettre un nom de table que workingfolder
        new Prepare_Sensors().exec(connection, [
                "startDate"    : "2024-08-25 06:30:00",
                "endDate"      : "2024-08-25 07:30:00",
                "trainingRatio": 0.8,
                "workingFolder": workingFolder,
                "targetSRID"   : 2056
        ])

        new Road_Emission_from_Traffic().exec(connection, [
                "tableRoads" : "ROADS"
        ])

        new Noise_level_from_source().exec(connection, [
                "tableSources": "ROADS",
                "tableBuilding": "BUILDINGS",
                "tableReceivers": "SENSORS_LOCATION",
                "confExportSourceId": true,
                "confMaxSrcDist": 250,
                "confDiffVertical": false,
                "confDiffHorizontal": false
        ])

        // Method to execute a series of operations for generate noise maps
        // todo create direct splitted sources, in this way not needed to user Split_Sources_Period
        new Data_Simulation().exec(connection,[
                "noiseMapLimit": 10
        ])

      new Noise_level_from_source().exec(connection, [
                "tableSources": "ROADS_GEOM",
                "tableSourcesEmission" : "LW_ROADS",
                "tableBuilding": "BUILDINGS",
                "tableReceivers": "SENSORS_LOCATION",
                "confExportSourceId": false,
                "confMaxSrcDist": 250,
                "confDiffVertical": false,
                "confDiffHorizontal": false
        ])

        // Extraction of the best maps
              new Extract_Best_Configuration().exec(connection,[
                      "observationTable": "SENSORS_MEASUREMENTS_TRAINING",
                      "noiseMapTable": "RECEIVERS_LEVEL"
              ])


              // Create a regular grid of receivers.
              new Regular_Grid().exec(connection,[
                      "fenceTableName": "BUILDINGS",
                      "buildingTableName": "BUILDINGS",
                      "sourcesTableName":"ROADS",
                      "delta": 200
              ])

             sql.execute("ALTER TABLE RECEIVERS DROP COLUMN ID_ROW, ID_COL")
              sql.execute("INSERT INTO RECEIVERS (THE_GEOM) SELECT The_GEOM FROM SENSORS_LOCATION; ")

             // Creation of the dynamic road using best configurations
              new NMs_4_BestConfigs().exec(connection)

        // Create NoiseMaps only for best configurations
              new Noise_level_from_source().exec(connection, [
                      "tableSources": "ROADS_GEOM",
                      "tableSourcesEmission" : "LW_ROADS_best",
                      "tableBuilding": "BUILDINGS",
                      "tableReceivers": "RECEIVERS",
                      "confExportSourceId": false,
                      "confMaxSrcDist": 250,
                      "confDiffVertical": false,
                      "confDiffHorizontal": false
              ])

        // Add Timestamp to the NMs
        sql.execute("DROP TABLE ASSIMILATED_MAPS IF EXISTS;")
        sql.execute("CREATE TABLE ASSIMILATED_MAPS AS SELECT b.T TIMESTAMP, b.IT IMAP, a.LAEQ, a.THE_GEOM, a.IDRECEIVER FROM BEST_CONFIGURATION_FULL b  LEFT JOIN RECEIVERS_LEVEL a ON a.PERIOD = b.PERIOD ; ")

          def columnNames = JDBCUtilities.getColumnNames(connection, "ASSIMILATED_MAPS")

          columnNames.containsAll(Arrays.asList("IMAP", "LAEQ"))

              new Export_Table().exec(connection,
                      ["tableToExport": "ASSIMILATED_MAPS",
                     "exportPath": "/home/aumond/Documents/github/noisemodelling_pierromond/wps_scripts/target/test.geojson" ])

        logger.info('End of Test for Data Assimilation:  The dynamic noise map LT_GEOM is save on '+ workingFolder+"results")
    }
}
