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

/**
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.scripts.Database_Manager

import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Display the list of tables (and their attributes).'
description = '&#10145;&#65039; Displays the list of tables that are in the database. </br> ' +
        '<hr>' +
        'Optionally it is also possible to display their attributes ("showColumns" parameter). </br> </br>' +
        '&#128161; To visualize the content of (a part of) a table, you can use "Table Visualization Data" script.'

inputs = [
        showColumns: [
                name       : 'Display columns of the tables',
                title      : 'Display columns of the tables',
                description: 'Do you want to display also the column of the tables ? </br></br>' +
                        '&#128161; Note : A small yellow key symbol (&#128273;) will appear if the column as a Primary Key constraint.',
                type       : Boolean.class,
                min        : 0, max: 1
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]




def exec(Connection connection, Map input, ProgressVisitor progress) {

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Display database')
    logger.info("inputs {}", input) // log inputs of the run

    Boolean showColumnName = false

    if(input['showColumns']) {
        showColumnName = input['showColumns'].toBoolean()
    }

    // list of the system tables
    List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]

    // Build the result string with every tables
    StringBuilder sb = new StringBuilder()

    // Get every table names
    List<String> tables = JDBCUtilities.getTableNames(connection, null, "PUBLIC", "%", null)
    DBTypes dbType = DBUtils.getDBType(connection)

    // Loop over the tables
    tables.each { t ->
        TableLocation tab = TableLocation.parse(t, dbType)
        if (!ignorelst.contains(tab.getTable())) {
            sb.append(tab.getTable())
            sb.append("</br>")
            if (showColumnName) {
                List<String> fields = JDBCUtilities.getColumnNames(connection, t)
                def geometryColumnNames = GeometryTableUtilities.getGeometryColumnNames(connection, tab)
                Integer keyColumnIndex = JDBCUtilities.getIntegerPrimaryKey(connection, tab)
                int columnIndex = 1;
                fields.each {
                    f ->
                        if (columnIndex == keyColumnIndex) {
                            sb.append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s&nbsp;&#128273;</br>", f))
                        } else if(geometryColumnNames.contains(f)) {
                            int epsg = 0;
                            try {
                                epsg = GeometryTableUtilities.getSRID(connection, tab)
                            } catch (Exception ex) {
                                //ignore
                            }
                            sb.append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s&nbsp;&#127760; (srid: %d)</br>", f, epsg))
                        } else {
                            sb.append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s</br>", f))
                        }
                        columnIndex++
                }
            }
            sb.append("</br>")
        }
    }

    if(sb.length() == 0) {
        sb.append('<div class="l-box" style="background-color: #f0f8ff; border: 1px solid #4682b4; padding: 20px; border-radius: 5px; margin: 10px 0;">')
        sb.append('<h2 style="color: #4682b4; margin-top: 0;">&#x1F5C4; Database is Empty</h2>')
        sb.append('<p style="font-size: 14px; color: #333;">No tables found in the database.</p>')
        sb.append('<p style="font-size: 14px; color: #666;">Please import data using **Import_File** to get started.</p>')
        sb.append('</div>')
    }
    // print to command window
    logger.info('End : Display database')

    // print to WPS Builder
    return sb.toString()
}

