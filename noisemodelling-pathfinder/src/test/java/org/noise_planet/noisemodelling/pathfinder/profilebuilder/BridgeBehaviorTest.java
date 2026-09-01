/**
 * Behavior and integration tests for Bridge class.
 */
package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavior and integration tests for Bridge class (non-DB).
 *
 * Responsibilities:
 * - Construct Bridge instances from in-memory BridgePoint fixtures
 * - Validate deck geometry generation, spatial queries and Bridge APIs
 *
 * Note: DB mapping is tested in `BridgePointMappingTest`.
 */
public class BridgeBehaviorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeBehaviorTest.class);

    private GeometryFactory geometryFactory;
    private List<Double> defaultAlphas;

    @BeforeEach
    public void setUp() {
        geometryFactory = new GeometryFactory();
        defaultAlphas = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8);
    }

    // Helper methods: in-memory fixtures only
    private List<BridgePoint> createTestBridgePoints() {
        List<Coordinate> bridgePointCoords = Arrays.asList(
                new Coordinate(0, 20, 10),
                new Coordinate(50, 20, 10),
                new Coordinate(100, 20, 10)
        );

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long i = 0; i < bridgePointCoords.size(); i++) {
            BridgePoint point = new BridgePoint.Builder(i, 100L, bridgePointCoords.get((int)i))
                .withHeightType(Scene.HeightType.ABSOLUTE)
                .withDeckThickness(0.5)
                .withWidth(5.0, 5.0)
                .withBarrierHeight(1.0, 1.0)
                .withPosition(BridgePoint.Position.CENTER)
                .withGirderType(Bridge.GirderType.STEEL_BOX)
                .withSlabType(Bridge.SlabType.STEEL)
                .build();
            bridgePoints.add(point);
        }

        return bridgePoints;
    }

    private List<BridgePoint> createTestBridgePointsWithRelativeHeight() {
        List<Coordinate> bridgePointCoords = Arrays.asList(
                new Coordinate(10, 0, 10),
                new Coordinate(10, 10, 10),
                new Coordinate(10, 20, 10)
        );

        List<BridgePoint> bridgePoints = new ArrayList<>();

        for (long pk = 0; pk < bridgePointCoords.size(); pk++) {
                Coordinate coord = bridgePointCoords.get((int)pk);
                BridgePoint point = new BridgePoint.Builder(pk, 101L, coord)
                    .withHeightType(Scene.HeightType.RELATIVE)
                    .withBarrierHeight(1.0,1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.STEEL_BOX)
                    .withSlabType(Bridge.SlabType.STEEL)
                    .build();
                bridgePoints.add(point);
        }

        return bridgePoints;
    }

    private List<BridgePoint> createTestBridgePoints2() {
        List<Coordinate> bridgePointCoords = Arrays.asList(
                new Coordinate(70, 0, 15),
                new Coordinate(70, 80, 15)
        );

        List<BridgePoint> bridgePoints = new ArrayList<>();

        for (long pk = 0; pk < bridgePointCoords.size(); pk++) {
                Coordinate coord = bridgePointCoords.get((int)pk);
                BridgePoint point = new BridgePoint.Builder(pk, 101L, coord)
                    .withBarrierHeight(1.0,1.0)
                    .withPosition(BridgePoint.Position.CENTER)
                    .withGirderType(Bridge.GirderType.CONCRETE_PLATE)
                    .withSlabType(Bridge.SlabType.CONCRETE)
                    .build();
                bridgePoints.add(point);
        }

        return bridgePoints;
    }

    private ProfileBuilder createProfileBuilder() {
        ProfileBuilder profileBuilder =  new ProfileBuilder();
        profileBuilder.addTopographicPoint(new Coordinate(0, 0, 0.5));
        profileBuilder.addTopographicPoint(new Coordinate(50, 0, 1.2));
        profileBuilder.addTopographicPoint(new Coordinate(100, 0, 2.8));
        profileBuilder.addTopographicPoint(new Coordinate(0, 50, 0.1));
        profileBuilder.addTopographicPoint(new Coordinate(50, 50, 3.0));
        profileBuilder.addTopographicPoint(new Coordinate(100, 50, 1.5));
        profileBuilder.addTopographicPoint(new Coordinate(0, 100, 2.1));
        profileBuilder.addTopographicPoint(new Coordinate(50, 100, 0.8));
        profileBuilder.addTopographicPoint(new Coordinate(100, 100, 2.5));
        profileBuilder.finishFeeding();

        return profileBuilder;
    }

    @Test
    public void testBridgeConstructionAndValidation() throws Exception  {
        List<BridgePoint> bridgePoints = createTestBridgePoints();
        List<BridgePoint> bridgePointsRel = createTestBridgePointsWithRelativeHeight();
        List<BridgePoint> bridgePoints2 = createTestBridgePoints2();

        LOGGER.info("testBridgeConstructionAndValidation: in-memory bridge points (fixture)");
        for (BridgePoint bp : bridgePoints) LOGGER.info("  {}", bp.toString());
        for (BridgePoint bp : bridgePointsRel) LOGGER.info("  {}", bp.toString());
        for (BridgePoint bp : bridgePoints2) LOGGER.info("  {}", bp.toString());

        // Create bridges using different in-memory fixtures
        Bridge bridge1 = new Bridge.Builder(bridgePoints).withAlphas(defaultAlphas).setPrimaryKey(1L).setGirderType(Bridge.GirderType.STEEL_BOX).setSlabType(Bridge.SlabType.STEEL).build();
        Bridge bridgeRel = new Bridge.Builder(bridgePointsRel).withAlphas(defaultAlphas).setPrimaryKey(1L).setGirderType(Bridge.GirderType.STEEL_BOX).setSlabType(Bridge.SlabType.STEEL).build();
        Bridge bridge2 = new Bridge.Builder(bridgePoints2).withAlphas(defaultAlphas).setPrimaryKey(2L).setGirderType(Bridge.GirderType.CONCRETE_PLATE).setSlabType(Bridge.SlabType.CONCRETE).build();

        LOGGER.info("Created bridges: b1.pk={}, b1.points={}, b1.girder={}, b1.slab={}", bridge1.getPrimaryKey(), bridge1.getBridgePointCount(), bridge1.getGirderType(), bridge1.getSlabType());
        LOGGER.info("Created bridges: bRel.pk={}, bRel.points={}, bRel.girder={}, bRel.slab={}", bridgeRel.getPrimaryKey(), bridgeRel.getBridgePointCount(), bridgeRel.getGirderType(), bridgeRel.getSlabType());
        LOGGER.info("Created bridges: b2.pk={}, b2.points={}, b2.girder={}, b2.slab={}", bridge2.getPrimaryKey(), bridge2.getBridgePointCount(), bridge2.getGirderType(), bridge2.getSlabType());

        ProfileBuilder profileBuilder = createProfileBuilder();
        bridge1.createDeckGeometry(profileBuilder);
        bridgeRel.createDeckGeometry(profileBuilder);
        bridge2.createDeckGeometry(profileBuilder);

        // In-memory bridges created from different fixtures should not be equal when points differ
        assertNotEquals(bridge1, bridge2);

        for (Coordinate deckGeometryCoord : bridge1.getDeckGeometry().getCoordinates()) {
            assertEquals(10.0, deckGeometryCoord.getZ(), 0.001);
        }

        for (Coordinate deckGeometryCoord : bridgeRel.getDeckGeometry().getCoordinates()) {
            assertTrue(deckGeometryCoord.getZ() > 10);
        }

        assertEquals(1L, bridge1.getPrimaryKey());
        assertNotNull(bridge1.getDeckGeometry());
        assertEquals(10.0, bridge1.getAverageAbsoluteDeckHeight(), 0.001);

        assertEquals(bridge1.getGirderType(), Bridge.GirderType.STEEL_BOX);
        assertEquals(bridge1.getSlabType(), Bridge.SlabType.STEEL);
        assertEquals(bridge2.getGirderType(), Bridge.GirderType.CONCRETE_PLATE);
        assertEquals(bridge2.getSlabType(), Bridge.SlabType.CONCRETE);

        List<Coordinate> pointsInsideOrOnBridge = List.of(
            new Coordinate(25, 17,10),
            new Coordinate(50, 20, 10),
            new Coordinate(75, 24, 11)
        );

        for (Coordinate pointInsideOrOnBridge : pointsInsideOrOnBridge) {
            assertTrue(bridge1.isPointWithinBridgeFootprint(pointInsideOrOnBridge));
            assertEquals(10.0, bridge1.getDeckHeightAtPoint(pointInsideOrOnBridge), 0.001);
            assertEquals(0.5, bridge1.getDeckThicknessAtPoint(pointInsideOrOnBridge), 0.001);
            assertTrue(bridge1.isPointAboveBridge(pointInsideOrOnBridge));
            assertFalse(bridge1.isPointBelowBridge(pointInsideOrOnBridge));
            assertTrue(bridge1.isPointOnBridge(pointInsideOrOnBridge));
            assertEquals(0.0, bridge1.getBarrierHeightAtPoint(pointInsideOrOnBridge), 0.001);
        }

        List<Coordinate> pointsInsideButBelowBridge = List.of(
            new Coordinate(25, 17,5),
            new Coordinate(50, 20, 3),
            new Coordinate(75, 24, 1)
        );

        for (Coordinate pointInsideButBelowBridge : pointsInsideButBelowBridge) {
            assertTrue(bridge1.isPointWithinBridgeFootprint(pointInsideButBelowBridge));
            assertTrue(bridge1.isPointBelowBridge(pointInsideButBelowBridge));
            assertFalse(bridge1.isPointAboveBridge(pointInsideButBelowBridge));
            assertFalse(bridge1.isPointOnBridge(pointInsideButBelowBridge));
        }

        List<Coordinate> pointsOnSideBoundary = List.of(
            new Coordinate(10, 15, 10),
            new Coordinate(80, 25, 100)
        );

        for (Coordinate pointOnSideBoundary : pointsOnSideBoundary) {
            assertTrue(bridge1.isPointWithinBridgeFootprint(pointOnSideBoundary));
            assertEquals(1.0, bridge1.getBarrierHeightAtPoint(pointOnSideBoundary), 0.001);
        }

        List<Coordinate> pointsOutsideBridge = List.of(
            new Coordinate(-10, 20, 10.0),
            new Coordinate(150, 20, 10.0),
            new Coordinate(50, 50, 10.0)
        );

        for (Coordinate pointOutsideBridge : pointsOutsideBridge) {
            assertFalse(bridge1.isPointWithinBridgeFootprint(pointOutsideBridge));
            assertTrue(Double.isNaN(bridge1.getDeckHeightAtPoint(pointOutsideBridge)));
            assertTrue(Double.isNaN(bridge1.getDeckThicknessAtPoint(pointOutsideBridge)));
            assertFalse(bridge1.isPointOnBridge(pointOutsideBridge));
        }
    }

    @Test
    public void testConstructorWithNullAlphas() {
        List<BridgePoint> bridgePoints = createTestBridgePoints();
        Bridge bridge = new Bridge.Builder(bridgePoints).setPrimaryKey(1L).build();
        assertNotNull(bridge);
    }

    @Test
    public void testAddAndRemoveBridgePoint() {
        List<BridgePoint> bridgePoints = createTestBridgePoints();
        Bridge bridge = new Bridge.Builder(bridgePoints).withAlphas(defaultAlphas).setPrimaryKey(1L).build();

        LOGGER.info("testAddAndRemoveBridgePoint: initial bridge pk={} points={}", bridge.getPrimaryKey(), bridge.getBridgePointCount());
        for (BridgePoint bp : bridge.getBridgePoints()) LOGGER.info("  {}", bp.toString());

        BridgePoint newPoint = new BridgePoint.Builder(5L, 100L, new Coordinate(120, 25, 2.0))
            .withHeightType(Scene.HeightType.RELATIVE)
            .withDeckThickness(0.5)
            .withWidth(5.0, 5.0)
            .withBarrierHeight(2.0, 3.0)
            .withPosition(BridgePoint.Position.CENTER)
            .withGirderType(Bridge.GirderType.STEEL_BOX)
            .withSlabType(Bridge.SlabType.STEEL)
            .build();

        bridge.addBridgePoint(newPoint);

        LOGGER.info("After add: bridge points={}", bridge.getBridgePointCount());
        for (BridgePoint bp : bridge.getBridgePoints()) LOGGER.info("  {}", bp.toString());

        assertEquals(4, bridge.getBridgePointCount());
        assertTrue(bridge.getBridgePoints().contains(newPoint));

        boolean removed = bridge.removeBridgePoint(1L);
        assertTrue(removed);

        LOGGER.info("After remove existing pk=1: removed={}, points={}", removed, bridge.getBridgePointCount());
        for (BridgePoint bp : bridge.getBridgePoints()) LOGGER.info("  {}", bp.toString());

        assertEquals(3, bridge.getBridgePointCount());

        boolean removedAgain = bridge.removeBridgePoint(999L);
        assertFalse(removedAgain);
    }

}

