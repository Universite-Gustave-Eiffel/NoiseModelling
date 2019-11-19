package org.noise_planet.noisemodelling.wps.NoiseModelling;

/*
 * @Author Pierre Aumond
 */

import groovy.sql.BatchingPreparedStatementWrapper
import org.h2.Driver
import org.h2gis.functions.factory.H2GISFunctions
import org.h2gis.functions.io.shp.SHPWrite
import org.h2gis.functions.spatial.convert.ST_Force3D
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

import java.sql.DriverManager


import geoserver.GeoServer
import geoserver.catalog.Store

import org.h2gis.api.ProgressVisitor
import org.geotools.jdbc.JDBCDataStore

import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos


import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.utilities.wrapper.*


import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap


import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry


import java.sql.SQLException
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.propagation.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream


title = 'Compute MultiRuns'
description = 'Compute MultiRuns.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', type: String.class],
          receiversTableName: [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName      : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          groundTableName   : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          multirunFilePath  : [name: 'multirunFilePath', title: 'multirunFilePath', type: String.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]


def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // -------------------
    // Get inputs
    // -------------------

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    String receivers_table_name = "RECEIVERS"
    if (input['receiversTableName']) {
        receivers_table_name = input['receiversTableName']
    }
    receivers_table_name = receivers_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
    }
    building_table_name = building_table_name.toUpperCase()

    String dem_table_name = ""
    if (input['demTableName']) {
        building_table_name = input['demTableName']
    }
    dem_table_name = dem_table_name.toUpperCase()

    String ground_table_name = ""
    if (input['groundTableName']) {
        ground_table_name = input['groundTableName']
    }
    ground_table_name = ground_table_name.toUpperCase()

    int n_thread = 1
    if (input['threadNumber']) {
        n_thread = Integer.valueOf(input['threadNumber'])
    }

    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Get name of the database
    String multirun_File_Path = input['multirunFilePath']

    int reflexion_order = 0
    double max_src_dist = 200
    double max_ref_dist = 50
    double wall_alpha = 0.1
    boolean compute_vertical_diffraction = false
    boolean compute_horizontal_diffraction = false

    // ----------------------------------
    // Start... 
    // ----------------------------------

    System.out.println("Run ...")

    List<ComputeRaysOut.verticeSL> allLevels = new ArrayList<>()

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->

        //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postgis database
        connection = new ConnectionWrapper(connection)

        // Init output logger
        Logger logger = LoggerFactory.getLogger(SensitivityProcess.class)
        logger.info(String.format("Working directory is %s", new File(workingDir).getAbsolutePath()))

        // Create spatial database
        //TimeZone tz = TimeZone.getTimeZone("UTC")
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        //df.setTimeZone(tz)
        //String dbName = new File("database2").toURI()
        //Connection connection = SFSUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true))
        Sql sql = new Sql(connection)

        File dest2 = new File("D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\data\\Exp_compICA2m.csv")

        int nvar = 0 // pas toucher
        int nr = 0 // pas toucher
        int nSimu = 0 // ne pas toucher
        int n_comp = 0 // pas toucher
        int i_read = 1   //nombre de lignes d'entête

        // lire les 4 premieres lignes de config
        new File("D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\data\\ConfigICA2.csv").splitEachLine(",") {
            fields ->
                switch (i_read) {
                    case 1:
                        nvar = (int) fields[0 as String]
                    case 2:
                        nr = (int) fields[0 as String]
                    case 3:
                        nSimu = (int) fields[0 as String]
                    case 4:
                        n_comp = (int) fields[0 as String]
                    default: break
                }
                i_read = i_read + 1
        }

        // Evaluate receiver points using provided buildings
        logger.info("Read Sources")
        sql.execute("DROP TABLE IF EXISTS RECEIVERS")
        SHPRead.readShape(connection, "data/RecepteursQuest3D.shp", "RECEIVERS")

        HashMap<Integer, Double> pop = new HashMap<>()
        // memes valeurs d e et n
        sql.eachRow('SELECT id, pop FROM RECEIVERS;') { row ->
            int id = (int) row[0 as String]
            pop.put(id, (Double) row[1 as String])
        }

        // Load roads
        logger.info("Read road geometries and traffic")
        // ICA 2019 - Sensitivity
        SHPRead.readShape(connection, "data/RoadsICA2.shp", "ROADS2")
        sql.execute("DROP TABLE ROADS if exists;")
        sql.execute('CREATE TABLE ROADS AS SELECT CAST( OSM_ID AS INTEGER ) OSM_ID , THE_GEOM, TMJA_D,TMJA_E,TMJA_N,\n' +
                'PL_D,PL_E,PL_N,\n' +
                'LV_SPEE,PV_SPEE, PVMT FROM ROADS2;')

        sql.execute('ALTER TABLE ROADS ALTER COLUMN OSM_ID SET NOT NULL;')
        sql.execute('ALTER TABLE ROADS ADD PRIMARY KEY (OSM_ID);')
        sql.execute("CREATE SPATIAL INDEX ON ROADS(THE_GEOM)")

        logger.info("Road file loaded")

        // ICA 2019 - Sensitivity
        SHPRead.readShape(connection, "data/Roads09083D.shp", "ROADS3")

        logger.info("Road file loaded")


        PropagationProcessPathData genericMeteoData = new PropagationProcessPathData()
        sensitivityProcessData.setSensitivityTable(dest2)
        //sensitivityProcessData.setRoadTable("ROADS3",sql,1)

        int GZIP_CACHE_SIZE = (int) Math.pow(2, 19)

        logger.info("Start time :" + df.format(new Date()))


        FileInputStream fileInputStream = new FileInputStream(new File("rays0908.gz").getAbsolutePath())
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream((fileInputStream), GZIP_CACHE_SIZE)
            DataInputStream dataInputStream = new DataInputStream(gzipInputStream)
            System.out.println("Read file and apply sensitivity analysis")
            int oldIdReceiver = -1
            int oldIdSource = -1

            FileWriter csvFile = new FileWriter(new File(workingDir, "simuICA2.csv"))
            List<double[]> simuSpectrum = new ArrayList<>()

            Map<Integer, List<double[]>> sourceLevel = new HashMap<>()

            System.out.println("Prepare Sources")
            def timeStart = System.currentTimeMillis()

            sourceLevel = sensitivityProcessData.getTrafficLevel("ROADS3", sql, nSimu)




            def timeStart2 = System.currentTimeMillis()
            System.out.println(timeStart2 - timeStart)
            logger.info("End Emission time :" + df.format(new Date()))
            System.out.println("Run SA")
            long computationTime = 0
            long startSimulationTime = System.currentTimeMillis()
            while (fileInputStream.available() > 0) {

                PointToPointPaths paths = new PointToPointPaths()
                paths.readPropagationPathListStream(dataInputStream)
                long startComputationTime = System.currentTimeMillis()
                int idReceiver = (Integer) paths.receiverId
                int idSource = (Integer) paths.sourceId

                if (idReceiver != oldIdReceiver) {
                    logger.info("Receiver: " + oldIdReceiver)
                    // Save old receiver values
                    if (oldIdReceiver != -1) {
                        for (int r = 0; r < nSimu; ++r) {
                            csvFile.append(String.format("%d\t%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\n", r, oldIdReceiver, pop.get(oldIdReceiver), simuSpectrum.get(r)[0], simuSpectrum.get(r)[1], simuSpectrum.get(r)[2], simuSpectrum.get(r)[3], simuSpectrum.get(r)[4], simuSpectrum.get(r)[5], simuSpectrum.get(r)[6], simuSpectrum.get(r)[7]))
                        }
                    }
                    // Create new receiver value
                    simuSpectrum.clear()
                    for (int r = 0; r < nSimu; ++r) {
                        simuSpectrum.add(new double[PropagationProcessPathData.freq_lvl.size()])
                    }
                }
                oldIdReceiver = idReceiver
                ComputeRaysOut out = new ComputeRaysOut(false, sensitivityProcessData.getGenericMeteoData(0))
                //double[] attenuation = out.computeAttenuation(sensitivityProcessData.getGenericMeteoData(r), idSource, paths.getLi(), idReceiver, propagationPaths)
                double[] attenuation = null
                List<PropagationPath> propagationPaths = new ArrayList<>()
                for (int r = 0; r < nSimu; r++) {
                    propagationPaths.clear()
                    for (int pP = 0; pP < paths.propagationPathList.size(); pP++) {

                        paths.propagationPathList.get(pP).initPropagationPath()
                        if (paths.propagationPathList.get(pP).refPoints.size() <= sensitivityProcessData.Refl[r]
                                && paths.propagationPathList.get(pP).difHPoints.size() <= sensitivityProcessData.Dif_hor[r]
                                && paths.propagationPathList.get(pP).difVPoints.size() <= sensitivityProcessData.Dif_ver[r]
                                && paths.propagationPathList.get(pP).SRList[0].dp <= sensitivityProcessData.DistProp[r]
                        ) {
                            propagationPaths.add(paths.propagationPathList.get(pP))
                        }
                    }


                    if (propagationPaths.size() > 0) {
                        //double[] attenuation = out.computeAttenuation(sensitivityProcessData.getGenericMeteoData(r), idSource, paths.getLi(), idReceiver, propagationPaths)
                        //double[] soundLevel = sumArray(attenuation, sourceLevel.get(idSource).get(r))
                        if (attenuation != null) {
                            if (sensitivityProcessData.TempMean[r] != sensitivityProcessData.TempMean[r - 1]
                                    || sensitivityProcessData.HumMean[r] != sensitivityProcessData.HumMean[r - 1]) {
                                attenuation = out.computeAttenuation(sensitivityProcessData.getGenericMeteoData(r), idSource, paths.getLi(), idReceiver, propagationPaths)
                            }
                        } else {
                            attenuation = out.computeAttenuation(sensitivityProcessData.getGenericMeteoData(r), idSource, paths.getLi(), idReceiver, propagationPaths)
                        }

                        double[] soundLevelDay = ComputeRays.wToDba(ComputeRays.multArray(sensitivityProcessData.wjSourcesD.get(idSource).get(r), ComputeRays.dbaToW(attenuation)))
                        double[] soundLevelEve = ComputeRays.wToDba(ComputeRays.multArray(sensitivityProcessData.wjSourcesE.get(idSource).get(r), ComputeRays.dbaToW(attenuation)))
                        double[] soundLevelNig = ComputeRays.wToDba(ComputeRays.multArray(sensitivityProcessData.wjSourcesN.get(idSource).get(r), ComputeRays.dbaToW(attenuation)))
                        double[] lDen = new double[soundLevelDay.length]
                        double[] lN = new double[soundLevelDay.length]
                        for (int i = 0; i < soundLevelDay.length; ++i) {
                            lDen[i] = 10.0D * Math.log10((12.0D / 24.0D) * Math.pow(10.0D, soundLevelDay[i] / 10.0D)
                                    + (4.0D / 24.0D) * Math.pow(10.0D, (soundLevelEve[i] + 5.0D) / 10.0D)
                                    + (8.0D / 24.0D) * Math.pow(10.0D, (soundLevelNig[i] + 10.0D) / 10.0D))
                            lN[i] = soundLevelNig[i]
                        }

                        simuSpectrum[r] = ComputeRays.sumDbArray(simuSpectrum[r], lDen)
                    }
                }
                computationTime += System.currentTimeMillis() - startComputationTime
            }

            logger.info("ComputationTime :" + computationTime.toString())
            logger.info("SimulationTime :" + (System.currentTimeMillis() - startSimulationTime).toString())

            csvFile.close()
            logger.info("End time :" + df.format(new Date()))

        } finally {

            fileInputStream.close()
        }


        long computationTime = System.currentTimeMillis() - start;

        return [result: "Calculation Done !"]


    }

}

