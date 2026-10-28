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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Bridge;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.BridgePoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bridge's own side parapets should act as reflective walls, but only for a source that is
 * itself classified on that bridge's deck (ACTUAL_SOURCE_ON_BRIDGE), and only where the parapet
 * reaches {@link org.noise_planet.noisemodelling.pathfinder.profilebuilder.BridgeService#MIN_REFLECTIVE_BARRIER_HEIGHT}.
 * Found missing from the original bridge v0 feature (PR #1061).
 */
public class BridgeParapetReflectionTest {

    /** A straight, 5 m-wide deck at absolute altitude 10 m over x in [0,100], centreline y=20. */
    private static ProfileBuilder bridgeWithBarrierHeight(double barrierHeight) {
        List<Coordinate> pts = Arrays.asList(
                new Coordinate(0, 20, 10),
                new Coordinate(50, 20, 10),
                new Coordinate(100, 20, 10));
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < pts.size(); i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 100L, pts.get((int) i))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(5.0, 5.0)
                    .withBarrierHeight(barrierHeight, barrierHeight)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(100L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.STEEL)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -20; x <= 120; x += 20) {
            for (int y = -20; y <= 120; y += 20) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();
        return pb;
    }

    private static List<CutProfile> computeReflectionProfiles(ProfileBuilder pb, Coordinate source, Coordinate receiver) {
        GeometryFactory factory = new GeometryFactory();
        Scene inputData = new Scene(pb);
        inputData.addReceiver(receiver);
        inputData.addSource(factory.createPoint(source));
        inputData.setComputeHorizontalDiffraction(false);
        inputData.setComputeVerticalDiffraction(false);
        inputData.maxRefDist = 60;
        inputData.maxSrcDist = 100;
        inputData.setReflexionOrder(1);
        PathFinder computeRays = new PathFinder(inputData);
        computeRays.setThreadCount(1);

        Envelope receiverPropagationEnvelope = new Envelope(receiver);
        receiverPropagationEnvelope.expandBy(inputData.maxSrcDist);
        List<Wall> buildWalls = inputData.profileBuilder.getWallsIn(receiverPropagationEnvelope);
        MirrorReceiversCompute receiverMirrorIndex = new MirrorReceiversCompute(buildWalls, receiver,
                inputData.reflexionOrder, inputData.maxSrcDist, inputData.maxRefDist);

        DefaultCutPlaneVisitor defaultCutPlaneVisitor = new DefaultCutPlaneVisitor(true, inputData);
        computeRays.computeReflexion(new PathFinder.ReceiverPointInfo(1, 1, receiver),
                new PathFinder.SourcePointInfo(1, 1, source, 1.0, new Orientation()),
                receiverMirrorIndex, defaultCutPlaneVisitor, CutPlaneVisitor.PathSearchStrategy.CONTINUE);
        return new ArrayList<>(defaultCutPlaneVisitor.cutProfiles);
    }

    @Test
    public void onDeckSourceReflectsOffSufficientParapet() {
        ProfileBuilder pb = bridgeWithBarrierHeight(1.0); // above the 0.5 m threshold
        // Source and receiver both on the deck (0.6 m above the 10 m deck surface), straddling the
        // centreline, so the right-side parapet (y=22.5) is the shortest reflecting path between them.
        Coordinate source = new Coordinate(30, 20, 10.6);
        Coordinate receiver = new Coordinate(70, 20, 10.6);
        List<CutProfile> profiles = computeReflectionProfiles(pb, source, receiver);
        assertFalse(profiles.isEmpty(),
                "an on-deck source should reflect off the bridge's own sufficiently tall parapet");
    }

    @Test
    public void onDeckSourceDoesNotReflectOffParapetBelowThreshold() {
        ProfileBuilder pb = bridgeWithBarrierHeight(0.3); // below the 0.5 m threshold
        Coordinate source = new Coordinate(30, 20, 10.6);
        Coordinate receiver = new Coordinate(70, 20, 10.6);
        List<CutProfile> profiles = computeReflectionProfiles(pb, source, receiver);
        assertTrue(profiles.isEmpty(),
                "a parapet below the reflective-height threshold must not be registered as a reflector");
    }

    @Test
    public void offDeckSourceDoesNotReflectOffBridgeParapet() {
        ProfileBuilder pb = bridgeWithBarrierHeight(1.0); // above the threshold
        // Same X/Y as the on-deck case, but well below the deck (ground level, unrelated to the bridge).
        Coordinate source = new Coordinate(30, 20, 2.0);
        Coordinate receiver = new Coordinate(70, 20, 2.0);
        List<CutProfile> profiles = computeReflectionProfiles(pb, source, receiver);
        assertTrue(profiles.isEmpty(),
                "a source that is not itself on the bridge must not reflect off its parapet");
    }

    @Test
    public void onlyContinuousSideSegmentsAreRegisteredAsReflectiveWalls() {
        ProfileBuilder pb = bridgeWithBarrierHeight(1.0);
        List<Wall> bridgeWalls = pb.getWallsIn(new Envelope(-50, 150, -50, 150)).stream()
                .filter(w -> w.type == ProfileBuilder.IntersectionType.BRIDGE)
                .collect(Collectors.toList());
        // 3 cross-sections -> 2 segments per side -> 4 total; none spans the 5 m deck width, which
        // would indicate an accidental end-cap connector between the two sides.
        assertEquals(4, bridgeWalls.size(), "expected one wall per side per inter-cross-section gap");
        for (Wall w : bridgeWalls) {
            double length = w.line.p0.distance(w.line.p1);
            assertTrue(length > 10, "expected a long side segment (~50 m), got " + length);
        }
    }
}
