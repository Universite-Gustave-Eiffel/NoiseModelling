/**
 * @Author Aumond Pierre
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.gpx.*
import org.h2gis.functions.io.osm.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*

import java.sql.Connection

import groovy.sql.Sql
import org.h2gis.utilities.wrapper.*


title = 'Random Grid'
description = 'Calculates a random grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name',  type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', min: 0, max: 1, type: String.class],
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

        sql.execute("drop table if exists receivers_build_0;")
        sql.execute("create table receivers_build_0 as SELECT ID ID_BUILD, ST_ExteriorRing(ST_Buffer(ST_SimplifyPreserveTopology(b.the_geom,2), 2, 'quad_segs=0 endcap=flat')) the_geom  from "+building_table_name+" b ;")
        sql.execute("ALTER TABLE receivers_build_0 ADD COLUMN id SERIAL PRIMARY KEY;")

        sql.execute("CREATE TABLE "+receivers_table_name+" AS SELECT ID, the_geom FROM receivers_build_0; ")
        sql.execute("drop table if exists indexed_points;")
        sql.execute("create table indexed_points(old_edge_id integer, the_geom geometry, number_on_line integer, gid integer);")

        sql.execute("drop table if exists bb;")
        sql.execute("create table bb as select ST_EXPAND(ST_Collect(ST_ACCUM(b.the_geom)),0,0)  the_geom from "+building_table_name+" b;")

        sql.execute("drop table if exists receivers_build_ratio;")
        sql.execute("create table receivers_build_ratio as select a.* from receivers_build_0 a, bb b where st_intersects(b.the_geom, a.the_geom) ORDER BY random() LIMIT 10;")

        sql.execute("drop table if exists indexed_points;")
        sql.execute("create table indexed_points(old_edge_id integer, the_geom geometry, number_on_line integer, gid integer);")

        current_fractional = 0.0
        current_number_of_point = 1
        def insertInIndexedPoints = "INSERT INTO indexed_points(old_edge_id, the_geom, number_on_line, gid) VALUES (?, ST_LocateAlong(?, ?), ?, ?);"

        sql.eachRow("SELECT id as id_column, st_transform(the_geom, 2154) as the_geom, ID as build_id, " +
                "st_length(st_transform(the_geom, 2154)) as line_length FROM receivers_build_ratio;"){ row ->
            current_fractional = 0.0
            while(current_fractional <= 1.0){
                sql.withBatch(insertInIndexedPoints) { batch ->
                    batch.addBatch(row.id_column, row.the_geom, current_fractional, current_number_of_point, row.build_id)
                }
                current_fractional = current_fractional + (5 / data[i].line_length)
                current_number_of_point = current_number_of_point + 1
            }
        }

        sql.execute("ALTER TABLE indexed_points ADD COLUMN id SERIAL PRIMARY KEY;")

        sql.execute("drop table if exists receivers_delete;")
        sql.execute("create table receivers_delete as SELECT r.ID, r.the_geom,r.gid build_id from indexed_points r, "+building_table_name+" b where st_intersects(b.the_geom, r.the_geom);")
        sql.execute("delete from indexed_points r where exists (select 1 from receivers_delete rd where r.ID=rd.ID);")
        sql.execute("drop table if exists receivers_delete;")

        sql.execute("alter table indexed_points DROP column id ;")
        sql.execute("alter table indexed_points add column id serial ;")

        sql.execute("drop table if exists receivers;")
        sql.execute("create table receivers as select id id, gid gid, ST_Translate(ST_force3d(the_geom),0,0,4) the_geom from indexed_points;")



        System.out.println("New receivers grid created ...")

        if (input['sourcesTableName']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done !"]
}
