/**
 * @Author Aumond Pierre
 * @Author Can Arnaud
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.convert.ST_Force3D
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.noise_planet.noisemodelling.propagation.ComputeRays

import java.sql.Connection


// Change code and use : createReceiversFromBuildings see down
title = 'Buildings Grid'
description = 'Calculates a regular grid of receivers around buildings. Step is the step value of the grid in the Cartesian plane in meters.'

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          fence  : [name: 'Fence', title: 'Fence', min: 0, max: 1, type: Geometry.class],
          fenceTableName  : [name: 'Fence table name', title: 'Fence table name', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', min: 0, max: 1, type: String.class],
          delta    : [name: 'step', title: 'step', description: 'Step in the Cartesian plane in meters', type: Double.class],
          hasPop    : [name: 'Has Pop ?', title: 'Has Pop ?', description: 'Do the buildings table has a population column ?', min: 0, max: 1, type: Boolean.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class],
          receiverstablename: [name: 'receiverstablename', description: 'Do not write the name of a table that contains a space. (default : RECEIVERS)', title: 'Name of receivers table', min: 0, max: 1, type: String.class],
          height    : [name: 'height', title: 'height', description: 'Height of receivers in meters', min: 0, max: 1, type: Double.class]]

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

    Boolean hasPop = false
    if (input['hasPop']) {
        hasPop = input['hasPop']
    }


    Double h = 4.0d
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

    String fence = null
    if (input['fence']) {
        fence = (String) input['fence']
    }

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()
        Sql sql = new Sql(connection)
        sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
        String queryGrid = null

        if (input['fence']) {
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))
            sql.execute(String.format("CREATE TABLE FENCE AS SELECT ST_AsText('"+ fence + "') the_geom"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE_2154"))
            sql.execute(String.format("CREATE TABLE FENCE_2154 AS SELECT ST_TRANSFORM(ST_SetSRID(the_geom,4326),2154) the_geom from FENCE"))
            sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))


            if (hasPop){
                sql.execute("DROP TABLE IF EXISTS GLUED_BUILDINGS")
                sql.execute("CREATE TABLE GLUED_BUILDINGS(id_build serial, the_geom GEOMETRY, pop FLOAT) AS SELECT null, ST_BUFFER(B.THE_GEOM, 2.0,'endcap=square join=bevel'), POP FROM "+building_table_name+" B, FENCE_2154 A WHERE A.THE_GEOM && B.THE_GEOM AND ST_INTERSECTS(A.THE_GEOM, B.THE_GEOM)")
                sql.execute("DROP TABLE IF EXISTS RECEIVERS")
                sql.execute("CREATE TABLE RECEIVERS(pk serial, the_geom GEOMETRY, id_build INT, pop FLOAT)")
                boolean pushed = false
                sql.withTransaction {
                    sql.withBatch("INSERT INTO RECEIVERS(the_geom, id_build, pop) VALUES (ST_MAKEPOINT(:px, :py, :pz), :id_build, :pop)") { BatchingPreparedStatementWrapper batch ->
                        sql.eachRow("SELECT THE_GEOM,id_build,pop FROM ST_EXPLODE('GLUED_BUILDINGS')") {
                            row ->
                                List<Coordinate> receivers = new ArrayList<>();
                                ComputeRays.splitLineStringIntoPoints((LineString) ST_Force3D.force3D(((Polygon) row["the_geom"]).exteriorRing), delta, receivers)
                                for (Coordinate p : receivers) {
                                    p.setOrdinate(2, h)
                                    batch.addBatch([px:p.x, py:p.y, pz:p.z, id_build:row["id_build"], pop:row["pop"]])
                                    pushed = true
                                }

                        }
                        if(pushed) {
                            batch.executeBatch()
                            pushed = false
                        }
                    }
                }
            }else{
                sql.execute("DROP TABLE IF EXISTS GLUED_BUILDINGS")
                sql.execute("CREATE TABLE GLUED_BUILDINGS(id_build serial, the_geom GEOMETRY) AS SELECT null, ST_BUFFER(B.THE_GEOM, 2.0,'endcap=square join=bevel') FROM "+building_table_name+" B, FENCE_2154 A WHERE A.THE_GEOM && B.THE_GEOM AND ST_INTERSECTS(A.THE_GEOM, B.THE_GEOM)")
                sql.execute("DROP TABLE IF EXISTS RECEIVERS")
                sql.execute("CREATE TABLE RECEIVERS(pk serial, the_geom GEOMETRY, id_build INT)")
                boolean pushed = false
                sql.withTransaction {
                    sql.withBatch("INSERT INTO RECEIVERS(the_geom, id_build) VALUES (ST_MAKEPOINT(:px, :py, :pz), :id_build)") { BatchingPreparedStatementWrapper batch ->
                        sql.eachRow("SELECT THE_GEOM,id_build FROM ST_EXPLODE('GLUED_BUILDINGS')") {
                            row ->
                                List<Coordinate> receivers = new ArrayList<>();
                                ComputeRays.splitLineStringIntoPoints((LineString) ST_Force3D.force3D(((Polygon) row["the_geom"]).exteriorRing), delta, receivers)
                                for (Coordinate p : receivers) {
                                    p.setOrdinate(2, h)
                                    batch.addBatch([px:p.x, py:p.y, pz:p.z, id_build:row["id_build"]])
                                    pushed = true
                                }

                        }
                        if(pushed) {
                            batch.executeBatch()
                            pushed = false
                        }
                    }
                }
            }

            sql.execute("Create spatial index on "+building_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+building_table_name+" b where ST_Z(g.the_geom) < b.HEIGHT+2 and g.the_geom && b.the_geom and ST_INTERSECTS(g.the_geom, ST_BUFFER(B.THE_GEOM, 2.0,'endcap=square join=bevel')) limit 1);")

            sql.execute("DROP TABLE GLUED_BUILDINGS")

        }else if (input['fenceTableName']) {
            sql.execute(String.format("drop table if exists buildtemp"))
            sql.execute(String.format("create table buildtemp (id serial, the_geom polygon) as select null, ST_CONVEXHULL (the_geom) from "+building_table_name+" where ST_AREA(the_geom)>100"));
            sql.execute(String.format("drop table if exists receivers_build"));
            sql.execute(String.format("create table receivers_build (ID int AUTO_INCREMENT PRIMARY KEY, the_geom GEOMETRY) as select NULL, ST_ToMultiPoint(ST_Densify(ST_ToMultiLine(ST_Buffer(b.the_geom, 2, 'quad_segs=0 endcap=butt')), "+delta+")) from buildtemp b"))
            sql.execute(String.format("drop table if exists receivers_temp"));
            sql.execute(String.format("create table receivers_temp as SELECT * from ST_EXPLODE('receivers_build')"))

            queryGrid = String.format("create table "+receivers_table_name+" (PK int AUTO_INCREMENT PRIMARY KEY, the_geom GEOMETRY) as SELECT NULL, r.the_geom from receivers_temp r, FENCE_2154 f where r.the_geom && f.the_geom and ST_INTERSECTS (r.the_geom, f.the_geom)")
            sql.execute(queryGrid)

        }else{

            sql.execute(String.format("drop table if exists buildtemp"))
            sql.execute(String.format("create table buildtemp (id serial, the_geom polygon) as select null, ST_CONVEXHULL (the_geom) from "+building_table_name+" where ST_AREA(the_geom)>100"));

            sql.execute(String.format("drop table if exists receivers_build"))
            sql.execute(String.format("create table receivers_build (ID int AUTO_INCREMENT PRIMARY KEY, the_geom GEOMETRY) as select NULL, ST_ToMultiPoint(ST_Densify(ST_ToMultiLine(ST_Buffer(b.the_geom, 2, 'quad_segs=0 endcap=butt')), "+delta+")) from buildtemp b"))

            sql.execute(String.format("drop table if exists receivers_temp"))
            sql.execute(String.format("create table receivers_temp as SELECT * from ST_EXPLODE('receivers_build')"))

            queryGrid = String.format("create table "+receivers_table_name+" (PK int AUTO_INCREMENT PRIMARY KEY, the_geom GEOMETRY) as SELECT NULL, r.the_geom from receivers_temp r")

            sql.execute(queryGrid)

        }

        // New receivers grid created ...

        sql.execute("Create spatial index on "+receivers_table_name+"(the_geom);")
        sql.execute("UPDATE "+receivers_table_name+" SET THE_GEOM = ST_UPDATEZ(The_geom,"+h+");")
        if (hasPop) sql.execute("UPDATE "+receivers_table_name+" b SET b.pop = b.pop/(SELECT COUNT(*) from receivers a where a.id_build = b.id_build group by a.id_build);")
        if (hasPop) sql.execute("DELETE FROM  "+receivers_table_name+" r WHERE r.pop=0;")

        if (input['fence']) {
            // Delete receivers near sources
            sql.execute("Create spatial index on FENCE_2154(the_geom);")
            sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from FENCE_2154 r where ST_Disjoint(g.the_geom, r.the_geom) limit 1);")
        }

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

    }
    // Process Done
    return [tableNameCreated: "Process done. Table of receivers "+ receivers_table_name +" created !"]
}


