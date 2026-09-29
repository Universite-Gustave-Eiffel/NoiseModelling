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

package org.noise_planet.noisemodelling.scripts.Database_Manager

import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.Statement

title = 'Copy a table with a select'
description = '&#10145;&#65039; Copy a (subset) of table to a new table. </br>' +
              '<hr>' +
              'This script copies a table with a `WHERE` query to a new table. It can be used to make a quick ' +
              'selection of receiver points, or select a specific set of sources. The selection is based on either a ' +
              'set of IDs, or a WKT string. When the WKT string is used, it selects all the elements WITHIN the WKT ' +
              'string.'

inputs = [
        tableName : [
                name       : 'Table name',
                title      : 'Table name',
                description: '&#127757; Name of the table to copy.',
                type       : String.class
        ],
        copyID   : [
                name       : 'ID(s) of row to be copied',
                title      : 'ID(s) of row to be copied',
                description: 'Comma separated list of IDs from the source table to be copied to the target table.',
                min        : 0, max: 1,
                type       : String.class
        ],
        wktString : [
                name       : 'WKT string',
                title      : 'WKT string',
                description: 'WKT string of geometry (polygon) that encloses features to be copied. It should have ' +
                             'the same SRID as the source table.</br>' +
                             'Make sure to use double quote (") around the WKT string.',
                min        : 0, max: 1,
                type       : String.class
        ]
]

outputs = [
        outputTable: [
                name: 'Name of the created table',
                title: 'Name of the created table',
                description: 'Name of the created table',
                type: String.class
        ]
]

/**
 * Main method
 * @param connection SQL Connection
 * @param input Map of inputs, should provide the same keys as described in the input metadata
 * @param progress Can be used to display the progression of the computation, and to check if the user canceled the execution
 * @return A map as described in the result metadata
 * @throws java.sql.SQLException if something went wrong
 */
def exec(Connection connection, Map input, ProgressVisitor progress) {
    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))
    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Copy table with selection')
    logger.info("inputs {}", input) // log inputs of the run

    // Get name of the table
    String tableName = input["tableName"] as String
    if (tableName.isEmpty()) {
        logger.error("tableName is a required input parameter.")
        resultString = "tableName is not found."
        throw new Exception('ERROR : ' + resultString)
    }
    sourceTableLocation = TableLocation.parse(tableName, dbType);
    pkObject = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection,sourceTableLocation)
    pkName = pkObject.first()
    pkIndex = pkObject.second()
    if(pkIndex == 0){
        logger.error("table " + tableName + " should have a Primary Key.")
        resultString = "table should have one Primary Key."
        throw new Exception('ERROR : ' + resultString)
    }
    geomColumn = GeometryTableUtilities.getFirstGeometryColumnNameAndIndex(connection,sourceTableLocation).first()
    srid = GeometryTableUtilities.getSRID(connection, sourceTableLocation)

    Statement stmt = connection.createStatement()
    stmt.execute("DROP TABLE IF EXISTS " + tableName + "_SELECT;")

    if ("copyID" in input.keySet()) {
        // Get the list of IDs
        String idList = input["copyID"]

        // Copy table with where clause and set index, PK and spatial index
        sqlString = "CREATE TABLE " + tableName + "_SELECT " +
                "AS SELECT * FROM " + tableName + " " +
                "WHERE " + pkName + " IN (" + idList + " );"
        stmt.execute(sqlString)
        resultString = "Selection of table " + tableName + " is copied to new table " + tableName + "_SELECT."
    }

    if (input["wktString"]){
        wkt = input['wktString']
        sqlString = "CREATE TABLE " + tableName + "_SELECT " +
                "AS SELECT * FROM " + tableName + " " +
                "WHERE ST_WITHIN(" + geomColumn + ", ST_GeomFromText('" + wkt + "'," + srid + "));"
        stmt.execute(sqlString)
        resultString = "Selection of table " + tableName + " is copied to new table " + tableName + "_SELECT."
    }

    stmt.execute("ALTER TABLE " + tableName + "_SELECT ALTER COLUMN " + pkName + " SET NOT NULL;")
    stmt.execute("ALTER TABLE " + tableName + "_SELECT ADD PRIMARY KEY (" + pkName + ");")

    if (!geomColumn.isEmpty()) {
        JDBCUtilities.createSpatialIndex(connection, tableName + "_SELECT", geomColumn);
        resultString += " Spatial index is created."
    }

    logger.info(resultString)
    logger.info('End : Copy table with selection')

    // Output the name of the output table
    return [outputTable: tableName + "_SELECT"]
}

def exec(Connection connection, Map input) {
    return exec(connection, input, new EmptyProgressVisitor())
}