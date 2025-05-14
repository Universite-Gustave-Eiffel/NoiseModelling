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
 */

package org.noise_planet.noisemodelling.wps.Experimental_Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.GroovyRowResult
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Noise Map From Attenuation Matrix'
description = 'Noise Map From Attenuation Matrix.' +
        '<br/>'

inputs = [
    matsimRoads: [
            name: 'Table name of the MATSIM table containing the roads geometries',
            title: 'Table name of the MATSIM table containing the roads geometries',
            description: 'Table name of the MATSIM table containing the roads geometries' +
                    '<br/>The table must contain the following fields : (PK, LINK_ID, THE_GEOM)',
            type: String.class
    ],
    matsimRoadsLw : [
            name: 'Table name of the MATSIM table containing the roads LW stats per timeBin',
            title: 'Table name of the MATSIM table containing the roads LW stats per timeBin',
            description: 'Table name of the MATSIM table containing the roads LW stats per timeBin' +
                    '<br/>The table must contain the following fields : ' +
                    '<br/>PK, LINK_ID, LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000, TIME',
            type: String.class
    ],
    receiversTable : [
            name: 'Name of the table containing the receivers',
            title: 'Name of the table containing the receivers',
            description: 'Name of the table containing the receivers' +
                    '<br/>The table must contain the following fields : ' +
                    '<br/>PK, THE_GEOM',
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
    timeBinSize: [
            name: 'The size of time bins in seconds.',
            title: 'The size of time bins in seconds.',
            description: 'This parameter dictates the time resolution of the resulting data ' +
                    '<br/>The time information stored will be the starting time of the time bins ' +
                    '<br/>For exemple with a timeBinSize of 3600, the data will be analysed using the following timeBins: ' +
                    '<br/>0, 3600, 7200, ..., 79200, 82800',
            type: Integer.class
    ],
    outTableName: [
            name: 'Output table name',
            title: 'Output table name',
            description: 'Output table name' +
                    '<br/>The table will contain the following fields :' +
                    '<br/>PK, IDRECEIVER, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ000, HZ8000, TIME',
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
@CompileStatic
static def exec(Connection connection, input) {

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Noise_From_Attenuation_Matrix')
    logger.info("inputs {}", input)

    String matsimRoads = input['matsimRoads']
    String matsimRoadsLw = input['matsimRoadsLw']
    String attenuationTable = input['attenuationTable']
    String receiversTable = input['receiversTable']
    String outTableName = input['outTableName']

    int timeBinSize = 3600
    if (input["timeBinSize"]) {
        timeBinSize = input["timeBinSize"] as int;
    }

    DatabaseMetaData dbMeta = connection.getMetaData();
    ResultSet rs = dbMeta.getIndexInfo(null, null, attenuationTable, false, false);


    sql.execute(String.format("DROP TABLE %s IF EXISTS", outTableName))
    String query = "CREATE TABLE " + outTableName + '''(
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
                        TIME int
                    )
                '''
    sql.execute(query)
    PreparedStatement insert_stmt = connection.prepareStatement(
            "INSERT INTO " + outTableName + " VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
    )

    logger.info("searching indexes on attenuation matrix ... ")
    ensureIndex(connection, attenuationTable, "IDSOURCE", false)
    ensureIndex(connection, attenuationTable, "IDRECEIVER", false)
    logger.info("searching indexes on traffic tables ... ")
    ensureIndex(connection, matsimRoads, "LINK_ID", false)
    ensureIndex(connection, matsimRoadsLw, "LINK_ID", false)
    ensureIndex(connection, matsimRoadsLw, "TIME", false)

    List<String> mrs_freqs = ["LW63", "LW125", "LW250", "LW500", "LW1000", "LW2000", "LW4000", "LW8000"]

    long count = 0, do_print = 1
    List<GroovyRowResult> receivers_res = sql.rows("SELECT * FROM " + receiversTable);
    long nb_receivers = receivers_res.size()
    long start = System.currentTimeMillis();
    for (GroovyRowResult receiver: receivers_res) {
        long receiver_id = receiver["PK"] as long;
        Geometry receiver_geom = receiver["THE_GEOM"] as Geometry;
        Map<Integer, List<Double>> levels = new HashMap<Integer, List<Double>>();
        List<GroovyRowResult> sources_att_res = sql.rows(String.format("SELECT lg.* FROM %s lg WHERE lg.IDRECEIVER = %d", attenuationTable, receiver_id));
        long nb_sources = sources_att_res.size();
        if (nb_sources == 0) {
            count++
            continue
        }
        for (GroovyRowResult sources_att: sources_att_res) {
            long source_id = sources_att["IDSOURCE"] as long;
            List<Double> attenuation = [
                    sources_att["HZ63"] as double,
                    sources_att["HZ125"] as double,
                    sources_att["HZ250"] as double,
                    sources_att["HZ500"] as double,
                    sources_att["HZ1000"] as double,
                    sources_att["HZ2000"] as double,
                    sources_att["HZ4000"] as double,
                    sources_att["HZ8000"] as double,
            ];
            List<GroovyRowResult> roads_stats_res = sql.rows(String.format(
                    "SELECT mrs.* FROM %s mrs INNER JOIN %s mr ON mr.LINK_ID = mrs.LINK_ID WHERE mr.PK = %d",
                    matsimRoadsLw, matsimRoads, source_id));
            for (GroovyRowResult roads_stats: roads_stats_res) {
                int timeBin = roads_stats["TIME"] as int
                if (!levels.containsKey(timeBin)) {
                    levels[timeBin] = [-99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0] as List<Double>
                }
                for (i in 0..<8) {
                    double new_level = (roads_stats[mrs_freqs[i]] as double) + attenuation[i];
                    levels[timeBin][i] = (double) 10 * Math.log10( Math.pow(10, (double) levels[timeBin][i] / 10.0) + Math.pow(10, (double) new_level / 10.0) );
                }
            }
        }

        for (int timeBin = 0; timeBin < 86400; timeBin += timeBinSize) {
            if (!levels.containsKey(timeBin)) {
                levels[timeBin] = [-99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0] as List<Double>
            }
            List<Double> ts_levels = levels[timeBin]
            insert_stmt.setLong(1, receiver_id)
            insert_stmt.setString(2, receiver_geom.toText())
            for (i in 0..<8) {
                insert_stmt.setDouble(i+3, ts_levels[i])
            }
            insert_stmt.setInt(11, timeBin)
            insert_stmt.execute()
        }
        if (count >= do_print) {
            double elapsed = (System.currentTimeMillis() - start + 1) / 1000
            logger.info(String.format("Processing Receiver %d (max:%d) - elapsed : %ss (%.1fit/s)",
                    count, nb_receivers, elapsed, count/elapsed))
            do_print *= 2
        }
        count ++
    }

    String prefix = "HZ"
    sql.execute("ALTER TABLE " + outTableName + " ADD COLUMN LEQA float as 10*log10((power(10,(" + prefix + "63-26.2)/10)+power(10,(" + prefix + "125-16.1)/10)+power(10,(" + prefix + "250-8.6)/10)+power(10,(" + prefix + "500-3.2)/10)+power(10,(" + prefix + "1000)/10)+power(10,(" + prefix + "2000+1.2)/10)+power(10,(" + prefix + "4000+1)/10)+power(10,(" + prefix + "8000-1.1)/10)))")
    sql.execute("ALTER TABLE " + outTableName + " ADD COLUMN LEQ float as 10*log10((power(10,(" + prefix + "63)/10)+power(10,(" + prefix + "125)/10)+power(10,(" + prefix + "250)/10)+power(10,(" + prefix + "500)/10)+power(10,(" + prefix + "1000)/10)+power(10,(" + prefix + "2000)/10)+power(10,(" + prefix + "4000)/10)+power(10,(" + prefix + "8000)/10)))")

    logger.info('End : Noise_From_Attenuation_Matrix')
    resultString = "Process done. Table of receivers " + outTableName + " created !"
    logger.info('Result : ' + resultString)
    return resultString
}

static boolean tableExists(Connection connection, String table) {
    DatabaseMetaData dbMeta = connection.getMetaData();
    ResultSet rs = dbMeta.getTables(null, null, table, null);
    boolean table_found = false;
    if (rs.next()) {
        table_found = true
    }
    return table_found
}

static boolean columnExists(Connection connection, String table, String column_name) {
    DatabaseMetaData dbMeta = connection.getMetaData();
    ResultSet rs = dbMeta.getColumns(null, null, table, column_name);
    boolean col_found = false;
    if (rs.next()) {
        col_found = true
    }
    return col_found
}

static boolean indexExists(Connection connection, String table, String column_name) {
    DatabaseMetaData dbMeta = connection.getMetaData();
    ResultSet rs = dbMeta.getIndexInfo(null, null, table, false, false);
    boolean index_found = false;
    while (rs.next()) {
        String column = rs.getString("COLUMN_NAME");
        String pos = rs.getString("ORDINAL_POSITION");
        if (column == column_name && pos == "1") {
            index_found = true;
        }
    }
    return index_found
}

static void ensureIndex(Connection connection, String table, String column_name, boolean spatial) {
    if (!indexExists(connection, table, column_name)) {
        Sql sql = new Sql(connection)
        sql.execute("CREATE " + (spatial ? "SPATIAL " : "") + "INDEX ON " + table + " (" + column_name + ")");
    }
}