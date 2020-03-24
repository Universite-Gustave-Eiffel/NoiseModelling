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
 * @Author Hesry Quentin, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.cts.crs.CRSException
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import javax.xml.stream.XMLStreamException
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.SQLException

title = 'Calculation of the Lden map from the road noise emission table'
description = 'Calculation of the Lden map from the road noise emission table (DEN format, see input details). </br> Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed. ' +
        '</br> </br> <b> The output table is called : LDEN_GEOM </b> ' +
        'and contain : </br>' +
        '-  <b> IDRECEIVER  </b> : an identifier (INTEGER, PRIMARY KEY). </br>' +
        '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT, LINESTRING, MULTILINESTRING). According to CNOSSOS-EU, you need to set a height of 0.05 m for a road traffic emission.</br> ' +
        '-  <b> Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT).'

inputs = [
        tableBuilding     : [name       : 'Buildings table name', title: 'Buildings table name',
                             description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                             type       : String.class],
        tableSources      : [name       : 'Sources table name', title: 'Sources table name',
                             description: '<b>Name of the Sources table.</b></br>  ' +
                                     '</br>  The table shall contain : </br> ' +
                                     '- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY). </br> ' +
                                     '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT, LINESTRING, MULTILINESTRING). According to CNOSSOS-EU, you need to set a height of 0.05 m for a road traffic emission.</br> ' +
                                     '- <b> LWD63, LWD125, LWD250, LWD500, LWD1000, LWD2000, LWD4000, LWD8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT). </br> ' +
                                     '- <b> LWE* </b> : 8 columns giving the evening emission sound level for each octave band (FLOAT).</br> ' +
                                     '- <b> LWN* </b> : 8 columns giving the night emission sound level for each octave band (FLOAT).</br> ' +
                                     '</br> </br> <b> This table can be generated from the WPS Block "Road_Emission_From_DEN". </b>',
                             type       : String.class],
        tableReceivers    : [name: 'Receivers table name', title: 'Receivers table name',
                             description: '<b>Name of the Receivers table.</b></br>  ' +
                                     '</br>  The table shall contain : </br> ' +
                                     '- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY). </br> ' +
                                     '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT).</br> ' +
                                     '</br> </br> <b> This table can be generated from the WPS Blocks in the "Receivers" folder. </b>',
                             type: String.class],
        tableDEM          : [name: 'DEM table name', title: 'DEM table name',
                             description: '<b>Name of the Digital Elevation Model table.</b></br>  ' +
                                     '</br>The table shall contain : </br> ' +
                                     '- <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT).</br> ' +
                                     '</br> </br> <b> This table can be generated from the WPS Block "AscToDem". </b>',
                             min: 0, max: 1, type: String.class],
        tableGroundAbs      : [name: 'Ground absorption table name', title: 'Ground absorption table name',
                             description: '<b>Name of the surface/ground acoustic absorption table.</b></br>  ' +
                                     '</br>The table shall contain : </br> ' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the sources (POLYGON or MULTIPOLYGON).</br> ' +
                                     '- <b> G </b> : the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft).</br> ',
                               min: 0, max: 1, type: String.class],
        paramWallAlpha    : [name: 'wallAlpha', title: 'Wall absorption coefficient',
                             description: 'Wall absorption coefficient (FLOAT between 0 : fully absorbent and strictly less than 1 : fully reflective)' +
                                     '</br> </br> <b> Default value : 0.1 </b> ',
                             min: 0, max: 1, type: String.class],
        confReflOrder     : [name: 'Order of reflexion', title: 'Order of reflexion',
                             description: 'Maximum number of reflections to be taken into account (INTEGER).' +
                                     '</br> </br> <b> Default value : 1 </b>' ,
                                     min: 0, max: 1, type: String.class],
        confMaxSrcDist    : [name: 'Maximum source-receiver distance', title: 'Maximum source-receiver distance',
                             description: 'Maximum distance between source and receiver (FLOAT, in meters).' +
                                '</br> </br> <b> Default value : 150 </b>',
                                min: 0, max: 1, type: String.class],
        confMaxReflDist   : [name: 'Maximum source-reflexion distance', title: 'Maximum source-reflexion distance',
                             description: 'Maximum reflection distance from the source (FLOAT, in meters).' +
                                     '</br> </br> <b> Default value : 50 </b>',
                             min: 0, max: 1, type: String.class],
        confThreadNumber  : [name: 'Thread number', title: 'Thread number',
                             description: 'Number of thread to use on the computer (INTEGER).' +
                                     '</br> To set this value, look at the number of cores you have.' +
                                     '</br> If it is set to 0, use the maximum number of cores available.' +
                                     '</br> </br> <b> Default value : 1 </b>',
                             min: 0, max: 1, type: String.class],
        confDiffVertical  : [name: 'Diffraction on vertical edges', title: 'Diffraction on vertical edges',
                             description: 'Compute or not the diffraction on vertical edges.' +
                                     '</br> </br> <b> Default value : false </b>',
                             min: 0, max: 1, type: Boolean.class],
        confDiffHorizontal: [name: 'Diffraction on horizontal edges', title: 'Diffraction on horizontal edges',
                             description: 'Compute or not the diffraction on horizontal edges.' +
                                     '</br> </br> <b> Default value : false </b>',
                             min: 0, max: 1, type: Boolean.class]]

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
    String resultString = null

    // print to command window
    System.out.println('Start : LDEN from Emission')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['tableSources']
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()

    String receivers_table_name = input['tableReceivers']
    // do it case-insensitive
    receivers_table_name = receivers_table_name.toUpperCase()

    String building_table_name = input['tableBuilding']
    // do it case-insensitive
    building_table_name = building_table_name.toUpperCase()


    String dem_table_name = ""
    if (input['tableDEM']) {
        dem_table_name = input['tableDEM']
    }
    // do it case-insensitive
    dem_table_name = dem_table_name.toUpperCase()

    String ground_table_name = ""
    if (input['tableGroundAbs']) {
        ground_table_name = input['tableGroundAbs']
    }
    // do it case-insensitive
    ground_table_name = ground_table_name.toUpperCase()

    int reflexion_order = 0
    if (input['confReflOrder']) {
        reflexion_order = Integer.valueOf(input['confReflOrder'])
    }

    double max_src_dist = 150
    if (input['confMaxSrcDist']) {
        max_src_dist = Double.valueOf(input['confMaxSrcDist'])
    }

    double max_ref_dist = 50
    if (input['confMaxReflDist']) {
        max_ref_dist = Double.valueOf(input['confMaxReflDist'])
    }

    double wall_alpha = 0.1
    if (input['paramWallAlpha']) {
        wall_alpha = Double.valueOf(input['paramWallAlpha'])
    }

    int n_thread = 1
    if (input['confThreadNumber']) {
        n_thread = Integer.valueOf(input['confThreadNumber'])
    }

    boolean compute_vertical_diffraction = false
    if (input['confDiffVertical']) {
        compute_vertical_diffraction = input['confDiffVertical']
    }

    boolean compute_horizontal_diffraction = false
    if (input['confDiffHorizontal']) {
        compute_horizontal_diffraction = input['confDiffHorizontal']
    }

    // -------------------------
    // Initialize some variables
    // -------------------------

    // Attenuation matrix table
    List<ComputeRaysOut.verticeSL> allLevels = new ArrayList<>()
    // Set of already processed receivers
    Set<Long> receivers = new HashSet<>()
    // Spectrum of the sound source
    Map<Integer, double[]> SourceSpectrum = new HashMap<>()

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
    pointNoiseMap.setMaximumError(0.1d)
    // Init Map
    pointNoiseMap.initialize(connection, new EmptyProgressVisitor())

    // --------------------------------------------
    // Initialize NoiseModelling emission part
    // --------------------------------------------

    TrafficPropagationProcessDataDENFactory TrafficPropagationProcessDataDENFactory = new TrafficPropagationProcessDataDENFactory()
    pointNoiseMap.setPropagationProcessDataFactory(TrafficPropagationProcessDataDENFactory)


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
            System.println("Compute... " + 100*k++/fullGridSize + " % ")

            // Run ray propagation
            IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers)

            // Return results with level spectrum for each source/receiver tuple
            if (out instanceof ComputeRaysOut) {
                ComputeRaysOut cellStorage = (ComputeRaysOut) out
                // Set attenuation matrix values
                allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel())

                // here you can eventually export the scene in kml format
                //exportScene(String.format(resultPath+"/scene_%d_%d.kml", i, j), cellStorage.inputData.freeFieldFinder, cellStorage);

                cellStorage.receiversAttenuationLevels.each { v ->
                    // Get global value in dB
                    //double globalDbValue = ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(v.value)))
                    // Get id of the source
                    def idSource = out.inputData.SourcesPk.get(v.sourceId)
                    // Set sound sources values
                    double[] w_spectrum = ComputeRays.wToDba(out.inputData.wjSourcesDEN.get(idSource))
                    SourceSpectrum.put(v.sourceId as Integer, w_spectrum)
                }
            }
        }
    }


    System.out.println('Intermediate  time : ' + TimeCategory.minus(new Date(), start))

    System.println("Combine attenuation matrix with sound level of the sources... ")
    // Iterate over attenuation matrix
    Map<Integer, double[]> soundLevels = new HashMap<>()
    k = 0
    int currentVal = 0
    for (int i = 0; i < allLevels.size(); i++) {

        k++
        currentVal = ProgressBar(Math.round(10*i/allLevels.size()).toInteger(),currentVal)
        // get attenuation matrix value
        double[] soundLevel = allLevels.get(i).value

        //get id from receiver and sound sources
        int idReceiver = (Integer) allLevels.get(i).receiverId
        int idSource = (Integer) allLevels.get(i).sourceId
        System.println(idReceiver)
        System.println(idSource)
        System.println(soundLevel)

        // if any attenuation matrix value is set to NaN
        if (!Double.isNaN(soundLevel[0]) && !Double.isNaN(soundLevel[1])
                && !Double.isNaN(soundLevel[2]) && !Double.isNaN(soundLevel[3])
                && !Double.isNaN(soundLevel[4]) && !Double.isNaN(soundLevel[5])
                && !Double.isNaN(soundLevel[6]) && !Double.isNaN(soundLevel[7])) {

            if (soundLevels.containsKey(idReceiver)) {
                // apply A ponderation
                //soundLevel = DBToDBA(soundLevel)
                // add Leq value to the pre-existing sound level on this receiver

                soundLevel = ComputeRays.sumDbArray(sumArraySR(soundLevel, SourceSpectrum.get(idSource)), soundLevels.get(idReceiver))
                soundLevels.replace(idReceiver, soundLevel)
            } else {
                // apply A ponderation
                //soundLevel = DBToDBA(soundLevel)
                // add a new Leq value on this receiver
                soundLevels.put(idReceiver, sumArraySR(soundLevel, SourceSpectrum.get(idSource)))
            }

        } else {
            System.err.println("Warning : NaN value detected on the receiver :" + idReceiver + "and the sound source :" + idSource)
        }
    }


    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Drop table LDEN if exists
    sql.execute("drop table if exists LDEN;")
    // Create table table LDEN
    sql.execute("create table LDEN (IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")

    // Insert value to the table LDEN
    def qry = 'INSERT INTO LDEN(IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?);'
    sql.withBatch(100, qry) { ps ->
        for (s in soundLevels) {
            ps.addBatch(s.key as Integer,
                    s.value[0] as Double, s.value[1] as Double, s.value[2] as Double,
                    s.value[3] as Double, s.value[4] as Double, s.value[5] as Double,
                    s.value[6] as Double, s.value[7] as Double)

        }
    }

    // Drop table LDEN_GEOM if exists
    sql.execute("drop table if exists LDEN_GEOM;")
    // Associate Geometry column to the table LDEN
    sql.execute("create table LDEN_GEOM  as select a.IDRECEIVER, b.*, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000 FROM "+receivers_table_name+" b LEFT JOIN LDEN a ON a.IDRECEIVER = b.PK;")

    // Drop temporary tables
    sql.execute("drop table if exists LDEN;")

    resultString = "Calculation Done ! The table LDEN_GEOM has been created."

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : LDEN from Emission')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}

