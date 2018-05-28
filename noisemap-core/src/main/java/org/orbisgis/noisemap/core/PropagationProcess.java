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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.orbisgis.noisemap.core.FastObstructionTest.*;

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
    // NMPB states Celerity of the sound in the air, taken equal to 340 m/s.
    private final static double CEL = 340;
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
    private final static Logger LOGGER = LoggerFactory.getLogger(PropagationProcess.class);


    private static double GetGlobalLevel(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
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
        Double bestClosestPtDist = Double.MAX_VALUE;
        Coordinate closestPt = null;
        double roadLength = 0.;
        for (int i = 1; i < points.length; i++) {
            LineSegment seg = new LineSegment(points[i - 1], points[i]);
            roadLength += seg.getLength();
            Coordinate ptClosest = seg.closestPoint(startPt);
            // Interpolate Z value
            ptClosest.setOrdinate(2, Vertex.interpolateZ(ptClosest, seg.p0, seg.p1));
            double closestDist = CGAlgorithms3D.distance(startPt, ptClosest);
            if (closestDist < bestClosestPtDist) {
                bestClosestPtDist = closestDist;
                closestPt = ptClosest;
            }
        }
        if (closestPt == null) {
            return 1.;
        }
        double delta = 20.;
        // If the minimum effective distance between the line source and the
        // receiver is smaller than the minimum distance constraint then the
        // discretization parameter is changed
        // Delta must not not too small to avoid memory overhead.
        if (bestClosestPtDist < minRecDist) {
            bestClosestPtDist = minRecDist;
        }
        if (bestClosestPtDist / 2 < delta) {
            delta = bestClosestPtDist / 2;
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


    public void computeReflexion(Coordinate receiverCoord,
                                 Coordinate srcCoord,List<Double> wj, List<FastObstructionTest.Wall> nearBuildingsWalls,
                                 double[] energeticSum, List<PropagationDebugInfo> debugInfo) {
        // Compute receiver mirror
        LineSegment srcReceiver = new LineSegment(srcCoord, receiverCoord);
        LineIntersector linters = new RobustLineIntersector();
        long imageReceiver = 0;
        MirrorReceiverIterator.It mirroredReceivers = new MirrorReceiverIterator.It(receiverCoord, nearBuildingsWalls,
                srcReceiver, data.maxRefDist, data.reflexionOrder, data.maxSrcDist);
        for (MirrorReceiverResult receiverReflection : mirroredReceivers) {
            double ReflectedSrcReceiverDistance = receiverReflection.getReceiverPos().distance(srcCoord);
            imageReceiver++;
            boolean validReflection = false;
            int reflectionOrderCounter = 0;
            MirrorReceiverResult receiverReflectionCursor = receiverReflection;
            // Test whether intersection point is on the wall
            // segment or not
            Coordinate destinationPt = new Coordinate(srcCoord);
            Wall seg = nearBuildingsWalls
                    .get(receiverReflection.getWallId());
            linters.computeIntersection(seg.p0, seg.p1,
                    receiverReflection.getReceiverPos(),
                    destinationPt);
            PropagationDebugInfo propagationDebugInfo = null;
            if(debugInfo != null) {
                propagationDebugInfo = new PropagationDebugInfo(new LinkedList<>(Arrays.asList(srcCoord)), new double[data.freq_lvl.size()]);
            }
            // While there is a reflection point on another wall. And intersection point is in the wall z bounds.
            while (linters.hasIntersection() && MirrorReceiverIterator.wallPointTest(seg, destinationPt))
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
                // Compute Z interpolation
                reflectionPt.setOrdinate(Coordinate.Z, Vertex.interpolateZ(linters.getIntersection(0),
                        receiverReflectionCursor.getReceiverPos(), destinationPt));

                // Test if there is no obstacles between the
                // reflection point and old reflection pt (or source
                // position)
                validReflection = (Double.isNaN(receiverReflectionCursor.getReceiverPos().z) ||
                        Double.isNaN(destinationPt.z) || seg.getBuildingId() == 0
                        || reflectionPt.z < data.freeFieldFinder.getBuildingRoofZ(seg.getBuildingId()))
                        && data.freeFieldFinder.isFreeField(reflectionPt, destinationPt);
                if (validReflection) // Reflection point can see
                // source or its image
                {
                    if(propagationDebugInfo != null) {
                        propagationDebugInfo.getPropagationPath().add(0, reflectionPt);
                    }
                    if (receiverReflectionCursor
                            .getParentMirror() == null) { // Direct
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
                        receiverReflectionCursor = receiverReflectionCursor.getParentMirror();
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
                if(propagationDebugInfo != null) {
                    propagationDebugInfo.getPropagationPath().add(0, receiverCoord);
                }
                // A path has been found
                refpathcount += 1;
                for (int idfreq = 0; idfreq < wj.size(); idfreq++) {
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
                    if(propagationDebugInfo != null) {
                        propagationDebugInfo.addNoiseContribution(idfreq, AttenuatedWj);
                    }
                }
                if(propagationDebugInfo != null && debugInfo != null) {
                    debugInfo.add(propagationDebugInfo);
                }
            }
        }
        this.dataOut.appendImageReceiver(imageReceiver);
    }
    public double computeDeltaDiffraction(int idfreq, double eLength, double deltaDistance) {
        double cprime;
        //C" NMPB 2008 P.33
        //Multiple diffraction
        //CPRIME=( 1+(5*gamma)^2)/((1/3)+(5*gamma)^2)
        double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / eLength, 2);
        //NFS 31-133 page 46
        if (eLength > 0.3) {
            cprime = (1. + gammaPart) / (ONETHIRD + gammaPart);
        } else {
            cprime = 1.;
        }

        //(7.11) NMP2008 P.32
        double testForm = (40 / freq_lambda[idfreq])
                * cprime * deltaDistance;
        double diffractionAttenuation = 0.;
        if (testForm >= -2.) {
            diffractionAttenuation = 10 * Math
                    .log10(3 + testForm);
        }
        // Limit to 0<=DiffractionAttenuation
        diffractionAttenuation = Math.max(0,
                diffractionAttenuation);
        //NF S 31-133 page 46
        //if delta diffraction > 25 we take 25dB for delta diffraction
        return Math.min(25., diffractionAttenuation);
    }

    private static List<Coordinate> removeDuplicates(List<Coordinate> coordinates) {
        return Arrays.asList(CoordinateUtils.removeDuplicatedCoordinates(
                coordinates.toArray(new Coordinate[coordinates.size()]), false));
    }

    private double[] computeDeltaDistance(DiffractionWithSoilEffetZone diffDataWithSoilEffet) {
        final Coordinate srcCoord = diffDataWithSoilEffet.getOSZone().getCoordinate(1);
        final Coordinate receiverCoord = diffDataWithSoilEffet.getROZone().getCoordinate(0);
        final LineSegment OSZone = diffDataWithSoilEffet.getOSZone();
        final LineSegment ROZone = diffDataWithSoilEffet.getROZone();
        final double fulldistance = diffDataWithSoilEffet.getFullDiffractionDistance();
        // R' is the projection of R on the mean ground plane (O,R)
        List<Coordinate> planeCoordinates = new ArrayList<>(diffDataWithSoilEffet.getrOgroundCoordinates());
        planeCoordinates.addAll(diffDataWithSoilEffet.getoSgroundCoordinates());
        planeCoordinates = JTSUtility.getNewCoordinateSystem(planeCoordinates);
        List<Coordinate> rOPlaneCoordinates = planeCoordinates.subList(0, diffDataWithSoilEffet.getrOgroundCoordinates().size());
        List<Coordinate> oSPlaneCoordinates = planeCoordinates.subList(rOPlaneCoordinates.size(), planeCoordinates.size());
        // Compute source position using new plane system
        Coordinate rotatedSource = new Coordinate(oSPlaneCoordinates.get(oSPlaneCoordinates.size() - 1));
        rotatedSource.setOrdinate(1, srcCoord.z);
        Coordinate rotatedOs = new Coordinate(oSPlaneCoordinates.get(0));
        rotatedOs.setOrdinate(1, OSZone.getCoordinate(0).z);
        // Compute receiver position using new plane system
        Coordinate rotatedReceiver = new Coordinate(rOPlaneCoordinates.get(0));
        rotatedReceiver.setOrdinate(1, receiverCoord.z);
        Coordinate rotatedOr = new Coordinate(rOPlaneCoordinates.get(rOPlaneCoordinates.size() - 1));
        rotatedOr.setOrdinate(1, ROZone.getCoordinate(1).z);
        // Compute mean ground plane
        final double[] oSFuncParam = JTSUtility.getLinearRegressionPolyline(removeDuplicates(oSPlaneCoordinates));
        final double[] rOFuncParam = JTSUtility.getLinearRegressionPolyline(removeDuplicates(rOPlaneCoordinates));
        // Compute source and receiver image on ground
        Coordinate rPrim = JTSUtility.makePointImage(rOFuncParam[0], rOFuncParam[1], rotatedReceiver);
        Coordinate sPrim = JTSUtility.makePointImage(oSFuncParam[0], oSFuncParam[1], rotatedSource);
        double deltaDistanceORprim = (fulldistance - CGAlgorithms3D.distance(ROZone.p0, ROZone.p1)
                + rPrim.distance(rotatedOr)) - rPrim.distance(rotatedSource);
        // S' is the projection of R on the mean ground plane (S,O)
        double deltaDistanceSprimO = (fulldistance - CGAlgorithms3D.distance(OSZone.p0, OSZone.p1)
                + sPrim.distance(rotatedOs)) - sPrim.distance(rotatedReceiver);
        return new double[] {deltaDistanceSprimO, deltaDistanceORprim};
    }

    public void computeHorizontalEdgeDiffraction(boolean obstructedSourceReceiver,Coordinate receiverCoord,
                                                 Coordinate srcCoord, List<Double> wj,
                                                 List<PropagationDebugInfo> debugInfo, double[] energeticSum) {
        DiffractionWithSoilEffetZone diffDataWithSoilEffet = data.freeFieldFinder.getPath(receiverCoord, srcCoord);
        GeometryFactory factory = new GeometryFactory();
        double deltadistance = diffDataWithSoilEffet.getDeltaDistance();
        double e = diffDataWithSoilEffet.geteLength();
        double fulldistance = diffDataWithSoilEffet.getFullDiffractionDistance();

        //delta diffraction
        if (Double.compare(deltadistance, -1.) != 0 && Double.compare(e, -1.) != 0 &&
                Double.compare(fulldistance, -1.) != 0) {
            PropagationDebugInfo propagationDebugInfo = null;
            if(debugInfo != null) {
                propagationDebugInfo = new PropagationDebugInfo(Arrays.asList(receiverCoord,
                        diffDataWithSoilEffet.getROZone().p1,
                        diffDataWithSoilEffet.getOSZone().p0, srcCoord),
                        new double[data.freq_lvl.size()]);
            }
            double gPathRO = 0;
            double gPathOS= 0;
            double gPathPrimeOS = 0;
            double ASoilOSMin = 0;
            double ASoilROMin = 0;
            double deltaDistanceORprim = 0;
            double deltaDistanceSprimO = 0;
            LineSegment ROZone = diffDataWithSoilEffet.getROZone();
            LineSegment OSZone = diffDataWithSoilEffet.getOSZone();
            if (data.geoWithSoilType != null) {
                double[] deltaDist = computeDeltaDistance(diffDataWithSoilEffet);
                deltaDistanceSprimO = deltaDist[0];
                deltaDistanceORprim = deltaDist[1];
                // test intersection with GeoSoil
                List<EnvelopeWithIndex<Integer>> resultZ0 = rTreeOfGeoSoil.query(new Envelope(ROZone.p0, ROZone.p1));
                List<EnvelopeWithIndex<Integer>> resultZ1 = rTreeOfGeoSoil.query(new Envelope(OSZone.p0, OSZone.p1));
                // if receiver-first intersection part has intersection(s)
                double totRODistance = 0.;
                double totOSDistance = 0.;
                if (!resultZ0.isEmpty()) {
                    //get every envelope intersected
                    for (EnvelopeWithIndex<Integer> envel : resultZ0) {
                        //get the geo intersected
                        Geometry geoInter = ROZone.toGeometry(factory).intersection(data.geoWithSoilType.get(envel.getId()).getGeo());

                        //add the intersected distance with ground effect
                        totRODistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                    }

                }
                //if last intersection-source part has intersection(s)
                if (!resultZ1.isEmpty()) {
                    //get every envelope intersected
                    for (EnvelopeWithIndex<Integer> envel : resultZ1) {
                        //get the geo intersected
                        Geometry geoInter = OSZone.toGeometry(factory).intersection(this.data.geoWithSoilType.get(envel.getId()).getGeo());
                        //add the intersected distance with ground effect
                        totOSDistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                    }
                }

                //NF S 31-133 page 40
                gPathRO = totRODistance / ROZone.getLength();
                gPathOS = totOSDistance / OSZone.getLength();
                //NF S 31-133 page 39
                double testFormOSZone = OSZone.getLength() / (30 * (OSZone.p0.z + srcCoord.z));

                if (testFormOSZone <= 1) {
                    gPathPrimeOS = testFormOSZone * gPathOS;
                } else {
                    gPathPrimeOS = gPathOS;
                }

                //NF S 31-133 page 41 and page 40
                ASoilOSMin = -3 * (1 - gPathPrimeOS);
                // NMPB 2008
                // There is no call here to take into account the correction G'trajet as the source considered is
                // no longer the road itself but the diffraction point. It is therefore clearly Gtrajet which must
                // be used in calculating the ground effects, including for the lower bound term of the formula
                // which becomes -3 (1-Gtrajet).
                ASoilROMin = -3 * (1 - gPathRO);
            }
            for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
                double AttenuatedWj = wj.get(idfreq);
                // Geometric dispersion
                //fulldistance-deltdistance is the distance direct between source and receiver
                AttenuatedWj = attDistW(AttenuatedWj, fulldistance - deltadistance);


                //if we add Ground effect
                double deltSoilSO = -3;
                double deltaSoilOR = 0.;
                // NF S 31-133 page 47 9.4.3.1
                // δ if negative if S R are not obstructed
                double deltaDiffSR = computeDeltaDiffraction(idfreq, e, obstructedSourceReceiver? deltadistance : -deltadistance);
                if (data.geoWithSoilType != null) {
                    double SoilSOAttenuation;
                    double SoilORAttenuation= 0;
                    //NF S 31-133 page 41
                    if (gPathRO > 0) {
                        SoilORAttenuation = getASoil(ROZone.p1.z, ROZone.p0.z, ROZone.getLength(), gPathRO, data.freq_lvl.get(idfreq), ASoilROMin);
                    }
                    //NF S 31-133 page 41
                    if (Double.compare(gPathOS, 0.) == 0) {
                        SoilSOAttenuation = -3.;
                    } else {
                        SoilSOAttenuation = getASoil(OSZone.p1.z, OSZone.p0.z, OSZone.getLength(), gPathPrimeOS, data.freq_lvl.get(idfreq), ASoilOSMin);
                    }
                    // delta soil
                    // Compute diffraction data for deltaDiffS'R
                    double deltaDiffSprimR = computeDeltaDiffraction(idfreq, e, deltaDistanceSprimO);
                    //Compute diffraction data for deltaDiffSR'
                    double deltaDiffSRprim = computeDeltaDiffraction(idfreq, e, deltaDistanceORprim);
                    deltSoilSO = getDeltaSoil(SoilSOAttenuation,deltaDiffSprimR,deltaDiffSR);
                    deltaSoilOR = getDeltaSoil(SoilORAttenuation,deltaDiffSRprim,deltaDiffSR);
                }

                //delta sol finished


                // Apply diffraction attenuation with ground effect if necessary
                AttenuatedWj = dbaToW(wToDba(AttenuatedWj)
                        - deltaDiffSR - deltSoilSO - deltaSoilOR);


                // Apply atmospheric absorption and ground
                AttenuatedWj = attAtmW(
                        AttenuatedWj,
                        fulldistance - deltadistance,
                        alpha_atmo[idfreq]);

                energeticSum[idfreq] += AttenuatedWj;
                if(propagationDebugInfo != null) {
                    propagationDebugInfo.addNoiseContribution(idfreq, AttenuatedWj);
                }
            }
            if(propagationDebugInfo != null && debugInfo != null) {
                debugInfo.add(propagationDebugInfo);
            }
        }
    }

    /**
     * Add vertical edge diffraction noise contribution to energetic sum.
     * @param regionCorners
     * @param srcCoord
     * @param receiverCoord
     * @param energeticSum
     * @param alpha_atmo
     * @param wj
     */
    public void computeVerticalEdgeDiffraction(List<Coordinate> regionCorners, Coordinate srcCoord,
                                               Coordinate receiverCoord, double energeticSum[], double[] alpha_atmo,
                                               List<Double> wj, List<PropagationDebugInfo> debugInfo) {
        final LineSegment receiverSrc = new LineSegment(receiverCoord, srcCoord);
        final double SrcReceiverDistance = CGAlgorithms3D.distance(srcCoord, receiverCoord);
        // Get the first valid receiver->corner
        int freqcount = data.freq_lvl.size();
        List<Integer> curCorner = new ArrayList<>();
        int firstCorner = nextFreeFieldNode(regionCorners, receiverCoord, receiverSrc, curCorner, 0, data.freeFieldFinder);
        if (firstCorner != -1) {
            // History of propagation through corners
            curCorner.add(firstCorner);
            while (!curCorner.isEmpty()) {
                Coordinate lastCorner = getProjectedZCoordinate(regionCorners.get(curCorner.get(curCorner.size() - 1)
                ), receiverSrc);
                // Test Path is free to the source
                if (data.freeFieldFinder.isFreeField(lastCorner, srcCoord)) {
                    // True then the path is clear
                    // Compute attenuation level
                    double eLength = 0;
                    //Compute distance of the corner path
                    for (int ie = 1; ie < curCorner.size(); ie++) {
                        Coordinate cornerA = regionCorners.get(curCorner.get(ie));
                        Coordinate cornerB = regionCorners.get(curCorner.get(ie - 1));
                        eLength += CGAlgorithms3D.distance(cornerA, cornerB);
                    }
                    // delta=SO^1+O^nO^(n+1)+O^nnR
                    double receiverCornerDistance = CGAlgorithms3D.distance(receiverCoord,
                            regionCorners.get(curCorner.get(0)));
                    double sourceCornerDistance = CGAlgorithms3D.distance(srcCoord,
                            regionCorners.get(curCorner.get(curCorner.size() - 1)));
                    double diffractionFullDistance = receiverCornerDistance + eLength
                                                               //Corner to corner distance
                            + sourceCornerDistance;
                    if (diffractionFullDistance < data.maxSrcDist) {
                        diffractionPathCount++;
                        double delta = diffractionFullDistance - SrcReceiverDistance;
                        PropagationDebugInfo propagationDebugInfo = null;
                        if (debugInfo != null) {
                            List<Coordinate> path = new ArrayList<>(curCorner.size() + 2);
                            path.add(receiverCoord);
                            for (Integer aCurCorner : curCorner) {
                                path.add(regionCorners.get(aCurCorner));
                            }
                            path.add(srcCoord);
                            propagationDebugInfo = new PropagationDebugInfo(path, new double[freqcount]);
                        }
                        for (int idfreq = 0; idfreq < freqcount; idfreq++) {
                            double diffractionAttenuation = computeDeltaDiffraction(idfreq, diffractionFullDistance,
                                    delta);
                            double attenuatedWj = wj.get(idfreq);
                            // Geometric dispersion
                            attenuatedWj = attDistW(attenuatedWj, SrcReceiverDistance);
                            // Apply diffraction attenuation
                            attenuatedWj = dbaToW(wToDba(attenuatedWj) - diffractionAttenuation);
                            // Apply atmospheric absorption and ground
                            attenuatedWj = attAtmW(attenuatedWj, diffractionFullDistance, alpha_atmo[idfreq]);

                            energeticSum[idfreq] += attenuatedWj;
                            if (propagationDebugInfo != null) {
                                propagationDebugInfo.addNoiseContribution(idfreq, attenuatedWj);
                            }
                        }
                        if (debugInfo != null) {
                            debugInfo.add(propagationDebugInfo);
                        }
                    }
                }
                // Process to the next corner
                int nextCorner = -1;
                if (data.diffractionOrder > curCorner.size()) {
                    // Continue to next order valid corner
                    nextCorner = nextFreeFieldNode(regionCorners, lastCorner, receiverSrc, curCorner, 0,
                            data.freeFieldFinder);
                    if (nextCorner != -1) {
                        curCorner.add(nextCorner);
                    }
                }
                while (nextCorner == -1 && !curCorner.isEmpty()) {
                    Coordinate startPoint = receiverCoord;
                    if (curCorner.size() > 1) {
                        startPoint = regionCorners.get(curCorner.get(curCorner.size() - 2));
                    }
                    // Next free field corner
                    nextCorner = nextFreeFieldNode(regionCorners, startPoint, receiverSrc, curCorner,
                            curCorner.get(curCorner.size() - 1), data.freeFieldFinder);
                    if (nextCorner != -1) {
                        curCorner.set(curCorner.size() - 1, nextCorner);
                    } else {
                        curCorner.remove(curCorner.size() - 1);
                    }
                }
            }
        }
    }

    /**
     * Compute project Z coordinate between p0 p1 of x,y.
     * @param coordinateWithoutZ Coordinate to set the Z value from Z interpolation of line
     * @param line Extract Z values of this segment
     * @return coordinateWithoutZ with Z value computed from line.
     */
    private static Coordinate getProjectedZCoordinate(Coordinate coordinateWithoutZ, LineSegment line) {
        // Z value is the interpolation of source-receiver line
        return new Coordinate(coordinateWithoutZ.x, coordinateWithoutZ.y, Vertex.interpolateZ(
                line.closestPoint(coordinateWithoutZ), line.p0, line.p1));
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

    private int nextFreeFieldNode(List<Coordinate> nodes, Coordinate startPt,LineSegment segmentConstraint,
                                  List<Integer> NodeExceptions, int firstTestNode,
                                  FastObstructionTest freeFieldFinder) {
        int validNode = firstTestNode;
        while (NodeExceptions.contains(validNode)
                || (validNode < nodes.size() && (Math.abs(segmentConstraint.projectionFactor(nodes.get(validNode))) > 1 || !freeFieldFinder.isFreeField(
                startPt, getProjectedZCoordinate(nodes.get(validNode),segmentConstraint))))) {
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
        return Wj / (4 * Math.PI * Math.max(1, distance * distance));
    }

    /**
     * Source-Receiver Direct+Reflection+Diffraction computation
     *
     * @param[in] srcCoord Coordinate of source
     * @param[in] receiverCoord Coordinate of receiver
     * @param[out] energeticSum Energy by frequency band
     * @param[in] alpha_atmo Atmospheric absorption by frequency band
     * @param[in] wj Source sound pressure level dB(A) by frequency band
     * @param[in] nearBuildingsWalls Walls within maxsrcdist
     * from receiver
     */
    @SuppressWarnings("unchecked")
    private void receiverSourcePropa(Coordinate srcCoord,
                                     Coordinate receiverCoord, double energeticSum[],
                                     double[] alpha_atmo, List<Double> wj,
                                     List<FastObstructionTest.Wall> nearBuildingsWalls, List<PropagationDebugInfo> debugInfo) {
        GeometryFactory factory = new GeometryFactory();

        List<Coordinate> regionCorners = fetchRegionCorners(new LineSegment(srcCoord, receiverCoord),data.maxRefDist);

        // Build mirrored receiver list from wall list

        int freqcount = data.freq_lvl.size();

        double PropaDistance = srcCoord.distance(receiverCoord);
        if (PropaDistance < data.maxSrcDist) {
            // Then, check if the source is visible from the receiver (not
            // hidden by a building)
            // Create the direct Line
            boolean somethingHideReceiver = false;
            boolean buildingOnPath = false;

            if(!data.computeVerticalDiffraction || !data.freeFieldFinder.isHasBuildingWithHeight()) {
                somethingHideReceiver = !data.freeFieldFinder.isFreeField(receiverCoord, srcCoord);
            } else {
                List<TriIdWithIntersection> propagationPath = new ArrayList<>();
               if(!data.freeFieldFinder.computePropagationPath(receiverCoord, srcCoord, false, propagationPath, false)) {
                   // Propagation path not found, there is not direct field
                   somethingHideReceiver = true;
               } else {
                   if (!propagationPath.isEmpty()) {
                       for (TriIdWithIntersection inter : propagationPath) {
                           if (inter.isIntersectionOnBuilding() || inter.isIntersectionOnTopography()) {
                               somethingHideReceiver = true;
                           }
                           if (inter.getBuildingId() != 0) {
                               buildingOnPath = true;
                           }
                       }
                   }
               }
            }
            double SrcReceiverDistance = CGAlgorithms3D.distance(srcCoord, receiverCoord);

// todo insert the condition delta < lambda/20 if Atalus (the attenuation from a possible bank source side) is used

            if (!somethingHideReceiver) {
                // Evaluation of energy at receiver
                // add=wj/(4*pi*distance²)
                //add ground effect if necessary
                double ASoilmin = 0;
                double ASoil;
                double gPath = 0;
                double gPathPrime = 0;
                double totRSDistance = 0.;
                double zr = receiverCoord.z;
                double zs = srcCoord.z;
                //will give a flag here for soil effect
                if (data.geoWithSoilType != null) {
                    LineString RSZone = factory.createLineString(new Coordinate[]{receiverCoord, srcCoord});
                    List<EnvelopeWithIndex<Integer>> resultZ0 = rTreeOfGeoSoil.query(RSZone.getEnvelopeInternal());
                    if (!resultZ0.isEmpty()) {
                        for (EnvelopeWithIndex<Integer> envel : resultZ0) {
                            //get the geo intersected
                            Geometry geoInter = RSZone.intersection(data.geoWithSoilType.get(envel.getId()).getGeo());
                            //add the intersected distance with ground effect
                            totRSDistance += getIntersectedDistance(geoInter) * this.data.geoWithSoilType.get(envel.getId()).getType();
                        }
                    }
                    gPath = totRSDistance / SrcReceiverDistance;
                    //NF S 31-133 page 39
                    List<TriIdWithIntersection> inters = new ArrayList<>();
                    data.freeFieldFinder.computePropagationPath(receiverCoord, srcCoord, false, inters, true);
                    List<Coordinate> rSground = data.freeFieldFinder.getGroundProfile(inters);
                    rSground = JTSUtility.getNewCoordinateSystem(rSground);
                    // Compute mean ground plan
                    double[] ab = JTSUtility.getLinearRegressionPolyline(removeDuplicates(rSground));
                    Coordinate rotatedReceiver = new Coordinate(rSground.get(0));
                    rotatedReceiver.setOrdinate(1, receiverCoord.z);
                    Coordinate rotatedSource = new Coordinate(rSground.get(rSground.size() - 1));
                    rotatedSource.setOrdinate(1, srcCoord.z);
                    zr = rotatedReceiver.distance(JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedReceiver));
                    zs = rotatedSource.distance(JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedSource));
                    double testForm = SrcReceiverDistance / (30 * (zs + zr));
                    if (testForm <= 1) {
                        gPathPrime = testForm * gPath;
                    } else {
                        gPathPrime = gPath;
                    }
                    ASoilmin = -3 * (1 - gPathPrime);
                }
                PropagationDebugInfo propagationDebugInfo = null;
                if(debugInfo != null) {
                    propagationDebugInfo = new PropagationDebugInfo(Arrays.asList(receiverCoord, srcCoord), new double[freqcount]);
                }
                for (int idfreq = 0; idfreq < freqcount; idfreq++) {
                    double AttenuatedWj = attDistW(wj.get(idfreq),
                            SrcReceiverDistance);
                    if (data.geoWithSoilType != null) {
                        if (Double.compare(gPath, 0) != 0) {
                            //get contribution of Ground Effect, ASoil will be a negative number so it's mean a contribution effect
                            ASoil = getASoil(zs, zr, SrcReceiverDistance, gPathPrime, data.freq_lvl.get(idfreq), ASoilmin);
                        } else {
                            //NF S 31-133 page 41 if gPath=0 we will add 3dB for the receiver point, -3 means it's a contribution effect
                            ASoil = -3;
                        }
                        AttenuatedWj = dbaToW(wToDba(AttenuatedWj) - ASoil);
                    }
                    AttenuatedWj = attAtmW(AttenuatedWj,
                            SrcReceiverDistance,
                            alpha_atmo[idfreq]);
                    energeticSum[idfreq] += AttenuatedWj;
                    if(propagationDebugInfo != null) {
                        propagationDebugInfo.addNoiseContribution(idfreq, AttenuatedWj);
                    }
                }
                if(propagationDebugInfo != null) {
                    debugInfo.add(propagationDebugInfo);
                }
            }
            //Process diffraction 3D
            if( data.computeVerticalDiffraction && buildingOnPath) {
                computeHorizontalEdgeDiffraction(somethingHideReceiver ,receiverCoord, srcCoord, wj, debugInfo, energeticSum);
            }
            // Process specular reflection
            if (data.reflexionOrder > 0) {
                computeReflexion(receiverCoord, srcCoord, wj, nearBuildingsWalls, energeticSum, debugInfo);
            } // End reflexion
            // ///////////
            // Process diffraction paths
            if (somethingHideReceiver && data.diffractionOrder > 0) {
                computeVerticalEdgeDiffraction(regionCorners, srcCoord, receiverCoord, energeticSum, alpha_atmo, wj, debugInfo);
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

            double dx = ptpos.x-receiverPos.x;
            double dy = ptpos.y-receiverPos.y;
            double dz = ptpos.z-receiverPos.z;

            double distanceSrcPt = Math.sqrt(dx*dx+dy*dy+dz*dz);// TODO i have change like line 384

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
     * Use {@link #cornersQuad} to fetch all region corners in max ref dist
     * @param fetchLine Compute distance from this line (Propagation line)
     * @param maxDistance Maximum distance to fetch corners (m)
     * @return Filtered corner index list
     */
    public List<Coordinate> fetchRegionCorners(LineSegment fetchLine, double maxDistance) {
        // Build Envelope
        GeometryFactory factory = new GeometryFactory();
        Geometry env = factory.createLineString(new Coordinate[]{fetchLine.p0, fetchLine.p1});
        env = env.buffer(maxDistance, 0, BufferParameters.CAP_FLAT);
        // Query corners in the current zone
        ArrayCoordinateListVisitor cornerQuery = new ArrayCoordinateListVisitor(env);
        Envelope queryEnv = new Envelope(fetchLine.p0, fetchLine.p1);
        queryEnv.expandBy(maxDistance);
        cornersQuad.query(queryEnv, cornerQuery);
        return cornerQuery.getItems();
    }

    /**
     * Compute sound level by frequency band at this receiver position
     *
     * @param receiverCoord
     * @param energeticSum
     */
    public void computeSoundLevelAtPosition(Coordinate receiverCoord, double energeticSum[], List<PropagationDebugInfo> debugInfo) {
        // List of walls within maxReceiverSource distance
        double srcEnergeticSum = BASE_LVL; //Global energetic sum of all sources processed
        STRtree walls = new STRtree();
        if (data.reflexionOrder > 0) {
            for(Wall wall : data.freeFieldFinder.getLimitsInRange(
                            data.maxSrcDist, receiverCoord, false)) {
                walls.insert(new Envelope(wall.p0, wall.p1), wall);
            }
        }
        // Source search by multiple range query
        HashSet<Integer> processedLineSources = new HashSet<Integer>(); //Already processed Raw source (line and/or points)
        double[] ranges = new double[]{Math.min(FIRST_STEP_RANGE,data.maxSrcDist / 6) , data.maxSrcDist / 5, data.maxSrcDist / 4, data.maxSrcDist / 2, data.maxSrcDist};
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
                        Coordinate ptpos = source.getCoordinate();
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
                double wAttDistSource = attDistW(allsourcefreqlvl, CGAlgorithms3D.distance(srcCoord, receiverCoord));
                srcEnergeticSum += wAttDistSource;
                if (Math.abs(wToDba(wAttDistSource + allreceiverfreqlvl) - wToDba(allreceiverfreqlvl)) > DBA_FORGET_SOURCE) {
                    sourceCount++;
                    Envelope query = new Envelope(receiverCoord, srcCoord);
                    query.expandBy(Math.min(data.maxRefDist, srcCoord.distance(receiverCoord)));
                    List queryResult = walls.query(query);
                    receiverSourcePropa(srcCoord, receiverCoord, energeticSum,
                            alpha_atmo, wj,
                            (List<FastObstructionTest.Wall>)queryResult, debugInfo);
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
        if (data.geoWithSoilType != null) {
            for (int i = 0; i < data.geoWithSoilType.size(); i++) {
                GeoWithSoilType geoWithSoilType = data.geoWithSoilType.get(i);
                rTreeOfGeoSoil.insert(geoWithSoilType.getGeo().getEnvelopeInternal(),
                        new EnvelopeWithIndex<Integer>(geoWithSoilType.getGeo().getEnvelopeInternal(), i));
            }
        }
    }

    public void runDebug(List<PropagationDebugInfo> debugInfo) {
        try {
            initStructures();

            // Computed sound level of vertices
            dataOut.setVerticesSoundLevel(new double[data.receivers.size()]);

            // For each vertices, find sources where the distance is within
            // maxSrcDist meters
            ProgressVisitor propaProcessProgression = data.cellProg;


            Runtime runtime = Runtime.getRuntime();
            int splitCount = runtime.availableProcessors();
            ThreadPool threadManager = new ThreadPool(
                    splitCount,
                    splitCount + 1, Long.MAX_VALUE,
                    TimeUnit.SECONDS);
            int maximumReceiverBatch = (int)Math.ceil(data.receivers.size() / (double)splitCount);
            int endReceiverRange = 0;
            while(endReceiverRange < data.receivers.size()) {
                int newEndReceiver = Math.min(endReceiverRange + maximumReceiverBatch, data.receivers.size());
                RangeReceiversComputation batchThread = new RangeReceiversComputation(endReceiverRange,
                        newEndReceiver, this, propaProcessProgression, debugInfo);
                //batchThread.run();
                threadManager.executeBlocking(batchThread);
                endReceiverRange = newEndReceiver;
            }
            threadManager.shutdown();
            threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            dataOut.appendFreeFieldTestCount(data.freeFieldFinder.getNbObstructionTest());
            dataOut.appendCellComputed();
            dataOut.appendDiffractionPath(diffractionPathCount);
            dataOut.appendReflexionPath(refpathcount);
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void run() {
        runDebug(null);
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
     * getASoil use equation ASol in NF S 31-133 page 41 to calculate Attenuation(or contribution) Ground Effect
     *
     * @param zs       z of source point
     * @param zr       z of receiver point
     * @param dp       dp in equation
     * @param gw       Gw
     * @param fm       frequency
     * @param aSoilMin min ASoil
     * @return ASoil
     */
    private static double getASoil(double zs, double zr, double dp, double gw, int fm, double aSoilMin) {
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


    /**
     * Formulae 7.18 and 7.20
     * @param aSoil Asol(O,R) or Asol(S,O) (sol mean ground)
     * @param deltaDifPrim Δdif(S,R') if Asol(S,O) is given or Δdif(S', R) if Asol(O,R)
     * @param deltaDif Δdif(S, R)
     * @return Δsol(S,O) if Asol(S,O) is given or Δsol(O,R) if Asol(O,R) is given
     */
    private double getDeltaSoil(double aSoil, double deltaDifPrim, double deltaDif) {
        return -20 * Math.log10(1 + (Math.pow(10, -aSoil / 20) - 1)) * Math.pow(10, -(deltaDifPrim - deltaDif)/20);
    }

    private static class RangeReceiversComputation implements Runnable {
        private final int startReceiver; // Included
        private final int endReceiver; // Excluded
        private PropagationProcess propagationProcess;
        private List<PropagationDebugInfo> debugInfo;
        private ProgressVisitor progressVisitor;

        private RangeReceiversComputation(int startReceiver, int endReceiver, PropagationProcess propagationProcess, ProgressVisitor progressVisitor, List<PropagationDebugInfo> debugInfo) {
            this.startReceiver = startReceiver;
            this.endReceiver = endReceiver;
            this.propagationProcess = propagationProcess;
            this.debugInfo = debugInfo;
            this.progressVisitor = progressVisitor;
        }

        @Override
        public void run() {
            for (int idReceiver = startReceiver; idReceiver < endReceiver; idReceiver++) {
                Coordinate receiverCoord = propagationProcess.data.receivers.get(idReceiver);
                double energeticSum[] = new double[propagationProcess.data.freq_lvl.size()];
                Arrays.fill(energeticSum, 0d);
                propagationProcess.computeSoundLevelAtPosition(receiverCoord, energeticSum, debugInfo);
                // Save the sound level at this receiver
                // Do the sum of all frequency bands
                double allfreqlvl = 0d;
                for (double anEnergeticSum : energeticSum) {
                    allfreqlvl += anEnergeticSum;
                }
                allfreqlvl = Math.max(allfreqlvl, BASE_LVL);
                propagationProcess.dataOut.setVerticeSoundLevel(idReceiver,allfreqlvl);
                progressVisitor.endStep();
            }
        }
    }
}
