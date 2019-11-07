/**
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

title = 'Add_Primary_Key'
description = 'Add a primary key to a table'

inputs = [
  databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : h2gisdb)', min : 0, max : 1, type: String.class],
  pkName: [name: 'Name of the pk field', title: 'Name of the pk field', description : 'Name of the pk field (default : will create a new field named pk_)', min : 0, max : 1, type: String.class],
  table: [name: 'table', description : 'Do not write the name of a table that contains a space.', title: 'Name of the table', type: String.class]
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
        String table = input["table"] as String
        table = table.toUpperCase()
        Statement stmt = connection.createStatement()
        String pkName = null
        if  (input['pkName']){
            pkName = input['pkName'] as String
            //stmt.execute("ALTER TABLE "+ table +" DROP PRIMARY KEY;" )
            stmt.execute("ALTER TABLE "+ table +" ALTER COLUMN "+pkName+" INT NOT NULL;" )
            stmt.execute("ALTER TABLE "+ table +" ADD PRIMARY KEY ("+pkName+");  " )
        }else{
            //stmt.execute("ALTER TABLE "+ table +" DROP PRIMARY KEY;" )
            stmt.execute("ALTER TABLE "+ table +" ADD pk_ INT AUTO_INCREMENT PRIMARY KEY;" )
            pkName = 'pk'
        }

        // print to Console windows
        return [result: table + " have a new primary key called " + pkName]
    }
}