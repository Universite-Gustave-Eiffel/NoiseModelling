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
import java.util.stream.Collectors;

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
    /** True if contains a building cutting point. */
    Boolean hasBuildingInter = false;
    /** True if contains a topography cutting point. */
    Boolean hasTopographyInter = false;
    /** True if contains a ground effect cutting point. */
    Boolean hasGroundEffectInter = false;
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
        hasBuildingInter = true;
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
        hasBuildingInter = true;
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
        hasBuildingInter = true;
    }

    /**
     * Add a topographic cutting point.
     * @param coord Coordinate of the cutting point.
     * @param id    Id of the cut topography.
     */
    public void addTopoCutPt(Coordinate coord, int id) {
        pts.add(new CutPoint(coord, TOPOGRAPHY, id));
        hasTopographyInter = true;
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
        hasGroundEffectInter = true;
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
        return hasBuildingInter;
    }

    public boolean intersectTopography(){
        return hasTopographyInter;
    }

    public boolean intersectGroundEffect(){
        return hasGroundEffectInter;
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
        return !hasBuildingInter && !hasTopographyInter;
    }

    /**
     * Get distance between a segment (p1,p2) and a point (point) with point perpendicular to (p1,p2)
     * @param p1
     * @param p2
     * @param point
     * @return distance in meters
     */
    private static double[] distance3D(Coordinate p1, Coordinate p2, Coordinate point) {
        double[] DistanceInfo = new double[2];
        double x1 = p1.getX();
        double y1 = p1.getY();
        double z1 = p1.getZ();

        double x2 = p2.getX();
        double y2 = p2.getY();
        double z2 = p2.getZ();

        double x0 = point.getX();
        double y0 = point.getY();
        double z0 = point.getZ();

        // Vector representing the LineSegment
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        // Vector from the start point of the LineSegment to the Point
        double px = x0 - x1;
        double py = y0 - y1;
        double pz = z0 - z1;

        // Compute the dot product of the vectors
        double dotProduct = dx * px + dy * py + dz * pz;

        // Calculate the projection of the Point onto the LineSegment
        double t = dotProduct / (dx * dx + dy * dy + dz * dz);

        // Calculate the closest point on the LineSegment to the Point
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        double closestZ = z1 + t * dz;

        // Calculate the distance between the closest point and the Point
        double distance = Math.sqrt((x0 - closestX) * (x0 - closestX)
                + (y0 - closestY) * (y0 - closestY)
                + (z0 - closestZ) * (z0 - closestZ));
        double sign = z0 - closestZ;
        DistanceInfo[0]=distance;
        DistanceInfo[1]=sign;
        return DistanceInfo;
    }

    @Override
    public String toString() {
        return "CutProfile{" + "pts=" + pts + ", source=" + source + ", receiver=" + receiver + ", " +
                "hasBuildingInter=" + hasBuildingInter + ", hasTopographyInter=" + hasTopographyInter + ", " +
                "hasGroundEffectInter=" + hasGroundEffectInter + ", isFreeField=" + isFreeField + ", " +
                "srcOrientation=" + srcOrientation + '}';
    }
}
