/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Univ Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

title = 'Add primary key column or constraint'
description = 'Add a primary key column or add a primary key constraint to a column of a table. ' +
        '</br>  It is strongly advised to add a primary key on one of the columns for the ' +
        'source and receiver tables before doing a calculation.'

inputs = [pkName: [name: 'Name of the column', title: 'Name of the column', description: 'Name of the column to be added, or for which the main key constraint will be added. </br> Primary keys must contain UNIQUE values, and cannot contain NULL values.', type: String.class],
          table : [name: 'Name of the table', description: 'Name of the table to which a primary key will be added.', title: 'Name of the table', type: String.class]]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Add primary key column or constraint')
    def start = new Date()

    // Get name of the table
    String table = input["table"] as String
    // do it case-insensitive
    table = table.toUpperCase()

    // Get name of the pk filed
    String pkName = input['pkName'] as String
    // do it case-insensitive
    pkName = pkName.toUpperCase()

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    // get the index of the primary key column (if exists > 0)
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, table)

    // get the index of the column given by the user (if exists > 0)
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)
    int pkUserIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), pkName)


    if (pkIndex > 0) {
        resultString = String.format("Source table %s does already contain a primary key", table)
        // print to command window
        System.out.println('ERROR : ' + resultString)
        System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))
        // print to WPS Builder
        return resultString
    }


    if (pkUserIndex > 0) {
        stmt.execute("ALTER TABLE " + table + " ALTER COLUMN " + pkName + " INT NOT NULL;")
        stmt.execute("ALTER TABLE " + table + " ADD PRIMARY KEY (" + pkName + ");  ")
        resultString = String.format(table + " has a new primary key constraint on " + pkName + ".")
    } else {
        stmt.execute("ALTER TABLE " + table + " ADD " + pkName + " INT AUTO_INCREMENT PRIMARY KEY;")
        resultString = String.format(table + " has a new primary key column which is called " + pkName + ".")
    }

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Add primary key column or constraint')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}

def run(input) {

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