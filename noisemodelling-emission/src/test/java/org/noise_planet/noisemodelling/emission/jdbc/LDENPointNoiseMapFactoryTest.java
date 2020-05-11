package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos;
import org.noise_planet.noisemodelling.emission.RSParametersCnossos;
import org.noise_planet.noisemodelling.propagation.EvaluateAttenuationCnossos;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;

import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class LDENPointNoiseMapFactoryTest {

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
            factory.start();
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            pointNoiseMap.setGridDim(1); // force grid to 1x1

            // Iterate over computation areas
            for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
                for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                    // Run ray propagation
                    pointNoiseMap.evaluateCell(connection, i, j, progressLogger, receivers);
                }
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
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM "+ ldenConfig.lDayTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[PropagationProcessPathData.freq_lvl.size()];
            for(int idfreq = 1; idfreq <= PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(83, leqs[0], 2.0);
            assertEquals(74, leqs[1], 2.0);
            assertEquals(73, leqs[2], 2.0);
            assertEquals(75, leqs[3], 2.0);
            assertEquals(79, leqs[4], 2.0);
            assertEquals(77, leqs[5], 2.0);
            assertEquals(68, leqs[6], 2.0);
            assertEquals(59, leqs[7], 2.0);
        }



        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM "+ ldenConfig.lEveningTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[PropagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(76.0, leqs[0], 2.0);
            assertEquals(69.0, leqs[1], 2.0);
            assertEquals(68.0, leqs[2], 2.0);
            assertEquals(70.0, leqs[3], 2.0);
            assertEquals(74.0, leqs[4], 2.0);
            assertEquals(71.0, leqs[5], 2.0);
            assertEquals(62.0, leqs[6], 2.0);
            assertEquals(53.0, leqs[7], 2.0);
        }


        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[PropagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(83.0, leqs[0], 2.0);
            assertEquals(74.0, leqs[1], 2.0);
            assertEquals(73.0, leqs[2], 2.0);
            assertEquals(75.0, leqs[3], 2.0);
            assertEquals(79.0, leqs[4], 2.0);
            assertEquals(76.0, leqs[5], 2.0);
            assertEquals(68.0, leqs[6], 2.0);
            assertEquals(58.0, leqs[7], 2.0);
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000) FROM "+ ldenConfig.lDenTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[PropagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(82.0, leqs[0], 2.0);
            assertEquals(75.0, leqs[1], 2.0);
            assertEquals(74.0, leqs[2], 2.0);
            assertEquals(76.0, leqs[3], 2.0);
            assertEquals(80.0, leqs[4], 2.0);
            assertEquals(77.0, leqs[5], 2.0);
            assertEquals(68.0, leqs[6], 2.0);
            assertEquals(59.0, leqs[7], 2.0);
        }
    }
}