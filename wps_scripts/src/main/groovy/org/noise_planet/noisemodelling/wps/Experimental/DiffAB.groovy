

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

package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Map Difference'
description = '&#10145;&#65039; Computes the difference between two noise maps'

inputs = [
        mainMapTable : [
                name: 'Primary map table name',
                title: 'Primary map table name',
                description: 'Name of the table containing the primary noise map data. <br/> <br/>' +
                        'The table must contain the following columns: <br/>' +
                        'IDRECEIVER, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000',
                type: String.class
        ],
        secondMapTable: [
                name: 'Secondary map table name',
                title: 'Secondary map table name',
                description: 'Name of the table containing the second noise map data. <br/> <br/>' +
                        'The table must contain the following columns: <br/>' +
                        'IDRECEIVER, T, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000',
                type: String.class
        ],
        invert: [
                name: 'Invert the substraction',
                title: 'Invert the substraction ?',
                description: 'Invert the substraction? </br>' +
                        '<ul>' +
                        '<li>False (default) : Primary map - Second map</li>' +
                        '<li>True : Second map - Primary map</li></ul>',
                min: 0,
                max: 1,
                type: Boolean.class
        ],
        outTable: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create <br/> <br/>' +
                        'The table will contain the following columns: <br/> ' +
                        'IDSOURCE, IDRECEIVER, T, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000',
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

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : DiffAB')
    logger.info("inputs {}", input)

    String mainMapTable = input['mainMapTable']
    String secondMapTable = input['secondMapTable']

    boolean invert = false;
    if (input['invert']) {
        invert = input['invert'] as boolean;
    }

    String outTable = input['outTable']

    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTable))

    String query = "CREATE TABLE " + outTable +" AS SELECT avg(abs(mmt.LEQA - smt.LEQA)) AS LEQA FROM "+ mainMapTable + " mmt"+
        " JOIN "+ secondMapTable + " smt"+
        " WHERE mmt.IDRECEIVER = smt.IDRECEIVER;"

    logger.info(query)
    sql.execute(query)

    logger.info('End : DiffAB')
    resultString = "Process done. Table " + outTable + " created !"
    logger.info('Result : ' + resultString)
    return resultString;
}

