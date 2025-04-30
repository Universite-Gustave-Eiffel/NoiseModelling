
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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ContainerNode
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.Point
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
                description: 'Table that contains the head position (POINTZ) of the train, the timestep and the train identifier',
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

    // Cache distance in metres when fetching all rails nearby the train position
    final double queryCacheDistance = 500
    // keep track of train settings changes
    String previousTrainset = ""
    // keep track of train identifier (if the train change we clear the train history)
    String previousTrainId = ""
    // Precomputed source distribution settings according to current trainSet
    TrainInfo trainInfo = null
    sql.eachRow("SELECT $fieldTimeStep, $fieldTrainId, $fieldTrainset, the_geom FROM $trainsPosition ORDER BY $fieldTrainId, $fieldTimeStep".toString()) {rs ->
        String trainset = rs.getString(fieldTrainset)
        String trainId = rs.getString(fieldTrainId)
        Geometry trainPosition = (Geometry) rs.getObject("the_geom")
        if(trainset != previousTrainset || trainId != previousTrainId) {
            // New train configuration
            // Precompute source distribution
            double vehicleSpeed = 160
            double vehiclePerHour = 1
            int rollingCondition = 0
            double idlingTime = 0
            int trackTransfer = 4
            int impactNoise = 0
            int bridgeTransfer = 0
            int curvature = 0
            int railRoughness = 1
            int nbTrack = 2
            double vMaxInfra = 160
            double commercialSpeed = 160
            boolean isTunnel = false
            RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                    impactNoise, bridgeTransfer, curvature, commercialSpeed, isTunnel, nbTrack)
            RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(trainset, vehicleSpeed,
                    vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);
            trainInfo = new TrainInfo(railway, trackParameters, vehicleParameters)
            previousTrainset = trainset
            previousTrainId = trainId
        }
        // Read from the database then network of rails near the train position
        // Do not query again if the new position of the train is still on the cached query area
        if(trainInfo != null && !trainInfo.nearbyRails.queryEnvelope.contains(trainPosition.coordinate)) {
            def data = sql.rows("SELECT * FROM $railwayGeometries WHERE THE_GEOM && ST_EXPAND(:geom,:dist, :dist)".toString(), [geom: trainPosition, dist:queryCacheDistance])
            Envelope queryEnvelope = new Envelope(trainPosition.coordinate)
            queryEnvelope.expandBy(queryCacheDistance)
            trainInfo.nearbyRails.update(queryEnvelope, data)
        }
        // Distribute sources of sourceDistribution object into the rails lines
        // The direction is currently deduced from the previous position of the specific train (so no sources on the first time step)
        // it could be also provided using a field (same order or reverse) according to the orientation (order of points) of the geometry the rail ?
        trainInfo.updatePosition(trainPosition.coordinate)

    }
}


@CompileStatic
class TrainInfo {
    RailwayCnossos railway
    RailwayTrackCnossosParameters trackParameters
    RailwayVehicleCnossosParameters vehicleParameters
    List<VehicleInfo> trainComposition = new ArrayList<>()
    // Keep track of the rail associated with the train
    AreaRails nearbyRails = new AreaRails()
    // We create invalid position by default
    Coordinate trainPosition = new Coordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    LineString trainTipRail = null
    // Tip position fraction on the linestring
    double tipFraction = Double.NaN

