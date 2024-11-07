/**
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

/**
 * Most of this class is extracted from the class ConvexHull from jts
 */

package org.noise_planet.noisemodelling.pathfinder.utils;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Assert;

import java.util.*;

/**
 * Computes the convex hull of a {@link Geometry}.
 * The convex hull is the smallest convex Geometry that contains all the
 * points in the input Geometry.
 * <p>
 * Uses the Graham Scan algorithm.
 * <p>
 * Incorporates heuristics to optimize checking for degenerate results,
 * and to reduce the number of points processed for large inputs.
 *
 *@version 1.7
 */
public class ConstrainedConvexHull
{
    private static final int TUNING_REDUCE_SIZE = 50;

    private GeometryFactory geomFactory;
    private Coordinate[] inputPts;

    /**
     * Create a new convex hull construction for the input {@link Geometry}.
     */
    public ConstrainedConvexHull(Geometry geometry)
    {
        this(geometry.getCoordinates(), geometry.getFactory());
    }
    /**
     * Create a new convex hull construction for the input {@link Coordinate} array.
     */
    public ConstrainedConvexHull(Coordinate[] pts, GeometryFactory geomFactory)
    {
        //-- suboptimal early uniquing - for performance testing only
        //inputPts = UniqueCoordinateArrayFilter.filterCoordinates(pts);

        inputPts = pts;
        this.geomFactory = geomFactory;
    }

    /**
     * Returns a {@link Geometry} that represents the convex hull of the input
     * geometry.
     * The returned geometry contains the minimal number of points needed to
     * represent the convex hull.  In particular, no more than two consecutive
     * points will be collinear.
     *
     * @return if the convex hull contains 3 or more points, a {@link Polygon};
     * 2 points, a {@link LineString};
     * 1 point, a {@link Point};
     * 0 points, an empty {@link GeometryCollection}.
     */
    private Stack<Coordinate> grahamScan(Coordinate[] c, Coordinate p1, Coordinate p2) {
        Stack<Coordinate> ps = new Stack<>();

        // Initialize the stack with the first point, ensuring it is p1.
        ps.push(p1);

        // Start scanning from the second point.
        for (int i = 1; i < c.length; i++) {
            Coordinate current = c[i];

            // If the current point is p2, add it directly and move to the next point.
            if (current.equals(p2)) {
                ps.push(p2);
                continue;
            }

            // Discard points that do not maintain the correct orientation.
            Coordinate p;
            for (p = ps.pop();
                 !ps.empty() && Orientation.index(ps.peek(), p, current) > 0;
                 p = ps.pop()) {

                // If p is p2, push it back immediately to avoid discarding it.
                if (p.equals(p2)) {
                    ps.push(p);
                    break;
                }
            }

            ps.push(p);
            ps.push(current);
        }

        // Add the starting point p1 to close the convex hull.
        ps.push(p1);

        return ps;
    }


    /**
     * Checks if there are <= 2 unique points,
     * which produce an obviously degenerate result.
     * If there are more points, returns null to indicate this.
     *
     * This is a fast check for an obviously degenerate result.
     * If the result is not obviously degenerate (at least 3 unique points found)
     * the full uniquing of the entire point set is
     * done only once during the reduce phase.
     *
     * @return a degenerate hull geometry, or null if the number of input points is large
     */
    private Geometry createFewPointsResult() {
        Coordinate[] uniquePts = extractUnique(inputPts, 2);
        if (uniquePts == null) {
            return null;
        }
        else if (uniquePts.length == 0) {
            return geomFactory.createGeometryCollection();
        }
        else if (uniquePts.length == 1) {
            return geomFactory.createPoint(uniquePts[0]);
        }
        else {
            return geomFactory.createLineString(uniquePts);
        }
    }

    private static Coordinate[] extractUnique(Coordinate[] pts) {
        return extractUnique(pts, -1);
    }

