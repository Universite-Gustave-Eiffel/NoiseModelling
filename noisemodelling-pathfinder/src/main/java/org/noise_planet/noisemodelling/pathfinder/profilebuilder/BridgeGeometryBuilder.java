/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.*;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.BridgePoint.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * BridgeGeometryBuilder is responsible for creating bridge deck polygons from bridge points.
 * 
 * Primary Responsibilities:
 * - Creating 3D bridge deck polygons with left and right width offsets
 * - Calculating offset coordinates using perpendicular vectors
 * - Managing bridge edge creation for acoustic calculations
 * - Coordinate transformations and vector operations
 * 
 * This class focuses on 3D geometry construction and does not handle:
 * - Bridge point data management (delegated to BridgePointManager)
 * - Interpolation within created geometries (delegated to BridgeTriangulation)
 * - Spatial queries on constructed geometries (delegated to BridgeQueryHelper)
 */
public class BridgeGeometryBuilder {
  
    /** Geometry factory for creating geometries */
    private GeometryFactory geometryFactory;
    
    /**
     * Constructor.
     */
    public BridgeGeometryBuilder() {
    this.geometryFactory = GeometryFactoryProvider.SHARED;
    }
    

    /**
     * Constructor with custom geometry factory.
     * @param geometryFactory Custom geometry factory
     */
    public BridgeGeometryBuilder(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory != null ? geometryFactory : GeometryFactoryProvider.SHARED;
    }
    
    public List<BridgePoint> createBridgeEdgePoints(BridgePointManager pointManager, ProfileBuilder profileBuilder, Position direction, boolean addBarrierHeight) {
        if (pointManager == null) {
            throw new IllegalArgumentException("Point manager is required to create bridge edge points");
        }
        
        List<BridgePoint> bridgeCenterPoints = pointManager.getBridgePoints();
        
        if (bridgeCenterPoints.size() < 2) {
            return null; // Need at least 2 points to create a polygon
        }
        
        List<BridgePoint> bridgeEdgePoints = new ArrayList<>();
        
        for (int i = 0; i < bridgeCenterPoints.size(); i++) {
            BridgePoint point = new BridgePoint(bridgeCenterPoints.get(i));
            Coordinate centerCoord = point.getCoordinate();
            double width = point.getWidth(direction);
            double effectiveHeight = pointManager.getEffectiveDeckHeight(i, profileBuilder);
            double barrierHeight = addBarrierHeight ? point.getBarrierHeight(direction) : 0.0;

            if (Double.isNaN(width)) {
                width = 0.0; // Default to center line if width is not specified
            }
            
            Coordinate coord = calculateOffsetCoordinate(centerCoord, width, direction, i, bridgeCenterPoints);
            coord.z = effectiveHeight + barrierHeight;
            point.setCoordinate(coord);
            point.setAbsoluteDeckHeight(effectiveHeight);
            point.setPosition(direction);
            bridgeEdgePoints.add(point);
        }
        return bridgeEdgePoints;
    }

    

    /**
     * Create a bridge 2D footprint polygon from the bridge points.
     * The polygon is created by:
     * 1. Creating the center line from bridge points
     * 2. Adding left and right width offsets
     * @param pointManager Bridge point manager containing sorted bridge points
     * @return Bridge polygon with Z coordinates
     */
    public Polygon createFootprintGeometry(BridgePointManager pointManager) {
        if (pointManager == null) {
            throw new IllegalArgumentException("Point manager is required to create footprint geometry");
        }
        ProfileBuilder nullProfileBuilder = new ProfileBuilder();
        return createDeckGeometry(pointManager, nullProfileBuilder);
    }
    
