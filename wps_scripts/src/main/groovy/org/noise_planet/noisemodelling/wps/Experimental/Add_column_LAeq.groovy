package org.noise_planet.noisemodelling.wps.NoiseModelling

/*
 * @Author Arnaud Can
 */
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection

title = 'Add column LAeq'
description = 'Adds a column LAeq to a table with freq values from 63Hz to 8kHz'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          inputlevels  : [name: 'Sound levels per frequency bands', title: 'Sound levels per frequency bands',description: 'columns values per freq', type: String.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]







static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // -------------------
    // Get inputs
    // -------------------

    String input_levels = "InputLevels"
    if (input['inputlevels']) {
        input_levels = input['inputlevels']
    }
    input_levels = input_levels.toUpperCase()

   
    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }


    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        //Statement sql = connection.createStatement()
        Sql sql = new Sql(connection)
        String laeqcalculate = "ALTER TABLE "+input_levels+" ADD COLUMN LeqA float as 10*log10((power(10,(HZ63-26.2)/10)+power(10,(HZ125-16.1)/10)+power(10,(HZ250-8.6)/10)+power(10,(HZ500-3.2)/10)+power(10,(HZ1000)/10)+power(10,(HZ2000+1.2)/10)+power(10,(HZ4000+1)/10)+power(10,(HZ8000-1.1)/10)))"
        sql.execute(laeqcalculate)
    }


    // Process Done
    return [result: "column LAeq added !"]

}

/**
 *
 */
