package org.noise_planet.noisemodelling.wps.Experimental


import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Create receivers based on a Matsim "facilities" file.'

inputs = [
    matsimRoadsStats : [
        name: 'Matsim Roads stats with LW table name',
        title: 'Matsim Roads stats with LW Table name',
        description: 'Matsim Roads stats with LW Table name : MATSIM_ROADS_STATS',
        type: String.class
    ],
    timeString: [
            name: '0_1, 1_2, 2_3, ...',
            title: 'Field name',
            description: 'Field name',
            type: String.class
    ],
    attenuationTable : [
            name: 'Attenuation Matrice Table name',
            title: 'Attenuation Matrice Table name',
            description: 'Attenuation Matrice Table name : LDAY_GEOM',
            type: String.class
    ],
    outTableName: [
        name: 'Output table name',
        title: 'Name of created table',
        description: 'Name of the table you want to create: RESULT_GEOM',
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

    String matsimRoads = "MATSIM_ROADS_STATS"
    if (input['matsimRoads']) {
        matsimRoads = input['matsimRoads']
    }
    String matsimRoadsStats = matsimRoads + "_STATS"

    String timeString = input["timeString"] as String;

    String attenuationTable = "LDAY_GEOM"
    if (input['attenuationTable']) {
        attenuationTable = input['attenuationTable']
    }
    attenuationTable = attenuationTable.toUpperCase()

    String outTableName = "RESULT_GEOM"
    if (input['outTableName']) {
        outTableName = input['outTableName']
    }
    outTableName = outTableName.toUpperCase()

    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTableName))

    String query = "CREATE TABLE " + outTableName + '''( 
            IDRECEIVER integer PRIMARY KEY,
            THE_GEOM geometry,
            HZ63 double precision,
            HZ125 double precision,
            HZ250 double precision,
            HZ500 double precision,
            HZ1000 double precision,
            HZ2000 double precision,
            HZ4000 double precision,
            HZ8000 double precision,
        ) AS
        SELECT lg.IDRECEIVER,  lg.THE_GEOM,
            10 * LOG10( SUM(POWER(10,lg.HZ63 / 10) + POWER(10,mrs.LW63 / 10)) ) AS HZ63,
            10 * LOG10( SUM(POWER(10,lg.HZ125 / 10) + POWER(10,mrs.LW125 / 10)) ) AS HZ125,
            10 * LOG10( SUM(POWER(10,lg.HZ250 / 10) + POWER(10,mrs.LW250 / 10)) ) AS HZ250,
            10 * LOG10( SUM(POWER(10,lg.HZ500 / 10) + POWER(10,mrs.LW500 / 10)) ) AS HZ500,
            10 * LOG10( SUM(POWER(10,lg.HZ1000 / 10) + POWER(10,mrs.LW1000 / 10)) ) AS HZ1000,
            10 * LOG10( SUM(POWER(10,lg.HZ2000 / 10) + POWER(10,mrs.LW2000 / 10)) ) AS HZ2000,
            10 * LOG10( SUM(POWER(10,lg.HZ4000 / 10) + POWER(10,mrs.LW4000 / 10)) ) AS HZ4000,
            10 * LOG10( SUM(POWER(10,lg.HZ8000 / 10) + POWER(10,mrs.LW8000 / 10)) ) AS HZ8000 
        FROM ''' + attenuationTable + '''  lg 
        INNER JOIN ''' + matsimRoads + ''' mr ON lg.IDSOURCE = mr.ID
        INNER JOIN ''' + matsimRoadsStats + ''' mrs ON mr.LINK_ID = mrs.LINK_ID
        WHERE mrs.TIMESTRING = \'''' + timeString + '''\'
        GROUP BY lg.IDRECEIVER, lg.THE_GEOM;
    ;'''

    sql.execute(query)

    sql.execute(String.format("DROP TABLE IF EXISTS ALT_%s", outTableName))

    query = "CREATE TABLE ALT_" + outTableName + '''( 
            IDRECEIVER integer PRIMARY KEY,
            THE_GEOM geometry,
            HZ63 double precision,
            HZ125 double precision,
            HZ250 double precision,
            HZ500 double precision,
            HZ1000 double precision,
            HZ2000 double precision,
            HZ4000 double precision,
            HZ8000 double precision,
        ) AS
        SELECT lg.IDRECEIVER,  lg.THE_GEOM,
            10 * LOG10( SUM(POWER(10,lg.HZ63 / 10) + POWER(10,mrs.ALT_LW63 / 10)) ) AS HZ63,
            10 * LOG10( SUM(POWER(10,lg.HZ125 / 10) + POWER(10,mrs.ALT_LW125 / 10)) ) AS HZ125,
            10 * LOG10( SUM(POWER(10,lg.HZ250 / 10) + POWER(10,mrs.ALT_LW250 / 10)) ) AS HZ250,
            10 * LOG10( SUM(POWER(10,lg.HZ500 / 10) + POWER(10,mrs.ALT_LW500 / 10)) ) AS HZ500,
            10 * LOG10( SUM(POWER(10,lg.HZ1000 / 10) + POWER(10,mrs.ALT_LW1000 / 10)) ) AS HZ1000,
            10 * LOG10( SUM(POWER(10,lg.HZ2000 / 10) + POWER(10,mrs.ALT_LW2000 / 10)) ) AS HZ2000,
            10 * LOG10( SUM(POWER(10,lg.HZ4000 / 10) + POWER(10,mrs.ALT_LW4000 / 10)) ) AS HZ4000,
            10 * LOG10( SUM(POWER(10,lg.HZ8000 / 10) + POWER(10,mrs.ALT_LW8000 / 10)) ) AS HZ8000 
        FROM ''' + attenuationTable + '''  lg 
        INNER JOIN ''' + matsimRoads + ''' mr ON lg.IDSOURCE = mr.ID
        INNER JOIN ''' + matsimRoadsStats + ''' mrs ON mr.LINK_ID = mrs.LINK_ID
        WHERE mrs.TIMESTRING = \'''' + timeString + '''\'
        AND mrs.IS_ALT_DIFF = TRUE
        GROUP BY lg.IDRECEIVER, lg.THE_GEOM;
    ;'''

    sql.execute(query)

    return [result: "Process done. Table of receivers " + outTableName + " created !"]
}

