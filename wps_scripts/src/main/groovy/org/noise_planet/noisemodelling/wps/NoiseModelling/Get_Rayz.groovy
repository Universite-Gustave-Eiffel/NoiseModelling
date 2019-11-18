package org.noise_planet.noisemodelling.wps.NoiseModelling;

/*
 * @Author Pierre Aumond
 */


import org.locationtech.jts.geom.Coordinate

import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import geoserver.GeoServer
import geoserver.catalog.Store

import groovy.transform.CompileStatic


import java.util.concurrent.ConcurrentLinkedDeque


import org.geotools.jdbc.JDBCDataStore


import javax.xml.stream.XMLStreamException
import org.cts.crs.CRSException

import java.sql.Connection

import org.h2gis.utilities.wrapper.*

import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap


import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor

import java.sql.SQLException

title = 'Get Rays in gunzip'
description = 'Compute all the rays and keep it in gunzip file.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          workingDir : [name: 'workingDir', title: 'workingDir', description: 'workingDir (ex : C:/Desktop/)', type: String.class],
          buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name with emission', type: String.class],
          receiversTableName: [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName      : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          groundTableName   : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          reflexionOrder    : [name: 'Number of reflexion', title: 'Number of reflexion', description: 'Maximum number of reflexion to consider (default = 0)', min: 0, max: 1, type: String.class],
          maxSrcDistance    : [name: 'Max Source Distance', title: 'Max Source Distance', description: 'Maximum distance to consider a sound source (default = 100 m)', min: 0, max: 1, type: String.class],
          maxRefDistance    : [name: 'Max Reflexion Distance', title: 'Max Reflexion Distance', description: 'Maximum distance to consider a reflexion (default = 50 m)', min: 0, max: 1, type: String.class],
          wallAlpha         : [name: 'wallAlpha', title: 'Wall alpha', description: 'Wall abosrption (default = 0.1)', min: 0, max: 1, type: String.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class],
          computeVertical   : [name: 'Compute vertical diffraction', title: 'Compute vertical diffraction', description: 'Compute or not the vertical diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          computeHorizontal : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]

/**
 * Read source database and compute the sound emission spectrum of roads sources*/
class TrafficPropagationProcessData extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesDEN = new ArrayList<>();
    public Map<Long, Integer> SourcesPk = new HashMap<>();


    public TrafficPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder);
    }

    def idSource = 0

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)
        SourcesPk.put(pk, idSource++)

        // Read average 24h traffic
        double[] ld = [ComputeRays.dbaToW(rs.getDouble('Ld63')),
                       ComputeRays.dbaToW(rs.getDouble('Ld125')),
                       ComputeRays.dbaToW(rs.getDouble('Ld250')),
                       ComputeRays.dbaToW(rs.getDouble('Ld500')),
                       ComputeRays.dbaToW(rs.getDouble('Ld1000')),
                       ComputeRays.dbaToW(rs.getDouble('Ld2000')),
                       ComputeRays.dbaToW(rs.getDouble('Ld4000')),
                       ComputeRays.dbaToW(rs.getDouble('Ld8000'))]
        double[] le = [ComputeRays.dbaToW(rs.getDouble('Le63')),
                       ComputeRays.dbaToW(rs.getDouble('Le125')),
                       ComputeRays.dbaToW(rs.getDouble('Le250')),
                       ComputeRays.dbaToW(rs.getDouble('Le500')),
                       ComputeRays.dbaToW(rs.getDouble('Le1000')),
                       ComputeRays.dbaToW(rs.getDouble('Le2000')),
                       ComputeRays.dbaToW(rs.getDouble('Le4000')),
                       ComputeRays.dbaToW(rs.getDouble('Le8000'))]
        double[] ln = [ComputeRays.dbaToW(rs.getDouble('Ln63')),
                       ComputeRays.dbaToW(rs.getDouble('Ln125')),
                       ComputeRays.dbaToW(rs.getDouble('Ln250')),
                       ComputeRays.dbaToW(rs.getDouble('Ln500')),
                       ComputeRays.dbaToW(rs.getDouble('Ln1000')),
                       ComputeRays.dbaToW(rs.getDouble('Ln2000')),
                       ComputeRays.dbaToW(rs.getDouble('Ln4000')),
                       ComputeRays.dbaToW(rs.getDouble('Ln8000'))]

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
        return wjSourcesDEN.get(sourceId);
    }
}


class TrafficPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessData(freeFieldFinder);
    }
}


def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def static exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
    try {
        FileOutputStream outData = new FileOutputStream(name);
        KMLDocument kmlDocument = new KMLDocument(outData);
        kmlDocument.setInputCRS("EPSG:2154");
        kmlDocument.writeHeader();
        if (manager != null) {
            kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices());
        }
        if (result != null) {
            kmlDocument.writeRays(result.getPropagationPaths());
        }
        if (manager != null && manager.isHasBuildingWithHeight()) {
            kmlDocument.writeBuildings(manager)
        }
        kmlDocument.writeFooter();
    } catch (XMLStreamException | CRSException ex) {
        throw new IOException(ex)
    }
}

