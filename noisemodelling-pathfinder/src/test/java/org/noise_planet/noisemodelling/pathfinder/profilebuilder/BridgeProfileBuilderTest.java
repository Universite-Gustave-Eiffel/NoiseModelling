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
    public void sourceAwayFromAnyBridgeStaysUnrelated() {
        ProfileBuilder pb = builderWithDeck();
        CutProfile profile = pb.getProfile(new Coordinate(10, 60, 2.0), new Coordinate(10, 90, 2.0));
        assertEquals(BridgeRelationship.RelationType.SOURCE_NOT_RELATED_TO_BRIDGE,
                profile.getSource().getBridgeRelationship().getRelationType());
        assertEquals(0, countBridgeCutPoints(profile));
    }
}