static double[] DBToDBA(double[] db) {
    double[] dbA = [-26.2, -16.1, -8.6, -3.2, 0, 1.2, 1.0, -1.1]
    for (int i = 0; i < db.length; ++i) {
        db[i] = db[i] + dbA[i]
    }
    return db

}

static double[] sumArraySR(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array");
    } else {
        double[] sum = new double[array1.length];

        for (int i = 0; i < array1.length; ++i) {
            sum[i] = (array1[i]) + (array2[i]);
        }

        return sum;
    }
}


static double[] sumArray(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array")
    } else {
        double[] sum = new double[array1.length]

        for (int i = 0; i < array1.length; ++i) {
            sum[i] = array1[i] + array2[i]
        }

        return sum
    }
}
// fonction pour Copier les fichiers dans un autre répertoire
private static void copyFileUsingStream(File source, File dest) throws IOException {
    InputStream is = null
    OutputStream os = null
    try {
        is = new FileInputStream(source)
        os = new FileOutputStream(dest)
        byte[] buffer = new byte[1024]
        int length
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length)
        }
    } finally {
        is.close()
        os.close()
    }
}


/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
class SensitivityProcessData {
    static public Map<Integer, double[]> soundSourceLevels = new HashMap<>()

    protected Map<Integer, List<double[]>> wjSourcesD = new HashMap<>()
    protected Map<Integer, List<double[]>> wjSourcesE = new HashMap<>()
    protected Map<Integer, List<double[]>> wjSourcesN = new HashMap<>()

