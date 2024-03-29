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
package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.noise_planet.noisemodelling.pathfinder.utils.GeoJSONDocument;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.noise_planet.noisemodelling.pathfinder.JTSUtility.dist2D;
import static org.noise_planet.noisemodelling.pathfinder.utils.GeometryUtils.projectPointOnSegment;
import static org.noise_planet.noisemodelling.pathfinder.utils.GeometryUtils.projectPointOnVector;

/**
 * PropagationPath
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */

// todo get out all the useless computations and functions
// todo please revise public, private, etc.

public class PropagationPath {
    public static final int FOOTER_RESERVED_SIZE = 120; // reserved size for geojson footer
    private List<ProfileBuilder.CutPoint> cutPoints = new ArrayList<>();
    // given by user
    private SegmentPath srSegment; // list of source-receiver path (including prime path)
    private List<PointPath> pointList; // list of points (source, receiver or diffraction and reflection points)
    private List<SegmentPath> segmentList; // list of segments [S,O1] and [On-1,R] (O1 and On-1 are respectively the first diffraction point and On-1 the last diffration point)
    private boolean favorable; // if true, favorable meteorological condition path
    int idSource;
    int idReceiver;
    private String timePeriod=""; // time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
    Orientation sourceOrientation = new Orientation(0,0,0);
    public Orientation raySourceReceiverDirectivity = new Orientation(); // direction of the source->receiver path relative to the source heading
    public double angle;
    double gs;
    // computed in Augmented Path
    public List<Integer> difHPoints = new ArrayList<Integer>(); // diffraction points indices on horizontal edges
    public List<Integer> difVPoints = new ArrayList<Integer>(); // diffraction points indices on vertical edges
    public List<Integer> refPoints = new ArrayList<Integer>(); // reflection points indices
    public boolean keepAbsorption = false;
    public AbsorptionData absorptionData = new AbsorptionData();
    public GroundAttenuation groundAttenuation = new GroundAttenuation();
    public ReflectionAttenuation reflectionAttenuation = new ReflectionAttenuation();

    public double deltaH = Double.MAX_VALUE;
    public double deltaF= Double.MAX_VALUE;
    public double deltaPrimeH= Double.MAX_VALUE;
    public double deltaPrimeF= Double.MAX_VALUE;
    public double deltaSPrimeRH= Double.MAX_VALUE;
    public double deltaSRPrimeH= Double.MAX_VALUE;
    public ABoundary aBoundaryH = new ABoundary();
    public ABoundary aBoundaryF = new ABoundary();
    public double deltaSPrimeRF= Double.MAX_VALUE;
    public double deltaSRPrimeF= Double.MAX_VALUE;
    public double e=0;
    public double deltaRetroH= Double.MAX_VALUE;
    public double deltaRetroF= Double.MAX_VALUE;

    public static class ABoundary {
        public double[] deltaDiffSR;
        public double[] aGroundSO;
        public double[] aGroundOR;
        public double[] deltaDiffSPrimeR;
        public double[] deltaDiffSRPrime;
        public double[] deltaGroundSO;
        public double[] deltaGroundOR;
        public double[] aDiff;

        private boolean init = false;

        public void init(int freqCount) {
            if(!init) {
                deltaDiffSR = new double[freqCount];
                aGroundSO = new double[freqCount];
                aGroundOR = new double[freqCount];
                deltaDiffSPrimeR = new double[freqCount];
                deltaDiffSRPrime = new double[freqCount];
                deltaGroundSO = new double[freqCount];
                deltaGroundOR = new double[freqCount];
                aDiff = new double[freqCount];
                init = true;
            }
        }
    }

    /**
     * 3D intersections points of the ray
     * @return
     */
    public List<ProfileBuilder.CutPoint> getCutPoints() {
        return cutPoints;
    }

    public void setCutPoints(List<ProfileBuilder.CutPoint> cutPoints) {
        this.cutPoints = cutPoints;
    }

    /**
     * parameters given by user
     * @param favorable
     * @param pointList
     * @param segmentList
     * @param angle         Angle between the 3D source and 3D receiver. Used to rose index.
     */
    public PropagationPath(boolean favorable, List<PointPath> pointList, List<SegmentPath> segmentList , SegmentPath srSegment, double angle) {
        this.favorable = favorable;
        this.pointList = pointList;
        this.segmentList = segmentList;
        this.srSegment = srSegment;
    }

