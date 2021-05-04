/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 * @Author Pierre Aumond, Universite Gustave Eiffel
 */
package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
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
import org.noise_planet.noisemodelling.emission.RoadSourceParametersCnossos
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

title = 'Get all rays'
description = 'Get all rays between sources and receivers. It will keep the results in a Zip file.' +
        '</br>This zip file contains : ' +
        '</br>- a gunzip file with the rays.' +
        '</br>- a param file with the parameter of the calculation.' +
        '</br>- the table that were used by the program in a geojson format.' +
        '</br> </br> <b>The output is called : Rays.zip and it saved on your hardrive at the Folder path destination.</b> \''

inputs = [
        exportPath        : [name       : 'Export folder',
                             title      : 'Path of the folder',
                             description: 'Path of the folder where you want to export the rays.  </br> For example : c:/home/outputRays/ ',
                             type       : String.class],
        tableBuilding     : [name       : 'Buildings table name', title: 'Buildings table name',
                             description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                             type       : String.class],
        roadsTableName: [name                                                                           : 'Roads table name', title: 'Roads table name', description: "<b>Name of the Roads table.</b>  </br>  " +
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
                "</br> </br> <b> This table can be generated from the WPS Block 'Import_OSM'. </b>.", type: String.class],
        tableReceivers    : [name       : 'Receivers table name', title: 'Receivers table name',
                             description: '<b>Name of the Receivers table.</b></br>  ' +
                                     '</br>  The table shall contain : </br> ' +
                                     '- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY). </br> ' +
                                     '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT).</br> ' +
                                     '</br> </br> <b> This table can be generated from the WPS Blocks in the "Receivers" folder. </b>',
                             type       : String.class],
        tableDEM          : [name       : 'DEM table name', title: 'DEM table name',
                             description: '<b>Name of the Digital Elevation Model table.</b></br>  ' +
                                     '</br>The table shall contain : </br> ' +
                                     '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT).</br> ' +
                                     '</br> </br> <b> This table can be generated from the WPS Block "Import_Asc_File". </b>',
                             min        : 0, max: 1, type: String.class],
        tableGroundAbs    : [name       : 'Ground absorption table name', title: 'Ground absorption table name',
                             description: '<b>Name of the surface/ground acoustic absorption table.</b></br>  ' +
                                     '</br>The table shall contain : </br> ' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the sources (POLYGON or MULTIPOLYGON).</br> ' +
                                     '- <b> G </b> : the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft).</br> ',
                             min        : 0, max: 1, type: String.class],
        paramWallAlpha    : [name       : 'wallAlpha', title: 'Wall absorption coefficient',
                             description: 'Wall absorption coefficient (FLOAT between 0 : fully absorbent and strictly less than 1 : fully reflective)' +
                                     '</br> </br> <b> Default value : 0.1 </b> ',
                             min        : 0, max: 1, type: String.class],
        confReflOrder     : [name       : 'Order of reflexion', title: 'Order of reflexion',
                             description: 'Maximum number of reflections to be taken into account (INTEGER).' +
                                     '</br> </br> <b> Default value : 1 </b>',
                             min        : 0, max: 1, type: String.class],
        confMaxSrcDist    : [name       : 'Maximum source-receiver distance', title: 'Maximum source-receiver distance',
                             description: 'Maximum distance between source and receiver (FLOAT, in meters).' +
                                     '</br> </br> <b> Default value : 150 </b>',
                             min        : 0, max: 1, type: String.class],
        confMaxReflDist   : [name       : 'Maximum source-reflexion distance', title: 'Maximum source-reflexion distance',
                             description: 'Maximum reflection distance from the source (FLOAT, in meters).' +
                                     '</br> </br> <b> Default value : 50 </b>',
                             min        : 0, max: 1, type: String.class],
        confThreadNumber  : [name       : 'Thread number', title: 'Thread number',
                             description: 'Number of thread to use on the computer (INTEGER).' +
                                     '</br> To set this value, look at the number of cores you have.' +
                                     '</br> If it is set to 0, use the maximum number of cores available.' +
                                     '</br> </br> <b> Default value : 1 </b>',
                             min        : 0, max: 1, type: String.class],
        confDiffVertical  : [name       : 'Diffraction on vertical edges', title: 'Diffraction on vertical edges',
                             description: 'Compute or not the diffraction on vertical edges.' +
                                     '</br> </br> <b> Default value : false </b>',
                             min        : 0, max: 1, type: Boolean.class],
        confDiffHorizontal: [name       : 'Diffraction on horizontal edges', title: 'Diffraction on horizontal edges',
                             description: 'Compute or not the diffraction on horizontal edges.' +
                                     '</br> </br> <b> Default value : false </b>',
                             min        : 0, max: 1, type: Boolean.class],
        exportReceiverRays: [name       : 'Export Rays for one receiver', title: 'Export Rays for one receiver',
                             description: 'PK of the receiver for which you want to export the rays (INTEGER).' +
                                     '</br> If the value is set to -1, no rays will be exported.' +
                                     '</br> The export format is kml (compatible with google earth).' +
                                     '</br> </br> <b> Default value : -1 </b>',
                             min        : 0, max: 1, type: Integer.class]]

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

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString

    // print to command window
    System.out.println('Start : Get Rayz')
    def start = new Date()

    // -------------------------
    // Initialize some variables
    // -------------------------

    def srcFiles = []
    GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
    // create properties file
    Properties properties = new Properties()
    List<ComputeRaysOutAttenuation.VerticeSL> allLevels = new ArrayList<>()
    // Set of already processed receivers
    Set<Long> receivers = new HashSet<>()
    // All rays storage
    String pkName

    // -------------------
    // Get every inputs
    // -------------------

    int reflexion_order = 0
    if (input['confReflOrder']) {
        reflexion_order = Integer.valueOf(input['confReflOrder'])
    }
    properties.setProperty("confReflOrder", reflexion_order.toString())

    double max_src_dist = 150
    if (input['confMaxSrcDist']) {
        max_src_dist = Double.valueOf(input['confMaxSrcDist'])
    }
    properties.setProperty("confMaxSrcDist", max_src_dist.toString())

    double max_ref_dist = 50
    if (input['confMaxReflDist']) {
        max_ref_dist = Double.valueOf(input['confMaxReflDist'])
    }
    properties.setProperty("confMaxReflDist", max_ref_dist.toString())

    double wall_alpha = 0.1
    if (input['paramWallAlpha']) {
        wall_alpha = Double.valueOf(input['paramWallAlpha'])
    }
    properties.setProperty("paramWallAlpha", wall_alpha.toString())

    int n_thread = 1
    if (input['confThreadNumber']) {
        n_thread = Integer.valueOf(input['confThreadNumber'])
    }
    properties.setProperty("confThreadNumber", n_thread.toString())

    boolean compute_vertical_diffraction = false
    if (input['confDiffVertical']) {
        compute_vertical_diffraction = input['confDiffVertical']
    }
    properties.setProperty("confDiffVertical", compute_vertical_diffraction.toString())

    boolean compute_horizontal_diffraction = false
    if (input['confDiffHorizontal']) {
        compute_horizontal_diffraction = input['confDiffHorizontal']
    }
    properties.setProperty("confDiffHorizontal", compute_horizontal_diffraction.toString())

    int exportReceiverRays = -1
    if (input['exportReceiverRays']) {
        exportReceiverRays = Integer.valueOf(input['exportReceiverRays'])
    }

    // Default values in NoiseModelling
    properties.setProperty("paramTemp", '15')
    properties.setProperty("paramHum", '70')

    String working_dir = input['exportPath']

    // --------------------------------------------
    // Initialize NoiseModelling propagation part
    // --------------------------------------------

    String sources_table_name = input['roadsTableName']
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()
    String fileName = "sources.geojson"
    srcFiles.add(fileName)
    // export table to geojson
    geoJsonDriver.exportTable(connection, sources_table_name, new File(working_dir + fileName), new EmptyProgressVisitor())
    // Get pk index
    int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection, sources_table_name)
    if (indexPk > 0) {
        pkName = JDBCUtilities.getFieldName(connection.getMetaData(), sources_table_name, indexPk)
    }
    properties.setProperty("pkSources", pkName)


    String receivers_table_name = input['tableReceivers']
    receivers_table_name = receivers_table_name.toUpperCase()
    fileName = "receivers.geojson"
    srcFiles.add(fileName)
    // export table to geojson
    geoJsonDriver.exportTable(connection, receivers_table_name, new File(working_dir + fileName), new EmptyProgressVisitor())
    // Get pk index
    indexPk = JDBCUtilities.getIntegerPrimaryKey(connection, receivers_table_name)
    if (indexPk > 0) {
        pkName = JDBCUtilities.getFieldName(connection.getMetaData(), receivers_table_name, indexPk)
    }
    properties.setProperty("pkReceivers", pkName)


    String building_table_name = input['tableBuilding']
    building_table_name = building_table_name.toUpperCase()
    fileName = "buildings.geojson"
    srcFiles.add(fileName)
    // export table to geojson
    geoJsonDriver.exportTable(connection, building_table_name, new File(working_dir + fileName), new EmptyProgressVisitor())



    String dem_table_name = ""
    if (input['tableDEM']) {
        dem_table_name = input['tableDEM']
        dem_table_name = dem_table_name.toUpperCase()
        fileName = "dem.geojson"
        srcFiles.add(fileName)
        // export table to geojson
        geoJsonDriver.exportTable(connection, dem_table_name, new File(working_dir + fileName), new EmptyProgressVisitor())
        properties.setProperty("tableDEM", "TRUE")
    }


    String ground_table_name = ""
    if (input['tableGroundAbs']) {
        ground_table_name = input['tableGroundAbs']
        ground_table_name = ground_table_name.toUpperCase()
        fileName = "groundabs.geojson"
        srcFiles.add(fileName)
        // export table to geojson
        geoJsonDriver.exportTable(connection, ground_table_name, new File(working_dir + fileName), new EmptyProgressVisitor())
        properties.setProperty("tableGroundAbs", "TRUE")
    }

    // --------------------------------------------
    // Initialize NoiseModelling propagation part
    // --------------------------------------------
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

    pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
    pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
    pointNoiseMap.setWallAbsorption(wall_alpha)
    pointNoiseMap.setThreadCount(n_thread)

    // Do not propagate for low emission or far away sources
    // Maximum error in dB
    //pointNoiseMap.setMaximumError(0.0d)
    // Init Map
    pointNoiseMap.initialize(connection, new EmptyProgressVisitor())


    // --------------------------------------------
    // Initialize NoiseModelling emission part
    // --------------------------------------------

    TrafficRayzPropagationProcessDataFactory trafficRayzPropagationProcessDataFactory = new TrafficRayzPropagationProcessDataFactory()
    pointNoiseMap.setPropagationProcessDataFactory(trafficRayzPropagationProcessDataFactory)

    // --------------------------------------------
    // Set storage parameters
    // --------------------------------------------

    // Init custom input in order to compute more than just attenuation
    PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory()
    pointNoiseMap.setComputeRaysOutFactory(storageFactory)
    storageFactory.setWorkingDir(working_dir)
    if (exportReceiverRays!=-1) storageFactory.setExportReceiverRays(exportReceiverRays)
    srcFiles.add("rays.gz")
    storageFactory.openPathOutputFile(new File(working_dir + "/rays.gz").absolutePath)

    // --------------------------------------------
    // Run Calculations
    // --------------------------------------------

    // Init ProgressLogger (loading bar)
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1)
    ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim())
    int fullGridSize = pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim()

    System.println("Start calculation... ")

    // Iterate over computation areas
    int k=0
    for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
        for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
            System.println("Compute... " + 100 * k++ / fullGridSize + " % ")

            // Run ray propagation
            IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers)

            // Return results with level spectrum for each source/receiver tuple
            if (out instanceof ComputeRaysOutAttenuation) {
                // Set attenuation matrix values
                allLevels.addAll(((ComputeRaysOutAttenuation) out).getVerticesSoundLevel())
            }
        }
    }
    System.println("Done - 100 % ")

    //Thread.sleep(3000)

    storageFactory.closeWriteThread()

    // Create properties file and save it
    String newAppConfigPropertiesFile = working_dir + "NM.properties"
    FileWriter fileWriter = new FileWriter(newAppConfigPropertiesFile)
    properties.store(fileWriter, "store to properties file")
    srcFiles.add("NM.properties")
    fileWriter.close()

    // Zip all NoiseModellong Get Rays files in folder and delete others
    FileOutputStream fos = new FileOutputStream(working_dir + "Rays.zip")
    ZipOutputStream zipOut = new ZipOutputStream(fos)
    for (String srcFile : srcFiles) {
        File fileToZip = new File(new File(working_dir + srcFile).absolutePath)
        FileInputStream fis = new FileInputStream(fileToZip)
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName())
        zipOut.putNextEntry(zipEntry)

        byte[] bytes = new byte[1024]
        int length
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
        fileSuccessfullyDeleted = new File(working_dir + srcFile).delete()
        System.println(srcFile + ' has been added to the zip file')
    }
    zipOut.close()
    fos.close()



    resultString = "Done ! The Rayz.zip file has been created in the folder : " + working_dir

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : LDEN from Emission')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}



