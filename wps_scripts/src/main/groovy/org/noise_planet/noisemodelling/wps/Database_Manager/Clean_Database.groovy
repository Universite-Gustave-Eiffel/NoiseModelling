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

import org.h2gis.functions.io.csv.*
import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.json.*
import org.h2gis.functions.io.kml.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.utilities.wrapper.ConnectionWrapper

import org.noisemodellingwps.utilities.WpsConnectionWrapper

title = 'Clean all the database'
description = 'Delete all tables from the database.'

inputs = [
    databaseName: [name: 'Name of the database', description : 'Name of the database', title: 'Name of the database', type: String.class]
]

outputs = [
    result: [name: 'Result', title: 'Result', type: String.class]
]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {
    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->

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