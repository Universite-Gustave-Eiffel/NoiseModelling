/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.prep.PreparedLineString;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.math.Vector2D;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CurvedProfileGenerator;

import java.util.*;

import static org.noise_planet.noisemodelling.pathfinder.PathFinder.cutRoofPointsWithPlane;
import static org.noise_planet.noisemodelling.pathfinder.PathFinder.filterPointsBySide;


public final class BuildingIntersectionPathVisitor implements ItemVisitor {
    Set<Integer> itemProcessed = new HashSet<>();
    Coordinate p1;
    Coordinate p2;
    boolean left;
    LineSegment p1Top2;
    PreparedLineString seg;
    Set<Integer> pushedBuildingsWideAnglePoints = new HashSet<>();
    Set<Integer> pushedWallsPoints = new HashSet<>();
    ProfileBuilder profileBuilder;
    Plane cutPlane;
    List<Coordinate> input;
    LineSegment intersectionLine = new LineSegment();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    boolean curved = false;
    double p1Height = 0;
    double p2Height = 0;

    public BuildingIntersectionPathVisitor(Coordinate p1, Coordinate p2, boolean left, ProfileBuilder profileBuilder,
                                           List<Coordinate> input, Plane cutPlane) {
        this.profileBuilder = profileBuilder;
        this.input = input;
        this.cutPlane = cutPlane;
        this.p1 = p1;
        this.p2 = p2;
        this.left = left;
        this.p1Top2 = new LineSegment(p1, p2);
        seg = new PreparedLineString(GEOMETRY_FACTORY.createLineString(new Coordinate[]{p1, p2}));
    }

    /**
     * If true, the path between p1 and p2 is curved (a segment of circle).
     * In this case, the curved coordinate system is used and the altitudes of intermediations buildings are modified accordingly.
     * If false, keep the coordinates of the buildings as they are in the input data.
     * @param curved true if the path between p1 and p2 is curved (a segment of circle)
     * @param p1Height the height of the point p1 relative to the ground in the curved coordinate system
     * @param p2Height the height of the point p2 relative to the ground in the curved coordinate system
     */
    public void setCurved(boolean curved, double p1Height, double p2Height) {
        this.curved = curved;
        this.p1Height = p1Height;
        this.p2Height = p2Height;
    }

    /**
     *
     * @return true if the path between p1 and p2 is curved (a segment of circle). In this case, the intersectionLine
     * is a chord of the circle.
     * If false, the path between p1 and p2 is a straight line.
     */
    public boolean isCurved() {
        return curved;
    }

    /**
     * @param segment When visit an item, only add the walls in the hull points input if it intersects with the segment
     *                in argument
     */
    public void setIntersectionLine(LineSegment segment) {
        this.intersectionLine = segment;
        itemProcessed.clear();
    }


    /**
     *
     * @param item the index item to be visited
     */
    @Override
    public void visitItem(Object item) {
        int id = (Integer) item;
        if(!itemProcessed.contains(id)) {
            itemProcessed.add(id);
            Wall processedWall = profileBuilder.getProcessedWalls().get(id);
            // Check if the wall intersects with the segment (only in 2D so it is useless to have a curved path)
            if(processedWall.getLineSegment().distance(intersectionLine) < ProfileBuilder.epsilon) {
                addItem(id);
            }
        }
    }


    /**
     * Add a wall (segment alone or from a segment of a building polygon) to the input list if not already done.
     * It could be ignored if it does not cross with the 3D cutPlane.
     * @param id the wall id to be added
     */
    public void addItem(int id) {
        Wall processedWall = profileBuilder.getProcessedWalls().get(id);
        if(processedWall.type == ProfileBuilder.IntersectionType.BUILDING) {
            if (pushedBuildingsWideAnglePoints.contains(processedWall.originId)) {
                // This building has already been pushed to input hull
                return;
            }
            List<Coordinate> roofPoints = profileBuilder.getPrecomputedWideAnglePoints(processedWall.originId + 1);
            if(roofPoints == null) {
                // weird building, no diffraction point
                return;
            }
            // remove points that are not on the correct side of the line p1Top2 (use only x,y coordinates)
            roofPoints = filterPointsBySide(p1Top2, left, roofPoints);
            if(curved && !roofPoints.isEmpty()) {
                // Adjust the altitude of the building roof points to be in the curved coordinate system
                roofPoints = Arrays.asList(CurvedProfileGenerator.applyTransformation(p1, p2,
                        roofPoints.toArray(new Coordinate[0]), false));
            }
            // Create a cut of the building volume
            roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
            if (!roofPoints.isEmpty()) {
                // inverse the curved transformation to have the correct altitude in the flat coordinate system
                if (curved) {
                    roofPoints = Arrays.asList(CurvedProfileGenerator.applyTransformation(p1, p2,
                            roofPoints.toArray(new Coordinate[0]), true));
                }
                input.addAll(roofPoints);
                pushedBuildingsWideAnglePoints.add(processedWall.originId);
                // Stop iterating bounding boxes
                throw new IllegalStateException();
            }
        } else if(processedWall.type == ProfileBuilder.IntersectionType.WALL) {
            // A wall not related to a building (polygon)
            if (pushedWallsPoints.contains(processedWall.originId)) {
                // This wall has already been pushed to input hull
                return;
            }
            // Create the diffraction point outside the wall segment
            // Diffraction point must not intersect with wall
            Vector2D translationVector = new Vector2D(processedWall.p0, processedWall.p1).normalize()
                    .multiply(ProfileBuilder.wideAngleTranslationEpsilon);
            Coordinate extendedP0 = new Coordinate(processedWall.p0.x - translationVector.getX(),
                    processedWall.p0.y - translationVector.getY(), processedWall.p0.z);
            Coordinate extendedP1 = new Coordinate(processedWall.p1.x + translationVector.getX(),
                    processedWall.p1.y + translationVector.getY(), processedWall.p1.z);
            List<Coordinate> roofPoints = Arrays.asList(extendedP0, extendedP1);
            // remove points that are not on the correct side of the line p1Top2 (use only x,y coordinates)
            roofPoints = filterPointsBySide(p1Top2, left, roofPoints);
            if(curved && !roofPoints.isEmpty()) {
                // Adjust the altitude of the building roof points to be in the curved coordinate system
                roofPoints = Arrays.asList(CurvedProfileGenerator.applyTransformation(p1, p2,
                        roofPoints.toArray(new Coordinate[0]), false));
            }
            // Create a cut of the building volume
            roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
            if (!roofPoints.isEmpty()) {
                // inverse the curved transformation to have the correct altitude in the flat coordinate system
                if (curved) {
                    roofPoints = Arrays.asList(CurvedProfileGenerator.applyTransformation(p1, p2,
                            roofPoints.toArray(new Coordinate[0]), true));
                }
                pushedWallsPoints.add(processedWall.originId);
                input.addAll(roofPoints);
                // Stop iterating bounding boxes
                throw new IllegalStateException();
            }
        }
    }
}