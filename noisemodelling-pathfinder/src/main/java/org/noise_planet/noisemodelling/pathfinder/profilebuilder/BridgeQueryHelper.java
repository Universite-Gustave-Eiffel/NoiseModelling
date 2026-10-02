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

/**
 * BridgeQueryHelper provides query operations for bridge geometry.
 * 
 * Primary Responsibilities:
 * - Point-in-bridge detection (above, below, on bridge)
 * - Bridge footprint containment checks
 * - Spatial relationship queries between points and bridge geometry
 * - Reflection relevance calculations for acoustic modeling
 * 
 * This class focuses on spatial queries and geometric relationships and does not handle:
 * - Bridge point data management (delegated to BridgePointManager)
 * - Detailed interpolation within triangles (delegated to BridgeTriangulation)
 * - 3D geometry construction (delegated to BridgeGeometryBuilder)
 */
public class BridgeQueryHelper {
    
    /** Bridge deck polygon geometry */
    private Polygon deckGeometry;
    
    /** Bridge footprint polygon geometry */
    private Polygon footprintGeometry;

    /** Optional bridge point manager to build footprint when deck is not available */
    private BridgePointManager pointManager;

    /** Optional geometry builder used to create footprint from bridge points */
    private BridgeGeometryBuilder geometryBuilder;

    /** Bridge triangulation for interpolation */
    private BridgeTriangulation triangulation;
    
    /** Default deck thickness in meters */
    private static final double DEFAULT_DECK_THICKNESS = 0.5;
    
    /**
     * Constructor.
     * @param deckGeometry Bridge deck polygon geometry
     * @param footprintGeometry Bridge footprint polygon geometry
     * @param triangulation Bridge triangulation for interpolation
     */
    public BridgeQueryHelper(Polygon deckGeometry, Polygon footprintGeometry, BridgeTriangulation triangulation) {
        this(deckGeometry, footprintGeometry, triangulation, null, null);
    }

    /**
     * Backwards-compatible constructor used in tests and callers that provide only deck and triangulation.
     */
    public BridgeQueryHelper(Polygon deckGeometry, BridgeTriangulation triangulation) {
        this(deckGeometry, null, triangulation, null, null);
    }

    /**
     * Extended constructor that can accept components to build footprint from bridge points
     */
    public BridgeQueryHelper(Polygon deckGeometry, Polygon footprintGeometry, BridgeTriangulation triangulation,
                             BridgePointManager pointManager, BridgeGeometryBuilder geometryBuilder) {
        this.deckGeometry = deckGeometry;
        this.pointManager = pointManager;
        this.geometryBuilder = geometryBuilder;
        // If footprint explicitly provided use it
        if (footprintGeometry != null) {
            this.footprintGeometry = footprintGeometry;
        } else if (deckGeometry != null) {
            // generate 2D footprint from deck
            this.footprintGeometry = removeZFromPolygon(deckGeometry);
        } else {
            this.footprintGeometry = null;
        }
        this.triangulation = triangulation;
    }

    /**
     * Create a 2D polygon from a 3D polygon by removing Z coordinates.
     * Returns null if input is null.
     */
    private Polygon removeZFromPolygon(Polygon poly) {
        if (poly == null) return null;

        GeometryFactory factory = poly.getFactory();

        // Exterior ring
        LineString exterior = poly.getExteriorRing();
        Coordinate[] exteriorCoords = exterior.getCoordinates();
        Coordinate[] newExterior = new Coordinate[exteriorCoords.length];
        for (int i = 0; i < exteriorCoords.length; i++) {
            newExterior[i] = new Coordinate(exteriorCoords[i].x, exteriorCoords[i].y);
        }
        LinearRing shell = factory.createLinearRing(newExterior);

        // Interior rings
        int holes = poly.getNumInteriorRing();
        LinearRing[] innerRings = new LinearRing[holes];
        for (int h = 0; h < holes; h++) {
            LineString ring = poly.getInteriorRingN(h);
            Coordinate[] ringCoords = ring.getCoordinates();
            Coordinate[] newRing = new Coordinate[ringCoords.length];
            for (int i = 0; i < ringCoords.length; i++) {
                newRing[i] = new Coordinate(ringCoords[i].x, ringCoords[i].y);
            }
            innerRings[h] = factory.createLinearRing(newRing);
        }

        return factory.createPolygon(shell, innerRings);
    }
    
    /**
     * Check if a point is within the bridge footprint (2D projection).
     * Uses covers() instead of contains() to include boundary points.
     * @param point Point to check
     * @return true if point is within bridge footprint (including boundary)
     */
    public boolean isPointWithinBridgeFootprint(Coordinate point) {
        if (footprintGeometry == null || point == null) return false;
        
        Point testPoint = footprintGeometry.getFactory().createPoint(new Coordinate(point.x, point.y));
        // Use covers() instead of contains() to include boundary points
        return footprintGeometry.covers(testPoint);
    }

    /**
     * Check if a sound source is positioned above the bridge.
     * @param pointGeometry Point coordinate (absolute z value)
     * @return true if source is above bridge
     */
    public boolean isPointAboveBridge(Coordinate pointGeometry) {
        if (deckGeometry == null || triangulation == null) return false;
        
        // Check if point is within bridge footprint first
        if (!isPointWithinBridgeFootprint(pointGeometry)) {
            return false;
        }
        
        double deckAbsoluteHeight = triangulation.getDeckHeightAtPoint(pointGeometry);
        
        return !Double.isNaN(deckAbsoluteHeight) && 
               pointGeometry.z >= deckAbsoluteHeight;
    }

