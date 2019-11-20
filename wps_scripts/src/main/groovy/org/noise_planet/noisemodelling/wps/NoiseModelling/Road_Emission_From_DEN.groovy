package org.noise_planet.noisemodelling.wps.NoiseModelling

/*
 * @Author Pierre Aumond 13/11/2019
 */

import geoserver.GeoServer
import geoserver.catalog.Store

import org.h2gis.api.ProgressVisitor
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import javax.xml.stream.XMLStreamException
import org.cts.crs.CRSException

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.sql.PreparedStatement
import groovy.sql.Sql
import org.h2gis.utilities.SFSUtilities
import org.h2gis.api.EmptyProgressVisitor
import org.noisemodellingwps.utilities.WpsConnectionWrapper
import org.h2gis.utilities.wrapper.*

import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry


import java.sql.SQLException
import java.util.ArrayList
import java.util.List

title = 'Compute Road Emission'
description = 'Compute Road Emission Noise Map from Day Evening Night traffic flow rate and speed estimates. '

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name',description: "Table with fields :<br/> " +
                  "PK Integer (Primary Key)<br/>" +
                  "TV_D double, Hourly average light and heavy vehicle count (6-18h)<br/>" +
                  "TV_E double, Hourly average light and heavy vehicle count (18-22h)<br/>" +
                  "TV_N double, Hourly average light and heavy vehicle count (22-6h)<br/>" +
                  "HV_D double, Hourly average heavy vehicle count (6-18h)<br/>" +
                  "HV_E double, Hourly average heavy vehicle count (18-22h)<br/>" +
                  "HV_N double, Hourly average heavy vehicle count (22-6h)<br/>" +
                  "LV_SPD_D double, Hourly average light vehicle speed (6-18h)<br/>" +
                  "LV_SPD_E double, Hourly average light vehicle speed (18-22h)<br/>" +
                  "LV_SPD_N double, Hourly average light vehicle speed (22-6h)<br/>" +
                  "HV_SPD_D double, Hourly average heavy vehicle speed (6-18h)<br/>" +
                  "HV_SPD_E double, Hourly average heavy vehicle speed (18-22h)<br/>" +
                  "HV_SPD_N double, Hourly average heavy vehicle speed (22-6h)<br/>" +
                  "PVMT varchar, Cnossos Road pavement (ex: NL05)", type: String.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    String output = null
    // -------------------
    // Get inputs
    // -------------------
    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // ----------------------------------
    // Start... 
    // ----------------------------------

    System.out.println("Run ...")

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->

        //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postgis database
        connection = new ConnectionWrapper(connection)
        System.out.println("Connection to the database ok ...")


        //Get the geometry field of the source table
        TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name)
        List<String> geomFields = SFSUtilities.getGeometryFields(connection, sourceTableIdentifier)
        if(geomFields.isEmpty()) {
            output = String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier)
            throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier));
        }
        String sourceGeomName =  geomFields.get(0);

        //Get the primary key field of the source table
        int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, sources_table_name);
        if(pkIndex < 1) {
            output = String.format("Source table %s does not contain a primary key", sourceTableIdentifier)
            throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier));
        }

        // open sql connection
        Sql sql = new Sql(connection)

        // create empty LW_ROADS
        sql.execute("drop table if exists LW_ROADS;")
        sql.execute("create table LW_ROADS (IDSOURCE integer, the_geom Geometry, " +
                "Ld63 double precision, Ld125 double precision, Ld250 double precision, Ld500 double precision, Ld1000 double precision, Ld2000 double precision, Ld4000 double precision, Ld8000 double precision," +
                "Le63 double precision, Le125 double precision, Le250 double precision, Le500 double precision, Le1000 double precision, Le2000 double precision, Le4000 double precision, Le8000 double precision," +
                "Ln63 double precision, Ln125 double precision, Ln250 double precision, Ln500 double precision, Ln1000 double precision, Ln2000 double precision, Ln4000 double precision, Ln8000 double precision);")

        def qry = 'INSERT INTO LW_ROADS(IDSOURCE,the_geom, ' +
                'Ld63, Ld125, Ld250, Ld500, Ld1000,Ld2000, Ld4000, Ld8000,' +
                'Le63, Le125, Le250, Le500, Le1000,Le2000, Le4000, Le8000,' +
                'Ln63, Ln125, Ln250, Ln500, Ln1000,Ln2000, Ln4000, Ln8000) ' +
                'VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'


        long start = System.currentTimeMillis()
        // Start
        // fill the table LW_ROADS
        sql.withBatch(100, qry) { ps ->
            PreparedStatement st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
            SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)
            while (rs.next()) {
                System.println(rs)
                Geometry geo = rs.getGeometry()
                def results = computeLw(rs.getLong(pkIndex), geo, rs)

                ps.addBatch(rs.getLong(pkIndex) as Integer,geo as Geometry,
                        results[0][0] as Double, results[0][1] as Double, results[0][2] as Double,
                        results[0][3] as Double, results[0][4]as Double, results[0][5] as Double,
                        results[0][6]as Double, results[0][7] as Double,
                        results[1][0] as Double, results[1][1] as Double, results[1][2] as Double,
                        results[1][3] as Double, results[1][4]as Double, results[1][5] as Double,
                        results[1][6]as Double, results[1][7] as Double,
                        results[2][0] as Double, results[2][1] as Double, results[2][2] as Double,
                        results[2][3] as Double, results[2][4]as Double, results[2][5] as Double,
                        results[2][6]as Double, results[2][7] as Double)
            }
        }

        sql.execute("UPDATE LW_ROADS SET THE_GEOM = ST_UPDATEZ(The_geom,0.05);")
        sql.execute("ALTER TABLE LW_ROADS ADD pk INT AUTO_INCREMENT PRIMARY KEY;" )
        long computationTime = System.currentTimeMillis() - start;
        output = "The Table LW_ROADS have been created"
        return [result: output]


    }

}

