package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.fgb.FGBRead;
import org.h2gis.functions.io.fgb.fileTable.FGBDriver;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.Attenuation;

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
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(NoiseMapByReceiverMakerTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    public static NoiseMapMaker runPropagation(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException, IOException {
        // Init NoiseModelling
        // Building height field name
        noiseMapByReceiverMaker.setHeightField("HEIGHT");


        // Init custom input in order to compute more than just attenuation
        // LW_ROADS contain Day Evening Night emission spectrum
        NoiseMapParameters noiseMapParameters = new NoiseMapParameters(NoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapParameters.setExportRaysMethod(NoiseMapParameters.ExportRaysMethods.TO_MEMORY);

        noiseMapParameters.setComputeLDay(false);
        noiseMapParameters.setComputeLEvening(false);
        noiseMapParameters.setComputeLNight(false);
        noiseMapParameters.setComputeLDEN(true);
        noiseMapParameters.keepAbsorption = true;

        NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, noiseMapParameters);

        noiseMapByReceiverMaker.setPropagationProcessDataFactory(noiseMapMaker);
        noiseMapByReceiverMaker.setComputeRaysOutFactory(noiseMapMaker);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        noiseMapByReceiverMaker.initialize(connection, new EmptyProgressVisitor());

        noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.DAY).setTemperature(20);
        noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.EVENING).setTemperature(16);
        noiseMapParameters.getPropagationProcessPathData(NoiseMapParameters.TIME_PERIOD.NIGHT).setTemperature(10);

        noiseMapByReceiverMaker.setGridDim(1);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        // Fetch cell identifiers with receivers
        Map<CellIndex, Integer> cells = noiseMapByReceiverMaker.searchPopulatedCells(connection);
        ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
        assertEquals(1, cells.size());
        for(CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
            // Run ray propagation
            noiseMapByReceiverMaker.evaluateCell(connection, cellIndex.getLatitudeIndex(),
                    cellIndex.getLongitudeIndex(), progressVisitor, receivers);
        }
        return noiseMapMaker;
    }

    @Test
    public void testNoLevelRegression() throws Exception {
        try(Statement st = connection.createStatement()) {
            FGBRead.execute(connection, RegressionTest.class.getResource("regression_nopath/LW_ROADS.fgb").getFile(), "LW_ROADS");
            FGBRead.execute(connection, RegressionTest.class.getResource("regression_nopath/DEM_SELECTION.fgb").getFile(), "DEM");
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");

            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (371505.98977727786405012 6657413.14829147700220346 4.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (371151.06905939488206059 6657414.96568119246512651 4.0)');");

            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "LW_ROADS", "RECEIVERS");

            noiseMapByReceiverMaker.setMaximumPropagationDistance(500.0);
            noiseMapByReceiverMaker.setSoundReflectionOrder(1);
            noiseMapByReceiverMaker.setThreadCount(1);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(true);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);


            NoiseMapMaker noiseMapMaker = runPropagation(connection, noiseMapByReceiverMaker);
            assertNotNull(noiseMapMaker.getLdenData().lDenLevels.peekFirst());
            assertEquals(36.77,
                    AcousticIndicatorsFunctions.sumDbArray(noiseMapMaker.getLdenData().lDenLevels.peekFirst().value),
                    AttenuationCnossosTest.ERROR_EPSILON_LOWEST);
        }
    }

    /**
     * Got reflection index out of bound exception in this scenario in the past (source->reflection->h diffraction->receiver)
     */
    @Test
    public void testScenarioOutOfBoundException() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("regression_nan/lw_roads.sql"));


            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "LW_ROADS", "RECEIVERS");

            noiseMapByReceiverMaker.setMaximumPropagationDistance(500.0);
            noiseMapByReceiverMaker.setSoundReflectionOrder(1);
            noiseMapByReceiverMaker.setThreadCount(1);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(true);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);


            NoiseMapMaker noiseMapMaker = runPropagation(connection, noiseMapByReceiverMaker);
            assertNotNull(noiseMapMaker.getLdenData().lDenLevels.peekFirst());
            assertEquals(36.77,
                    AcousticIndicatorsFunctions.sumDbArray(noiseMapMaker.getLdenData().lDenLevels.peekFirst().value),
                    AttenuationCnossosTest.ERROR_EPSILON_LOWEST);
        }
    }


}
