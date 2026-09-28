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
              'selection of receiver points, or select to include or exclude screens.'

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
    logger.info('Start : Import File')
    logger.info("inputs {}", input) // log inputs of the run

    // Get name of the table
    String tableName = input["tableName"] as String
    if (tableName.isEmpty()) {
        resultString = "tableName is not found."
        throw new Exception('ERROR : ' + resultString)
    }
    sourceTableLocation = TableLocation.parse(tableName, dbType);
    pkObject = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection,sourceTableLocation)
    pkName = pkObject.first()
    pkIndex = pkObject.second()
    if(pkIndex == 0){
        resultString = "table should have one Primary Key."
        throw new Exception('ERROR : ' + resultString)
    }
    geomColumn = GeometryTableUtilities.getFirstGeometryColumnNameAndIndex(connection,sourceTableLocation).first()


    // Get the list of IDs
    String idList = input["copyID"]

    // Copy table with where clause and set index, PK and spatial index
    Statement stmt = connection.createStatement()
    stmt.execute("DROP TABLE IF EXISTS " + tableName + "_SELECT;")
    sqlString = "CREATE TABLE " + tableName + "_SELECT " +
                "AS SELECT * FROM " + tableName + " " +
                "WHERE " + pkName + " IN (" + idList + " );"
    stmt.execute(sqlString)

    stmt.execute("ALTER TABLE " + tableName + "_SELECT ALTER COLUMN " + pkName + " SET NOT NULL;")
    stmt.execute("ALTER TABLE " + tableName + "_SELECT ADD PRIMARY KEY (" + pkName + ");")

    if (!geomColumn.isEmpty()) {
        JDBCUtilities.createSpatialIndex(connection, tableName + "_SELECT", geomColumn);
    }
}

def exec(Connection connection, Map input) {
    return exec(connection, input, new EmptyProgressVisitor())
}