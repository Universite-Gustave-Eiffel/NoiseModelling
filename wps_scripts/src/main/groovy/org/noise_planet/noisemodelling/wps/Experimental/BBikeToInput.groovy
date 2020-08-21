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
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader

import java.sql.Connection
import java.sql.Statement

title = 'Clean and fence BBBike tables'
description = 'Clean and fence BBBike tables - Convert OSM/OSM.GZ file (https://www.openstreetmap.org) to input tables. ' +
        ' <br>Be careful, this treatment can be blocking if the table is large. Some bugs have also been detected for some specific areas.' +
        '<br> The user can choose to create one to three output tables : <br>' +
        '-  <b> BUILDINGS  </b> : a table containing the building. </br>' +
        '-  <b> GROUND  </b> : surface/ground acoustic absorption table. </br>' +
        '-  <b> ROADS  </b> : a table containing the roads. </br>'

inputs = [newSRID  : [name: 'Projection identifier', title: 'Projection identifier', description: 'New projection identifier (also called SRID) of your table. ' +
        'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). ' +
        '</br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. ' +
        '</br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.',
                      type: Integer.class]]

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
    // Get new SRID
    Integer newSrid = input['newSRID'] as Integer



    // -------------------------
    // Initialize some variables
    // -------------------------

    // This is the name of the OSM tables when the file is imported.
    String[] osm_tables = ["BUILDINGS", "LANDUSE", "\"NATURAL\"",
                            "PLACES", "POINTS","RAILWAYS", "ROADS", "WATERWAYS"]

    String[] osm_tables_temp = ["buildings_temp", "landuse_temp", "natural_temp",
                           "places_temp", "points_temp","railways_temp", "roads_temp", "waterways_temp"]
    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)
    connection = new ConnectionWrapper(connection)

    int srid = 0000
    osm_tables.each { tableName ->
        // get the PrimaryKey field if exists to keep it in the final table
        int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, tableName)

        // Build the result string with every tables
        StringBuilder sbFields = new StringBuilder()
        // Get the column names to keep all column in the final table
        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), tableName)
        int k = 1
        String pkField = ""
        fields.each {
            f ->
                if (f != "THE_GEOM") {
                    sbFields.append(String.format(" , %s ", f))
                }
                if (pkIndex == k) pkField = f.toString()
                k++
        }


        //get SRID of the table
        srid = SFSUtilities.getSRID(connection, TableLocation.parse(tableName))
        // if a SRID exists
        if (srid > 0) {
            if (srid == newSrid)
                resultString = "The table already counts " + newSrid.toString() + " as SRID."
            else {
                sql.execute("CREATE table temp as select ST_Transform(the_geom," + newSrid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
                sql.execute("DROP TABLE" + TableLocation.parse(tableName).toString())
                sql.execute("CREATE TABLE" + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
                sql.execute("DROP TABLE TEMP")
                if (pkField != "") {
                    sql.execute("ALTER TABLE " + tableName.toString() + " ALTER COLUMN " + pkField + " INT NOT NULL;")
                    sql.execute("ALTER TABLE " + tableName.toString() + " ADD PRIMARY KEY (" + pkField + ");  ")
                }
                resultString = "SRID changed from " + srid.toString() + " to " + newSrid.toString() + "."
            }
        } else {     // if the table doesn't have any associated SRID
            sql.execute("CREATE table temp as select ST_SetSRID(the_geom," + newSrid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
            sql.execute("DROP TABLE" + TableLocation.parse(tableName).toString())
            sql.execute("CREATE TABLE" + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
            sql.execute("DROP TABLE TEMP")
            if (pkField != "") {
                sql.execute("ALTER TABLE " + tableName.toString() + " ALTER COLUMN " + pkField + " INT NOT NULL;")
                sql.execute("ALTER TABLE " + tableName.toString() + " ADD PRIMARY KEY (" + pkField + ");  ")
            }
            resultString = "No SRID found ! Table " + tableName.toString() + " has now the SRID : " + newSrid.toString() + "."
        }

    }

    System.out.println('SRID ok')


    String smallOthersChanges = 'drop table WATERWAYS_TEMP if exists;'+
            'Create table WATERWAYS_TEMP as select ST_UNION(the_geom) the_geom, PK, OSM_ID, TYPE, NAME, WIDTH from WATERWAYS ;'+

            'drop table RAILWAYS_TEMP if exists;'+
            'Create table RAILWAYS_TEMP as select ST_UNION(the_geom) the_geom, PK, OSM_ID, TYPE, NAME from RAILWAYS ;'

    sql.execute(smallOthersChanges)


    sql.execute('drop table if exists buildings_temp;'+
            'create table buildings_temp as select ST_MAKEVALID(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(THE_GEOM,0.1),1)) THE_GEOM, PK, OSM_ID, NAME, "TYPE" from buildings  WHERE ST_Perimeter(THE_GEOM)<1000;')


    System.out.println('Make valid ok')

    sql.execute("ALTER TABLE buildings_temp ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE buildings_temp ADD PRIMARY KEY (PK); ")
    sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON buildings_temp(the_geom);'+
            'drop table if exists tmp_relation_buildings;'+
            'create table tmp_relation_buildings as select s1.PK as PK_BUILDING, S2.PK as PK2_BUILDING FROM buildings_temp S1, buildings_temp S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;')

    System.out.println('Intersection founded')

    sql.execute("CREATE INDEX ON tmp_relation_buildings(PK_BUILDING);"+
            "drop table if exists tmp_buildings_truncated;" +
            "create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), 0.1, 'join=mitre')) the_geom from tmp_relation_buildings r, buildings_temp s1, buildings_temp s2 WHERE PK_BUILDING = S1.PK  AND PK2_BUILDING = S2.PK   GROUP BY PK_BUILDING;")

    System.out.println('Intersection tmp_buildings_truncated')
    sql.execute("DROP TABLE IF EXISTS BUILDINGS2;")
    sql.execute("create table BUILDINGS2(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)  as select s.PK, s.the_geom from  BUILDINGS_TEMP s where PK not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, the_geom from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);")

    sql.execute("drop table if exists tmp_buildings_truncated;")

 sql.execute("DROP TABLE IF EXISTS BUILDINGS;")
    sql.execute("create table BUILDINGS as select *, 10.0 HEIGHT FROM BUILDINGS2;")
    sql.execute("DROP TABLE IF EXISTS BUILDINGS2;")
	
    System.out.println('Set height')
    
    sql.execute("drop table ROADS_TEMP if exists;")
    sql.execute("create table ROADS_TEMP as select ST_UNION(a.the_geom) the_geom, a.PK, a.OSM_ID, a.NAME, a.REF,a.TYPE, a.ONEWAY, a.MAXSPEED,  a.BRIDGE from ROADS a WHERE ST_GeometryTypeCode(a.THE_GEOM) = 2 ;")
    sql.execute("drop table ROADS_TEMP2 if exists;")
    sql.execute("CREATE TABLE ROADS_TEMP2 AS SELECT ST_UpdateZ(ST_FORCE3D(THE_GEOM),0.05) THE_GEOM, PK, OSM_ID, NAME,\"REF\" R, \"TYPE\" T,ONEWAY,  MAXSPEED MAX_SPEED, BRIDGE FROM ROADS_TEMP;")

    String Roads_Import2 = "DROP TABLE IF EXISTS ROADS_AADF;\n" +
            "CREATE TABLE ROADS_AADF(ID SERIAL,OSM_ID long , THE_GEOM LINESTRING, CLAS_ADM int, AADF int, CLAS_ALT int) as SELECT null, OSM_ID, THE_GEOM,\n" +
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
            "CASEWHEN(T = 'living_street',6, 6)))))))))))))))))) CLAS_ALT  FROM ROADS_TEMP2 ;"

    sql.execute(Roads_Import2)



    // Create a second sql connection to interact with the database in SQL
    def aadf_d = [17936, 7124, 1400, 700, 350, 175]
    def aadf_e = [3826, 1069, 400, 200, 100, 50]
    def aadf_n = [2152, 712, 200, 100, 50, 25]
    def hv_d = [0.2, 0.2, 0.15, 0.10, 0.05, 0.02]
    def hv_e = [0.2, 0.15, 0.10, 0.06, 0.02, 0.01]
    def hv_n = [0.2, 0.05, 0.05, 0.03, 0.01, 0.0]
    def speed = [110, 80, 50, 50, 30, 30]

    Sql sql2 = new Sql(connection)

    // Create final Road table
    sql.execute("drop table if exists ROADS2;")
    sql.execute("create table ROADS2 (PK serial, ID_WAY integer, THE_GEOM geometry, TV_D integer, TV_E integer,TV_N integer,HV_D integer,HV_E integer,HV_N integer,LV_SPD_D integer,LV_SPD_E integer,LV_SPD_N integer,HV_SPD_D integer, HV_SPD_E integer,HV_SPD_N integer, PVMT varchar(10));")
    def qry = 'INSERT INTO ROADS2(ID_WAY, THE_GEOM, TV_D, TV_E,TV_N,HV_D,HV_E,HV_N,LV_SPD_D,LV_SPD_E,LV_SPD_N,HV_SPD_D , HV_SPD_E ,HV_SPD_N , PVMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'

    sql2.eachRow('SELECT OSM_ID, THE_GEOM, CLAS_ALT FROM ROADS_AADF ;') { row ->
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

    sql.execute("DROP TABLE ROADS_AADF IF EXISTS;")
    sql.execute("DROP TABLE ROADS_TEMP IF EXISTS;")
    sql.execute("DROP TABLE ROADS_TEMP2 IF EXISTS;")

    System.println('The table ROADS has been created.')


    // LANDUSE TO GROUND
    sql.execute("DROP TABLE GROUND IF EXISTS;")
    sql.execute("create table GROUND(PK serial, the_geom geometry, surfcat varchar, G double) as select null,  l.THE_GEOM the_geom , l.TYPE, 1 from LANDUSE l where l.TYPE IN ('grass', 'village_green', 'park');")
    System.out.println('Landuse ok Ground created')



    resultString = resultString + "<br> Calculation Done !"

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Osm To Input Data')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString


}

