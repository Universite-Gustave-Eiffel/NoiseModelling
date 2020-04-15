/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

title = 'Compute road emission noise map from road table.'
description = 'Compute Road Emission Noise Map from Day Evening Night traffic flow rate and speed estimates (specific format, see input details). ' +
        '</br> </br> <b> The output table is called : LW_ROADS </b> '

inputs = [tableRoads: [name: 'Roads table name', title: 'Roads table name', description: "<b>Name of the Roads table.</b>  </br>  " +
        "<br>  The table shall contain : </br>" +
        "- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)<br/>" +
        "- <b> TV_D </b> : Hourly average light and heavy vehicle count (6-18h) (DOUBLE)<br/>" +
        "- <b>TV_E </b> :  Hourly average light and heavy vehicle count (18-22h) (DOUBLE)<br/>" +
        "- <b> TV_N </b> :  Hourly average light and heavy vehicle count (22-6h) (DOUBLE)<br/>" +
        "- <b> HV_D </b> :  Hourly average heavy vehicle count (6-18h) (DOUBLE)<br/>" +
        "- <b> HV_E </b> :  Hourly average heavy vehicle count (18-22h) (DOUBLE)<br/>" +
        "- <b> HV_N </b> :  Hourly average heavy vehicle count (22-6h) (DOUBLE)<br/>" +
        "- <b> LV_SPD_D </b> :  Hourly average light vehicle speed (6-18h) (DOUBLE)<br/>" +
        "- <b> LV_SPD_E </b> :  Hourly average light vehicle speed (18-22h) (DOUBLE)<br/>" +
        "- <b> LV_SPD_N </b> :  Hourly average light vehicle speed (22-6h) (DOUBLE)<br/>" +
        "- <b> HV_SPD_D </b> :  Hourly average heavy vehicle speed (6-18h) (DOUBLE)<br/>" +
        "- <b> HV_SPD_E </b> :  Hourly average heavy vehicle speed (18-22h) (DOUBLE)<br/>" +
        "- <b> HV_SPD_N </b> :  Hourly average heavy vehicle speed (22-6h) (DOUBLE)<br/>" +
        "- <b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)" +
        "</br> </br> <b> This table can be generated from the WPS Block 'OsmToInputData'. </b>.", type: String.class]]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]

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
def exec(Connection connection, input) {

    //Load GeneralTools.groovy
    File generalTools = new File(new File("").absolutePath+"/data_dir/scripts/wpsTools/GeneralTools.groovy")

    //if we are in dev, the path is not the same as for geoserver
    if (new File("").absolutePath.substring(new File("").absolutePath.length() - 11) == 'wps_scripts') {
        generalTools = new File(new File("").absolutePath+"/src/main/groovy/org/noise_planet/noisemodelling/wpsTools/GeneralTools.groovy")
     }

    // Get external tools
    Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(generalTools)
    GroovyObject tools = (GroovyObject) groovyClass.newInstance()


    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Road Emission from DEN')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['tableRoads'] as String
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()

    //Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name)
    List<String> geomFields = SFSUtilities.getGeometryFields(connection, sourceTableIdentifier)
    if (geomFields.isEmpty()) {
        resultString = String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier)
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }

    //Get the primary key field of the source table
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, sources_table_name)
    if (pkIndex < 1) {
        resultString = String.format("Source table %s does not contain a primary key", sourceTableIdentifier)
        throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier))
    }


    // -------------------
    // Init table LW_ROADS
    // -------------------

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // drop table LW_ROADS if exists and the create and prepare the table
    sql.execute("drop table if exists LW_ROADS;")
    sql.execute("create table LW_ROADS (pk integer, the_geom Geometry, " +
            "LWD63 double precision, LWD125 double precision, LWD250 double precision, LWD500 double precision, LWD1000 double precision, LWD2000 double precision, LWD4000 double precision, LWD8000 double precision," +
            "LWE63 double precision, LWE125 double precision, LWE250 double precision, LWE500 double precision, LWE1000 double precision, LWE2000 double precision, LWE4000 double precision, LWE8000 double precision," +
            "LWN63 double precision, LWN125 double precision, LWN250 double precision, LWN500 double precision, LWN1000 double precision, LWN2000 double precision, LWN4000 double precision, LWN8000 double precision);")

    def qry = 'INSERT INTO LW_ROADS(pk,the_geom, ' +
            'LWD63, LWD125, LWD250, LWD500, LWD1000,LWD2000, LWD4000, LWD8000,' +
            'LWE63, LWE125, LWE250, LWE500, LWE1000,LWE2000, LWE4000, LWE8000,' +
            'LWN63, LWN125, LWN250, LWN500, LWN1000,LWN2000, LWN4000, LWN8000) ' +
            'VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'


    // --------------------------------------
    // Start calculation and fill the table
    // --------------------------------------

    // Get Class to compute LW
    WpsPropagationProcessDataDENFactory wpsPropagationProcessDataDENFactory =  new WpsPropagationProcessDataDENFactory()


    // Get size of the table (number of road segments
    PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) AS total FROM " + sources_table_name)
    ResultSet rs1 = st.executeQuery().unwrap(ResultSet.class)
    int nbRoads = 0
    while (rs1.next()) {
        nbRoads = rs1.getInt("total")
        System.println('The table Roads has ' + nbRoads + ' road segments.')
    }
    //System.println('The table Roads has ' + nbRoads + ' road segments.')
    int k = 0
    int currentVal = 0
    sql.withBatch(100, qry) { ps ->
        st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
        SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

        while (rs.next()) {
            k++
            currentVal = tools.invokeMethod("ProgressBar", [Math.round(10*k/nbRoads).toInteger(),currentVal])
            //System.println(rs)
            Geometry geo = rs.getGeometry()

            // Compute emission sound level for each road segment
            def results = computeLw("Classic".toString(),rs)

            // fill the LW_ROADS table
            ps.addBatch(rs.getLong(pkIndex) as Integer, geo as Geometry,
                    results[0][0] as Double, results[0][1] as Double, results[0][2] as Double,
                    results[0][3] as Double, results[0][4] as Double, results[0][5] as Double,
                    results[0][6] as Double, results[0][7] as Double,
                    results[1][0] as Double, results[1][1] as Double, results[1][2] as Double,
                    results[1][3] as Double, results[1][4] as Double, results[1][5] as Double,
                    results[1][6] as Double, results[1][7] as Double,
                    results[2][0] as Double, results[2][1] as Double, results[2][2] as Double,
                    results[2][3] as Double, results[2][4] as Double, results[2][5] as Double,
                    results[2][6] as Double, results[2][7] as Double)
        }
    }

    // Add Z dimension to the road segments
    sql.execute("UPDATE LW_ROADS SET THE_GEOM = ST_UPDATEZ(The_geom,0.05);")
    // Add primary key to the road table
    sql.execute("ALTER TABLE LW_ROADS ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE LW_ROADS ADD PRIMARY KEY (PK);  ")

    resultString = "Calculation Done ! The table LW_ROADS has been created."

    // print to command window
    System.out.println('\nResult : ' + resultString)
    System.out.println('End : LW_ROADS from Emission')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}

