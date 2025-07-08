package org.noise_planet.noisemodelling.wps.Data_Assimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.BatchingStatementWrapper
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

// ----------------- WPS Metadata ------------------
title = 'all configurations '
description = 'process to generate all configurations.'
inputs = [
        trafficValues: [
                name: 'Traffic values',
                title: 'Traffic values',
                description: 'list of variation values in % for traffic like [0.01,1.0, 2.0,3,4]',
                type: String.class
        ],
        temperatureValues : [
                name       : 'Temperature values',
                title      : 'Temperature values',
                description: 'List of temperature values for the road traffic emission',
                type       : String.class
        ]
]


outputs = [
        result: [
                name: 'ALL_CONFIGURATIONS',
                title: 'ALL_CONFIGURATIONS',
                description: 'A sql table named ALL_CONFIGURATIONS ',
                type: String.class
        ]
]

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

@CompileStatic
def exec(Connection connection,input) {
    connection = new ConnectionWrapper(connection)
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    logger.info('Start generate all possible configuration')

    String trafficString = input['trafficValues'] as String
    def trafficList = trafficString.replaceAll(" ", "")  // Remove brackets
            .split(",")                  // Split by comma
            .collect { it.trim().toDouble() }  // Convert to double
    double[] trafficValues = trafficList as double[]

    String tempString = input['temperatureValues'] as String
    def tempList = tempString.replaceAll(" ", "")  // Remove brackets
            .split(",")                  // Split by comma
            .collect { it.trim().toDouble() }  // Convert to double
    double[] temperatureValues = tempList as double[]

    getAllConfig(connection,trafficValues,temperatureValues)
    logger.info('End generate all possible configuration')
    return "Calculation Done ! The table ALL_CONFIGURATIONS has been created."

}


/**
 * Generates all possible value combinations based on two list and into a sql table "ALL_CONFIGURATIONS".
 *
 * The generated combinations include values for type of roads primary, secondary, tertiary, others, and temperature.
 *
 * The total number of combinations is calculated as:
 * (number of `vals` elements) ^ (number of paramèters)  * (number of `temps` elements).
 *
 * The sql table  follows the structure:
 * IT, PRIMARY, SECONDARY, TERTIARY, OTHERS, TEMP.
 *
 * @param connection : Connection to the data base
 * @param vals : list of traffic values
 * @param temps : list of temperature values
 */
@CompileStatic
def getAllConfig(Connection connection,double[] vals,double[] temps) {
    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE ALL_CONFIGURATIONS IF EXISTS")
    sql.execute("CREATE TABLE ALL_CONFIGURATIONS(IT INTEGER PRIMARY KEY AUTO_INCREMENT,PRIMARY_VAL FLOAT,SECONDARY_VAL FLOAT,TERTIARY_VAL FLOAT,OTHERS_VAL FLOAT,TEMP_VAL double)")

    String insertQuery = "INSERT INTO ALL_CONFIGURATIONS (PRIMARY_VAL, SECONDARY_VAL, TERTIARY_VAL, OTHERS_VAL, TEMP_VAL) VALUES (?, ?, ?, ?, ?)"
    int totalCombinations = vals.length * vals.length * vals.length * vals.length * temps.length

    sql.withBatch(100, insertQuery) { BatchingPreparedStatementWrapper ps ->
        for (int i = 0; i < totalCombinations; i++) {
            int indexPrimary = (int) (i / (vals.length * vals.length * vals.length * temps.length)) % vals.length
            int indexSecondary = (int) (i / (vals.length * vals.length * temps.length)) % vals.length
            int indexTertiary = (int) (i / (vals.length * temps.length)) % vals.length
            int indexOthers = (int) (i / temps.length) % vals.length
            int indexTemps = i % temps.length

            double primary = vals[indexPrimary]
            double secondary = vals[indexSecondary]
            double tertiary = vals[indexTertiary]
            double others = vals[indexOthers]
            double valTemps = temps[indexTemps]

            // Skip incoherent combinations
            if (others / primary <= 20 && secondary / primary <= 20 && tertiary / primary <= 20 && tertiary /secondary <= 20  &&  others / secondary <= 20 && others / tertiary <= 20){
                ps.addBatch(primary, secondary, tertiary, others, valTemps)
            }
        }
    }

}
