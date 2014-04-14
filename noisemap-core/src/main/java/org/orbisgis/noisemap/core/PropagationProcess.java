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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nicolas Fortin
 */
public class PropagationProcess implements Runnable {
    private final static double BASE_LVL = 1.; // 0dB lvl
    private final static double ONETHIRD = 1. / 3.;
    private final static double MERGE_SRC_DIST = 1.;
    private final static double DBA_FORGET_SOURCE = 0.03;
    private final static double FIRST_STEP_RANGE = 90;
    private final static double W_RANGE = Math.pow(10, 94. / 10.); //94 dB(A) range search. Max iso level is >75 dB(a).
    private final static double CEL = 344.23935;
    private final static int LIMITATION_RECEIVER_MIRROR = 1000;
    private final static int LIMITATION_DIFFRACTION_PATH = 1000;
    private Thread thread;
    private PropagationProcessData data;
    private PropagationProcessOut dataOut;
    private Quadtree cornersQuad;
    private int nbfreq;
    private long diffractionPathCount = 0;
    private long refpathcount = 0;
    private double[] alpha_atmo;
    private double[] freq_lambda;
    private STRtree rTreeOfGeoSoil;
    private boolean hasBuildingHeight;
    private final static Logger LOGGER = LoggerFactory.getLogger(PropagationProcess.class);


