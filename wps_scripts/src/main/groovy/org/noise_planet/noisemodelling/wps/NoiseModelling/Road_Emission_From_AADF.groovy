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
description = 'Compute Road Emission Noise Map from Estimated Annual average daily flows (AADF) estimates. ' +
        'This block allows to calculate a road traffic noise emission map ' +
        'from the AADF estimates given in the ROADS.shp file of the tutorial.' +
        'The average traffic is first converted to hourly traffic before the calculation of Lday, Levening and Lnight using' +
        'distribution in Berengier et al., 2019 : "DEUFRABASE: A Simple Tool for the Evaluation of the Noise Impact of ' +
        'Pavements in Typical Road Geometries".'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', type: String.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]

class Globals {
    static double[] wjSourcesD = new double[PropagationProcessPathData.freq_lvl.size()]
    static double[] wjSourcesE =new double[PropagationProcessPathData.freq_lvl.size()]
    static double[] wjSourcesN = new double[PropagationProcessPathData.freq_lvl.size()]
}

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
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
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // ----------------------------------
    // Start... 
    // ----------------------------------

    System.out.println("Run ...")

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->

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
        System.out.println("Start ...")
        // fill the table LW_ROADS
        sql.withBatch(100, qry) { ps ->
            PreparedStatement st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
            SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)
            while (rs.next()) {
                System.println(rs)
                Geometry geo = rs.getGeometry()
                computeLw(rs.getLong(pkIndex), geo, rs)
                System.println(Globals.wjSourcesD[0])
                ps.addBatch(rs.getLong(pkIndex) as Integer,geo as Geometry,
                        Globals.wjSourcesD[0] as Double, Globals.wjSourcesD[1] as Double, Globals.wjSourcesD[2] as Double,
                        Globals.wjSourcesD[3] as Double, Globals.wjSourcesD[4]as Double, Globals.wjSourcesD[5] as Double,
                        Globals.wjSourcesD[6]as Double, Globals.wjSourcesD[7] as Double,
                        Globals.wjSourcesE[0] as Double, Globals.wjSourcesE[1] as Double, Globals.wjSourcesE[2] as Double,
                        Globals.wjSourcesE[3] as Double, Globals.wjSourcesE[4]as Double, Globals.wjSourcesE[5] as Double,
                        Globals.wjSourcesE[6]as Double, Globals.wjSourcesE[7] as Double,
                        Globals.wjSourcesN[0] as Double, Globals.wjSourcesN[1] as Double, Globals.wjSourcesN[2] as Double,
                        Globals.wjSourcesN[3] as Double, Globals.wjSourcesN[4]as Double, Globals.wjSourcesN[5] as Double,
                        Globals.wjSourcesN[6]as Double, Globals.wjSourcesN[7] as Double)
            }
        }

        sql.execute("UPDATE LW_ROADS SET THE_GEOM = ST_UPDATEZ(The_geom,0.05);")
        long computationTime = System.currentTimeMillis() - start;
        output = "The Table LW_ROADS have been created"
        return [result: output]


    }

}

