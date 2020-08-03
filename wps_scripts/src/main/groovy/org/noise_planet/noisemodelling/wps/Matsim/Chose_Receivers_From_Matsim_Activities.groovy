package org.noise_planet.noisemodelling.wps.Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import java.sql.*
import groovy.sql.Sql

title = 'Chose Receivers From Matsim Activities'
description = 'Chose one receiver per Mastim Activity'

inputs = [
        activitiesTable : [
                name: 'Name of the table containing the activities',
                title: 'Name of the table containing the activities',
                description: 'Name of the table containing the activities',
                type: String.class
        ],
        receiversTableName : [
                name: 'Name of the table containing the receivers',
                title: 'Name of the table containing the receivers',
                description: 'Name of the table containing the receivers',
                type: String.class
        ],
        outTableName: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create: ACTIVITIES',
                min: 0,
                max: 1,
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

    String outTableName = "RECEIVERS"
    if (input['outTableName']) {
        outTableName = input['outTableName']
    }

    String activitiesTable = "ACTIVITIES"
    if (input['activitiesTable']) {
        activitiesTable = input['activitiesTable']
    }

    String receiversTable = "ALL_RECEIVERS"
    if (input['receiversTable']) {
        receiversTable = input['receiversTable']
    }

    Sql sql = new Sql(connection)
    //Delete previous receivers
    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTableName))

    String query = "CREATE TABLE " + outTableName + '''( 
        PK integer PRIMARY KEY AUTO_INCREMENT,
        FACILITY_ID varchar(255),
        ORIGIN_GEOM geometry,
        THE_GEOM geometry,
        TYPES varchar(255)
    ) AS
    SELECT A.PK, A.FACILITY_ID, A.THE_GEOM AS ORIGIN_GEOM, (
        SELECT R.THE_GEOM 
        FROM ''' + receiversTable+ ''' R
        WHERE ST_EXPAND(A.THE_GEOM, 200, 200) && R.THE_GEOM
        ORDER BY ST_Distance(A.THE_GEOM, R.THE_GEOM) ASC LIMIT 1
    ) AS THE_GEOM, A.TYPES 
    FROM ''' + activitiesTable + ''' A ''';
    sql.execute(query);
    sql.execute("CREATE INDEX ON " + outTableName + "(FACILITY_ID)");
    sql.execute("CREATE SPATIAL INDEX ON " + outTableName + "(THE_GEOM)");

    sql.execute("UPDATE " + outTableName + " SET THE_GEOM = CASE " + '''
        WHEN THE_GEOM IS NULL 
        THEN ORIGIN_GEOM 
        ELSE THE_GEOM 
        END
    ''')

    return [result: "Process done. Table of receivers " + outTableName + " created !"]
}