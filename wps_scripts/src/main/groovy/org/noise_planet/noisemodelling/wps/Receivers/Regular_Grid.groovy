/**
 * @Author Aumond Pierre
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.locationtech.jts.geom.Geometry
import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Calculates a regular grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          fence  : [name: 'Fence', title: 'Fence', min: 0, max: 1, type: Geometry.class],
          fenceTableName  : [name: 'Fence table name', title: 'Fence table name', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', min: 0, max: 1, type: String.class],
          delta    : [name: 'offset', title: 'offset', description: 'Offset in the Cartesian plane in meters', type: Double.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          receiverstablename: [name: 'receiverstablename', description: 'Do not write the name of a table that contains a space. (default : RECEIVERS)', title: 'Name of receivers table', min: 0, max: 1, type: String.class],
          height    : [name: 'height', title: 'height', description: 'Height of receivers in meters', min: 0, max: 1, type: Double.class]]

outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String receivers_table_name = "RECEIVERS"
    if (input['receiverstablename']) {
        receivers_table_name = input['receiverstablename']
    }
    receivers_table_name = receivers_table_name.toUpperCase()

    String fence_table_name = "FENCE_2154"
    if (input['fenceTableName']) {
        fence_table_name = input['fenceTableName']
    }
    fence_table_name = fence_table_name.toUpperCase()


    Double delta = 10
    if (input['delta']) {
        delta = input['delta']
    }

    Double h = 4
    if (input['height']) {
        h = input['height']
    }

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
    }
    building_table_name = building_table_name.toUpperCase()

    System.out.println("--------------------------------------------")
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
        System.out.println("Delete previous receivers grid...")
        sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
        String queryGrid = null



        if (input['fence']) {
            System.out.println("--------------------------------------------")
            System.out.println((String) fence)
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))
            sql.execute(String.format("CREATE TABLE FENCE AS SELECT ST_AsText('"+ fence + "') the_geom"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE_2154"))
            sql.execute(String.format("CREATE TABLE FENCE_2154 AS SELECT ST_TRANSFORM(ST_SetSRID(the_geom,4326),2154) the_geom from FENCE"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))

             queryGrid = String.format("CREATE TABLE "+receivers_table_name+" AS SELECT * FROM ST_MakeGridPoints('FENCE_2154',"
                    + delta + ","
                    + delta + ");")



        }else if (input['fenceTableName']) {

            queryGrid = String.format("CREATE TABLE "+receivers_table_name+" AS SELECT * FROM ST_MakeGridPoints('"+ fence_table_name + "',"
                    + delta + ","
                    + delta + ");")



        }else{
            queryGrid  = String.format("CREATE TABLE "+receivers_table_name+" AS SELECT * FROM ST_MakeGridPoints('"
                    + building_table_name + "',"
                    + delta + ","
                    + delta + ");")
        }

        sql.execute(queryGrid)

         System.out.println("New receivers grid created ...")

        sql.execute("Create spatial index on "+receivers_table_name+"(the_geom);")
        sql.execute("UPDATE "+receivers_table_name+" SET THE_GEOM = ST_UPDATEZ(The_geom,"+h+");")
        sql.execute("ALTER TABLE "+ receivers_table_name +" ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )


        if (input['fence']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("Create spatial index on FENCE_2154(the_geom);")
            sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from FENCE_2154 r where ST_Disjoint(g.the_geom, r.the_geom) limit 1);")
        }
        if (input['fenceTableName']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("Create spatial index on "+fence_table_name+"(the_geom);")
            sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from "+fence_table_name+" r where ST_Disjoint(g.the_geom, r.the_geom) limit 1);")
        }

        if (input['buildingTableName']) {
            System.out.println("Delete receivers inside buildings ...")
            sql.execute("Create spatial index on "+building_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+building_table_name+" b where ST_Z(the_geom) < b.HAUTEUR and g.the_geom && b.the_geom and ST_distance(b.the_geom, g.the_geom) < 1 limit 1);")
        }
        if (input['sourcesTableName']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }


    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done. Table of receivers "+ receivers_table_name +" created !"]
}

