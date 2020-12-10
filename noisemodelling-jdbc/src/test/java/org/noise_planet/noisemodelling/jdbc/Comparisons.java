package org.noise_planet.noisemodelling.jdbc;

import org.h2.util.StringUtils;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.jdbc.Utils;
import org.noise_planet.noisemodelling.pathfinder.GeoWithSoilType;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class Comparisons {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateAttenuationCnossosTest.class);
    // TODO reduce error epsilon
    private static final double ERROR_EPSILON_high = 3;
    private static final double ERROR_EPSILON_very_high = 15;
    private static final double ERROR_EPSILON_medium = 2;
    private static final double ERROR_EPSILON_low = 1;
    private static final double ERROR_EPSILON_very_low = 0.1;

    private static final double[] HOM_WIND_ROSE = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] FAV_WIND_ROSE = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PointNoiseMapTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }


    /**
     * WayneComparisons1 Comparison with some CadnaA modelisation done by Wayne - test 1
     */
    @Test
    public void WayneComparisons1() throws SQLException, IOException {

        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("Wayne/test1/ROADS.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("Wayne/test1/BUILDINGS.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("Wayne/test1/LDAY.shp").getFile());

        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(connection, ldenConfig);

        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lEveningTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, ldenConfig.lDenTable));
        //1,800,200,60,60
        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);
        ldenConfig.setMergeSources(false); // No idsource column

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS",
                "LDAY");

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

        // Check dB ranges of result
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lDayTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for(int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
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

            assertEquals(85, rs.getDouble(9), 2.0);
            assertEquals(82,rs.getDouble(10), 2.0);
        }



        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lEveningTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
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

            assertEquals(80, rs.getDouble(9), 2.0);
            assertEquals(77,rs.getDouble(10), 2.0);
        }


        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lNightTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                leqs[idfreq - 1] = rs.getDouble(idfreq);
            }
            assertEquals(73.9, leqs[0], 2.0);
            assertEquals(66.72, leqs[1], 2.0);
            assertEquals(65.89, leqs[2], 2.0);
            assertEquals(67.36, leqs[3], 2.0);
            assertEquals(71.13, leqs[4], 2.0);
            assertEquals(68.53, leqs[5], 2.0);
            assertEquals(59.88, leqs[6], 2.0);
            assertEquals(50.87, leqs[7], 2.0);

            assertEquals(77.74, rs.getDouble(9), 2.0);
            assertEquals(74.31,rs.getDouble(10), 2.0);
        }

        try(ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(HZ63) , MAX(HZ125), MAX(HZ250), MAX(HZ500), MAX(HZ1000), MAX(HZ2000), MAX(HZ4000), MAX(HZ8000), MAX(LEQ), MAX(LAEQ) FROM "+ ldenConfig.lDenTable)) {
            assertTrue(rs.next());
            double[] leqs = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];
            for (int idfreq = 1; idfreq <= ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
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

            assertEquals(86, rs.getDouble(9), 2.0);
            assertEquals(83,rs.getDouble(10), 2.0);
        }



    }

}
