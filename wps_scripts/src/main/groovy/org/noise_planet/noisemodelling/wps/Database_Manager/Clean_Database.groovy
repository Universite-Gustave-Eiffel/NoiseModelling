/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection
import java.sql.Statement

title = 'Delete all tables'
description = 'Delete all non-system tables from the database.'

inputs = [
    databaseName: [name: 'Name of the database', description : 'Name of the database', title: 'Name of the database', type: String.class]
]

outputs = [
    result: [name: 'Result', title: 'Result', type: String.class]
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

        List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]
        // Excute code
        StringBuilder sb = new StringBuilder()

        // Remove all tables from the database
        List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
        tables.each { t ->
            TableLocation tab = TableLocation.parse(t)
            if(!ignorelst.contains(tab.getTable())) {
                if(sb.size() > 0) {
                    sb.append(" || ")
                }
                sb.append(tab.getTable())

                Statement stmt = connection.createStatement()
                stmt.execute("drop table if exists " + tab)
            }
        }


        // print to Console windows
        return [result : "The table(s) " + sb.toString() + " was/were dropped"]
    }
}