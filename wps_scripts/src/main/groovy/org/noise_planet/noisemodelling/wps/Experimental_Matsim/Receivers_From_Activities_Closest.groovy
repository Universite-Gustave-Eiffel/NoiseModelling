/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
/**
 * @Author Valentin Le Bescond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Experimental_Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Chose Closest Receivers For Matsim Activities'
description = 'Chose the closest receiver in a RECEIVERS table for every Mastim Activity in an ACTIVITIES table'

inputs = [
        activitiesTable : [
                name: 'Name of the table containing the activities',
                title: 'Name of the table containing the activities',
                description: 'Name of the table containing the activities' +
                        '<br/>The table must contain the following fields : ' +
                        '<br/>PK, FACILITY, THE_GEOM, TYPES',
                type: String.class
        ],
        receiversTable : [
                name: 'Name of the table containing the receivers',
                title: 'Name of the table containing the receivers',
                description: 'Name of the table containing the receivers' +
                        '<br/>The table must contain the following fields : ' +
                        '<br/>PK, THE_GEOM',
                type: String.class
        ],
        outTableName: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create' +
                        '<br/>The table will contain the following fields : ' +
                        '<br/>PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES',
                type: String.class
        ]
]

outputs = [
        result: [
                name: 'Result output string',
                title: 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type: String.class
        ]
]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // Get name of the database
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString = null

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Receivers_From_Activities_Closest')
    logger.info("inputs {}", input)

    String outTableName = input['outTableName']
    String activitiesTable = input['activitiesTable']
    String receiversTable = input['receiversTable']

    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTableName))

    String query = "CREATE TABLE " + outTableName + '''( 
        PK integer PRIMARY KEY AUTO_INCREMENT,
        FACILITY varchar(255),
        ORIGIN_GEOM geometry,
        THE_GEOM geometry,
        TYPES varchar(255)
    ) AS
    SELECT A.PK, A.FACILITY, A.THE_GEOM AS ORIGIN_GEOM, (
        SELECT R.THE_GEOM 
        FROM ''' + receiversTable + ''' R
        WHERE ST_EXPAND(A.THE_GEOM, 200, 200) && R.THE_GEOM
        ORDER BY ST_Distance(A.THE_GEOM, R.THE_GEOM) ASC LIMIT 1
    ) AS THE_GEOM, A.TYPES 
    FROM ''' + activitiesTable + ''' A ''';
    sql.execute(query);
    sql.execute("CREATE INDEX ON " + outTableName + "(FACILITY)");
    sql.execute("CREATE SPATIAL INDEX ON " + outTableName + "(THE_GEOM)");

    sql.execute("UPDATE " + outTableName + " SET THE_GEOM = CASE " + '''
        WHEN THE_GEOM IS NULL 
        THEN ST_UpdateZ(ORIGIN_GEOM, 4.0)
        ELSE THE_GEOM 
        END
    ''')

    logger.info('End : Receivers_From_Activities_Closest')
    resultString = "Process done. Table of receivers " + outTableName + " created !"
    logger.info('Result : ' + resultString)
    return resultString;
}