    /**
     * Extracts unique coordinates from an array of coordinates,
     * up to an (optional) maximum count of values.
     * If more than the given maximum of unique values are found,
     * this is reported by returning <code>null</code>.
     * This avoids scanning all input points if not needed.
     * If the maximum points is not specified, all unique points are extracted.
     *
     * @param pts an array of Coordinates
     * @param maxPts the maximum number of unique points to scan, or -1
     * @return an array of unique values, or null
     */
    private static Coordinate[] extractUnique(Coordinate[] pts, int maxPts) {
        Set<Coordinate> uniquePts = new HashSet<Coordinate>();
        for (Coordinate pt : pts) {
            uniquePts.add(pt);
            //-- if maxPts is provided, exit if more unique pts found
            if (maxPts >= 0 && uniquePts.size() > maxPts) return null;
        }
        return CoordinateArrays.toCoordinateArray(uniquePts);
    }

    /**
     * An alternative to Stack.toArray, which is not present in earlier versions
     * of Java.
     */
    protected Coordinate[] toCoordinateArray(Stack<Coordinate> stack) {
        Coordinate[] coordinates = new Coordinate[stack.size()];
        for (int i = 0; i < stack.size(); i++) {
            Coordinate coordinate = (Coordinate) stack.get(i);
            coordinates[i] = coordinate;
        }
        return coordinates;
    }

    /**
     * Uses a heuristic to reduce the number of points scanned
     * to compute the hull.
     * The heuristic is to find a polygon guaranteed to
     * be in (or on) the hull, and eliminate all points inside it.
     * A quadrilateral defined by the extremal points
     * in the four orthogonal directions
     * can be used, but even more inclusive is
     * to use an octilateral defined by the points in the 8 cardinal directions.
     * <p>
     * Note that even if the method used to determine the polygon vertices
     * is not 100% robust, this does not affect the robustness of the convex hull.
     * <p>
     * To satisfy the requirements of the Graham Scan algorithm,
     * the returned array has at least 3 entries.
     * <p>
     * This has the side effect of making the reduced points unique,
     * as required by the convex hull algorithm used.
     *
     * @param inputPts the points to reduce
     * @return the reduced list of points (at least 3)
     */
    private Coordinate[] reduce(Coordinate[] inputPts)
    {
        //Coordinate[] polyPts = computeQuad(inputPts);
        Coordinate[] innerPolyPts = computeInnerOctolateralRing(inputPts);

        // unable to compute interior polygon for some reason
        // Copy the input array, since it will be sorted later
        if (innerPolyPts == null)
            return inputPts.clone();

//    LinearRing ring = geomFactory.createLinearRing(polyPts);
//    System.out.println(ring);

        // add points defining polygon
        Set<Coordinate> reducedSet = new HashSet();
        for (int i = 0; i < innerPolyPts.length; i++) {
            reducedSet.add(innerPolyPts[i]);
        }
        /**
         * Add all unique points not in the interior poly.
         * CGAlgorithms.isPointInRing is not defined for points exactly on the ring,
         * but this doesn't matter since the points of the interior polygon
         * are forced to be in the reduced set.
         */
        for (int i = 0; i < inputPts.length; i++) {
            if (! PointLocation.isInRing(inputPts[i], innerPolyPts)) {
                reducedSet.add(inputPts[i]);
            }
        }
        Coordinate[] reducedPts = CoordinateArrays.toCoordinateArray(reducedSet);

        // ensure that computed array has at least 3 points (not necessarily unique)
        if (reducedPts.length < 3)
            return padArray3(reducedPts);
        return reducedPts;
    }

    private Coordinate[] padArray3(Coordinate[] pts)
    {
        Coordinate[] pad = new Coordinate[3];
        for (int i = 0; i < pad.length; i++) {
            if (i < pts.length) {
                pad[i] = pts[i];
            }
            else
                pad[i] = pts[0];
        }
        return pad;
    }

    /**
     * Sorts the points radially CW around the point with minimum Y and then X.
     *
     * @param pts the points to sort
     * @return the sorted points
     */
    private Coordinate[] preSort(Coordinate[] pts) {
        Coordinate t;

        /**
         * find the lowest point in the set. If two or more points have
         * the same minimum Y coordinate choose the one with the minimum X.
         * This focal point is put in array location pts[0].
         */
        for (int i = 1; i < pts.length; i++) {
            if ((pts[i].y < pts[0].y) || ((pts[i].y == pts[0].y) && (pts[i].x < pts[0].x))) {
                t = pts[0];
                pts[0] = pts[i];
                pts[i] = t;
            }
        }

        // sort the points radially around the focal point.
        Arrays.sort(pts, 1, pts.length, new RadialComparator(pts[0]));

        return pts;
    }

