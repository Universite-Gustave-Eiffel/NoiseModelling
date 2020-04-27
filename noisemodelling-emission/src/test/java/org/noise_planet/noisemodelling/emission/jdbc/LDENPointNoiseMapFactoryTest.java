package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.propagation.ComputeRays;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testTableGenerationFromTraffic() throws SQLException, IOException {
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile());
        SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("receivers.shp").getFile());

        LDENPointNoiseMapFactory factory = new LDENPointNoiseMapFactory(LDENPointNoiseMapFactory.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW);

        assertFalse(JDBCUtilities.tableExists(connection, factory.lDayTable));
        assertFalse(JDBCUtilities.tableExists(connection, factory.lEveningTable));
        assertFalse(JDBCUtilities.tableExists(connection, factory.lNightTable));
        assertFalse(JDBCUtilities.tableExists(connection, factory.lDenTable));

        factory.setComputeLDay(true);
        factory.setComputeLEvening(true);
        factory.setComputeLNight(true);
        factory.setMergeSources(true);

        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_TRAFF",
                "RECEIVERS");

        pointNoiseMap.setComputeRaysOutFactory(factory);
        pointNoiseMap.setPropagationProcessDataFactory(factory);

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(false);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();


        // Iterate over computation areas
        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                // Run ray propagation
                pointNoiseMap.evaluateCell(connection, i, j, new EmptyProgressVisitor(), receivers);
            }
        }

        assertTrue(JDBCUtilities.tableExists(connection, factory.lDayTable));
        assertTrue(JDBCUtilities.tableExists(connection, factory.lEveningTable));
        assertTrue(JDBCUtilities.tableExists(connection, factory.lNightTable));
        assertTrue(JDBCUtilities.tableExists(connection, factory.lDenTable));
    }
}