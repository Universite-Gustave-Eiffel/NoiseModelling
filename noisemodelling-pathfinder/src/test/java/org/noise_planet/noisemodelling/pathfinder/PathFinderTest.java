/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.junit.After;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.math.Plane3D;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.operation.distance3d.PlanarPolygon3D;
import org.noise_planet.noisemodelling.pathfinder.cnossos.CnossosPath;
//import org.noise_planet.noisemodelling.pathfinder.path.CnossosPathParameters;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.path.PointPath;
import org.noise_planet.noisemodelling.pathfinder.path.SegmentPath;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilderDecorator;
import org.noise_planet.noisemodelling.pathfinder.utils.documents.KMLDocument;

import javax.xml.stream.XMLStreamException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.NaN;
import static org.junit.Assert.assertEquals;

public class PathFinderTest {

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

    @After

    /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder().finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.0)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
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
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
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
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(1.0)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
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
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.2)
                .vEdgeDiff(true)
                .hEdgeDiff(true)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
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


    public static void addGroundAttenuationTC5(ProfileBuilder profileBuilder) {
        profileBuilder
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2);
    }

    public static void addTopographicTC5Model(ProfileBuilder profileBuilder) {
        profileBuilder
                // top horizontal line
                .addTopographicLine(0, 80, 0, 120, 80, 0)
                .addTopographicLine(120, 80, 0, 225, 80, 0)
                // bottom horizontal line
                .addTopographicLine(225, -20, 0, 120, -20, 0)
                .addTopographicLine(120, -20, 0, 0, -20, 0)
                // right vertical line
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                // left vertical line
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                // center vertical line
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                // elevated rectangle
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10);

        // ramp connection
        profileBuilder.addTopographicLine(120, 80, 0, 185, 75, 10)
                .addTopographicLine(120, -20, 0, 185, -5, 10)
                .addTopographicLine(205, 75, 10, 225, 80, 0)
                .addTopographicLine(205, -5, 10, 225, -20, 0);
    }


    public static void addTopographicTC23Model(ProfileBuilder profileBuilder) {
        // Create parallel lines for the slope edge because unit test table values are rounded and
        // the rounding make the lines non-parallel

        // base line left
        Vector3D v1 = new Vector3D(new Coordinate(46.27, 36.28, 0),
                new Coordinate(59.6, -9.87, 0));

        // original top segment (left side)
        Vector3D v2 = new Vector3D(new Coordinate(54.68, 37.59, 5),
                new Coordinate(67.35, -6.83, 5));


        // base line right
        Vector3D v3 = new Vector3D(new Coordinate(63.71, 41.16, 0),
                new Coordinate(76.84, -5.28, 0));

        Vector3D parallelPoint1 = new Vector3D(new Coordinate(54.68, 37.59, 5)).add(v1.normalize().divide(1/v2.length()));
        Vector3D parallelPoint2 = new Vector3D(new Coordinate(55.93, 37.93, 5)).add(v3.normalize().divide(1/v2.length()));


        profileBuilder.addTopographicLine(30, -14, 0, 122, -14, 0)// 1
                .addTopographicLine(122, -14, 0, 122, 45, 0)// 2
                .addTopographicLine(122, 45, 0, 30, 45, 0)// 3
                .addTopographicLine(30, 45, 0, 30, -14, 0)// 4
                .addTopographicLine(59.6, -9.87, 0, 76.84, -5.28, 0)// 5
                .addTopographicLine(76.84, -5.28, 0, 63.71, 41.16, 0)// 6
                .addTopographicLine(63.71, 41.16, 0, 46.27, 36.28, 0)// 7
                .addTopographicLine(46.27, 36.28, 0, 59.6, -9.87, 0)// 8
                .addTopographicLine(46.27, 36.28, 0, 54.68, 37.59, 5)// 9
                .addTopographicLine(54.68, 37.59, 5, parallelPoint2.getX(), parallelPoint2.getY(), 5)// 10
                .addTopographicLine(55.93, 37.93, 5, 63.71, 41.16, 0)// 11
                .addTopographicLine(59.6, -9.87, 0, parallelPoint1.getX(), parallelPoint1.getY(), 5)// 12
                .addTopographicLine(parallelPoint1.getX(), parallelPoint1.getY(), 5, parallelPoint2.getX(), parallelPoint2.getY(), 5)// 13
                .addTopographicLine(parallelPoint2.getX(), parallelPoint2.getY(), 5, 76.84, -5.28, 0)// 14
                .addTopographicLine(54.68, 37.59, 5, parallelPoint1.getX(), parallelPoint1.getY(), 5)// 15
                .addTopographicLine(55.93, 37.93, 5, parallelPoint2.getX(), parallelPoint2.getY(), 5); // 16
    }

    /**
     * Test TC05 -- Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC05() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .setGs(0.9)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {194.16, 14.00}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.51},{0.64}
                //{(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };
        /* Table 18 */
        double [][] meanPlanes = new double[][]{
                //  a      b    zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 194.59, 0.51, 0.64}
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths()); // table17
        assertPlanes(meanPlanes, propDataOut.getPropagationPaths().get(0).getSRSegment()); // table 18
        assertPlanes(meanPlanes, propDataOut.getPropagationPaths().get(0).getSegmentList()); // table 18
    }

    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .setGs(0.9)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertEquals(1, propDataOut.getPropagationPaths().size());
        assertEquals(2, propDataOut.getPropagationPaths().get(0).getSegmentList().size());

        // Test R-CRIT table 27
        Coordinate D = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).d + propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).d - propDataOut.getPropagationPaths().get(0).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }
        //computeRays.
        //Expected values
        double[][][] pts = new double[][][]{
                {{0.00, 1.00}, {178.84, 10.0}, {194.16, 11.5}} //Path 1 : direct
        };

        /* Table 23 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(112.41, 0.00));
        expectedZ_profile.add(new Coordinate(178.84, 10.00));
        expectedZ_profile.add(new Coordinate(194.16, 10.00));

        /* Table 25 */
        Coordinate expectedSPrime =new Coordinate(0.31,-5.65);
        Coordinate expectedRPrime =new Coordinate(194.16,8.5);

        if(!profileBuilder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSRSegment().sPrime,propDataOut.getPropagationPaths().get(0).getSRSegment().rPrime);
        }


        /* Table 24 */
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
        assertZProfil(expectedZ_profile, propDataOut.getPropagationPaths().get(0).getCutProfile().computePts2DGround());
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
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.9)
                .hEdgeDiff(true)
                .vEdgeDiff(false)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 33 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(170.23, 0.00));
        expectedZ_profile.add(new Coordinate(170.23, 6.00));
        expectedZ_profile.add(new Coordinate(170.23, 0.00));
        expectedZ_profile.add(new Coordinate(194.16, 0.00));

        /* Table 34 */
        Coordinate expectedSPrime =new Coordinate(0.00,-1.00);
        Coordinate expectedRPrime =new Coordinate(194.16,-4.00);
        if(!profileBuilder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(propDataOut.getPropagationPaths().get(0).getSegmentList().size()-1).rPrime);
        }


        double[][] gPaths = new double[][]{
                {0.55, 0.20},{0.61,  NaN} //Path 1 : direct
        };

        /* Table 35 */
        double [][] segmentsMeanPlanes = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.00, 0.00, 1.00, 6.00, 170.23, 0.55, 0.61},
                {0.00, 0.00, 6.00, 4.00, 023.93, 0.20,  NaN}
        };


        //Assertion
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes, propDataOut.getPropagationPaths().get(0).getSegmentList());
        try {
            exportScene("target/T07.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.9)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);


        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();

        //Expected values

        /*Table 41 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(170.49, 0.00));
        expectedZ_profile.add(new Coordinate(170.49, 6.00));
        expectedZ_profile.add(new Coordinate(170.49, 0.00));
        expectedZ_profile.add(new Coordinate(194.16, 0.00));

        /* Table 42 */
        Coordinate expectedSPrime =new Coordinate(0.00,-1.00);
        Coordinate expectedRPrime =new Coordinate(194.16,-4.00);
        if(!profileBuilder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(propDataOut.getPropagationPaths().get(0).getSegmentList().size()-1).rPrime);
        }

        /* Table 43 */
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

        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
        try {
            exportScene("target/T08.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short barrier
     */
    @Test
    public void TC09() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);
        // add wall
        profileBuilder.addWall(new Coordinate[]{
                                new Coordinate(175, 50, 17),
                                new Coordinate(190, 10, 14)},
                        1)

                //.setzBuildings(true)
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();

        //Expected values

        /* Table 59 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(112.41, 0.00));
        expectedZ_profile.add(new Coordinate(170.49, 8.74));
        expectedZ_profile.add(new Coordinate(170.49, 16.63));
        expectedZ_profile.add(new Coordinate(170.49, 8.74));
        expectedZ_profile.add(new Coordinate(178.84, 10.00));
        expectedZ_profile.add(new Coordinate(194.16, 10.00));

        /* Table 61 */
        Coordinate expectedSPrime =new Coordinate(0.24,-4.92);
        Coordinate expectedRPrime =new Coordinate(194.48,6.59);
        if(!profileBuilder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(propDataOut.getPropagationPaths().get(0).getSegmentList().size()-1).rPrime);
        }

        /* Table 60 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b    zs    zr      dp    Gp   Gp'
                {0.04, -1.96, 2.96, 11.68, 170.98, 0.55, 0.76},
                {0.04, 1.94, 7.36, 3.71, 23.54, 0.20,  0.20}
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
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
        try {
            exportScene("target/T09.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at low height
     */
    @Test
    public void TC10() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10),
                });

        profileBuilder.addGroundEffect(0.0, 100.0, 0.0, 100.0, 0.5);

        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 74 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(5, 0.00));
        expectedZ_profile.add(new Coordinate(5, 10.00));
        expectedZ_profile.add(new Coordinate(15, 10));
        expectedZ_profile.add(new Coordinate(15, 0));
        expectedZ_profile.add(new Coordinate(20, 0));

        /* Table 75 */
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

        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
        try {
            exportScene("target/T10.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at low height
     */
    @Test
    public void TC11() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10),
                });
        profileBuilder.addGroundEffect(0.0, 100.0, 0.0, 100.0, 0.5);

        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 15)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 85 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(5, 0.00));
        expectedZ_profile.add(new Coordinate(5, 10.00));
        expectedZ_profile.add(new Coordinate(15, 10.00));
        expectedZ_profile.add(new Coordinate(15, 0));
        expectedZ_profile.add(new Coordinate(20, 0));

        /* Table 86 */
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
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal object – receiver at low height
     */
    @Test
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
                });

        profileBuilder.addGroundEffect(0.0, 50, 0.0, 50, 0.5);

        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(0, 10, 1)
                .addReceiver(30, 20, 6)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);


        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 100 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(12.26, 0.00));
        expectedZ_profile.add(new Coordinate(12.26, 10.00));
        expectedZ_profile.add(new Coordinate(18.82, 10));
        expectedZ_profile.add(new Coordinate(18.82, 0));
        expectedZ_profile.add(new Coordinate(31.62, 0));

        /* Table 101 */
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
        assertEquals(3, propDataOut.getPropagationPaths().size());

        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());

        assertEquals(3, propDataOut.getPropagationPaths().get(0).getSegmentList().size());
        Coordinate sPrime = propDataOut.pathParameters.get(0).getSegmentList().get(0).sPrime;
        Coordinate rPrime = propDataOut.pathParameters.get(0).getSegmentList().get(2).rPrime;

        assertCoordinateEquals("TC12 Table 102 S' S->O", new Coordinate(0, -1), sPrime, DELTA_COORDS);
        assertCoordinateEquals("TC12 Table 102 R' O->R", new Coordinate(31.62, -6), rPrime, DELTA_COORDS);
    }

    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal object
     */
    @Test
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
                });
        profileBuilder.addGroundEffect(0, 50, -20, 80, 0.5)
                .addGroundEffect(50, 150, -20, 80, 0.9)
                .addGroundEffect(150, 225, -20, 80, 0.2);
        addTopographicTC5Model(profileBuilder);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 28.5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();

        //Expected values

        /* Table 117 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(112.41, 0.00));
        expectedZ_profile.add(new Coordinate(164.07, 7.8));
        expectedZ_profile.add(new Coordinate(164.07, 30.00));
        expectedZ_profile.add(new Coordinate(181.83, 30));
        expectedZ_profile.add(new Coordinate(181.83, 10));
        expectedZ_profile.add(new Coordinate(194.16, 10));

        /* Table 118 */
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
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building – receiver at large height
     * Wrong value of z1 in Cnossos document for the 3 paths
     */
    @Test
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
                });
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(8, 10, 1)
                .addReceiver(25, 20, 23)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.2)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);


        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 132 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(5.39, 0.00));
        expectedZ_profile.add(new Coordinate(5.39, 10.00));
        expectedZ_profile.add(new Coordinate(11.49, 10.0));
        expectedZ_profile.add(new Coordinate(11.49, 0.0));
        expectedZ_profile.add(new Coordinate(19.72, 0));

        /* Table 133 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a       b    zs     zr    dp    Gp   Gp'
                {0.00,   0.00, 1.00, 10.00, 5.39, 0.20, 0.20},
                {-1.02, 17.11, 1.08, 18.23, 0.72, 0.11,  NaN} // Fix Cnossos document Zs is 1.08 not 0
        };
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs     zr     dp    Gp    Gp'
                {-0.02, 1.13, 0.10, 22.32, 19.57, 0.18, 0.20} // Fix Cnossos document Zs is 0.1 not 0
        };
        double [][] segmentsMeanPlanes2 = new double[][]{
                //  a     b    zs     zr      dp    Gp    Gp'
                {0.00, 1.35, 0.32, 21.69, 22.08, 0.17, 0.20} // Fix Cnossos document Zs is 0.32 not 0
        };


        //Assertion
        // Wrong value of z1 in Cnossos document for the 3 paths
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment());
    }

    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     * right : error in value of b cnossos table 149 right path
     */
    @Test
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
                });
        profileBuilder.addGroundEffect(0, 100, 0.0, 150, 0.5);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();


        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(100, 15, 5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 148 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(5.02, 0.00));
        expectedZ_profile.add(new Coordinate(5.02, 8.00));
        expectedZ_profile.add(new Coordinate(15.07, 8.0));
        expectedZ_profile.add(new Coordinate(15.08, 0.0));
        expectedZ_profile.add(new Coordinate(24.81, 0.0));
        expectedZ_profile.add(new Coordinate(24.81, 12.0));
        expectedZ_profile.add(new Coordinate(30.15, 12.00));
        expectedZ_profile.add(new Coordinate(30.15, 0.00));
        expectedZ_profile.add(new Coordinate(37.19, 0.0));
        expectedZ_profile.add(new Coordinate(37.19, 10.0));
        expectedZ_profile.add(new Coordinate(41.52, 10.0));
        expectedZ_profile.add(new Coordinate(41.52, 0.0));
        expectedZ_profile.add(new Coordinate(50.25, 0.0));

        /* Table 149 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr    dp    Gp   Gp'
                {0.00, 0.00,  1.00, 8.00, 5.02, 0.50, 0.50},
                {0.00, 0.00, 10.00, 5.00, 8.73, 0.50,  NaN}
        };
        double [][] segmentsMeanPlanes1 = new double[][]{ // right
                //  a      b    zs    zr     dp    Gp    Gp'
                {0.08, -1.19, 2.18, 2.01, 54.80, 0.46, 0.48}
        };
        double [][] segmentsMeanPlanes2 = new double[][]{ // left
                //  a     b    zs    zr     dp    Gp    Gp'
                {0.00, 0.00, 1.00, 5.00, 53.60, 0.50, 0.50}
        };


        //Assertion
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes2, propDataOut.getPropagationPaths().get(2).getSRSegment()); // left
        //assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment()); // right : error in value of b cnossos

        //exportRays("target/T06.geojson", propDataOut);
        try {
            exportScene("target/T15.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test TC16 -- Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);

        profileBuilder.addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .setGs(0.9)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);


        //Expected values

        /* Table 163 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(112.41, 0.0));
        expectedZ_profile.add(new Coordinate(178.84, 10));
        expectedZ_profile.add(new Coordinate(194.16, 10));

        /* Table 169 */
        List<Coordinate> expectedZProfileReflection = new ArrayList<>();
        expectedZProfileReflection.add(new Coordinate(0.0, 0.0));
        expectedZProfileReflection.add(new Coordinate(117.12, 0.0));
        expectedZProfileReflection.add(new Coordinate(129.75, 1.82));
        expectedZProfileReflection.add(new Coordinate(129.75, 1.82));
        expectedZProfileReflection.add(new Coordinate(129.75, 1.82));
        expectedZProfileReflection.add(new Coordinate(183.01, 10));
        expectedZProfileReflection.add(new Coordinate(198.04, 10));

        /* Table 166 */
        Coordinate expectedSPrime =new Coordinate(0.42,-6.64);
        Coordinate expectedRPrime =new Coordinate(194.84,1.70);
        if(!profileBuilder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSRSegment().sPrime,propDataOut.getPropagationPaths().get(0).getSRSegment().rPrime);
        }

        /* Table 165 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.83, 3.83, 6.16, 194.59, 0.54, 0.64}
        };

        /* Table 171 */
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.05, -2.80, 3.80, 6.37, 198.45, 0.51, 0.65}
        };

        //Assertion

        // Check SR direct line
        List<Coordinate> result = propDataOut.getPropagationPaths().get(0).getCutProfile().computePts2DGround();
        assertZProfil(expectedZ_profile,result);
        assertEquals(2, propDataOut.getPropagationPaths().size());
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSRSegment());

        // Check reflection path
        result = propDataOut.getPropagationPaths().get(1).getCutProfile().computePts2DGround();
        assertZProfil(expectedZProfileReflection, result);
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());

        try {
            exportScene("target/T16.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties reduced receiver height
     *
     * No data provided usable for testing.
     */
    //TODO : no data provided in the document for this test.
    @Test
    public void TC17() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();

        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);

        profileBuilder.addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Expected Values

        /* Table 178 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(112.41, 0.0));
        expectedZ_profile.add(new Coordinate(178.84, 10));
        expectedZ_profile.add(new Coordinate(194.16, 10));

        //Assertion
        assertZProfil(expectedZ_profile,result);

        // Test R-CRIT table 179
        Coordinate D = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).d + propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).d - propDataOut.getPropagationPaths().get(0).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(-deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }


        // Test R-CRIT table 184
        /*Coordinate D = propDataOut.getPropagationPaths().get(1).getSegmentList().get(0).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(1).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(1).getSRSegment().rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(1).getSegmentList().get(0).d + D.distance(propDataOut.getPropagationPaths().get(1).getPointList().get(3).coordinate) - propDataOut.getPropagationPaths().get(1).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }*/

    }

    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     * Error: Strange rays On -> R
     */

    @Test
    public void TC18() {
        //Profile building
        ProfileBuilder builder = new ProfileBuilder();
        addGroundAttenuationTC5(builder);
        addTopographicTC5Model(builder);
                // Add building
        builder.addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)}, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), 1)

                .addWall(new Coordinate[]{
                        new Coordinate(87, 50,12),
                        new Coordinate(92, 32,12)}, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), 2)
                //.setzBuildings(true)
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 12)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        // Expected Values

        /* Table 193  Z Profile SR */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(112.41, 0.0));
        expectedZ_profile.add(new Coordinate(178.84, 10));
        expectedZ_profile.add(new Coordinate(194.16, 10));

        CutProfile cutProfile = propDataOut.getPropagationPaths().get(0).getCutProfile();
        List<Coordinate> result = cutProfile.computePts2DGround();
        assertZProfil(expectedZ_profile, result);


        /* Table 194 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a      b    zs    zr      dp    Gp    Gp'
                {0.05, -2.83, 3.83, 4.16, 194.48, 0.51, 0.58}
        };

        /* Table 197 */
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a      b    zs    zr      dp    Gp    Gp'
                {0.0, 0.0, 1.0, 12.0, 85.16, 0.7, 0.86},
                {0.11, -12.03, 14.16, 1.29, 112.14, 0.37,  NaN}
        };




        // S-R (not the rayleigh segments SO OR)
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSRSegment());
        // Check reflexion mean planes
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSegmentList());
        try {
            exportScene("target/T18.kml", builder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties:
     * erreur Cnossos: left path -> gPath table 207
     */
    @Test
    public void TC19() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        addTopographicTC5Model(profileBuilder);
        addGroundAttenuationTC5(profileBuilder);

        profileBuilder.addBuilding(new Coordinate[]{
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
                .setzBuildings(true)
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 30, 14)
                .hEdgeDiff(true)
                //.vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;


        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 208 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.00, 0.00));
        expectedZ_profile.add(new Coordinate(100.55, 0.00));
        expectedZ_profile.add(new Coordinate(100.55, 7.00));
        expectedZ_profile.add(new Coordinate(108.60, 7.00));
        expectedZ_profile.add(new Coordinate(108.60, 0.0));
        expectedZ_profile.add(new Coordinate(110.61, 0.0));
        expectedZ_profile.add(new Coordinate(145.34, 5.31));
        expectedZ_profile.add(new Coordinate(145.34, 14.00));
        expectedZ_profile.add(new Coordinate(145.34, 5.31));
        expectedZ_profile.add(new Coordinate(171.65, 9.34));
        expectedZ_profile.add(new Coordinate(171.66, 14.50));
        expectedZ_profile.add(new Coordinate(171.66, 9.34));
        expectedZ_profile.add(new Coordinate(175.97, 10));
        expectedZ_profile.add(new Coordinate(191.05, 10));

        /* Table 209 */
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
        assertZProfil(expectedZ_profile,result);
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
                .addTopographicLine(185, 75, 10, 185, -5, 10);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 25, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        //Expected values

        /* Table 221 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(110.34, 0.0));
        expectedZ_profile.add(new Coordinate(175.54, 10));
        expectedZ_profile.add(new Coordinate(190.59, 10));

        /* Table 230 S -> R TC21 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.06, -2.84, 3.84, 6.12, 191.02, 0.50, 0.65}
        };

        //Assertion
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     * problème ISO
     */
    @Test
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
                .addTopographicLine(185, 75, 10, 185, -5, 10);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 25, 14)
                .hEdgeDiff(true)
                //.vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=0;

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Test R-CRIT table 235
        Coordinate D = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).d + D.distance(propDataOut.getPropagationPaths().get(0).getPointList().get(2).coordinate) - propDataOut.getPropagationPaths().get(0).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }
        //Expected values

        /* Table 228 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(110.34, 0.0));
        expectedZ_profile.add(new Coordinate(146.75, 5.58));
        expectedZ_profile.add(new Coordinate(147.26, 5.66));
        expectedZ_profile.add(new Coordinate(175.54, 10));
        expectedZ_profile.add(new Coordinate(190.59, 10));

        /* Table 229 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.02, -1.04, 2.04, 9.07, 146.96, 0.60, 0.77},
                {0.10, -8.64, 5.10, 3.12, 43.87, 0.20, NaN}
        };

        /* Table 230  S -> R */
        double [][] segmentsMeanPlanes1 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.06, -2.84, 3.84, 6.12, 191.02, 0.5, 0.65}
        };
        try {
            exportScene("target/T21.kml", profileBuilder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //Assertion
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes1, propDataOut.getPropagationPaths().get(1).getSRSegment());
        //assertPlanes(segmentsMeanPlanes0, propDataOut.getPropagationPaths().get(0).getSegmentList());
    }


    // TODO rayons manquants left and right
    @Test
    public void TC22(){

        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                        new Coordinate(197, 36.0, 20),
                        new Coordinate(179, 36, 20),
                        new Coordinate(179, 15, 20),
                        new Coordinate(197, 15, 20),
                        new Coordinate(197, 21, 20),
                        new Coordinate(187, 21, 20),
                        new Coordinate(187, 30, 20),
                        new Coordinate(197, 30, 20),
                        new Coordinate(197, 36, 20)},-1)

                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)

                .addTopographicLine(0, 80, 0, 255, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 74, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10);
        builder.setzBuildings(true);
        builder.finishFeeding();

        //  .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(10, 10, 1)
                .addReceiver(187.05, 25, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Expected Values

        /* Table 248 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(110.39, 0.0));
        expectedZ_profile.add(new Coordinate(169.60, 9.08));
        expectedZ_profile.add(new Coordinate(175.62, 10));
        expectedZ_profile.add(new Coordinate(177.63, 10));
        expectedZ_profile.add(new Coordinate(177.68, 10));

        /* Table 249 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.04, -2.06, 3.06, 14.75, 170.26, 0.54, 0.79},
                {0.0, 10, 10, 4.00, 0.05, 0.20, NaN}
        };
        assertPlanes(segmentsMeanPlanes0,propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertZProfil(expectedZ_profile,result);
    }

    @Test
    public void TC23() {

        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(8, 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 9),
                        new Coordinate(110, 34, 9),
                        new Coordinate(110, 26, 9),
                        new Coordinate(75, 26, 9)},buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 8),
                        new Coordinate(118, 18, 8),
                        new Coordinate(118, 10, 8),
                        new Coordinate(83, 10, 8)},buildingsAbs)
                // Ground Surface
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(59.6, -9.87, 0), // 5
                        new Coordinate(76.84, -5.28, 0), // 5-6
                        new Coordinate(63.71, 41.16, 0), // 6-7
                        new Coordinate(46.27, 36.28, 0), // 7-8
                        new Coordinate(59.6, -9.87, 0)
                }), 1.)
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(30, -14, 0), // 5
                        new Coordinate(122, -14, 0), // 5-6
                        new Coordinate(122, 45, 0), // 6-7
                        new Coordinate(30, 45, 0), // 7-8
                        new Coordinate(30, -14, 0)
                }), 0.);
        addTopographicTC23Model(builder);
        builder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(38, 14, 1)
                .addReceiver(107, 25.95, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=0;


        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Expected Value

        /* Table 264 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(14.21, 0.0));
        expectedZ_profile.add(new Coordinate(19.06, 2.85));
        expectedZ_profile.add(new Coordinate(22.64, 5.0));
        expectedZ_profile.add(new Coordinate(23.98, 5.0));
        expectedZ_profile.add(new Coordinate(28.45, 2.34));
        expectedZ_profile.add(new Coordinate(32.30, -0.0));
        expectedZ_profile.add(new Coordinate(70.03, 0.0));

        /* Table 268 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.19, -1.17, 2.13, 1.94, 22.99, 0.37, 0.07},
                {-0.05, 2.89, 3.35, 4.73, 46.04, 0.18, NaN}
        };
        try {
            exportScene("target/T23.kml", builder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertPlanes(segmentsMeanPlanes0,propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertZProfil(expectedZ_profile,result);


    }

    @Test
    public void TC24() {
        //AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(8, 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();



        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 9),
                        new Coordinate(110, 34, 9),
                        new Coordinate(110, 26, 9),
                        new Coordinate(75, 26, 9)},buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 6),
                        new Coordinate(118, 18, 6),
                        new Coordinate(118, 10, 6),
                        new Coordinate(83, 10, 6)},buildingsAbs)
                // Ground Surface
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(59.6, -9.87, 0), // 5
                        new Coordinate(76.84, -5.28, 0), // 5-6
                        new Coordinate(63.71, 41.16, 0), // 6-7
                        new Coordinate(46.27, 36.28, 0), // 7-8
                        new Coordinate(59.6, -9.87, 0)
                }), 1.)
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(30, -14, 0), // 5
                        new Coordinate(122, -14, 0), // 5-6
                        new Coordinate(122, 45, 0), // 6-7
                        new Coordinate(30, 45, 0), // 7-8
                        new Coordinate(30, -14, 0)
                }), 0.);
        builder.setzBuildings(true);
        addTopographicTC23Model(builder);
        builder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(38, 14, 1)
                .addReceiver(106, 18.5, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;
        rayData.computeHorizontalDiffraction=false;
        rayData.computeVerticalDiffraction=true;

        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertEquals(2, propDataOut.getPropagationPaths().size());

        // Expected Values

        /* Table 279 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(14.46, 0.0));
        expectedZ_profile.add(new Coordinate(23.03, 5.0));
        expectedZ_profile.add(new Coordinate(24.39, 5.0));
        expectedZ_profile.add(new Coordinate(32.85, 0.0));
        expectedZ_profile.add(new Coordinate(45.10, 0.0));
        expectedZ_profile.add(new Coordinate(45.10, 6.0));
        expectedZ_profile.add(new Coordinate(60.58, 6.0));
        expectedZ_profile.add(new Coordinate(60.58, 0.0));
        expectedZ_profile.add(new Coordinate(68.15, 0.0));


        /* Table 287 Z-Profile SO */
        List<Coordinate> expectedZ_profileSO = new ArrayList<>();
        expectedZ_profileSO.add(new Coordinate(0.0, 0.0));
        expectedZ_profileSO.add(new Coordinate(14.13, 0.0));
        expectedZ_profileSO.add(new Coordinate(22.51, 5.0));

        List<Coordinate> expectedZ_profileOR = new ArrayList<>();
        expectedZ_profileOR.add(new Coordinate(22.51, 5.0));
        expectedZ_profileOR.add(new Coordinate(23.84, 5.0));
        expectedZ_profileOR.add(new Coordinate(32.13, 0.0));
        expectedZ_profileOR.add(new Coordinate(43.53, 0.0));
        expectedZ_profileOR.add(new Coordinate(70.74, 0.0));

        List<Coordinate> result = propDataOut.getPropagationPaths().get(0).getCutProfile().computePts2DGround();
        assertZProfil(expectedZ_profile,result);

        /* Table 280 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.18, -1.17, 2.13, 1.94, 23.37, 0.37, 0.07},
                {0.0, 0.0, 6.0, 4.0, 7.57, 0.00, NaN}
        };
        assertPlanes(segmentsMeanPlanes0,propDataOut.getPropagationPaths().get(0).getSegmentList());



    }

    @Test
    public void TC25(){
        ///AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(8, 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 0),
                        new Coordinate(110, 34, 0),
                        new Coordinate(110, 26, 0),
                        new Coordinate(75, 26, 0)}, 9, buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 0),
                        new Coordinate(118, 18, 0),
                        new Coordinate(118, 10, 0),
                        new Coordinate(83, 10, 0)}, 6, buildingsAbs)
                // Ground Surface

                .addWall(new Coordinate[]{
                        new Coordinate(59.19, 24.47, 5),
                        new Coordinate(64.17, 6.95, 5)
                }, 0)
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(38, 14, 1)
                .addReceiver(106, 18.5, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=0;


        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Expected Values

        /* Table 301 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(23.77, 0.0));
        expectedZ_profile.add(new Coordinate(45.10, 0.0));
        expectedZ_profile.add(new Coordinate(60.58, 0.0));
        expectedZ_profile.add(new Coordinate(68.15, 0.0));

        /* Table 302 */
        Coordinate expectedSPrime =new Coordinate(0.00,-1.00);
        Coordinate expectedRPrime =new Coordinate(68.15,-4.0);

        if(!builder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(propDataOut.getPropagationPaths().get(0).getSegmentList().size()-1).rPrime);
        }

        /* Table 303 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.0, 0.0, 1.0, 5.0, 23.77, 0.0, 0.0},
                {0.0, 0.0, 6.0, 4.0, 7.57, 0.0, NaN}
        };
        assertPlanes(segmentsMeanPlanes0,propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertZProfil(expectedZ_profile,result);
    }

    /**
     * No datas cnossos for test
     */
    @Test
    public void TC26(){

        GeometryFactory factory = new GeometryFactory();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        // screen
        builder.addWall(new Coordinate[]{
                        new Coordinate(74.0, 52.0, 6),
                        new Coordinate(130.0, 60.0, 8)}, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1)

                .addGroundEffect(factory.toGeometry(new Envelope(0, 50, -10, 100)), 0.0)
                .addGroundEffect(factory.toGeometry(new Envelope(50, 150, -10, 100)), 0.5)
                .setzBuildings(true)
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(10, 10, 0.05)
                .addReceiver(120, 20, 8)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;
        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
        // No datas cnossos for test
    }

    /**
     * erreur de chemin:  Segment list
     */
    @Test
    public void TC27(){
        GeometryFactory factory = new GeometryFactory();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addTopographicLine(80.0, 20.0, -0.5, 110.0, 20.0, -0.5)
                .addTopographicLine(110.0, 20.0, -0.5, 111.0, 20.0, 0.0)
                .addTopographicLine(111.0, 20.0, 0.0, 215.0, 20.0, 0.0)
                .addTopographicLine(215.0, 20.0, 0.0, 215.0, 80.0, 0.0)
                .addTopographicLine(215.0, 80.0, 0.0, 111.0, 80.0, 0.0)
                .addTopographicLine(111.0, 80.0, 0.0, 110.0, 80.0, -0.5)
                .addTopographicLine(110.0, 80.0, -0.5, 80.0, 80.0, -0.5)
                .addTopographicLine(80.0, 80.0, -0.5, 80.0, 20.0, -0.5)
                .addTopographicLine(110.0, 20.0, -0.5, 110.0, 80.0, -0.5)
                .addTopographicLine(111.0, 20.0, 0.0, 111.0, 80.0, 0.0)

                .addGroundEffect(80, 110, 20, 80, 0.0)
                .addGroundEffect(110, 215, 20, 80, 1.0)
                .addWall(new Coordinate[]{
                        new Coordinate(114.0, 52.0, 2.5),
                        new Coordinate(170.0, 60.0, 4.5)}, -1)

                .finishFeeding();


        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(105, 35, -0.45)
                .addReceiver(200, 50, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        //rayData.reflexionOrder=1;

        //Out and computation settings
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Test R-CRIT table 333 diffraction
        /*Coordinate D = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(0).getSegmentList().get(1).rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).d + D.distance(propDataOut.getPropagationPaths().get(0).getPointList().get(2).coordinate) - propDataOut.getPropagationPaths().get(0).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }*/

        // Test R-CRIT table 338 reflexion: Error: no data for "Rayleigh-Criterion" (favourable) we just have (homogeneous) data
        Coordinate D = propDataOut.getPropagationPaths().get(1).getSegmentList().get(1).r;
        Coordinate Sp = propDataOut.getPropagationPaths().get(1).getSegmentList().get(0).sPrime;
        Coordinate Rp = propDataOut.getPropagationPaths().get(1).getSRSegment().rPrime ;

        double deltaD = propDataOut.getPropagationPaths().get(1).getSegmentList().get(0).s.distance(D) + D.distance(propDataOut.getPropagationPaths().get(1).getPointList().get(3).coordinate) - propDataOut.getPropagationPaths().get(1).getSRSegment().d;
        double deltaDE = Sp.distance(D) + D.distance(Rp) - Sp.distance(Rp);
        List<Integer> res1 = new ArrayList<>(3) ;
        List<Integer> res2 = new ArrayList<>(3);

        for(int f : computeRays.getData().freq_lvl) {
            if(deltaD > -(340./f) / 20) {
                res1.add(1);
            }
            if (!(deltaD > (((340./f) / 4) - deltaDE))){
                res2.add(0);
            }
        }

        /* Table 331 */
        Coordinate expectedSPrime =new Coordinate(0.01,-0.69);
        Coordinate expectedRPrime =new Coordinate(96.18,-4.0);

        if(!builder.getWalls().isEmpty()){
            assertMirrorPoint(expectedSPrime,expectedRPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(0).sPrime,propDataOut.getPropagationPaths().get(0).getSegmentList().get(propDataOut.getPropagationPaths().get(0).getSegmentList().size()-1).rPrime);
        }
        /* Table 329 */
        double [][] segmentsMeanPlanesH = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.04, -0.57, 0.12, 0.35, 6.09, 0.17, 0.07},
                {0.0, 0.0, 0.0, 4.0, 90.10, 1.0, 1.0}
        };

        try {
            exportScene("target/T27.kml", builder, propDataOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertPlanes(segmentsMeanPlanesH,propDataOut.getPropagationPaths().get(0).getSegmentList());


    }

    /**
     * error:       if b = 0.68: -> z2 = 0.32. In Cnossos z2 = 1.32 if b = 0.68
     */
    @Test
    public void TC28(){
        GeometryFactory factory = new GeometryFactory();


        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                        new Coordinate(113, 10, 0),
                        new Coordinate(127, 16, 0),
                        new Coordinate(102, 70, 0),
                        new Coordinate(88, 64, 0)}, 6, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(176, 19, 0),
                        new Coordinate(164, 88, 0),
                        new Coordinate(184, 91, 0),
                        new Coordinate(196, 22, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(250, 70, 0),
                        new Coordinate(250, 180, 0),
                        new Coordinate(270, 180, 0),
                        new Coordinate(270, 70, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(332, 32, 0),
                        new Coordinate(348, 126, 0),
                        new Coordinate(361, 108, 0),
                        new Coordinate(349, 44, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(400, 5, 0),
                        new Coordinate(400, 85, 0),
                        new Coordinate(415, 85, 0),
                        new Coordinate(415, 5, 0)}, 9, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(444, 47, 0),
                        new Coordinate(436, 136, 0),
                        new Coordinate(516, 143, 0),
                        new Coordinate(521, 89, 0),
                        new Coordinate(506, 87, 0),
                        new Coordinate(502, 127, 0),
                        new Coordinate(452, 123, 0),
                        new Coordinate(459, 48, 0)}, 12, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(773, 12, 0),
                        new Coordinate(728, 90, 0),
                        new Coordinate(741, 98, 0),
                        new Coordinate(786, 20, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(972, 82, 0),
                        new Coordinate(979, 121, 0),
                        new Coordinate(993, 118, 0),
                        new Coordinate(986, 79, 0)}, 8, -1)
                .addGroundEffect(-11, 1011, -300, 300,0.5);


        builder.finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(builder)
                .addSource(0, 50, 4)
                .addReceiver(1000, 100, 1)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();
        rayData.reflexionOrder=1;
        PathFinderVisitor propDataOut = new PathFinderVisitor(true);
        PathFinder computeRays = new PathFinder(rayData);
        computeRays.setThreadCount(1);

        computeRays.run(propDataOut);

        CutProfile cutProfile = computeRays.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS, false);
        List<Coordinate> result = cutProfile.computePts2DGround();


        // Expected Values


        /* Table 346 */
        List<Coordinate> expectedZ_profile = new ArrayList<>();
        expectedZ_profile.add(new Coordinate(0.0, 0.0));
        expectedZ_profile.add(new Coordinate(92.45, 0.0));
        expectedZ_profile.add(new Coordinate(108.87, 0.0));
        expectedZ_profile.add(new Coordinate(169.34, 0.0));
        expectedZ_profile.add(new Coordinate(189.71, 0.0));
        expectedZ_profile.add(new Coordinate(338.36, 0.0));
        expectedZ_profile.add(new Coordinate(353.88, 0.0));
        expectedZ_profile.add(new Coordinate(400.5, 0.0));
        expectedZ_profile.add(new Coordinate(415.52, 0.0));
        expectedZ_profile.add(new Coordinate(442.3, 0.0));
        expectedZ_profile.add(new Coordinate(457.25, 0.0));
        expectedZ_profile.add(new Coordinate(730.93, 0.0));
        expectedZ_profile.add(new Coordinate(748.07, 0.0));
        expectedZ_profile.add(new Coordinate(976.22, 0.0));
        expectedZ_profile.add(new Coordinate(990.91, 0.0));
        expectedZ_profile.add(new Coordinate(1001.25, 0.0));

        /* Table 348 */
        double [][] segmentsMeanPlanes0 = new double[][]{
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.0, 0.25, 3.75, 9.09, 169.37, 0.45, 0.48},
                {0.0, 0.0, 8.0, 1.0, 10.34, 0.5, NaN}
        };

        double [][] segmentsMeanPlanes1 = new double[][]{ // Right
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.0, 0.0, 4.0, 1.0, 1028.57, 0.5, 0.5}
        };

        double [][] segmentsMeanPlanes2 = new double[][]{ // left
                //  a     b     zs    zr      dp    Gp   Gp'
                {0.0, 0.68, 3.32, 1.12, 1022.31, 0.49, 0.49}
        };
        assertZProfil(expectedZ_profile,result);
        assertPlanes(segmentsMeanPlanes0,propDataOut.getPropagationPaths().get(0).getSegmentList());
        assertPlanes(segmentsMeanPlanes1,propDataOut.getPropagationPaths().get(1).getSRSegment());
        //assertPlanes(segmentsMeanPlanes2,propDataOut.getPropagationPaths().get(2).getSRSegment()); // if b = 0.68: -> z2 = 0.32. In Cnossos z2 = 1.32 if b = 0.68

    }


    /**
     * Assertions for a list of {@link CnossosPath}.
     * @param expectedPts    Array of arrays of array of expected coordinates (xyz) of points of paths. To each path
     *                       corresponds an array of points. To each point corresponds an array of coordinates (xyz).
     * @param expectedGPaths Array of arrays of gPaths values. To each path corresponds an arrays of gPath values.
     * @param actualPathParameters    Computed arrays of {@link CnossosPath}.
     */
    private static void assertPaths(double[][][] expectedPts, double[][] expectedGPaths, List<CnossosPath> actualPathParameters) {
        assertEquals("Expected path count is different than actual path count.", expectedPts.length, actualPathParameters.size());
        for(int i=0; i<expectedPts.length; i++) {
            CnossosPath pathParameters = actualPathParameters.get(i);
            for(int j=0; j<expectedPts[i].length; j++){
                PointPath point = pathParameters.getPointList().get(j);
                assertEquals("Path "+i+" point "+j+" coord X", expectedPts[i][j][0], point.coordinate.x, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Y", expectedPts[i][j][1], point.coordinate.y, DELTA_COORDS);
            }
            assertEquals("Expected path["+i+"] segments count is different than actual path segment count.", expectedGPaths[i].length, pathParameters.getSegmentList().size());
            for(int j=0; j<expectedGPaths[i].length; j++) {
                assertEquals("Path " + i + " g path " + j, expectedGPaths[i][j], pathParameters.getSegmentList().get(j).gPath, DELTA_G_PATH);
            }
        }
    }


    /**
     * Assertions for a list of {@link CnossosPath}.
     * @param expectedPts    Array of arrays of array of expected coordinates (xyz) of points of paths. To each path
     *                       corresponds an array of points. To each point corresponds an array of coordinates (xyz).
     * @param actualPathParameters    Computed arrays of {@link CnossosPath}.
     */
    private static void assertPaths(double[][][] expectedPts, List<CnossosPath> actualPathParameters) {
        assertEquals("Expected path count is different than actual path count.", expectedPts.length, actualPathParameters.size());
        for(int i=0; i<expectedPts.length; i++) {
            CnossosPath pathParameters = actualPathParameters.get(i);
            for(int j=0; j<expectedPts[i].length; j++){
                PointPath point = pathParameters.getPointList().get(j);
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

    private static void assertZProfil(List<Coordinate> expectedZ_profile, List<Coordinate> actualZ_profile) {
        if (expectedZ_profile.size() != actualZ_profile.size()){
            assertEquals("Expected zprofil count is different than actual zprofil count.", expectedZ_profile.size(), actualZ_profile.size());
        }
        for (int i = 0; i < actualZ_profile.size(); i++) {
            assertEquals(String.format(Locale.ROOT, "Coord X point %d", i), expectedZ_profile.get(i).x, actualZ_profile.get(i).x, DELTA_COORDS);
            assertEquals(String.format(Locale.ROOT, "Coord Y point %d", i), expectedZ_profile.get(i).y, actualZ_profile.get(i).y, DELTA_COORDS);
        }
    }

    private static void assertMirrorPoint(Coordinate expectedSprime, Coordinate expectedRprime,Coordinate actualSprime, Coordinate actualRprime) {
        assertCoordinateEquals("Sprime ",expectedSprime, actualSprime, DELTA_COORDS);
        assertCoordinateEquals("Rprime ",expectedRprime, actualRprime, DELTA_COORDS);;
    }

    private static void assertCoordinateEquals(String message,Coordinate expected, Coordinate actual, double toleranceX) {
        double diffX = Math.abs(expected.getX() - actual.getX());
        double diffY = Math.abs(expected.getY() - actual.getY());

        if (diffX > toleranceX || diffY > toleranceX) {
            String result = String.format("Expected coordinate: (%.3f, %.3f), Actual coordinate: (%.3f, %.3f)",
                    expected.getX(), expected.getY(), actual.getX(), actual.getY());
            throw new AssertionError(message+result);
        }
    }

    private void exportScene(String name, ProfileBuilder builder, PathFinderVisitor result) throws IOException {
        try {
            Coordinate proj = new Coordinate( 351714.794877, 6685824.856402, 0);
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            //kmlDocument.doTransform(builder.getTriangles());
            kmlDocument.setInputCRS("EPSG:2154");
            //kmlDocument.setInputCRS("EPSG:" + crs);
            kmlDocument.setOffset(proj);
            kmlDocument.writeHeader();
            if(builder != null) {
                kmlDocument.writeTopographic(builder.getTriangles(), builder.getVertices());
                kmlDocument.writeBuildings(builder);
                kmlDocument.writeWalls(builder);
                //kmlDocument.writeProfile(PathFinder.getData().profileBuilder.getProfile(rayData.sourceGeometries.get(0).getCoordinate(), rayData.receivers.get(0), computeRays.getData().gS);
                //kmlDocument.writeProfile("S:0 R:0", builder.getProfile(result.getInputData().sourceGeometries.get(0).getCoordinate(),result.getInputData().receivers.get(0)));
            }
            if(result != null) {
                kmlDocument.writeRays(result.getPropagationPaths());
            }
            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }

}