/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 * remove receiverpath or put to keep rays or not
 */
@CompileStatic
class PropagationPathStorage extends ComputeRaysOutAttenuation {
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
        IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
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

            System.out.println("The number of stored rays is  : " + nRays)
            dataOutputStream.flush()
            gzipOutputStream.close()
            //fileOutputStream.close()
            if (exportReceiverRays > 0)   {
                kmlDocument.writeFooter()
                System.out.println("A kml file has been exported for receiver " + exportReceiverRays)
            }
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
 * TrafficRayzPropagationProcessDataFactory
 */
class TrafficRayzPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficRayzPropagationProcessData(freeFieldFinder);
    }

    @Override
    void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException {

    }
}

/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
@CompileStatic
class TrafficRayzPropagationProcessData extends PropagationProcessData {
    // Lden values
    static List<Integer> freq_lvl = Arrays.asList(PropagationProcessPathData.asOctaveBands(PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
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
        double tvD = rs.getDouble("TV_D")
        double tvE = rs.getDouble("TV_E")
        double tvN = rs.getDouble("TV_N")

        // todo remove this...
        double hvD = 0.01*rs.getDouble("HV_D")*rs.getDouble("TV_D")
        double hvE = 0.01*rs.getDouble("HV_E")*rs.getDouble("TV_E")
        double hvN = 0.01*rs.getDouble("HV_N")*rs.getDouble("TV_N")

        double lvSpeedD = rs.getDouble("LV_SPD_D")
        double lvSpeedE = rs.getDouble("LV_SPD_E")
        double lvSpeedN = rs.getDouble("LV_SPD_N")

        double hvSpeedD = rs.getDouble("HV_SPD_D")
        double hvSpeedE = rs.getDouble("HV_SPD_E")
        double hvSpeedN = rs.getDouble("HV_SPD_N")

        // Annual Average Daily Flow (AADF) estimates
        String pavement = rs.getString("PVMT")

        // Compute day average level
        double[] ld = new double[freq_lvl.size()];
        double[] le = new double[freq_lvl.size()];
        double[] ln = new double[freq_lvl.size()];

        double Temperature = 20.0d
        double Ts_stud = 0
        double Pm_stud = 0
        double Junc_dist = 0
        int Junc_type = 0


        int idFreq = 0
        for (int freq : freq_lvl) {
            RoadSourceParametersCnossos rsParametersCnossos = new RoadSourceParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                    lvSpeedD, Math.max(0, tvD - hvD), hvD, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }



        idFreq = 0
        for (int freq : freq_lvl) {
            RoadSourceParametersCnossos rsParametersCnossos = new RoadSourceParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                    lvSpeedE, Math.max(0, tvE - hvE), hvE, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }

        idFreq = 0
        for (int freq : freq_lvl) {
            RoadSourceParametersCnossos rsParametersCnossos = new RoadSourceParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                    lvSpeedN, Math.max(0, tvN - hvN), hvN, 0, 0, 0, freq, Temperature,
                    pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
            ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
        }

        double[] lden = new double[freq_lvl.size()]

        idFreq = 0
        for (int freq : freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] +
                    4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) +
                    8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }

        wjSourcesDEN.add(lden)

    }

    @Override
    double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesDEN.get(sourceId)
    }
}

