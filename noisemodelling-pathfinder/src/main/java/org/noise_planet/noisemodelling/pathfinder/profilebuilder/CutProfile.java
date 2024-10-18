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
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import static org.noise_planet.noisemodelling.pathfinder.utils.geometry.JTSUtility.dist2D;
import static org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder.IntersectionType.*;


public class CutProfile {
    /** List of cut points. */
    ArrayList<CutPoint> pts = new ArrayList<>();
    /** Source cut point. */
    CutPoint source;
    /** Receiver cut point. */
    CutPoint receiver;
    //TODO cache has intersection properties
    /** True if Source-Receiver linestring is below building intersection */
    Boolean hasBuildingIntersection = false;
    /** True if Source-Receiver linestring is below topography cutting point. */
    Boolean hasTopographyIntersection = false;
    Boolean isFreeField;
    double distanceToSR = 0;
    Orientation srcOrientation;

    /**
     * Add the source point.
     * @param coord Coordinate of the source point.
     */
    public CutPoint addSource(Coordinate coord) {
        source = new CutPoint(coord, SOURCE, -1);
        pts.add(source);
        return source;
    }

    /**
     * Add the receiver point.
     * @param coord Coordinate of the receiver point.
     */
    public CutPoint addReceiver(Coordinate coord) {
        receiver = new CutPoint(coord, RECEIVER, -1);
        pts.add(receiver);
        return receiver;
    }

    /**
     * Add a building cutting point.
     * @param coord      Coordinate of the cutting point.
     * @param buildingId Id of the cut building.
     */
    public CutPoint addBuildingCutPt(Coordinate coord, int buildingId, int wallId, boolean corner) {
        CutPoint cut = new CutPoint(coord, ProfileBuilder.IntersectionType.BUILDING, buildingId, corner);
        cut.buildingId = buildingId;
        cut.wallId = wallId;
        pts.add(cut);
        return cut;
    }

    /**
     * Add a building cutting point.
     * @param coord Coordinate of the cutting point.
     * @param id    Id of the cut building.
     */
    public CutPoint addWallCutPt(Coordinate coord, int id, boolean corner) {
        CutPoint wallPoint = new CutPoint(coord, ProfileBuilder.IntersectionType.WALL, id, corner);
        wallPoint.wallId = id;
        pts.add(wallPoint);
        return wallPoint;
    }

    /**
     * Add a building cutting point.
     * @param coord Coordinate of the cutting point.
     * @param id    Id of the cut building.
     */
    public void addWallCutPt(Coordinate coord, int id, boolean corner, List<Double> alphas) {
        pts.add(new CutPoint(coord, ProfileBuilder.IntersectionType.WALL, id, corner));
        pts.get(pts.size()-1).wallId = id;
        pts.get(pts.size()-1).setWallAlpha(alphas);
    }

    /**
     * Add a topographic cutting point.
     * @param coord Coordinate of the cutting point.
     * @param id    Id of the cut topography.
     * @return Added cut point instance
     */
    public CutPoint addTopoCutPt(Coordinate coord, int id) {
        CutPoint topoCutPoint = new CutPoint(coord, TOPOGRAPHY, id);
        topoCutPoint.setZGround(coord.z);
        pts.add(topoCutPoint);
        return topoCutPoint;
    }

    /**
     * In order to reduce the number of reallocation, reserve the provided points size
     * @param numberOfPointsToBePushed Number of items to preallocate
     */
    public void reservePoints(int numberOfPointsToBePushed) {
        pts.ensureCapacity(pts.size() + numberOfPointsToBePushed);
    }

    /**
     * Add a ground effect cutting point.
     * @param coordinate Coordinate of the cutting point.
     * @param id    Id of the cut topography.
     */
    public CutPoint addGroundCutPt(Coordinate coordinate, int id, double groundCoefficient) {
        CutPoint pt = new CutPoint(coordinate, ProfileBuilder.IntersectionType.GROUND_EFFECT, id);
        pt.setGroundCoef(groundCoefficient);
        pts.add(pt);
        return pt;
    }

    /**
     * Retrieve the cutting points.
     * @return The cutting points.
     */
    public List<CutPoint> getCutPoints() {
        return pts;
    }
    public void setCutPoints ( ArrayList<CutPoint> ge){
        pts = ge;
    }

    /**
     * Retrieve the profile source.
     * @return The profile source.
     */
    public CutPoint getSource() {
        return source;
    }

    /**
     * get Distance of the not free field point to the Source-Receiver Segement
     * @return
     */
    public double getDistanceToSR(){return distanceToSR;}
    /**
     * Retrieve the profile receiver.
     * @return The profile receiver.
     */
    public CutPoint getReceiver() {
        return receiver;
    }

    /**
     * Sort the CutPoints by distance with c0
     */
    public void sort(Coordinate c0) {
        pts.sort(new CutPointDistanceComparator(c0));
    }

    /**
     * Add an existing CutPoint.
     * @param cutPoint CutPoint to add.
     */
    public void addCutPt(CutPoint cutPoint) {
        pts.add(cutPoint);
    }

    /**
     * Reverse the order of the CutPoints.
     */
    public void reverse() {
        Collections.reverse(pts);
    }

    public void setSrcOrientation(Orientation srcOrientation){
        this.srcOrientation = srcOrientation;
    }

    public Orientation getSrcOrientation(){
        return srcOrientation;
    }

    public boolean intersectBuilding(){
        return hasBuildingIntersection;
    }

    public boolean intersectTopography(){
        return hasTopographyIntersection;
    }

    /**
     * compute the path between two points
     * @param p0
     * @param p1
     * @return the absorption coefficient of this path
     */
    public double getGPath(CutPoint p0, CutPoint p1) {
        double totalLength = 0;
        double rsLength = 0.0;

        // Extract part of the path from the specified argument
        List<CutPoint> reduced = pts.subList(pts.indexOf(p0), pts.indexOf(p1) + 1);

        for(int index = 0; index < reduced.size() - 1; index++) {
            CutPoint current = reduced.get(index);
            double segmentLength = current.getCoordinate().distance(reduced.get(index+1).getCoordinate());
            rsLength += segmentLength * current.getGroundCoef();
            totalLength += segmentLength;
        }
        return rsLength / totalLength;
    }

    public double getGPath() {
        return getGPath(getSource(), getReceiver());
    }

    /**
     *
     * @return
     */
    public boolean isFreeField() {
        return !hasBuildingIntersection && !hasTopographyIntersection;
    }


    @Override
    public String toString() {
        return "CutProfile{" +
                "pts=" + pts +
                ", source=" + source +
                ", receiver=" + receiver +
                ", hasBuildingIntersection=" + hasBuildingIntersection +
                ", hasTopographyIntersection=" + hasTopographyIntersection +
                ", isFreeField=" + isFreeField +
                ", distanceToSR=" + distanceToSR +
                ", srcOrientation=" + srcOrientation +
                '}';
    }
}