    /**
     * Uses the Graham Scan algorithm to compute the convex hull vertices.
     *
     * @param c a list of points, with at least 3 entries
     * @return a Stack containing the ordered points of the convex hull ring
     */
    private Stack<Coordinate> grahamScan(Coordinate[] c) {
        Coordinate p;
        Stack<Coordinate> ps = new Stack<Coordinate>();
        ps.push(c[0]);
        ps.push(c[1]);
        ps.push(c[2]);
        for (int i = 3; i < c.length; i++) {
            Coordinate cp = c[i];
            p = (Coordinate) ps.pop();
            // check for empty stack to guard against robustness problems
            while (
                    ! ps.empty() &&
                            Orientation.index((Coordinate) ps.peek(), p, cp) > 0) {
                p = (Coordinate) ps.pop();
            }
            ps.push(p);
            ps.push(cp);
        }
        ps.push(c[0]);
        return ps;
    }

    /**
     *@return    whether the three coordinates are collinear and c2 lies between
     *      c1 and c3 inclusive
     */
    private boolean isBetween(Coordinate c1, Coordinate c2, Coordinate c3) {
        if (Orientation.index(c1, c2, c3) != 0) {
            return false;
        }
        if (c1.x != c3.x) {
            if (c1.x <= c2.x && c2.x <= c3.x) {
                return true;
            }
            if (c3.x <= c2.x && c2.x <= c1.x) {
                return true;
            }
        }
        if (c1.y != c3.y) {
            if (c1.y <= c2.y && c2.y <= c3.y) {
                return true;
            }
            if (c3.y <= c2.y && c2.y <= c1.y) {
                return true;
            }
        }
        return false;
    }

    private Coordinate[] computeInnerOctolateralRing(Coordinate[] inputPts) {
        Coordinate[] octPts = computeInnerOctolateralPts(inputPts);
        CoordinateList coordList = new CoordinateList();
        coordList.add(octPts, false);

        // points must all lie in a line
        if (coordList.size() < 3) {
            return null;
        }
        coordList.closeRing();
        return coordList.toCoordinateArray();
    }

    /**
     * Computes the extremal points of an inner octolateral.
     * Some points may be duplicates - these are collapsed later.
     *
     * @param inputPts the points to compute the octolateral for
     * @return the extremal points of the octolateral
     */
    private Coordinate[] computeInnerOctolateralPts(Coordinate[] inputPts)
    {
        Coordinate[] pts = new Coordinate[8];
        for (int j = 0; j < pts.length; j++) {
            pts[j] = inputPts[0];
        }
        for (int i = 1; i < inputPts.length; i++) {
            if (inputPts[i].x < pts[0].x) {
                pts[0] = inputPts[i];
            }
            if (inputPts[i].x - inputPts[i].y < pts[1].x - pts[1].y) {
                pts[1] = inputPts[i];
            }
            if (inputPts[i].y > pts[2].y) {
                pts[2] = inputPts[i];
            }
            if (inputPts[i].x + inputPts[i].y > pts[3].x + pts[3].y) {
                pts[3] = inputPts[i];
            }
            if (inputPts[i].x > pts[4].x) {
                pts[4] = inputPts[i];
            }
            if (inputPts[i].x - inputPts[i].y > pts[5].x - pts[5].y) {
                pts[5] = inputPts[i];
            }
            if (inputPts[i].y < pts[6].y) {
                pts[6] = inputPts[i];
            }
            if (inputPts[i].x + inputPts[i].y < pts[7].x + pts[7].y) {
                pts[7] = inputPts[i];
            }
        }
        return pts;

    }

