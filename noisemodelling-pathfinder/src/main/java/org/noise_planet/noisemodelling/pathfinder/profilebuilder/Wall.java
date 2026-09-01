package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;


public class Wall extends LineObstruction {

    /** Type of the wall: Building if coming from a polygon building or Wall if coming from a linestring definition*/
    public final ProfileBuilder.IntersectionType type;

    public long primaryKey = -1;

    /** Is the wall definition valid? */
    boolean isValid;

    /**
     * Constructor using a lineSegment, id and relative height.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineSegment line, int originId, ProfileBuilder.IntersectionType type) {
        this.line = line;
        this.originId = originId;
        this.type = type;

        this.isValid = validateZCoordinates();
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
     * Test if both points of the wall LineSegment have a valid Z value (not NaN)
     */
    private boolean validateZCoordinates() {
        return (!Double.isNaN(line.p0.getZ()) && !Double.isNaN(line.p1.getZ()));
    }

    /**
     * Database primary key of this wall or the building
     *
     * @param primaryKey primary key value
     */
    public void setPrimaryKey(long primaryKey) {
        this.primaryKey = primaryKey;
    }

    /** @return database primary key of this wall or its parent building/bridge (-1 if unset). */
    public long getPrimaryKey() {
        return primaryKey;
    }

    public ProfileBuilder.IntersectionType getType() {
        return type;
    }

}