/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.AttenuationOutputMultiThread;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.delaunay.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.WallAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.ReceiverNoiseLevel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;

/**
 * Test class evaluation and testing attenuation values.
 */
public class SceneWithEmissionTest {
    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;
    private List<Long> testIgnoreNonSignificantSourcesParam(Connection connection, double maxError) throws SQLException, IOException {
        return testIgnoreNonSignificantSourcesParam(connection, maxError, "BUILDINGS",
                "LW_ROADS", "RECEIVERS", "");
    }

    private List<Long> testIgnoreNonSignificantSourcesParam(
            Connection connection, double maxError, String buildingsTableName, String sourcesTableName,
            String receiverTableName, String sourcesEmissionTableName) throws SQLException, IOException {

        // Init NoiseModelling
        NoiseMapByReceiverMaker noiseMap = new NoiseMapByReceiverMaker(buildingsTableName,
                sourcesTableName, receiverTableName);

        noiseMap.setMaximumPropagationDistance(5000.0);
        noiseMap.setSoundReflectionOrder(1);
        noiseMap.setThreadCount(1);
        noiseMap.setComputeHorizontalDiffraction(true);
        noiseMap.setComputeVerticalDiffraction(true);
        if(!sourcesEmissionTableName.isEmpty()) {
            noiseMap.setSourcesEmissionTableName(sourcesEmissionTableName);
        }
        NoiseMapDatabaseParameters parameters = noiseMap.getNoiseMapDatabaseParameters();
        parameters.mergeSources = false;
        parameters.maximumError = maxError;

        // Building height field name
        noiseMap.setHeightField("HEIGHT");


        noiseMap.setGridDim(1);

        noiseMap.run(connection, new EmptyProgressVisitor());

        Statement st = connection.createStatement();
        List<Long> sourcePks = new LinkedList<>();
        try(ResultSet rs = st.executeQuery("SELECT DISTINCT IDSOURCE FROM " + parameters.receiversLevelTable)) {
            while (rs.next()) {
                sourcePks.add(rs.getLong(1));
            }
        }
        return sourcePks;
    }

    static public void assertInferiorThan(double expected, double actual) {
        assertTrue(expected < actual, String.format(Locale.ROOT, "Expected %f < %f", expected, actual));
    }

    /**
     * Test optimisation feature {@link NoiseMapDatabaseParameters#setMaximumError(double)}
     * This feature is disabled and all sound sources are computed
     */
    @Test
    public void testIgnoreNonSignificantSources() throws Exception {
        final double maxError = 0.5;
        try (Connection connection =
                     JDBCUtilities.wrapConnection(
                             H2GISDBFactory.createSpatialDataBase(
                                     "testReceiverOverBuilding", true, ""))) {
            try (Statement st = connection.createStatement()) {
                st.execute(Utils.getRunScriptRes("scenario_skip_far_source.sql"));



                List<Long> allSourcesPk = testIgnoreNonSignificantSourcesParam(connection, 0.);
                List<Long> ignoreFarSourcesPk = testIgnoreNonSignificantSourcesParam(connection, maxError);
                assertEquals(2, allSourcesPk.size());
                assertEquals(1, ignoreFarSourcesPk.size());
            }
        }
    }

