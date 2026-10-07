/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReflection;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointTopography;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.PointPath;
import org.noise_planet.noisemodelling.propagation.cnossos.SegmentPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the Rayleigh diffraction computation applied to reflected paths.
 * <p>
 * A terrain obstacle located between the reflection point and the receiver (or between the source
 * and the reflection point) is not necessarily visible from the direct source-receiver line, so it
 * was previously ignored. The diffraction must be computed on each real sub segment of the path.
 */
public class TestRayleighReflectionDiffraction {

    /**
     * Source -> reflection -> receiver profile. The obstacle is below the direct source-receiver
     * line but above the reflection -> receiver segment.
     */
    private static CutProfile buildReflectionProfileWithObstacle() {
        CutProfile cutProfile = new CutProfile();
        cutProfile.setProfileType(CutProfile.PROFILE_TYPE.REFLECTION);

        CutPointSource source = new CutPointSource(new Coordinate(0, 0, 1));
        source.setZGround(0);
        source.setGroundCoefficient(0);
        cutProfile.cutPoints.add(source);

        CutPoint reflectionBase = new CutPoint(new Coordinate(40, 0, 2), 0, 0);
        CutPointReflection reflection = new CutPointReflection(reflectionBase,
                new LineSegment(new Coordinate(40, -5, 2), new Coordinate(40, 5, 10)),
                Arrays.asList(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1));
        cutProfile.cutPoints.add(reflection);

        // Terrain obstacle: the direct S-R line passes well above it (so the grazing Rayleigh
        // check on the direct line does not trigger), but it blocks the reflection leg.
        CutPointTopography obstacle = new CutPointTopography(new Coordinate(45, 0, 6));
        obstacle.setGroundCoefficient(0);
        cutProfile.cutPoints.add(obstacle);

        CutPointReceiver receiver = new CutPointReceiver(new Coordinate(100, 0, 21));
        receiver.setZGround(0);
        receiver.setGroundCoefficient(0);
        cutProfile.cutPoints.add(receiver);
        return cutProfile;
    }

    @Test
    public void reflectionSubSegmentProducesRayleighDiffraction() {
        CutProfile cutProfile = buildReflectionProfileWithObstacle();
        List<Integer> cut2DGroundIndex = new ArrayList<>();
        List<Coordinate> pts2D = cutProfile.computePts2D();
        Coordinate[] pts2DGround = cutProfile.computePts2DGround(cut2DGroundIndex).toArray(new Coordinate[0]);
        CnossosPath path = new CnossosPath(cutProfile);
        path.setFavourable(false);

        List<PointPath> points = new ArrayList<>();
        List<SegmentPath> segments = new ArrayList<>();
        List<Double> frequencies = new ProfileBuilder().exactFrequencyArray;

        int reflectionIndex = 1;
        int receiverIndex = cutProfile.cutPoints.size() - 1;
        CnossosPathBuilder.computeRayleighDiff(cutProfile, path, reflectionIndex, receiverIndex,
                points, segments, pts2D, pts2DGround, cut2DGroundIndex, frequencies);

        assertEquals(1, points.size(), "One terrain diffraction point is expected on the reflection leg");
        assertEquals(PointPath.POINT_TYPE.DIFH_RCRIT, points.getFirst().type);
        assertEquals(2, segments.size(), "The reflection leg must be split in two segments at the obstacle");
        assertTrue(path.delta > 0, "The path difference of the diffraction must be positive");
    }

    @Test
    public void reflectionPathWithObstacleOnReceiverLeg() {
        CutProfile cutProfile = buildReflectionProfileWithObstacle();
        CnossosPath path = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, false,
                new ProfileBuilder().exactFrequencyArray, 0.0, false);

        assertNotNull(path);
        assertEquals(CutProfile.PROFILE_TYPE.REFLECTION, path.getCutProfile().getProfileType());
        assertTrue(path.getPointList().stream()
                        .anyMatch(point -> point.type == PointPath.POINT_TYPE.DIFH_RCRIT),
                "A terrain diffraction point must be added on the reflection -> receiver leg");
        assertTrue(path.delta > 0, "The path difference of the diffraction must be positive");
    }
}
