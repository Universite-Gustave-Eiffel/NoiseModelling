package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.math.Vector3D
import org.noise_planet.noisemodelling.jdbc.*
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

title = 'Compute Dynamic NoiseMap'
description = 'Compute Dynamic NoiseMap from individual moving point sources'

inputs = [databaseName        : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          buildingTableName   : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTimeTableName: [name: 'Sources Time table name', title: 'Sources Time table name', description: 'Column idSource, column idPosition, column idTime', type: String.class],
          receiversTableName  : [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName        : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          exportReceiverRays  : [name       : 'Export Rays for one receiver', title: 'Export Rays for one receiver',
                                 description: 'PK of the receiver for which you want to export the rays (INTEGER).' +
                                         '</br> If the value is set to -1, no rays will be exported.' +
                                         '</br> The export format is kml (compatible with google earth).' +
                                         '</br> </br> <b> Default value : -1 </b>',
                                 min        : 0, max: 1, type: Integer.class],
          groundTableName     : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          reflexionOrder      : [name: 'Number of reflexion', title: 'Number of reflexion', description: 'Maximum number of reflexion to consider (default = 0)', min: 0, max: 1, type: String.class],
          maxSrcDistance      : [name: 'Max Source Distance', title: 'Max Source Distance', description: 'Maximum distance to consider a sound source (default = 100 m)', min: 0, max: 1, type: String.class],
          maxRefDistance      : [name: 'Max Reflexion Distance', title: 'Max Reflexion Distance', description: 'Maximum distance to consider a reflexion (default = 50 m)', min: 0, max: 1, type: String.class],
          wallAlpha           : [name: 'wallAlpha', title: 'Wall alpha', description: 'Wall abosrption (default = 0.1)', min: 0, max: 1, type: String.class],
          threadNumber        : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class],
          computeVertical     : [name: 'Compute vertical diffraction', title: 'Compute vertical diffraction', description: 'Compute or not the vertical diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          computeHorizontal   : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]



/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 * remove receiverpath or put to keep rays or not
 */
@CompileStatic
class PropagationPathStorage extends ComputeRaysOutAttenuation {
    // Thread safe queue object
    protected PropagationProcessData inputData
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue

    PropagationPathStorage(PropagationProcessData inputData, PropagationProcessPathData pathData, ConcurrentLinkedDeque<PointToPointPaths> pathQueue) {
        super(true, pathData, inputData)
        this.inputData = (PropagationProcessData) inputData
        this.pathQueue = pathQueue
    }

    @Override
    double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        return new double[0]
    }

    @Override
    double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath)
        return attenuation
    }

    @Override
    void finalizeReceiver(long l) {

    }

    @Override
    IComputeRaysOut subProcess() {
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
                pathPk.setIdReceiver((int) paths.receiverId)
                pathPk.setIdSource((int) paths.sourceId)
                paths.propagationPathList.add(pathPk)

            }
            receiverPaths.add(paths)

            double[] aGlobalMeteo = propagationPathStorage.computeAttenuation(propagationPathStorage.genericMeteoData, sourceId, sourceLi, receiverId, propagationPath);
            if (aGlobalMeteo != null && aGlobalMeteo.length > 0) {

                propagationPathStorage.receiversAttenuationLevels.add(new ComputeRaysOutAttenuation.VerticeSL(paths.receiverId, paths.sourceId, aGlobalMeteo))
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
        IComputeRaysOut subProcess() {
            return null
        }


    }

}
/**
 * Export paths on disk
 */
@CompileStatic
class PropagationPathStorageFactory implements PointNoiseMap.IComputeRaysOutFactory {
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue = new ConcurrentLinkedDeque<>()
    GZIPOutputStream gzipOutputStream
    //FileOutputStream fileOutputStream
    AtomicBoolean waitForMorePaths = new AtomicBoolean(true)
    public static final int GZIP_CACHE_SIZE = (int) Math.pow(2, 19)
    String workingDir
    long exportReceiverRays
    int nRays = 0

    int getnRays() {
        return nRays
    }

