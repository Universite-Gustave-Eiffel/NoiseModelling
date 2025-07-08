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

import java.sql.Connection


title = 'Merged Sensors and Receivers'
description = 'Adding the sensors into the RECEIVERS after creating a regular grid of receivers.'

inputs = [
        tableReceivers: [
                name: 'The receiver table',
                title: 'The receiver table',
                description: 'The receiver table',
                type: String.class
        ],
        tableSensors: [
                name: 'The Sensors table',
                title: 'The Sensors table',
                description: 'The Sensors table ',
                type: String.class
        ]
]

outputs = [
        result: [
                name: 'Merged table',
                title: 'Merged table',
                description: 'Receiver table containing all sensors',
                type: String.class
        ]
]

@CompileStatic
static def exec(Connection connection,inputs) {
    connection = new ConnectionWrapper(connection)
    String receiverTable = inputs['tableReceivers'] as String
    String tableSensors = inputs['tableSensors'] as String

    Sql sql = new Sql(connection)
    sql.execute("ALTER TABLE " + receiverTable + " DROP COLUMN ID_ROW, ID_COL")
    sql.execute("ALTER TABLE " + receiverTable + " ADD COLUMN IDNAME VARCHAR;")
    sql.execute("UPDATE " + receiverTable + "  SET IDNAME = 'REC_MAP'")

    sql.execute("INSERT INTO  " + receiverTable + "  (THE_GEOM, IDNAME) SELECT The_GEOM THE_GEOM , IDSENSOR IDNAME FROM " + tableSensors)

    return "Calculation Done ! The tables " + receiverTable + " and "+tableSensors +" have been merged."

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