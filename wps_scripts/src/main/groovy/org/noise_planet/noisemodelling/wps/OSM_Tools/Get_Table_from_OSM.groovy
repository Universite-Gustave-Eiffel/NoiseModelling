/**
 * @Author Aumond Pierre, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.OSM_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.io.osm.OSMRead
import org.locationtech.jts.geom.Geometry

import java.sql.Connection
import java.sql.Statement

// import org.orbisgis.orbisprocess.geoclimate.Geoclimate

title = 'Import from OSM'
description = 'Convert OSM/OSM.GZ file (https://www.openstreetmap.org) to a compatible building and/or vegetation table and/or roads.'

inputs = [pathFile       : [name: 'Path of the input File', description: 'Path of the input File (including extension .osm.gz)', title: 'Path of the input File', type: String.class],
          convert2Building: [name: 'convert2Building', title: 'Extract building', description: 'convert2Building', min: 0, max: 1, type: Boolean.class],
          convert2Vegetation: [name: 'convert2Vegetation', title: 'Extract surface', description: 'convert2Vegetation', min: 0, max: 1, type: Boolean.class],
          convert2Roads: [name: 'convert2Roads', title: 'Extract roads', description: 'Extract roads and metadata, apply default road traffic and speed', min: 0, max: 1, type: Boolean.class],
          AADENF:  [name: 'AADENF', title: 'Annual average DEN flows', description: 'Convert annual average daily flows (AADF) to annual average day/evening/night flows', type: Boolean.class, min: 0, max: 1],
          targetSRID:  [name: 'targetSRID', title: 'Projection identifier', description: 'All coordinates will be projected into the specified SRID. ex: 3857 is Web Mercator projection', type: Integer.class, min: 0, max: 1],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class]]

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
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        exec(connection, input)
    }
}


def exec(connection, input) {

    Boolean convert2Building = false
    if ('convert2Building' in input) {
        convert2Building = input['convert2Building'] as Boolean
    }

    Boolean convert2Vegetation = false
    if ('convert2Vegetation' in input) {
        convert2Vegetation = input['convert2Vegetation'] as Boolean
    }

    Boolean convert2Roads = false
    if ('convert2Roads' in input) {
        convert2Roads = input['convert2Roads'] as Boolean
    }

    Integer srid = 3857
    if ('targetSRID' in input) {
        srid = input['targetSRID'] as Integer
    }

    Boolean aadenf = false
    if ('AADENF' in input) {
        aadenf = input['AADENF'] as Boolean
    }


    String[] osm_tables = ["MAP_NODE","MAP_NODE_MEMBER","MAP_NODE_TAG",
                           "MAP_RELATION","MAP_RELATION_MEMBER","MAP_RELATION_TAG",
                           "MAP_TAG","MAP_WAY","MAP_WAY_MEMBER","MAP_WAY_NODE","MAP_WAY_TAG"]

    List<String> tables = new ArrayList<>();

    String pathFile = input["pathFile"] as String


    Statement sql = connection.createStatement()

    osm_tables.each { tableName ->
        sql.execute("DROP TABLE IF EXISTS " + tableName)
    }

    OSMRead.readOSM(connection, pathFile, "MAP")


    if (convert2Building){
        String Buildings_Import = "DROP TABLE IF EXISTS MAP_BUILDINGS;\n" +
                "CREATE TABLE MAP_BUILDINGS(ID_WAY BIGINT PRIMARY KEY) AS SELECT DISTINCT ID_WAY\n" +
                "FROM MAP_WAY_TAG WT, MAP_TAG T\n" +
                "WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('building');\n" +
                "DROP TABLE IF EXISTS MAP_BUILDINGS_GEOM;\n" +
                "\n" +
                "CREATE TABLE MAP_BUILDINGS_GEOM AS SELECT ID_WAY,\n" +
                "ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)) THE_GEOM FROM (SELECT (SELECT\n" +
                "ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM\n" +
                "MAP_NODE N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY\n" +
                "WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY\n" +
                "FROM MAP_WAY W,MAP_BUILDINGS B\n" +
                "WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) =\n" +
                "ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) AND ST_NUMGEOMETRIES(THE_GEOM) >\n" +
                "2;\n" +
                "DROP TABLE MAP_BUILDINGS;\n" +
                "alter table MAP_BUILDINGS_GEOM add column height double;\n" +
                "update MAP_BUILDINGS_GEOM set height = (select round(\"VALUE\" * 3.0 + RAND() * 2,1) from MAP_WAY_TAG where id_tag = (SELECT ID_TAG FROM MAP_TAG T WHERE T.TAG_KEY = 'building:levels' LIMIT 1) and id_way = MAP_BUILDINGS_GEOM.id_way);\n" +
                "update MAP_BUILDINGS_GEOM set height = round(4 + RAND() * 2,1) where height is null;\n" +
                "drop table if exists BUILDINGS_OSM;\n" +
                "create table BUILDINGS_OSM(id_way serial, the_geom geometry CHECK ST_SRID(THE_GEOM)="+srid+", height double) as select id_way,  ST_SETSRID(ST_SimplifyPreserveTopology(st_buffer(ST_TRANSFORM(ST_SETSRID(THE_GEOM, 4326), "+srid+"), -0.1, 'join=mitre'),0.1), "+srid+") the_geom , height from MAP_BUILDINGS_GEOM;\n" +
                "drop table if exists MAP_BUILDINGS_GEOM;"
        tables.add("BUILDINGS_OSM")
        sql.execute(Buildings_Import)
    }
    if (convert2Vegetation){
        String Vegetation_Import = "DROP TABLE IF EXISTS MAP_SURFACE;\n" +
                "CREATE TABLE MAP_SURFACE(id serial, ID_WAY BIGINT, surf_cat varchar) AS SELECT null, ID_WAY, \"VALUE\" surf_cat\n" +
                "FROM MAP_WAY_TAG WT, MAP_TAG T\n" +
                "WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('surface', 'landcover', 'natural', 'landuse', 'leisure');\n" +
                "DROP TABLE IF EXISTS MAP_SURFACE_GEOM;\n" +
                "CREATE TABLE MAP_SURFACE_GEOM AS SELECT ID_WAY,\n" +
                "ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)) THE_GEOM, surf_cat FROM (SELECT (SELECT\n" +
                "ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM\n" +
                "MAP_NODE N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY\n" +
                "WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY, B.surf_cat\n" +
                "FROM MAP_WAY W,MAP_SURFACE B\n" +
                "WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) =\n" +
                "ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) AND ST_NUMGEOMETRIES(THE_GEOM) >\n" +
                "2;\n" +
                "drop table if exists SURFACE_OSM;\n" +
                "create table SURFACE_OSM(id_way serial, the_geom geometry CHECK ST_SRID(THE_GEOM)="+srid+", surf_cat varchar, G double) as select id_way,  ST_TRANSFORM(ST_SETSRID(THE_GEOM, 4326), "+srid+") the_geom , surf_cat, 1 g from MAP_SURFACE_GEOM where surf_cat IN ('grass', 'village_green', 'park');\n" +
                "drop table if exists MAP_SURFACE_GEOM;"
        tables.add("SURFACE_OSM")
        sql.execute(Vegetation_Import)
    }

    if(convert2Roads) {

        String osmImport = "DROP TABLE MAP_ROADS_speed IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS_speed(ID_WAY BIGINT PRIMARY KEY,MAX_SPEED BIGINT ) AS SELECT DISTINCT ID_WAY, VALUE MAX_SPEED FROM MAP_WAY_TAG WT, MAP_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('maxspeed');\n" +
                "DROP TABLE MAP_ROADS_HGW IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS_HGW(ID_WAY BIGINT PRIMARY KEY,HIGHWAY_TYPE varchar(30) ) AS SELECT DISTINCT ID_WAY, VALUE HIGHWAY_TYPE FROM MAP_WAY_TAG WT, MAP_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('highway');\n" +
                "DROP TABLE MAP_ROADS IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS AS SELECT a.ID_WAY, a.HIGHWAY_TYPE, b.MAX_SPEED  FROM MAP_ROADS_HGW a LEFT JOIN MAP_ROADS_speed b ON a.ID_WAY = b.ID_WAY;\n" +
                "DROP TABLE MAP_ROADS_speed IF EXISTS;\n" +
                "DROP TABLE MAP_ROADS_HGW IF EXISTS;\n" +
                "DROP TABLE IF EXISTS MAP_ROADS_GEOM;\n" +
                "CREATE TABLE MAP_ROADS_GEOM AS SELECT ID_WAY, MAX_SPEED," +
                "st_setsrid(st_updatez(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(ST_TRANSFORM(ST_SETSRID(ST_MAKELINE(THE_GEOM), 4326), "+srid+"),0.1),1), 0.05), "+srid+") THE_GEOM, " +
                "HIGHWAY_TYPE T FROM (SELECT (SELECT\n" + "ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM MAP_NODE\n" +
                "N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY)\n" +
                "THE_GEOM ,W.ID_WAY, B.HIGHWAY_TYPE, B.MAX_SPEED FROM MAP_WAY W,MAP_ROADS B WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE;\n" +
                "DROP TABLE MAP_ROADS;"
        sql.execute(osmImport)


        if(aadenf) {

            def aadf_d =[17936,7124,1400,700,350,175]
            def aadf_e =[3826,1069,400,200,100,50]
            def aadf_n =[2152,712,200,100,50,25]
            def hv_d = [0.2,0.2,0.15,0.10,0.05,0.02]
            def hv_e = [0.2,0.15,0.10,0.06,0.02,0.01]
            def hv_n =[0.2,0.05,0.05,0.03,0.01,0.0]
            def speed = [110,80,50,50,30,30]

            String roadsImport = "DROP TABLE IF EXISTS ROADS_AADF;\n" +
                    "CREATE TABLE ROADS_AADF(ID SERIAL,ID_WAY long , THE_GEOM LINESTRING CHECK ST_SRID(THE_GEOM)="+srid+", CLAS_ADM int, AADF int, CLAS_ALT int) as SELECT null, ID_WAY, THE_GEOM,\n" +
                    "CASEWHEN(T = 'trunk', 21,\n" +
                    "CASEWHEN(T = 'primary', 41,\n" +
                    "CASEWHEN(T = 'secondary', 41,\n" +
                    "CASEWHEN(T = 'tertiary',41, 57)))) CLAS_ADM,\n" +

                    "CASEWHEN(T = 'trunk', 47000,\n" +
                    "CASEWHEN(T = 'primary', 35000,\n" +
                    "CASEWHEN(T = 'secondary', 12000,\n" +
                    "CASEWHEN(T = 'tertiary',7800,\n" +
                    "CASEWHEN(T = 'residential',4000, 1600\n" +
                    "))))) AADF," +

                    "CASEWHEN(T = 'trunk', 1,\n" +
                    "CASEWHEN(T = 'trunk_link', 1,\n" +
                    "CASEWHEN(T = 'primary', 2,\n" +
                    "CASEWHEN(T = 'primary_link', 2,\n" +
                    "CASEWHEN(T = 'secondary', 3,\n" +
                    "CASEWHEN(T = 'secondary_link', 3,\n" +
                    "CASEWHEN(T = 'tertiary', 3,\n" +
                    "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED > 40, 4,\n" +
                    "CASEWHEN(T = 'residential' AND MAX_SPEED > 40, 4,\n" +
                    "CASEWHEN(T = 'unclassified' AND MAX_SPEED > 40, 4,\n" +
                    "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED <= 40, 5,\n" +
                    "CASEWHEN(T = 'residential' AND MAX_SPEED <= 40, 5,\n" +
                    "CASEWHEN(T = 'unclassified' AND MAX_SPEED <= 40, 5,\n" +
                    "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED IS NULL, 5,\n" +
                    "CASEWHEN(T = 'residential' AND MAX_SPEED IS NULL, 5,\n" +
                    "CASEWHEN(T = 'unclassified' AND MAX_SPEED IS NULL, 5,\n" +
                    "CASEWHEN(T = 'service', 6,\n" +
                    "CASEWHEN(T = 'living_street',6, 6)))))))))))))))))) CLAS_ALT  FROM MAP_ROADS_GEOM ;"

            sql.execute(roadsImport)

            Sql sql_connect = new Sql(connection)
            sql.execute("drop table if exists ROADS;")
            sql.execute("create table ROADS (ID_WAY integer, THE_GEOM geometry, TV_D integer, TV_E integer,TV_N integer,HV_D integer,HV_E integer,HV_N integer,LV_SPD_D integer,LV_SPD_E integer,LV_SPD_N integer,HV_SPD_D integer, HV_SPD_E integer,HV_SPD_N integer, PVMT varchar(10));")
            def qry = 'INSERT INTO ROADS(ID_WAY, THE_GEOM, TV_D, TV_E,TV_N,HV_D,HV_E,HV_N,LV_SPD_D,LV_SPD_E,LV_SPD_N,HV_SPD_D , HV_SPD_E ,HV_SPD_N , PVMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'

            sql_connect.eachRow('SELECT ID_WAY, THE_GEOM, CLAS_ALT FROM ROADS_AADF ;') { row ->
                int idway = (int) row[0]
                Geometry the_geom = (Geometry) row[1]
                int classif = (int) row[2] -1


                sql_connect.withBatch(100, qry) { ps ->
                    ps.addBatch(idway as Integer, the_geom as Geometry,
                            aadf_d[classif] as Integer, aadf_e[classif]  as Integer, aadf_n[classif]  as Integer,
                            aadf_d[classif]*hv_d[classif] as Integer, aadf_e[classif]*hv_e[classif]  as Integer, aadf_n[classif]*hv_n[classif]  as Integer,
                            speed[classif] as Integer, speed[classif]  as Integer, speed[classif]  as Integer,
                            speed[classif] as Integer, speed[classif]  as Integer, speed[classif]  as Integer,
                            'NL08' as String)

                }
            }

            } else{
                String roadsImport = "DROP TABLE IF EXISTS ROADS;\n" +
                        "CREATE TABLE ROADS(PK SERIAL,ID_WAY long , THE_GEOM LINESTRING CHECK ST_SRID(THE_GEOM)="+srid+", CLAS_ADM int, AADF int, SPEED int) as SELECT null, ID_WAY, THE_GEOM,\n" +
                        "CASEWHEN(T = 'trunk', 21,\n" +
                        "CASEWHEN(T = 'primary', 41,\n" +
                        "CASEWHEN(T = 'secondary', 41,\n" +
                        "CASEWHEN(T = 'tertiary',41, 57)))) CLAS_ADM,\n" +
                        "CASEWHEN(T = 'trunk', 47000,\n" +
                        "CASEWHEN(T = 'primary', 35000,\n" +
                        "CASEWHEN(T = 'secondary', 12000,\n" +
                        "CASEWHEN(T = 'tertiary',7800,\n" +
                        "CASEWHEN(T = 'residential',4000, 1600\n" +
                        "))))) AADF, MAX_SPEED SPEED FROM MAP_ROADS_GEOM where T in ('trunk', 'primary', 'secondary', 'tertiary', 'residential', 'unclassified') ;"
                sql.execute(roadsImport)
            }

        tables.add("ROADS")
        }




    sql.execute("DROP TABLE MAP_ROADS_GEOM IF EXISTS;")
    sql.execute("DROP TABLE MAP_SURFACE IF EXISTS;")
    sql.execute("DROP TABLE MAP_ROADS_GEOM IF EXISTS;")

    osm_tables.each { tableName ->
        sql.execute("DROP TABLE IF EXISTS " + tableName)
    }



    return [tableNameCreated: String.join(", ", tables)]
}




