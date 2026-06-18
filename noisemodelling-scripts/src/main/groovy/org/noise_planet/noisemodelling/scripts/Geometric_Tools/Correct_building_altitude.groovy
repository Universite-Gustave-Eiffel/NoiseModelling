/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Universite Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.scripts.Geometric_Tools

import groovy.sql.Sql
import org.h2gis.utilities.GeometryMetaData
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.Statement

title = 'Correct building altitude'
description = '&#10145;&#65039; Correct building geometries when their Z coordinate is the ground altitude by adding the building height column to each geometry vertex.'

inputs = [
        tableName   : [
                name       : 'Name of the buildings table',
                title      : 'Name of the buildings table',
                description: 'Name of the buildings table on which the geometry altitude will be corrected.',
                type       : String.class
        ],
        heightColumn: [
                name       : 'Height column',
                title      : 'Height column',
                description: 'Column containing building heights in meters. Default: HEIGHT',
                type       : String.class,
                min        : 0, max: 1,
                default    : 'HEIGHT'
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

def exec(Connection connection, Map input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Correct building altitude')
    logger.info("inputs {}", input)

    Sql sql = new Sql(connection)
    Statement statement = connection.createStatement()

    String tableName = normalizeIdentifier(input['tableName'], 'tableName')
    String heightColumn = normalizeIdentifier(input['heightColumn'] ?: 'HEIGHT', 'heightColumn')

    if (!JDBCUtilities.tableExists(connection, tableName)) {
        throw new IllegalArgumentException("The table " + tableName + " does not exist.")
    }
    if (!JDBCUtilities.hasField(connection, tableName, heightColumn)) {
        throw new IllegalArgumentException("Column " + heightColumn + " does not exist in table " + tableName + ".")
    }

    List<String> geometryColumnNames = GeometryTableUtilities.getGeometryColumnNames(connection, tableName)
    if (geometryColumnNames.isEmpty()) {
        throw new IllegalArgumentException("The table " + tableName + " does not contain a geometry column.")
    }

    String geometryColumnName = geometryColumnNames.get(0)
    String tableIdentifier = TableLocation.parse(tableName, DBUtils.getDBType(connection)).toString()
    String geometryIdentifier = statement.enquoteIdentifier(geometryColumnName, false)
    String heightIdentifier = statement.enquoteIdentifier(heightColumn, false)

    Number nullHeightCount = sql.firstRow("SELECT COUNT(*) AS NB FROM " + tableIdentifier +
            " WHERE " + heightIdentifier + " IS NULL").NB as Number
    if (nullHeightCount.intValue() > 0) {
        throw new IllegalArgumentException("Column " + heightColumn + " contains null height value(s).")
    }

    Number negativeHeightCount = sql.firstRow("SELECT COUNT(*) AS NB FROM " + tableIdentifier +
            " WHERE " + heightIdentifier + " < 0").NB as Number
    if (negativeHeightCount.intValue() > 0) {
        throw new IllegalArgumentException("Column " + heightColumn + " contains negative height value(s).")
    }

    Number twoDimensionalGeometryCount = sql.firstRow("SELECT COUNT(*) AS NB FROM " + tableIdentifier +
            " WHERE " + geometryIdentifier + " IS NOT NULL AND ST_IS3D(" + geometryIdentifier + ") <> 1").NB as Number
    if (twoDimensionalGeometryCount.intValue() > 0) {
        throw new IllegalArgumentException("The geometry column " + geometryColumnName + " contains 2D geometries.")
    }

    GeometryMetaData metaData = GeometryTableUtilities.getMetaData(connection,
            TableLocation.parse(tableName, DBUtils.getDBType(connection)), geometryColumnName)
    metaData.setHasZ(true)
    metaData.initGeometryType()

    String sqlUpdate = String.format(Locale.ROOT,
            "ALTER TABLE %s ALTER COLUMN %s %s USING ST_SETSRID(ST_TRANSLATE(%s, 0, 0, %s), ST_SRID(%s))",
            tableIdentifier,
            geometryIdentifier,
            metaData.getSQL(),
            geometryIdentifier,
            heightIdentifier,
            geometryIdentifier)
    statement.execute(sqlUpdate)

    String resultString = "Process done. Building altitude has been corrected in table " + tableName +
            " using " + heightColumn + "."
    logger.info('Result : ' + resultString)
    logger.info('End : Correct building altitude')
    return resultString
}

private static String normalizeIdentifier(Object value, String parameterName) {
    if (value == null || value.toString().trim().isEmpty()) {
        throw new IllegalArgumentException(parameterName + " is required.")
    }
    String identifier = value.toString().trim().toUpperCase(Locale.ROOT)
    if (!(identifier ==~ /[A-Z][A-Z0-9_]*/)) {
        throw new IllegalArgumentException(parameterName + " must be a simple SQL identifier.")
    }
    return identifier
}