    TrainInfo(RailwayCnossos railway, RailwayTrackCnossosParameters trackParameters, RailwayVehicleCnossosParameters vehicleParameters) {
        this.railway = railway
        this.trackParameters = trackParameters
        this.vehicleParameters = vehicleParameters
        // Precompute all sources positions normalized to distance from the tip of first vehicle
        // vehicleParameters.typeVehicle define a specific train composition (main engine - wagons etc)
        // Look for this vehicle in the JSON document
        JsonNode trainNode = railway.trainsetData.get(vehicleParameters.typeVehicle)
        if(trainNode instanceof ContainerNode) {
            // in vehicleNode we have a map with the key that define the "wagon" type and the value is the quantity (int)
            // The first element is supposed to be the engine, then other elements may not represent the real order of wagons
            double tipDistanceFromFirstVehicle = 0
            trainNode.fields().each {vehicle ->
                def vehicleIdentifier = vehicle.key as String
                def vehicleQuantity = vehicle.value.intValue()
                // Look for the wagon characteristics
                JsonNode vehicleNode = railway.vehicleData.get(vehicleIdentifier)
                if(vehicleNode instanceof ContainerNode) {
                    def maxSpeed = vehicleNode.get("Vmax").asDouble()
                    double length = vehicleNode.get("Length").asDouble()
                    int nbCoach = vehicleNode.get("NbCoach").asInt() // number of sources/wagons
                    double firstSourcePosition = vehicleNode.get("FirstSourcePosition").asDouble() // distance
                    double sourceSpacing = vehicleNode.get("SourceSpacing").asDouble() // distance between each coach
                    for(vehicleId in 0..< vehicleQuantity) {
                        // The reference point is at the tip of the first vehicle
                        // first source position is relative to the tip of the train (our the_geom coordinate)
                        double referenceDistance = trainComposition.isEmpty() ? firstSourcePosition : tipDistanceFromFirstVehicle + firstSourcePosition
                        for (coachId in 0..<nbCoach) {
                            trainComposition.add(new VehicleInfo(referenceDistance, 0.5d, 0.0d))
                            referenceDistance += sourceSpacing
                        }
                        // next vehicle tip position will be at this new location (we add the full vehicle length)
                        tipDistanceFromFirstVehicle += length
                    }
                } else {
                    def allVehicles = railway.vehicleData.fieldNames().collect {it}.join(", ")
                    throw new IllegalArgumentException("Vehicle identifier is set as '$vehicleIdentifier' but such" +
                            " vehicle is not defined (possible values $allVehicles)")
                }
            }
        } else {
            def allTrains = railway.trainsetData.fieldNames().collect {it}.join(", ")
            throw new IllegalArgumentException("Train identifier is set as '$vehicleParameters.typeVehicle' but such" +
                    " train composition is not defined (possible values $allTrains)")
        }
    }

    void updatePosition(Coordinate newPosition) {
        if(trainPosition.isValid() && !trainComposition.isEmpty()) {
            // We can compute the direction of the train
            Coordinate vehiclePosition = newPosition
            if(trainTipRail == null) {
                // we need to find the closest rail
                trainTipRail = nearbyRails.findClosestFeature(vehiclePosition)
                if(trainTipRail != null) {
                    double frontRailTotalLength = trainTipRail.length
                    tipFraction = LineStringUtils.getLineStringLengthFraction(trainTipRail, vehiclePosition)
                    // Initialize the first vehicle
                    if(!trainComposition.isEmpty()) {
                        // found out the direction of the train
                        double previousFraction = LineStringUtils.getLineStringLengthFraction(trainTipRail, trainPosition)
                        VehicleInfo firstVehicle = trainComposition.first()
                        double firstVehicleFraction = tipFraction
                        if(previousFraction < tipFraction) {
                            // the train is moving following the same order than the position of the
                            // points in the linestring
                            firstVehicleFraction -= firstVehicle.distanceFromTheGeom / frontRailTotalLength
                        } else {
                            // the train is moving following the inverse order than the position of the
                            // points in the linestring
                            firstVehicleFraction += firstVehicle.distanceFromTheGeom / frontRailTotalLength
                        }
                        firstVehicle.sourceCoordinate = LineStringUtils.getPositionFromFraction(trainTipRail, firstVehicleFraction)
                        firstVehicle.currentRail = trainTipRail
                    }
                }
            }
            VehicleInfo forwardVehicle = trainComposition.first()
            trainComposition.forEach { vehicleInfo ->
                if(vehicleInfo.currentRail == null) {
                    // we need to find the closest rail
                    vehicleInfo.currentRail = nearbyRails.findClosestFeature(vehiclePosition)

                }
                forwardVehicle = vehicleInfo
            }
        }
        trainPosition = newPosition
    }
}


