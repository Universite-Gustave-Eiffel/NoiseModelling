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

package org.noise_planet.noisemodelling.scripts.NoiseModelling

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

title = 'Convert emission table from terts to octacve'
description = 'Create a emission table with octave frequency bands from terts bands</br>' +
        '<hr>' +
        'This script converts an existing emission table with terts frequency bands to octave bands.'

inputs = [
        tableName : [
                name       : 'Table name',
                title      : 'Table name',
                description: '&#127757; Name of the table to copy.',
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
    logger.info('Start : Terts to octaves')
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
    stmt.execute("DROP TABLE IF EXISTS " + tableName + "_OCTAVE;")

    def frequencies = [50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
                       800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000] as int[];
    StringBuilder sqlString = new StringBuilder();
    sqlString.append("CREATE TABLE " + tableName + "_OCTAVE " +
            "AS SELECT PK, THE_GEOM, DIR_ID, GS, HRAIL, CREF ");
    for (String period : new String[]{"D", "E", "N"}) {
        for (int i=0; i<24; i=i+3) {
            sqlString.append(", 10*log10(");
            sqlString.append("+power(10,HZ").append(period).append(frequencies[i]).append("/10)");
            sqlString.append("+power(10,HZ").append(period).append(frequencies[i+1]).append("/10)");
            sqlString.append("+power(10,HZ").append(period).append(frequencies[i+2]).append("/10)");
            sqlString.append(") AS HZ").append(period).append(frequencies[i+1]);
        }
    }
    sqlString.append(" FROM ").append(tableName).append(";");
    stmt.execute(sqlString.toString());
    resultString = "Terts bands of " + tableName + " are converted to octaves in table " + tableName + "_OCTAVE."

    stmt.execute("ALTER TABLE " + tableName + "_OCTAVE ALTER COLUMN " + pkName + " SET NOT NULL;")
    stmt.execute("ALTER TABLE " + tableName + "_OCTAVE ADD PRIMARY KEY (" + pkName + ");")

    if (!geomColumn.isEmpty()) {
        JDBCUtilities.createSpatialIndex(connection, tableName + "_OCTAVE", geomColumn);
        resultString += " Spatial index is created."
    }

    logger.info(resultString)
    logger.info('End : Copy table with selection')

    // Output the name of the output table
    return [outputTable: tableName + "_OCTAVE"]
}

def exec(Connection connection, Map input) {
    return exec(connection, input, new EmptyProgressVisitor())
}