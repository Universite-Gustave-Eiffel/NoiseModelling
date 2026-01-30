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

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.json.*
import java.sql.Connection

title = 'Map Difference'
description = '&#10145;&#65039; Computes the difference between two noise maps'

//TODO EDIT information

inputs = [
        file1Path: [
                name: 'First GeoJSON file',
                title: 'First GeoJSON file',
                description: 'Path to the first GeoJSON file to merge',
                type: String.class,
        ],
        file2Path: [
                name: 'Second GeoJSON file',
                title: 'Second GeoJSON file',
                description: 'Path to the second GeoJSON file to merge',
                type: String.class,
        ],
        nameFile: [
                name: 'Output GeoJSON file',
                title: 'Output GeoJSON file',
                description: 'Name of the output GeoJSON file (without extension)',
                min: 0, max: 1,
                type: String.class,
        ],
]

outputs = [
        result: [
                name: 'Result output string',
                title: 'Result output string',
                description: 'Path to the merged GeoJSON file',
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

    // Paths to GeoJSON files to be merged
    def file1Path = input["file1Path"] ?: "file1.geojson"
    def file2Path = input["file2Path"] ?: "file2.geojson"
    def nameFile = input["nameFile"] ?: "fusion"

    def pathUse = new File("").absolutePath
    def folderUse = "/src/test/resources/org/noise_planet/noisemodelling/wps/"
    def file1 = new File(pathUse + folderUse + file1Path)
    def file2 = new File(pathUse + folderUse + file2Path)

    if (!file1.exists() || !file2.exists()) {
        throw new FileNotFoundException("One or both input files do not exist.")
    }

    // Read both GeoJSON files
    def jsonSlurper = new JsonSlurper()
    def geojson1 = jsonSlurper.parseText(file1.text)
    def geojson1Features = geojson1["features"] as List
    def geojson2 = jsonSlurper.parseText(file2.text)
    def geojson2Features = geojson2["features"] as List

    for (int n = 0; n < geojson2Features.size(); n++) {
        def feature = geojson2Features[n]
        def pkValue = feature["properties"]["pk"]

        if (pkValue != null) {
            try {
                // Convert pk value to a number
                int pkAsInt = 0
                if (pkValue instanceof Number) {
                    pkAsInt = pkValue as int
                } else if (pkValue instanceof String) {
                    pkAsInt = Integer.parseInt(pkValue)
                } else {
                    throw new IllegalArgumentException("The ‘pk’ type is not supported: ${pkValue.class}")
                }
                int newPk = pkAsInt + geojson1Features.size()
                feature["properties"]["pk"] = newPk
                println "Update of pk to index ${n} : ${pkValue} → ${newPk}"
            } catch (Exception e) {
                println "Error at index ${n}: Cannot convert ‘pk’ to a number. Current value : ${pkValue}. ${e.message}"
            }
        } else {
            println "Warning: ‘pk’ is null at index ${n}. Value unchanged.."
        }
    }

    // Merge feature tables
    def mergedFeatures = geojson1Features + geojson2Features

    // Create a new GeoJSON object with the merged features
    def mergedGeoJson = [
            type: "FeatureCollection",
            name: "TrainNetwork_Fusion",
            crs: geojson1["crs"] ?: geojson2["crs"],
            features: mergedFeatures
    ]

    // Convert to JSON and write to a file
    def geojsonString = JsonOutput.prettyPrint(JsonOutput.toJson(mergedGeoJson))
    def file = new File(pathUse+folderUse+nameFile+".geojson")

    file.write(geojsonString)

    println("RailWay network GeoJSON file create")

}