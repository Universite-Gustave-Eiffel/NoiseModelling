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
 */


package org.noise_planet.noisemodelling.wps.Geometric_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.Statement

title = 'Change or set SRID'
description = 'Transforms table from its original coordinate reference system (CRS) to the CRS specified by Spatial Reference Identifier (SRID). </br> If the table does not have an associated SRID, the new SRID is associated with the table.'

inputs = [
        newSRID  : [
                name       : 'Projection identifier',
                title      : 'Projection identifier',
                description: 'New projection identifier (also called SRID) of your table. ' +
                        'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). ' +
                        '</br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. ' +
                        '</br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.',
                type       : Integer.class
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

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null


    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Change SRID')
    logger.info("inputs {}", input) // log inputs of the run

    // Get name of the table
    String tableName = input["tableName"] as String
    // do it case-insensitive
    tableName = tableName.toUpperCase()

    // Get new SRID
    Integer newSrid = input['newSRID'] as Integer


    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()


    // get the PrimaryKey field if exists to keep it in the final table
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, new TableLocation(tableName))

    // Build the result string with every tables
    StringBuilder sbFields = new StringBuilder()
    // Get the column names to keep all column in the final table
    List<String> fields = JDBCUtilities.getColumnNames(connection, tableName)
    int k = 1
    String pkField = ""
    fields.each {
        f ->
            if (f != "THE_GEOM") {
                sbFields.append(String.format(" , %s ", f))
            }
            if (pkIndex == k) pkField = f.toString()
            k++
    }


    //get SRID of the table
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableName))
    // if a SRID exists
    if (srid > 0) {
        if (srid == newSrid)
            resultString = "The table already counts " + newSrid.toString() + " as SRID."
        else {
            stmt.execute("CREATE table temp as select ST_Transform(the_geom," + newSrid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
            stmt.execute("DROP TABLE " + TableLocation.parse(tableName).toString())
            stmt.execute("CREATE TABLE " + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
            stmt.execute("DROP TABLE TEMP")
            if (pkField != "") {
                stmt.execute("ALTER TABLE " + tableName.toString() + " ALTER COLUMN " + pkField + " INT NOT NULL;")
                stmt.execute("ALTER TABLE " + tableName.toString() + " ADD PRIMARY KEY (" + pkField + ");  ")
            }
            resultString = "SRID changed from " + srid.toString() + " to " + newSrid.toString() + "."
        }
    } else {     // if the table doesn't have any associated SRID
        stmt.execute("CREATE table temp as select ST_SetSRID(the_geom," + newSrid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
        stmt.execute("DROP TABLE " + TableLocation.parse(tableName).toString())
        stmt.execute("CREATE TABLE " + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
        stmt.execute("DROP TABLE TEMP")
        if (pkField != "") {
            stmt.execute("ALTER TABLE " + tableName.toString() + " ALTER COLUMN " + pkField + " INT NOT NULL;")
            stmt.execute("ALTER TABLE " + tableName.toString() + " ADD PRIMARY KEY (" + pkField + ");  ")
        }
        resultString = "No SRID found ! Table " + tableName.toString() + " has now the SRID : " + newSrid.toString() + "."
    }


    logger.info(resultString)
    logger.info('End : Change SRID')

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