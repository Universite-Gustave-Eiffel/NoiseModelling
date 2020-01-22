package org.noise_planet.noisemodelling.wps.Experimental

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

title = 'Export Rays.zip'
description = 'Compute all the rays and keep it in zip file.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          workingDir : [name: 'workingDir', title: 'Output Directory', description: 'Where Rays will be exported (ex : C:/Desktop/)', type: String.class],
          buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', type: String.class],
          receiversTableName: [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName      : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          groundTableName   : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          reflexionOrder    : [name: 'Number of reflexion', title: 'Number of reflexion', description: 'Maximum number of reflexion to consider (default = 0)', min: 0, max: 1, type: String.class],
          maxSrcDistance    : [name: 'Max Source Distance', title: 'Max Source Distance', description: 'Maximum distance to consider a sound source (default = 100 m)', min: 0, max: 1, type: String.class],
          maxRefDistance    : [name: 'Max Reflexion Distance', title: 'Max Reflexion Distance', description: 'Maximum distance to consider a reflexion (default = 50 m)', min: 0, max: 1, type: String.class],
          wallAlpha         : [name: 'wallAlpha', title: 'Wall alpha', description: 'Wall abosrption (default = 0.1)', min: 0, max: 1, type: String.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class],
          computeVertical   : [name: 'Compute vertical diffraction', title: 'Compute vertical diffraction', description: 'Compute or not the vertical diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          exportReceiverRays   : [name: 'Export Rays of one receiver', title: 'Export Rays of one receiver', description: 'Primary Key Id of the receiver to export (default = -1 (no receivers exported))', min: 0, max: 1, type: Integer.class],
          computeHorizontal : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]

/**
 * Read source database and compute the sound emission spectrum of roads sources*/
class TrafficRayzPropagationProcessData extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesDEN = new ArrayList<>()
    //public Map<Long, Integer> SourcesPk = new HashMap<>()


    TrafficRayzPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    int idSource = 0

    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)

        idSource++

        int pkSource = rs.getInt("PK")
        //SourcesPk.put(pk, pkSource)
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

        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]
        int idFreq = 0
        for(int freq : PropagationProcessPathData.freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] +
                    4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) +
                    8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }

        wjSourcesDEN.add(lden)

    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesDEN.get(sourceId)
    }
}


class TrafficRayzPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficRayzPropagationProcessData(freeFieldFinder);
    }
}

