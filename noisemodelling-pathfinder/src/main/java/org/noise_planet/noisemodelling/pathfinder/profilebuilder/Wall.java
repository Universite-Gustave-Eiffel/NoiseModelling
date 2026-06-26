package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.Arrays;


public class Wall extends Obstruction {
    /** Type of the wall */
    public final ProfileBuilder.IntersectionType type;
    /** Id or index of the source building or topographic triangle. */
    public final int originId;
    public long primaryKey = -1;

    /** Input relative height of the wall. Can be NaN if the relative height is not defined */
    public double relativeHeight;

    /** Do the wall coordinates all have Z values? */
    public boolean hasValidZCoordinates = false;

    /** Is the wall definition valid? */
    boolean isValid;

    public Coordinate p0;
    public Coordinate p1;
    public LineSegment ls;
    public int processedWallIndex;

    /**
     * Constructor using segment and id.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineSegment line, int originId, ProfileBuilder.IntersectionType type) {
        this.p0 = line.p0;
        this.p1 = line.p1;
        this.ls = line;
        this.originId = originId;
        this.type = type;
//
//        this.hasValidZCoordinates = validateZCoordinates();
//        this.isValid = hasValidZCoordinates;

    }

    /**
     * Constructor using start/end point and id.
     * @param p0       Start point of the segment.
     * @param p1       End point of the segment.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(Coordinate p0, Coordinate p1, int originId, ProfileBuilder.IntersectionType type) {
        this.p0 = p0;
        this.p1 = p1;
        this.ls = new LineSegment(p0, p1);
        this.originId = originId;
        this.type = type;
    }


    /**
     * Test if both point of the wall LineSegment have a valid Z value (not NaN)
     */
    private boolean validateZCoordinates() {
        return (!Double.isNaN(p0.getZ()) && !Double.isNaN(p1.getZ()));
    }

    /**
     * Database primary key of this wall or the building
     * @param primaryKey primary key value
     * @return this
     */
    public Wall setPrimaryKey(long primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    /**
     * @return Index of this wall in the ProfileBuild list
     */
    public int getProcessedWallIndex() {
        return processedWallIndex;
    }

    /**
     * @param processedWallIndex Index of this wall in the ProfileBuild list
     */
    public Wall setProcessedWallIndex(int processedWallIndex) {
        this.processedWallIndex = processedWallIndex;
        return this;
    }

    /**
     * Sets the wall height.
     * @param relativeHeight Wall height.
     */
    public void setRelativeHeight(double relativeHeight) {
        this.relativeHeight = relativeHeight;
    }

    public LineSegment getLineSegment() {
        return ls;
    }

    /**
     * Retrieve the id or index of the source building or topographic triangle.
     * @return Id or index of the source building or topographic triangle.
     */
    public int getOriginId() {
        return originId;
    }

    /**
     * Retrieve the height of the wall.
     * @return Height of the wall.
     */
    public double getRelativeHeight() {
        return relativeHeight;
    }

    public ProfileBuilder.IntersectionType getType() {
        return type;
    }
}