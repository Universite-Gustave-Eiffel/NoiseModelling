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

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.propagation.AttenuationParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

title = 'Generate default atmospherics settings from the PERIOD field of a noise emission table'
description = '&#10145;&#65039; Generate default atmospherics settings from the PERIOD field of a noise emission table.' +
        ' It is used to export the result table to be edited and reimported to be used into Noise_level_from_source or' +
        ' Noise_level_from_traffic. This table make you able to change the temperature and other settings for each time period of the simulation'

inputs = [
        tableSourcesEmission            : [
                name       : 'Sources emission table name',
                title      : 'Sources emission table name',
                description: 'Name of the Sources table (ex. SOURCES_EMISSION) </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li><b> IDSOURCE </b>* : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)</li>' +
                        '<li><b> PERIOD </b>* : Time period, you will find this column on the output (VARCHAR)</li>',
                type: String.class
        ],
        tablePeriodAtmosphericSettings          : [
                name       : 'Atmospheric settings table name',
                title      : 'Atmospheric settings table name output for each time period',
                description: 'Name of the Atmospheric settings table </br> </br>' +
                        'The table will contain the following columns: </br> <ul>' +
                        '<li> <b> PERIOD </b>: time period (VARCHAR PRIMARY KEY) </li> ' +
                        '<li> <b> WINDROSE </b>: probability of occurrences of favourable propagation conditions (ARRAY(16)) </li> ' +
                        '<li> <b> TEMPERATURE </b>: Temperature in celsius (FLOAT) </li> ' +
                        '<li> <b> PRESSURE </b>: air pressure in pascal (FLOAT) </li> ' +
                        '<li> <b> HUMIDITY </b>: air humidity in percentage (FLOAT) </li> ' +
                        '<li> <b> GDISC </b>: choose between accept G discontinuity or not (BOOLEAN) default true </li> ' +
                        '<li> <b> PRIME2520 </b>: choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false </li> ' +
                        '</ul> Default to SOURCES_ATMOSPHERIC' ,
                min        : 0, max: 1, type: String.class
        ],
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
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
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, Map input) {

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    String tableSourcesEmission = input.get("tableSourcesEmission") as String

    def tablePeriodAtmosphericSettings = "SOURCES_ATMOSPHERIC"

    if(input.containsKey("tablePeriodAtmosphericSettings")) {
        tablePeriodAtmosphericSettings = input.get("tablePeriodAtmosphericSettings") as String
    }

    List<String> periods = JDBCUtilities.getUniqueFieldValues(connection, tableSourcesEmission, "PERIOD")

    AttenuationParameters defaultParameters = new AttenuationParameters()

    periods.each { String period ->
        defaultParameters.writeToDatabase(connection, tablePeriodAtmosphericSettings, period)
    }

    return "Calculation Done ! The table $tablePeriodAtmosphericSettings have been created, you can now export it, edit it and reimport to be used into Noise_level_from_source or Noise_level_from_traffic."
}
