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

import static java.lang.Double.NaN;

import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge class represents a bridge structure for acoustic calculations.
 * This class serves as the main coordinator that delegates responsibilities to specialized components:
 * 
 * Responsibility Distribution:
 * - BridgePointManager: Manages bridge point collections and height interpolation
 * - BridgeGeometryBuilder: Creates 3D deck geometry and edge structures
 * - BridgeTriangulation: Handles triangular interpolation for point-in-polygon operations
 * - BridgeQueryHelper: Performs spatial queries and point position determinations
 * 
 * Acoustic Capabilities:
 * - Sound diffraction at bridge edges for sources located above the bridge
 * - Mirror image sources from back surface reflections
 * - Virtual sources at bridge bottom for structural noise propagation
 * - Bridge geometry management through integrated components
 */
public class Bridge extends Obstruction {

    public enum GirderType {
        STEEL_BOX,
        STEEL_PLATE,
        CONCRETE_BOX,
        CONCRETE_PLATE,
        CONCRETE_HOLLOW_SLAB;
        
        /**
         * Convert string representation to GirderType enum value.
         * Case-insensitive matching is supported.
         * 
         * @param value String representation of girder type
         * @return Corresponding GirderType enum value
         * @throws IllegalArgumentException if the value doesn't match any enum constant
         */
        public static GirderType fromString(String value) {
            if (value == null) {
                return STEEL_BOX; // Default value
            }
            try {
                return GirderType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown GirderType: " + value + 
                        ". Valid values are: STEEL_BOX, STEEL_PLATE, CONCRETE_BOX, CONCRETE_PLATE, CONCRETE_HOLLOW_SLAB");
            }
        }
    }

    public enum SlabType {
        STEEL,
        CONCRETE;
        
        /**
         * Convert string representation to SlabType enum value.
         * Case-insensitive matching is supported.
         * 
         * @param value String representation of slab type
         * @return Corresponding SlabType enum value
         * @throws IllegalArgumentException if the value doesn't match any enum constant
         */
        public static SlabType fromString(String value) {
            if (value == null) {
                return STEEL; // Default value
            }
            try {
                return SlabType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown SlabType: " + value + 
                        ". Valid values are: STEEL, CONCRETE");
            }
        }
    }

    private GirderType girderType = null;
    private SlabType slabType = null;
    
    /** Manager for bridge points collection and operations */
    private BridgePointManager pointManager;
    
    /** Builder for creating bridge deck geometry */
    private BridgeGeometryBuilder geometryBuilder;
    
    /** Triangulation handler for interpolation operations */
    private BridgeTriangulation triangulation;
    
    /** Query helper for spatial operations */
    private BridgeQueryHelper queryHelper;
    
    /** Bridge deck geometry polygon with Z coordinates */
    private Polygon deckGeometry;

    /** Bridge edge polygon for diffraction calculations. */
    private Polygon edge;
    
    /** Primary key of the bridge in the database. */
    private long primaryKey = -1;

    /** Geometry factory for creating JTS geometries */
    private static final GeometryFactory geometryFactory = GeometryFactoryProvider.SHARED;

    private static final double OFFSET = 0.001;

    public static class Builder{
        private final List<BridgePoint> bridgePoints;
        private List<Double> alphas = null;
        private long primaryKey = -1;
        private GirderType girderType = null;
        private SlabType slabType = null;
        
        private BridgePointManager pointManager;
        private BridgeGeometryBuilder geometryBuilder;
        private BridgeTriangulation triangulation;
        private BridgeQueryHelper queryHelper;

        public Builder(List<BridgePoint> bridgePoints){
            this.bridgePoints = bridgePoints;
        }

        public Builder withAlphas(List<Double> alphas){
            this.alphas = alphas;
            return this;
        }

        public Builder setPrimaryKey(long primaryKey){
            this.primaryKey = primaryKey;
            return this;
        }

        public Builder setGirderType(GirderType girderType){
            this.girderType = girderType;
            return this;
        }

        public Builder setSlabType(SlabType slabType){
            this.slabType = slabType;
            return this;
        }

        public Bridge build(){
            if (this.primaryKey == -1L){
                this.primaryKey = this.bridgePoints.get(0).getBridgePrimaryKey();
                for (BridgePoint bp : this.bridgePoints){
                    if (bp.getBridgePrimaryKey() != this.primaryKey){
                        throw new IllegalArgumentException("All BridgePoints must have the same bridgePrimaryKey to build a Bridge.");
                    }
                }
            }
            
            this.pointManager = new BridgePointManager(bridgePoints);
            this.geometryBuilder = new BridgeGeometryBuilder();
            this.triangulation = new BridgeTriangulation(geometryFactory);
            this.queryHelper = new BridgeQueryHelper(null, null, triangulation, pointManager, geometryBuilder);

            if (this.girderType == null) {
                this.girderType = this.bridgePoints.get(0).getGirderType();
                for (BridgePoint bp : this.bridgePoints){
                    if (bp.getGirderType() != this.girderType){
                        throw new IllegalArgumentException("Inconsistent GirderType among BridgePoints to build a Bridge.");
                    }
                }
            }

            if (this.slabType == null) {
                this.slabType = this.bridgePoints.get(0).getSlabType();
                for (BridgePoint bp : this.bridgePoints){
                    if (bp.getSlabType() != this.slabType){
                        throw new IllegalArgumentException("Inconsistent SlabType among BridgePoints to build a Bridge.");
                    }
                }
            }

            return new Bridge(this);
        }
    }

    public Bridge(Builder builder){
        super();
        this.primaryKey = builder.primaryKey;
        this.pointManager = builder.pointManager;
        this.geometryBuilder = builder.geometryBuilder;
        this.triangulation = builder.triangulation;
        this.queryHelper = builder.queryHelper;
        
        if (builder.alphas != null) {
            setAlpha(builder.alphas);
        }

        this.girderType = builder.girderType;
        this.slabType = builder.slabType;

    }
        
    /**
     * Create Bridge instances from a list of BridgePoints grouped by their bridgePrimaryKey.
     * This method groups the provided BridgePoints by their bridge primary key and creates
     * a Bridge instance for each group.
     * 
     * @param bridgePoints List of bridge points to be grouped and converted to Bridges
     * @param defaultAlphas Default absorption coefficients by frequency band
     * @return List of Bridge instances, one for each unique bridgePrimaryKey
     */
    public static List<Bridge> createBridgesFromPoints(List<BridgePoint> bridgePoints, 
                                                      List<Double> defaultAlphas) {
        List<Bridge> bridges = new ArrayList<>();
        
        if (bridgePoints == null || bridgePoints.isEmpty()) {
            return bridges;
        }
        
        // Group bridge points by bridgePrimaryKey
        Map<Long, List<BridgePoint>> bridgePointGroups = new HashMap<>();
        for (BridgePoint point : bridgePoints) {
            long bridgePrimaryKey = point.getBridgePrimaryKey();
            bridgePointGroups.computeIfAbsent(bridgePrimaryKey, k -> new ArrayList<>()).add(point);
        }
        
        // Create Bridge instances for each group
        for (Map.Entry<Long, List<BridgePoint>> entry : bridgePointGroups.entrySet()) {
            List<BridgePoint> pointsForBridge = entry.getValue();

            Bridge bridge = new Builder(pointsForBridge)
                .withAlphas(defaultAlphas)
                .build();

            bridges.add(bridge);
        }
        
        return bridges;
    }


    /**
     * Create Bridge instances from a list of BridgePoints grouped by their bridgePrimaryKey
     * with default values.
     * 
     * @param bridgePoints List of bridge points to be grouped and converted to Bridges
     * @return List of Bridge instances, one for each unique bridgePrimaryKey
     */
    public static List<Bridge> createBridgesFromPoints(List<BridgePoint> bridgePoints) {
        return createBridgesFromPoints(bridgePoints, null);
    }

    /**
     * Get the bridge deck absolute elevation at a specific point using triangulation interpolation.
     * Works for points inside the bridge deck polygon and on the boundary.
     * @param point Point to get deck elevation for
     * @return Absolute deck elevation at the specified point, or Double.NaN if point is outside polygon
     */
    public double getDeckHeightAtPoint(Coordinate point) {
        return triangulation.getDeckHeightAtPoint(point);
    }

    /**
     * Get the deck thickness at a specific point.
     * @param point Point to get deck thickness for
     * @return Deck thickness at the specified point, or NaN if not available
     */
    public double getDeckThicknessAtPoint(Coordinate point) {
        return triangulation.getDeckThicknessAtPoint(point);
    }

    /**
     * Get the barrier height at a specific point.
     * @param point Point to get barrier height for
     * @return Barrier height at the specified point
     */
    public double getBarrierHeightAtPoint(Coordinate point) {
        return triangulation.getBarrierHeightAtPoint(point);
    }

    /**
     * Create bridge deck geometry from bridge points.
     * This method performs the following operations:
     * 1. Creates 3D deck geometry polygon from bridge points
     * 2. Generates edge points for diffraction calculations
     * 3. Initializes triangulation for height interpolation
     * 4. Creates edge geometry for acoustic calculations
     * 5. Updates query helper with the new geometry
     * 
     * @param profileBuilder Profile builder for ground height calculation
     */
    public void createDeckGeometry(ProfileBuilder profileBuilder) {
        // Create deck geometry using the builder
        this.deckGeometry = geometryBuilder.createDeckGeometry(pointManager, profileBuilder);
        
        if (deckGeometry != null) {
            // Create edge points for triangulation from the point manager
            BridgePointManager edgePointManager = new BridgePointManager(BridgePointManager.SortOrder.SIDE_TO_SIDE);
            edgePointManager.addBridgePoints(geometryBuilder.createBridgeEdgePoints(pointManager, profileBuilder, BridgePoint.Position.RIGHT, false));
            edgePointManager.addBridgePoints(geometryBuilder.createBridgeEdgePoints(pointManager, profileBuilder, BridgePoint.Position.LEFT, false));

            // // Create triangulation for interpolation
            triangulation.triangulateGeometry(edgePointManager.getBridgePoints());
            
            // Create edge for acoustic calculations
            this.edge = geometryBuilder.createEdge(pointManager, profileBuilder);
            
            // Update query helper with new geometry; BridgeQueryHelper will generate 2D footprint
            queryHelper.updateGeometry(deckGeometry, null, triangulation);
        }
    }


    /**
     * Check if a sound source is positioned above the bridge.
     * @param pointGeometry Point coordinate (absolute z value)
     * @return true if source is above bridge
     */
    public boolean isPointAboveBridge(Coordinate pointGeometry) {
        return queryHelper.isPointAboveBridge(pointGeometry);
    }

    /**
     * Check if a sound source is positioned below the bridge.
     * @param pointGeometry Point coordinate (absolute z value)
     * @return true if source is below bridge deck
     */
    public boolean isPointBelowBridge(Coordinate pointGeometry) {
        return queryHelper.isPointBelowBridge(pointGeometry);
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
        return queryHelper.isPointOnBridge(pointGeometry, tolerance);
    }

    /**
     * Check if a point is located on this bridge.
     * This method is designed for structural noise calculations and includes sources
     * that are in direct contact with or very close to the bridge deck.
     * @param pointGeometry Point source coordinate
     * @return true if point source is on bridge deck (with tolerance for structural coupling)
     */
    public boolean isPointOnBridge(Coordinate pointGeometry) {
        return queryHelper.isPointOnBridge(pointGeometry);
    }

    /**
     * Check if a point is within the bridge footprint (2D projection).
     * Uses covers() instead of contains() to include boundary points.
     * @param point Point to check
     * @return true if point is within bridge footprint (including boundary)
     */
    public boolean isPointWithinBridgeFootprint(Coordinate point) {
        return queryHelper.isPointWithinBridgeFootprint(point);
    }

    /**
     * Check if the bridge deck geometry intersects with the specified geometry.
     * 
     * @param geom Geometry to test for intersection
     * @return true if the geometries intersect, false otherwise
     */
    public boolean intersects(Geometry geom) {
        return deckGeometry.intersects(geom);
    }



    // Getters and Setters
    
    /**
     * Get the 2D bounding envelope of the bridge footprint.
     * 
     * @return 2D envelope (bounding box) of the bridge
     */
    public Envelope getEnvelope2D() {
        return queryHelper.getEnvelope2D();
    }

    /**
     * Get the 3D deck geometry of the bridge (may be null if not created).
     * @return deck geometry polygon (with Z) or null
     */
    public Geometry getDeckGeometry() {
        return deckGeometry;
    }

    /**
     * Get the 2D footprint geometry of the bridge (projection without Z coordinates).
     * If a footprint was explicitly created it is returned. Otherwise if a deck
     * geometry exists a 2D copy without Z is produced and returned.
     * This is useful for spatial queries that don't require height information.
     * 
     * @return footprint polygon (2D) or null if no geometry available
     */
    public Geometry getFootprintGeometry() {
        return queryHelper.getFootprintGeometry();
    }

    /**
     * Get the bridge point manager.
     * @return Bridge point manager containing bridge points
     */
    public BridgePointManager getPointManager() {
        return pointManager;
    }

    /**
     * Get the edge polygon of the bridge for diffraction calculations.
     * Returns a copy of the edge geometry to prevent external modifications.
     * 
     * @return Copy of edge polygon or null if not created
     */
    public Polygon getEdge() {
        return edge != null ? (Polygon) edge.copy() : null;
    }

    /**
     * Get the primary key of the bridge in the database.
     * 
     * @return Primary key value, or -1 if not set
     */
    public long getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Calculate the average absolute deck height across bridge points at CENTER position.
     * Delegates to the BridgePointManager which only considers CENTER points.
     * @return average absolute deck height or Double.NaN if no valid CENTER values
     */
    public double getAverageAbsoluteDeckHeight() {
        if (pointManager == null) {
            return Double.NaN;
        }
        return pointManager.getAverageAbsoluteDeckHeight();
    }

    /**
     * Add a bridge point to the collection.
     * @param bridgePoint Bridge point to add
     */
    public void addBridgePoint(BridgePoint bridgePoint) {
        pointManager.addBridgePoint(bridgePoint);
    }
    
    /**
     * Remove a bridge point from the collection by primary key.
     * @param pointPrimaryKey Primary key of the bridge point to remove
     * @return true if the point was removed
     */
    public boolean removeBridgePoint(long pointPrimaryKey) {
        return pointManager.removeBridgePoint(pointPrimaryKey);
    }
    
    /**
     * Get the list of bridge points (read-only).
     * @return Copy of bridge points list
     */
    public List<BridgePoint> getBridgePoints() {
        return pointManager.getBridgePoints();
    }
    
    /**
     * Get the number of bridge points.
     * @return Number of bridge points
     */
    public int getBridgePointCount() {
        return pointManager.size();
    }
    
    /**
     * Check if the bridge points collection is empty.
     * @return true if empty
     */
    public boolean hasBridgePoints() {
        return !pointManager.isEmpty();
    }


    /**
     * Get the girder type of the bridge structure.
     * 
     * @return Girder type (STEEL_BOX, STEEL_PLATE, CONCRETE_BOX, etc.)
     */
    public GirderType getGirderType() {
        return girderType;
    }

    /**
     * Set the girder type of the bridge structure.
     * This affects the structural acoustic properties of the bridge.
     * 
     * @param girderType Girder type to set
     */
    public void setGirderType(GirderType girderType) {
        this.girderType = girderType;
    }

    /**
     * Get the slab type (deck material) of the bridge.
     * 
     * @return Slab type (STEEL or CONCRETE)
     */
    public SlabType getSlabType() {
        return slabType;
    }

    /**
     * Set the slab type (deck material) of the bridge.
     * This affects the acoustic absorption and reflection properties.
     * 
     * @param slabType Slab type to set (STEEL or CONCRETE)
     */
    public void setSlabType(SlabType slabType) {
        this.slabType = slabType;
    }
    

    /**
     * Generate sources on the bridge deck at specified height.
     * Creates virtual sources on top of the bridge deck to simulate sound sources
     * located at a specific height above the deck surface. This is used for traffic
     * noise sources or other elevated sources on the bridge.
     * Returns empty list if source is not below the bridge or deck height cannot be determined.
     * 
     * @param sourcePos Source position (X, Y coordinates)
     * @param sourceHeight Source height above the deck surface in meters
     * @return List of virtual source coordinates with absolute Z values, empty if not applicable
     */
    public List<Coordinate> generateSourcesOnBridge(Coordinate sourcePos, double sourceHeight) {
        List<Coordinate> onSources = new ArrayList<>();
        
        if (deckGeometry == null || !queryHelper.isPointBelowBridge(sourcePos)) {
            return onSources; // No sources if source not below bridge
        }
        
        // Create source positions on the bridge deck
        double deckHeight = triangulation.getDeckHeightAtPoint(sourcePos);
        if (!Double.isNaN(deckHeight)) {
            // Place source on the deck
            double absoluteSourceHeight = deckHeight + sourceHeight;

            Coordinate onSource = new Coordinate(sourcePos.x, sourcePos.y, absoluteSourceHeight);
            onSources.add(onSource);
        }

        return onSources;
    }    

    /**
     * Generate sources on the bridge deck at default height (0.05m above deck).
     * Convenience method that uses a default source height of 5 centimeters above the deck surface.
     * This is typically used for road traffic noise sources on bridges.
     * 
     * @param sourcePos Source position (X, Y coordinates)
     * @return List of virtual source coordinates with absolute Z values, empty if not applicable
     */
    public List<Coordinate> generateSourcesOnBridge(Coordinate sourcePos) {
        return generateSourcesOnBridge(sourcePos, 0.05);
    }

    /**
     * Generate virtual sources at bridge bottom for structural noise propagation.
     * This method creates virtual sources below the bridge deck to simulate sound 
     * transmission through the bridge structure (structure-borne noise).
     * The virtual source is placed 1mm below the deck bottom surface.
     * Only generates sources if the original source is ON the bridge deck.
     * 
     * @param sourcePos Source position (must be on bridge deck)
     * @return List of virtual source coordinates below bridge, empty if source not on bridge
     */
    public List<Coordinate> generateVirtualSourcesAtBridgeBottom(Coordinate sourcePos) {
        List<Coordinate> virtualSources = new ArrayList<>();
        
        if (deckGeometry == null || !queryHelper.isPointOnBridge(sourcePos)) {
            return virtualSources; // No virtual sources if source not on bridge
        }
        
        // Create virtual source positions below the bridge deck
        // For simplicity, we create one virtual source directly below the original source
        double deckHeight = getDeckHeightAtPoint(sourcePos);
        if (!Double.isNaN(deckHeight)) {
            // Place virtual source 1 millimeter below deck bottom (considering deck thickness)
            double virtualSourceHeight = deckHeight - (getDeckThicknessAtPoint(sourcePos) + OFFSET);

            Coordinate virtualSource = new Coordinate(sourcePos.x, sourcePos.y, virtualSourceHeight);
            virtualSources.add(virtualSource);
        }
        
        return virtualSources;
    }

    /**
     * Generate mirror image sources by reflection at the bridge bottom surface.
     * This method creates virtual sources above the bridge deck to simulate sound 
     * reflection by the bridge bottom plane. The mirror source is placed symmetrically
     * relative to the bridge bottom surface.
     * Only generates mirror sources if the original source is BELOW the bridge.
     * 
     * @param sourcePos Source position (must be below bridge)
     * @return List of mirror source coordinates above bridge, empty if source not below bridge
     */
    public List<Coordinate> generateMirrorImageSourcesByBridge(Coordinate sourcePos) {
        List<Coordinate> mirrorSources = new ArrayList<>();
        
        if (deckGeometry == null || !queryHelper.isPointBelowBridge(sourcePos)) {
            return mirrorSources; // No mirror sources if source not below bridge
        }
        
        // Create mirror source positions above the bridge deck
        double deckHeight = getDeckHeightAtPoint(sourcePos);
        if (!Double.isNaN(deckHeight)) {
            // Place mirror source 
            double bridgeBottom = deckHeight - getDeckThicknessAtPoint(sourcePos);
            double mirrorSourceHeight = sourcePos.z + 2 * (bridgeBottom - sourcePos.z);

            Coordinate mirrorSource = new Coordinate(sourcePos.x, sourcePos.y, mirrorSourceHeight);
            mirrorSources.add(mirrorSource);
        }

        return mirrorSources;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Bridge bridge = (Bridge) obj;
        
        // Compare primary key
        if (primaryKey != bridge.primaryKey) return false;
        
        // Compare girder type
        if (girderType != bridge.girderType) return false;
        
        // Compare slab type
        if (slabType != bridge.slabType) return false;
        
        // Compare deck geometry
        if (deckGeometry == null ? bridge.deckGeometry != null : !deckGeometry.equals(bridge.deckGeometry)) return false;
        
        // Compare edge geometry
        if (edge == null ? bridge.edge != null : !edge.equals(bridge.edge)) return false;
        
        // Compare bridge points if pointManager exists
        if (pointManager != null && bridge.pointManager != null) {
            List<BridgePoint> thisPoints = pointManager.getBridgePoints();
            List<BridgePoint> otherPoints = bridge.pointManager.getBridgePoints();
            if (thisPoints.size() != otherPoints.size()) return false;
            for (int i = 0; i < thisPoints.size(); i++) {
                if (!thisPoints.get(i).equals(otherPoints.get(i))) return false;
            }
        } else if (pointManager != bridge.pointManager) {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryKey, girderType, slabType, deckGeometry, edge,
                           pointManager != null ? pointManager.getBridgePoints().size() : 0);
    }

}