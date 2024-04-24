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

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Noise Map From Attenuation Matrix'
description = 'Noise Map From Attenuation Matrix.' +
        '<br/>'

inputs = [
        lwTable : [
                name: 'LW(t)',
                title: 'LW(t)',
                description: 'LW(t)' +
                        '<br/>The table must contain the following fields :' +
                        '<br/>PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, T' +
                        '<br/> with PK, the IDSOURCE and T a timestring',
                type: String.class
        ],
        attenuationTable : [
        name: 'Attenuation Matrix Table name',
        title: 'Attenuation Matrix Table name',
        description: 'Attenuation Matrix Table name, Obtained from the Noise_level_from_source script with "confExportSourceId" enabled' +
                '<br/>The table must contain the following fields :' +
                '<br/>IDRECEIVER, IDSOURCE, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000',
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

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Noise_From_Attenuation_Matrix_MatSim')
    logger.info("inputs {}", input)


    String outputTable = input['outputTable'].toString().toUpperCase()
    String attenuationTable = input['attenuationTable'].toString().toUpperCase()
    String lwTable = input['lwTable'].toString().toUpperCase()
    String timeString = "IT"

    DatabaseMetaData dbMeta = connection.getMetaData();
    ResultSet rs = dbMeta.getIndexInfo(null, null, attenuationTable, false, false);

    logger.info("searching indexes on attenuation matrix ... ")
    boolean indexIDSOURCE = false;
    boolean indexGEOM = false;
    while (rs.next()) {
        String column = rs.getString("COLUMN_NAME");
        String pos = rs.getString("ORDINAL_POSITION");
        if (column == "IDSOURCE" && pos == "1") {
            indexIDSOURCE = true;
            logger.info("index on attenuation matrix IDSOURCE found")
        }
        if (column == "THE_GEOM" && pos == "1") {
            indexGEOM = true;
            logger.info("index on attenuation matrix THE_GEOM found")
        }
    }

    if (!indexIDSOURCE) {
        logger.info("index on attenuation matrix IDSOURCE, NOT found, creating one...")
        sql.execute("CREATE INDEX ON " + attenuationTable + " (IDSOURCE)");
    }
    if (!indexGEOM) {
        logger.info("index on attenuation matrix THE_GEOM, NOT found, creating one...")
        sql.execute("CREATE SPATIAL INDEX ON " + attenuationTable + " (THE_GEOM)");
    }


    String query = '''CREATE TABLE  ''' + outputTable + '''(
            PK integer PRIMARY KEY AUTO_INCREMENT,
            IDRECEIVER integer,
            THE_GEOM geometry,
            HZ63 double precision,
            HZ125 double precision,
            HZ250 double precision,
            HZ500 double precision,
            HZ1000 double precision,
            HZ2000 double precision,
            HZ4000 double precision,
            HZ8000 double precision,
            TIMESTRING varchar
        );
        INSERT INTO ''' + outputTable +'''(IDRECEIVER , THE_GEOM , HZ63 , HZ125 , HZ250 , HZ500 , HZ1000 , HZ2000 , HZ4000 , HZ8000 , TIMESTRING ) 
        SELECT lg.IDRECEIVER,  lg.THE_GEOM,
            10 * LOG10( SUM(POWER(10,(mr.HZ63 + lg.HZ63) / 10))) AS HZ63,
            10 * LOG10( SUM(POWER(10,(mr.HZ125 + lg.HZ125) / 10))) AS HZ125,
            10 * LOG10( SUM(POWER(10,(mr.HZ250 + lg.HZ250) / 10))) AS HZ250,
            10 * LOG10( SUM(POWER(10,(mr.HZ500 + lg.HZ500) / 10))) AS HZ500,
            10 * LOG10( SUM(POWER(10,(mr.HZ1000 + lg.HZ1000) / 10))) AS HZ1000,
            10 * LOG10( SUM(POWER(10,(mr.HZ2000 + lg.HZ2000) / 10))) AS HZ2000,
            10 * LOG10( SUM(POWER(10,(mr.HZ4000 + lg.HZ4000) / 10))) AS HZ4000,
            10 * LOG10( SUM(POWER(10,(mr.HZ8000 + lg.HZ8000) / 10))) AS HZ8000,
            mr.T AS TIMESTRING
        FROM ''' + attenuationTable + '''  lg 
        INNER JOIN ''' + lwTable + ''' mr ON lg.IDSOURCE = mr.PK
        GROUP BY lg.IDRECEIVER, lg.THE_GEOM, mr.T;
    '''

    String query2 = '''CREATE TABLE '''+outputTable+''' AS SELECT lg.IDRECEIVER,  lg.THE_GEOM,
            10 * LOG10( SUM(POWER(10,(mr.HZ63 + lg.HZ63) / 10))) AS HZ63,
            10 * LOG10( SUM(POWER(10,(mr.HZ125 + lg.HZ125) / 10))) AS HZ125,
            10 * LOG10( SUM(POWER(10,(mr.HZ250 + lg.HZ250) / 10))) AS HZ250,
            10 * LOG10( SUM(POWER(10,(mr.HZ500 + lg.HZ500) / 10))) AS HZ500,
            10 * LOG10( SUM(POWER(10,(mr.HZ1000 + lg.HZ1000) / 10))) AS HZ1000,
            10 * LOG10( SUM(POWER(10,(mr.HZ2000 + lg.HZ2000) / 10))) AS HZ2000,
            10 * LOG10( SUM(POWER(10,(mr.HZ4000 + lg.HZ4000) / 10))) AS HZ4000,
            10 * LOG10( SUM(POWER(10,(mr.HZ8000 + lg.HZ8000) / 10))) AS HZ8000,
            mr.T AS TIMESTRING
        FROM ''' + attenuationTable + '''  lg , ''' + lwTable + ''' mr WHERE lg.IDSOURCE = mr.PK 
        GROUP BY lg.IDRECEIVER, lg.THE_GEOM, mr.T;
    '''

    long start = System.currentTimeMillis();
    sql.execute(String.format("DROP TABLE IF EXISTS LT_GEOM"))
    logger.info(query)
    sql.execute(query)
    long stop = System.currentTimeMillis();
    println(stop-start)
     start = System.currentTimeMillis();
    sql.execute(String.format("DROP TABLE IF EXISTS "+outputTable+";"))
    logger.info(query2)
    sql.execute(query2)
     stop = System.currentTimeMillis();
    println(stop-start)

    String prefix = "HZ"
    sql.execute("ALTER TABLE  "+outputTable+" ADD COLUMN LEQA float as 10*log10((power(10,(" + prefix + "63-26.2)/10)+power(10,(" + prefix + "125-16.1)/10)+power(10,(" + prefix + "250-8.6)/10)+power(10,(" + prefix + "500-3.2)/10)+power(10,(" + prefix + "1000)/10)+power(10,(" + prefix + "2000+1.2)/10)+power(10,(" + prefix + "4000+1)/10)+power(10,(" + prefix + "8000-1.1)/10)))")
    sql.execute("ALTER TABLE "+outputTable+" ADD COLUMN LEQ float as 10*log10((power(10,(" + prefix + "63)/10)+power(10,(" + prefix + "125)/10)+power(10,(" + prefix + "250)/10)+power(10,(" + prefix + "500)/10)+power(10,(" + prefix + "1000)/10)+power(10,(" + prefix + "2000)/10)+power(10,(" + prefix + "4000)/10)+power(10,(" + prefix + "8000)/10)))")

    logger.info('End : Noise_From_Attenuation_Matrix_MatSim')
    resultString = "Process done. Table of receivers LT_GEOM created !"
    logger.info('Result : ' + resultString)
    return resultString
}

