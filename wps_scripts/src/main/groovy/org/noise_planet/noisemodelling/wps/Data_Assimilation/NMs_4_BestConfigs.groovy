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

package org.noise_planet.noisemodelling.wps.Data_Assimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Dynamic Road Traffic Emission'
description = 'Creation of the dynamic road using best configurations'
inputs = [
        bestConfig: [
                name: 'The best configuration table',
                title: 'The best configuration table',
                description: 'The best configuration table BEST_CONFIGURATION_FULL',
                type: String.class
        ],
        roadEmission: [
                name: 'The Road Emission table',
                title: 'The Road Emission table',
                description: 'The Road Emission table LW_ROADS',
                type:  String.class
        ]
 ]
outputs = [
        results: [
                name: 'Dynamic Road Emission Table',
                title: 'Dynamic Road Emission Table',
                description: 'Dynamic Road Emission Table using best configuration LW_ROADS_best',
                type: String.class
        ]
]

@CompileStatic
static def exec(Connection connection,inputs){
    connection = new ConnectionWrapper(connection)
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Traffic calibration')

    String bestConfig = inputs["bestConfig"] as String
    String roadEmission = inputs["roadEmission"] as String
    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE LW_ROADS_best IF EXISTS")
    // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
    sql.execute("CREATE TABLE LW_ROADS_best AS SELECT distinct r.* FROM "+roadEmission+" r, "+bestConfig+" c WHERE c.IT = r.PERIOD")

    // Add Z dimension to the road segments
    sql.execute("CREATE INDEX ON LW_ROADS_best(IDSOURCE, PERIOD)")

    logger.info('End Traffic calibration')
    return "Calculation Done ! The table LW_ROADS_best has been created."
}

// run the script
static def run(input) {

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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}