static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {
    def srcFiles =[]
    String fileName =""
    GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
    Properties properties = new Properties()
    // -------------------
    // Get inputs
    // -------------------

    String working_dir = "D:\\aumond\\Documents\\Boulot\\Articles\\2019_XX_XX Sensitivity"
    if (input['workingDir']) {
        working_dir = input['workingDir']
    }



    int reflexion_order = 0
    if (input['reflexionOrder']) {
        reflexion_order = Integer.valueOf(input['reflexionOrder'])
    }
    properties.setProperty("reflexion_order", reflexion_order.toString())


    double max_src_dist = 200
    if (input['maxSrcDistance']) {
        max_src_dist = Double.valueOf(input['maxSrcDistance'])
    }
    properties.setProperty("maxSrcDistance", max_src_dist.toString())

    double max_ref_dist = 50
    if (input['maxRefDistance']) {
        max_ref_dist = Double.valueOf(input['maxRefDistance'])
    }
    properties.setProperty("maxRefDistance", max_ref_dist.toString())

    double wall_alpha = 0.1
    if (input['wallAlpha']) {
        wall_alpha = Double.valueOf(input['wallAlpha'])
    }
    properties.setProperty("wallAlpha", wall_alpha.toString())


    long exportReceiverRays =-1
    if (input['exportReceiverRays']) {
        exportReceiverRays = Integer.valueOf(input['exportReceiverRays'])
    }

    int n_thread = 1
    if (input['threadNumber']) {
        n_thread = Integer.valueOf(input['threadNumber'])
    }

    boolean compute_vertical_diffraction = false
    if (input['computeVertical']) {
        compute_vertical_diffraction = input['computeVertical']
    }
    properties.setProperty("computeVertical", compute_vertical_diffraction.toString())

    boolean compute_horizontal_diffraction = false
    if (input['computeHorizontal']) {
        compute_horizontal_diffraction = input['computeHorizontal']
    }
    properties.setProperty("computeHorizontal", compute_horizontal_diffraction.toString())

    // Get name of the database
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // ----------------------------------
    // Start... 
    // ----------------------------------

    System.out.println("Run ...")

    List<ComputeRaysOut.verticeSL> allLevels = new ArrayList<>()
    // Attenuation matrix table
    ArrayList<PropagationPath> propaMap2 = new ArrayList<>()
    // All rays storage

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->


        //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postgis database
        connection = new ConnectionWrapper(connection)
        String pkName = ""
        String sources_table_name = "SOURCES"
        if (input['sourcesTableName']) {
            sources_table_name = input['sourcesTableName']
            sources_table_name = sources_table_name.toUpperCase()
            fileName = "sources.geojson"
            srcFiles.add(fileName)
            geoJsonDriver.exportTable(connection, sources_table_name, new File(working_dir+fileName), new EmptyProgressVisitor())
            int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection, sources_table_name)
            if(indexPk > 0) {
                pkName = JDBCUtilities.getFieldName(connection.getMetaData(), sources_table_name, indexPk)
            }
            properties.setProperty("pkSources", pkName)
        }

        String receivers_table_name = "RECEIVERS"
        if (input['receiversTableName']) {
            receivers_table_name = input['receiversTableName']
            receivers_table_name = receivers_table_name.toUpperCase()
            fileName = "receivers.geojson"
            srcFiles.add(fileName)
            geoJsonDriver.exportTable(connection, receivers_table_name, new File(working_dir+fileName), new EmptyProgressVisitor())
            int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection, receivers_table_name)
            if(indexPk > 0) {
                pkName = JDBCUtilities.getFieldName(connection.getMetaData(), receivers_table_name, indexPk)
            }
            properties.setProperty("pkReceivers", pkName)
        }

        String building_table_name = "BUILDINGS"
        if (input['buildingTableName']) {
            building_table_name = input['buildingTableName']
            building_table_name = building_table_name.toUpperCase()
            fileName = "buildings.geojson"
            srcFiles.add(fileName)
            geoJsonDriver.exportTable(connection, building_table_name, new File(working_dir+fileName), new EmptyProgressVisitor())
            properties.setProperty("buildings", "TRUE")
        }

        String dem_table_name = ""
        if (input['demTableName']) {
            dem_table_name = input['demTableName']
            dem_table_name = dem_table_name.toUpperCase()
            fileName = "dem.geojson"
            srcFiles.add(fileName)
            geoJsonDriver.exportTable(connection, dem_table_name, new File(working_dir+fileName), new EmptyProgressVisitor())
            properties.setProperty("demTableName", "TRUE")
        }


        String ground_table_name = ""
        if (input['groundTableName']) {
            ground_table_name = input['groundTableName']
            ground_table_name = ground_table_name.toUpperCase()
            fileName = "ground.geojson"
            srcFiles.add(fileName)
            geoJsonDriver.exportTable(connection, ground_table_name, new File(working_dir+fileName), new EmptyProgressVisitor())
            properties.setProperty("groundTableName", "TRUE")
        }

        System.out.println("Connection to the database ok ...")
        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap(building_table_name, sources_table_name, receivers_table_name)
        pointNoiseMap.setComputeHorizontalDiffraction(compute_horizontal_diffraction)
        pointNoiseMap.setComputeVerticalDiffraction(compute_vertical_diffraction)
        pointNoiseMap.setSoundReflectionOrder(reflexion_order)
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT")
        // Import table with Snow, Forest, Grass, Pasture field polygons. Attribute G is associated with each polygon
        if (ground_table_name != "") {
            pointNoiseMap.setSoilTableName(ground_table_name)
        }
        // Point cloud height above sea level POINT(X Y Z)
        if (dem_table_name != "") {
            pointNoiseMap.setDemTable(dem_table_name)
        }
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);

        pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
        pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
        pointNoiseMap.setWallAbsorption(wall_alpha)
        pointNoiseMap.setThreadCount(n_thread)

        TrafficRayzPropagationProcessDataFactory trafficRayzPropagationProcessDataFactory = new TrafficRayzPropagationProcessDataFactory()
        pointNoiseMap.setPropagationProcessDataFactory(trafficRayzPropagationProcessDataFactory)

        // Init custom input in order to compute more than just attenuation
        PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory()
        pointNoiseMap.setComputeRaysOutFactory(storageFactory)
        storageFactory.setWorkingDir(working_dir)
        storageFactory.setExportReceiverRays(exportReceiverRays)

        RootProgressVisitor progressLogger = new RootProgressVisitor(2, true, 1)

        System.out.println("Init Map ...")

        long start = System.currentTimeMillis();

        System.out.println("Start ...")
        srcFiles.add("rays.gz")
        storageFactory.openPathOutputFile(new File(working_dir + "/rays.gz").absolutePath)
        pointNoiseMap.initialize(connection, progressLogger)
        progressLogger.endStep()
        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>()
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim())
        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                System.println("Compute... i:" + i.toString() + " j: " +j.toString() )
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers)
                if (out instanceof ComputeRaysOut) {
                    allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel())
                }
            }
        }
        storageFactory.closeWriteThread()

        String newAppConfigPropertiesFile = working_dir + "NM.properties"
        FileWriter fileWriter = new FileWriter(newAppConfigPropertiesFile)
        properties.store(fileWriter, "store to properties file")
        srcFiles.add("NM.properties")
        fileWriter.close()

        FileOutputStream fos = new FileOutputStream(working_dir + "Rays.zip")
        ZipOutputStream zipOut = new ZipOutputStream(fos)
        for (String srcFile : srcFiles) {
            File fileToZip = new File(new File(working_dir + srcFile).absolutePath)
            FileInputStream fis = new FileInputStream(fileToZip)
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName())
            zipOut.putNextEntry(zipEntry)

            byte[] bytes = new byte[1024]
            int length
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length)
            }
            fis.close()
            fileSuccessfullyDeleted =  new File(working_dir + srcFile).delete()
        }
        zipOut.close()
        fos.close()



        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start;

        return [result: "Calculation Done ! LDEN_GEOM / Rays number : " + storageFactory.getnRays()]


    }

}



