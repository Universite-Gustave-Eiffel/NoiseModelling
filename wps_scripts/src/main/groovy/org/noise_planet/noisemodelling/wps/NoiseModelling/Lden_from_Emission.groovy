package org.noise_planet.noisemodelling.wps.NoiseModelling;

/*
 * @Author Hesry Quentin
 * @Author Pierre Aumond
 * @Author Nicolas Fortin
 */

import geoserver.GeoServer
import geoserver.catalog.Store

import org.h2gis.api.ProgressVisitor
import org.geotools.jdbc.JDBCDataStore
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import javax.xml.stream.XMLStreamException
import org.cts.crs.CRSException

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.PreparedStatement

import groovy.sql.Sql
import org.h2gis.utilities.SFSUtilities
import org.h2gis.api.EmptyProgressVisitor
import org.noisemodellingwps.utilities.WpsConnectionWrapper
import org.h2gis.utilities.wrapper.*

import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry


import java.sql.SQLException
import java.util.ArrayList
import java.util.List

title = 'Compute Lden Map from Road Emission'
description = 'Compute Lden Map from Road Emission.'

inputs = [databaseName      : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database. (default : h2gisdb)', min: 0, max: 1, type: String.class],
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
class TrafficPropagationProcessDataDEN extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesDEN = new ArrayList<>();
    public Map<Long, Integer> SourcesPk = new HashMap<>();


    public TrafficPropagationProcessDataDEN(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder);
    }

    int idSource = 0

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


class TrafficPropagationProcessDataDENFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessDataDEN(freeFieldFinder);
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

        TrafficPropagationProcessDataDENFactory TrafficPropagationProcessDataDENFactory = new TrafficPropagationProcessDataDENFactory();
        pointNoiseMap.setPropagationProcessDataFactory(TrafficPropagationProcessDataDENFactory)


        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        System.out.println("Init Map ...")
        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim());

        long start = System.currentTimeMillis();
        System.out.println("Start ...")

        Map<Integer, double[]> SourceSpectrum = new HashMap<>()

        // Iterate over computation areas
        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                // Return results with level spectrum for each source/receiver tuple
                if (out instanceof ComputeRaysOut) {

                    ComputeRaysOut cellStorage = (ComputeRaysOut) out;

                    allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel())

                    //exportScene(String.format(resultPath+"/scene_%d_%d.kml", i, j), cellStorage.inputData.freeFieldFinder, cellStorage);
                    cellStorage.receiversAttenuationLevels.each { v ->
                        double globalDbValue = ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(v.value)));
                        def idSource = out.inputData.SourcesPk.get(v.sourceId)
                        double[] w_spectrum = ComputeRays.wToDba(out.inputData.wjSourcesDEN.get(idSource))
                        SourceSpectrum.put(v.sourceId as Integer, w_spectrum)
                    }
                }
            }
        }


        Map<Integer, double[]> soundLevels = new HashMap<>()
        for (int i = 0; i < allLevels.size(); i++) {
            int idReceiver = (Integer) allLevels.get(i).receiverId
            int idSource = (Integer) allLevels.get(i).sourceId

            double[] soundLevel = allLevels.get(i).value
            if (!Double.isNaN(soundLevel[0]) && !Double.isNaN(soundLevel[1]) && !Double.isNaN(soundLevel[2]) && !Double.isNaN(soundLevel[3]) && !Double.isNaN(soundLevel[4]) && !Double.isNaN(soundLevel[5]) && !Double.isNaN(soundLevel[6]) && !Double.isNaN(soundLevel[7])

            ) {
                if (soundLevels.containsKey(idReceiver)) {
                    //soundLevel = DBToDBA(soundLevel)

                    soundLevel = ComputeRays.sumDbArray(sumArraySR(soundLevel, SourceSpectrum.get(idSource)), soundLevels.get(idReceiver))
                    soundLevels.replace(idReceiver, soundLevel)
                } else {
                    //soundLevel = DBToDBA(soundLevel)
                    soundLevels.put(idReceiver, sumArraySR(soundLevel, SourceSpectrum.get(idSource)))


                }
            } else {
                System.out.println("NaN on Rec :" + idReceiver + "and Src :" + idSource)
            }
        }


        Sql sql = new Sql(connection)
        System.out.println("Export data to table")
        sql.execute("drop table if exists LDEN;")
        sql.execute("create table LDEN (IDRECEIVER integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")

        def qry = 'INSERT INTO LDEN(IDRECEIVER,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?);'

        sql.withBatch(100, qry) { ps ->
            for (s in soundLevels) {
                ps.addBatch(s.key as Integer,
                        s.value[0] as Double, s.value[1] as Double, s.value[2] as Double,
                        s.value[3] as Double, s.value[4] as Double, s.value[5] as Double,
                        s.value[6] as Double, s.value[7] as Double)

            }
        }

        sql.execute("drop table if exists LDEN_GEOM;")
        sql.execute("create table LDEN_GEOM  as select a.IDRECEIVER, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000 FROM RECEIVERS b LEFT JOIN LDEN a ON a.IDRECEIVER = b.PK;")


        System.out.println("Done !")


        long computationTime = System.currentTimeMillis() - start;

        return [result: "Calculation Done ! LDEN_GEOM"]


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