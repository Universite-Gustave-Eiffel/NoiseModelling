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

package org.noise_planet.noisemodelling.wps.Data_Assimilation

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import geoserver.GeoServer
import geoserver.catalog.Store
import org.h2gis.utilities.wrapper.ConnectionWrapper

import java.sql.Connection


title = 'Creation of the result table'
description = 'Creation of the result table.'

inputs = [
        bestConfigTable: [
                name: 'The best configuration table',
                title: 'The best configuration table',
                description: 'The best configuration table',
                type: String.class
        ],
        receiverLevel: [
                name: 'The receivers Level table',
                title: 'The receivers Level table',
                description: 'The receivers Level table ',
                type:  String.class
        ],
        outputTable: [
                name: 'The output  table name',
                title: 'The output  table name',
                description: 'The output  table name',
                type: String.class
        ]
]
outputs = [
        result: [
                name: 'The result table',
                title: 'The result table',
                description: 'The result table',
                type: String.class
        ]
]

@CompileStatic
static def exec(Connection connection,inputs) {
    connection = new ConnectionWrapper(connection)
    String bestConfigTable = inputs['bestConfigTable'] as String
    String receiverLevel = inputs['receiverLevel'] as String
    String outputTable = inputs['outputTable'] as String

    Sql sql = new Sql(connection)
    // Add Timestamp to the NMs
    sql.execute("DROP TABLE "+outputTable+" IF EXISTS;")
    sql.execute("CREATE TABLE "+outputTable+" AS SELECT b.EPOCH TIMESTAMP, a.LAEQ, a.THE_GEOM, a.IDRECEIVER FROM "+bestConfigTable+" b  LEFT JOIN "+receiverLevel+" a ON a.PERIOD = b.IT ; ")
    sql.execute("ALTER TABLE "+outputTable+" ALTER COLUMN TIMESTAMP SET DATA TYPE INTEGER")

    def columnNames = JDBCUtilities.getColumnNames(connection, outputTable)

    columnNames.containsAll(Arrays.asList("IMAP", "LAEQ"))

    return "Calculation Done ! The table "+outputTable+" has been created."

}

// run the script
static def run(input) {

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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}
