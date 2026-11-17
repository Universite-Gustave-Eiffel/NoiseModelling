/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.pathfinder.utils.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReflection;

import java.util.List;
import java.util.ArrayList;

import static java.lang.Math.asin;
import static java.lang.Math.max;

/**
 * Generate a curved profile (favourable propagation conditions) from a coordinate list and two endpoints (source and receiver)
 * Based on:
 * Salomons, E., Van Maercke, D., Defrance, J.,&amp;De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
 * @author Pierre Aumond
 */
public class CurvedProfileGenerator {

    /**
     * Eq.2.5.24 and Eq. 2.5.25
     * @param mn Length of ray
     * @param d Distance between source and receiver
     * @return Length of curved ray
     */
    public static double toCurve(double mn, double d){
        return 2*max(1000, 8*d)* asin(mn/(2*max(1000, 8*d)));
    }

    /**
     * Salomons, E., Van Maercke, D., Defrance, J.,&amp;De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
     * <p>
     * The curved ray follows the unfolded propagation path. Each cut point is moved by the height of the arc at its
     * abscissa along that path, the cumulative horizontal distance between consecutive cut points (the abscissa used
     * by {@link JTSUtility#getNewCoordinateSystem(List)}), with d the total length of the unfolded path. For a direct
     * profile this equals the distance to the source. For a reflection profile it does not: the cut points after the
     * reflection can lie farther from the source than the receiver, and the distance to the source in plan would raise
     * them instead of lowering them.
     * @param flatProfile Cut points of the profile, ordered from the source to the receiver
     * @param inversed If true, apply the inverse transformation (from curved to flat)
     * @return The cut points of the curved profile
     */
    public static List<CutPoint> applyTransformation(List<CutPoint> flatProfile, boolean inversed) {
        int pointCount = flatProfile.size();
        double[] abscissa = new double[pointCount];
        for (int i = 1; i < pointCount; i++) {
            abscissa[i] = abscissa[i - 1] + flatProfile.get(i - 1).getCoordinate().distance(flatProfile.get(i).getCoordinate());
        }
        double d = pointCount > 0 ? abscissa[pointCount - 1] : 0;

        List<CutPoint> curvedProfile = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            CutPoint cp = flatProfile.get(i);
            double dz = curvedOffset(abscissa[i], d, inversed);
            CutPoint newCp = cp.clone();
            newCp.setZGround(cp.zGround + dz);
            Coordinate c = cp.getCoordinate();
            newCp.setCoordinate(new Coordinate(c.x, c.y, c.z + dz));

            // If this is a reflection point, also move the wall by the offset of the reflection point, so that the
            // wall altitude interpolated at the reflection point is in the same curved coordinate system as the ray
            if (newCp instanceof CutPointReflection) {
                CutPointReflection reflectionPoint = (CutPointReflection) newCp;
                if (reflectionPoint.wall != null) {
                    Coordinate p0 = new Coordinate(reflectionPoint.wall.p0);
                    Coordinate p1 = new Coordinate(reflectionPoint.wall.p1);
                    p0.z += dz;
                    p1.z += dz;
                    // Create a NEW LineSegment with transformed coordinates
                    reflectionPoint.wall = new LineSegment(p0, p1);
                }
            }

            curvedProfile.add(newCp);
        }
        return curvedProfile;
    }

    /**
     * Salomons, E., Van Maercke, D., Defrance, J.,&amp;De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param flatProfile Array of coordinates representing the flat profile (should be discretized with segments distance &lt; 50 m)
     * @param inverse If true, apply the inverse transformation (from curved to flat)
     * @return Array of coordinates representing the curved profile
     */
    public static Coordinate[] applyTransformation(Coordinate cs, Coordinate cr, Coordinate[] flatProfile, boolean inverse) {
        Coordinate[] curvedProfile = new Coordinate[flatProfile.length];

        // Calculate projected distance between source and receiver on the vertical plane
        double d = cs.distance(cr);

        for (int i = 0; i < flatProfile.length; i++) {
            Coordinate p = flatProfile[i];
            // it is a simplification for the inverse because p.distance3D(cs) is not good if we are not on the curved
            // profile; not mathematically exact, but it gives a close working inverse.
            double z = curvedOffset(p.distance(cs), d, inverse);

            // Create new coordinate with transformed z
            curvedProfile[i] = new Coordinate(p.x, p.y, p.z + z);
        }

        return curvedProfile;
    }

    /**
     * Equation (4) of Salomons et al. (2011): vertical offset of the curved coordinate system
     * @param x Abscissa of the point along the path from the source
     * @param d Length of the path from the source to the receiver
     * @param inverse If true, the offset of the inverse transformation (from curved to flat)
     * @return The vertical offset to add to the altitude of the point
     */
    private static double curvedOffset(double x, double d, boolean inverse) {
        // Calculate radius of curvature (Γ)
        double radius = Math.max(1000, 8 * d);
        double base = Math.sqrt(radius * radius - d * d / 4);
        double z = base - Math.sqrt(radius * radius - Math.pow(x - d / 2, 2));
        return inverse ? -z : z;
    }

}