static void computeLw(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

    String AAFD_FIELD_NAME = "AADF"

    // Annual Average Daily Flow (AADF) estimates
    String ROAD_CATEGORY_FIELD_NAME = "CLAS_ADM"
    def lv_hourly_distribution = [0.56, 0.3, 0.21, 0.26, 0.69, 1.8, 4.29, 7.56, 7.09, 5.5, 4.96, 5.04,
                                  5.8, 6.08, 6.23, 6.67, 7.84, 8.01, 7.12, 5.44, 3.45, 2.26, 1.72, 1.12];
    def hv_hourly_distribution = [1.01, 0.97, 1.06, 1.39, 2.05, 3.18, 4.77, 6.33, 6.72, 7.32, 7.37, 7.4,
                                  6.16, 6.22, 6.84, 6.74, 6.23, 4.88, 3.79, 3.05, 2.36, 1.76, 1.34, 1.07];

    int LDAY_START_HOUR = 6
    int LDAY_STOP_HOUR = 18
    int LEVENING_STOP_HOUR = 22
    int[] nightHours=[22, 23, 0, 1, 2, 3, 4, 5]
    double HV_PERCENTAGE = 0.1

    def idSource = 0

    idSource = idSource +1
    // Read average 24h traffic
    double tmja = rs.getDouble(AAFD_FIELD_NAME)

    //130 km/h 1:Autoroute
    //80 km/h  2:Nationale
    //50 km/h  3:Départementale
    //50 km/h  4:Voirie CUN
    //50 km/h  5:Inconnu
    //50 km/h  6:Privée
    //50 km/h  7:Communale
    int road_cat = rs.getInt(ROAD_CATEGORY_FIELD_NAME)

    int roadType;
    if (road_cat == 1) {
        roadType = 10;
    } else {
        if (road_cat == 2) {
            roadType = 42;
        } else {
            roadType = 62;
        }
    }
    double speed_lv = 50;
    if (road_cat == 1) {
        speed_lv = 120;
    } else {
        if (road_cat == 2) {
            speed_lv = 80;
        }
    }

    /**
     * Vehicles category Table 3 P.31 CNOSSOS_EU_JRC_REFERENCE_REPORT
     * lv : Passenger cars, delivery vans ≤ 3.5 tons, SUVs , MPVs including trailers and caravans
     * mv: Medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle
     * hgv: Heavy duty vehicles, touring cars, buses, with three or more axles
     * wav:  mopeds, tricycles or quads ≤ 50 cc
     * wbv:  motorcycles, tricycles or quads > 50 cc
     * @param lv_speed Average light vehicle speed
     * @param mv_speed Average medium vehicle speed
     * @param hgv_speed Average heavy goods vehicle speed
     * @param wav_speed Average light 2 wheels vehicle speed
     * @param wbv_speed Average heavy 2 wheels vehicle speed
     * @param lvPerHour Average light vehicle per hour
     * @param mvPerHour Average heavy vehicle per hour
     * @param hgvPerHour Average heavy vehicle per hour
     * @param wavPerHour Average heavy vehicle per hour
     * @param wbvPerHour Average heavy vehicle per hour
     * @param FreqParam Studied Frequency
     * @param Temperature Temperature (Celsius)
     * @param roadSurface roadSurface empty default, NL01 FR01 ..
     * @param Ts_stud A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres and during .
     * @param Pm_stud Average proportion of vehicles equipped with studded tyres
     * @param Junc_dist Distance to junction
     * @param Junc_type Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     */
    // Compute day average level
    double[] ld = new double[PropagationProcessPathData.freq_lvl.size()];
    double[] le = new double[PropagationProcessPathData.freq_lvl.size()];
    double[] ln = new double[PropagationProcessPathData.freq_lvl.size()];

    double lvPerHour = 0;
    double mvPerHour = 0;
    double hgvPerHour = 0;
    double wavPerHour = 0;
    double wbvPerHour = 0;
    double Temperature = 20.0d;
    String roadSurface = "FR_R2";
    double Ts_stud = 0.5;
    double Pm_stud = 4;
    double Junc_dist = 0;
    int Junc_type = 0;
    double slopePercentage = 0;
    double speedLv = speed_lv;
    double speedMv = speed_lv;
    double speedHgv = speed_lv;
    double speedWav = speed_lv;
    double speedWbv = speed_lv;

    for (int h = LDAY_START_HOUR; h < LDAY_STOP_HOUR; h++) {
        lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0);
        hgvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0);
        int idFreq = 0;
        for (int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                    speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                    roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType);
            ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }
    // Average
    for (int i = 0; i < ld.length; i++) {
        ld[i] = ld[i] / (LDAY_STOP_HOUR - LDAY_START_HOUR);
    }

    // Evening
    for (int h = LDAY_STOP_HOUR; h < LEVENING_STOP_HOUR; h++) {
        lvPerHour = tmja * (1- HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
        mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
        int idFreq = 0
        for(int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                    speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                    roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
            le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }

    for(int i=0; i<le.size(); i++) {
        le[i] = (le[i] / (LEVENING_STOP_HOUR - LDAY_STOP_HOUR))
    }

    // Night
    for (int h : nightHours) {
        lvPerHour = tmja * (1- HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
        mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
        int idFreq = 0
        for(int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                    speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                    roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
            rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
            ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }
    }
    for(int i=0; i<ln.size(); i++) {
        ln[i] = (ln[i] / nightHours.length)
    }

    Globals.wjSourcesD = ld
    Globals.wjSourcesE = le
    Globals.wjSourcesN = ln
}


