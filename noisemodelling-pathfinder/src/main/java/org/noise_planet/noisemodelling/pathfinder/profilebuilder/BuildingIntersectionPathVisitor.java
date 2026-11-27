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
    // Caches shared between the four side hull searches of the same source-receiver couple
    // (left/right x straight/curved): the candidate walls and the plane cuts do not depend
    // on the side, and the curved rejection does not depend on the side either
    List<Integer> acceptedCandidates = new ArrayList<>();
    boolean candidatesCollected = false;
    Map<Integer, List<Coordinate>> planeCutCache = new HashMap<>();
    Map<Integer, Boolean> curvedRejectCache = new HashMap<>();

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
     */
    public void setCurved(boolean curved) {
        this.curved = curved;
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
     * Prepare the visitor for a new side hull search of the same source-receiver couple.
     * The candidate walls and the plane cut caches are kept.
     * @param left Side to search
     * @param curved True to search with the curved coordinate system
     * @param input Where the hull points are pushed
     */
    public void reset(boolean left, boolean curved, List<Coordinate> input) {
        this.left = left;
        this.curved = curved;
        this.input = input;
        itemProcessed.clear();
        pushedBuildingsWideAnglePoints.clear();
        pushedWallsPoints.clear();
    }

    /**
     * @return True when the candidate walls of the first search can be processed again with
     * {@link #replayCandidates()} instead of asking the spatial index again
     */
    public boolean isCandidatesCollected() {
        return candidatesCollected;
    }

    public void setCandidatesCollected(boolean candidatesCollected) {
        this.candidatesCollected = candidatesCollected;
    }

    /**
     * Process again the candidate walls accepted by the first search, without asking the
     * spatial index again
     */
    public void replayCandidates() {
        for (int idCandidate = 0; idCandidate < acceptedCandidates.size(); idCandidate++) {
            addItem(acceptedCandidates.get(idCandidate));
        }
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
            LineObstruction processedObstruction = profileBuilder.getProcessedObstructions().get(id);
            // Check if the wall intersects with the segment (only in 2D so it is useless to have a curved path)
            if(processedObstruction.getLineSegment().distance(intersectionLine) < ProfileBuilder.epsilon) {
                if (!candidatesCollected) {
                    acceptedCandidates.add(id);
                }
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
        LineObstruction processedObstruction = profileBuilder.getProcessedObstructions().get(id);
        if (!(processedObstruction instanceof Wall)) {
            return;
        }
        Wall processedWall = (Wall) processedObstruction;
        if(processedWall.type == ProfileBuilder.IntersectionType.BUILDING) {
            if (pushedBuildingsWideAnglePoints.contains(processedWall.originId)) {
                // This building has already been pushed to input hull
                return;
            }
            List<Coordinate> roofPoints = profileBuilder.getPrecomputedWideAnglePoints(processedWall.originId + 1);
            if(roofPoints == null || roofPoints.size() < 2) {
                // weird building, no diffraction point
                return;
            }
            if(curved && isCurvedRayBelowRoof(id, roofPoints)) {
                // The building roof is below the curved ray
                return;
            }
            // Create a cut of the building volume
            roofPoints = cutRoofPointsWithPlaneCached(id, roofPoints);

            // remove points that are not on the correct side of the line p1Top2 (use only x,y coordinates)
            roofPoints = filterPointsBySide(p1Top2, left, roofPoints);
            if (!roofPoints.isEmpty()) {
                input.addAll(roofPoints);
                pushedBuildingsWideAnglePoints.add(processedWall.originId);
            }
        } else if(processedWall.type == ProfileBuilder.IntersectionType.WALL) {
            // A wall not related to a building (polygon)
            if (pushedWallsPoints.contains(processedWall.originId)) {
                // This wall has already been pushed to input hull
                return;
            }
            // Create the diffraction point outside the wall segment
            // Diffraction point must not intersect with wall
            Vector2D translationVector = new Vector2D(processedWall.line.p0, processedWall.line.p1).normalize()
                    .multiply(ProfileBuilder.wideAngleTranslationEpsilon);
            Coordinate extendedP0 = new Coordinate(processedWall.line.p0.x - translationVector.getX(),
                    processedWall.line.p0.y - translationVector.getY(), processedWall.line.p0.z);
            Coordinate extendedP1 = new Coordinate(processedWall.line.p1.x + translationVector.getX(),
                    processedWall.line.p1.y + translationVector.getY(), processedWall.line.p1.z);
            List<Coordinate> roofPoints = Arrays.asList(extendedP0, extendedP1);
            if(curved && isCurvedRayBelowRoof(id, roofPoints)) {
                // The wall top is below the curved ray
                return;
            }
            // Create a cut of the building volume
            roofPoints = cutRoofPointsWithPlaneCached(id, roofPoints);
            // remove points that are not on the correct side of the line p1Top2 (use only x,y coordinates)
            roofPoints = filterPointsBySide(p1Top2, left, roofPoints);
            if (!roofPoints.isEmpty()) {
                pushedWallsPoints.add(processedWall.originId);
                input.addAll(roofPoints);
            }
        }
    }

    /**
     * Cut of the building volume with the top points z moved to the bottom following the
     * curved coordinate system formulae, empty when the top is below the curved ray.
     * The result only depends on the wall, so it is computed once per wall.
     */
    private boolean isCurvedRayBelowRoof(int id, List<Coordinate> roofPoints) {
        Boolean curvedRejected = curvedRejectCache.get(id);
        if (curvedRejected == null) {
            // Adjust the altitude of the building roof points to be in the curved coordinate system
            List<Coordinate> curvedRoofPoints = Arrays.asList(CurvedProfileGenerator.applyTransformation(p1, p2,
                    roofPoints.toArray(new Coordinate[0]), false));
            curvedRejected = cutRoofPointsWithPlane(cutPlane, curvedRoofPoints).isEmpty();
            curvedRejectCache.put(id, curvedRejected);
        }
        return curvedRejected;
    }

    /**
     * Cut of the building volume with the vertical plane between p1 and p2. The result only
     * depends on the wall, so it is computed once per wall. Callers must not change the
     * returned list.
     */
    private List<Coordinate> cutRoofPointsWithPlaneCached(int id, List<Coordinate> roofPoints) {
        List<Coordinate> cutPoints = planeCutCache.get(id);
        if (cutPoints == null) {
            cutPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
            planeCutCache.put(id, cutPoints);
        }
        return cutPoints;
    }
}