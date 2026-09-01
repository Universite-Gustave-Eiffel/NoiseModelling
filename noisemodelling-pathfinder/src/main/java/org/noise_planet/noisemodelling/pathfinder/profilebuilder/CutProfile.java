/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CurvedProfileGenerator;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.JTSUtility;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CutProfile {

    /**
     * Profile type from source to receiver
     * Left and Right are a path using the convex hull on the intersection plane with buildings
     */
    public enum PROFILE_TYPE { DIRECT, LEFT, RIGHT, REFLECTION }

    /** List of cut points.
     * First point is source, last point is receiver */
    public ArrayList<CutPoint> cutPoints = new ArrayList<>();

    /** True if Source-Receiver linestring is below building intersection, only used at the generation of the profile to skip searching for lateral cut planes */
    @JsonIgnore
    public boolean hasBuildingIntersection = false;

    /** True if Source-Receiver linestring is below topography cutting point., only used at the generation of the profile to skip searching for lateral cut planes */
    @JsonIgnore
    public boolean hasTopographyIntersection = false;

    /** True if the Source-Receiver ray is blocked by a bridge deck, only used at the generation of the profile. */
    @JsonIgnore
    public boolean hasBridgeIntersection = false;

    /** True if the path between source and receiver is curved, the coordinates are the original,
     *  only the cutting planes for left and right are not the same */
    public boolean curvedPath = false;

    public PROFILE_TYPE profileType = PROFILE_TYPE.DIRECT;

    public Orientation raySourceReceiverDirectivity = new Orientation(); // direction of the source->receiver path relative to the source heading

    /**
     * Empty constructor for deserialization
     */
    public CutProfile() {
    }

    public CutProfile(CutPointSource source, CutPointReceiver receiver) {
        cutPoints.add(source);
        cutPoints.add(receiver);
    }

    /**
     * @return Cut Profile type
     */
    public PROFILE_TYPE getProfileType() {
        return profileType;
    }

    /**
     * @param profileType The cut profile type
     */
    public void setProfileType(PROFILE_TYPE profileType) {
        this.profileType = profileType;
    }

    /**
     * @param curvedPath True if the path between source and receiver is curved, the coordinates are the original,
     *                   only the cutting planes for left and right are not the same
     */
    public void setCurvedPath(boolean curvedPath) {
        this.curvedPath = curvedPath;
    }

    /**
     * @return True if the path between source and receiver is curved
     */
    public boolean isCurvedPath() {
        return curvedPath;
    }

    /**
     * @return the cutPoints
     */
    public ArrayList<CutPoint> getCutPoints() {
        return cutPoints;
    }

    /**
     * Replace the cut point list.
     * @param cutPoints new cut points
     */
    public void setCutPoints(ArrayList<CutPoint> cutPoints) {
        this.cutPoints = cutPoints;
    }

    /**
     * @return the cut point coordinates projected onto the 2D source-receiver plane (x = distance from source).
     */
    public List<Coordinate> generateCutPointCoordinates2D() {
        List<Coordinate> cutPointCoordinates = cutPoints.stream()
                .map(CutPoint::getCoordinate)
                .collect(Collectors.toList());
        return JTSUtility.getNewCoordinateSystem(cutPointCoordinates);
    }

    /**
     * Insert and sort cut points,
     * @param sortBySourcePosition After inserting points, sort the by the distance from the source
     * @param cutPointsToInsert Points to insert
     */
    public void insertCutPoint(boolean sortBySourcePosition, CutPoint... cutPointsToInsert) {
        CutPointSource sourcePoint = getSource();
        CutPointReceiver receiverPoint = getReceiver();
        cutPoints.addAll(1, Arrays.asList(cutPointsToInsert));
        if(sortBySourcePosition) {
            sort(sourcePoint.coordinate);
            // move source as the first point
            int sourceIndex = cutPoints.indexOf(sourcePoint);
            if (sourceIndex != 0) {
                cutPoints.remove(sourceIndex);
                cutPoints.addFirst(sourcePoint);
            }
            // move receiver as the last point
            int receiverIndex = cutPoints.indexOf(receiverPoint);
            if (receiverIndex != cutPoints.size() - 1) {
                cutPoints.remove(receiverIndex);
                cutPoints.add(cutPoints.size(), receiverPoint);
            }
        }
    }

    /**
     * Sort the CutPoints by distance with c0
     */
    public void sort(Coordinate c0) {
        cutPoints.sort(new CutPointDistanceComparator(c0));
    }

    /**
     * compute the path between two points
     * @param p0 Point 0
     * @param p1 Point 1
     * @return the absorption coefficient of this path
     */
    @JsonIgnore
    public double getGPath(CutPoint p0, CutPoint p1, double buildingRoofG) {
        double totalLength = 0;
        double rsLength = 0.0;

        // Extract part of the path from the specified argument
        int i0 = cutPoints.indexOf(p0);
        int i1 = cutPoints.indexOf(p1);
        if(i0 == -1 || i1 == -1 || i1 < i0) {
            return 0.0;
        }

        boolean aboveRoof = false;
        for(int index = 0; index < i1; index++) {
            CutPoint current = cutPoints.get(index);
            if(current instanceof CutPointWall) {
                CutPointWall currentWall = (CutPointWall) current;
                if(!aboveRoof && currentWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_ENTER)) {
                    aboveRoof = true;
                } else if(aboveRoof && currentWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT)) {
                    aboveRoof = false;
                }
            }
            if(index >= i0) {
                double segmentLength = current.getCoordinate().distance(cutPoints.get(index + 1).getCoordinate());
                rsLength += segmentLength * (aboveRoof ? buildingRoofG : current.getGroundCoefficient());
                totalLength += segmentLength;
            }
        }
        return rsLength / totalLength;
    }

    @JsonIgnore
    public double getGPath() {
        if(!cutPoints.isEmpty()) {
            return getGPath(cutPoints.getFirst(), cutPoints.getLast(), Scene.DEFAULT_G_BUILDING);
        } else {
            return 0;
        }
    }

    /**
     * Compute the G coefficient for path segment using indices instead of CutPoint references.
     * This avoids indexOf() calls that fail when using transformed CutPoints.
     * @param i0 Index of first CutPoint in segment
     * @param i1 Index of last CutPoint in segment
     * @param buildingRoofG Ground absorption coefficient for building roofs
     * @return Weighted average of ground absorption coefficients along the segment
     */
    @JsonIgnore
    public double getGPathByIndex(int i0, int i1, double buildingRoofG) {
        if(i0 == -1 || i1 == -1 || i1 < i0 || i0 >= cutPoints.size() || i1 >= cutPoints.size()) {
            return 0.0;
        }

        double totalLength = 0;
        double rsLength = 0.0;

        boolean aboveRoof = false;
        for(int index = 0; index < i1; index++) {
            CutPoint current = cutPoints.get(index);
            if(current instanceof CutPointWall) {
                CutPointWall currentWall = (CutPointWall) current;
                if(!aboveRoof && currentWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_ENTER)) {
                    aboveRoof = true;
                } else if(aboveRoof && currentWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT)) {
                    aboveRoof = false;
                }
            }
            if(index >= i0) {
                double segmentLength = current.getCoordinate().distance(cutPoints.get(index + 1).getCoordinate());
                rsLength += segmentLength * (aboveRoof ? buildingRoofG : current.getGroundCoefficient());
                totalLength += segmentLength;
            }
        }
        return rsLength / totalLength;
    }

    public Orientation getRaySourceReceiverDirectivity() {
        return raySourceReceiverDirectivity;
    }

    public void setRaySourceReceiverDirectivity(Orientation raySourceReceiverDirectivity) {
        this.raySourceReceiverDirectivity = raySourceReceiverDirectivity;
    }

    /**
     *
     * @return
     */
    @JsonIgnore
    public boolean isFreeField() {
        return !hasBuildingIntersection && !hasTopographyIntersection && !hasBridgeIntersection;
    }

    /**
     * @param maximumReceiverWallDistance Maximum horizontal receiver-to-wall distance in meters
     * @return True if this reflection profile contains a last reflection before the receiver and the receiver
     *         is closer than the provided distance to that reflective wall, even if other events occur afterwards
     */
    @JsonIgnore
    public boolean hasCloseReflectionBeforeReceiver(double maximumReceiverWallDistance) {
        if(profileType != PROFILE_TYPE.REFLECTION || cutPoints.size() < 3 || maximumReceiverWallDistance < 0) {
            return false;
        }
        CutPointReceiver receiver = getReceiver();
        if(receiver == null) {
            return false;
        }
        CutPointReflection lastReflectionBeforeReceiver = null;
        for(int i = cutPoints.size() - 2; i >= 1; i--) {
            CutPoint cutPoint = cutPoints.get(i);
            if(cutPoint instanceof CutPointReflection) {
                lastReflectionBeforeReceiver = (CutPointReflection) cutPoint;
                break;
            }
        }
        if(lastReflectionBeforeReceiver == null || lastReflectionBeforeReceiver.wall == null) {
            return false;
        }
        return lastReflectionBeforeReceiver.wall.distance(receiver.coordinate) < maximumReceiverWallDistance;
    }


    @Override
    public String toString() {
        return "CutProfile{" +
                "pts=" + cutPoints +
                ", hasBuildingIntersection=" + hasBuildingIntersection +
                ", hasTopographyIntersection=" + hasTopographyIntersection +
                '}';
    }

    /**
     * From the vertical plane cut, extract only the top elevation points
     * (buildings/walls top or ground if no buildings) then re-project it into
     * a 2d coordinate system. The first point is always x=0.
     * @return the computed 2D coordinate list of DEM
     */
    public List<Coordinate> computePts2DGround() {
        return computePts2DGround(0, null);
    }

    /**
     * @return @return the computed coordinate list
     */
    public List<Coordinate> computePts2D(boolean curvedPath) {
        return computePts2D(curvedPath, null);
    }

    /**
     * Compute 2D coordinates, optionally applying curved transformation
     * @param curvedPath Whether to apply curved transformation
     * @param transformedCutPointsOut If not null and curvedPath is true, will be populated with transformed CutPoints
     * @return The computed 2D coordinate list
     */
    public List<Coordinate> computePts2D(boolean curvedPath, List<CutPoint> transformedCutPointsOut) {
        List<Coordinate> pts2D;
        if(curvedPath) {
            List<CutPoint> transformedCutPoints = CurvedProfileGenerator.applyTransformation(cutPoints, false);
            // If caller wants the transformed cut points, populate the output list
            if (transformedCutPointsOut != null) {
                transformedCutPointsOut.clear();
                transformedCutPointsOut.addAll(transformedCutPoints);
            }
            pts2D = transformedCutPoints.stream()
                    .map(CutPoint::getCoordinate)
                    .collect(Collectors.toList());
        } else {
            pts2D = cutPoints.stream()
                    .map(CutPoint::getCoordinate)
                    .collect(Collectors.toList());
        }
        pts2D = JTSUtility.getNewCoordinateSystem(pts2D);
        return pts2D;
    }

    /**
     * @return @return the computed coordinate list
     */
    public List<Coordinate> computePts2D() {
        return computePts2D(false);
    }

    public List<Integer> getConvexHullIndices(List<Coordinate> coordinates2d) {
        return getConvexHullIndices(coordinates2d, false);
    }


    public List<Integer> getConvexHullIndices(List<Coordinate> coordinates2d, boolean ignoreWall) {
        if(coordinates2d.size() != cutPoints.size()) {
            throw new IllegalArgumentException("Coordinates size must be equal to cut points size");
        }
        // Filter out points that are below the line segment
        List<Coordinate> convexHullInput = new ArrayList<>();
        // Add source position
        convexHullInput.add(coordinates2d.getFirst());
        // Add valid diffraction point, building/walls/dem
        for (int idPoint=1; idPoint < cutPoints.size() - 1; idPoint++) {
            CutPoint currentPoint = cutPoints.get(idPoint);
            // We only add the point at the top of the wall, not the point at the bottom of the wall
            if(currentPoint instanceof CutPointTopography
                    || (currentPoint instanceof CutPointWall
                    && Double.compare(currentPoint.getCoordinate().z, currentPoint.getzGround()) != 0 && !ignoreWall)) {
                convexHullInput.add(coordinates2d.get(idPoint));
            }
        }
        // Add receiver position
        convexHullInput.add(coordinates2d.getLast());

        // Compute the convex hull using JTS
        List<Coordinate> convexHullPoints = new ArrayList<>();
        if(convexHullInput.size() > 2) {
            GeometryFactory geomFactory = new GeometryFactory();
            Coordinate[] coordsArray = convexHullInput.toArray(new Coordinate[0]);
            ConvexHull convexHull = new ConvexHull(coordsArray, geomFactory);
            Coordinate[] hullCoords = convexHull.getConvexHull().getCoordinates();
            List<Coordinate> hullCoordsList = Arrays.asList(hullCoords);
            int indexFirst = hullCoordsList.indexOf(coordinates2d.get(0));
            int indexLast = hullCoordsList.lastIndexOf(coordinates2d.get(coordinates2d.size() - 1));
            if(indexFirst == -1 || indexLast == -1 || indexFirst > indexLast) {
                throw new IllegalArgumentException("Wrong input data ");
            }
            // Remove consecutive duplicate coordinates (previously done with a costly union())
            List<Coordinate> uniqueCoords = new ArrayList<>(indexLast - indexFirst + 1);
            for (int i = indexFirst; i <= indexLast; i++) {
                if (uniqueCoords.isEmpty() || !uniqueCoords.get(uniqueCoords.size() - 1).equals(hullCoords[i])) {
                    uniqueCoords.add(hullCoords[i]);
                }
            }
            Coordinate[] convexHullCoords = uniqueCoords.toArray(new Coordinate[0]);
            // Convert the result back to your format (List<Point2D> pts)
            if (convexHullCoords.length == 3) {
                convexHullPoints = Arrays.asList(convexHullCoords);
            } else {
                for (Coordinate convexHullCoordinates : convexHullCoords) {
                    // Check if the y-coordinate is valid (not equal to Double.MAX_VALUE and not infinite)
                    if (convexHullCoordinates.y == Double.MAX_VALUE || Double.isInfinite(convexHullCoordinates.y)) {
                        continue; // Skip this point as it's not part of the hull
                    }
                    convexHullPoints.add(convexHullCoordinates);
                }
            }
        } else {
            convexHullPoints = convexHullInput;
        }
        // Map each hull point back to its index in the profile
        Map<Coordinate, Integer> coordinateIndex = new HashMap<>(coordinates2d.size());
        for (int i = coordinates2d.size() - 1; i >= 0; i--) {
            coordinateIndex.put(coordinates2d.get(i), i);
        }
        List<Integer> hullIndices = new ArrayList<>(convexHullPoints.size());
        for (Coordinate coordinate : convexHullPoints) {
            hullIndices.add(coordinateIndex.getOrDefault(coordinate, -1));
        }
        return hullIndices;
    }

    /**
     * From the vertical plane cut, extract only the top elevation points
     * (buildings/walls top or ground if no buildings) then re-project it into
     * a 2d coordinate system. The first point is always x=0.
     * @param index Corresponding index from parameter to return list items
     * @return the computed 2D coordinate list of DEM
     */
    public List<Coordinate> computePts2DGround(List<Integer> index) {
        return computePts2DGround(0, index);
    }

    /**
     * From the vertical plane cut, extract only the top elevation points
     * (buildings/walls top or ground if no buildings)
     * @param pts Cut points
     * @param index Corresponding index from parameter to return list items
     * @return the computed coordinate list of the vertical cut
     */
    public static List<Coordinate> computePtsGround(List<CutPoint> pts, List<Integer> index) {

        List<Coordinate> pts2D = new ArrayList<>(pts.size());
        if(pts.isEmpty()) {
            return pts2D;
        }
        // keep track of the obstacle under our current position.
        boolean overArea = false;
        for (CutPoint cut : pts) {
            if (cut instanceof CutPointWall) {
                CutPointWall cutPointWall = (CutPointWall) cut;
                if (cutPointWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT)) {
                    overArea = true;
                } else {
                    break;
                }
            }
        }
        for (CutPoint cut : pts) {
            if (cut instanceof CutPointGroundEffect) {
                if (index != null) {
                    index.add(pts2D.size() - 1);
                }
                continue;
            }
            if (cut instanceof CutPointWall) {
                // Z ground profile must add intermediate ground points before adding the top level of building/wall
                CutPointWall cutPointWall = (CutPointWall) cut;
                if (cutPointWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_ENTER) ||
                        cutPointWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.THIN_WALL_ENTER_EXIT)) {
                    pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
                    overArea = true;
                }
                pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getCoordinate().z));
                if (cutPointWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.BUILDING_EXIT) ||
                        cutPointWall.intersectionType.equals(CutPointWall.INTERSECTION_TYPE.THIN_WALL_ENTER_EXIT)) {
                    pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
                    overArea = false;
                }
            } else if (cut instanceof CutPointReflection) {
                // Z ground profile is duplicated for reflection point before and after
                pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
                pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
                pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
            } else {
                // we will ignore topographic point if we are over a building
                if (!(overArea && cut instanceof CutPointTopography)) {
                    pts2D.add(new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround()));
                }
            }
            if (index != null) {
                index.add(pts2D.size() - 1);
            }
        }
        return pts2D;
    }

    /**
     * From the vertical plane cut, extract only the top elevation points
     * (buildings/walls top or ground if no buildings) then re-project it into
     * a 2d coordinate system. The first point is always x=0.
     * @param pts Cut points
     * @param tolerance Simplify the point list by not adding points where the distance from the line segments
     *                 formed from the previous and the next point is inferior to this tolerance (remove intermediate collinear points)
     * @param index Corresponding index from parameter to return list items
     * @return the computed 2D coordinate list of DEM
     */
    public static List<Coordinate> computePts2DGround(List<CutPoint> pts, double tolerance, List<Integer> index) {
        return JTSUtility.getNewCoordinateSystem(computePtsGround(pts, index), tolerance);
    }

    /**
     * From the vertical plane cut, extract only the top elevation points
     * (buildings/walls top or ground if no buildings) then re-project it into
     * a 2d coordinate system. The first point is always x=0.
     * @param tolerance Simplify the point list by not adding points where the distance from the line segments
     *                 formed from the previous and the next point is inferior to this tolerance (remove intermediate collinear points)
     * @param index Corresponding index from parameter to return list items
     * @return the computed 2D coordinate list of DEM
     */
    public List<Coordinate> computePts2DGround(double tolerance, List<Integer> index) {
        return computePts2DGround(this.cutPoints, tolerance, index);
    }

    @JsonIgnore
    public CutPointSource getSource() {
        return !cutPoints.isEmpty() && cutPoints.getFirst() instanceof CutPointSource ?
                (CutPointSource) cutPoints.getFirst() : null;
    }

    @JsonIgnore
    public CutPointReceiver getReceiver() {
        return !cutPoints.isEmpty() && cutPoints.getLast() instanceof CutPointReceiver ?
                (CutPointReceiver) cutPoints.getLast() : null;
    }
    @JsonIgnore
    public Orientation getSourceOrientation() {
        return this.getSource().orientation;
    }

}
