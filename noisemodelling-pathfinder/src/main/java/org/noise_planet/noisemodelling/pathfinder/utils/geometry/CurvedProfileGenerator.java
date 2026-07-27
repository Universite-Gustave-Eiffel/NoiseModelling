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
import org.noise_planet.noisemodelling.pathfinder.utils.ComplexNumber;

import java.util.List;
import java.util.ArrayList;

import static java.lang.Math.asin;
import static java.lang.Math.max;

/**
 * Generate a curved profile from a coordinate list and two endpoints (source and receiver)
 * @author Pierre Aumond
 * @author Martin Glesser
 */
public class CurvedProfileGenerator {

    /**
     * Compute the length of a sound ray curve in favourable conditions.
     * Ref: CNOSSOS (Directive 2002/49/EC) Eq.2.5.24 and Eq. 2.5.25
     *
     * @param mn Length of ray MN in homogeneous conditions
     * @param d 3D distance between source and receiver of the unfolded path
     * @return Length of curved ray
     */
    public static double toCurve(double mn, double d){
        double curvatureRadius = max(1000, 8*d);
        return 2*curvatureRadius * asin(mn/(2*curvatureRadius));
    }

    /**
     * Manage coordinate transformation.
     *
     * @param flatProfile 3D geometrical profile
     * @param inversed If true, apply the inverse transformation (from curved to flat)
     * @return list of CutPoints representing the curved profile
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
            newCp.coordinate.z = curvedCoords[i].z;

            // If this is a reflection point, also transform the wall coordinates
            if (newCp instanceof CutPointReflection) {
                CutPointReflection reflectionPoint = (CutPointReflection) newCp;
                if (reflectionPoint.wall != null) {
                    // Transform wall endpoints using the same curved transformation
                    Coordinate[] wallCoords = new Coordinate[]{
                            new Coordinate(reflectionPoint.wall.p0),
                            new Coordinate(reflectionPoint.wall.p1)
                    };
                    Coordinate[] transformedWallCoords = applyTransformation(cs, cr, wallCoords, inversed);

                    // Create a NEW LineSegment with transformed coordinates
                    reflectionPoint.wall = new LineSegment(transformedWallCoords[0], transformedWallCoords[1]);
                }
            }

            curvedProfile.add(newCp);
        }
        return curvedProfile;
    }

    /**
     * Generate a curved profile.
     * Ref: A. Kok and A. Van Beek, “Amendments for CNOSSOS-EU,” RIVM, 2019 (Annex G1)
     *
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param flatProfile Array of coordinates representing the flat profile (should be discretized with segments distance <= 50 m)
     * @param inverse If true, apply the inverse transformation (from curved to flat)
     * @return Array of coordinates representing the curved profile
     */
    public static Coordinate[] applyTransformation(Coordinate cs, Coordinate cr, Coordinate[] flatProfile, boolean inverse) {
        Coordinate[] curvedProfile = new Coordinate[flatProfile.length];

        // Calculate projected distance between source and receiver on the vertical plane
        double d = cs.distance(cr);

        // Calculate radius of curvature (Γ)
        double radius = Math.max(1000, 8 * d);

        double base = Math.sqrt(radius * radius - d * d / 4);

        for (int i = 0; i < flatProfile.length; i++) {
            Coordinate p = flatProfile[i];

            // Apply equation (4) for z coordinate transformation
            double z = base -
                    Math.sqrt(radius * radius - Math.pow(p.distance(cs) - d/2, 2));

            if(inverse) {
                z = -z;
                // it is a simplification because p.distance3D(cs) is not good if we are not on the curved profile
                // not mathematically exact, but it gives a close working inverse.
            }

            // Create new coordinate with transformed z
            curvedProfile[i] = new Coordinate(p.x, p.y, p.z + z);
        }

        return curvedProfile;
    }

    /**
     * Generate a curved profile (CNOSSOS favourable propagation conditions) from a coordinate list, two endpoints
     * source and receiver).
     * Ref: Salomons, E., Van Maercke, D., Defrance, J. and De Roo, F. (2011). The Harmonoise sound propagation model.
     * Acta acustica united with acustica, 97(1), 62-74 (section 2.5)
     *
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param flatProfile2D Array of coordinates representing the flat profile (should be discretized with segments
     *                      distance <= 50 m)
     * @return Array of coordinates representing the curved profile
     */
    public static Coordinate[] applyHarmonoiseTransformation(
            Coordinate cs, Coordinate cr, Coordinate[] flatProfile2D){

        // Calculate projected distance between source and receiver on the vertical plane
        double d = cs.distance(cr);

        // Calculate radius of curvature (Γ)
        double radius = Math.max(1000, 8 * d);

        return applyHarmonoiseTransformation(cs, cr, flatProfile2D, radius);
    }

    /**
     * Generate a curved profile from a coordinate list, two endpoints (source and receiver) and a curvature radius.
     * Ref: Salomons, E., Van Maercke, D., Defrance, J.,&amp;De Roo, F. (2011). The Harmonoise sound propagation model.
     * Acta acustica united with acustica, 97(1), 62-74 (section 2.5)
     *
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param flatProfile2D Array of coordinates representing the flat profile (should be discretized with segments
     *                      distance <= 50 m)
     * @param radius Radius of curvature
     * @return Array of coordinates representing the curved profile
     */
    public static Coordinate[] applyHarmonoiseTransformation(
            Coordinate cs, Coordinate cr, Coordinate[] flatProfile2D, double radius){
        Coordinate[] curvedProfile = new Coordinate[flatProfile2D.length];

        // Ground curvature
        double hSource = cs.z;
        double hReceiver = cr.z;
        double hm = (hSource + hReceiver) / 2;
        double c0 = 2* (hm + radius); // Eq. 77
        ComplexNumber c = new ComplexNumber(0, c0); // Eq. 76
        double xc = 0.5 * (flatProfile2D[0].x + flatProfile2D[flatProfile2D.length-1].x);
        double yc = 0.5 * (flatProfile2D[0].y + flatProfile2D[flatProfile2D.length-1].y) + hm;
        ComplexNumber w0 = new ComplexNumber(xc, yc); // Eq. 75

        double deltaY = 0;
        for (int i = 0; i < flatProfile2D.length; i++) {
            ComplexNumber w = new ComplexNumber(flatProfile2D[i].x, flatProfile2D[i].y);
            ComplexNumber wPrim = ComplexNumber.divide(
                    ComplexNumber.multiply(c, ComplexNumber.subtract(w, w0)),
                    ComplexNumber.add(c, ComplexNumber.subtract(w, w0))
            ); // Eq. 74

            // Create new coordinate with transformed z (incl. profile translation)
            if (i == 0) {
                deltaY = flatProfile2D[i].y - wPrim.getIm();
                curvedProfile[i] = new Coordinate(wPrim.getRe() + xc, flatProfile2D[i].y , flatProfile2D[i].z);
            } else {
                curvedProfile[i] = new Coordinate(wPrim.getRe() + xc, wPrim.getIm() + deltaY, flatProfile2D[i].z);
            }
        }

        // Return
        return curvedProfile;
    }
}

