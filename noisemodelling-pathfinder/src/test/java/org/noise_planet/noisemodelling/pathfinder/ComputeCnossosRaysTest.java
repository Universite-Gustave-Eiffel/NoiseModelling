package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Arrays;
import java.util.List;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

public class ComputeCnossosRaysTest {

    /**
     *  Error for coordinates
     */
    private static final double DELTA_COORDS = 0.1;

    /**
     *  Error for planes values
     */
    private static final double DELTA_PLANES = 0.1;

    /**
     *  Error for G path value
     */
    private static final double DELTA_G_PATH = 0.02;

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
                {{0.00, 1.00}, {180.00, 3.44}, {221.23, 4.0}},//Path 2 : right side
                {{0.00, 1.00}, {169.78, 3.61}, {194.78, 4.0}},//Path 3 : left side
        };
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 6.00, 170.49, 0.55, 0.61},
                {0.00, 0.00, 6.00, 4.00, 023.68, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 221.23, 0.46, 0.46}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 194.78, 0.51, 0.51}
        };

        //Assertion
        //assertPaths(pts, propDataOut.getPropagationPaths());
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
                {{0.00, 1.00}, {180.00, 11.58}, {221.23, 14.0}}, //Path 3 : right side
                {{0.00, 1.00}, {169.78, 12.33}, {194.78, 14.0}}  //Path 2 : left side
        };
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.04, -1.96, 2.96, 11.68, 170.98, 0.55, 0.76},
                {0.04, 1.94, 7.36, 3.71, 23.54, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.06, -3.10, 4.09, 3.77, 221.62, 0.46, 0.49}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.05, -2.82, 3.81, 6.23, 195.20, 0.51, 0.64}
        };

        //Assertion
        //assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at low height
     */
    //@Test
    public void TC10() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10),
                })
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
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
                {{0.00, 1.00}, {5.0, 10.0}, {15.0, 10.0}, {20.0, 4.0}},    //Path 1 : direct
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}, //Path 2 : right side
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}  //Path 3 : left side
        };
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs     zr    dp    Gp   Gp'
                {0.00, 0.00, 1.00, 10.00, 5.00, 0.50, 0.50},
                {0.00, 0.00, 10.00, 4.00, 5.00, 0.50,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr     dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 24.15, 0.50, 0.50}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr     dp    Gp   Gp'
                {0.00, 0.00, 1.00, 4.00, 24.15, 0.50, 0.50}
        };

        //Assertion
        //assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at low height
     */
   // @Test
    public void TC11() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10),
                })
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 15)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        /*double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {5.0, 10.0}, {15.0, 10.0}, {20.0, 4.0}},    //Path 1 : direct
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}, //Path 2 : right side
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}  //Path 3 : left side
        };*/
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs     zr    dp    Gp   Gp'
                {0.00, 0.00, 1.00, 10.00, 5.00, 0.50, 0.50},
                {-0.89, 17.78, 2.49, 11.21, 7.89, 0.17,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs     zr     dp    Gp    Gp'
                {0.10, -0.13, 1.13, 12.59, 24.98, 0.44, 0.50}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a      b    zs     zr     dp    Gp    Gp'
                {0.10, -0.13, 1.13, 12.59, 24.98, 0.44, 0.50}
        };

        //Assertion
        //assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal object – receiver at low height
     */
   // @Test
    public void TC12() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(11.0, 15.5, 10),
                        new Coordinate(12.0, 13.0, 10),
                        new Coordinate(14.5, 12.0, 10),
                        new Coordinate(17.0, 13.0, 10),
                        new Coordinate(18.0, 15.5, 10),
                        new Coordinate(17.0, 18.0, 10),
                        new Coordinate(14.5, 19.0, 10),
                        new Coordinate(12.0, 18.0, 10),
                })
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 10, 1)
                .addReceiver(30, 20, 6)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        /*double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {5.0, 10.0}, {15.0, 10.0}, {20.0, 4.0}},    //Path 1 : direct
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}, //Path 2 : right side
                {{0.00, 1.00}, {7.07, 1.88}, {17.07, 3.12}, {24.14, 4.0}}  //Path 3 : left side
        };*/
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr     dp    Gp   Gp'
                {0.00, 0.00, 1.00, 10.0, 12.26, 0.50, 0.50},
                {0.00, 0.00, 10.0, 6.00, 12.80, 0.50,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b    zs    zr     dp    Gp    Gp'
                {0.00, 0.00, 1.00, 6.00, 32.11, 0.50, 0.50}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr     dp    Gp    Gp'
                {0.00, 0.00, 1.00, 6.00, 32.66, 0.50, 0.50}
        };

        //Assertion
        //assertPaths(pts, propDataOut.getPropagationPaths());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal object
     */
 //   @Test
    public void TC13() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(169.4, 41.0, 30),
                        new Coordinate(172.5, 33.5, 30),
                        new Coordinate(180.0, 30.4, 30),
                        new Coordinate(187.5, 33.5, 30),
                        new Coordinate(190.6, 41.0, 30),
                        new Coordinate(187.5, 48.5, 30),
                        new Coordinate(180.0, 51.6, 30),
                        new Coordinate(172.5, 48.5, 30),
                })
                .addGroundEffect(0, 50, -20, 80, 0.5)
                .addGroundEffect(50, 150, -20, 80, 0.9)
                .addGroundEffect(150, 225, -20, 80, 0.2)
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
                .addReceiver(200, 50, 28.5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a      b    zs     zr      dp    Gp   Gp'
                {0.04, -1.68, 2.68, 25.86, 164.99, 0.71, 0.54},
                {0.00, 10.00, 20.0, 18.50, 12.33, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs     zr      dp    Gp    Gp'
                {0.06, -2.99, 3.98, 19.83, 201.30, 0.61, 0.53}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a      b    zs     zr      dp    Gp    Gp'
                {0.05, -2.82, 3.82, 20.69, 196.29, 0.63, 0.54}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building – receiver at large height
     */
   // @Test
    public void TC14() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(11.0, 15.5, 10),
                        new Coordinate(12.0, 13.0, 10),
                        new Coordinate(14.5, 12.0, 10),
                        new Coordinate(17.0, 13.0, 10),
                        new Coordinate(18.0, 15.5, 10),
                        new Coordinate(17.0, 18.0, 10),
                        new Coordinate(14.5, 19.0, 10),
                        new Coordinate(12.0, 18.0, 10),
                })
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(8, 10, 1)
                .addReceiver(25, 20, 23)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.2)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a       b    zs     zr    dp    Gp   Gp'
                {0.00,   0.00, 1.00, 10.00, 5.39, 0.20, 0.20},
                {-1.02, 17.11, 0.00, 18.23, 0.72, 0.11,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs     zr     dp    Gp    Gp'
                {-0.02, 1.13, 0.00, 22.32, 19.57, 0.18, 0.20}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs     zr      dp    Gp    Gp'
                {0.00, 1.35, 0.00, 21.69, 22.08, 0.17, 0.20}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
 //   @Test
    public void TC15() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55.0, 5.0, 8),
                        new Coordinate(65.0, 5.0, 8),
                        new Coordinate(65.0, 15.0, 8),
                        new Coordinate(55.0, 15.0, 8),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(70.0, 14.5, 12),
                        new Coordinate(80.0, 10.2, 12),
                        new Coordinate(80.0, 20.2, 12),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(90.1, 19.5, 10),
                        new Coordinate(93.3, 17.8, 10),
                        new Coordinate(87.3, 6.6, 10),
                        new Coordinate(84.1, 8.3, 10),
                })
                /*.addBuilding(new Coordinate[]{
                        new Coordinate(94.9, 14.1, 10),
                        new Coordinate(98.02, 12.3, 10),
                        new Coordinate(92.03, 1.2, 10),
                        new Coordinate(88.86, 2.9, 10),
                })*/
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(100, 15, 5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr    dp    Gp   Gp'
                {0.00, 0.00,  1.00, 8.00, 5.02, 0.50, 0.50},
                {0.00, 0.00, 10.00, 5.00, 8.73, 0.50,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs    zr     dp    Gp    Gp'
                {0.08, -1.19, 2.18, 2.01, 54.80, 0.46, 0.48}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs    zr     dp    Gp    Gp'
                {0.00, 0.00, 1.00, 5.00, 53.60, 0.50, 0.50}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC16 -- Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() {
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

                .addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 194.59, 0.54, 0.64}
        };

        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.80, 3.80, 6.37, 198.45, 0.51, 0.65}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSRSegment());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties reduced receiver height
     *
     * No data provided usable for testing.
     */
    //TODO : no data provided in the document for this test.
  //  @Test
    public void TC17() {
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

                .addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 194.59, 0.54, 0.64}
        };

        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.80, 3.80, 6.37, 198.45, 0.51, 0.65}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSRSegment());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
    }

    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    //TODO : not tested
    //@Test
    public void TC18() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15),
                }, -1)
                .addWall(new Coordinate[]{
                        new Coordinate(87, 50, 12),
                        new Coordinate(92, 32, 12),
                }, -1)
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
                .addReceiver(200, 50, 12)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
    }

    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
  //  @Test
    public void TC19() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(100, 24, 12),
                        new Coordinate(118, 24, 12),
                        new Coordinate(118, 30, 12),
                        new Coordinate(100, 30, 12),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(110, 15, 7),
                        new Coordinate(118, 15, 7),
                        new Coordinate(118, 24, 7),
                        new Coordinate(110, 24, 7),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(100, 9, 12),
                        new Coordinate(118, 9, 12),
                        new Coordinate(118, 15, 12),
                        new Coordinate(100, 15, 12),
                })
                .addWall(new Coordinate[]{
                        new Coordinate(156.00, 28.00, 14),
                        new Coordinate(145.00, 7.00, 14),
                }, -1)
                .addWall(new Coordinate[]{
                        new Coordinate(175.00, 35.00, 14.5),
                        new Coordinate(188.00, 19.00, 14.5),
                }, -1)
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
                .addReceiver(200, 30, 14)
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
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a      b     zs     zr      dp    Gp   Gp'
                {0.03, -1.09,  2.09, 10.89, 145.65, 0.57, 0.78},
                {0.02,  6.42,  4.76,  3.89,  19.38, 0.20,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs    zr      dp    Gp    Gp'
                {0.06, -2.92, 3.92, 5.66, 196.38, 0.50, 0.62}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a      b    zs    zr      dp    Gp    Gp'
                {0.06, -2.01, 3.00, 5.00, 192.81, 0.46, 0.55}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC20() {
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
                .addReceiver(200, 25, 14)
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
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 191.02, 0.54, 0.64}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */
   // @Test
    public void TC21()  {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(167.2, 39.5, 11.5),
                        new Coordinate(151.6, 48.5, 11.5),
                        new Coordinate(141.1, 30.3, 11.5),
                        new Coordinate(156.7, 21.3, 11.5),
                        new Coordinate(159.7, 26.5, 11.5),
                        new Coordinate(151.0, 31.5, 11.5),
                        new Coordinate(155.5, 39.3, 11.5),
                        new Coordinate(164.2, 34.3, 11.5)
                })
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
                .addReceiver(200, 25, 14)
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
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.02, -1.04, 2.04, 9.07, 146.96, 0.60, 0.77},
                {0.10, -8.64, 5.10, 3.12, 43.87, 0.20, NaN}
        };

        //Assertion
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertEquals(3, propDataOut.getPropagationPaths().size());
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
        SegmentPath segment = segments[0];
        assertEquals("a", expectedPlanes[0][0], segment.a, DELTA_PLANES);
        assertEquals("b", expectedPlanes[0][1], segment.b, DELTA_PLANES);
        assertEquals("zs", expectedPlanes[0][2], segment.zsH, DELTA_PLANES);
        assertEquals("zr", expectedPlanes[0][3], segment.zrH, DELTA_PLANES);
        assertEquals("dp", expectedPlanes[0][4], segment.dp, DELTA_PLANES);
        assertEquals("gPath", expectedPlanes[0][5], segment.gPath, DELTA_PLANES);
        if(!Double.isNaN(expectedPlanes[0][6])) {
            assertEquals("gPrimePath", expectedPlanes[0][6], segment.gPathPrime, DELTA_PLANES);
        }

        if(segments.length>1) {
            segment = segments[segments.length - 1];
            assertEquals("a", expectedPlanes[1][0], segment.a, DELTA_PLANES);
            assertEquals("b", expectedPlanes[1][1], segment.b, DELTA_PLANES);
            assertEquals("zs", expectedPlanes[1][2], segment.zsH, DELTA_PLANES);
            assertEquals("zr", expectedPlanes[1][3], segment.zrH, DELTA_PLANES);
            assertEquals("dp", expectedPlanes[1][4], segment.dp, DELTA_PLANES);
            assertEquals("gPath", expectedPlanes[1][5], segment.gPath, DELTA_PLANES);
            if (!Double.isNaN(expectedPlanes[1][6])) {
                assertEquals("gPrimePath", expectedPlanes[1][6], segment.gPathPrime, DELTA_PLANES);
            }
        }
    }

}
