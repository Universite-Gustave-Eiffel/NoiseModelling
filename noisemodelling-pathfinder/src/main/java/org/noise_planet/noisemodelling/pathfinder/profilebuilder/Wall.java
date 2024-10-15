package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



public class Wall implements ProfileBuilder.Obstacle {
    /** Segment of the wall. */
    final LineString line;
    /** Type of the wall */
    final ProfileBuilder.IntersectionType type;
    /** Id or index of the source building or topographic triangle. */
    final int originId;
    /** Wall alpha value. */
    List<Double> alphas;
    /** Wall height, if -1, use z coordinate. */
    double height;
    boolean hasP0Neighbour = false;
    boolean hasP1Neighbour = false;
    public Coordinate p0;
    public Coordinate p1;
    LineSegment ls;
    ProfileBuilder.Obstacle obstacle = this;
    int processedWallIndex;
    static final GeometryFactory FACTORY = new GeometryFactory();
    /**
     * Constructor using segment and id.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineSegment line, int originId, ProfileBuilder.IntersectionType type) {
        this.p0 = line.p0;
        this.p1 = line.p1;
        this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
        this.ls = line;
        this.originId = originId;
        this.type = type;
        this.alphas = new ArrayList<>();
    }
    /**
     * Constructor using segment and id.
     * @param line     Segment of the wall.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(LineString line, int originId, ProfileBuilder.IntersectionType type) {
        this.line = line;
        this.p0 = line.getCoordinateN(0);
        this.p1 = line.getCoordinateN(line.getNumPoints()-1);
        this.ls = new LineSegment(p0, p1);
        this.originId = originId;
        this.type = type;
        this.alphas = new ArrayList<>();
    }

    /**
     * Constructor using start/end point and id.
     * @param p0       Start point of the segment.
     * @param p1       End point of the segment.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(Coordinate p0, Coordinate p1, int originId, ProfileBuilder.IntersectionType type) {
        this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
        this.p0 = p0;
        this.p1 = p1;
        this.ls = new LineSegment(p0, p1);
        this.originId = originId;
        this.type = type;
        this.alphas = new ArrayList<>();
    }

    /**
     * Constructor using start/end point and id.
     * @param p0       Start point of the segment.
     * @param p1       End point of the segment.
     * @param originId Id or index of the source building or topographic triangle.
     */
    public Wall(Coordinate p0, Coordinate p1, int originId, ProfileBuilder.IntersectionType type, boolean hasP0Neighbour, boolean hasP1Neighbour) {
        this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
        this.p0 = p0;
        this.p1 = p1;
        this.ls = new LineSegment(p0, p1);
        this.originId = originId;
        this.type = type;
        this.alphas = new ArrayList<>();
        this.hasP0Neighbour = hasP0Neighbour;
        this.hasP1Neighbour = hasP1Neighbour;
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
     * Sets the wall alphas.
     * @param alphas Wall alphas.
     */
    public void setAlpha(List<Double> alphas) {
        this.alphas = alphas;
    }

    /**
     * Sets the wall height.
     * @param height Wall height.
     */
    public void setHeight(double height) {
        this.height = height;
    }

    public void setObstacle(ProfileBuilder.Obstacle obstacle) {
        this.obstacle = obstacle;
    }

    /**
     * Retrieve the segment.
     * @return Segment of the wall.
     */
    public LineString getLine() {
        return line;
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
     * Retrieve the alphas of the wall.
     * @return Alphas of the wall.
     */
    public List<Double> getAlphas() {
        return alphas;
    }

    /**
     * Retrieve the height of the wall.
     * @return Height of the wall.
     */
    public double getHeight() {
        return height;
    }

    public ProfileBuilder.IntersectionType getType() {
        return type;
    }

    /*public boolean hasP0Neighbour() {
        return hasP0Neighbour;
    }

    public boolean hasP1Neighbour() {
        return hasP1Neighbour;
    }*/

    public ProfileBuilder.Obstacle getObstacle() {
        return obstacle;
    }

    @Override
    public Collection<? extends Wall> getWalls() {
        return Collections.singleton(this);
    }
}