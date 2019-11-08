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

import groovy.sql.Sql

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

title = 'Drop_a_Table'
description = 'Delete a table from the database'

inputs = [
  databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : h2gisdb)', min : 0, max : 1, type: String.class],
  tableToDrop: [name: 'tableToDrop', description : 'Do not write the name of a table that contains a space.', title: 'Name of the table to drop', type: String.class]
]

outputs = [
    result: [name: 'result', title: 'result', type: String.class]
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
        // Execute
        String table = input["tableToDrop"] as String
        table = table.toUpperCase()

        List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]

        int flag =0
        List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
        tables.each { t ->
            TableLocation tab = TableLocation.parse(t)
            if(!ignorelst.contains(tab.getTable())) {

                if (tab.getTable()==table)   {
                    Statement stmt = connection.createStatement()
                    String dropTable = "Drop table if exists " + table 
                    stmt.execute(dropTable)   
                    returnString = "The table " + table + " was dropped !"
                    flag = 1
                }

                if (flag==0) returnString = "The table to drop was not found"
            }
        }
    
        return [result: returnString]
    }
}
