/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.cnossos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReflection;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the geometry of propagation paths. A path can mix a reflection
 * and diffractions; the produced point list must stay sorted by distance along the
 * profile so that {@link CnossosPath#asGeom()} builds a continuous polyline.
 */
class MixedReflectionDiffractionPathTest {
    /** Coordinate tolerance in meters. */
    private static final double COORDINATE_TOLERANCE = 0.1;
    private static final String MIXED_PROFILE_RESOURCE =
            "/org/noise_planet/noisemodelling/propagation/RegressionTestMixedReflectionRayleigh.json";
    /**
     * The reported PK=454 row was exported with a single 500 Hz band. Using this band
     * reproduces exactly the three duplicated 308.37 m Rayleigh points followed by the
     * 398.13 m one and the 286.69 m reflection point.
     */
    private static final List<Double> EXACT_FREQUENCIES = Arrays.asList(500.0);

    private static CutProfile loadProfile(URL url) throws IOException {
        assertNotNull(url, "Missing cut profile resource");
        try (InputStream inputStream = url.openStream()) {
            return new ObjectMapper().readValue(inputStream, CutProfile.class);
        }
    }

    private static CnossosPath computeHomogeneousPath(CutProfile cutProfile) {
        return CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, EXACT_FREQUENCIES,
                cutProfile.getSource().groundCoefficient, false);
    }

    private static void assertPointsAreOrdered(CnossosPath path) {
        List<PointPath> points = path.getPointList();
        assertNotNull(points);
        assertTrue(points.size() >= 2, "A path must contain at least the source and the receiver");
        assertEquals(PointPath.POINT_TYPE.SRCE, points.get(0).type);
        assertEquals(PointPath.POINT_TYPE.RECV, points.get(points.size() - 1).type);
        double lastDistance = Double.NEGATIVE_INFINITY;
        for (PointPath point : points) {
            assertTrue(point.coordinate.x >= lastDistance,
                    "pointList must be sorted by profile distance, got x=" + point.coordinate.x
                            + " after x=" + lastDistance);
            lastDistance = point.coordinate.x;
        }
    }

    private static void assertLineStringHasNoBacktracking(LineString lineString) {
        assertTrue(lineString.getNumPoints() >= 2);
        Coordinate source = lineString.getCoordinateN(0);
        Coordinate receiver = lineString.getCoordinateN(lineString.getNumPoints() - 1);
        double dx = receiver.x - source.x;
        double dy = receiver.y - source.y;
        double length2 = dx * dx + dy * dy;
        assertTrue(length2 > 0, "Source and receiver must not be at the same location");
        double previousProjection = Double.NEGATIVE_INFINITY;
        double cumulativeLength = 0;
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Coordinate current = lineString.getCoordinateN(i);
            double projection = ((current.x - source.x) * dx + (current.y - source.y) * dy) / length2;
            assertTrue(projection > previousProjection,
                    "The LineString backtracks at vertex " + i + " (projection=" + projection + ")");
            previousProjection = projection;
            if (i > 0) {
                Coordinate previous = lineString.getCoordinateN(i - 1);
                assertTrue(current.distance3D(previous) > 0,
                        "Duplicate LineString vertex at index " + i);
                cumulativeLength += current.distance(previous);
            }
        }
        assertEquals(lineString.getLength(), cumulativeLength, 1e-6,
                "The plan-view length must equal the sum of the segment lengths");
    }

    private static int findCutPointIndex(CutProfile cutProfile, PointPath point) {
        List<Coordinate> pts2D = cutProfile.computePts2D();
        for (int i = 0; i < pts2D.size(); i++) {
            Coordinate coordinate = pts2D.get(i);
            if (coordinate.x == point.coordinate.x && coordinate.y == point.coordinate.y) {
                return i;
            }
        }
        throw new AssertionError("No cut point matches the path point at x="
                + point.coordinate.x + ", y=" + point.coordinate.y);
    }

    private static void assertVerticesAreAboveGround(CutProfile cutProfile, LineString lineString) {
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Coordinate vertex = lineString.getCoordinateN(i);
            CutPoint nearest = cutProfile.cutPoints.stream()
                    .min((a, b) -> Double.compare(
                            a.getCoordinate().distance(vertex),
                            b.getCoordinate().distance(vertex)))
                    .orElseThrow();
            assertTrue(vertex.z >= nearest.getzGround() - COORDINATE_TOLERANCE,
                    "Vertex " + i + " (z=" + vertex.z + ") is below the ground (zGround="
                            + nearest.getzGround() + ")");
        }
    }

    @Test
    void mixedReflectionAndRayleighDiffractionPathIsOrdered() throws IOException {
        CutProfile cutProfile = loadProfile(
                MixedReflectionDiffractionPathTest.class.getResource(MIXED_PROFILE_RESOURCE));
        CnossosPath path = computeHomogeneousPath(cutProfile);
        assertNotNull(path, "The mixed reflection/diffraction path must be computed");

        assertPointsAreOrdered(path);
        List<PointPath> points = path.getPointList();

        List<Integer> reflectionIndices = java.util.stream.IntStream.range(0, points.size())
                .filter(i -> points.get(i).type == PointPath.POINT_TYPE.REFL)
                .boxed()
                .collect(Collectors.toList());
        assertEquals(1, reflectionIndices.size(), "The path must contain exactly one reflection");
        int reflectionIndex = reflectionIndices.get(0);
        assertTrue(reflectionIndex > 0, "The reflection must be after the source");
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).type == PointPath.POINT_TYPE.DIFH
                    || points.get(i).type == PointPath.POINT_TYPE.DIFH_RCRIT
                    || points.get(i).type == PointPath.POINT_TYPE.DIFV) {
                assertTrue(i > reflectionIndex,
                        "Diffraction point at index " + i + " must be after the reflection at index "
                                + reflectionIndex);
            }
        }

        // The three duplicated cut points of the same wall must produce a single point
        List<PointPath> diffractionPoints = points.stream()
                .filter(p -> p.type == PointPath.POINT_TYPE.DIFH_RCRIT)
                .collect(Collectors.toList());
        assertEquals(2, diffractionPoints.size(), "Duplicate Rayleigh diffraction points must be removed");
        for (int i = 1; i < diffractionPoints.size(); i++) {
            assertTrue(!diffractionPoints.get(i).coordinate.equals2D(
                            diffractionPoints.get(i - 1).coordinate),
                    "Rayleigh diffraction points must not be duplicated");
        }

        LineString lineString = path.asGeom();
        assertEquals(points.size(), lineString.getNumPoints(),
                "The LineString must contain one vertex per path point");
        assertLineStringHasNoBacktracking(lineString);
        assertVerticesAreAboveGround(cutProfile, lineString);
        double profileLength = cutProfile.computePts2D().get(cutProfile.cutPoints.size() - 1).x;
        assertEquals(profileLength, lineString.getLength(), COORDINATE_TOLERANCE,
                "The plan-view LineString must have the same length as the cut profile");

        Coordinate source = cutProfile.getSource().getCoordinate();
        Coordinate receiver = cutProfile.getReceiver().getCoordinate();
        Coordinate first = lineString.getCoordinateN(0);
        Coordinate last = lineString.getCoordinateN(lineString.getNumPoints() - 1);
        assertEquals(source.x, first.x, 1e-6);
        assertEquals(source.y, first.y, 1e-6);
        assertEquals(source.z, first.z, 1e-6);
        assertEquals(receiver.x, last.x, 1e-6);
        assertEquals(receiver.y, last.y, 1e-6);
        assertEquals(receiver.z, last.z, 1e-6);

        // The reflection geometry must be the reflection point of the cut profile, located
        // on the reflection wall. This used to be a point ~95 m away because asGeom() was
        // reading the profile monotonically while the point list backtracked.
        CutPointReflection reflectionCut = (CutPointReflection) cutProfile.cutPoints.stream()
                .filter(CutPointReflection.class::isInstance)
                .findFirst()
                .orElseThrow();
        Coordinate reflectionVertex = lineString.getCoordinateN(reflectionIndex);
        assertEquals(reflectionCut.getCoordinate().x, reflectionVertex.x, COORDINATE_TOLERANCE);
        assertEquals(reflectionCut.getCoordinate().y, reflectionVertex.y, COORDINATE_TOLERANCE);
        assertEquals(reflectionCut.getCoordinate().z, reflectionVertex.z, COORDINATE_TOLERANCE);

        LineSegment wall = new LineSegment(reflectionCut.wall.p0, reflectionCut.wall.p1);
        assertTrue(wall.distance(new Coordinate(reflectionVertex.x, reflectionVertex.y))
                        < COORDINATE_TOLERANCE,
                "The reflection vertex must lie on the reflection wall");
        double wallRatio = wall.projectionFactor(new Coordinate(reflectionVertex.x, reflectionVertex.y));
        double wallZ = wall.p0.z + wallRatio * (wall.p1.z - wall.p0.z);
        assertEquals(wallZ, reflectionVertex.z, COORDINATE_TOLERANCE,
                "The reflection height must be consistent with the wall");
    }

    @Test
    void directReflectionAndDiffractionPathsStayOrdered() throws IOException {
        String[] testCases = {"TC01_Direct", "TC26_Reflection", "TC08_Direct", "TC23_Direct"};
        for (String testCase : testCases) {
            URL resource = PathFinder.class.getResource("test_cases/" + testCase + ".json");
            CutProfile cutProfile = loadProfile(resource);
            CutProfile.PROFILE_TYPE profileType = cutProfile.profileType;
            CnossosPath path = computeHomogeneousPath(cutProfile);
            assertNotNull(path, "Path must be computed for " + testCase);
            assertPointsAreOrdered(path);
            long reflectionCount = path.getPointList().stream()
                    .filter(p -> p.type == PointPath.POINT_TYPE.REFL).count();
            long diffractionCount = path.getPointList().stream()
                    .filter(p -> p.type == PointPath.POINT_TYPE.DIFH
                            || p.type == PointPath.POINT_TYPE.DIFH_RCRIT
                            || p.type == PointPath.POINT_TYPE.DIFV)
                    .count();
            switch (testCase) {
                case "TC01_Direct":
                    assertEquals(0, reflectionCount, "TC01 must stay a direct path");
                    assertEquals(0, diffractionCount, "TC01 must stay a direct path");
                    break;
                case "TC26_Reflection":
                    assertEquals(1, reflectionCount, "TC26 must stay a reflection path");
                    assertEquals(0, diffractionCount, "TC26 must stay a reflection path");
                    break;
                default:
                    assertEquals(0, reflectionCount, testCase + " must stay a diffraction path");
                    assertTrue(diffractionCount > 0, testCase + " must stay a diffraction path");
                    break;
            }
            if (profileType == CutProfile.PROFILE_TYPE.DIRECT
                    || profileType == CutProfile.PROFILE_TYPE.REFLECTION) {
                assertLineStringHasNoBacktracking(path.asGeom());
            }
        }
    }

    @Test
    void mixedGroundAttenuationUsesTheObstacleAnchor() throws IOException {
        CutProfile cutProfile = loadProfile(
                MixedReflectionDiffractionPathTest.class.getResource(MIXED_PROFILE_RESOURCE));
        // Mixed ground coefficients so that choosing another cut point for the same
        // obstacle would change the gPath and the ground attenuation.
        for (int i = 0; i < cutProfile.cutPoints.size(); i++) {
            cutProfile.cutPoints.get(i).groundCoefficient = i < 20 ? 0.2 : (i < 29 ? 0.8 : 0.5);
        }
        // At 1000 Hz only the 308.37 m obstacle is selected. The cut points 29, 30 and
        // 31 (wall, ground effect and hidden topography) describe that same obstacle.
        CnossosPath path = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile,
                Arrays.asList(1000.0), cutProfile.getSource().groundCoefficient, false);
        assertNotNull(path);
        assertEquals(4, path.getPointList().size());
        assertEquals(2, path.getSegmentList().size(),
                "The duplicated obstacle cut points must not create extra segments");

        PointPath diffraction = path.getPointList().stream()
                .filter(p -> p.type == PointPath.POINT_TYPE.DIFH_RCRIT)
                .findFirst()
                .orElseThrow();
        int anchorIndex = findCutPointIndex(cutProfile, diffraction);
        assertEquals(cutProfile.getGPath(cutProfile.getSource(),
                        cutProfile.cutPoints.get(anchorIndex), 0.0),
                path.getSegmentList().get(0).gPath, 1e-9,
                "S->O must use the first cut point of the obstacle");
        assertEquals(cutProfile.getGPath(cutProfile.cutPoints.get(anchorIndex),
                        cutProfile.getReceiver(), 0.0),
                path.getSegmentList().get(1).gPath, 1e-9,
                "O->R must use the first cut point of the obstacle");

        ProfileBuilder profileBuilder = new ProfileBuilder().finishFeeding();
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        AttenuationParameters parameters = new AttenuationParameters(
                Arrays.asList(1000), Arrays.asList(1000.0), Arrays.asList(0.0));
        CnossosAttenuationOutput output = new CnossosAttenuationOutput(cutProfile);
        output.propagationPath = path;
        AttenuationCnossos.computeCnossosAttenuation(parameters, scene, output, true);
        assertEquals(-2.2219091447950743, output.aBoundary.aGroundSO[0], 1e-6);
        assertEquals(-1.8137505079371063, output.aBoundary.aGroundOR[0], 1e-6);
    }

    @Test
    void sortAndDedupeKeepsDifferentPointTypesAtTheSameProfilePosition() {
        List<PointPath> points = new ArrayList<>();
        points.add(new PointPath(new Coordinate(0, 50), 0, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(10, 40), 0, PointPath.POINT_TYPE.REFL));
        points.add(new PointPath(new Coordinate(20, 30), 0, PointPath.POINT_TYPE.RECV));
        points.add(new PointPath(new Coordinate(10, 40), 0, PointPath.POINT_TYPE.DIFH_RCRIT));
        points.add(new PointPath(new Coordinate(10, 40), 0, PointPath.POINT_TYPE.DIFH_RCRIT));
        points.add(new PointPath(new Coordinate(5, 45), 0, PointPath.POINT_TYPE.DIFH_RCRIT));

        CnossosPathBuilder.sortAndDedupePoints(points);

        assertEquals(5, points.size());
        assertEquals(List.of(0.0, 5.0, 10.0, 10.0, 20.0),
                points.stream().map(point -> point.coordinate.x).collect(Collectors.toList()));

        // Different point types at the same position are kept; same-type duplicates are not.
        List<PointPath.POINT_TYPE> typesAtTen = points.stream()
                .filter(point -> point.coordinate.x == 10)
                .map(point -> point.type)
                .collect(Collectors.toList());
        assertTrue(typesAtTen.contains(PointPath.POINT_TYPE.REFL));
        assertEquals(1, typesAtTen.stream()
                .filter(type -> type == PointPath.POINT_TYPE.DIFH_RCRIT).count());
    }
}