    public static PropagationProcessPathData genericMeteoData = new PropagationProcessPathData()

    // Init des variables
    ArrayList<Integer> Simu = new ArrayList<Integer>() // numero simu
    ArrayList<Double> CalcTime = new ArrayList<Double>() // temps calcul


    ArrayList<Integer> Refl = new ArrayList<Integer>()
    ArrayList<Integer> Dif_hor = new ArrayList<Integer>()
    ArrayList<Integer> Dif_ver = new ArrayList<Integer>()
    ArrayList<Double> DistProp = new ArrayList<Double>()
    ArrayList<Integer> Veg = new ArrayList<Integer>()
    ArrayList<Integer> TMJA = new ArrayList<Integer>()
    ArrayList<Integer> PL = new ArrayList<Integer>()
    ArrayList<Integer> Speed = new ArrayList<Integer>()
    ArrayList<Integer> RoadType = new ArrayList<Integer>()
    ArrayList<Double> TempMean = new ArrayList<Double>()
    ArrayList<Double> HumMean = new ArrayList<Double>()
    ArrayList<Integer> Meteo = new ArrayList<Integer>()
    ArrayList<Double> SpeedMean = new ArrayList<Double>()
    ArrayList<Double> FlowMean = new ArrayList<Double>()


    ArrayList<Integer> pk = new ArrayList<Integer>()
    ArrayList<Integer> osm_id = new ArrayList<Integer>()
    ArrayList<Geometry> the_geom = new ArrayList<Geometry>()
    ArrayList<Double> LV_SPEE = new ArrayList<Double>()
    ArrayList<Double> PV_SPEE = new ArrayList<Double>()
    ArrayList<Double> SPEED_D = new ArrayList<Double>()
    ArrayList<Double> TMJA_D = new ArrayList<Double>()
    ArrayList<Double> TMJA_E = new ArrayList<Double>()
    ArrayList<Double> TMJA_N = new ArrayList<Double>()
    ArrayList<Double> TMJA_DD = new ArrayList<Double>()
    ArrayList<Double> TMJA_ED = new ArrayList<Double>()
    ArrayList<Double> TMJA_ND = new ArrayList<Double>()
    ArrayList<Double> PL_D = new ArrayList<Double>()
    ArrayList<Double> PL_E = new ArrayList<Double>()
    ArrayList<Double> PL_N = new ArrayList<Double>()
    ArrayList<Double> PL_DD = new ArrayList<Double>()
    ArrayList<Double> PL_ED = new ArrayList<Double>()
    ArrayList<Double> PL_ND = new ArrayList<Double>()
    ArrayList<String> PVMT = new ArrayList<String>()


