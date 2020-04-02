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
 * @Author Pierre Aumond, Univ Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Dynamic_Tools

import groovy.sql.Sql
import groovy.time.TimeCategory
import org.h2gis.api.EmptyProgressVisitor
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RSParametersDynamic


import geoserver.GeoServer
import geoserver.catalog.Store


import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection

import org.h2gis.utilities.wrapper.*

import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap


import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.h2gis.api.ProgressVisitor

import java.sql.SQLException

title = 'Road traffic probabilistic modeling'
description = 'Compute road traffic probabilistic modeling as describe in <b>Aumond, P., Jacquesson, L., & Can, A. (2018). Probabilistic modeling framework for multisource sound mapping. Applied Acoustics, 139, 34-43. </b>.' +
        '</br>The user can indicate the number of iterations he wants the model to calculate.' +
        '</br> </br> <b> The first output table is called : L_PROBA_GEOM </b> ' +
        'and contain : </br>' +
        '-  <b> I  </b> : The i iteration (INTEGER).</br>' +
        '-  <b> IDRECEIVER  </b> : an identifier (INTEGER, PRIMARY KEY). </br>' +
        '- <b> THE_GEOM </b> : the 3D geometry of the receivers (POINT). </br> ' +
        '-  <b> Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT).'

inputs = [
        tableBuilding     : [name       : 'Buildings table name', title: 'Buildings table name',
                             description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                             type       : String.class],
        tableRoads    : [name                                                                                 : 'Roads table name', title: 'Roads table name', description: "<b>Name of the Roads table.</b>  </br>  " +
                "<br>  The table shall contain : </br>" +
                "- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)<br/>" +
                "- <b> TV_D </b> : Hourly average light and heavy vehicle count (DOUBLE)<br/>" +
                "- <b> HV_D </b> :  Hourly average heavy vehicle count (DOUBLE)<br/>" +
                "- <b> LV_SPD_D </b> :  Hourly average light vehicle speed (DOUBLE)<br/>" +
                "- <b> HV_SPD_D </b> :  Hourly average heavy vehicle speed  (DOUBLE)<br/>" +
                "- <b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)" +
                "</br> </br> <b> This table can be generated from the WPS Block 'OsmToInputData'. </b>.", type: String.class],
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
                                     '</br> </br> <b> This table can be generated from the WPS Block "AscToDem". </b>',
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
        nIterations       : [name       : 'Iteration number', title: 'Iteration number',
                             description: 'Number of the iterations to compute (INTEGER). </br> </br> <b> Default value : 100 </b>',
                             min        : 0, max: 1, type: Integer.class],
]

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

    // Get external tools
    File sourceFile = new File("src/main/groovy/org/noise_planet/noisemodelling/wpsTools/GeneralTools.groovy")
    Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile)
    GroovyObject tools = (GroovyObject) groovyClass.newInstance()


    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Open sql connection to communicate with the database
    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Traffic Probabilistic Modelling')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    int nIterations = 300
    if (input['nIterations']) {
        nIterations = Integer.valueOf(input['nIterations'])
    }

    String sources_table_name = input['tableRoads']
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
    Object trafficPropagationProcessDataFactory = Class.forName("org.noise_planet.noisemodelling.wpsTools.WpsPropagationProcessDataFactory").newInstance()
    pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory)

    Object trafficPropagationProcessData = Class.forName("org.noise_planet.noisemodelling.wpsTools.WpsPropagationProcessData").newInstance()
    trafficPropagationProcessData.invokeMethod("setInputFormat",["Proba"])

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

            IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers)
            if (out instanceof ComputeRaysOut) {
                allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel())
            }
        }
    }

    System.out.println('Intermediate  time : ' + TimeCategory.minus(new Date(), start))

    System.println("Create the random road traffic table over the number of iterations... ")

    sql.execute("set @grid_density=10;\n" +
            "create table TRAFIC_DENSITY (the_geom geometry, LV int, HV INT, SPEED double, DENSITY_LV double, DENSITY_HV double, DENSITY_TV double) as \n" +
            "    select THE_GEOM, TV_D - HV_D, HV_D , \n" +
            "        case when LV_SPD_D  < 20 then 20 \n" +
            "            else LV_SPD_D  end, \n" +
            "        case when LV_SPD_D  < 20 then 0.001*(TV_D-HV_D)/20 \n" +
            "            else 0.001*(TV_D-HV_D)/LV_SPD_D end, \n" +
            "        case when HV_SPD_D < 20 then 0.001*HV_D/20 \n" +
            "            else 0.001*HV_D/HV_SPD_D  end, \n" +
            "        case when LV_SPD_D< 20 then 0.001*(TV_D)/20 \n" +
            "            else 0.001*(TV_D)/LV_SPD_D end \n" +
            "        from " + sources_table_name + " WHERE (TV_D IS NOT NULL);" +
            "drop table if exists traf_explode;\n" +
            "create table traf_explode as SELECT * FROM ST_Explode('TRAFIC_DENSITY');\n" +
            "alter table traf_explode add length double as select ST_LENGTH(the_geom) ;\n" +
            "drop table grid_traf2 if exists;\n" +
            "create table grid_traf2 (the_geom geometry, SPEED int, DENSITY_LV double, DENSITY_HV double, DENSITY_TV double) as SELECT ST_Tomultipoint(ST_Densify(the_geom, 10)), SPEED, DENSITY_LV, DENSITY_HV, DENSITY_TV from traf_explode;\n" +
            "drop table grid_traf_tot if exists;\n" +
            "create table grid_traf_tot as SELECT * from  ST_explode('grid_traf2');\n" +
            "alter table grid_traf_tot ADD number_veh double as select DENSITY_TV;\n" +
            "drop table grid_traf2 if exists;\n" +
            "drop table CARS2D if exists;\n" +
            "create table CARS2D as SELECT ST_FORCE2D(the_geom) the_geom, speed, density_LV, density_HV, density_TV, explod_id exp_id, number_veh from grid_traf_tot WHERE DENSITY_TV > 0 and DENSITY_TV IS NOT NULL;\n" +
            "alter table CARS2D add column PK serial ;\n" +
            "drop table ROADS_PROBA if exists;\n" +
            "create table ROADS_PROBA as SELECT ST_UPDATEZ(ST_FORCE3D(the_geom),0.05,1) the_geom,ST_Z(ST_AddZ(ST_FORCE3D(the_geom),0.05)) z, speed, density_LV LV, density_HV HV, DENSITY_TV TV, exp_id, number_veh from ST_Explode('CARS2D');\n" +
            "alter table ROADS_PROBA add column PK serial ;\n" +
            "drop table grid_traf_tot if exists;" +
            "drop table CARS2D if exists;" +
            "drop table grid_traf2 if exists;" +
            "drop table if exists traf_explode;" +
            "drop table TRAFIC_DENSITY if exists;")


    Object probaProcessData = Class.forName("org.noise_planet.noisemodelling.wpsTools.ProbabilisticProcessData").newInstance()
    probaProcessData.invokeMethod("setProbaTable",["ROADS_PROBA", sql])

    sql.execute("drop table ROADS_PROBA if exists;")

    System.out.println('Intermediate  time : ' + TimeCategory.minus(new Date(), start))
    System.out.println("Export data to table")

    sql.execute("drop table if exists L_PROBA;")
    sql.execute("create table L_PROBA(IT integer, IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")
    def qry = 'INSERT INTO L_PROBA(IT , IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

    k = 0
    int currentVal = 0
    for (int it = 1; it < nIterations; it++) {
        // Iterate over attenuation matrix
        Map<Integer, double[]> soundLevels = new HashMap<>()
        Map<Integer, double[]> sourceLev = new HashMap<>()


        for (int i = 0; i < allLevels.size(); i++) {

            k++
            currentVal = tools.invokeMethod("ProgressBar", [Math.round(10*i/(allLevels.size()*nIterations)).toInteger(),currentVal])

            // get attenuation matrix value
            double[] soundLevel = allLevels.get(i).value

            //get id from receiver and sound sources
            int idReceiver = (Integer) allLevels.get(i).receiverId
            int idSource = (Integer) allLevels.get(i).sourceId

            // get source SoundLevel
            if (!sourceLev.containsKey(idSource)) {
                double[] carsLevel = probaProcessData.invokeMethod("getCarsLevel",[it, idSource])
                sourceLev.put(idSource, carsLevel)
            }

            if (sourceLev.get(idSource)[0] > 0) {
                // if any attenuation matrix value is set to NaN
                if (!Double.isNaN(soundLevel[0]) && !Double.isNaN(soundLevel[1])
                        && !Double.isNaN(soundLevel[2]) && !Double.isNaN(soundLevel[3])
                        && !Double.isNaN(soundLevel[4]) && !Double.isNaN(soundLevel[5])
                        && !Double.isNaN(soundLevel[6]) && !Double.isNaN(soundLevel[7])) {

                    if (soundLevels.containsKey(idReceiver)) {
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
                }
            }
        }

        sql.withBatch(100, qry) { ps ->
            for (Map.Entry<Integer, double[]> s : soundLevels.entrySet()) {

                ps.addBatch(it as Integer, s.key as Integer,
                        s.value[0] as Double, s.value[1] as Double, s.value[2] as Double,
                        s.value[3] as Double, s.value[4] as Double, s.value[5] as Double,
                        s.value[6] as Double, s.value[7] as Double)

            }
        }
    }


    // Drop table LDEN_GEOM if exists
    sql.execute("drop table if exists L_PROBA_GEOM;")
    // Associate Geometry column to the table LDEN
    sql.execute("CREATE INDEX ON L_PROBA(IDRECEIVER);")
    sql.execute("CREATE INDEX ON RECEIVERS(PK);")
    sql.execute("create table L_PROBA_GEOM  as select a.IT,a.IDRECEIVER, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000  FROM L_PROBA a LEFT JOIN  RECEIVERS b  ON a.IDRECEIVER = b.PK;")


    resultString = "Calculation Done ! The table L_PROBA_GEOM has been created."

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Traffic Probabilistic Modelling')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString
}