    void openPathOutputFile(String path) {
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
        System.println("Export domain : Cell number " + inputData.cellId.toString())
        compressedDoc = new ZipOutputStream(new FileOutputStream(path))
        compressedDoc.putNextEntry(new ZipEntry("doc.kml"))
        kmlDocument = new KMLDocument(compressedDoc)
        kmlDocument.writeHeader()
        kmlDocument.setInputCRS("EPSG:2154")
        kmlDocument.setOffset(new Coordinate(0, 0, 0))
        kmlDocument.writeTopographic(inputData.freeFieldFinder.getTriangles(), inputData.freeFieldFinder.getVertices())
        kmlDocument.writeBuildings(inputData.freeFieldFinder)
        kmlDocument.writeFooter()
        compressedDoc.closeEntry()
        compressedDoc.close()
    }

    @Override
    IComputeRaysOut create(PropagationProcessData propagationProcessData, PropagationProcessPathData propagationProcessPathData) {
        if (exportReceiverRays > 0) exportDomain(propagationProcessData, new File(this.workingDir, String.format("Domain_part_%d.kmz", propagationProcessData.cellId)).absolutePath)
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

            if (exportReceiverRays > 0){
                compressedDoc = new ZipOutputStream(new FileOutputStream(
                        String.format(workingDir + "RaysFromRec_" + exportReceiverRays + ".kmz")))
                compressedDoc.putNextEntry(new ZipEntry("doc.kml"))
                kmlDocument = new KMLDocument(compressedDoc)
                kmlDocument.writeHeader()
                kmlDocument.setInputCRS("EPSG:2154")
                kmlDocument.setOffset(new Coordinate(0, 0, 0))
            }
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(gzipOutputStream))

            while (waitForMorePaths.get() || !pathQueue.isEmpty()) {
                while (!pathQueue.isEmpty()) {
                    PointToPointPaths paths = pathQueue.pop()
                    //long start = System.currentTimeMillis();
                    paths.writePropagationPathListStream(dataOutputStream)

                    //System.out.println(System.currentTimeMillis() - start )
                    paths.countRays()
                    nRays = nRays + paths.getnRays()
                    if (paths.receiverId == exportReceiverRays) {
                        // Export rays
                        kmlDocument.writeRays(paths.getPropagationPathList())

                    }

                }
                Thread.sleep(10)
            }

            System.out.println("The number of stored rays is 1 : " + nRays)
            dataOutputStream.flush()
            System.out.println("The number of stored rays is 2 : " + exportReceiverRays)
            gzipOutputStream.close()
            System.out.println("The number of stored rays is 3 : " + nRays)
            //fileOutputStream.close()
            if (exportReceiverRays > 0)   {
                System.out.println("The number of stored rays is 4 : " + nRays)
                kmlDocument.writeFooter()
                System.out.println("A kml file has been exported for receiver " + exportReceiverRays)
            }
            System.out.println("The number of stored rays is 5 : " + nRays)
            compressedDoc.closeEntry()
            compressedDoc.close()


        }
    }
}

/**
 * Write Path in local table
 */
@CompileStatic
class PointToPointPaths {
    ArrayList<PropagationPath> propagationPathList
    double li
    long sourceId
    long receiverId
    int nRays = 0

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

    void writePropagationPathListStream(DataOutputStream out) throws IOException {

        out.writeLong(receiverId)
        out.writeLong(sourceId)
        out.writeDouble(li)
        out.writeInt(propagationPathList.size())
        for (PropagationPath propagationPath : propagationPathList) {
            propagationPath.writeStream(out)

        }
    }


}

/**
 *
 */
class DroneThirdProcessData {

    static double[] readDronetothirdOctave(double[] values) {
        double[] valueoct = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        for (int i = 0; i < 24; i++) {
            valueoct[i] = values[i]
        }
        return valueoct
    }

    static double[] readDroneFile(String filename, int theta, int phi)
            throws Exception {
        String line = null
        //System.out.println("line "+ phi+ ":" + theta)
        double[] lvl = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]

