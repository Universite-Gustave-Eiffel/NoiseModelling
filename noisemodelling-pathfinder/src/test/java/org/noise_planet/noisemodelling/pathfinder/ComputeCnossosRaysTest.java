package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

public class ComputeCnossosRaysTest {

    /**
     *  Error for coordinates
     */
    private static final double DELTA_COORDS = 0.01;

    /**
     *  Error for planes values
     */
    private static final double DELTA_PLANES = 0.01;

    /**
     *  Error for G path value
     */
    private static final double DELTA_G_PATH = 0.01;

    /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder().finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.0)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 4.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.0} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC02 -- Mixed ground (G = 0,5)
     */
    @Test
    public void TC02() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder().finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 4.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.5} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC03 -- Mixed ground (G = 0,5)
     */
    @Test
    public void TC03() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder().finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(1.0)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 4.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {1.0} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     */
    @Test
    public void TC04() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.2)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.9)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .vEdgeDiff(true)
                .hEdgeDiff(true)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 4.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.2*(40.88/194.16) + 0.5*(102.19/194.16) + 0.9*(51.09/194.16)} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC05 -- Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC05() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
        //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
        //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 14.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };
        double [][] meanPlanes = new double[][]{
                //  a      b    zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 194.59, 0.51, 0.64}
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
        assertPlanes(meanPlanes, propDataOut.getPropagationPaths().get(0).getSRSegment());
        assertPlanes(meanPlanes, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }

    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
        //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
        //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {178.84, 10.0}, {194.16, 11.5}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.53, 0.20} //Path 1 : direct
        };
        double [][] srMeanPlanes = new double[][]{
                //  a      b    zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 3.66, 194.45, 0.51, 0.56}
        };
        double [][] segmentsMeanPlanes = new double[][]{
                //  a      b    zs    zr      dp    Gp   Gp'
                {0.05, -2.33, 3.33, 3.95, 179.06, 0.53, 0.60},
                {0.00, 10.00, 0.00, 1.50, 015.33, 0.20,  NaN}
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
        assertPlanes(srMeanPlanes, propDataOut.getPropagationPaths().get(0).getSRSegment());
        assertPlanes(segmentsMeanPlanes, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }

    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07() {

        GeometryFactory factory = new GeometryFactory();

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()

                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(100, 240, 0),
                                new Coordinate(265, -180, 0)},
                        6, 1)
                // Add ground effect
                .addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9)
                .addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5)
                .addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2)

                .finishFeeding();


        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(false)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {170.23, 6.0}, {194.16, 4.0}} //Path 1 : direct
        };
        double [][] segmentsMeanPlanes = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 6.00, 170.23, 0.55, 0.61},
                {0.00, 0.00, 6.00, 4.00, 023.93, 0.20,  NaN}
        };

        //Assertion
        assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }

    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08() {

        GeometryFactory factory = new GeometryFactory();

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()

                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(175, 50, 0),
                                new Coordinate(190, 10, 0)},
                        6, 1)
                // Add ground effect
                .addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9)
                .addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5)
                .addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2)

                .finishFeeding();


        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {170.49, 6.0}, {194.16, 4.0}}, //Path 1 : direct
                {{0.00, 1.00}, {169.78, 3.61}, {194.78, 4.0}}, //Path 2 : left side
                {{0.00, 1.00}, {180.00, 3.44}, {221.23, 4.0}}  //Path 3 : right side
        };
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 6.00, 170.49, 0.55, 0.61},
                {0.00, 0.00, 6.00, 4.00, 023.68, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 194.78, 0.51, 0.51}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 221.23, 0.46, 0.46}
        };

        //Assertion
        assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short barrier
     */
    @Test
    public void TC09() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(175, 50, 17),
                                new Coordinate(190, 10, 14)},
                         1)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {170.49, 16.63}, {194.16, 14.0}}, //Path 1 : direct
                {{0.00, 1.00}, {169.78, 12.33}, {194.78, 14.0}}, //Path 2 : left side
                {{0.00, 1.00}, {180.00, 11.58}, {221.23, 14.0}}  //Path 3 : right side
        };
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.04, -1.96, 2.96, 11.68, 170.98, 0.55, 0.76},
                {0.04, 1.94, 7.36, 3.71, 23.54, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.05, -2.82, 3.81, 6.23, 195.20, 0.51, 0.64}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.06, -3.10, 4.09, 3.77, 221.62, 0.46, 0.49}
        };

        //Assertion
        assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Assertions for a list of {@link PropagationPath}.
     * @param expectedPts    Array of arrays of array of expected coordinates (xyz) of points of paths. To each path
     *                       corresponds an array of points. To each point corresponds an array of coordinates (xyz).
     * @param expectedGPaths Array of arrays of gPaths values. To each path corresponds an arrays of gPath values.
     * @param actualPaths    Computed arrays of {@link PropagationPath}.
     */
    private static void assertPaths(double[][][] expectedPts, double[][] expectedGPaths, List<PropagationPath> actualPaths) {
        assertEquals("Expected path count is different than actual path count.", expectedPts.length, actualPaths.size());
        for(int i=0; i<expectedPts.length; i++) {
            PropagationPath path = actualPaths.get(i);
            for(int j=0; j<expectedPts[i].length; j++){
                PointPath point = path.getPointList().get(j);
                assertEquals("Path "+i+" point "+j+" coord X", expectedPts[i][j][0], point.coordinate.x, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Y", expectedPts[i][j][1], point.coordinate.y, DELTA_COORDS);
            }
            assertEquals("Expected path["+i+"] segments count is different than actual path segment count.", expectedGPaths[i].length, path.getSegmentList().size());
            for(int j=0; j<expectedGPaths[i].length; j++) {
                assertEquals("Path " + i + " g path " + j, expectedGPaths[i][j], path.getSegmentList().get(j).gPath, DELTA_G_PATH);
            }
        }
    }


    /**
     * Assertions for a list of {@link PropagationPath}.
     * @param expectedPts    Array of arrays of array of expected coordinates (xyz) of points of paths. To each path
     *                       corresponds an array of points. To each point corresponds an array of coordinates (xyz).
     * @param actualPaths    Computed arrays of {@link PropagationPath}.
     */
    private static void assertPaths(double[][][] expectedPts, List<PropagationPath> actualPaths) {
        assertEquals("Expected path count is different than actual path count.", expectedPts.length, actualPaths.size());
        for(int i=0; i<expectedPts.length; i++) {
            PropagationPath path = actualPaths.get(i);
            for(int j=0; j<expectedPts[i].length; j++){
                PointPath point = path.getPointList().get(j);
                assertEquals("Path "+i+" point "+j+" coord X", expectedPts[i][j][0], point.coordinate.x, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Y", expectedPts[i][j][1], point.coordinate.y, DELTA_COORDS);
            }
        }
    }
    private static void assertPlanes(double[][] expectedPlanes, List<SegmentPath> segments) {
        assertPlanes(expectedPlanes, segments.toArray(new SegmentPath[0]));
    }

    private static void assertPlanes(double[][] expectedPlanes, SegmentPath... segments) {
        assertEquals("Expected segments count is different than actual path count.", expectedPlanes.length, segments.length);
        for(int i=0; i<expectedPlanes.length; i++) {
            SegmentPath segment = segments[i];
            assertEquals("a", expectedPlanes[i][0], segment.a, DELTA_PLANES);
            assertEquals("b", expectedPlanes[i][1], segment.b, DELTA_PLANES);
            assertEquals("zs", expectedPlanes[i][2], segment.zsH, DELTA_PLANES);
            assertEquals("zr", expectedPlanes[i][3], segment.zrH, DELTA_PLANES);
            assertEquals("dp", expectedPlanes[i][4], segment.dp, DELTA_PLANES);
            assertEquals("gPath", expectedPlanes[i][5], segment.gPath, DELTA_PLANES);
            if(!Double.isNaN(expectedPlanes[i][6])) {
                assertEquals("gPrimePath", expectedPlanes[i][6], segment.gPathPrime, DELTA_PLANES);
            }
        }
    }

}
