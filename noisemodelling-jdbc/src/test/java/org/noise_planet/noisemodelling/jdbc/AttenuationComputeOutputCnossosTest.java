/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.AttenuationOutputMultiThread;
import org.noise_planet.noisemodelling.jdbc.output.ResultsCache;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;
import org.noise_planet.noisemodelling.propagation.cnossos.SegmentPath;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.WallAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.dbaToW;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.multiplicationArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumDbArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.wToDb;

/**
 * Test class evaluation and testing attenuation values.
 */
public class AttenuationComputeOutputCnossosTest {

    /**
     *  Error for planes values
     */
    public static final double DELTA_PLANES = 0.1;

    /**
     *  Error for coordinates
     */
    public static final double DELTA_COORDS = 0.1;

    /**
     *  Error for G path value
     */
    public static final double DELTA_G_PATH = 0.02;

    private final static Logger LOGGER = LoggerFactory.getLogger(AttenuationComputeOutputCnossosTest.class);
    public static final double ERROR_EPSILON_HIGHEST = 1e5;
    public static final double ERROR_EPSILON_VERY_HIGH = 15;
    public static final double ERROR_EPSILON_HIGH = 3;
    public static final double ERROR_EPSILON_MEDIUM = 1;
    public static final double ERROR_EPSILON_LOW = 0.5;
    public static final double ERROR_EPSILON_VERY_LOW = 0.1;
    public static final double ERROR_EPSILON_LOWEST = 0.02;

