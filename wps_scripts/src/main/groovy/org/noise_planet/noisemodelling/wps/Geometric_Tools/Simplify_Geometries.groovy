/**
 * @Author Tomáš Anda
 */

package org.noise_planet.noisemodelling.wps.Geometric_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.h2gis.utilities.GeometryTableUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException

title = 'Simplify geometries'

description = 'Use Douglas-Peucker algorithm to simplify geometries in the selected table.' +
              '</br> Input table geometries will be updated.'

inputs = [
        tableName: [
                title      : 'Name of the table',
                name       : 'Name of the table',
                description: 'Name of the table on which geometries will be simplified.', 
                type       : String.class
        ],
        distanceTolerance: [
                name       : 'Distance tolerance',
                title      : 'Distance tolerance',
                description: 'Sets the distance tolerance for the simplification (FLOAT).' +
                             '</br> </br> <b> Default value : 1 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        preserveTopology: [
                title      : 'Preserve topology?',
                name       : 'Preserve topology?',
                description: 'Do you want to preserve topology?' + 
                             '</br> </br> <b> Default value : false </b>',
                min        : 0, max: 1,
                type       : Boolean.class
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

def exec(Connection connection, input) {

    // Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database   
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // Print to command window
    logger.info('Start : Template')
    logger.info("inputs {}", input) // log inputs of the run

    Double distanceTolerance = 1
    if (input['distanceTolerance']) {
        distanceTolerance = input['distanceTolerance'] as Double
    }

    String table_name = input['tableName'] as String
    table_name = table_name.toUpperCase()

    // Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(table_name)
    List<String> geomFields = GeometryTableUtilities.getGeometryFields(connection, sourceTableIdentifier)
    if (geomFields.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }

    // Get the SRID of the table and check if it's in a metric projection
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(table_name))
    if (srid == 3785 || srid == 4326) throw new IllegalArgumentException("Error : This SRID is not metric. Please use another SRID for your table.")
    if (srid == 0) throw new IllegalArgumentException("Error : The table does not have an associated SRID.")

    Boolean preserveTopology = false;
    if (input['preserveTopology']) {
        preserveTopology = input['preserveTopology']
    }

    if (preserveTopology) {
        sql.execute("UPDATE " + table_name + " SET THE_GEOM = ST_SETSRID(ST_SimplifyPreserveTopology(THE_GEOM, " + distanceTolerance + "), " + srid + ");")
    } else {
        logger.info('Topology may not be preserved and may result in invalid geometries')
        sql.execute("UPDATE " + table_name + " SET THE_GEOM = ST_SETSRID(ST_Simplify(THE_GEOM, " + distanceTolerance + "), " + srid + ");")
    }

    resultString = "Process done. Geometries in table " + table_name + " have been simplified."

    logger.info('End : Geometries simplified')

    return resultString
}

