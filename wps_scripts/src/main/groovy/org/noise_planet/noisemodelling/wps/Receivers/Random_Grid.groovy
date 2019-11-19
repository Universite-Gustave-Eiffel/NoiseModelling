/**
 * @Author Aumond Pierre
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection

import groovy.sql.Sql

title = 'Random Grid'
description = 'Calculates a random grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name',  type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', min: 0, max: 1, type: String.class],
          nReceivers    : [name: 'nReceivers', title: 'nReceivers', description: 'Number of receivers', type: Integer.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : RECEIVERS)', title: 'Name of output table', min: 0, max: 1, type: String.class]]

outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    String receivers_table_name = "RECEIVERS"

    Integer nReceivers = 100
    if (input['nReceivers']) {
        nReceivers = input['nReceivers']
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

        def min_max = sql.firstRow("SELECT ST_XMAX(the_geom) as maxX, ST_XMIN(the_geom) as minX, ST_YMAX(the_geom) as maxY, ST_YMIN(the_geom) as minY"
                +" FROM "
                +"("
                +" SELECT ST_Collect(the_geom) as the_geom "
                +" FROM " + sources_table_name
                +" UNION ALL "
                +" SELECT the_geom "
                +" FROM " + building_table_name
                +");")

        sql.execute("create table "+receivers_table_name+" as select ST_MAKEPOINT(RAND()*("+min_max.maxX.toString()+" - "+min_max.minX.toString()+") + "+min_max.minX.toString()+", RAND()*("+min_max.maxY.toString()+" - "+min_max.minY.toString()+") + "+min_max.minY.toString()+") as the_geom from system_range(0,"+nReceivers.toString()+");")


        System.out.println("New receivers grid created ...")

        sql.execute("Create spatial index on "+receivers_table_name+"(the_geom);")
        if (input['buildingTableName']) {
            System.out.println("Delete receivers inside buildings ...")
            sql.execute("Create spatial index on "+building_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+building_table_name+" b where g.the_geom && b.the_geom and ST_distance(b.the_geom, g.the_geom) < 1 limit 1);")
        }
        if (input['sourcesTableName']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done !"]
}