    /**
     *@param  coordinates  the vertices of a linear ring, which may or may not be
     *      flattened (i.e. vertices collinear)
     *@return           a 2-vertex <code>LineString</code> if the vertices are
     *      collinear; otherwise, a <code>Polygon</code> with unnecessary
     *      (collinear) vertices removed
     */
    private Geometry lineOrPolygon(Coordinate[] coordinates) {

        coordinates = cleanRing(coordinates);
        if (coordinates.length == 3) {
            return geomFactory.createLineString(new Coordinate[]{coordinates[0], coordinates[1]});
        }
        LinearRing linearRing = geomFactory.createLinearRing(coordinates);
        return geomFactory.createPolygon(linearRing);
    }

    /**
     * Cleans a list of points by removing interior collinear vertices.
     *
     * @param  original  the vertices of a linear ring, which may or may not be
     *      flattened (i.e. vertices collinear)
     * @return the coordinates with unnecessary (collinear) vertices removed
     */
    private Coordinate[] cleanRing(Coordinate[] original) {
        Assert.equals(original[0], original[original.length - 1]);
        List<Coordinate> cleanedRing = new ArrayList<Coordinate>();
        Coordinate previousDistinctCoordinate = null;
        for (int i = 0; i <= original.length - 2; i++) {
            Coordinate currentCoordinate = original[i];
            Coordinate nextCoordinate = original[i+1];
            if (currentCoordinate.equals(nextCoordinate)) {
                continue;
            }
            if (previousDistinctCoordinate != null
                    && isBetween(previousDistinctCoordinate, currentCoordinate, nextCoordinate)) {
                continue;
            }
            cleanedRing.add(currentCoordinate);
            previousDistinctCoordinate = currentCoordinate;
        }
        cleanedRing.add(original[original.length - 1]);
        Coordinate[] cleanedRingCoordinates = new Coordinate[cleanedRing.size()];
        return (Coordinate[]) cleanedRing.toArray(cleanedRingCoordinates);
    }


    /**
     * Compares {@link Coordinate}s for their angle and distance
     * relative to an origin.
     * The origin is assumed to be lower in Y and then X than
     * all other point inputs.
     * The points are ordered CCW around the origin.
     *
     * @author Martin Davis
     * @version 1.7
     */
    private static class RadialComparator
            implements Comparator<Coordinate>
    {
        private Coordinate origin;

        /**
         * Creates a new comparator using a given origin.
         * The origin must be lower in Y and then X to all
         * compared points.
         *
         * @param origin the origin of the radial comparison
         */
        public RadialComparator(Coordinate origin)
        {
            this.origin = origin;
        }

        @Override
        public int compare(Coordinate p1, Coordinate p2)
        {
            int comp = polarCompare(origin, p1, p2);
            return comp;
        }

        /**
         * Given two points p and q compare them with respect to their radial
         * ordering about point o.  First checks radial ordering.
         * using a CCW orientation.
         * If the points are collinear, the comparison is based
         * on their distance to the origin.
         * <p>
         * p < q iff
         * <ul>
         * <li>ang(o-p) < ang(o-q) (e.g. o-p-q is CCW)
         * <li>or ang(o-p) == ang(o-q) && dist(o,p) < dist(o,q)
         * </ul>
         *
         * @param o the origin
         * @param p a point
         * @param q another point
         * @return -1, 0 or 1 depending on whether p is less than,
         * equal to or greater than q
         */
        private static int polarCompare(Coordinate o, Coordinate p, Coordinate q)
        {
            int orient = Orientation.index(o, p, q);
            if (orient == Orientation.COUNTERCLOCKWISE) return 1;
            if (orient == Orientation.CLOCKWISE) return -1;

            /**
             * The points are collinear,
             * so compare based on distance from the origin.
             * The points p and q are >= to the origin,
             * so they lie in the closed half-plane above the origin.
             * If they are not in a horizontal line,
             * the Y ordinate can be tested to determine distance.
             * This is more robust than computing the distance explicitly.
             */
            if (p.y > q.y) return 1;
            if (p.y < q.y) return -1;

            /**
             * The points lie in a horizontal line, which should also contain the origin
             * (since they are collinear).
             * Also, they must be above the origin.
             * Use the X ordinate to determine distance.
             */
            if (p.x > q.x) return 1;
            if (p.x < q.x) return -1;
            // Assert: p = q
            return 0;
        }
    }
}