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
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Convert screens to building format.'
description = 'Convert the screens to the building format. A width of 10 cm will be defined. If you also give a building table, this WPS block allows you to merge the two layers together.' +
        '</br> Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.' +
        '</br> </br> <b> The output table is called : BUILDINGS_SCREENS </b> and contain : </br>' +
        '- <b> THE_GEOM </b> : the 2D geometry of the created table (POLYGON or MULTIPOLYGON). </br>' +
        '- <b> HEIGHT </b> : the height of the created polygons (FLOAT)'

inputs = [
        tableBuilding: [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: '<b>Name of the Buildings table.</b>  </br>  ' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                        '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableScreens : [
                name       : 'Screens table name', title: 'Screens table name',
                description: '<b>Name of the Screens table.</b>  </br>  ' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the screens (POLYGON or MULTIPOLYGON). </br>' +
                        '- <b> HEIGHT </b> : the height of the screens (FLOAT)',
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

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Screen to Building')
    logger.info("inputs {}", input) // log inputs of the run

    // import screen_table_name
    String screen_table_name = input['tableScreens']
    // do it case-insensitive
    screen_table_name = screen_table_name.toUpperCase()

    // import building_table_name
    String building_table_name = ""
    if (input['tableBuilding']) {
        building_table_name = input['tableBuilding'] as String
    }
    // do it case-insensitive
    building_table_name = building_table_name.toUpperCase()


    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Make sure that the screens do not cross each other with a 50 cm buffer
    double distance_truncate_screens = 0.5

    // Future width of the screens
    double screens_width = 0.1


    //get SRID of the table
    int sridScreens = SFSUtilities.getSRID(connection, TableLocation.parse(screen_table_name))
    if (sridScreens == 3785 || sridScreens == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for Screens.")
    if (sridScreens == 0) throw new IllegalArgumentException("Error : The table screens does not have an associated SRID.")

    //get SRID of the table
    int sridBuildings = SFSUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    if (sridBuildings == 3785 || sridBuildings == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for Buildings.")
    if (sridBuildings == 0) throw new IllegalArgumentException("Error : The table buildings does not have an associated SRID.")

    if (sridBuildings != sridScreens) throw new IllegalArgumentException("Error : The SRID of table screens and buildings are not the same.")

    // Check for intersections between walls
    int intersectingWalls = sql.firstRow("select count(*) interswalls from " + screen_table_name + " E1, " + screen_table_name + " E2 where E1.pk < E2.pk AND ST_Distance(E1.the_geom, E2.the_geom) < " + distance_truncate_screens + ";")[0] as Integer
    if (intersectingWalls > 0) {
        sql.execute("CREATE SPATIAL INDEX IF NOT EXISTS SCREEN_INDEX ON " + screen_table_name + "(the_geom)")
        sql.execute("drop table if exists tmp_relation_screen_screen")
        sql.execute("create table tmp_relation_screen_screen as select s1.pk as PK_SCREEN, S2.PK as PK2_SCREEN FROM " + screen_table_name + " S1, " + screen_table_name + " S2 WHERE S1.PK < S2.PK AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= " + distance_truncate_screens)
        sql.execute("drop table if exists tmp_screen_truncated")
        sql.execute("create table tmp_screen_truncated as select PK_SCREEN, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), " + distance_truncate_screens + ")) the_geom,s1.height from tmp_relation_screen_screen r, " + screen_table_name + " s1, " + screen_table_name + " s2 WHERE PK_SCREEN = S1.pk AND PK2_SCREEN = S2.PK  GROUP BY pk_screen, s1.height;")
        sql.execute("DROP TABLE IF EXISTS TMP_NEW_SCREENS;")
        sql.execute("create table TMP_NEW_SCREENS as select s.pk, s.the_geom, s.height from  " + screen_table_name + " s where pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, the_geom, height from tmp_screen_truncated;");
        screen_table_name = "TMP_NEW_SCREENS"
    }

    if (building_table_name) {

        // Remove parts of the screen too close from buildings
        // Find screen intersecting buildings
        sql.execute("CREATE SPATIAL INDEX IF NOT EXISTS SCREEN_INDEX ON " + screen_table_name + "(the_geom)")
        sql.execute("drop table if exists tmp_relation_screen_building;")
        sql.execute("create table tmp_relation_screen_building as select b.pk as PK_building, s.pk as pk_screen" +
                " from " + building_table_name + " b, " + screen_table_name + " s where b.the_geom && s.the_geom and" +
                " ST_Distance(b.the_geom, s.the_geom) <= " + distance_truncate_screens)

        // For intersecting screens, remove parts closer than distance_truncate_screens
        sql.execute("drop table if exists tmp_screen_truncated;")
        sql.execute("create table tmp_screen_truncated as select pk_screen, ST_DIFFERENCE(s.the_geom, " +
                "ST_BUFFER(ST_ACCUM(b.the_geom), " + distance_truncate_screens + ")) the_geom,s.height from tmp_relation_screen_building r, " +
                building_table_name + " b, " + screen_table_name + " s WHERE PK_building = b.pk AND pk_screen = s.pk " +
                "GROUP BY pk_screen, s.height;")

        // Merge untruncated screens and truncated screens
        sql.execute("DROP TABLE IF EXISTS TMP_SCREENS")
        sql.execute("create table TMP_SCREENS as select s.pk, s.the_geom, s.height from " + screen_table_name + " s where pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, the_geom, height from tmp_screen_truncated;")
        sql.execute("drop table if exists tmp_screen_truncated;")

        // Convert linestring screens to polygons with buffer function
        sql.execute("DROP TABLE IF EXISTS TMP_BUFFERED_SCREENS")
        sql.execute("CREATE TABLE TMP_BUFFERED_SCREENS as select ST_SETSRID(ST_BUFFER(sc.the_geom," + screens_width + ", 'join=mitre endcap=flat'), ST_SRID(sc.the_geom)) the_geom, HEIGHT from TMP_SCREENS sc")
        sql.execute("DROP TABLE IF EXISTS TMP_SCREENS")

        // Merge buildings and buffered screens
        sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
        sql.execute("CREATE TABLE BUILDINGS_SCREENS as select the_geom the_geom, HEIGHT from TMP_BUFFERED_SCREENS sc UNION ALL select the_geom, HEIGHT from " + building_table_name + " ")
        sql.execute("DROP TABLE IF EXISTS TMP_BUFFERED_SCREENS")
        sql.execute("DROP TABLE IF EXISTS BUFFERED_SCREENS")

    } else {
        sql.execute("DROP TABLE IF EXISTS BUILDINGS_SCREENS")
        sql.execute("CREATE TABLE BUILDINGS_SCREENS as select ST_BUFFER(sc.the_geom," + screens_width + ", 'join=mitre endcap=flat') the_geom, HEIGHT from " + screen_table_name + " sc")

    }

    sql.execute("Create spatial index on BUILDINGS_SCREENS(the_geom);")
    sql.execute("ALTER TABLE BUILDINGS_SCREENS ADD pk INT AUTO_INCREMENT PRIMARY KEY;")

    resultString = "The table BUILDINGS_SCREENS has been created."

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Screen to Building')

    // print to WPS Builder
    return resultString

}

