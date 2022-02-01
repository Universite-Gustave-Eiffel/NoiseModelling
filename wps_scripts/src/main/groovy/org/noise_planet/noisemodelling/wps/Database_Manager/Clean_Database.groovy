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

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.Statement

title = 'Delete all database tables'
description = 'Delete all non-system tables of the database.'

inputs = [
        areYouSure: [
                name: 'Are you sure ?',
                title: 'Are you sure?',
                description: 'Are you sure you want to delete all the tables in the database?',
                type: Boolean.class
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

def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Clean Database')
    logger.info("inputs {}", input) // log inputs of the run

    // Get name of the table
    Boolean areYouSure  = input['areYouSure'] as Boolean

    // list of the system tables
    List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]

    if (areYouSure) {
        // Build the result string with every tables
        StringBuilder sb = new StringBuilder()

        // Get every table names
        List<String> tables = JDBCUtilities.getTableNames(connection, null, "PUBLIC", "%", null)
        // Loop over the tables
        tables.each { t ->
            TableLocation tab = TableLocation.parse(t)
            if (!ignorelst.contains(tab.getTable())) {
                // Add the name of the table in the string builder
                if (sb.size() > 0) {
                    sb.append(" || ")
                }
                sb.append(tab.getTable())

                // Create a connection statement to interact with the database in SQL
                Statement stmt = connection.createStatement()
                // Drop the table
                stmt.execute("drop table if exists " + tab)
            }
        }
        resultString = "The table(s) " + sb.toString() + " was/were dropped."
    } else {
        resultString = "If you're not sure, we won't do anything !"
    }

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Clean Database')

    // print to WPS Builder
    return resultString

}

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