        // wrap a BufferedReader around FileReader
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))

        // use the readLine method of the BufferedReader to read one line at a time.
        // the readLine method returns null when there is nothing else to read.
        int k = 1
        int ntheta = 0
        int nphi = 0

        while ((line = bufferedReader.readLine()) != null) {
            if (k == 4) { // PHIOBSEC  nphi X X X
                String[] values = line.split("\\s+")
                nphi = Integer.valueOf(values[1])
                if (phi == nphi) phi = 0
            }
            if (k == 6) {
                String[] values = line.split("\\s+")
                ntheta = Integer.valueOf(values[1])
                if (theta == ntheta) theta = 0
            }
            if (k == (14 + (nphi + 2) * theta + phi)) {
                //System.out.println("line "+ (14+(nphi+2)*theta+phi))
                String[] values = line.split("\\s+")

                double[] parsed = new double[values.length - 1]
                // Correction P.Dieumegard to pass from noise level at 1m to power source (+11dB) and to remove the effect of ground plate (-6dB)
                for (int i = 1; i < values.length; i++) parsed[i - 1] = Double.valueOf(values[i]) + 11 - 6
                lvl = parsed

                break
            }
            k++
        }

        // close the BufferedReader when we're done
        bufferedReader.close()
        return lvl
    }

    double[] getDroneLevel(int idSphere, double theta, double phi) throws SQLException {
        double[] res_d = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                          0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]


        int thetathird = (int) Math.round(theta / 3)
        int phithird = (int) Math.round(phi / 3)

        res_d = readDronetothirdOctave(readDroneFile("C:/Users/aumond/Documents/Projets/2019_2021 DRONE/2021_10_14 NM/PierreD/TO_PIERRE_A/NOISE_SOURCES/idNoiseSphere_" + idSphere + ".txt", thetathird, phithird))

        //System.out.println(res_d)

        return res_d
    }

}


/** Read source database and compute the sound emission spectrum of roads sources **/
@CompileStatic
class DroneThirdPropagationProcessData extends PropagationProcessData {

    protected List<double[]> wjSourcesD = new ArrayList<>()

    public DroneThirdPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

        super.addSource(pk, geom, rs)

        PropagationProcessPathData propagationProcessPathData = new PropagationProcessPathData();

        // We put maximum expected source level at 120 dB in order to not propagate if expected noise level change is < than 0.1 dB
        wjSourcesD.add(ComputeRays.dbaToW([120] * propagationProcessPathData.freq_lvl.size() as double[]))
    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }

}


class DroneThirdPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException {
    }

    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new DroneThirdPropagationProcessData(freeFieldFinder)
    }
}

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
    // Advanced user can replace this database for a PostGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}


