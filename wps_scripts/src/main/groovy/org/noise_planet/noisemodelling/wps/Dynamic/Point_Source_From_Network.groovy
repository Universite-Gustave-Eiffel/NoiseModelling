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
/**
 * @Author Valentin Le Bescond, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Dynamic

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryMetaData
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection


title = 'Create Point Source From a network'
description = ' <br><br> Creates a SOURCES_GEOM point source table from a network linestring table. This table is useful to compute the attenuation matrix between sources and receivers.' +
              'Create point sources from the network every "gridStep" meters. This point source will be used to compute the noise attenuation level from them to each receiver. </br></br>'
inputs = [
        tableNetwork: [
                name: 'Input table name',
                title: 'Input table name',
                description: 'Name of the network table. <br/> <br/>' +
                             'Must contain at least:</br>'+
                             '- <b>PK</b>: identifier with a Primary Key constraint</br>' +
                             '- <b>THE_GEOM</b>: geometric column',
                type: String.class
        ],
        gridStep : [name : 'gridStep',
                    title : "gridStep",
                    description : "Distance between location of possible sources along the network in meters.</br> <b> Default value : 10 </b>",
                    type: Integer.class,
                    min : 0,
                    max :1],
        height : [name : 'Source height',
                    title : "Source height",
                    description : "Height of the source in meters. </br> <b> Default value : 0.05 </b>",
                    type: Double.class,
                    min : 0,
                    max :1]
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


def exec(connection, Map input) {


    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString = null

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Point_Source_From_Network.groovy')
    logger.info("inputs {}", input)

    int gridStep = 10 // 10 meters is the default value
    if (input['gridStep']) {
        gridStep = Integer.valueOf(input['gridStep'] as String)
    }

    double h =  0.05 // height of the source (0.05 m) for road traffic
    if (input['height']) {
        h = input['height'] as Double
    }

    String roadsTableName = input['tableNetwork']

    sql.execute("DROP TABLE IF EXISTS SOURCES_POINTS")
    sql.execute("CREATE TABLE SOURCES_POINTS (ROAD_ID BIGINT, THE_GEOM GEOMETRY);");
    sql.execute("INSERT INTO SOURCES_POINTS (ROAD_ID, THE_GEOM) SELECT r.PK AS ROAD_ID," +
            " ST_TOMULTIPOINT(ST_Densify(r.THE_GEOM, "+gridStep+"))AS THE_GEOM FROM "+roadsTableName+" r;");
    sql.execute("DROP TABLE IF EXISTS SOURCES_GEOM")
    sql.execute("CREATE TABLE SOURCES_GEOM AS SELECT * FROM ST_EXPLODE('SOURCES_POINTS');");
    sql.execute("ALTER TABLE SOURCES_GEOM ADD PK INT AUTO_INCREMENT PRIMARY KEY;")
    sql.execute("DROP TABLE IF EXISTS SOURCES_POINTS")
    sql.execute("CREATE SPATIAL INDEX ON SOURCES_GEOM(THE_GEOM);")


    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(roadsTableName))
    def table_name = "SOURCES_GEOM"
    GeometryMetaData metaData = GeometryTableUtilities.getMetaData(connection, TableLocation.parse(table_name, DBUtils.getDBType(connection)), "THE_GEOM");
    metaData.setSRID(srid)
    metaData.setHasZ(true)
    metaData.initGeometryType()

    connection.createStatement().execute(String.format(Locale.ROOT, "ALTER TABLE %s ALTER COLUMN %s %s USING ST_SetSRID(ST_UPDATEZ(%s, %f),%d)",
            TableLocation.parse(table_name, DBUtils.getDBType(connection)), "THE_GEOM" , metaData.getSQL(),"THE_GEOM", h,srid))


    logger.info('End !')

    return resultString
}

