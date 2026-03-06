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
 * @Author Adrien Le Bellec, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.wps.Dynamic

import groovy.json.JsonOutput
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Map Difference'
description = '&#10145;&#65039; Computes the difference between two noise maps'

inputs = [
        railwayGeom: [
                name: 'Railway geometries table',
                title: 'Railway geometries table',
                description: 'Table that contains geometries of rails. Wagons will be attached to the provided linestring. Default [[0.0, 0.0, 0.0],[1000.0, 0.0, 0.0]]',
                type: List.class,
                min:0, max:1,
        ],
        fieldTrainset: [
                name: 'Field train identifier',
                title: 'Field train identifier',
                description: 'Name of the field that identifies the train (show RailwayTrainsets.json for for details). Default FRET',
                type: String.class,
                min:0, max:1,
        ],
        speedSet: [
                name: 'Train speed',
                title: 'Train speed',
                description: 'Initialization of train speed in km/h. Defaukt 200km/h.',
                type: Double.class,
                min:0, max:1,
        ],
        integrationTimeSet: [
                name: 'Integration time',
                title: 'Integration time',
                description: 'Set integration time for dynamic analysis. Default 1 second',
                type: Double.class,
                min:0, max:1,
        ],
        idSection: [
                name: 'Identification railway section',
                title: 'Identification railway section',
                description: 'Set identification section parameter. Default idSection=1',
                type: Integer.class,
                min:0, max:1,
        ],
        trian_id: [
                name: 'Train identification',
                title: 'Train identification',
                description: 'Set train identification parameter. Default trian_id=1',
                type: Integer.class,
                min:0, max:1,
        ],
        timeStartSet: [
                name: 'Start time',
                title: 'Start time',
                description: 'Set start time as Timestamp date (https://www.epochconverter.com/). Default 1734297900',
                type: Integer.class,
                min:0, max:1,
        ],
        nameFile: [
                name: 'name file',
                title: 'name file',
                description: 'Name of the saved geojson file. Default discretized_points.geojson',
                type: String.class,
                min:0, max:1,
        ],
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

    // Définir la géométrie LineString initiale
    def railwayGeom = [[0.0, 0.0, 0.0],[1000.0, 0.0, 0.0]]
    if(input["railwayGeom"]) {
        railwayGeom = input["railwayGeom"] as List
    }

    def fieldTrainset = "FRET"
    if(input["fieldTrainset"]) {
        fieldTrainset = input["fieldTrainset"] as String
    }
    def speedSet = 200.00
    if(input["speedSet"]) {
        speedSet = input["speedSet"] as Double
    }
    def integrationTimeSet = 1
    if(input["integrationTimeSet"]) {
        integrationTimeSet = input["integrationTimeSet"] as Double
    }
    def idSection = 1
    if(input["idSection"]) {
        idSection = input["idSection"] as Integer
    }
    def timeStartSet = 1734297900
    if(input["timeStartSet"]) {
        timeStartSet = input["timeStartSet"] as Integer
    }
    def train_id = 1
    if(input["train_id"]) {
        train_id = input["train_id"] as Integer
    }


    def nameFile = "discretized_points.geojson"
    if(input["nameFile"]) {
        nameFile = input["nameFile"] as String
    }

    def field_1=1

    // conversion km/h to m/s
    def deltaD = speedSet/ 3.6 * integrationTimeSet as Double

    // List features storage
    def features = []

    // initialization primary key value
    def pk = 1

    // check LineString segments
    def lastPoint = railwayGeom[0]
    features.add(createFeature(lastPoint, field_1++, pk++, idSection, train_id, speedSet, fieldTrainset, timeStartSet++))
    def bufferFirstPoint = 0
    for (int i = 0; i < railwayGeom.size() - 1; i++) {
        def start = railwayGeom[i]
        def end = railwayGeom[i + 1]

        // Calcul de la distance totale du segment
        def dx = end[0] - start[0]
        def dy = end[1] - start[1]
        def dz = end[2] - start[2]
        def segmentLength = getTotalDistance(dx, dy, dz)

        def numPoints = (int)(segmentLength / deltaD)
        if (i > 0) {
            def dxBuffer = start[0] - lastPoint[0]
            def dyBuffer = start[1] - lastPoint[1]
            def dzBuffer = start[2] - lastPoint[2]
            def bufferDistance = getTotalDistance(dxBuffer, dyBuffer, dzBuffer)
            bufferFirstPoint = deltaD-bufferDistance

            def ratio = bufferFirstPoint / segmentLength
            def x1 = start[0] + dx * ratio
            def y1 = start[1] + dy * ratio
            def z1 = start[2] + dz * ratio
            def newPoint = [x1, y1, z1]
            features.add(createFeature(newPoint, field_1++, pk++, idSection, train_id, speedSet, fieldTrainset, timeStartSet++))

            numPoints = (int)((segmentLength-bufferFirstPoint)/ deltaD)
            for (int j = 1; j <= numPoints; j++) {
                def distance = j * deltaD
                if (distance > segmentLength) break
                ratio = distance / segmentLength
                def x = x1 + dx * ratio
                def y = y1 + dy * ratio
                def z = z1 + dz * ratio
                newPoint = [x, y, z]
                features.add(createFeature(newPoint, field_1++, pk++, idSection, train_id, speedSet, fieldTrainset, timeStartSet++))
                lastPoint = newPoint
            }
        }else{
            // Génération des points intermédiaires dans le segment actuel
            for (int j = 1; j <= numPoints; j++) {
                def distance = j * deltaD
                if (distance > segmentLength) break
                def ratio = distance / segmentLength
                def x = start[0] + dx * ratio
                def y = start[1] + dy * ratio
                def z = start[2] + dz * ratio
                def newPoint = [x, y, z]
                features.add(createFeature(newPoint, field_1++, pk++, idSection, train_id, speedSet, fieldTrainset, timeStartSet++))
                lastPoint = newPoint
            }
        }
    }


// Creeate final GeoJSON file
    def geojson = [
            type: "FeatureCollection",
            name: "PointFast",
            crs: [
                    type: "name",
                    properties: [
                            name: "urn:ogc:def:crs:EPSG::32635"
                    ]
            ],
            features: features
    ]

    def geojsonString = JsonOutput.prettyPrint(JsonOutput.toJson(geojson))
    def pathUse = new File("").absolutePath
    def folderUse = "/src/test/resources/org/noise_planet/noisemodelling/wps/Dynamic/Rail/TrainExport/"
    def file = new File(pathUse+folderUse+nameFile)

    file.write(geojsonString)

    println("GeoJSON File created : ${file.absolutePath}")

}

def createFeature(def coordinates, def field_1, def pk, def idSection, def train_id, def speed, def train_set, def timestep) {
    return [
            type: "Feature",
            properties: [
                    field_1: field_1,
                    pk: pk,
                    idSection: idSection,
                    x: coordinates[0],
                    y: coordinates[1],
                    z: coordinates[2],
                    angle: 0,
                    train_id: train_id,
                    speed: speed,
                    train_set: train_set,
                    timestep: timestep
            ],
            geometry: [
                    type: "Point",
                    coordinates: coordinates
            ]
    ]
}

static double getTotalDistance(dx,dy,dz) {
    def totalDistance = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2) +Math.pow(dz,2)  )
    return totalDistance
}

