package org.noise_planet.noisemodelling.wps.DataAssimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection


title = 'Extraction of the best configurations'
description = 'Extraction of the best maps, i.e. those that minimise the difference between the measured and simulated values, by calculating the minimum median values. '

inputs = [
        observationTable: [
                name: 'Input table',
                title: 'table of observationSensor containing the training data Set',
                type: String.class
        ],
        noiseMapTable: [
                name: 'Input table',
                title: 'table of noiseMapTable containing the noise maps after simulation',
                type: String.class
        ]
]

outputs = [
        result: [
                name: 'Best Configuration Table',
                description: 'BEST_CONFIG table created ',
                type: Sql.class
        ]
]

static def exec(Connection connection, input){

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Extract best configuration')

    String observationTable = input['observationTable']
    String noiseMapTable = input['noiseMapTable']
    Sql sql = new Sql(connection)

    sql.execute("CREATE TABLE file1_cleaned AS " +
            "SELECT " +
            "    IDRECEIVER AS ID_sensor, " +
            "    T, " +
            "    LEQA " +
            "FROM "+observationTable+"; \n" )
    sql.execute("CREATE TABLE file2_cleaned AS \n" +
            "SELECT \n" +
            "    IDRECEIVER AS ID_sensor, \n" +
            "    IT, \n" +
            "    LEQA\n" +
            "FROM "+noiseMapTable+"; \n" )
    sql.execute("CREATE TABLE joined_data AS \n" +
            "SELECT \n" +
            "    f1.ID_sensor, \n" +
            "    f1.T, \n" +
            "    f2.IT, \n" +
            "    f1.LEQA AS LEQA_file1, \n" +
            "    f2.LEQA AS LEQA_file2\n" +
            "FROM file1_cleaned f1\n" +
            "INNER JOIN file2_cleaned f2 \n" +
            "    ON f1.ID_sensor = f2.ID_sensor;\n")

    sql.execute("CREATE TABLE agg_data AS \n" +
            "SELECT \n" +
            "    T, \n" +
            "    IT, \n" +
            "    MEDIAN(ABS(LEQA_file1 - LEQA_file2)) AS median_abs_diff, \n" +
            "    MEDIAN(LEQA_file1) AS value_file1,\n" +
            "    MEDIAN(LEQA_file2) AS value_file2,\n" +
            "    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY LEQA_file1) AS file1_lower,\n" +
            "    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY LEQA_file1) AS file1_upper,\n" +
            "    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY LEQA_file2) AS file2_lower,\n" +
            "    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY LEQA_file2) AS file2_upper\n" +
            "FROM joined_data\n" +
            "GROUP BY T, IT;\n" )

    sql.execute("CREATE TABLE best_IT AS \n" +
            "SELECT \n" +
            "    T,\n" +
            "    IT,\n" +
            "    median_abs_diff,\n" +
            "    value_file1,\n" +
            "    value_file2,\n" +
            "    file1_lower,\n" +
            "    file1_upper,\n" +
            "    file2_lower,\n" +
            "    file2_upper\n" +
            "FROM agg_data\n" +
            "WHERE (T, median_abs_diff) IN (\n" +
            "    SELECT \n" +
            "        T, \n" +
            "        MIN(median_abs_diff)\n" +
            "    FROM agg_data\n" +
            "    GROUP BY T\n" +
            ");")

    sql.execute("CREATE TABLE BEST_CONFIGURATION AS SELECT DISTINCT T, IT,ROUND(median_abs_diff,2) AS LEQA_DIFF FROM BEST_IT;")

    // Create the BEST_CONFIG table to store the best configurations with adding the corresponding combination.
    sql.execute("CREATE TABLE BEST_CONFIG AS " +
            "SELECT DISTINCT b.T, b.IT, b.LEQA_DIFF, a.PRIMARY_VAL, a.SECONDARY_VAL, a.TERTIARY_VAL, a.OTHERS_VAL, a.TEMP_VAL " +
            "FROM BEST_CONFIGURATION b " +
            "JOIN ALL_CONFIGURATIONS a ON b.IT = a.IT")

    logger.info('End Extract best configuration')
}
// run the script
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

