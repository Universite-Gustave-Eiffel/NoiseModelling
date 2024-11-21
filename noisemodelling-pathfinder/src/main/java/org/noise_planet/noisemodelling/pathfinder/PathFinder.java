/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.algorithm.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.noise_planet.noisemodelling.pathfinder.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.path.*;
import org.noise_planet.noisemodelling.pathfinder.path.SegmentPath;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.path.PointPath;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.JTSUtility;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ReceiverStatsMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Double.isNaN;
import static java.lang.Math.*;
import static org.noise_planet.noisemodelling.pathfinder.PathFinder.ComputationSide.LEFT;
import static org.noise_planet.noisemodelling.pathfinder.PathFinder.ComputationSide.RIGHT;
import static org.noise_planet.noisemodelling.pathfinder.path.PointPath.POINT_TYPE.*;
import static org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder.IntersectionType.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.geometry.GeometricAttenuation.getADiv;
import static org.noise_planet.noisemodelling.pathfinder.utils.geometry.GeometryUtils.projectPointOnLine;
import static org.noise_planet.noisemodelling.pathfinder.utils.Utils.*;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 * @author Sylvain Palominos
 */
public class PathFinder {
    private static final double ALPHA0 = 2e-4;
    // distance from wall for reflection points and diffraction points
    private static final double NAVIGATION_POINT_DISTANCE_FROM_WALLS = ProfileBuilder.MILLIMETER;
    private static final double epsilon = 1e-7;
    private static final double MAX_RATIO_HULL_DIRECT_PATH = 4;
    public static final Logger LOGGER = LoggerFactory.getLogger(PathFinder.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public Scene getData() {
        return data;
    }
    /** Propagation data to use for computation. */
    private final Scene data;

    /** Number of thread used for ray computation. */
    private int threadCount ;
    private ProfilerThread profilerThread;

    /**
     * Create new instance from the propagation data.
     * @param data Propagation data used for ray computation.
     */
    public PathFinder(Scene data) {
        this.data = data;
        this.threadCount = Runtime.getRuntime().availableProcessors();
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

    /**
     * Sets the number of thread to use.
     * @param threadCount Number of thread.
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Run computation and store the results in the given output.
     * @param computeRaysOut Result output.
     */
    public void run(IComputePathsOut computeRaysOut) {
        ProgressVisitor visitor = data.cellProg;
        ThreadPool threadManager = new ThreadPool(threadCount, threadCount + 1, Long.MAX_VALUE, TimeUnit.SECONDS);
        int maximumReceiverBatch = (int) ceil(data.receivers.size() / (double) threadCount);
        int endReceiverRange = 0;
        //Launch execution of computation by batch
        while (endReceiverRange < data.receivers.size()) {
            //Break if the progress visitor is cancelled
            if (visitor != null && visitor.isCanceled()) {
                break;
            }
            int newEndReceiver = min(endReceiverRange + maximumReceiverBatch, data.receivers.size());
            ThreadPathFinder batchThread = new ThreadPathFinder(endReceiverRange, newEndReceiver,
                    this, visitor, computeRaysOut, data);
            if (threadCount != 1) {
                threadManager.executeBlocking(batchThread);
            } else {
                batchThread.run();
            }
            endReceiverRange = newEndReceiver;
        }
        //Once the execution ends, shutdown the thread manager and await termination
        threadManager.shutdown();
        try {
            if(!threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Timeout elapsed before termination.");
            }
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Compute the rays to the given receiver.
     * @param rcv     Receiver point.
     * @param dataOut Computation output.
     * @param visitor Progress visitor used for cancellation and progression managing.
     */
    public void computeRaysAtPosition(PointPath.ReceiverPointInfo rcv, IComputePathsOut dataOut, ProgressVisitor visitor) {
        MirrorReceiversCompute receiverMirrorIndex = null;

        if(data.reflexionOrder > 0) {
            Envelope receiverPropagationEnvelope = new Envelope(rcv.getCoord());
            receiverPropagationEnvelope.expandBy(data.maxSrcDist);
            List<Wall> buildWalls = data.profileBuilder.getWallsIn(receiverPropagationEnvelope);
            receiverMirrorIndex = new MirrorReceiversCompute(buildWalls, rcv.position, data.reflexionOrder,
                    data.maxSrcDist, data.maxRefDist);
        }

        //Compute the source search area
        double searchSourceDistance = data.maxSrcDist;
        Envelope receiverSourceRegion = new Envelope(
                rcv.getCoord().x - searchSourceDistance,
                rcv.getCoord().x + searchSourceDistance,
                rcv.getCoord().y - searchSourceDistance,
                rcv.getCoord().y + searchSourceDistance
        );
        Iterator<Integer> regionSourcesLst = data.sourcesIndex.query(receiverSourceRegion);
        List<PointPath.SourcePointInfo> sourceList = new ArrayList<>();
        //Already processed Raw source (line and/or points)
        HashSet<Integer> processedLineSources = new HashSet<>();
        // Sum of all sources power using only geometric dispersion with direct field
        double totalPowerRemaining = 0;
        while (regionSourcesLst.hasNext()) {
            Integer srcIndex = regionSourcesLst.next();
            if (!processedLineSources.contains(srcIndex)) {
                processedLineSources.add(srcIndex);
                Geometry source = data.sourceGeometries.get(srcIndex);
                double[] wj = data.getMaximalSourcePower(srcIndex);
                if (source instanceof Point) {
                    Coordinate ptpos = source.getCoordinate();
                    if (ptpos.distance(rcv.getCoord()) < data.maxSrcDist) {
                        Orientation orientation = null;
                        if(data.sourcesPk.size() > srcIndex) {
                            orientation = data.sourceOrientation.get(data.sourcesPk.get(srcIndex));
                        }
                        if(orientation == null) {
                            orientation = new Orientation(0,0, 0);
                        }
                        totalPowerRemaining += insertPtSource((Point) source, rcv.getCoord(), srcIndex, sourceList, wj, 1., orientation);
                    }
                } else if (source instanceof LineString) {
                    totalPowerRemaining += addLineSource((LineString) source, rcv.getCoord(), srcIndex, sourceList, wj);
                } else if (source instanceof MultiLineString) {
                    for (int id = 0; id < source.getNumGeometries(); id++) {
                        Geometry subGeom = source.getGeometryN(id);
                        if (subGeom instanceof LineString) {
                            totalPowerRemaining += addLineSource((LineString) subGeom, rcv.getCoord(), srcIndex, sourceList, wj);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Sound source %s geometry are not supported", source.getGeometryType()));
                }
            }
        }
        // Sort sources by power contribution descending
        Collections.sort(sourceList);
        double powerAtSource = 0;
        // For each Pt Source - Pt Receiver
        AtomicInteger raysCount = new AtomicInteger(0);
        for (PointPath.SourcePointInfo src : sourceList) {
            double[] power = rcvSrcPropagation(src, src.li, rcv, dataOut, raysCount, receiverMirrorIndex);
            double global = sumArray(power.length, dbaToW(power));
            totalPowerRemaining -= src.globalWj;
            if (power.length > 0) {
                powerAtSource += global;
            } else {
                powerAtSource += src.globalWj;
            }
            totalPowerRemaining = max(0, totalPowerRemaining);
            // If the delta between already received power and maximal potential power received is inferior than than data.maximumError
            if ((visitor != null && visitor.isCanceled()) || (data.maximumError > 0 &&
                    wToDba(powerAtSource + totalPowerRemaining) - wToDba(powerAtSource) < data.maximumError)) {
                break; //Stop looking for more rays
            }
        }

        if(profilerThread != null &&
                profilerThread.getMetric(ReceiverStatsMetric.class) != null) {
            profilerThread.getMetric(ReceiverStatsMetric.class).onReceiverRays(rcv.getId(), raysCount.get());
        }

        // No more rays for this receiver
        dataOut.finalizeReceiver(rcv.getId());
    }

    /**
     * Calculation of the propagation between the given source and receiver. The result is registered in the given
     * output.
     * @param src     Source point.
     * @param srcLi   Source power per meter coefficient.
     * @param rcv     Receiver point.
     * @param dataOut Output.
     * @return
     */
    private double[] rcvSrcPropagation(PointPath.SourcePointInfo src, double srcLi, PointPath.ReceiverPointInfo rcv,
                                       IComputePathsOut dataOut, AtomicInteger raysCount,
                                       MirrorReceiversCompute receiverMirrorIndex) {

        double propaDistance = src.getCoord().distance(rcv.getCoord());
        if (propaDistance < data.maxSrcDist) {
            // Process direct : horizontal and vertical diff
            List<CnossosPath> pathParameters = new ArrayList<>(directPath(src, rcv,
                    data.computeVerticalDiffraction, data.computeHorizontalDiffraction, data.isBodyBarrier()));
            // Process reflection
            if (data.reflexionOrder > 0) {
                pathParameters.addAll(computeReflexion(rcv.getCoord(), src.getCoord(), false,
                        src.getOrientation(), receiverMirrorIndex));
            }
            if (!pathParameters.isEmpty()) {
                if(raysCount != null) {
                    raysCount.addAndGet(pathParameters.size());
                }
                return dataOut.addPropagationPaths(src.getId(), srcLi, rcv.getId(), pathParameters);
            }
        }
        return new double[0];
    }

    /**
     * Direct Path computation.
     * @param src Source point.
     * @param rcv Receiver point.
     * @return Calculated propagation paths.
     */
    public List<CnossosPath> directPath(PointPath.SourcePointInfo src,
                                        PointPath.ReceiverPointInfo rcv, boolean verticalDiffraction, boolean horizontalDiffraction, boolean bodyBarrier) {
        return directPath(src.getCoord(), src.getId(), src.getOrientation(), rcv.getCoord(), rcv.getId(),
                verticalDiffraction, horizontalDiffraction, bodyBarrier);
    }

    /**
     * Direct Path computation.
     * @param srcCoord Source point coordinate.
     * @param srcId    Source point identifier.
     * @param rcvCoord Receiver point coordinate.
     * @param rcvId    Receiver point identifier.
     * @return Calculated propagation paths.
     */
    public List<CnossosPath> directPath(Coordinate srcCoord, int srcId, Orientation orientation, Coordinate rcvCoord,
                                        int rcvId, boolean verticalDiffraction, boolean horizontalDiffraction,
                                        boolean bodyBarrier) {
        List<CnossosPath> pathsParameters = new ArrayList<>();
        CutProfile cutProfile = data.profileBuilder.getProfile(srcCoord, rcvCoord, data.gS, !verticalDiffraction);
        if(cutProfile.getSource() != null) {
            cutProfile.getSource().setId(srcId);
        }
        if(cutProfile.getReceiver() != null) {
            cutProfile.getReceiver().setId(rcvId);
        }
        cutProfile.setSrcOrientation(orientation);

        if(verticalDiffraction || cutProfile.isFreeField()) {
            CnossosPath hEdgePath = computeHEdgeDiffraction(cutProfile, bodyBarrier);
            if (hEdgePath != null) {
                pathsParameters.add(hEdgePath);
            }
        }

        // do not do horizontal plane diffraction if there is no obstacles between source and receiver
        // ISO/TR 17534-4:2020
        // "As a general principle, lateral diffraction is considered only if the direct line of sight
        // between source and receiver is blocked and does not penetrate the terrain profile.
        // In addition, the source must not be a mirror source due to reflection"
        if (horizontalDiffraction && !cutProfile.isFreeField()) {
            CnossosPath vEdgePath = computeVEdgeDiffraction(rcvCoord, srcCoord, data, RIGHT, orientation);
            if (vEdgePath != null && vEdgePath.getPointList() != null) {
                pathsParameters.add(vEdgePath);
            }
            vEdgePath = computeVEdgeDiffraction(rcvCoord, srcCoord, data, LEFT, orientation);
            if (vEdgePath != null && vEdgePath.getPointList() != null) {
                pathsParameters.add(vEdgePath);
            }
        }

        for(CnossosPath pathParameters : pathsParameters) {
            pathParameters.idSource = srcId;
            pathParameters.idReceiver = rcvId;
            pathParameters.setSourceOrientation(orientation);
        }

        return pathsParameters;
    }


    /**
     * Eq.2.5.24 and Eq. 2.5.25
     * @param mn
     * @param d
     * @return
     */

    private static double toCurve(double mn, double d){
        return 2*max(1000, 8*d)* asin(mn/(2*max(1000, 8*d)));
    }


    /**
     *
     * @param src
     * @param rcv
     * @param meanPlane
     * @param gPath
     * @param gS
     * @return
     */
    private static SegmentPath computeSegment(Coordinate src, Coordinate rcv, double[] meanPlane, double gPath, double gS) {
        return computeSegment(src, src.y, rcv, rcv.y, meanPlane, gPath, gS);
    }


    /**
     * Compute the segment path
     * @param src
     * @param rcv
     * @param meanPlane
     * @return the calculated segment
     */
    private static SegmentPath computeSegment(Coordinate src, Coordinate rcv, double[] meanPlane) {
        return computeSegment(src, src.y, rcv, rcv.y, meanPlane, 0, 0);
    }

    /**
     * Compute the segment path with more attribute
     * @param src
     * @param sz
     * @param rcv
     * @param rz
     * @param meanPlane
     * @param gPath
     * @param gS
     * @return the computed segment path
     */

    private static SegmentPath computeSegment(Coordinate src, double sz, Coordinate rcv, double rz, double[] meanPlane, double gPath, double gS) {
        SegmentPath seg = new SegmentPath();
        Coordinate srcZ = new Coordinate(src.x, sz);
        Coordinate rcvZ = new Coordinate(rcv.x, rz);
        Coordinate sourcePointOnMeanPlane = projectPointOnLine(srcZ, meanPlane[0], meanPlane[1]);
        Coordinate receiverPointOnMeanPlane = projectPointOnLine(rcvZ, meanPlane[0], meanPlane[1]);
        Vector2D sourceToProjectedPoint = Vector2D.create(srcZ, sourcePointOnMeanPlane);
        Vector2D receiverToProjectedPoint = Vector2D.create(rcvZ, receiverPointOnMeanPlane);
        seg.s = srcZ;
        seg.r = rcvZ;
        seg.sMeanPlane = sourcePointOnMeanPlane;
        seg.rMeanPlane = receiverPointOnMeanPlane;
        seg.sPrime = Vector2D.create(sourcePointOnMeanPlane).add(sourceToProjectedPoint).toCoordinate();
        seg.rPrime = Vector2D.create(receiverPointOnMeanPlane).add(receiverToProjectedPoint).toCoordinate();

        seg.d = src.distance(rcv);
        seg.dp =sourcePointOnMeanPlane.distance(receiverPointOnMeanPlane);
        seg.zsH = srcZ.distance(sourcePointOnMeanPlane);
        seg.zrH = rcvZ.distance(receiverPointOnMeanPlane);
        seg.a = meanPlane[0];
        seg.b = meanPlane[1];
        seg.testFormH = seg.dp/(30*(seg.zsH +seg.zrH));
        seg.gPath = gPath;
        seg.gPathPrime = seg.testFormH <= 1 ? seg.gPath*(seg.testFormH) + gS*(1-seg.testFormH) : seg.gPath; // 2.5.14
        double deltaZT = 6e-3 * seg.dp / (seg.zsH + seg.zrH);
        double deltaZS = ALPHA0 * pow((seg.zsH / (seg.zsH + seg.zrH)), 2) * (seg.dp*seg.dp / 2); //2.5.19
        seg.zsF = seg.zsH + deltaZS + deltaZT;
        double deltaZR = ALPHA0 * pow((seg.zrH / (seg.zsH + seg.zrH)), 2) * (seg.dp*seg.dp / 2);
        seg.zrF = seg.zrH + deltaZR + deltaZT;
        seg.testFormF = seg.dp/(30*(seg.zsF +seg.zrF));
        return seg;
    }


    /**
     *
     * @param pts
     * @return @return the computed coordinate list
     */
    private static List<Coordinate> computePts2D(List<CutPoint> pts) {
        List<Coordinate> pts2D = pts.stream()
                .map(CutPoint::getCoordinate)
                .collect(Collectors.toList());
        pts2D = JTSUtility.getNewCoordinateSystem(pts2D);
        return pts2D;
    }

    /**
     *
     * @param sourceOrientation
     * @param src
     * @param next
     * @return
     */
    private static Orientation computeOrientation(Orientation sourceOrientation, Coordinate src, Coordinate next){
        if(sourceOrientation == null) {
            return null;
        }
        Vector3D outgoingRay = new Vector3D(new Coordinate(next.x - src.x,
                next.y - src.y,
                next.z - src.z)).normalize();
        return Orientation.fromVector(Orientation.rotate(sourceOrientation, outgoingRay, true), 0);
    }

    /**
     * Compute horizontal diffraction (diffraction of vertical edge.)
     * @param receiverCoordinates Receiver coordinates.
     * @param sourceCoordinates Source coordinates.
     * @param data     Propagation data.
     * @param side     Side to compute. From Source to receiver coordinates
     * @return The propagation path of the horizontal diffraction.
     */
    public CnossosPath computeVEdgeDiffraction(Coordinate receiverCoordinates, Coordinate sourceCoordinates,
                                               Scene data, ComputationSide side, Orientation orientation) {

        List<Coordinate> coordinates = computeSideHull(side == LEFT, new Coordinate(sourceCoordinates), new Coordinate(receiverCoordinates), data.profileBuilder);

        List<CutPoint> cutPoints = new ArrayList<>();

        if(coordinates.size() > 2) {
            // Fetch vertical profile between each point of the diffraction path
            for(int i=0; i<coordinates.size()-1; i++) {
                CutProfile profile = data.profileBuilder.getProfile(coordinates.get(i), coordinates.get(i+1), data.gS,
                        false);
                // Push new plane (except duplicate points for intermediate segments)
                if( i > 0 ) {
                    // update first point when it is not source but diffraction point
                    CutPoint vDiffPoint = profile.getCutPoints().get(0);
                    vDiffPoint.setType(ProfileBuilder.IntersectionType.V_EDGE_DIFFRACTION);
                }
                if(i+1 == coordinates.size() - 1) {
                    // we keep the last point as it is really the receiver
                    cutPoints.addAll(profile.getCutPoints());
                } else {
                    cutPoints.addAll(profile.getCutPoints().subList(0, profile.getCutPoints().size() - 1));
                }
            }
            CutProfile mainProfile = new CutProfile();
            mainProfile.addCutPoints(cutPoints);
            mainProfile.setSource(cutPoints.get(0));
            mainProfile.setSrcOrientation(orientation);
            mainProfile.setReceiver(cutPoints.get(cutPoints.size() -  1));
            // Compute Ray path from vertical cuts (like a folding screen)
            CnossosPath cnossosPath = computeHEdgeDiffraction(mainProfile, data.isBodyBarrier());

            if(cnossosPath == null) {
                // path not valid (ex: intersection with ground)
                return null;
            }

            return cnossosPath;
        }
        return null;
    }


    /**
     * convertit une série de points 3D en une série de points 2D
     * @param coordinates
     * @return
     */
    private List<Coordinate> toDirectLine(List<Coordinate> coordinates) {
        List<Coordinate> coords = new ArrayList<>();
        if(coordinates.isEmpty()) {
            return coords;
        }
        Coordinate prev = coordinates.get(0);
        double d = 0;
        for(Coordinate c : coordinates) {
            d+=prev.distance(c);
            prev = c;
            coords.add(new Coordinate(d, c.z));
        }
        return coords;
    }



    private void computeRayleighDiff(SegmentPath srSeg, CutProfile cutProfile, CnossosPath pathParameters,
                                     LineSegment dSR, List<SegmentPath> segments, List<PointPath> points,
                                     List<Coordinate> pts2D, Coordinate[] pts2DGround, List<Integer> cut2DGroundIndex) {
        final List<CutPoint> cuts = cutProfile.getCutPoints();

        Coordinate src = pts2D.get(0);
        Coordinate rcv = pts2D.get(pts2D.size() - 1);
        CutPoint srcCut = cutProfile.getSource();
        CutPoint rcvCut = cutProfile.getReceiver();
        for (int iO = 1; iO < pts2DGround.length - 1; iO++) {
            int i0Cut = cut2DGroundIndex.indexOf(iO);
            Coordinate o = pts2DGround[iO];

            double dSO = src.distance(o);
            double dOR = o.distance(rcv);
            double deltaH = dSR.orientationIndex(o) * (dSO + dOR - srSeg.d);
            List<Integer> freqs = data.freq_lvl;
            boolean rcrit = false;
            for(int f : freqs) {
                if(deltaH > -(340./f) / 20) {
                    rcrit = true;
                    break;
                }
            }
            if (rcrit) {
                rcrit = false;
                //Add point path

                //Plane S->O
                Coordinate[] soCoords = Arrays.copyOfRange(pts2DGround, 0, iO + 1);
                double[] abs = JTSUtility.getMeanPlaneCoefficients(soCoords);
                SegmentPath seg1 = computeSegment(src, o, abs);

                //Plane O->R
                Coordinate[] orCoords = Arrays.copyOfRange(pts2DGround, iO, pts2DGround.length);
                double[] abr = JTSUtility.getMeanPlaneCoefficients(orCoords);
                SegmentPath seg2 = computeSegment(o, rcv, abr);

                Coordinate srcPrime = new Coordinate(src.x + (seg1.sMeanPlane.x - src.x) * 2, src.y + (seg1.sMeanPlane.y - src.y) * 2);
                Coordinate rcvPrime = new Coordinate(rcv.x + (seg2.rMeanPlane.x - rcv.x) * 2, rcv.y + (seg2.rMeanPlane.y - rcv.y) * 2);

                LineSegment dSPrimeRPrime = new LineSegment(srcPrime, rcvPrime);
                srSeg.dPrime = srcPrime.distance(rcvPrime);
                seg1.dPrime = srcPrime.distance(o);
                seg2.dPrime = o.distance(rcvPrime);

                double deltaPrimeH = dSPrimeRPrime.orientationIndex(o) * (seg1.dPrime + seg2.dPrime - srSeg.dPrime);
                for(int f : freqs) {
                    if(deltaH > (340./f) / 4 - deltaPrimeH) {
                        rcrit = true;
                        break;
                    }
                }
                if (rcrit) {
                    pathParameters.deltaH = deltaH;
                    pathParameters.deltaPrimeH = deltaPrimeH;
                    seg1.setGpath(cutProfile.getGPath(srcCut, cuts.get(i0Cut)), srcCut.getGroundCoef());
                    seg2.setGpath(cutProfile.getGPath(cuts.get(i0Cut), rcvCut), srcCut.getGroundCoef());

                    if(dSR.orientationIndex(o) == 1) {
                        pathParameters.deltaF = toCurve(dSO, srSeg.d) + toCurve(dOR, srSeg.d) - toCurve(srSeg.d, srSeg.d);
                    }
                    else {
                        Coordinate pA = dSR.pointAlong((o.x-src.x)/(rcv.x-src.x));
                        pathParameters.deltaF =2*toCurve(src.distance(pA), srSeg.d) + 2*toCurve(pA.distance(rcv), srSeg.d) - toCurve(dSO, srSeg.d) - toCurve(dOR, srSeg.d) - toCurve(srSeg.d, srSeg.d);
                    }

                    LineSegment sPrimeR = new LineSegment(seg1.sPrime, rcv);
                    double dSPrimeO = seg1.sPrime.distance(o);
                    double dSPrimeR = seg1.sPrime.distance(rcv);
                    pathParameters.deltaSPrimeRH = sPrimeR.orientationIndex(o)*(dSPrimeO + dOR - dSPrimeR);

                    LineSegment sRPrime = new LineSegment(src, seg2.rPrime);
                    double dORPrime = o.distance(seg2.rPrime);
                    double dSRPrime = src.distance(seg2.rPrime);
                    pathParameters.deltaSRPrimeH = sRPrime.orientationIndex(o)*(dSO + dORPrime - dSRPrime);

                    if(dSPrimeRPrime.orientationIndex(o) == 1) {
                        pathParameters.deltaPrimeF = toCurve(seg1.dPrime, srSeg.dPrime) + toCurve(seg2.dPrime, srSeg.dPrime) - toCurve(srSeg.dPrime, srSeg.dPrime);
                    }
                    else {
                        Coordinate pA = dSPrimeRPrime.pointAlong((o.x-srcPrime.x)/(rcvPrime.x-srcPrime.x));
                        pathParameters.deltaPrimeF =2*toCurve(srcPrime.distance(pA), srSeg.dPrime) + 2*toCurve(pA.distance(srcPrime), srSeg.dPrime) - toCurve(seg1.dPrime, srSeg.dPrime) - toCurve(seg2.dPrime, srSeg.d) - toCurve(srSeg.dPrime, srSeg.dPrime);
                    }

                    segments.add(seg1);
                    segments.add(seg2);

                    points.add(new PointPath(o, o.z, new ArrayList<>(), DIFH_RCRIT));
                }
            }
        }
    }

    /**
     * Given the vertical cut profile (can be a single plane or multiple like a folding panel) return the ray path
     * following Cnossos specification, or null if there is no valid path.
     * @param cutProfile Vertical cut of a domain
     * @param bodyBarrier
     * @return The cnossos path or null
     */
    public CnossosPath computeHEdgeDiffraction(CutProfile cutProfile , boolean bodyBarrier) {
        List<SegmentPath> segments = new ArrayList<>();
        List<PointPath> points = new ArrayList<>();
        final List<CutPoint> cutProfilePoints = cutProfile.getCutPoints();

        List<Coordinate> pts2D = computePts2D(cutProfilePoints);
        if(pts2D.size() != cutProfilePoints.size()) {
            throw new IllegalArgumentException("The two arrays size should be the same");
        }

        List<Integer> cut2DGroundIndex = new ArrayList<>(cutProfile.getCutPoints().size());
        Coordinate[] pts2DGround = cutProfile.computePts2DGround(cut2DGroundIndex).toArray(new Coordinate[0]);
        double[] meanPlane = JTSUtility.getMeanPlaneCoefficients(pts2DGround);
        Coordinate firstPts2D = pts2D.get(0);
        Coordinate lastPts2D = pts2D.get(pts2D.size()-1);
        SegmentPath srPath = computeSegment(firstPts2D, lastPts2D, meanPlane, cutProfile.getGPath(), cutProfile.getSource().getGroundCoef());
        srPath.setPoints2DGround(pts2DGround);
        srPath.dc = CGAlgorithms3D.distance(cutProfile.getReceiver().getCoordinate(),
                cutProfile.getSource().getCoordinate());
        CnossosPath pathParameters = new CnossosPath();
        pathParameters.setCutProfile(cutProfile);
        pathParameters.setFavorable(true);
        pathParameters.setPointList(points);
        pathParameters.setSegmentList(segments);
        pathParameters.setSRSegment(srPath);
        pathParameters.init(data.freq_lvl.size());
        pathParameters.angle=Angle.angle(cutProfile.getReceiver().getCoordinate(), cutProfile.getSource().getCoordinate());
        // Extract the first and last points to define the line segment
        Coordinate firstPt = pts2D.get(0);
        Coordinate lastPt = pts2D.get(pts2D.size() - 1);

        // Filter out points that are below the line segment
        List<Coordinate> convexHullInput = new ArrayList<>();
        // Add source position
        convexHullInput.add(pts2D.get(0));
        // Add valid diffraction point, building/walls/dem
        for (int idPoint=1; idPoint < cutProfilePoints.size() - 1; idPoint++) {
            boolean validIntersection = false;
            CutPoint currentPoint = cutProfilePoints.get(idPoint);
            switch (currentPoint.getType()) {
                case BUILDING:
                case WALL:
                    // We only add the point at the top of the wall, not the point at the bottom of the wall
                    validIntersection = Double.compare(currentPoint.getCoordinate().z, currentPoint.getzGround()) != 0;
                    break;
                case TOPOGRAPHY:
                    validIntersection = true;
                    break;
                default:
            }
            if(validIntersection) {
                convexHullInput.add(pts2D.get(idPoint));
            }
        }
        // Add receiver position
        convexHullInput.add(pts2D.get(pts2D.size() - 1));

        // Compute the convex hull using JTS
        List<Coordinate> convexHullPoints = new ArrayList<>();
        if(convexHullInput.size() > 2) {
            GeometryFactory geomFactory = new GeometryFactory();
            Coordinate[] coordsArray = convexHullInput.toArray(new Coordinate[0]);
            ConvexHull convexHull = new ConvexHull(coordsArray, geomFactory);
            Coordinate[] convexHullCoords = convexHull.getConvexHull().getCoordinates();
            int indexFirst = Arrays.asList(convexHull.getConvexHull().getCoordinates()).indexOf(firstPt);
            int indexLast = Arrays.asList(convexHull.getConvexHull().getCoordinates()).lastIndexOf(lastPt);
            convexHullCoords = Arrays.copyOfRange(convexHullCoords, indexFirst, indexLast + 1);
            CoordinateSequence coordSequence = geomFactory.getCoordinateSequenceFactory().create(convexHullCoords);
            Geometry geom = geomFactory.createLineString(coordSequence);
            Geometry uniqueGeom = geom.union(); // Removes duplicate coordinates
            convexHullCoords = uniqueGeom.getCoordinates();
            // Convert the result back to your format (List<Point2D> pts)
            if (convexHullCoords.length == 3) {
                convexHullPoints = Arrays.asList(convexHullCoords);
            } else {
                for (int j = 0; j < convexHullCoords.length; j++) {
                    // Check if the y-coordinate is valid (not equal to Double.MAX_VALUE and not infinite)
                    if (convexHullCoords[j].y == Double.MAX_VALUE || Double.isInfinite(convexHullCoords[j].y)) {
                        continue; // Skip this point as it's not part of the hull
                    }
                    convexHullPoints.add(convexHullCoords[j]);
                }
            }
        } else {
            convexHullPoints = convexHullInput;
        }
        List<Coordinate> pts = convexHullPoints;

        Coordinate src = cutProfile.getSource().getCoordinate();

        // Move then check reflection height if there is diffraction on the path
        if(pts.size() > 2) {
            for (int i = 1; i < pts.size(); i++) {
                int i0 = pts2D.indexOf(pts.get(i - 1));
                int i1 = pts2D.indexOf(pts.get(i));
                LineSegment segmentHull = new LineSegment(pts.get(i - 1), pts.get(i));
                for (int pointIndex = i0 + 1; pointIndex < i1; pointIndex++) {
                    final CutPoint currentPoint = cutProfilePoints.get(pointIndex);
                    // If the current point is the reflection point (not on the ground level)
                    if (currentPoint.getType().equals(REFLECTION) &&
                            Double.compare(currentPoint.getCoordinate().z, currentPoint.getzGround()) != 0) {
                        MirrorReceiver mirrorReceiver = currentPoint.getMirrorReceiver();
                        Coordinate interpolatedReflectionPoint = segmentHull.closestPoint(pts2D.get(pointIndex));
                        // Check if the new elevation of the reflection point is not higher than the wall
                        double wallAltitudeAtReflexionPoint = Vertex.interpolateZ(mirrorReceiver.getReflectionPosition(),
                                mirrorReceiver.getWall().p0, mirrorReceiver.getWall().p1);
                        if(wallAltitudeAtReflexionPoint + epsilon >= interpolatedReflectionPoint.y) {
                            // update the reflection position
                            currentPoint.getCoordinate().setZ(interpolatedReflectionPoint.y);
                            pts2D.get(pointIndex).setY(interpolatedReflectionPoint.y);
                        } else {
                            // Reflection is not valid, so the whole path is not valid
                            return null;
                        }
                    }
                }
            }
        }

        // Create segments from each diffraction point to the receiver
        for (int i = 1; i < pts.size(); i++) {
            int i0 = pts2D.indexOf(pts.get(i - 1));
            int i1 = pts2D.indexOf(pts.get(i));
            int i0Ground = cut2DGroundIndex.get(i0);
            int i1Ground = cut2DGroundIndex.get(i1);
            final CutPoint cutPt0 = cutProfilePoints.get(i0);
            final CutPoint cutPt1 = cutProfilePoints.get(i1);
            // ground index may be near the diffraction point
            // mean ground plane is computed using from the bottom of the walls
            if (i0Ground < i1Ground - 1) {
                CutPoint nextPoint = cutProfilePoints.get(i0 + 1);
                if (cutPt0.getCoordinate().distance(nextPoint.getCoordinate()) <= ProfileBuilder.MILLIMETER + epsilon
                        && Double.compare(nextPoint.getCoordinate().z, nextPoint.getzGround()) == 0
                && (nextPoint.getType().equals(WALL) || nextPoint.getType().equals(BUILDING))) {
                    i0Ground += 1;
                }
            }
            if (i1Ground - 1 > i0Ground) {
                CutPoint previousPoint = cutProfilePoints.get(i1 - 1);
                if (cutPt1.getCoordinate().distance(previousPoint.getCoordinate()) <= ProfileBuilder.MILLIMETER +
                        epsilon && Double.compare(previousPoint.getCoordinate().z, previousPoint.getzGround()) == 0
                        && (previousPoint.getType().equals(WALL) || previousPoint.getType().equals(BUILDING))) {
                    i1Ground -= 1;
                }
            }
            // Create a profile for the segment i0->i1
            CutProfile profileSeg = new CutProfile();
            profileSeg.addCutPoints(cutProfilePoints.subList(i0, i1 + 1));
            profileSeg.setSource(cutPt0);
            profileSeg.setReceiver(cutPt1);


            if (points.isEmpty()) {
                // First segment, add the source point in the array
                points.add(new PointPath(pts2D.get(i0), cutPt0.getzGround(), cutPt0.getWallAlpha(), cutPt1.getBuildingId(), SRCE));
                // look for the first reflection before the first diffraction, the source orientation is to the first reflection point
                Coordinate targetPosition = cutProfilePoints.get(i1).getCoordinate();
                for (int pointIndex = i0 + 1; pointIndex < i1; pointIndex++) {
                    final CutPoint currentPoint = cutProfilePoints.get(pointIndex);
                    if ((currentPoint.getType().equals(REFLECTION) || currentPoint.getType().equals(V_EDGE_DIFFRACTION)) &&
                            Double.compare(currentPoint.getCoordinate().z, currentPoint.getzGround()) != 0) {
                        // The first reflection (the one not at ground level)
                        // from the source coordinate is the direction of the propagation
                        targetPosition = currentPoint.getCoordinate();
                        break;
                    }
                }
                points.get(0).orientation = computeOrientation(cutProfile.getSrcOrientation(),
                        cutProfilePoints.get(0).getCoordinate(), targetPosition);
                pathParameters.raySourceReceiverDirectivity = points.get(0).orientation;
                src = pts2D.get(i0);
            }
            // Add reflection/vertical edge diffraction points/segments between i0 i1
            int previousPivotPoint = i0;
            for (int pointIndex = i0 + 1; pointIndex < i1; pointIndex++) {
                final CutPoint currentPoint = cutProfilePoints.get(pointIndex);
                if (currentPoint.getType().equals(REFLECTION) &&
                        Double.compare(currentPoint.getCoordinate().z, currentPoint.getzGround()) != 0) {
                    // If the current point is a reflection and not before/after the reflection
                    MirrorReceiver mirrorReceiver = currentPoint.getMirrorReceiver();
                    double wallAltitudeAtReflexionPoint = Vertex.interpolateZ(mirrorReceiver.getReflectionPosition(),
                            mirrorReceiver.getWall().p0, mirrorReceiver.getWall().p1);
                    PointPath reflectionPoint = new PointPath(pts2D.get(pointIndex),currentPoint.getzGround(), currentPoint.getWallAlpha(), REFL);
                    reflectionPoint.obstacleZ = wallAltitudeAtReflexionPoint;
                    reflectionPoint.setWallId(currentPoint.getWallId());
                    points.add(reflectionPoint);
                } else if (currentPoint.getType().equals(V_EDGE_DIFFRACTION)) {
                    // current point is a vertical edge diffraction (there is no additional points unlike reflection)
                    PointPath diffractionPoint = new PointPath(pts2D.get(pointIndex),currentPoint.getzGround(), new ArrayList<>(), DIFV);
                    diffractionPoint.setWallId(currentPoint.getWallId());
                    points.add(diffractionPoint);
                    // Compute additional segment
                    Coordinate[] segmentGroundPoints = Arrays.copyOfRange(pts2DGround, i0Ground,cut2DGroundIndex.get(pointIndex) + 1);
                    meanPlane = JTSUtility.getMeanPlaneCoefficients(segmentGroundPoints);
                    SegmentPath seg = computeSegment(pts2D.get(previousPivotPoint), pts2D.get(pointIndex),
                            meanPlane, profileSeg.getGPath(cutPt0, cutProfilePoints.get(pointIndex)), data.gS);
                    seg.setPoints2DGround(segmentGroundPoints);
                    previousPivotPoint = pointIndex;
                    segments.add(seg);
                }
            }
            points.add(new PointPath(pts2D.get(i1), cutPt1.getzGround(), cutPt1.getWallAlpha(), cutPt1.getBuildingId(), RECV));
            if(previousPivotPoint != i0 && i == pts.size() - 1) {
                // we added segments before i1 vertical plane diffraction point, but it is the last vertical plane
                // diffraction point and we must add the remaining segment between the last horizontal diffraction point
                // and the last point
                Coordinate[] segmentGroundPoints = Arrays.copyOfRange(pts2DGround, i1Ground, pts2DGround.length);
                meanPlane = JTSUtility.getMeanPlaneCoefficients(segmentGroundPoints);
                SegmentPath seg = computeSegment(pts2D.get(previousPivotPoint), pts2D.get(pts2D.size() - 1),
                        meanPlane, profileSeg.getGPath(cutPt1, cutProfilePoints.get(cutProfilePoints.size() - 1)),
                        data.gS);
                seg.setPoints2DGround(segmentGroundPoints);
                segments.add(seg);
            }
            if(pts.size() == 2) {
                // no diffraction over buildings/dem, we already computed SR segment
                break;
            }
            Coordinate[] segmentGroundPoints = Arrays.copyOfRange(pts2DGround, i0Ground,i1Ground + 1);
            meanPlane = JTSUtility.getMeanPlaneCoefficients(segmentGroundPoints);
            SegmentPath path = computeSegment(pts2D.get(i0), pts2D.get(i1), meanPlane, profileSeg.getGPath(),
                    profileSeg.getSource().getGroundCoef());
            path.setPoints2DGround(segmentGroundPoints);
            segments.add(path);
            if (i != pts.size() - 1) {
                PointPath pt = points.get(points.size() - 1);
                pt.type = DIFH;
                pt.bodyBarrier = bodyBarrier;
                if (pt.buildingId != -1) {
                    pt.alphaWall = data.profileBuilder.getBuilding(pt.buildingId).getAlphas();
                    pt.setObstacleZ(data.profileBuilder.getBuilding(pt.buildingId).getZ());
                } else if (pt.wallId != -1) {
                    pt.alphaWall = data.profileBuilder.getWall(pt.wallId).getAlphas();
                    Wall wall = data.profileBuilder.getWall(pt.wallId);
                    pt.setObstacleZ(Vertex.interpolateZ(pt.coordinate, wall.p0, wall.p1));
                }
            }
        }

        if(points.isEmpty()) {
            return null;
        }

        Coordinate rcv = points.get(points.size()-1).coordinate;
        PointPath p0 = points.stream().filter(p -> p.type.equals(DIFH)).findFirst().orElse(null);
        if(p0==null){
            // Direct propagation (no diffraction over obstructing objects)
            boolean horizontalPlaneDiffractionOrReflection = cutProfile.getCutPoints().stream()
                    .anyMatch(
                            cutPoint -> cutPoint.getType().equals(V_EDGE_DIFFRACTION) ||
                                    cutPoint.getType().equals(REFLECTION));
            List<SegmentPath> rayleighSegments = new ArrayList<>();
            List<PointPath> rayleighPoints = new ArrayList<>();
            // do not check for rayleigh if the path is not direct between R and S
            if(!horizontalPlaneDiffractionOrReflection) {
                // Check for Rayleigh criterion for segments computation
                LineSegment dSR = new LineSegment(firstPts2D, lastPts2D);
                // Look for diffraction over edge on free field (frequency dependent)
                computeRayleighDiff(srPath, cutProfile, pathParameters, dSR, rayleighSegments, rayleighPoints, pts2D,
                        pts2DGround, cut2DGroundIndex);
            }
            if(rayleighSegments.isEmpty()) {
                // We don't have a Rayleigh diffraction over DEM. Only direct SR path
                if(segments.isEmpty()) {
                    segments.add(pathParameters.getSRSegment());
                }
                // Compute cumulated distance between the first diffraction and the last diffraction point
                pathParameters.e = 0;
                LineSegment sr = new LineSegment(pts2D.get(0), pts2D.get(pts2D.size() - 1));
                for(int idPoint = 1; idPoint < points.size() - 2; idPoint++) {
                    pathParameters.e += points.get(idPoint).coordinate.distance(points.get(idPoint+1).coordinate);
                }
                pathParameters.deltaH = segments.get(0).d + pathParameters.e + segments.get(segments.size()-1).d - srPath.dc;
                pathParameters.deltaF = pathParameters.deltaH;
            } else {
                segments.addAll(rayleighSegments);
                points.addAll(1, rayleighPoints);
            }
            return pathParameters;
        }
        Coordinate c0 = p0.coordinate;
        PointPath pn = points.stream().filter(p -> p.type.equals(DIFH)).reduce((first, second) -> second).orElse(null);
        if(pn==null){
            return null;
        }
        Coordinate cn = pn.coordinate;

        SegmentPath seg1 = segments.get(0);
        SegmentPath seg2 = segments.get(segments.size()-1);

        double dSO0 = seg1.d;
        double dOnR = seg2.d;
        LineSegment sr = new LineSegment(src, rcv);

        LineSegment sPrimeR = new LineSegment(seg1.sPrime, rcv);
        double dSPrimeR = seg1.sPrime.distance(rcv);
        double dSPrimeO = seg1.sPrime.distance(c0);
        // Compute cumulated distance between the first diffraction and the last diffraction point
        pathParameters.e = 0;
        for(int idPoint = 1; idPoint < points.size() - 2; idPoint++) {
            pathParameters.e += points.get(idPoint).coordinate.distance(points.get(idPoint+1).coordinate);
        }
        pathParameters.deltaSPrimeRH = sPrimeR.orientationIndex(c0)*(dSPrimeO + pathParameters.e + dOnR - dSPrimeR);
        pathParameters.deltaSPrimeRF = toCurve(dSPrimeO, dSPrimeR) + toCurve(pathParameters.e, dSPrimeR) + toCurve(dOnR, dSPrimeR) - toCurve(dSPrimeR, dSPrimeR);

        LineSegment sRPrime = new LineSegment(src, seg2.rPrime);
        double dSRPrime = src.distance(seg2.rPrime);
        double dORPrime = cn.distance(seg2.rPrime);
        pathParameters.deltaSRPrimeH = (src.x>seg2.rPrime.x?-1:1)*sRPrime.orientationIndex(cn)*(dSO0 + pathParameters.e + dORPrime - dSRPrime);
        pathParameters.deltaSRPrimeF = toCurve(dSO0, dSRPrime) + toCurve(pathParameters.e, dSRPrime) + toCurve(dORPrime, dSRPrime) - toCurve(dSRPrime, dSRPrime);

        Coordinate srcPrime = new Coordinate(src.x + (seg1.sMeanPlane.x - src.x) * 2, src.y + (seg1.sMeanPlane.y - src.y) * 2);
        Coordinate rcvPrime = new Coordinate(rcv.x + (seg2.rMeanPlane.x - rcv.x) * 2, rcv.y + (seg2.rMeanPlane.y - rcv.y) * 2);

        LineSegment dSPrimeRPrime = new LineSegment(srcPrime, rcvPrime);
        srPath.dPrime = srcPrime.distance(rcvPrime);
        seg1.dPrime = srcPrime.distance(c0);
        seg2.dPrime = cn.distance(rcvPrime);


        pathParameters.deltaH = sr.orientationIndex(c0) * (dSO0 + pathParameters.e + dOnR - srPath.dc);
        if (sr.orientationIndex(c0) == 1) {
            pathParameters.deltaF = toCurve(seg1.d, srPath.d) + toCurve(pathParameters.e, srPath.d) + toCurve(seg2.d, srPath.d) - toCurve(srPath.d, srPath.d);
        } else {
            Coordinate pA = sr.pointAlong((c0.x - srcPrime.x) / (rcvPrime.x - srcPrime.x));
            pathParameters.deltaF = 2 * toCurve(srcPrime.distance(pA), srPath.dPrime) + 2 * toCurve(pA.distance(rcvPrime), srPath.dPrime) - toCurve(seg1.dPrime, srPath.dPrime) - toCurve(seg2.dPrime, srPath.dPrime) - toCurve(srPath.dPrime, srPath.dPrime);
        }

        pathParameters.deltaPrimeH = dSPrimeRPrime.orientationIndex(c0) * (seg1.dPrime + pathParameters.e + seg2.dPrime - srPath.dPrime);

        pathParameters.deltaPrimeH = dSPrimeRPrime.orientationIndex(c0) * (seg1.dPrime + seg2.dPrime - srPath.dPrime);
        if(dSPrimeRPrime.orientationIndex(c0) == 1) {
            pathParameters.deltaPrimeF = toCurve(seg1.dPrime, srPath.dPrime) + toCurve(seg2.dPrime, srPath.dPrime) - toCurve(srPath.dPrime, srPath.dPrime);
        }
        else {
            Coordinate pA = dSPrimeRPrime.pointAlong((c0.x-srcPrime.x)/(rcvPrime.x-srcPrime.x));
            pathParameters.deltaPrimeF =2*toCurve(srcPrime.distance(pA), srPath.dPrime) + 2*toCurve(pA.distance(srcPrime), srPath.dPrime) - toCurve(seg1.dPrime, srPath.dPrime) - toCurve(seg2.dPrime, srPath.d) - toCurve(srPath.dPrime, srPath.dPrime);
        }

        return pathParameters;
    }

    /**
     * Compute Side Hull
     * Create a line between p1 and p2. Find the first intersection of this line with a building then create a ConvexHull
     * with the points of buildings in intersection. While there is an intersection add more points to the convex hull.
     * The side diffraction path is found when there is no more intersection.
     *
     * @param left If true return the path on the left side between p1 and p2; else on the right side
     * @param p1   First point
     * @param p2   Second point
     * @return
     */
    public List<Coordinate> computeSideHull(boolean left, Coordinate p1, Coordinate p2, ProfileBuilder profileBuilder) {
        if (p1.equals(p2)) {
            return new ArrayList<>();
        }

        // Intersection test cache
        Set<LineSegment> freeFieldSegments = new HashSet<>();

        List<Coordinate> input = new ArrayList<>();

        Coordinate[] coordinates = new Coordinate[0];
        int indexp1 = 0;
        int indexp2 = 0;

        boolean convexHullIntersects = true;

        input.add(p1);
        input.add(p2);

        Plane cutPlane = computeZeroRadPlane(p1, p2);

        BuildingIntersectionPathVisitor buildingIntersectionPathVisitor = new BuildingIntersectionPathVisitor(p1, p2, left,
                profileBuilder, input, cutPlane);

        data.profileBuilder.getWallsOnPath(p1, p2, buildingIntersectionPathVisitor);

        int k;
        while (convexHullIntersects) {
            ConvexHull convexHull = new ConvexHull(input.toArray(new Coordinate[0]), GEOMETRY_FACTORY);
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

                        int inputPointsBefore = input.size();

                        // Visit buildings that are between the provided hull points
                        profileBuilder.getWallsOnPath(coordinates[k], coordinates[k + 1], buildingIntersectionPathVisitor);

                        if (inputPointsBefore == input.size()) {
                            freeFieldSegments.add(freeFieldTestSegment);
                        } else {
                            convexHullIntersects = true;
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

        List<Coordinate> sideHullPath;
        if (left) {
            sideHullPath = Arrays.asList(Arrays.copyOfRange(coordinates, indexp1, indexp2 + 1));
        } else {
            List<Coordinate> inversePath = Arrays.asList(Arrays.copyOfRange(coordinates, indexp2, coordinates.length));
            Collections.reverse(inversePath);
            sideHullPath = inversePath;
        }
        return  sideHullPath;
    }

    /**
     *
     * @param p0
     * @param p1
     * @return
     */
    public static Plane computeZeroRadPlane(Coordinate p0, Coordinate p1) {
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D s = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p0.x, p0.y, p0.z);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D r = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p1.x, p1.y, p1.z);
        double angle = atan2(p1.y - p0.y, p1.x - p0.x);
        // Compute rPrime, the third point of the plane that is at -PI/2 with SR vector
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D rPrime = s.add(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(cos(angle - PI / 2), sin(angle - PI / 2), 0));
        Plane p = new Plane(r, s, rPrime, 1e-6);
        // Normal of the cut plane should be upward
        if (p.getNormal().getZ() < 0) {
            p.revertSelf();
        }
        return p;
    }

    /**
     * Remove points that are left or right of the provided segment
     * @param sr Source receiver segment
     * @param left Side to keep
     * @param segmentsCoordinates Roof points
     * @return Only points of the requested side
     */
    public static List<Coordinate> filterPointsBySide(LineSegment sr, boolean left,
                                                      List<Coordinate> segmentsCoordinates) {
        List<Coordinate> keptSegments = new ArrayList<>(segmentsCoordinates.size());
        for(Coordinate vertex : segmentsCoordinates) {
            int orientationIndex = sr.orientationIndex(vertex);
            if((orientationIndex == 1 && left) || (orientationIndex == -1 && !left)) {
                keptSegments.add(vertex);
            }
        }
        return keptSegments;
    }

    /**
     *
     * @param plane 3D plane with position and normal vector
     * @param roofPts Top altitude coordinates that create segments of verticals walls, these walls will be cut by
     *               the plane.
     * @return Remaining segments coordinates after the plane cutting
     */
    public static List<Coordinate> cutRoofPointsWithPlane(Plane plane, List<Coordinate> roofPts) {
        List<Coordinate> polyCut = new ArrayList<>(roofPts.size());
        double lastOffset = 0;
        Coordinate cPrev = null;
        for (Coordinate cCur : roofPts) {
            double offset = plane.getOffset(coordinateToVector(cCur));
            if (cPrev != null && ((offset >= 0 && lastOffset < 0) || (offset < 0 && lastOffset >= 0))) {
                // Interpolate vector
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(coordinateToVector(cPrev), coordinateToVector(cCur), epsilon));
                polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            if (offset >= 0) {
                org.apache.commons.math3.geometry.euclidean.threed.Vector3D i = plane.intersection(new Line(new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(cCur.x, cCur.y, Double.MIN_VALUE), coordinateToVector(cCur), epsilon));
                if (i != null) polyCut.add(new Coordinate(i.getX(), i.getY(), i.getZ()));
            }
            lastOffset = offset;
            cPrev = cCur;
        }
        return polyCut;
    }

    /**
     *
     * @param p coordinate
     * @return the three dimensions vector of p
     */
    public static org.apache.commons.math3.geometry.euclidean.threed.Vector3D coordinateToVector(Coordinate p) {
        return new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(p.x, p.y, p.z);
    }

    /**
     * Add points to the main profile, the last point is before the reflection on the wall
     * The profile of reflection is receiver -> on the ground before reflection -> reflection position -> on the ground after reflection
     * @param reflectionPoint The point to use and recognised as a reflection point (currently is categorized as source or receiver)
     * @param cutProfile The profile where we can found the point on the first argument
     * @param mirrorReceiver Associated mirror receiver
     */
    private void updateReflectionPointAttributes(CutPoint reflectionPoint, CutProfile cutProfile, MirrorReceiver mirrorReceiver) {
        reflectionPoint.setType(REFLECTION);
        reflectionPoint.setWallAlpha(mirrorReceiver.getWall().getAlphas());
        reflectionPoint.setMirrorReceiver(mirrorReceiver);
        CutPoint reflectionPointBeforeAndAfter = new CutPoint(reflectionPoint);
        reflectionPointBeforeAndAfter.getCoordinate().setZ(reflectionPoint.getzGround());
        // insert ground reflection point
        cutProfile.getCutPoints().add(cutProfile.getCutPoints().indexOf(reflectionPoint), new CutPoint(reflectionPointBeforeAndAfter));
        cutProfile.getCutPoints().add(cutProfile.getCutPoints().indexOf(reflectionPoint)+1, new CutPoint(reflectionPointBeforeAndAfter));
    }


    /**
     *
     * @param rcvCoord
     * @param srcCoord
     * @param favorable
     * @param orientation
     * @param receiverMirrorIndex
     * @return propagation path list
     */
    public List<CnossosPath> computeReflexion(Coordinate rcvCoord, Coordinate srcCoord, boolean favorable,
                                              Orientation orientation, MirrorReceiversCompute receiverMirrorIndex) {

        // Compute receiver mirror
        LineIntersector linters = new RobustLineIntersector();
        //Keep only building walls which are not too far.
        List<MirrorReceiver> mirrorResults = receiverMirrorIndex.findCloseMirrorReceivers(srcCoord);

        List<CnossosPath> reflexionPathParameters = new ArrayList<>();

        for (MirrorReceiver receiverReflection : mirrorResults) {
            Wall seg = receiverReflection.getWall();
            List<MirrorReceiver> rayPath = new ArrayList<>();
            MirrorReceiver receiverReflectionCursor = receiverReflection;
            // Test whether intersection point is on the wall
            // segment or not
            Coordinate destinationPt = new Coordinate(srcCoord);

            linters.computeIntersection(seg.p0, seg.p1,
                    receiverReflection.getReceiverPos(),
                    destinationPt);
            while (linters.hasIntersection()) {
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
                vec_epsilon.x *= NAVIGATION_POINT_DISTANCE_FROM_WALLS;
                vec_epsilon.y *= NAVIGATION_POINT_DISTANCE_FROM_WALLS;
                // Translate reflection pt by epsilon to get outside
                // the wall
                reflectionPt.x -= vec_epsilon.x;
                reflectionPt.y -= vec_epsilon.y;
                // Compute Z interpolation
                reflectionPt.setOrdinate(Coordinate.Z, Vertex.interpolateZ(linters.getIntersection(0),
                        receiverReflectionCursor.getReceiverPos(), destinationPt));
                MirrorReceiver reflResult = new MirrorReceiver(receiverReflectionCursor);
                reflResult.setReflectionPosition(reflectionPt);
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
                    seg = receiverReflectionCursor.getWall();
                    linters.computeIntersection(seg.p0, seg.p1,
                            receiverReflectionCursor
                                    .getReceiverPos(),
                            destinationPt
                    );
                }
            }
            // A valid propagation path as been found (without looking at occlusion)
            CutProfile mainProfile = new CutProfile();
            // Compute direct path between source and first reflection point, add profile to the data
            CutProfile cutProfile = data.profileBuilder.getProfile(srcCoord, rayPath.get(0).getReflectionPosition(),
                    data.gS, !data.computeVerticalDiffraction);
            if(!cutProfile.isFreeField() && !data.computeVerticalDiffraction) {
                // (maybe there is a blocking building/dem, and we disabled diffraction)
                continue;
            }
            // Add points to the main profile, remove the last point, or it will be duplicated later
            mainProfile.addCutPoints(cutProfile.getCutPoints().subList(0, cutProfile.getCutPoints().size() - 1));

            // Add intermediate reflections
            boolean validReflection = true;
            for (int idPt = 0; idPt < rayPath.size() - 1; idPt++) {
                MirrorReceiver firstPoint = rayPath.get(idPt);
                MirrorReceiver secondPoint = rayPath.get(idPt + 1);
                cutProfile = data.profileBuilder.getProfile(firstPoint.getReflectionPosition(),
                        secondPoint.getReflectionPosition(), data.gS, true);
                updateReflectionPointAttributes(cutProfile.getCutPoints().get(0), cutProfile, firstPoint);
                if(!cutProfile.isFreeField() && !data.computeVerticalDiffraction) {
                    // (maybe there is a blocking building/dem, and we disabled diffraction)
                    validReflection = false;
                    break;
                }
                // Add points to the main profile, remove the last point, or it will be duplicated later
                mainProfile.addCutPoints(cutProfile.getCutPoints().subList(0, cutProfile.getCutPoints().size() - 1));
            }
            if(!validReflection) {
                continue;
            }
            // Compute direct path between receiver and last reflection point, add profile to the data
            cutProfile = data.profileBuilder.getProfile(rayPath.get(rayPath.size() - 1).getReflectionPosition(),
                    rcvCoord, data.gS, true);
            if(!cutProfile.isFreeField() && !data.computeVerticalDiffraction) {
                // (maybe there is a blocking building/dem, and we disabled diffraction)
                continue;
            }
            updateReflectionPointAttributes(cutProfile.getCutPoints().get(0), cutProfile, rayPath.get(rayPath.size() - 1));
            // Add points to the main profile, remove the last point, or it will be duplicated later
            mainProfile.addCutPoints(cutProfile.getCutPoints());
            mainProfile.setSource(mainProfile.getCutPoints().get(0));
            mainProfile.setSrcOrientation(orientation);
            mainProfile.setReceiver(mainProfile.getCutPoints().get(mainProfile.getCutPoints().size() - 1));

            // Compute Ray path from vertical cuts (like a folding screen)
            CnossosPath cnossosPath = computeHEdgeDiffraction(mainProfile, data.isBodyBarrier());

            if(cnossosPath == null) {
                // path not valid (ex: reflexion over the wall)
                continue;
            }

            reflexionPathParameters.add(cnossosPath);
        }
        return reflexionPathParameters;
    }

    /**
     * @param geom Geometry
     * @param segmentSizeConstraint Maximal distance between points
     * @return Fixed distance between points
     * @param pts computed points
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
            double targetSegmentSize = geomLength / ceil(geomLength / segmentSizeConstraint);
            Coordinate[] points = geom.getCoordinates();
            double segmentLength = 0.;

            // Mid point of segmented line source
            Coordinate midPoint = null;
            for (int i = 0; i < points.length - 1; i++) {
                Coordinate a = points[i];
                final Coordinate b = points[i + 1];
                double length = a.distance3D(b);
                if (isNaN(length)) {
                    length = a.distance(b);
                }
                while (length + segmentLength > targetSegmentSize) {
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
                    if (isNaN(length)) {
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


    /**
     * Apply a linestring over the digital elevation model by offsetting the z value with the ground elevation.
     * @param lineString
     * @param profileBuilder
     * @param epsilon ignore elevation point where linear interpolation distance is inferior that this value
     * @return computed lineString
     */
    private static LineString splitLineSource(LineString lineString, ProfileBuilder profileBuilder, double epsilon) {
        ArrayList<Coordinate> newGeomCoordinates = new ArrayList<>();
        Coordinate[] coordinates = lineString.getCoordinates();
        for(int idPoint = 0; idPoint < coordinates.length - 1; idPoint++) {
            Coordinate p0 = coordinates[idPoint];
            Coordinate p1 = coordinates[idPoint + 1];
            List<Coordinate> groundProfileCoordinates = new ArrayList<>();
            profileBuilder.fetchTopographicProfile(groundProfileCoordinates, p0, p1, false);
            newGeomCoordinates.ensureCapacity(newGeomCoordinates.size() + groundProfileCoordinates.size());
            if(idPoint == 0) {
                newGeomCoordinates.add(new Coordinate(p0.x, p0.y, p0.z + groundProfileCoordinates.get(0).z));
            }
            Coordinate previous = groundProfileCoordinates.get(0);
            for(int groundPoint = 1; groundPoint < groundProfileCoordinates.size() - 1; groundPoint++) {
                final Coordinate current = groundProfileCoordinates.get(groundPoint);
                final Coordinate next = groundProfileCoordinates.get(groundPoint+1);
                // Do not add topographic points which are simply the linear interpolation between two points
                // triangulation add a lot of interpolated lines from line segment DEM
                if(CGAlgorithms3D.distancePointSegment(current, previous, next) >= epsilon) {
                    // interpolate the Z (height) values of the source then add the altitude
                    previous = current;
                    newGeomCoordinates.add(
                            new Coordinate(current.x, current.y, current.z + Vertex.interpolateZ(current, p0, p1)));
                }
            }
            newGeomCoordinates.add(new Coordinate(p1.x, p1.y, p1.z +
                    groundProfileCoordinates.get(groundProfileCoordinates.size() - 1).z));
        }
        return GEOMETRY_FACTORY.createLineString(newGeomCoordinates.toArray(new Coordinate[0]));
    }

    /**
     * Update ground Z coordinates of sound sources absolute to sea levels
     */
    public void makeSourceRelativeZToAbsolute() {
        List<Geometry> sourceCopy = new ArrayList<>(data.sourceGeometries.size());
        for (Geometry source : data.sourceGeometries) {
            Geometry offsetGeometry = source.copy();
            if(source instanceof LineString) {
                offsetGeometry = splitLineSource((LineString) source, data.profileBuilder, ProfileBuilder.MILLIMETER);
            } else if(source instanceof MultiLineString) {
                LineString[] newGeom = new LineString[source.getNumGeometries()];
                for(int idGeom = 0; idGeom < source.getNumGeometries(); idGeom++) {
                    newGeom[idGeom] = splitLineSource((LineString) source.getGeometryN(idGeom),
                            data.profileBuilder, ProfileBuilder.MILLIMETER);
                }
                offsetGeometry = GEOMETRY_FACTORY.createMultiLineString(newGeom);
            }
            // Offset the geometry with value of elevation for each coordinate
            sourceCopy.add(offsetGeometry);
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
        for(Coordinate receiver : data.receivers) {
            receiver.setZ(receiver.getZ() + data.profileBuilder.getZGround(receiver));
        }
    }


    /**
     * Compute maximal power at freefield at the receiver position with reflective ground
     * @param source
     * @param receiverPos
     * @param sourceId
     * @param sourceList
     * @param wj
     * @param li
     * @param orientation
     * @return
     */
    private static double insertPtSource(Coordinate source, Coordinate receiverPos, Integer sourceId,
                                         List<PointPath.SourcePointInfo> sourceList, double[] wj, double li, Orientation orientation) {
        double aDiv = -getADiv(CGAlgorithms3D.distance(receiverPos, source));
        double[] srcWJ = new double[wj.length];
        for (int idFreq = 0; idFreq < srcWJ.length; idFreq++) {
            srcWJ[idFreq] = wj[idFreq] * li * dbaToW(aDiv) * dbaToW(3);
        }
        sourceList.add(new PointPath.SourcePointInfo(srcWJ, sourceId, source, li, orientation));
        return sumArray(srcWJ.length, srcWJ);
    }


    /**
     *
     * @param source
     * @param receiverPos
     * @param sourceId
     * @param sourceList
     * @param wj
     * @param li
     * @param orientation
     * @return
     */
    private static double insertPtSource(Point source, Coordinate receiverPos, Integer sourceId,
                                         List<PointPath.SourcePointInfo> sourceList, double[] wj, double li, Orientation orientation) {
        // Compute maximal power at freefield at the receiver position with reflective ground
        double aDiv = -getADiv(CGAlgorithms3D.distance(receiverPos, source.getCoordinate()));
        double[] srcWJ = new double[wj.length];
        for (int idFreq = 0; idFreq < srcWJ.length; idFreq++) {
            srcWJ[idFreq] = wj[idFreq] * li * dbaToW(aDiv) * dbaToW(3);
        }
        sourceList.add(new PointPath.SourcePointInfo(srcWJ, sourceId, source.getCoordinate(), li, orientation));
        return sumArray(srcWJ.length, srcWJ);
    }

    /**
     * Compute li to equation 4.1 NMPB 2008 (June 2009)
     * @param source
     * @param receiverCoord
     * @param srcIndex
     * @param sourceList
     * @param wj
     * @return
     */
    private double addLineSource(LineString source, Coordinate receiverCoord, int srcIndex, List<PointPath.SourcePointInfo> sourceList, double[] wj) {
        double totalPowerRemaining = 0;
        ArrayList<Coordinate> pts = new ArrayList<>();
        Coordinate nearestPoint = JTSUtility.getNearestPoint(receiverCoord, source);
        double segmentSizeConstraint = max(1, receiverCoord.distance3D(nearestPoint) / 2.0);
        if (isNaN(segmentSizeConstraint)) {
            segmentSizeConstraint = max(1, receiverCoord.distance(nearestPoint) / 2.0);
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
                Orientation orientation;
                if(data.sourcesPk.size() > srcIndex && data.sourceOrientation.containsKey(data.sourcesPk.get(srcIndex))) {
                    // If the line source already provide an orientation then alter the line orientation
                    orientation = data.sourceOrientation.get(data.sourcesPk.get(srcIndex));
                    orientation = Orientation.fromVector(
                            Orientation.rotate(new Orientation(orientation.yaw, orientation.roll, 0),
                                    v.normalize()), orientation.roll);
                } else {
                    orientation = Orientation.fromVector(Orientation.rotate(new Orientation(0,0,0), v.normalize()), 0);
                }
                totalPowerRemaining += insertPtSource(pt, receiverCoord, srcIndex, sourceList, wj, li, orientation);
            }
        }
        return totalPowerRemaining;
    }

    public enum ComputationSide {LEFT, RIGHT}


}
