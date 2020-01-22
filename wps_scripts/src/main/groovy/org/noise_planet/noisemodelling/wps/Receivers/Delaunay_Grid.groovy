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

inputs = [buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          fence  : [name: 'Fence', title: 'Fence', min: 0, max: 1, type: Geometry.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', type: String.class],
          maxPropDist  : [name: 'Maximum Propagation Distance', title: 'Maximum Propagation Distance', description: 'Set Maximum Propagation Distance, Avoid loading to much geometries when doing Delaunay triangulation (default = 500 m)', min: 0, max: 1, type: Double.class],
          roadWidth  : [name: 'Road Width', title: 'Road Width', description: 'Set Road Width. No receivers closer than road width distance. (default = 2m)', min: 0, max: 1, type: Double.class],
          maxArea  : [name: 'Maximum Area', title: 'Maximum Area', description: 'Set Maximum Area. No triangles larger than provided area.(default = 2500m2)', min: 0, max: 1, type: Double.class],
          sourceDensification  : [name: 'Source Densification', title: 'Source Densification', description: 'Set Source Densification. Densification of receivers near sound sources. (default = 8)', min: 0, max: 1, type: Double.class],
          height    : [name: 'Height', title: 'Height', description: ' Receiver height relative to the ground in meters (default : 4m)', min: 0, max: 1, type: Double.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : RECEIVERS)', title: 'Name of output table', min: 0, max: 1, type: String.class]]

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

    String receivers_table_name = "RECEIVERS"
    if (input['outputTableName']) {
        receivers_table_name = input['outputTableName']
    }
    receivers_table_name = receivers_table_name.toUpperCase()

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
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

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }


    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
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


    }

    return [tableNameCreated: "Process done !"]
}

