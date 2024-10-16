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
import java.util.List;


public  class CutPoint implements Comparable<CutPoint> {
    /** {@link Coordinate} of the cut point. */
    Coordinate coordinate;
    /** Intersection type. */
    ProfileBuilder.IntersectionType type;
    /** Identifier of the cut element. */
    int id;
    /** Identifier of the building containing the point. -1 if no building. */
    int buildingId;
    /** Identifier of the wall containing the point. -1 if no wall. */
    int wallId;
    /** Height of the building containing the point. NaN of no building. */
    double height;
    /** Topographic height of the point. */
    double zGround = Double.NaN;
    /** Ground effect coefficient. 0 if there is no coefficient. */
    double groundCoef;
    /** Wall alpha. NaN if there is no coefficient. */
    List<Double> wallAlpha = Collections.emptyList();
    boolean corner;

    /**
     * Constructor using a {@link Coordinate}.
     * @param coord Coordinate to copy.
     * @param type  Intersection type.
     * @param id    Identifier of the cut element.
     */
    public CutPoint(Coordinate coord, ProfileBuilder.IntersectionType type, int id, boolean corner) {
        this.coordinate = new Coordinate(coord.x, coord.y, coord.z);
        this.type = type;
        this.id = id;
        this.buildingId = -1;
        this.wallId = -1;
        this.groundCoef = 0;
        this.wallAlpha = new ArrayList<>();
        this.height = 0;
        this.corner = corner;
    }
    public CutPoint(Coordinate coord, ProfileBuilder.IntersectionType type, int id) {
        this(coord, type, id, false);
    }

    public CutPoint() {
        coordinate = new Coordinate();
    }

    /**
     * Copy constructor
     * @param cut
     */
    public CutPoint(CutPoint cut) {
        this.coordinate = new Coordinate(cut.getCoordinate());
        this.type = cut.type;
        this.id = cut.id;
        this.buildingId = cut.buildingId;
        this.wallId = cut.wallId;
        this.groundCoef = cut.groundCoef;
        this.wallAlpha = new ArrayList<>(cut.wallAlpha);
        this.height = cut.height;
        this.zGround = cut.zGround;
        this.corner = cut.corner;
    }

    public void setType(ProfileBuilder.IntersectionType type) {
        this.type = type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * Sets the id of the building containing the point.
     * @param buildingId Id of the building containing the point.
     */
    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
        this.wallId = -1;
    }

    /**
     * Sets the id of the wall containing the point.
     * @param wallId Id of the wall containing the point.
     */
    public void setWallId(int wallId) {
        this.wallId = wallId;
        this.buildingId = -1;
    }

    /**
     * Sets the ground coefficient of this point.
     * @param groundCoef The ground coefficient of this point.
     */
    public void setGroundCoef(double groundCoef) {
        this.groundCoef = groundCoef;
    }

    /**
     * Sets the building height.
     * @param height The building height.
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Sets the topographic height.
     * @param zGround The topographic height.
     */
    /*public void setzGround(double zGround) {
        this.zGround = zGround;
    }*/

    /**
     * Sets the wall alpha.
     * @param wallAlpha The wall alpha.
     */
    public void setWallAlpha(List<Double> wallAlpha) {
        this.wallAlpha = wallAlpha;
    }

    /**
     * Retrieve the coordinate of the point.
     * @return The coordinate of the point.
     */
    public Coordinate getCoordinate(){
        return coordinate;
    }

    /**
     * Retrieve the identifier of the cut element.
     * @return Identifier of the cut element.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieve the identifier of the building containing the point. If no building, returns -1.
     * @return Building identifier or -1
     */
    public int getBuildingId() {
        return buildingId;
    }

    /**
     * Retrieve the identifier of the wall containing the point. If no wall, returns -1.
     * @return Wall identifier or -1
     */
    public int getWallId() {
        return wallId;
    }

    /**
     * Retrieve the ground effect coefficient of the point. If there is no coefficient, returns 0.
     * @return Ground effect coefficient or NaN.
     */
    public double getGroundCoef() {
        return groundCoef;
    }

    /**
     * Retrieve the height of the building containing the point. If there is no building, returns NaN.
     * @return The building height, or NaN if no building.
     */
    public double getHeight() {
        return height;
    }

    /**
     * Retrieve the topographic height of the point.
     * @return The topographic height of the point.
     */
    public Double getzGround() {
        return zGround;
    }

    /**
     * Return the wall alpha value.
     * @return The wall alpha value.
     */
    public List<Double> getWallAlpha() {
        return wallAlpha;
    }

    public ProfileBuilder.IntersectionType getType() {
        return type;
    }

    @Override
    public String toString() {
        String str = "";
        str += type.name();
        str += " ";
        str += "(" + coordinate.x +"," + coordinate.y +"," + coordinate.z + ") ; ";
        str += "grd : " + groundCoef + " ; ";
        str += "topoH : " + zGround + " ; ";
        str += "buildH : " + height + " ; ";
        str += "buildId : " + buildingId + " ; ";
        str += "alpha : " + wallAlpha + " ; ";
        str += "id : " + id + " ; ";
        return str;
    }

    /**
     *
     * @param cutPoint the object to be compared.
     * @return
     */
    @Override
    public int compareTo(CutPoint cutPoint) {
        return this.coordinate.compareTo(cutPoint.coordinate);
    }

    public boolean isCorner(){
        return corner;
    }
}