@CompileStatic
class LineStringUtils {
    /**
     * Find the fraction [0,1] where the closest perpendicular position of the provided coordinate is
     * @param ls
     * @param coordinate
     * @return fraction [0,1] of the total length of the linestring following order of points
     */
    static double getLineStringLengthFraction(LineString ls, Coordinate coordinate) {
        LineSegment seg = new LineSegment()
        CoordinateSequence cs = ls.getCoordinateSequence()
        double closestDistanceLength = Double.NaN
        double minimumDistance = Double.MAX_VALUE
        double cumulatedDistance = 0
        for(pointId in 0..< cs.size() - 1) {
            cs.getCoordinate(pointId, seg.p0)
            cs.getCoordinate(pointId + 1, seg.p1)
            double distance = seg.distance(coordinate)
            double segmentLength = seg.length
            if(distance < minimumDistance) {
                closestDistanceLength = cumulatedDistance + seg.segmentFraction(coordinate) * segmentLength
                minimumDistance = distance
            }
            cumulatedDistance += segmentLength
        }
        return closestDistanceLength / cumulatedDistance
    }

    /**
     * Get the coordinate from the fraction of the length of the linestring
     * @param ls
     * @param lineStringFraction
     * @return
     */
    static Coordinate getPositionFromFraction(LineString ls, double lineStringFraction) {
        double fullLength = ls.length
        double targetDistance = fullLength * lineStringFraction
        LineSegment seg = new LineSegment()
        CoordinateSequence cs = ls.getCoordinateSequence()
        double cumulatedDistance = 0
        for(pointId in 0..< cs.size() - 1) {
            cs.getCoordinate(pointId, seg.p0)
            cs.getCoordinate(pointId + 1, seg.p1)
            double segmentLength = seg.length
            if(cumulatedDistance + segmentLength > targetDistance) {
                // the fraction is on this segment
                return seg.pointAlong((targetDistance-cumulatedDistance)/segmentLength)
            }
            cumulatedDistance += segmentLength
        }
        return cs.getCoordinate(cs.size() - 1)
    }
}

@CompileStatic
class VehicleInfo {
    double distanceFromTheGeom = 0 // source distance from the reference point
    double height = 0.5 // source height
    double lateralOffset = 0 // source lateral distance from the center of the train

    Coordinate sourceCoordinate = new Coordinate()
    LineString currentRail = null

    VehicleInfo() {
    }

    VehicleInfo(double distanceFromTheGeom, double height, double lateralOffset) {
        this.distanceFromTheGeom = distanceFromTheGeom
        this.height = height
        this.lateralOffset = lateralOffset
    }
}

/**
 * Cache for spatial query on rails geometries
 */
@CompileStatic
class AreaRails {
    Envelope queryEnvelope
    List<GroovyRowResult> allFeaturesInArea
    GeometryFactory gf = new GeometryFactory()

    AreaRails() {
        queryEnvelope = new Envelope()
        allFeaturesInArea = new ArrayList<>()
    }

    void update(Envelope queryEnvelope, List<GroovyRowResult> allFeaturesInArea) {
        this.queryEnvelope = queryEnvelope
        this.allFeaturesInArea = allFeaturesInArea
    }

    /**
     * Read cached linestrings and find the closest linestring to the provided coordinate
     * @param coordinate Coordinate (same projection than rails)
     * @return The closest linestring
     */
    LineString findClosestFeature(Coordinate coordinate) {
        Point pt = gf.createPoint(coordinate)
        double minDistance = Double.MAX_VALUE
        LineString minDistanceGeometry = null
        allFeaturesInArea.forEach {
            Object geomObj = it.get("THE_GEOM")
            if(geomObj instanceof LineString) {
                LineString ls = (LineString)geomObj
                double distance = ls.distance(pt)
                if(distance < minDistance) {
                    minDistance = distance
                    minDistanceGeometry = ls
                }
            } else if(geomObj instanceof MultiLineString) {
                MultiLineString mls = (MultiLineString) geomObj;
                for(int geomId in 0..< mls.getNumGeometries()) {
                    LineString ls = (LineString)mls.getGeometryN(geomId)
                    double distance = ls.distance(pt)
                    if(distance < minDistance) {
                        minDistance = distance
                        minDistanceGeometry = ls
                    }
                }
            }
        }

        return minDistanceGeometry
    }

}

