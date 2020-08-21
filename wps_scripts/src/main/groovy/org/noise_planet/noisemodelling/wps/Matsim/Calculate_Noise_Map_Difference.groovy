package org.noise_planet.noisemodelling.wps.Matsim

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

title = 'Map Difference'
description = 'Map Difference'

inputs = [
        mainMapTable : [
                name: 'Intput Roads table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: ROADS',
                type: String.class
        ],
        secondMapTable: [
                name: 'Intput Roads table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: ROADS',
                type: String.class
        ],
        invert: [
                name: 'Intput Roads table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: ROADS',
                min: 0,
                max: 1,
                type: Boolean.class
        ],
        outTable: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create',
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

    String mainMapTable = "ALT_RESULT_GEOM_13H45_14H00"
    if (input['mainMapTable']) {
        mainMapTable = input['mainMapTable']
    }

    String secondMapTable = "RESULT_GEOM_13H45_14H00"
    if (input['secondMapTable']) {
        secondMapTable = input['secondMapTable']
    }

    boolean invert = false;
    if (input['invert']) {
        invert = input['invert'] as boolean;
    }

    String outTable = "DIFF_RESULT_GEOM_13H45_14H00"
    if (input['outTable']) {
        outTable = input['outTable']
    }

    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTable))

    String query = "CREATE TABLE " + outTable + '''( 
            ID integer PRIMARY KEY,
            THE_GEOM geometry,
            HZ63 double precision,
            HZ125 double precision,
            HZ250 double precision,
            HZ500 double precision,
            HZ1000 double precision,
            HZ2000 double precision,
            HZ4000 double precision,
            HZ8000 double precision,
            LAEQ double precision,
            LEQ double precision
        ) AS
        SELECT mmt.IDRECEIVER, mmt.THE_GEOM,
        ''' + (invert ? "- " : "") + '''(mmt.HZ63 - smt.HZ63) as HZ63,
        ''' + (invert ? "- " : "") + '''(mmt.HZ125 - smt.HZ125) as HZ125,
        ''' + (invert ? "- " : "") + '''(mmt.HZ250 - smt.HZ250) as HZ250,
        ''' + (invert ? "- " : "") + '''(mmt.HZ500 - smt.HZ500) as HZ500,
        ''' + (invert ? "- " : "") + '''(mmt.HZ1000 - smt.HZ1000) as HZ1000,
        ''' + (invert ? "- " : "") + '''(mmt.HZ2000 - smt.HZ2000) as HZ2000,
        ''' + (invert ? "- " : "") + '''(mmt.HZ4000 - smt.HZ4000) as HZ4000,
        ''' + (invert ? "- " : "") + '''(mmt.HZ8000 - smt.HZ8000) as HZ8000,
        ''' + (invert ? "- " : "") + '''(mmt.LEQA - smt.LEQA) as LAEQ,
        ''' + (invert ? "- " : "") + '''(mmt.LEQ - smt.LEQ) as LEQ
        FROM ''' + mainMapTable + ''' mmt
        LEFT JOIN ''' + secondMapTable + ''' smt
        ON mmt.IDRECEIVER = smt.IDRECEIVER
    ;'''

    sql.execute(query)

    return [result: "Process done. Table " + outTable + " created !"]
}

