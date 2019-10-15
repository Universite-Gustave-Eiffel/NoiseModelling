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

title = 'Display Tables'
description = 'Display all tables in the database'

inputs = [
   databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
   showColumns: [name: 'Display column names', title: 'Display column names', description: 'Display the names of the table columns (default : yes)', min : 0, max : 1, type: Boolean.class]
]

outputs = [
    result: [name: 'Result', title: 'Result', type: String.class]
]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {
    List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]
    Boolean showColumnName = true

    if ('showColumns' in input) {
        showColumnName = input['showColumns'] as Boolean
    }

    // Excute code
    StringBuilder sb = new StringBuilder()

    List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
    tables.each { t ->
        TableLocation tab = TableLocation.parse(t)
        if(!ignorelst.contains(tab.getTable())) {
            sb.append(tab.getTable())
            sb.append("</br>")
            if (showColumnName) {
                List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), t)
                fields.each { f -> sb.append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s</br>", f))
                }
            }
        }
        sb.append("</br>")
    }

    // print to Console windows
    return sb.toString()
}

def run(input) {

    // Get name of the database
    String dbName = ""
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result : exec(connection, input)]
    }

}