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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;

import java.util.Objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.LineString;
import java.sql.SQLException;

import static java.lang.Double.NaN;

/**
 * BridgePoint class represents a single point on a bridge structure.
 * 
 * Primary Responsibilities:
 * - Storing geometric and structural information for a specific bridge location
 * - Managing height data (absolute and relative deck heights)
 * - Managing bridge geometry properties (width, thickness, barrier heights)
 * - Supporting positional information (CENTER, LEFT, RIGHT)
 * 
 * Data Scope:
 * - All height values are in meters using absolute elevation coordinates unless specified otherwise
 * - Supports both absolute elevation and relative height (above ground surface) data
 * - Immutable data container with validation methods
 * 
 * This class is a pure data container and does not handle:
 * - Collection management operations (delegated to BridgePointManager)
 * - Geometric calculations or interpolations (delegated to other components)
 * - Spatial queries or relationships (delegated to BridgeQueryHelper)
 */
public class BridgePoint {
    
    /**
     * Enum for bridge point position
     */
    public enum Position {
        CENTER,
        LEFT,
        RIGHT;
        
        /**
         * Convert string representation to Position enum value.
         * Case-insensitive matching is supported.
         * 
         * @param value String representation of girder type
         * @return Corresponding GirderType enum value
         * @throws IllegalArgumentException if the value doesn't match any enum constant
         */
        public static Position fromString(String value) {
            if (value == null) {
                return CENTER; // Default value
            }
            try {
                return Position.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown Position: " + value + 
                        ". Valid values are: CENTER, LEFT, RIGHT");
            }
        }
    }

    /** 3D coordinate of the bridge point */
    private Coordinate coordinate;
    
    /** Primary key identifier for this bridge point */
    private long primaryKey = -1L;
    
    /** Primary key identifier for this bridge */
    private long bridgePrimaryKey = -1L;

    /** Position of the bridge point (center, left, or right) */
    private Position position;

    /** Absolute deck height in meters (elevation above sea level) */
    private double absoluteDeckHeight = NaN;
    
    /** Relative deck height in meters (height above ground surface) */
    private double relativeDeckHeight = NaN;

    /** Deck thickness in meters */
    private double deckThickness = NaN;

    /** Width of the bridge deck on the right side in meters */
    private double rightWidth = NaN;
    
    /** Width of the bridge deck on the left side in meters */
    private double leftWidth = NaN;
    
    /** Height of the right side barrier/parapet in meters */
    private double rightBarrierHeight = NaN;
    
    /** Height of the left side barrier/parapet in meters */
    private double leftBarrierHeight = NaN;

    private Bridge.GirderType girderType = null;

    private Bridge.SlabType slabType = null;

    public static class Builder{
        private final long primaryKey;
        private final long bridgePrimaryKey;
        private final Coordinate coordinate;
        private Scene.HeightType heightType = Scene.HeightType.ABSOLUTE;
        private double absoluteDeckHeight = NaN;
        private double relativeDeckHeight = NaN;
        private double deckThickness = 0.5;
        private double rightWidth = 5.0;
        private double leftWidth = 5.0;
        private double rightBarrierHeight = 1.0;
        private double leftBarrierHeight = 1.0;
        private Position position = Position.CENTER;
        private Bridge.GirderType girderType = Bridge.GirderType.STEEL_BOX;
        private Bridge.SlabType slabType = Bridge.SlabType.STEEL;

        public Builder(long primaryKey, long bridgePrimaryKey, Coordinate coordinate){
            this.primaryKey = primaryKey;
            this.bridgePrimaryKey = bridgePrimaryKey;
            this.coordinate = new Coordinate(coordinate);
            this.absoluteDeckHeight = this.coordinate.getZ();
            this.relativeDeckHeight = NaN;
        }

        public Builder withHeightType(Scene.HeightType heightType){
            this.heightType = heightType;
            if (heightType == Scene.HeightType.ABSOLUTE){
                this.absoluteDeckHeight = this.coordinate.getZ();
                this.relativeDeckHeight = NaN;
            } else if (heightType == Scene.HeightType.RELATIVE){
                this.relativeDeckHeight = this.coordinate.getZ();
                this.absoluteDeckHeight = NaN;
            }
            return this;
        }

        public Builder withAbsoluteDeckHeight(double absoluteDeckHeight){
            this.heightType = Scene.HeightType.ABSOLUTE;
            this.absoluteDeckHeight = absoluteDeckHeight;
            this.relativeDeckHeight = NaN;
            return this;
        }

        public Builder withRelativeDeckHeight(double relativeDeckHeight){
            this.heightType = Scene.HeightType.RELATIVE;
            this.relativeDeckHeight = relativeDeckHeight;
            this.absoluteDeckHeight = NaN;
            return this;
        }

        public Builder withDeckThickness(double deckThickness){
            this.deckThickness = deckThickness;
            return this;
        }

        public Builder withWidth(double rightWidth, double leftWidth){
            this.rightWidth = rightWidth;
            this.leftWidth = leftWidth;
            return this;
        }
        
        public Builder withWidth(double width){
            this.rightWidth = width;
            this.leftWidth = width;
            return this;
        }

        public Builder withBarrierHeight(double rightBarrierHeight, double leftBarrierHeight){
            this.rightBarrierHeight = rightBarrierHeight;
            this.leftBarrierHeight = leftBarrierHeight;
            return this;
        }

        public Builder withBarrierHeight(double barrierHeight){
            this.rightBarrierHeight = barrierHeight;
            this.leftBarrierHeight = barrierHeight;
            return this;
        }

        public Builder withPosition(Position position){
            this.position = position;
            return this;
        }

        
        public Builder withCenter(){
            this.position = BridgePoint.Position.CENTER;
            return this;
        }

        public Builder withLeft(){
            this.position = BridgePoint.Position.LEFT;
            return this;
        }

        public Builder withRight(){
            this.position = BridgePoint.Position.RIGHT;
            return this;
        }


        public Builder withGirderType(Bridge.GirderType girderType){
            this.girderType = girderType;
            return this;
        }

        public Builder withSlabType(Bridge.SlabType slabType){
            this.slabType = slabType;
            return this;
        }

        public BridgePoint build(){
            return new BridgePoint(this);
        }
    }


    public BridgePoint(Builder builder){
        this.coordinate = builder.coordinate;
        this.primaryKey = builder.primaryKey;
        this.bridgePrimaryKey = builder.bridgePrimaryKey;
        this.absoluteDeckHeight = builder.absoluteDeckHeight;
        this.relativeDeckHeight = builder.relativeDeckHeight;
        this.deckThickness = builder.deckThickness;
        this.rightWidth = builder.rightWidth;
        this.leftWidth = builder.leftWidth;
        this.rightBarrierHeight = builder.rightBarrierHeight;
        this.leftBarrierHeight = builder.leftBarrierHeight;
        this.position = builder.position;
        this.girderType = builder.girderType;
        this.slabType = builder.slabType;
    }

    /**
        * Construct a bridge point from a database row.
        * @param rs Result set containing bridge-point attributes
     */

    public BridgePoint(ResultSet rs)  throws SQLException{
        
        Geometry geom = (Geometry) rs.getObject("THE_GEOM");

        if (!(geom instanceof Point)){
            throw new SQLException("Expected POINT geometry for bridge point, got: " + geom.getGeometryType());
        }

        double absoluteDeckHeight = rs.getDouble("ABSOLUTE_DECK_HEIGHT");
        if (rs.wasNull()) {
            absoluteDeckHeight = Double.NaN;
        }

        double relativeDeckHeight = rs.getDouble("RELATIVE_DECK_HEIGHT");
        if (rs.wasNull()) {
            relativeDeckHeight = Double.NaN;
        }

        this.coordinate = new Coordinate(geom.getCoordinate());
        this.primaryKey = rs.getLong("PK");
        this.bridgePrimaryKey = rs.getLong("BRIDGE_PK");
        this.absoluteDeckHeight = absoluteDeckHeight;
        this.relativeDeckHeight = relativeDeckHeight;
        this.deckThickness = rs.getDouble("DECK_THICKNESS");
        this.rightWidth = rs.getDouble("RIGHT_WIDTH");
        this.leftWidth = rs.getDouble("LEFT_WIDTH");
        this.rightBarrierHeight = rs.getDouble("RIGHT_BARRIER_HEIGHT");
        this.leftBarrierHeight = rs.getDouble("LEFT_BARRIER_HEIGHT");
        this.position = BridgePoint.Position.fromString(rs.getString("POSITION"));
        this.girderType = Bridge.GirderType.fromString(rs.getString("GIRDER_TYPE"));
        this.slabType = Bridge.SlabType.fromString(rs.getString("SLAB_TYPE"));
    }

    /**
     * Copy constructor that creates a deep copy of another BridgePoint.
     * All fields are copied, including a deep copy of the coordinate.
     * @param other The BridgePoint to copy
     */
    public BridgePoint(BridgePoint other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot copy from null BridgePoint");
        }
        
        // Deep copy of coordinate
        this.coordinate = other.coordinate != null ? new Coordinate(other.coordinate) : null;
        
        // Copy all other fields
        this.primaryKey = other.primaryKey;
        this.bridgePrimaryKey = other.bridgePrimaryKey;
        this.position = other.position;
        this.absoluteDeckHeight = other.absoluteDeckHeight;
        this.relativeDeckHeight = other.relativeDeckHeight;
        this.deckThickness = other.deckThickness;
        this.rightWidth = other.rightWidth;
        this.leftWidth = other.leftWidth;
        this.rightBarrierHeight = other.rightBarrierHeight;
        this.leftBarrierHeight = other.leftBarrierHeight;
        this.girderType = other.girderType;
        this.slabType = other.slabType;
    }

    /**
     * Create a list of BridgePoints from a list of coordinates.
     * Each coordinate is converted to a BridgePoint with the specified structural properties.
     * The height interpretation depends on the heightType parameter (ABSOLUTE or RELATIVE).
     * 
     * @param coordinates List of 3D coordinates representing bridge point positions
     * @param bridgePrimaryKey Primary key identifier for the bridge
     * @param heightType Type of height interpretation (ABSOLUTE for elevation, RELATIVE for height above ground)
     * @param deckThickness Thickness of the bridge deck in meters
     * @param rightWidth Width of the bridge deck on the right side in meters
     * @param leftWidth Width of the bridge deck on the left side in meters
     * @param rightBarrierHeight Height of the right side barrier in meters
     * @param leftBarrierHeight Height of the left side barrier in meters
     * @param girderType Type of bridge girder structure
     * @param slabType Type of bridge slab material
     * @return List of BridgePoint objects created from the coordinates
     */
    public static List<BridgePoint> createBridgePoints(
        List<Coordinate> coordinates, long bridgePrimaryKey,
        Scene.HeightType heightType, double deckThickness, double rightWidth, double leftWidth,
        double rightBarrierHeight, double leftBarrierHeight, Position position, Bridge.GirderType girderType, Bridge.SlabType slabType) {

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (long pk = 0L; pk < coordinates.size(); pk++) {
            Coordinate coord = coordinates.get((int) pk);
            double absoluteDeckHeight = NaN;
            double relativeDeckHeight = NaN;
            if (heightType == Scene.HeightType.ABSOLUTE) {
                absoluteDeckHeight = coord.getZ();
            } else if (heightType == Scene.HeightType.RELATIVE) {
                relativeDeckHeight = coord.getZ();
            }
            BridgePoint point = new BridgePoint.Builder(pk, bridgePrimaryKey, coord)
                .withHeightType(heightType)
                .withDeckThickness(deckThickness)
                .withWidth(rightWidth, leftWidth)
                .withBarrierHeight(rightBarrierHeight, leftBarrierHeight)
                .withPosition(position)
                .withGirderType(girderType)
                .withSlabType(slabType)
                .build();
            bridgePoints.add(point);
        }
        return bridgePoints;
    }

    
    /**
     * Create a list of BridgePoints from a LineString geometry.
     * Extracts all coordinates from the LineString and converts them to BridgePoints
     * with the specified structural properties.
     * The height interpretation depends on the heightType parameter (ABSOLUTE or RELATIVE).
     * 
     * @param lineString LineString geometry representing the bridge center line
     * @param bridgePrimaryKey Primary key identifier for the bridge
     * @param heightType Type of height interpretation (ABSOLUTE for elevation, RELATIVE for height above ground)
     * @param deckThickness Thickness of the bridge deck in meters
     * @param rightWidth Width of the bridge deck on the right side in meters
     * @param leftWidth Width of the bridge deck on the left side in meters
     * @param rightBarrierHeight Height of the right side barrier in meters
     * @param leftBarrierHeight Height of the left side barrier in meters
     * @param girderType Type of bridge girder structure
     * @param slabType Type of bridge slab material
     * @return List of BridgePoint objects created from the LineString coordinates
     */
    public static List<BridgePoint> createBridgePoints(
        LineString lineString, long bridgePrimaryKey,
        Scene.HeightType heightType, double deckThickness, double rightWidth, double leftWidth,
        double rightBarrierHeight, double leftBarrierHeight, Position position, Bridge.GirderType girderType, Bridge.SlabType slabType) {

        List<BridgePoint> bridgePoints = new ArrayList<>();
        for (int i = 0; i < lineString.getNumPoints(); i++) {
            Coordinate coord = lineString.getCoordinateN(i);
            double absoluteDeckHeight = NaN;
            double relativeDeckHeight = NaN;
            if (heightType == Scene.HeightType.ABSOLUTE) {
                absoluteDeckHeight = coord.getZ();
            } else if (heightType == Scene.HeightType.RELATIVE) {
                relativeDeckHeight = coord.getZ();
            }
            BridgePoint point = new BridgePoint.Builder((long) i, bridgePrimaryKey, coord)
                .withHeightType(heightType)
                .withDeckThickness(deckThickness)
                .withWidth(rightWidth, leftWidth)
                .withBarrierHeight(rightBarrierHeight, leftBarrierHeight)
                .withPosition(position)
                .withGirderType(girderType)
                .withSlabType(slabType)
                .build();
            bridgePoints.add(point);
        }
        return bridgePoints;
    }
    // Getters
    
    /**
     * Get the 3D coordinate of this bridge point.
     * @return The coordinate of the bridge point
     */
    public Coordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Get the primary key identifier of this bridge point.
     * @return The primary key
     */
    public long getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Get the primary key identifier of the bridge this point belongs to.
     * @return The bridge primary key
     */
    public long getBridgePrimaryKey() {
        return bridgePrimaryKey;
    }

    /**
     * Get the position of this bridge point.
     * @return The position (CENTER, LEFT, or RIGHT)
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Get the absolute deck height in meters (elevation above sea level).
     * @return The absolute deck height, or NaN if not set
     */
    public double getAbsoluteDeckHeight() {
        return absoluteDeckHeight;
    }

    /**
     * Get the relative deck height in meters (height above ground surface).
     * @return The relative deck height, or NaN if not set
     */
    public double getRelativeDeckHeight() {
        return relativeDeckHeight;
    }

    /**
     * Get the deck thickness in meters (vertical thickness of the bridge deck).
     * @return The deck thickness, or NaN if not set
     */
    public double getDeckThickness() {
        return deckThickness;
    }

    /**
     * Get the width of the bridge deck on the right side in meters.
     * @return The right width, or NaN if not set
     */
    public double getRightWidth() {
        return rightWidth;
    }

    /**
     * Get the width of the bridge deck on the left side in meters.
     * @return The left width, or NaN if not set
     */
    public double getLeftWidth() {
        return leftWidth;
    }

    /**
     * Get the width of the bridge deck on the specified side.
     * Convenience method that returns left or right width based on the position parameter.
     * 
     * @param side Position side (LEFT or RIGHT) - CENTER is not valid for this method
     * @return The width on the specified side, or NaN if not set
     * @throws IllegalArgumentException if side is CENTER instead of LEFT or RIGHT
     */
    public double getWidth(Position side) {
        if(side == Position.LEFT) {
            return getLeftWidth();
        } else if(side == Position.RIGHT) {
            return getRightWidth();
        } else {
            throw new IllegalArgumentException("LEFT or RIGHT must be specified to get bridge width");
        }
    }

    /**
     * Get the height of the right side barrier/parapet in meters.
     * @return The right barrier height, or NaN if not set
     */
    public double getRightBarrierHeight() {
        return rightBarrierHeight;
    }

    /**
     * Get the height of the left side barrier/parapet in meters.
     * @return The left barrier height, or NaN if not set
     */
    public double getLeftBarrierHeight() {
        return leftBarrierHeight;
    }

    /**
     * Get the barrier height on the specified side of the bridge.
     * Convenience method that returns left or right barrier height based on the position parameter.
     * 
     * @param side Position side (LEFT or RIGHT) - CENTER is not valid for this method
     * @return The barrier height on the specified side, or NaN if not set
     * @throws IllegalArgumentException if side is CENTER instead of LEFT or RIGHT
     */
    public double getBarrierHeight(Position side) {
        if(side == Position.LEFT) {
            return getLeftBarrierHeight();
        } else if(side == Position.RIGHT) {
            return getRightBarrierHeight();
        } else {
            throw new IllegalArgumentException("LEFT or RIGHT must be specified to get bridge barrier height");
        }
    }


    // Setters
    
    /**
     * Set the 3D coordinate of this bridge point.
     * @param coordinate The coordinate to set
     */
    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * Set the primary key identifier of this bridge point.
     * @param primaryKey The primary key to set
     */
    public void setPrimaryKey(long primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Set the primary key identifier of the bridge this point belongs to.
     * @param bridgePrimaryKey The bridge primary key to set
     */
    public void setBridgePrimaryKey(long bridgePrimaryKey) {
        this.bridgePrimaryKey = bridgePrimaryKey;
    }

    /**
     * Set the position of this bridge point.
     * @param position The position to set (CENTER, LEFT, or RIGHT)
     */
    public void setPosition(Position position) {
        this.position = position;
    }
    /**
     * Set the absolute deck height in meters (elevation above sea level).
     * @param absoluteDeckHeight The absolute deck height to set
     */
    public void setAbsoluteDeckHeight(double absoluteDeckHeight) {
        this.absoluteDeckHeight = absoluteDeckHeight;
    }

    /**
     * Set the relative deck height in meters (height above ground surface).
     * @param relativeDeckHeight The relative deck height to set
     */
    public void setRelativeDeckHeight(double relativeDeckHeight) {
        this.relativeDeckHeight = relativeDeckHeight;
    }

    /**
     * Set the deck thickness in meters (vertical thickness of the bridge deck).
     * @param deckThickness The deck thickness to set
     */
    public void setDeckThickness(double deckThickness) {
        this.deckThickness = deckThickness;
    }


    /**
     * Set the width of the bridge deck on the right side in meters.
     * @param rightWidth The right width to set
     */
    public void setRightWidth(double rightWidth) {
        this.rightWidth = rightWidth;
    }

    /**
     * Set the width of the bridge deck on the left side in meters.
     * @param leftWidth The left width to set
     */
    public void setLeftWidth(double leftWidth) {
        this.leftWidth = leftWidth;
    }

    /**
     * Set the height of the right side barrier/parapet in meters.
     * @param rightBarrierHeight The right barrier height to set
     */
    public void setRightBarrierHeight(double rightBarrierHeight) {
        this.rightBarrierHeight = rightBarrierHeight;
    }

    /**
     * Set the height of the left side barrier/parapet in meters.
     * @param leftBarrierHeight The left barrier height to set
     */
    public void setLeftBarrierHeight(double leftBarrierHeight) {
        this.leftBarrierHeight = leftBarrierHeight;
    }
    
    /**
     * Check if this bridge point has a valid coordinate.
     * @return true if coordinate is not null
     */
    public boolean hasValidCoordinate() {
        return coordinate != null;
    }
    
    /**
     * Check if this bridge point has absolute deck height data.
     * @return true if absolute deck height is not NaN
     */
    public boolean hasAbsoluteDeckHeight() {
        return !Double.isNaN(absoluteDeckHeight);
    }
    
    /**
     * Check if this bridge point has relative deck height data.
     * @return true if relative deck height is not NaN
     */
    public boolean hasRelativeDeckHeight() {
        return !Double.isNaN(relativeDeckHeight);
    }
    
    /**
     * Check if this bridge point has width data.
     * @return true if either right or left width is not NaN
     */
    public boolean hasWidthData() {
        return !Double.isNaN(rightWidth) || !Double.isNaN(leftWidth);
    }
    
    /**
     * Check if this bridge point has barrier height data.
     * @return true if either right or left barrier height is not NaN
     */
    public boolean hasBarrierHeightData() {
        return !Double.isNaN(rightBarrierHeight) || !Double.isNaN(leftBarrierHeight);
    }

    /**
     * Get the girder type of the bridge at this point.
     * 
     * @return Girder type (STEEL_BOX, STEEL_PLATE, CONCRETE_BOX, etc.)
     */
    public Bridge.GirderType getGirderType() {
        return girderType;
    }
    
    /**
     * Get the slab type (deck material) of the bridge at this point.
     * 
     * @return Slab type (STEEL or CONCRETE)
     */
    public Bridge.SlabType getSlabType() {
        return slabType;
    }
    
    /**
     * Returns a string representation of this BridgePoint.
     * Includes all non-null and non-NaN fields for debugging and logging purposes.
     * 
     * @return String representation of this BridgePoint with all available data
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BridgePoint{");
        sb.append("bridgePrimaryKey=").append(bridgePrimaryKey);
        sb.append(", position=").append(position);
        if (coordinate != null) {
            sb.append(", coordinate=").append(coordinate);
        }
        if (!Double.isNaN(absoluteDeckHeight)) {
            sb.append(", absoluteDeckHeight=").append(absoluteDeckHeight);
        }
        if (!Double.isNaN(relativeDeckHeight)) {
            sb.append(", relativeDeckHeight=").append(relativeDeckHeight);
        }
        if (!Double.isNaN(deckThickness)) {
            sb.append(", deckThickness=").append(deckThickness);
        }
        if (!Double.isNaN(rightWidth)) {
            sb.append(", rightWidth=").append(rightWidth);
        }
        if (!Double.isNaN(leftWidth)) {
            sb.append(", leftWidth=").append(leftWidth);
        }
        if (!Double.isNaN(rightBarrierHeight)) {
            sb.append(", rightBarrierHeight=").append(rightBarrierHeight);
        }
        if (!Double.isNaN(leftBarrierHeight)) {
            sb.append(", leftBarrierHeight=").append(leftBarrierHeight);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BridgePoint that = (BridgePoint) obj;
        
        if (primaryKey != that.primaryKey) return false;
        if (bridgePrimaryKey != that.bridgePrimaryKey) return false;
        if (Double.compare(that.absoluteDeckHeight, absoluteDeckHeight) != 0) return false;
        if (Double.compare(that.relativeDeckHeight, relativeDeckHeight) != 0) return false;
        if (Double.compare(that.deckThickness, deckThickness) != 0) return false;
        if (Double.compare(that.rightWidth, rightWidth) != 0) return false;
        if (Double.compare(that.leftWidth, leftWidth) != 0) return false;
        if (Double.compare(that.rightBarrierHeight, rightBarrierHeight) != 0) return false;
        if (Double.compare(that.leftBarrierHeight, leftBarrierHeight) != 0) return false;
        
        if (coordinate != null){
            if(coordinate.getX() != that.coordinate.getX()) return false;
            if(coordinate.getY() != that.coordinate.getY()) return false;
        } 
        if (position != that.position) return false;
        if (girderType != that.girderType) return false;
        if (slabType != that.slabType) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate, primaryKey, bridgePrimaryKey, 
            absoluteDeckHeight, relativeDeckHeight, deckThickness, 
            rightWidth, leftWidth, rightBarrierHeight, leftBarrierHeight, 
            position, girderType, slabType);
    }

}
