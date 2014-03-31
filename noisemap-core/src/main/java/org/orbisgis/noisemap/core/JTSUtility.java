/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author Nicolas Fortin
 */
public class JTSUtility {
    /**
     * Utility class
     **/
    private JTSUtility() {}

    /**
     * Create a linear interpolation of the provided control points.
     * @param points Array of points
     * @param delta Distance between interval points
     * @return Interpolated points
     */
    public static Coordinate[] splitMultiPointsInRegularPoints(
            Coordinate[] points, double delta) {
        GeometryFactory gf = new GeometryFactory();
        if (points.length == 0) {
            return null;
        }
        if (points.length == 1) {
            return points;
        }
        // If the distance between the first and the last point is smaller than
        // delta, then return the avg point
        LineString Line = gf.createLineString(points);
        Double length = Line.getLength();
        if (length < delta) {
            if (points.length == 2) {
                return new Coordinate[] { new Coordinate(
                        (points[1].x + points[0].x) / 2.,
                        (points[1].y + points[0].y) / 2.,
                        (points[1].y + points[0].y) / 2.) };
            } else {
                return Line.getInteriorPoint().getCoordinates();
            }
        }
        // We can't set an absolute value, but we can provide a way to compute
        // a delta value that will build a regular set of points with the
        // constraint of a maximum delta value.
        Double ModifiedDelta = length / Math.ceil(length / delta);
        // The line length is greater than Delta, then we will create a set of
        // points where each point lies at the distance of the modified delta
        int nbpts = (int) Math.ceil(length / delta);
        Coordinate[] deltaPoints = new Coordinate[nbpts];
        int pts = 0;
        int cursorPts = 1; // This is the navigation cursor inside the points
        // array
        Coordinate refpt = points[0];
        Double distToNextPt = ModifiedDelta;
        while (pts < nbpts) {
            Coordinate nextbldpts;
            Double distToNextControlPt = refpt.distance(points[cursorPts]);
            while (distToNextControlPt < distToNextPt
                    && points.length != cursorPts + 1) {
                // The distance with the next control point is not sufficient to
                // build a new point
                distToNextPt -= distToNextControlPt;
                refpt = points[cursorPts];
                cursorPts++;
                distToNextControlPt = refpt.distance(points[cursorPts]);
            }
            if (distToNextControlPt >= distToNextPt) {
                // AB Normalized vector
                Coordinate B = points[cursorPts];
                Double ABlen = B.distance(refpt);
                Coordinate AB = new Coordinate((B.x - refpt.x) / ABlen, (B.y - refpt.y)
                        / ABlen, (B.z - refpt.z) / ABlen);
                // Compute intermediate P vector
                Coordinate P = new Coordinate(refpt.x + AB.x * distToNextPt, refpt.y
                        + AB.y * distToNextPt, refpt.z + AB.z * distToNextPt);
                nextbldpts = P;
            } else {
                nextbldpts = points[cursorPts];
            }
            refpt = nextbldpts;
            deltaPoints[pts] = nextbldpts;
            pts++;
            distToNextPt = ModifiedDelta;
        }
        return deltaPoints;
    }
}
