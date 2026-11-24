/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.pathfinder;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CurvedProfileGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class CurvedProfileTest {

    /**
     * Test case 28 for favourable propagation conditions between source and receiver
     */
    @Test
    public void testTC28DirectCurvedProfile() {

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                        new Coordinate(113, 10, 6),
                        new Coordinate(127, 16, 6),
                        new Coordinate(102, 70, 6),
                        new Coordinate(88, 64, 6)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(176, 19, 10),
                        new Coordinate(164, 88, 10),
                        new Coordinate(184, 91, 10),
                        new Coordinate(196, 22, 10)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(250, 70, 14),
                        new Coordinate(250, 180, 14),
                        new Coordinate(270, 180, 14),
                        new Coordinate(270, 70, 14)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(332, 32, 10),
                        new Coordinate(348, 126, 10),
                        new Coordinate(361, 108, 10),
                        new Coordinate(349, 44, 10)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(400, 5, 9),
                        new Coordinate(400, 85, 9),
                        new Coordinate(415, 85, 9),
                        new Coordinate(415, 5, 9)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(444, 47, 12),
                        new Coordinate(436, 136, 12),
                        new Coordinate(516, 143, 12),
                        new Coordinate(521, 89, 12),
                        new Coordinate(506, 87, 12),
                        new Coordinate(502, 127, 12),
                        new Coordinate(452, 123, 12),
                        new Coordinate(459, 48, 12)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(773, 12, 14),
                        new Coordinate(728, 90, 14),
                        new Coordinate(741, 98, 14),
                        new Coordinate(786, 20, 14)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(972, 82, 8),
                        new Coordinate(979, 121, 8),
                        new Coordinate(993, 118, 8),
                        new Coordinate(986, 79, 8)}, -1)
                .addGroundEffect(-11, 1011, -300, 300,0.5);


        builder.finishFeeding();
        Coordinate source = new Coordinate(0, 50, 4);
        Coordinate receiver = new Coordinate(1000, 100, 1);
        CutProfile profile = builder.getProfile(source, receiver);

        List<Integer> homogeneousHullIndices = profile.getConvexHullIndices(profile.computePts2D());
        // Expected
        // dSR       : 1001.2537 m
        // dSO       : 169.4555  m
        // dOR       :  12.4836  m

        assertEquals(7, homogeneousHullIndices.size());
        assertEquals(169.4555, profile.getCutPoints().get(homogeneousHullIndices.get(1)).getCoordinate().distance3D(source), 1e-4);
        assertEquals(12.4836, profile.getCutPoints().get(homogeneousHullIndices.get(homogeneousHullIndices.size() - 2)).getCoordinate().distance3D(receiver), 1e-4);

        // Check new convex hull
        List<Integer> curvatureHullIndices = profile.getConvexHullIndices(profile.computePts2D(true));
        // Expected
        // dSR       : 1001.2537
        // dSO       : 991.5540 m
        // dOR       :  12.4836 m

        assertEquals(3, curvatureHullIndices.size());
        double SO = CurvedProfileGenerator.toCurve(
                profile.getCutPoints().get(curvatureHullIndices.get(1)).getCoordinate().distance3D(source),
                source.distance3D(receiver));
        assertEquals(991.5540, SO, 1e-4);
        double OR = CurvedProfileGenerator.toCurve(
                profile.getCutPoints().get(curvatureHullIndices.get(curvatureHullIndices.size() - 2))
                        .getCoordinate().distance3D(receiver), source.distance3D(receiver));
        assertEquals(12.4836, OR, 1e-4);
    }

    @Test
    public void testInverseCurved() {
        Coordinate source = new Coordinate(50, 15, 500.5);
        Coordinate receiver = new Coordinate(950, -25, 502.5);
        int intermediateSteps = 20;
        List<Coordinate> flatProfile = new ArrayList<>();
        flatProfile.add(source);
        for (int i = 1; i <= intermediateSteps; i++) {
            double ratio = (double) i / (intermediateSteps + 1);
            double x = source.x + ratio * (receiver.x - source.x);
            double y = source.y + ratio * (receiver.y - source.y);
            flatProfile.add(new Coordinate(x, y, 500));
        }
        flatProfile.add(receiver);

        Coordinate[] curvedCoordinates = CurvedProfileGenerator.applyTransformation(source, receiver,
                flatProfile.toArray(new Coordinate[0]), false);
        Coordinate[] inverseCoordinates = CurvedProfileGenerator.applyTransformation(source, receiver,
                curvedCoordinates, true);
        for (int i = 0; i < inverseCoordinates.length; i++) {
            double expectedX = flatProfile.get(i).x;
            double expectedY = flatProfile.get(i).y;
            double expectedZ = flatProfile.get(i).z;
            double computedX = inverseCoordinates[i].x;
            double computedY = inverseCoordinates[i].y;
            double computedZ = inverseCoordinates[i].z;
            assertEquals(expectedX, computedX, 1e-5, String.format(Locale.ROOT, "Error at point %d : expectedX %.6f, computedX %.6f", i, expectedX, computedX));
            assertEquals(expectedY, computedY, 1e-5, String.format(Locale.ROOT, "Error at point %d : expectedY %.6f, computedY %.6f", i, expectedY, computedY));
            assertEquals(expectedZ, computedZ, 0.01, String.format(Locale.ROOT, "Error at point %d : expectedZ %.6f, computedZ %.6f", i, expectedZ, computedZ));
        }
    }

    @Test
    public void testCurvedGroundFromGraph() {

        Coordinate source = new Coordinate(0,0,0);
        Coordinate receiver = new Coordinate(1040,0,3);

        // Values are extracted from a 2D graph, so it is not accurate
        List<Coordinate> coordinates = List.of(
                new Coordinate(0, 0,0),
                new Coordinate(50,  0,-2.933),
                new Coordinate(100, 0, -5.594),
                new Coordinate(150, 0, -7.972),
                new Coordinate(200, 0, -9.984),
                new Coordinate(250, 0, -11.699),
                new Coordinate(300, 0, -13.162),
                new Coordinate(350, 0, -14.332),
                new Coordinate(400, 0, -15.199),
                new Coordinate(450, 0, -15.888),
                new Coordinate(500, 0, -16.119),
                new Coordinate(550, 0, -16.116),
                new Coordinate(600, 0, -15.765),
                new Coordinate(650, 0, -15.131),
                new Coordinate(700, 0, -14.224),
                new Coordinate(750, 0, -13.023),
                new Coordinate(800, 0, -11.519),
                new Coordinate(850, 0, -9.717),
                new Coordinate(900, 0, -7.553),
                new Coordinate(950, 0, -5.123),
                new Coordinate(1000,0, -2.377),
                new Coordinate(1040,0, 0)
        );
        Coordinate[] flatGroundcoordinates = new Coordinate[coordinates.size()];
        for (int i = 0; i < flatGroundcoordinates.length; i++) {
            flatGroundcoordinates[i] = new Coordinate(coordinates.get(i).x, 0, 0);
        }
        Coordinate[] curvedCoordinates = CurvedProfileGenerator.applyTransformation(source, receiver, flatGroundcoordinates, false);
        for (int i = 0; i < curvedCoordinates.length; i++) {
            double expectedZ = coordinates.get(i).z;
            double computedZ = curvedCoordinates[i].z;
            assertEquals(expectedZ, computedZ, 0.3, String.format(Locale.ROOT, "Error at point %d : expected %.3f, computed %.3f", i, expectedZ, computedZ));
        }

        // Compute inverse transformation
        Coordinate[] inverseCoordinates = CurvedProfileGenerator.applyTransformation(source, receiver, curvedCoordinates, true);
        for (int i = 0; i < inverseCoordinates.length; i++) {
            double expectedX = flatGroundcoordinates[i].x;
            double expectedY = flatGroundcoordinates[i].y;
            double expectedZ = flatGroundcoordinates[i].z;
            double computedX = inverseCoordinates[i].x;
            double computedY = inverseCoordinates[i].y;
            double computedZ = inverseCoordinates[i].z;
            assertEquals(expectedX, computedX, 1e-5, String.format(Locale.ROOT, "Error at point %d : expectedX %.6f, computedX %.6f", i, expectedX, computedX));
            assertEquals(expectedY, computedY, 1e-5, String.format(Locale.ROOT, "Error at point %d : expectedY %.6f, computedY %.6f", i, expectedY, computedY));
            assertEquals(expectedZ, computedZ, 0.01, String.format(Locale.ROOT, "Error at point %d : expectedZ %.6f, computedZ %.6f", i, expectedZ, computedZ));
        }
    }




    /**
     * Test case 28 for favourable propagation conditions over left and right curved profiles
     */
    @Test
    public void testTC28LateralCurvedProfile() {

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        ProfileBuilder profileBuilder = builder.addBuilding(new Coordinate[]{
                        new Coordinate(113, 10, 6),
                        new Coordinate(127, 16, 6),
                        new Coordinate(102, 70, 6),
                        new Coordinate(88, 64, 6)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(176, 19, 10),
                        new Coordinate(164, 88, 10),
                        new Coordinate(184, 91, 10),
                        new Coordinate(196, 22, 10)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(250, 70, 14),
                        new Coordinate(250, 180, 14),
                        new Coordinate(270, 180, 14),
                        new Coordinate(270, 70, 14)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(332, 32, 10),
                        new Coordinate(348, 126, 10),
                        new Coordinate(361, 108, 10),
                        new Coordinate(349, 44, 10)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(400, 5, 9),
                        new Coordinate(400, 85, 9),
                        new Coordinate(415, 85, 9),
                        new Coordinate(415, 5, 9)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(444, 47, 12),
                        new Coordinate(436, 136, 12),
                        new Coordinate(516, 143, 12),
                        new Coordinate(521, 89, 12),
                        new Coordinate(506, 87, 12),
                        new Coordinate(502, 127, 12),
                        new Coordinate(452, 123, 12),
                        new Coordinate(459, 48, 12)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(773, 12, 14),
                        new Coordinate(728, 90, 14),
                        new Coordinate(741, 98, 14),
                        new Coordinate(786, 20, 14)}, -1)

                .addBuilding(new Coordinate[]{
                        new Coordinate(972, 82, 8),
                        new Coordinate(979, 121, 8),
                        new Coordinate(993, 118, 8),
                        new Coordinate(986, 79, 8)}, -1)
                .addGroundEffect(-11, 1011, -300, 300, 0.5);


        builder.finishFeeding();
        Scene scene = new Scene(builder);
        scene.maxSrcDist = 5000;
        PathFinder pathFinder = new PathFinder(scene);
        Coordinate source = new Coordinate(0, 50, 4);
        Coordinate receiver = new Coordinate(1000, 100, 1);
        List<Coordinate> curvedSideHull = pathFinder.computeSideHull(true, source, receiver, true);
        // Left Diffraction over a single building (2 corner of the building) near the receiver
        assertEquals(4, curvedSideHull.size());
        assertEquals(0, new Coordinate(979, 121, 0).distance(curvedSideHull.get(1)), 0.1);
        assertEquals(0, new Coordinate(993, 118, 0).distance(curvedSideHull.get(2)), 0.1);

        curvedSideHull = pathFinder.computeSideHull(false, source, receiver, true);
        // Right Diffraction over two buildings (lowest corner of each building) last two buildings near the receiver
        assertEquals(4, curvedSideHull.size());
        assertEquals(0, new Coordinate(773, 12, 0).distance(curvedSideHull.get(1)), 0.1);
        assertEquals(0, new Coordinate(986, 79, 0).distance(curvedSideHull.get(2)), 0.1);

    }

    /**
     * The cut points of a reflection profile are moved by the arc at their abscissa along the unfolded path, not at
     * their distance to the source in plan. The receiver is 200 m from the source and the wall 50 m behind the
     * receiver, so the unfolded path is 300 m long and the points after the reflection lie farther from the source
     * than the receiver. They must be lowered like every point between the source and the receiver.
     */
    @Test
    public void testReflectionProfileFollowsUnfoldedPath() {
        double wallTop = 3.0;
        LineSegment wall = new LineSegment(new Coordinate(250, -50, wallTop), new Coordinate(250, 50, wallTop));
        List<CutPoint> flatProfile = new ArrayList<>();
        flatProfile.add(new CutPointSource(new CutPoint(new Coordinate(0, 0, 0.05), 0, 0)));
        flatProfile.add(new CutPoint(new Coordinate(125, 0, 0.66), 0, 0));
        flatProfile.add(new CutPointReflection(new CutPoint(new Coordinate(250, 0, 1.26), 0, 0), wall,
                Arrays.asList(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1)));
        flatProfile.add(new CutPoint(new Coordinate(225, 0, 1.38), 0, 0));
        flatProfile.add(new CutPointReceiver(new CutPoint(new Coordinate(200, 0, 1.5), 0, 0)));
        double[] abscissa = new double[]{0, 125, 250, 275, 300};
        double d = 300;
        double radius = Math.max(1000, 8 * d);

        List<CutPoint> curvedProfile = CurvedProfileGenerator.applyTransformation(flatProfile, false);

        assertEquals(flatProfile.size(), curvedProfile.size());
        for (int i = 0; i < flatProfile.size(); i++) {
            double expectedOffset = Math.sqrt(radius * radius - d * d / 4)
                    - Math.sqrt(radius * radius - Math.pow(abscissa[i] - d / 2, 2));
            CutPoint flat = flatProfile.get(i);
            CutPoint curved = curvedProfile.get(i);
            assertEquals(flat.getCoordinate().z + expectedOffset, curved.getCoordinate().z, 1e-9);
            assertEquals(flat.zGround + expectedOffset, curved.zGround, 1e-9);
            assertEquals(flat.getCoordinate().x, curved.getCoordinate().x, 0);
            assertEquals(flat.getCoordinate().y, curved.getCoordinate().y, 0);
            if (i > 0 && i < flatProfile.size() - 1) {
                assertTrue(expectedOffset < 0, "points between the source and the receiver are lowered");
            } else {
                assertEquals(0, expectedOffset, 1e-9);
            }
        }
        // The wall is moved by the offset of the reflection point
        CutPointReflection curvedReflection = (CutPointReflection) curvedProfile.get(2);
        assertEquals(curvedProfile.get(2).getCoordinate().z - 1.26 + wallTop, curvedReflection.wall.p0.z, 1e-9);
        assertEquals(curvedProfile.get(2).getCoordinate().z - 1.26 + wallTop, curvedReflection.wall.p1.z, 1e-9);
        // The flat profile is left unchanged
        assertEquals(wallTop, ((CutPointReflection) flatProfile.get(2)).wall.p0.z, 0);
        assertEquals(1.26, flatProfile.get(2).getCoordinate().z, 0);
    }

    /**
     * On a direct profile the abscissa along the path is the distance to the source, so the cut point transformation
     * gives the same altitudes as the coordinate transformation between the source and the receiver.
     */
    @Test
    public void testDirectProfileMatchesCoordinateTransformation() {
        Coordinate source = new Coordinate(10, 20, 0.05);
        Coordinate receiver = new Coordinate(310, 420, 4);
        List<CutPoint> flatProfile = new ArrayList<>();
        flatProfile.add(new CutPointSource(new CutPoint(source, 0, 0)));
        for (int i = 1; i < 10; i++) {
            double t = i / 10.0;
            flatProfile.add(new CutPoint(new Coordinate(10 + 300 * t, 20 + 400 * t, 2 + i), 1 + 0.1 * i, 0.5));
        }
        flatProfile.add(new CutPointReceiver(new CutPoint(receiver, 0, 0)));

        List<CutPoint> curvedProfile = CurvedProfileGenerator.applyTransformation(flatProfile, false);
        Coordinate[] curvedCoordinates = CurvedProfileGenerator.applyTransformation(source, receiver,
                flatProfile.stream().map(CutPoint::getCoordinate).toArray(Coordinate[]::new), false);

        for (int i = 0; i < flatProfile.size(); i++) {
            assertEquals(curvedCoordinates[i].z, curvedProfile.get(i).getCoordinate().z, 1e-9);
        }
    }
}
