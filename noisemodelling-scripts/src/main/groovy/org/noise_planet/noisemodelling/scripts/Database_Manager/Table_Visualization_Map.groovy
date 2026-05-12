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

import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

title = 'Display a table on a map.'
description = '&#10145;&#65039; Display a table containing a geometric column on a map &#128506;</br> '+
        '<hr>' +
        'Technically, it groups all the geometries of a table and returns them in WKT OGC format. </br> </br> '+
        '&#x1F6A8; Be careful, this treatment can be blocked if the table is too large.'

executionTimeout = 120 // For synchronous WPS, it will wait this time before returning a message, but it will still run the execution in the background

inputs = [
        inputSRID: [
                name       : 'Projection identifier',
                title      : 'Projection identifier',
                description: '&#127757; Original projection identifier (also called SRID) of your table. It should be an <a href="https://epsg.io/" target="_blank">EPSG</a> code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). (INTEGER) </br> </br>' +
                             'All coordinates will be projected from the specified EPSG to <a href="https://epsg.io/4326" target="_blank">WGS84</a> coordinates. </br> </br>' +
                              'This entry is optional because many formats already include the projection and you can also import files without geometry attributes.',
                type       : Integer.class,
                min        : 0, max: 1
        ],
        tableName: [
                name       : 'Name of the table',
                title      : 'Name of the table',
                description: 'Name of the table you want to display.',
                type       : String.class
        ]
]

outputs = [
        result: [
                name: 'Result output geometry',
                title: 'Result output geometry',
                description: 'This is the output geometry in WKT OGC format',
                type: Geometry.class
        ]
]

def exec(Connection connection, Map input) {

    // output geometry, the information given back to the user
    Geometry geom = null

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Display a table on a map')
    logger.info("inputs {}", input) // log inputs of the run

    // Get name of the table
    String tableName = input["tableName"] as String
    // do it case-insensitive
    tableName = tableName.toUpperCase()

    // Default SRID (WGS84)
    Integer srid = 4326
    // Get user SRID
    if (input['inputSRID']) {
        srid = input['inputSRID'] as Integer
    }
    DBTypes dbType = DBUtils.getDBType(connection)

    // Read Geometry Index and type of the table
    List<String> spatialFieldNames = GeometryTableUtilities.getGeometryColumnNames(connection, TableLocation.parse(tableName, dbType))

    // If the table does not contain a geometry field
    if (spatialFieldNames.isEmpty()) {
        throw new Exception("The table does not contain a geometry field")
    }

    // Get the SRID of the table
    Integer tableSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableName, dbType))

    if (tableSrid != 0 && tableSrid != srid && input['inputSRID']) throw new Exception("The table already has a different SRID than the one you gave.")

    // Replace default SRID by the srid of the table
    if (tableSrid != 0) srid = tableSrid

    // Display the actual SRID in the command window
    logger.info("The actual SRID of the table is " + srid)

    // Project geometry in WGS84 (EPSG:4326) and groups all the geometries of the table
    String geomField = "ST_ACCUM(ST_TRANSFORM(" + TableLocation.quoteIdentifier(spatialFieldNames.get(0)) + " ,4326))"
    ResultSet rs = stmt.executeQuery(String.format("select %s " + spatialFieldNames.get(0) + " from %s", geomField, tableName))

    // Get the geometry field from the table
    while (rs.next()) {
        geom = (Geometry) rs.getObject(1)
    }

    // print to command window
    String wkt = asWKT(geom)
    if (wkt.size() > 100) {
        logger.info('Result (100 first characters) : ' + wkt.substring(0, 100) + '...')
    } else {
        logger.info('Result : ' + wkt)
    }

    logger.info('End : Display a table on a map')

    // print to WPS Builder
    return geom
}


/**
 * Convert a Geometry value into a Well Known Text value.
 * @param geometry Geometry instance
 * @return The String representation
 */
static String asWKT(Geometry geometry) {
    if (geometry == null) {
        return null
    }
    WKTWriter wktWriter = new WKTWriter()
    return wktWriter.write(geometry)
}