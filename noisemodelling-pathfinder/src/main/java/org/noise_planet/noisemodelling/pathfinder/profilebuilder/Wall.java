package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;


public class Wall extends LineObstruction {

    /** Type of the wall: Building if coming from a polygon building or Wall if coming from a linestring definition*/
    public final ProfileBuilder.IntersectionType type;

    public long primaryKey = -1;

    /** Input relative height of the wall. Can be NaN if the relative height is not defined */
    public double relativeHeight;

    /** Do the wall coordinates all have Z values? */
    public boolean hasValidZCoordinates = false;

    /** Is the wall definition valid? */
    boolean isValid;

    /**
     * Constructor using segment and id and relative height.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineSegment line, int originId, ProfileBuilder.IntersectionType type, double relativeHeight) {
        this.line = line;
        this.originId = originId;
        this.type = type;
        this.relativeHeight = relativeHeight;

        this.hasValidZCoordinates = validateZCoordinates();
        this.isValid = hasValidZCoordinates || !Double.isNaN(relativeHeight);
    }

    /**
     * Constructor using segment and id.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineSegment line, int originId, ProfileBuilder.IntersectionType type) {
        this(line, originId, type, Double.NaN);
    }

    /**
     * Constructor using start/end point and id.
     * @param p0       Start point of the segment.
     * @param p1       End point of the segment.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(Coordinate p0, Coordinate p1, int originId, ProfileBuilder.IntersectionType type) {
        this(new LineSegment(p0, p1), originId, type);
    }

    /**
     * Test if both point of the wall LineSegment have a valid Z value (not NaN)
     */
    private boolean validateZCoordinates() {
        return (!Double.isNaN(line.p0.getZ()) && !Double.isNaN(line.p1.getZ()));
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
     * Sets the wall height.
     * @param relativeHeight Wall height.
     */
    public void setRelativeHeight(double relativeHeight) {
        this.relativeHeight = relativeHeight;
        this.isValid = hasValidZCoordinates || !Double.isNaN(relativeHeight);
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


    /**
     * Compute all line points Z (absolute altitude) based on defined relativeHeight and topo if it exists
     * Erases all previous Z values
     * @param profileBuilder
     * @return
     */
    public void applyRelativeHeightAndTopo(ProfileBuilder profileBuilder) {
        line.p0.setZ(profileBuilder.getZGround(line.p0) + relativeHeight);
        line.p1.setZ(profileBuilder.getZGround(line.p1) + relativeHeight);
        hasValidZCoordinates = validateZCoordinates();
    }
}