    /**
     * Copy constructor
     * @param other
     */
    public PropagationPath(PropagationPath other) {
        this.srSegment = other.srSegment;
        this.pointList = other.pointList;
        this.segmentList = other.segmentList;
        this.favorable = other.favorable;
        this.idSource = other.idSource;
        this.idReceiver = other.idReceiver;
        this.sourceOrientation = other.sourceOrientation;
        this.raySourceReceiverDirectivity = other.raySourceReceiverDirectivity;
        this.angle = other.angle;
        this.gs = other.gs;
        this.difHPoints = other.difHPoints;
        this.difVPoints = other.difVPoints;
        this.refPoints = other.refPoints;
        this.keepAbsorption = other.keepAbsorption;
        this.absorptionData = new AbsorptionData(other.absorptionData);
        this.groundAttenuation = new GroundAttenuation(other.groundAttenuation);
        this.reflectionAttenuation = other.reflectionAttenuation;
        this.deltaH = other.deltaH;
        this.deltaF = other.deltaF;
        this.deltaPrimeH = other.deltaPrimeH;
        this.deltaPrimeF = other.deltaPrimeF;
        this.deltaSPrimeRH = other.deltaSPrimeRH;
        this.deltaSRPrimeH = other.deltaSRPrimeH;
        this.aBoundaryH = other.aBoundaryH;
        this.aBoundaryF = other.aBoundaryF;
        this.deltaSPrimeRF = other.deltaSPrimeRF;
        this.deltaSRPrimeF = other.deltaSRPrimeF;
        this.e = other.e;
        this.deltaRetroH = other.deltaRetroH;
        this.deltaRetroF = other.deltaRetroF;
        this.cutPoints = new ArrayList<>(other.cutPoints);
        this.timePeriod = other.timePeriod;
    }

    public PropagationPath() {

    }

    /**
     * @return time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public String getTimePeriod() {
        return timePeriod;
    }

    /**
     * @param timePeriod time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Orientation getSourceOrientation() {
        return sourceOrientation;
    }

    public void setSourceOrientation(Orientation sourceOrientation) {
        this.sourceOrientation = sourceOrientation;
    }

    public Orientation getRaySourceReceiverDirectivity() {
        return raySourceReceiverDirectivity;
    }

    public void setRaySourceReceiverDirectivity(Orientation raySourceReceiverDirectivity) {
        this.raySourceReceiverDirectivity = raySourceReceiverDirectivity;
    }

    /**
     * @return Ground factor of the source area. Gs=0 for road platforms, slab tracks. Gs=1 for rail tracks on ballast
     */
    public double getGs() {
        return gs;
    }

    public void setGs(double gs) {
        this.gs = gs;
    }


    /**
     * @return Propagation path as a geometry object
     */
    public LineString asGeom() {
        // try to compute 3d ray geometry using two different list of points (one in 2D and the ground cut points in 3d)
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[pointList.size()];
        int i=0;
        double cutPointDistance = 0;
        int cutPointCursor = 0;
        if(cutPoints.isEmpty() || coordinates.length <= 1) {
            return geometryFactory.createLineString();
        }
        for(PointPath pointPath : pointList) {
            // report x,y from cut point
            while(cutPointCursor < cutPoints.size() - 1) {
                if(pointPath.coordinate.x > cutPointDistance) {
                    cutPointCursor++;
                    cutPointDistance += cutPoints.get(cutPointCursor-1).getCoordinate()
                            .distance(cutPoints.get(cutPointCursor).getCoordinate());
                } else {
                    break;
                }
            }
            Coordinate rayPoint = new Coordinate(cutPoints.get(cutPointCursor).getCoordinate());
            rayPoint.setZ(pointPath.coordinate.y);
            if(cutPointCursor > 0) {
                final Coordinate p0 = cutPoints.get(cutPointCursor - 1).getCoordinate();
                final Coordinate p1 = cutPoints.get(cutPointCursor).getCoordinate();
                double distanceP0P1 = p1.distance(p0);
                // compute ratio of pointPath position between p0 and p1
                double ratio = Math.min(1, Math.max(0, (pointPath.coordinate.x - (cutPointDistance - distanceP0P1)) / distanceP0P1));
                // interpolate x,y coordinates
                rayPoint = new LineSegment(p0, p1).pointAlong(ratio);
                rayPoint.setZ(pointPath.coordinate.y);
            }
            coordinates[i++] = new Coordinate(rayPoint);
        }
        return geometryFactory.createLineString(coordinates);
    }