    private static final double[] HOM_WIND_ROSE = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] FAV_WIND_ROSE = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;
    private static final double[] SOUND_POWER_LEVELS = new double[]{93, 93, 93, 93, 93, 93, 93, 93};
    private static final double[] A_WEIGHTING = new double[]{-26.2, -16.1, -8.6, -3.2, 0.0, 1.2, 1.0, -1.1};



    private static void assertDoubleArrayEquals(String valueName, double[] expected, double [] actual, double delta) {
        assertEquals(expected.length, actual.length, valueName + ": Different array length;");
        for(int i=0; i< expected.length; i++) {
            if(!Double.isNaN(expected[i])){
                assertEquals(expected[i], actual[i], delta, valueName + ": Arrays first differed at element ["+i+"];");
            }
        }
    }
    private static void writeResultsToRst(String fileName, String testName, String variableName, double[] expected, double[] actual) {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.append("Test Case: " + testName + "\n");
            writer.append("Variable: " + variableName + "\n");
            writer.append("Expected: " + Arrays.toString(expected) + "\n");
            writer.append("Actual: " + Arrays.toString(actual) + "\n");
            writer.append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test body-barrier effect
     * NMPB08 – Railway Emission Model
     * Programmers Guide
     */
    @Test
    public void testBodyBarrier() {

        GeometryFactory f = new GeometryFactory();

        // Hard barrier
        List<Double> alphas = Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addWall(new Coordinate[]{
                        new Coordinate(3, -100, 0),
                        new Coordinate(3, 100, 0)
                }, 2.5,alphas,1)
                .finishFeeding();

        //Propagation data building
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);

        scene.addSource(f.createPoint(new Coordinate(0.5, 0, 0.)));
        scene.addReceiver(new Coordinate(25, 0, 4));
        scene.defaultGroundAttenuation = 1.0;
        scene.reflexionOrder=1;
        scene.maxSrcDist = 1000;
        scene.setComputeHorizontalDiffraction(false);
        scene.setComputeVerticalDiffraction(true);
        scene.setBodyBarrier(true);

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);
        scene.defaultCnossosParameters = attData;

        //Run computation
        AttenuationComputeOutput propDataOut0 = new AttenuationComputeOutput(true, true,
                scene);

        PathFinder computeRays0 = new PathFinder(scene);
        computeRays0.setThreadCount(1);
        computeRays0.run(propDataOut0);
        double[] values0 = propDataOut0.receiversAttenuationLevels.pop().levels;

        // Barrier, no interaction
        scene.setBodyBarrier(false);
        AttenuationComputeOutput propDataOut1 = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays1 = new PathFinder(scene);
        computeRays1.setThreadCount(1);
        computeRays1.run(propDataOut1);
        double[] values1 = propDataOut1.receiversAttenuationLevels.pop().levels;

        // Soft barrier (a=0.5)

        alphas = Arrays.asList(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        scene.profileBuilder.processedWalls.get(0).setAlpha(alphas);
        scene.reflexionOrder=1;
        scene.maxSrcDist = 1000;
        scene.setComputeHorizontalDiffraction(false);
        scene.setComputeVerticalDiffraction(true);
        scene.setBodyBarrier(true);

        AttenuationComputeOutput propDataOut2 = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays2 = new PathFinder(scene);
        computeRays2.run(propDataOut2);
        double[] values2 = propDataOut2.receiversAttenuationLevels.pop().levels;

        // No barrier
        scene.profileBuilder.processedWalls.get(0).height = 0;
        scene.reflexionOrder=1;
        scene.maxSrcDist = 1000;
        scene.setComputeHorizontalDiffraction(false);
        scene.setComputeVerticalDiffraction(true);
        scene.setBodyBarrier(false);
        AttenuationComputeOutput propDataOut3 = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays3 = new PathFinder(scene);
        computeRays3.run(propDataOut3);
        double[] values3 = propDataOut3.receiversAttenuationLevels.pop().levels;


        double[] values0A = sumArray(values0, new double[]{-26.2,-16.1,-8.6,3.2,0,1.2,1.0,-1.1});
        double[] values1A = sumArray(values1, new double[]{-26.2,-16.1,-8.6,3.2,0,1.2,1.0,-1.1});
        double[] values2A = sumArray(values2, new double[]{-26.2,-16.1,-8.6,3.2,0,1.2,1.0,-1.1});
        double[] values3A = sumArray(values3, new double[]{-26.2,-16.1,-8.6,3.2,0,1.2,1.0,-1.1});
        double r0A = AcousticIndicatorsFunctions.wToDb(sumArray(dbaToW(values0A)));
        double r1A = AcousticIndicatorsFunctions.wToDb(sumArray(dbaToW(values1A)));
        double r2A = AcousticIndicatorsFunctions.wToDb(sumArray(dbaToW(values2A)));
        double r3A = AcousticIndicatorsFunctions.wToDb(sumArray(dbaToW(values3A)));

        assertEquals(19.2,r3A-r1A,0.5);
        assertEquals(11.7,r0A-r1A,1);
        assertEquals(6.6,r2A-r1A,1);
    }

    /**
     * Test reflexion ray has contribution :
     *
     *   ------------------------------
     *               /\
     *              | R
     *              | |
     *              |/
     *              S
     */
    @Test
    public void testSimpleReflexion() {
        //Profile building
        GeometryFactory f = new GeometryFactory();
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addWall(new Coordinate[]{
                        new Coordinate(-100, 40, 10),
                        new Coordinate(100, 40, 10)
                }, -1)
                .finishFeeding();

        //Propagation data building
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(f.createPoint(new Coordinate(30, -10, 2)));
        scene.addReceiver(new Coordinate(30, 20, 2));
        scene.setDefaultGroundAttenuation(0.0);
        scene.reflexionOrder=0;

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);
        scene.defaultCnossosParameters = attData;

        //Run computation
        AttenuationComputeOutput propDataOut0 = new AttenuationComputeOutput(true, true, scene);
        PathFinder pathFinder = new PathFinder(scene);
        pathFinder.setThreadCount(1);
        scene.reflexionOrder=0;
        pathFinder.run(propDataOut0);
        double[] values0 = propDataOut0.receiversAttenuationLevels.pop().levels;

        AttenuationComputeOutput propDataOut1 = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays1 = new PathFinder(scene);
        computeRays1.setThreadCount(1);
        scene.reflexionOrder=1;
        computeRays1.run(propDataOut1);
        double[] values1 = propDataOut1.receiversAttenuationLevels.pop().levels;
        assertNotEquals(values0[0], values1[0]);
        assertNotEquals(values0[1], values1[1]);
        assertNotEquals(values0[2], values1[2]);
        assertNotEquals(values0[3], values1[3]);
        assertNotEquals(values0[4], values1[4]);
        assertNotEquals(values0[5], values1[5]);
        assertNotEquals(values0[6], values1[6]);
        assertNotEquals(values0[7], values1[7]);
    }

    /**
     * Test identic rays : to the east, to the west
     */
    @Test
    public void eastWestTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addBuilding(new Coordinate[]{
                        new Coordinate(10, -5),
                        new Coordinate(20, -5),
                        new Coordinate(20, 5),
                        new Coordinate(10, 5)
                }, 0.0)
                .addBuilding(new Coordinate[]{
                        new Coordinate(-10, -5),
                        new Coordinate(-20, -5),
                        new Coordinate(-20, 5),
                        new Coordinate(-10, 5)
                }, 0.0)
                .finishFeeding();

        //Propagation data building
        GeometryFactory f = new GeometryFactory();
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(f.createPoint(new Coordinate(0, 0, 2)));
        scene.addReceiver(new Coordinate(30, 0, 2));
        scene.addReceiver(new Coordinate(-30, 0, 2));
        scene.defaultGroundAttenuation = 0.0;
        scene.reflexionOrder = 0;

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        scene.defaultCnossosParameters = attData;

        //Out and computation settings
        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
        SegmentPath s0 = propDataOut.getPropagationPaths().get(0).getSRSegment();
        SegmentPath s1 = propDataOut.getPropagationPaths().get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().levels, propDataOut.receiversAttenuationLevels.pop().levels, Double.MIN_VALUE);
    }

    /**
     * Test identic rays : to the east, to the west
     */
    @Test
    public void northSouthTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addBuilding(new Coordinate[]{
                        new Coordinate(-5, 10),
                        new Coordinate(-5, 20),
                        new Coordinate(5, 20),
                        new Coordinate(5, 10)
                }, 1.0)
                .addBuilding(new Coordinate[]{
                        new Coordinate(-5, -10 ),
                        new Coordinate(-5, -20 ),
                        new Coordinate(5, -20),
                        new Coordinate(5, -10)
                }, 1.0)
                .finishFeeding();

        //Propagation data building
        GeometryFactory f = new GeometryFactory();
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(f.createPoint(new Coordinate(0, 0, 2)));
        scene.addReceiver(new Coordinate(0, 30, 2));
        scene.addReceiver(new Coordinate(0, -30, 2));

        scene.reflexionOrder = 0;

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertEquals(2, propDataOut.getPropagationPaths().size());

        SegmentPath s0 = propDataOut.getPropagationPaths().get(0).getSRSegment();
        SegmentPath s1 = propDataOut.getPropagationPaths().get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().levels, propDataOut.receiversAttenuationLevels.pop().levels, Double.MIN_VALUE);
    }

    @Test
    public void northSouthGroundTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addGroundEffect(-50, 50, -5, 5, 0.5)
                .finishFeeding();

        //Propagation data building
        GeometryFactory f = new GeometryFactory();
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(f.createPoint(new Coordinate(0, 0, 2)));
        scene.addReceiver(new Coordinate(0, 30, 2));
        scene.addReceiver(new Coordinate(0, -30, 2));

        scene.reflexionOrder = 0;

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertEquals(2, propDataOut.getPropagationPaths().size());

        SegmentPath s0 = propDataOut.getPropagationPaths().get(0).getSRSegment();
        SegmentPath s1 = propDataOut.getPropagationPaths().get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().levels, propDataOut.receiversAttenuationLevels.pop().levels, Double.MIN_VALUE);
    }

    @Test
    public void eastWestGroundTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addGroundEffect(-5, 5, -50, 50, 0.5)
                .finishFeeding();


        //Propagation data building
        GeometryFactory f = new GeometryFactory();
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(f.createPoint(new Coordinate(0, 0, 2)));
        scene.addReceiver(new Coordinate(30, 0, 2));
        scene.addReceiver(new Coordinate(-30, 0, 2));
        scene.defaultGroundAttenuation = 0.0;
        scene.reflexionOrder = 0;

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        scene.defaultCnossosParameters = attData;

        //Out and computation settings
        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, scene);
        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
        SegmentPath s0 = propDataOut.getPropagationPaths().get(0).getSRSegment();
        SegmentPath s1 = propDataOut.getPropagationPaths().get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().levels, propDataOut.receiversAttenuationLevels.pop().levels, Double.MIN_VALUE);
    }
