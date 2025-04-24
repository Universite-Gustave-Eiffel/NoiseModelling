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

    sql.execute("ALTER TABLE RECEIVERS_LEVEL ADD COLUMN TEMP DOUBLE PRECISION")
    sql.execute("UPDATE RECEIVERS_LEVEL SET TEMP = (SELECT MAX(TEMP_D) FROM ROADS_CONFIG RC WHERE RC.PERIOD = RECEIVERS_LEVEL.PERIOD)")

    sql.execute("""
    DROP TABLE IF EXISTS DIFF_TEMP;
    CREATE TABLE DIFF_TEMP AS 
    SELECT f1.T AS T, f2.PERIOD AS PERIOD, f1.TEMP AS TEMPOBS, f2.TEMP AS TEMPSMOD, 
           MEDIAN(ABS(f1.TEMP - f2.TEMP)) AS diff_temp
    FROM SENSORS_MEASUREMENTS_TRAINING f1, RECEIVERS_LEVEL f2 
    GROUP BY f1.T, f1.TEMP, f2.TEMP, f2.PERIOD;

    DROP TABLE IF EXISTS BEST_TEMP;
    CREATE TABLE BEST_TEMP AS 
    SELECT T, TEMPOBS, TEMPSMOD,PERIOD, diff_temp FROM (
        SELECT *, 
               ROW_NUMBER() OVER (PARTITION BY T ORDER BY diff_temp) AS rn
        FROM DIFF_TEMP
    ) sub
    WHERE rn = 1;
""")



    sql.execute("DROP TABLE agg_data IF EXISTS")
    sql.execute("CREATE TABLE agg_data AS SELECT " +
            "f1.T, f2.PERIOD, ROUND(MEDIAN(ABS(f1.LEQA - f2.LAEQ)), 4) AS median_abs_diff " +
            "FROM "+observationTable+"  f1, "+noiseMapTable+" f2, best_temp bt " +
            "WHERE bt.PERIOD = f2.PERIOD AND bt.T = f1.T " +
            "GROUP BY f1.T, f2.PERIOD;")

    sql.execute("DROP TABLE BEST_CONFIGURATION IF EXISTS")
    sql.execute("CREATE TABLE BEST_CONFIGURATION AS  SELECT * FROM agg_data a  WHERE median_abs_diff = ( SELECT MIN(median_abs_diff)   FROM agg_data WHERE T = a.T  );")

    // Create the BEST_CONFIG table to store the best configurations with adding the corresponding combination.
    sql.execute("DROP TABLE BEST_CONFIGURATION_full IF EXISTS")
    sql.execute("CREATE TABLE BEST_CONFIGURATION_full AS SELECT b.*, a.* FROM BEST_CONFIGURATION b, ALL_CONFIGURATIONS a WHERE b.PERIOD = a.IT")

    sql.execute("DROP TABLE BEST_CONFIGURATION IF EXISTS")
    sql.execute("DROP TABLE agg_data IF EXISTS")
    sql.execute("DROP TABLE BEST_TEMP IF EXISTS")

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

