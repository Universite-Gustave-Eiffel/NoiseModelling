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
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter

import java.sql.Connection

title = 'Display first rows of a table.'
description = 'Display first rows of a table containing. </br> Be careful, this treatment can be blocking if the table is large.'

inputs = [
        linesNumber: [name: 'Number of rows', title: 'Number of rows', description: 'Number of rows you want to display. (INTEGER) </br> </br> <b> Default value : 10 </b> ',min: 0, max: 1, type: Integer.class],
        tableName: [name: 'Name of the table', title: 'Name of the table', description: 'Name of the table you want to display.', type: String.class]
]

outputs = [result: [name: 'Result output', title: 'Result output', description: 'This is a HTML table', type: String.class]]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // print to command window
    System.out.println('Start : Display first rows of a table')
    def start = new Date()

    // Get the number of rows the user want to display
    int linesNumber = 10
    if (input['linesNumber']) {
        linesNumber = input['linesNumber'] as Integer
    }

    // Get name of the table
    String tableName = input["tableName"] as String
    // do it case-insensitive
    tableName = tableName.toUpperCase()

    // Create a connection statement to interact with the database in SQL
    Sql sql = new Sql(connection)

    List output = sql.rows(String.format("select * from %s LIMIT %s", tableName, linesNumber.toString()))

    System.out.println('End : Display first rows of a table')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return mapToTable(output, sql, tableName)
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

/**
 * Convert a list to HTML table
 * @param list
 * @return
 */
static String mapToTable(List<Map> list, Sql sql, String tableName) {

    StringBuilder output = new StringBuilder()

    Map first = list.first()

    output.append("The total number of rows is " + sql.firstRow('SELECT COUNT(*) FROM ' + tableName)[0])
    output.append("</br> </br> ")
    output.append("<table  border=' 1px solid black'><thead><tr>")

    first.each { key, val ->
        output.append("<th>${key}</th>")
    }

    output.append("</tr></thead><tbody>")
    WKTWriter wktWriter = new WKTWriter(3)
    list.each { map ->
        if (map.size() > 0) {

            def values = map.values()

            output.append("<tr>")

            values.each {
                def val = it
                if(it instanceof Geometry) {
                    val = wktWriter.write(it)
                }
                output.append "<td><div style='width: 150px;'>${val}</div></td>"
            }

            output.append("</tr>")
        }
    }
    output.append("</tbody></table>")

    output.toString()
}