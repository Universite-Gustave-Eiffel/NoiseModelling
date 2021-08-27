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
package org.noise_planet.noisemodelling.pathfinder;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.locationtech.jts.algorithm.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.noise_planet.noisemodelling.pathfinder.utils.ProfilerThread;
import org.noise_planet.noisemodelling.pathfinder.utils.ReceiverStatsMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticPropagation.getADiv;


/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeRays {
    // Reject side diffraction if hull length > than direct length
    // because 20 * LOG10(4) = 12 dB, so small contribution in comparison with diffraction on horizontal edge
    // in order to reduce computational cost
    private final static double MAX_RATIO_HULL_DIRECT_PATH = 4;
    private int threadCount;
    private PropagationProcessData data;
    private ProfilerThread profilerThread;

    private STRtree rTreeOfGeoSoil;
    private final static Logger LOGGER = LoggerFactory.getLogger(ComputeRays.class);

    /**
     * Eq 2.5.9
     * The ‘long-term’ sound level along a path starting from a given point source is
     * obtained from the logarithmic sum of the weighted sound energy
     * in homogeneous conditions and the sound energy in favourable conditions.
     * @param array1
     * @param array2
     * @param p the mean occurrence p of favourable conditions in the direction of the path (S,R)
     * @return
     */
    public static double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(p * dbaToW(array1[i]) + (1 - p) * dbaToW(array2[i]));
        }
        return sum;
    }

    /**
     * energetic Sum of dBA array
     *
     * @param array1
     * @param array2
     * @return
     */
    public static double[] sumDbArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(dbaToW(array1[i]) + dbaToW(array2[i]));
        }
        return sum;
    }

    /**
     * Multiply component of two same size array
     *
     * @param array1
     * @param array2
     * @return
     */
    public static double[] multArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = array1[i] * array2[i];
        }
        return sum;
    }

    public static double sumArray(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    public static double sumArray(double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < energeticSum.length; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    /**
     * Element wise sum array without any other operations.
     *
     * @param array1 First array
     * @param array2 Second array
     * @return Sum of the two arrays
     */
    public static double[] sumArray(double array1[], double array2[]) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Arrays with different size");
        }
        double[] ret = new double[array1.length];
        for (int idfreq = 0; idfreq < array1.length; idfreq++) {
            ret[idfreq] = array1[idfreq] + array2[idfreq];
        }
        return ret;
    }

    public ComputeRays(PropagationProcessData data) {
        this.data = data;
        Runtime runtime = Runtime.getRuntime();
        this.threadCount = runtime.availableProcessors();
    }

    /**
     * Computation stacks and timing are collected by this class in order
     * to profile the execution of the simulation
     * @return Instance of ProfilerThread or null
     */
    public ProfilerThread getProfilerThread() {
        return profilerThread;
    }

    /**
     * Computation stacks and timing are collected by this class in order
     * to profile the execution of the simulation
     * @param profilerThread Instance of ProfilerThread
     */
    public void setProfilerThread(ProfilerThread profilerThread) {
        this.profilerThread = profilerThread;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Update ground Z coordinates of sound sources absolute to sea levels
     */
    public void makeSourceRelativeZToAbsolute() {
        AbsoluteCoordinateSequenceFilter filter = new AbsoluteCoordinateSequenceFilter(data.freeFieldFinder, true);
        List<Geometry> sourceCopy = new ArrayList<>(data.sourceGeometries.size());
        for (Geometry source : data.sourceGeometries) {
            filter.reset();
            Geometry cpy = source.copy();
            cpy.apply(filter);
            sourceCopy.add(cpy);
        }
        data.sourceGeometries = sourceCopy;
    }

    /**
     * Update ground Z coordinates of sound sources and receivers absolute to sea levels
     */
    public void makeRelativeZToAbsolute() {
        makeSourceRelativeZToAbsolute();
        makeReceiverRelativeZToAbsolute();
    }

    /**
     * Update ground Z coordinates of receivers absolute to sea levels
     */
    public void makeReceiverRelativeZToAbsolute() {
        AbsoluteCoordinateSequenceFilter filter = new AbsoluteCoordinateSequenceFilter(data.freeFieldFinder, true);
        CoordinateSequence sequence = new CoordinateArraySequence(data.receivers.toArray(new Coordinate[data.receivers.size()]));
        for (int i = 0; i < sequence.size(); i++) {
            filter.filter(sequence, i);
        }
        data.receivers = Arrays.asList(sequence.toCoordinateArray());
    }

    public static double dbaToW(double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public static double[] dbaToW(double[] dBA) {
        double[] ret = new double[dBA.length];
        for (int i = 0; i < dBA.length; i++) {
            ret[i] = dbaToW(dBA[i]);
        }
        return ret;
    }

    public static double wToDba(double w) {
        return 10 * Math.log10(w);
    }

    public static double[] wToDba(double[] w) {
        double[] ret = new double[w.length];
        for (int i = 0; i < w.length; i++) {
            ret[i] = wToDba(w[i]);
        }
        return ret;
    }

    /**
     * @param startPt Compute the closest point on lineString with this coordinate,
     *                use it as one of the splitted points
     * @return li coefficient to apply to equivalent source point from the sound power per metre set on linear source
     */
    /**
     * @param geom                  Geometry
     * @param segmentSizeConstraint Maximal distance between points
     * @return Fixed distance between points
     * @param[out] pts computed points
     */
    public static double splitLineStringIntoPoints(LineString geom, double segmentSizeConstraint,
                                                   List<Coordinate> pts) {
        // If the linear sound source length is inferior than half the distance between the nearest point of the sound
        // source and the receiver then it can be modelled as a single point source
        double geomLength = geom.getLength();
        if (geomLength < segmentSizeConstraint) {
            // Return mid point
            Coordinate[] points = geom.getCoordinates();
            double segmentLength = 0;
            final double targetSegmentSize = geomLength / 2.0;
            for (int i = 0; i < points.length - 1; i++) {
                Coordinate a = points[i];
                final Coordinate b = points[i + 1];
                double length = a.distance3D(b);
                if (length + segmentLength > targetSegmentSize) {
                    double segmentLengthFraction = (targetSegmentSize - segmentLength) / length;
                    Coordinate midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                            a.y + segmentLengthFraction * (b.y - a.y),
                            a.z + segmentLengthFraction * (b.z - a.z));
                    pts.add(midPoint);
                    break;
                }
                segmentLength += length;
            }
            return geom.getLength();
        } else {
            double targetSegmentSize = geomLength / Math.ceil(geomLength / segmentSizeConstraint);
            Coordinate[] points = geom.getCoordinates();
            double segmentLength = 0.;

            // Mid point of segmented line source
            Coordinate midPoint = null;
            for (int i = 0; i < points.length - 1; i++) {
                Coordinate a = points[i];
                final Coordinate b = points[i + 1];
                double length = a.distance3D(b);
                if (Double.isNaN(length)) {
                    length = a.distance(b);
                }
                while (length + segmentLength > targetSegmentSize) {
                    //LineSegment segment = new LineSegment(a, b);
                    double segmentLengthFraction = (targetSegmentSize - segmentLength) / length;
                    Coordinate splitPoint = new Coordinate();
                    splitPoint.x = a.x + segmentLengthFraction * (b.x - a.x);
                    splitPoint.y = a.y + segmentLengthFraction * (b.y - a.y);
                    splitPoint.z = a.z + segmentLengthFraction * (b.z - a.z);
                    if (midPoint == null && length + segmentLength > targetSegmentSize / 2) {
                        segmentLengthFraction = (targetSegmentSize / 2.0 - segmentLength) / length;
                        midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                                a.y + segmentLengthFraction * (b.y - a.y),
                                a.z + segmentLengthFraction * (b.z - a.z));
                    }
                    pts.add(midPoint);
                    a = splitPoint;
                    length = a.distance3D(b);
                    if (Double.isNaN(length)) {
                        length = a.distance(b);
                    }
                    segmentLength = 0;
                    midPoint = null;
                }
                if (midPoint == null && length + segmentLength > targetSegmentSize / 2) {
                    double segmentLengthFraction = (targetSegmentSize / 2.0 - segmentLength) / length;
                    midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                            a.y + segmentLengthFraction * (b.y - a.y),
                            a.z + segmentLengthFraction * (b.z - a.z));
                }
                segmentLength += length;
            }
            if (midPoint != null) {
                pts.add(midPoint);
            }
            return targetSegmentSize;
        }
    }

    public void computeReflexionOverBuildings(Coordinate p0, Coordinate p1, List<PointPath> points, List<SegmentPath> segments, List<SegmentPath> srPath) {
        List<PropagationPath> propagationPaths = directPath(p0, p1, data.isComputeVerticalDiffraction(), false);
        if (!propagationPaths.isEmpty()) {
            PropagationPath propagationPath = propagationPaths.get(0);
            points.addAll(propagationPath.getPointList());
            segments.addAll(propagationPath.getSegmentList());
            srPath.add(new SegmentPath(1.0, new Vector3D(p0, p1), p0));
        }
    }

    public static int[] asWallArray(MirrorReceiverResult res) {
        int depth = 0;
        MirrorReceiverResult cursor = res;
        while (cursor != null) {
            depth++;
            cursor = cursor.getParentMirror();
        }
        int[] walls = new int[depth];
        cursor = res;
        int i = 0;
        while (cursor != null) {
            walls[(depth - 1) - (i++)] = cursor.getBuildingId();
            cursor = cursor.getParentMirror();
        }
        return walls;
    }

    public List<PropagationPath> computeReflexion(Coordinate receiverCoord, Coordinate srcCoord, boolean favorable,
                                                  List<FastObstructionTest.Wall> nearBuildingsWalls,
                                                  List<MirrorReceiverResult> receiverReflections) {
        // Compute receiver mirror
        LineIntersector linters = new RobustLineIntersector();

        List<PropagationPath> reflexionPropagationPaths = new ArrayList<>();

        for (MirrorReceiverResult receiverReflection : receiverReflections) {
            // Check propagation distance limitation
            if(receiverReflection.getReceiverPos().distance3D(srcCoord) > data.maxSrcDist) {
                break;
            }
            // Print wall reflections
            //System.out.println(Arrays.toString(asWallArray(receiverReflection)));
            List<MirrorReceiverResult> rayPath = new ArrayList<>(data.reflexionOrder + 2);
            boolean validReflection = false;
            MirrorReceiverResult receiverReflectionCursor = receiverReflection;
            // Test whether intersection point is on the wall
            // segment or not
            Coordinate destinationPt = new Coordinate(srcCoord);

            FastObstructionTest.Wall seg = nearBuildingsWalls.get(receiverReflection.getWallId());
            linters.computeIntersection(seg.p0, seg.p1,
                    receiverReflection.getReceiverPos(),
                    destinationPt);

            // Check first wall distance reflection limitation
            if(linters.hasIntersection() && new Coordinate(
                    linters.getIntersection(0)).distance(srcCoord) > data.maxRefDist) {
                break;
            }
            while (linters.hasIntersection() && MirrorReceiverIterator.wallPointTest(seg, destinationPt)) {
                // There are a probable reflection point on the segment
                Coordinate reflectionPt = new Coordinate(
                        linters.getIntersection(0));
                if (reflectionPt.equals(destinationPt)) {
                    break;
                }
                Coordinate vec_epsilon = new Coordinate(
                        reflectionPt.x - destinationPt.x,
                        reflectionPt.y - destinationPt.y);
                double length = vec_epsilon
                        .distance(new Coordinate(0., 0., 0.));
                // Normalize vector
                vec_epsilon.x /= length;
                vec_epsilon.y /= length;
                // Multiply by epsilon in meter
                vec_epsilon.x *= FastObstructionTest.wideAngleTranslationEpsilon;
                vec_epsilon.y *= FastObstructionTest.wideAngleTranslationEpsilon;
                // Translate reflection pt by epsilon to get outside
                // the wall
                reflectionPt.x -= vec_epsilon.x;
                reflectionPt.y -= vec_epsilon.y;
                // Compute Z interpolation
                reflectionPt.setOrdinate(Coordinate.Z, Vertex.interpolateZ(linters.getIntersection(0),
                        receiverReflectionCursor.getReceiverPos(), destinationPt));

                // Test if there is no obstacles between the
                // reflection point and old reflection pt (or source position)
                validReflection = Double.isNaN(receiverReflectionCursor.getReceiverPos().z) ||
                        Double.isNaN(reflectionPt.z) || Double.isNaN(destinationPt.z) || seg.getBuildingId() == 0
                        || (reflectionPt.z < data.freeFieldFinder.getBuildingRoofZ(seg.getBuildingId())
                        && reflectionPt.z > data.freeFieldFinder.getHeightAtPosition(reflectionPt)
                        && destinationPt.z > data.freeFieldFinder.getHeightAtPosition(destinationPt));
                if (validReflection) // Source point can see receiver image
                {
                    MirrorReceiverResult reflResult = new MirrorReceiverResult(receiverReflectionCursor);
                    reflResult.setReceiverPos(reflectionPt);
                    rayPath.add(reflResult);
                    if (receiverReflectionCursor
                            .getParentMirror() == null) { // Direct to the receiver
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
            if (validReflection && !rayPath.isEmpty()) {
                // Check intermediate reflections
                for (int idPt = 0; idPt < rayPath.size() - 1; idPt++) {
                    Coordinate firstPt = rayPath.get(idPt).getReceiverPos();
                    MirrorReceiverResult refl = rayPath.get(idPt + 1);
                    if (!data.freeFieldFinder.isFreeField(firstPt, refl.getReceiverPos())) {
                        validReflection = false;
                        break;
                    }
                }
                if (!validReflection) {
                    continue;
                }
                // A valid propagation path as been found
                List<PointPath> points = new ArrayList<PointPath>();
                List<SegmentPath> segments = new ArrayList<SegmentPath>();
                List<SegmentPath> srPath = new ArrayList<SegmentPath>();
                // Compute direct path between source and first reflection point, add profile to the data
                computeReflexionOverBuildings(srcCoord, rayPath.get(0).getReceiverPos(), points, segments, srPath);
                if (points.isEmpty()) {
                    continue;
                }
                PointPath reflPoint = points.get(points.size() - 1);
                reflPoint.setType(PointPath.POINT_TYPE.REFL);
                reflPoint.setBuildingId(rayPath.get(0).getBuildingId());
                reflPoint.setAlphaWall(data.freeFieldFinder.getBuildingAlpha(reflPoint.getBuildingId()));
                // Add intermediate reflections
                for (int idPt = 0; idPt < rayPath.size() - 1; idPt++) {
                    Coordinate firstPt = rayPath.get(idPt).getReceiverPos();
                    MirrorReceiverResult refl = rayPath.get(idPt + 1);
                    reflPoint = new PointPath(refl.getReceiverPos(), 0,  data.freeFieldFinder.getBuildingAlpha(refl.getBuildingId()), refl.getBuildingId(), PointPath.POINT_TYPE.REFL);
                    points.add(reflPoint);
                    segments.add(new SegmentPath(1, new Vector3D(firstPt), refl.getReceiverPos()));
                }
                // Compute direct path between receiver and last reflection point, add profile to the data
                List<PointPath> lastPts = new ArrayList<>();
                computeReflexionOverBuildings(rayPath.get(rayPath.size() - 1).getReceiverPos(), receiverCoord, lastPts, segments, srPath);
                if (lastPts.isEmpty()) {
                    continue;
                }
                points.addAll(lastPts.subList(1, lastPts.size()));
                for (int i = 1; i < points.size(); i++) {
                    if (points.get(i).type == PointPath.POINT_TYPE.REFL) {
                        if (i < points.size() - 1) {
                            // A diffraction point may have offset in height the reflection coordinate
                            points.get(i).coordinate.z = Vertex.interpolateZ(points.get(i).coordinate, points.get(i - 1).coordinate, points.get(i + 1).coordinate);
                            //check if in building && if under floor
                            if (points.get(i).coordinate.z > data.freeFieldFinder.getBuildingRoofZ(points.get(i).getBuildingId())
                                    || points.get(i).coordinate.z <= data.freeFieldFinder.getHeightAtPosition(points.get(i).coordinate)) {
                                points.clear();
                                segments.clear();
                                break;
                            }
                        } else {
                            LOGGER.warn("Invalid state, reflexion point on last point");
                            points.clear();
                            segments.clear();
                            break;
                        }
                    }
                }
                if (points.size() > 2) {
                    reflexionPropagationPaths.add(new PropagationPath(favorable, points, segments, srPath));
                }
            }
        }
        return reflexionPropagationPaths;
    }


    private static List<Coordinate> removeDuplicates(List<Coordinate> coordinates) {
        return Arrays.asList(CoordinateUtils.removeDuplicatedCoordinates(
                coordinates.toArray(new Coordinate[coordinates.size()]), false));
    }


    /**
     * @param receiverCoord
     * @param srcCoord
     * @param inters        PropagationPath between srcCoord and receiverCoord (or null if must be computed here)
     */
    public PropagationPath computeFreefield(Coordinate receiverCoord,
                                            Coordinate srcCoord, List<TriIdWithIntersection> inters) {

        GeometryFactory factory = new GeometryFactory();
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();

        double gPath;
        double totRSDistance = 0.;
        double altR = 0;
        double altS = 0;
        Coordinate projReceiver;
        Coordinate projSource;


        //will give a flag here for soil effect
        final List<GeoWithSoilType> soilTypeList = data.getSoilList();
        LineString RSZone = factory.createLineString(new Coordinate[]{receiverCoord, srcCoord});
        List<EnvelopeWithIndex<Integer>> resultZ0 = rTreeOfGeoSoil.query(RSZone.getEnvelopeInternal());
        for (EnvelopeWithIndex<Integer> envel : resultZ0) {
            RectangleLineIntersector rectangleLineIntersector = new RectangleLineIntersector(envel);
            if (rectangleLineIntersector.intersects(receiverCoord, srcCoord)) {
                try {
                    //get the geo intersected
                    Geometry geoInter = RSZone.intersection(soilTypeList.get(envel.getId()).getGeo());
                    //add the intersected distance with ground effect
                    totRSDistance += getIntersectedDistance(geoInter) * soilTypeList.get(envel.getId()).getType();
                } catch (TopologyException | IllegalArgumentException ex) {
                    // Ignore
                }
            }
        }
        // Compute GPath using 2D Length
        gPath = totRSDistance / RSZone.getLength();

        if (inters == null) {
            inters = new ArrayList<>();
            data.freeFieldFinder.computePropagationPath(srcCoord, receiverCoord, false, inters, true);
        }
        List<Coordinate> rSground = data.freeFieldFinder.getGroundProfile(inters);
        altR = rSground.get(inters.size() - 1).z;    // altitude Receiver
        altS = rSground.get(0).z; // altitude Source
        double angle = new LineSegment(rSground.get(0), rSground.get(rSground.size() - 1)).angle();
        rSground = JTSUtility.getNewCoordinateSystem(rSground);

        // Compute mean ground plan
        double[] ab = JTSUtility.getMeanPlaneCoefficients(rSground.toArray(new Coordinate[rSground.size()]));
        Coordinate pInit = new Coordinate();
        Coordinate rotatedReceiver = new Coordinate(rSground.get(rSground.size() - 1));
        rotatedReceiver.setOrdinate(1, receiverCoord.z);
        Coordinate rotatedSource = new Coordinate(rSground.get(0));
        rotatedSource.setOrdinate(1, srcCoord.z);
        projReceiver = JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedReceiver);
        projSource = JTSUtility.makeProjectedPoint(ab[0], ab[1], rotatedSource);
        pInit = JTSUtility.makeProjectedPoint(ab[0], ab[1], new Coordinate(0, 0, 0));
        projReceiver = JTSUtility.getOldCoordinateSystem(projReceiver, angle);
        projSource = JTSUtility.getOldCoordinateSystem(projSource, angle);
        pInit = JTSUtility.getOldCoordinateSystem(pInit, angle);

        projReceiver.x = srcCoord.x + projReceiver.x;
        projSource.x = srcCoord.x + projSource.x;
        projReceiver.y = srcCoord.y + projReceiver.y;
        projSource.y = srcCoord.y + projSource.y;
        pInit.x = srcCoord.x + pInit.x;
        pInit.y = srcCoord.y + pInit.y;

        segments.add(new SegmentPath(gPath, new Vector3D(projSource, projReceiver), pInit));

        points.add(new PointPath(srcCoord, altS, new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(receiverCoord, altR, new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        PropagationPath propagationPath = new PropagationPath(false, points, segments, segments);
        propagationPath.setGs(data.gS);
        return propagationPath;

    }


    public PropagationPath computeVerticalEdgeDiffraction(Coordinate receiverCoord,
                                                          Coordinate srcCoord, String side) {

        PropagationPath propagationPath2 = new PropagationPath();
        List<Coordinate> coordinates = new ArrayList<>();

        if (side.equals("right")) {
            // Right hand
            coordinates = computeSideHull(false, srcCoord, receiverCoord);
            Collections.reverse(coordinates);
        } else if (side.equals("left")) {
            coordinates = computeSideHull(true, srcCoord, receiverCoord);
        }

        if (!coordinates.isEmpty()) {
            if (coordinates.size() > 2) {
                PropagationPath propagationPath = computeFreefield(coordinates.get(1), coordinates.get(0), null);
                propagationPath.getPointList().get(1).setType(PointPath.POINT_TYPE.DIFV);
                propagationPath2.setPointList(propagationPath.getPointList());
                propagationPath2.setSegmentList(propagationPath.getSegmentList());
                int j;
                for (j = 1; j < coordinates.size() - 2; j++) {
                    propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j), null);
                    propagationPath.getPointList().get(1).setType(PointPath.POINT_TYPE.DIFV);
                    propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                    propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());
                }
                propagationPath = computeFreefield(coordinates.get(j + 1), coordinates.get(j), null);
                propagationPath2.getPointList().add(propagationPath.getPointList().get(1));
                propagationPath2.getSegmentList().addAll(propagationPath.getSegmentList());

            }
        }
        return propagationPath2;
}

    public PropagationPath computeHorizontalEdgeDiffraction(boolean obstructedSourceReceiver, Coordinate receiverCoord,
                                                            Coordinate srcCoord, List<TriIdWithIntersection> allInterPoints) {

        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        boolean validDiffraction;

        DiffractionWithSoilEffetZone diffDataWithSoilEffet;


        if (!obstructedSourceReceiver) {
            diffDataWithSoilEffet = data.freeFieldFinder.getPathInverse(receiverCoord, srcCoord);
            validDiffraction = false;
        } else {
            diffDataWithSoilEffet = data.freeFieldFinder.getPath(receiverCoord, srcCoord, allInterPoints);
            // Offset Coordinates by epsilon
            validDiffraction = diffDataWithSoilEffet.getROZone() != null;
        }
        // todo not sure about this part...
        if (validDiffraction) {
            List<Coordinate> offsetPath = new ArrayList<>(diffDataWithSoilEffet.getPath());
            for (int i = 1; i < offsetPath.size() - 1; i++) {
                Coordinate dest = offsetPath.get(i);
                Vector2D v = new Vector2D(offsetPath.get(0), dest).normalize().multiply(FastObstructionTest.epsilon);
                offsetPath.set(i, new Coordinate(dest.x - v.getX(), dest.y - v.getY(), dest.z));
            }
            for (int j = offsetPath.size() - 1; j > 1; j--) {
                PropagationPath propagationPath1 = computeFreefield(offsetPath.get(j - 1), offsetPath.get(j), null);
                propagationPath1.getPointList().get(1).setType(PointPath.POINT_TYPE.DIFH);
                if (j == offsetPath.size() - 1) {
                    propagationPath1.getPointList().get(0).setCoordinate(offsetPath.get(j));
                    points.add(propagationPath1.getPointList().get(0));
                }
                points.add(propagationPath1.getPointList().get(1));
                segments.addAll(propagationPath1.getSegmentList());
            }

            PropagationPath propagationPath2 = computeFreefield(offsetPath.get(0), offsetPath.get(1), null);
            points.add(propagationPath2.getPointList().get(1));
            segments.add(propagationPath2.getSegmentList().get(0));

        } else {
            PropagationPath propagationPath = computeFreefield(receiverCoord, srcCoord, null);
            points.addAll(propagationPath.getPointList());
            segments.addAll(propagationPath.getSegmentList());
            srPath.addAll(propagationPath.getSRList());
        }
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getSegmentLength() < 0.1) {
                segments.remove(i);
                points.remove(i + 1);
            }
        }
        return new PropagationPath(true, points, segments, srPath);
    }

    public static Plane ComputeZeroRadPlane(Coordinate p0, Coordinate p1) {
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D s = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p0.x, p0.y, p0.z);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D r = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p1.x, p1.y, p1.z);
        double angle = Math.atan2(p1.y - p0.y, p1.x - p0.x);
        // Compute rPrime, the third point of the plane that is at -PI/2 with SR vector
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D rPrime = s.add(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(Math.cos(angle - Math.PI / 2), Math.sin(angle - Math.PI / 2), 0));
        Plane p = new Plane(r, s, rPrime, 1e-6);
        // Normal of the cut plane should be upward
        if (p.getNormal().getZ() < 0) {
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
        for (int idp = 0; idp < roofPts.size(); idp++) {
            double offset = plane.getOffset(CoordinateToVector(roofPts.get(idp)));
            if (lastOffset != null && ((offset >= 0 && lastOffset < 0) || (offset < 0 && lastOffset >= 0))) {
                // Interpolate vector
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(CoordinateToVector(roofPts.get(idp - 1)), CoordinateToVector(roofPts.get(idp)), FastObstructionTest.epsilon));
                polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            if (offset >= 0) {
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(roofPts.get(idp).x, roofPts.get(idp).y, Double.MIN_VALUE), CoordinateToVector(roofPts.get(idp)), FastObstructionTest.epsilon));
                polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            lastOffset = offset;
        }
        return polyCut;
    }

    /**
     * Compute Side Hull
     * Create a line between p1 and p2. Find the first intersection of this line with a building then create a ConvexHull
     * with the points of buildings in intersection. While there is an intersection add more points to the convex hull.
     * The side diffraction path is found when there is no more intersection.
     *
     * @param left If true return path between p1 and p2; else p2 to p1
     * @param p1   First point
     * @param p2   Second point
     * @return
     */
    public List<Coordinate> computeSideHull(boolean left, Coordinate p1, Coordinate p2) {
        if (p1.equals(p2)) {
            return new ArrayList<>();
        }

        // Intersection test cache
        Set<LineSegment> freeFieldSegments = new HashSet<>();
        GeometryFactory geometryFactory = new GeometryFactory();

        List<Coordinate> input = new ArrayList<>();

        Coordinate[] coordinates = new Coordinate[0];
        int indexp1 = 0;
        int indexp2 = 0;

        boolean convexHullIntersects = true;

        input.add(p1);
        input.add(p2);

        Set<Integer> buildingInHull = new HashSet<>();

        Plane cutPlane = ComputeZeroRadPlane(p1, p2);

        IntersectionRayVisitor intersectionRayVisitor = new IntersectionRayVisitor(
                data.freeFieldFinder.getPolygonWithHeight(), p1, p2, data.freeFieldFinder, input, buildingInHull, cutPlane);

        data.freeFieldFinder.getBuildingsOnPath(p1, p2, intersectionRayVisitor);

        int k;
        while (convexHullIntersects) {
            ConvexHull convexHull = new ConvexHull(input.toArray(new Coordinate[0]), geometryFactory);
            Geometry convexhull = convexHull.getConvexHull();

            if (convexhull.getLength() / p1.distance(p2) > MAX_RATIO_HULL_DIRECT_PATH) {
                return new ArrayList<>();
            }

            convexHullIntersects = false;
            coordinates = convexhull.getCoordinates();

            input.clear();
            input.addAll(Arrays.asList(coordinates));

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
                        // Check if we still are in the propagation domain
                        if (!data.freeFieldFinder.getMeshEnvelope().contains(coordinates[k]) ||
                                !data.freeFieldFinder.getMeshEnvelope().contains(coordinates[k + 1])) {
                            // This side goes over propagation path
                            return new ArrayList<>();
                        }
                        intersectionRayVisitor = new IntersectionRayVisitor(data.freeFieldFinder.getPolygonWithHeight(),
                                coordinates[k], coordinates[k + 1], data.freeFieldFinder, input, buildingInHull, cutPlane);
                        data.freeFieldFinder.getBuildingsOnPath(coordinates[k], coordinates[k + 1], intersectionRayVisitor);
                        if (!intersectionRayVisitor.doContinue()) {
                            convexHullIntersects = true;
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
        // Check for invalid coordinates
        for (Coordinate p : coordinates) {
            if (p.z < 0) {
                return new ArrayList<>();
            }
        }

        if (left) {
            return Arrays.asList(Arrays.copyOfRange(coordinates, indexp1, indexp2 + 1));
        } else {
            ArrayList<Coordinate> inversePath = new ArrayList<>();
            inversePath.addAll(Arrays.asList(Arrays.copyOfRange(coordinates, indexp2, coordinates.length)));
            Collections.reverse(inversePath);
            return inversePath;
        }
    }


    public List<PropagationPath> directPath(Coordinate srcCoord,
                                            Coordinate receiverCoord, boolean verticalDiffraction, boolean horizontalDiffraction) {


        List<PropagationPath> propagationPaths = new ArrayList<>();


        // Then, check if the source is visible from the receiver (not
        // hidden by a building)
        // Create the direct Line

        boolean freefield = true;
        boolean topographyHideReceiver = false;

        List<TriIdWithIntersection> inters = new ArrayList<>();
        data.freeFieldFinder.computePropagationPath(srcCoord, receiverCoord, false, inters, true);
        for (TriIdWithIntersection intersection : inters) {
            if (intersection.getBuildingId() > 0) {
                topographyHideReceiver = true;
            }
            if (intersection.isIntersectionOnBuilding() || intersection.isIntersectionOnTopography()) {
                freefield = false;
                if (intersection.isIntersectionOnTopography()) {
                    topographyHideReceiver = true;
                }
            }
        }

        // double fav_probability = favrose[(int) (Math.round(calcRotationAngleInDegrees(srcCoord, receiverCoord) / 30))];

        if (freefield) {
            PropagationPath propagationPath = computeFreefield(receiverCoord, srcCoord, inters);
            propagationPaths.add(propagationPath);
        }

        //Process diffraction 3D
        // todo include rayleigh criterium
        if (verticalDiffraction && !freefield) {
            PropagationPath propagationPath3 = computeFreefield(receiverCoord, srcCoord, inters);
            PropagationPath propagationPath = computeHorizontalEdgeDiffraction(topographyHideReceiver, receiverCoord, srcCoord, inters);
            propagationPath.getSRList().addAll(propagationPath3.getSRList());
            propagationPaths.add(propagationPath);


        }

        if (topographyHideReceiver && data.isComputeHorizontalDiffraction() && horizontalDiffraction && !freefield) {
            // todo if one of the points > roof or < floor, get out this path
            PropagationPath propagationPath3 = computeFreefield(receiverCoord, srcCoord, inters);

            PropagationPath propagationPath = computeVerticalEdgeDiffraction(srcCoord, receiverCoord, "left");
            if (propagationPath.getPointList()!=null) {
                for (int i = 0; i < propagationPath.getSegmentList().size(); i++) {
                    if (propagationPath.getSegmentList().get(i).getSegmentLength() < 0.1) {
                        propagationPath.getSegmentList().remove(i);
                        propagationPath.getPointList().remove(i + 1);
                    }
                }
                propagationPath.setSRList(propagationPath3.getSRList());
                propagationPaths.add(propagationPath);
            }
            propagationPath = computeVerticalEdgeDiffraction(srcCoord, receiverCoord, "right");
            if (propagationPath.getPointList()!=null) {
                for (int i = 0; i < propagationPath.getSegmentList().size(); i++) {
                    if (propagationPath.getSegmentList().get(i).getSegmentLength() < 0.1) {
                        propagationPath.getSegmentList().remove(i);
                        propagationPath.getPointList().remove(i + 1);
                    }
                }
                propagationPath.setSRList(propagationPath3.getSRList());
                propagationPaths.add(propagationPath);
            }
        }
        return propagationPaths;
    }

    /**
     * Source-Receiver Direct+Reflection+Diffraction computation
     *
     * @param src           Sound source informations
     * @param receiverCoord      coordinate of receiver
     * @param rcvId              receiver identifier
     * @param nearBuildingsWalls Walls to use in reflection
     * @param raysCount number of rays computed in this function
     * @param dataOut
     * @return Minimal power level (dB) or maximum attenuation (dB)
     */
    private double[] receiverSourcePropa(SourcePointInfo src,
                                         Coordinate receiverCoord, int rcvId, AtomicInteger raysCount,
                                         IComputeRaysOut dataOut,List<FastObstructionTest.Wall> nearBuildingsWalls, List<MirrorReceiverResult> mirrorReceiverResults) {
        Coordinate srcCoord = src.position;
        int srcId = src.sourcePrimaryKey;
        double sourceLi = src.li;

        // Build mirrored receiver list from wall list

        double PropaDistance = srcCoord.distance(receiverCoord);
        if (PropaDistance < data.maxSrcDist) {

            // Process direct path (including horizontal and vertical diffraction)
            List<PropagationPath> propagationPaths = directPath(srcCoord, receiverCoord, data.isComputeVerticalDiffraction(), true);

            // Process specular reflection
            if (data.reflexionOrder > 0) {
                List<PropagationPath> propagationPaths_all = computeReflexion(receiverCoord, srcCoord,
                        false, nearBuildingsWalls, mirrorReceiverResults);
                propagationPaths.addAll(propagationPaths_all);
            }

            if (propagationPaths.size() > 0) {
                for (PropagationPath propagationPath : propagationPaths) {
                    propagationPath.idSource = srcId;
                    propagationPath.idReceiver = rcvId;
                    // Compute the propagation source phi and theta
                    propagationPath.setSourceOrientation(src.getOrientation());
                    propagationPath.setGs(src.getGs());
                }

                if(raysCount != null) {
                    raysCount.addAndGet(propagationPaths.size());
                }

                return dataOut.addPropagationPaths(srcId, sourceLi, rcvId, propagationPaths);
            }
        }
        return new double[0];
    }

    private static double insertPtSource(Coordinate receiverPos, Coordinate ptpos, double[] wj, double li, Integer sourceId, List<SourcePointInfo> sourceList, Orientation orientation, double gs) {
        // Compute maximal power at freefield at the receiver position with reflective ground
        double aDiv = -getADiv(CGAlgorithms3D.distance(receiverPos, ptpos));
        double[] srcWJ = new double[wj.length];
        for (int idFreq = 0; idFreq < srcWJ.length; idFreq++) {
            srcWJ[idFreq] = wj[idFreq] * li * dbaToW(aDiv) * dbaToW(3);
        }
        sourceList.add(new SourcePointInfo(srcWJ, sourceId, ptpos, li, orientation, gs));
        return ComputeRays.sumArray(srcWJ.length, srcWJ);
    }

    private double addLineSource(LineString source, Coordinate receiverCoord, int srcIndex, List<SourcePointInfo> sourceList, double[] wj) {
        double totalPowerRemaining = 0;
        ArrayList<Coordinate> pts = new ArrayList<Coordinate>();

        // Compute li to equation 4.1 NMPB 2008 (June 2009)
        Coordinate nearestPoint = JTSUtility.getNearestPoint(receiverCoord, source);
        double segmentSizeConstraint = Math.max(1, receiverCoord.distance3D(nearestPoint) / 2.0);
        if (Double.isNaN(segmentSizeConstraint)) {
            segmentSizeConstraint = Math.max(1, receiverCoord.distance(nearestPoint) / 2.0);
        }
        double li = splitLineStringIntoPoints(source, segmentSizeConstraint, pts);
        for (int ptIndex = 0; ptIndex < pts.size(); ptIndex++) {
            Coordinate pt = pts.get(ptIndex);
            if (pt.distance(receiverCoord) < data.maxSrcDist) {
                // use the orientation computed from the line source coordinates
                Vector3D v;
                if(ptIndex == 0) {
                    v = new Vector3D(source.getCoordinates()[0], pts.get(ptIndex));
                } else {
                    v = new Vector3D(pts.get(ptIndex - 1), pts.get(ptIndex));
                }
                Orientation inputOrientation;
                if(data.sourcesPk.size() > srcIndex && data.sourceOrientation.containsKey(data.sourcesPk.get(srcIndex))) {
                    // If the line source already provide an orientation then alter the line orientation
                    inputOrientation = data.sourceOrientation.get(data.sourcesPk.get(srcIndex));
                    inputOrientation = Orientation.fromVector(
                            Orientation.rotate(new Orientation(inputOrientation.yaw, inputOrientation.roll, 0),
                                    v.normalize()), inputOrientation.roll);
                } else {
                    inputOrientation = Orientation.fromVector(v.normalize(), 0);
                }
                double gs;
                if(data.sourcesPk.size() > srcIndex && data.sourceGs.containsKey(data.sourcesPk.get(srcIndex))) {
                    // If the line source already provide an orientation then alter the line orientation
                    gs = data.sourceGs.get(data.sourcesPk.get(srcIndex));
                } else {
                    gs = this.data.gS;
                }
                totalPowerRemaining += insertPtSource(receiverCoord, pt, wj, li, srcIndex, sourceList, inputOrientation, gs);
            }
        }
        return totalPowerRemaining;
    }

    /**
     * Compute sound level by frequency band at this receiver position
     *
     * @param receiverCoord
     */
    public void computeRaysAtPosition(Coordinate receiverCoord, int idReceiver, List<PropagationDebugInfo> debugInfo, IComputeRaysOut dataOut, ProgressVisitor progressVisitor) {
        // List of walls within maxReceiverSource distance
        HashSet<Integer> processedLineSources = new HashSet<Integer>(); //Already processed Raw source (line and/or points)
        List<FastObstructionTest.Wall> wallsReceiver = new ArrayList<>();
        List<MirrorReceiverResult> mirrorReceiverResults = new ArrayList<>();
        if (data.reflexionOrder > 0) {
            wallsReceiver.addAll(data.freeFieldFinder.getLimitsInRange(
                    data.maxRefDist, receiverCoord, false));
            new MirrorReceiverIterator.It(receiverCoord, wallsReceiver, data.reflexionOrder).forEach(mirrorReceiverResults::add);
        }
        double searchSourceDistance = data.maxSrcDist;
        Envelope receiverSourceRegion = new Envelope(receiverCoord.x
                - searchSourceDistance, receiverCoord.x + searchSourceDistance,
                receiverCoord.y - searchSourceDistance, receiverCoord.y
                + searchSourceDistance
        );
        Iterator<Integer> regionSourcesLst = data.sourcesIndex
                .query(receiverSourceRegion);
        List<SourcePointInfo> sourceList = new ArrayList<>();

        // Sum of all sources power using only geometric dispersion with direct field
        double totalPowerRemaining = 0;
        if (data.noiseFloor>0) {
            totalPowerRemaining = dbaToW(data.noiseFloor);
        }
        while (regionSourcesLst.hasNext()) {
            Integer srcIndex = regionSourcesLst.next();
            if (!processedLineSources.contains(srcIndex)) {
                processedLineSources.add(srcIndex);
                Geometry source = data.sourceGeometries.get(srcIndex);
                double[] wj = data.getMaximalSourcePower(srcIndex);
                if (source instanceof Point) {
                    Coordinate ptpos = source.getCoordinate();
                    if (ptpos.distance(receiverCoord) < data.maxSrcDist) {
                        Orientation orientation = null;
                        if(data.sourcesPk.size() > srcIndex) {
                            orientation = data.sourceOrientation.get(data.sourcesPk.get(srcIndex));
                        }
                        if(orientation == null) {
                            orientation = new Orientation(0,0, 0);
                        }

                        double gs;
                        if(data.sourcesPk.size() > srcIndex && data.sourceGs.containsKey(data.sourcesPk.get(srcIndex))) {
                            // If the line source already provide an orientation then alter the line orientation
                            gs = data.sourceGs.get(data.sourcesPk.get(srcIndex));
                        } else {
                            gs = data.gS;
                        }

                        totalPowerRemaining += insertPtSource(receiverCoord, ptpos, wj, 1., srcIndex, sourceList, orientation, gs);
                    }
                } else if (source instanceof LineString) {
                    // Discretization of line into multiple point
                    // First point is the closest point of the LineString from
                    // the receiver
                    totalPowerRemaining += addLineSource((LineString) source, receiverCoord, srcIndex, sourceList, wj);
                } else if (source instanceof MultiLineString) {
                    for (int id = 0; id < source.getNumGeometries(); id++) {
                        Geometry subGeom = source.getGeometryN(id);
                        if (subGeom instanceof LineString) {
                            totalPowerRemaining += addLineSource((LineString) subGeom, receiverCoord, srcIndex, sourceList, wj);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Sound source %s geometry are not supported", source.getGeometryType()));
                }
            }
        }

        // Sort sources by power contribution descending
        Collections.sort(sourceList);
        // Final sound power level at each receiver
        double maximumPowerAtReceiver = 0;
        if (data.noiseFloor>0) {
            maximumPowerAtReceiver = dbaToW(data.noiseFloor);
        }
        //Iterate over source point sorted by maximal power by descending order
        AtomicInteger raysCount = new AtomicInteger(0);
        for (SourcePointInfo src : sourceList) {
            // If the delta between already received power and maximal potential power received is inferior than than data.maximumError
            if ((progressVisitor != null && progressVisitor.isCanceled()) || (data.maximumError > 0 && wToDba(maximumPowerAtReceiver + totalPowerRemaining) - wToDba(maximumPowerAtReceiver) < data.maximumError)) {
                break; //Stop looking for more rays
            }


            // For each Pt Source - Pt Receiver
            Coordinate srcCoord = src.position;

            Set<FastObstructionTest.Wall> wallsSource = new HashSet<>(wallsReceiver);
            if (data.reflexionOrder > 0) {
                wallsSource.addAll(data.freeFieldFinder.getLimitsInRange(
                        data.maxRefDist, srcCoord, false));
            }
            double[] power = receiverSourcePropa(src, receiverCoord, idReceiver
                    , raysCount, dataOut, wallsReceiver, mirrorReceiverResults);
            double global = ComputeRays.sumArray(power.length, ComputeRays.dbaToW(power));
            totalPowerRemaining -= src.globalWj;
            if (power.length > 0) {
                maximumPowerAtReceiver += global;
            } else {
                maximumPowerAtReceiver += src.globalWj;
            }
            totalPowerRemaining = Math.max(0, totalPowerRemaining);


        }
        if(profilerThread != null &&
                profilerThread.getMetric(ReceiverStatsMetric.class) != null) {
            profilerThread.getMetric(ReceiverStatsMetric.class).onReceiverRays(idReceiver, raysCount.get());
        }
        // No more rays for this receiver
        dataOut.finalizeReceiver(idReceiver);
    }

    /**
     * Must be called before computeSoundLevelAtPosition     */
    public void initStructures() {
        //Build R-tree for soil geometry and soil type
        rTreeOfGeoSoil = new STRtree();
        List<GeoWithSoilType> soilTypeList = data.getSoilList();
        if (soilTypeList != null) {
            for (int i = 0; i < soilTypeList.size(); i++) {
                GeoWithSoilType geoWithSoilType = soilTypeList.get(i);
                rTreeOfGeoSoil.insert(geoWithSoilType.getGeo().getEnvelopeInternal(),
                        new EnvelopeWithIndex<Integer>(geoWithSoilType.getGeo().getEnvelopeInternal(), i));
            }
        }
    }

    public void runDebug(IComputeRaysOut computeRaysOut, List<PropagationDebugInfo> debugInfo) {

        initStructures();

        // Computed sound level of vertices
        //dataOut.setVerticesSoundLevel(new double[data.receivers.size()]);

        // For each vertices, find sources where the distance is within
        // maxSrcDist meters
        ProgressVisitor propaProcessProgression = data.cellProg;

        if(threadCount == 0) {
            Runtime runtime = Runtime.getRuntime();
            this.threadCount = Math.max(1, runtime.availableProcessors());
        }

        ThreadPool threadManager = new ThreadPool(
                threadCount,
                threadCount + 1, Long.MAX_VALUE,
                TimeUnit.SECONDS);

        ConcurrentLinkedDeque<Integer> receiversToCompute = new ConcurrentLinkedDeque<>();
        // receiversToCompute is a stack of receiver to compute
        // all concurrent threads will consume this stack in order to keep the number of working
        // concurrent thread until the end
        for(int receiverId =0; receiverId < data.receivers.size(); receiverId++) {
            receiversToCompute.add(receiverId);
        }
        for(int idThread = 0; idThread < threadCount; idThread++) {
            if (propaProcessProgression != null && propaProcessProgression.isCanceled()) {
                break;
            }
            RangeReceiversComputation batchThread = new RangeReceiversComputation(receiversToCompute, this, debugInfo, propaProcessProgression,
                    computeRaysOut.subProcess());
            if (threadCount != 1) {
                threadManager.executeBlocking(batchThread);
            } else {
                batchThread.run();
            }
        }
        threadManager.shutdown();
        try {
            threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    public void run(IComputeRaysOut computeRaysOut) {
        runDebug(computeRaysOut, null);
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

private static final class RangeReceiversComputation implements Runnable {
    private ConcurrentLinkedDeque<Integer> receiversToCompute;
    private ComputeRays propagationProcess;
    private List<PropagationDebugInfo> debugInfo;
    private ProgressVisitor progressVisitor;
    private IComputeRaysOut dataOut;

    public RangeReceiversComputation(ConcurrentLinkedDeque<Integer> receiversToCompute, ComputeRays propagationProcess,
                                     List<PropagationDebugInfo> debugInfo, ProgressVisitor progressVisitor,
                                     IComputeRaysOut dataOut) {
        this.receiversToCompute = receiversToCompute;
        this.propagationProcess = propagationProcess;
        this.debugInfo = debugInfo;
        this.progressVisitor = progressVisitor;
        this.dataOut = dataOut;
    }

    @Override
    public void run() {
        try {
            while(!receiversToCompute.isEmpty()) {
                int idReceiver = receiversToCompute.pop();
                if (progressVisitor != null) {
                    if (progressVisitor.isCanceled()) {
                        break;
                    }
                }
                Coordinate receiverCoord = propagationProcess.data.receivers.get(idReceiver);
                long start = 0;
                if(propagationProcess.profilerThread != null) {
                    start = propagationProcess.profilerThread.timeTracker.get();
                }

                propagationProcess.computeRaysAtPosition(receiverCoord, idReceiver, debugInfo, dataOut, progressVisitor);

                // Save computation time for this receiver
                if(propagationProcess.profilerThread != null &&
                        propagationProcess.profilerThread.getMetric(ReceiverStatsMetric.class) != null) {
                    propagationProcess.profilerThread.getMetric(ReceiverStatsMetric.class).onEndComputation(idReceiver,
                            (int) (propagationProcess.profilerThread.timeTracker.get() - start));
                }

                if (progressVisitor != null) {
                    progressVisitor.endStep();
                }

            }
        } catch (NoSuchElementException ex) {
            // ignore as it is expected at the end of the computation
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
            if (progressVisitor != null) {
                progressVisitor.cancel();
            }
            throw ex;
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

    public void reset() {
        geometryChanged.set(false);
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

private static final class SourcePointInfo implements Comparable<SourcePointInfo> {
    private double[] wj;
    private double li; //
    private int sourcePrimaryKey;
    private Coordinate position;
    private double globalWj;
    private Orientation orientation;
    private double gs;

    /**
     *
     * @param wj Source power for each frequency bands
     * @param sourcePrimaryKey
     * @param position
     * @param li Coefficient of power per meter for this point source
     * @param orientation
     */
    public SourcePointInfo(double[] wj, int sourcePrimaryKey, Coordinate position, double li, Orientation orientation, double gs) {
        this.wj = wj;
        this.sourcePrimaryKey = sourcePrimaryKey;
        this.position = position;
        if (Double.isNaN(position.z)) {
            this.position = new Coordinate(position.x, position.y, 0);
        }
        this.globalWj = ComputeRays.sumArray(wj.length, wj);
        this.li = li;
        this.orientation = orientation;
        this.gs = gs;
    }

    /**
     * @return coefficient to apply to linear source as sound power per meter length
     */
    public double getLi() {
        return li;
    }

    public double[] getWj() {
        return wj;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public double getGs() {
        return gs;
    }


    public void setWj(double[] wj) {
        this.wj = wj;
        this.globalWj = ComputeRays.sumArray(wj.length, wj);
    }

    @Override
    public int compareTo(SourcePointInfo sourcePointInfo) {
        int cmp = -Double.compare(globalWj, sourcePointInfo.globalWj);
        if (cmp == 0) {
            return Integer.compare(sourcePrimaryKey, sourcePointInfo.sourcePrimaryKey);
        } else {
            return cmp;
        }
    }
}

private static final class IntersectionRayVisitor extends FastObstructionTest.IntersectionRayVisitor {
    Set<Integer> buildingsInIntersection;
    FastObstructionTest freeFieldFinder;
    Plane cutPlane;
    List<Coordinate> input;
    boolean foundIntersection = false;

    public IntersectionRayVisitor(List<MeshBuilder.PolygonWithHeight> polygonWithHeight, Coordinate p1,
                                  Coordinate p2, FastObstructionTest freeFieldFinde, List<Coordinate> input, Set<Integer> buildingsInIntersection, Plane cutPlane) {
        super(polygonWithHeight, p1, p2);
        this.freeFieldFinder = freeFieldFinde;
        this.input = input;
        this.buildingsInIntersection = buildingsInIntersection;
        this.cutPlane = cutPlane;
    }

    @Override
    public void addBuilding(int buildingId) {
        if (buildingsInIntersection.contains(buildingId)) {
            return;
        }
        List<Coordinate> roofPoints = freeFieldFinder.getWideAnglePointsByBuilding(buildingId, 0, 2 * Math.PI);
        // Create a cut of the building volume
        roofPoints = cutRoofPointsWithPlane(cutPlane, roofPoints);
        if (!roofPoints.isEmpty()) {
            input.addAll(roofPoints.subList(0, roofPoints.size() - 1));
            buildingsInIntersection.add(buildingId);
            foundIntersection = true;
        }
    }

    public boolean doContinue() {
        return !foundIntersection;
    }
}
}
