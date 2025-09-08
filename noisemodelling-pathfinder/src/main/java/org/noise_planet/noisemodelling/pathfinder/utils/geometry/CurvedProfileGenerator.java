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

/**
 * Generate a curved profile (favorable propagation conditions) from a coordinate list and two endpoints (source and receiver)
 * Based on:
 * Salomons, E., Van Maercke, D., Defrance, J., & De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
 * @author Pierre Aumond
 */
public class CurvedProfileGenerator {

    // Method to transform a list of flat coordinates to curved coordinates


    public static double computeZp(Coordinate cs, Coordinate cr, double C0, Coordinate p) {
        double xpp = p.x - (cs.x+cr.x)/2;
        double ypp = p.y - (cs.y+cr.y)/2;
        double zpp = p.z - (cs.z+cr.z)/2;

        double xpp2 = xpp*xpp;
        double ypp2 = ypp*ypp;
        double zpp2 = zpp*zpp;

        return (C0*(xpp2+ypp2+zpp2+zpp*C0))/(xpp2+ypp2+((C0+zpp)*(C0+zpp)));
    }

    /**
     * Salomons, E., Van Maercke, D., Defrance, J., & De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param hs Height of source above ground
     * @param hr Height of receiver above ground
     * @param flatProfile Array of coordinates representing the flat profile (should be discretized with segments distance < 50 m)
     * @return Array of coordinates representing the curved profile
     */
    public static Coordinate[] applyTransformation(Coordinate cs, Coordinate cr,double hs,double hr,  Coordinate[] flatProfile) {
        Coordinate[] curvedProfile = new Coordinate[flatProfile.length];

        // Radius of curvature
        double R_c = Math.max(1000, 8 * cs.distance3D(cr));
        double C0 = 2*(((hs+hr)/2)+R_c);
        double zs = computeZp(cs, cr, C0, cs) - cs.z;

        for (int i = 0; i < flatProfile.length; i++) {
            double zp = computeZp(cs, cr, C0, flatProfile[i]);
            curvedProfile[i] = new Coordinate(flatProfile[i].x, flatProfile[i].y, zp - zs); // Adjust z to keep the endpoints consistent
        }
        return curvedProfile;
    }

    /**
     * Salomons, E., Van Maercke, D., Defrance, J., & De Roo, F. (2011). The Harmonoise sound propagation model. Acta acustica united with acustica, 97(1), 62-74.
     * @param flatProfile
     * @return
     */

    public static List<CutPoint> applyTransformation(List<CutPoint> flatProfile) {

        // Get chord endpoints
        CutPoint sourcePoint = flatProfile.get(0);
        CutPoint receiverPoint = flatProfile.get(flatProfile.size() - 1);
        Coordinate cs = sourcePoint.getCoordinate();
        Coordinate cr = receiverPoint.getCoordinate();
        double hs = cs.getZ() - sourcePoint.zGround;
        double hr = cr.getZ() - receiverPoint.zGround;

        List<CutPoint> curvedProfile = new ArrayList<>();
        Coordinate[] curvedCoords = applyTransformation(cs, cr, hs, hr, flatProfile.stream().map(CutPoint::getCoordinate).toArray(Coordinate[]::new));
        Coordinate[] groundCoords = applyTransformation(cs, cr, hs, hr, flatProfile.stream().map(p -> new Coordinate(p.getCoordinate().x, p.getCoordinate().y, p.zGround)).toArray(Coordinate[]::new));

        for (int i = 0; i < curvedCoords.length; i++) {
            CutPoint cp = flatProfile.get(i);
            CutPoint newCp = cp.clone();
            newCp.setZGround(groundCoords[i].z);
            newCp.setCoordinate(curvedCoords[i]);
            curvedProfile.add(newCp);
        }
        return curvedProfile;
    }

}