@CompileStatic
/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 * remove receiverpath or put to keep rays or not
 */
class PropagationPathStorage extends ComputeRaysOut {
    // Thread safe queue object
    protected TrafficRayzPropagationProcessData inputData
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue

    PropagationPathStorage(PropagationProcessData inputData, PropagationProcessPathData pathData, ConcurrentLinkedDeque<PointToPointPaths> pathQueue) {
        super(true, pathData, inputData)
        this.inputData = (TrafficRayzPropagationProcessData) inputData
        this.pathQueue = pathQueue
    }

    @Override
    double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        return new double[0]
    }

    @Override
    double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        /*if (receiverId==11 && sourceId == 42171){
            receiverId == 11
        }*/
        double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath)

        return attenuation
    }

    @Override
    void finalizeReceiver(long l) {

    }

    @Override
    IComputeRaysOut subProcess(int i, int i1) {
        return new PropagationPathStorageThread(this)
    }

    static class PropagationPathStorageThread implements IComputeRaysOut {
        // In order to keep consecutive receivers into the deque an intermediate list is built for each thread
        private List<PointToPointPaths> receiverPaths = new ArrayList<>()
        private PropagationPathStorage propagationPathStorage

        PropagationPathStorageThread(PropagationPathStorage propagationPathStorage) {
            this.propagationPathStorage = propagationPathStorage
        }

        @Override
        double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            PointToPointPaths paths = new PointToPointPaths()
            paths.li = sourceLi
            paths.receiverId = (propagationPathStorage.inputData.receiversPk.get((int) receiverId).intValue())
            paths.sourceId = propagationPathStorage.inputData.sourcesPk.get((int) sourceId).intValue()

            paths.propagationPathList = new ArrayList<>(propagationPath.size())
            for (PropagationPath path : propagationPath) {
                // Copy path content in order to keep original ids for other method calls
                PropagationPath pathPk = new PropagationPath(path.isFavorable(), path.getPointList(),
                        path.getSegmentList(), path.getSRList())
                pathPk.setIdReceiver((int)paths.receiverId)
                pathPk.setIdSource((int)paths.sourceId)
                paths.propagationPathList.add(pathPk)
                receiverPaths.add(paths)
            }
            double[] aGlobalMeteo = propagationPathStorage.computeAttenuation(propagationPathStorage.genericMeteoData, sourceId, sourceLi, receiverId, propagationPath);
            if (aGlobalMeteo != null && aGlobalMeteo.length > 0)  {

                propagationPathStorage.receiversAttenuationLevels.add(new ComputeRaysOut.verticeSL(paths.receiverId, paths.sourceId, aGlobalMeteo))
                return aGlobalMeteo
            } else {
                return new double[0]
            }
        }



        @Override
        void finalizeReceiver(long receiverId) {
            propagationPathStorage.pathQueue.addAll(receiverPaths)
            receiverPaths.clear()
        }

        @Override
        IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
            return null
        }


    }

}



