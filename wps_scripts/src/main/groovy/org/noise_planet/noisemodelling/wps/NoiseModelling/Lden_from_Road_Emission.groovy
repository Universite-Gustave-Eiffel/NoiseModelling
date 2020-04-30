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
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import java.sql.Connection
import java.sql.SQLException

title = 'Calculation of the Lden map from the road noise emission table'
description = 'Calculation of the Lden map from the road noise emission table (DEN format, see input details). </br> Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed. ' +
        '</br> </br> <b> The output table is called : LDEN_GEOM </b> ' +
        'and contain : </br>' +
        '-  <b> IDRECEIVER  </b> : an identifier (INTEGER, PRIMARY KEY). </br>' +
        '- <b> THE_GEOM </b> : the 3D geometry of the receivers (POINT).</br> ' +
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
                                     '</br> </br> <b> This table can be generated from the WPS Block "Road_Emission_from_Traffic". </b>',
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
                                     '</br> </br> <b> This table can be generated from the WPS Block "Import_Asc_File". </b>',
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

    //Load GeneralTools.groovy
    File  sourceFile = new File(new File("").absolutePath+"/data_dir/scripts/wpsTools/GeneralTools.groovy")
    //if we are in dev, the path is not the same as for geoserver
    if (new File("").absolutePath.substring(new File("").absolutePath.length() - 11) == 'wps_scripts') sourceFile = new File(new File("").absolutePath+"/src/main/groovy/org/noise_planet/noisemodelling/wpsTools/GeneralTools.groovy")

    // Get external tools
    Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile)
    GroovyObject tools = (GroovyObject) groovyClass.newInstance()

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
    List<ComputeRaysOut.VerticeSL> allLevels = new ArrayList<>()
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

    WpsPropagationProcessDataFactory wpsPropagationProcessDataFactory =  new WpsPropagationProcessDataFactory()
    pointNoiseMap.setPropagationProcessDataFactory(wpsPropagationProcessDataFactory)

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
        currentVal = tools.invokeMethod("ProgressBar", [Math.round(10 * i / allLevels.size()).toInteger(), currentVal])
        // get attenuation matrix value
        double[] soundLevel = allLevels.get(i).value

        //get id from receiver and sound sources
        int idReceiver = (Integer) allLevels.get(i).receiverId
        int idSource = (Integer) allLevels.get(i).sourceId

        // if any attenuation matrix value is set to NaN
        if (!Double.isNaN(soundLevel[0]) && !Double.isNaN(soundLevel[1])
                && !Double.isNaN(soundLevel[2]) && !Double.isNaN(soundLevel[3])
                && !Double.isNaN(soundLevel[4]) && !Double.isNaN(soundLevel[5])
                && !Double.isNaN(soundLevel[6]) && !Double.isNaN(soundLevel[7])) {

            if (soundLevels.containsKey(idReceiver)) {
                // apply A ponderation
                //soundLevel = DBToDBA(soundLevel)
                // add Leq value to the pre-existing sound level on this receiver
                double[] sumArray = tools.invokeMethod("sumArraySR", [soundLevel, SourceSpectrum.get(idSource)])
                soundLevel = ComputeRays.sumDbArray(sumArray, soundLevels.get(idReceiver))
                soundLevels.replace(idReceiver, soundLevel)
            } else {
                // apply A ponderation
                //soundLevel = DBToDBA(soundLevel)
                // add a new Leq value on this receiver
                double[] sumArray =  tools.invokeMethod("sumArraySR", [soundLevel, SourceSpectrum.get(idSource)])
                soundLevels.put(idReceiver, sumArray)
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
 * Read source database and compute the sound emission spectrum of roads sources
 * */
class WpsPropagationProcessData extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesD = new ArrayList<>()
    public List<double[]> wjSourcesE = new ArrayList<>()
    public List<double[]> wjSourcesN = new ArrayList<>()
    public List<double[]> wjSourcesDEN = new ArrayList<>()

    public Map<Long, Integer> SourcesPk = new HashMap<>()

    public String inputFormat = 'EmissionDEN'
    int idSource = 0

    WpsPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat
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

class WpsPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new WpsPropagationProcessData(freeFieldFinder)
    }
}