    private static Map<String, Double> fetchReceiverLevel(Connection connection) throws SQLException {
        Map<String, Double> allSourcesReceiverLevel = new HashMap<>();
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT PERIOD, LEQ FROM RECEIVERS_LEVEL")) {
                // Sum contribution of all sources
                while (rs.next()) {
                    allSourcesReceiverLevel.merge(rs.getString("PERIOD"), dBToW(rs.getDouble("LEQ")), Double::sum);
                }
            }
        }
        return allSourcesReceiverLevel;
    }

    /**
     * Test optimisation feature {@link NoiseMapDatabaseParameters#setMaximumError(double)}
     * This feature is disabled and all sound sources are computed
     */
    @Test
    public void testIgnoreNonSignificantSources2() throws Exception {
        final double maxError = 3.0;

        try (Connection connection =
                     JDBCUtilities.wrapConnection(
                             H2GISDBFactory.createSpatialDataBase(
                                     "testIgnoreNonSignificantSources2", true, ""))) {
            try (Statement st = connection.createStatement()) {
                st.execute(Utils.getRunScriptRes("skip_far_source2.sql"));
                List<Long> allSourcesPk = testIgnoreNonSignificantSourcesParam(connection, 0.,
                        "BUILDINGS", "SOURCES_GEOM",
                        "RECEIVERS", "SOURCES_EMISSION");
                Map<String, Double> allSourcesReceiverLevel = fetchReceiverLevel(connection);
                List<Long> ignoreFarSourcesPk = testIgnoreNonSignificantSourcesParam(connection, maxError,
                        "BUILDINGS", "SOURCES_GEOM",
                        "RECEIVERS", "SOURCES_EMISSION");

                Map<String, Double> someSourcesReceiverLevel = fetchReceiverLevel(connection);
                // The noise level error should be in the expected range
                for (Map.Entry<String, Double> entry : allSourcesReceiverLevel.entrySet()) {
                    String period = entry.getKey();
                    double levelAllSources = wToDb(entry.getValue());
                    assertTrue(someSourcesReceiverLevel.containsKey(period));
                    double levelLimitedSources = wToDb(someSourcesReceiverLevel.get(period));
                    assertTrue(Math.abs(levelAllSources - levelLimitedSources) < maxError);
                }
                // Some sources should be skipped or maxDbError not doing its job
                assertNotEquals( allSourcesPk.size(), ignoreFarSourcesPk.size());
            }
        }
    }

    /**
     * Check if Li coefficient computation and line source subdivision are correctly done
     */
    @Test
    public void testSourceLines()  throws ParseException {

        // First Compute the scene with only point sources at 1m each
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        LineString geomSource = (LineString)wktReader.read("LINESTRING (51 40.5 0.05, 51 55.5 0.05)");

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        double[] roadLvl = AcousticIndicatorsFunctions.dBToW(new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95});

        SceneWithEmission scene = new SceneWithEmission(builder);
        scene.addReceiver(new Coordinate(50, 50, 0.05));
        scene.addReceiver(new Coordinate(48, 50, 4));
        scene.addReceiver(new Coordinate(44, 50, 4));
        scene.addReceiver(new Coordinate(40, 50, 4));
        scene.addReceiver(new Coordinate(20, 50, 4));
        scene.addReceiver(new Coordinate(0, 50, 4));

        List<Coordinate> srcPtsRef = new ArrayList<>();
        PathFinder.splitLineStringIntoPoints(geomSource, 1.0, srcPtsRef);
        for (long i = 0; i < srcPtsRef.size(); i++) {
            Coordinate srcPtRef = srcPtsRef.get((int) i);
            scene.addSource(i, factory.createPoint(srcPtRef));
            scene.addSourceEmission(i, "", roadLvl);
        }

        scene.setComputeHorizontalDiffraction(true);
        scene.setComputeVerticalDiffraction(true);
        scene.maxSrcDist = 2000;

        AttenuationParameters attData = scene.defaultCnossosParameters;
        attData.setHumidity(70);
        attData.setTemperature(10);

        AttenuationOutputMultiThread propDataOut = new AttenuationOutputMultiThread(scene);

        PathFinder computeRays = new PathFinder(scene);
        computeRays.makeRelativeZToAbsolute();
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);


        // Second compute the same scene but with a line source
        scene.clearSources();
        scene.addSource(1L, geomSource);
        scene.addSourceEmission(1L, "", roadLvl);

        AttenuationOutputMultiThread propDataOutTest = new AttenuationOutputMultiThread(scene);
        computeRays.run(propDataOutTest);

        List<ReceiverNoiseLevel> levelsPerReceiver = new ArrayList<>(propDataOut.resultsCache.receiverLevels);
        List<ReceiverNoiseLevel> levelsPerReceiverLines = new ArrayList<>(propDataOutTest.resultsCache.receiverLevels);

        assertEquals(6, levelsPerReceiver.size());
        assertEquals(6, levelsPerReceiverLines.size());

        for(int i = 0; i < levelsPerReceiver.size(); i++) {
            assertArrayEquals(levelsPerReceiver.get(i).levels, levelsPerReceiverLines.get(i).levels, 0.2);
        }
    }



    /**
     * Test of convergence of power at receiver when increasing the reflection order
     * Event at 100 order of reflection then final noise level should not be
     * superior to 3.0 decibels compared to direct power
     */
    @Test
    public void testReflexionConvergence() {

        //Profile building
        List<Integer> alphaWallFrequencies = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(
               ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
        List<Double> alphaWall = new ArrayList<>(alphaWallFrequencies.size());
        for(int frequency : alphaWallFrequencies) {
            alphaWall.add(WallAbsorption.getWallAlpha(100000, frequency));
        }

        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addWall(new Coordinate[]{
                        new Coordinate(6, 0, 4),
                        new Coordinate(-5, 12, 4),
                }, 8, alphaWall, 0)
                .addWall(new Coordinate[]{
                        new Coordinate(14, 4, 4),
                        new Coordinate(3, 16, 4),
                }, 8, alphaWall, 1);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();


        double[] sourcePower = new double[profileBuilder.frequencyArray.size()];
        Arrays.fill(sourcePower,  AcousticIndicatorsFunctions.dBToW(70.0));

        //Propagation data building
        SceneWithEmission scene = new SceneWithEmission(profileBuilder);
        GeometryFactory f = new GeometryFactory();
        scene.addSource(1L, f.createPoint(new Coordinate(8, 5.5, 0.1)));
        scene.addReceiver(new Coordinate(4.5, 8, 1.6));
        scene.setDefaultGroundAttenuation(0.5);
        scene.addSourceEmission(1L, "", sourcePower);

        scene.maxSrcDist = 100*800;
        scene.maxRefDist = 100*800;
        //Propagation process path data building
        AttenuationParameters attData = new AttenuationParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        double firstPowerAtReceiver = 0;
        for(int i = 0; i < 100; i++) {

            //Out and computation settings
            AttenuationOutputMultiThread propDataOut = new AttenuationOutputMultiThread(scene);
            scene.reflexionOrder = i;
            PathFinder computeRays = new PathFinder(scene);
            computeRays.setThreadCount(1);

            //Run computation
            computeRays.run(propDataOut);

            //Actual values
            // number of propagation paths between two walls = reflectionOrder * 2 + 1
            assertEquals(i * 2 + 1, propDataOut.cnossosPathCount.get());

            double globalPowerAtReceiver = AcousticIndicatorsFunctions.sumDbArray(propDataOut.resultsCache.receiverLevels.pop().levels);
            if(i == 0) {
                firstPowerAtReceiver = globalPowerAtReceiver;
            } else {
                assertEquals(firstPowerAtReceiver, globalPowerAtReceiver, 3.0);
            }
        }
    }


    /**
     * Test reported issue with receiver over building
     */
    @Test
    public void testReceiverOverBuilding() throws LayerDelaunayError, ParseException {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        WKTReader wktReader = new WKTReader();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.addBuilding(wktReader.read("POLYGON ((-111 -35, -111 82, 70 82, 70 285, 282 285, 282 -35, -111 -35))"), 10, -1);

        builder.finishFeeding();

        double[] roadLvl = AcousticIndicatorsFunctions.dBToW(new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95});

        SceneWithEmission scene = new SceneWithEmission(builder);
        scene.addReceiver(new Coordinate(162, 80, 150));
        scene.addSource(1L, factory.createPoint(new Coordinate(-150, 200, 1)));
        scene.setComputeHorizontalDiffraction(true);
        scene.setComputeVerticalDiffraction(true);
        scene.addSourceEmission(1L, "", roadLvl);

        scene.maxSrcDist = 2000;

        scene.defaultCnossosParameters.setHumidity(70);
        scene.defaultCnossosParameters.setTemperature(10);

        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);
        AttenuationOutputMultiThread outputMultiThread = new AttenuationOutputMultiThread(scene);
        computeRays.run(outputMultiThread);

        assertEquals(1, outputMultiThread.resultsCache.queueSize.get());

        assertEquals(14.6, AcousticIndicatorsFunctions.wToDb(sumArray(roadLvl.length,
                AcousticIndicatorsFunctions.dBToW(outputMultiThread.resultsCache.receiverLevels.pop().levels))),
                0.1);
    }

}