@CompileStatic
class PropagationPathStorageFactory implements PointNoiseMap.IComputeRaysOutFactory {
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue = new ConcurrentLinkedDeque<>()
    GZIPOutputStream gzipOutputStream
    //FileOutputStream fileOutputStream
    AtomicBoolean waitForMorePaths = new AtomicBoolean(true)
    public static final int GZIP_CACHE_SIZE = (int)Math.pow(2, 19)
    String workingDir
    long exportReceiverRays
    int nRays = 0

    int getnRays() {
        return nRays
    }

    void openPathOutputFile(String path) {
        //fileOutputStream =  new FileOutputStream(path)
        //WriteThread writeThread = new WriteThread(pathQueue, waitForMorePaths, fileOutputStream)
        gzipOutputStream = new GZIPOutputStream(new FileOutputStream(path), GZIP_CACHE_SIZE)
        WriteThread writeThread = new WriteThread(pathQueue, waitForMorePaths, gzipOutputStream)
        new Thread(writeThread).start()
    }

    void setWorkingDir(String workingDir) {
        this.workingDir = workingDir
    }

    void setExportReceiverRays(long exportReceiverRays) {
        this.exportReceiverRays = exportReceiverRays
    }


    void exportDomain(PropagationProcessData inputData, String path) {
        /*GeoJSONDocument geoJSONDocument = new GeoJSONDocument(new FileOutputStream(path))
        geoJSONDocument.writeHeader()
        geoJSONDocument.writeTopographic(inputData.freeFieldFinder.getTriangles(), inputData.freeFieldFinder.getVertices())
        geoJSONDocument.writeFooter()*/
        KMLDocument kmlDocument
        ZipOutputStream compressedDoc
        System.println( "Cellid" + inputData.cellId.toString())
        compressedDoc = new ZipOutputStream(new FileOutputStream(path))
        compressedDoc.putNextEntry(new ZipEntry("doc.kml"))
        kmlDocument = new KMLDocument(compressedDoc)
        kmlDocument.writeHeader()
        kmlDocument.setInputCRS("EPSG:2154")
        kmlDocument.setOffset(new Coordinate(0,0,0))
        kmlDocument.writeTopographic(inputData.freeFieldFinder.getTriangles(), inputData.freeFieldFinder.getVertices())
        kmlDocument.writeBuildings(inputData.freeFieldFinder)
        kmlDocument.writeFooter()
        compressedDoc.closeEntry()
        compressedDoc.close()
    }

