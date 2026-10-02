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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * BridgeTriangulation handles the triangulation of bridge deck geometry for efficient interpolation.
 * 
 * Primary Responsibilities:
 * - Creating triangles from bridge deck polygon vertices
 * - Point-in-triangle detection using barycentric coordinates
 * - Height interpolation within triangles (deck height, thickness)
 * - Barrier height interpolation along triangle edges
 * 
 * This class focuses purely on geometric interpolation operations and does not handle:
 * - Bridge point management (delegated to BridgePointManager)
 * - Spatial queries beyond triangular interpolation (delegated to BridgeQueryHelper)
 * - Geometry construction (delegated to BridgeGeometryBuilder)
 */
public class BridgeTriangulation {
    
    /** List of triangles for interpolation */
    private List<Triangle> deckTriangles = new ArrayList<>();
    
    /** Geometry factory for creating JTS geometries */
    private final GeometryFactory geometryFactory;
    
    /**
     * Create BridgeTriangulation with geometry factory.
     * @param geometryFactory Geometry factory for JTS operations
     */
    public BridgeTriangulation(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }
    
    /**
     * Simple triangle representation for interpolation.
     */
    public static class Triangle {
        final BridgePoint bridgePoint1, bridgePoint2, bridgePoint3;
        final Coordinate p1, p2, p3;
        
        Triangle(BridgePoint bridgePoint1, BridgePoint bridgePoint2, BridgePoint bridgePoint3) {
            this.bridgePoint1 = bridgePoint1;
            this.bridgePoint2 = bridgePoint2;
            this.bridgePoint3 = bridgePoint3;
            this.p1 = bridgePoint1.getCoordinate();
            this.p2 = bridgePoint2.getCoordinate();
            this.p3 = bridgePoint3.getCoordinate();
        }
        
        
        List<Double> interpolateWeight(Coordinate point) {
            List<Double> weights = new ArrayList<>();
            double denom = (p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.y - p3.y);
            if (Math.abs(denom) < 1e-10) return weights; // Degenerate triangle
            
            double a = ((p2.y - p3.y) * (point.x - p3.x) + (p3.x - p2.x) * (point.y - p3.y)) / denom;
            double b = ((p3.y - p1.y) * (point.x - p3.x) + (p1.x - p3.x) * (point.y - p3.y)) / denom;
            double c = 1 - a - b;
            weights.add(a);
            weights.add(b);
            weights.add(c);
            return weights;
        }
          
        /**
         * Check if point is inside this triangle using barycentric coordinates.
         * Uses a small tolerance to handle numerical precision issues and include boundary points.
         */
        boolean contains(Coordinate point) {
            List<Double> weights = interpolateWeight(point);
            
            // Check if weights list is empty (degenerate triangle)
            if (weights.isEmpty()) {
                return false;
            }
            
            // Small tolerance for numerical precision and boundary inclusion
            final double tolerance = 1e-8;
            return weights.get(0) >= -tolerance && weights.get(1) >= -tolerance && weights.get(2) >= -tolerance;
        }

        
        /**
         * Check if point is on an outer edge of the triangle.
         * An outer edge is defined as an edge where both endpoints have the same position (LEFT or RIGHT).
         * Uses a small tolerance to handle numerical precision issues.
         */
        boolean onOuterEdge(Coordinate point) {
            List<Double> weights = interpolateWeight(point);
            
            // Check if weights list is empty (degenerate triangle)
            if (weights.isEmpty()) {
                return false;
            }
            
            // Small tolerance for numerical precision and boundary inclusion
            final double tolerance = 1e-3;
            
            // Check each edge and whether it's an outer edge
            boolean on23Edge = weights.get(0) < tolerance; // Edge between points 2 and 3
            boolean on31Edge = weights.get(1) < tolerance; // Edge between points 3 and 1  
            boolean on12Edge = weights.get(2) < tolerance; // Edge between points 1 and 2
            
            // An edge is "outer" if both endpoints have the same position (LEFT or RIGHT)
            boolean is23Outer = bridgePoint2.getPosition() == bridgePoint3.getPosition() && 
                               bridgePoint2.getPosition() != BridgePoint.Position.CENTER;
            boolean is31Outer = bridgePoint3.getPosition() == bridgePoint1.getPosition() && 
                               bridgePoint3.getPosition() != BridgePoint.Position.CENTER;
            boolean is12Outer = bridgePoint1.getPosition() == bridgePoint2.getPosition() && 
                               bridgePoint1.getPosition() != BridgePoint.Position.CENTER;
            
            return (on23Edge && is23Outer) || (on31Edge && is31Outer) || (on12Edge && is12Outer);
        }

