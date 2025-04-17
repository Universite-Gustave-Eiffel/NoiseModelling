package org.noise_planet.noisemodelling.wps.DataAssimilation

import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection


title = 'Dynamic Road Traffic Emission'
description = 'Creation of the dynamic road using best configurations'

outputs = [
        result: [
                name: 'Dynamic Road Table',
                description: 'Receiver table created ',
                type: Sql.class
        ]
]

static def exec(Connection connection){
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Traffic calibration')

    Sql sql = new Sql(connection)
    sql.execute("ALTER TABLE RECEIVERS DROP COLUMN ID_ROW, ID_COL")
    sql.execute("INSERT INTO RECEIVERS (THE_GEOM) SELECT s.The_GEOM FROM SENSORS s WHERE s.DEVEUI in (select m.DEVEUI FROM SENSORS_MEASUREMENTS m) ")

    // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
    createDynamicRoadsTable(sql)

    logger.info('End Traffic calibration')
}


/**
 * Creates the DYNAMIC_ROADS table and populates it with computed values.
 * Uses road and best configurations data to generate dynamic traffic values.
 *
 * @param sql Sql instance used to execute table creation and data insertion.
 */
static def createDynamicRoadsTable(Sql sql) {

    sql.execute("CREATE TABLE DYNAMIC_ROADS (" +
            "PK serial PRIMARY KEY," +
            "TIME integer," +
            "LINK_ID INTEGER," +
            "THE_GEOM GEOMETRY," +
            "TYPE CHARACTER VARYING," +
            "LV_D INTEGER," +
            "LV_E INTEGER," +
            "LV_N INTEGER," +
            "HGV_D INTEGER," +
            "HGV_E INTEGER," +
            "HGV_N INTEGER," +
            "LV_SPD_D INTEGER," +
            "LV_SPD_E INTEGER," +
            "LV_SPD_N INTEGER," +
            "HGV_SPD_D INTEGER," +
            "HGV_SPD_E INTEGER," +
            "HGV_SPD_N INTEGER," +
            "PVMT CHARACTER VARYING(10)," +
            "TEMP DOUBLE)")

    sql.execute("INSERT INTO DYNAMIC_ROADS (TIME, LINK_ID, THE_GEOM, TYPE, LV_D, LV_E, LV_N, HGV_D, HGV_E, HGV_N, LV_SPD_D, LV_SPD_E, LV_SPD_N, HGV_SPD_D, HGV_SPD_E, HGV_SPD_N, PVMT, TEMP)" +
            "SELECT " +
            "distinct c.T," +
            "r.ID_WAY, r.THE_GEOM, r.TYPE," +
            "CASE " +
            "WHEN r.TYPE = 'primary' OR r.TYPE = 'primary_link' THEN r.LV_D * c.PRIMARY_VAL " +
            "WHEN r.TYPE = 'secondary' OR r.TYPE = 'secondary_link' THEN r.LV_D * c.SECONDARY_VAL " +
            "WHEN r.TYPE = 'tertiary' OR r.TYPE = 'tertiary_link' THEN r.LV_D * c.TERTIARY_VAL " +
            "ELSE r.LV_D * c.OTHERS_VAL " +
            "END AS LV_D, " +
            "r.LV_E, r.LV_N," +
            "CASE " +
            "WHEN r.TYPE = 'primary' OR r.TYPE = 'primary_link' THEN r.HGV_D * c.PRIMARY_VAL " +
            "WHEN r.TYPE = 'secondary' OR r.TYPE = 'secondary_link' THEN r.HGV_D * c.SECONDARY_VAL " +
            "WHEN r.TYPE = 'tertiary' OR r.TYPE = 'tertiary_link' THEN r.HGV_D * c.TERTIARY_VAL " +
            "ELSE r.HGV_D * c.OTHERS_VAL " +
            "END AS HGV_D, " +
            "r.HGV_E, r.HGV_N, r.LV_SPD_D, r.LV_SPD_E, r.LV_SPD_N, r.HGV_SPD_D, r.HGV_SPD_E, r.HGV_SPD_N, r.PVMT, c.TEMP_VAL " +
            "FROM ROADS r " +
            "CROSS JOIN BEST_CONFIG c")
}

