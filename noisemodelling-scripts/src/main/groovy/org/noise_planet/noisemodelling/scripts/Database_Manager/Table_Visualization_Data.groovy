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


package org.noise_planet.noisemodelling.scripts.Database_Manager



import groovy.sql.Sql

import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.Statement


title = 'Display first rows of a query result.'
description = '&#10145;&#65039; Display the content of a SQL query result. </br>' +
              '<hr>' +
              'You can provide either a table name or a complete SELECT SQL query. </br>' +
              'Using "linesNumber" parameter, you can choose the number of lines to display </br> </br>' +
              '&#x1F6A8; Be careful, this treatment can be very long if the query returns many rows.'

inputs = [linesNumber: [name       : 'Number of rows',
                        title      : 'Number of rows',
                        description: 'Number of rows you want to display. This parameter is ignored if your SQL query already contains a LIMIT clause.',
                        default    : 10,
                        type       : Integer.class],
          tableName  : [name       : 'Table name',
                        title      : 'Table name',
                        description: 'Table name or SQL SELECT query (e.g., mytable or <code>SELECT * FROM mytable</code>)',
                        type       : String.class]]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]



def exec(Connection connection, input) {

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Display first rows of a query result')
    logger.info("inputs {}", input) // log inputs of the run

    // Get the number of rows the user want to display
    int linesNumber = 10
    if (input['linesNumber']) {
        linesNumber = input['linesNumber'] as Integer
    }

    // Get SQL query or table name
    String sqlQuery = input["tableName"] as String

    // Create a connection statement to interact with the database in SQL
    Sql sql = new Sql(connection)

    List output
    String finalQuery
    boolean isTableName = !sqlQuery.toUpperCase().trim().startsWith("SELECT ")

    if (isTableName) {
        // If the input is a table name, create a SELECT query
        Statement statement = connection.createStatement()
        finalQuery = String.format("SELECT * FROM %s", statement.enquoteIdentifier(sqlQuery, false))
    } else {
        // If the input is already a SQL query, use it as is
        // Additional validation: prevent common SQL injection patterns
        def upperQuery = sqlQuery.toUpperCase()
        if (upperQuery.contains(" DROP ") || upperQuery.contains(" DELETE ") ||
                upperQuery.contains(" UPDATE ") || upperQuery.contains(" INSERT ") ||
                upperQuery.contains(" ALTER ") || upperQuery.contains(" CREATE ") ||
                upperQuery.contains(" TRUNCATE ")) {
            throw new IllegalArgumentException("Query contains forbidden SQL keywords")
        }
        finalQuery = sqlQuery
    }

    // Add LIMIT clause if not already present
    if (!finalQuery.toUpperCase().contains("LIMIT")) {
        finalQuery += String.format(" LIMIT %s", linesNumber.toString())
    }

    output = sql.rows(finalQuery)

    // Check if the query returned any result
    if (output.isEmpty()) {
        logger.info("The query did not return any result.")
        return "The query did not return any result."
    }

    logger.info('End : Display first rows of a query result')


    // print to WPS Builder
    return mapToTable(output, sql, sqlQuery, connection, isTableName)
}




/**
 * Convert a list to HTML table
 * @param list
 * @param isTableName true if the query was a simple table name, false if it was a custom SQL query
 * @return
 */
static String mapToTable(List<Map> list, Sql sql, String queryOrTableName, Connection connection, boolean isTableName) {

    StringBuilder output = new StringBuilder()

    Map first = list.first()

    if (isTableName) {
        // Only show total count and metadata for table names
        try {
            output.append("The total number of rows is " + sql.firstRow('SELECT COUNT(*) FROM ' + queryOrTableName.toUpperCase())[0])
        } catch (Exception e) {
            output.append("Unable to determine total row count for this query")
        }

        //get SRID of the table
        try {
            int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(queryOrTableName))

            if (srid > 0) {
                output.append("</br>")
                output.append("The srid of the table is " + srid)
            } else {
                output.append("</br>")
                output.append("This table doesn't have any srid")
            }
        } catch (Exception e) {
            output.append("</br>")
            output.append("Unable to determine SRID information")
        }

        //get primary key of the table
        try {
            int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(queryOrTableName))

            if (pkIndex > 0) {
                output.append("</br>")
                output.append("The table has the following primary key : " + JDBCUtilities.getColumnName(connection, queryOrTableName, pkIndex))
            } else {
                output.append("</br>")
                output.append("This table does not have primary key.")
            }
        } catch (Exception e) {
            output.append("</br>")
            output.append("Unable to determine primary key information")
        }

        output.append("</br> </br> ")
    } else {
        output.append("SQL Query: <code>" + queryOrTableName + "</code></br>")
        output.append("Showing first " + list.size() + " rows</br> </br> ")
    }

    // Add CSS styling for table cells with scroll support
    output.append("<style>")
    output.append(".table-visualization { border-collapse: collapse; width: 100%; }")
    output.append(".table-visualization th { background-color: #f2f2f2; padding: 8px; border: 1px solid black; text-align: left; }")
    output.append(".table-visualization td { border: 1px solid black; padding: 0; }")
    output.append(".table-cell-content { width: 200px; height: 100px; overflow: auto; padding: 8px; word-break: break-word; font-family: monospace; font-size: 12px; }")
    output.append("</style>")

    output.append("<table class='table-visualization'><thead><tr>")

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
                output.append "<td><div class='table-cell-content'>${val}</div></td>"
            }

            output.append("</tr>")
        }
    }
    output.append("</tbody></table>")

    output.toString()
}