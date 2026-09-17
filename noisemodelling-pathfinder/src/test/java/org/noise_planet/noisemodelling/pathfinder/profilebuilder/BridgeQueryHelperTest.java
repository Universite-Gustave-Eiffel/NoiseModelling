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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgeQueryHelper class.
 * Tests spatial queries, point-in-bridge detection, and geometric calculations.
 */
public class BridgeQueryHelperTest {

    private BridgeQueryHelper queryHelper;
    private Polygon testBridgeDeck;
    private BridgeTriangulation mockTriangulation;
    private GeometryFactory geometryFactory;

    @BeforeEach
    public void setUp() {
        geometryFactory = new GeometryFactory();

        // Create a rectangular bridge deck polygon (20x10 at height 15)
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0, 15),
            new Coordinate(20, 0, 15),
            new Coordinate(20, 10, 15),
            new Coordinate(0, 10, 15),
            new Coordinate(0, 0, 15)
        };

        LinearRing ring = geometryFactory.createLinearRing(coords);
        testBridgeDeck = geometryFactory.createPolygon(ring);

        // Create mock triangulation
        mockTriangulation = createMockTriangulation();

        queryHelper = new BridgeQueryHelper(testBridgeDeck, mockTriangulation);
    }

    @Test
    public void testIsPointWithinBridgeFootprint() {
        // Inside / outside
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 5)),
                  "Point inside should be within footprint");
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(25, 5)),
                   "Point outside should not be within footprint");

        // Boundary is inclusive: corners and edge midpoints
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(0, 0)),
                  "Bottom-left corner should be within footprint");
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(20, 10)),
                  "Top-right corner should be within footprint");
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 0)),
                  "Point on bottom edge should be within footprint");
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(0, 5)),
                  "Point on left edge should be within footprint");

        // Just outside the boundary on every side
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(-0.1, 5)),
                   "Point just outside left boundary should not be within footprint");
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(20.1, 5)),
                   "Point just outside right boundary should not be within footprint");
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, -0.1)),
                   "Point just outside bottom boundary should not be within footprint");
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 10.1)),
                   "Point just outside top boundary should not be within footprint");
    }

    @Test
    public void testIsPointWithinBridgeFootprintWithNullInputs() {
        assertFalse(queryHelper.isPointWithinBridgeFootprint(null),
                   "Null point should not be within footprint");

        BridgeQueryHelper nullDeckHelper = new BridgeQueryHelper(null, mockTriangulation);
        assertFalse(nullDeckHelper.isPointWithinBridgeFootprint(new Coordinate(10, 5)),
                   "Should return false when deck geometry is null");
    }

    @Test
    public void testIsPointAboveBridge() {
        // Constant-height triangulation: basic above/at/below + footprint interaction
        assertTrue(queryHelper.isPointAboveBridge(new Coordinate(10, 5, 20)),
                  "Point above deck height should be above bridge");
        assertTrue(queryHelper.isPointAboveBridge(new Coordinate(10, 5, 15)),
                  "Point at deck height should be above bridge");
        assertFalse(queryHelper.isPointAboveBridge(new Coordinate(10, 5, 10)),
                   "Point below deck height should not be above bridge");
        assertFalse(queryHelper.isPointAboveBridge(new Coordinate(25, 5, 20)),
                   "Point outside footprint should not be above bridge");

        // Variable-height triangulation: the deck height used is the one interpolated at that point
        BridgeQueryHelper variableHelper = new BridgeQueryHelper(testBridgeDeck, createVariableHeightTriangulation());
        assertTrue(variableHelper.isPointAboveBridge(new Coordinate(0, 5, 15.0)),
                  "Point at variable deck height should be above bridge");
        assertFalse(variableHelper.isPointAboveBridge(new Coordinate(0, 5, 14.9)),
                   "Point below variable deck height should not be above bridge");
        assertTrue(variableHelper.isPointAboveBridge(new Coordinate(20, 5, 16.0)),
                  "Point at higher variable deck height should be above bridge");

        // NaN deck height (e.g. point outside the triangulated area) is never "above"
        BridgeQueryHelper nanHeightHelper = new BridgeQueryHelper(testBridgeDeck, createNaNTriangulation());
        assertFalse(nanHeightHelper.isPointAboveBridge(new Coordinate(10, 5, 20)),
                   "Should return false when deck height is NaN");
    }

    @Test
    public void testIsPointBelowBridge() {
        // Constant-thickness triangulation: basic below/at-deck-minus-thickness/above + footprint interaction
        assertTrue(queryHelper.isPointBelowBridge(new Coordinate(10, 5, 10)),
                  "Point well below deck should be below bridge");
        assertTrue(queryHelper.isPointBelowBridge(new Coordinate(10, 5, 14.0)),
                  "Point below deck minus thickness should be below bridge");
        assertFalse(queryHelper.isPointBelowBridge(new Coordinate(10, 5, 20)),
                   "Point above deck should not be below bridge");
        assertFalse(queryHelper.isPointBelowBridge(new Coordinate(10, 5, 15)),
                   "Point at deck level should not be below bridge");
        assertFalse(queryHelper.isPointBelowBridge(new Coordinate(25, 5, 10)),
                   "Point outside footprint should not be below bridge");

        // Variable-thickness triangulation: the threshold used is deck height minus thickness at that point
        BridgeQueryHelper variableHelper = new BridgeQueryHelper(testBridgeDeck, createVariableHeightTriangulation());
        assertTrue(variableHelper.isPointBelowBridge(new Coordinate(10, 10, 14.0)),
                  "Point below deck minus variable thickness should be below bridge");
        assertFalse(variableHelper.isPointBelowBridge(new Coordinate(10, 10, 15.0)),
                   "Point above deck minus thickness should not be below bridge");

        // NaN deck height/thickness is never "below"
        BridgeQueryHelper nanThicknessHelper = new BridgeQueryHelper(testBridgeDeck, createNaNTriangulation());
        assertFalse(nanThicknessHelper.isPointBelowBridge(new Coordinate(10, 5, 14.0)),
                   "Should return false when deck height is NaN");
    }

    @Test
    public void testIsPointOnBridgeToleranceBehavior() {
        // Exact match and default tolerance (2.0m)
        assertTrue(queryHelper.isPointOnBridge(new Coordinate(10, 5, 15)),
                  "Point at deck height should be on bridge");
        assertTrue(queryHelper.isPointOnBridge(new Coordinate(10, 5, 16.5)),
                  "Point within default tolerance (1.5m offset) should be on bridge");
        assertFalse(queryHelper.isPointOnBridge(new Coordinate(10, 5, 18)),
                   "Point outside default tolerance (3m offset) should not be on bridge");

        // Same 1.5m-above-deck point, explicit tolerance narrows/widens the result
        Coordinate pointAbove = new Coordinate(10, 5, 16.5);
        assertFalse(queryHelper.isPointOnBridge(pointAbove, 1.0),
                   "Point should not be on bridge with a tolerance smaller than its offset");
        assertTrue(queryHelper.isPointOnBridge(pointAbove, 2.0),
                  "Point should be on bridge once tolerance covers its offset");
        assertTrue(queryHelper.isPointOnBridge(pointAbove, 5.0),
                  "Point should be on bridge with a generous tolerance");

        // Boundary is inclusive (<=), not exclusive (<)
        assertTrue(queryHelper.isPointOnBridge(new Coordinate(10, 5, 15.0), 0.0),
                  "Point exactly at deck height should be on bridge with zero tolerance");
        assertTrue(queryHelper.isPointOnBridge(new Coordinate(10, 5, 17.0), 2.0),
                  "Point exactly at the tolerance boundary should be on bridge");
        assertFalse(queryHelper.isPointOnBridge(new Coordinate(10, 5, 17.1), 2.0),
                   "Point just past the tolerance boundary should not be on bridge");

        // Tolerance never overrides the footprint check
        assertFalse(queryHelper.isPointOnBridge(new Coordinate(25, 5, 15)),
                   "Point outside footprint should not be on bridge regardless of height");
    }

    @Test
    public void testIsRelevantForReflection() {
        Coordinate receiver = new Coordinate(30, 15, 12);

        // Source must be below the bridge to be relevant
        assertTrue(queryHelper.isRelevantForReflection(new Coordinate(10, 5, 10), receiver, 50.0),
                  "Should be relevant when source below bridge and within distance");
        assertFalse(queryHelper.isRelevantForReflection(new Coordinate(10, 5, 20), receiver, 50.0),
                   "Should not be relevant when source above bridge");
        assertFalse(queryHelper.isRelevantForReflection(new Coordinate(10, 5, 15), receiver, 50.0),
                   "Source at deck level should not be relevant for reflection");
        assertTrue(queryHelper.isRelevantForReflection(new Coordinate(10, 5, 14.4), receiver, 50.0),
                  "Source below deck minus thickness should be relevant for reflection");

        // Source must be within the bridge footprint
        assertFalse(queryHelper.isRelevantForReflection(new Coordinate(50, 50, 10), receiver, 50.0),
                   "Should not be relevant when source outside footprint");

        // Max reflection distance and receiver position
        assertTrue(queryHelper.isRelevantForReflection(new Coordinate(10, 5, 5), new Coordinate(15, 8, 12), 10.0),
                  "Should be relevant for a nearby receiver");
        assertFalse(queryHelper.isRelevantForReflection(new Coordinate(-10, -10, 10), new Coordinate(-20, -20, 12), 5.0),
                   "Should not be relevant when the source-receiver line is far from the bridge");
    }

    @Test
    public void testGetEnvelope2D() {
        Envelope envelope = queryHelper.getEnvelope2D();
        assertNotNull(envelope, "Should return envelope");

        assertEquals(0.0, envelope.getMinX(), 1e-10, "Envelope min X should be exact");
        assertEquals(20.0, envelope.getMaxX(), 1e-10, "Envelope max X should be exact");
        assertEquals(0.0, envelope.getMinY(), 1e-10, "Envelope min Y should be exact");
        assertEquals(10.0, envelope.getMaxY(), 1e-10, "Envelope max Y should be exact");
        assertEquals(20.0 * 10.0, envelope.getArea(), 1e-10, "Envelope area should be correct");
    }

    @Test
    public void testGetEnvelope2DWithNullGeometry() {
        BridgeQueryHelper nullHelper = new BridgeQueryHelper(null, mockTriangulation);
        Envelope envelope = nullHelper.getEnvelope2D();

        assertNotNull(envelope, "Should return envelope even with null geometry");
        assertTrue(envelope.isNull(), "Envelope should be empty when geometry is null");
    }

    @Test
    public void testGetGeometry() {
        Geometry geometry = queryHelper.getGeometry();
        assertEquals(testBridgeDeck, geometry, "Should return the bridge deck geometry");
    }

    @Test
    public void testUpdateGeometry() {
        // Replace with an offset rectangle, then with a distant triangle: old footprint invalid, new one valid
        Coordinate[] newCoords = new Coordinate[] {
            new Coordinate(5, 5, 20), new Coordinate(15, 5, 20),
            new Coordinate(15, 15, 20), new Coordinate(5, 15, 20), new Coordinate(5, 5, 20)
        };
        Polygon newDeck = geometryFactory.createPolygon(geometryFactory.createLinearRing(newCoords));
        queryHelper.updateGeometry(newDeck, null, createMockTriangulation());
        queryHelper.getFootprintGeometry();

        assertEquals(newDeck, queryHelper.getGeometry(), "Should update to new geometry");
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 10)),
                  "Should work with updated geometry");
        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(0, 0)),
                   "Old geometry area should no longer be valid");

        Coordinate[] triangleCoords = new Coordinate[] {
            new Coordinate(50, 50, 25), new Coordinate(60, 50, 25), new Coordinate(55, 60, 25), new Coordinate(50, 50, 25)
        };
        Polygon triangleDeck = geometryFactory.createPolygon(geometryFactory.createLinearRing(triangleCoords));
        queryHelper.updateGeometry(triangleDeck, null, createMockTriangulation());
        queryHelper.getFootprintGeometry();

        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 10)),
                   "Point in previous geometry should no longer be valid");
        assertTrue(queryHelper.isPointWithinBridgeFootprint(new Coordinate(55, 55)),
                  "Point in new triangular geometry should be valid");
        Envelope newEnvelope = queryHelper.getEnvelope2D();
        assertEquals(50.0, newEnvelope.getMinX(), 0.001, "New envelope min X should be updated");
        assertEquals(60.0, newEnvelope.getMaxX(), 0.001, "New envelope max X should be updated");
    }

    @Test
    public void testUpdateGeometryWithNullValues() {
        queryHelper.updateGeometry(null, null, null);

        assertFalse(queryHelper.isPointWithinBridgeFootprint(new Coordinate(10, 5)),
                   "Should return false after updating to null geometry");
        assertNull(queryHelper.getGeometry(), "Geometry should be null after update");

        Envelope envelope = queryHelper.getEnvelope2D();
        assertTrue(envelope.isNull(), "Envelope should be null after updating to null geometry");
    }

    @Test
    public void testQueryHelperWithNullTriangulation() {
        BridgeQueryHelper nullTriangulationHelper = new BridgeQueryHelper(testBridgeDeck, null);

        // Should handle null triangulation gracefully
        assertFalse(nullTriangulationHelper.isPointAboveBridge(new Coordinate(10, 5, 20)),
                   "Should return false when triangulation is null");
        assertFalse(nullTriangulationHelper.isPointBelowBridge(new Coordinate(10, 5, 10)),
                   "Should return false when triangulation is null");
        assertFalse(nullTriangulationHelper.isPointOnBridge(new Coordinate(10, 5, 15)),
                   "Should return false when triangulation is null");

        // But footprint check should still work
        assertTrue(nullTriangulationHelper.isPointWithinBridgeFootprint(new Coordinate(10, 5)),
                  "Footprint check should work without triangulation");
    }

    @Test
    public void testQueryHelperWithEmptyGeometry() {
        // Create a degenerate polygon (all points at same location)
        try {
            LinearRing degenerateRing = geometryFactory.createLinearRing(new Coordinate[] {
                new Coordinate(0, 0), new Coordinate(0, 0), new Coordinate(0, 0), new Coordinate(0, 0)
            });
            Polygon degenerateDeck = geometryFactory.createPolygon(degenerateRing);
            BridgeQueryHelper degenerateHelper = new BridgeQueryHelper(degenerateDeck, mockTriangulation);

            // A degenerate polygon might still contain its single point
            // Let's test a point that's definitely different
            assertFalse(degenerateHelper.isPointWithinBridgeFootprint(new Coordinate(10, 10)),
                       "Point away from degenerate geometry should not be within footprint");
        } catch (Exception e) {
            // If degenerate geometry creation fails, test with null geometry instead
            BridgeQueryHelper nullHelper = new BridgeQueryHelper(null, mockTriangulation);
            assertFalse(nullHelper.isPointWithinBridgeFootprint(new Coordinate(0, 0)),
                       "No point should be within null geometry footprint");
        }
    }

    @Test
    public void testComplexGeometry() {
        // L-shaped bridge: containment must follow the concave outline, not its bounding box
        Coordinate[] lShapeCoords = new Coordinate[] {
            new Coordinate(0, 0, 15), new Coordinate(20, 0, 15), new Coordinate(20, 5, 15),
            new Coordinate(10, 5, 15), new Coordinate(10, 15, 15), new Coordinate(0, 15, 15), new Coordinate(0, 0, 15)
        };
        BridgeQueryHelper lShapeHelper = new BridgeQueryHelper(
                geometryFactory.createPolygon(geometryFactory.createLinearRing(lShapeCoords)), mockTriangulation);

        assertTrue(lShapeHelper.isPointWithinBridgeFootprint(new Coordinate(15, 2)),
                  "Point in horizontal part should be within footprint");
        assertTrue(lShapeHelper.isPointWithinBridgeFootprint(new Coordinate(5, 10)),
                  "Point in vertical part should be within footprint");
        assertFalse(lShapeHelper.isPointWithinBridgeFootprint(new Coordinate(15, 10)),
                   "Point in cutout area should not be within footprint");
    }

    @Test
    public void testVerySmallGeometry() {
        // Create a very small bridge (1x1 meter)
        Coordinate[] smallCoords = new Coordinate[] {
            new Coordinate(100, 100, 15), new Coordinate(101, 100, 15),
            new Coordinate(101, 101, 15), new Coordinate(100, 101, 15), new Coordinate(100, 100, 15)
        };
        BridgeQueryHelper smallHelper = new BridgeQueryHelper(
                geometryFactory.createPolygon(geometryFactory.createLinearRing(smallCoords)), mockTriangulation);

        assertTrue(smallHelper.isPointWithinBridgeFootprint(new Coordinate(100.5, 100.5)),
                  "Point in center of small geometry should be within footprint");
        assertFalse(smallHelper.isPointWithinBridgeFootprint(new Coordinate(99.9, 100.5)),
                   "Point just outside small geometry should not be within footprint");

        Envelope smallEnvelope = smallHelper.getEnvelope2D();
        assertEquals(1.0, smallEnvelope.getWidth(), 1e-10, "Small envelope width should be 1");
        assertEquals(1.0, smallEnvelope.getHeight(), 1e-10, "Small envelope height should be 1");
    }

    @Test
    public void testPerformanceWithLargeGeometry() {
        // Create a large bridge with many vertices (approximating a circle)
        int numVertices = 100;
        Coordinate[] circleCoords = new Coordinate[numVertices + 1];
        double centerX = 0, centerY = 0, radius = 50;

        for (int i = 0; i < numVertices; i++) {
            double angle = 2 * Math.PI * i / numVertices;
            circleCoords[i] = new Coordinate(
                centerX + radius * Math.cos(angle),
                centerY + radius * Math.sin(angle),
                15
            );
        }
        circleCoords[numVertices] = new Coordinate(circleCoords[0]); // Close the ring

        LinearRing circleRing = geometryFactory.createLinearRing(circleCoords);
        Polygon circleDeck = geometryFactory.createPolygon(circleRing);

        BridgeQueryHelper circleHelper = new BridgeQueryHelper(circleDeck, mockTriangulation);

        // Test multiple points to ensure performance is reasonable
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            double x = (i % 100) - 50; // Range from -50 to 49
            double y = (i / 100) - 5;  // Range from -5 to 4
            circleHelper.isPointWithinBridgeFootprint(new Coordinate(x, y));
        }
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 1000, "Performance test should complete within 1 second");

        // Test some specific points
        assertTrue(circleHelper.isPointWithinBridgeFootprint(new Coordinate(0, 0)),
                  "Center point should be within circular footprint");
        assertTrue(circleHelper.isPointWithinBridgeFootprint(new Coordinate(25, 0)),
                  "Point at half radius should be within circular footprint");
        assertFalse(circleHelper.isPointWithinBridgeFootprint(new Coordinate(60, 0)),
                   "Point outside radius should not be within circular footprint");
    }

    // Helper methods

    private BridgeTriangulation createMockTriangulation() {
        return new BridgeTriangulation(GeometryFactoryProvider.SHARED) {
            @Override
            public double getDeckHeightAtPoint(Coordinate point) {
                // Return a constant deck height for testing
                return 15.0;
            }

            @Override
            public double getDeckThicknessAtPoint(Coordinate point) {
                // Return default thickness for testing
                return 0.5;
            }
        };
    }

    private BridgeTriangulation createVariableHeightTriangulation() {
        return new BridgeTriangulation(GeometryFactoryProvider.SHARED) {
            @Override
            public double getDeckHeightAtPoint(Coordinate point) {
                // Return height based on X coordinate for slope testing
                return 15.0 + (point.x / 20.0); // Height from 15.0 to 16.0
            }

            @Override
            public double getDeckThicknessAtPoint(Coordinate point) {
                // Return variable thickness
                return 0.5 + (point.y / 20.0); // Thickness from 0.5 to 1.0
            }
        };
    }

    private BridgeTriangulation createNaNTriangulation() {
        return new BridgeTriangulation(GeometryFactoryProvider.SHARED) {
            @Override
            public double getDeckHeightAtPoint(Coordinate point) {
                return Double.NaN;
            }

            @Override
            public double getDeckThicknessAtPoint(Coordinate point) {
                return Double.NaN;
            }
        };
    }
}
