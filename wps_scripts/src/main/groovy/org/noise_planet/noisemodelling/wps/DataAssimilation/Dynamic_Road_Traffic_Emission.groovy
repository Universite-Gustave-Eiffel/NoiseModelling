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

package org.noise_planet.noisemodelling.wps.DataAssimilation

import groovy.sql.Sql
import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator
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

    sql.execute("DROP TABLE DYNAMIC_ROADS IF EXISTS")
    // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
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
            "CROSS JOIN BEST_CONFIGURATION_FULL c")

    sql.execute("drop table if exists LW_ROADS;")
    sql.execute("create table LW_ROADS (LINK_ID integer, PERIOD INTEGER, the_geom Geometry, " +
            "HZ63 double precision, HZ125 double precision, HZ250 double precision, HZ500 double precision, HZ1000 double precision, HZ2000 double precision, HZ4000 double precision, HZ8000 double precision);")

    def qry = 'INSERT INTO LW_ROADS(LINK_ID,PERIOD, the_geom, ' +
            'HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000) ' +
            'VALUES (?,?,?,?,?,?,?,?,?,?,?);'

    int k = 0
    def st = connection.prepareStatement("SELECT * FROM DYNAMIC_ROADS" )
    int coefficientVersion = 2
    sql.withBatch( 100, qry) { ps ->

        SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

        Map<String, Integer> sourceFieldsCache = new HashMap<>()

        while (rs.next()) {
            k++
            //logger.info(rs)
            Geometry geo = rs.getGeometry()

            // Compute emission sound level for each road segment

            double[][] results = EmissionTableGenerator.computeLw(rs, coefficientVersion, sourceFieldsCache)
            def lday = AcousticIndicatorsFunctions.wToDb(results[0])
            // fill the LW_ROADS table
            ps.addBatch(rs.getInt("LINK_ID") as Integer,rs.getInt("TIME") as Integer,  geo as Geometry,
                    lday[0] as Double, lday[1] as Double, lday[2] as Double,
                    lday[3] as Double, lday[4] as Double, lday[5] as Double,
                    lday[6] as Double, lday[7] as Double)
        }
    }

    // Add Z dimension to the road segments
    sql.execute("UPDATE LW_ROADS SET THE_GEOM = ST_UPDATEZ(The_geom,0.05);")


    logger.info('End Traffic calibration')
}