//
//    @Test
//    public void TestFavorableConditionAttenuationRose() {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Create obstruction test object
//        ProfileBuilder builder = new ProfileBuilder();
//
//        builder.finishFeeding();
//
//        //Propagation data building
//        Vector3D northReceiver = new Vector3D(0, 100, 4);
//        List<Vector3D> receivers = new ArrayList<>();
//        receivers.add(northReceiver);
//        receivers.add(Orientation.rotate(new Orientation(45, 0, 0), northReceiver)); // NW
//        receivers.add(Orientation.rotate(new Orientation(90, 0, 0), northReceiver)); // W
//        receivers.add(Orientation.rotate(new Orientation(135, 0, 0), northReceiver)); // SW
//        receivers.add(Orientation.rotate(new Orientation(180, 0, 0), northReceiver)); // S
//        receivers.add(Orientation.rotate(new Orientation(225, 0, 0), northReceiver)); // SE
//        receivers.add(Orientation.rotate(new Orientation(270, 0, 0), northReceiver)); // E
//        receivers.add(Orientation.rotate(new Orientation(315, 0, 0), northReceiver)); // NE
//        ProfileBuilderDecorator propagationDataBuilder = new ProfileBuilderDecorator(builder)
//                .addSource(0, 0, 4);
//        for(Vector3D receiver : receivers) {
//            propagationDataBuilder.addReceiver(receiver.getX(), receiver.getY(), receiver.getZ());
//        }
//        SceneWithAttenuation scene = new SceneWithAttenuation(builder);
//        scene.setDefaultGroundAttenuation(0.5);
//        scene.setComputeHorizontalDiffraction(true);
//        scene.reflexionOrder=1;
//        scene.maxSrcDist = 1500;
//
//        double[][] windRoseTest = new double[receivers.size()][];
//        // generate favorable condition for each direction
//        for(int idReceiver : IntStream.range(0, receivers.size()).toArray()) {
//            windRoseTest[idReceiver] = new double[AttenuationCnossosParameters.DEFAULT_WIND_ROSE.length];
//            double angle = Math.atan2(receivers.get(idReceiver).getY(), receivers.get(idReceiver).getX());
//            Arrays.fill(windRoseTest[idReceiver], 1);
//            int roseIndex = AttenuationCnossosParameters.getRoseIndex(angle);
//            windRoseTest[idReceiver][roseIndex] = 0.5;
//        }
//        for(int idReceiver : IntStream.range(0, receivers.size()).toArray()) {
//            double[] favorableConditionDirections = windRoseTest[idReceiver];
//            //Propagation process path data building
//            AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
//            attData.setHumidity(HUMIDITY);
//            attData.setTemperature(TEMPERATURE);
//            attData.setWindRose(favorableConditionDirections);
//
//            //Out and computation settings
//            AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, attData, scene);
//            PathFinder computeRays = new PathFinder(scene);
//            computeRays.setThreadCount(1);
//            computeRays.run(propDataOut);
//
//            int maxPowerReceiverIndex = -1;
//            double maxGlobalValue = Double.NEGATIVE_INFINITY;
//            for (AttenuationComputeOutput.SourceReceiverAttenuation v : propDataOut.getVerticesSoundLevel()) {
//                double globalValue = AcousticIndicatorsFunctions.sumDbArray(v.value);
//                if (globalValue > maxGlobalValue) {
//                    maxGlobalValue = globalValue;
//                    maxPowerReceiverIndex = v.receiver.receiverIndex;
//                }
//            }
//            assertEquals(idReceiver, maxPowerReceiverIndex);
//        }
//    }
//
//
//    private double testIgnoreNonSignificantSourcesParam(Connection connection, double maxError) throws SQLException, IOException {
//        // Init NoiseModelling
//        LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS",
//                "LW_ROADS", "RECEIVERS");
//
//        ldenNoiseMapLoader.setMaximumPropagationDistance(5000.0);
//        ldenNoiseMapLoader.setSoundReflectionOrder(1);
//        ldenNoiseMapLoader.setThreadCount(1);
//        ldenNoiseMapLoader.setComputeHorizontalDiffraction(true);
//        ldenNoiseMapLoader.setComputeVerticalDiffraction(true);
//        // Building height field name
//        ldenNoiseMapLoader.setHeightField("HEIGHT");
//
//
//        // Init custom input in order to compute more than just attenuation
//        // LW_ROADS contain Day Evening Night emission spectrum
//        LdenNoiseMapParameters ldenNoiseMapParameters = new LdenNoiseMapParameters(LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
//        ldenNoiseMapParameters.setExportRaysMethod(LdenNoiseMapParameters.ExportRaysMethods.TO_MEMORY);
//
//        ldenNoiseMapParameters.setComputeLDay(false);
//        ldenNoiseMapParameters.setComputeLEvening(false);
//        ldenNoiseMapParameters.setComputeLNight(false);
//        ldenNoiseMapParameters.setComputeLDEN(true);
//        ldenNoiseMapParameters.keepAbsorption = true;
//        ldenNoiseMapParameters.setMaximumError(maxError);
//
//        NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, ldenNoiseMapParameters);
//
//        ldenNoiseMapLoader.setPropagationProcessDataFactory(noiseMapMaker);
//        ldenNoiseMapLoader.setComputeRaysOutFactory(noiseMapMaker);
//
//        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);
//
//        ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());
//
//        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.DAY).setTemperature(20);
//        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.EVENING).setTemperature(16);
//        ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.NIGHT).setTemperature(10);
//
//        ldenNoiseMapLoader.setGridDim(1);
//
//        // Set of already processed receivers
//        Set<Long> receivers = new HashSet<>();
//
//        // Fetch cell identifiers with receivers
//        Map<CellIndex, Integer> cells = ldenNoiseMapLoader.searchPopulatedCells(connection);
//        ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
//        assertEquals(1, cells.size());
//        for (CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
//            // Run ray propagation
//            IComputePathsOut out = ldenNoiseMapLoader.evaluateCell(connection, cellIndex.getLatitudeIndex(),
//                    cellIndex.getLongitudeIndex(), progressVisitor, receivers);
//            assertInstanceOf(AttenuationOutputMultiThread.class, out);
//            AttenuationOutputMultiThread rout = (AttenuationOutputMultiThread) out;
//            assertEquals(1, rout.resultsCache.lDenLevels.size());
//            AttenuationComputeOutput.SourceReceiverAttenuation sl = rout.resultsCache.lDenLevels.pop();
//            return AcousticIndicatorsFunctions.sumDbArray(sl.value);
//        }
//        return 0;
//    }

    static public void assertInferiorThan(double expected, double actual) {
        assertTrue(expected < actual, String.format(Locale.ROOT, "Expected %f < %f", expected, actual));
    }
