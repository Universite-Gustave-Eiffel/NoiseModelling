package org.noise_planet.noisemodelling.wps.NoiseModelling

/*
 * @Author Pierre Aumond
 */

import geoserver.catalog.Store
import geoserver.GeoServer
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut
import org.noise_planet.noisemodelling.propagation.PropagationPath
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import java.sql.Connection
import java.sql.SQLException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

title = 'Compute MultiRuns'
description = 'Compute MultiRuns.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          workingDir : [name: 'workingDir', title: 'workingDir', description: 'workingDir (ex : C:/Desktop/)', type: String.class],
          nSimu  : [name: 'nSimu', title: 'nSimu',min: 0, max: 1, type: Integer.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]


def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    SensitivityProcessData sensitivityProcessData = new SensitivityProcessData()

    // -------------------
    // Get inputs
    // -------------------
    String workingDir = "D:\\aumond\\Documents\\Boulot\\Articles\\2019_XX_XX Sensitivity"
    if (input['workingDir']) {
        workingDir = input['workingDir']
    }

    int n_Simu = null
    if (input['nSimu']) {
        n_Simu = Integer.valueOf(input['nSimu'])
    }

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

        System.out.println("Run ...")
        // Init output logger

        System.out.println(String.format("Working directory is %s", new File(workingDir).getAbsolutePath()))

        // Create spatial database

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

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
                        nvar = (int) fields[0].toInteger()
                    case 2:
                        nr = (int) fields[0].toInteger()
                    case 3:
                        nSimu = (int) fields[0].toInteger()
                    case 4:
                        n_comp = (int) fields[0].toInteger()
                    default: break
                }
                i_read = i_read + 1
        }

        if (n_Simu != null)   nSimu = n_Simu

        // Evaluate receiver points using provided buildings
        String fileZip = workingDir +"Rays.zip"
        FileInputStream fileInputStream = new FileInputStream(new File(fileZip).getAbsolutePath())
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)
        ZipEntry entry = zipInputStream.getNextEntry()
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = workingDir + File.separator + entry.getName()
            GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
            switch (entry.getName()) {
                case 'buildings.geojson':
                    sql.execute("DROP TABLE BUILDINGS if exists;")
                    extractFile(zipInputStream, filePath)
                    geoJsonDriver.importFile(connection, 'BUILDINGS', new File(filePath), new EmptyProgressVisitor())
                    System.println("import Buildings")
                    break
                case 'sources.geojson':
                    sql.execute("DROP TABLE SOURCES if exists;")
                    extractFile(zipInputStream, filePath)
                    geoJsonDriver.importFile(connection, 'SOURCES', new File(filePath), new EmptyProgressVisitor())
                    System.println("import Sources")
                    break
                case 'receivers.geojson':
                    sql.execute("DROP TABLE ROADS if exists;")
                    extractFile(zipInputStream, filePath)
                    geoJsonDriver.importFile(connection, 'ROADS', new File(filePath), new EmptyProgressVisitor())
                    System.println("import Roads")
                    break
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.getNextEntry()
        }

        fileInputStream.close()
        zipInputStream.close()


       /* System.out.println("Read Sources")
        sql.execute("DROP TABLE IF EXISTS RECEIVERS")
        SHPRead.readShape(connection, "D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\data\\RecepteursQuest3D.shp", "RECEIVERS")

        HashMap<Integer, Double> pop = new HashMap<>()
        // memes valeurs d e et n
        sql.eachRow('SELECT id, pop FROM RECEIVERS;') { row ->
            int id = (int) row[0]
            pop.put(id, (Double) row[1])
        }

        // Load roads
        System.out.println("Read road geometries and traffic")
        // ICA 2019 - Sensitivity
        sql.execute("DROP TABLE ROADS2 if exists;")
        SHPRead.readShape(connection, "D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\data\\RoadsICA2.shp", "ROADS2")
        sql.execute("DROP TABLE ROADS if exists;")
        sql.execute('CREATE TABLE ROADS AS SELECT CAST( OSM_ID AS INTEGER ) OSM_ID , THE_GEOM, TMJA_D,TMJA_E,TMJA_N,\n' +
                'PL_D,PL_E,PL_N,\n' +
                'LV_SPEE,PV_SPEE, PVMT FROM ROADS2;')

        sql.execute('ALTER TABLE ROADS ALTER COLUMN OSM_ID SET NOT NULL;')
        sql.execute('ALTER TABLE ROADS ADD PRIMARY KEY (OSM_ID);')
        sql.execute("CREATE SPATIAL INDEX ON ROADS(THE_GEOM)")

        System.out.println("Road file loaded")

        // ICA 2019 - Sensitivity
        sql.execute("DROP TABLE ROADS3 if exists;")
        SHPRead.readShape(connection, "D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\data\\Roads09083D.shp", "ROADS3")

        System.out.println("Road file loaded")*/


        PropagationProcessPathData genericMeteoData = new PropagationProcessPathData()
        sensitivityProcessData.setSensitivityTable(dest2)
        sensitivityProcessData.setRoadTable("ROADS",sql)

        int GZIP_CACHE_SIZE = (int) Math.pow(2, 19)

        System.out.println("Start time :" + df.format(new Date()))


        //FileInputStream fileInputStream = new FileInputStream(new File("D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\rays0908.gz").getAbsolutePath())
         FileInputStream fileInputStream2 = new FileInputStream(new File(fileZip).getAbsolutePath())

        try {
            //


            //DataInputStream dataInputStream = new DataInputStream(gzipInputStream)
            //System.out.println(dataInputStream)

            int oldIdReceiver = -1
            int oldIdSource = -1

            FileWriter csvFile = new FileWriter(new File("D:\\aumond\\Documents\\CENSE\\LorientMapNoise\\simuICA2.csv"))
            List<double[]> simuSpectrum = new ArrayList<>()

            Map<Integer, List<double[]>> sourceLevel = new HashMap<>()

            System.out.println("Prepare Sources")
            def timeStart = System.currentTimeMillis()

           sourceLevel = sensitivityProcessData.getTrafficLevel("ROADS", sql, nSimu)

            def timeStart2 = System.currentTimeMillis()
            System.out.println(timeStart2 - timeStart)
            System.out.println("End Emission time :" + df.format(new Date()))
            System.out.println("Run SA")
            long computationTime = 0
            long startSimulationTime = System.currentTimeMillis()

            ZipInputStream zipInputStream2 = new ZipInputStream(fileInputStream2)
            ZipEntry entry2 = zipInputStream2.getNextEntry()

            while (entry2 != null) {

                switch (entry2.getName()) {
                    case 'rays.gz' :
                        GZIPInputStream gzipInputStream = new GZIPInputStream(zipInputStream2, GZIP_CACHE_SIZE)
                        DataInputStream dataInputStream = new DataInputStream(gzipInputStream)
                        //System.out.println(zipInputStream2.available()>0)

                        while (fileInputStream2.available() > 0) {
                            PointToPointPathsMultiRuns paths = new PointToPointPathsMultiRuns()
                            paths.readPropagationPathListStream(dataInputStream)
                            long startComputationTime = System.currentTimeMillis()
                            int idReceiver = (Integer) paths.receiverId
                            int idSource = (Integer) paths.sourceId

                            if (idReceiver != oldIdReceiver) {
                                System.out.println("Receiver: " + oldIdReceiver)
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
                }
                zipInputStream2.closeEntry()
                entry2 = zipInputStream2.getNextEntry()
            }

            System.out.println("ComputationTime :" + computationTime.toString())
            System.out.println("SimulationTime :" + (System.currentTimeMillis() - startSimulationTime).toString())

           // csvFile.close()
            System.out.println("End time :" + df.format(new Date()))

        } finally {

            fileInputStream2.close()
        }

       // long computationTime = System.currentTimeMillis() - start;
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


    void setRoadTable(String tablename, Sql sql) {
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


@CompileStatic
class PointToPointPathsMultiRuns {
    ArrayList<PropagationPath> propagationPathList;
    double li
    long sourceId
    long receiverId

    /**
     * Writes the content of this object into <code>out</code>.
     * @param out the stream to write into
     * @throws java.io.IOException if an I/O-error occurs
     */
    void writePropagationPathListStream( DataOutputStream out) throws IOException {

        out.writeLong(receiverId)
        out.writeLong(sourceId)
        out.writeDouble(li)
        out.writeInt(propagationPathList.size())
        for(PropagationPath propagationPath : propagationPathList) {
            propagationPath.writeStream(out);
        }
    }

    /**
     * Reads the content of this object from <code>out</code>. All
     * properties should be set to their default value or to the value read
     * from the stream.
     * @param in the stream to read
     * @throws IOException if an I/O-error occurs
     */
    void readPropagationPathListStream( DataInputStream inputStream) throws IOException {
        if (propagationPathList==null){
            propagationPathList = new ArrayList<>()
        }

        receiverId = inputStream.readLong()
        sourceId = inputStream.readLong()
        li = inputStream.readDouble()
        int propagationPathsListSize = inputStream.readInt()
        propagationPathList.ensureCapacity(propagationPathsListSize)
        for(int i=0; i < propagationPathsListSize; i++) {
            PropagationPath propagationPath = new PropagationPath()
            propagationPath.readStream(inputStream)
            propagationPathList.add(propagationPath)
        }
    }

}

private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[4096]
    int read = 0
    while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read)
    }
    bos.close()
}