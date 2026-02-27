
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

/**
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Author Adrien Le Bellec, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Dynamic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ContainerNode
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.BatchingPreparedStatementWrapper
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
import org.locationtech.jts.triangulate.quadedge.Vertex
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters

import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayCnossosvar;
import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayVehicleCnossosParametersvar;


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
        /*sourceRelativePosition: [
                name: 'Train source Position file',
                title: 'Train source Position file',
                description: 'File URL, specification of the train source position',
                min:0, max:1,
                type: String.class
        ],*/
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

    String sourceRelativePosition = "RailwaySourcePosition.json" as String
    if(input["sourceRelativePosition"]) {
        sourceRelativePosition = input["sourceRelativePosition"] as String
    }

    String trainTrainsetData = "RailwayTrainsets.json" as String
    if(input["trainTrainsetData"]) {
        trainTrainsetData = input["trainTrainsetData"] as String
    }

    String trainVehicleData = "RailwayVehiclesCnossosSNCF_2022.json" as String
    if(input["trainVehicleData"]) {
        trainVehicleData = input["trainVehicleData"] as String
    }

    String trainCoefficientsData = "RailwayCnossosSNCF_2022.json" as String
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
/*    try {
        URL trainTrainsetDataUrl = new URL(trainTrainsetData)
        trainTrainsetDataUrl.withInputStream { InputStream stream ->
            railway.setTrainSetDataFile("RailwayTrainsets.json")
        }
    } catch (MalformedURLException ignored) {
        railway.setTrainSetDataFile("RailwayTrainsets.json")
    }
    try {
        URL trainVehicleDataUrl = new URL(trainVehicleData)
        trainVehicleDataUrl.withInputStream { InputStream stream ->
            railway.setVehicleDataFile(stream as String)
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
    }*/

    railway.setTrainSetDataFile(trainTrainsetData)
    railway.setVehicleDataFile(trainVehicleData)
    railway.setRailwayDataFile(trainCoefficientsData)

    // Cache distance in metres when fetching all rails nearby the train position
    final double queryCacheDistance = 500
    // keep track of train settings changes
    String previousTrainset = ""
    // keep track of train identifier (if the train change we clear the train history)
    String previousTrainId = ""
    // Precomputed source distribution settings according to current trainSet
    TrainInfo trainInfo = null
    int BATCH_SIZE = 500
    // Currently all sound sources are duplicated at each time step
    // it would be efficient to merge the same kind of source source generated at the same location (with an appropriate snap distance)
    int sourceCounter = 1

    def bands =  ["HZ50", "HZ63", "HZ80", "HZ100", "HZ125", "HZ160", "HZ200", "HZ250", "HZ315", "HZ400", "HZ500",
                  "HZ630", "HZ800", "HZ1000", "HZ1250", "HZ1600", "HZ2000", "HZ2500", "HZ3150", "HZ4000", "HZ5000",
                  "HZ6300", "HZ8000", "HZ10000"]
    sql.execute("DROP TABLE IF EXISTS SOURCES_GEOM")
    sql.execute("DROP TABLE IF EXISTS SOURCES_EMISSION")
    sql.execute("CREATE TABLE SOURCES_GEOM(IDSOURCE integer primary key,timestep long, THE_GEOM GEOMETRY(POINTZ, $srid), DIR_ID integer, YAW real, PITCH real, ROLL real)".toString())
    sql.execute("CREATE TABLE SOURCES_EMISSION(IDSOURCE integer, PERIOD VARCHAR, DIR_ID integer, ${bands.collect(){it+" real"}.join(", ")})".toString())

    sql.withBatch(BATCH_SIZE, "INSERT INTO SOURCES_GEOM(IDSOURCE,TIMESTEP,THE_GEOM, DIR_ID, YAW, PITCH, ROLL) VALUES (?, ?, ?, ?, ?, ?, 0)") { BatchingPreparedStatementWrapper sourceGeomBatch ->
        sql.withBatch(BATCH_SIZE, "INSERT INTO SOURCES_EMISSION(IDSOURCE, PERIOD, DIR_ID,${bands.join(", ")}) VALUES (?, ?, ?," +
                " ${(["?"] * bands.size()).join(", ")})") { BatchingPreparedStatementWrapper sourcePowerBatch ->
            sql.eachRow('SELECT v.THE_GEOM, v.TRAIN_ID, v.TIMESTEP, v.TRAIN_SET, v.SPEED, r.NTRACK, r.TRACKTRANS, r.RAILROUGHN, r.IMPACTNOIS, r.CURVATURE, r.BRIDGETRAN FROM ' + trainsPosition + ' v INNER JOIN ' + railwayGeometries + ' r ON v.IDSECTION = r.IDSECTION;') { rs ->

                String trainset = rs.getString(fieldTrainset)
                String trainId = rs.getString(fieldTrainId)
                long timeStep = rs.getLong(fieldTimeStep)
                Geometry trainPosition = (Geometry) rs.getObject("the_geom")
                if (trainset != previousTrainset || trainId != previousTrainId) {
                    // New train configuration
                    // Precompute source distribution
                    double trainSpeed = rs.getDouble("SPEED")
                    int trackTransfer = rs.getInt("TRACKTRANS")
                    int impactNoise = rs.getInt("IMPACTNOIS")
                    int bridgeTransfer = rs.getInt("BRIDGETRAN")
                    int curvature = rs.getInt("CURVATURE")
                    int railRoughness = rs.getInt("RAILROUGHN")
                    int nbTrack = rs.getInt("NTRACK")
                    double vMaxInfra = trainSpeed
                    double commercialSpeed = trainSpeed
                    boolean isTunnel = false
                    RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                            impactNoise, bridgeTransfer, curvature, commercialSpeed, isTunnel, nbTrack)
                    RailwayVehicleCnossosParametersvar vehicleParameters = new RailwayVehicleCnossosParametersvar(trainset, trainSpeed, 0, 0);

                    trainInfo = new TrainInfo(railway, trackParameters, vehicleParameters, trainSpeed)
                    previousTrainset = trainset
                    previousTrainId = trainId
                }
                // Read from the database then network of rails near the train position
                // Do not query again if the new position of the train is still on the cached query area
                if (trainInfo != null && !trainInfo.nearbyRails.queryEnvelope.contains(trainPosition.coordinate)) {
                    def data = sql.rows("SELECT * FROM $railwayGeometries WHERE THE_GEOM && ST_EXPAND(:geom,:dist, :dist)".toString(), [geom: trainPosition, dist: queryCacheDistance])
                    Envelope queryEnvelope = new Envelope(trainPosition.coordinate)
                    queryEnvelope.expandBy(queryCacheDistance)
                    trainInfo.nearbyRails.update(queryEnvelope, data)
                }
                // Distribute sources of sourceDistribution object into the rails lines
                // The direction is currently deduced from the previous position of the specific train (so no sources on the first time step)
                // it could be also provided using a field (same order or reverse) according to the orientation (order of points) of the geometry the rail ?
                trainInfo.updatePosition(trainPosition.coordinate)

                if(trainInfo.trainTipRail != null) {
                    for(VehicleInfo vehicleInfo in trainInfo.trainComposition) {
                        // Insert the source emission into the output table
                        // Insert the source geometry and directivity into the output table
                        for (int directivityId = 1; directivityId <= 6; ++directivityId){
                            int idSource = sourceCounter++
                            def sourcePower = [idSource, timeStep.toString(), directivityId] as List<Object>
                            double[][] lWRailWay = [vehicleInfo.rolling , vehicleInfo.tractionA,vehicleInfo.tractionB , vehicleInfo.aerodynamicA,vehicleInfo.aerodynamicB , vehicleInfo.bridge ]
                            sourcePower = sourcePower + (lWRailWay[directivityId-1] as List<Object>)

                            //TODO edit if data extact position
                            //double[] positionRelative = getRelativePosition(directivityId,null, vehicleInfo.source.yaw)
                            Coordinate SourcePosition = new Coordinate(0,0,0)

                            SourcePosition.setX(vehicleInfo.source.position.x)
                            SourcePosition.setY(vehicleInfo.source.position.y)

                            if (directivityId==3||directivityId==5){
                                double directivityH = 4;
                                SourcePosition.setZ(vehicleInfo.source.position.z+directivityH)
                            } else {
                                double directivityH = 0.5;
                                SourcePosition.setZ(vehicleInfo.source.position.z+directivityH)
                            }
                            Point point = trainPosition.getFactory().createPoint(SourcePosition)

                            point.setSRID(srid)
                            sourcePowerBatch.addBatch(sourcePower)
                            def sourceGeom = [idSource, timeStep, point, directivityId, vehicleInfo.source.yaw, vehicleInfo.source.pitch] as List<Object>
                            sourceGeomBatch.addBatch(sourceGeom)
                        }
                    }
                }
            }
        }
    }
}

