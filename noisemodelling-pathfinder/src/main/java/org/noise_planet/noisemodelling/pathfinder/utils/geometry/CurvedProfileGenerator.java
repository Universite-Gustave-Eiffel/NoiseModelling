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

import java.util.Arrays;
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
     * Generate a curved profile (CNOSSOS favourable propagation conditions) from a cutPoint list.
     * Ref: A. Kok and A. Van Beek, “Amendments for CNOSSOS-EU,” RIVM, 2019 (Annex G1)
     *
     * @param straightProfile List of cutPoints representing the uncurved profile
     * @return list of CutPoints representing the curved profile
     */
    public static List<CutPoint> applyTransformation(List<CutPoint> straightProfile) {
        // Get chord endpoints
        CutPoint sourcePoint = straightProfile.getFirst();
        CutPoint receiverPoint = straightProfile.getLast();
        Coordinate cs = sourcePoint.getCoordinate();
        Coordinate cr = receiverPoint.getCoordinate();

        List<CutPoint> curvedProfile = new ArrayList<>();
        List<Coordinate> curvedCoords = applyTransformation(cs, cr,
                straightProfile.stream()
                        .map(CutPoint::getCoordinate)
                        .toList()
        );
        List <Coordinate> groundCoords = applyTransformation(cs, cr,
                straightProfile.stream()
                        .map(p -> new Coordinate(p.getCoordinate().x, p.getCoordinate().y, p.zGround))
                        .toList()
        );

        for (int i = 0; i < curvedCoords.size(); i++) {
            CutPoint cp = straightProfile.get(i);
            CutPoint newCp = cp.clone();
            newCp.setZGround(groundCoords.get(i).z);
            newCp.setCoordinate(curvedCoords.get(i));

            // If this is a reflection point, also transform the wall coordinates
            if (newCp instanceof CutPointReflection reflectionPoint) {
                if (reflectionPoint.wall != null) {
                    // Transform wall endpoints using the same curved transformation
                    List<Coordinate> wallCoords = List.of(
                            new Coordinate(reflectionPoint.wall.p0),
                            new Coordinate(reflectionPoint.wall.p1)
                    );
                    List<Coordinate> transformedWallCoords = applyTransformation(cs, cr, wallCoords);

                    // Create a NEW LineSegment with transformed coordinates
                    reflectionPoint.wall = new LineSegment(transformedWallCoords.getFirst(), transformedWallCoords.get(1));
                }
            }

            curvedProfile.add(newCp);
        }
        return curvedProfile;
    }

