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
 * @Author Valentin Le Bescond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.template

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException

// ----------------
// This is a short template.
// Some doubts may remain about writing a WPS script. 
// You can look at other WPS scripts to get inspiration. 
// ----------------

title = 'Simple Title' // This is not use in WPS Builder. The real title of the WPS bloc is the name of the file. It can't contain space or special character.

description = 'Description of the WPS.' +
        '</br> Description of the WPS.' +
        '</br> </br> <b> Description of the output tables : NAME OF THE OUTPUT TABLES </b> ' +
        'and contain : </br>' +
        '-  <b> NAME OF ATTRIBUTE 1 </b> : description attribute 1 (INTEGER, PRIMARY KEY). </br>' +
        '-  <b> NAME OF ATTRIBUTE 2 </b> : description attribute 2 (POINT).'

inputs = [
        input1     : [
                name  : 'Title of the input bloc', // This is not use in WPS Builder. The real title of the WPS bloc is the title item.
                title : 'Title of the input bloc', // Please be short
                description: '<b>Name of the input table.</b>  </br>  ' + // Please be long
                        '<br>  The table shall contain : </br>' +
                        '- <b> NAME OF ATTRIBUTE 1 </b> : description attribute 1 (POLYGON or MULTIPOLYGON). </br>' +
                        '- <b> NAME OF ATTRIBUTE 2 </b> : description attribute 2 (FLOAT)',
                type       : String.class // Input type
        ],
        input2     : [
                name  : 'Title of the input bloc', // This is not use in WPS Builder. The real title of the WPS bloc is the title item.
                title : 'Title of the input bloc', // Please be short
                description: '<b>Name of the input table.</b>  </br>  ' + // Please be long
                        '<br>  The table shall contain : </br>' +
                        '- <b> NAME OF ATTRIBUTE 1 </b> : description attribute 1 (POLYGON or MULTIPOLYGON). </br>' +
                        '- <b> NAME OF ATTRIBUTE 2 </b> : description attribute 2 (FLOAT)',
                min  : 0, max: 1, // it makes this bloc optional
                type  : Integer.class // Input type
        ]
]

outputs = [
        result: [
                name: 'Result output string', // This is not use in WPS Builder. The real title of the WPS bloc is the title item.
                title: 'Result output string', // Please be short
                description: 'This type of result does not allow the blocks to be linked together.',  // This is not use in WPS Builder.
                type: String.class
        ]
]

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {
    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a PostGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// Functions definition
def testFunction(Sql sql, String test) {
    StringBuilder sb = new StringBuilder("create table ")
    sb.append(test)
}

// Main function of the script
def exec(Connection connection, input) {

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database   
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    // This is the output displayed in the WPS Builder. HTML code can be used. 
    // Please inform the user about the actions that have been performed by the script (e.g. table creation), and about any warnings he should receive.
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    // Please use logger.info at least when the script starts, ends and creates a table. 
    // You can use the warning but the user could not be noticed. Please fill in the resultString variable with your warnings.
    // Don't register errors by this logger, use "throw Exception" instead.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Template')
    logger.info("inputs {}", input) // log inputs of the run

    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['input1'] as String
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()

    //Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name)
    List<String> geomFields = GeometryTableUtilities.getGeometryFields(connection, sourceTableIdentifier)

    // Please throw Exception in this format
    if (geomFields.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }

    String receivers_table_name = input['input2']
    // do it case-insensitive
    receivers_table_name = receivers_table_name.toUpperCase()

    // -------------------
    // Calculation
    // -------------------

    def a = 0
    a=a+1

    // -------------------
    // Print results
    // -------------------

    resultString = "Calculation Done ! LDEN_GEOM table has been created."

    // print to command window and geoserver log
    logger.info('Result : ' + resultString)
    logger.info('End : Template')

    // send resultString to WPS Builder
    return resultString

}

