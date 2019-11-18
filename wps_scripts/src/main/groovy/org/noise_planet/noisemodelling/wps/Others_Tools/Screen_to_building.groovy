/**
 * @Author Can Arnaud
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import org.h2gis.utilities.wrapper.*

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.geotools.data.simple.*

import java.sql.Connection
import org.locationtech.jts.geom.Geometry
import java.sql.*
import groovy.sql.Sql


title = 'Screen to Buildings'
description = 'Screen to Buildings.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', min: 0, max: 1, type: String.class],
          fence  : [name: 'Fence', title: 'Fence', min: 0, max: 1, type: Geometry.class],
          screenTableName  : [name: 'Screen table name', title: 'Screen table name', type: String.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : SCREENS)', title: 'Name of output table', min: 0, max: 1, type: String.class]]



outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String screen_table_name = "Ecran_zone_etude_FINAL"
    if (input['screenTableName']) {
        screen_table_name = input['screenTableName']
    }
    screen_table_name = screen_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
    }

    building_table_name = building_table_name.toUpperCase()

    String fence = null
    if (input['fence']) {
        fence = (String) input['fence']
    }

    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()
        Sql sql = new Sql(connection)
        System.out.println("Delete previous Screens table...")
        sql.execute(String.format("DROP TABLE IF EXISTS SCREENS"))
        if (input['fence']) {
            System.out.println((String) fence)
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))
            sql.execute(String.format("CREATE TABLE FENCE AS SELECT ST_AsText('"+ fence + "') the_geom"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE_2154"))
            sql.execute(String.format("CREATE TABLE FENCE_2154 AS SELECT ST_TRANSFORM(ST_SetSRID(the_geom,4326),2154) the_geom from FENCE"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))
            sql.execute("Create spatial index on FENCE_2154(the_geom);")

            sql.execute("create table SCREENS as select * from ST_Explode('" + screen_table_name + "')")
        }else{
            sql.execute("create table SCREENS as select * from ST_Explode('" + screen_table_name + "')")
        }


        if (input['buildingTableName']) {

            sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")
            if (input['fence']) {
                sql.execute("CREATE TABLE BUFFERED_SCREENS as select ST_SetSRID(ST_BUFFER(sc.the_geom,0.2, 'endcap=flat'),2154)  the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc, FENCE_2154 fe WHERE sc.the_geom && fe.the_geom AND ST_INTERSECTS(sc.the_geom, fe.the_geom)")
            }else{
                sql.execute("CREATE TABLE BUFFERED_SCREENS as select ST_SetSRID(ST_BUFFER(sc.the_geom,0.2, 'endcap=flat'),2154)  the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc")
            }

            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            sql.execute("CREATE TABLE BUILDINGS_SCREENS as select the_geom, HEIGHT, A from BUFFERED_SCREENS sc UNION select the_geom, HEIGHT, null A from "+building_table_name+" ")

            sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")


        }else{
            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            if (input['fence']) {
                sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_SetSRID(ST_BUFFER(sc.the_geom,0.2, 'endcap=flat'),2154)  the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc, FENCE_2154 fe WHERE sc.the_geom && fe.the_geom AND ST_INTERSECTS(sc.the_geom, fe.the_geom)")
            }else{
                sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_SetSRID(ST_BUFFER(sc.the_geom,0.2, 'endcap=flat'),2154)  the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc")
            }
        }

        sql.execute("Create spatial index on BUILDINGS_SCREENS(the_geom);")
        sql.execute("ALTER TABLE BUILDINGS_SCREENS ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )


    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done ! Table BUILDINGS_SCREENS has been created"]
}

