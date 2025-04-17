
/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.wps.Dynamic

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException

title = 'Map Difference'
description = '&#10145;&#65039; Computes the difference between two noise maps'

inputs = [
        trainsPosition : [
                name: 'Trains position table',
                title: 'Trains position table',
                description: 'Table that contains the head position (POINTZ) of the train at each timestep',
                type: String.class
        ],
        railwayGeometries: [
                name: 'Railway geometries table',
                title: 'Railway geometries table',
                description: 'Table that contains geometries of rails. Wagons will be attached to the provided linestring',
                type: String.class
        ],
        fieldTrainset: [
                name: 'Field train set',
                title: 'Field train set',
                description: 'Name of the field that identifies the train characteristics. Default train_set',
                min:0, max:1,
                type: String.class
        ],
        fieldTrainId: [
                name: 'Field train identifier',
                title: 'Field train identifier',
                description: 'Name of the field that identifies the train. Default train_id',
                min:0, max:1,
                type: String.class
        ],
        fieldTimeStep: [
                name: 'Field time step',
                title: 'Field time step',
                description: 'Name of the field that identifies the time. Default period',
                min:0, max:1,
                type: String.class
        ],
        trainTrainsetData: [
                name: 'Train composition file',
                title: 'Train composition file',
                description: 'File URL, specification of the composition of a train' +
                        ' (one train can contain one or more vehicles).' +
                        ' Example file <a href="https://github.com/Universite-Gustave-Eiffel/NoiseModelling/blob/v5.0.0/noisemodelling-emission/src/main/resources/org/noise_planet/noisemodelling/emission/railway/RailwayTrainsets.json">RailwayTrainsets.json</a>' +
                        'Default value RailwayTrainsets.json. Can be any URL',
                min:0, max:1,
                type: String.class
        ],
        trainVehicleData: [
                name: 'Vehicles characteristics file',
                title: 'Vehicles characteristics file',
                description: 'File URL, coefficients related to the characteristics of vehicles' +
                        'Example file <a href="https://github.com/Universite-Gustave-Eiffel/NoiseModelling/blob/v5.0.0/noisemodelling-emission/src/main/resources/org/noise_planet/noisemodelling/emission/railway/RailwayVehiclesCnossos.json">RailwayVehiclesCnossos.json</a>' +
                        'Default value RailwayVehiclesCnossos.json. Accepted values RailwayVehiclesCnossos.json RailwayVehiclesCnossos_2015.json RailwayVehiclesNMPB.json or any URL ',
                min:0, max:1,
                type: String.class
        ],
        trainCoefficientsData: [
                name: 'CNOSSOS coefficients file',
                title: 'CNOSSOS coefficients file',
                description: 'File URL, coefficients related to the emission of vehicles' +
                        'Example file <a href="https://github.com/Universite-Gustave-Eiffel/NoiseModelling/blob/v5.0.0/noisemodelling-emission/src/main/resources/org/noise_planet/noisemodelling/emission/railway/RailwayCnossosSNCF_2021.json">RailwayCnossosSNCF_2021.json</a>' +
                        'Default RailwayCnossosSNCF_2021.json. Accepted value RailwayCnossosSNCF_2021.json RailwayCnossosEU_2020.json or any URL',
                min:0, max:1,
                type: String.class
        ]
]

