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
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Calculates a regular grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [
        buildingTableName : [
                name : 'Buildings table name',
                title: 'Buildings table name',
                description: 'Buildings table, receivers that are into this tables geometries will be deleted',
                min        : 0, max: 1,
                type : String.class
        ],
        fence             : [
                name       : 'Fence geometry',
                title      : 'Extent filter',
                description: 'Create receivers only in the provided polygon',
                min        : 0, max: 1,
                type       : Geometry.class
        ],
        fenceTableName    : [
                name       : 'Fence geometry from table',
                title      : 'Filter using table bounding box',
                description: 'Extract the bounding box of the specified table then create only receivers on the table bounding box' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : any geometry type. </br>',
                min        : 0, max: 1,
                type       : String.class
        ],
        sourcesTableName  : [
                name       : 'Sources table name',
                title      : 'Sources table name',
                description: 'Keep only receivers at least at 1 meters of provided sources geometries' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : any geometry type. </br>',
                min        : 0, max: 1,
                type       : String.class
        ],
        delta             : [
                name       : 'offset',
                title      : 'offset',
                description: 'Offset in the Cartesian plane in meters',
                type       : Double.class
        ],
        receiverstablename: [
                name       : 'receiverstablename',
                description: 'Do not write the name of a table that contains a space. (default : RECEIVERS)',
                title      : 'Name of receivers table',
                min        : 0, max: 1,
                type       : String.class
        ],
        height            : [
                name       : 'height',
                title      : 'height',
                description: 'Height of receivers in meters',
                min        : 0, max: 1,
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



def exec(connection, input) {

    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null


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
    if(input['buildingTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    }

    if (srid == 0 && input['sourcesTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name) as String)
    }

    if (srid == 0 && input['fenceTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(input['fenceTableName'] as String))
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
    } else if (input['fenceTableName']) {
        fenceGeom = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(input['fenceTableName'] as String), "THE_GEOM")
    } else {
        fenceGeom = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(building_table_name), "THE_GEOM")
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