static double[][] computeLw(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

    double tvD = rs.getDouble("TV_D")
    double tvE = rs.getDouble("TV_E")
    double tvN = rs.getDouble("TV_N")

    double hvD = rs.getDouble("HV_D")
    double hvE = rs.getDouble("HV_E")
    double hvN = rs.getDouble("HV_N")

    double lvSpeedD = rs.getDouble("LV_SPD_D")
    double lvSpeedE = rs.getDouble("LV_SPD_E")
    double lvSpeedN = rs.getDouble("LV_SPD_N")

    double hvSpeedD = rs.getDouble("HV_SPD_D")
    double hvSpeedE = rs.getDouble("HV_SPD_E")
    double hvSpeedN = rs.getDouble("HV_SPD_N")

    // Annual Average Daily Flow (AADF) estimates
    String pavement = rs.getString("PVMT");

    int LDAY_START_HOUR = 6
    int LDAY_STOP_HOUR = 18
    int LEVENING_STOP_HOUR = 22
    int[] nightHours=[22, 23, 0, 1, 2, 3, 4, 5]

    // Compute day average level
    double[] ld = new double[PropagationProcessPathData.freq_lvl.size()];
    double[] le = new double[PropagationProcessPathData.freq_lvl.size()];
    double[] ln = new double[PropagationProcessPathData.freq_lvl.size()];

    double Temperature = 20.0d
    double Ts_stud = 0
    double Pm_stud = 0
    double Junc_dist = 0
    int Junc_type = 0

    for (int h = LDAY_START_HOUR; h < LDAY_STOP_HOUR; h++) {
        int idFreq = 0
        for (int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                    lvSpeedD, Math.max(0, tvD - hvD), hvD, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }
    // Average
    for (int i = 0; i < ld.length; i++) {
        ld[i] = ld[i] / (LDAY_STOP_HOUR - LDAY_START_HOUR);
    }

    // Evening
    for (int h = LDAY_STOP_HOUR; h < LEVENING_STOP_HOUR; h++) {
         int idFreq = 0
        for(int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                    lvSpeedE, Math.max(0, tvE - hvE), hvE, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }

    for(int i=0; i<le.size(); i++) {
        le[i] = (le[i] / (LEVENING_STOP_HOUR - LDAY_STOP_HOUR))
    }

    // Night
    for (int h : nightHours) {
        int idFreq = 0
        for(int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                    lvSpeedN, Math.max(0, tvN - hvN), hvN, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }
    for(int i=0; i<ln.size(); i++) {
        ln[i] = (ln[i] / nightHours.length)
    }

    return [ld,le,ln]
}