//
//    /**
//     * Test optimisation feature {@link NoiseMapDatabaseParameters#setMaximumError(double)}
//     * This feature is disabled and all sound sources are computed
//     */
//    @Test
//    public void testIgnoreNonSignificantSources() throws Exception {
//        final double maxError = 0.5;
//        try (Connection connection =
//                     JDBCUtilities.wrapConnection(
//                             H2GISDBFactory.createSpatialDataBase(
//                                     "testReceiverOverBuilding", true, ""))) {
//            try (Statement st = connection.createStatement()) {
//                st.execute(Utils.getRunScriptRes("scenario_skip_far_source.sql"));
//                double levelAllSources = testIgnoreNonSignificantSourcesParam(connection, 0.);
//                double levelIgnoreFarSources = testIgnoreNonSignificantSourcesParam(connection, maxError);
//                assertNotEquals(levelAllSources, levelIgnoreFarSources, 0.0001);
//                assertInferiorThan(Math.abs(levelAllSources - levelIgnoreFarSources), maxError);
//            }
//        }
//    }

    @Test
    public void testRoseIndex() {
        double angle_section = (2 * Math.PI) / AttenuationCnossosParameters.DEFAULT_WIND_ROSE.length;
        double angleStart = Math.PI / 2 - angle_section / 2;
        for(int i = 0; i < AttenuationCnossosParameters.DEFAULT_WIND_ROSE.length; i++) {
            double angle = angleStart - angle_section * i - angle_section / 3;
            int index = AttenuationCnossosParameters.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);angle = angleStart - angle_section * i - angle_section * 2.0/3.0;
            index = AttenuationCnossosParameters.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);
        }
    }
