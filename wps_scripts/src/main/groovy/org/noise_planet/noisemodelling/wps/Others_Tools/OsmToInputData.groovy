/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.io.osm.OSMRead
import org.locationtech.jts.geom.Geometry

import java.sql.Connection
import java.sql.Statement

title = 'Import tables from OSM'
description = 'Convert OSM/OSM.GZ file (https://www.openstreetmap.org) to input tables. ' +
        ' <br>Be careful, this treatment can be blocking if the table is large. Some bugs have also been detected for some specific areas.' +
        '<br> The user can choose to create one to three output tables : <br>' +
        '-  <b> BUILDINGS  </b> : a table containing the building. </br>' +
        '-  <b> GROUND  </b> : surface/ground acoustic absorption table. </br>' +
        '-  <b> ROADS  </b> : a table containing the roads. </br>'

inputs = [pathFile        : [name       : 'Path of the OSM file',
                             title      : 'Path of the OSM file',
                             description: 'Path of the OSM file including extension. </br> For example : c:/home/area.osm.gz',
                             type       : String.class],
          convert2Building: [name       : 'Do not import Buildings',
                             title      : 'Do not import Buildings',
                             description: 'If the box is checked, the table BUILDINGS will NOT be extracted. ' +
                                     '<br>  The table will contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT). ' +
                                     'If the height of the buildings is not available then it is deducted from the number of floors (if available) with the addition of a small random variation from one building to another. ' +
                                     'Finally, if no information is available, a height of 5 m is set by default.',
                             min        : 0, max: 1,
                             type       : Boolean.class],
          convert2Ground  : [name       : 'Do not import Surface acoustic absorption',
                             title      : 'Do not import Surface acoustic absorption',
                             description: 'If the box is checked, the table GROUND will NOT be extracted.' +
                                     '</br>The table will contain : </br> ' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the sources (POLYGON or MULTIPOLYGON).</br> ' +
                                     '- <b> G </b> : the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft).</br> ',
                             min        : 0, max: 1,
                             type       : Boolean.class],
          convert2Roads   : [name       : 'Do not import Roads',
                             title      : 'Do not import Roads',
                             description: 'If the box is checked, the table ROADS will NOT be extracted. ' +
                                     "<br>  The table will contain : </br>" +
                                     "- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)<br/>" +
                                     "- <b> TV_D </b> : Hourly average light and heavy vehicle count (6-18h) (DOUBLE)<br/>" +
                                     "- <b>TV_E </b> :  Hourly average light and heavy vehicle count (18-22h) (DOUBLE)<br/>" +
                                     "- <b> TV_N </b> :  Hourly average light and heavy vehicle count (22-6h) (DOUBLE)<br/>" +
                                     "- <b> HV_D </b> :  Hourly average heavy vehicle count (6-18h) (DOUBLE)<br/>" +
                                     "- <b> HV_E </b> :  Hourly average heavy vehicle count (18-22h) (DOUBLE)<br/>" +
                                     "- <b> HV_N </b> :  Hourly average heavy vehicle count (22-6h) (DOUBLE)<br/>" +
                                     "- <b> LV_SPD_D </b> :  Hourly average light vehicle speed (6-18h) (DOUBLE)<br/>" +
                                     "- <b> LV_SPD_E </b> :  Hourly average light vehicle speed (18-22h) (DOUBLE)<br/>" +
                                     "- <b> LV_SPD_N </b> :  Hourly average light vehicle speed (22-6h) (DOUBLE)<br/>" +
                                     "- <b> HV_SPD_D </b> :  Hourly average heavy vehicle speed (6-18h) (DOUBLE)<br/>" +
                                     "- <b> HV_SPD_E </b> :  Hourly average heavy vehicle speed (18-22h) (DOUBLE)<br/>" +
                                     "- <b> HV_SPD_N </b> :  Hourly average heavy vehicle speed (22-6h) (DOUBLE)<br/>" +
                                     "- <b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)" +
                                     "</br> </br> <b> This information is created using the importance of the roads in OSM.</b>.",
                             min        : 0, max: 1,
                             type       : Boolean.class],
          targetSRID      : [name       : 'Target projection identifier', title: 'Target projection identifier',
                             description: 'Target projection identifier (also called SRID) of your table. It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). </br>  The target SRID must be in metric coordinates. </br>', type: Integer.class],
]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = ""

    // print to command window
    System.out.println('Start : Get Input Data from OSM')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    String pathFile = input["pathFile"] as String

    Boolean ignoreBuilding = false
    if ('convert2Building' in input) {
        ignoreBuilding = input['convert2Building'] as Boolean
    }

    Boolean ignoreGround = false
    if ('convert2Ground' in input) {
        ignoreGround = input['convert2Ground'] as Boolean
    }

    Boolean ignoreRoads = false
    if ('convert2Roads' in input) {
        ignoreRoads = input['convert2Roads'] as Boolean
    }

    Integer srid = 3857
    if ('targetSRID' in input) {
        srid = input['targetSRID'] as Integer
    }

    // -------------------------
    // Initialize some variables
    // -------------------------

    // This is the name of the OSM tables when the file is imported.
    String[] osm_tables = ["MAP_NODE", "MAP_NODE_MEMBER", "MAP_NODE_TAG",
                           "MAP_RELATION", "MAP_RELATION_MEMBER", "MAP_RELATION_TAG",
                           "MAP_TAG", "MAP_WAY", "MAP_WAY_MEMBER", "MAP_WAY_NODE", "MAP_WAY_TAG"]

    // Create a sql connection to interact with the database in SQL
    Statement sql = connection.createStatement()


    // -------------------------
    // Run table creation
    // -------------------------

    // drop previous osm tables is exists
    osm_tables.each { tableName ->
        sql.execute("DROP TABLE IF EXISTS " + tableName)
    }

    // import OSM file
    OSMRead.readOSM(connection, pathFile, "MAP")


    // IMPORT BUILDINGS
    if (!ignoreBuilding) {
        String Buildings_Import = '''
                DROP TABLE IF EXISTS MAP_BUILDINGS;
                -- list ways associated to building tag
                CREATE TABLE MAP_BUILDINGS(ID_WAY BIGINT PRIMARY KEY) AS SELECT DISTINCT ID_WAY
                FROM MAP_WAY_TAG WT, MAP_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('building');
                
                -- add ways reffered as building from relation table (using outer ring only)
                insert into MAP_BUILDINGS SELECT DISTINCT ID_WAY
                FROM MAP_RELATION_TAG WT, MAP_TAG T, MAP_WAY_MEMBER WM WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('building') AND WM.ID_RELATION = WT.ID_RELATION AND ROLE = 'outer';
                
                -- create polygons from the selected ways and re-project coordinates
                DROP TABLE IF EXISTS MAP_BUILDINGS_GEOM;
                CREATE TABLE MAP_BUILDINGS_GEOM(ID_WAY INTEGER PRIMARY KEY, THE_GEOM GEOMETRY) AS SELECT ID_WAY,
                ST_TRANSFORM(ST_SETSRID(ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)), 4326), '''+srid+''') THE_GEOM FROM (SELECT (SELECT
                ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM
                MAP_NODE N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY
                WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY
                FROM MAP_WAY W,MAP_BUILDINGS B
                WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_NUMGEOMETRIES(THE_GEOM) > 2 AND ST_GEOMETRYN(THE_GEOM,1) =
                ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM));
                
                CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON MAP_BUILDINGS_GEOM(the_geom);
                -- list buildings that intersects with other buildings that have a greater area
                drop table if exists tmp_relation_buildings_buildings;
                create table tmp_relation_buildings_buildings as select s1.ID_WAY as PK_BUILDING, S2.ID_WAY as PK2_BUILDING FROM MAP_BUILDINGS_GEOM S1, MAP_BUILDINGS_GEOM S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;
                
                -- Alter that small area buildings by removing shared area
                drop table if exists tmp_buildings_truncated;
                create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), 0.1, 'join=mitre')) the_geom from tmp_relation_buildings_buildings r, MAP_BUILDINGS_GEOM s1, MAP_BUILDINGS_GEOM s2 WHERE PK_BUILDING = S1.ID_WAY AND PK2_BUILDING = S2.ID_WAY  GROUP BY PK_BUILDING;
                
                -- merge original buildings with altered buildings 
                DROP TABLE IF EXISTS BUILDINGS;
                create table BUILDINGS(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)  as select s.id_way, ST_SETSRID(s.the_geom, '''+srid+''') from  MAP_BUILDINGS_GEOM s where id_way not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, ST_SETSRID(the_geom, '''+srid+''') from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);

                drop table if exists tmp_buildings_truncated;
                alter table BUILDINGS add column height double;
                -- Update height from way attributes
                update BUILDINGS set height = (select round("VALUE" * 3.0 + RAND() * 2,1) from MAP_WAY_TAG where id_tag = (SELECT ID_TAG FROM MAP_TAG T WHERE T.TAG_KEY = 'building:levels' LIMIT 1) and id_way = BUILDINGS.pk);
                -- update height from relation attributes
                update BUILDINGS set height = (select round("TAG_VALUE" * 3.0 + RAND() * 2,1) from MAP_RELATION_TAG WT, MAP_WAY_MEMBER WM where id_tag = (SELECT ID_TAG FROM MAP_TAG T WHERE T.TAG_KEY = 'building:levels' LIMIT 1) and WM.ID_RELATION = WT.ID_RELATION AND wm.id_way = BUILDINGS.pk);
                -- update for buildings without height infos
                update BUILDINGS set height = round(4 + RAND() * 2,1) where height is null;
                
                drop table if exists MAP_BUILDINGS_GEOM;'''
        sql.execute(Buildings_Import)
        System.println('The table BUILDINGS has been created.')
        resultString = resultString + ' <br> The table BUILDINGS has been created.'
    }

    // IMPORT GROUND
    if (!ignoreGround) {
        String Ground_Import = "DROP TABLE IF EXISTS MAP_SURFACE;\n" +
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
                "drop table if exists GROUND;\n" +
                "create table GROUND(PK serial, the_geom geometry CHECK ST_SRID(THE_GEOM)=" + srid + ", surf_cat varchar, G double) as select null,  ST_TRANSFORM(ST_SETSRID(THE_GEOM, 4326), " + srid + ") the_geom , surf_cat, 1 g from MAP_SURFACE_GEOM where surf_cat IN ('grass', 'village_green', 'park');\n" +
                "drop table if exists MAP_SURFACE_GEOM;"

        sql.execute(Ground_Import)
        sql.execute("DROP TABLE IF EXISTS MAP_SURFACE;")

        System.println('The table GROUND has been created.')
        resultString = resultString + ' <br> The table GROUND has been created.'
    }

    // IMPORT GROUND
    if (!ignoreRoads) {
        String Roads_Import = "DROP TABLE MAP_ROADS_speed IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS_speed(ID_WAY BIGINT PRIMARY KEY,MAX_SPEED BIGINT ) AS SELECT DISTINCT ID_WAY, VALUE MAX_SPEED FROM MAP_WAY_TAG WT, MAP_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('maxspeed');\n" +
                "DROP TABLE MAP_ROADS_HGW IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS_HGW(ID_WAY BIGINT PRIMARY KEY,HIGHWAY_TYPE varchar(30) ) AS SELECT DISTINCT ID_WAY, VALUE HIGHWAY_TYPE FROM MAP_WAY_TAG WT, MAP_TAG T WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('highway');\n" +
                "DROP TABLE MAP_ROADS IF EXISTS;\n" +
                "CREATE TABLE MAP_ROADS AS SELECT a.ID_WAY, a.HIGHWAY_TYPE, b.MAX_SPEED  FROM MAP_ROADS_HGW a LEFT JOIN MAP_ROADS_speed b ON a.ID_WAY = b.ID_WAY;\n" +
                "DROP TABLE MAP_ROADS_speed IF EXISTS;\n" +
                "DROP TABLE MAP_ROADS_HGW IF EXISTS;\n" +
                "DROP TABLE IF EXISTS MAP_ROADS_GEOM;\n" +
                "CREATE TABLE MAP_ROADS_GEOM AS SELECT ID_WAY, MAX_SPEED," +
                "st_setsrid(st_updatez(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(ST_TRANSFORM(ST_SETSRID(ST_MAKELINE(THE_GEOM), 4326), " + srid + "),0.1),1), 0.05), " + srid + ") THE_GEOM, " +
                "HIGHWAY_TYPE T FROM (SELECT (SELECT\n" + "ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM MAP_NODE\n" +
                "N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY)\n" +
                "THE_GEOM ,W.ID_WAY, B.HIGHWAY_TYPE, B.MAX_SPEED FROM MAP_WAY W,MAP_ROADS B WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE;\n" +
                "DROP TABLE MAP_ROADS;"
        sql.execute(Roads_Import)

        def aadf_d = [17936, 7124, 1400, 700, 350, 175]
        def aadf_e = [3826, 1069, 400, 200, 100, 50]
        def aadf_n = [2152, 712, 200, 100, 50, 25]
        def hv_d = [0.2, 0.2, 0.15, 0.10, 0.05, 0.02]
        def hv_e = [0.2, 0.15, 0.10, 0.06, 0.02, 0.01]
        def hv_n = [0.2, 0.05, 0.05, 0.03, 0.01, 0.0]
        def speed = [110, 80, 50, 50, 30, 30]

        String Roads_Import2 = "DROP TABLE IF EXISTS ROADS_AADF;\n" +
                "CREATE TABLE ROADS_AADF(ID SERIAL,ID_WAY long , THE_GEOM LINESTRING CHECK ST_SRID(THE_GEOM)=" + srid + ", CLAS_ADM int, AADF int, CLAS_ALT int) as SELECT null, ID_WAY, THE_GEOM,\n" +
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

        sql.execute(Roads_Import2)

        // Create a second sql connection to interact with the database in SQL
        Sql sql2 = new Sql(connection)

        // Create final Road table
        sql.execute("drop table if exists ROADS;")
        sql.execute("create table ROADS (PK serial, ID_WAY integer, THE_GEOM geometry, TV_D integer, TV_E integer,TV_N integer,HV_D integer,HV_E integer,HV_N integer,LV_SPD_D integer,LV_SPD_E integer,LV_SPD_N integer,HV_SPD_D integer, HV_SPD_E integer,HV_SPD_N integer, PVMT varchar(10));")
        def qry = 'INSERT INTO ROADS(ID_WAY, THE_GEOM, TV_D, TV_E,TV_N,HV_D,HV_E,HV_N,LV_SPD_D,LV_SPD_E,LV_SPD_N,HV_SPD_D , HV_SPD_E ,HV_SPD_N , PVMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'

        sql2.eachRow('SELECT ID_WAY, THE_GEOM, CLAS_ALT FROM ROADS_AADF ;') { row ->
            int idway = (int) row[0]
            Geometry the_geom = (Geometry) row[1]
            int classif = (int) row[2] - 1

            sql2.withBatch(100, qry) { ps ->
                ps.addBatch(idway as Integer, the_geom as Geometry,
                        aadf_d[classif] as Integer, aadf_e[classif] as Integer, aadf_n[classif] as Integer,
                        aadf_d[classif] * hv_d[classif] as Integer, aadf_e[classif] * hv_e[classif] as Integer, aadf_n[classif] * hv_n[classif] as Integer,
                        speed[classif] as Integer, speed[classif] as Integer, speed[classif] as Integer,
                        speed[classif] as Integer, speed[classif] as Integer, speed[classif] as Integer,
                        'NL08' as String)

            }
        }

        sql.execute("DROP TABLE MAP_ROADS_GEOM IF EXISTS;")
        sql.execute("DROP TABLE ROADS_AADF IF EXISTS;")

        System.println('The table ROADS has been created.')
        resultString = resultString + ' <br> The table ROADS has been created.'
    }


    // drop created osm tables is exists
    osm_tables.each { tableName ->
        sql.execute("DROP TABLE IF EXISTS " + tableName)
    }



    resultString = resultString + "<br> Calculation Done !"

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Osm To Input Data')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString


}


/* String roadsImport = "DROP TABLE IF EXISTS ROADS;\n" +
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
sql.execute(roadsImport) */

