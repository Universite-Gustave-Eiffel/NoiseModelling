/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 *
 * @Author Nicolas Fortin, Univ Gustave Eiffel
 * @Author Pierre Aumond, Univ Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection
import java.sql.Statement

title = 'Delete all database tables'
description = 'Delete all non-system tables of the database.'

inputs = [
        areYouSure: [name: 'Are you sure ?', title: 'Are you sure?', description: 'Are you sure you want to delete all the tables in the database?', type: Boolean.class]
]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.',  type: String.class]]

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
    System.out.println('Start : Clean Database')
    def start = new Date()

    // Get name of the table
    Boolean areYouSure  = input['areYouSure'] as Boolean

    // list of the system tables
    List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]

    if (areYouSure) {
        // Build the result string with every tables
        StringBuilder sb = new StringBuilder()

        // Get every table names
        List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
        // Loop over the tables
        tables.each { t ->
            TableLocation tab = TableLocation.parse(t)
            if (!ignorelst.contains(tab.getTable())) {
                // Add the name of the table in the string builder
                if (sb.size() > 0) {
                    sb.append(" || ")
                }
                sb.append(tab.getTable())

                // Create a connection statement to interact with the database in SQL
                Statement stmt = connection.createStatement()
                // Drop the table
                stmt.execute("drop table if exists " + tab)
            }
        }
        resultString = "The table(s) " + sb.toString() + " was/were dropped."
    } else {
        resultString = "If you're not sure, we won't do anything !"
    }

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Clean Database')
    System.out.println('Duration : ' + TimeCategory.minus( new Date(), start ))

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