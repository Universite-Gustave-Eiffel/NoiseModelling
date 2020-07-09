
package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store

import org.geotools.jdbc.JDBCDataStore

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos;
import org.noise_planet.noisemodelling.emission.RSParametersCnossos;

import java.sql.Connection;
import java.sql.Statement;

import java.util.*;

import groovy.sql.Sql
import groovy.sql.GroovyRowResult

title = 'Import data from Mastim output'

description = 'Read Mastim events output file in order to get traffic NoiseModelling input'

inputs = [
    roadsTableName: [
        name: 'MATSIM_ROADS',
        title: 'Table name',
        description: 'Table name',
        type: String.class
    ],
    statsTableName: [
        name: 'MATSIM_ROADS_STATS',
        title: 'Table name',
        description: 'Table name',
        type: String.class
    ],
    timeString: [
        name: '0_1, 1_2, 2_3, ...',
        title: 'Field name',
        description: 'Field name',
        type: String.class
    ],
    outTableName: [
        name: 'ROADS',
        title: 'Table name',
        description: 'Table name',
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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            exec(connection, input)
            return [result: "OK"]
    }
}

// main function of the script
def exec(Connection connection, input) {
    
    String roadsTableName = input["roadsTableName"] as String;
    String statsTableName = input["statsTableName"] as String;
    String timeString = input["timeString"] as String;
    String outTableName = input["outTableName"] as String;

    Statement sql = connection.createStatement()
    
    sql.execute("DROP TABLE IF EXISTS " + outTableName)
    sql.execute("CREATE TABLE " + outTableName + '''( 
        PK integer PRIMARY KEY, 
        THE_GEOM geometry,
        LV_D double,
        LV_SPD_D double
    );''')
    
    sql.execute("MERGE INTO " + outTableName + '''
    SELECT R.ID PK, R.THE_GEOM THE_GEOM, S.TV LV_D, S.TV_SPD LV_SPD_D
    FROM ''' + roadsTableName + ''' R, ''' + statsTableName + ''' S
    WHERE R.LINK_ID = S.LINK_ID AND S.TIMESTRING = \'''' + timeString + '''\';
    ''')
}