double[][] computeLw(String Format, SpatialResultSet rs) throws SQLException {

    // Compute day average level
    double[] ld = new double[PropagationProcessPathData.freq_lvl.size()]
    double[] le = new double[PropagationProcessPathData.freq_lvl.size()]
    double[] ln = new double[PropagationProcessPathData.freq_lvl.size()]
    double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]

    if (Format == 'Proba') {
        double val = ComputeRays.dbaToW((BigDecimal) 90.0)
        ld = [val,val,val,val,val,val,val,val]
        le = [val,val,val,val,val,val,val,val]
        ln = [val,val,val,val,val,val,val,val]
    }

    if (Format == 'EmissionDEN') {
        // Read average 24h traffic
        ld = [ComputeRays.dbaToW(rs.getDouble('LWD63')),
              ComputeRays.dbaToW(rs.getDouble('LWD125')),
              ComputeRays.dbaToW(rs.getDouble('LWD250')),
              ComputeRays.dbaToW(rs.getDouble('LWD500')),
              ComputeRays.dbaToW(rs.getDouble('LWD1000')),
              ComputeRays.dbaToW(rs.getDouble('LWD2000')),
              ComputeRays.dbaToW(rs.getDouble('LWD4000')),
              ComputeRays.dbaToW(rs.getDouble('LWD8000'))]

        le = [ComputeRays.dbaToW(rs.getDouble('LWE63')),
              ComputeRays.dbaToW(rs.getDouble('LWE125')),
              ComputeRays.dbaToW(rs.getDouble('LWE250')),
              ComputeRays.dbaToW(rs.getDouble('LWE500')),
              ComputeRays.dbaToW(rs.getDouble('LWE1000')),
              ComputeRays.dbaToW(rs.getDouble('LWE2000')),
              ComputeRays.dbaToW(rs.getDouble('LWE4000')),
              ComputeRays.dbaToW(rs.getDouble('LWE8000'))]

        ln = [ComputeRays.dbaToW(rs.getDouble('LWN63')),
              ComputeRays.dbaToW(rs.getDouble('LWN125')),
              ComputeRays.dbaToW(rs.getDouble('LWN250')),
              ComputeRays.dbaToW(rs.getDouble('LWN500')),
              ComputeRays.dbaToW(rs.getDouble('LWN1000')),
              ComputeRays.dbaToW(rs.getDouble('LWN2000')),
              ComputeRays.dbaToW(rs.getDouble('LWN4000')),
              ComputeRays.dbaToW(rs.getDouble('LWN8000'))]
    }
    if (Format == 'Classic') {
        // Get input traffic data
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

        String pavement = rs.getString("PVMT")

        // this options can be activated if needed
        double Temperature = 20.0d
        double Ts_stud = 0
        double Pm_stud = 0
        double Junc_dist = 300
        int Junc_type = 0

        // Day
        int idFreq = 0
        for (int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                    lvSpeedD, Math.max(0, tvD - hvD), 0, hvD, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }

        // Evening
        idFreq = 0
        for (int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                    lvSpeedE, Math.max(0, tvE - hvE), 0, hvE, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }

        // Night
        idFreq = 0
        for (int freq : PropagationProcessPathData.freq_lvl) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                    lvSpeedN, Math.max(0, tvN - hvN), 0, hvN, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }


    }

    if (Format == "AADF") {
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
        int[] nightHours = [22, 23, 0, 1, 2, 3, 4, 5]
        double HV_PERCENTAGE = 0.1

        int idSource = 0

        idSource = idSource + 1
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
            lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
            mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
            int idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                        speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                        roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }
        }

        for (int i = 0; i < le.size(); i++) {
            le[i] = (le[i] / (LEVENING_STOP_HOUR - LDAY_STOP_HOUR))
        }

        // Night
        for (int h : nightHours) {
            lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
            mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
            int idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                        speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                        roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }
        }
        for (int i = 0; i < ln.size(); i++) {
            ln[i] = (ln[i] / nightHours.length)
        }
    }

    int idFreq = 0
    // Combine day evening night sound levels
    for (int freq : PropagationProcessPathData.freq_lvl) {
        lden[idFreq++] = (12 * ld[idFreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
    }

    return [ld, le, ln, lden]
}



/**
 * Read source database and compute the sound emission spectrum of roads sources
 * */
class WpsPropagationProcessDataDEN extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesD = new ArrayList<>()
    public List<double[]> wjSourcesE = new ArrayList<>()
    public List<double[]> wjSourcesN = new ArrayList<>()
    public List<double[]> wjSourcesDEN = new ArrayList<>()

    public Map<Long, Integer> SourcesPk = new HashMap<>()

    public String inputFormat = "EmissionDEN"
    int idSource = 0

    WpsPropagationProcessDataDEN(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)
        SourcesPk.put(pk, idSource++)


        def res = computeLw(inputFormat, rs)
        wjSourcesD.add(res[0])
        wjSourcesE.add(res[1])
        wjSourcesN.add(res[2])
        wjSourcesDEN.add(res[3])

    }

    double[][] computeLw(String Format, SpatialResultSet rs) throws SQLException {

        // Compute day average level
        double[] ld = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] le = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] ln = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]

        if (Format == 'Proba') {
            double val = ComputeRays.dbaToW((BigDecimal) 90.0)
            ld = [val,val,val,val,val,val,val,val]
            le = [val,val,val,val,val,val,val,val]
            ln = [val,val,val,val,val,val,val,val]
        }

        if (Format == 'EmissionDEN') {
            // Read average 24h traffic
            ld = [ComputeRays.dbaToW(rs.getDouble('LWD63')),
                  ComputeRays.dbaToW(rs.getDouble('LWD125')),
                  ComputeRays.dbaToW(rs.getDouble('LWD250')),
                  ComputeRays.dbaToW(rs.getDouble('LWD500')),
                  ComputeRays.dbaToW(rs.getDouble('LWD1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD8000'))]

            le = [ComputeRays.dbaToW(rs.getDouble('LWE63')),
                  ComputeRays.dbaToW(rs.getDouble('LWE125')),
                  ComputeRays.dbaToW(rs.getDouble('LWE250')),
                  ComputeRays.dbaToW(rs.getDouble('LWE500')),
                  ComputeRays.dbaToW(rs.getDouble('LWE1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE8000'))]

            ln = [ComputeRays.dbaToW(rs.getDouble('LWN63')),
                  ComputeRays.dbaToW(rs.getDouble('LWN125')),
                  ComputeRays.dbaToW(rs.getDouble('LWN250')),
                  ComputeRays.dbaToW(rs.getDouble('LWN500')),
                  ComputeRays.dbaToW(rs.getDouble('LWN1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN8000'))]
        }
        if (Format == 'Classic') {
            // Get input traffic data
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

            String pavement = rs.getString("PVMT")

            // this options can be activated if needed
            double Temperature = 20.0d
            double Ts_stud = 0
            double Pm_stud = 0
            double Junc_dist = 300
            int Junc_type = 0

            // Day
            int idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                        lvSpeedD, Math.max(0, tvD - hvD), 0, hvD, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }

            // Evening
            idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                        lvSpeedE, Math.max(0, tvE - hvE), 0, hvE, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }

            // Night
            idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                        lvSpeedN, Math.max(0, tvN - hvN), 0, hvN, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }


        }

        if (Format == "AADF") {
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
            int[] nightHours = [22, 23, 0, 1, 2, 3, 4, 5]
            double HV_PERCENTAGE = 0.1

            int idSource = 0

            idSource = idSource + 1
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
                lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
                mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
                int idFreq = 0
                for (int freq : PropagationProcessPathData.freq_lvl) {
                    RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                            speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                            roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                    rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                    le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
                }
            }

            for (int i = 0; i < le.size(); i++) {
                le[i] = (le[i] / (LEVENING_STOP_HOUR - LDAY_STOP_HOUR))
            }

            // Night
            for (int h : nightHours) {
                lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
                mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
                int idFreq = 0
                for (int freq : PropagationProcessPathData.freq_lvl) {
                    RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                            speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                            roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                    rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                    ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
                }
            }
            for (int i = 0; i < ln.size(); i++) {
                ln[i] = (ln[i] / nightHours.length)
            }
        }

        int idFreq = 0
        // Combine day evening night sound levels
        for (int freq : PropagationProcessPathData.freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }

        return [ld, le, ln, lden]
    }


    @Override
    double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }
}

class WpsPropagationProcessDataDENFactory implements PointNoiseMap.PropagationProcessDataFactory {



    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new WpsPropagationProcessDataDEN(freeFieldFinder)
    }
}