    Map<Integer, List<double[]>> getTrafficLevel(String tablename, Sql sql, int nSimu) throws SQLException {
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
        //connect
        Map<Integer, List<double[]>> sourceLevel = new HashMap<>()

        // memes valeurs d e et n


        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
        sql.eachRow('SELECT pk, the_geom , OSM_ID, ' +
                'TMJA_D,TMJA_E,TMJA_N,TMJA_DD,TMJA_ED,TMJA_ND,' +
                'PL_D,PL_E,PL_N,PL_DD,PL_ED,PL_ND,' +
                ' LV_SPEE, PV_SPEE,SPEED_D, ' +
                'PVMT FROM ' + tablename + ';') { row ->

            /* pk.add((int) fields[0])
             the_geom.add((Geometry) fields[1])
             osm_id.add((int) fields[2])
             TMJA_D.add((double) fields[3])
             TMJA_E.add((double) fields[4])
             TMJA_N.add((double) fields[5])
             TMJA_DD.add((double) fields[6])
             TMJA_ED.add((double) fields[7])
             TMJA_ND.add((double) fields[8])
             PL_D.add((double) fields[9])
             PL_E.add((double) fields[10])
             PL_N.add((double) fields[11])
             PL_DD.add((double) fields[12])
             PL_ED.add((double) fields[13])
             PL_ND.add((double) fields[14])
             LV_SPEE.add((double) fields[15])
             PV_SPEE.add((double) fields[16])
             SPEED_D.add((double) fields[17])
             PVMT.add((String) fields[18])*/


            int id = (int) row[2].toInteger()
            //System.out.println("Source :" + id)
            Geometry the_geom = row[1]
            def speed_lv = (double) row[15]
            def speed_pl = (double) row[16]
            def speed_lv_d = (double) row[17]

            def lv_d_speed = (double) row[15]
            def mv_d_speed = (double) 0.0
            def hv_d_speed = (double) row[16]
            def wav_d_speed = (double) 0.0
            def wbv_d_speed = (double) 0.0
            def lv_e_speed = (double) row[15]
            def mv_e_speed = (double) 0.0
            def hv_e_speed = (double) row[16]
            def wav_e_speed = (double) 0.0
            def wbv_e_speed = (double) 0.0
            def lv_n_speed = (double) row[15]
            def mv_n_speed = (double) 0.0
            def hv_n_speed = (double) row[16]
            def wav_n_speed = (double) 0.0
            def wbv_n_speed = (double) 0.0
            def TMJAD_Ref = (double) row[3]
            def TMJAE_Ref = (double) row[4]
            def TMJAN_Ref = (double) row[5]
            def TMJAD_D = (double) row[6]
            def TMJAE_D = (double) row[7]
            def TMJAN_D = (double) row[8]
            def TMJAD = (double) row[3]
            def TMJAE = (double) row[4]
            def TMJAN = (double) row[5]
            def PLD = (double) row[9]
            def PLE = (double) row[10]
            def PLN = (double) row[11]
            def PLD_Ref = (double) row[9]
            def PLE_Ref = (double) row[10]
            def PLN_Ref = (double) row[11]
            def PLD_D = (double) row[12]
            def PLE_D = (double) row[13]
            def PLN_D = (double) row[14]



            def vl_d_per_hour = (double) (TMJAD - (PLD * TMJAD / 100))
            def ml_d_per_hour = (double) 0.0
            def pl_d_per_hour = (double) (TMJAD * PLD / 100)
            def wa_d_per_hour = (double) 0.0
            def wb_d_per_hour = (double) 0.0
            def vl_e_per_hour = (double) (TMJAE - (TMJAE * PLE / 100))
            def ml_e_per_hour = (double) 0.0
            def pl_e_per_hour = (double) (TMJAE * PLE / 100)
            def wa_e_per_hour = (double) 0.0
            def wb_e_per_hour = (double) 0.0
            def vl_n_per_hour = (double) (TMJAN - (TMJAN * PLN / 100))
            def ml_n_per_hour = (double) 0.0
            def pl_n_per_hour = (double) (TMJAN * PLN / 100)
            def wa_n_per_hour = (double) 0.0
            def wb_n_per_hour = (double) 0.0
            def Zstart = (double) 0.0
            def Zend = (double) 0.0
            def Juncdist = (double) 250.0
            def Junc_type = (int) 1
            def road_pav = (String) row[18]

            // Ici on calcule les valeurs d'emission par tronçons et par fréquence

            List<double[]> sourceLevel2 = new ArrayList<>()
            List<double[]> sl_res_d = new ArrayList<>()
            List<double[]> sl_res_e = new ArrayList<>()
            List<double[]> sl_res_n = new ArrayList<>()

            for (int r = 0; r < nSimu; ++r) {

                int kk = 0
                double[] res_d = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
                double[] res_e = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
                double[] res_n = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]

                if (PL[r]) {
                    PLD = PLD_Ref
                    PLE = PLE_Ref
                    PLN = PLN_Ref
                } else {
                    PLD = PLD_D
                    PLE = PLE_D
                    PLN = PLN_D
                }

                if (TMJA[r]) {
                    TMJAD = TMJAD_Ref
                    TMJAE = TMJAE_Ref
                    TMJAN = TMJAN_Ref

                } else {
                    TMJAD = TMJAD_D
                    TMJAE = TMJAE_D
                    TMJAN = TMJAN_D
                }

                vl_d_per_hour = (TMJAD - (PLD * TMJAD / 100))
                pl_d_per_hour = (TMJAD * PLD / 100)
                vl_e_per_hour = (TMJAE - (TMJAE * PLE / 100))
                pl_e_per_hour = (TMJAE * PLE / 100)
                vl_n_per_hour = (TMJAN - (TMJAN * PLN / 100))
                pl_n_per_hour = (TMJAN * PLN / 100)

                if (Speed[r]) {
                    lv_d_speed = speed_lv
                    hv_d_speed = speed_pl
                    lv_e_speed = speed_lv
                    hv_e_speed = speed_pl
                    lv_n_speed = speed_lv
                    hv_n_speed = speed_pl
                } else {
                    lv_d_speed = speed_lv_d
                    hv_d_speed = speed_lv_d
                    lv_e_speed = speed_lv_d
                    hv_e_speed = speed_lv_d
                    lv_n_speed = speed_lv_d
                    hv_n_speed = speed_lv_d
                }




                for (f in list) {
                    // fois 0.5 car moitié dans un sens et moitié dans l'autre
                    /*String RS = "NL01
                    "
                    switch (R_Road[r]){
                        case 1:
                            RS ="NL01"
                            break
                        case 2 :
                            RS ="NL02"
                            break
                        case 3 :
                            RS ="NL03"
                            break
                        case 4 :
                            RS ="NL04"
                            break
                    }*/
                    String RS = "NL05"
                    if (RoadType[r] == 1) {
                        RS = road_pav
                    }

                    /*"Refl",...
                    "Dif_hor",...
                    "Dif_ver ",...
                    "DistProp",...
                    "Veg",...
                    "TMJA",...
                    "PL",...
                    "Speed",...
                    "RoadType",...
                    "TempMean",...
                    "HumMean",...
                    "Meteo",...
                    "SpeedMean",...
                    "FlowMean"};*/

                    RSParametersCnossos srcParameters_d = new RSParametersCnossos(lv_d_speed * SpeedMean[r], mv_d_speed * SpeedMean[r], hv_d_speed * SpeedMean[r], wav_d_speed * SpeedMean[r], wbv_d_speed * SpeedMean[r],
                            vl_d_per_hour * FlowMean[r], ml_d_per_hour * FlowMean[r], pl_d_per_hour * FlowMean[r], wa_d_per_hour * FlowMean[r], wb_d_per_hour * FlowMean[r],
                            f, TempMean[r], RS, 0, 0, 250, 1)
                    RSParametersCnossos srcParameters_e = new RSParametersCnossos(lv_e_speed * SpeedMean[r], mv_e_speed * SpeedMean[r], hv_e_speed * SpeedMean[r], wav_e_speed * SpeedMean[r], wbv_e_speed * SpeedMean[r],
                            vl_e_per_hour * FlowMean[r], ml_e_per_hour * FlowMean[r], pl_e_per_hour * FlowMean[r], wa_e_per_hour * FlowMean[r], wb_e_per_hour * FlowMean[r],
                            f, TempMean[r], RS, 0, 0, 250, 1)
                    RSParametersCnossos srcParameters_n = new RSParametersCnossos(lv_n_speed * SpeedMean[r], mv_n_speed * SpeedMean[r], hv_n_speed * SpeedMean[r], wav_n_speed * SpeedMean[r], wbv_n_speed * Speed[r],
                            vl_n_per_hour * FlowMean[r], ml_n_per_hour * FlowMean[r], pl_n_per_hour * FlowMean[r], wa_n_per_hour * FlowMean[r], wb_n_per_hour * FlowMean[r],
                            f, TempMean[r], RS, 0, 0, 250, 1)

                    srcParameters_d.setSlopePercentage(RSParametersCnossos.computeSlope(Zstart, Zend, the_geom.getLength()))
                    srcParameters_e.setSlopePercentage(RSParametersCnossos.computeSlope(Zstart, Zend, the_geom.getLength()))
                    srcParameters_n.setSlopePercentage(RSParametersCnossos.computeSlope(Zstart, Zend, the_geom.getLength()))
                    //res_d[kk] = EvaluateRoadSourceCnossos.evaluate(srcParameters_d)
                    //res_e[kk] = EvaluateRoadSourceCnossos.evaluate(srcParameters_e)
                    //res_n[kk] = EvaluateRoadSourceCnossos.evaluate(srcParameters_n)
                    //srcParameters_d.setSlopePercentage(RSParametersCnossos.computeSlope(Zend, Zstart, the_geom.getLength()))
                    //srcParameters_e.setSlopePercentage(RSParametersCnossos.computeSlope(Zend, Zstart, the_geom.getLength()))
                    //srcParameters_n.setSlopePercentage(RSParametersCnossos.computeSlope(Zend, Zstart, the_geom.getLength()))
                    res_d[kk] = 10 * Math.log10(
                            (1.0 / 24.0) *
                                    (12 * Math.pow(10, (10 * Math.log10(Math.pow(10, EvaluateRoadSourceCnossos.evaluate(srcParameters_d) / 10))) / 10)
                                            + 4 * Math.pow(10, (10 * Math.log10(Math.pow(10, EvaluateRoadSourceCnossos.evaluate(srcParameters_e) / 10))) / 10)
                                            + 8 * Math.pow(10, (10 * Math.log10(Math.pow(10, EvaluateRoadSourceCnossos.evaluate(srcParameters_n) / 10))) / 10))
                    )
                    res_d[kk] += ComputeRays.dbaToW(EvaluateRoadSourceCnossos.evaluate(srcParameters_d))
                    res_e[kk] += ComputeRays.dbaToW(EvaluateRoadSourceCnossos.evaluate(srcParameters_e))
                    res_n[kk] += ComputeRays.dbaToW(EvaluateRoadSourceCnossos.evaluate(srcParameters_n))

                    kk++
                }
                sl_res_d.add(res_d)
                sl_res_e.add(res_e)
                sl_res_n.add(res_n)
                sourceLevel2.add(res_d)
            }
            wjSourcesD.put(id, sl_res_d)
            wjSourcesE.put(id, sl_res_e)
            wjSourcesN.put(id, sl_res_n)
            sourceLevel.put(id, sourceLevel2)
        }
        return sourceLevel
    }

    PropagationProcessPathData getGenericMeteoData(int r) {
        genericMeteoData.setHumidity(HumMean[r])
        genericMeteoData.setTemperature(TempMean[r])
        return genericMeteoData
    }


    void setSensitivityTable(File file) {
        //////////////////////
        // Import file text
        //////////////////////
        int i_read = 0
        // Remplissage des variables avec le contenu du fichier plan d'exp
        file.splitEachLine(",") { fields ->

            Refl.add(fields[0].toInteger())
            Dif_hor.add(fields[1].toInteger())
            Dif_ver.add(fields[2].toInteger())
            DistProp.add(fields[3].toFloat())
            Veg.add(fields[4].toInteger())
            TMJA.add(fields[5].toInteger())
            PL.add(fields[6].toInteger())
            Speed.add(fields[7].toInteger())
            RoadType.add(fields[8].toInteger())
            TempMean.add(fields[9].toFloat())
            HumMean.add(fields[10].toFloat())
            Meteo.add(fields[11].toInteger())
            SpeedMean.add(fields[12].toFloat())
            FlowMean.add(fields[13].toFloat())

            Simu.add(fields[14].toInteger())

            i_read = i_read + 1
        }

    }


    void setRoadTable(String tablename, Sql sql, int nSimu) {
        //////////////////////
        // Import file text
        //////////////////////


        sql.eachRow('SELECT pk, osm_id, the_geom , OSM_ID, TMJA_D,TMJA_E,TMJA_N,TMJA_DD,TMJA_ED,TMJA_ND,PL_D,PL_E,PL_N,PL_DD,PL_ED,PL_ND, LV_SPEE, PV_SPEE,SPEED_D, PVMT FROM ' + tablename + ';') { fields ->

            pk.add((int) fields[0])
            the_geom.add(fields[1])
            osm_id.add((int) fields[2])
            TMJA_D.add((double) fields[3])
            TMJA_E.add((double) fields[4])
            TMJA_N.add((double) fields[5])
            TMJA_DD.add((double) fields[6])
            TMJA_ED.add((double) fields[7])
            TMJA_ND.add((double) fields[8])
            PL_D.add((double) fields[9])
            PL_E.add((double) fields[10])
            PL_N.add((double) fields[11])
            PL_DD.add((double) fields[12])
            PL_ED.add((double) fields[13])
            PL_ND.add((double) fields[14])
            LV_SPEE.add((double) fields[15])
            PV_SPEE.add((double) fields[16])
            SPEED_D.add((double) fields[17])
            PVMT.add((String) fields[18])

        }

    }

}


