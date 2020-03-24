/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities

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
    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }
    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return exec(connection, input)
    }
}

def exec(Connection connection, input) {

    String screen_table_name =  input['screenTableName']
    screen_table_name = screen_table_name.toUpperCase()

    String building_table_name = ""
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName'] as String
    }

    building_table_name = building_table_name.toUpperCase()

    Sql sql = new Sql(connection)

    double distance_truncate_screens = 0.5

    // Check for intersections between walls
    int intersectingWalls = sql.firstRow("select count(*) interswalls from "+screen_table_name+" E1, "+screen_table_name+" E2 where E1.pk < E2.pk AND ST_Distance(E1.the_geom, E2.the_geom) < "+distance_truncate_screens+";")[0] as Integer
    if(intersectingWalls > 0) {
        sql.execute("CREATE SPATIAL INDEX IF NOT EXISTS SCREEN_INDEX ON "+screen_table_name+"(the_geom)")
        sql.execute("drop table if exists tmp_relation_screen_screen")
        sql.execute("create table tmp_relation_screen_screen as select s1.pk as PK_SCREEN, S2.PK as PK2_SCREEN FROM "+screen_table_name+" S1, "+screen_table_name+" S2 WHERE S1.PK < S2.PK AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= "+distance_truncate_screens)
        sql.execute("drop table if exists tmp_screen_truncated")
        sql.execute("create table tmp_screen_truncated as select PK_SCREEN, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), "+distance_truncate_screens+")) the_geom,s1.height from tmp_relation_screen_screen r, "+screen_table_name+" s1, "+screen_table_name+" s2 WHERE PK_SCREEN = S1.pk AND PK2_SCREEN = S2.PK  GROUP BY pk_screen, s1.height;")
        sql.execute("DROP TABLE IF EXISTS TMP_NEW_SCREENS;")
        sql.execute("create table TMP_NEW_SCREENS as select s.pk, s.the_geom, s.height from  "+screen_table_name+" s where pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, the_geom, height from tmp_screen_truncated;");
        screen_table_name = "TMP_NEW_SCREENS"
    }

    if (input['buildingTableName']) {

        // Remove parts of the screen too close from buildings
        // Find screen intersecting buildings
        sql.execute("CREATE SPATIAL INDEX IF NOT EXISTS SCREEN_INDEX ON "+screen_table_name+"(the_geom)")
        sql.execute("drop table if exists tmp_relation_screen_building;")
        sql.execute("create table tmp_relation_screen_building as select b.pk as PK_building, s.pk as pk_screen" +
                " from "+building_table_name+" b, "+screen_table_name+" s where b.the_geom && s.the_geom and" +
                " ST_Distance(b.the_geom, s.the_geom) <= "+distance_truncate_screens)
        // For intersecting screens, remove parts closer than distance_truncate_screens
        sql.execute("drop table if exists tmp_screen_truncated;")
        sql.execute("create table tmp_screen_truncated as select pk_screen, ST_DIFFERENCE(s.the_geom, " +
                "ST_BUFFER(ST_ACCUM(b.the_geom), "+distance_truncate_screens+")) the_geom,s.height from tmp_relation_screen_building r, " +
                building_table_name+" b, "+screen_table_name+" s WHERE PK_building = b.pk AND pk_screen = s.pk " +
                "GROUP BY pk_screen, s.height;")

        // Merge untruncated screens and truncated screens
        sql.execute("DROP TABLE IF EXISTS TMP_SCREENS")
        sql.execute("create table TMP_SCREENS as select s.pk, s.the_geom, s.height from "+screen_table_name+" s where pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, the_geom, height from tmp_screen_truncated;")
        sql.execute("drop table if exists tmp_screen_truncated;")
        // Convert linestring screens to polygons with buffer function
        sql.execute("DROP TABLE IF EXISTS TMP_BUFFERED_SCREENS")
        sql.execute("CREATE TABLE TMP_BUFFERED_SCREENS as select ST_BUFFER(sc.the_geom,0.1, 'join=mitre endcap=flat') the_geom,  HEIGHT HEIGHT from TMP_SCREENS sc")
        sql.execute("DROP TABLE IF EXISTS TMP_SCREENS")
        // Merge buildings and buffered screens
        sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
        sql.execute("CREATE TABLE BUILDINGS_SCREENS as select the_geom the_geom, HEIGHT from TMP_BUFFERED_SCREENS sc UNION ALL select the_geom, HEIGHT from "+building_table_name+" ")
        sql.execute("DROP TABLE IF EXISTS TMP_BUFFERED_SCREENS")
        sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")


    }else{
        sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
        sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_BUFFER(sc.the_geom,0.1, 'join=mitre endcap=flat') the_geom, HEIGHT HEIGHT from "+screen_table_name+" sc")

    }


    sql.execute("Create spatial index on BUILDINGS_SCREENS(the_geom);")
    sql.execute("ALTER TABLE BUILDINGS_SCREENS ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )


    return [tableNameCreated: "Process done ! Table BUILDINGS_SCREENS has been created"]
}

