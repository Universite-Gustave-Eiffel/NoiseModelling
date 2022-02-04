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
 * @Author Aumond Pierre, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Geometric_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryMetaData
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Set_Height'
description = 'Set a new height of a set of receivers or sources (Points or LineStrings).'

inputs = [
        tableName: [
                title      : 'Name of the table',
                name       : 'Name of the table',
                description: 'Name of the table on which the height will be modified.',
                type       : String.class
        ],
        height: [
                name       : 'New height',
                title      : 'New height',
                description: 'New height for the input table. The height must be defined in meters. (FLOAT)',
                type       : Double.class
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

// run the script
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
    String resultString = ""

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Set new height')
    logger.info("inputs {}", input) // log inputs of the run

    String table_name = input['tableName']  as String
    table_name = table_name.toUpperCase()

    Double h = input['height']

    //get SRID of the table
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(table_name))
    if (srid == 3785 || srid == 4326) throw new IllegalArgumentException("Error : This SRID is not metric. Please use another SRID for your table.")
    if (srid == 0) throw new IllegalArgumentException("Error : The table does not have an associated SRID.")

    GeometryMetaData metaData = GeometryTableUtilities.getMetaData(connection, TableLocation.parse(table_name, DBUtils.getDBType(connection)), "THE_GEOM");
    metaData.setSRID(srid)
    metaData.setHasZ(true)
    metaData.initGeometryType()
    connection.createStatement().execute(String.format(Locale.ROOT, "ALTER TABLE %s ALTER COLUMN %s %s USING ST_SetSRID(ST_UPDATEZ(%s, %f),%d)",
            TableLocation.parse(table_name, DBUtils.getDBType(connection)), "THE_GEOM" , metaData.getSQL(),"THE_GEOM", h,srid))

    resultString = "Process done. Table of receivers " + table_name + " has now a new height set to " + h + "."

    logger.info('End : Set new height')

    return resultString
}


