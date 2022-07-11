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
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader

import org.noise_planet.noisemodelling.emission.*
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.jdbc.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger

title = 'Delaunay Grid'
description = 'Calculates a delaunay grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [
        tableBuilding      : [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: '<b>Name of the Buildings table.</b>  </br>  ' +
                        '<br>  The table shall contain : </br>' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>',
                type       : String.class
        ],
        fence              : [
                name       : 'Fence geometry', title: 'Extent filter',
                description: 'Create receivers only in the provided polygon',
                min        : 0, max: 1,
                type       : Geometry.class
        ],
        sourcesTableName   : [
                name       : 'Sources table name',
                title      : 'Sources table name',
                description: 'Road table, receivers will not be created on the specified road width',
                type       : String.class
        ],
        maxPropDist        : [
                name       : 'Maximum Propagation Distance',
                title      : 'Maximum Propagation Distance',
                description: 'Set Maximum propagation distance in meters. Avoid loading to much geometries when doing Delaunay triangulation. (FLOAT)' +
                        '</br> </br> <b> Default value : 500 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        roadWidth          : [
                name       : 'Source Width',
                title      : 'Source Width',
                description: 'Set Road Width in meters. No receivers closer than road width distance.(FLOAT) ' +
                        '</br> </br> <b> Default value : 2 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        maxArea            : [
                name       : 'Maximum Area',
                title      : 'Maximum Area',
                description: 'Set Maximum Area in m2. No triangles larger than provided area. Smaller area will create more receivers. (FLOAT)' +
                        '</br> </br> <b> Default value : 2500 </b> ',
                min        : 0, max: 1,
                type       : Double.class
        ],
        height             : [
                name       : 'Height',
                title      : 'Height',
                description: ' Receiver height relative to the ground in meters (FLOAT).' +
                        '</br> </br> <b> Default value : 4 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        outputTableName    : [
                name       : 'outputTableName',
                description: 'Do not write the name of a table that contains a space. ' +
                        '</br> </br> <b> Default value : RECEIVERS </b>',
                title      : 'Name of output table',
                min        : 0, max: 1,
                type       : String.class
        ],
        isoSurfaceInBuildings: [
                name: 'Create IsoSurfaces over buildings',
                title: 'Create IsoSurfaces over buildings',
                description: 'If enabled isosurfaces will be visible at the location of buildings',
                type: Boolean.class,
                min        : 0, max: 1,
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


def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Delaunay grid')
    logger.info("inputs {}", input) // log inputs of the run


    String receivers_table_name = "RECEIVERS"
    if (input['outputTableName']) {
        receivers_table_name = input['outputTableName']
    }
    receivers_table_name = receivers_table_name.toUpperCase()

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    } else {
        return "Source table must be specified"
    }
    sources_table_name = sources_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['tableBuilding']) {
        building_table_name = input['tableBuilding']
    }
    building_table_name = building_table_name.toUpperCase()

    boolean isoSurfaceInBuildings = false;
    if(input['isoSurfaceInBuildings)']) {
        isoSurfaceInBuildings = input['isoSurfaceInBuildings'] as Boolean
    }


    Double maxPropDist = 600.0
    if (input['maxPropDist']) {
        maxPropDist = input['maxPropDist'] as Double
    }

    Double height = 4.0
    if (input['height']) {
        height = input['height'] as Double
    }

    Double roadWidth = 2.0
    if (input['roadWidth']) {
        roadWidth = input['roadWidth'] as Double
    }

    Double maxArea = 2500
    if (input.containsKey('maxArea')) {
        maxArea = input['maxArea'] as Double
    }

    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))

    Geometry fence = null
    WKTReader wktReader = new WKTReader()
    if (input['fence']) {
        fence = wktReader.read(input['fence'] as String)
    }

    //Statement sql = connection.createStatement()
    Sql sql = new Sql(connection)
    connection = new ConnectionWrapper(connection)
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1)

    // Delete previous receivers grid
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
    sql.execute("DROP TABLE IF EXISTS TRIANGLES")

    // Generate receivers grid for noise map rendering
    TriangleNoiseMap noiseMap = new TriangleNoiseMap(building_table_name, sources_table_name)

    if (fence != null) {
        // Reproject fence
        int targetSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
        if (targetSrid == 0) {
            targetSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
        }
        if (targetSrid != 0) {
            // Transform fence to the same coordinate system than the buildings & sources
            fence = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(fence, 4326), targetSrid)
            noiseMap.setMainEnvelope(fence.getEnvelopeInternal())
        } else {
            System.err.println("Unable to find buildings or sources SRID, ignore fence parameters")
        }
    }


    // Avoid loading to much geometries when doing Delaunay triangulation
    noiseMap.setMaximumPropagationDistance(maxPropDist)
    // Receiver height relative to the ground
    noiseMap.setReceiverHeight(height)
    // No receivers closer than road width distance
    noiseMap.setRoadWidth(roadWidth)
    // No triangles larger than provided area
    noiseMap.setMaximumArea(maxArea)

    noiseMap.setIsoSurfaceInBuildings(isoSurfaceInBuildings)

    logger.info("Delaunay initialize")
    noiseMap.initialize(connection, new EmptyProgressVisitor())

    if(input['errorDumpFolder']) {
        // Will write the input mesh in this folder in order to
        // help debugging delaunay triangulation
        noiseMap.setExceptionDumpFolder(input['errorDumpFolder'] as String)
    }

    AtomicInteger pk = new AtomicInteger(0)
    ProgressVisitor progressVisitorNM = progressLogger.subProcess(noiseMap.getGridDim() * noiseMap.getGridDim())

    try {
        for (int i = 0; i < noiseMap.getGridDim(); i++) {
            for (int j = 0; j < noiseMap.getGridDim(); j++) {
                logger.info("Compute cell " + (i * noiseMap.getGridDim() + j + 1) + " of " + noiseMap.getGridDim() * noiseMap.getGridDim())
                noiseMap.generateReceivers(connection, i, j, receivers_table_name, "TRIANGLES", pk)
                progressVisitorNM.endStep()
            }
        }
    } catch (LayerDelaunayError ex) {
        logger.error("Got an error use the errorDumpFolder parameter with a folder path in order to save the " +
                "input geometries for debugging purpose")
        throw ex
    }

    logger.info("Create spatial index on "+receivers_table_name+" table")
    sql.execute("Create spatial index on " + receivers_table_name + "(the_geom);")

    int nbReceivers = sql.firstRow("SELECT COUNT(*) FROM " + receivers_table_name)[0] as Integer

    // Process Done
    resultString = "Process done. " + receivers_table_name + " (" + nbReceivers + " receivers) and TRIANGLES tables created. "

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Delaunay grid')

    // print to WPS Builder
    return resultString

}