outputs = [
        result: [
                name: 'Result output string',
                title: 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type: String.class
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


def run(Map input) {

    // Get name of the database
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
@CompileStatic
def exec(Connection connection, Map input) {

    Logger logger = LoggerFactory.getLogger("DynamicTrainFromAADTTraffic")

    connection = new ConnectionWrapper(connection)

    final def railwayGeometries = input["railwayGeometries"] as String
    final def trainsPosition = input["trainsPosition"] as String

    def fieldTrainset = "train_set"
    if(input["fieldTrainset"]) {
        fieldTrainset = input["fieldTrainset"] as String
    }

    def fieldTrainId = "train_id"
    if(input["fieldTrainId"]) {
        fieldTrainId = input["fieldTrainId"] as String
    }

    def fieldTimeStep = "period"
    if(input["fieldTrainset"]) {
        fieldTimeStep = input["fieldTimeStep"] as String
    }

    String trainTrainsetData = "RailwayTrainsets.json" as String
    if(input["trainTrainsetData"]) {
        trainTrainsetData = input["trainTrainsetData"] as String
    }

    String trainVehicleData = "RailwayVehiclesCnossos.json" as String
    if(input["trainVehicleData"]) {
        trainVehicleData = input["trainVehicleData"] as String
    }

    String trainCoefficientsData = "RailwayCnossosSNCF_2021.json" as String
    if(input["trainCoefficientsData"]) {
        trainCoefficientsData = input["trainCoefficientsData"] as String
    }


    Sql sql = new Sql(connection)

    final int srid = GeometryTableUtilities.getSRID(connection, railwayGeometries) as Integer

    // check that the srid is a metric unit coordinate reference system

    def isMetric = sql.firstRow("SELECT COUNT(*) FROM SPATIAL_REF_SYS WHERE SRID=$srid" +
            " AND PROJ4TEXT LIKE '%units=m%'")[0] as Boolean

    if (!isMetric) {
        throw new SQLException("Geometry projection system of the table $railwayGeometries must be metric ! (not EPSG=$srid)")
    }

    RailwayCnossos railway = new RailwayCnossos()

    // Fetch configuration
    try {
        URL trainTrainsetDataUrl = new URL(trainTrainsetData)
        trainTrainsetDataUrl.withInputStream { InputStream stream ->
            railway.setTrainSetDataFile(stream)
        }
    } catch (MalformedURLException ignored) {
        railway.setTrainSetDataFile(trainTrainsetData)
    }
    try {
        URL trainVehicleDataUrl = new URL(trainVehicleData)
        trainVehicleDataUrl.withInputStream { InputStream stream ->
            railway.setVehicleDataFile(stream)
        }
    } catch (MalformedURLException ignored) {
        railway.setVehicleDataFile(trainVehicleData)
    }
    try {
        URL trainCoefficientsDataUrl = new URL(trainCoefficientsData)
        trainCoefficientsDataUrl.withInputStream { InputStream stream ->
            railway.setRailwayDataFile(stream)
        }
    } catch (MalformedURLException ignored) {
        railway.setRailwayDataFile(trainCoefficientsData)
    }

    final double queryCacheDistance = 500
    // keep track of train settings changes
    String previousTrainset = ""
    // keep track of train identifier (if the train change we clear the train history)
    String previousTrainId = ""
    // Keep track of the rail associated with the train
    AreaRails previousNetworkData = null
    // Precomputed source distribution settings according to current trainSet
    TrainInfo sourceDistribution = null
    // keep track of previous rails to put the wagons on the good position
    LinkedList<RailInfo> railNavigationHistory = new LinkedList<>()
    sql.eachRow("SELECT $fieldTimeStep, $fieldTrainId, $fieldTrainset, the_geom FROM $trainsPosition ORDER BY $fieldTrainId, $fieldTimeStep".toString()) {rs ->
        String trainset = rs.getString(fieldTrainset)
        Geometry trainPosition = (Geometry) rs.getObject("the_geom")
        // Read from the database then network of rails near the train position
        // Do not query again if the position is near the last position
        if(previousNetworkData == null || !previousNetworkData.queryEnvelope.contains(trainPosition.coordinate)) {
            def data = sql.rows("SELECT * FROM $railwayGeometries WHERE THE_GEOM && ST_EXPAND(:geom,:dist, :dist)".toString(), [geom: trainPosition, dist:queryCacheDistance])
            Envelope queryEnvelope = new Envelope(trainPosition.coordinate)
            queryEnvelope.expandBy(queryCacheDistance)
            previousNetworkData = new AreaRails(queryEnvelope,data)
            // Look for closest rail
            previousNetworkData.lookForClosestFeature(trainPosition.coordinate)
        }
        if(trainset != previousTrainset) {
            previousNetworkData = null
            // New train configuration
            // Precompute source distribution
            double vehicleSpeed = 160
            double vehiclePerHour = 1
            int rollingCondition = 0
            double idlingTime = 0
            int trackTransfer = 4
            int impactNoise = 0
            int bridgeTransfert = 0
            int curvature = 0
            int railRoughness = 1
            int nbTrack = 2
            double vMaxInfra = 160
            double commercialSpeed = 160
            boolean isTunnel = false
            RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                    impactNoise, bridgeTransfert, curvature, commercialSpeed, isTunnel, nbTrack)
            RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(trainset, vehicleSpeed,
                    vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);
            sourceDistribution = new TrainInfo(railway, trackParameters, vehicleParameters)
        }
    }

}

@CompileStatic
class RailInfo {
    int primaryKey
    Geometry railGeometry;

    RailInfo(int primaryKey, Geometry railGeometry) {
        this.primaryKey = primaryKey
        this.railGeometry = railGeometry
    }
}

@CompileStatic
class TrainInfo {
    RailwayCnossos railway
    RailwayTrackCnossosParameters trackParameters
    RailwayVehicleCnossosParameters vehicleParameters

    TrainInfo(RailwayCnossos railway, RailwayTrackCnossosParameters trackParameters, RailwayVehicleCnossosParameters vehicleParameters) {
        this.railway = railway
        this.trackParameters = trackParameters
        this.vehicleParameters = vehicleParameters
        // Precompute all sources positions normalized to distance from center of first engine wagon
    }
}

@CompileStatic
class AreaRails {
    Envelope queryEnvelope
    Geometry closestRailGeometry
    int closestRailRowIndex
    List<GroovyRowResult> allFeaturesInArea

    AreaRails(Envelope queryEnvelope, List<GroovyRowResult> allFeaturesInArea) {
        this.queryEnvelope = queryEnvelope
        this.closestRailGeometry = closestRailGeometry
        this.closestRailRowIndex = closestRailRowIndex
        this.allFeaturesInArea = allFeaturesInArea
    }

    void lookForClosestFeature(Coordinate position) {

    }
}