/**
 * Apply A ponderation to an octave band array from 63 to 8000 Hz
 * @param db
 * @return db
 */
static double[] DBToDBA(double[] db) {
    double[] dbA = [-26.2, -16.1, -8.6, -3.2, 0, 1.2, 1.0, -1.1]
    for (int i = 0; i < db.length; ++i) {
        db[i] = db[i] + dbA[i]
    }
    return db

}

/**
 * Sum two Array "octave band by octave band"
 * @param array1
 * @param array2
 * @return sum of to array
 */
double[] sumArraySR(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array")
    } else {
        double[] sum = new double[array1.length]

        for (int i = 0; i < array1.length; ++i) {
            sum[i] = (array1[i]) + (array2[i])
        }

        return sum
    }
}

/**
 * Class to read sound sources
 */
class TrafficPropagationProcessDataDEN extends PropagationProcessData {

    public List<double[]> wjSourcesDEN = new ArrayList<>()
    public Map<Long, Integer> SourcesPk = new HashMap<>()


    TrafficPropagationProcessDataDEN(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    int idSource = 0

    /**
     * Read Sound sources table and add to wjSourcesDEN variable
     * @param pk
     * @param geom
     * @param rs
     * @throws SQLException
     */
    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)
        SourcesPk.put(pk, idSource++)

