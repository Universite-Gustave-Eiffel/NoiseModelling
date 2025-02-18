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


package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = '&#10145;&#65039; Computes a regular grid of receivers. </br>' +
              '<hr>' +
              'The receivers are spaced at a distance "delta" (Offset) in the Cartesian plane in meters. </br> </br>'+
              'The grid will be based on:<ul>' +
              '<li> the BUILDINGS table extent (option by default)</li>' +
              '<li> <b>OR</b> a single Geometry "fence" (see "Extent filter" parameter).</li></ul></br>' +
              '&#x2705; The output table is called <b>RECEIVERS</b> </br></br>'+
              '<img src="/wps_images/regular_grid_output.png" alt="Regular grid output" width="95%" align="center">'

inputs = [
        buildingTableName : [
                name :       'Buildings table name',
                title:       'Buildings table name',
                description: 'Name of the Buildings table. Receivers inside buildings will be removed.</br></br>' +
                             'The table must contain: <ul>' +
                             '<li><b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON)</li></ul>',
                min        : 0, max: 1,
                type : String.class
        ],
        fence             : [
                name       : 'Extent geometry',
                title      : 'Extent filter',
                description: 'Create receivers only in the provided polygon (fence)',
                min        : 0, max: 1,
                type       : Geometry.class
        ],
        fenceTableName : [
                name       : 'Table bounding box name',
                title      : 'Table bounding box name',
                description: 'Using the bounding box of the given table name, define the envelope of the output grid: <ol>' +
                             '<li> Extract the bounding box of the specified table,</li>' +
                             '<li> then create only receivers on the table bounding box.</li></ol>' +
                             'The given table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type with the appropriate SRID </li></ul>',
                type       : String.class
        ],
        sourcesTableName  : [
                name       : 'Sources table name',
                title      : 'Sources table name',
                description: 'Keep only receivers at least at 1 meters of provided sources geometries </br> </br>' +
                             'The given table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type. </li></ul>',
                min        : 0, max: 1,
                type       : String.class
        ],
        delta             : [
                name       : 'Offset',
                title      : 'Offset',
                description: 'Offset in the Cartesian plane (in meters) </br> </br>' +
                             '&#128736; Default value: <b>10 </b>',
                min        : 0, max        : 1,
                type       : Double.class
        ],
        receiverstablename: [
                name       : 'receiverstablename',
                title      : 'Name of receivers table',
                description: 'Name of the output table.</br> </br>' +
                             'Do not write the name of a table that contains a space.</br> </br>' +
                             '&#128736; Default value: <b>RECEIVERS </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        height : [
                name       : 'Height',
                title      : 'Height',
                description: 'Height of receivers (in meter) (FLOAT) </br> </br>' +
                             '&#128736; Default value: <b>4</b>',
                min        : 0, 
                max        : 1,
                type       : Double.class
        ],
        outputTriangleTable            : [
                name       : 'Output triangle table',
                title      : 'Output triangle table',
                description: 'Output a triangle table in order to be used to generate iso contours with Create_Isosurface',
                min        : 0, max: 1,
                type       : Boolean.class
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

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


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



def exec(connection, Map input) {

    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null


    if (!input.containsKey('fenceTableName') && !input.containsKey('fence')) {
        throw new SQLException("Fence geometry or fence table name must be provided, could be the buildings table or source table.")
    }

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Random grid')
    logger.info("inputs {}", input) // log inputs of the run


    String receivers_table_name = "RECEIVERS"
    if (input['receiverstablename']) {
        receivers_table_name = input['receiverstablename']
    }
    receivers_table_name = receivers_table_name.toUpperCase()

    Double delta = 10
    if (input['delta']) {
        delta = input['delta'] as Double
    }

    Double h = 4
    if (input['height']) {
        h = input['height'] as Double
    }

    boolean createTriangles = false
    if(input['outputTriangleTable']) {
        createTriangles = Boolean.parseBoolean(input['outputTriangleTable'] as String)
    }

    String sources_table_name = ""
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    String building_table_name = ""
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
    }
    building_table_name = building_table_name.toUpperCase()

    // Try to find the best SRID for receivers table
    int srid = 0
    if(input['fenceTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(input['fenceTableName'] as String))
    }
    if(srid == 0 && input['buildingTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name) as String)
    }
    if (srid == 0 && input['sourcesTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name) as String)
    }

    Geometry fenceGeom = null
    if (input['fence']) {
        if (srid != 0) {
            // Transform fence to the same coordinate system than the buildings & sources
            WKTReader wktReader = new WKTReader()
            def fence = wktReader.read(input['fence'] as String)
            fenceGeom = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(fence, 4326), srid)
        } else {
            throw new Exception("Unable to find buildings or sources SRID, ignore fence parameters")
        }
    } else {
        fenceGeom = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(input['fenceTableName'] as String), "THE_GEOM")
    }

    //Delete previous receivers grid.
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))

    sql.execute("CREATE TABLE " + receivers_table_name + "(THE_GEOM GEOMETRY, ID_COL INTEGER, ID_ROW INTEGER) AS SELECT ST_SETSRID(ST_UPDATEZ(THE_GEOM, " + h + "), " + srid + ") THE_GEOM, ID_COL, ID_ROW FROM ST_MakeGridPoints(ST_GeomFromText('" + fenceGeom + "')," + delta + "," + delta + ");")
    sql.execute("ALTER TABLE " + receivers_table_name + " ADD COLUMN PK SERIAL PRIMARY KEY")

    logger.info("Create spatial index on " + receivers_table_name)
    sql.execute("Create spatial index on " + receivers_table_name + "(the_geom);")

    if (input['fence']) {
        // Delete points outside geom but inside
        sql.execute("DELETE FROM " + receivers_table_name + " WHERE NOT ST_Intersects(THE_GEOM, :geom)", ['geom': fenceGeom])
    }

    if (input['buildingTableName']) {
        logger.info("Delete receivers inside buildings")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + building_table_name + " b where ST_Z(g.the_geom) < b.HEIGHT and g.the_geom && b.the_geom and ST_INTERSECTS(g.the_geom, b.the_geom) and ST_distance(b.the_geom, g.the_geom) < 1 limit 1);")
    }
    if (input['sourcesTableName']) {
        logger.info("Delete receivers near sources")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + sources_table_name + " r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
    }
    if(createTriangles) {
        sql.execute("DROP TABLE IF EXISTS TRIANGLES")
        sql.execute("CREATE TABLE TRIANGLES(pk serial NOT NULL, the_geom geometry(POLYGON Z, "+srid+"), PK_1 integer not null," +
                " PK_2 integer not null, PK_3 integer not null, cell_id integer not null, PRIMARY KEY (PK))")
        sql.execute("INSERT INTO TRIANGLES(THE_GEOM, PK_1, PK_2, PK_3, CELL_ID) " +
                "SELECT ST_ConvexHull(ST_UNION(A.THE_GEOM, ST_UNION(B.THE_GEOM, C.THE_GEOM))) THE_GEOM, " +
                "A.PK PK_1, B.PK PK_2, C.PK PK_3, 0" +
                "  FROM "+receivers_table_name+" A, "+receivers_table_name+" B, "+receivers_table_name+" C " +
                "WHERE A.ID_ROW = B.ID_ROW + 1 AND A.ID_COL  = B.ID_COL AND " +
                "A.ID_ROW = C.ID_ROW + 1 AND A.ID_COL = C.ID_COL + 1;")
        sql.execute("INSERT INTO TRIANGLES(THE_GEOM, PK_1, PK_2, PK_3, CELL_ID) " +
                "SELECT ST_ConvexHull(ST_UNION(A.THE_GEOM, ST_UNION(B.THE_GEOM, C.THE_GEOM))) THE_GEOM, " +
                "A.PK PK_1, B.PK PK_2, C.PK PK_3, 0" +
                "  FROM "+receivers_table_name+" A, "+receivers_table_name+" B, "+receivers_table_name+" C " +
                "WHERE A.ID_ROW = B.ID_ROW + 1 AND A.ID_COL  = B.ID_COL + 1" +
                " AND A.ID_ROW = C.ID_ROW AND A.ID_COL = C.ID_COL + 1;")
    }

    return [tableNameCreated: "Process done. Table of receivers " + receivers_table_name + " created !"]
}