/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BridgePointManager handles the collection and management of BridgePoint objects.
 * 
 * Primary Responsibilities:
 * - Managing bridge points collection (add, remove, sort)
 * - Multiple sorting strategies (by primary key, counter-clockwise, side-to-side)
 * - Height interpolation for points with missing elevation data
 * - Ground surface height calculation using ProfileBuilder integration
 * - Point lookup and validation operations
 * 
 * This class focuses on bridge point data management and does not handle:
 * - Geometric interpolation within triangles (delegated to BridgeTriangulation)
 * - Spatial queries beyond point management (delegated to BridgeQueryHelper)
 * - 3D geometry construction (delegated to BridgeGeometryBuilder)
 */
public class BridgePointManager {
  
    public enum SortOrder {
        BY_PRIMARY_KEY,
        CLOCKWISE,
        SIDE_TO_SIDE
    }
  

    private SortOrder sortOrder = SortOrder.BY_PRIMARY_KEY;

    /** List of bridge points sorted by primary key */
    private List<BridgePoint> bridgePoints;
    
    /**
     * Constructor with empty point collection.
     */
    public BridgePointManager() {
        this.bridgePoints = new ArrayList<>();
    }
    
    /**
     * Constructor with initial bridge points.
     * @param bridgePoints Initial list of bridge points
     * @param sortOrder Sorting order for the bridge points
     */
    public BridgePointManager(List<BridgePoint> bridgePoints, SortOrder sortOrder) {
        this.bridgePoints = new ArrayList<>(bridgePoints);
        this.sortOrder = sortOrder;
        sortBridgePoints();
    }
    
    /**
     * Constructor with initial bridge points.
     * @param bridgePoints Initial list of bridge points
     */
    public BridgePointManager(List<BridgePoint> bridgePoints) {
        this.bridgePoints = new ArrayList<>(bridgePoints);
        sortByPrimaryKey();
    }
    
    /**
     * Constructor with initial bridge points.
     * @param sortOrder Sorting order for the bridge points
     */
    public BridgePointManager(SortOrder sortOrder) {
        this.bridgePoints = new ArrayList<>();
        this.sortOrder = sortOrder;
    }
    
    /**
     * Add a bridge point to the collection.
     * @param bridgePoint Bridge point to add
     */
    public void addBridgePoint(BridgePoint bridgePoint) {
        if (bridgePoint != null) {
            bridgePoints.add(bridgePoint);
            sortBridgePoints();
        }
    }
    
    /**
     * Add a bridge point to the collection.
     * @param bridgePoints Bridge points to add
     */
    public void addBridgePoints(List<BridgePoint> bridgePoints) {
        if (bridgePoints != null) {
            for (int i = 0; i < bridgePoints.size(); i++) {
                if (bridgePoints.get(i) != null) {
                    this.bridgePoints.add(bridgePoints.get(i));
                }
            }
            sortBridgePoints();
        }
    }
    /**
     * Remove a bridge point from the collection by primary key.
     * @param primaryKey Primary key of the bridge point to remove
     * @return true if the point was removed
     */
    public boolean removeBridgePoint(long primaryKey) {
        return bridgePoints.removeIf(point -> point.getPrimaryKey() == primaryKey);
    }
    
    /**
     * Sort bridge points according to the current sort order.
     */
    private void sortBridgePoints() {
        if (sortOrder == SortOrder.BY_PRIMARY_KEY) {
            sortByPrimaryKey();
        } else if (sortOrder == SortOrder.CLOCKWISE) {
            sortClockwise();
        } else if (sortOrder == SortOrder.SIDE_TO_SIDE) {
            sortSideToSide();
        }
    }
    
    /**
     * Sort bridge points by position first, then by primary key.
     */
    private void sortByPrimaryKey() {
        Collections.sort(
          bridgePoints, 
          Comparator.comparing(BridgePoint::getPosition).thenComparingLong(BridgePoint::getPrimaryKey)
        );
    }
    
