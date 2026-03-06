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
 * @Author Adrien Le Bellec, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Dynamic

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Noise Train Map From Attenuation Matrix'
description = 'Noise Train Map From Attenuation Matrix.' +
        '<br/>'

inputs = [
        lwTable : [
                name: 'LW(PERIOD)',
                title: 'LW(PERIOD)',
                description: 'LW(PERIOD) ex. SOURCES_EMISSION' +
                        '<br/>The table must contain the following fields :' +
                        '<br/>IDSOURCE, PERIOD, Hz50 ,Hz63 ,Hz80 , Hz100 ,Hz125 ,Hz160 , Hz200 ,Hz250 ,Hz315 , Hz400 ,Hz500 ,Hz630 , Hz800 ,Hz1000 ,Hz1250 , Hz1600 ,Hz2000 ,Hz2500 , Hz3150 ,Hz4000 ,Hz5000 , Hz6300 ,Hz8000 ,Hz10000' +
                        '<br/> IDSOURCE link to primary key of attenuation table and PERIOD a varchar',
                type: String.class
        ],
        lwTable_sourceId: [
                name: 'LW(PERIOD) source index field',
                title: 'LW(PERIOD) source index field',
                description: 'LW(PERIOD) source index field. Default is IDSOURCE',
                min        : 0,
                max        : 1,
                type: String.class
        ],
        attenuationTable : [
        name: 'Attenuation Matrix Table name',
        title: 'Attenuation Matrix Table name',
        description: 'Attenuation Matrix Table name, Obtained from the Noise_level_from_source script with "confExportSourceId" enabled. Should be RECEIVERS_LEVEL' +
                '<br/>The table must contain the following fields :' +
                '<br/>IDRECEIVER, IDSOURCE, THE_GEOM, Hz50 ,Hz63 ,Hz80 , Hz100 ,Hz125 ,Hz160 , Hz200 ,Hz250 ,Hz315 , Hz400 ,Hz500 ,Hz630 , Hz800 ,Hz1000 ,Hz1250 , Hz1600 ,Hz2000 ,Hz2500 , Hz3150 ,Hz4000 ,Hz5000 , Hz6300 ,Hz8000 ,Hz10000',
        type: String.class
    ],
        outputTable : [
                name: 'outputTable Matrix Table name',
                title: 'outputTable Matrix Table name',
                description: 'outputTable',
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
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Noise_From_Attenuation_Matrix')
    logger.info("inputs {}", input)



    String lwTable_sourceId = "IDSOURCE"
    if (input['lwTable_sourceId']) {
        lwTable_sourceId = input['lwTable_sourceId']
    }


    String outputTable = input['outputTable'].toString().toUpperCase()
    String attenuationTable = input['attenuationTable'].toString().toUpperCase()
    String lwTable = input['lwTable'].toString().toUpperCase()
    String timeString = "PERIOD"
    String prefix = "HZ"

    // Groovy Dollar slashy string that contain the queries

    def query2 = $/CREATE TABLE $outputTable AS SELECT lg.IDRECEIVER,
            mr.$timeString AS $timeString,
            lg.the_geom,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}50 + lg.${prefix}50) / 10))) AS ${prefix}50,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}63 + lg.${prefix}63) / 10))) AS ${prefix}63,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}80 + lg.${prefix}80) / 10))) AS ${prefix}80,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}100 + lg.${prefix}100) / 10))) AS ${prefix}100,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}125 + lg.${prefix}125) / 10))) AS ${prefix}125,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}160 + lg.${prefix}160) / 10))) AS ${prefix}160,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}200 + lg.${prefix}200) / 10))) AS ${prefix}200,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}250 + lg.${prefix}250) / 10))) AS ${prefix}250,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}315 + lg.${prefix}315) / 10))) AS ${prefix}315,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}400 + lg.${prefix}400) / 10))) AS ${prefix}400,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}500 + lg.${prefix}500) / 10))) AS ${prefix}500,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}630 + lg.${prefix}630) / 10))) AS ${prefix}630,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}800 + lg.${prefix}800) / 10))) AS ${prefix}800,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}1000 + lg.${prefix}1000) / 10))) AS ${prefix}1000,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}1250 + lg.${prefix}1250) / 10))) AS ${prefix}1250,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}1600 + lg.${prefix}1600) / 10))) AS ${prefix}1600,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}2000 + lg.${prefix}2000) / 10))) AS ${prefix}2000,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}2500 + lg.${prefix}2500) / 10))) AS ${prefix}2500,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}3150 + lg.${prefix}3150) / 10))) AS ${prefix}3150,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}4000 + lg.${prefix}4000) / 10))) AS ${prefix}4000,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}5000 + lg.${prefix}5000) / 10))) AS ${prefix}5000,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}6300 + lg.${prefix}6300) / 10))) AS ${prefix}6300,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}8000 + lg.${prefix}8000) / 10))) AS ${prefix}8000,
            10 * LOG10( SUM(POWER(10,(mr.${prefix}10000 + lg.${prefix}10000) / 10))) AS ${prefix}10000
        FROM $attenuationTable  lg , $lwTable mr WHERE lg.IDSOURCE = mr.$lwTable_sourceId 
        GROUP BY lg.IDRECEIVER, mr.$timeString;
        ALTER TABLE  $outputTable ADD COLUMN LAEQ float as 10*log10((
power(10,(${prefix}50-30)/10)+power(10,(${prefix}63-26.2)/10)+power(10,(${prefix}80-22.2)/10)+
power(10,(${prefix}100-19.1)/10)+power(10,(${prefix}125-16.1)/10)+power(10,(${prefix}160-13.4)/10)+
power(10,(${prefix}200-10.9)/10)+power(10,(${prefix}250-8.6)/10)+power(10,(${prefix}315-6.6)/10)+
power(10,(${prefix}400-4.8)/10)+power(10,(${prefix}500-3.2)/10)+power(10,(${prefix}630-1.9)/10)+
power(10,(${prefix}800-0.8)/10)+power(10,(${prefix}1000)/10)+power(10,(${prefix}1250+0.6)/10)+
power(10,(${prefix}1600+1)/10)+power(10,(${prefix}2000+1.2)/10)+power(10,(${prefix}2500+1.3)/10)+
power(10,(${prefix}3150+1.2)/10)+power(10,(${prefix}4000+1)/10)+power(10,(${prefix}5000+0.5)/10)+
power(10,(${prefix}6300-0.1)/10)))+power(10,(${prefix}8000-1.1)/10)))+power(10,(${prefix}10000-2.5)/10)));
        ALTER TABLE $outputTable ADD COLUMN LEQ float as 10*log10((
power(10,(${prefix}50)/10)+power(10,(${prefix}63)/10)+power(10,(${prefix}80)/10)+
power(10,(${prefix}100)/10)+power(10,(${prefix}125)/10)+power(10,(${prefix}160)/10)+
power(10,(${prefix}200)/10)+power(10,(${prefix}250)/10)+power(10,(${prefix}315)/10)+
power(10,(${prefix}400)/10)+power(10,(${prefix}500)/10)+power(10,(${prefix}630)/10)+
power(10,(${prefix}800)/10)+power(10,(${prefix}1000)/10)+power(10,(${prefix}1250)/10)+
power(10,(${prefix}1600)/10)+power(10,(${prefix}2000)/10)+power(10,(${prefix}2500)/10)+
power(10,(${prefix}3150)/10)+power(10,(${prefix}4000)/10)+power(10,(${prefix}5000)/10)+
power(10,(${prefix}6300)/10)))+power(10,(${prefix}8000)/10)))+power(10,(${prefix}10000)/10)));
        CREATE UNIQUE INDEX ON $outputTable (IDRECEIVER, $timeString);
    /$

    sql.execute(query2.toString())

    logger.info('End : Noise_From_Attenuation_Matrix_MatSim')
    resultString = "Process done. Table of receivers LT_GEOM created !"
    logger.info('Result : ' + resultString)
    return resultString
}