        /**
         * Interpolate Z value using barycentric coordinates.
         */
        double interpolateDeckHeight(Coordinate point) {
            double deckHeight1 = bridgePoint1.getAbsoluteDeckHeight();
            double deckHeight2 = bridgePoint2.getAbsoluteDeckHeight();
            double deckHeight3 = bridgePoint3.getAbsoluteDeckHeight();

            if (Double.isNaN(deckHeight1) || Double.isNaN(deckHeight2) || Double.isNaN(deckHeight3)) {
                return Double.NaN;
            }

            List<Double> weights = interpolateWeight(point);
            
            // Check if weights list is empty (degenerate triangle)
            if (weights.isEmpty()) {
                return Double.NaN;
            }
            
            return weights.get(0) * deckHeight1 + weights.get(1) * deckHeight2 + weights.get(2) * deckHeight3;
        }
        
        double interpolateDeckThickness(Coordinate point) {
            double deckThickness1 = bridgePoint1.getDeckThickness();
            double deckThickness2 = bridgePoint2.getDeckThickness();
            double deckThickness3 = bridgePoint3.getDeckThickness();

            if (Double.isNaN(deckThickness1) || Double.isNaN(deckThickness2) || Double.isNaN(deckThickness3)) {
                return Double.NaN;
            }

            List<Double> weights = interpolateWeight(point);

            // Check if weights list is empty (degenerate triangle)
            if (weights.isEmpty()) {
                return Double.NaN;
            }

            return weights.get(0) * deckThickness1 + weights.get(1) * deckThickness2 + weights.get(2) * deckThickness3;

        }

