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
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Add_Laeq_Leq_columns
import org.noise_planet.noisemodelling.wps.DataAssimilation.All_Possible_Configuration
import org.noise_planet.noisemodelling.wps.DataAssimilation.Data_Simulation
import org.noise_planet.noisemodelling.wps.DataAssimilation.Dynamic_Road_Traffic_Emission
import org.noise_planet.noisemodelling.wps.DataAssimilation.Extract_Best_Configuration
import org.noise_planet.noisemodelling.wps.DataAssimilation.Prepare_Sensors
import org.noise_planet.noisemodelling.wps.Dynamic.Noise_From_Attenuation_Matrix
import org.noise_planet.noisemodelling.wps.Dynamic.Split_Sources_Period
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

class TestDataAssimilation extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestDataAssimilation.class)

    void testSimulation() {

        logger.info('Start Test for Data Assimilation')

        // Path folder containing all the necessary data for this test
        String workingFolder = TestDataAssimilation.class.getResource("dataAssimilation/").getPath()
/*
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
                "tableSources": "SOURCES_0DB",
                "tableBuilding": "BUILDINGS",
                "tableReceivers": "SENSORS_LOCATION",
                "confExportSourceId": true,
                "confMaxSrcDist": 250,
                "confDiffVertical": false,
                "confDiffHorizontal": false
        ])

        // Method to execute a series of operations for generate noise maps
        new Data_Simulation().exec(connection,[
                "noiseMapLimit": 10
        ])

        new Add_Laeq_Leq_columns().exec(connection, [
                "prefix": "HZ",
                "tableName": "NOISE_MAPS"
        ])

        // Extraction of the best maps
        new Extract_Best_Configuration().exec(connection,[
                "observationTable": "SENSORS_MEASUREMENTS_TRAINING",
                "noiseMapTable": "NOISE_MAPS"
        ])


        // Create a regular grid of receivers.
        new Regular_Grid().exec(connection,[
                "fenceTableName": "BUILDINGS",
                "buildingTableName": "BUILDINGS",
                "sourcesTableName":"ROADS",
                "delta": 200
        ])

        Sql sql = new Sql(connection)
        sql.execute("ALTER TABLE RECEIVERS DROP COLUMN ID_ROW, ID_COL")
        sql.execute("INSERT INTO RECEIVERS (THE_GEOM) SELECT The_GEOM FROM SENSORS_LOCATION; ")

        // Creation of the dynamic road using best configurations
        new Dynamic_Road_Traffic_Emission().exec(connection)


    // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
    // This method place randomly the vehicles on the network according to the traffic flow
    new Split_Sources_Period().exec(connection,
                                    ["tableSourceDynamic": "LW_ROADS",
                                    "sourceIndexFieldName" : "LINK_ID",
                                    "sourcePeriodFieldName" : "PERIOD"])
*/
    // Compute the noise level from the network sources for each time period
    new Noise_level_from_source().exec(connection,
                                       ["tableBuilding"   : "BUILDINGS",
                                       "tableSources"   : "SOURCES_GEOM",
                                       "tableEmission"   : "SOURCES_EMISSION",
                                       "tableReceivers": "RECEIVERS"
    ])

    def columnNames = JDBCUtilities.getColumnNames(connection, "RECEIVERS_LEVEL")

    columnNames.containsAll(Arrays.asList("PERIOD", "LAEQ"))

        new Export_Table().exec(connection,
                ["tableToExport": "RECEIVERS_LEVEL",
               "exportPath": "/home/aumond/Documents/github/noisemodelling_pierromond/wps_scripts/target/test.geojson" ])

        logger.info('End of Test for Data Assimilation:  The dynamic noise map LT_GEOM is save on '+ workingFolder+"results")
    }
}
