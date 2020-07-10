package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.Scenario
import org.matsim.core.config.ConfigUtils
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.facilities.ActivityFacilities
import org.matsim.facilities.ActivityFacility
import org.matsim.facilities.MatsimFacilitiesReader

import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Create receivers based on a Matsim "facilities" file.'

inputs = [
        roadsTableName: [
                name: 'Intput Roads table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: ROADS',
                type: String.class
        ],
        sourceTableName: [
                name: 'Output Source table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: SOURCES_0DB',
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


def run(input) {

    // Get name of the database
    String dbName = "h2gis"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection -> exec(connection, input)
    }
}


def exec(connection, input) {

    String roadsTableName = "ROADS"
    if (input['roadsTableName']) {
        roadsTableName = input['roadsTableName']
    }
    roadsTableName = roadsTableName.toUpperCase()

    String sourceTableName = "SOURCES_0DB"
    if (input['sourceTableName']) {
        sourceTableName = input['sourceTableName']
    }
    sourceTableName = sourceTableName.toUpperCase()

    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", sourceTableName))

    String query = "CREATE TABLE " + sourceTableName + '''( 
        PK integer PRIMARY KEY,
        THE_GEOM geometry,
        LWD63 double precision, LWD125 double precision, LWD250 double precision, LWD500 double precision, LWD1000 double precision, LWD2000 double precision, LWD4000 double precision, LWD8000 double precision,
        LWE63 double precision, LWE125 double precision, LWE250 double precision, LWE500 double precision, LWE1000 double precision, LWE2000 double precision, LWE4000 double precision, LWE8000 double precision,
        LWN63 double precision, LWN125 double precision, LWN250 double precision, LWN500 double precision, LWN1000 double precision, LWN2000 double precision, LWN4000 double precision, LWN8000 double precision
        ) AS
        SELECT PK, THE_GEOM,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        FROM ''' + roadsTableName + ''' 
    ;'''

    sql.execute(query)

    return [result: "Process done. Table " + sourceTableName + " created !"]
}

