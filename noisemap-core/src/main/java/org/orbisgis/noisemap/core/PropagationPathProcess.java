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


import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.orbisgis.noisemap.core.FastObstructionTest.Wall;
import static org.orbisgis.noisemap.core.FastObstructionTest.calcRotationAngleInDegrees;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class PropagationPathProcess implements Runnable {
    private final static double BASE_LVL = 1.; // 0dB lvl
    private final static double ONETHIRD = 1. / 3.;
    private Thread thread;
    private PropagationProcessPathData data;
    private PropagationProcessOut dataOut;
    private int nbfreq;
    private double[] alpha_atmo;
    private double[] freq_lambda;
    private final static Logger LOGGER = LoggerFactory.getLogger(PropagationPathProcess.class);



    private static double GetGlobalLevel(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    public PropagationPathProcess(PropagationProcessPathData data,
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


    /*public double computeELength() {
        return ;
    }

    public double deltaDistance() {
        return ;
    }*/

    public double computeDeltaDiffraction(int idfreq, double eLength, double deltaDistance, double h0) {
        double cprime;
        //C" NMPB 2008 P.33
        //Multiple diffraction
        //CPRIME=( 1+(5*gamma)^2)/((1/3)+(5*gamma)^2)
        double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / eLength, 2);
        double Ch=Math.min(h0*(data.celerity/freq_lambda[idfreq])/250,1);
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
            diffractionAttenuation = 10*Ch * Math
                    .log10(3 + testForm);
        }
        // Limit to 0<=DiffractionAttenuation
        diffractionAttenuation = Math.max(0,
                diffractionAttenuation);

        return  diffractionAttenuation;
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
        return new double[]{deltaDistanceSprimO, deltaDistanceORprim};
    }




    /*private double[] computeCoordprime(DiffractionWithSoilEffetZone diffDataWithSoilEffet, boolean obstructedSourceReceiver) {
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

        rPrim.setOrdinate(0,receiverCoord.x-rPrim.x); // todo check that !!
        sPrim.setOrdinate(0,receiverCoord.x-sPrim.x); // todo check that !!
        rPrim.setOrdinate(2,rPrim.y);
        sPrim.setOrdinate(2,sPrim.y);
        rPrim.setOrdinate(1,receiverCoord.y);
        sPrim.setOrdinate(1,srcCoord.y);

        DiffractionWithSoilEffetZone diffDataWithSoilEffetSprime = data.freeFieldFinder.getPath(receiverCoord, sPrim);
        DiffractionWithSoilEffetZone diffDataWithSoilEffetRprime = data.freeFieldFinder.getPath(rPrim, srcCoord);
        final double DeltaDistanceSp = diffDataWithSoilEffetSprime.getDeltaDistance();
        final double DeltaDistanceRp = diffDataWithSoilEffetRprime.getDeltaDistance();
        final double DeltaDistanceSpfav = diffDataWithSoilEffetSprime.getDeltaDistancefav();
        final double DeltaDistanceRpfav = diffDataWithSoilEffetRprime.getDeltaDistancefav();
        if (obstructedSourceReceiver) {
            return new double[]{DeltaDistanceSp, DeltaDistanceRp, DeltaDistanceSpfav, DeltaDistanceRpfav};
        }else{
            return new double[]{-DeltaDistanceSp, -DeltaDistanceRp, -DeltaDistanceSpfav, -DeltaDistanceRpfav};
        }
    }*/



    /**
     * Compute project Z coordinate between p0 p1 of x,y.
     * @param coordinateWithoutZ coordinate to set the Z value from Z interpolation of line
     * @param line Extract Z values of this segment
     * @return coordinateWithoutZ with Z value computed from line.
     */
    private static Coordinate getProjectedZCoordinate(Coordinate coordinateWithoutZ, LineSegment line) {
        // Z value is the interpolation of source-receiver line
        return new Coordinate(coordinateWithoutZ.x, coordinateWithoutZ.y, Vertex.interpolateZ(
                line.closestPoint(coordinateWithoutZ), line.p0, line.p1));
    }

    /**
     * ISO-9613 p1
     * @param frequency acoustic frequency (Hz)
     * @param temperature Temperative in celsius
     * @param pressure atmospheric pressure (in Pa)
     * @param humidity relative humidity (in %) (0-100)
     * @return Attenuation coefficient dB/KM
     */
    public static double getAlpha(double frequency, double temperature, double pressure, double humidity) {
        return PropagationProcessData.getCoefAttAtmos(frequency, humidity, pressure, temperature + PropagationProcessData.K_0);
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

            double dx = ptpos.x - receiverPos.x;
            double dy = ptpos.y - receiverPos.y;
            double dz = ptpos.z - receiverPos.z;

            double distanceSrcPt = Math.sqrt(dx * dx + dy * dy + dz * dz);// TODO i have change like line 384

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
     * @param AGroundHmin min ASoil
     * @param cel      sound celerity m/s
     * @return ASoil
     */
    private static double getASoil(double zs, double zr, double dp, double gw, int fm, double AGroundHmin, double cel) {
        //NF S 31-133 page 41 c
        double k = 2 * Math.PI * fm / cel;
        //NF S 31-113 page 41 w
        double w = 0.0185 * Math.pow(fm, 2.5) * Math.pow(gw, 2.6) /
                (Math.pow(fm, 1.5) * Math.pow(gw, 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(fm, 0.75) * Math.pow(gw, 1.3) + 1.16 * Math.pow(10, 6));
        //NF S 31-113 page 41 Cf
        double cf = dp * (1 + 3 * w * dp * Math.pow(Math.E, -Math.sqrt(w * dp))) / (1 + w * dp);
        //NF S 31-113 page 41 A sol
        double ASoil = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(dp, 2) *
                (Math.pow(zs, 2) - Math.sqrt(2 * cf / k) * zs + cf / k) * (Math.pow(zr, 2) - Math.sqrt(2 * cf / k) * zr + cf / k));
        ASoil = Math.max(ASoil, AGroundHmin);
        return ASoil;

    }

    /**
     * getAGroundF use equation ASol in NF S 31-133 page 41 to calculate Attenuation(or contribution) Ground Effect
     *
     * @param zs          z of source point
     * @param zr          z of receiver point
     * @param dp          dp in equation
     * @param gw          Gw
     * @param fm          frequency
     * @param AGroundFMin min ASoil
     * @param cel         Sound celerity m/s
     * @return AGroundF
     */
    private static double getAGroundF(double zs, double zr, double dp, double gw, int fm, double AGroundFMin, double cel) {
        // CNOSSOS p89


        //NF S 31-133 page 41 c
        double k = 2 * Math.PI * fm / cel;
        //NF S 31-113 page 41 w
        double w = 0.0185 * Math.pow(fm, 2.5) * Math.pow(gw, 2.6) /
                (Math.pow(fm, 1.5) * Math.pow(gw, 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(fm, 0.75) * Math.pow(gw, 1.3) + 1.16 * Math.pow(10, 6));
        //NF S 31-113 page 41 Cf
        double cf = dp * (1 + 3 * w * dp * Math.pow(Math.E, -Math.sqrt(w * dp))) / (1 + w * dp);
        //NF S 31-113 page 41 A sol
        double AGroundF = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(dp, 2) *
                (Math.pow(zs, 2) - Math.sqrt(2 * cf / k) * zs + cf / k) * (Math.pow(zr, 2) - Math.sqrt(2 * cf / k) * zr + cf / k));
        AGroundF = Math.max(AGroundF, AGroundFMin);
        return AGroundF;

    }


    /**
     * Formulae 7.18 and 7.20
     *
     * @param aSoil        Asol(O,R) or Asol(S,O) (sol mean ground)
     * @param deltaDifPrim Δdif(S,R') if Asol(S,O) is given or Δdif(S', R) if Asol(O,R)
     * @param deltaDif     Δdif(S, R)
     * @return Δsol(S, O) if Asol(S,O) is given or Δsol(O,R) if Asol(O,R) is given
     */
    private double getDeltaSoil(double aSoil, double deltaDifPrim, double deltaDif) {
        return -20 * Math.log10(1 + (Math.pow(10, -aSoil / 20) - 1) * Math.pow(10, -(deltaDifPrim - deltaDif) / 20));
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
    }


    public void runDebug(List<PropagationDebugInfo> debugInfo) {
        try {
            initStructures();

            // Computed sound level of vertices
            dataOut.setVerticesSoundLevel(new double[1]);

            // todo check if db_d as values inside table if false then send error
            Runtime runtime = Runtime.getRuntime();
            int splitCount = runtime.availableProcessors();
            ThreadPool threadManager = new ThreadPool(
                    splitCount,
                    splitCount + 1, Long.MAX_VALUE,
                    TimeUnit.SECONDS);
            int maximumReceiverBatch = (int) Math.ceil(1/ (double) splitCount);
            int endReceiverRange = 0;
            while (endReceiverRange < 1) {
                int newEndReceiver = Math.min(endReceiverRange + maximumReceiverBatch, 1);
                PropagationPathProcess.propagationPathComputation batchThread = new PropagationPathProcess.propagationPathComputation(this);
                //batchThread.run();
                threadManager.executeBlocking(batchThread);
                endReceiverRange = newEndReceiver;
            }
            threadManager.shutdown();
            threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public void run() {
        runDebug(null);
    }

    private static class propagationPathComputation implements Runnable {
        private PropagationPathProcess propagationPathProcess;

        public propagationPathComputation(PropagationPathProcess propagationPathProcess) {
            this.propagationPathProcess = propagationPathProcess;
        }
        @Override
        public void run() {
            double energeticSum[] = new double[propagationPathProcess.data.freq_lvl.size()];
            Arrays.fill(energeticSum, 0d);
            // Save the sound level at this receiver
            // Do the sum of all frequency bands
            double allfreqlvl = 0d;
            for (double anEnergeticSum : energeticSum) {
                allfreqlvl += anEnergeticSum;
            }
            allfreqlvl = Math.max(allfreqlvl, BASE_LVL);
            propagationPathProcess.dataOut.setVerticeSoundLevel(0, allfreqlvl);

        }

    }




    /**
     * Offset de Z coordinates by the height of the ground
     */
    private static final class AbsoluteCoordinateSequenceFilter implements CoordinateSequenceFilter {
        AtomicBoolean geometryChanged = new AtomicBoolean(false);
        FastObstructionTest fastObstructionTest;

        public AbsoluteCoordinateSequenceFilter(FastObstructionTest fastObstructionTest) {
            this.fastObstructionTest = fastObstructionTest;
        }

        @Override
        public void filter(CoordinateSequence coordinateSequence, int i) {
            Coordinate pt = coordinateSequence.getCoordinate(i);
            Double zGround = fastObstructionTest.getHeightAtPosition(pt);
            if(!zGround.isNaN()) {
                pt.setOrdinate(2, zGround + pt.getOrdinate(2));
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