    /**
     * Create a bridge deck 3D polygon from the bridge points.
     * The polygon is created by:
     * 1. Creating the center line from bridge points
     * 2. Adding left and right width offsets
     * 3. Using effective heights for Z coordinates
     * @param pointManager Bridge point manager containing sorted bridge points
     * @param profileBuilder Profile builder for ground height calculation
     * @return Bridge polygon with Z coordinates
     */
    public Polygon createDeckGeometry(BridgePointManager pointManager, ProfileBuilder profileBuilder) {
        if (pointManager == null || pointManager.size() < 2) {
            throw new IllegalArgumentException("Point manager required to create bridge geometry is null or has insufficient points");
        }
        
        // Create edge points for right and left sides
        List<BridgePoint> rightEdgePoints = createBridgeEdgePoints(pointManager, profileBuilder, Position.RIGHT, false);
        List<BridgePoint> leftEdgePoints = createBridgeEdgePoints(pointManager, profileBuilder, Position.LEFT, false);

        // Build polygon coordinates in correct order to avoid self-intersection:
        // Right side forward + Left side backward forms a closed ring
        List<Coordinate> coordinates = new ArrayList<>();
        
        // Add right edge points (forward direction)
        for (BridgePoint point : rightEdgePoints) {
            coordinates.add(point.getCoordinate());
        }
        
        // Add left edge points (reverse direction)
        for (int i = leftEdgePoints.size() - 1; i >= 0; i--) {
            coordinates.add(leftEdgePoints.get(i).getCoordinate());
        }
        
        // Close the ring
        coordinates.add(new Coordinate(coordinates.get(0)));
        
        Coordinate[] coordArray = coordinates.toArray(new Coordinate[0]);
        LinearRing ring = geometryFactory.createLinearRing(coordArray);
        return geometryFactory.createPolygon(ring);
    }
    
    
    /**
     * Initialize bridge edges for acoustic calculations.
        * @param pointManager Bridge point manager used to build edge geometry
        * @param profileBuilder Profile builder used for ground-related computations
     * @return List of bridge edges as LineString geometries
     */
    public Polygon createEdge(BridgePointManager pointManager, ProfileBuilder profileBuilder) {
        if (pointManager == null || pointManager.size() < 2) {
            throw new IllegalArgumentException("Point manager required to create bridge geometry is null or has insufficient points");
        }
        
        BridgePointManager pointManagerForEdge = new BridgePointManager(BridgePointManager.SortOrder.CLOCKWISE);
        List<BridgePoint> bridgeEdgePointsRight = createBridgeEdgePoints(pointManager, profileBuilder, Position.RIGHT, true);
        List<BridgePoint> bridgeEdgePointsLeft = createBridgeEdgePoints(pointManager, profileBuilder, Position.LEFT, true);
        pointManagerForEdge.addBridgePoints(bridgeEdgePointsRight);
        pointManagerForEdge.addBridgePoints(bridgeEdgePointsLeft);

        long origPointPk = pointManager.getBridgePointByIndex(0).getPrimaryKey();
        double origPointHeight = pointManager.getEffectiveDeckHeight(0, profileBuilder);
        List<BridgePoint> origBridgeEdgePoints = new ArrayList<>(List.of(
            new BridgePoint(bridgeEdgePointsRight.get(0)),
            new BridgePoint(bridgeEdgePointsLeft.get(0))
        ));
        for (BridgePoint p : origBridgeEdgePoints) {
            p.setPrimaryKey(origPointPk - 1);
            p.setCoordinate(new Coordinate(p.getCoordinate().x, p.getCoordinate().y, origPointHeight));
        }
        pointManagerForEdge.addBridgePoints(origBridgeEdgePoints);

        long termPointPk = pointManager.getBridgePointByIndex(pointManager.size() - 1).getPrimaryKey();
        double termPointHeight = pointManager.getEffectiveDeckHeight(pointManager.size() - 1, profileBuilder);
        List<BridgePoint> termBridgeEdgePoints = new ArrayList<>(List.of(
            new BridgePoint(bridgeEdgePointsRight.get(bridgeEdgePointsRight.size() - 1)),
            new BridgePoint(bridgeEdgePointsLeft.get(bridgeEdgePointsLeft.size() - 1))
        ));
        for (BridgePoint p : termBridgeEdgePoints) {
            p.setPrimaryKey(termPointPk + 1);
            p.setCoordinate(new Coordinate(p.getCoordinate().x, p.getCoordinate().y, termPointHeight));
        }
        pointManagerForEdge.addBridgePoints(termBridgeEdgePoints);

        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < pointManagerForEdge.size(); i++) {
            coordinates.add(pointManagerForEdge.getBridgePointByIndex(i).getCoordinate());
        }
        coordinates.add(new Coordinate(coordinates.get(0)));


        if (!coordinates.isEmpty()) {
            coordinates.add(new Coordinate(coordinates.get(0)));
        }
        
