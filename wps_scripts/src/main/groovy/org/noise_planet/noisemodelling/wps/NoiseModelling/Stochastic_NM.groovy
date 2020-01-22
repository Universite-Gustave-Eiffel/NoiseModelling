package org.noise_planet.noisemodelling.wps.NoiseModelling;

/*
 * @Author Pierre Aumond
 */
import groovy.sql.Sql
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

title = 'Stochastic Map'
description = 'Compute Stochastic Map.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
          buildingTableName : [name: 'Buildings table name', title: 'Buildings table name', type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Sources table name', type: String.class],
          receiversTableName: [name: 'Receivers table name', title: 'Receivers table name', type: String.class],
          demTableName      : [name: 'DEM table name', title: 'DEM table name', min: 0, max: 1, type: String.class],
          groundTableName   : [name: 'Ground table name', title: 'Ground table name', min: 0, max: 1, type: String.class],
          reflexionOrder    : [name: 'Number of reflexion', title: 'Number of reflexion', description: 'Maximum number of reflexion to consider (default = 0)', min: 0, max: 1, type: String.class],
          maxSrcDistance    : [name: 'Max Source Distance', title: 'Max Source Distance', description: 'Maximum distance to consider a sound source (default = 100 m)', min: 0, max: 1, type: String.class],
          maxRefDistance    : [name: 'Max Reflexion Distance', title: 'Max Reflexion Distance', description: 'Maximum distance to consider a reflexion (default = 50 m)', min: 0, max: 1, type: String.class],
          nReplications         : [name: 'nReplications', title: 'nReplications', description: 'nReplications (default = 300)', min: 0, max: 1, type: Integer.class],
          wallAlpha         : [name: 'wallAlpha', title: 'Wall alpha', description: 'Wall abosrption (default = 0.1)', min: 0, max: 1, type: String.class],
          threadNumber      : [name: 'Thread number', title: 'Thread number', description: 'Number of thread to use on the computer (default = 1)', min: 0, max: 1, type: String.class],
          computeVertical   : [name: 'Compute vertical diffraction', title: 'Compute vertical diffraction', description: 'Compute or not the vertical diffraction (default = false)', min: 0, max: 1, type: Boolean.class],
          computeHorizontal : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]

/**
 * Read source database and compute the sound emission spectrum of roads sources*/