    private static double GetGlobalLevel(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    /**
     * Occlusion test on two walls. Segments are CCW oriented.
     *
     * @param wall1
     * @param wall2
     * @return True if the walls are face to face
     */
    static public boolean wallWallTest(LineSegment wall1, LineSegment wall2) {
        return ((CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(0), wall1.getCoordinate(0)}) || CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), wall2.getCoordinate(1), wall1.getCoordinate(0)})) && (CGAlgorithms.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(0), wall2.getCoordinate(0)}) || CGAlgorithms.isCCW(new Coordinate[]{wall2.getCoordinate(0), wall2.getCoordinate(1), wall1.getCoordinate(1), wall2.getCoordinate(0)})));
    }

    /**
     * Occlusion test on two walls. Segments are CCW oriented.
     *
     * @param wall1
     * @param pt
     * @return True if the wall is oriented to the point
     */
    static public boolean wallPointTest(LineSegment wall1, Coordinate pt) {
        return CGAlgorithms.isCCW(new Coordinate[]{wall1.getCoordinate(0), wall1.getCoordinate(1), pt, wall1.getCoordinate(0)});
    }

    /**
     * Recursive method to feed mirrored receiver position on walls. No
     * obstruction test is done.
     *
     * @param receiversImage     Add receiver image here
     * @param receiverCoord      Receiver coordinate or precedent mirrored coordinate
     * @param lastResult         Last row index. -1 if first reflexion
     * @param nearBuildingsWalls Walls to be reflected on
     * @param depth              Depth of reflection TODO Use segment orientation to filter
     *                           wall list
     */
    static private void feedMirroredReceiverResults(
            List<MirrorReceiverResult> receiversImage,
            Coordinate receiverCoord, int lastResult,
            List<LineSegment> nearBuildingsWalls, int depth,
            double distanceLimitation) {
        // For each wall (except parent wall) compute the mirrored coordinate
        int exceptionWallId = -1;
        if (lastResult != -1) {
            exceptionWallId = receiversImage.get(lastResult).getWallId();
        }
        int wallId = 0;
        for (LineSegment wall : nearBuildingsWalls) {
            if (wallId != exceptionWallId) {
                //Counter ClockWise test. Walls vertices are CCW oriented.
                //This help to test if a wall could see a point or another wall
                //If the triangle formed by two point of the wall + the receiver is CCW then the wall is oriented toward the point.
                boolean isCCW = false;
                if (lastResult == -1) { //If the receiverCoord is not an image
                    isCCW = wallPointTest(wall, receiverCoord);
                } else {
                    //Call wall visibility test
                    isCCW = wallWallTest(nearBuildingsWalls.get(exceptionWallId), wall);
                }

                if (isCCW) {
                    Coordinate intersectionPt = wall.project(receiverCoord);
                    if (wall.distance(receiverCoord) < distanceLimitation) // Test
                    // maximum
                    // distance
                    // constraint
                    {
                        Coordinate mirrored = new Coordinate(2 * intersectionPt.x
                                - receiverCoord.x, 2 * intersectionPt.y
                                - receiverCoord.y);
                        receiversImage.add(new MirrorReceiverResult(mirrored,
                                lastResult, wallId));
                        if (depth > 0) {
                            feedMirroredReceiverResults(receiversImage, mirrored,
                                    receiversImage.size() - 1, nearBuildingsWalls,
                                    depth - 1, distanceLimitation);
                        }
                    }
                }
            }
            wallId++;
            if (receiversImage.size() > LIMITATION_RECEIVER_MIRROR) {
                break;
            }
        }
    }

    /**
     * Compute all receiver position mirrored by specified segments
     *
     * @param receiverCoord      Position of the original receiver
     * @param nearBuildingsWalls Segments to mirror to
     * @param order              Order of reflections 1 to a limited number
     * @param distanceLimitation Limitation of searching mirrored receivers
     * @return List of possible reflections
     */
    static public List<MirrorReceiverResult> getMirroredReceiverResults(
            Coordinate receiverCoord, List<LineSegment> nearBuildingsWalls,
            int order, double distanceLimitation) {
        List<MirrorReceiverResult> receiversImage = new ArrayList<MirrorReceiverResult>();
        feedMirroredReceiverResults(receiversImage, receiverCoord, -1,
                nearBuildingsWalls, order - 1, distanceLimitation);
        return receiversImage;
    }

    public PropagationProcess(PropagationProcessData data,
                              PropagationProcessOut dataOut) {
        thread = new Thread(this);
        this.dataOut = dataOut;
        this.data = data;
    }

    public void start() {
        thread.start();
    }

    public void join() {
        try {
            thread.join();
        } catch (Exception e) {
            return;
        }
    }

    public static double dbaToW(double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public static double wToDba(double w) {
        return 10 * Math.log10(w);
    }

    /**
     * @param startPt Compute the closest point on lineString with this coordinate,
     *                use it as one of the splitted points
     * @return computed delta
     */
    private double splitLineStringIntoPoints(Geometry geom, Coordinate startPt,
                                             List<Coordinate> pts, double minRecDist) {
        // Find the position of the closest point
        Coordinate[] points = geom.getCoordinates();
        // For each segments
        Double closestPtDist = Double.MAX_VALUE;
        Coordinate closestPt = null;
        double roadLength = 0.;
        for (int i = 1; i < points.length; i++) {
            LineSegment seg = new LineSegment(points[i - 1], points[i]);
            roadLength += seg.getLength();
            Coordinate SegClosest = seg.closestPoint(startPt);
            double segcdist = SegClosest.distance(startPt);
            if (segcdist < closestPtDist) {
                closestPtDist = segcdist;
                closestPt = SegClosest;
            }
        }
        if (closestPt == null) {
            return 1.;
        }
        double delta = 20.;
        // If the minimum effective distance between the line source and the
        // receiver is smaller than the minimum distance constraint then the
        // discretisation parameter is changed
        // Delta must not not too small to avoid memory overhead.
        if (closestPtDist < minRecDist) {
            closestPtDist = minRecDist;
        }
        if (closestPtDist / 2 < delta) {
            delta = closestPtDist / 2;
        }
        pts.add(closestPt);
        Coordinate[] splitedPts = JTSUtility
                .splitMultiPointsInRegularPoints(points, delta);
        for (Coordinate pt : splitedPts) {
            if (pt.distance(closestPt) > delta) {
                pts.add(pt);
            }
        }
        if (delta < roadLength) {
            return delta;
        } else {
            return roadLength;
        }
    }

    /**
     * ISO-9613 p1 - At 15°C 70% humidity
     *
     * @param freq Third octave frequency
     * @return Attenuation coefficient dB/KM
     */
    private static double getAlpha(int freq) {
        switch (freq) {
            case 100:
                return 0.25;
            case 125:
                return 0.38;
            case 160:
                return 0.57;
            case 200:
                return 0.82;
            case 250:
                return 1.13;
            case 315:
                return 1.51;
            case 400:
                return 1.92;
            case 500:
                return 2.36;
            case 630:
                return 2.84;
            case 800:
                return 3.38;
            case 1000:
                return 4.08;
            case 1250:
                return 5.05;
            case 1600:
                return 6.51;
            case 2000:
                return 8.75;
            case 2500:
                return 12.2;
            case 3150:
                return 17.7;
            case 4000:
                return 26.4;
            case 5000:
                return 39.9;
            default:
                return 0.;
        }
    }

    private int nextFreeFieldNode(List<Coordinate> nodes, Coordinate startPt,
                                  List<Integer> NodeExceptions, int firstTestNode,
                                  FastObstructionTest freeFieldFinder) {
        int validNode = firstTestNode;
        while (NodeExceptions.contains(validNode)
                || (validNode < nodes.size() && !freeFieldFinder.isFreeField(
                startPt, nodes.get(validNode)))) {
            validNode++;
        }
        if (validNode >= nodes.size()) {
            return -1;
        }
        return validNode;
    }

    /**
     * Compute attenuation of sound energy by distance. Minimum distance is one
     * meter.
     *
     * @param Wj       Source level
     * @param distance Distance in meter
     * @return Attenuated sound level. Take only account of geometric dispersion
     * of sound wave.
     */
    public static double attDistW(double Wj, double distance) {
        if (distance < 1.) // No infinite sound level
        {
            return Wj / (4 * Math.PI);
        } else {
            return Wj / (4 * Math.PI * distance * distance);
        }
    }

    /**
     * Source-Receiver Direct+Reflection+Diffraction computation
     *
     * @param[in] srcCoord Coordinate of source
     * @param[in] receiverCoord Coordinate of receiver
     * @param[out] energeticSum Energy by frequency band
     * @param[in] alpha_atmo Atmospheric absorption by frequency band
     * @param[in] wj Source sound pressure level dB(A) by frequency band
     * @param[in] mirroredReceiver Receivers mirrored by walls (for reflection)
     * @param[in] nearBuildingsWalls Walls within maxsrcdist
     * @param[in] regionCorners Corners within maxsrcdist
     * @param[in] regionCornersFreeToReceiver List of index of corners visible
     * from receiver
     * @param[in] freq_lambda Array of sound wave lambda value by frequency band
     */
    @SuppressWarnings("unchecked")
    private void receiverSourcePropa(Coordinate srcCoord,
                                     Coordinate receiverCoord, double energeticSum[],
                                     double[] alpha_atmo, List<Double> wj,
                                     List<MirrorReceiverResult> mirroredReceiver,
                                     List<LineSegment> nearBuildingsWalls,
                                     List<Coordinate> regionCorners,
                                     List<Integer> regionCornersFreeToReceiver, double[] freq_lambda) {
        GeometryFactory factory = new GeometryFactory();
        int freqcount = data.freq_lvl.size();
        double SrcReceiverDistance = srcCoord.distance(receiverCoord);
        if (SrcReceiverDistance < data.maxSrcDist) {
            // Then, check if the source is visible from the receiver (not
            // hidden by a building)
            // Create the direct Line
            boolean somethingHideReceiver;
            somethingHideReceiver = !data.freeFieldFinder.isFreeField(
                    receiverCoord, srcCoord);
            if (!somethingHideReceiver) {
                // Evaluation of energy at receiver
                // add=wj/(4*pi*distance²)
                //add sool effet if necessary
                double ASoilmin;
                double ASoil;
                double gPath;
                double gPathPrime;
                double totRSDistance = 0.;
                //will give a flag here for soil effet
                if (data.geoWithSoilType != null) {

                    LineString RSZone = factory.createLineString(new Coordinate[]{receiverCoord, srcCoord});
                    List<EnvelopeWithIndex<Integer>> resultZ0 = rTreeOfGeoSoil.query(RSZone.getEnvelopeInternal());
                    if (!resultZ0.isEmpty()) {
                        for (EnvelopeWithIndex<Integer> envel : resultZ0) {
                            //get the geo intersected
                            Geometry geoInter = RSZone.intersection(data.geoWithSoilType.get(envel.getId()).getGeo());
                            //add the intersected distance with soil effet
                            totRSDistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                        }
                    }
                    gPath = totRSDistance / SrcReceiverDistance;
                    //NF S 31-133 page 39
                    double testForm = SrcReceiverDistance / (30 * (receiverCoord.z + srcCoord.z));
                    if (testForm <= 1) {
                        gPathPrime = testForm * gPath;
                    } else {
                        gPathPrime = gPath;
                    }


                    ASoilmin = -3 * (1 - gPathPrime);

                    for (int idfreq = 0; idfreq < freqcount; idfreq++) {
                        double AttenuatedWj = attDistW(wj.get(idfreq),
                                SrcReceiverDistance);

                        if (Double.compare(gPath, 0) != 0) {
                            //get apport of Soil Effet, ASoil will be a negative number so it's mean a apport effet
                            ASoil = getASoil(srcCoord.z, receiverCoord.z, SrcReceiverDistance, gPathPrime, idfreq, ASoilmin);
                        } else {
                            //NF S 31-133 page 41 if gPath=0 we will add 3dB for the receiver point, -3 means it's a apport effet
                            ASoil = -3;
                        }
                        AttenuatedWj = dbaToW(wToDba(AttenuatedWj) - ASoil);

                        AttenuatedWj = attAtmW(AttenuatedWj,
                                SrcReceiverDistance,
                                alpha_atmo[idfreq]);
                        energeticSum[idfreq] += AttenuatedWj;
                    }
                } else {
                    for (int idfreq = 0; idfreq < freqcount; idfreq++) {
                        double AttenuatedWj = attDistW(wj.get(idfreq),
                                SrcReceiverDistance);

                        AttenuatedWj = attAtmW(AttenuatedWj,
                                SrcReceiverDistance,
                                alpha_atmo[idfreq]);
                        energeticSum[idfreq] += AttenuatedWj;
                    }
                }
            }
            //Process diffraction 3D

            DiffractionWithSoilEffetZone diffDataWithSoilEffet = data.freeFieldFinder.getPath(receiverCoord, srcCoord);
            Double[] diffractiondata = diffDataWithSoilEffet.getDiffractionData();

            double deltadistance = diffractiondata[data.freeFieldFinder.Delta_Distance];
            double e = diffractiondata[data.freeFieldFinder.E_Length];
            double fulldistance = diffractiondata[data.freeFieldFinder.Full_Difrraction_Distance];


            //delt diffraction
            if (Double.compare(deltadistance, -1.) != 0 && Double.compare(e, -1.) != 0 && Double.compare(fulldistance, -1.) != 0 && somethingHideReceiver) {
                for (int idfreq = 0; idfreq < freqcount; idfreq++) {

                    double cprime;
                    //C" NMPB 2008 P.33

                    //Multiple diffraction
                    //CPRIME=( 1+(5*gamma)^2)/((1/3)+(5*gamma)^2)
                    double gammapart = Math.pow((5 * freq_lambda[idfreq]) / e, 2);
                    //NFS 31-133 page 46
                    if (e > 0.3) {
                        cprime = (1. + gammapart) / (ONETHIRD + gammapart);
                    } else {
                        cprime = 1.;
                    }

                    //(7.11) NMP2008 P.32
                    double testForm = (40 / freq_lambda[idfreq])
                            * cprime * deltadistance;
                    double DiffractionAttenuation = 0.;
                    if (testForm >= -2.) {
                        DiffractionAttenuation = 10 * Math
                                .log10(3 + testForm);
                    }
                    // Limit to 0<=DiffractionAttenuation
                    DiffractionAttenuation = Math.max(0,
                            DiffractionAttenuation);
                    //NF S 31-133 page 46
                    //if delt diffraction > 25 we take 25dB for delt diffraction
                    DiffractionAttenuation = Math.min(25., DiffractionAttenuation);
                    double AttenuatedWj = wj.get(idfreq);
                    // Geometric dispersion
                    //fulldistance-deltdistance is the distance direct between source and receiver
                    AttenuatedWj = attDistW(AttenuatedWj, fulldistance - deltadistance);


                    //if we add Soil effet
                    //delt soil

                    double deltSoilSO = 0.;
                    double deltSoilOR = 0.;
                    if (data.geoWithSoilType != null) {
                        double SoilSOAttenuation = 0.;
                        double SoilORAttenuation = 0.;
                        double gPathRO;
                        double gPathOS;
                        double gPathPrimeRO;
                        double gPathPrimeOS;
                        LineString ROZone = diffDataWithSoilEffet.getROZone();
                        LineString OSZone = diffDataWithSoilEffet.getOSZone();
                        double totRODistance = 0.;
                        double totOSDistance = 0.;
                        if (!rTreeOfGeoSoil.isEmpty()) {
                            //test intersection with GeoSoil
                            List<EnvelopeWithIndex<Integer>> resultZ0 = rTreeOfGeoSoil.query(ROZone.getEnvelopeInternal());
                            List<EnvelopeWithIndex<Integer>> resultZ1 = rTreeOfGeoSoil.query(OSZone.getEnvelopeInternal());
                            //if receiver-first intersection part has intersection(s)
                            if (!resultZ0.isEmpty()) {
                                //get every envelope intersected
                                for (EnvelopeWithIndex<Integer> envel : resultZ0) {
                                    //get the geo intersected
                                    Geometry geoInter = ROZone.intersection(data.geoWithSoilType.get(envel.getId()).getGeo());

                                    //add the intersected distance with soil effet
                                    totRODistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                                }

                            }
                            //if last intersection-source part has intersection(s)
                            if (!resultZ1.isEmpty()) {
                                //get every envelope intersected
                                for (EnvelopeWithIndex<Integer> envel : resultZ1) {
                                    //get the geo intersected
                                    Geometry geoInter = OSZone.intersection(this.data.geoWithSoilType.get(envel.getId()).getGeo());
                                    //add the intersected distance with soil effet
                                    totOSDistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                                }

                            }


                        }

                        //NF S 31-133 page 40
                        gPathRO = totRODistance / ROZone.getLength();
                        gPathOS = totOSDistance / OSZone.getLength();
                        //NF S 31-133 page 39
                        double testFormROZone = ROZone.getLength() / (30 * (receiverCoord.z + ROZone.getEndPoint().getCoordinate().z));
                        double testFormOSZone = OSZone.getLength() / (30 * (OSZone.getStartPoint().getCoordinate().z + srcCoord.z));
                        if (testFormROZone <= 1) {
                            gPathPrimeRO = testFormROZone * gPathRO;
                        } else {
                            gPathPrimeRO = gPathRO;
                        }

                        if (testFormOSZone <= 1) {
                            gPathPrimeOS = testFormOSZone * gPathOS;
                        } else {
                            gPathPrimeOS = gPathOS;
                        }

                        //NF S 31-133 page 41 and page 40
                        double ASoilOSMin = -3 * (1 - gPathPrimeOS);
                        double ASoilROMin = -3 * (1 - gPathRO);

                        //NF S 31-133 page 41
                        if (Double.compare(gPathRO, 0.) == 0) {
                            SoilORAttenuation = -3.;
                        } else {

                            SoilORAttenuation = getASoil(ROZone.getEndPoint().getCoordinate().z, ROZone.getStartPoint().getCoordinate().z, ROZone.getLength(), gPathRO, idfreq, ASoilROMin);
                        }
                        //NF S 31-133 page 41
                        if (Double.compare(gPathOS, 0.) == 0) {
                            SoilSOAttenuation = -3.;
                        } else {
                            SoilSOAttenuation = getASoil(OSZone.getEndPoint().getCoordinate().z, OSZone.getStartPoint().getCoordinate().z, OSZone.getLength(), gPathPrimeOS, idfreq, ASoilOSMin);

                        }

                        deltSoilSO = getDeltSoil(SoilSOAttenuation);
                        deltSoilOR = getDeltSoil(SoilORAttenuation);
                    }

                    //delt sol finished


                    // Apply diffraction attenuation with soil effet if neceesary
                    AttenuatedWj = dbaToW(wToDba(AttenuatedWj)
                            - DiffractionAttenuation - deltSoilSO - deltSoilOR);


                    // Apply atmospheric absorption and ground
                    AttenuatedWj = attAtmW(
                            AttenuatedWj,
                            fulldistance - deltadistance,
                            alpha_atmo[idfreq]);

                    energeticSum[idfreq] += AttenuatedWj;
                }


            }


            // Process specular reflection
            if (data.reflexionOrder > 0) {
                NonRobustLineIntersector linters = new NonRobustLineIntersector();
                for (MirrorReceiverResult receiverReflection : mirroredReceiver) {


                    double ReflectedSrcReceiverDistance = receiverReflection
                            .getReceiverPos().distance(srcCoord);
                    if (ReflectedSrcReceiverDistance < data.maxSrcDist) {
                        boolean validReflection = false;
                        int reflectionOrderCounter = 0;
                        MirrorReceiverResult receiverReflectionCursor = receiverReflection;
                        // Test whether intersection point is on the wall
                        // segment or not
                        Coordinate destinationPt = new Coordinate(srcCoord);
                        LineSegment seg = nearBuildingsWalls
                                .get(receiverReflection.getWallId());
                        linters.computeIntersection(seg.p0, seg.p1,
                                receiverReflection.getReceiverPos(),
                                destinationPt);
                        while (linters.hasIntersection() && PropagationProcess.wallPointTest(seg, destinationPt)) // While there is a
                        // reflection point
                        // on another wall
                        {
                            reflectionOrderCounter++;
                            // There are a probable reflection point on the
                            // segment
                            Coordinate reflectionPt = new Coordinate(
                                    linters.getIntersection(0));
                            // Translate reflection point by epsilon value to
                            // increase computation robustness
                            Coordinate vec_epsilon = new Coordinate(
                                    reflectionPt.x - destinationPt.x,
                                    reflectionPt.y - destinationPt.y);
                            double length = vec_epsilon
                                    .distance(new Coordinate(0., 0., 0.));
                            // Normalize vector
                            vec_epsilon.x /= length;
                            vec_epsilon.y /= length;
                            // Multiply by epsilon in meter
                            vec_epsilon.x *= 0.01;
                            vec_epsilon.y *= 0.01;
                            // Translate reflection pt by epsilon to get outside
                            // the wall
                            reflectionPt.x -= vec_epsilon.x;
                            reflectionPt.y -= vec_epsilon.y;
                            // Test if there is no obstacles between the
                            // reflection point and old reflection pt (or source
                            // position)
                            validReflection = data.freeFieldFinder.isFreeField(
                                    reflectionPt, destinationPt);
                            if (validReflection) // Reflection point can see
                            // source or its image
                            {
                                if (receiverReflectionCursor
                                        .getMirrorResultId() == -1) { // Direct
                                    // to
                                    // the
                                    // receiver
                                    validReflection = data.freeFieldFinder
                                            .isFreeField(reflectionPt,
                                                    receiverCoord);
                                    break; // That was the last reflection
                                } else {
                                    // There is another reflection
                                    destinationPt.setCoordinate(reflectionPt);
                                    // Move reflection information cursor to a
                                    // reflection closer
                                    receiverReflectionCursor = mirroredReceiver
                                            .get(receiverReflectionCursor
                                                    .getMirrorResultId());
                                    // Update intersection data
                                    seg = nearBuildingsWalls
                                            .get(receiverReflectionCursor
                                                    .getWallId());
                                    linters.computeIntersection(seg.p0, seg.p1,
                                            receiverReflectionCursor
                                                    .getReceiverPos(),
                                            destinationPt
                                    );
                                    validReflection = false;
                                }
                            } else {
                                break;
                            }
                        }
                        if (validReflection) {
                            //NTODO remove output
                            /*
   						    System.out.print("("+srcCoord+")Path : ");
							receiverReflectionCursor = receiverReflection;
							while(receiverReflectionCursor != null) {
								System.out.print(receiverReflectionCursor.getWallId()+" ");
								if(receiverReflectionCursor
										.getMirrorResultId()!=-1) {
								receiverReflectionCursor = mirroredReceiver
								.get(receiverReflectionCursor
										.getMirrorResultId());
								}else{
									receiverReflectionCursor=null;
								}
							}
							System.out.println();
							*/
                            // A path has been found
                            refpathcount += 1;
                            for (int idfreq = 0; idfreq < freqcount; idfreq++) {
                                // Geometric dispersion
                                double AttenuatedWj = attDistW(wj.get(idfreq),
                                        ReflectedSrcReceiverDistance);
                                // Apply wall material attenuation
                                AttenuatedWj *= Math.pow((1 - data.wallAlpha),
                                        reflectionOrderCounter);
                                // Apply atmospheric absorption and ground
                                AttenuatedWj = attAtmW(
                                        AttenuatedWj,
                                        ReflectedSrcReceiverDistance,
                                        alpha_atmo[idfreq]);
                                energeticSum[idfreq] += AttenuatedWj;
                            }
                        }
                    }
                }
            } // End reflexion
            // ///////////
            // Process diffraction paths
            if (somethingHideReceiver && data.diffractionOrder > 0
                    && !regionCornersFreeToReceiver.isEmpty()) {
                // Get the first valid receiver->corner
                int receiverFreeCornerIndex = 0;
                int firstCorner = regionCornersFreeToReceiver
                        .get(receiverFreeCornerIndex);
                if (firstCorner != -1) {
                    // History of propagation through corners
                    List<Integer> curCorner = new ArrayList<Integer>();
                    curCorner.add(firstCorner);
                    while (!curCorner.isEmpty()) {
                        Coordinate lastCorner = regionCorners.get(curCorner
                                .get(curCorner.size() - 1));
                        // Test Path is free to the source
                        if (data.freeFieldFinder.isFreeField(lastCorner,
                                srcCoord)) {
                            // True then the path is clear
                            // Compute attenuation level
                            double elength = 0;
                            //Compute distance of the corner path
                            for (int ie = 1; ie < curCorner.size(); ie++) {
                                elength += regionCorners.get(
                                        curCorner.get(ie - 1)).distance(
                                        regionCorners.get(curCorner.get(ie)));
                            }
                            // delta=SO^1+O^nO^(n+1)+O^nnR
                            double diffractionFullDistance = receiverCoord
                                    .distance(regionCorners.get(curCorner            //Receiver to first corner distance
                                            .get(0)))
                                    + elength                                        //Corner to corner distance
                                    + srcCoord
                                    .distance(regionCorners.get(curCorner    //Last corner to source distance
                                            .get(curCorner.size() - 1)));
                            if (diffractionFullDistance < data.maxSrcDist) {
                                diffractionPathCount++;
                                double delta = diffractionFullDistance
                                        - SrcReceiverDistance;

                                for (int idfreq = 0; idfreq < freqcount; idfreq++) {

                                    double cprime;
                                    //C" NMPB 2008 P.33
                                    if (curCorner.size() == 1) {
                                        cprime = 1; //Single diffraction cprime=1
                                    } else {
                                        //Multiple diffraction
                                        //CPRIME=( 1+(5*gamma)^2)/((1/3)+(5*gamma)^2)
                                        double gammapart = Math.pow((5 * freq_lambda[idfreq]) / diffractionFullDistance, 2);
                                        cprime = (1. + gammapart) / (ONETHIRD + gammapart);
                                    }
                                    //(7.11) NMP2008 P.32
                                    double testForm = (40 / freq_lambda[idfreq])
                                            * cprime * delta;
                                    double DiffractionAttenuation = 0.;
                                    if (testForm >= -2.) {
                                        DiffractionAttenuation = 10 * Math
                                                .log10(3 + testForm);
                                    } else {

                                    }
                                    // Limit to 0<=DiffractionAttenuation
                                    DiffractionAttenuation = Math.max(0,
                                            DiffractionAttenuation);
                                    double AttenuatedWj = wj.get(idfreq);
                                    // Geometric dispersion
                                    AttenuatedWj = attDistW(AttenuatedWj, SrcReceiverDistance);
                                    // Apply diffraction attenuation
                                    AttenuatedWj = dbaToW(wToDba(AttenuatedWj)
                                            - DiffractionAttenuation);
                                    // Apply atmospheric absorption and ground
                                    AttenuatedWj = attAtmW(
                                            AttenuatedWj,
                                            diffractionFullDistance,
                                            alpha_atmo[idfreq]);

                                    energeticSum[idfreq] += AttenuatedWj;
                                }
                                if (diffractionPathCount > LIMITATION_DIFFRACTION_PATH) {
                                    break; //exit diffraction search
                                }
                                // TODO removing
								/*
								 * if(somethingHideReceiver) { Coordinate[]
								 * coordinates=new
								 * Coordinate[2+curCorner.size()];
								 * coordinates[0]=receiverCoord; int idvertex=1;
								 * for(int idcorner : curCorner) {
								 * coordinates[idvertex
								 * ]=regionCorners.get(idcorner); idvertex++; }
								 * coordinates[coordinates.length-1]=srcCoord;
								 * Value[] row=new Value[3];
								 * row[0]=ValueFactory.
								 * createValue(factory.createLineString
								 * (coordinates));
								 * row[1]=ValueFactory.createValue(idReceiver);
								 * row
								 * [2]=ValueFactory.createValue(WToDba(largeAtt
								 * )); try { driver.addValues(row); } catch
								 * (DriverException e) { // TODO Auto-generated
								 * catch block e.printStackTrace(); return; } }
								 * //END REMOVING
								 */
                            }
                        }
                        // Process to the next corner
                        int nextCorner = -1;
                        if (data.diffractionOrder > curCorner.size()) {
                            // Continue to next order valid corner
                            nextCorner = nextFreeFieldNode(regionCorners,
                                    lastCorner, curCorner, 0,
                                    data.freeFieldFinder);
                            if (nextCorner != -1) {
                                curCorner.add(nextCorner);
                            }
                        }
                        while (nextCorner == -1 && !curCorner.isEmpty()) {
                            if (curCorner.size() > 1) {
                                // Next free field corner
                                nextCorner = nextFreeFieldNode(regionCorners,
                                        regionCorners.get(curCorner
                                                .get(curCorner.size() - 2)),
                                        curCorner, curCorner.get(curCorner
                                                .size() - 1),
                                        data.freeFieldFinder
                                );
                            } else {
                                // Next receiver-corner tuple
                                receiverFreeCornerIndex++;
                                if (receiverFreeCornerIndex < regionCornersFreeToReceiver
                                        .size()) {
                                    nextCorner = regionCornersFreeToReceiver
                                            .get(receiverFreeCornerIndex);
                                } else {
                                    nextCorner = -1;
                                }
                            }
                            if (nextCorner != -1) {
                                curCorner.set(curCorner.size() - 1, nextCorner);
                            } else {
                                curCorner.remove(curCorner.size() - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void insertPtSource(Coordinate receiverPos, Coordinate ptpos, List<Double> wj, double li, List<Coordinate> srcPos, List<ArrayList<Double>> srcWj, PointsMerge sourcesMerger, List<Integer> srcSortedIndex, List<Double> srcDistSorted) {
        int mergedSrcIndex = sourcesMerger.getOrAppendVertex(ptpos);
        if (mergedSrcIndex < srcPos.size()) {
            ArrayList<Double> mergedWj = srcWj.get(mergedSrcIndex);
            //A source already exist and is close enough to merge
            for (int fb = 0; fb < wj.size(); fb++) {
                mergedWj.set(fb, mergedWj.get(fb) + wj.get(fb) * li);
            }
        } else {
            //New source
            ArrayList<Double> liWj = new ArrayList<Double>(wj.size());
            for (Double lvl : wj) {
                liWj.add(lvl * li);
            }
            srcPos.add(ptpos);
            srcWj.add(liWj);
            double distanceSrcPt = ptpos.distance(receiverPos);
            int index = Collections.binarySearch(srcDistSorted, distanceSrcPt);
            if (index >= 0) {
                srcSortedIndex.add(index, mergedSrcIndex);
                srcDistSorted.add(index, distanceSrcPt);
            } else {
                srcSortedIndex.add(-index - 1, mergedSrcIndex);
                srcDistSorted.add(-index - 1, distanceSrcPt);
            }
        }
    }

    /**
     * Compute the attenuation of atmospheric absorption
     *
     * @param Wj         Source energy
     * @param dist       Propagation distance
     * @param alpha_atmo Atmospheric alpha (dB/km)
     * @return
     */
    private Double attAtmW(double Wj, double dist, double alpha_atmo) {
        return dbaToW(wToDba(Wj) - (alpha_atmo * dist) / 1000.);
    }

    /**
     * Compute sound level by frequency band at this receiver position
     *
     * @param receiverCoord
     * @param energeticSum
     */
    public void computeSoundLevelAtPosition(Coordinate receiverCoord, double energeticSum[]) {
        // List of walls within maxReceiverSource distance
        double srcEnergeticSum = BASE_LVL; //Global energetic sum of all sources processed
        List<LineSegment> nearBuildingsWalls = null;
        List<MirrorReceiverResult> mirroredReceiver = null;
        if (data.reflexionOrder > 0) {

            nearBuildingsWalls = new ArrayList<LineSegment>(
                    data.freeFieldFinder.getLimitsInRange(
                            data.maxRefDist, receiverCoord)
            );
            // Build mirrored receiver list from wall list
            mirroredReceiver = getMirroredReceiverResults(receiverCoord,
                    nearBuildingsWalls, data.reflexionOrder,
                    data.maxRefDist * 2);
            this.dataOut.appendImageReceiver(mirroredReceiver.size());
        }
        List<Coordinate> regionCorners = new ArrayList<Coordinate>();
        List<Integer> regionCornersFreeToReceiver = new ArrayList<Integer>(); // Corners
        // free
        // field
        // with
        // receiver
        if (data.diffractionOrder > 0) {
            // Query corners in the current zone
            ArrayCoordinateListVisitor cornerQuery = new ArrayCoordinateListVisitor(
                    receiverCoord, data.maxRefDist);
            cornersQuad.query(new Envelope(receiverCoord.x
                    - data.maxRefDist, receiverCoord.x + data.maxRefDist,
                    receiverCoord.y - data.maxRefDist, receiverCoord.y
                    + data.maxRefDist
            ), cornerQuery);
            regionCorners = cornerQuery.getItems();
            // regionCornersFreeToReceiver.ensureCapacity(regionCorners.size());
            for (int icorner = 0; icorner < regionCorners.size(); icorner++) {
                if (data.freeFieldFinder.isFreeField(receiverCoord,
                        regionCorners.get(icorner))) {
                    regionCornersFreeToReceiver.add(icorner);
                }
            }
        }
        // Source search by multiple range query
        HashSet<Integer> processedLineSources = new HashSet<Integer>(); //Already processed Raw source (line and/or points)
        double[] ranges = new double[]{FIRST_STEP_RANGE, data.maxSrcDist / 5, data.maxSrcDist / 4, data.maxSrcDist / 2, data.maxSrcDist};
        long sourceCount = 0;

        for (double searchSourceDistance : ranges) {
            Envelope receiverSourceRegion = new Envelope(receiverCoord.x
                    - searchSourceDistance, receiverCoord.x + searchSourceDistance,
                    receiverCoord.y - searchSourceDistance, receiverCoord.y
                    + searchSourceDistance
            );
            Iterator<Integer> regionSourcesLst = data.sourcesIndex
                    .query(receiverSourceRegion);

            PointsMerge sourcesMerger = new PointsMerge(MERGE_SRC_DIST);
            List<Integer> srcSortByDist = new ArrayList<Integer>();
            List<Double> srcDist = new ArrayList<Double>();
            List<Coordinate> srcPos = new ArrayList<Coordinate>();
            List<ArrayList<Double>> srcWj = new ArrayList<ArrayList<Double>>();
            while (regionSourcesLst.hasNext()) {
                Integer srcIndex = regionSourcesLst.next();
                if (!processedLineSources.contains(srcIndex)) {
                    processedLineSources.add(srcIndex);
                    Geometry source = data.sourceGeometries.get(srcIndex);
                    List<Double> wj = data.wj_sources.get(srcIndex); // DbaToW(sdsSources.getDouble(srcIndex,dbField
                    if (source instanceof Point) {
                        Coordinate ptpos = ((Point) source).getCoordinate();
                        insertPtSource(receiverCoord, ptpos, wj, 1., srcPos, srcWj, sourcesMerger, srcSortByDist, srcDist);
                        // Compute li to equation 4.1 NMPB 2008 (June 2009)
                    } else {
                        // Discretization of line into multiple point
                        // First point is the closest point of the LineString from
                        // the receiver
                        ArrayList<Coordinate> pts = new ArrayList<Coordinate>();
                        double li = splitLineStringIntoPoints(source, receiverCoord,
                                pts, data.minRecDist);
                        for (Coordinate pt : pts) {
                            insertPtSource(receiverCoord, pt, wj, li, srcPos, srcWj, sourcesMerger, srcSortByDist, srcDist);
                        }
                        // Compute li to equation 4.1 NMPB 2008 (June 2009)
                    }
                }
            }
            //Iterate over source point sorted by their distance from the receiver
            for (int mergedSrcId : srcSortByDist) {
                // For each Pt Source - Pt Receiver
                Coordinate srcCoord = srcPos.get(mergedSrcId);
                ArrayList<Double> wj = srcWj.get(mergedSrcId);
                double allreceiverfreqlvl = GetGlobalLevel(nbfreq, energeticSum);
                double allsourcefreqlvl = 0;
                for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
                    allsourcefreqlvl += wj.get(idfreq);
                }

                double wAttDistSource = attDistW(allsourcefreqlvl, srcCoord.distance(receiverCoord));
                srcEnergeticSum += wAttDistSource;
                if (Math.abs(wToDba(wAttDistSource + allreceiverfreqlvl) - wToDba(allreceiverfreqlvl)) > DBA_FORGET_SOURCE) {
                    sourceCount++;
                    receiverSourcePropa(srcCoord, receiverCoord, energeticSum,
                            alpha_atmo, wj, mirroredReceiver,
                            nearBuildingsWalls, regionCorners,
                            regionCornersFreeToReceiver, freq_lambda);
                }
            }
            //srcEnergeticSum=GetGlobalLevel(nbfreq,energeticSum);
            if (Math.abs(wToDba(attDistW(W_RANGE, searchSourceDistance) + srcEnergeticSum) - wToDba(srcEnergeticSum)) < DBA_FORGET_SOURCE) {
                break; //Stop search for fartest sources
            }
        }
        dataOut.appendSourceCount(sourceCount);
    }

    /**
     * Must be called before computeSoundLevelAtPosition
     */
    public void initStructures() {
        nbfreq = data.freq_lvl.size();
        // Init wave length for each frequency
        freq_lambda = new double[nbfreq];
        for (int idf = 0; idf < nbfreq; idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = CEL / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }
        // Compute atmospheric alpha value by specified frequency band
        alpha_atmo = new double[data.freq_lvl.size()];
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            alpha_atmo[idfreq] = getAlpha(data.freq_lvl.get(idfreq));
        }
        // /////////////////////////////////////////////
        // Search diffraction corners
        cornersQuad = new Quadtree();
        if (data.diffractionOrder > 0) {
            List<Coordinate> corners = data.freeFieldFinder.getWideAnglePoints(
                    Math.PI * (1 + 1 / 16.0), Math.PI * (2 - (1 / 16.)));
            // Build Quadtree
            for (Coordinate corner : corners) {
                cornersQuad.insert(new Envelope(corner), corner);
            }
        }
        //Build R-tree for soil geometry and soil type
        rTreeOfGeoSoil = new STRtree();
        {
            if (data.geoWithSoilType != null) {
                for (int i = 0; i < data.geoWithSoilType.size(); i++) {
                    rTreeOfGeoSoil.insert(data.geoWithSoilType.get(i).getGeo().getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(data.geoWithSoilType.get(i).getGeo().getEnvelopeInternal(),
                            i));

                }
            }
        }
    }

    @Override
    public void run() {
        try {
            initStructures();
            GeometryFactory factory = new GeometryFactory();

            // TODO comment debugging code

            /*
             * Type
             * meta_type[]={TypeFactory.createType(Type.GEOMETRY),TypeFactory.createType
             * (Type.INT),TypeFactory.createType(Type.DOUBLE)}; String
             * meta_name[]={"the_geom","difid","largebandatt"}; DefaultMetadata
             * metadata = new DefaultMetadata(meta_type,meta_name); DiskBufferDriver
             * driver; try { driver = new DiskBufferDriver(data.dsf,metadata ); }
             * catch (DriverException e) { e.printStackTrace(); return; }
             */

            double verticesSoundLevel[] = new double[data.vertices.size()]; // Computed
            // sound
            // level
            // of
            // vertices


            // For each vertices, find sources where the distance is within
            // maxSrcDist meters
            ProgressionProcess propaProcessProgression = data.cellProg;
            int idReceiver = 0;
            long min_compute_time = Long.MAX_VALUE;
            long max_compute_time = 0;
            long sum_compute = 0;
            for (Coordinate receiverCoord : data.vertices) {
                long debReceiverTime = System.nanoTime();

                propaProcessProgression.nextSubProcessEnd();
                double energeticSum[] = new double[data.freq_lvl.size()];
                for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
                    energeticSum[idfreq] = 0.0;
                }
                computeSoundLevelAtPosition(receiverCoord, energeticSum);
                // Save the sound level at this receiver
                // Do the sum of all frequency bands
                double allfreqlvl = 0;
                for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
                    allfreqlvl += energeticSum[idfreq];
                }
                allfreqlvl = Math.max(allfreqlvl, BASE_LVL);
                verticesSoundLevel[idReceiver] = allfreqlvl;

                long computeTime = System.nanoTime() - debReceiverTime;
                min_compute_time = Math.min(computeTime, min_compute_time);
                max_compute_time = Math.max(computeTime, max_compute_time);
                sum_compute += computeTime;
                idReceiver++;
            }
            if (data.triangles != null) { //Triangle output type
                // Subdivide each triangle, and apply BiCubic interpolation.
                        /*
                         * ArrayList<Triangle> bicubictri=new ArrayList<Triangle>();
                         * bicubictri.ensureCapacity(data.triangles.size()); for(Triangle tri :
                         * data.triangles) { //////////////////////// //Find the fourth vertex }
                         */
                // Now export all triangles with the sound level at each vertices
                int tri_id = 0;
                for (Triangle tri : data.triangles) {
                    Coordinate pverts[] = {data.vertices.get(tri.getA()),
                            data.vertices.get(tri.getB()),
                            data.vertices.get(tri.getC()),
                            data.vertices.get(tri.getA())};
                    dataOut.addValues(new PropagationResultTriRecord(
                            factory.createPolygon(factory.createLinearRing(pverts), null),
                            verticesSoundLevel[tri.getA()],
                            verticesSoundLevel[tri.getB()],
                            verticesSoundLevel[tri.getC()],
                            data.cellId,
                            tri_id));
                    tri_id++;
                }
            } else {
                //Vertices output type
                for (int receiverId = 0; receiverId < data.vertices.size(); receiverId++) {
                    dataOut.addValues(new PropagationResultPtRecord(data.receiverRowId.get(receiverId), data.cellId, verticesSoundLevel[receiverId]));
                }
            }
            dataOut.appendFreeFieldTestCount(data.freeFieldFinder.getNbObstructionTest());
            dataOut.appendCellComputed();
            dataOut.updateMaximalReceiverComputationTime(max_compute_time);
            dataOut.updateMinimalReceiverComputationTime(min_compute_time);
            dataOut.addSumReceiverComputationTime(sum_compute);
            dataOut.appendDiffractionPath(diffractionPathCount);
            dataOut.appendReflexionPath(refpathcount);
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }


    private double getIntersectedDistance(Geometry geo) {

        double totDistance = 0.;
        for (int i = 0; i < geo.getNumGeometries(); i++) {
            Coordinate[] coordinates = geo.getGeometryN(i).getCoordinates();
            if (coordinates.length > 1 && geo.getGeometryN(i) instanceof LineString) {
                totDistance += geo.getGeometryN(i).getLength();
            }
        }
        return totDistance;

    }

    /**
     * getASoil use equation ASol in NF S 31-133 page 41 to calculate Attenuation(or contribuition) Soil Effet
     *
     * @param zs       z of source point
     * @param zr       z of receiver point
     * @param dp       dp in equation
     * @param gw       Gw
     * @param fm       frequence
     * @param aSoilMin min ASoil
     * @return ASoil
     */
    private double getASoil(double zs, double zr, double dp, double gw, int fm, double aSoilMin) {
        //NF S 31-133 page 41 c
        double k = 2 * Math.PI * fm / CEL;
        //NF S 31-113 page 41 w
        double w = 0.0185 * Math.pow(fm, 2.5) * Math.pow(gw, 2.6) /
                (Math.pow(fm, 1.5) * Math.pow(gw, 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(fm, 0.75) * Math.pow(gw, 1.3) + 1.16 * Math.pow(10, 6));
        //NF S 31-113 page 41 Cf
        double cf = dp * (1 + 3 * w * dp * Math.pow(Math.E, -Math.sqrt(w * dp))) / (1 + w * dp);
        //NF S 31-113 page 41 A sol
        double ASoil = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(dp, 2) *
                (Math.pow(zs, 2) - Math.sqrt(2 * cf / k) * zs + cf / k) * (Math.pow(zr, 2) - Math.sqrt(2 * cf / k) * zr + cf / k));
        ASoil = Math.max(ASoil, aSoilMin);
        return ASoil;

    }


    private double getDeltSoil(double ASoil) {

        return -20 * Math.log10(1 + (Math.pow(10, -ASoil / 20) - 1));
    }

}
