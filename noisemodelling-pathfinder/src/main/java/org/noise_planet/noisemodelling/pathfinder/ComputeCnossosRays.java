/*
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

import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.algorithm.RectangleLineIntersector;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.math.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.noise_planet.noisemodelling.pathfinder.ComputeRays.splitLineStringIntoPoints;
import static org.noise_planet.noisemodelling.pathfinder.ComputeRays.sumArray;
import static org.noise_planet.noisemodelling.pathfinder.Utils.dbaToW;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticPropagation.getADiv;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeCnossosRays {
    private final static Logger LOGGER = LoggerFactory.getLogger(ComputeCnossosRays.class);

    private int threadCount;

    public ComputeCnossosRays() {
        Runtime runtime = Runtime.getRuntime();
        this.threadCount = Runtime.getRuntime().availableProcessors();
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    //TODO maybe keepRays can be moved inside CnossosPropagationData
    public void run(CnossosPropagationData data, IComputeRaysOut computeRaysOut) {
        ProgressVisitor progressVisitor = data.cellProg;
        int splitCount = threadCount;
        ThreadPool threadManager = new ThreadPool(splitCount, splitCount + 1, Long.MAX_VALUE, TimeUnit.SECONDS);
        int maximumReceiverBatch = (int) Math.ceil(data.receivers.size() / (double) splitCount);
        int endReceiverRange = 0;
        while (endReceiverRange < data.receivers.size()) {
            if (progressVisitor != null && progressVisitor.isCanceled()) {
                break;
            }
            int newEndReceiver = Math.min(endReceiverRange + maximumReceiverBatch, data.receivers.size());
            RangeReceiversComputation batchThread = new RangeReceiversComputation(endReceiverRange,
                    newEndReceiver, this, progressVisitor,
                    computeRaysOut.subProcess(endReceiverRange, newEndReceiver), data);
            if (threadCount != 1) {
                threadManager.executeBlocking(batchThread);
            } else {
                batchThread.run();
            }
            endReceiverRange = newEndReceiver;
        }
        threadManager.shutdown();
        try {
            threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    public PropagationPath computeFreefield(Coordinate rcvCoord, Coordinate srcCoord, ProfileBuilder.CutProfile cutProfile, CnossosPropagationData data) {
        List<PointPath> points = new ArrayList<>();
        List<SegmentPath> segments = new ArrayList<>();

        Coordinate projReceiver;
        Coordinate projSource;

        List<ProfileBuilder.CutPoint> cutPoints;
        if(cutProfile == null) {
            ProfileBuilder.CutPoint src = new ProfileBuilder.CutPoint(srcCoord, ProfileBuilder.IntersectionType.SOURCE, -1);
            ProfileBuilder.CutPoint rcv = new ProfileBuilder.CutPoint(rcvCoord, ProfileBuilder.IntersectionType.RECEIVER, -1);
            cutPoints = Arrays.asList(src, rcv);
        }
        else {
            cutPoints = cutProfile.getCutPoints();
        }
        double totRSDistance = 0;
        double length = 0;
        for (int i = 0; i < cutPoints.size()-1; i++) {
            ProfileBuilder.CutPoint pt0 = cutPoints.get(i);
            ProfileBuilder.CutPoint pt1 = cutPoints.get(i+1);
            LineSegment segment = new LineSegment(pt0.getCoordinate(), pt1.getCoordinate());
            length += segment.getLength();
            totRSDistance += segment.getLength() * pt0.getGroundCoef();
        }
        // Compute GPath using 2D Length
        double gPath = totRSDistance / length;
        List<Coordinate> rSground = cutPoints.stream().map(ProfileBuilder.CutPoint::getCoordinate).collect(Collectors.toList());
        double angle = new LineSegment(rSground.get(0), rSground.get(rSground.size() - 1)).angle();
        rSground = JTSUtility.getNewCoordinateSystem(rSground);

        // Compute mean ground plan
        double[] ab = JTSUtility.getMeanPlaneCoefficients(rSground.toArray(new Coordinate[rSground.size()]));
        Coordinate pInit;
        Coordinate rotatedReceiver = new Coordinate(rSground.get(rSground.size() - 1));
        rotatedReceiver.setOrdinate(1, rcvCoord.z);
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

        points.add(new PointPath(srcCoord, srcCoord.z, data.gS, new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(rcvCoord, rcvCoord.z, data.gS, new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));

        return new PropagationPath(false, points, segments, segments);
    }

    public List<PropagationPath> directPath(Coordinate srcCoord, int srcId,
                                            Coordinate rcvCoord, int rcvId,
                                            CnossosPropagationData data) {
        List<PropagationPath> propagationPaths = new ArrayList<>();
        ProfileBuilder.CutProfile cutProfile = data.profileBuilder.getProfile(srcCoord, rcvCoord);
        boolean freeField = !cutProfile.intersectBuilding() && !cutProfile.intersectTopography();
        boolean topographyHideReceiver = !freeField && cutProfile.intersectTopography() || cutProfile.intersectBuilding();

        if(freeField) {
            propagationPaths.add(computeFreefield(rcvCoord, srcCoord, cutProfile, data));
        }

        if(!freeField && data.isComputeVerticalDiffraction()) {
            PropagationPath propagationPath3 = computeFreefield(rcvCoord, srcCoord, cutProfile, data);
            PropagationPath propagationPath = computeHorizontalEdgeDiffraction(topographyHideReceiver, rcvCoord, srcCoord, cutProfile, data);
            propagationPath.getSRList().addAll(propagationPath3.getSRList());
            propagationPaths.add(propagationPath);
        }

        for(PropagationPath propagationPath : propagationPaths) {
            propagationPath.idSource = srcId;
            propagationPath.idReceiver = rcvId;
        }

        return propagationPaths;
    }

    public PropagationPath computeHorizontalEdgeDiffraction(boolean obstructedSourceReceiver, Coordinate rcvCoord,
                                                            Coordinate srcCoord, ProfileBuilder.CutProfile cutProfile,
                                                            CnossosPropagationData data) {
        List<PointPath> points = new ArrayList<>();
        List<SegmentPath> segments = new ArrayList<>();
        List<SegmentPath> srPath = new ArrayList<>();

        if (!obstructedSourceReceiver) {
            PropagationPath propagationPath = computeFreefield(rcvCoord, srcCoord, data.profileBuilder.getProfile(srcCoord, rcvCoord), data);
            points.addAll(propagationPath.getPointList());
            segments.addAll(propagationPath.getSegmentList());
            srPath.addAll(propagationPath.getSRList());
        } else {
            List<Coordinate> oldPoints = cutProfile.getCutPoints().stream()
                    .filter(cutPoint -> cutPoint.getType() != ProfileBuilder.IntersectionType.GROUND_EFFECT)
                    .map(ProfileBuilder.CutPoint::getCoordinate)
                    .collect(Collectors.toList());
            Collections.reverse(oldPoints);
            List<Coordinate> newPoints = JTSUtility.getNewCoordinateSystem(oldPoints);
            List<Coordinate> upperHull = JTSUtility.getXAscendingHullPoints(newPoints.toArray(new Coordinate[0]));
            LinkedList<LineSegment> path = new LinkedList<>();
            for (int i = 0; i < upperHull.size() - 1; i++) {
                path.add(new LineSegment(upperHull.get(i), upperHull.get(i+1)));
            }
            List<Coordinate> offsetPath = new ArrayList<>();
            Coordinate coordinate;
            double angle = new LineSegment(srcCoord, rcvCoord).angle();
            for (int i =0; i<path.size();i++) {
                coordinate = JTSUtility.getOldCoordinateSystem(path.get(i).p0, angle);
                coordinate.x = rcvCoord.x-coordinate.x;
                coordinate.y = rcvCoord.y-coordinate.y;
                offsetPath.add(coordinate);
            }
            coordinate = JTSUtility.getOldCoordinateSystem(path.get(path.size()-1).p1, angle);
            coordinate.x = rcvCoord.x-coordinate.x;
            coordinate.y = rcvCoord.y-coordinate.y;
            offsetPath.add(coordinate);
            // Offset Coordinates by epsilon
            for (int i = 1; i < offsetPath.size() - 1; i++) {
                Coordinate dest = offsetPath.get(i);
                Vector2D v = new Vector2D(offsetPath.get(0), dest).normalize().multiply(FastObstructionTest.epsilon);
                offsetPath.set(i, new Coordinate(dest.x - v.getX(), dest.y - v.getY(), dest.z));
            }
            for (int j = offsetPath.size() - 1; j > 1; j--) {
                PropagationPath propagationPath1 = computeFreefield(offsetPath.get(j - 1), offsetPath.get(j), data.profileBuilder.getProfile(offsetPath.get(j - 1), offsetPath.get(j)), data);
                propagationPath1.getPointList().get(1).setType(PointPath.POINT_TYPE.DIFH);
                if (j == offsetPath.size() - 1) {
                    propagationPath1.getPointList().get(0).setCoordinate(offsetPath.get(j));
                    points.add(propagationPath1.getPointList().get(0));
                }
                points.add(propagationPath1.getPointList().get(1));
                segments.addAll(propagationPath1.getSegmentList());
            }

            PropagationPath propagationPath2 = computeFreefield(offsetPath.get(0), offsetPath.get(1), data.profileBuilder.getProfile(offsetPath.get(0), offsetPath.get(1)), data);
            points.add(propagationPath2.getPointList().get(1));
            segments.add(propagationPath2.getSegmentList().get(0));
        }
        return new PropagationPath(true, points, segments, srPath);
    }

    private double[] receiverSourcePropa(Coordinate srcCoord, int srcId, double sourceLi,
                                         Coordinate receiverCoord, int rcvId,
                                         IComputeRaysOut dataOut, CnossosPropagationData data) {

        List<PropagationPath> propagationPaths;
        double PropaDistance = srcCoord.distance(receiverCoord);
        if (PropaDistance < data.maxSrcDist) {
            propagationPaths = directPath(srcCoord, srcId, receiverCoord, rcvId, data);
            if (propagationPaths.size() > 0) {
                return dataOut.addPropagationPaths(srcId, sourceLi, rcvId, propagationPaths);
            }
        }
        return new double[0];
    }

    private static double insertPtSource(Coordinate receiverPos, Coordinate ptpos, double[] wj, double li, Integer sourceId, List<SourcePointInfo> sourceList) {
        // Compute maximal power at freefield at the receiver position with reflective ground
        double aDiv = -getADiv(CGAlgorithms3D.distance(receiverPos, ptpos));
        double[] srcWJ = new double[wj.length];
        for (int idFreq = 0; idFreq < srcWJ.length; idFreq++) {
            srcWJ[idFreq] = wj[idFreq] * li * dbaToW(aDiv) * dbaToW(3);
        }
        sourceList.add(new SourcePointInfo(srcWJ, sourceId, ptpos, li));
        return ComputeRays.sumArray(srcWJ.length, srcWJ);
    }

    private double addLineSource(LineString source, Coordinate receiverCoord, int srcIndex, List<SourcePointInfo> sourceList, double[] wj, CnossosPropagationData data) {
        double totalPowerRemaining = 0;
        ArrayList<Coordinate> pts = new ArrayList<Coordinate>();
        // Compute li to equation 4.1 NMPB 2008 (June 2009)
        Coordinate nearestPoint = JTSUtility.getNearestPoint(receiverCoord, source);
        double segmentSizeConstraint = Math.max(1, receiverCoord.distance3D(nearestPoint) / 2.0);
        if (Double.isNaN(segmentSizeConstraint)) {
            segmentSizeConstraint = Math.max(1, receiverCoord.distance(nearestPoint) / 2.0);
        }
        double li = splitLineStringIntoPoints(source, segmentSizeConstraint, pts);
        for (Coordinate pt : pts) {
            if (pt.distance(receiverCoord) < data.maxSrcDist) {
                totalPowerRemaining += insertPtSource(receiverCoord, pt, wj, li, srcIndex, sourceList);
            }
        }
        return totalPowerRemaining;
    }

    public void computeRaysAtPosition(Coordinate receiverCoord, int idReceiver, CnossosPropagationData data, IComputeRaysOut dataOut, ProgressVisitor progressVisitor) {
        //Compute the source search area
        double searchSourceDistance = data.maxSrcDist;
        Envelope receiverSourceRegion = new Envelope(receiverCoord.x
                - searchSourceDistance, receiverCoord.x + searchSourceDistance,
                receiverCoord.y - searchSourceDistance, receiverCoord.y
                + searchSourceDistance
        );
        Iterator<Integer> regionSourcesLst = data.sourcesIndex.query(receiverSourceRegion);
        List<SourcePointInfo> sourceList = new ArrayList<>();
        // Sum of all sources power using only geometric dispersion with direct field
        //double totalPowerRemaining = 0;
        HashSet<Integer> processedLineSources = new HashSet<>(); //Already processed Raw source (line and/or points)
        while (regionSourcesLst.hasNext()) {
            Integer srcIndex = regionSourcesLst.next();
            if (!processedLineSources.contains(srcIndex)) {
                processedLineSources.add(srcIndex);
                Geometry source = data.sourceGeometries.get(srcIndex);
                double[] wj = data.getMaximalSourcePower(srcIndex);
                if (source instanceof Point) {
                    Coordinate ptpos = source.getCoordinate();
                    if (ptpos.distance(receiverCoord) < data.maxSrcDist) {
                        insertPtSource(receiverCoord, ptpos, wj, 1., srcIndex, sourceList);
                    }
                } else if (source instanceof LineString) {
                    addLineSource((LineString) source, receiverCoord, srcIndex, sourceList, wj, data);
                } else if (source instanceof MultiLineString) {
                    for (int id = 0; id < source.getNumGeometries(); id++) {
                        Geometry subGeom = source.getGeometryN(id);
                        if (subGeom instanceof LineString) {
                            addLineSource((LineString) subGeom, receiverCoord, srcIndex, sourceList, wj, data);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Sound source %s geometry are not supported", source.getGeometryType()));
                }
            }
        }
        // Sort sources by power contribution descending
        Collections.sort(sourceList);
        double powerAtSource = 0;
        for (SourcePointInfo src : sourceList) {
            // For each Pt Source - Pt Receiver
            Coordinate srcCoord = src.position;
            receiverSourcePropa(srcCoord, src.sourcePrimaryKey, src.li, receiverCoord, idReceiver, dataOut, data);

            // If the delta between already received power and maximal potential power received is inferior than than data.maximumError
            if ((progressVisitor != null && progressVisitor.isCanceled())) {
                break; //Stop looking for more rays
            }
        }
        // No more rays for this receiver
        dataOut.finalizeReceiver(idReceiver);
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
    private final int startReceiver; // Included
    private final int endReceiver; // Excluded
    private final ComputeCnossosRays propagationProcess;
    private final ProgressVisitor progressVisitor;
    private final IComputeRaysOut dataOut;
    private final CnossosPropagationData data;

    public RangeReceiversComputation(int startReceiver, int endReceiver, ComputeCnossosRays propagationProcess,
                                     ProgressVisitor progressVisitor, IComputeRaysOut dataOut,
                                     CnossosPropagationData data) {
        this.startReceiver = startReceiver;
        this.endReceiver = endReceiver;
        this.propagationProcess = propagationProcess;
        this.progressVisitor = progressVisitor;
        this.dataOut = dataOut;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            for (int idReceiver = startReceiver; idReceiver < endReceiver; idReceiver++) {
                if (progressVisitor != null) {
                    if (progressVisitor.isCanceled()) {
                        break;
                    }
                }
                Coordinate receiverCoord = data.receivers.get(idReceiver);

                propagationProcess.computeRaysAtPosition(receiverCoord, idReceiver, data, dataOut, progressVisitor);

                if (progressVisitor != null) {
                    progressVisitor.endStep();
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
            if (progressVisitor != null) {
                progressVisitor.cancel();
            }
            throw ex;
        }
    }
}

private static final class SourcePointInfo implements Comparable<SourcePointInfo> {
    private double[] wj;
    private double li; //
    private int sourcePrimaryKey;
    private Coordinate position;
    private double globalWj;

    /**
     * @param wj               Maximum received power from this source
     * @param sourcePrimaryKey
     * @param position
     */
    public SourcePointInfo(double[] wj, int sourcePrimaryKey, Coordinate position, double li) {
        this.wj = wj;
        this.sourcePrimaryKey = sourcePrimaryKey;
        this.position = position;
        if (Double.isNaN(position.z)) {
            this.position = new Coordinate(position.x, position.y, 0);
        }
        this.globalWj = sumArray(wj.length, wj);
        this.li = li;
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

    public void setWj(double[] wj) {
        this.wj = wj;
        this.globalWj = sumArray(wj.length, wj);
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
}
