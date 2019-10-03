/**
* @Author Hesry Quentin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.scriptwps

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
description = 'Display Tables'

inputs = [
 //       exportPath: [name: 'Export path', title: 'Path of the file to export', description: 'Path of the input File (including extension .csv, .shp, etc.)', type: String.class],
 //       tableToExport: [name: 'Name of the table to export', title: 'Name of the table to export',  type: String.class]
]

outputs = [
        result: [name: 'result', title: 'result', type: Boolean.class]
]

def static Connection openPostgreSQLDataStoreConnection() {
    Store store = new GeoServer().catalog.getStore("h2gis")
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(connection) {
    StringBuilder sb = new StringBuilder()
    List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
    tables.each { t ->
        TableLocation tab = TableLocation.parse(t)
        sb.append(tab.getTable())
        sb.append("\n")
        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), t)
        fields.each { f ->
            sb.append(String.format("\t%s\n", f))
        }
    }
    return sb.toString()
}

def run(input) {
        Connection connection = openPostgreSQLDataStoreConnection()
        return [result : exec(connection)]
}