        /**
         * Interpolate barrier height for a point on triangle edges.
         * Returns interpolated barrier height if the point is on an edge and both endpoints
         * of that edge belong to the same side (both right or both left), otherwise returns 0.
         * @param point Point coordinate to interpolate barrier height for
         * @return Interpolated barrier height, or 0 if conditions are not met
         */
        double interpolateBarrier(Coordinate point) {
            double barrierHeight1;
            double barrierHeight2;
            double barrierHeight3;

            if (bridgePoint1.getPosition() == BridgePoint.Position.LEFT) {
                barrierHeight1 = bridgePoint1.getLeftBarrierHeight();
            } else {
                barrierHeight1 = bridgePoint1.getRightBarrierHeight();
            }
            if (bridgePoint2.getPosition() == BridgePoint.Position.LEFT) {
                barrierHeight2 = bridgePoint2.getLeftBarrierHeight();
            } else {
                barrierHeight2 = bridgePoint2.getRightBarrierHeight();
            }
            if (bridgePoint3.getPosition() == BridgePoint.Position.LEFT) {
                barrierHeight3 = bridgePoint3.getLeftBarrierHeight();
            } else {
                barrierHeight3 = bridgePoint3.getRightBarrierHeight();
            }

            if (Double.isNaN(barrierHeight1) || Double.isNaN(barrierHeight2) || Double.isNaN(barrierHeight3)) {
                return 0.0;
            }
            if (!onOuterEdge(point)) {
                return 0.0;
            }
            
            List<Double> weights = interpolateWeight(point);

            // Check if weights list is empty (degenerate triangle)
            if (weights.isEmpty()) {
                return 0.0;
            }

            return weights.get(0) * barrierHeight1 + weights.get(1) * barrierHeight2 + weights.get(2) * barrierHeight3;
        }
        
    }
    
    
    /**
     * Triangulate the deck geometry for efficient interpolation.
     * This method only accepts bridge point lists where LEFT and RIGHT positions are equal in count.
     * @param bridgeEdgePoints List of bridge points forming the deck polygon
     * @throws IllegalArgumentException if LEFT and RIGHT positions are not equal in count
     */
    public void triangulateGeometry(List<BridgePoint> bridgeEdgePoints) {
        deckTriangles.clear();
        
        if (bridgeEdgePoints.isEmpty()) return;

        int numVertices = bridgeEdgePoints.size();

        if (numVertices < 3) return; // Invalid polygon - need at least 3 points
        
        // Validate that LEFT and RIGHT positions are equal in count
        validatePositionBalance(bridgeEdgePoints);
        
        // Use Delaunay triangulation for better coverage
        List<Coordinate> coordinates = new ArrayList<>();
        for (BridgePoint point : bridgeEdgePoints) {
            coordinates.add(point.getCoordinate());
        }
        
        DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
        builder.setSites(coordinates);
        
        // Get the triangulation
        org.locationtech.jts.geom.Geometry triangulation = builder.getTriangles(geometryFactory);
        
        // Extract triangles
        for (int i = 0; i < triangulation.getNumGeometries(); i++) {
            org.locationtech.jts.geom.Polygon poly = (org.locationtech.jts.geom.Polygon) triangulation.getGeometryN(i);
            Coordinate[] coords = poly.getCoordinates();
            if (coords.length >= 4) { // Triangle has 4 coords (closed)
                // Find corresponding BridgePoints
                BridgePoint bp1 = findBridgePointByCoordinate(bridgeEdgePoints, coords[0]);
                BridgePoint bp2 = findBridgePointByCoordinate(bridgeEdgePoints, coords[1]);
                BridgePoint bp3 = findBridgePointByCoordinate(bridgeEdgePoints, coords[2]);
                if (bp1 != null && bp2 != null && bp3 != null) {
                    deckTriangles.add(new Triangle(bp1, bp2, bp3));
                }
            }
        }
    }
    
    /**
     * Find BridgePoint by coordinate from the list.
     * @param bridgeEdgePoints List of bridge points
     * @param coord Coordinate to find
     * @return BridgePoint or null if not found
     */
    private BridgePoint findBridgePointByCoordinate(List<BridgePoint> bridgeEdgePoints, Coordinate coord) {
        for (BridgePoint point : bridgeEdgePoints) {
            if (point.getCoordinate().equals2D(coord)) {
                return point;
            }
        }
        return null;
    }
    
    /**
     * Validate that the bridge points have equal numbers of LEFT and RIGHT positions.
     * @param bridgeEdgePoints List of bridge points to validate
     * @throws IllegalArgumentException if LEFT and RIGHT positions are not equal in count
     */
    private void validatePositionBalance(List<BridgePoint> bridgeEdgePoints) {
        int leftCount = 0;
        int rightCount = 0;
        
        for (BridgePoint point : bridgeEdgePoints) {
            if (point.getPosition() == BridgePoint.Position.LEFT) {
                leftCount++;
            } else if (point.getPosition() == BridgePoint.Position.RIGHT) {
                rightCount++;
            }
        }
        
        if (leftCount != rightCount) {
            throw new IllegalArgumentException(
                String.format("Bridge points must have equal numbers of LEFT and RIGHT positions. " +
                            "Found %d LEFT and %d RIGHT positions.", leftCount, rightCount)
            );
        }
    }
    
