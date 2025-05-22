package org.noise_planet.noisemodelling.wps.DataAssimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
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
                type: Double[].class
        ],
        temperatureValues : [
                name       : 'Temperature values',
                title      : 'Temperature values',
                description: 'List of temperature values for the road traffic emission',
                type       : Double[].class
        ]
]


outputs = [
        result: [
                name: 'ALL_CONFIGURATIONS',
                description: 'A sql table named ALL_CONFIGURATIONS ',
                type: Sql
        ]
]
@CompileStatic
static def exec(Connection connection,input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    logger.info('Start generate all possible configuration')
    double[] trafficValues = input['trafficValues'] as double[]
    int[] temperatureValues = input['temperatureValues'] as int[]

    getAllConfig(connection,trafficValues,temperatureValues)
    logger.info('End generate all possible configuration')

}

static def run(input) {

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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
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
static def getAllConfig(Connection connection,double[] vals,int[] temps) {
    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE ALL_CONFIGURATIONS IF EXISTS")
    sql.execute("CREATE TABLE ALL_CONFIGURATIONS(IT INTEGER PRIMARY KEY AUTO_INCREMENT,PRIMARY_VAL FLOAT,SECONDARY_VAL FLOAT,TERTIARY_VAL FLOAT,OTHERS_VAL FLOAT,TEMP_VAL INTEGER)")

    String insertQuery = "INSERT INTO ALL_CONFIGURATIONS (PRIMARY_VAL, SECONDARY_VAL, TERTIARY_VAL, OTHERS_VAL, TEMP_VAL) VALUES (?, ?, ?, ?, ?)"
    int totalCombinations = vals.length * vals.length * vals.length * vals.length * temps.length

    sql.withBatch(insertQuery) { ps ->
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
            int valTemps = temps[indexTemps]

            // Skip incoherent combinations
            if (others / primary <= 20 && secondary / primary <= 20 && tertiary / primary <= 20 && tertiary /secondary <= 20  &&  others / secondary <= 20 && others / tertiary <= 20){
                ps.addBatch([primary, secondary, tertiary, others, valTemps])
            }
        }
    }

}
