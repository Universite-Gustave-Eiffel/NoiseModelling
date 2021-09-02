package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ComputeCnossosRaysTest {

    //Error for coordinates
    private static final double DELTA_COORDS = 1e-8;

    //Error for G path value
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
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
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
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
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
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
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
                .horizontalDiff(true)
                .verticalDiff(true)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
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
                .addTopographicLine(205, -5, 10, 205, 75, 0)
                .addTopographicLine(205, 75, 0, 185, 75, 0)
                .addTopographicLine(185, 75, 0, 185, -5, 0)
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
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
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
                .addTopographicLine(205, -5, 10, 205, 75, 0)
                .addTopographicLine(205, 75, 0, 185, 75, 0)
                .addTopographicLine(185, 75, 0, 185, -5, 0)
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
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
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
                .addWalls(new Coordinate[]{
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
                .build();

        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {176.58, 45.07, 6.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.55,0.2} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
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
            for(int j=0; j<expectedPts.length; j++){
                PointPath point = path.getPointList().get(j);
                assertEquals("Path "+i+" point "+j+" coord X", expectedPts[i][j][0], point.coordinate.x, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Y", expectedPts[i][j][1], point.coordinate.y, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Z", expectedPts[i][j][2], point.coordinate.z, DELTA_COORDS);
            }
            assertEquals("Expected path["+i+"] segments count is different than actual path segment count.", expectedGPaths[i].length, path.getSegmentList().size());
            for(int j=0; j<expectedGPaths[i].length; j++) {
                assertEquals("Path " + i + " g path " + j, expectedGPaths[i][j], path.getSegmentList().get(j).gPath, DELTA_G_PATH);
            }



        }
    }


}
