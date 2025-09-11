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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;

import java.util.List;
import java.util.ArrayList;

import static java.lang.Math.asin;
import static java.lang.Math.max;

/**
 * Generate a curved profile (favorable propagation conditions) from a coordinate list and two endpoints (source and receiver)
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
     * @param flatProfile
     * @return
     */
    public static List<CutPoint> applyTransformation(List<CutPoint> flatProfile, boolean inversed) {
        // Get chord endpoints
        CutPoint sourcePoint = flatProfile.get(0);
        CutPoint receiverPoint = flatProfile.get(flatProfile.size() - 1);
        Coordinate cs = sourcePoint.getCoordinate();
        Coordinate cr = receiverPoint.getCoordinate();

        List<CutPoint> curvedProfile = new ArrayList<>();
        Coordinate[] curvedCoords = applyTransformation(cs, cr, flatProfile.stream().map(CutPoint::getCoordinate).toArray(Coordinate[]::new), inversed);
        Coordinate[] groundCoords = applyTransformation(cs, cr, flatProfile.stream().map(p -> new Coordinate(p.getCoordinate().x, p.getCoordinate().y, p.zGround)).toArray(Coordinate[]::new), inversed);

        for (int i = 0; i < curvedCoords.length; i++) {
            CutPoint cp = flatProfile.get(i);
            CutPoint newCp = cp.clone();
            newCp.setZGround(groundCoords[i].z);
            newCp.setCoordinate(curvedCoords[i]);
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
        double d = cs.distance3D(cr);

        // Calculate radius of curvature (Γ)
        double radius = Math.max(1000, 8 * d);

        for (int i = 0; i < flatProfile.length; i++) {
            Coordinate p = flatProfile[i];

            // Apply equation (4) for z coordinate transformation
            double z = Math.sqrt(radius * radius - d * d / 4) -
                    Math.sqrt(radius * radius - Math.pow(p.distance3D(cs) - d/2, 2));

            if(inverse) {
                z = -z;
                // it is a simplification because p.distance3D(cs) is not good if we are not on the curved profile
            }

            // Create new coordinate with transformed z
            curvedProfile[i] = new Coordinate(p.x, p.y, p.z + z);
        }

        return curvedProfile;
    }

}

