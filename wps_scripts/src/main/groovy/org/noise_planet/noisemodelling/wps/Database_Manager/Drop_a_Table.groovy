/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection
import java.sql.Statement

title = 'Remove table'
description = 'Delete a table from the database. The data will be deleted and can no longer be recovered.'

inputs = [
  databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
  tableToDrop: [name: 'tableToDrop', description : 'Do not write the name of a table that contains a space.', title: 'Name of the table to drop', type: String.class]
]

outputs = [
    result: [name: 'result', title: 'result', type: String.class]
]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    // Get name of the database
    String dbName = ""
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        // Execute
        String table = input["tableToDrop"] as String
        table = table.toUpperCase()
        Statement stmt = connection.createStatement()
        String dropTable = "Drop table if exists " + table
        stmt.execute(dropTable)

        // print to Console windows
        return [result: table + " was dropped !"]
    }
}