    /**
     * Check if a sound source is positioned below the bridge.
     * @param pointGeometry Point coordinate (absolute z value)
     * @return true if source is below bridge deck
     */
    public boolean isPointBelowBridge(Coordinate pointGeometry) {
        if (deckGeometry == null || triangulation == null) return false;
        
        // Check if point is within bridge footprint first
        if (!isPointWithinBridgeFootprint(pointGeometry)) {
            return false;
        }
        
        double deckAbsoluteHeight = triangulation.getDeckHeightAtPoint(pointGeometry);
        double effectiveThickness = getEffectiveDeckThickness(pointGeometry);
        
        return !Double.isNaN(deckAbsoluteHeight) &&
               pointGeometry.z < (deckAbsoluteHeight - effectiveThickness);
    }

    /**
     * Check if a point is located on this bridge.
     * This method is designed for structural noise calculations and includes sources
     * that are in direct contact with or very close to the bridge deck.
     * @param pointGeometry Point source coordinate
     * @param tolerance Tolerance in meters for structural coupling
     * @return true if point source is on bridge deck (with tolerance for structural coupling)
     */
    public boolean isPointOnBridge(Coordinate pointGeometry, double tolerance) {
        if (deckGeometry == null || triangulation == null) return false;
        
        // Check if point is within bridge footprint first
        if (!isPointWithinBridgeFootprint(pointGeometry)) {
            return false;
        }
        
        double deckAbsoluteHeight = triangulation.getDeckHeightAtPoint(pointGeometry);
        
        return !Double.isNaN(deckAbsoluteHeight) && 
               Math.abs(pointGeometry.z - deckAbsoluteHeight) <= tolerance;
    }

    /**
     * Check if a point is located on this bridge with default tolerance.
     * This method is designed for structural noise calculations and includes sources
     * that are in direct contact with or very close to the bridge deck.
     * @param pointGeometry Point source coordinate
     * @return true if point source is on bridge deck (with tolerance for structural coupling)
     */
    public boolean isPointOnBridge(Coordinate pointGeometry) {
        return isPointOnBridge(pointGeometry, 2.0);
    }
    
    /**
     * Check if this bridge is relevant for reflection calculation between source and receiver.
     * @param sourcePos Source position
     * @param receiverPos Receiver position
     * @param maxReflectionDistance Maximum reflection distance
     * @return true if bridge could create reflections for this source-receiver pair
     */
    public boolean isRelevantForReflection(Coordinate sourcePos, Coordinate receiverPos, double maxReflectionDistance) {
        if (deckGeometry == null) {
            return false;
        }
        
        // First check if source is below bridge (required for reflection)
        if (!isPointBelowBridge(sourcePos)) {
            return false;
        }
        
        // Create line segment between source and receiver
    GeometryFactory factory = GeometryFactoryProvider.SHARED;
        LineSegment sourceReceiverLine = new LineSegment(sourcePos, receiverPos);
        
        // Check if bridge deck intersects with the source-receiver line vicinity
        double distanceToBridge = deckGeometry.distance(sourceReceiverLine.toGeometry(factory));

        return distanceToBridge <= maxReflectionDistance;
    }
    
    /**
     * Get the effective deck thickness used for calculations.
     * @param point Point coordinate to get deck thickness for
     * @return Effective deck thickness, or default if not available
     */
    private double getEffectiveDeckThickness(Coordinate point) {
        if (triangulation != null) {
            double thickness = triangulation.getDeckThicknessAtPoint(point);
            return Double.isNaN(thickness) ? DEFAULT_DECK_THICKNESS : thickness;
        }
        return DEFAULT_DECK_THICKNESS;
    }
    
    /**
     * Get the deck geometry envelope for spatial indexing.
     * @return 2D envelope of the bridge deck, or empty envelope if no geometry
     */
    public Envelope getEnvelope2D() {
        return deckGeometry != null ? deckGeometry.getEnvelopeInternal() : new Envelope();
    }
    
    /**
     * Get the deck geometry.
     * @return Bridge deck polygon geometry
     */
    public Geometry getGeometry() {
        return deckGeometry;
    }

    /**
     * Get or build the 2D footprint geometry.
     * If a footprint is already available it is returned. If not and a deck exists a 2D copy is returned.
     * If neither deck nor explicit footprint exist but a BridgePointManager and BridgeGeometryBuilder were
     * provided, the footprint is created from bridge points and cached.
     * @return footprint polygon or null
     */
    public Geometry getFootprintGeometry() {
        if (footprintGeometry != null) return footprintGeometry;
        if (deckGeometry != null) {
            footprintGeometry = removeZFromPolygon(deckGeometry);
            return footprintGeometry;
        }
        if (pointManager != null && geometryBuilder != null) {
            footprintGeometry = geometryBuilder.createFootprintGeometry(pointManager);
            return footprintGeometry;
        }
        return null;
    }
    
    /**
     * Update the deck geometry and related components.
     * @param deckGeometry New deck geometry
     * @param footprintGeometry New footprint geometry
     * @param triangulation New triangulation
     */
    public void updateGeometry(Polygon deckGeometry, Polygon footprintGeometry, BridgeTriangulation triangulation) {
        this.deckGeometry = deckGeometry;
        if (footprintGeometry != null) {
            this.footprintGeometry = footprintGeometry;
        } else {
            this.footprintGeometry = removeZFromPolygon(deckGeometry);
        }
        this.triangulation = triangulation;
    }
}
