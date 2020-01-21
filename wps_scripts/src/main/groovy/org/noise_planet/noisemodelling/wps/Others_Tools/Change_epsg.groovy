/**
 * @Author Pierre Aumond
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.*

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.geotools.data.simple.*

import java.sql.Connection
import org.locationtech.jts.geom.Geometry
import java.sql.*
import groovy.sql.Sql


title = 'Change EPSG'
description = 'Change EPSG'

inputs = [tableName : [name: 'table name', title: 'table name', type: String.class],
          newEpsg  : [name: 'epsg', title: 'epsg', type: Integer.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class]]


outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String outputString = ""
    String table_name = input['tableName']
    table_name = table_name.toUpperCase()

    int newEpsg = input['newEpsg']

    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()

        int srid = SFSUtilities.getSRID(connection, TableLocation.parse(table_name))
        if (srid >0) {
            connection.createStatement().execute(String.format("UPDATE %s SET THE_GEOM = ST_Transform(the_geom,%d)",
                    TableLocation.parse(table_name).toString(),newEpsg.toInteger()))
            outputString = "Process done ! epsg changed from "+  srid.toString() +" to " + newEpsg.toString()
        } else{
            outputString = "Error ! no epsg found !"
        }

    }
    System.out.println("Process Done !")
    return [tableNameCreated: outputString]
}

