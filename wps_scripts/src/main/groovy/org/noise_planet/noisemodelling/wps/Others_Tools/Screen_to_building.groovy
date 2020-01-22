/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection

title = 'Screen to Buildings'
description = 'Screen to Buildings.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          screenTableName  : [name: 'Screen table name', title: 'Screen table name',  description: 'nedd the following columns : Height',type: String.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : SCREENS)', title: 'Name of output table', min: 0, max: 1, type: String.class]]



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

    String screen_table_name =  input['screenTableName']
    screen_table_name = screen_table_name.toUpperCase()

    String building_table_name = ""
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName'] as String
    }

    building_table_name = building_table_name.toUpperCase()

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()
        Sql sql = new Sql(connection)

        sql.execute(String.format("DROP TABLE IF EXISTS SCREENS"))
        sql.execute("create table SCREENS as select * from " + screen_table_name )

        if (input['buildingTableName']) {
            sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")
            sql.execute("CREATE TABLE BUFFERED_SCREENS as select ST_BUFFER(sc.the_geom,0.1, 'join=bevel') the_geom,  HEIGHT HEIGHT from SCREENS sc")

            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_SimplifyPreserveTopology(the_geom,0.1) the_geom, HEIGHT from BUFFERED_SCREENS sc UNION ALL select the_geom, HEIGHT from "+building_table_name+" ")

            sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")


        }else{
            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_SimplifyPreserveTopology(ST_BUFFER(sc.the_geom,0.1, 'join=bevel'),0.1) the_geom, HEIGHT HEIGHT from SCREENS sc")

        }


        sql.execute("Create spatial index on BUILDINGS_SCREENS(the_geom);")
        sql.execute("ALTER TABLE BUILDINGS_SCREENS ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )


    }

    return [tableNameCreated: "Process done ! Table BUILDINGS_SCREENS has been created"]
}