class DbUtilities {


    private static String getDataBasePath(String dbName) {
        return dbName.startsWith("file:/") ? (new File(URI.create(dbName))).getAbsolutePath() : (new File(dbName)).getAbsolutePath()
    }


    static Connection createSpatialDataBase(String dbName, boolean initSpatial) throws SQLException, ClassNotFoundException {
        String dbFilePath = getDataBasePath(dbName);
        File dbFile = new File(dbFilePath + ".mv.db")

        String databasePath = "jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5"

        if (dbFile.exists()) {
            dbFile.delete()
        }

        dbFile = new File(dbFilePath + ".mv.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
        Driver.load()
        Connection connection = DriverManager.getConnection(databasePath, "sa", "sa")
        if (initSpatial) {
            H2GISFunctions.load(connection)
        }

        return connection
    }

    @CompileStatic
    static void createReceiversFromBuildings(Sql sql, String buildingName, String areaTable) {
        sql.execute("DROP TABLE IF EXISTS GLUED_BUILDINGS")
        sql.execute("CREATE TABLE GLUED_BUILDINGS AS SELECT ST_UNION(ST_ACCUM(ST_BUFFER(B.THE_GEOM, 2.0,'endcap=square join=bevel'))) the_geom FROM " + buildingName + " B, " + areaTable + " A WHERE A.THE_GEOM && B.THE_GEOM AND ST_INTERSECTS(A.THE_GEOM, B.THE_GEOM)")
        Logger logger = LoggerFactory.getLogger("test")
        sql.execute("DROP TABLE IF EXISTS RECEIVERS")
        sql.execute("CREATE TABLE RECEIVERS(pk serial, the_geom GEOMETRY)")
        boolean pushed = false
        sql.withTransaction {
            sql.withBatch("INSERT INTO receivers(the_geom) VALUES (ST_MAKEPOINT(:px, :py, :pz))") { BatchingPreparedStatementWrapper batch ->
                sql.eachRow("SELECT THE_GEOM FROM ST_EXPLODE('GLUED_BUILDINGS')") {
                    row ->
                        List<Coordinate> receivers = new ArrayList<>();
                        ComputeRays.splitLineStringIntoPoints((LineString) ST_Force3D.force3D(((Polygon) row["the_geom"]).exteriorRing), 5.0d, receivers)
                        for (Coordinate p : receivers) {
                            p.setOrdinate(2, 4.0d)
                            batch.addBatch([px: p.x, py: p.y, pz: p.z])
                            pushed = true
                        }

                }
                if (pushed) {
                    batch.executeBatch()
                    pushed = false
                }
            }
        }
        SHPWrite.exportTable(sql.getConnection(), "data/receivers.shp", "RECEIVERS")
        sql.execute("DROP TABLE GLUED_BUILDINGS")
    }
}