    /**
     * Check if three coordinates are all different from each other.
     * Uses a small tolerance for numerical precision.
     * @param coord1 First coordinate
     * @param coord2 Second coordinate  
     * @param coord3 Third coordinate
     * @return true if all three coordinates are different, false otherwise
     */
    private boolean areCoordinatesDifferent(Coordinate coord1, Coordinate coord2, Coordinate coord3) {
        final double tolerance = 1e-10;
        
        // Check if coord1 and coord2 are different
        boolean coord1Coord2Different = Math.abs(coord1.x - coord2.x) > tolerance || 
                                       Math.abs(coord1.y - coord2.y) > tolerance ||
                                       Math.abs(coord1.z - coord2.z) > tolerance;
        
        // Check if coord2 and coord3 are different
        boolean coord2Coord3Different = Math.abs(coord2.x - coord3.x) > tolerance || 
                                       Math.abs(coord2.y - coord3.y) > tolerance ||
                                       Math.abs(coord2.z - coord3.z) > tolerance;
        
        // Check if coord1 and coord3 are different
        boolean coord1Coord3Different = Math.abs(coord1.x - coord3.x) > tolerance || 
                                       Math.abs(coord1.y - coord3.y) > tolerance ||
                                       Math.abs(coord1.z - coord3.z) > tolerance;
        
        return coord1Coord2Different && coord2Coord3Different && coord1Coord3Different;
    }
    
    
    /**
     * Get absolute deck height at a point using triangular interpolation.
     * @param point Point coordinate
     * @return Absolute deck height at the point, or NaN if not found
     */
    public double getDeckHeightAtPoint(Coordinate point) {
        if (deckTriangles.isEmpty()) {
            return Double.NaN;
        }
        
        // Find triangle containing the point and interpolate
        for (Triangle triangle : deckTriangles) {
            if (triangle.contains(point)) {
                return triangle.interpolateDeckHeight(point);
            }
        }
        
        return Double.NaN;
    }

    
    /**
     * Get deck thickness at a point using triangular interpolation.
     * @param point Point coordinate
     * @return Absolute deck thickness at the point, or NaN if not found
     */
    public double getDeckThicknessAtPoint(Coordinate point) {
        if (deckTriangles.isEmpty()) {
            return Double.NaN;
        }
        
        // Find triangle containing the point and interpolate
        for (Triangle triangle : deckTriangles) {
            if (triangle.contains(point)) {
                return triangle.interpolateDeckThickness(point);
            }
        }
        
        return Double.NaN;
    }
    
    /**
     * Get the triangle containing the specified point.
     * @param point Point coordinate
     * @return Triangle containing the point, or null if not found
     */
    public Triangle getTriangleContainingPoint(Coordinate point) {
        if (deckTriangles.isEmpty()) {
            return null;
        }
        
        // Find triangle containing the point
        for (Triangle triangle : deckTriangles) {
            if (triangle.contains(point)) {
                return triangle;
            }
        }
        
        return null;
    }
    
    /**
     * Get barrier height at a point using triangle edge interpolation.
     * @param point Point coordinate
     * @return Barrier height at the point
     */
    public double getBarrierHeightAtPoint(Coordinate point) {
        if (deckTriangles.isEmpty()) {
            return 0.0;
        }
        
        // Find triangle containing the point and interpolate barrier height
        for (Triangle triangle : deckTriangles) {
            if (triangle.contains(point)) {
                return triangle.interpolateBarrier(point);
            }
        }
        
        return 0.0;
    }

    
    /**
     * Get all triangles in the deck geometry.
     * @return List of all triangles (read-only copy)
     */
    public List<Triangle> getTriangles() {
        return new ArrayList<>(deckTriangles);
    }
    
    /**
     * Clear all triangulation data.
     */
    public void clear() {
        deckTriangles.clear();
    }
    
    /**
     * Check if triangulation has been performed.
     * @return true if triangles exist
     */
    public boolean hasTriangles() {
        return !deckTriangles.isEmpty();
    }
}
