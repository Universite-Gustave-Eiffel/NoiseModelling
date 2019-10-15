/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps

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

title = 'Display Tables'
description = 'Display all tables in the database'

inputs = [
   databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database', type: String.class],
   showColumns: [name: 'Display column names', title: 'Display column names', description: 'Display the names of the table columns (default : yes)', min : 0, max : 1, type: Boolean.class]
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
    Connection connection = openPostgreSQLDataStoreConnection(dbName)

    Boolean showColumnName = true
    if (input['showColumns']){showColumnName = input['showColumns'] as Boolean}

    // Excute code
    StringBuilder sb = new StringBuilder()
    
    List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
    tables.each { t ->
        TableLocation tab = TableLocation.parse(t)
        sb.append(" || ")
        sb.append(tab.getTable())
        
        if (showColumnName){
            sb.append(" ( ")
            List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), t)
            fields.each { f ->
                sb.append(String.format("\t%s - ", f))
            }
            sb.append(" ) ")
        }
    }
    sb.append(" || ")

    // print to Console windows
     return [result : sb.toString()]
}