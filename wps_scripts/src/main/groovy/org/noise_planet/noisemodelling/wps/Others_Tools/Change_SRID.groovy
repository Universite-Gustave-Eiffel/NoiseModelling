/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection

title = 'Change SRID'
description = 'Change SRID'

inputs = [tableName : [name: 'table name', title: 'table name', type: String.class],
          newEpsg  : [name: 'SRID', title: 'SRID', type: Integer.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class]]


outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String outputString = ""
    String table_name = input['tableName']
    table_name = table_name.toUpperCase()

    int newEpsg = input['newEpsg']

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()

        int srid = SFSUtilities.getSRID(connection, TableLocation.parse(table_name))
        if (srid >0) {
            connection.createStatement().execute(String.format("UPDATE %s SET THE_GEOM = ST_Transform(the_geom,%d)",
                    TableLocation.parse(table_name).toString(),newEpsg.toInteger()))
            outputString = "Process done ! epsg changed from "+  srid.toString() +" to " + newEpsg.toString()
        } else{
            connection.createStatement().execute(String.format("UPDATE %s SET THE_GEOM = ST_SetSRID(the_geom,%d)",
                    TableLocation.parse(table_name).toString(),newEpsg.toInteger()))
            outputString = "Error ! no epsg found ! new EPSG set to SRID input ..."
        }

    }
    System.out.println("Process Done !")
    return [tableNameCreated: outputString]
}

