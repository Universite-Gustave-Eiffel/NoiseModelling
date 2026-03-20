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

//TODO edit default values
inputs = [
        railwayGeom: [
                name: 'Railway geometries table',
                title: 'Railway geometries table',
                description: 'Table that contains geometries of rails. Wagons will be attached to the provided linestring',
                type: List.class,
                min:0, max:1,
        ],
        idSection: [
                name: 'idSection',
                title: 'Railway section identifier',
                description: 'Name of the field that identifies railway section. Default 1',
                min:0, max:1,
                type: String.class,
                min:0, max:1,
        ],
        nTrack: [
                name: 'nTrack',
                title: 'Number tracks',
                description: 'Name of the field that identifies the number of tracks. Default 1',
                min:0, max:1,
                type: Double.class,
                min:0, max:1,
        ],
        speedTrack: [
                name: 'speedTrack',
                title: 'Speed track',
                description: 'Maximal Speed track (km/h). Default 200',
                min:0, max:1,
                type: Double.class,
                min:0, max:1,
        ],
        trackTrans: [
                name: 'trackTrans',
                title: 'Track transfer',
                description: 'Name of the field that identifies the track transfer. Default 1',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        railRoughn: [
                name: 'railRoughn',
                title: 'Railway roughness identifier',
                description: 'Name of the field that identifies the rail roughness. Default 1',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        impactNois: [
                name: 'impactNois',
                title: 'Impact noise transfer function identifier',
                description: 'Name of the field that identifies the train. 0 - No impact || 1 - 1 switch/joint/crossing per 100m. Default 0',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        curvature: [
                name: 'curvature',
                title: 'Section curvature identifier',
                description: 'Identifier of the curvature section. 0 - R>500 m || 1 - 300 m<R<500 m || 2 - R<300 m. Default 0',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        bridgeTran: [
                name: 'bridgeTran',
                title: 'Bridge transfer identifier',
                description: 'Identifier for the transfer function or flat-rate correction for the bridge. Default 0',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        speedComme: [
                name: 'speedComme',
                title: 'Speed Commercial',
                description: 'Commercial speed of the section (km/h). Default 200',
                min:0, max:1,
                type: Integer.class,
                min:0, max:1,
        ],
        isTunnel: [
                name: 'isTunnel',
                title: 'Network is Tunnel',
                description: 'Caracterise network as tunel. Default false',
                min:0, max:1,
                type: Boolean.class,
                min:0, max:1,
        ],
        nameFile: [
                name: 'nameFile',
                title: 'name file save',
                description: 'Name of the file saved. Default trainNetwork',
                min:0, max:1,
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
    def railwayGeom = [
            [0.0, 0.0, 0.0],
            [1000.0, 0.0, 0.0]
    ]
    if(input["railwayGeom"]) {
        railwayGeom = input["railwayGeom"] as List
    }

    def idSection = 1
    if(input["idSection"]) {
        idSection = input["idSection"] as Integer
    }
    def nTrack = 1
    if(input["nTrack"]) {
        nTrack = input["nTrack"] as Integer
    }
    def speedTrack = 1
    if(input["speedTrack"]) {
        speedTrack = input["speedTrack"] as Integer
    }
    def trackTrans = 1
    if(input["trackTrans"]) {
        trackTrans = input["trackTrans"] as Integer
    }
    def railRoughn = 1
    if(input["railRoughn"]) {
        railRoughn = input["railRoughn"] as Integer
    }

    def impactNois = 0
    if(input["impactNois"]) {
        impactNois = input["impactNois"] as Integer
    }

    def curvature = 0
    if(input["curvature"]) {
        curvature = input["curvature"] as Integer
    }

    def bridgeTran = 0
    if(input["bridgeTran"]) {
        bridgeTran = input["bridgeTran"] as Integer
    }

    def speedComme = 200
    if(input["speedComme"]) {
        speedComme = input["speedComme"] as Integer
    }

    def isTunnel = false
    if(input["isTunnel"]) {
        isTunnel = input["isTunnel"] as boolean
    }


    def nameFile = "trainNetwork"
    if(input["nameFile"]) {
        nameFile = input["nameFile"] as String
    }

    def features = []
    def pk = 1
        def feature = [
                type: "Feature",
                properties: [
                        PK: pk++,
                        IDSECTION: idSection,
                        nTrack: nTrack,
                        speedTrack: speedTrack,
                        trackTrans: trackTrans,
                        railRoughn: railRoughn,
                        impactNois: impactNois,
                        curvature: curvature,
                        bridgeTran: bridgeTran,
                        speedComme: speedComme,
                        isTunnel: isTunnel
                ],
                geometry: [
                        type: "LineString",
                        coordinates: railwayGeom
                ]
        ]
        features.add(feature)

    def geojson = [
            type: "FeatureCollection",
            name: "TrainNetwork",
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
    def file = new File(pathUse+folderUse+nameFile+".geojson")

    file.write(geojsonString)

    println("Fichier RailWay network GeoJSON créé : ${file.absolutePath}")

}

