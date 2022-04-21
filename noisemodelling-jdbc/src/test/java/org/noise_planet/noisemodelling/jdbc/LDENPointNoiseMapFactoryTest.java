package org.noise_planet.noisemodelling.jdbc;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPDriverFunction;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.jdbc.utils.MakeLWTable;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.Assert.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.sumArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.sumDbArray;

public class LDENPointNoiseMapFactoryTest {

    static Logger LOGGER = LoggerFactory.getLogger(LDENPointNoiseMapFactoryTest.class);

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LDENPointNoiseMapFactoryTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testNoiseEmission() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);
        ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.TIME_PERIOD_DAY, new PropagationProcessPathData());
        ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.TIME_PERIOD_EVENING, new PropagationProcessPathData());
        ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.TIME_PERIOD_NIGHT, new PropagationProcessPathData());
        ldenConfig.setCoefficientVersion(1);
        LDENPropagationProcessData process = new LDENPropagationProcessData(null, ldenConfig);
        try(Statement st = connection.createStatement()) {
            double lv_speed = 70;
            int lv_per_hour = 1000;
            double mv_speed = 70;
            int mv_per_hour = 1000;
            double hgv_speed = 70;
            int hgv_per_hour = 1000;
            double wav_speed = 70;
            int wav_per_hour = 1000;
            double wbv_speed = 70;
            int wbv_per_hour = 1000;
            double Temperature = 15;
            String RoadSurface = "NL01";
            double Pm_stud = 0.5;
            double Ts_stud = 4;
            double Junc_dist = 200;
            int Junc_type = 1;
            StringBuilder qry = new StringBuilder("SELECT ");
            qry.append(lv_speed).append(" LV_SPD_D, ");
            qry.append(lv_per_hour).append(" LV_D, ");
            qry.append(mv_speed).append(" MV_SPD_D, ");
            qry.append(mv_per_hour).append(" MV_D, ");
            qry.append(hgv_speed).append(" HGV_SPD_D, ");
            qry.append(hgv_per_hour).append(" HGV_D, ");
            qry.append(wav_speed).append(" WAV_SPD_D, ");
            qry.append(wav_per_hour).append(" WAV_D, ");
            qry.append(wbv_speed).append(" WBV_SPD_D, ");
            qry.append(wbv_per_hour).append(" WBV_D, ");
            qry.append(Temperature).append(" TEMP, ");
            qry.append(Pm_stud).append(" PM_STUD, ");
            qry.append(Ts_stud).append(" TS_STUD, ");
            qry.append(Junc_dist).append(" JUNC_DIST, '");
            qry.append(Junc_type).append("' JUNC_TYPE, '");
            qry.append(RoadSurface).append("' PVMT ");
            try(ResultSet rs = st.executeQuery(qry.toString())) {
                assertTrue(rs.next());
                double[] leq = process.getEmissionFromResultSet(rs, "D", 10);
                assertEquals(77.67 , leq[leq.length - 1] , 0.1);
            }
        }
    }

    @Test
    public void testNoiseEmissionRailWay() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrain.dbf").getFile());
        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWayTwoGeoms() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 2)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }


    @Test
    public void testNoiseEmissionRailWaySingleGeom() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }


    @Test
    public void testNoiseEmissionRailWaySingleGeomSingleTrain() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");
        connection.createStatement().execute("DELETE FROM RAILTRAIN WHERE PK NOT IN (SELECT R1.PK FROM RAILTRAIN R1, RAILTRACK R2 WHERE r1.IDSECTION = R2.IDSECTION LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWay_OC5() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/OC/RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/OC/RailTrain.dbf").getFile());

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        v.setNbTrack(2);
        RailWayLW railWayLW = v.getRailWayLW();
        List<LineString> geometries = v.getRailWayLWGeometry();

        v = railWayLWIterator.next();
        assertFalse(railWayLWIterator.hasNext());

    }

    @Test
    public void testNoiseEmissionRailWay_BM() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/BM/RailTrack.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/BM/RailTrain.dbf").getFile());

        HashMap<String, double[]> Resultats = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        double resD,resE,resN;

        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();

            RailWayLW railWayLW = v.getRailWayLWDay();
            double[] rolling = railWayLW.getLWRolling();
            double[] tractiona = railWayLW.getLWTractionA();
            double[] tractionb = railWayLW.getLWTractionB();
            double[] aeroa = railWayLW.getLWAerodynamicA();
            double[] aerob = railWayLW.getLWAerodynamicB();
            double[] LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            double[] LWA = sumArray(LW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            rolling = railWayLW.getLWRolling();
            tractiona = railWayLW.getLWTractionA();
            tractionb = railWayLW.getLWTractionB();
            aeroa = railWayLW.getLWAerodynamicA();
            aerob = railWayLW.getLWAerodynamicB();
            LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            LWA = sumArray(LW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            rolling = railWayLW.getLWRolling();
            tractiona = railWayLW.getLWTractionA();
            tractionb = railWayLW.getLWTractionB();
            aeroa = railWayLW.getLWAerodynamicA();
            aerob = railWayLW.getLWAerodynamicB();
            LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            LWA = sumArray(LW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            Resultats.put(idSection,new double[]{resD, resE, resN});

        }

        assertFalse(railWayLWIterator.hasNext());

    }

    @Test
    public void testNoiseEmissionRailWay_Section556() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/RAIL_SECTIONS.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/RAIL_TRAFIC.dbf").getFile());

        HashMap<String, double[]> Resultats = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAIL_SECTIONS", "RAIL_TRAFIC");

        double resD,resE,resN;

       // RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.current();

        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();

            RailWayLW railWayLW = v.getRailWayLWDay();
            double[] rolling = railWayLW.getLWRolling();
            double[] tractiona = railWayLW.getLWTractionA();
            double[] tractionb = railWayLW.getLWTractionB();
            double[] aeroa = railWayLW.getLWAerodynamicA();
            double[] aerob = railWayLW.getLWAerodynamicB();
            double[] LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            double[] LWA = sumArray(LW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            rolling = railWayLW.getLWRolling();
            tractiona = railWayLW.getLWTractionA();
            tractionb = railWayLW.getLWTractionB();
            aeroa = railWayLW.getLWAerodynamicA();
            aerob = railWayLW.getLWAerodynamicB();
            LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            LWA = sumArray(LW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            rolling = railWayLW.getLWRolling();
            tractiona = railWayLW.getLWTractionA();
            tractionb = railWayLW.getLWTractionB();
            aeroa = railWayLW.getLWAerodynamicA();
            aerob = railWayLW.getLWAerodynamicB();
            LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
            LWA = sumArray(LW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            Resultats.put(idSection,new double[]{resD, resE, resN});

        }

        /*RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
        RailWayLW railWayLW = v.getRailWayLWDay();
        double[] rolling = railWayLW.getLWRolling();
        double[] tractiona = railWayLW.getLWTractionA();
        double[] tractionb = railWayLW.getLWTractionB();
        double[] aeroa = railWayLW.getLWAerodynamicA();
        double[] aerob = railWayLW.getLWAerodynamicB();
        double[] LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
        double[] LWA = sumArray(LW, dBA);
        double resD = sumDbArray(LWA);

        railWayLW = v.getRailWayLWEvening();
        rolling = railWayLW.getLWRolling();
        tractiona = railWayLW.getLWTractionA();
        tractionb = railWayLW.getLWTractionB();
        aeroa = railWayLW.getLWAerodynamicA();
        aerob = railWayLW.getLWAerodynamicB();
        LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
        LWA = sumArray(LW, dBA);
        double resE = sumDbArray(LWA);

        railWayLW = v.getRailWayLWNight();
        rolling = railWayLW.getLWRolling();
        tractiona = railWayLW.getLWTractionA();
        tractionb = railWayLW.getLWTractionB();
        aeroa = railWayLW.getLWAerodynamicA();
        aerob = railWayLW.getLWAerodynamicB();*
       /* LW = sumDbArray(sumDbArray(sumDbArray(sumDbArray(rolling, tractiona), tractionb), aeroa), aerob);
        LWA = sumArray(LW, dBA);
        double resN = sumDbArray(LWA);*/

        /*String idSection = v.getIdSection();
        Resultats.put(idSection,new double[]{resD, resE, resN});
        v = railWayLWIterator.next();*/
/*
        StringBuilder keyRow = new StringBuilder();
        StringBuilder value1 = new StringBuilder();
        StringBuilder value2 = new StringBuilder();
        StringBuilder value3 = new StringBuilder();

        Iterator keys = Resultats.keySet().iterator();
        boolean notFirst = true;
        while(keys.hasNext()) {
            String key = (String)keys.next();

            if(!notFirst) {
                keyRow.append(",");
                value1.append(",");
                value2.append(",");
                value3.append(",");
            }
            keyRow.append(key);
            value1.append((Double)Resultats.get(key)[0]);
            value2.append((Double)Resultats.get(key)[1]);
            value3.append((Double)Resultats.get(key)[2]);
            notFirst = false;
        }
        String csv = keyRow.toString()+"\n"+value1.toString()+"\n"+value2.toString()+"\n"+value3.toString();

*/
  /*      Resultats.toString();

        StringBuilder csv = new StringBuilder();
        int row = 0;
        ArrayList<double[]> list = new ArrayList<double[]>();

        // Write the keys row:
        Iterator keys = Resultats.keySet().iterator();
        boolean notFirst = true;
        while(keys.hasNext()) {
            String key = (String)keys.next();
            double[] tmp = Resultats.get(key);
            if(!notFirst) {
                csv.append(",");
            }
            csv.append(key);
            // store list
            list.add(tmp);
            notFirst = false;
        }



        // Write the rest of the rows
        while(row<3) {
            notFirst = true;
            for(int x=0;x<list.size();x++) {
                if(!notFirst) {
                    csv.append(",");
                }
                csv.append((double)list.get(x)[row]);
                notFirst = false;
            }
            row++;
        }
        csv.append("\n");
*/

        assertFalse(railWayLWIterator.hasNext());

    }

    public void toString(HashMap<String, ArrayList<String>> map) {
        for(int i = 0; i < map.size(); i++) {
            ArrayList<String> list = new ArrayList<String>(map.keySet());
            String key = list.get(i);
            System.out.println(key);
            for(int j = 0; j < map.get(key).size(); j++)
                System.out.println(map.get(key).get(j));
        }
    }

    @Test
    public void testNoiseEmissionRailWayForPropa556() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/RAIL_SECTIONS.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/RAIL_TRAFIC.dbf").getFile());

        MakeLWTable.makeTrainLWTable(connection, "RAIL_SECTIONS", "RAIL_TRAFIC",
                "LW_RAILWAY");


        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/Recepteurs.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("Test/556/Buildings2.shp").getFile());

        // ICI POUR CHANGER HAUTEUR ET G ECRAN
        //connection.createStatement().execute("CREATE TABLE SCREENS AS SELECT ST_BUFFER(the_geom, 0.5, 'join=mitre endcap=flat') as the_geom, pk as pk, 3.0 as height, g as g FROM Rail_protect");

        // Count receivers
        int nbReceivers = 0;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM RECEPTEURS")) {
            assertTrue(rs.next());
            nbReceivers = rs.getInt(1);
        }

        // ICI HAUTEUR RECPTEUR
        connection.createStatement().execute("SELECT UpdateGeometrySRID('RECEPTEURS', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154);");

        connection.createStatement().execute("UPDATE RECEPTEURS SET THE_GEOM = ST_UPDATEZ(THE_GEOM,4.0);");
        //connection.createStatement().execute("UPDATE LW_RAILWAY SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(THE_GEOM,0.5),2154);");


        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);
        ldenConfig.setExportRays(false);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);
        factory.setKeepRays(false);
        factory.insertTrainDirectivity();


        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS2", "LW_RAILWAY",
                "RECEPTEURS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        //pointNoiseMap.setDemTable("DEM");
        pointNoiseMap.setMaximumError(0.2d);
        pointNoiseMap.setMaximumPropagationDistance(800.0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        pointNoiseMap.setSoundReflectionOrder(1);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, false, 1);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            factory.start();
            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                if (out instanceof ComputeRaysOutAttenuation) {
                    ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                    exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                }
            }
        }finally {
            factory.stop();
        }




        connection.commit();
        // retrieve receiver value
        assertEquals(nbReceivers, receivers.size());

        // ICI A MODIFIER
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "+ ldenConfig.lDayTable + " LVL")) {
            assertTrue(rs.next());
            /*assertTrue(rs.next());
            assertEquals(47.60, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);*/
        }

     /*  connection.createStatement().execute("CREATE TABLE RESULTS AS SELECT R.the_geom the_geom, R.PK pk, R.PK2 pk2,laeq laeq FROM "+ ldenConfig.lDayTable + " LVL, RECEPTEURS R WHERE LVL.IDRECEIVER = R.PK2");
        SHPDriverFunction shpDriver = new SHPDriverFunction();
        shpDriver.exportTable(connection, "RESULTS", new File("target/Results_railway_Propa_1.shp"), true, new EmptyProgressVisitor());
        shpDriver.exportTable(connection, "RECEPTEURS", new File("target/RECEPTEURS.shp"), true, new EmptyProgressVisitor());


        shpDriver.exportTable(connection, "SCREENS", new File("target/SCREENS_control.shp"), true, new EmptyProgressVisitor());
        shpDriver.exportTable(connection, "LW_RAILWAY", new File("target/LW_RAILWAY_control.shp"), true, new EmptyProgressVisitor());
*/

    }

    @Test
    public void testNoiseEmissionRailWayForPropa() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        MakeLWTable.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY");



        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PropaRail/Recepteurs.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PropaRail/Buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PropaRail/Rail_protect.shp").getFile());

        // ICI POUR CHANGER HAUTEUR ET G ECRAN
        connection.createStatement().execute("CREATE TABLE SCREENS AS SELECT ST_BUFFER(the_geom, 0.5, 'join=mitre endcap=flat') as the_geom, pk as pk, 3.0 as height, g as g FROM Rail_protect");

        // Count receivers
        int nbReceivers = 0;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM RECEPTEURS")) {
            assertTrue(rs.next());
            nbReceivers = rs.getInt(1);
        }

        // ICI HAUTEUR RECPTEUR
        connection.createStatement().execute("SELECT UpdateGeometrySRID('RECEPTEURS', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154);");

        connection.createStatement().execute("UPDATE RECEPTEURS SET THE_GEOM = ST_UPDATEZ(THE_GEOM,4.0);");
        //connection.createStatement().execute("UPDATE LW_RAILWAY SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(THE_GEOM,0.5),2154);");


        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(false);
        ldenConfig.setComputeLNight(false);
        ldenConfig.setComputeLDEN(false);
        ldenConfig.setExportRays(true);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);
        factory.setKeepRays(true);
        factory.insertTrainDirectivity();


        PointNoiseMap pointNoiseMap = new PointNoiseMap("SCREENS", "LW_RAILWAY",
                "RECEPTEURS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        //pointNoiseMap.setDemTable("DEM");

        pointNoiseMap.setMaximumPropagationDistance(850.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, false, 1);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            factory.start();
            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                if (out instanceof ComputeRaysOutAttenuation) {
                    ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                    exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                }
            }
        }finally {
            factory.stop();
        }




        connection.commit();
        // retrieve receiver value
        assertEquals(nbReceivers, receivers.size());

        // ICI A MODIFIER
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT PK,PK2,laeq FROM "+ ldenConfig.lDayTable + " LVL, RECEPTEURS R WHERE LVL.IDRECEIVER = R.PK2 ORDER BY PK2")) {
            assertTrue(rs.next());
            assertTrue(rs.next());
            /*assertEquals(47.60, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);*/
        }

        connection.createStatement().execute("CREATE TABLE RESULTS AS SELECT R.the_geom the_geom, R.PK pk, R.PK2 pk2,laeq laeq FROM "+ ldenConfig.lDayTable + " LVL, RECEPTEURS R WHERE LVL.IDRECEIVER = R.PK2");
        SHPDriverFunction shpDriver = new SHPDriverFunction();
        shpDriver.exportTable(connection, "RESULTS", new File("target/Results_railway_Propa_1.shp"), true, new EmptyProgressVisitor());
        shpDriver.exportTable(connection, "RECEPTEURS", new File("target/RECEPTEURS.shp"), true, new EmptyProgressVisitor());


        shpDriver.exportTable(connection, "SCREENS", new File("target/SCREENS_control.shp"), true, new EmptyProgressVisitor());
        shpDriver.exportTable(connection, "LW_RAILWAY", new File("target/LW_RAILWAY_control.shp"), true, new EmptyProgressVisitor());


    }

    @Test
    public void testTableGenerationFromTraffic() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);
        ldenConfig.setMergeSources(true); // No idsource column

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_TRAFF",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            pointNoiseMap.setGridDim(4); // force grid size

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
            }
        }finally {
            factory.stop();
        }
        connection.commit();

        // Check table creation
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        // Check table number of rows
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lDayTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lEveningTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lDenTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }

        // Check dB ranges of result
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lDayTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
            for(int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(87, leqs[0], 2.0);
            assertEquals(78, leqs[1], 2.0);
            assertEquals(78, leqs[2], 2.0);
            assertEquals(79, leqs[3], 2.0);
            assertEquals(82, leqs[4], 2.0);
            assertEquals(80, leqs[5], 2.0);
            assertEquals(72, leqs[6], 2.0);
            assertEquals(62, leqs[7], 2.0);

            assertEquals(90, rs.getDouble(9), 2.0);
            assertEquals(86,rs.getDouble(10), 2.0);
        }



        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lEveningTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(82.0, leqs[0], 2.0);
            assertEquals(74.0, leqs[1], 2.0);
            assertEquals(74.0, leqs[2], 2.0);
            assertEquals(75.0, leqs[3], 2.0);
            assertEquals(78.0, leqs[4], 2.0);
            assertEquals(75.0, leqs[5], 2.0);
            assertEquals(67.0, leqs[6], 2.0);
            assertEquals(57.0, leqs[7], 2.0);

            assertEquals(85, rs.getDouble(9), 2.0);
            assertEquals(81,rs.getDouble(10), 2.0);
        }


        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(79, leqs[0], 2.0);
            assertEquals(71, leqs[1], 2.0);
            assertEquals(70, leqs[2], 2.0);
            assertEquals(72, leqs[3], 2.0);
            assertEquals(75, leqs[4], 2.0);
            assertEquals(72, leqs[5], 2.0);
            assertEquals(64, leqs[6], 2.0);
            assertEquals(55, leqs[7], 2.0);

            assertEquals(81, rs.getDouble(9), 2.0);
            assertEquals(78,rs.getDouble(10), 2.0);
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lDenTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(87.0, leqs[0], 2.0);
            assertEquals(79.0, leqs[1], 2.0);
            assertEquals(79.0, leqs[2], 2.0);
            assertEquals(80.0, leqs[3], 2.0);
            assertEquals(83.0, leqs[4], 2.0);
            assertEquals(81.0, leqs[5], 2.0);
            assertEquals(72.0, leqs[6], 2.0);
            assertEquals(63.0, leqs[7], 2.0);

            assertEquals(90, rs.getDouble(9), 2.0);
            assertEquals(87,rs.getDouble(10), 2.0);
        }
    }


    @Test
    public void testTableGenerationFromTrafficNightOnly() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        ldenConfig.setComputeLDay(false);
        ldenConfig.setComputeLEvening(false);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(false);
        ldenConfig.setMergeSources(true); // No idsource column

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_TRAFF",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            pointNoiseMap.setGridDim(4); // force grid size

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
            }
        }finally {
            factory.stop();
        }
        connection.commit();

        // Check table creation
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(78, leqs[0], 2.0);
            assertEquals(71, leqs[1], 2.0);
            assertEquals(70, leqs[2], 2.0);
            assertEquals(72, leqs[3], 2.0);
            assertEquals(75, leqs[4], 2.0);
            assertEquals(72, leqs[5], 2.0);
            assertEquals(64, leqs[6], 2.0);
            assertEquals(55, leqs[7], 2.0);

            assertEquals(82, rs.getDouble(9), 2.0);
            assertEquals(78,rs.getDouble(10), 2.0);
        }

    }

    @Test
    public void testTableGenerationFromTrafficNightOnlyLaeq() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        ldenConfig.setComputeLDay(false);
        ldenConfig.setComputeLEvening(false);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLAEQOnly(true);
        ldenConfig.setComputeLDEN(false);
        ldenConfig.setMergeSources(true); // No idsource column

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_TRAFF",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            pointNoiseMap.setGridDim(4); // force grid size

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
            }
        }finally {
            factory.stop();
        }
        connection.commit();

        // Check table creation
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            assertEquals(830, rs.getInt(1));
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(LAEQ) LAEQ FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            assertEquals(78, rs.getDouble("LAEQ"), 2.0);
        }

    }

    @Test
    public void testReadFrequencies() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        assertNotNull(ldenConfig.propagationProcessPathDataDay);
        assertNotNull(ldenConfig.propagationProcessPathDataEvening);
        assertNotNull(ldenConfig.propagationProcessPathDataNight);

        assertEquals(8, ldenConfig.propagationProcessPathDataDay.freq_lvl.size());

        try(Statement st = connection.createStatement()) {
            // drop all columns except 1000 Hz
            st.execute("ALTER TABLE lw_roads drop column LWD63");
            st.execute("ALTER TABLE lw_roads drop column LWD125");
            st.execute("ALTER TABLE lw_roads drop column LWD250");
            st.execute("ALTER TABLE lw_roads drop column LWD500");
            st.execute("ALTER TABLE lw_roads drop column LWD2000");
            st.execute("ALTER TABLE lw_roads drop column LWD4000");
            st.execute("ALTER TABLE lw_roads drop column LWD8000");
            st.execute("ALTER TABLE lw_roads drop column LWE63");
            st.execute("ALTER TABLE lw_roads drop column LWE125");
            st.execute("ALTER TABLE lw_roads drop column LWE250");
            st.execute("ALTER TABLE lw_roads drop column LWE500");
            st.execute("ALTER TABLE lw_roads drop column LWE1000");
            st.execute("ALTER TABLE lw_roads drop column LWE2000");
            st.execute("ALTER TABLE lw_roads drop column LWE4000");
            st.execute("ALTER TABLE lw_roads drop column LWE8000");
            st.execute("ALTER TABLE lw_roads drop column LWN63");
            st.execute("ALTER TABLE lw_roads drop column LWN125");
            st.execute("ALTER TABLE lw_roads drop column LWN250");
            st.execute("ALTER TABLE lw_roads drop column LWN500");
            st.execute("ALTER TABLE lw_roads drop column LWN1000");
            st.execute("ALTER TABLE lw_roads drop column LWN2000");
            st.execute("ALTER TABLE lw_roads drop column LWN4000");
            st.execute("ALTER TABLE lw_roads drop column LWN8000");
        }

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        assertEquals(1, ldenConfig.propagationProcessPathDataDay.freq_lvl.size());

        assertEquals(1000, (int)ldenConfig.propagationProcessPathDataDay.freq_lvl.get(0));
    }

    @Test
    public void testNoDemBuildingsZ() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        try(Statement st = connection.createStatement()) {
            // Alter buildings polygons Z
            //st.execute("UPDATE BUILDINGS SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(ST_FORCE3D(THE_GEOM), 50), 2154)");
            st.execute("CREATE TABLE BUILDINGS_Z(PK SERIAL PRIMARY KEY, the_geom GEOMETRY, HEIGHT FLOAT ) AS SELECT " +
                    "(row_number() over())::int, ST_UPDATEZ(ST_FORCE3D(THE_GEOM),50.0), HEIGHT FROM BUILDINGS;");
            st.execute("SELECT UpdateGeometrySRID('BUILDINGS_Z', 'THE_GEOM', 2154);");

            // Use only a subset of receivers
            st.execute("SELECT UpdateGeometrySRID('RECEIVERS', 'THE_GEOM', 2154);");
            st.execute("DELETE FROM RECEIVERS WHERE ST_DISTANCE('SRID=2154;POINT (223940.83614225042 6757305.252751735)'::geometry, THE_GEOM) > 300");
        }

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS_Z", "LW_ROADS",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);




        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            pointNoiseMap.setGridDim(1); // force grid size

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut ret = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                if(ret instanceof LDENComputeRaysOut) {
                    LDENComputeRaysOut out = (LDENComputeRaysOut)ret;
                    for(Coordinate v : out.ldenPropagationProcessData.profileBuilder.getVertices()) {
                        assertEquals(0.0, v.z, 1e-6);
                    }
                }
            }
        }finally {
            factory.stop();
        }
        connection.commit();


    }

    // Check regression of finding cell i,j that contains receivers
    @Test
    public void testRegression1() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/lw_roads_fence.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/bati_fence.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/receivers.shp").getFile());

        Set<PointNoiseMap.CellIndex> expected = new HashSet<>();
        expected.add(new PointNoiseMap.CellIndex(0, 0));
        expected.add(new PointNoiseMap.CellIndex(1, 0));
        expected.add(new PointNoiseMap.CellIndex(2, 0));
        expected.add(new PointNoiseMap.CellIndex(3, 0));
        expected.add(new PointNoiseMap.CellIndex(0, 1));
        expected.add(new PointNoiseMap.CellIndex(1, 1));
        expected.add(new PointNoiseMap.CellIndex(2, 1));
        expected.add(new PointNoiseMap.CellIndex(3, 1));
        expected.add(new PointNoiseMap.CellIndex(4, 1));
        expected.add(new PointNoiseMap.CellIndex(5, 1));
        expected.add(new PointNoiseMap.CellIndex(6, 1));
        expected.add(new PointNoiseMap.CellIndex(7, 1));
        expected.add(new PointNoiseMap.CellIndex(0, 2));
        expected.add(new PointNoiseMap.CellIndex(1, 2));
        expected.add(new PointNoiseMap.CellIndex(2, 2));
        expected.add(new PointNoiseMap.CellIndex(3, 2));
        expected.add(new PointNoiseMap.CellIndex(4, 2));
        expected.add(new PointNoiseMap.CellIndex(5, 2));
        expected.add(new PointNoiseMap.CellIndex(6, 2));
        expected.add(new PointNoiseMap.CellIndex(7, 2));
        expected.add(new PointNoiseMap.CellIndex(0, 3));
        expected.add(new PointNoiseMap.CellIndex(1, 3));
        expected.add(new PointNoiseMap.CellIndex(2, 3));
        expected.add(new PointNoiseMap.CellIndex(3, 3));
        expected.add(new PointNoiseMap.CellIndex(4, 3));
        expected.add(new PointNoiseMap.CellIndex(5, 3));
        expected.add(new PointNoiseMap.CellIndex(6, 3));
        expected.add(new PointNoiseMap.CellIndex(7, 3));
        expected.add(new PointNoiseMap.CellIndex(0, 4));
        expected.add(new PointNoiseMap.CellIndex(2, 4));
        expected.add(new PointNoiseMap.CellIndex(3, 4));
        expected.add(new PointNoiseMap.CellIndex(4, 4));
        expected.add(new PointNoiseMap.CellIndex(5, 4));
        expected.add(new PointNoiseMap.CellIndex(6, 4));
        expected.add(new PointNoiseMap.CellIndex(7, 4));
        expected.add(new PointNoiseMap.CellIndex(2, 5));
        expected.add(new PointNoiseMap.CellIndex(3, 5));
        expected.add(new PointNoiseMap.CellIndex(4, 5));
        expected.add(new PointNoiseMap.CellIndex(5, 5));
        expected.add(new PointNoiseMap.CellIndex(3, 6));
        expected.add(new PointNoiseMap.CellIndex(4, 6));
        expected.add(new PointNoiseMap.CellIndex(5, 6));
        expected.add(new PointNoiseMap.CellIndex(4, 7));
        expected.add(new PointNoiseMap.CellIndex(5, 7));
        expected.add(new PointNoiseMap.CellIndex(6, 7));
        // Count receivers
        int nbReceivers = 0;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM RECEIVERS")) {
            assertTrue(rs.next());
            nbReceivers = rs.getInt(1);
        }

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BATI_FENCE", "LW_ROADS_FENCE",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(750.0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        pointNoiseMap.setSoundReflectionOrder(0);
        //pointNoiseMap.setThreadCount(1);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, false, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // check if expected cells are found
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                assertTrue(expected.contains(cellIndex));
                expected.remove(cellIndex);
            }
            assertTrue(expected.isEmpty());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
            }
        }finally {
            factory.stop();
        }
        connection.commit();
        // Check if all receivers are computed
        assertEquals(nbReceivers, receivers.size());


    }

    @Test
    public void TestPointSource() throws SQLException, IOException {
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/DEM_Fence.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/LANDCOVER.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/RCVS20.shp").getFile());

        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/RCVSCircle.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/NO_BUILD.shp").getFile());
        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/BUILD_GRID2.shp").getFile());

        SHPRead.importTable(connection, LDENPointNoiseMapFactoryTest.class.getResource("PointSource/SourceSi.shp").getFile());

        // PROPAGATION PART
        // --------------

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(false);
        ldenConfig.setComputeLNight(false);
        ldenConfig.setComputeLDEN(false);
        ldenConfig.setMergeSources(true); // No idsource column

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        // ICI HAUTEUR RECPTEUR

        connection.createStatement().execute("SELECT UpdateGeometrySRID('RCVSCIRCLE', 'THE_GEOM', 2154);");

        connection.createStatement().execute("CREATE TABLE RECEIVERS(PK SERIAL PRIMARY KEY, the_geom GEOMETRY(POINTZ) ) AS SELECT (row_number() over())::int, ST_UPDATEZ(ST_FORCE3D(THE_GEOM),5.0) FROM RCVSCIRCLE;");
       // connection.createStatement().execute("UPDATE RCVS20 SET THE_GEOM = ST_UPDATEZ(ST_FORCE3D(THE_GEOM),5.0);");
        connection.createStatement().execute("UPDATE SOURCESI SET THE_GEOM = ST_UPDATEZ(THE_GEOM,10.0);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('NO_BUILD', 'THE_GEOM', 2154);");
        connection.createStatement().execute("UPDATE NO_BUILD SET HEIGHT = 0;");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('BUILD_GRID2', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('DEM_FENCE', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LANDCOVER', 'THE_GEOM', 2154);");
        //connection.createStatement().execute("UPDATE BUILD_GRID2 SET HEIGHT = 0;");
        String name_output = "real";

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILD_GRID2", "SOURCESI",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);
        pointNoiseMap.setHeightField("HEIGHT");
        pointNoiseMap.setMaximumPropagationDistance(100);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(1);
        pointNoiseMap.setDemTable("DEM_FENCE");
        pointNoiseMap.setSoilTableName("LANDCOVER");

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        try {
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            factory.start();

            pointNoiseMap.setGridDim(1); // force grid size

            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            // Iterate over computation areas
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                // Export as a Google Earth 3d scene
                if (out instanceof ComputeRaysOutAttenuation) {
                    ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                    exportScene(String.format(Locale.ROOT,"target/PtSource_scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                }
            }
        }finally {
            factory.stop();
        }
        connection.commit();

        // Check table creation
        assertTrue(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM " + ldenConfig.lDayTable)) {
            assertTrue(rs.next());
            assertEquals(4361, rs.getInt(1));
        }

        connection.createStatement().execute("CREATE TABLE RESULTS AS SELECT R.the_geom the_geom, R.PK pk, LVL.* FROM "+ ldenConfig.lDayTable + " LVL, RECEIVERS R WHERE LVL.IDRECEIVER = R.PK");
        SHPDriverFunction shpDriver = new SHPDriverFunction();
        shpDriver.exportTable(connection, "RESULTS", new File("target/Results_PtSource"+name_output+".shp"), true,new EmptyProgressVisitor());



        try(ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "+ ldenConfig.lDayTable)) {
            assertTrue(rs.next());
         /*   double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(75, leqs[0], 2.0);
            assertEquals(69, leqs[1], 2.0);
            assertEquals(68, leqs[2], 2.0);
            assertEquals(69, leqs[3], 2.0);
            assertEquals(71, leqs[4], 2.0);
            assertEquals(69, leqs[5], 2.0);
            assertEquals(60, leqs[6], 2.0);
            assertEquals(51, leqs[7], 2.0);

            assertEquals(79, rs.getDouble(9), 2.0);
            assertEquals(75,rs.getDouble(10), 2.0);*/
        }


    }

    public static void exportScene(String name, ProfileBuilder builder, ComputeRaysOutAttenuation result) throws IOException {
        try {
            //List<PropagationPath> propagationPaths = new ArrayList<>();
            //propagationPaths.addAll(((LDENComputeRaysOut) result).ldenData.rays);
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
            kmlDocument.writeHeader();
            if(builder != null) {
                kmlDocument.writeTopographic(builder.getTriangles(), builder.getVertices());
            }
            if(result != null) {
              //  kmlDocument.writeRays(propagationPaths);
            }
            if(builder != null) {
                kmlDocument.writeBuildings(builder);
            }
            if(result != null && !result.getInputData().sourceGeometries.isEmpty() && !result.getInputData().receivers.isEmpty())  {
                kmlDocument.writeProfile(builder.getProfile(result.getInputData().sourceGeometries.get(0).getCoordinate(),result.getInputData().receivers.get(0)));
            }

            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }
}