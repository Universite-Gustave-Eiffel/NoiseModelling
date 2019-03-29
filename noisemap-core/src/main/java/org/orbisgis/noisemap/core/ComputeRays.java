/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.locationtech.jts.algorithm.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.triangulate.Segment;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.poly2tri.triangulation.util.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.orbisgis.noisemap.core.FastObstructionTest.Wall;


/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeRays implements Runnable {
    private final static double BASE_LVL = 1.; // 0dB lvl
    private final static double ONETHIRD = 1. / 3.;
    // Reject side diffraction if hull length > than direct length
    // because 20 * LOG10(4) = 12 dB, so small contribution in comparison with diffraction on horizontal edge
    // in order to reduce computational cost
    private final static double MAX_RATIO_HULL_DIRECT_PATH = 4;
    private final static double MERGE_SRC_DIST = 1.;
    private final static double FIRST_STEP_RANGE = 90;
    private final static double W_RANGE = Math.pow(10, 94. / 10.); //94 dB(A) range search. Max iso level is >75 dB(a).
    private Thread thread;
    private PropagationProcessData data;
    private ComputeRaysOut dataOut;
    private Quadtree cornersQuad;
    private int nbfreq;
    private long diffractionPathCount = 0;
    private long refpathcount = 0;
    private double[] alpha_atmo;
    private double[] freq_lambda;
    // todo implement this next variable as input parameter
    private double gS = 0; // 0 si route, 1 si ballast

    private STRtree rTreeOfGeoSoil;
    private final static Logger LOGGER = LoggerFactory.getLogger(ComputeRays.class);

    private double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(p * dbaToW(array1[i]) + (1 - p) * dbaToW(array2[i]));
        }
        return sum;
    }

    private double[] sumArray(double[] array1, double[] array2) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(dbaToW(array1[i]) + dbaToW(array2[i]));
        }
        return sum;
    }

    private void computeWithMeteo(HashMap<Integer, double[]> aGlobal, PropagationProcessPathData propData, ComputeRaysOut propDataOut, double p) {
        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        double[] aGlobalMeteo = new double[8];
        for (PropagationPath propath : propDataOut.propagationPaths) {
            aGlobalMeteo = aGlobal.get(propath.idSource);
            propath.setFavorable(false);
            evaluateAttenuationCnossos.evaluate(propath, propData);
            if (aGlobalMeteo != null) {
                aGlobalMeteo = sumArray(evaluateAttenuationCnossos.getaGlobal(), aGlobalMeteo);
            } else {
                aGlobalMeteo = evaluateAttenuationCnossos.getaGlobal();
            }

            propath.setFavorable(true);
            evaluateAttenuationCnossos.evaluate(propath, propData);
            aGlobalMeteo = sumArrayWithPonderation(aGlobalMeteo, evaluateAttenuationCnossos.getaGlobal(), p);
            aGlobal.put(propath.idSource, aGlobalMeteo);
        }
    }

    private static double GetGlobalLevel(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    public ComputeRays(PropagationProcessData data,
                       ComputeRaysOut dataOut) {
        thread = new Thread(this);
        this.dataOut = dataOut;
        this.data = data;
    }

    /**
     * Update ground Z coordinates of sound sources and receivers absolute to sea levels
     */
    public void makeRelativeZToAbsolute() {
        AbsoluteCoordinateSequenceFilter filter = new AbsoluteCoordinateSequenceFilter(data.freeFieldFinder, true);
        for (Geometry source : data.sourceGeometries) {
            source.apply(filter);
        }
        CoordinateSequence sequence = new CoordinateArraySequence(data.receivers.toArray(new Coordinate[data.receivers.size()]));
        for (int i = 0; i < sequence.size(); i++) {
            filter.filter(sequence, i);
        }
        data.receivers = Arrays.asList(sequence.toCoordinateArray());
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


    public List<PropagationPath> computeReflexion(Coordinate receiverCoord,
                                            Coordinate srcCoord, boolean favorable, List<Wall> nearBuildingsWalls,
                                            List<PropagationDebugInfo> debugInfo) {
        // Compute receiver mirror
        LineSegment srcReceiver = new LineSegment(srcCoord, receiverCoord);
        LineIntersector linters = new RobustLineIntersector();
        long imageReceiver = 0;

        double altR = 0;
        double altS = 0;
        Coordinate lastPoint = new Coordinate();
        Coordinate projSource;
        GeometryFactory factory = new GeometryFactory();
        double gPath = 0;
        double totRSDistance = 0.;
        boolean flagRefl = false ;

        Coordinate reflectionPt = new Coordinate();
        List<PropagationPath> propagationPaths_all = new ArrayList<>();


        MirrorReceiverIterator.It mirroredReceivers = new MirrorReceiverIterator.It(receiverCoord, nearBuildingsWalls,
                srcReceiver, data.maxRefDist, data.reflexionOrder, data.maxSrcDist);

        for (MirrorReceiverResult receiverReflection : mirroredReceivers) {

            PropagationPath propagationPath = new PropagationPath();
            List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
            List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
            List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
            int refcount = 0;

            double ReflectedSrcReceiverDistance = receiverReflection.getReceiverPos().distance(srcCoord);

            imageReceiver++;
            boolean validReflection = false;
            MirrorReceiverResult receiverReflectionCursor = receiverReflection;
            // Test whether intersection point is on the wall
            // segment or not
            Coordinate destinationPt = new Coordinate(srcCoord);

            Wall seg = nearBuildingsWalls.get(receiverReflection.getWallId());
            linters.computeIntersection(seg.p0, seg.p1,
                    receiverReflection.getReceiverPos(),
                    destinationPt);
            PropagationDebugInfo propagationDebugInfo = null;
            if (debugInfo != null) {
                propagationDebugInfo = new PropagationDebugInfo(new LinkedList<>(Arrays.asList(srcCoord)), new double[data.freq_lvl.size()]);
            }
            // While there is a reflection point on another wall. And intersection point is in the wall z bounds.
            reflectionPt = new Coordinate(linters.getIntersection(0));
            while (linters.hasIntersection() && MirrorReceiverIterator.wallPointTest(seg, destinationPt)) {
                // There are a probable reflection point on the segment
                if (reflectionPt.equals(destinationPt)) {
                    break;
                }
                // Compute Z interpolation
                reflectionPt.setOrdinate(Coordinate.Z, Vertex.interpolateZ(linters.getIntersection(0),
                        receiverReflectionCursor.getReceiverPos(), destinationPt));

                // Test if there is no obstacles between the
                // reflection point and old reflection pt (or source position)
                validReflection = (Double.isNaN(receiverReflectionCursor.getReceiverPos().z) ||
                        Double.isNaN(reflectionPt.z) || Double.isNaN(destinationPt.z) || seg.getBuildingId() == 0
                        || reflectionPt.z < data.freeFieldFinder.getBuildingRoofZ(seg.getBuildingId())
                        || reflectionPt.z > data.freeFieldFinder.getHeightAtPosition(reflectionPt)
                        || destinationPt.z > data.freeFieldFinder.getHeightAtPosition(destinationPt));
                if (validReflection) // Reflection point can see
                // source or its image
                // source or its image
                {

                    if (propagationDebugInfo != null) {
                        propagationDebugInfo.getPropagationPath().add(0, reflectionPt);
                    }
                    if (receiverReflectionCursor
                            .getParentMirror() == null) { // Direct to the receiver
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
            if (validReflection && !Double.isNaN(reflectionPt.z) ) {
                if (propagationDebugInfo != null) {
                    propagationDebugInfo.getPropagationPath().add(0, receiverCoord);
                }
                lastPoint = reflectionPt;

                // A path has been found
                List<PropagationPath> propagationPaths = directPath(destinationPt, reflectionPt, data.computeVerticalDiffraction,false,  debugInfo);

                if (propagationPaths.size() > 0 ) {
                    refpathcount += 1;
                    refcount +=1;
                    propagationPath = propagationPaths.get(0);
                    propagationPath.getPointList().get(propagationPath.getPointList().size() - 1).setType(PropagationPath.PointPath.POINT_TYPE.REFL);
                    propagationPath.getPointList().get(propagationPath.getPointList().size() - 1).setBuildingId(receiverReflection.getBuildingId());

                    propagationPath.getPointList().get(propagationPath.getPointList().size() - 1).setAlphaWall(data.freeFieldFinder.getBuildingAlpha(receiverReflection.getBuildingId()));
                    if (refcount > 1) {
                        propagationPath.getPointList().remove(0);
                    }
                    points.addAll(propagationPath.getPointList());
                    segments.addAll(propagationPath.getSegmentList());
                }


                if (propagationDebugInfo != null && debugInfo != null) {
                    debugInfo.add(propagationDebugInfo);
                }


            }
            if (refcount > 0 ) {
                List<PropagationPath> propagationPaths = directPath(lastPoint, receiverCoord, data.computeVerticalDiffraction,false, debugInfo);
                if (propagationPaths.size() > 0 ) {
                    propagationPath = propagationPaths.get(0);
                    propagationPath.getPointList().remove(0);
                    points.addAll(propagationPath.getPointList());
                    segments.addAll(propagationPath.getSegmentList());
                    srPath.add(new PropagationPath.SegmentPath(0.0, new Vector3D(srcCoord, receiverCoord)));


                    for (int i = 1; i < points.size(); i++) {
                        if (points.get(i).type == PropagationPath.PointPath.POINT_TYPE.DIFH) {
                            if (points.get(i).coordinate.z <= data.freeFieldFinder.getHeightAtPosition(points.get(i).coordinate)) {
                                points.clear();
                                segments.clear();
                            }
                        }
                        if (points.get(i).type == PropagationPath.PointPath.POINT_TYPE.REFL) {
                            points.get(i).coordinate.z = Vertex.interpolateZ(points.get(i).coordinate, points.get(i - 1).coordinate, points.get(i + 1).coordinate);
                            //check if in building && if under floor
                            if (points.get(i).coordinate.z > data.freeFieldFinder.getBuildingRoofZ(points.get(i).getBuildingId())
                                    || points.get(i).coordinate.z <= data.freeFieldFinder.getHeightAtPosition(points.get(i).coordinate)) {
                                points.clear();
                                segments.clear();
                            }


                        }
                    }
                    if (points.size() > 2) {
                    /*for (int i = 1; i < points.size(); i++) {
                        if (points.get(i).coordinate.z <10) {
                            points.clear();
                            segments.clear();
                        }

                    }*/
                        propagationPaths_all.add(new PropagationPath(favorable, points, segments, srPath));
                    }
                }
            }
        }

        this.dataOut.appendImageReceiver(imageReceiver);
        return propagationPaths_all;
    }


    private static List<Coordinate> removeDuplicates(List<Coordinate> coordinates) {
        return Arrays.asList(CoordinateUtils.removeDuplicatedCoordinates(
                coordinates.toArray(new Coordinate[coordinates.size()]), false));
    }


    /**
     * @param receiverCoord
     * @param srcCoord
     * @param inters PropagationPath between srcCoord and receiverCoord (or null if must be computed here)
     * @param debugInfo
     */
    public PropagationPath computeFreefield(Coordinate receiverCoord,
                                            Coordinate srcCoord,List<TriIdWithIntersection> inters,
                                            List<PropagationDebugInfo> debugInfo) {

        GeometryFactory factory = new GeometryFactory();
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();

        double gPath = 0;
        double totRSDistance = 0.;
        double altR = 0;
        double altS = 0;
        Coordinate projReceiver;
        Coordinate projSource;

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
            // Compute GPath using 2D Length
            gPath = totRSDistance / RSZone.getLength();

            if(inters == null) {
                inters = new ArrayList<>();
                data.freeFieldFinder.computePropagationPath(srcCoord, receiverCoord, false, inters, true);
            }
            List<Coordinate> rSground = data.freeFieldFinder.getGroundProfile(inters);
            altR = rSground.get(inters.size() - 1).z;    // altitude Receiver
            altS = rSground.get(0).z; // altitude Source
            double angle = new LineSegment(rSground.get(0), rSground.get(rSground.size() - 1)).angle();
            rSground = JTSUtility.getNewCoordinateSystem(rSground);

            // Compute mean ground plan
            double[] ab = JTSUtility.getLinearRegressionPolyline(removeDuplicates(rSground));
            Coordinate rotatedReceiver = new Coordinate(rSground.get(rSground.size() - 1));
            rotatedReceiver.setOrdinate(1, receiverCoord.z);
            Coordinate rotatedSource = new Coordinate(rSground.get(0));
            rotatedSource.setOrdinate(1, srcCoord.z);
            projReceiver = JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedReceiver);
            projSource = JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedSource);

            //projReceiver.x = rSground.get(rSground.size() - 1).x - projReceiver.x ;
            //projSource.x = rSground.get(rSground.size() - 1).x - projSource.x ;

            projReceiver = JTSUtility.getOldCoordinateSystem(projReceiver, angle);
            projSource = JTSUtility.getOldCoordinateSystem(projSource, angle);

            projReceiver.x = srcCoord.x + projReceiver.x;
            projSource.x = srcCoord.x + projSource.x;
            projReceiver.y = srcCoord.y + projReceiver.y;
            projSource.y = srcCoord.y + projSource.y;

            List<Coordinate> Test = new ArrayList<Coordinate>();
            Test.add(projSource);
            Test.add(projReceiver);

            JTSUtility.getLinearRegressionPolyline(removeDuplicates(JTSUtility.getNewCoordinateSystem(Test)));
            //double zr = rotatedReceiver.distance(JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedReceiver));
            //double zs = rotatedSource.distance(JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedSource));

            segments.add(new PropagationPath.SegmentPath(gPath, new Vector3D(projSource, projReceiver)));

        } else {
            segments.add(new PropagationPath.SegmentPath(0.0, new Vector3D(srcCoord, receiverCoord)));
        }
        srPath.add(segments.get(0));
        points.add(new PropagationPath.PointPath(srcCoord, altS, gS, Double.NaN, -1, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(receiverCoord, altR, gS, Double.NaN, -1, PropagationPath.PointPath.POINT_TYPE.RECV));

        PropagationDebugInfo propagationDebugInfo = null;
        if (debugInfo != null) {
            propagationDebugInfo = new PropagationDebugInfo(Arrays.asList(receiverCoord, srcCoord), new double[data.freq_lvl.size()]);
        }


        return new PropagationPath(false, points, segments, segments);

    }


    public PropagationPath computeHorizontalEdgeDiffraction(boolean obstructedSourceReceiver, Coordinate receiverCoord,
                                                            Coordinate srcCoord, List<PropagationDebugInfo> debugInfo) {

        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        boolean validDiffraction;

        DiffractionWithSoilEffetZone diffDataWithSoilEffet;


        if (!obstructedSourceReceiver) {
            diffDataWithSoilEffet = data.freeFieldFinder.getPathInverse(receiverCoord, srcCoord);
            validDiffraction = false;
        } else {
            diffDataWithSoilEffet = data.freeFieldFinder.getPath(receiverCoord, srcCoord);
            // Offset Coordinates by epsilon
            validDiffraction = true;
        }
        // todo not sure about this part...
        if (validDiffraction) {
                List<Coordinate> offsetPath = new ArrayList<>(diffDataWithSoilEffet.getPath());
                for(int i = 1; i < offsetPath.size() - 1; i++) {
                    Coordinate dest = offsetPath.get(i);
                    Vector2D v = new Vector2D(offsetPath.get(0), dest).normalize().multiply(FastObstructionTest.epsilon);
                    offsetPath.set(i, new Coordinate(dest.x - v.getX(), dest.y - v.getY(), dest.z));
                }
                for (int j = offsetPath.size() - 1; j > 1; j--) {
                    PropagationPath propagationPath1 = computeFreefield(offsetPath.get(j - 1), offsetPath.get(j), null, debugInfo);
                    propagationPath1.getPointList().get(1).setType(PropagationPath.PointPath.POINT_TYPE.DIFH);
                    if (j == offsetPath.size() - 1) {
                        propagationPath1.getPointList().get(0).setCoordinate(offsetPath.get(j));
                        points.add(propagationPath1.getPointList().get(0));
                    }
                    points.add(propagationPath1.getPointList().get(1));
                    segments.addAll(propagationPath1.getSegmentList());
                }

                PropagationPath propagationPath2 = computeFreefield(offsetPath.get(0), offsetPath.get(1),null, debugInfo);
                points.add(propagationPath2.getPointList().get(1));
                segments.add(propagationPath2.getSegmentList().get(0));

        } else {
            PropagationPath propagationPath = computeFreefield(receiverCoord, srcCoord,null, debugInfo);
            points.addAll(propagationPath.getPointList());
            segments.addAll(propagationPath.getSegmentList());
            srPath.addAll(propagationPath.getSRList());
        }

        return new PropagationPath(true, points, segments, srPath);
    }

    public HashSet<Integer> getBuildingsOnPath(Coordinate p1, Coordinate p2) {
        HashSet<Integer> buildingsOnPath = new HashSet<>();
        List<TriIdWithIntersection> propagationPath = new ArrayList<>();
        data.freeFieldFinder.computePropagationPath(p1, p2, false, propagationPath, true);
        if (!propagationPath.isEmpty()) {
            for (TriIdWithIntersection inter : propagationPath) {
                if (inter.getBuildingId() != 0) {
                    buildingsOnPath.add(inter.getBuildingId());
                }
            }
        }
        return buildingsOnPath;
    }


    public static Plane ComputeZeroRadPlane(Coordinate p0, Coordinate p1) {
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D s = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p0.x, p0.y, p0.z);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D r = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p1.x, p1.y, p1.z);
        double angle = Math.atan2(p1.y - p0.y, p1.x - p0.x);
        // Compute rPrime, the third point of the plane that is at -PI/2 with SR vector
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D rPrime = s.add(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(Math.cos(angle - Math.PI / 2),Math.sin(angle - Math.PI / 2),0));
        Plane p = new Plane(r, s, rPrime, 1e-6);
        // Normal of the cut plane should be upward
        if(p.getNormal().getZ() < 0) {
            p.revertSelf();
        }
        return p;
    }


//    public org.apache.commons.math3.geometry.euclidean.threed.Vector3D transform(Plane plane, Coordinate p) {
//        org.apache.commons.math3.geometry.euclidean.twod.Vector2D sp = plane.toSubSpace(p);
//        return new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(sp.getX(), sp.getY(), plane.getOffset(p));
//    }

    public static org.apache.commons.math3.geometry.euclidean.threed.Vector3D CoordinateToVector(Coordinate p) {
        return new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p.x, p.y, p.z);
    }

    public static List<Coordinate> cutRoofPointsWithPlane(Plane plane, List<Coordinate> roofPts) {
        List<Coordinate> polyCut = new ArrayList<>(roofPts.size());
        Double lastOffset = null;
        for(int idp = 0; idp < roofPts.size(); idp++) {
            double offset = plane.getOffset(CoordinateToVector(roofPts.get(idp)));
            if(lastOffset != null && ((offset >= 0 && lastOffset < 0) || (offset < 0 && lastOffset >= 0))) {
                // Interpolate vector
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(CoordinateToVector(roofPts.get(idp - 1)),CoordinateToVector(roofPts.get(idp)),FastObstructionTest.epsilon));
                polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            if(offset >= 0) {
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(roofPts.get(idp).x,roofPts.get(idp).y,Double.MIN_VALUE),CoordinateToVector(roofPts.get(idp)),FastObstructionTest.epsilon));
                polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            lastOffset = offset;
        }
        return polyCut;
    }
    /**
     *
     * @param left If true return path between p1 and p2; else p2 to p1
     * @param p1 First point
     * @param p2 Second point
     * @return
     */
    public List<Coordinate> computeSideHull(boolean left, Coordinate p1, Coordinate p2) {
        if(p1.equals(p2)) {
            return new ArrayList<>();
        }

        Plane cutPlane = ComputeZeroRadPlane(p1, p2);

        final LineSegment receiverSrc = new LineSegment(p1, p2);
        // Intersection test cache
        Set<LineSegment> freeFieldSegments = new HashSet<>();
        GeometryFactory geometryFactory = new GeometryFactory();

        List<Coordinate> input = new ArrayList<>();

        Coordinate[] coordinates = new Coordinate[0];
        int indexp1 = 0;
        int indexp2 = 0;

        boolean convexHullIntersects = true;
        HashSet<Integer> buildingsOnPath = new HashSet<>();

        input.add(p1);
        input.add(p2);

        for(int i : getBuildingsOnPath(p1, p2)) {
            if(!buildingsOnPath.contains(i)) {
                List<Coordinate> roofPoints = data.freeFieldFinder.getWideAnglePointsByBuilding(i, 0, 2 * Math.PI);
                roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
                if(!roofPoints.isEmpty()) {
                    input.addAll(roofPoints.subList(0, roofPoints.size() - 1));
                    buildingsOnPath.add(i);
                }
            }
        }
        int k;
        while (convexHullIntersects) {
            ConvexHull convexHull = new ConvexHull(input.toArray(new Coordinate[input.size()]), geometryFactory);
            Geometry convexhull = convexHull.getConvexHull();

            if (convexhull.getLength() / p1.distance(p2) > MAX_RATIO_HULL_DIRECT_PATH) {
                return new ArrayList<>();
            }

            convexHullIntersects = false;
            coordinates = convexhull.getCoordinates();

            indexp1 = -1;
            for (int i = 0; i < coordinates.length - 1; i++) {
                if (coordinates[i].equals(p1)) {
                    indexp1 = i;
                    break;
                }
            }
            if (indexp1 == -1) {
                // P1 does not belong to convex vertices, cannot compute diffraction
                // TODO handle concave path
                return new ArrayList<>();
            }
            // Transform array to set p1 at index=0
            Coordinate[] coordinatesShifted = new Coordinate[coordinates.length];
            // Copy from P1 to end in beginning of new array
            int len = (coordinates.length - 1) - indexp1;
            System.arraycopy(coordinates, indexp1, coordinatesShifted, 0, len);
            // Copy from 0 to P1 in the end of array
            System.arraycopy(coordinates, 0, coordinatesShifted, len, coordinates.length - len - 1);
            coordinatesShifted[coordinatesShifted.length - 1] = coordinatesShifted[0];
            coordinates = coordinatesShifted;
            indexp1 = 0;
            indexp2 = -1;
            for (int i = 1; i < coordinates.length - 1; i++) {
                if (coordinates[i].equals(p2)) {
                    indexp2 = i;
                    break;
                }
            }
            if (indexp2 == -1) {
                // P2 does not belong to convex vertices, cannot compute diffraction
                // TODO handle concave path
                return new ArrayList<>();
            }
            for (k = 0; k < coordinates.length - 1; k++) {
                LineSegment freeFieldTestSegment = new LineSegment(coordinates[k], coordinates[k + 1]);
                // Ignore intersection if iterating over other side (not parts of what is returned)
                if (left && k < indexp2 || !left && k >= indexp2) {
                    if (!freeFieldSegments.contains(freeFieldTestSegment)) {
                        HashSet<Integer> buildingsOnPath2 = getBuildingsOnPath(coordinates[k], coordinates[k + 1]);
                        for (int i : buildingsOnPath2) {
                            if (!buildingsOnPath.contains(i)) {
                                List<Coordinate> roofPoints = data.freeFieldFinder.getWideAnglePointsByBuilding(i, 0, 2 * Math.PI);
                                roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
                                if (!roofPoints.isEmpty()) {
                                    convexHullIntersects = true;
                                    input.addAll(roofPoints.subList(0, roofPoints.size() - 1));
                                }
                                buildingsOnPath.add(i);
                            }
                        }
                        if (!convexHullIntersects) {
                            freeFieldSegments.add(freeFieldTestSegment);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        if(left) {
            return Arrays.asList(Arrays.copyOfRange(coordinates,indexp1, indexp2 + 1));
        } else {
            ArrayList<Coordinate> inversePath = new ArrayList<>();
            inversePath.addAll(Arrays.asList(Arrays.copyOfRange(coordinates,indexp2, coordinates.length)));
            Collections.reverse(inversePath);
            return inversePath;
        }
    }

    public List<List<Coordinate>> computeVerticalEdgeDiffraction(Coordinate p1,
                                                                 Coordinate p2, List<PropagationDebugInfo> debugInfo) {
        List<List<Coordinate>> paths = new ArrayList<>();
        List<Coordinate> p1leftp2 = computeSideHull(true, p1, p2);
        if(!p1leftp2.isEmpty()) {
            paths.add(p1leftp2);
        }
        List<Coordinate> p1rightp2 = computeSideHull(false, p1, p2);
        if(!p1rightp2.isEmpty()) {
            paths.add(p1rightp2);
        }
        return paths;
    }

    /**
     * Compute project Z coordinate between p0 p1 of x,y.
     *
     * @param coordinateWithoutZ coordinate to set the Z value from Z interpolation of line
     * @param line               Extract Z values of this segment
     * @return coordinateWithoutZ with Z value computed from line.
     */
    private static Coordinate getProjectedZCoordinate(Coordinate coordinateWithoutZ, LineSegment line) {
        // Z value is the interpolation of source-receiver line
        return new Coordinate(coordinateWithoutZ.x, coordinateWithoutZ.y, Vertex.interpolateZ(
                line.closestPoint(coordinateWithoutZ), line.p0, line.p1));
    }

    /**
     * ISO-9613 p1
     *
     * @param frequency   acoustic frequency (Hz)
     * @param temperature Temperative in celsius
     * @param pressure    atmospheric pressure (in Pa)
     * @param humidity    relative humidity (in %) (0-100)
     * @return Attenuation coefficient dB/KM
     */
    public static double getAlpha(double frequency, double temperature, double pressure, double humidity) {
        return PropagationProcessData.getCoefAttAtmos(frequency, humidity, pressure, temperature + PropagationProcessData.K_0);
    }

    private int nextFreeFieldNode(List<Coordinate> nodes, Coordinate startPt, LineSegment segmentConstraint,
                                  List<Integer> NodeExceptions, int firstTestNode,
                                  FastObstructionTest freeFieldFinder) {
        int validNode = firstTestNode;
        while (NodeExceptions.contains(validNode)
                || (validNode < nodes.size() && (Math.abs(segmentConstraint.projectionFactor(nodes.get(validNode))) > 1 || !freeFieldFinder.isFreeField(
                startPt, getProjectedZCoordinate(nodes.get(validNode), segmentConstraint))))) {
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

    private boolean[] findBuildingOnPath(Coordinate srcCoord,
                                         Coordinate receiverCoord, boolean vertivalDiffraction) {

        boolean somethingHideReceiver = false;
        boolean buildingOnPath = false;
        boolean[] somethingOnPath = new boolean[2];
        if (!vertivalDiffraction || !data.freeFieldFinder.isHasBuildingWithHeight()) {
            somethingHideReceiver = !data.freeFieldFinder.isFreeField(receiverCoord, srcCoord);
        } else {
            List<TriIdWithIntersection> propagationPath = new ArrayList<>();
            if (!data.freeFieldFinder.computePropagationPath(receiverCoord, srcCoord, false, propagationPath, false)) {
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
        somethingOnPath[0] = somethingHideReceiver;
        somethingOnPath[1] = buildingOnPath;
        return somethingOnPath;
    }

    List<PropagationPath> directPath(Coordinate srcCoord,
                                             Coordinate receiverCoord, boolean verticalDiffraction,boolean horizontalDiffraction,  List<PropagationDebugInfo> debugInfo) {


        List<PropagationPath> propagationPaths = new ArrayList<>();

        // Then, check if the source is visible from the receiver (not
        // hidden by a building)
        // Create the direct Line

        boolean freefield = true;
        boolean somethingHideReceiver = false;
        boolean buildingOnPath = false;

        List<TriIdWithIntersection> inters = new ArrayList<>();
        data.freeFieldFinder.computePropagationPath(srcCoord, receiverCoord, false, inters, true);
        for(TriIdWithIntersection intersection : inters) {
            if(intersection.getBuildingId() > 0) {
                buildingOnPath = true;
            }
            if(intersection.isIntersectionOnBuilding() || intersection.isIntersectionOnTopography()) {
                freefield = false;
            }
        }

        // double fav_probability = favrose[(int) (Math.round(calcRotationAngleInDegrees(srcCoord, receiverCoord) / 30))];

        if (!somethingHideReceiver && !buildingOnPath) {
            PropagationPath propagationPath = computeFreefield(receiverCoord, srcCoord,inters, debugInfo);
            propagationPaths.add(propagationPath);
        }

        //Process diffraction 3D
        // todo include rayleigh criterium
        if (verticalDiffraction && buildingOnPath && !freefield) {
            PropagationPath propagationPath3 = computeFreefield(receiverCoord, srcCoord, inters, debugInfo);
            PropagationPath propagationPath = computeHorizontalEdgeDiffraction(somethingHideReceiver, receiverCoord, srcCoord, debugInfo);
            propagationPath.getSRList().addAll(propagationPath3.getSRList());
            propagationPaths.add(propagationPath);


        }

        if (somethingHideReceiver && data.diffractionOrder > 0 && horizontalDiffraction ) {
            // todo if one of the points > roof or < floor, get out this path
            PropagationPath propagationPath = new PropagationPath();
            PropagationPath propagationPath2 = new PropagationPath();

            // Left hand

            //List<List<Coordinate>> diffractedPaths = computeVerticalEdgeDiffraction(srcCoord, receiverCoord, debugInfo);
            List<Coordinate> coordinates = computeSideHull(true, srcCoord, receiverCoord);
            if(!coordinates.isEmpty()) {

                if (coordinates.size() > 2 && coordinates.size() <= data.diffractionOrder + 2) {

                    // if (coordinates.size()>2 && coordinates.size() <= data.diffractionOrder+2 && convexhullValid) {

                    propagationPath = computeFreefield(coordinates.get(1), coordinates.get(0),null, debugInfo);
                    propagationPath.getPointList().get(1).setType(PropagationPath.PointPath.POINT_TYPE.DIFV);

                    propagationPath2 = propagationPath;
                    int j;
                    for (j = 1; j < coordinates.size() - 2; j++) {
                        propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j),null, debugInfo);
                        propagationPath.getPointList().get(1).setType(PropagationPath.PointPath.POINT_TYPE.DIFV);
                        propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                        propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());
                    }
                    propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j),null, debugInfo);
                    propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                    propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());
                    propagationPaths.add(propagationPath2);
                }
            }

            // Right hand
            coordinates = computeSideHull(false, srcCoord, receiverCoord);
            if(!coordinates.isEmpty()) {
                if (coordinates.size() > 2 && coordinates.size() <= data.diffractionOrder + 2) {
                    Collections.reverse(coordinates);
                    propagationPath = computeFreefield(coordinates.get(1), coordinates.get(0),null, debugInfo);
                    propagationPath.getPointList().get(1).setType(PropagationPath.PointPath.POINT_TYPE.DIFV);
                    propagationPath2 = propagationPath;
                    int j;
                    for (j = 1; j < coordinates.size() - 2; j++) {
                        propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j),null, debugInfo);
                        propagationPath.getPointList().get(1).setType(PropagationPath.PointPath.POINT_TYPE.DIFV);
                        propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                        propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());
                    }
                    propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j),null, debugInfo);
                    propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                    propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());
                    propagationPaths.add(propagationPath2);

                }
            }
        }
        return propagationPaths;
    }

    /**
     * Source-Receiver Direct+Reflection+Diffraction computation
     *
     * @param[in] srcCoord coordinate of source
     * @param[in] receiverCoord coordinate of receiver
     * @param[out] energeticSum Energy by frequency band
     * @param[in] alpha_atmo Atmospheric absorption by frequency band
     * @param[in] wj Source sound pressure level dB(A) by frequency band
     * @param[in] nearBuildingsWalls Walls within maxsrcdist
     * from receiver
     */

    private void receiverSourcePropa(Coordinate srcCoord, int srcId,
                                     Coordinate receiverCoord, int rcvId,
                                     List<Wall> nearBuildingsWalls, List<PropagationDebugInfo> debugInfo) {

        GeometryFactory factory = new GeometryFactory();
        List<PropagationPath> propagationPaths = new ArrayList<>();
        // Build mirrored receiver list from wall list

        double PropaDistance = srcCoord.distance(receiverCoord);
        if (PropaDistance < data.maxSrcDist) {

            // Process direct path (including horizontal and vertical diffractions)
            propagationPaths = directPath(srcCoord, receiverCoord, data.computeVerticalDiffraction, true, debugInfo);

            // Process specular reflection
            if (data.reflexionOrder > 0) {
                List<PropagationPath> propagationPaths_all = computeReflexion(receiverCoord, srcCoord, false, nearBuildingsWalls, debugInfo);
                propagationPaths.addAll(propagationPaths_all);
            }

            if (propagationPaths.size() > 0) {
                this.dataOut.setSrcIdAndAddPropagationPaths(propagationPaths, srcId, rcvId);
            }
        }
    }

    private static void insertPtSource(Coordinate receiverPos, Coordinate ptpos, List<Double> wj, double li, List<Coordinate> srcPos, List<ArrayList<Double>> srcWj, PointsMerge sourcesMerger, List<Integer> srcSortedIndex, List<Integer> indexedSource, Integer sourceId, List<Double> srcDistSorted) {
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

            double dx = ptpos.x - receiverPos.x;
            double dy = ptpos.y - receiverPos.y;
            double dz = ptpos.z - receiverPos.z;

            double distanceSrcPt = Math.sqrt(dx * dx + dy * dy + dz * dz);// TODO i have change like line 384

            int index = Collections.binarySearch(srcDistSorted, distanceSrcPt);
            if (index >= 0) {
                srcSortedIndex.add(index, mergedSrcIndex);
                srcDistSorted.add(index, distanceSrcPt);
                indexedSource.add(index, sourceId);
            } else {
                srcSortedIndex.add(-index - 1, mergedSrcIndex);
                srcDistSorted.add(-index - 1, distanceSrcPt);
                indexedSource.add(-index - 1, sourceId);
            }
        }
    }


    /**
     * Use {@link #cornersQuad} to fetch all region corners in max ref dist
     *
     * @param fetchLine   Compute distance from this line (Propagation line)
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
    public void computeRaysAtPosition(Coordinate receiverCoord, int idReceiver, double energeticSum[], List<PropagationDebugInfo> debugInfo) {
        // List of walls within maxReceiverSource distance
        double srcEnergeticSum = BASE_LVL; //Global energetic sum of all sources processed
        STRtree walls = new STRtree();
        if (data.reflexionOrder > 0) {
            for (Wall wall : data.freeFieldFinder.getLimitsInRange(
                    data.maxSrcDist, receiverCoord, false)) {
                walls.insert(new Envelope(wall.p0, wall.p1), wall);
            }
        }

        // Source search by multiple range query
        HashSet<Integer> processedLineSources = new HashSet<Integer>(); //Already processed Raw source (line and/or points)
        double[] ranges = new double[]{Math.min(FIRST_STEP_RANGE, data.maxSrcDist / 6), data.maxSrcDist / 5, data.maxSrcDist / 4, data.maxSrcDist / 2, data.maxSrcDist};
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
            List<Integer> indexedSrc = new ArrayList<>();
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
                        insertPtSource(receiverCoord, ptpos, wj, 1., srcPos, srcWj, sourcesMerger, srcSortByDist, indexedSrc, srcIndex, srcDist);

                        // Compute li to equation 4.1 NMPB 2008 (June 2009)
                    } else {
                        // Discretization of line into multiple point
                        // First point is the closest point of the LineString from
                        // the receiver
                        ArrayList<Coordinate> pts = new ArrayList<Coordinate>();
                        double li = splitLineStringIntoPoints(source, receiverCoord,
                                pts, data.minRecDist);
                        for (Coordinate pt : pts) {
                            insertPtSource(receiverCoord, pt, wj, li, srcPos, srcWj, sourcesMerger, srcSortByDist, indexedSrc, srcIndex, srcDist);
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
                if (Math.abs(wToDba(wAttDistSource + allreceiverfreqlvl) - wToDba(allreceiverfreqlvl)) > data.forgetSource) {
                    sourceCount++;
                    Envelope query = new Envelope(receiverCoord, srcCoord);
                    query.expandBy(Math.min(data.maxRefDist, srcCoord.distance(receiverCoord)));
                    List queryResult = walls.query(query);
                    receiverSourcePropa(srcCoord, indexedSrc.get(mergedSrcId), receiverCoord, idReceiver,
                            (List<Wall>) queryResult, debugInfo);
                }

            }
            //srcEnergeticSum=GetGlobalLevel(nbfreq,energeticSum);
            if (Math.abs(wToDba(attDistW(W_RANGE, searchSourceDistance) + srcEnergeticSum) - wToDba(srcEnergeticSum)) < data.forgetSource) {
                break; //Stop search for furthest sources
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
                freq_lambda[idf] = data.celerity / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }
        // Compute atmospheric alpha value by specified frequency band
        alpha_atmo = new double[data.freq_lvl.size()];
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            alpha_atmo[idfreq] = getAlpha(data.freq_lvl.get(idfreq), data.temperature, data.pressure, data.humidity);
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
            //dataOut.setVerticesSoundLevel(new double[data.receivers.size()]);

            // For each vertices, find sources where the distance is within
            // maxSrcDist meters
            ProgressVisitor propaProcessProgression = data.cellProg;

            // todo check if db_d as values inside table if false then send error
            Runtime runtime = Runtime.getRuntime();
            int splitCount = runtime.availableProcessors();
            ThreadPool threadManager = new ThreadPool(
                    splitCount,
                    splitCount + 1, Long.MAX_VALUE,
                    TimeUnit.SECONDS);
            int maximumReceiverBatch = (int) Math.ceil(data.receivers.size() / (double) splitCount);
            int endReceiverRange = 0;
            while (endReceiverRange < data.receivers.size()) {
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

    private class RangeReceiversComputation implements Runnable {
        private final int startReceiver; // Included
        private final int endReceiver; // Excluded
        private ComputeRays propagationProcess;
        private List<PropagationDebugInfo> debugInfo;
        private ProgressVisitor progressVisitor;

        private RangeReceiversComputation(int startReceiver, int endReceiver, ComputeRays propagationProcess, ProgressVisitor progressVisitor, List<PropagationDebugInfo> debugInfo) {
            this.startReceiver = startReceiver;
            this.endReceiver = endReceiver;
            this.propagationProcess = propagationProcess;
            this.debugInfo = debugInfo;
            this.progressVisitor = progressVisitor;
        }

        private class EnergeticSourceComparator implements Comparator<PropagationProcess_Att_f.energeticSource> {
            public int compare(PropagationProcess_Att_f.energeticSource a, PropagationProcess_Att_f.energeticSource b) {
                return a.sourceId - b.sourceId;
            }
        }

        @Override
        public void run() {
            List<PropagationProcessOut_Att_f.verticeSL> VerticeSoundLevel = new ArrayList<>();

            for (int idReceiver = startReceiver; idReceiver < endReceiver; idReceiver++) {
                HashMap<Integer, double[]> aGlobal = new HashMap<>();
                Coordinate receiverCoord = propagationProcess.data.receivers.get(idReceiver);
                double energeticSum[] = new double[propagationProcess.data.freq_lvl.size()];
                ArrayList<PropagationProcess_Att_f.energeticSource> energeticId = new ArrayList<>();
                Arrays.fill(energeticSum, 0d);

                propagationProcess.initStructures();
                propagationProcess.computeRaysAtPosition(receiverCoord, idReceiver, energeticSum, debugInfo);

                //  long startTime = System.nanoTime();
                double p = 0.5; // probability favourable conditions
                PropagationProcessPathData propData = new PropagationProcessPathData();
                propData.setTemperature(10);
                propData.setHumidity(70);
                propData.setPrime2520(true);

                computeWithMeteo(aGlobal, propData, propagationProcess.dataOut, p);

                Iterator it = aGlobal.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    propagationProcess.dataOut.addVerticeSoundLevel(idReceiver, (Integer) pair.getKey(), (double[]) pair.getValue());
                    it.remove(); // avoids a ConcurrentModificationException
                }


                // energeticId.sort(new ComputeRays.RangeReceiversComputation.EnergeticSourceComparator());


                /*// ICI CEST POUR SOMMER LES PT SOURCES DUNE LIGNE SOURCE
                int s_Id_t0 = 0;
                int s_Id_t1 = 0;
                int max = -1;
                int len = energeticId.size();
                for (int i = 0; i < len; i++) {
                    if (energeticId.get(i).sourceId > max) {
                        max = energeticId.get(i).sourceId;
                    }
                }


                for (PropagationProcess_Att_f.energeticSource source : energeticId) {
                    s_Id_t1 = source.sourceId;


                    // SOMME SUR LES POINTS SOURCES DE LA LIGNE OU SUR UN POINT SOURCE
                    allfreqs[source.sourceId][0] = allfreqs[source.sourceId][0]+ source.freqs[0]+source.freqs[1]+source.freqs[2]+source.freqs[3]+source.freqs[4]+source.freqs[5]+source.freqs[6]+source.freqs[7];
                    allfreqs[source.sourceId][1] = allfreqs[source.sourceId][1] + source.freqs[0];
                    allfreqs[source.sourceId][2] = allfreqs[source.sourceId][2]+ source.freqs[1];
                    allfreqs[source.sourceId][3] = allfreqs[source.sourceId][3] + source.freqs[2];
                    allfreqs[source.sourceId][4] = allfreqs[source.sourceId][4]+ source.freqs[3];
                    allfreqs[source.sourceId][5] = allfreqs[source.sourceId][5] + source.freqs[4];
                    allfreqs[source.sourceId][6] = allfreqs[source.sourceId][6]+ source.freqs[5];
                    allfreqs[source.sourceId][7] = allfreqs[source.sourceId][7] + source.freqs[6];
                    allfreqs[source.sourceId][8] = allfreqs[source.sourceId][8] + source.freqs[7];

                    s_Id_t0 = source.sourceId;


                }

                for(int i=0; i<=max; i++) {
                    //
                    allfreqs[i][0] =  wToDba(allfreqs[i][0]) - wToDba(propagationProcess.data.wj_sources.get(i).get(0) + propagationProcess.data.wj_sources.get(i).get(1) + propagationProcess.data.wj_sources.get(i).get(2) + propagationProcess.data.wj_sources.get(i).get(3) + propagationProcess.data.wj_sources.get(i).get(4) + propagationProcess.data.wj_sources.get(i).get(5) + propagationProcess.data.wj_sources.get(i).get(6) + propagationProcess.data.wj_sources.get(i).get(7));
                    allfreqs[i][1] =  wToDba(allfreqs[i][1]) - wToDba(propagationProcess.data.wj_sources.get(i).get(0));
                    allfreqs[i][2] =  wToDba(allfreqs[i][2]) - wToDba(propagationProcess.data.wj_sources.get(i).get(1));
                    allfreqs[i][3] =  wToDba(allfreqs[i][3]) - wToDba(propagationProcess.data.wj_sources.get(i).get(2));
                    allfreqs[i][4] =  wToDba(allfreqs[i][4]) - wToDba(propagationProcess.data.wj_sources.get(i).get(3));
                    allfreqs[i][5] =  wToDba(allfreqs[i][5]) - wToDba(propagationProcess.data.wj_sources.get(i).get(4));
                    allfreqs[i][6] =  wToDba(allfreqs[i][6]) - wToDba(propagationProcess.data.wj_sources.get(i).get(5));
                    allfreqs[i][7] =  wToDba(allfreqs[i][7]) - wToDba(propagationProcess.data.wj_sources.get(i).get(6));
                    allfreqs[i][8] =  wToDba(allfreqs[i][8]) - wToDba(propagationProcess.data.wj_sources.get(i).get(7));
                    if (allfreqs[i][0] > -90){
                        propagationProcess.dataOut.addVerticeSoundLevel(idReceiver, i, allfreqs[i]);
                    }
                }*/
                progressVisitor.endStep();
            }
        }
    }

    /**
     * Offset de Z coordinates by the height of the ground
     */
    public static final class AbsoluteCoordinateSequenceFilter implements CoordinateSequenceFilter {
        AtomicBoolean geometryChanged = new AtomicBoolean(false);
        FastObstructionTest fastObstructionTest;
        boolean resetZ;

        /**
         * Constructor
         *
         * @param fastObstructionTest Initialised instance of fastObstructionTest
         * @param resetZ              If filtered geometry contain Z and resetZ is false, do not update Z.
         */
        public AbsoluteCoordinateSequenceFilter(FastObstructionTest fastObstructionTest, boolean resetZ) {
            this.fastObstructionTest = fastObstructionTest;
            this.resetZ = resetZ;
        }

        @Override
        public void filter(CoordinateSequence coordinateSequence, int i) {
            Coordinate pt = coordinateSequence.getCoordinate(i);
            Double zGround = fastObstructionTest.getHeightAtPosition(pt);
            if (!zGround.isNaN() && (resetZ || Double.isNaN(pt.getOrdinate(2)) || Double.compare(0, pt.getOrdinate(2)) == 0)) {
                pt.setOrdinate(2, zGround + (Double.isNaN(pt.getOrdinate(2)) ? 0 : pt.getOrdinate(2)));
                geometryChanged.set(true);
            }
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean isGeometryChanged() {
            return geometryChanged.get();
        }
    }
}
