/**
 * @Author Aumond Pierre, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry

import groovy.sql.Sql

import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.locationtech.jts.io.WKTReader
import org.noise_planet.noisemodelling.propagation.jdbc.TriangleNoiseMap
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor

import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger
import org.h2gis.utilities.wrapper.*


title = 'Delaunay Grid'
description = 'Calculates a delaunay grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [tableBuilding : [name: 'Buildings table name', title: 'Buildings table name',
                               description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                       '<br>  The table shall contain : </br>' +
                                       '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>',
                               type: String.class],
          fence  : [name: 'Fence', title: 'Receiver position filter', description: 'Receivers will only be created inside the specified area', min: 0, max: 1, type: Geometry.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name',description: 'Road table, receivers will not be created on the specified road width', type: String.class],
          maxPropDist  : [name: 'Maximum Propagation Distance', title: 'Maximum Propagation Distance', description: 'Set Maximum Propagation Distance, Avoid loading to much geometries when doing Delaunay triangulation (default = <b>500</b> m)', min: 0, max: 1, type: Double.class],
          roadWidth  : [name: 'Road Width', title: 'Source Width', description: 'Set Road Width. No receivers closer than road width distance. (default = <b>2</b> m)', min: 0, max: 1, type: Double.class],
          maxArea  : [name: 'Maximum Area', title: 'Maximum Area', description: 'Set Maximum Area. No triangles larger than provided area.(default = <b>2500</b>m2) Smaller area will create more receivers', min: 0, max: 1, type: Double.class],
          sourceDensification  : [name: 'Source Densification', title: 'Source Densification', description: 'Set additional receivers near sound sources (roads). (default = 8)', min: 0, max: 1, type: Double.class],
          height    : [name: 'Height', title: 'Height', description: ' Receiver height relative to the ground in meters (default : <b>4</b>m)', min: 0, max: 1, type: Double.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : <b>RECEIVERS</b>)', title: 'Name of output table', min: 0, max: 1, type: String.class]]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]


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


    Double maxPropDist = 500.0
    if (input['maxPropDist']) {
        maxPropDist = input['maxPropDist']
    }

    Double height = 4.0
    if (input['height']) {
        height = input['height']
    }

    Double roadWidth = 2.0
    if (input['roadWidth']) {
        roadWidth = input['roadWidth']
    }

    Double maxArea = 2500
    if (input['maxArea']) {
        maxArea = input['maxArea']
    }

    Double sourceDensification = 8
    if (input['sourceDensification']) {
        sourceDensification = input['sourceDensification']
    }

    Geometry fence = null
    WKTReader wktReader = new WKTReader()
    if (input['fence']) {
        fence = wktReader.read(input['fence'] as String)
    }

    //Statement sql = connection.createStatement()
    Sql sql = new Sql(connection)
    connection = new ConnectionWrapper(connection)
    RootProgressVisitor progressLogger = new RootProgressVisitor(2, true, 1)

    // Delete previous receivers grid
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
    sql.execute("DROP TABLE IF EXISTS TRIANGLES")

    // Generate receivers grid for noise map rendering
    TriangleNoiseMap noiseMap = new TriangleNoiseMap(building_table_name, sources_table_name)

    if (fence != null) {
        // Reproject fence
        int targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(building_table_name))
        if(targetSrid == 0) {
            targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
        }
        if(targetSrid != 0) {
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
    // Densification of receivers near sound sources
    noiseMap.setSourceDensification(sourceDensification)

    noiseMap.initialize(connection, new EmptyProgressVisitor())
    AtomicInteger pk = new AtomicInteger(0)
    ProgressVisitor progressVisitorNM = progressLogger.subProcess(noiseMap.getGridDim() * noiseMap.getGridDim())

    for (int i = 0; i < noiseMap.getGridDim(); i++) {
        for (int j = 0; j < noiseMap.getGridDim(); j++) {
            noiseMap.generateReceivers(connection, i, j, receivers_table_name, "TRIANGLES", pk)
            progressVisitorNM.endStep()
        }
    }


    sql.execute("Create spatial index on "+receivers_table_name+"(the_geom);")

    return "Table TRIANGLES and " + receivers_table_name + " created"
}