def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // -------------------
    // Get inputs
    // -------------------

    String working_dir = ""
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


    int nreplications = 300
    if (input['nReplications']) {
        nreplications = Integer.valueOf(input['nReplications'])
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
        Sql sql = new Sql(connection)
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

        ProbaPropagationProcessDataFactory probaPropagationProcessDataFactory = new ProbaPropagationProcessDataFactory()
        pointNoiseMap.setPropagationProcessDataFactory(probaPropagationProcessDataFactory)

        RootProgressVisitor progressLogger = new RootProgressVisitor(2, true, 1)

        System.out.println("Init Map ...")

        long start = System.currentTimeMillis();

        System.out.println("Start ...")
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


        sql.execute("set @grid_density=10;\n" +
                "drop table TRAFIC_DENSITY if exists;\n" +
                "create table TRAFIC_DENSITY (the_geom geometry, TV int, PL INT, SPEED double, DENSITY_TV double, DENSITY_PL double, DENSITY_TOT double) as \n" +
                "    select THE_GEOM, TV_D  - (TV_D *@PL_D/100), TV_D *@PL_D/100, \n" +
                "        case when @SPEED_CST < 20 then 20 \n" +
                "            else @SPEED_CST end, \n" +
                "        case when @SPEED_CST< 20 then 0.001*(AADF - (AADF*@PL_D/100))/20 \n" +
                "            else 0.001*(AADF - (AADF*@PL_D/100))/@SPEED_CST end, \n" +
                "        case when @SPEED_CST< 20 then 0.001*(AADF*@PL_D/100)/20 \n" +
                "            else 0.001*(AADF*@PL_D/100)/@SPEED_CST end, \n" +
                "        case when @SPEED_CST< 20 then 0.001*(AADF)/20 \n" +
                "            else 0.001*(AADF)/@SPEED_CST end \n" +
                "        from ROADS WHERE (AADF IS NOT NULL);" +
                "drop table if exists traf_explode;\n" +
                "create table traf_explode as SELECT * FROM ST_Explode('TRAFIC_DENSITY');\n" +
                "alter table traf_explode add length double as select ST_LENGTH(the_geom) ;\n" +
                "drop table grid_traf2 if exists;\n" +
                "create table grid_traf2 (the_geom geometry, SPEED int, DENSITY_TV double, DENSITY_PL double, DENSITY_TOT double) as SELECT ST_Tomultipoint(ST_Densify(the_geom, 10)), SPEED, DENSITY_TV, DENSITY_PL, DENSITY_TOT from traf_explode;\n" +
                "drop table grid_traf_tot if exists;\n" +
                "create table grid_traf_tot as SELECT * from  ST_explode('grid_traf2');\n" +
                "alter table grid_traf_tot ADD number_veh double as select DENSITY_TOT;\n" +
                "drop table grid_traf2 if exists;\n" +
                "drop table CARS2D if exists;\n" +
                "create table CARS2D as SELECT ST_FORCE2D(the_geom) the_geom, speed, density_TV, density_pl, density_tot, explod_id exp_id, number_veh from grid_traf_tot WHERE DENSITY_TOT > 0 and DENSITY_TOT IS NOT NULL;\n" +
                "alter table CARS2D add column PK serial ;\n" +
                "drop table CARS3D if exists;\n" +
                "create table CARS3D as SELECT ST_UPDATEZ(ST_FORCE3D(the_geom),0.05,1) the_geom,ST_Z(ST_AddZ(ST_FORCE3D(the_geom),0.05)) z, speed, density_TV TV, density_pl PL, density_tot TOT, exp_id, number_veh from ST_Explode('CARS2D');\n" +
                "alter table CARS3D add column PK serial ;\n" +
                "drop table grid_traf_tot if exists;" +
                "drop table CARS2D if exists;" +
                "drop table grid_traf2 if exists;" +
                "drop table if exists traf_explode;" +
                "drop table TRAFIC_DENSITY if exists;")


        ProbaProcessData probaProcessData = new ProbaProcessData()
        probaProcessData.setProbaTable("CARS3D", sql)

        System.out.println("Export data to table")
        sql.execute("drop table if exists LDAY;")
        sql.execute("create table LDAY (TIME integer, IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")
        def qry = 'INSERT INTO LDAY(TIME , IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

        def t_old = -1
        def idSource_old = -1

        for (int t=1;t<nreplications;t++){
            Map<Integer, double[]> soundLevels = new HashMap<>()
            Map<Integer, double[]> sourceLev = new HashMap<>()

            for (int i=0;i< allLevels.size() ; i++) {

                int idReceiver = (Integer) allLevels.get(i).receiverId
                int idSource = (Integer) allLevels.get(i).sourceId
                double[] soundLevel = allLevels.get(i).value

                if (!sourceLev.containsKey(idSource)) {
                    sourceLev.put(idSource, probaProcessData.getCarsLevel( t,idSource))
                }


                if (sourceLev.get(idSource)[0]>0){
                    if (soundLevels.containsKey(idReceiver)) {
                        soundLevel = ComputeRays.sumDbArray(sumLinearArray(soundLevel,sourceLev.get(idSource)), soundLevels.get(idReceiver))
                        soundLevels.replace(idReceiver, soundLevel)
                    } else {
                        soundLevels.put(idReceiver, sumLinearArray(soundLevel,sourceLev.get(idSource)))
                    }


                    // closing writer connection

                }
            }
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
        }

        System.out.println("Join Results with Geometry")
        sql.execute("CREATE INDEX ON LDAY(IDRECEIVER);")
        sql.execute("CREATE INDEX ON RECEIVERS(PK);")

        sql.execute("drop table if exists LDAY_GEOM;")
        sql.execute("create table LDAY_GEOM  as select a. TIME,a.IDRECEIVER, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000  FROM LDAY a LEFT JOIN  RECEIVERS b  ON a.IDRECEIVER = b.PK;")


        System.out.println("Done ! LDAY_GEOM")


        /*
        writer.close()


        Map<Integer, double[]> soundLevels2 = new HashMap<>()

        System.println("Write results to csv file...")
        CSVWriter writer2 = new CSVWriter(new FileWriter(working_dir + "/ResultatsProba.csv"))

        for (int i=0;i< allLevels.size() ; i++) {
            int idReceiver = (Integer) allLevels.get(i).receiverId
            int idSource = (Integer) allLevels.get(i).sourceId
            double[] soundLevel = allLevels.get(i).value
            if (!Double.isNaN(soundLevel[0])
                    && !Double.isNaN(soundLevel[1])
                    && !Double.isNaN(soundLevel[2])
                    && !Double.isNaN(soundLevel[3])
                    && !Double.isNaN(soundLevel[4])
                    && !Double.isNaN(soundLevel[5])
                    && !Double.isNaN(soundLevel[6])
                    && !Double.isNaN(soundLevel[7])

            ) {

                writer2.writeNext([idReceiver,idSource, DBToDBA(soundLevel)] as String[])



                // if (soundLevels.containsKey(idReceiver)) {
                //     soundLevel = ComputeRays.sumDbArray(soundLevel, soundLevels.get(idReceiver))
                //     soundLevels.replace(idReceiver, soundLevel)
                // } else {
                //     soundLevels.put(idReceiver, soundLevel)
                // }
            } else {
                System.println("NaN on Rec :" + idReceiver + "and Src :" + idSource)
            }
        }

        writer2.close()
*/

        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start;

        return [result: "Calculation Done ! LDAY_GEOM"]


    }

}

