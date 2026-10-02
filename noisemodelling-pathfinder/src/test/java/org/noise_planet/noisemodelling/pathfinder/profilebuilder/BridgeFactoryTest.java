/**
 * Unit tests for Bridge.createBridgesFromPoints factory behavior.
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BridgeFactoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeFactoryTest.class);

    @Test
    public void testCreateBridgesFromPoints_nullAndEmpty() {
        List<Bridge> resNull = Bridge.createBridgesFromPoints(null);
        assertNotNull(resNull);
        assertTrue(resNull.isEmpty());

        List<Bridge> resEmpty = Bridge.createBridgesFromPoints(new ArrayList<>());
        assertNotNull(resEmpty);
        assertTrue(resEmpty.isEmpty());
    }

    @Test
    public void testCreateBridgesFromPoints_groupingAndAlphas() {
        // prepare points for two bridges (bridgePK 1 and 2), mixed order
        BridgePoint p1 = new BridgePoint.Builder(10L, 1L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint p2 = new BridgePoint.Builder(11L, 2L, new Coordinate(0.0, 50.0, 12.0)).build();
        BridgePoint p3 = new BridgePoint.Builder(12L, 1L, new Coordinate(50.0, 0.0, 10.0)).build();
        BridgePoint p4 = new BridgePoint.Builder(13L, 2L, new Coordinate(50.0, 50.0, 12.0)).build();

        List<BridgePoint> pts = Arrays.asList(p2, p1, p4, p3); // intentionally mixed order

        // Log input bridge points
        LOGGER.info("testCreateBridgesFromPoints_groupingAndAlphas: Input BridgePoints (mixed order)");
        for (BridgePoint bp : pts) {
            LOGGER.info("  {}", bp.toString());
        }

        List<Double> alphas = Arrays.asList(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        List<Bridge> bridges = Bridge.createBridgesFromPoints(pts, alphas);

        assertEquals(2, bridges.size(), "Should produce two Bridge instances (two bridge PKs)");

        // collect PKs and verify both groups present
        List<Long> pks = bridges.stream().map(Bridge::getPrimaryKey).collect(Collectors.toList());
        assertTrue(pks.contains(1L));
        assertTrue(pks.contains(2L));

        // Log created bridges
        LOGGER.info("Created {} bridges", bridges.size());
        for (Bridge b : bridges) {
            LOGGER.info("  Bridge pk={} points={} girder={} slab={} alphas={}", b.getPrimaryKey(), b.getBridgePointCount(), b.getGirderType(), b.getSlabType(), b.getAlphas());
        }
    }

    @Test
    public void testCreateBridgesFromPoints_inconsistentGirderTypeThrows() {
        BridgePoint a = new BridgePoint.Builder(1L, 100L, new Coordinate(0,0,10.0))
                .withGirderType(Bridge.GirderType.STEEL_BOX)
                .build();
        BridgePoint b = new BridgePoint.Builder(2L, 100L, new Coordinate(50,0,10.0))
                .withGirderType(Bridge.GirderType.CONCRETE_PLATE)
                .build();

        List<BridgePoint> pts = Arrays.asList(a, b);

        LOGGER.info("testCreateBridgesFromPoints_inconsistentGirderTypeThrows: input points:");
        for (BridgePoint bp : pts) LOGGER.info("  {}", bp.toString());

        assertThrows(IllegalArgumentException.class, () -> Bridge.createBridgesFromPoints(pts));
    }

    @Test
    public void testCreateBridgesFromPoints_threeGroups() {
        BridgePoint p1 = new BridgePoint.Builder(1L, 10L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint p2 = new BridgePoint.Builder(2L, 11L, new Coordinate(10.0, 0.0, 10.0)).build();
        BridgePoint p3 = new BridgePoint.Builder(3L, 12L, new Coordinate(20.0, 0.0, 10.0)).build();

        List<BridgePoint> pts = Arrays.asList(p1, p2, p3);

        LOGGER.info("testCreateBridgesFromPoints_threeGroups: Input points:");
        for (BridgePoint bp : pts) LOGGER.info("  {}", bp.toString());

        List<Bridge> bridges = Bridge.createBridgesFromPoints(pts);
        LOGGER.info("Created {} bridges", bridges.size());
        assertEquals(3, bridges.size(), "Should produce three Bridge instances for three distinct bridge PKs");

        List<Long> pks = bridges.stream().map(Bridge::getPrimaryKey).collect(Collectors.toList());
        assertTrue(pks.contains(10L));
        assertTrue(pks.contains(11L));
        assertTrue(pks.contains(12L));
    }

    @Test
    public void testCreateBridgesFromPoints_noAlphas_leavesEmpty() {
        BridgePoint p1 = new BridgePoint.Builder(21L, 30L, new Coordinate(0.0, 0.0, 8.0)).build();
        List<Bridge> bridges = Bridge.createBridgesFromPoints(Arrays.asList(p1));
        LOGGER.info("testCreateBridgesFromPoints_noAlphas_leavesEmpty: created {} bridges", bridges.size());
        assertEquals(1, bridges.size());
        Bridge b = bridges.get(0);
        assertNotNull(b.getAlphas());
        assertTrue(b.getAlphas().isEmpty(), "When no alphas provided, bridge should have empty alphas until initialization");
    }

    @Test
    public void testCreateBridgesFromPoints_duplicatePoints_preserved() {
        BridgePoint p = new BridgePoint.Builder(31L, 40L, new Coordinate(0.0, 0.0, 9.0)).build();
        List<BridgePoint> pts = Arrays.asList(p, p); // duplicate same object twice

        LOGGER.info("testCreateBridgesFromPoints_duplicatePoints_preserved: Input points (duplicates):");
        for (BridgePoint bp : pts) LOGGER.info("  {}", bp.toString());

        List<Bridge> bridges = Bridge.createBridgesFromPoints(pts);
        LOGGER.info("Created {} bridges", bridges.size());
        assertEquals(1, bridges.size());
        Bridge b = bridges.get(0);
        assertEquals(2, b.getBridgePointCount(), "Duplicate points should be preserved by BridgePointManager.addBridgePoints");
    }
}
