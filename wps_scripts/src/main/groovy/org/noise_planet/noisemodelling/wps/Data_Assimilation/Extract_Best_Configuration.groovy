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

package org.noise_planet.noisemodelling.wps.Data_Assimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet


title = 'Extraction of the best configurations'
description = 'Extraction of the best maps, i.e. those that minimise the difference between the measured and simulated values, by calculating the minimum median values. '

inputs = [
        observationTable: [
                name: 'Sensors measurement training table',
                title: 'Measurement table',
                description: 'table of observationSensor containing the training data Set',
                type: String.class
        ],
        noiseMapTable: [
                name: 'Noise map table',
                title: 'Noise map table',
                description: 'table of noiseMapTable containing the noise maps after simulation',
                type: String.class
        ],
        tempToleranceThreshold: [
                name: 'temperature tolerance threshold ',
                title: 'temperature tolerance threshold ',
                description: 'temperature tolerance threshold pour extraire la best configuration',
                type: Double.class
        ]
]

outputs = [
        result: [
                name: 'Best Configuration Table',
                title: 'Best Configuration Table',
                description: 'BEST_CONFIGURATION_FULL table created ',
                type: String.class
        ]
]

@CompileStatic
static def exec(Connection connection, input){
    connection = new ConnectionWrapper(connection)
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Extract best configuration')

    String observationTable = input['observationTable']
    String noiseMapTable = input['noiseMapTable']
    double threshold = input['tempToleranceThreshold'] as double

    Sql sql = new Sql(connection)

    sql.execute("ALTER TABLE RECEIVERS_LEVEL ADD COLUMN TEMP DOUBLE PRECISION")
    sql.execute("UPDATE RECEIVERS_LEVEL SET TEMP = (SELECT TEMP_VAL FROM FILTERED_CONFIGURATIONS RC WHERE RC.IT = RECEIVERS_LEVEL.PERIOD)")

    sql.execute("CREATE INDEX IF NOT EXISTS idx_observation_t ON " + observationTable +"(EPOCH); " )
    sql.execute(" CREATE INDEX IF NOT EXISTS idx_observation_L ON  "+ observationTable +"(LAEQ);")

    // Average observed temperatures per time step T
    sql.execute("DROP TABLE IF EXISTS OBS_TEMP_UNIQ; ")
    sql.execute(" CREATE TABLE OBS_TEMP_UNIQ AS " +
            "    SELECT EPOCH,  ROUND(MEDIAN(TEMP),4) AS TEMP" +
            "    FROM " + observationTable + "  GROUP BY EPOCH")
    sql.execute("CREATE INDEX IF NOT EXISTS idx_obs_temp_uniq_t ON OBS_TEMP_UNIQ(EPOCH);")

    // Average simulated temperatures by period
    sql.execute(" DROP TABLE IF EXISTS NOISE_TEMP_UNIQ ")
    sql.execute("CREATE TABLE NOISE_TEMP_UNIQ AS " +
            "    SELECT IT as PERIOD, AVG(TEMP_VAL) AS TEMP" +
            "    FROM FILTERED_CONFIGURATIONS GROUP BY PERIOD;")
    sql.execute("CREATE INDEX IF NOT EXISTS idx_noise_temp_uniq_period ON NOISE_TEMP_UNIQ(PERIOD)")


    sql.execute("CREATE INDEX IF NOT EXISTS idx_obs_temp_uniq_temp ON OBS_TEMP_UNIQ(TEMP);")
    sql.execute("CREATE INDEX IF NOT EXISTS idx_noise_temp_uniq_temp ON NOISE_TEMP_UNIQ(TEMP)")
    // Cartesian product T × PERIOD with gaps
    sql.execute("DROP TABLE IF EXISTS BEST_TEMP ")
    sql.execute("CREATE TABLE BEST_TEMP AS " +
            " SELECT" +
            "    o.EPOCH," +
            "    n.PERIOD," +
            "    o.TEMP AS TEMPOBS," +
            "    n.TEMP AS TEMPSMOD," +
            "    o.TEMP - n.TEMP AS diff_temp " +
            " FROM" +
            "    OBS_TEMP_UNIQ o " +
            " LEFT JOIN" +
            "    NOISE_TEMP_UNIQ n ON n.TEMP BETWEEN o.TEMP - ${threshold} AND o.TEMP + ${threshold};")

    sql.execute("CREATE INDEX IF NOT EXISTS idx_best_temp_period ON BEST_TEMP(PERIOD); " )
    sql.execute(" CREATE INDEX IF NOT EXISTS idx_best_temp_t ON BEST_TEMP(EPOCH);")

    //2. Drop if temp tables exist
    sql.execute("DROP TABLE IF EXISTS filtered_obs;")
    sql.execute("DROP TABLE IF EXISTS filtered_noise;")
    sql.execute("DROP TABLE IF EXISTS agg_data;")

    List<Integer> tValues = new ArrayList<>();
    PreparedStatement st = connection.prepareStatement("SELECT DISTINCT EPOCH FROM " + observationTable);
    ResultSet rs = st.executeQuery()
    while (rs.next()) {
        tValues.add(rs.getInt("EPOCH"))
    }

    // Drop and create all necessary empty tables
    sql.execute("DROP TABLE IF EXISTS filtered_obs")
    sql.execute("CREATE TABLE filtered_obs AS SELECT * FROM "+observationTable +" WHERE 1=0")

    sql.execute("DROP TABLE IF EXISTS filtered_noise")
    sql.execute("CREATE TABLE filtered_noise AS SELECT * FROM "+noiseMapTable +" WHERE 1=0")

    sql.execute("DROP TABLE agg_data IF EXISTS")
    sql.execute("CREATE TABLE agg_data (" +
            "    EPOCH integer," +
            "    PERIOD CHARACTER VARYING," +
            "    median_abs_diff float) ;")

    sql.execute("DROP TABLE BEST_CONFIGURATION IF EXISTS")
    sql.execute("CREATE TABLE BEST_CONFIGURATION (" +
            "    EPOCH integer," +
            "    PERIOD CHARACTER VARYING," +
            "    min_median_diff float) ;")


    int i =0
    int tmax = tValues.size()
    for (int t:tValues){
        i++

        // Empty the filtered tables to avoid data accumulation
        sql.execute("TRUNCATE TABLE filtered_obs")
        sql.execute("TRUNCATE TABLE filtered_noise")
        sql.execute("TRUNCATE TABLE agg_data;")

        sql.execute("""
        INSERT INTO filtered_obs 
        SELECT f1.*
        FROM """ + observationTable + """ f1
        WHERE f1.EPOCH = """ + t+""" ;  """)

        sql.execute("""
        INSERT INTO filtered_noise 
        SELECT f2.*
        FROM """ + noiseMapTable + """ f2
        JOIN BEST_TEMP bt ON f2.PERIOD = bt.PERIOD 
        WHERE bt.EPOCH = """ + t+""" ;  """)

        sql.execute("CREATE INDEX IF NOT EXISTS idx_filtered_noise_period ON filtered_noise(PERIOD); " )
        sql.execute(" CREATE INDEX IF NOT EXISTS idx_filtered_obs_t ON filtered_obs(EPOCH);")

        // Insert into agg_data for current T
        sql.execute("""
        INSERT INTO agg_data
        SELECT 
            f1.EPOCH, 
            f2.PERIOD, 
            ROUND(MEDIAN(ABS(f1.LAEQ - f2.LAEQ)), 4) AS median_abs_diff
        FROM filtered_obs f1, filtered_noise f2 
        WHERE f1.EPOCH= """+t+"""
        GROUP BY f1.EPOCH, f2.PERIOD
        """)

        sql.execute("INSERT INTO BEST_CONFIGURATION  SELECT * FROM agg_data a  WHERE median_abs_diff = ( SELECT MIN(median_abs_diff)  FROM agg_data )")

        if (i%1 == 0){
            logger.info('tmax = '+tmax+' (%) ' + 100*i/tmax)
        }
    }

// Create the BEST_CONFIG table to store the best configurations with adding the corresponding combination.
    sql.execute("DROP TABLE BEST_CONFIGURATION_full IF EXISTS")
    sql.execute("CREATE TABLE BEST_CONFIGURATION_full AS SELECT b.*, a.* FROM BEST_CONFIGURATION b, FILTERED_CONFIGURATIONS a WHERE b.PERIOD = a.IT")
    sql.execute("ALTER TABLE BEST_CONFIGURATION_full  DROP COLUMN PERIOD")

    sql.execute("DROP TABLE BEST_CONFIGURATION IF EXISTS")
    sql.execute("DROP TABLE agg_data IF EXISTS")
    sql.execute("DROP TABLE BEST_TEMP IF EXISTS")
    sql.execute("DROP TABLE FILTERED_CONFIGURATIONS IF EXISTS")

    logger.info('End Extract best configuration')
    return "Calculation Done ! The table BEST_CONFIGURATION_FULL has been created."

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