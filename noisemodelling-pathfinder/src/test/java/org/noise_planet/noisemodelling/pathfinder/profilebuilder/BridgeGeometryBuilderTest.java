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
    public void testDefaultConstructor() {
        BridgeGeometryBuilder builder = new BridgeGeometryBuilder();
        assertNotNull(builder.getGeometryFactory(), "Should have a default geometry factory");
    }

    @Test
    public void testConstructorWithNullGeometryFactory() {
        BridgeGeometryBuilder builder = new BridgeGeometryBuilder(null);
        assertNotNull(builder.getGeometryFactory(), "Should use default factory when null is provided");
    }

    @Test
    public void testCreateDeckGeometryWithNullPointManager() {
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(null, null);
        }, "Should throw IllegalArgumentException when point manager is null");
    }

    @Test
    public void testCreateDeckGeometryWithEmptyPoints() {
        BridgePointManager pointManager = new BridgePointManager();
        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(pointManager, null);
        }, "Should throw IllegalArgumentException when no bridge points");
    }

    @Test
    public void testCreateDeckGeometryWithSinglePoint() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        pointManager.addBridgePoint(point);

        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        }, "Should throw IllegalArgumentException when only one bridge point");
    }

    @Test
    public void testCreateDeckGeometryWithTwoPoints() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with two points");
        
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(5, coords.length, "Polygon should have 5 coordinates (4 + closing)");
    }

    @Test
    public void testCreateDeckGeometryWithMultiplePoints() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(50.0, 0.0, 11.0)).build();
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 0.0, 12.0)).build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);
        pointManager.addBridgePoint(point3);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with multiple points");
        
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(7, coords.length, "Polygon should have 7 coordinates (6 + closing)");
    }

    @Test
    public void testCreateDeckGeometryWithZeroWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(0.0, 0.0)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(0.0, 0.0)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon even with zero widths");
        
        // With zero widths, the polygon should be degenerate (line)
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(5, coords.length, "Should still create proper polygon structure");
    }

    @Test
    public void testCreateDeckGeometryWithNaNWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(Double.NaN, Double.NaN)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(Double.NaN, Double.NaN)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with NaN widths");
        
        // NaN widths should be treated as 0.0
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(5, coords.length, "Should create proper polygon structure");
    }

    @Test
    public void testCreateEdgesWithNullGeometry() {
        List<LineString> edges = geometryBuilder.createEdges(null);
        assertNotNull(edges, "Should return empty list, not null");
        assertTrue(edges.isEmpty(), "Should return empty list when geometry is null");
    }

    @Test
    public void testCreateEdgesWithValidPolygon() {
        // Create a simple rectangular polygon
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0, 10),
            new Coordinate(10, 0, 10),
            new Coordinate(10, 5, 10),
            new Coordinate(0, 5, 10),
            new Coordinate(0, 0, 10) // Closing coordinate
        };
        
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(ring);
        
        List<LineString> edges = geometryBuilder.createEdges(polygon);
        assertNotNull(edges, "Should return edge list");
        assertEquals(4, edges.size(), "Should have 4 edges for rectangle");
        
        // Check that each edge is a valid LineString
        for (LineString edge : edges) {
            assertNotNull(edge, "Each edge should be non-null");
            assertEquals(2, edge.getNumPoints(), "Each edge should have 2 points");
        }
    }

    @Test
    public void testSetGeometryFactory() {
        GeometryFactory newFactory = new GeometryFactory();
        geometryBuilder.setGeometryFactory(newFactory);
        assertEquals(newFactory, geometryBuilder.getGeometryFactory(), "Should update geometry factory");
    }

    @Test
    public void testSetNullGeometryFactory() {
        geometryBuilder.setGeometryFactory(null);
        assertNotNull(geometryBuilder.getGeometryFactory(), "Should use default factory when setting null");
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
    public void testCreateBridgeEdgePointsRightDirection() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        List<BridgePoint> edgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.RIGHT, false);
        assertNotNull(edgePoints, "Should create edge points");
        assertEquals(2, edgePoints.size(), "Should have 2 edge points");
        
        // Check that points have right position
        for (BridgePoint point : edgePoints) {
            assertEquals(BridgePoint.Position.RIGHT, point.getPosition(), "Points should have RIGHT position");
        }
    }

    @Test
    public void testCreateBridgeEdgePointsLeftDirection() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0)).build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        List<BridgePoint> edgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.LEFT, false);
        assertNotNull(edgePoints, "Should create edge points");
        assertEquals(2, edgePoints.size(), "Should have 2 edge points");
        
        // Check that points have left position
        for (BridgePoint point : edgePoints) {
            assertEquals(BridgePoint.Position.LEFT, point.getPosition(), "Points should have LEFT position");
        }
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
        
        BridgePoint rightPoint1 = rightEdgePoints.get(0);
        BridgePoint rightPoint2 = rightEdgePoints.get(1);
        
        // For a horizontal line going east, right side should be in -Y direction (90 degrees clockwise from direction)
        assertEquals(0.0, rightPoint1.getCoordinate().x, 0.001, "Right point 1 X should match");
        assertEquals(-5.0, rightPoint1.getCoordinate().y, 0.001, "Right point 1 Y should be offset -5.0");
        assertEquals(100.0, rightPoint2.getCoordinate().x, 0.001, "Right point 2 X should match");
        assertEquals(-5.0, rightPoint2.getCoordinate().y, 0.001, "Right point 2 Y should be offset -5.0");

        // Test left direction (should offset in -Y direction for horizontal line)
        List<BridgePoint> leftEdgePoints = geometryBuilder.createBridgeEdgePoints(pointManager, createMockProfileBuilder(5.0), BridgePoint.Position.LEFT, false);
        assertNotNull(leftEdgePoints, "Should create left edge points");
        
        BridgePoint leftPoint1 = leftEdgePoints.get(0);
        BridgePoint leftPoint2 = leftEdgePoints.get(1);
        
        // For a horizontal line going east, left side should be in +Y direction (90 degrees counter-clockwise from direction)
        assertEquals(0.0, leftPoint1.getCoordinate().x, 0.001, "Left point 1 X should match");
        assertEquals(5.0, leftPoint1.getCoordinate().y, 0.001, "Left point 1 Y should be offset +5.0");
        assertEquals(100.0, leftPoint2.getCoordinate().x, 0.001, "Left point 2 X should match");
        assertEquals(5.0, leftPoint2.getCoordinate().y, 0.001, "Left point 2 Y should be offset +5.0");
    }

    // Test for edge cases and error conditions

    @Test
    public void testCreateDeckGeometryWithInsufficientEdgePoints() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        pointManager.addBridgePoint(point);

        assertThrows(IllegalArgumentException.class, () -> {
            geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        }, "Should throw IllegalArgumentException when insufficient points for polygon");
    }

    @Test
    public void testCreateEdgesWithTriangularPolygon() {
        // Create a triangular polygon
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0, 10),
            new Coordinate(10, 0, 10),
            new Coordinate(5, 5, 10),
            new Coordinate(0, 0, 10) // Closing coordinate
        };
        
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(ring);
        
        List<LineString> edges = geometryBuilder.createEdges(polygon);
        assertNotNull(edges, "Should return edge list");
        assertEquals(3, edges.size(), "Should have 3 edges for triangle");
    }

    @Test
    public void testCreateEdgesWithComplexPolygon() {
        // Create a hexagonal polygon
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0, 10),
            new Coordinate(2, 0, 10),
            new Coordinate(3, 1, 10),
            new Coordinate(2, 2, 10),
            new Coordinate(0, 2, 10),
            new Coordinate(-1, 1, 10),
            new Coordinate(0, 0, 10) // Closing coordinate
        };
        
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon polygon = geometryFactory.createPolygon(ring);
        
        List<LineString> edges = geometryBuilder.createEdges(polygon);
        assertNotNull(edges, "Should return edge list");
        assertEquals(6, edges.size(), "Should have 6 edges for hexagon");
        
        // Verify edge coordinates
        for (int i = 0; i < edges.size(); i++) {
            LineString edge = edges.get(i);
            assertEquals(2, edge.getNumPoints(), "Each edge should have exactly 2 points");
            assertEquals(coords[i], edge.getCoordinateN(0), "First coordinate should match");
            assertEquals(coords[i + 1], edge.getCoordinateN(1), "Second coordinate should match");
        }
    }

    // Test for different bridge configurations

    @Test
    public void testCreateDeckGeometryWithAsymmetricWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(7.0, 3.0) // Different right/left widths
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(6.0, 4.0)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with asymmetric widths");
        
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(5, coords.length, "Polygon should have 5 coordinates (4 + closing)");
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area");
    }

    @Test
    public void testCreateDeckGeometryWithVaryingWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(2.0, 2.0)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(50.0, 0.0, 11.0))
            .withWidth(5.0, 5.0)
            .build();
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(3.0, 3.0)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);
        pointManager.addBridgePoint(point3);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with varying widths");
        
        Coordinate[] coords = polygon.getCoordinates();
        assertEquals(7, coords.length, "Polygon should have 7 coordinates (6 + closing)");
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area");
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

    // Test for coordinate precision and edge cases

    @Test
    public void testCreateDeckGeometryWithVerySmallWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(0.001, 0.001)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(0.001, 0.001)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with very small widths");
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area even with small widths");
    }

    @Test
    public void testCreateDeckGeometryWithLargeWidths() {
        BridgePointManager pointManager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0))
            .withWidth(100.0, 100.0)
            .build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 12.0))
            .withWidth(100.0, 100.0)
            .build();
        
        pointManager.addBridgePoint(point1);
        pointManager.addBridgePoint(point2);

        Polygon polygon = geometryBuilder.createDeckGeometry(pointManager, createMockProfileBuilder(5.0));
        assertNotNull(polygon, "Should create polygon with large widths");
        assertTrue(polygon.isValid(), "Created polygon should be valid");
        assertTrue(polygon.getArea() > 0, "Polygon should have positive area with large widths");
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