/*    def getRelativePosition(int directivityId, positionSource, double yaw) throws SQLException {
        double heightDirectity
        double[] positionRelative = new double[3]
        if (positionSource != null){
            positionRelative[0]=positionSource[0]/Math.sin(yaw)
            positionRelative[1]=positionSource[1]/Math.sin(yaw)
            positionRelative[2]=positionSource[2]
        }else{
            if (directivityId==2||directivityId==4){
                heightDirectity = 4
            }else{
                heightDirectity = 0.5
            }
            positionRelative[0]=0
            positionRelative[1]=0
            positionRelative[2]=heightDirectity
        }
        return positionRelative
    }
*/

@CompileStatic
class TrainInfo {
    static final double LOOK_FOR_CLOSEST_RAIL = 1.0 // will look for another closest rail if we are at least from this distance of the old rail
    RailwayCnossos railway
    RailwayTrackCnossosParameters trackParameters
    RailwayVehicleCnossosParametersvar vehicleParameters
    List<VehicleInfo> trainComposition = new ArrayList<>()
    // Keep track of the rail associated with the train
    AreaRails nearbyRails = new AreaRails()
    // We create invalid position by default
    Coordinate trainPosition = new Coordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    LineString trainTipRail = null

    TrainInfo(RailwayCnossos railway, RailwayTrackCnossosParameters trackParameters, RailwayVehicleCnossosParametersvar vehicleParameters,double trainSpeed) {
        this.railway = railway
        this.trackParameters = trackParameters
        this.vehicleParameters = vehicleParameters

        RailwayCnossosvar railwayCnossosvar = new RailwayCnossosvar();
        RailWayCnossosParameters  trainSourceLevel = new RailWayCnossosParameters();


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

                RailwayVehicleCnossosParametersvar vehicleParametersIdentifier = new RailwayVehicleCnossosParametersvar(vehicleIdentifier, trainSpeed, 0, 0);

                railwayCnossosvar.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
                railwayCnossosvar.setTrainSetDataFile("RailwayTrainsets.json");
                railwayCnossosvar.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
                trainSourceLevel = railwayCnossosvar.evaluate(vehicleParametersIdentifier, trackParameters);

                double[] ROLLING = getSourceLevel("ROLLING", trainSourceLevel);
                double[] TRACTIONA = getSourceLevel("TRACTIONA", trainSourceLevel);
                double[] TRACTIONB = getSourceLevel("TRACTIONB", trainSourceLevel);
                double[] AERODYNAMICA = getSourceLevel("AERODYNAMICA", trainSourceLevel);
                double[] AERODYNAMICB = getSourceLevel("AERODYNAMICB", trainSourceLevel);
                double[] BRIDGE = getSourceLevel("BRIDGE", trainSourceLevel);

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
                            trainComposition.add(new VehicleInfo(referenceDistance, 0.0d, 0.0d, ROLLING,TRACTIONA,TRACTIONB,AERODYNAMICA,AERODYNAMICB,BRIDGE))
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
            // The first wagon cannot rely on the next wagon (yes)
            // so the processing is separate for this one:
            if (trainTipRail == null || trainTipRail.distance(trainTipRail.getFactory().createPoint(vehiclePosition)) > LOOK_FOR_CLOSEST_RAIL) {
                // we need to find the closest rail
                trainTipRail = nearbyRails.findClosestFeature(vehiclePosition)
            }
            if (trainTipRail != null) {
                double frontRailTotalLength = trainTipRail.length
                double tipFraction = LineStringUtils.getLineStringLengthFraction(trainTipRail, vehiclePosition)
                // Initialize the first vehicle
                if (!trainComposition.isEmpty()) {
                    // found out the direction of the train
                    double previousFraction = LineStringUtils.getLineStringLengthFraction(trainTipRail, trainPosition)
                    VehicleInfo firstVehicle = trainComposition.first()
                    firstVehicle.locationFractionOnCurrentRail = tipFraction
                    if (previousFraction < tipFraction) {
                        // the train is moving following the same order than the position of the
                        // points in the linestring
                        firstVehicle.locationFractionOnCurrentRail -= firstVehicle.distanceFromTheGeom / frontRailTotalLength
                        firstVehicle.followDirectionRailGeometry = true
                    } else {
                        // the train is moving following the inverse order than the position of the
                        // points in the linestring
                        firstVehicle.locationFractionOnCurrentRail += firstVehicle.distanceFromTheGeom / frontRailTotalLength
                        firstVehicle.followDirectionRailGeometry = false
                    }
                    firstVehicle.source = LineStringUtils.getPositionFromFraction(trainTipRail,
                            firstVehicle.locationFractionOnCurrentRail)
                    firstVehicle.setCurrentRail(trainTipRail)
                }
            }
            VehicleInfo forwardVehicle = trainComposition.first()
            for(VehicleInfo vehicleInfo in trainComposition.subList(1, trainComposition.size())) {
                // compute the new fraction distance from the forward wagon data
                double distanceOnRailFromNextWagon = vehicleInfo.distanceFromTheGeom - forwardVehicle.distanceFromTheGeom
                double nextPositionOnRail = forwardVehicle.locationFractionOnCurrentRail *
                        forwardVehicle.currentRailLength + distanceOnRailFromNextWagon * (forwardVehicle.followDirectionRailGeometry ? -1 : 1)
                if(nextPositionOnRail < 0 || nextPositionOnRail > forwardVehicle.currentRailLength) {
                    // the source is not the same rail than the next wagon
                    // we can still project the source position using the closest line segment
                    // so we could fetch the nearest rail
                    double fractionOnLineString = nextPositionOnRail / forwardVehicle.currentRailLength
                    PositionAndOrientation detachedSourcePosition = LineStringUtils.getPositionFromFraction(forwardVehicle.getCurrentRail(),
                            fractionOnLineString)
                    LineString closestRail = nearbyRails.findClosestFeature(detachedSourcePosition.position)
                    vehicleInfo.setCurrentRail(closestRail)
                    if(closestRail != null && closestRail == vehicleInfo.currentRail) {
                        // There is no rails where the wagon is located..
                        // maybe there is an hole in the network, we still place the wagon on the map by extrapolating using the line segment
                        vehicleInfo.followDirectionRailGeometry = forwardVehicle.followDirectionRailGeometry
                        vehicleInfo.locationFractionOnCurrentRail = fractionOnLineString
                    } else {
                        // we found another rail closest than the one of the next wagon
                        vehicleInfo.locationFractionOnCurrentRail = LineStringUtils.getLineStringLengthFraction(vehicleInfo.getCurrentRail(), detachedSourcePosition.position)
                        // by using the next wagon coordinate we fix the source direction relative to the new geometry
                        double nextWagonFraction = LineStringUtils.getLineStringLengthFraction(vehicleInfo.getCurrentRail(), forwardVehicle.source.position)
                        vehicleInfo.followDirectionRailGeometry = vehicleInfo.locationFractionOnCurrentRail < nextWagonFraction
                    }
                } else {
                    // still on the same rail that the forward wagon
                    vehicleInfo.followDirectionRailGeometry = forwardVehicle.followDirectionRailGeometry
                    vehicleInfo.locationFractionOnCurrentRail = nextPositionOnRail / forwardVehicle.currentRailLength
                    if(vehicleInfo.getCurrentRail() != forwardVehicle.getCurrentRail()) {
                        vehicleInfo.setCurrentRail(forwardVehicle.getCurrentRail())
                    }
                }
                // compute new source coordinate
                vehicleInfo.source = LineStringUtils.getPositionFromFraction(vehicleInfo.getCurrentRail(),
                        vehicleInfo.locationFractionOnCurrentRail)
                if(!vehicleInfo.followDirectionRailGeometry) {
                    vehicleInfo.source.reverse()
                }
                forwardVehicle = vehicleInfo
            }
        }
        trainPosition = newPosition
    }

    double[] getSourceLevel(String sourceType,RailWayCnossosParameters resultatslWRailWay){
        double[] lWextract =resultatslWRailWay.getRailwaySourceList().get(sourceType).getlW();
        return lWextract
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
    static PositionAndOrientation getPositionFromFraction(LineString ls, double lineStringFraction) {
        double fullLength = ls.length
        double targetDistance = fullLength * lineStringFraction
        LineSegment seg = new LineSegment()
        CoordinateSequence cs = ls.getCoordinateSequence()
        double cumulatedDistance = 0
        for(pointId in 0..< cs.size() - 1) {
            cs.getCoordinate(pointId, seg.p0)
            cs.getCoordinate(pointId + 1, seg.p1)
            double segmentLength = seg.length
            // If the target distance is before or after the points, extrapolate the value using the first or last segment
            if(cumulatedDistance + segmentLength > targetDistance || pointId + 1 == cs.size() - 1) {
                // the fraction is on this segment
                Coordinate positionOnSegment = seg.pointAlong((targetDistance-cumulatedDistance)/segmentLength);
                positionOnSegment.z = Vertex.interpolateZ(positionOnSegment, seg.p0, seg.p1)
                // YAW is clockwise direction (geographic orientation)
                // Source horizontal orientation in degrees. For points 0° North, 90° East
                // Values 0-360
                final double yaw = (Math.toDegrees(-seg.angle() + Math.PI / 2.0) + 360) % 360
                // Source vertical orientation in degrees. 0° front 90° Top 270° Bottom
                final double pitch = (Math.toDegrees(Math.atan2(seg.p1.z - seg.p0.z, segmentLength)) + 360) % 360
                return new PositionAndOrientation(positionOnSegment, yaw, pitch)
            }
            cumulatedDistance += segmentLength
        }
        return new PositionAndOrientation()
    }
}