//
//    /**
//     * Check if Li coefficient computation and line source subdivision are correctly done
//     * @throws LayerDelaunayError
//     */
//    @Test
//    public void testSourceLines()  throws LayerDelaunayError, IOException, ParseException {
//
//        // First Compute the scene with only point sources at 1m each
//        GeometryFactory factory = new GeometryFactory();
//        WKTReader wktReader = new WKTReader(factory);
//        LineString geomSource = (LineString)wktReader.read("LINESTRING (51 40.5 0.05, 51 55.5 0.05)");
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));
//
//        //Create obstruction test object
//        ProfileBuilder builder = new ProfileBuilder();
//
//        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);
//
//        builder.finishFeeding();
//
//        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
//        for(int i = 0; i < roadLvl.length; i++) {
//            roadLvl[i] = dbaToW(roadLvl[i]);
//        }
//
//        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
//        rayData.addReceiver(new Coordinate(50, 50, 0.05));
//        rayData.addReceiver(new Coordinate(48, 50, 4));
//        rayData.addReceiver(new Coordinate(44, 50, 4));
//        rayData.addReceiver(new Coordinate(40, 50, 4));
//        rayData.addReceiver(new Coordinate(20, 50, 4));
//        rayData.addReceiver(new Coordinate(0, 50, 4));
//
//        List<Coordinate> srcPtsRef = new ArrayList<>();
//        PathFinder.splitLineStringIntoPoints(geomSource, 1.0, srcPtsRef);
//        for(Coordinate srcPtRef : srcPtsRef) {
//            rayData.addSource(factory.createPoint(srcPtRef), roadLvl);
//        }
//
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.setComputeVerticalDiffraction(true);
//        rayData.maxSrcDist = 2000;
//
//        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//
//        RayOut propDataOut = new RayOut(true, attData, rayData);
//        PathFinder computeRays = new PathFinder(rayData);
//        computeRays.makeRelativeZToAbsolute();
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//
//        // Second compute the same scene but with a line source
//        rayData.clearSources();
//        rayData.addSource(geomSource, roadLvl);
//        RayOut propDataOutTest = new RayOut(true, attData, rayData);
//        computeRays.run(propDataOutTest);
//
//        // Merge levels for each receiver for point sources
//        Map<Long, double[]> levelsPerReceiver = new HashMap<>();
//        for(AttenuationComputeOutput.SourceReceiverAttenuation lvl : propDataOut.receiversAttenuationLevels) {
//            if(!levelsPerReceiver.containsKey(lvl.receiver.receiverPk)) {
//                levelsPerReceiver.put(lvl.receiver.receiverPk, lvl.value);
//            } else {
//                // merge
//                levelsPerReceiver.put(lvl.receiver.receiverPk, sumDbArray(levelsPerReceiver.get(lvl.receiver.receiverPk),
//                        lvl.value));
//            }
//        }
//
//
//        // Merge levels for each receiver for lines sources
//        Map<Long, double[]> levelsPerReceiverLines = new HashMap<>();
//        for(AttenuationComputeOutput.SourceReceiverAttenuation lvl : propDataOutTest.receiversAttenuationLevels) {
//            if(!levelsPerReceiverLines.containsKey(lvl.receiver.receiverPk)) {
//                levelsPerReceiverLines.put(lvl.receiver.receiverPk, lvl.value);
//            } else {
//                // merge
//                levelsPerReceiverLines.put(lvl.receiver.receiverPk, sumDbArray(levelsPerReceiverLines.get(lvl.receiver.receiverPk),
//                        lvl.value));
//            }
//        }
//
//        assertEquals(6, levelsPerReceiverLines.size());
//        assertEquals(6, levelsPerReceiver.size());
//
//        for(int i = 0; i < levelsPerReceiver.size(); i++) {
//            assertArrayEquals(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i), 0.2);
//        }
//    }



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
        Arrays.fill(sourcePower, 70.0);

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
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        double firstPowerAtReceiver = 0;
        for(int i = 0; i < 100; i++) {

            //Out and computation settings
            AttenuationComputeOutput propDataOut = new AttenuationOutputMultiThread(false, false, scene);
            scene.reflexionOrder = i;
            PathFinder computeRays = new PathFinder(scene);
            computeRays.setThreadCount(1);

            //Run computation
            computeRays.run(propDataOut);

            //Actual values
            // number of propagation paths between two walls = reflectionOrder * 2 + 1
            assertEquals(i * 2 + 1, propDataOut.rayCount.get());

            double globalPowerAtReceiver = AcousticIndicatorsFunctions.sumArray(propDataOut.receiversAttenuationLevels.pop().levels);
            if(i == 0) {
                firstPowerAtReceiver = globalPowerAtReceiver;
            } else {
                assertEquals(firstPowerAtReceiver, globalPowerAtReceiver, 3.0);
            }
        }
    }
