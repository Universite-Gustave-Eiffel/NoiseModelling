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
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection


title = 'Display first rows of a table.'
description = 'Display first rows of a table containing. ' +
        '</br> Be careful, this treatment can be very long if the table is large.'

inputs = [
        linesNumber: [
                name       : 'Number of rows',
                title      : 'Number of rows',
                description: 'Number of rows you want to display. (INTEGER) ' +
                        '</br> </br> <b> Default value : 10 </b> ',
                min        : 0, max: 1,
                type       : Integer.class
        ],
        tableName  : [
                name       : 'Name of the table',
                title      : 'Name of the table',
                description: 'Name of the table you want to display.',
                type       : String.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
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

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Display first rows of a table')
    logger.info("inputs {}", input) // log inputs of the run

    // Get the number of rows the user want to display
    int linesNumber = 10
    if (input['linesNumber']) {
        linesNumber = input['linesNumber'] as Integer
    }

    // Get name of the table
    String tableName = input["tableName"] as String
    // do it case-insensitive
    tableName = tableName.toUpperCase()

    // Create a connection statement to interact with the database in SQL
    Sql sql = new Sql(connection)

    List output = sql.rows(String.format("select * from %s LIMIT %s", tableName, linesNumber.toString()))

    logger.info('End : Display first rows of a table')


    // print to WPS Builder
    return mapToTable(output, sql, tableName, connection)
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

/**
 * Convert a list to HTML table
 * @param list
 * @return
 */
static String mapToTable(List<Map> list, Sql sql, String tableName, Connection connection) {

    StringBuilder output = new StringBuilder()

    Map first = list.first()

    output.append("The total number of rows is " + sql.firstRow('SELECT COUNT(*) FROM ' + tableName)[0])

    //get SRID of the table
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableName))

    if (srid > 0) {
        output.append("</br>")
        output.append("The srid of the table is " + srid)
    } else {
        output.append("</br>")
        output.append("This table doesn't have any srid")
    }

    //get SRID of the table
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(tableName))

    if (pkIndex > 0) {
        output.append("</br>")
        output.append("The table has the following primary key : " + JDBCUtilities.getColumnName(connection, tableName, pkIndex))
    } else {
        output.append("</br>")
        output.append("This table does not have primary key.")
    }


    output.append("</br> </br> ")
    output.append("<table  border=' 1px solid black'><thead><tr>")

    first.each { key, val ->
        output.append("<th>${key}</th>")
    }

    output.append("</tr></thead><tbody>")
    WKTWriter wktWriter = new WKTWriter(3)
    list.each { map ->
        if (map.size() > 0) {

            def values = map.values()

            output.append("<tr>")

            values.each {
                def val = it
                if (it instanceof Geometry) {
                    val = wktWriter.write(it)
                }
                output.append "<td><div style='width: 150px;'>${val}</div></td>"
            }

            output.append("</tr>")
        }
    }
    output.append("</tbody></table>")

    output.toString()
}