//    /**
//     * Generate a curved profile (CNOSSOS favourable propagation conditions) from a coordinate list, two endpoints
//     * source and receiver).
//     * Ref: Salomons, E., Van Maercke, D., Defrance, J. and De Roo, F. (2011). The Harmonoise sound propagation model.
//     * Acta acustica united with acustica, 97(1), 62-74 (section 2.5)
//     *
//     * @param cs Source coordinate
//     * @param cr Receiver coordinate
//     * @param straightProfile List of 3D coordinates representing the uncurved profile (should be discretized with
//     *                      segments distance <= 50 m)
//     * @return List of 3D coordinates representing the curved profile
//     */
//    public static List <Coordinate> applyTransformation(
//            Coordinate cs, Coordinate cr, List <Coordinate> straightProfile){
//
//        // Calculate projected distance between source and receiver on the vertical plane
//        double d = cs.distance(cr);
//
//        // Calculate radius of curvature (Γ) for favourable condition
//        double radius = Math.max(1000, 8 * d);
//
//        // Compute curved profile
//        List <Coordinate> curvedProfile2D = applyTransformation(cs, cr, straightProfile, radius);
//        List <Coordinate> curvedProfile = Arrays.asList(new Coordinate[straightProfile.size()]);
//        for (int i = 0; i < straightProfile.size(); i++) {
//            curvedProfile.set(i, new Coordinate(
//                    straightProfile.get(i).x,
//                    straightProfile.get(i).y,
//                    curvedProfile2D.get(i).y)
//            );
//        }
//
//        return curvedProfile;
//    }


    /**
     * Generate a curved profile.
     * Ref: A. Kok and A. Van Beek, “Amendments for CNOSSOS-EU,” RIVM, 2019 (Annex G1)
     *
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param straightProfile List of coordinates representing the flat profile (should be discretized with segments distance <= 50 m)
     * @return List of coordinates representing the curved profile
     */
     public static List<Coordinate> applyTransformation(Coordinate cs, Coordinate cr, List<Coordinate> straightProfile) {
        List <Coordinate> curvedProfile = Arrays.asList(new Coordinate[straightProfile.size()]);

        // Calculate projected distance between source and receiver on the vertical plane
        double d = cs.distance(cr);

        // Calculate radius of curvature (Γ)
        double radius = Math.max(1000, 8 * d);

        double base = Math.sqrt(radius * radius - d * d / 4);

        for (int i = 0; i < straightProfile.size(); i++) {
            Coordinate p = straightProfile.get(i);

            // Apply equation (4) for z coordinate transformation
            double z = base -
                    Math.sqrt(radius * radius - Math.pow(p.distance(cs) - d/2, 2));

            // Create new coordinate with transformed z
            curvedProfile.set(i, new Coordinate(p.x, p.y, p.z + z));
        }

        return curvedProfile;
    }

    /**
     * Generate a curved profile from a coordinate list, two endpoints (source and receiver) and a curvature radius.
     * Ref: Salomons, E., Van Maercke, D., Defrance, J.,&amp;De Roo, F. (2011). The Harmonoise sound propagation model.
     * Acta acustica united with acustica, 97(1), 62-74 (section 2.5)
     * Note: This implementation yield similar results to the one from applyTransformation. However, it works only on
     * the whole ground profile (from zGroundSource to zGroundReceiver).
     *
     * @param cs Source coordinate
     * @param cr Receiver coordinate
     * @param straightProfile2D List of 2D coordinates representing the whole unfolded non-curved profile (from
     *                        zGround_source to zGround_receiver)
     * @param radius Radius of curvature
     * @return List of 2D coordinates representing the unfolded curved profile
     */
    public static List<Coordinate> applyHarmonoiseTransformation(
            Coordinate cs, Coordinate cr, List<Coordinate> straightProfile2D, double radius){
        // Ground curvature
        double hSource = cs.z;
        double hReceiver = cr.z;
        double hm = (hSource + hReceiver) / 2;
        double c0 = 2* (hm + radius); // Eq. 77
        ComplexNumber c = new ComplexNumber(0, c0); // Eq. 76
        double xc = 0.5 * (straightProfile2D.getFirst().x + straightProfile2D.getLast().x);
        double yc = 0.5 * (straightProfile2D.getFirst().y + straightProfile2D.getLast().y) + hm;
        ComplexNumber w0 = new ComplexNumber(xc, yc); // Eq. 75

        double deltaY = 0;
        List<Coordinate> curvedProfile = Arrays.asList(new Coordinate[straightProfile2D.size()]);
        for (int i = 0; i < straightProfile2D.size(); i++) {
            ComplexNumber w = new ComplexNumber(straightProfile2D.get(i).x, straightProfile2D.get(i).y);
            ComplexNumber wPrim = ComplexNumber.divide(
                    ComplexNumber.multiply(c, ComplexNumber.subtract(w, w0)),
                    ComplexNumber.add(c, ComplexNumber.subtract(w, w0))
            ); // Eq. 74

            // Create new coordinate with transformed z (incl. profile translation)
            if (i == 0) {
                deltaY = straightProfile2D.get(i).y - wPrim.getIm();
                curvedProfile.set(i,
                        new Coordinate(wPrim.getRe() + xc, straightProfile2D.get(i).y , straightProfile2D.get(i).z));
            } else {
                curvedProfile.set(i,
                        new Coordinate(wPrim.getRe() + xc, wPrim.getIm() + deltaY, straightProfile2D.get(i).z));
            }
        }

        // Return
        return curvedProfile;
    }
}

