/**
 * @Author Aumond Pierre
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore

import java.util.Map
import java.util.HashMap

import java.sql.Connection
import java.sql.Statement

import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.gpx.*
import org.h2gis.functions.io.osm.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*
import org.h2gis.api.EmptyProgressVisitor

import org.h2gis.utilities.wrapper.*

title = 'Regular Grid'
description = 'Calculates a regular grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', min: 0, max: 1, type: String.class],
          delta    : [name: 'offset', title: 'offset', description: 'Offset in the Cartesian plane in meters', type: Double.class],
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

    Double delta = 10
    if (input['delta']) {
        delta = input['delta']
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
        Statement sql = connection.createStatement()

        System.out.println("Delete previous receivers grid...")
        sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))

        String queryGrid = String.format("CREATE TABLE "+receivers_table_name+" AS SELECT * FROM ST_MakeGridPoints('"
                + building_table_name + "',"
                + delta + ","
                + delta + ");")
        sql.execute(queryGrid)

        System.out.println("New receivers grid created ...")

        if (input['buildingTableName']) {
            System.out.println("Delete receivers inside buildings ...")
            sql.execute("Create spatial index on "+receivers_table_name+"(the_geom);")
            sql.execute("Create spatial index on "+building_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+building_table_name+" b where g.the_geom && b.the_geom and ST_distance(b.the_geom, g.the_geom) < 1 limit 1);")
        }
        if (input['sourcesTableName']) {
            System.out.println("Delete receivers near sources ...")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

    }
    System.out.println("Process Done !")
    return [tableNameCreated: "Process done !"]
}