    public String profileAsJSON(int sizeLimitation) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GeoJSONDocument geoJSONDocument = new GeoJSONDocument(byteArrayOutputStream);
        geoJSONDocument.writeHeader();
        for (ProfileBuilder.CutPoint cutPoint : cutPoints) {
            if(sizeLimitation > 0 && byteArrayOutputStream.size() + FOOTER_RESERVED_SIZE > sizeLimitation) {
                break;
            }
            geoJSONDocument.writeCutPoint(cutPoint);
        }
        geoJSONDocument.writeFooter();
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }

    public int getIdSource() {
        return idSource;
    }

    public void setIdSource(int idSource) {
        this.idSource = idSource;
    }

    public int getIdReceiver() {
        return idReceiver;
    }

    public void setIdReceiver(int idReceiver) {
        this.idReceiver = idReceiver;
    }

    /**
     * Writes the content of this object into <code>out</code>.
     * @param out the stream to write into
     * @throws java.io.IOException if an I/O-error occurs
     */
    /*
    public void writeStream( DataOutputStream out ) throws IOException {
        out.writeBoolean(favorable);
        out.writeInt(idSource);
        out.writeFloat(sourceOrientation.yaw);
        out.writeFloat(sourceOrientation.pitch);
        out.writeFloat(sourceOrientation.roll);
        out.writeFloat((float) gs);
        out.writeInt(idReceiver);
        out.writeInt(pointList.size());
        for(PointPath pointPath : pointList) {
            pointPath.writeStream(out);
        }
        out.writeInt(segmentList.size());
        for(SegmentPath segmentPath : segmentList) {
            segmentPath.writeStream(out);
        }
        srSegment.writeStream(out);
    }
*/
    /**
     * Reads the content of this object from <code>out</code>. All
     * properties should be set to their default value or to the value read
     * from the stream.
     * @param in the stream to read
     * @throws IOException if an I/O-error occurs
     */
    public void readStream( DataInputStream in ) throws IOException {
        favorable = in.readBoolean();
        idSource = in.readInt();
        float bearing = in.readFloat();
        float inclination = in.readFloat();
        float roll = in.readFloat();
        double gs = in.readFloat();
        setGs(gs);
        setSourceOrientation(new Orientation(bearing, inclination, roll));

        idReceiver = in.readInt();
        int pointListSize = in.readInt();
        pointList = new ArrayList<>(pointListSize);
        for(int i=0; i < pointListSize; i++) {
            PointPath pointPath = new PointPath();
            pointPath.readStream(in);
            pointList.add(pointPath);
        }
        int segmentListSize = in.readInt();
        segmentList = new ArrayList<>(segmentListSize);
        for(int i=0; i < segmentListSize; i++) {
            SegmentPath segmentPath = new SegmentPath();
            segmentPath.readStream(in);
            segmentList.add(segmentPath);
        }
        SegmentPath srSegment = new SegmentPath();
        srSegment.readStream(in);
    }

    public List<PointPath> getPointList() {return pointList;}

    public List<SegmentPath> getSegmentList() {return segmentList;}

    public SegmentPath getSRSegment() {return srSegment;}

    public void setPointList(List<PointPath> pointList) {this.pointList = pointList;}

    public void setSegmentList(List<SegmentPath>  segmentList) {this.segmentList = segmentList;}

    public void setSRSegment(SegmentPath srSegment) {this.srSegment = srSegment;}


    public PropagationPath(List<SegmentPath> segmentList) {
        this.segmentList = segmentList;
    }

    public boolean isFavorable() {
        return favorable;
    }

    public void setFavorable(boolean favorable) {
        this.favorable =  favorable;
    }

    double computeZs(SegmentPath segmentPath) {
        double zs = pointList.get(segmentPath.idPtStart).coordinate.z - projectPointOnSegment(pointList.get(segmentPath.idPtStart).coordinate,segmentPath.meanGdPlane,segmentPath.pInit).z;
        return ((zs > 0) ? zs : 0); // Section 2.5.3 - If the equivalent height of a point becomes negative, i.e. if the point is located below the mean ground plane, a null height is retained, and the equivalent point is then identical with its possible image.
    }

    public double computeZr(SegmentPath segmentPath) {
        double zr = pointList.get(segmentPath.idPtFinal).coordinate.z - projectPointOnSegment(pointList.get(segmentPath.idPtFinal).coordinate,segmentPath.meanGdPlane,segmentPath.pInit).z;
        return ((zr > 0) ? zr : 0); // Section 2.5.3 - If the equivalent height of a point becomes negative, i.e. if the point is located below the mean ground plane, a null height is retained, and the equivalent point is then identical with its possible image.
    }

    /**
     * Eq 2.5.19
     * @param segmentPath
     * @return
     */
    public double computeZsPrime(SegmentPath segmentPath) {
        // The height corrections deltazs and deltazr convey the effect of the sound ray bending. deltazT accounts for the effect of the turbulence.
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zsH + segmentPath.zrH);
        double deltazs = alpha0 * Math.pow((segmentPath.zsH / (segmentPath.zsH + segmentPath.zrH)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zsH + deltazs + deltazt;
    }

    /**
     * Eq 2.5.19
     * @param segmentPath
     * @return
     */
    public double computeZrPrime(SegmentPath segmentPath) {
        // The height corrections deltazs and deltazr convey the effect of the sound ray bending. deltazT accounts for the effect of the turbulence.
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zsH + segmentPath.zrH);
        double deltazr = alpha0 * Math.pow((segmentPath.zrH / (segmentPath.zsH + segmentPath.zrH)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zrH + deltazr + deltazt;
    }

    /**
     * Eq.2.5.24 and Eq. 2.5.25
     * @param dSeg
     * @param d
     * @return
     */
    private double getRayCurveLength(double dSeg,double d) {
        double gamma = Math.max(1000,8*d); // Eq. 2.5.24
        return 2*gamma*Math.asin(dSeg/(2*gamma)); // Eq. 2.5.25

    }

    public static void writeCoordinate(DataOutputStream out, Coordinate p) throws IOException {
        out.writeDouble(p.x);
        out.writeDouble(p.y);
        out.writeDouble(p.z);
    }

    public static Coordinate readCoordinate(DataInputStream in) throws IOException {
        return new Coordinate(in.readDouble(), in.readDouble(), in.readDouble());
    }

    public static void writeVector(DataOutputStream out, Vector3D p) throws IOException {
        out.writeDouble(p.getX());
        out.writeDouble(p.getY());
        out.writeDouble(p.getZ());
    }

    public static Vector3D readVector(DataInputStream in) throws IOException {
        return new Vector3D(in.readDouble(), in.readDouble(), in.readDouble());
    }

    /**
     * Writes the content of this object into <code>out</code>.
     * @param out the stream to write into
     * @throws java.io.IOException if an I/O-error occurs
     */
    /*
    public static void writePropagationPathListStream( DataOutputStream out, List<PropagationPath> propagationPaths ) throws IOException {
        out.writeInt(propagationPaths.size());
        for(PropagationPath propagationPath : propagationPaths) {
            propagationPath.writeStream(out);
        }
    }*/

    /**
     * Reads the content of this object from <code>out</code>. All
     * properties should be set to their default value or to the value read
     * from the stream.
     * @param in the stream to read
     * @throws IOException if an I/O-error occurs
     */
    public static void readPropagationPathListStream( DataInputStream in , ArrayList<PropagationPath> propagationPaths) throws IOException {
        int propagationPathsListSize = in.readInt();
        propagationPaths.ensureCapacity(propagationPathsListSize);
        for(int i=0; i < propagationPathsListSize; i++) {
            PropagationPath propagationPath = new PropagationPath();
            propagationPath.readStream(in);
            propagationPaths.add(propagationPath);
        }
    }

    //Following classes are use for testing purpose
    public static class AbsorptionData {
        public double[] aAtm = new double[0];
        public double[] aDiv = new double[0];
        public double[] aRef = new double[0];
        public double[] aBoundaryH = new double[0];
        public double[] aBoundaryF = new double[0];
        public double[] aGlobalH = new double[0];
        public double[] aGlobalF = new double[0];
        public double[] aDifH = new double[0];
        public double[] aDifF = new double[0];
        public double[] aGlobal = new double[0];
        public double[] aSource = new double[0]; // directivity attenuation

        public void init(int size) {
            aAtm = new double[size];
            aDiv = new double[size];
            aRef = new double[size];
            aBoundaryH = new double[size];
            aBoundaryF = new double[size];
            aGlobalH = new double[size];
            aGlobalF = new double[size];
            aDifH = new double[size];
            aDifF = new double[size];
            aGlobal = new double[size];
            aSource = new double[size];
        }

        public AbsorptionData() {
        }

        public AbsorptionData(AbsorptionData other) {
            this.aAtm = other.aAtm.clone();
            this.aDiv = other.aDiv.clone();
            this.aRef = other.aRef.clone();
            this.aBoundaryH = other.aBoundaryH.clone();
            this.aBoundaryF = other.aBoundaryF.clone();
            this.aGlobalH = other.aGlobalH.clone();
            this.aGlobalF = other.aGlobalF.clone();
            this.aDifH = other.aDifH.clone();
            this.aDifF = other.aDifF.clone();
            this.aGlobal = other.aGlobal.clone();
            this.aSource = other.aSource.clone();
        }
    }

    public static class GroundAttenuation {
        public double[] wH;
        public double[] cfH;
        public double[] aGroundH;
        public double[] wF;
        public double[] cfF;
        public double[] aGroundF;

        public void init(int size) {
            wH = new double[size];
            cfH = new double[size];
            aGroundH = new double[size];
            wF = new double[size];
            cfF = new double[size];
            aGroundF = new double[size];
        }

        public GroundAttenuation() {
        }

        public GroundAttenuation(GroundAttenuation other) {
            this.wH = other.wH;
            this.cfH = other.cfH;
            this.aGroundH = other.aGroundH;
            this.wF = other.wF;
            this.cfF = other.cfF;
            this.aGroundF = other.aGroundF;
        }
    }

    public static class ReflectionAttenuation {
        public double[] dLRetro;
        public double[] dLAbs;

        public void init(int size) {
            dLRetro = new double[size];
            dLAbs = new double[size];
        }
    }
}