@CompileStatic
class PositionAndOrientation {
    Coordinate position
    double yaw // YAW en degree
    double pitch // PITCH degree

    PositionAndOrientation() {
        position = new Coordinate()
    }

    PositionAndOrientation(Coordinate position, double yaw, double pitch) {
        this.position = position
        this.yaw = yaw
        this.pitch = pitch
    }

    void reverse() {
        yaw = (yaw + 180) % 360
        pitch = (pitch + 180) % 360
    }
}


@CompileStatic
class VehicleInfo {
    double distanceFromTheGeom = 0 // source distance from the reference point
    double height = 0 // source height
    double lateralOffset = 0 // source lateral distance from the center of the train
    double[] rolling = new double[24]
    double[] tractionA = new double[24]
    double[] tractionB = new double[24]
    double[] aerodynamicA = new double[24]
    double[] aerodynamicB = new double[24]
    double[] bridge = new double[24]


    PositionAndOrientation source = new PositionAndOrientation()

    private LineString currentRail = null
    double currentRailLength = 0
    // [0-1] current location on the linestring of currentRail
    double locationFractionOnCurrentRail = 0
    boolean followDirectionRailGeometry = true;

    VehicleInfo() {
    }

    VehicleInfo(double distanceFromTheGeom, double height, double lateralOffset,double[] rolling,double[] tractionA,double[] tractionB,double[] aerodynamicA,double[] aerodynamicB,double[] bridge) {
        this.distanceFromTheGeom = distanceFromTheGeom
        this.height = height
        this.rolling = rolling
        this.tractionA = tractionA
        this.tractionB = tractionB
        this.aerodynamicA = aerodynamicA
        this.aerodynamicB = aerodynamicB
        this.bridge = bridge
    }

    LineString getCurrentRail() {
        return currentRail
    }

    void setCurrentRail(LineString currentRail) {
        this.currentRail = currentRail
        this.currentRailLength = currentRail.length
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
