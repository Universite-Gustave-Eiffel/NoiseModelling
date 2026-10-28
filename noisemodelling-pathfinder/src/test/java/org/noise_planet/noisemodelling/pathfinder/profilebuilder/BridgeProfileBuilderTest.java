/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.path.BridgeRelationship;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that a bridge deck registered on a {@link ProfileBuilder} is used by
 * {@link ProfileBuilder#getProfile(Coordinate, Coordinate)}: the source is classified against the
 * deck, and a source that sits on the deck yields {@link CutPointBridgeWall} cut points where the
 * ray leaves the structure.
 */
public class BridgeProfileBuilderTest {

    /** A straight, horizontal deck at absolute altitude 10 m over the strip y in [15, 25], x in [0, 100]. */
    private static ProfileBuilder builderWithDeck() {
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
                    .withBarrierHeight(1.0, 1.0)
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
        for (int x = 0; x <= 100; x += 50) {
            for (int y = 0; y <= 100; y += 50) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();
        return pb;
    }

    private static long countBridgeCutPoints(CutProfile profile) {
        return profile.getCutPoints().stream().filter(p -> p instanceof CutPointBridgeWall).count();
    }

    /** deck A (pk 100) at y in [15,25], z 10; deck B (pk 200) at y in [55,65], z 6. */
    private static ProfileBuilder builderWithTwoDecks() {
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = 0; x <= 100; x += 50) {
            for (int y = -20; y <= 120; y += 40) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(deck(100L, 20.0, 10.0));
        pb.addBridge(deck(200L, 60.0, 6.0));
        pb.finishFeeding();
        return pb;
    }

    private static Bridge deck(long pk, double centreY, double deckZ) {
        List<BridgePoint> pts = new ArrayList<>();
        for (long i = 0; i < 3; i++) {
            pts.add(new BridgePoint.Builder(i, pk, new Coordinate(i * 50, centreY, deckZ))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5).withWidth(5.0, 5.0).withBarrierHeight(1.0, 1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX).withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        return new Bridge.Builder(pts).setPrimaryKey(pk)
                .setGirderType(Bridge.GirderType.STEEL_BOX).setSlabType(Bridge.SlabType.STEEL).build();
    }

    @Test
    public void sourceOnOneDeckIsRelatedToThatDeckOnly() {
        ProfileBuilder pb = builderWithTwoDecks();
        assertEquals(2, pb.getBridgeCount());
        // source on deck A, receiver on the ground past deck B: the ray crosses both decks
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 10.6), new Coordinate(50, 90, 1.5));
        assertEquals(BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());
        assertEquals(100L, profile.getSource().getBridgeRelationship().getBridgePkOn(),
                "the source is on deck A (pk 100), not deck B");
        // both decks leave bridge cut points where the ray crosses them
        assertTrue(countBridgeCutPoints(profile) >= 2,
                "the ray crosses two decks, got " + countBridgeCutPoints(profile) + " bridge cut points");
    }

    @Test
    public void deckIsRegistered() {
        ProfileBuilder pb = builderWithDeck();
        assertTrue(pb.hasBridges());
        assertEquals(1, pb.getBridgeCount());
    }

    @Test
    public void sourceOnDeckIsClassifiedAsOnBridge() {
        ProfileBuilder pb = builderWithDeck();
        // source on the deck centreline, just above the deck top; receiver on the ground beyond the deck
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 10.6), new Coordinate(50, 60, 1.5));
        assertEquals(BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());
    }

    @Test
    public void sourceOnDeckYieldsBridgeCutPoints() {
        ProfileBuilder pb = builderWithDeck();
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 10.6), new Coordinate(50, 60, 1.5));
        assertTrue(countBridgeCutPoints(profile) > 0,
                "a source on the deck should produce a CutPointBridgeWall where the ray leaves the structure");
    }

    @Test
    public void sourceOnDeckSitsAtDeckLevelInTheGroundProfile() {
        ProfileBuilder pb = builderWithDeck();
        // source on the deck (deck top = 10 m), ground receiver beyond the deck edge
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 10.6), new Coordinate(50, 60, 1.5));
        List<Coordinate> ground = CutProfile.computePtsGround(profile.getCutPoints(), null);
        assertTrue(ground.size() >= 2);
        // the ground reference under the source is the deck surface, ~10 m, not the terrain at 0
        assertEquals(10.0, ground.get(0).z, 0.5,
                "on-deck source should sit on the deck surface in the elevation profile");
        // once the ray has left the deck the reference drops back to the terrain
        assertEquals(0.0, ground.get(ground.size() - 1).z, 0.5,
                "past the deck edge the ground profile returns to the terrain");
    }

    @Test
    public void groundProfileUnchangedWithoutAnyBridge() {
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = 0; x <= 100; x += 50) {
            for (int y = 0; y <= 100; y += 50) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.finishFeeding();
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 2.0), new Coordinate(50, 60, 1.5));
        List<Coordinate> ground = CutProfile.computePtsGround(profile.getCutPoints(), null);
        for (Coordinate c : ground) {
            assertEquals(0.0, c.z, 1e-9, "flat terrain, no bridge: every ground point stays at 0");
        }
    }

    @Test
    public void sourceBelowTheDeckUndersideIsClassifiedAsUnderBridgeNotOnIt() {
        ProfileBuilder pb = builderWithDeck();
        // deck top = 10 m, thickness = 0.5 m -> underside = 9.5 m. A source at 8.5 m is a full
        // metre below the underside (clear air), yet still within the +/-2 m "on bridge" tolerance
        // measured from the deck TOP (used for structural-coupling sources sitting on the deck
        // surface), so it must not be classified as on the deck.
        CutProfile profile = pb.getProfile(new Coordinate(50, 20, 8.5), new Coordinate(50, 60, 1.5));
        assertEquals(BridgeRelationship.RelationType.IMAGINARY_SOURCE_UNDER_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType(),
                "a source clearly below the deck underside must be classified as under the bridge, not on it");
    }

    @Test
    public void bentDeckDoesNotReportAHeightInTheGapInsideTheBend() {
        // An L-shaped (90-degree bend) deck. The point cloud of left/right edge points has a
        // convex hull that cuts across the inside of the bend, covering area that is not actually
        // part of the (concave, from the outside) deck outline.
        List<Coordinate> pts = Arrays.asList(
                new Coordinate(0, 0, 10),
                new Coordinate(50, 0, 10),
                new Coordinate(50, 50, 10));
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < pts.size(); i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 1L, pts.get((int) i))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(5.0, 5.0)
                    .withBarrierHeight(1.0, 1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.STEEL)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -20; x <= 80; x += 20) {
            for (int y = -20; y <= 80; y += 20) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        // just inside the bend, well within the convex hull of all edge points, but outside the
        // deck's own (narrow) footprint
        Coordinate insideTheBendGap = new Coordinate(43, 20);
        assertFalse(bridge.isPointWithinBridgeFootprint(insideTheBendGap),
                "sanity check: this point should not be on the deck itself");
        assertTrue(Double.isNaN(bridge.getDeckHeightAtPoint(insideTheBendGap)),
                "a point outside the deck's own footprint must not get an interpolated deck height, "
                        + "even if it falls inside the edge points' convex hull");
    }

    @Test
    public void mergeFactorEscalatesWhenTheInitialFactorIsNotEnoughToCloseTheGap() {
        // width=5 -> initial threshold (factor 0.1) = 0.5m. This near-duplicate spacing (~0.67m)
        // is deliberately above that (so attempt 1 must NOT merge, leaving a gap) but below factor
        // 0.2's threshold (1.0m), so escalation must kick in and fix it on attempt 2.
        List<Coordinate> centreline = Arrays.asList(
                new Coordinate(0, 0, 10),
                new Coordinate(50, 0, 10),
                new Coordinate(50.6, 0.3, 10),
                new Coordinate(50.3, 25, 10),
                new Coordinate(50, 50, 10));
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < centreline.size(); i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 1L, centreline.get((int) i))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(5.0, 5.0)
                    .withBarrierHeight(1.0, 1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.STEEL)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -50; x <= 100; x += 25) {
            for (int y = -50; y <= 100; y += 25) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        int gaps = 0, samples = 0;
        for (double x = -10; x <= 60; x += 0.5) {
            for (double y = -10; y <= 60; y += 0.5) {
                Coordinate c = new Coordinate(x, y);
                if (bridge.isPointWithinBridgeFootprint(c)) {
                    samples++;
                    if (Double.isNaN(bridge.getDeckHeightAtPoint(c))) {
                        gaps++;
                    }
                }
            }
        }
        assertTrue(gaps == 0, "found " + gaps + "/" + samples + " gaps - escalation should have fixed this");
    }

    @Test
    public void nearDuplicateCrossSectionAtASharpBendDoesNotLeaveAHeightGap() {
        // A deck whose cross-sections turn sharply (east, then north) via a near-duplicate pair
        // of points (50,0) / (50.2,0.1) just 0.22 m apart, and again at (50,80) / (50.05,80.02).
        // This is a common real-world digitising artifact. Naively, the direction used to offset
        // each cross-section to its left/right edge is estimated from its neighbours on both
        // sides; right at a near-duplicate pair that straddles a sharp turn, the two points get
        // edge offsets in wildly different directions (one still "facing" the incoming leg, the
        // other the outgoing leg), leaving the triangulation unable to cover the sliver of deck
        // between them.
        List<Coordinate> centreline = Arrays.asList(
                new Coordinate(0, 0, 10),
                new Coordinate(50, 0, 10),
                new Coordinate(50.2, 0.1, 10),
                new Coordinate(50.3, 25, 10),
                new Coordinate(50, 50, 10),
                new Coordinate(50, 80, 10),
                new Coordinate(50.05, 80.02, 10),
                new Coordinate(50, 120, 10));
        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < centreline.size(); i++) {
            bridgePoints.add(new BridgePoint.Builder(i, 1L, centreline.get((int) i))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(5.0, 5.0)
                    .withBarrierHeight(1.0, 1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.STEEL)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -50; x <= 100; x += 25) {
            for (int y = -50; y <= 150; y += 25) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        // Sample densely along the deck's own edge polygon (what calculateFirstBridgeCutpoint
        // walks when looking for a diffraction point) and check every point that is within the
        // bridge footprint (per the inclusive covers()-based check) for a NaN height - a
        // coverage gap left by an unstable direction estimate near the sharp bend.
        Coordinate[] edgeCoords = bridge.getEdge().getCoordinates();
        int gapCount = 0;
        int sampleCount = 0;
        for (int i = 0; i < edgeCoords.length - 1; i++) {
            Coordinate a = edgeCoords[i];
            Coordinate b = edgeCoords[i + 1];
            int steps = 20;
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                Coordinate p = new Coordinate(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
                sampleCount++;
                if (bridge.isPointWithinBridgeFootprint(p) && Double.isNaN(bridge.getDeckHeightAtPoint(p))) {
                    gapCount++;
                }
            }
        }
        assertTrue(gapCount == 0, "found " + gapCount + "/" + sampleCount
                + " points on the deck's own edge that are within the footprint but get NaN height");

        // Also probe a dense grid over the whole footprint envelope, not just the edge polyline.
        int interiorGap = 0;
        int interiorSample = 0;
        for (double x = -10; x <= 60; x += 0.5) {
            for (double y = -10; y <= 130; y += 0.5) {
                Coordinate p = new Coordinate(x, y);
                if (bridge.isPointWithinBridgeFootprint(p)) {
                    interiorSample++;
                    if (Double.isNaN(bridge.getDeckHeightAtPoint(p))) {
                        interiorGap++;
                    }
                }
            }
        }
        assertTrue(interiorGap == 0, "found " + interiorGap + "/" + interiorSample
                + " points within the footprint (dense grid) that get NaN height");
    }

    @Test
    public void denseRoadSamplingOnAStraightRealWorldDeckFindsNoHeightGap() {
        // Reproduces the geometry from PR #1061's bridge report (BRIDGE_PK=1 in the reviewer's
        // bridgepoints.geojson): 38 real cross-sections, ~10 m apart, forming a perfectly straight
        // deck (no bend, no near-duplicate points) - ruling out the two known coverage-gap causes
        // already covered by the other tests in this file. Road sources are sampled every ~1.85 m
        // (27 points per 50 m, as in the reviewer's LW_ROADS), which does not line up with the 10 m
        // cross-section spacing, to check whether that misalignment alone can still land a sample
        // point in an uncovered sliver of the triangulation.
        double[][] crossSections = {
                {-770895.605, -1137437.618, 392.697}, {-770901.754, -1137445.504, 392.740},
                {-770907.903, -1137453.390, 392.784}, {-770914.053, -1137461.276, 392.827},
                {-770920.202, -1137469.162, 392.871}, {-770926.351, -1137477.047, 392.914},
                {-770932.500, -1137484.933, 392.958}, {-770938.649, -1137492.819, 393.001},
                {-770944.799, -1137500.705, 393.045}, {-770950.948, -1137508.591, 393.088},
                {-770957.097, -1137516.477, 393.131}, {-770963.246, -1137524.363, 393.175},
                {-770969.395, -1137532.249, 393.218}, {-770975.545, -1137540.135, 393.262},
                {-770981.694, -1137548.020, 393.305}, {-770987.843, -1137555.906, 393.349},
                {-770993.992, -1137563.792, 393.392}, {-771000.142, -1137571.678, 393.436},
                {-771006.291, -1137579.564, 393.479}, {-771012.440, -1137587.450, 393.522},
                {-771018.589, -1137595.336, 393.566}, {-771024.738, -1137603.222, 393.609},
                {-771030.888, -1137611.108, 393.653}, {-771037.037, -1137618.993, 393.696},
                {-771043.186, -1137626.879, 393.740}, {-771049.335, -1137634.765, 393.783},
                {-771055.484, -1137642.651, 393.827}, {-771061.634, -1137650.537, 393.870},
                {-771067.783, -1137658.423, 393.914}, {-771073.932, -1137666.309, 393.957},
                {-771080.081, -1137674.195, 394.000}, {-771086.230, -1137682.081, 394.044},
                {-771092.380, -1137689.966, 394.087}, {-771098.529, -1137697.852, 394.131},
                {-771104.678, -1137705.738, 394.174}, {-771110.827, -1137713.624, 394.218},
                {-771116.976, -1137721.510, 394.261}, {-771121.542, -1137727.365, 394.293}};

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < crossSections.length; i++) {
            double[] cs = crossSections[(int) i];
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(cs[0], cs[1], cs[2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -771200; x <= -770800; x += 40) {
            for (int y = -1137800; y <= -1137400; y += 40) {
                pb.addTopographicPoint(new Coordinate(x, y, 390.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        double[] segLengths = new double[crossSections.length - 1];
        double totalLength = 0;
        for (int i = 0; i < segLengths.length; i++) {
            double dx = crossSections[i + 1][0] - crossSections[i][0];
            double dy = crossSections[i + 1][1] - crossSections[i][1];
            segLengths[i] = Math.sqrt(dx * dx + dy * dy);
            totalLength += segLengths[i];
        }

        // 27 vertices per 50 m, as reported for the reviewer's LW_ROADS.
        double step = 50.0 / 27.0;
        int gapCount = 0;
        int sampleCount = 0;
        List<Coordinate> gapCoordinates = new ArrayList<>();
        for (double dist = 0; dist <= totalLength; dist += step) {
            double cum = 0;
            for (int i = 0; i < segLengths.length; i++) {
                boolean lastSegment = i == segLengths.length - 1;
                if (dist <= cum + segLengths[i] || lastSegment) {
                    double t = segLengths[i] == 0 ? 0 : (dist - cum) / segLengths[i];
                    t = Math.max(0, Math.min(1, t));
                    double x = crossSections[i][0] + (crossSections[i + 1][0] - crossSections[i][0]) * t;
                    double y = crossSections[i][1] + (crossSections[i + 1][1] - crossSections[i][1]) * t;
                    Coordinate sample = new Coordinate(x, y);
                    sampleCount++;
                    if (bridge.isPointWithinBridgeFootprint(sample) && Double.isNaN(bridge.getDeckHeightAtPoint(sample))) {
                        gapCount++;
                        gapCoordinates.add(sample);
                    }
                    break;
                }
                cum += segLengths[i];
            }
        }
        assertTrue(gapCount == 0, "found " + gapCount + "/" + sampleCount
                + " road sample points (spaced ~1.85 m along the centreline) within the footprint "
                + "that get a NaN deck height: " + gapCoordinates);
    }

    @Test
    public void denseRoadSamplingNearTheDeckShoulderFindsNoHeightGap() {
        // Same straight, real-world deck as denseRoadSamplingOnAStraightRealWorldDeckFindsNoHeightGap,
        // but sampled 6.49 m off the centreline (right at the inner edge of the +/-6.5 m deck width,
        // where a shoulder lane would sit) instead of on the centreline itself, to check whether the
        // coverage gap shows up only near the deck's lateral boundary rather than down its middle.
        double[][] crossSections = {
                {-770895.605, -1137437.618, 392.697}, {-770901.754, -1137445.504, 392.740},
                {-770907.903, -1137453.390, 392.784}, {-770914.053, -1137461.276, 392.827},
                {-770920.202, -1137469.162, 392.871}, {-770926.351, -1137477.047, 392.914},
                {-770932.500, -1137484.933, 392.958}, {-770938.649, -1137492.819, 393.001},
                {-770944.799, -1137500.705, 393.045}, {-770950.948, -1137508.591, 393.088},
                {-770957.097, -1137516.477, 393.131}, {-770963.246, -1137524.363, 393.175},
                {-770969.395, -1137532.249, 393.218}, {-770975.545, -1137540.135, 393.262},
                {-770981.694, -1137548.020, 393.305}, {-770987.843, -1137555.906, 393.349},
                {-770993.992, -1137563.792, 393.392}, {-771000.142, -1137571.678, 393.436},
                {-771006.291, -1137579.564, 393.479}, {-771012.440, -1137587.450, 393.522},
                {-771018.589, -1137595.336, 393.566}, {-771024.738, -1137603.222, 393.609},
                {-771030.888, -1137611.108, 393.653}, {-771037.037, -1137618.993, 393.696},
                {-771043.186, -1137626.879, 393.740}, {-771049.335, -1137634.765, 393.783},
                {-771055.484, -1137642.651, 393.827}, {-771061.634, -1137650.537, 393.870},
                {-771067.783, -1137658.423, 393.914}, {-771073.932, -1137666.309, 393.957},
                {-771080.081, -1137674.195, 394.000}, {-771086.230, -1137682.081, 394.044},
                {-771092.380, -1137689.966, 394.087}, {-771098.529, -1137697.852, 394.131},
                {-771104.678, -1137705.738, 394.174}, {-771110.827, -1137713.624, 394.218},
                {-771116.976, -1137721.510, 394.261}, {-771121.542, -1137727.365, 394.293}};

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < crossSections.length; i++) {
            double[] cs = crossSections[(int) i];
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(cs[0], cs[1], cs[2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -771200; x <= -770800; x += 40) {
            for (int y = -1137800; y <= -1137400; y += 40) {
                pb.addTopographicPoint(new Coordinate(x, y, 390.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        double[] segLengths = new double[crossSections.length - 1];
        double totalLength = 0;
        for (int i = 0; i < segLengths.length; i++) {
            double dx = crossSections[i + 1][0] - crossSections[i][0];
            double dy = crossSections[i + 1][1] - crossSections[i][1];
            segLengths[i] = Math.sqrt(dx * dx + dy * dy);
            totalLength += segLengths[i];
        }

        double step = 50.0 / 27.0;
        double shoulderOffset = 6.49;
        int gapCount = 0;
        int sampleCount = 0;
        List<Coordinate> gapCoordinates = new ArrayList<>();
        for (double dist = 0; dist <= totalLength; dist += step) {
            double cum = 0;
            for (int i = 0; i < segLengths.length; i++) {
                boolean lastSegment = i == segLengths.length - 1;
                if (dist <= cum + segLengths[i] || lastSegment) {
                    double t = segLengths[i] == 0 ? 0 : (dist - cum) / segLengths[i];
                    t = Math.max(0, Math.min(1, t));
                    double x = crossSections[i][0] + (crossSections[i + 1][0] - crossSections[i][0]) * t;
                    double y = crossSections[i][1] + (crossSections[i + 1][1] - crossSections[i][1]) * t;
                    double dx = crossSections[i + 1][0] - crossSections[i][0];
                    double dy = crossSections[i + 1][1] - crossSections[i][1];
                    double len = Math.sqrt(dx * dx + dy * dy);
                    double nx = -dy / len;
                    double ny = dx / len;
                    for (int side = -1; side <= 1; side += 2) {
                        Coordinate sample = new Coordinate(x + nx * shoulderOffset * side, y + ny * shoulderOffset * side);
                        sampleCount++;
                        if (bridge.isPointWithinBridgeFootprint(sample) && Double.isNaN(bridge.getDeckHeightAtPoint(sample))) {
                            gapCount++;
                            gapCoordinates.add(sample);
                        }
                    }
                    break;
                }
                cum += segLengths[i];
            }
        }
        assertTrue(gapCount == 0, "found " + gapCount + "/" + sampleCount
                + " road sample points (spaced ~1.85 m, 6.49 m off centre on either side) within the "
                + "footprint that get a NaN deck height: " + gapCoordinates);
    }

    @Test
    public void realLwRoadsPointsOnPr1061BridgeFindNoHeightGap() {
        // Reproduces PR #1061's actual report: the 38 real cross-sections of BRIDGE_PK=1 from the
        // reviewer's bridgepoints.geojson, probed with the 191 real LW_ROADS vertices (PK 25-32,
        // BRIDGE_PK=1) from the reviewer's own LW_ROADS.geojson - the exact road geometry that
        // produced the over-loud under-deck receivers - instead of a synthetic sampling along the
        // bridge's own centreline.
        double[][] crossSections = {
                {-770895.605, -1137437.618, 392.697}, {-770901.754, -1137445.504, 392.740},
                {-770907.903, -1137453.390, 392.784}, {-770914.053, -1137461.276, 392.827},
                {-770920.202, -1137469.162, 392.871}, {-770926.351, -1137477.047, 392.914},
                {-770932.500, -1137484.933, 392.958}, {-770938.649, -1137492.819, 393.001},
                {-770944.799, -1137500.705, 393.045}, {-770950.948, -1137508.591, 393.088},
                {-770957.097, -1137516.477, 393.131}, {-770963.246, -1137524.363, 393.175},
                {-770969.395, -1137532.249, 393.218}, {-770975.545, -1137540.135, 393.262},
                {-770981.694, -1137548.020, 393.305}, {-770987.843, -1137555.906, 393.349},
                {-770993.992, -1137563.792, 393.392}, {-771000.142, -1137571.678, 393.436},
                {-771006.291, -1137579.564, 393.479}, {-771012.440, -1137587.450, 393.522},
                {-771018.589, -1137595.336, 393.566}, {-771024.738, -1137603.222, 393.609},
                {-771030.888, -1137611.108, 393.653}, {-771037.037, -1137618.993, 393.696},
                {-771043.186, -1137626.879, 393.740}, {-771049.335, -1137634.765, 393.783},
                {-771055.484, -1137642.651, 393.827}, {-771061.634, -1137650.537, 393.870},
                {-771067.783, -1137658.423, 393.914}, {-771073.932, -1137666.309, 393.957},
                {-771080.081, -1137674.195, 394.000}, {-771086.230, -1137682.081, 394.044},
                {-771092.380, -1137689.966, 394.087}, {-771098.529, -1137697.852, 394.131},
                {-771104.678, -1137705.738, 394.174}, {-771110.827, -1137713.624, 394.218},
                {-771116.976, -1137721.510, 394.261}, {-771121.542, -1137727.365, 394.293}};

        // LW_ROADS.geojson, features PK 25-32 (BRIDGE_PK=1), coordinates deduplicated across
        // adjacent features' shared endpoints.
        double[][] roadPoints = {
                {-770896.2075053671, -1137438.3906861336}, {-770897.446, -1137439.979},
                {-770898.673, -1137441.552}, {-770899.9, -1137443.126}, {-770901.127, -1137444.699},
                {-770902.353, -1137446.272}, {-770903.58, -1137447.846}, {-770904.807, -1137449.419},
                {-770906.034, -1137450.993}, {-770907.261, -1137452.566}, {-770908.488, -1137454.14},
                {-770909.715, -1137455.713}, {-770910.942, -1137457.287}, {-770912.169, -1137458.86},
                {-770913.396, -1137460.434}, {-770914.623, -1137462.007}, {-770915.85, -1137463.58},
                {-770917.077, -1137465.154}, {-770918.304, -1137466.727}, {-770919.531, -1137468.301},
                {-770920.758, -1137469.874}, {-770921.985, -1137471.448}, {-770923.211, -1137473.021},
                {-770924.438, -1137474.595}, {-770925.665, -1137476.168}, {-770926.892, -1137477.742},
                {-770926.964, -1137477.835}, {-770928.119, -1137479.315}, {-770929.346, -1137480.888},
                {-770930.573, -1137482.462}, {-770931.8, -1137484.035}, {-770933.027, -1137485.609},
                {-770934.254, -1137487.182}, {-770935.481, -1137488.756}, {-770936.708, -1137490.329},
                {-770937.935, -1137491.903}, {-770939.162, -1137493.476}, {-770940.389, -1137495.05},
                {-770941.616, -1137496.623}, {-770942.843, -1137498.197}, {-770944.069, -1137499.77},
                {-770945.296, -1137501.343}, {-770946.523, -1137502.917}, {-770947.75, -1137504.49},
                {-770948.977, -1137506.064}, {-770950.204, -1137507.637}, {-770951.431, -1137509.211},
                {-770952.658, -1137510.784}, {-770953.885, -1137512.358}, {-770955.112, -1137513.931},
                {-770956.339, -1137515.505}, {-770957.566, -1137517.078}, {-770957.711, -1137517.264},
                {-770958.793, -1137518.651}, {-770960.02, -1137520.225}, {-770961.247, -1137521.798},
                {-770962.474, -1137523.372}, {-770963.7, -1137524.945}, {-770964.927, -1137526.519},
                {-770966.154, -1137528.092}, {-770967.381, -1137529.666}, {-770968.608, -1137531.239},
                {-770969.835, -1137532.813}, {-770971.062, -1137534.386}, {-770972.289, -1137535.959},
                {-770973.516, -1137537.533}, {-770974.743, -1137539.106}, {-770975.97, -1137540.68},
                {-770977.197, -1137542.253}, {-770978.424, -1137543.827}, {-770979.651, -1137545.4},
                {-770980.878, -1137546.974}, {-770982.105, -1137548.547}, {-770983.332, -1137550.121},
                {-770984.558, -1137551.694}, {-770985.785, -1137553.268}, {-770987.012, -1137554.841},
                {-770988.239, -1137556.414}, {-770988.457, -1137556.694}, {-770989.466, -1137557.988},
                {-770990.693, -1137559.561}, {-770991.92, -1137561.135}, {-770993.147, -1137562.708},
                {-770994.374, -1137564.282}, {-770995.601, -1137565.855}, {-770996.828, -1137567.429},
                {-770998.055, -1137569.002}, {-770999.282, -1137570.576}, {-771000.509, -1137572.149},
                {-771001.736, -1137573.722}, {-771002.963, -1137575.296}, {-771004.19, -1137576.869},
                {-771005.416, -1137578.443}, {-771006.643, -1137580.016}, {-771007.87, -1137581.59},
                {-771009.097, -1137583.163}, {-771010.324, -1137584.737}, {-771011.551, -1137586.31},
                {-771012.778, -1137587.884}, {-771014.005, -1137589.457}, {-771015.232, -1137591.03},
                {-771016.459, -1137592.604}, {-771017.686, -1137594.177}, {-771018.913, -1137595.751},
                {-771019.203, -1137596.123}, {-771020.14, -1137597.324}, {-771021.367, -1137598.898},
                {-771022.594, -1137600.471}, {-771023.821, -1137602.045}, {-771025.047, -1137603.618},
                {-771026.274, -1137605.192}, {-771027.501, -1137606.765}, {-771028.728, -1137608.339},
                {-771029.955, -1137609.912}, {-771031.182, -1137611.485}, {-771032.409, -1137613.059},
                {-771033.636, -1137614.632}, {-771034.863, -1137616.206}, {-771036.09, -1137617.779},
                {-771037.317, -1137619.353}, {-771038.544, -1137620.926}, {-771039.771, -1137622.5},
                {-771040.998, -1137624.073}, {-771042.225, -1137625.647}, {-771043.452, -1137627.22},
                {-771044.679, -1137628.793}, {-771045.905, -1137630.367}, {-771047.132, -1137631.94},
                {-771048.359, -1137633.514}, {-771049.586, -1137635.087}, {-771049.949, -1137635.552},
                {-771050.813, -1137636.661}, {-771052.04, -1137638.234}, {-771053.267, -1137639.808},
                {-771054.494, -1137641.381}, {-771055.721, -1137642.955}, {-771056.948, -1137644.528},
                {-771058.175, -1137646.101}, {-771059.402, -1137647.675}, {-771060.629, -1137649.248},
                {-771061.856, -1137650.822}, {-771063.083, -1137652.395}, {-771064.31, -1137653.969},
                {-771065.537, -1137655.542}, {-771066.763, -1137657.116}, {-771067.99, -1137658.689},
                {-771069.217, -1137660.263}, {-771070.444, -1137661.836}, {-771071.671, -1137663.41},
                {-771072.898, -1137664.983}, {-771074.125, -1137666.556}, {-771075.352, -1137668.13},
                {-771076.579, -1137669.703}, {-771077.806, -1137671.277}, {-771079.033, -1137672.85},
                {-771080.26, -1137674.424}, {-771080.695, -1137674.982}, {-771081.487, -1137675.997},
                {-771082.714, -1137677.571}, {-771083.941, -1137679.144}, {-771085.168, -1137680.718},
                {-771086.395, -1137682.291}, {-771087.621, -1137683.864}, {-771088.848, -1137685.438},
                {-771090.075, -1137687.011}, {-771091.302, -1137688.585}, {-771092.529, -1137690.158},
                {-771093.756, -1137691.732}, {-771094.983, -1137693.305}, {-771096.21, -1137694.879},
                {-771097.437, -1137696.452}, {-771098.664, -1137698.026}, {-771099.891, -1137699.599},
                {-771101.118, -1137701.172}, {-771102.345, -1137702.746}, {-771103.572, -1137704.319},
                {-771104.799, -1137705.893}, {-771106.026, -1137707.466}, {-771107.252, -1137709.04},
                {-771108.479, -1137710.613}, {-771109.706, -1137712.187}, {-771110.933, -1137713.76},
                {-771111.441, -1137714.411}, {-771112.16, -1137715.334}, {-771113.387, -1137716.907},
                {-771114.614, -1137718.481}, {-771115.841, -1137720.054}, {-771117.068, -1137721.627},
                {-771118.295, -1137723.201}, {-771119.522, -1137724.774}, {-771121.0172688751, -1137726.6919414136}};

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < crossSections.length; i++) {
            double[] cs = crossSections[(int) i];
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(cs[0], cs[1], cs[2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -771200; x <= -770800; x += 40) {
            for (int y = -1137800; y <= -1137400; y += 40) {
                pb.addTopographicPoint(new Coordinate(x, y, 390.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        int gapCount = 0;
        int outsideFootprintCount = 0;
        List<Coordinate> gapCoordinates = new ArrayList<>();
        for (double[] rp : roadPoints) {
            Coordinate sample = new Coordinate(rp[0], rp[1]);
            if (!bridge.isPointWithinBridgeFootprint(sample)) {
                outsideFootprintCount++;
                continue;
            }
            if (Double.isNaN(bridge.getDeckHeightAtPoint(sample))) {
                gapCount++;
                gapCoordinates.add(sample);
            }
        }
        assertTrue(outsideFootprintCount == 0, outsideFootprintCount + "/" + roadPoints.length
                + " real LW_ROADS points fall outside the deck's own footprint entirely");
        assertTrue(gapCount == 0, "found " + gapCount + "/" + roadPoints.length
                + " real LW_ROADS points within the footprint that get a NaN deck height: " + gapCoordinates);
    }

    @Test
    public void onDeckSourceOnPr1061BridgeIsClassifiedAndShieldedForAReceiverUnderTheDeck() {
        // Same real PR #1061 deck as realLwRoadsPointsOnPr1061BridgeFindNoHeightGap. Here a source
        // sits on-axis on the deck (correctly resolved absolute Z = deck height + 0.05, as
        // ProfileBuilder#makeGeometryRelativeZToAbsoluteOnBridge would produce) and a receiver sits
        // 3 m off-axis, well under the deck (like the reviewer's receiver 29) - to check whether
        // classification and bridge cut points work correctly once Z is resolved the right way (the
        // other two tests already show the Z resolution itself finds no NaN gap for this bridge).
        double[][] crossSections = {
                {-770895.605, -1137437.618, 392.697}, {-770901.754, -1137445.504, 392.740},
                {-770907.903, -1137453.390, 392.784}, {-770914.053, -1137461.276, 392.827},
                {-770920.202, -1137469.162, 392.871}, {-770926.351, -1137477.047, 392.914},
                {-770932.500, -1137484.933, 392.958}, {-770938.649, -1137492.819, 393.001},
                {-770944.799, -1137500.705, 393.045}, {-770950.948, -1137508.591, 393.088},
                {-770957.097, -1137516.477, 393.131}, {-770963.246, -1137524.363, 393.175},
                {-770969.395, -1137532.249, 393.218}, {-770975.545, -1137540.135, 393.262},
                {-770981.694, -1137548.020, 393.305}, {-770987.843, -1137555.906, 393.349},
                {-770993.992, -1137563.792, 393.392}, {-771000.142, -1137571.678, 393.436},
                {-771006.291, -1137579.564, 393.479}, {-771012.440, -1137587.450, 393.522},
                {-771018.589, -1137595.336, 393.566}, {-771024.738, -1137603.222, 393.609},
                {-771030.888, -1137611.108, 393.653}, {-771037.037, -1137618.993, 393.696},
                {-771043.186, -1137626.879, 393.740}, {-771049.335, -1137634.765, 393.783},
                {-771055.484, -1137642.651, 393.827}, {-771061.634, -1137650.537, 393.870},
                {-771067.783, -1137658.423, 393.914}, {-771073.932, -1137666.309, 393.957},
                {-771080.081, -1137674.195, 394.000}, {-771086.230, -1137682.081, 394.044},
                {-771092.380, -1137689.966, 394.087}, {-771098.529, -1137697.852, 394.131},
                {-771104.678, -1137705.738, 394.174}, {-771110.827, -1137713.624, 394.218},
                {-771116.976, -1137721.510, 394.261}, {-771121.542, -1137727.365, 394.293}};

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < crossSections.length; i++) {
            double[] cs = crossSections[(int) i];
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(cs[0], cs[1], cs[2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        for (int x = -771200; x <= -770800; x += 40) {
            for (int y = -1137800; y <= -1137400; y += 40) {
                pb.addTopographicPoint(new Coordinate(x, y, 390.0));
            }
        }
        pb.addBridge(bridge);
        pb.finishFeeding();

        // On-axis source at cross-section index 7 ((-770938.649, -1137492.819), deck height
        // 393.001), Z resolved exactly as the real pipeline would: deck height + 0.05 m relative.
        double sourceX = -770938.649, sourceY = -1137492.819;
        double deckHeight = bridge.getDeckHeightAtPoint(new Coordinate(sourceX, sourceY));
        assertFalse(Double.isNaN(deckHeight), "sanity check: this point must resolve to a real deck height");
        Coordinate source = new Coordinate(sourceX, sourceY, deckHeight + 0.05);

        // Perpendicular direction from the two neighbouring cross-sections, offset 3 m, like the
        // reviewer's receiver 29 ("3 m off axis, under the deck").
        double dx = -770944.799 - (-770932.500);
        double dy = -1137500.705 - (-1137484.933);
        double len = Math.sqrt(dx * dx + dy * dy);
        double nx = -dy / len, ny = dx / len;
        Coordinate receiver = new Coordinate(sourceX + nx * 3.0, sourceY + ny * 3.0, 391.5);

        CutProfile profile = pb.getProfile(source, receiver);
        assertEquals(BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType(),
                "the on-deck source must be classified as on the bridge, not under it or unrelated");
        assertTrue(countBridgeCutPoints(profile) > 0,
                "an on-deck source over a receiver under the deck must yield bridge cut points "
                        + "(shielding), found " + countBridgeCutPoints(profile));

        // PR #1061 symptom (kaasym/Jirka, receiver "29"): the ground profile must actually step down
        // from the deck surface once the ray reaches the bridge's own edge cut point, otherwise the
        // CNOSSOS diffraction check sees a flat profile (source and receiver at the same reference
        // height) and computes no shielding at all, even though a bridge cut point is present.
        List<Coordinate> ground = CutProfile.computePtsGround(profile.getCutPoints(), null);
        double lastGroundZ = ground.get(ground.size() - 1).z;
        assertTrue(lastGroundZ < deckHeight - 1.0,
                "ground reference near the receiver must drop well below the deck surface ("
                        + deckHeight + " m), found " + lastGroundZ + " m - the bridge edge cut point "
                        + "must be flagged as BUILDING_EXIT or computePtsGround ignores it entirely");
    }

    @Test
    public void buildingOverlappingBridgeFootprintFailsFast() {
        // Deck at y in [15,25], x in [0,100] (same geometry as builderWithDeck()). A building sat
        // squarely inside that 2D footprint has no supported 3D treatment here (a building/wall
        // reflection point could otherwise land inside the footprint and pick up a BridgeRelationship
        // it was never meant to have) - this must fail fast instead of silently misclassifying.
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
                    .withBarrierHeight(1.0, 1.0)
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
        for (int x = 0; x <= 100; x += 50) {
            for (int y = 0; y <= 100; y += 50) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        // Squarely inside the deck's footprint (x in [40,60], y in [18,22]).
        pb.addBuilding(new Coordinate[]{
                new Coordinate(40, 18, 5),
                new Coordinate(60, 18, 5),
                new Coordinate(60, 22, 5),
                new Coordinate(40, 22, 5),
                new Coordinate(40, 18, 5)}, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, pb::finishFeeding,
                "a building overlapping a bridge footprint in 2D must fail fast");
        assertTrue(ex.getMessage().contains("Building"), "exception message should name the offending building");
    }

    @Test
    public void wallOverlappingBridgeFootprintFailsFast() {
        // Same deck as above; a wall crossing straight through the footprint must also fail fast.
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
                    .withBarrierHeight(1.0, 1.0)
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
        for (int x = 0; x <= 100; x += 50) {
            for (int y = 0; y <= 100; y += 50) {
                pb.addTopographicPoint(new Coordinate(x, y, 0.0));
            }
        }
        pb.addBridge(bridge);
        // Crosses the deck footprint (y in [15,25]) at x=50.
        pb.addWall(new Coordinate[]{
                new Coordinate(50, 10, 8),
                new Coordinate(50, 30, 8)}, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, pb::finishFeeding,
                "a wall crossing a bridge footprint in 2D must fail fast");
        assertTrue(ex.getMessage().contains("Wall"), "exception message should name the offending wall");
    }

    @Test
    public void topographyUnderTheDeckDoesNotInflateTheOffAxisPivotPath() {
        // Real PR #1061 regression: with non-flat terrain, a CutPointTopography already found by
        // walking the straight source-receiver line (before the bridge's off-axis "OTHER" pivot is
        // known) can sit within the SAME bridge's own footprint. Since that stretch of the real
        // path runs along the deck (or under it, for an imaginary source), that terrain point does
        // not describe anything on the real path - and ProfileBuilder#addBridgeCutPts always
        // inserts the pivot right after the source without re-sorting (see its sortCutPoints=false
        // branch), so a leftover topography point ends up listed in the wrong place relative to the
        // pivot. CutProfile#computePts2D's cumulative-distance unfolding just walks the cutPoints
        // list in order, so a misplaced point turns into a large spurious back-and-forth detour
        // instead of a small, physically plausible one.
        //
        // Real geometry from the reviewer's bridgepoints.geojson (BRIDGE_PK=1, PK8..PK45) and a
        // small patch of the reviewer's own DEM raster (non-flat terrain under the deck) around a
        // source ~75 m from receiver "29" along the deck and that same receiver.
        double[][] crossSections = {
                {-770895.605, -1137437.618, 392.697}, {-770901.754, -1137445.504, 392.740},
                {-770907.903, -1137453.390, 392.784}, {-770914.053, -1137461.276, 392.827},
                {-770920.202, -1137469.162, 392.871}, {-770926.351, -1137477.047, 392.914},
                {-770932.500, -1137484.933, 392.958}, {-770938.649, -1137492.819, 393.001},
                {-770944.799, -1137500.705, 393.045}, {-770950.948, -1137508.591, 393.088},
                {-770957.097, -1137516.477, 393.131}, {-770963.246, -1137524.363, 393.175},
                {-770969.395, -1137532.249, 393.218}, {-770975.545, -1137540.135, 393.262},
                {-770981.694, -1137548.020, 393.305}, {-770987.843, -1137555.906, 393.349},
                {-770993.992, -1137563.792, 393.392}, {-771000.142, -1137571.678, 393.436},
                {-771006.291, -1137579.564, 393.479}, {-771012.440, -1137587.450, 393.522},
                {-771018.589, -1137595.336, 393.566}, {-771024.738, -1137603.222, 393.609},
                {-771030.888, -1137611.108, 393.653}, {-771037.037, -1137618.993, 393.696},
                {-771043.186, -1137626.879, 393.740}, {-771049.335, -1137634.765, 393.783},
                {-771055.484, -1137642.651, 393.827}, {-771061.634, -1137650.537, 393.870},
                {-771067.783, -1137658.423, 393.914}, {-771073.932, -1137666.309, 393.957},
                {-771080.081, -1137674.195, 394.000}, {-771086.230, -1137682.081, 394.044},
                {-771092.380, -1137689.966, 394.087}, {-771098.529, -1137697.852, 394.131},
                {-771104.678, -1137705.738, 394.174}, {-771110.827, -1137713.624, 394.218},
                {-771116.976, -1137721.510, 394.261}, {-771121.542, -1137727.365, 394.293}};

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < crossSections.length; i++) {
            double[] cs = crossSections[(int) i];
            bridgePoints.add(new BridgePoint.Builder(i, 1L, new Coordinate(cs[0], cs[1], cs[2]))
                    .withHeightType(Scene.HeightType.ABSOLUTE)
                    .withDeckThickness(0.5)
                    .withWidth(6.5, 6.5)
                    .withBarrierHeight(0.0, 0.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build());
        }
        Bridge bridge = new Bridge.Builder(bridgePoints)
                .setPrimaryKey(1L)
                .setGirderType(Bridge.GirderType.STEEL_BOX)
                .setSlabType(Bridge.SlabType.CONCRETE)
                .build();
        ProfileBuilder pb = new ProfileBuilder();
        // Non-flat terrain under/around the deck (real DEM sample points, ~50 m spacing).
        pb.addTopographicPoint(new Coordinate(-771074.197, -1137663.186, 388.951));
        pb.addTopographicPoint(new Coordinate(-771074.197, -1137613.186, 386.717));
        pb.addTopographicPoint(new Coordinate(-771024.197, -1137663.186, 387.775));
        pb.addTopographicPoint(new Coordinate(-771024.197, -1137613.186, 385.892));
        pb.addBridge(bridge);
        pb.finishFeeding();

        double deckHeight = bridge.getDeckHeightAtPoint(new Coordinate(-771075.5704271316, -1137668.4100210904));
        Coordinate source = new Coordinate(-771075.5704271316, -1137668.4100210904, deckHeight + 0.05);
        // Receiver "29" from the bug report, under the deck, off-axis.
        Coordinate receiver = new Coordinate(-771032.19, -1137607.9, 385.892 + 2.0);

        CutProfile profile = pb.getProfile(source, receiver);
        assertEquals(BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());

        for (CutPoint cp : profile.getCutPoints()) {
            assertFalse(cp instanceof CutPointTopography && bridge.isPointWithinBridgeFootprint(cp.getCoordinate()),
                    "a topography point inside the bridge's own footprint must not remain in the profile: " + cp.getCoordinate());
        }

        List<Coordinate> pts2D = profile.computePts2D();
        double unfolded = pts2D.get(pts2D.size() - 1).x;
        double dc = source.distance3D(receiver);
        assertTrue(unfolded < dc * 1.5,
                "the 2D-unfolded profile length must stay close to the true distance (dc=" + dc
                        + "), found " + unfolded + " - a stray topography point is turning this into a "
                        + "spurious back-and-forth detour");
    }

    @Test
    public void sourceAwayFromAnyBridgeStaysUnrelated() {
        ProfileBuilder pb = builderWithDeck();
        CutProfile profile = pb.getProfile(new Coordinate(10, 60, 2.0), new Coordinate(10, 90, 2.0));
        assertEquals(BridgeRelationship.RelationType.SOURCE_NOT_RELATED_TO_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());
        assertEquals(0, countBridgeCutPoints(profile));
    }
}
