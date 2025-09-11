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
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CurvedProfileGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class CurvedProfileTest {

    /**
     * Test case 28 for favorable propagation conditions between source and receiver
     */
    @Test
    public void testTC28DirectCurvedProfile() {

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
        List<CutPoint> curvedProfile = CurvedProfileGenerator.applyTransformation(profile.getCutPoints(), false);
        assertInstanceOf(CutPointSource.class, curvedProfile.get(0));
        assertInstanceOf(CutPointReceiver.class, curvedProfile.get(curvedProfile.size() - 1));
        CutProfile curvedCutProfile = new CutProfile((CutPointSource) curvedProfile.get(0),
                (CutPointReceiver) curvedProfile.get(curvedProfile.size() - 1));
        curvedCutProfile.cutPoints = new ArrayList<>(curvedProfile);
        // Check new convex hull
        List<Integer> curvatureHullIndices = curvedCutProfile.getConvexHullIndices(curvedCutProfile.computePts2D());
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
     * Test case 28 for favorable propagation conditions over left and right curved profiles
     */
    @Test
    public void testTC28LateralCurvedProfile() {

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
                .addGroundEffect(-11, 1011, -300, 300, 0.5);


        builder.finishFeeding();
        Scene scene = new Scene(builder);
        scene.maxSrcDist = 5000;
        PathFinder pathFinder = new PathFinder(scene);
        Coordinate source = new Coordinate(0, 50, 4);
        Coordinate receiver = new Coordinate(1000, 100, 1);
        List<Coordinate> curvedSideHull = pathFinder.computeSideHull(true, source, receiver, true);
        // Diffraction over a single building (2 corner of the building) near the receiver
        assertEquals(4, curvedSideHull.size());
        curvedSideHull = pathFinder.computeSideHull(false, source, receiver, true);
        // Diffraction over two buildings (one corner for each building) near the receiver
        assertEquals(4, curvedSideHull.size());

    }
}