    /**
     * Sort bridge points in counter-clockwise order for polygon construction.
     * Filters to only include LEFT and RIGHT position points and orders them
     * with RIGHT points first (in ascending primary key order) followed by
     * LEFT points (in descending primary key order).
     */
    private void sortClockwise() {
        bridgePoints = bridgePoints.stream()
            .filter(point -> {
                BridgePoint.Position pos = point.getPosition();
                return pos == BridgePoint.Position.RIGHT || pos == BridgePoint.Position.LEFT;
            })
            .sorted(Comparator
                .<BridgePoint, Integer>comparing(point -> 
                    point.getPosition() == BridgePoint.Position.LEFT ? 0 : 1)
                .thenComparing(point -> 
                    point.getPosition() == BridgePoint.Position.LEFT ? 
                        point.getPrimaryKey() : -point.getPrimaryKey()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Sort bridge points in side-to-side order for triangulation.
     * Groups points by primary key, then by position (LEFT/RIGHT).
     */
    private void sortSideToSide() {
        bridgePoints = bridgePoints.stream()
            .filter(point -> {
                BridgePoint.Position pos = point.getPosition();
                return pos == BridgePoint.Position.RIGHT || pos == BridgePoint.Position.LEFT;
            })        
            .sorted(
                Comparator.comparingLong(BridgePoint::getPrimaryKey).thenComparing(BridgePoint::getPosition)
            )
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Update minimum ground surface height using ProfileBuilder.
     * Similar to Building.updateZTopo method.
     * @param profileBuilder Profile builder for ground height calculation
     * @return Minimum ground surface height
     */
    public double updateGroundHeight(ProfileBuilder profileBuilder) {
        if (bridgePoints.isEmpty() || profileBuilder == null) {
            return Double.NaN;
        }
        
        double minZ = Double.MAX_VALUE;
        AtomicInteger triangleHint = new AtomicInteger(-1);
        
        for (BridgePoint point : bridgePoints) {
            if (point.getCoordinate() != null) {
                double groundZ = profileBuilder.getZGround(point.getCoordinate(), triangleHint);
                minZ = Math.min(minZ, groundZ);
            }
        }
        
        return (minZ == Double.MAX_VALUE) ? Double.NaN : minZ;
    }
    
    /**
     * Get ground surface height at a specific bridge point.
     * @param point Bridge point
     * @param profileBuilder Profile builder for ground height calculation
     * @return Ground surface height at the point
     */
    public double getGroundHeightAtPoint(BridgePoint point, ProfileBuilder profileBuilder) {
        if (point == null || point.getCoordinate() == null || profileBuilder == null) {
            return Double.NaN;
        }
        
        AtomicInteger triangleHint = new AtomicInteger(-1);
        return profileBuilder.getZGround(point.getCoordinate(), triangleHint);
    }
    
    /**
     * Get the effective height for a bridge point.
     * Uses absolute height if available, relative height + ground elevation at the point if relative height is available,
     * or interpolated value if both are NaN.
     * @param index Index of the point in the sorted list
     * @param profileBuilder Profile builder for ground height calculation
     * @return Effective height
     */
    public double getEffectiveDeckHeight(int index, ProfileBuilder profileBuilder) {
        BridgePoint point = bridgePoints.get(index);
        double absoluteHeight = point.getAbsoluteDeckHeight();
        double relativeHeight = point.getRelativeDeckHeight();
        
        // Use absolute height if available
        if (!Double.isNaN(absoluteHeight)) {
            return absoluteHeight;
        }
        
        // Use relative height + ground elevation at this point if available
        if (!Double.isNaN(relativeHeight)) {
            double groundHeight = getGroundHeightAtPoint(point, profileBuilder);
            if (!Double.isNaN(groundHeight)) {
                return groundHeight + relativeHeight;
            }
        }
        
        // Interpolate if both are NaN
        return interpolateDeckHeight(index, profileBuilder);
    }
    
    /**
     * Interpolate deck height for a point at given index using neighboring points.
     * Uses distance-based weighting for interpolation.
     * @param index Index of the point to interpolate
     * @param profileBuilder Profile builder for ground height calculation
     * @return Interpolated deck height
     */
    public double interpolateDeckHeight(int index, ProfileBuilder profileBuilder) {
        if (bridgePoints.isEmpty()) {
            throw new IllegalStateException("No bridge points available for interpolation");
        }
        
        // Find previous point with valid deck height
        double prevDeckHeight = Double.NaN;
        Coordinate prevCoord = null;
        for (int i = index - 1; i >= 0; i--) {
            double deckHeight = getValidDeckHeight(bridgePoints.get(i), profileBuilder);
            if (!Double.isNaN(deckHeight)) {
                prevDeckHeight = deckHeight;
                prevCoord = bridgePoints.get(i).getCoordinate();
                break;
            }
        }
        
        // Find next point with valid deck height
        double nextDeckHeight = Double.NaN;
        Coordinate nextCoord = null;
        for (int i = index + 1; i < bridgePoints.size(); i++) {
            double deckHeight = getValidDeckHeight(bridgePoints.get(i), profileBuilder);
            if (!Double.isNaN(deckHeight)) {
                nextDeckHeight = deckHeight;
                nextCoord = bridgePoints.get(i).getCoordinate();
                break;
            }
        }
        
        // Get current point coordinate for distance calculation
        Coordinate currentCoord = bridgePoints.get(index).getCoordinate();
        
        // Interpolate based on available data
        if (!Double.isNaN(prevDeckHeight) && !Double.isNaN(nextDeckHeight) && 
            prevCoord != null && nextCoord != null && currentCoord != null) {
            
            // Calculate distances
            double distToPrev = calculateDistance(currentCoord, prevCoord);
            double distToNext = calculateDistance(currentCoord, nextCoord);
            double totalDist = distToPrev + distToNext;
            
            if (totalDist > 0) {
                // Distance-based weighted interpolation (inverse distance weighting)
                double weightPrev = distToNext / totalDist; // Further point gets less weight
                double weightNext = distToPrev / totalDist; // Further point gets less weight
                return prevDeckHeight * weightPrev + nextDeckHeight * weightNext;
            } else {
                // Points are at the same location, use average
                return (prevDeckHeight + nextDeckHeight) / 2.0;
            }
        } else {
            // No valid interpolation possible - return error value
            throw new IllegalStateException("No valid deck height available for interpolation");
        }
    }
    
    /**
     * Calculate Euclidean distance between two coordinates.
     * @param coord1 First coordinate
     * @param coord2 Second coordinate
     * @return Distance between coordinates
     */
    private double calculateDistance(Coordinate coord1, Coordinate coord2) {
        if (coord1 == null || coord2 == null) {
            return 0.0;
        }
        double dx = coord1.x - coord2.x;
        double dy = coord1.y - coord2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Get valid deck height from a bridge point (absolute or relative + ground at this point).
     * @param point Bridge point
     * @param profileBuilder Profile builder for ground height calculation
     * @return Valid deck height or NaN if both absolute and relative heights are NaN
     */
    private double getValidDeckHeight(BridgePoint point, ProfileBuilder profileBuilder) {
        double absoluteHeight = point.getAbsoluteDeckHeight();
        if (!Double.isNaN(absoluteHeight)) {
            return absoluteHeight;
        }
        
        double relativeHeight = point.getRelativeDeckHeight();
        if (!Double.isNaN(relativeHeight)) {
            double groundHeight = getGroundHeightAtPoint(point, profileBuilder);
            if (!Double.isNaN(groundHeight)) {
                return groundHeight + relativeHeight;
            }
        }
        
        return Double.NaN;
    }
    
    /**
     * Calculate the average absolute deck height across all bridge points.
     * Only points with a finite absolute deck height are considered.
     * @return average absolute deck height or Double.NaN if no valid values
     */
    public double getAverageAbsoluteDeckHeight() {
        if (bridgePoints.isEmpty()) {
            return Double.NaN;
        }

        double sum = 0.0;
        int count = 0;
        for (BridgePoint point : bridgePoints) {
            // consider only points that are in CENTER position
            BridgePoint.Position pos = point.getPosition();
            if (pos != BridgePoint.Position.CENTER) {
                continue;
            }

            double h = point.getAbsoluteDeckHeight();
            if (!Double.isNaN(h) && !Double.isInfinite(h)) {
                sum += h;
                count++;
            }
        }

        return (count == 0) ? Double.NaN : (sum / count);
    }
    
    // Getters and Setters
    
    /**
     * Get the list of bridge points (read-only).
     * @return Copy of bridge points list
     */
    public List<BridgePoint> getBridgePoints() {
        return new ArrayList<>(bridgePoints);
    }
    
    /**
     * Get the number of bridge points.
     * @return Number of bridge points
     */
    public int size() {
        return bridgePoints.size();
    }
    
    /**
     * Check if the bridge points collection is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return bridgePoints.isEmpty();
    }
    
    /**
     * Clear all bridge points.
     */
    public void clear() {
        bridgePoints.clear();
    }
    
    /**
     * Get bridge point at specified index.
     * @param index Index
     * @return Bridge point at index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public BridgePoint getBridgePointByIndex(int index) {
        return bridgePoints.get(index);
    }
    
    /**
     * Get list of all primary keys from bridge points.
     * @return List of primary keys in sorted order
     */
    public List<Long> getPrimaryKeys() {
        List<Long> primaryKeys = new ArrayList<>();
        for (BridgePoint point : bridgePoints) {
            primaryKeys.add(point.getPrimaryKey());
        }
        return primaryKeys;
    }
    
    /**
     * Get bridge point by primary key.
     * @param primaryKey Primary key to search for
     * @return Bridge point with the specified primary key, or null if not found
     */
    public BridgePoint getBridgePointByPrimaryKey(long primaryKey) {
        for (BridgePoint point : bridgePoints) {
            if (point.getPrimaryKey() == primaryKey) {
                return point;
            }
        }
        return null;
    }
}