def run(input) {


    // -------------------
    // Get inputs
    // -------------------

    String working_dir = "D:\\aumond\\Documents\\Boulot\\Articles\\2019_XX_XX Sensitivity"
    if (input['workingDir']) {
        working_dir = input['workingDir']
    }



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

    int reflexion_order = 0
    if (input['reflexionOrder']) {
        reflexion_order = Integer.valueOf(input['reflexionOrder'])
    }

    double max_src_dist = 200
    if (input['maxSrcDistance']) {
        max_src_dist = Double.valueOf(input['maxSrcDistance'])
    }

    double max_ref_dist = 50
    if (input['maxRefDistance']) {
        max_ref_dist = Double.valueOf(input['maxRefDistance'])
    }

    double wall_alpha = 0.1
    if (input['wallAlpha']) {
        wall_alpha = Double.valueOf(input['wallAlpha'])
    }

    int n_thread = 1
    if (input['threadNumber']) {
        n_thread = Integer.valueOf(input['threadNumber'])
    }

    boolean compute_vertical_diffraction = false
    if (input['computeVertical']) {
        compute_vertical_diffraction = input['computeVertical']
    }

    boolean compute_horizontal_diffraction = false
    if (input['computeHorizontal']) {
        compute_horizontal_diffraction = input['computeHorizontal']
    }

    // Get name of the database
    String dbName = "h2gisdb"
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
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->

        //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postgis database
        connection = new ConnectionWrapper(connection)

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
        if (ground_table_name != "") {
            pointNoiseMap.setSoilTableName(dem_table_name)
        }
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);

        pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
        pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
        pointNoiseMap.setWallAbsorption(wall_alpha)
        pointNoiseMap.setThreadCount(n_thread)

        TrafficPropagationProcessDataFactory trafficPropagationProcessDataFactory = new TrafficPropagationProcessDataFactory()
        pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory)

        // Init custom input in order to compute more than just attenuation
        PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory()
        pointNoiseMap.setComputeRaysOutFactory(storageFactory)
        storageFactory.setWorkingDir(working_dir)

        RootProgressVisitor progressLogger = new RootProgressVisitor(2, true, 1)

        System.out.println("Init Map ...")

        long start = System.currentTimeMillis();

        System.out.println("Start ...")
        storageFactory.openPathOutputFile(new File(working_dir + "/rayz.gz").absolutePath)
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

        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start;

        return [result: "Calculation Done ! LDEN_GEOM"]


    }

}



@CompileStatic
/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 * remove receiverpath or put to keep rays or not
 */
class PropagationPathStorage extends ComputeRaysOut {
    // Thread safe queue object
    protected TrafficPropagationProcessData inputData
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue

    PropagationPathStorage(PropagationProcessData inputData, PropagationProcessPathData pathData, ConcurrentLinkedDeque<PointToPointPaths> pathQueue) {
        super(true, pathData, inputData)
        this.inputData = (TrafficPropagationProcessData)inputData
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
    AtomicBoolean waitForMorePaths = new AtomicBoolean(true)
    public static final int GZIP_CACHE_SIZE = (int)Math.pow(2, 19)
    String workingDir

    void openPathOutputFile(String path) {
        gzipOutputStream = new GZIPOutputStream(new FileOutputStream(path), GZIP_CACHE_SIZE)
        new Thread(new WriteThread(pathQueue, waitForMorePaths, gzipOutputStream)).start()
    }

    void setWorkingDir(String workingDir) {
        this.workingDir = workingDir
    }

    void exportDomain(PropagationProcessData inputData, String path) {
        /*GeoJSONDocument geoJSONDocument = new GeoJSONDocument(new FileOutputStream(path))
        geoJSONDocument.writeHeader()
        geoJSONDocument.writeTopographic(inputData.freeFieldFinder.getTriangles(), inputData.freeFieldFinder.getVertices())
        geoJSONDocument.writeFooter()*/
        KMLDocument kmlDocument
        ZipOutputStream compressedDoc
        System.println( "Cellid" + inputData.cellId.toString())
        compressedDoc = new ZipOutputStream(new FileOutputStream(
                String.format("domain_%d.kmz", inputData.cellId)))
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
        exportDomain(propagationProcessData, new File(this.workingDir, String.format("_%d.geojson", propagationProcessData.cellId)).absolutePath)
        return new PropagationPathStorage(propagationProcessData, propagationProcessPathData, pathQueue)
    }

    void closeWriteThread() {
        waitForMorePaths.set(false)
    }

    /**
     * Write paths on disk using a single thread
     */
    static class WriteThread implements Runnable {
        ConcurrentLinkedDeque<PointToPointPaths> pathQueue
        AtomicBoolean waitForMorePaths
        GZIPOutputStream gzipOutputStream

        WriteThread(ConcurrentLinkedDeque<PointToPointPaths> pathQueue, AtomicBoolean waitForMorePaths, GZIPOutputStream gzipOutputStream) {
            this.pathQueue = pathQueue
            this.waitForMorePaths = waitForMorePaths
            this.gzipOutputStream = gzipOutputStream
        }

        @Override
        void run() {
            long exportReceiverRay = 2 // primary key of receiver to export
            KMLDocument kmlDocument

            ZipOutputStream compressedDoc

            compressedDoc = new ZipOutputStream(new FileOutputStream(
                    String.format("domain.kmz")))
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
            DataOutputStream dataOutputStream = new DataOutputStream(gzipOutputStream)
            while (waitForMorePaths.get()) {
                while(!pathQueue.isEmpty()) {
                    PointToPointPaths paths = pathQueue.pop()
                    paths.writePropagationPathListStream(dataOutputStream)

                    if(paths.receiverId == exportReceiverRay) {
                        // Export rays
                        kmlDocument.writeRays(paths.getPropagationPathList())

                    }

                }
                Thread.sleep(10)
            }
            dataOutputStream.flush()
            gzipOutputStream.close()
            kmlDocument.writeFooter()
            compressedDoc.closeEntry()
            compressedDoc.close()



        }
    }
}


@CompileStatic
class PointToPointPaths {
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
