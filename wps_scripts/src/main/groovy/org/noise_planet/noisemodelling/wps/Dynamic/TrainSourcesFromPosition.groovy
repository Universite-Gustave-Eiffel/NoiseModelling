
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
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
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

    Sql sql = new Sql(connection)

    final int srid = GeometryTableUtilities.getSRID(connection, railwayGeometries) as Integer

    // check that the srid is a metric unit coordinate reference system

    def isMetric = sql.firstRow("SELECT COUNT(*) FROM SPATIAL_REF_SYS WHERE SRID=$srid" +
            " AND PROJ4TEXT LIKE '%units=m%'")[0] as Boolean

    if (!isMetric) {
        throw new SQLException("Geometry projection system of the table $railwayGeometries must be metric ! (not EPSG=$srid)")
    }

    // keep track of train settings changes
    String previousTrainset = ""
    // keep track of train identifier (if the train change we clear the train history)
    String previousTrainId = ""
    // keep track of previous rails to put the wagons on the good position
    LinkedList<RailInfo> railNavigationHistory = new LinkedList<>()
    sql.eachRow("SELECT $fieldTimeStep, $fieldTrainId, $fieldTrainset FROM $trainsPosition ORDER BY $fieldTrainId, $fieldTimeStep") {

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
