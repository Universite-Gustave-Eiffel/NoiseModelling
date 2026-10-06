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
    public void sourceAwayFromAnyBridgeStaysUnrelated() {
        ProfileBuilder pb = builderWithDeck();
        CutProfile profile = pb.getProfile(new Coordinate(10, 60, 2.0), new Coordinate(10, 90, 2.0));
        assertEquals(BridgeRelationship.RelationType.SOURCE_NOT_RELATED_TO_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());
        assertEquals(0, countBridgeCutPoints(profile));
    }
}