class ProbaProcessData {

    Map<Integer,Double> SPEED = new HashMap<>()
    Map<Integer,Double> TV = new HashMap<>()
    Map<Integer,Double> PL = new HashMap<>()

    double[] getCarsLevel(int t, int idSource) throws SQLException {
        double[] res_d = [0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]
        double[] res_TV = [0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]
        double[] res_PL = [0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]
        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
        // memes valeurs d e et n


        def random = Math.random()
        if (random < TV.get(idSource)){
            int kk=0
            for (f in list) {

                double speed = SPEED.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                int RoadSurface = 0
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                int veh_type = 1
                int acc_type= 1
                double LwStd= 1
                int VehId = 10

                RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  veh_type, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,VehId)
                rsParameters.setSlopePercentage(0)

                res_TV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }

        }
        if (random < PL.get(idSource)){
            int kk=0
            for (f in list) {
                double speed = SPEED.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                int RoadSurface = 0
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                int veh_type = 3
                int acc_type= 1
                double LwStd= 1
                int VehId = 10

                RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  veh_type, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,VehId)
                rsParameters.setSlopePercentage(0)

                res_PL[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }
        }
        int kk=0
        for (f in list) {
            res_d[kk] = 10 * Math.log10(
                    (1.0 / 2.0) *
                            ( Math.pow(10, (10 * Math.log10(Math.pow(10, res_TV[kk] / 10))) / 10)
                                    + Math.pow(10, (10 * Math.log10(Math.pow(10, res_PL[kk] / 10))) / 10)
                            )
            )
            kk++
        }




        return res_d
    }

    void setProbaTable(String tablename, Sql sql) {
        //////////////////////
        // Import file text
        //////////////////////
        int i_read = 0;

        // Remplissage des variables avec le contenu du fichier plan d'exp
        sql.eachRow('SELECT PK,  SPEED, PL,TV FROM ' + tablename +';') { row ->
            int pk = (int) row[0]

            SPEED.put(pk,(double) row[1])
            PL.put(pk,(double) row[2])
            TV.put(pk, (double) row[3])

        }


    }

}



/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
class ProbaPropagationProcessData extends PropagationProcessData {

    protected List<double[]> wjSourcesD = new ArrayList<>()

    public ProbaPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

        super.addSource(pk, geom, rs)

        double db_m63 = 90
        double db_m125 = 90
        double db_m250 = 90
        double db_m500 =90
        double db_m1000 = 90
        double db_m2000 =90
        double db_m4000 = 90
        double db_m8000 = 90

        double[] res_d =  [db_m63,db_m125,db_m250,db_m500,db_m1000,db_m2000,db_m4000,db_m8000]
        wjSourcesD.add(ComputeRays.dbaToW(res_d))
    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }


}

class ProbaPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new ProbaPropagationProcessData(freeFieldFinder)
    }
}

static double[] DBToDBA(double[] db){
    double[] dbA = [-26.2,-16.1,-8.6,-3.2,0,1.2,1.0,-1.1]
    for(int i = 0; i < db.length; ++i) {
        db[i] = db[i] + dbA[i]
    }
    return db

}

static double[] sumLinearArray(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array")
    } else {
        double[] sum = new double[array1.length]

        for(int i = 0; i < array1.length; ++i) {
            sum[i] = array1[i] + array2[i]
        }

        return sum
    }
}



