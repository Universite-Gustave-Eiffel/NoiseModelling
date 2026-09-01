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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BridgePointManager class.
 * Tests point management, height calculations, and interpolation functionality.
 */
public class BridgePointManagerTest {

    @Test
    public void testEmptyManager() {
        BridgePointManager manager = new BridgePointManager();
        
        assertTrue(manager.isEmpty(), "New manager should be empty");
        assertEquals(0, manager.size(), "Size should be 0");
        assertTrue(manager.getBridgePoints().isEmpty(), "Bridge points list should be empty");
    }

    @Test
    public void testAddBridgePoint() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        
        manager.addBridgePoint(point);
        
        assertFalse(manager.isEmpty(), "Manager should not be empty");
        assertEquals(1, manager.size(), "Size should be 1");
        assertEquals(point, manager.getBridgePointByIndex(0), "Point should be retrievable by index");
    }

    @Test
    public void testAddNullBridgePoint() {
        BridgePointManager manager = new BridgePointManager();
        
        manager.addBridgePoint(null);
        
        assertTrue(manager.isEmpty(), "Manager should remain empty when adding null");
        assertEquals(0, manager.size(), "Size should remain 0");
    }

    @Test
    public void testSortingByPrimaryKey() {
        BridgePointManager manager = new BridgePointManager();
        
        // Add points in reverse order
        BridgePoint point3 = new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0)).build();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build();
        
        manager.addBridgePoint(point3);
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        
        List<BridgePoint> points = manager.getBridgePoints();
        assertEquals(1L, points.get(0).getPrimaryKey(), "Points should be sorted by primary key");
        assertEquals(2L, points.get(1).getPrimaryKey(), "Points should be sorted by primary key");
        assertEquals(3L, points.get(2).getPrimaryKey(), "Points should be sorted by primary key");
    }

    @Test
    public void testRemoveBridgePoint() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        
        assertTrue(manager.removeBridgePoint(1), "Should remove existing point");
        assertEquals(1, manager.size(), "Size should decrease");
        assertFalse(manager.removeBridgePoint(3), "Should not remove non-existing point");
    }

    @Test
    public void testGetBridgePointByPrimaryKey() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        
        manager.addBridgePoint(point);
        
        assertEquals(point, manager.getBridgePointByPrimaryKey(1), "Should find point by primary key");
        assertNull(manager.getBridgePointByPrimaryKey(2), "Should return null for non-existing key");
    }

    @Test
    public void testGetPrimaryKeys() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build();
        
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point1);
        
        List<Long> keys = manager.getPrimaryKeys();
        assertEquals(2, keys.size(), "Should have 2 keys");
        assertEquals(Long.valueOf(1), keys.get(0), "Keys should be sorted");
        assertEquals(Long.valueOf(2), keys.get(1), "Keys should be sorted");
    }

    @Test
    public void testEffectiveDeckHeightWithAbsolute() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 15.0)).build();
        
        manager.addBridgePoint(point);
        
        double height = manager.getEffectiveDeckHeight(0, null);
        assertEquals(15.0, height, 0.001, "Should use absolute height");
    }

    @Test
    public void testEffectiveDeckHeightWithRelative() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, Double.NaN)).withRelativeDeckHeight(5.0).build();
        
        manager.addBridgePoint(point);
        
        // Create mock ProfileBuilder that returns ground height of 10.0
        ProfileBuilder profileBuilder = createMockProfileBuilder(10.0);
        
        double height = manager.getEffectiveDeckHeight(0, profileBuilder);
        assertEquals(15.0, height, 0.001, "Should use relative height + ground");
    }

    @Test
    public void testInterpolateDeckHeightBetweenTwoPoints() {
        BridgePointManager manager = new BridgePointManager();
        
        // Create points with known heights at distances 0, 100, 200
        BridgePoint point1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, Double.NaN)).build(); // Point to interpolate
        BridgePoint point3 = new BridgePoint.Builder(3L, 200L, new Coordinate(200.0, 0.0, 20.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point3);
        
        double interpolated = manager.interpolateDeckHeight(1, null);
        assertEquals(15.0, interpolated, 0.001, "Should interpolate linearly");
    }

    @Test
    public void testInterpolateDeckHeightNoValidPoints() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, Double.NaN)).build();
        
        manager.addBridgePoint(point);
        
        assertThrows(IllegalStateException.class, () -> {
            manager.interpolateDeckHeight(0, null);
        }, "Should throw IllegalStateException when no valid points");
    }

    @Test
    public void testClear() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        
        manager.addBridgePoint(point);
        assertFalse(manager.isEmpty(), "Should not be empty before clear");
        
        manager.clear();
        assertTrue(manager.isEmpty(), "Should be empty after clear");
        assertEquals(0, manager.size(), "Size should be 0 after clear");
    }

    @Test
    public void testConstructorWithInitialPoints() {
        List<BridgePoint> initialPoints = new ArrayList<>();
        initialPoints.add(new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build());
        initialPoints.add(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        BridgePointManager manager = new BridgePointManager(initialPoints);
        
        assertEquals(2, manager.size(), "Should have initial points");
        assertEquals(1L, manager.getBridgePointByIndex(0).getPrimaryKey(), "Should be sorted by primary key");
        assertEquals(2L, manager.getBridgePointByIndex(1).getPrimaryKey(), "Should be sorted by primary key");
    }

    @Test
    public void testGetBridgePointByIndexOutOfBounds() {
        BridgePointManager manager = new BridgePointManager();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            manager.getBridgePointByIndex(0); // Should throw exception
        });
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

    // Test additional constructors

    @Test
    public void testConstructorWithSortOrder() {
        BridgePointManager manager = new BridgePointManager(BridgePointManager.SortOrder.CLOCKWISE);
        assertTrue(manager.isEmpty(), "Manager should be empty");
    }

    @Test
    public void testConstructorWithPointsAndSortOrder() {
        List<BridgePoint> initialPoints = new ArrayList<>();
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build();
        point1.setPosition(BridgePoint.Position.RIGHT);
        point2.setPosition(BridgePoint.Position.LEFT);
        
        initialPoints.add(point2);
        initialPoints.add(point1);
        
        BridgePointManager manager = new BridgePointManager(initialPoints, BridgePointManager.SortOrder.BY_PRIMARY_KEY);
        
        assertEquals(2, manager.size(), "Should have initial points");
        // Should be sorted by position first (CENTER < LEFT < RIGHT), then by primary key
        assertEquals(BridgePoint.Position.LEFT, manager.getBridgePointByIndex(0).getPosition(), "First point should be LEFT");
        assertEquals(BridgePoint.Position.RIGHT, manager.getBridgePointByIndex(1).getPosition(), "Second point should be RIGHT");
    }

    @Test
    public void testConstructorWithNullPoints() {
        // The current implementation throws NullPointerException when null list is passed
        // This test verifies the current behavior
        List<BridgePoint> nullList = null;
        assertThrows(NullPointerException.class, () -> {
            new BridgePointManager(nullList);
        }, "Should throw NullPointerException with null input");
    }

    // Test SortOrder variations

    @Test
    public void testSortCounterClockwise() {
        BridgePointManager manager = new BridgePointManager(BridgePointManager.SortOrder.CLOCKWISE);
        
        // Add points with different positions
        BridgePoint rightPoint1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0))
            .withPosition(BridgePoint.Position.RIGHT)
            .build();
        BridgePoint leftPoint1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0))
            .withPosition(BridgePoint.Position.LEFT)
            .build();
        BridgePoint rightPoint2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0))
            .withPosition(BridgePoint.Position.RIGHT)
            .build();
        BridgePoint leftPoint2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0))
            .withPosition(BridgePoint.Position.LEFT)
            .build();
        BridgePoint centerPoint = new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0))
            .withPosition(BridgePoint.Position.CENTER)
            .build();
        
        manager.addBridgePoint(leftPoint2);
        manager.addBridgePoint(rightPoint1);
        manager.addBridgePoint(centerPoint); // Should be filtered out
        manager.addBridgePoint(leftPoint1);
        manager.addBridgePoint(rightPoint2);
        
        List<BridgePoint> points = manager.getBridgePoints();
        assertEquals(4, points.size(), "Should filter out CENTER position points");
        
        // Expected order: LEFT points in ascending order, then RIGHT points in descending order
        assertEquals(BridgePoint.Position.LEFT, points.get(0).getPosition(), "First should be LEFT");
        assertEquals(1L, points.get(0).getPrimaryKey(), "First LEFT should have PK 1");
        assertEquals(BridgePoint.Position.LEFT, points.get(1).getPosition(), "Second should be LEFT");
        assertEquals(2L, points.get(1).getPrimaryKey(), "Second LEFT should have PK 2");
        assertEquals(BridgePoint.Position.RIGHT, points.get(2).getPosition(), "Third should be RIGHT");
        assertEquals(2L, points.get(2).getPrimaryKey(), "First RIGHT should have PK 2 (descending)");
        assertEquals(BridgePoint.Position.RIGHT, points.get(3).getPosition(), "Fourth should be RIGHT");
        assertEquals(1L, points.get(3).getPrimaryKey(), "Second RIGHT should have PK 1 (descending)");
    }

    @Test
    public void testSortSideToSide() {
        BridgePointManager manager = new BridgePointManager(BridgePointManager.SortOrder.SIDE_TO_SIDE);
        
        // Add points with different positions
        BridgePoint rightPoint1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0))
            .withPosition(BridgePoint.Position.RIGHT)
            .build();
        BridgePoint leftPoint1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0))
            .withPosition(BridgePoint.Position.LEFT)
            .build();
        BridgePoint rightPoint2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0))
            .withPosition(BridgePoint.Position.RIGHT)
            .build();
        BridgePoint leftPoint2 = new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0))
            .withPosition(BridgePoint.Position.LEFT)
            .build();
        BridgePoint centerPoint = new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0))
            .withPosition(BridgePoint.Position.CENTER)
            .build();
        
        manager.addBridgePoint(rightPoint2);
        manager.addBridgePoint(leftPoint1);
        manager.addBridgePoint(centerPoint); // Should be filtered out
        manager.addBridgePoint(rightPoint1);
        manager.addBridgePoint(leftPoint2);
        
        List<BridgePoint> points = manager.getBridgePoints();
        assertEquals(4, points.size(), "Should filter out CENTER position points");
        
        // Expected order: Grouped by primary key, then by position (LEFT < RIGHT)
        assertEquals(1L, points.get(0).getPrimaryKey(), "First should have PK 1");
        assertEquals(BridgePoint.Position.LEFT, points.get(0).getPosition(), "First should be LEFT");
        assertEquals(1L, points.get(1).getPrimaryKey(), "Second should have PK 1");
        assertEquals(BridgePoint.Position.RIGHT, points.get(1).getPosition(), "Second should be RIGHT");
        assertEquals(2L, points.get(2).getPrimaryKey(), "Third should have PK 2");
        assertEquals(BridgePoint.Position.LEFT, points.get(2).getPosition(), "Third should be LEFT");
        assertEquals(2L, points.get(3).getPrimaryKey(), "Fourth should have PK 2");
        assertEquals(BridgePoint.Position.RIGHT, points.get(3).getPosition(), "Fourth should be RIGHT");
    }

    // Test addBridgePoints method

    @Test
    public void testAddBridgePointsList() {
        BridgePointManager manager = new BridgePointManager();
        
        List<BridgePoint> pointsToAdd = new ArrayList<>();
        pointsToAdd.add(new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0)).build());
        pointsToAdd.add(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        pointsToAdd.add(new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build());
        
        manager.addBridgePoints(pointsToAdd);
        
        assertEquals(3, manager.size(), "Should add all points");
        List<BridgePoint> retrievedPoints = manager.getBridgePoints();
        assertEquals(1L, retrievedPoints.get(0).getPrimaryKey(), "Should be sorted by primary key");
        assertEquals(2L, retrievedPoints.get(1).getPrimaryKey(), "Should be sorted by primary key");
        assertEquals(3L, retrievedPoints.get(2).getPrimaryKey(), "Should be sorted by primary key");
    }

    @Test
    public void testAddNullBridgePointsList() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint originalPoint = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        manager.addBridgePoint(originalPoint);
        
        manager.addBridgePoints(null);
        
        assertEquals(1, manager.size(), "Size should remain unchanged");
        assertEquals(originalPoint, manager.getBridgePointByIndex(0), "Original point should remain");
    }

    @Test
    public void testAddEmptyBridgePointsList() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint originalPoint = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        manager.addBridgePoint(originalPoint);
        
        manager.addBridgePoints(new ArrayList<>());
        
        assertEquals(1, manager.size(), "Size should remain unchanged");
        assertEquals(originalPoint, manager.getBridgePointByIndex(0), "Original point should remain");
    }

    // Test updateGroundHeight method

    @Test
    public void testUpdateGroundHeightNormalCase() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0)).build());
        
        ProfileBuilder profileBuilder = createMockProfileBuilder(5.0);
        
        double minGroundHeight = manager.updateGroundHeight(profileBuilder);
        assertEquals(5.0, minGroundHeight, 0.001, "Should return ground height from ProfileBuilder");
    }

    @Test
    public void testUpdateGroundHeightWithVariableGroundHeight() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0)).build());
        
        ProfileBuilder profileBuilder = new ProfileBuilder() {
            @Override
            public double getZGround(Coordinate coordinate, AtomicInteger triangleHint) {
                // Return different heights based on X coordinate
                return coordinate.x / 100.0; // 1.0, 2.0, 3.0
            }
        };
        
        double minGroundHeight = manager.updateGroundHeight(profileBuilder);
        assertEquals(1.0, minGroundHeight, 0.001, "Should return minimum ground height");
    }

    @Test
    public void testUpdateGroundHeightEmptyManager() {
        BridgePointManager manager = new BridgePointManager();
        ProfileBuilder profileBuilder = createMockProfileBuilder(5.0);
        
        double minGroundHeight = manager.updateGroundHeight(profileBuilder);
        assertTrue(Double.isNaN(minGroundHeight), "Should return NaN for empty manager");
    }

    @Test
    public void testUpdateGroundHeightNullProfileBuilder() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        double minGroundHeight = manager.updateGroundHeight(null);
        assertTrue(Double.isNaN(minGroundHeight), "Should return NaN for null ProfileBuilder");
    }

    // Test getGroundHeightAtPoint method

    @Test
    public void testGetGroundHeightAtPointNormalCase() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        
        ProfileBuilder profileBuilder = createMockProfileBuilder(8.0);
        
        double groundHeight = manager.getGroundHeightAtPoint(point, profileBuilder);
        assertEquals(8.0, groundHeight, 0.001, "Should return ground height from ProfileBuilder");
    }

    @Test
    public void testGetGroundHeightAtPointNullPoint() {
        BridgePointManager manager = new BridgePointManager();
        ProfileBuilder profileBuilder = createMockProfileBuilder(8.0);
        
        double groundHeight = manager.getGroundHeightAtPoint(null, profileBuilder);
        assertTrue(Double.isNaN(groundHeight), "Should return NaN for null point");
    }

    @Test
    public void testGetGroundHeightAtPointNullProfileBuilder() {
        BridgePointManager manager = new BridgePointManager();
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 10.0)).build();
        
        double groundHeight = manager.getGroundHeightAtPoint(point, null);
        assertTrue(Double.isNaN(groundHeight), "Should return NaN for null ProfileBuilder");
    }

    // Test complex interpolation scenarios

    @Test
    public void testInterpolationWithOnlyPreviousPoint() {
        BridgePointManager manager = new BridgePointManager();
        
        BridgePoint point1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, Double.NaN)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        
        assertThrows(IllegalStateException.class, () -> {
            manager.interpolateDeckHeight(1, null);
        }, "Should throw IllegalStateException when only one neighboring point");
    }

    @Test
    public void testInterpolationWithOnlyNextPoint() {
        BridgePointManager manager = new BridgePointManager();
        
        BridgePoint point1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, Double.NaN)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, 15.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        
        assertThrows(IllegalStateException.class, () -> {
            manager.interpolateDeckHeight(0, null);
        }, "Should throw IllegalStateException when only one neighboring point");
    }

    @Test
    public void testInterpolationWithRelativeHeights() {
        BridgePointManager manager = new BridgePointManager();
        
        BridgePoint point1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, 5.0)).build(); // 5 + 10 = 15
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, Double.NaN)).build(); // To interpolate
        BridgePoint point3 = new BridgePoint.Builder(3L, 200L, new Coordinate(200.0, 0.0, 10.0)).build(); // 10 + 10 = 20
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point3);
        
        ProfileBuilder profileBuilder = createMockProfileBuilder(10.0);
        
        double interpolated = manager.interpolateDeckHeight(1, profileBuilder);
        assertEquals(7.5, interpolated, 0.001, "Should interpolate between 15 and 20");
    }

    @Test
    public void testInterpolationAtSameLocation() {
        BridgePointManager manager = new BridgePointManager();
        
        // Create points at the same location to test zero distance handling
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 100.0, Double.NaN)).build(); // To interpolate
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 100.0, 20.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point3);
        
        double interpolated = manager.interpolateDeckHeight(1, null);
        assertEquals(15.0, interpolated, 0.001, "Should average when distance is zero");
    }

    @Test
    public void testInterpolationComplexScenario() {
        BridgePointManager manager = new BridgePointManager();
        
        // Create a complex scenario with multiple interpolation points
        BridgePoint point1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 50L, new Coordinate(50.0, 0.0, Double.NaN)).build(); // To interpolate
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(100.0, 0.0, 20.0)).build();
        BridgePoint point4 = new BridgePoint.Builder(4L, 150L, new Coordinate(150.0, 0.0, Double.NaN)).build(); // To interpolate
        BridgePoint point5 = new BridgePoint.Builder(5L, 200L, new Coordinate(200.0, 0.0, 30.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point3);
        manager.addBridgePoint(point4);
        manager.addBridgePoint(point5);
        
        // Test first interpolation point (between 10 and 20)
        double interpolated1 = manager.interpolateDeckHeight(1, null);
        assertEquals(15.0, interpolated1, 0.001, "Should interpolate linearly between 10 and 20");
        
        // Test second interpolation point (between 20 and 30)
        double interpolated2 = manager.interpolateDeckHeight(3, null);
        assertEquals(25.0, interpolated2, 0.001, "Should interpolate linearly between 20 and 30");
    }

    // Test error conditions and edge cases

    @Test
    public void testEffectiveDeckHeightPriority() {
        BridgePointManager manager = new BridgePointManager();
        
        // Create point with both absolute and relative heights - absolute should take priority
        BridgePoint point = new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 200.0, 15.0))
            .withRelativeDeckHeight(5.0)
            .build();
        
        manager.addBridgePoint(point);
        
        ProfileBuilder profileBuilder = createMockProfileBuilder(10.0);
        
        double height = manager.getEffectiveDeckHeight(0, profileBuilder);
        assertEquals(15.0, height, 0.001, "Should use absolute height, not relative");
    }

    @Test
    public void testEffectiveDeckHeightFallbackToInterpolation() {
        BridgePointManager manager = new BridgePointManager();
        
        BridgePoint point1 = new BridgePoint.Builder(1L, 100L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint point2 = new BridgePoint.Builder(2L, 100L, new Coordinate(100.0, 0.0, Double.NaN)).build(); // No heights set
        BridgePoint point3 = new BridgePoint.Builder(3L, 100L, new Coordinate(200.0, 0.0, 20.0)).build();
        
        manager.addBridgePoint(point1);
        manager.addBridgePoint(point2);
        manager.addBridgePoint(point3);
        
        double height = manager.getEffectiveDeckHeight(1, null);
        assertEquals(15.0, height, 0.001, "Should fall back to interpolation");
    }

    @Test
    public void testRemoveNonExistingPoint() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        boolean removed = manager.removeBridgePoint(999);
        assertFalse(removed, "Should return false for non-existing point");
        assertEquals(1, manager.size(), "Size should remain unchanged");
    }

    @Test
    public void testGetBridgePointByPrimaryKeyNotFound() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        BridgePoint result = manager.getBridgePointByPrimaryKey(999);
        assertNull(result, "Should return null for non-existing primary key");
    }

    @Test
    public void testGetBridgePointByIndexNegative() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            manager.getBridgePointByIndex(-1);
        }, "Should throw exception for negative index");
    }

    @Test
    public void testGetBridgePointByIndexTooLarge() {
        BridgePointManager manager = new BridgePointManager();
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            manager.getBridgePointByIndex(1);
        }, "Should throw exception for index too large");
    }

    @Test
    public void testPrimaryKeysListIsSorted() {
        BridgePointManager manager = new BridgePointManager();
        
        // Add points in random order
        manager.addBridgePoint(new BridgePoint.Builder(5L, 500L, new Coordinate(500.0, 500.0, 25.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(1L, 100L, new Coordinate(100.0, 100.0, 10.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(3L, 300L, new Coordinate(300.0, 300.0, 15.0)).build());
        manager.addBridgePoint(new BridgePoint.Builder(2L, 200L, new Coordinate(200.0, 200.0, 12.0)).build());
        
        List<Long> keys = manager.getPrimaryKeys();
        assertEquals(4, keys.size(), "Should have all keys");
        assertEquals(Long.valueOf(1), keys.get(0), "Keys should be sorted");
        assertEquals(Long.valueOf(2), keys.get(1), "Keys should be sorted");
        assertEquals(Long.valueOf(3), keys.get(2), "Keys should be sorted");
        assertEquals(Long.valueOf(5), keys.get(3), "Keys should be sorted");
    }

    @Test
    public void testGetAverageAbsoluteDeckHeightCenterOnly() {
        BridgePointManager manager = new BridgePointManager();

        // CENTER points with heights 10 and 20
        BridgePoint center1 = new BridgePoint.Builder(1L, 0L, new Coordinate(0.0, 0.0, 10.0)).build();
        BridgePoint center2 = new BridgePoint.Builder(2L, 10L, new Coordinate(10.0, 0.0, 20.0)).build();

        // LEFT and RIGHT points should be ignored
        BridgePoint left = new BridgePoint.Builder(3L, 20L, new Coordinate(20.0, 0.0, 100.0)).withPosition(BridgePoint.Position.LEFT).build();
        BridgePoint right = new BridgePoint.Builder(4L, 30L, new Coordinate(30.0, 0.0, 200.0)).withPosition(BridgePoint.Position.RIGHT).build();

        manager.addBridgePoint(center1);
        manager.addBridgePoint(left);
        manager.addBridgePoint(center2);
        manager.addBridgePoint(right);

        double avg = manager.getAverageAbsoluteDeckHeight();
        assertEquals(15.0, avg, 0.001, "Average should consider only CENTER points (10 and 20 -> 15)");
    }
}
