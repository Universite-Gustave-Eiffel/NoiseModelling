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
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.JTSUtility;

import java.util.ArrayList;
import java.util.List;

import static org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder.IntersectionType.*;

public class CutProfile {
    /** List of cut points. */
    public ArrayList<CutPoint> cutPoints = new ArrayList<>();

    /** True if Source-Receiver linestring is below building intersection */
    public boolean hasBuildingIntersection = false;
    /** True if Source-Receiver linestring is below topography cutting point. */
    public boolean hasTopographyIntersection = false;

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
        List<CutPoint> reduced = cutPoints.subList(cutPoints.indexOf(p0), cutPoints.indexOf(p1) + 1);

        for(int index = 0; index < reduced.size() - 1; index++) {
            CutPoint current = reduced.get(index);
            double segmentLength = current.getCoordinate().distance(reduced.get(index+1).getCoordinate());
            rsLength += segmentLength * current.getGroundCoefficient();
            totalLength += segmentLength;
        }
        return rsLength / totalLength;
    }

    public double getGPath() {
        if(!cutPoints.isEmpty()) {
            return getGPath(cutPoints.get(0), cutPoints.get(cutPoints.size() - 1));
        } else {
            return 0;
        }
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
        // keep track of the obstacle under our current position. If -1 there is only ground below
        int overObstacleIndex = pts.get(0).getBuildingId();
        for (int i=0; i < pts.size(); i++) {
            CutPoint cut = pts.get(i);
            if (cut.getType() != GROUND_EFFECT) {
                Coordinate coordinate;
                if (BUILDING.equals(cut.getType()) || WALL.equals(cut.getType())) {
                    if(Double.compare(cut.getCoordinate().z, cut.getzGround()) == 0) {
                        // current position is at the ground level in front of or behind the first/last wall
                        if(overObstacleIndex == -1) {
                            overObstacleIndex = cut.getId();
                        } else {
                            overObstacleIndex = -1;
                        }
                    }
                    // Take the obstacle altitude instead of the ground level
                    coordinate = new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getCoordinate().z);
                } else {
                    coordinate = new Coordinate(cut.getCoordinate().x, cut.getCoordinate().y, cut.getzGround());
                }
                // we will ignore topographic point if we are over a building
                if(!(overObstacleIndex >= 0 && TOPOGRAPHY.equals(cut.getType()))) {
                    pts2D.add(coordinate);
                }
            }
            if(index != null) {
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
}
