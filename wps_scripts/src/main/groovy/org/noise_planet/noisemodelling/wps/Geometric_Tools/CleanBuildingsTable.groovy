/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.wps.Geometric_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Clean BUILDINGS Table'
description = 'Clean the BUILDINGS table, avoiding all overlapping areas and unclosed polygons. ' +
        '</br> The input table will be erased and replaced by the cleaned table.'

inputs = [
        tableName: [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: '<b>Name of the Buildings table.</b> ' +
                        '</br> The table must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed. ' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON).' +
                        '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                type       : String.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

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
    String resultString = null

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Clean buildings table')
    logger.info("inputs {}", input) // log inputs of the run

    // -------------------
    // Get every inputs
    // -------------------

    // import building_table_name
    String building_table_name = input['tableName'] as String
    // do it case-insensitive
    tableName = building_table_name.toUpperCase()

    // -------------------------
    // Initialize some variables
    // -------------------------

    sql.execute('drop table if exists buildings_temp;' +
            'create table buildings_temp as select ST_MAKEVALID(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(THE_GEOM,0.1),0.1)) THE_GEOM, PK, HEIGHT from '+building_table_name+'  WHERE ST_Perimeter(THE_GEOM)<1000;')

    logger.info('Make valid every buildings - ok')

    sql.execute("ALTER TABLE buildings_temp ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE buildings_temp ADD PRIMARY KEY (PK); ")
    sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON buildings_temp(the_geom);' +
            'drop table if exists tmp_relation_buildings;' +
            'create table tmp_relation_buildings as select s1.PK as PK_BUILDING, S2.PK as PK2_BUILDING FROM buildings_temp S1, buildings_temp S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;')

    logger.info('Intersection founded')

    sql.execute("CREATE INDEX ON tmp_relation_buildings(PK_BUILDING);" +
            "drop table if exists tmp_buildings_truncated;" +
            "create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), 0.1, 'join=mitre')) the_geom, s1.HEIGHT from tmp_relation_buildings r, buildings_temp s1, buildings_temp s2 WHERE PK_BUILDING = S1.PK  AND PK2_BUILDING = S2.PK   GROUP BY PK_BUILDING;")

    logger.info('Intersection remove buildings with intersections')

    sql.execute("DROP TABLE IF EXISTS "+building_table_name+";")
    sql.execute("create table "+building_table_name+"(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY, HEIGHT FLOAT)  as select s.PK, s.the_geom, s.HEIGHT from  BUILDINGS_TEMP s where PK not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, the_geom, HEIGHT from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);")

    sql.execute("drop table if exists tmp_buildings_truncated;")

    resultString = resultString + "Calculation Done !"

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Clean buildings table')

    // print to WPS Builder
    return resultString

}
