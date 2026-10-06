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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgePoint class.
 * Tests data container functionality, validation methods, and copy operations.
 */
public class BridgePointTest {

    @Test
    public void testBridgePointConstructorWithCoordinate() {
        Coordinate coord = new Coordinate(100.0, 200.0, 15.0);
        BridgePoint point = new BridgePoint.Builder(0L, 0L, coord).build();
        
        assertEquals(coord, point.getCoordinate(), "Coordinate should be set");
        assertEquals(0L, point.getPrimaryKey(), "Primary key should use default");
        assertEquals(BridgePoint.Position.CENTER, point.getPosition(), "Position should use default");
    }

    @Test
    public void testBridgePointFullConstructor() {
        Coordinate coord = new Coordinate(100.0, 200.0, 15.0);
        BridgePoint point = new BridgePoint.Builder(1L, 100L, coord)
            .withWidth(5.0,6.0)
            .withBarrierHeight(2.0,3.0)
            .build();
        
        assertEquals(coord, point.getCoordinate(), "Coordinate should be set");
        assertEquals(1L, point.getPrimaryKey(), "Primary key should be set");
        assertEquals(100L, point.getBridgePrimaryKey(), "Bridge primary key should be set");
        assertEquals(15.0, point.getAbsoluteDeckHeight(), 0.001, "Absolute deck height should be set");
        assertTrue(Double.isNaN(point.getRelativeDeckHeight()), "Relative deck height should be NaN");
        assertEquals(0.5, point.getDeckThickness(), 0.001, "Deck thickness should be set");
        assertEquals(5.0, point.getRightWidth(), 0.001, "Right width should be set");
        assertEquals(6.0, point.getLeftWidth(), 0.001, "Left width should be set");
        assertEquals(2.0, point.getRightBarrierHeight(), 0.001, "Right barrier height should be set");
        assertEquals(3.0, point.getLeftBarrierHeight(), 0.001, "Left barrier height should be set");
    }

    @Test
    public void testBridgePointCopyConstructor() {
        Coordinate coord = new Coordinate(100.0, 200.0, 15.0);
        BridgePoint original = new BridgePoint.Builder(1L, 100L, coord).build();
        
        BridgePoint copy = new BridgePoint(original);

        assertEquals(original, copy, "Copy should be equal to original");
    }

    @Test
    public void testBridgePointCoordinateIndependence() {
        Coordinate originalCoord = new Coordinate(100.0, 200.0, 15.0);
        BridgePoint point1 = new BridgePoint.Builder(0L, 0L, originalCoord).build();
        BridgePoint point2 = new BridgePoint(point1);
        
        // Modify the original coordinate
        originalCoord.x = 999.0;
        originalCoord.y = 888.0;
        originalCoord.z = 777.0;
        
        // point1 should not be affected (assuming coordinate is copied in constructor)
        // This test verifies the behavior - if coordinate is stored by reference, this test will fail
        assertNotEquals(999.0, point1.getCoordinate().x, "Point1 coordinate should not be affected by original coordinate modification");
        
        // point2 should definitely not be affected (deep copy in copy constructor)
        assertNotEquals(999.0, point2.getCoordinate().x, "Point2 coordinate should not be affected by original coordinate modification");
        assertEquals(100.0, point2.getCoordinate().x, 0.001, "Point2 should have original coordinate values");
    }

}