        Coordinate[] coordArray = coordinates.toArray(new Coordinate[0]);
        LinearRing ring = geometryFactory.createLinearRing(coordArray);
        return geometryFactory.createPolygon(ring);
    }

    
    /**
     * Initialize bridge edges for acoustic calculations.
     * @param deckGeometry Bridge deck polygon geometry
     * @return List of bridge edges as LineString geometries
     */
    public List<LineString> createEdges(Polygon deckGeometry) {
        List<LineString> edges = new ArrayList<>();
        
        if (deckGeometry == null) {
            return edges;
        }

        // Extract edges from bridge geometry
        LineString exteriorRing = deckGeometry.getExteriorRing();
        
        // Add exterior ring segments as potential diffraction edges
        Coordinate[] coords = exteriorRing.getCoordinates();
        
        for (int i = 0; i < coords.length - 1; i++) {
            // Create 2D coordinates by removing Z component
            // Coordinate coord1 = new Coordinate(coords[i].x, coords[i].y);
            // Coordinate coord2 = new Coordinate(coords[i + 1].x, coords[i + 1].y);
            // Coordinate coord1 = triangulation.getBarrierHeightAtPoint(null)
            LineString edge = geometryFactory.createLineString(new Coordinate[]{coords[i], coords[i+1]});
            edges.add(edge);
        }
        
        return edges;
    }
    
    /**
     * Calculate offset coordinate for left or right side of the bridge.
     * @param centerCoord Center coordinate
     * @param width Width offset distance
     * @param isRight true for right side, false for left side
     * @param index Index of the point in the bridge points list
     * @param bridgeCenterPoints List of bridge points for direction calculation
     * @return Offset coordinate
     */
    private Coordinate calculateOffsetCoordinate(Coordinate centerCoord, double width, BridgePoint.Position direction, int index, List<BridgePoint> bridgeCenterPoints) {
        if (width == 0.0) {
            return new Coordinate(centerCoord);
        }
        
        // Calculate direction vector for the bridge line at this point
        Coordinate directionVector = calculateDirection(index, bridgeCenterPoints);
        
        // Calculate perpendicular vector (90 degrees rotation)
        double perpX = directionVector.y; // Rotate 90 degrees counterclockwise
        double perpY = -directionVector.x;

        // Normalize the perpendicular vector
        double length = Math.sqrt(perpX * perpX + perpY * perpY);
        if (length > 0) {
            perpX /= length;
            perpY /= length;
        }
        
        // Apply right/left orientation
        if (direction == BridgePoint.Position.LEFT) {
            perpX = -perpX;
            perpY = -perpY;
        }
        
        // Calculate offset coordinate
        return new Coordinate(
            centerCoord.x + perpX * width,
            centerCoord.y + perpY * width,
            centerCoord.z
        );
    }
    
    /**
     * Calculate direction vector at a given point index.
     * @param index Index of the point
     * @param bridgeCenterPoints List of bridge points
     * @return Normalized direction vector
     */
    private Coordinate calculateDirection(int index, List<BridgePoint> bridgeCenterPoints) {
        if (bridgeCenterPoints.size() < 2) {
            return new Coordinate(1.0, 0.0); // Default direction
        }
        
        Coordinate current = bridgeCenterPoints.get(index).getCoordinate();
        
        if (index == 0) {
            // First point: use direction to next point
            Coordinate next = bridgeCenterPoints.get(1).getCoordinate();
            return normalizeVector(new Coordinate(next.x - current.x, next.y - current.y));
        } else if (index == bridgeCenterPoints.size() - 1) {
            // Last point: use direction from previous point
            Coordinate prev = bridgeCenterPoints.get(index - 1).getCoordinate();
            return normalizeVector(new Coordinate(current.x - prev.x, current.y - prev.y));
        } else {
            // Middle point: use average direction
            Coordinate prev = bridgeCenterPoints.get(index - 1).getCoordinate();
            Coordinate next = bridgeCenterPoints.get(index + 1).getCoordinate();
            return normalizeVector(new Coordinate(next.x - prev.x, next.y - prev.y));
        }
    }
    
    /**
     * Normalize a vector.
     * @param vector Vector to normalize
     * @return Normalized vector
     */
    private Coordinate normalizeVector(Coordinate vector) {
        double length = Math.sqrt(vector.x * vector.x + vector.y * vector.y);
        if (length > 0) {
            return new Coordinate(vector.x / length, vector.y / length);
        }
        return new Coordinate(1.0, 0.0); // Default direction
    }
    
    /**
     * Get the geometry factory.
     * @return Geometry factory
     */
    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }
    
    /**
     * Set the geometry factory.
     * @param geometryFactory New geometry factory
     */
    public void setGeometryFactory(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory != null ? geometryFactory : GeometryFactoryProvider.SHARED;
    }
}
