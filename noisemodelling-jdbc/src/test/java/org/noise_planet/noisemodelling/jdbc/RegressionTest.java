package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.Attenuation;

import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class RegressionTest {

    private Connection connection;

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(NoiseMapByReceiverMakerTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    /**
     * Got reflection out of bound in this scenario
     */
    @Test
    public void testScenarioOutOfBoundException() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("regression_nan/lw_roads.sql"));

            // Init NoiseModelling
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "LW_ROADS", "RECEIVERS");

            noiseMapByReceiverMaker.setMaximumPropagationDistance(500.0);
            noiseMapByReceiverMaker.setSoundReflectionOrder(1);
            noiseMapByReceiverMaker.setThreadCount(1);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(true);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);
            // Building height field name
            noiseMapByReceiverMaker.setHeightField("HEIGHT");

            // Init custom input in order to compute more than just attenuation
            // LW_ROADS contain Day Evening Night emission spectrum
            NoiseMapParameters noiseMapParameters = new NoiseMapParameters(NoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);

            noiseMapParameters.setComputeLDay(false);
            noiseMapParameters.setComputeLEvening(false);
            noiseMapParameters.setComputeLNight(false);
            noiseMapParameters.setComputeLDEN(true);

            NoiseMapMaker tableWriter = new NoiseMapMaker(connection, noiseMapParameters);

            noiseMapByReceiverMaker.setPropagationProcessDataFactory(tableWriter);
            noiseMapByReceiverMaker.setComputeRaysOutFactory(tableWriter);

            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            noiseMapByReceiverMaker.initialize(connection, new EmptyProgressVisitor());

            noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.DAY).setTemperature(20);
            noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.EVENING).setTemperature(16);
            noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.NIGHT).setTemperature(10);

            noiseMapByReceiverMaker.setGridDim(1);

            // Set of already processed receivers
            Set<Long> receivers = new HashSet<>();

            // Iterate over computation areas
            try {
                tableWriter.start();
                // Fetch cell identifiers with receivers
                Map<CellIndex, Integer> cells = noiseMapByReceiverMaker.searchPopulatedCells(connection);
                ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
                for(CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                    // Run ray propagation
                    noiseMapByReceiverMaker.evaluateCell(connection, cellIndex.getLatitudeIndex(),
                            cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                }
            } finally {
                tableWriter.stop();
            }

        }
    }


}