        // Read average 24h traffic
        double[] ld = [ComputeRays.dbaToW(rs.getDouble('LWD63')),
                       ComputeRays.dbaToW(rs.getDouble('LWD125')),
                       ComputeRays.dbaToW(rs.getDouble('LWD250')),
                       ComputeRays.dbaToW(rs.getDouble('LWD500')),
                       ComputeRays.dbaToW(rs.getDouble('LWD1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD8000'))]

        double[] le = [ComputeRays.dbaToW(rs.getDouble('LWE63')),
                       ComputeRays.dbaToW(rs.getDouble('LWE125')),
                       ComputeRays.dbaToW(rs.getDouble('LWE250')),
                       ComputeRays.dbaToW(rs.getDouble('LWE500')),
                       ComputeRays.dbaToW(rs.getDouble('LWE1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE8000'))]

        double[] ln = [ComputeRays.dbaToW(rs.getDouble('LWN63')),
                       ComputeRays.dbaToW(rs.getDouble('LWN125')),
                       ComputeRays.dbaToW(rs.getDouble('LWN250')),
                       ComputeRays.dbaToW(rs.getDouble('LWN500')),
                       ComputeRays.dbaToW(rs.getDouble('LWN1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN8000'))]

        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]

        int idFreq = 0
        // Combine day evening night sound levels
        for (int freq : PropagationProcessPathData.freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }
        wjSourcesDEN.add(lden)
    }

    @Override
    double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesDEN.get(sourceId)
    }
}


/**
 * ???
 */
class TrafficPropagationProcessDataDENFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessDataDEN(freeFieldFinder)
    }
}

/**
 * Export scene to kml format
 * @param name
 * @param manager
 * @param result
 * @return
 * @throws IOException
 */
def static exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
    try {
        FileOutputStream outData = new FileOutputStream(name)
        KMLDocument kmlDocument = new KMLDocument(outData)
        kmlDocument.setInputCRS("EPSG:2154")
        kmlDocument.writeHeader()
        if (manager != null) {
            kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices())
        }
        if (result != null) {
            kmlDocument.writeRays(result.getPropagationPaths())
        }
        if (manager != null && manager.isHasBuildingWithHeight()) {
            kmlDocument.writeBuildings(manager)
        }
        kmlDocument.writeFooter()
    } catch (XMLStreamException | CRSException ex) {
        throw new IOException(ex)
    }
}



/**
 * Spartan ProgressBar
 * @param newVal
 * @param currentVal
 * @return
 */
static int ProgressBar(int newVal, int currentVal)
{
    if(newVal != currentVal) {
        currentVal = newVal
        System.print( 10*currentVal + '% ... ')
    }
    return currentVal
}