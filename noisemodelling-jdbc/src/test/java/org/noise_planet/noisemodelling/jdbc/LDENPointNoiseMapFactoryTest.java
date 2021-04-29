package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.Assert.*;

public class LDENPointNoiseMapFactoryTest {

    static Logger LOGGER = LoggerFactory.getLogger(LDENPointNoiseMapFactoryTest.class);

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LDENPointNoiseMapFactoryTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testNoiseEmission() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);
        ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData());
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
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.read(connection, LDENPointNoiseMapFactoryTest.class.getResource("RailTrain.dbf").getFile());


        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_RAILWAY_FLOW);
        ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData());
        ldenConfig.setCoefficientVersion(2);
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN", ldenConfig);

        RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        RailWayLW railWayLW = v.getRailWayLW();
        List<LineString> geometries = v.getRailWayLWGeometry( 2); // TODO edit with specific distance set (plamade or other)

        assertTrue(railWayLWIterator.hasNext());

    }

    @Test
    public void testTableGenerationFromTraffic() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

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
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for(int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(83, leqs[0], 2.0);
            assertEquals(76, leqs[1], 2.0);
            assertEquals(75, leqs[2], 2.0);
            assertEquals(76, leqs[3], 2.0);
            assertEquals(79, leqs[4], 2.0);
            assertEquals(77, leqs[5], 2.0);
            assertEquals(68, leqs[6], 2.0);
            assertEquals(59, leqs[7], 2.0);

            assertEquals(86, rs.getDouble(9), 2.0);
            assertEquals(82,rs.getDouble(10), 2.0);
        }



        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lEveningTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(78.0, leqs[0], 2.0);
            assertEquals(72.0, leqs[1], 2.0);
            assertEquals(70.0, leqs[2], 2.0);
            assertEquals(72.0, leqs[3], 2.0);
            assertEquals(74.0, leqs[4], 2.0);
            assertEquals(72.0, leqs[5], 2.0);
            assertEquals(63.0, leqs[6], 2.0);
            assertEquals(54.0, leqs[7], 2.0);

            assertEquals(82, rs.getDouble(9), 2.0);
            assertEquals(78,rs.getDouble(10), 2.0);
        }


        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(76, leqs[0], 2.0);
            assertEquals(69, leqs[1], 2.0);
            assertEquals(68, leqs[2], 2.0);
            assertEquals(69, leqs[3], 2.0);
            assertEquals(71, leqs[4], 2.0);
            assertEquals(68, leqs[5], 2.0);
            assertEquals(60, leqs[6], 2.0);
            assertEquals(51, leqs[7], 2.0);

            assertEquals(79, rs.getDouble(9), 2.0);
            assertEquals(75,rs.getDouble(10), 2.0);
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lDenTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(84.0, leqs[0], 2.0);
            assertEquals(77.0, leqs[1], 2.0);
            assertEquals(76.0, leqs[2], 2.0);
            assertEquals(77.0, leqs[3], 2.0);
            assertEquals(80.0, leqs[4], 2.0);
            assertEquals(77.0, leqs[5], 2.0);
            assertEquals(69.0, leqs[6], 2.0);
            assertEquals(59.0, leqs[7], 2.0);

            assertEquals(87, rs.getDouble(9), 2.0);
            assertEquals(83,rs.getDouble(10), 2.0);
        }
    }


    @Test
    public void testTableGenerationFromTrafficNightOnly() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

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
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
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
            assertEquals(75,rs.getDouble(10), 2.0);
        }

    }

    @Test
    public void testReadFrequencies() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        assertNotNull(ldenConfig.propagationProcessPathData);

        assertEquals(8, ldenConfig.propagationProcessPathData.freq_lvl.size());

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

        assertEquals(1, ldenConfig.propagationProcessPathData.freq_lvl.size());

        assertEquals(1000, (int)ldenConfig.propagationProcessPathData.freq_lvl.get(0));
    }

    @Test
    public void testNoDemBuildingsZ() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);
        pointNoiseMap.setSoundReflectionOrder(0);

        try(Statement st = connection.createStatement()) {
            // Alter buildings polygons Z
            st.execute("UPDATE BUILDINGS SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(ST_FORCE3D(THE_GEOM), 50), 2154)");
            // Use only a subset of receivers
            st.execute("DELETE FROM RECEIVERS WHERE ST_DISTANCE('POINT (223940.83614225042 6757305.252751735)'::geometry, THE_GEOM) > 300");
        }


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
                    for(Coordinate v : out.ldenPropagationProcessData.freeFieldFinder.getVertices()) {
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
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/lw_roads_fence.shp").getFile(), "LW_ROADS");
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/bati_fence.shp").getFile(), "BUILDINGS");
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("regression1/receivers.shp").getFile(), "RECEIVERS");

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

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(750.0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
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
                pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
            }
        }finally {
            factory.stop();
        }
        connection.commit();
        // retrieve receiver value
        assertEquals(nbReceivers, receivers.size());

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT leq, laeq FROM "+ ldenConfig.lDayTable + " LVL, RECEIVERS R WHERE LVL.IDRECEIVER = R.PK2 AND ID = 200")) {
            assertTrue(rs.next());
            assertEquals(56.58, rs.getDouble(1), 2.0);
            assertEquals(49.24,rs.getDouble(2), 2.0);
        }

    }
}