//
//
//    /**
//     * Test reported issue with receiver over building
//     */
//    @Test
//    public void testReceiverOverBuilding() throws LayerDelaunayError, ParseException {
//
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));
//
//        WKTReader wktReader = new WKTReader();
//        //Create obstruction test object
//        ProfileBuilder builder = new ProfileBuilder();
//
//        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);
//
//        builder.addBuilding(wktReader.read("POLYGON ((-111 -35, -111 82, 70 82, 70 285, 282 285, 282 -35, -111 -35))"), 10, -1);
//
//        builder.finishFeeding();
//
//        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
//        for(int i = 0; i < roadLvl.length; i++) {
//            roadLvl[i] = dbaToW(roadLvl[i]);
//        }
//
//        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
//        rayData.addReceiver(new Coordinate(162, 80, 150));
//        rayData.addSource(factory.createPoint(new Coordinate(-150, 200, 1)), roadLvl);
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.setComputeVerticalDiffraction(true);
//
//        rayData.maxSrcDist = 2000;
//
//        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//        RayOut propDataOut = new RayOut(true, attData, rayData);
//        PathFinder computeRays = new PathFinder(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        assertEquals(1, propDataOut.receiversAttenuationLevels.size());
//
//        assertEquals(14.6, wToDba(sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
//    }

}

