package org.noise_planet.noisemodelling.wps.NoiseModelling

/*
 * @Author Pierre Aumond
 */

import com.opencsv.CSVWriter

import geoserver.GeoServer
import geoserver.catalog.Store

import groovy.sql.Sql
import groovy.transform.CompileStatic

import java.util.ArrayList
import java.util.List
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.PreparedStatement
import java.sql.SQLException

import javax.xml.stream.XMLStreamException

import org.cts.crs.CRSException

import org.geotools.jdbc.JDBCDataStore

import groovy.transform.CompileStatic

import org.h2gis.utilities.SFSUtilities
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.utilities.wrapper.*
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.utilities.SpatialResultSet

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Coordinate

import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos

import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationPath
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor

import org.noisemodellingwps.utilities.WpsConnectionWrapper

import org.slf4j.Logger
import org.slf4j.LoggerFactory


title = 'Compute Dynamic NoiseMap'
description = 'Compute Dynamic NoiseMap from individual moving point sources'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
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
          computeHorizontal : [name: 'Compute horizontal diffraction', title: 'Compute horizontal diffraction', description: 'Compute or not the horizontal diffraction (default = false)', min: 0, max: 1, type: Boolean.class]]

outputs = [result: [name: 'result', title: 'Result', type: String.class]]


/**
 *
 */
class DynamicProcessData {

    double[] getDroneLevel(String tablename, Sql sql, int t, int idSource) throws SQLException {
        double[] res_d = [0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0]
        // memes valeurs d e et n
        sql.eachRow('SELECT id, the_geom,\n' +
                'db_m63,db_m125,db_m250,db_m500,db_m1000,db_m2000,db_m4000,db_m8000,t FROM ' + tablename +' WHERE ID = '+ idSource.toString()+' AND T = '+ t.toString()+';') { row ->
            int id = (int) row[0]
            //System.out.println("Source :" + id)
            Geometry the_geom = row[1]
            def db_m63 = row[2]
            def db_m125 = row[3]
            def db_m250 = row[4]
            def db_m500 = row[5]
            def db_m1000 = row[6]
            def db_m2000 = row[7]
            def db_m4000 = row[8]
            def db_m8000 = row[9]
            int time = (int) row[10]


            res_d = [db_m63,db_m125,db_m250,db_m500,db_m1000,db_m2000,db_m4000,db_m8000]

        }

        return res_d
    }

}


/** Read source database and compute the sound emission spectrum of roads sources **/
@CompileStatic
class DronePropagationProcessData extends PropagationProcessData {

    protected List<double[]> wjSourcesD = new ArrayList<>()

    public DronePropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

        super.addSource(pk, geom, rs)

        Geometry the_geom = rs.getGeometry("the_geom")
        double db_m63 = rs.getDouble("db_m63")
        double db_m125 = rs.getDouble("db_m125")
        double db_m250 = rs.getDouble("db_m250")
        double db_m500 = rs.getDouble("db_m500")
        double db_m1000 = rs.getDouble("db_m1000")
        double db_m2000 = rs.getDouble("db_m2000")
        double db_m4000 = rs.getDouble("db_m4000")
        double db_m8000 = rs.getDouble("db_m8000")
        int t = rs.getInt("T")
        int id = rs.getInt("ID")

        double[] res_d = new double[PropagationProcessPathData.freq_lvl.size()]

        res_d = [db_m63,db_m125,db_m250,db_m500,db_m1000,db_m2000,db_m4000,db_m8000]

        wjSourcesD.add(ComputeRays.dbaToW(res_d))
    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }



}


class DronePropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new DronePropagationProcessData(freeFieldFinder)
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
        pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false)
        pointNoiseMap.setSourceHasAbsoluteZCoordinates(false)

        // Building height field name
        pointNoiseMap.setHeightField("HAUTEUR")
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

        //TrafficPropagationProcessDataFactory trafficPropagationProcessDataFactory = new TrafficPropagationProcessDataFactory();
        //pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory);

        //PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory()
        //pointNoiseMap.setComputeRaysOutFactory(storageFactory)

        DronePropagationProcessDataFactory dronePropagationProcessDataFactory = new DronePropagationProcessDataFactory()
        pointNoiseMap.setPropagationProcessDataFactory(dronePropagationProcessDataFactory)


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
                if (out instanceof ComputeRaysOut) {
                    allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel())
                }
            }
        }



        DynamicProcessData dynamicProcessData = new DynamicProcessData()

        Sql sql = new Sql(connection)
        System.out.println("Export data to table")
        sql.execute("drop table if exists LDAY;")
        sql.execute("create table LDAY (TIME integer, IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")
        def qry = 'INSERT INTO LDAY(TIME , IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

        for (int t=0;t<100;t++){
            System.out.println(t.toString())
            Map<Integer, double[]> soundLevels = new HashMap<>()
            for (int i=0;i< allLevels.size() ; i++) {
                int idReceiver = (Integer) allLevels.get(i).receiverId
                int idSource = (Integer) allLevels.get(i).sourceId
                double[] soundLevel = allLevels.get(i).value
                double[] sourceLev = dynamicProcessData.getDroneLevel(sources_table_name, sql, t,idSource)
                if (sourceLev[0]>0){
                    if (soundLevels.containsKey(idReceiver)) {
                        soundLevel = ComputeRays.sumDbArray(sumLinearArray(soundLevel,sourceLev), soundLevels.get(idReceiver))
                        soundLevels.replace(idReceiver, soundLevel)
                    } else {
                        soundLevels.put(idReceiver, sumLinearArray(soundLevel,sourceLev))
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
        sql.execute("CREATE INDEX ON RECEIVERS(ID);")

        sql.execute("drop table if exists LDAY_GEOM;")
        sql.execute("create table LDAY_GEOM  as select a. TIME,a.IDRECEIVER, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000  FROM LDAY a LEFT JOIN  RECEIVERS b  ON a.IDRECEIVER = b.ID;")


        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start

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

static double[] sumLinearArray(double[] array1, double[] array2) {
    if (array1.length != array2.length) {
        throw new IllegalArgumentException("Not same size array")
    } else {
        double[] sum = new double[array1.length];

        for(int i = 0; i < array1.length; ++i) {
            sum[i] = array1[i] + array2[i]
        }

        return sum;
    }
}