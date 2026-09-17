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
import org.locationtech.jts.geom.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgeGeometryBuilder class.
 * Tests bridge deck polygon creation, offset calculations, and edge generation.
 */
public class BridgeGeometryBuilderTest {

    private BridgeGeometryBuilder geometryBuilder;
    private GeometryFactory geometryFactory;

    @BeforeEach
    public void setUp() {
        geometryFactory = new GeometryFactory();
        geometryBuilder = new BridgeGeometryBuilder(geometryFactory);
    }

    @Test
    public void testConstructorGeometryFactoryFallback() {
        assertNotNull(new BridgeGeometryBuilder().getGeometryFactory(),
                "Should have a default geometry factory");
        assertNotNull(new BridgeGeometryBuilder(null).getGeometryFactory(),
                "Should use default factory when null is provided");
    }

    @Test
    public void testCreateDeckGeometryWithInsufficientInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(null, null);
        }, "Should throw IllegalArgumentException when point manager is null");

        BridgePointManager emptyManager = new BridgePointManager();
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(emptyManager, null);
        }, "Should throw IllegalArgumentException when no bridge points");

        BridgePointManager singlePointManager = new BridgePointManager();
        singlePointManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build());
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(singlePointManager, createMockProfileBuilder(5.0));
        }, "Should throw IllegalArgumentException when only one bridge point");
    }

    @Test
    public void testCreateDeckGeometryCoordinateCount() {
        BridgePointManager twoPointManager = new BridgePointManager();
        twoPointManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build());
        twoPointManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).build());

        Polygon twoPointPolygon = geometryBuilder.createDeckGeometry(twoPointManager, createMockProfileBuilder(5.0));
        assertNotNull(twoPointPolygon, "Should create polygon with two points");
        assertEquals(5, twoPointPolygon.getCoordinates().length, "Polygon should have 5 coordinates (4 + closing)");

        BridgePointManager threePointManager = new BridgePointManager();
        threePointManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build());
        threePointManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(50.0, 0.0, 11.0)).build());
        threePointManager.addBridgePoint(new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 0.0, 12.0)).build());

        Polygon threePointPolygon = geometryBuilder.createDeckGeometry(threePointManager, createMockProfileBuilder(5.0));
        assertNotNull(threePointPolygon, "Should create polygon with multiple points");
        assertEquals(7, threePointPolygon.getCoordinates().length, "Polygon should have 7 coordinates (6 + closing)");
    }

    @Test
    public void testCreateDeckGeometryWithDegenerateWidths() {
        // Zero and NaN widths should both be tolerated, NaN treated as 0.0
        BridgePointManager zeroWidthManager = new BridgePointManager();
        zeroWidthManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).withWidth(0.0, 0.0).build());
        zeroWidthManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).withWidth(0.0, 0.0).build());
        Polygon zeroWidthPolygon = geometryBuilder.createDeckGeometry(zeroWidthManager, createMockProfileBuilder(5.0));
        assertNotNull(zeroWidthPolygon, "Should create polygon even with zero widths");
        assertEquals(5, zeroWidthPolygon.getCoordinates().length, "Should still create proper polygon structure");

        BridgePointManager nanWidthManager = new BridgePointManager();
        nanWidthManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).withWidth(Double.NaN, Double.NaN).build());
        nanWidthManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).withWidth(Double.NaN, Double.NaN).build());
        Polygon nanWidthPolygon = geometryBuilder.createDeckGeometry(nanWidthManager, createMockProfileBuilder(5.0));
        assertNotNull(nanWidthPolygon, "Should create polygon with NaN widths");
        assertEquals(5, nanWidthPolygon.getCoordinates().length, "Should create proper polygon structure");
    }

    @Test
    public void testCreateEdgesWithNullGeometry() {
        List<LineString> edges = geometryBuilder.createEdges(null);
        assertNotNull(edges, "Should return empty list, not null");
        assertTrue(edges.isEmpty(), "Should return empty list when geometry is null");
    }

    @Test
    public void testCreateEdgesForVariousPolygonShapes() {
        // Rectangle: 4 edges
        List<LineString> rectangleEdges = geometryBuilder.createEdges(closedPolygon(
                new Coordinate(0, 0, 10), new Coordinate(10, 0, 10), new Coordinate(10, 5, 10), new Coordinate(0, 5, 10)));
        assertEquals(4, rectangleEdges.size(), "Should have 4 edges for rectangle");
        for (LineString edge : rectangleEdges) {
            assertEquals(2, edge.getNumPoints(), "Each edge should have 2 points");
        }

        // Triangle: 3 edges
        List<LineString> triangleEdges = geometryBuilder.createEdges(closedPolygon(
                new Coordinate(0, 0, 10), new Coordinate(10, 0, 10), new Coordinate(5, 5, 10)));
        assertEquals(3, triangleEdges.size(), "Should have 3 edges for triangle");

        // Hexagon: 6 edges, and verify each edge's coordinates match the source ring
        Coordinate[] hexagon = new Coordinate[] {
            new Coordinate(0, 0, 10), new Coordinate(2, 0, 10), new Coordinate(3, 1, 10),
            new Coordinate(2, 2, 10), new Coordinate(0, 2, 10), new Coordinate(-1, 1, 10)
        };
        List<LineString> hexagonEdges = geometryBuilder.createEdges(closedPolygon(hexagon));
        assertEquals(6, hexagonEdges.size(), "Should have 6 edges for hexagon");
        Coordinate[] closedHexagon = closedPolygon(hexagon).getCoordinates();
        for (int i = 0; i < hexagonEdges.size(); i++) {
            LineString edge = hexagonEdges.get(i);
            assertEquals(2, edge.getNumPoints(), "Each edge should have exactly 2 points");
            assertEquals(closedHexagon[i], edge.getCoordinateN(0), "First coordinate should match");
            assertEquals(closedHexagon[i + 1], edge.getCoordinateN(1), "Second coordinate should match");
        }
    }

    private Polygon closedPolygon(Coordinate... openRing) {
        Coordinate[] closed = new Coordinate[openRing.length + 1];
        System.arraycopy(openRing, 0, closed, 0, openRing.length);
        closed[openRing.length] = openRing[0];
        return geometryFactory.createPolygon(geometryFactory.createLinearRing(closed));
    }

    @Test
    public void testDeckGeometryWithComplexPath() {
        BridgePointManager pointManager = new BridgePointManager();

        // Create a curved bridge path
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(25.0, 25.0, 11.0)).build();
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(50.0, 25.0, 12.0)).build();
        BridgePoint point4 = new BridgePoint.Builder(4L, 100L, new Coordinate(75.0, 0.0, 11.0)).build();
        BridgePoint point5 = new BridgePoint.Builder(5L, 100L, new Coordinate(100.0, 0.0, 10.0)).build();

        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);
        pointManager.addBridgePoint(point3);
        pointManager.addBridgePoint(point4);
        pointManager.addBridgePoint(point5);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon for complex path");

        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(11, coords.length, "Should have correct number of coordinates for 5 points");

        // Verify that the polygon is valid
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area");
    }

    @Test
    public void testDeckGeometryHeightPropagation() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 20.0)).build();

        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon");

        Coordinate[] coords = polygon.getCoordinates();

        // Check that Z coordinates are properly set (should be bridge deck heights)
        for (int i = 0; i < coords.length - 1; i++) { // Exclude closing coordinate
            assertFalse(Double.isNaN(coords[i].z), "Z coordinate should not be NaN");
            assertTrue(coords[i].z >= 10.0, "Z coordinate should be at least minimum deck height");
        }
    }

    // Helper methods
    private ProfileBuilder createMockProfileBuilder(double groundHeight) {
        return new ProfileBuilder() {
            @Override
            public double getZGround(Coordinate coordinate, AtomicInteger triangleHint) {
                return groundHeight;
            }
        };
    }

    // Test for createBridgeEdgePoints method

    @Test
    public void testCreateBridgeEdgePointsWithNullPointManager() {
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createBridgeEdgePoints(null, createMockProfileBuilder(5.0), BridgePoint.Position.RIGHT, false);
        }, "Should throw IllegalArgumentException when point manager is null");
    }

    @Test
    public void testCreateBridgeEdgePointsWithSinglePoint() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        pointManager.addBridgePoint(point);

        List<BridgePoint> edgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.RIGHT, false);
        assertNull(edgePoints, "Should return null when only one bridge point");
    }

    @Test
    public void testCreateBridgeEdgePointsWithNaNWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(Double.NaN, Double.NaN)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(Double.NaN, Double.NaN)
            .build();

        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        List<BridgePoint> edgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.RIGHT, false);
        assertNotNull(edgePoints, "Should create edge points with NaN widths");
        assertEquals(2, edgePoints.size(), "Should have 2 edge points");

        // With NaN widths treated as 0.0, edge points should be at center line
        BridgePoint edgePoint1 = edgePoints.get(0);
        BridgePoint edgePoint2 = edgePoints.get(1);

        assertEquals(0.0, edgePoint1.getCoordinate().x, 0.001, "First edge point X should be at center");
        assertEquals(0.0, edgePoint1.getCoordinate().y, 0.001, "First edge point Y should be at center");
        assertEquals(100.0, edgePoint2.getCoordinate().x, 0.001, "Second edge point X should be at center");
        assertEquals(0.0, edgePoint2.getCoordinate().y, 0.001, "Second edge point Y should be at center");
    }

    @Test
    public void testCreateBridgeEdgePointsOffsetCalculation() {
        BridgePointManager pointManager = new BridgePointManager();
        // Create a horizontal line from (0,0) to (100,0)
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).build();

        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        // Test right direction (should offset in +Y direction for horizontal line)
        List<BridgePoint> rightEdgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.RIGHT, false);
        assertNotNull(rightEdgePoints, "Should create right edge points");
        assertEquals(2, rightEdgePoints.size(), "Should have 2 edge points");

        BridgePoint rightPoint1 = rightEdgePoints.get(0);
        BridgePoint rightPoint2 = rightEdgePoints.get(1);

        // For a horizontal line going east, right side should be in -Y direction (90 degrees clockwise from direction)
        assertEquals(BridgePoint.Position.RIGHT, rightPoint1.getPosition(), "Points should have RIGHT position");
        assertEquals(0.0, rightPoint1.getCoordinate().x, 0.001, "Right point 1 X should match");
        assertEquals(-5.0, rightPoint1.getCoordinate().y, 0.001, "Right point 1 Y should be offset -5.0");
        assertEquals(100.0, rightPoint2.getCoordinate().x, 0.001, "Right point 2 X should match");
        assertEquals(-5.0, rightPoint2.getCoordinate().y, 0.001, "Right point 2 Y should be offset -5.0");

        // Test left direction (should offset in -Y direction for horizontal line)
        List<BridgePoint> leftEdgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.LEFT, false);
        assertNotNull(leftEdgePoints, "Should create left edge points");
        assertEquals(2, leftEdgePoints.size(), "Should have 2 edge points");

        BridgePoint leftPoint1 = leftEdgePoints.get(0);
        BridgePoint leftPoint2 = leftEdgePoints.get(1);

        // For a horizontal line going east, left side should be in +Y direction (90 degrees counter-clockwise from direction)
        assertEquals(BridgePoint.Position.LEFT, leftPoint1.getPosition(), "Points should have LEFT position");
        assertEquals(0.0, leftPoint1.getCoordinate().x, 0.001, "Left point 1 X should match");
        assertEquals(5.0, leftPoint1.getCoordinate().y, 0.001, "Left point 1 Y should be offset +5.0");
        assertEquals(100.0, leftPoint2.getCoordinate().x, 0.001, "Left point 2 X should match");
        assertEquals(5.0, leftPoint2.getCoordinate().y, 0.001, "Left point 2 Y should be offset +5.0");
    }

    // Test for different bridge configurations

    @Test
    public void testCreateDeckGeometryWithVariousWidths() {
        // Asymmetric per-point widths (different right/left)
        BridgePointManager asymmetricManager = new BridgePointManager();
        asymmetricManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).withWidth(7.0, 3.0).build());
        asymmetricManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).withWidth(6.0, 4.0).build());
        Polygon asymmetricPolygon = geometryBuilder.createDeckGeometry(asymmetricManager, createMockProfileBuilder(5.0));
        assertNotNull(asymmetricPolygon, "Should create polygon with asymmetric widths");
        assertEquals(5, asymmetricPolygon.getCoordinates().length, "Polygon should have 5 coordinates (4 + closing)");
        assertTrue(asymmetricPolygon.isValid(), "Created polygon should be valid");
        assertTrue(asymmetricPolygon.getArea() > 0, "Polygon should have positive area");

        // Width varying along the deck (narrow-wide-narrow)
        BridgePointManager varyingManager = new BridgePointManager();
        varyingManager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).withWidth(2.0, 2.0).build());
        varyingManager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(50.0, 0.0, 11.0)).withWidth(5.0, 5.0).build());
        varyingManager.addBridgePoint(new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 0.0, 12.0)).withWidth(3.0, 3.0).build());
        Polygon varyingPolygon = geometryBuilder.createDeckGeometry(varyingManager, createMockProfileBuilder(5.0));
        assertNotNull(varyingPolygon, "Should create polygon with varying widths");
        assertEquals(7, varyingPolygon.getCoordinates().length, "Polygon should have 7 coordinates (6 + closing)");
        assertTrue(varyingPolygon.isValid(), "Created polygon should be valid");
        assertTrue(varyingPolygon.getArea() > 0, "Polygon should have positive area");

        // Extreme magnitudes: very small and very large widths must both stay valid
        for (double width : new double[] {0.001, 100.0}) {
            BridgePointManager manager = new BridgePointManager();
            manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).withWidth(width, width).build());
            manager.addBridgePoint(new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).withWidth(width, width).build());
            Polygon polygon = geometryBuilder.createDeckGeometry(manager, createMockProfileBuilder(5.0));
            assertNotNull(polygon, "Should create polygon with width " + width);
            assertTrue(polygon.isValid(), "Created polygon should be valid for width " + width);
            assertTrue(polygon.getArea() > 0, "Polygon should have positive area for width " + width);
        }
    }

    @Test
    public void testCreateDeckGeometryWithCurvedBridge() {
        BridgePointManager pointManager = new BridgePointManager();

        // Create a curved bridge (quarter circle)
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(10.0, 10.0, 11.0)).build();
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(0.0, 20.0, 12.0)).build();

        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);
        pointManager.addBridgePoint(point3);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon for curved bridge");

        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(7, coords.length, "Should have correct number of coordinates");
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area");
    }

    @Test
    public void testGeometryFactoryGetterSetter() {
        GeometryFactory originalFactory = geometryBuilder.getGeometryFactory();
        assertNotNull(originalFactory, "Should have geometry factory");

        GeometryFactory newFactory = new GeometryFactory();
        geometryBuilder.setGeometryFactory(newFactory);
        assertEquals(newFactory, geometryBuilder.getGeometryFactory(), "Should return new geometry factory");

        // Test setting back to null
        geometryBuilder.setGeometryFactory(null);
        assertNotNull(geometryBuilder.getGeometryFactory(), "Should use default factory when null is set");
        assertNotEquals(newFactory, geometryBuilder.getGeometryFactory(), "Should not use previous factory after null");
    }
}
