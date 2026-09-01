/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.noise_planet.noisemodelling.pathfinder.path.BridgeRelationship;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgeService, focusing on setEffectiveBridgeCutPoint behavior.
 */
public class BridgeServiceTest {

    private BridgeService bridgeService;

    @BeforeEach
    public void setUp() {
        bridgeService = new BridgeService(10, null);
    }

    /**
     * When source has no bridge relationship (bridgeHeight = NaN), a BUILDING_EXIT cut point
     * must be removed by setEffectiveBridgeCutPoint.
     *
     * This is the guard condition: bridgeHeightOn must be non-NaN for BUILDING_EXIT to survive.
     */
    @Test
    public void testSetEffectiveBridgeCutPoint_skipsExitWhenBridgeHeightIsNaN() {
        // Source unrelated to any bridge → bridgeHeight defaults to NaN, bridgePkOn = -1
        CutPointSource source = new CutPointSource(new Coordinate(0, 0, 10.0));
        CutPointReceiver receiver = new CutPointReceiver(new Coordinate(10, 0, 5.0));
        CutProfile profile = new CutProfile(source, receiver);

        CutPointBridgeWall wall = new CutPointBridgeWall(0,
                new Coordinate(5, 0, 15.0),
                new LineSegment(new Coordinate(4, 1, 15), new Coordinate(6, -1, 15)),
                Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8), 1L);
        wall.setIntersectionType(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT);
        wall.setBridgeHeight(37.0);
        profile.insertCutPoint(true, wall);

        bridgeService.setEffectiveBridgeCutPoint(profile);

        long bridgeWallCount = profile.getCutPoints().stream()
                .filter(cp -> cp instanceof CutPointBridgeWall).count();
        assertEquals(0, bridgeWallCount,
                "BUILDING_EXIT must be removed when bridgeHeightOn is NaN (source not on any bridge)");
    }

    /**
     * When source is on a bridge (bridgeHeight is valid and non-NaN), a BUILDING_EXIT cut point
     * on the same bridge must be kept in the profile.
     *
     * This is the normal ACTUAL_SOURCE_ON_BRIDGE → outside receiver propagation path.
     * The bug fixed in CutPointSource was that bridgeRelationship was never copied from
     * SourcePointInfo, so bridgeHeight was never set, causing BUILDING_EXIT to be incorrectly removed.
     */
    @Test
    public void testSetEffectiveBridgeCutPoint_keepsExitWhenBridgeHeightIsValid() {
        // Source on bridge 1 with valid deck height → BUILDING_EXIT for bridge 1 must be kept
        CutPointSource source = new CutPointSource(new Coordinate(0, 0, 10.0));
        source.setBridgeHeight(10.0);
        source.setBridgeRelationship(new BridgeRelationship(
                BridgeRelationship.RelationType.ACTUAL_SOURCE_ON_BRIDGE, 1L, -1L));
        CutPointReceiver receiver = new CutPointReceiver(new Coordinate(10, 0, 5.0));
        CutProfile profile = new CutProfile(source, receiver);

        CutPointBridgeWall wall = new CutPointBridgeWall(0,
                new Coordinate(5, 0, 15.0),
                new LineSegment(new Coordinate(4, 1, 15), new Coordinate(6, -1, 15)),
                Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8), 1L);
        wall.setIntersectionType(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT);
        wall.setBridgeHeight(37.0);
        profile.insertCutPoint(true, wall);

        bridgeService.setEffectiveBridgeCutPoint(profile);

        long bridgeWallCount = profile.getCutPoints().stream()
                .filter(cp -> cp instanceof CutPointBridgeWall).count();
        assertEquals(1, bridgeWallCount,
                "BUILDING_EXIT must be kept when source is on the bridge (bridgeHeightOn is valid)");
    }
}
