/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.wps.Dynamic

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Aggregate by source index'
description = 'Split a single table with duplicated geometry and source identifier into SOURCES_GEOM and SOURCES_EMISSION tables'

inputs = [
        tableSourceDynamic: [
                name       : 'Source table name',
                title      : 'Source table name',
                description: "<b>Name of the Source table.</b>  </br>  " +
                        "The source table have for the same index multiple periods," +
                        " other columns can be any supported columns of noise level from emission or noise level from traffic",
                type       : String.class
        ],
        sourceIndexFieldName: [
                name       : 'Source index field name',
                title      : 'Source index field name',
                description: "The field name of the source index, will be translated into IDSOURCE",
                type       : String.class
        ],
        sourcePeriodFieldName: [
                name       : 'Source period field name',
                title      : 'Source period field name',
                description: "The field name of the source period (ex. <b>T</b>), will be translated into PERIOD",
                type       : String.class
        ],
        sourceGeomTableName: [
                name       : 'Source geometry table name',
                title      : 'Source geometry table name',
                description: "The output table that contain the distinct source index with the appropriate geometry. Default is SOURCES_GEOM",
                min        : 0, max        : 1,
                type       : String.class
        ],
        sourceEmissionTableName: [
                name       : 'Source emission table name',
                title      : 'Source emission table name',
                description: "The output table that contain for each source index, the period and other attributes of" +
                        " the source. Default is SOURCES_EMISSION. Can be used directly on noise_level_from_source or" +
                        " Noise_From_Attenuation_Matrix",
                min        : 0, max        : 1,
                type       : String.class
        ],
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

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")


    String tableSourceDynamic = input["tableSourceDynamic"] as String

    String sourceIndexFieldName = TableLocation.capsIdentifier(input["sourceIndexFieldName"] as String, dbType)

    String sourcePeriodFieldName = TableLocation.capsIdentifier(input["sourcePeriodFieldName"] as String, dbType)

    String sourceGeomTableName = "SOURCES_GEOM"
    if(input.containsKey("sourceGeomTableName")) {
        sourceGeomTableName = input["sourceGeomTableName"] as String
    }

    String sourceEmissionTableName = "SOURCES_EMISSION"
    if(input.containsKey("sourceEmissionTableName")) {
        sourceEmissionTableName = input["sourceEmissionTableName"] as String
    }

    int sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableSourceDynamic, dbType))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+tableSourceDynamic+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+tableSourceDynamic+" does not have an associated SRID.")

    def columnNames = JDBCUtilities.getColumnNames(connection, tableSourceDynamic)
    columnNames.remove(TableLocation.capsIdentifier("THE_GEOM", dbType))
    columnNames.remove(sourcePeriodFieldName)
    columnNames.remove(sourceIndexFieldName)
    def additionalColumns = String.join(", ", columnNames)
    // Groovy Dollar slashy string that contain the queries

    def query = $/
        CREATE TABLE $sourceGeomTableName(IDSOURCE INT PRIMARY KEY, THE_GEOM GEOMETRY)
            AS SELECT $sourceIndexFieldName IDSOURCE, ANY_VALUE(THE_GEOM) THE_GEOM FROM $tableSourceDynamic GROUP BY IDSOURCE;

        CREATE TABLE $sourceEmissionTableName AS SELECT $sourceIndexFieldName IDSOURCE, $sourcePeriodFieldName PERIOD,
                 $additionalColumns FROM $tableSourceDynamic;

        CREATE INDEX ON $sourceEmissionTableName (IDSOURCE, PERIOD);

        SELECT UpdateGeometrySRID('$sourceGeomTableName','the_geom', $sridSources);
    /$

    sql.execute(query.toString())

    return "SOURCES_GEOM"
}