// Main function of the script
def exec(Connection connection, input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    def srcFiles = []
    // -------------------
    // Get inputs
    // -------------------

    String sources_time_table_name = "SOURCES"
    if (input['sourcesTimeTableName']) {
        sources_time_table_name = input['sourcesTimeTableName']
    }
    sources_time_table_name = sources_time_table_name.toUpperCase()


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

    double max_src_dist = 100
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
    String dbName = ""
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    int exportReceiverRays = -1
    if (input['exportReceiverRays']) {
        exportReceiverRays = Integer.valueOf(input['exportReceiverRays'])
    }

    // ----------------------------------
    // Start...
    // ----------------------------------

    logger.info("Run ...")


    // All rays storage

    // Enable third octave
    PropagationProcessPathData propagationProcessPathData = new PropagationProcessPathData(true);

    // Init NoiseModelling
    PointNoiseMap pointNoiseMap = new PointNoiseMap(building_table_name, sources_time_table_name, receivers_table_name)
    pointNoiseMap.setPropagationProcessPathData(propagationProcessPathData)
    pointNoiseMap.setComputeHorizontalDiffraction(compute_horizontal_diffraction)
    pointNoiseMap.setComputeVerticalDiffraction(compute_vertical_diffraction)
    pointNoiseMap.setSoundReflectionOrder(reflexion_order)
    pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false)
    pointNoiseMap.setSourceHasAbsoluteZCoordinates(false)

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
    // pointNoiseMap.setMaximumError(0.1d);


    pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
    pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
    pointNoiseMap.setWallAbsorption(wall_alpha)
    pointNoiseMap.setThreadCount(n_thread)

    // Init custom input in order to compute more than just attenuation
    DroneThirdPropagationProcessDataFactory droneThirdPropagationProcessDataFactory = new DroneThirdPropagationProcessDataFactory()
    pointNoiseMap.setPropagationProcessDataFactory(droneThirdPropagationProcessDataFactory)

    pointNoiseMap.setPropagationProcessPathData(propagationProcessPathData)


    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

    logger.info("Init Map ...")
    pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

    // Init custom input in order to compute more than just attenuation
    PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory()
    pointNoiseMap.setComputeRaysOutFactory(storageFactory)
    storageFactory.setWorkingDir("C:/Users/aumond/Documents/Projets/2019_2021 DRONE/2021_10_14 NM/OUTPUT/")
    if (exportReceiverRays != -1) storageFactory.setExportReceiverRays(exportReceiverRays)
    srcFiles.add("rays.gz")
    storageFactory.openPathOutputFile(new File("C:/Users/aumond/Documents/Projets/2019_2021 DRONE/2021_10_14 NM/OUTPUT/rays.gz").absolutePath)

    // Set of already processed receivers
    Set<Long> receivers = new HashSet<>()

    Map cells = pointNoiseMap.searchPopulatedCells(connection);
    ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());

    long start = System.currentTimeMillis()
    logger.info("Start ...")


    sql.execute("drop table if exists LDAY;")
    sql.execute("create table LDAY (TIME integer, IDRECEIVER integer, " +
            "Hz50 double precision, Hz63 double precision,  Hz80 double precision, " +
            "Hz100 double precision, Hz125 double precision, Hz160 double precision, " +
            " Hz200 double precision,  Hz250 double precision,  Hz315 double precision, " +
            " Hz400 double precision, Hz500 double precision,  Hz630 double precision, " +
            " Hz800 double precision, Hz1000 double precision,  Hz1250 double precision, " +
            " Hz1600 double precision, Hz2000 double precision,  Hz2500 double precision, " +
            " Hz3150 double precision, Hz4000 double precision,  Hz5000 double precision, " +
            " Hz6300 double precision, Hz8000 double precision, Hz10000 double precision   );")

    def qry = "INSERT INTO LDAY(TIME , IDRECEIVER,Hz50,Hz63,Hz80, " +
            "Hz100,Hz125,Hz160," +
            "Hz200, Hz250, Hz315," +
            "Hz400, Hz500, " +
            "Hz630,Hz800, " +
            "Hz1000,Hz1250,Hz1600," +
            "Hz2000, Hz2500, Hz3150," +
            "Hz4000, Hz5000, " +
            "Hz6300,Hz8000, " +
            "Hz10000) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);"

    Map<Integer, Coordinate> geomReceivers = new HashMap<>()

    // Collect All sources and receivers coordinates in order to compute phi and theta of S-R ray vector
    String receiversPkName = JDBCUtilities.getFieldName(connection.getMetaData(),
            receivers_table_name, JDBCUtilities.getIntegerPrimaryKey(connection, receivers_table_name))
    sql.eachRow('SELECT ' + receiversPkName + ',ST_X(the_geom),ST_Y(the_geom),ST_Z(the_geom) FROM ' + receivers_table_name) { row ->
        geomReceivers.put(row[0] as Integer, new Coordinate(row[1] as Double, row[2] as Double, row[3] as Double))
    }
    String sourcePkName = JDBCUtilities.getFieldName(connection.getMetaData(),
            sources_time_table_name, JDBCUtilities.getIntegerPrimaryKey(connection, sources_time_table_name))
    Map<Integer, Coordinate> geomFixedSources = new HashMap<>()
    sql.eachRow('SELECT ' + sourcePkName + ', ST_X(the_geom),ST_Y(the_geom),ST_Z(the_geom) FROM ' + sources_time_table_name) { row ->
        geomFixedSources.put(row[0] as Integer, new Coordinate(row[1] as Double, row[2] as Double, row[3] as Double))
    }


    DroneThirdProcessData droneThirdProcessData = new DroneThirdProcessData()

    logger.info("taille de cellulle : " + pointNoiseMap.getCellWidth().toString())

    new TreeSet<>(cells.keySet()).each { cellIndex ->
        Envelope cellEnvelope = pointNoiseMap.getCellEnv(pointNoiseMap.getMainEnvelope(),
                cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), pointNoiseMap.getCellWidth(),
                pointNoiseMap.getCellHeight());
        logger.info("Compute domain is " + new GeometryFactory().toGeometry(cellEnvelope))
        ProgressVisitor cellProg = progressVisitor.subProcess(2)
        IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), cellProg, receivers)
        logger.info(out.toString())
        logger.info("*********************")
        if (out instanceof IComputeRaysOut) {
            logger.info("*********************")
            Map<Long, ArrayList<ComputeRaysOutAttenuation.VerticeSL>> levelsBySource = new HashMap<>()

            // Index and store (in memory) levels by source identifier (position)
            List<ComputeRaysOutAttenuation.VerticeSL> verticeSLList = out.getVerticesSoundLevel()
            logger.info(String.format("Computation of attenuation done, looking for drone emission (%d rays to process)", verticeSLList.size() as Integer))
            for (ComputeRaysOutAttenuation.VerticeSL att : verticeSLList) {
                ArrayList<ComputeRaysOutAttenuation.VerticeSL> srcReceivers
                if (!levelsBySource.containsKey(att.sourceId as Long)) {
                    srcReceivers = new ArrayList<ComputeRaysOutAttenuation.VerticeSL>()
                    levelsBySource.put(att.sourceId as Long, srcReceivers)
                } else {
                    srcReceivers = levelsBySource.get(att.sourceId as Long)
                }
                srcReceivers.add(att)
            }

            Map<Integer, double[]> soundLevels = new HashMap<>()

            int soundLevelsTime = -1
            logger.info("*--------------------------------------- ")
            int nbsourcest = sql.firstRow("SELECT COUNT(*) CPT FROM " + sources_time_table_name)[0] as Integer
            ProgressVisitor srcProg = cellProg.subProcess(nbsourcest)
            sql.eachRow('SELECT PK, PHI, T, idSource, IDNOISESPHERE FROM ' + sources_time_table_name + ' ORDER BY T,idSource ASC') { row ->
                int idPositionDynamic = row.PK as Integer
                int idSphere = row.IDNOISESPHERE as Integer
                int idSource = row.IDSOURCE
                int t = row.T as Integer
                double phiHelico = row.PHI as double
                logger.info("pas de temps : " + t.toString())


                if (soundLevelsTime != -1 && soundLevelsTime != t && !soundLevels.isEmpty()) {
                    // New time step process all receivers for the previous time steps
                    sql.withBatch(100, qry) { ps ->
                        for (Map.Entry<Integer, double[]> entry : soundLevels.entrySet()) {
                            Integer key = entry.getKey()
                            double[] value = entry.getValue()
                            ps.addBatch(soundLevelsTime as Integer, key as Integer,
                                    value[0] as Double, value[1] as Double, value[2] as Double,
                                    value[3] as Double, value[4] as Double, value[5] as Double,
                                    value[6] as Double, value[7] as Double, value[8] as Double,
                                    value[9] as Double, value[10] as Double, value[11] as Double,
                                    value[12] as Double, value[13] as Double, value[14] as Double,
                                    value[15] as Double, value[16] as Double, value[17] as Double,
                                    value[18] as Double, value[19] as Double, value[20] as Double,
                                    value[21] as Double, value[22] as Double, value[23] as Double)
                        }
                    }
                    soundLevels.clear()
                }
                soundLevelsTime = t

                for (ComputeRaysOutAttenuation.VerticeSL att : levelsBySource.get(idPositionDynamic as Long)) {
                    int idReceiver = (Integer) att.receiverId
                    Coordinate A = geomFixedSources.get(idPositionDynamic)
                    Coordinate B = geomReceivers.get(idReceiver)
                    Vector3D vector = new Vector3D(A, B)

                    double r = A.distance3D(B)
                    // Correction P.Dieumegard correct sphere orientation
                    //double angle = Math.atan2(vector.getY(), vector.getX())
                    double angle = 3.14 / 2 - Math.atan2(vector.getY(), vector.getX())
                    if (angle < 0)
                        angle = angle + 2 * 3.14
                    angle = angle * 360 / (2 * 3.14)
                    double phi = angle - phiHelico
                    if (phi < 0)
                        phi = phi + 360
                    double theta = 180 - (Math.acos(vector.getZ() / r) * 180 / 3.14)

                    double[] soundLevel = att.value
                    double[] sourceLev = droneThirdProcessData.getDroneLevel(idSphere, theta, phi)
                    if (soundLevels.containsKey(idReceiver)) {
                        soundLevel = ComputeRays.sumDbArray(sumLinearArray(soundLevel, sourceLev), soundLevels.get(idReceiver))
                        soundLevels.replace(idReceiver, soundLevel)
                    } else {
                        soundLevels.put(idReceiver, sumLinearArray(soundLevel, sourceLev))
                    }
                }
                srcProg.endStep()
            }


            if (!soundLevels.isEmpty()) {
                // insert final batch of time receivers
                sql.withBatch(100, qry) { ps ->
                    for (Map.Entry<Integer, double[]> entry : soundLevels.entrySet()) {
                        Integer key = entry.getKey()
                        double[] value = entry.getValue()
                        ps.addBatch(soundLevelsTime as Integer, key as Integer,
                                value[0] as Double, value[1] as Double, value[2] as Double,
                                value[3] as Double, value[4] as Double, value[5] as Double,
                                value[6] as Double, value[7] as Double, value[8] as Double,
                                value[9] as Double, value[10] as Double, value[11] as Double,
                                value[12] as Double, value[13] as Double, value[14] as Double,
                                value[15] as Double, value[16] as Double, value[17] as Double,
                                value[18] as Double, value[19] as Double, value[20] as Double,
                                value[21] as Double, value[22] as Double, value[23] as Double)
                    }
                }
            }
        }
        cellProg.endStep()
    }


    logger.info("Export data to table")

    logger.info("Join Results with Geometry")
    sql.execute("CREATE INDEX ON LDAY(IDRECEIVER);")

    sql.execute("drop table if exists LDRONE_GEOM;")
    sql.execute("create table LDRONE_GEOM  as select a.TIME ,a.IDRECEIVER, b.THE_GEOM, " +
            "a.Hz50,a.Hz63,a.Hz80, " +
            "a.Hz100,a.Hz125,a.Hz160," +
            "a.Hz200, a.Hz250, a.Hz315," +
            "a.Hz400, a.Hz500, " +
            "a.Hz630,a.Hz800, " +
            "a.Hz1000,a.Hz1250,a.Hz1600," +
            "a.Hz2000, a.Hz2500, a.Hz3150," +
            "a.Hz4000, a.Hz5000, " +
            "a.Hz6300,a.Hz8000, " +
            "a.Hz10000" +
            " FROM LDAY a ,MICROPHONES b where a.IDRECEIVER = b." + receiversPkName + " ORDER BY TIME, IDRECEIVER")
    // Add primary key constraint to check for duplicates
    logger.info("Add primary key on output table")
    sql.execute("ALTER TABLE LDRONE_GEOM ALTER COLUMN TIME SET NOT NULL")
    sql.execute("ALTER TABLE LDRONE_GEOM ALTER COLUMN IDRECEIVER SET NOT NULL")
    sql.execute("ALTER TABLE LDRONE_GEOM ADD PRIMARY KEY (TIME, IDRECEIVER)")

    logger.info("Done !")

    storageFactory.closeWriteThread()
    // Zip all NoiseModellong Get Rays files in folder and delete others
    // FileOutputStream fos = new FileOutputStream("/home/pierre/Desktop/TOOLS/NoiseModelling_3.3.1_Drone/data_dir/data/DEV3912/DEBUG/" + "Rays.zip")
    // ZipOutputStream zipOut = new ZipOutputStream(fos)
    // for (String srcFile : srcFiles) {
    //     File fileToZip = new File(new File("/home/pierre/Desktop/TOOLS/NoiseModelling_3.3.1_Drone/data_dir/data/DEV3912/DEBUG/" + srcFile).absolutePath)
    //     FileInputStream fis = new FileInputStream(fileToZip)
    //     ZipEntry zipEntry = new ZipEntry(fileToZip.getName())
    //    zipOut.putNextEntry(zipEntry)

    //    byte[] bytes = new byte[1024]
    //    int length
    //    while ((length = fis.read(bytes)) >= 0) {
    //        zipOut.write(bytes, 0, length)
    //    }
    //    fis.close()
    //    fileSuccessfullyDeleted = new File("/home/pierre/Desktop/TOOLS/NoiseModelling_3.3.1_Drone/data_dir/data/DEV3912/DEBUG/" + srcFile).delete()
    //    System.println(srcFile + ' has been added to the zip file')
    // }
    // zipOut.close()
    //fos.close()
    long computationTime = System.currentTimeMillis() - start
    logger.info(String.format("Calculation done in %d seconds, %d milliseconds by receiver (%d receivers)", (computationTime / 1000) as Long, (computationTime / receivers.size()) as Long, receivers.size() as Integer))

    return "Calculation Done ! LDRONE_GEOM has been created !"

}

static double[] sumLinearArray(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array")
    } else {
        double[] sum = new double[array1.length];

        for (int i = 0; i < array1.length; ++i) {
            sum[i] = array1[i] + array2[i]
        }

        return sum;
    }
}





