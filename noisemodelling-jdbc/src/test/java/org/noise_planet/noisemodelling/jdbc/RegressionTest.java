package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class RegressionTest {

    private Connection connection;

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LdenAttenuationOutputMultiThreadLoaderTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    public static NoiseMapMaker runPropagation(Connection connection, LdenNoiseMapLoader ldenNoiseMapLoader) throws SQLException, IOException {
        // Init NoiseModelling
        // Building height field name
        ldenNoiseMapLoader.setHeightField("HEIGHT");


        // Init custom input in order to compute more than just attenuation
        // LW_ROADS contain Day Evening Night emission spectrum
        LdenNoiseMapParameters ldenNoiseMapParameters = new LdenNoiseMapParameters(LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
        ldenNoiseMapParameters.setExportRaysMethod(LdenNoiseMapParameters.ExportRaysMethods.TO_MEMORY);

        ldenNoiseMapParameters.setComputeLDay(false);
        ldenNoiseMapParameters.setComputeLEvening(false);
        ldenNoiseMapParameters.setComputeLNight(false);
        ldenNoiseMapParameters.setComputeLDEN(true);
        ldenNoiseMapParameters.keepAbsorption = true;

        NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, ldenNoiseMapParameters);

        ldenNoiseMapLoader.setPropagationProcessDataFactory(noiseMapMaker);
        ldenNoiseMapLoader.setComputeRaysOutFactory(noiseMapMaker);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());

        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.DAY).setTemperature(20);
        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.EVENING).setTemperature(16);
        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.NIGHT).setTemperature(10);

        ldenNoiseMapLoader.setGridDim(1);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        // Fetch cell identifiers with receivers
        Map<CellIndex, Integer> cells = ldenNoiseMapLoader.searchPopulatedCells(connection);
        ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
        assertEquals(1, cells.size());
        for(CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
            // Run ray propagation
            ldenNoiseMapLoader.evaluateCell(connection, cellIndex.getLatitudeIndex(),
                    cellIndex.getLongitudeIndex(), progressVisitor, receivers);
        }
        return noiseMapMaker;
    }

    /**
     * Got reflection index out of bound exception in this scenario in the past (source->reflection->h diffraction->receiver)
     */
    @Test
    public void testScenarioOutOfBoundException() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("regression_nan/lw_roads.sql"));


            LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS",
                    "LW_ROADS", "RECEIVERS");

            ldenNoiseMapLoader.setMaximumPropagationDistance(500.0);
            ldenNoiseMapLoader.setSoundReflectionOrder(1);
            ldenNoiseMapLoader.setThreadCount(1);
            ldenNoiseMapLoader.setComputeHorizontalDiffraction(true);
            ldenNoiseMapLoader.setComputeVerticalDiffraction(true);


            NoiseMapMaker noiseMapMaker = runPropagation(connection, ldenNoiseMapLoader);
            assertNotNull(noiseMapMaker.getOutputDataDeque().lDenLevels.peekFirst());
            assertEquals(36.77,
                    AcousticIndicatorsFunctions.sumDbArray(noiseMapMaker.getOutputDataDeque().lDenLevels.peekFirst().value),
                    AttenuationComputeOutputCnossosTest.ERROR_EPSILON_LOWEST);
        }
    }


}
