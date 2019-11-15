/**
 * @Author Can Arnaud
 */

package org.noise_planet.noisemodelling.wps.Receivers

import org.h2gis.functions.io.gpx.*
import org.h2gis.functions.io.osm.*

import org.h2gis.utilities.wrapper.*


import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.geotools.data.simple.*

import java.sql.Connection;
import org.locationtech.jts.geom.Geometry
import java.io.*
import java.sql.*
import groovy.sql.Sql

import org.h2gis.functions.io.csv.*
import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.json.*
import org.h2gis.functions.io.kml.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*

title = 'Receivers Grid around Buildings'
description = 'Calculates a regular grid of receivers around buildings. Step is the step value of the grid in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', min: 0, max: 1, type: String.class],
          fence  : [name: 'Fence', title: 'Fence', min: 0, max: 1, type: Geometry.class],
          screenTableName  : [name: 'Screen table name', title: 'Sources table name', type: String.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : SCREENS)', title: 'Name of output table', min: 0, max: 1, type: String.class],
          height    : [name: 'height', title: 'height', description: 'Height of receivers in meters', min: 0, max: 1, type: Double.class]]



outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String screen_table_name = "Ecran_zone_étude_FINAL"
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

            sql.execute("create table SCREENS as select * from ST_Explode('" + screen_table_name + "')")
        }else{
            sql.execute("create table SCREENS as select * from ST_Explode('" + screen_table_name + "')")
        }


        sql.execute("Create spatial index on FENCE_2154(the_geom);")
        //sql.execute("Create spatial index on SCREENS_2154(the_geom);")

        if (input['buildingTableName']) {
            // add Fusion table
            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_BUFFER(sc.the_geom,0.2, 'endcap=flat') the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc, FENCE_2154 fe WHERE sc.the_geom && fe.the_geom AND ST_INTERSECTS(sc.the_geom, fe.the_geom) ")
                }else{
            sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
            sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_BUFFER(sc.the_geom,0.2, 'endcap=flat')  the_geom,  Hmax HEIGHT, Absorbtion A from SCREENS sc, FENCE_2154 fe WHERE sc.the_geom && fe.the_geom AND ST_INTERSECTS(sc.the_geom, fe.the_geom)")
        }

        sql.execute("Create spatial index on BUILDINGS_SCREENS(the_geom);")
        sql.execute("ALTER TABLE BUILDINGS_SCREENS ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )


    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done ! Table BUILDINGS_SCREENS has been created"]
}