    @Override
    IComputeRaysOut create(PropagationProcessData propagationProcessData, PropagationProcessPathData propagationProcessPathData) {
        if (exportReceiverRays>0) exportDomain(propagationProcessData, new File(this.workingDir, String.format("domain_%d.kmz", propagationProcessData.cellId)).absolutePath)
        return new PropagationPathStorage(propagationProcessData, propagationProcessPathData, pathQueue)
    }

    void closeWriteThread() {
        waitForMorePaths.set(false)
    }

    /**
     * Write paths on disk using a single thread
     */
    class WriteThread implements Runnable {
        ConcurrentLinkedDeque<PointToPointPaths> pathQueue
        AtomicBoolean waitForMorePaths
        GZIPOutputStream gzipOutputStream
        //FileOutputStream fileOutputStream

        WriteThread(ConcurrentLinkedDeque<PointToPointPaths> pathQueue, AtomicBoolean waitForMorePaths, GZIPOutputStream gzipOutputStream) {
            this.pathQueue = pathQueue
            this.waitForMorePaths = waitForMorePaths
            this.gzipOutputStream = gzipOutputStream
        }

       /* WriteThread(ConcurrentLinkedDeque<PointToPointPaths> pathQueue, AtomicBoolean waitForMorePaths, FileOutputStream fileOutputStream) {
            this.pathQueue = pathQueue
            this.waitForMorePaths = waitForMorePaths
            this.fileOutputStream = fileOutputStream
        }*/

        @Override
        void run() {

            KMLDocument kmlDocument
            ZipOutputStream compressedDoc

            compressedDoc = new ZipOutputStream(new FileOutputStream(
                    String.format(workingDir + "RaysFromRecv"+ exportReceiverRays +".kmz")))
            compressedDoc.putNextEntry(new ZipEntry("doc.kml"))
            kmlDocument = new KMLDocument(compressedDoc)
            kmlDocument.writeHeader()
            kmlDocument.setInputCRS("EPSG:2154")
            kmlDocument.setOffset(new Coordinate(0,0,0))


            /*PropagationProcessPathData genericMeteoData = new PropagationProcessPathData()
            genericMeteoData.setHumidity(70)
            genericMeteoData.setTemperature(10)
            ComputeRaysOut out = new ComputeRaysOut(false, genericMeteoData)
*/
            DataOutputStream dataOutputStream = new DataOutputStream( new BufferedOutputStream(gzipOutputStream))
            while (waitForMorePaths.get()) {
                while(!pathQueue.isEmpty()) {
                    PointToPointPaths paths = pathQueue.pop()
                    //long start = System.currentTimeMillis();
                    paths.writePropagationPathListStream(dataOutputStream)

                    //System.out.println(System.currentTimeMillis() - start )
                    paths.countRays()
                    nRays = nRays + paths.getnRays()
                    /*if(paths.receiverId == exportReceiverRays) {
                        // Export rays
                        kmlDocument.writeRays(paths.getPropagationPathList())

                    }*/

                }
                Thread.sleep(10)
            }

            System.out.println("nRays : " + nRays)
            dataOutputStream.flush()
            gzipOutputStream.close()
            //fileOutputStream.close()
            kmlDocument.writeFooter()
            compressedDoc.closeEntry()
            compressedDoc.close()



        }
    }
}


@CompileStatic
class PointToPointPaths {
    ArrayList<PropagationPath> propagationPathList
    double li
    long sourceId
    long receiverId
    int nRays =0

    int getnRays() {
        return nRays
    }

    void countRays() throws IOException {

         for (PropagationPath propagationPath : propagationPathList) {
            nRays++
        }
    }

     /* Writes the content of this object into <code>out</code>.
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



}
