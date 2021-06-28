package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer

/*
 * @Author Pierre Aumond
 */

import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RoadSourceParametersDynamic
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import java.sql.Connection
import java.sql.SQLException

title = 'Compute Dynamic NoiseMap'
description = 'Compute Dynamic NoiseMap from individual moving point sources'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTimeTableName  : [name: 'Sources Time table name', title: 'Sources Time table name',description: 'Column idSource, column idPosition, column idTime', type: String.class],
          sourcesPositionTableName  : [name: 'Sources position table name', title: 'Sources position table name',description: 'Column the_geom, column idPosition', type: String.class],
          receiversTableName: [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName      : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          groundTableName   : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          reflexionOrder    : [name: 'Number of reflexion', title: 'Number of reflexion', description: 'Maximum number of reflexion to consider (default = 0)', min: 0, max: 1, type: String.class],
          maxSrcDistance    : [name: 'Max Source Distance', title: 'Max Source Distance', description: 'Maximum distance to consider a sound source (default = 100 m)', min: 0, max: 1, type: String.class],
          maxRefDistance    : [name: 'Max Reflexion Distance', title: 'Max Reflexion Distance', description: 'Maximum distance to consider a reflexion (default = 50 m)', min: 0, max: 1, type: String.class],
          wallAlpha         : [name: 'wallAlpha', title: 'Wall alpha', description: 'Wall abosrption (default = 0.1)', min: 0, max: 1, type: String.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class],
          computeVertical   : [name: 'Compute vertical diffraction', title: 'Compute vertical diffraction', description: 'Compute or not the vertical diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          computeHorizontal : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          tini   : [name: 'T_ini', title: 'T_ini', description: 'Define the initial time step (default = 1)', min: 0, max: 1, type: String.class],
          tend   : [name: 'T_end', title: 'T_end', description: 'Define the final time step (default = 100)', min: 0, max: 1, type: String.class]]
     // TO DO : mettre plutôt un t_ini et un t_end   
outputs = [result: [name: 'result', title: 'Result', type: String.class]]

/**
 *
 */
class CarsProcessData {

    static double[] thirdOctaveToOctave(double[] values) {
        double[] valueoct = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        for (int i = 0; i < 8; i++) {
            valueoct[i] = 10 * Math.log10(Math.pow(10, values[i * 3 + 1] / 10) + Math.pow(10, values[i * 3 + 2] / 10) + Math.pow(10, values[i * 3 + 3] / 10))
        }
        return valueoct
    }

    double[] getCarLevel(String tablename, Sql sql, int t, int idSource) throws SQLException {
        double[] res_d = [0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]
        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
        //System.println('res_d'+res_d)
        sql.eachRow('SELECT PK, ID_VEH, speed,acc, veh_type ,t FROM ' + tablename + ' WHERE PK = ' + idSource.toString() + ' AND T = ' + t.toString() + ';') { row ->
               
                String veh_type = (String) row[4]
                
                int veh_type_int = 0

                switch(veh_type) {
                    case 'VL':
                        veh_type_int = 1
                        break
                    case 'PL':
                        veh_type_int = 3
                        break
                                           case 'TypeTrolley':
                        veh_type_int = 3
                        break
                    case 'BUS':
                        veh_type_int = 3
                        break
                }
        
                //if (veh_type=="VL") {veh_type_int = 0}
                //if (veh_type=="PL") {veh_type_int = 2}

                int id_veh = (int) row[1]
                double speed = row[2]
                double acc =  row[3]
                
                double Temperature = 20
                int RoadSurface = 0
                boolean Stud = false
                double Junc_dist = 200
                int Junc_type = 1
                int acc_type= 1
                double LwStd= 0
                int kk=0
                for (f in list) {   
                    
                    int FreqParam = f

                    RoadSourceParametersDynamic rsParameters = new RoadSourceParametersDynamic(speed,  acc,  veh_type_int, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,id_veh)
                    //RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  1, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,id_veh)
                    
                    //System.println(rsParameters)
                    rsParameters.setSlopePercentage(0)
                    //System.println('avant')
                    res_d[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)

                    //res_d[kk] = 10 * Math.log10(Math.pow(10, res_d[kk]/ 10) + Math.pow(10, 4)  )

                    //System.println(EvaluateRoadSourceDynamic.evaluate(rsParameters))
                    //System.println('apres')
                    kk++
                }
                 System.println( res_d)
        }
      
        return res_d
    }
}

/** Read source database and compute the sound emission spectrum of roads sources **/
@CompileStatic
class CarsPropagationProcessData extends PropagationProcessData {
    static List<Integer> freq_lvl = Arrays.asList(PropagationProcessPathData.asOctaveBands(PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE));

    protected List<double[]> wjSourcesD = new ArrayList<>()

    public CarsPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

        super.addSource(pk, geom, rs)

        Geometry the_geom = rs.getGeometry("the_geom")
        double db_m63 = 90
        double db_m125 = 90
        double db_m250 = 90
        double db_m500 = 90
        double db_m1000 = 90
        double db_m2000 = 90
        double db_m4000 = 90
        double db_m8000 = 90

        double[] res_d = new double[freq_lvl.size()]

        res_d = [db_m63, db_m125, db_m250, db_m500, db_m1000, db_m2000, db_m4000, db_m8000]

        wjSourcesD.add(ComputeRays.dbaToW(res_d))
    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }


}


class CarsPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new CarsPropagationProcessData(freeFieldFinder)
    }


    @Override
    void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException {

    }
}


def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // -------------------
    // Get inputs
    // -------------------

    String sources_position_table_name = "SOURCES"
    if (input['sourcesPositionTableName']) {
        sources_position_table_name = input['sourcesPositionTableName']
    }
    sources_position_table_name = sources_position_table_name.toUpperCase()

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

    double t_end = 100
    if (input['tend']) {
        t_end = Double.valueOf(input['tend'])
    }

    double t_ini = 1
    if (input['tini']) {
        t_ini = Double.valueOf(input['tini'])
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
    String dbName = "h2gisdb"
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // ----------------------------------
    // Start...
    // ----------------------------------

    System.out.println("Run ...")

    List<ComputeRaysOutAttenuation.VerticeSL> allLevels = new ArrayList<>()

    // Attenuation matrix table
    ArrayList<PropagationPath> propaMap2 = new ArrayList<>()
    // All rays storage

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->

        //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postgis database
        connection = new ConnectionWrapper(connection)

        System.out.println("Connection to the database ok ...")
        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap(building_table_name, sources_position_table_name, receivers_table_name)
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
        pointNoiseMap.setMaximumError(0.1d);

        pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
        pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
        pointNoiseMap.setWallAbsorption(wall_alpha)
        pointNoiseMap.setThreadCount(n_thread)

        // Init custom input in order to compute more than just attenuation
        CarsPropagationProcessDataFactory carsPropagationProcessDataFactory = new CarsPropagationProcessDataFactory()
        pointNoiseMap.setPropagationProcessDataFactory(carsPropagationProcessDataFactory)

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        System.out.println("Init Map ...")
        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>()
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim());

        long start = System.currentTimeMillis()
        System.out.println("Start ...")

        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers)
                if (out instanceof ComputeRaysOutAttenuation) {
                    allLevels.addAll(((ComputeRaysOutAttenuation) out).getVerticesSoundLevel())
                }
            }
        }


        CarsProcessData carsProcessData = new CarsProcessData()

        Sql sql = new Sql(connection)
        //sql.withStatement { stmt -> stmt.setQueryTimeout(60); }
        System.out.println("Export data to table")
        sql.execute("drop table if exists LDAY;")
        sql.execute("create table LDAY (TIME integer, IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")
        def qry = 'INSERT INTO LDAY(TIME , IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

        System.out.println("LDay created")


        Map<Integer, Coordinate> geomReceivers = new HashMap<>()

        sql.eachRow('SELECT PK,ST_X(the_geom),ST_Y(the_geom),ST_Z(the_geom) FROM RECEIVERS;') { row ->
            geomReceivers.put(row[0], new Coordinate(row[1] as Double, row[2] as Double, row[3] as Double))

        }
        System.out.println("l399")

        Map<Integer, double[]> geomFixedSources = new HashMap<>()
        sql.eachRow('SELECT PK, ST_X(the_geom),ST_Y(the_geom),ST_Z(the_geom) FROM '+sources_position_table_name+' ;') { row ->
            geomFixedSources.put((int) row[0], [row[1] as Double, row[2] as Double, row[3] as Double])
        }
        System.out.println("l405")

        sql.execute("CREATE SPATIAL INDEX ON "+sources_position_table_name+"(THE_GEOM);")
                System.out.println("spatial index on sources positions created")
        System.out.println("l409")

        //sql.execute("CREATE SPATIAL INDEX ON "+sources_time_table_name+"(THE_GEOM);")
          //      System.out.println("spatial index on trajectories")

        // as output, closest_point contains as number of lines the number of time steps times the vehicles concerned within sources_time_table_name (TRAJ_SYMUVIA for isntance) trajectory. 
        // PK_TIME is the time steps, PK_POSITION is the indice in the table sources_position_table_name
        sql.execute("drop table if exists closest_point;")
        sql.execute("create table closest_point as SELECT b.pk PK_TIME,b.the_geom, (SELECT a.PK FROM "+sources_position_table_name+" a WHERE ST_EXPAND(b.the_geom,25,25) && a.the_geom  ORDER BY ST_Distance(a.the_geom, b.the_geom) ASC LIMIT 1) PK_POSITION FROM "+sources_time_table_name+" b WHERE b.T >="+t_ini+" AND b.T <="+t_end+" ;")
        System.out.println("closest_point created")


        //sql.execute("drop table if exists Traj_light;")
        //sql.execute("create table Traj_light (PK int PRIMARY KEY, the_geom GEOMETRY, ID int, ID_VEH int, SPEED float, ACC float, T int, VEH_TYPE string) as SELECT b.PK, b.the_geom, b.ID, b.ID_VEH, b.SPEED, b.ACC,   FROM "+sources_time_table_name+" b WHERE WHERE b.T >="+t_ini+" AND b.T <="+t_end+" ; ")

        // attention, Arnaud added the_geom column on 23/01/2020
        sql.execute("drop table if exists "+sources_time_table_name+"_with_pkPosition;")
        sql.execute("create table "+sources_time_table_name+"_with_pkPosition AS SELECT  a.PK PK, a.the_geom the_geom, b.PK_POSITION PK_POSITION, a.T T, a.ID_VEH ID_VEH FROM "+sources_time_table_name+" a, closest_point b WHERE a.PK = b.PK_TIME ;")

        System.out.println(" Table "+sources_time_table_name+"_with_pkPosition created")

        for (int t = t_ini; t <= t_end; t++) {
            System.out.println(t.toString())

            Map<Integer, double[]> soundLevels = new HashMap<>()

            sql.eachRow('SELECT PK, PK_POSITION, T, ID_VEH FROM ' + sources_time_table_name + '_with_pkPosition WHERE T = ' + t.toString() + ';') { row ->

                //System.println(row)
                int time = row[2]
                int idSource = row[3]
                int pk = row[0]
                int idPositionDynamic = row[1]

                for (int i = 0; i < allLevels.size(); i++) {
                    int idPositionFix = (Integer) allLevels.get(i).sourceId
                    if (idPositionFix == idPositionDynamic) {
                        int idReceiver = (Integer) allLevels.get(i).receiverId
                       // System.println(idReceiver)

                        double[] soundLevel = allLevels.get(i).value
                        double[] sourceLev = carsProcessData.getCarLevel(sources_time_table_name, sql, t, pk)
                        if (soundLevels.containsKey(idReceiver)) {
                            soundLevel = ComputeRays.sumDbArray(sumLinearArray(soundLevel, sourceLev), soundLevels.get(idReceiver))
                            soundLevels.replace(idReceiver, soundLevel)
                        } else {
                            soundLevels.put(idReceiver, sumLinearArray(soundLevel, sourceLev))
                        }
                    }
                }


            }
            System.println("print...")
            sql.withBatch(100, qry) { ps ->
                for (Map.Entry<Integer, double[]> entry : soundLevels.entrySet()) {
                    Integer key = entry.getKey()
                    double[] value = entry.getValue()
                    value = DBToDBA(value)
                    ps.addBatch(t as Integer, key as Integer,
                            value[0] as Double, value[1] as Double, value[2] as Double,
                            value[3] as Double, value[4] as Double, value[5] as Double,
                            value[6] as Double, value[7] as Double)


                }
            }
                System.out.println("le pas de temps" +t+ "a ete traite")

        }


        System.out.println("Join Results with Geometry")
        sql.execute("CREATE INDEX ON LDAY(IDRECEIVER);")
        sql.execute("CREATE INDEX ON RECEIVERS(PK);")

        sql.execute("drop table if exists L_CARS_GEOM;")
        sql.execute("create table L_CARS_GEOM  as select a.TIME ,a.IDRECEIVER, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000  FROM LDAY a LEFT JOIN  RECEIVERS b  ON a.IDRECEIVER = b.PK;")


        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start

        return [result: "Calculation Done ! L_CARS_GEOM has been created !"]


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
