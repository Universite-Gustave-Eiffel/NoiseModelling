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
    Orientation sourceOrientation = new Orientation(0,0,0);
    public Orientation raySourceReceiverDirectivity = new Orientation(); // direction of the source->receiver path relative to the source heading
    public double angle;
    double gs;
    private boolean initialized = false;
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
        this.initialized = other.initialized;
        this.difHPoints = other.difHPoints;
        this.difVPoints = other.difVPoints;
        this.refPoints = other.refPoints;
        this.keepAbsorption = other.keepAbsorption;
        this.absorptionData = other.absorptionData;
        this.groundAttenuation = other.groundAttenuation;
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
    }

    public PropagationPath() {

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
        if(cutPoints.isEmpty()) {
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
                // interpolate coordinates
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


    public boolean isInitialized() {
        return initialized;
    }

    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
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
        setInitialized(false);
    }

    /**
     * Initialise the propagation path
     */
    public void initPropagationPath() {
        if(!isInitialized()) {
            //computeAugmentedPath();
            //computeAugmentedSegments();
            //computeAugmentedSRPath();
            setInitialized(true);
        }
    }

    /**
     * Initialise all values that depends of the global path
     * as distances, Gpath, etc.
     */
    public void computeAugmentedSRPath() {
        double dPath =0 ;

        srSegment.idPtStart = 0;
        srSegment.idPtFinal = pointList.size()-1;

        // In case of reflections the slanted path passes through the image sources.
        if (refPoints.size()>0){
            Coordinate ini = srSegment.s;
            Coordinate iniGround = srSegment.sMeanPlane;
            srSegment.d =0.;
            srSegment.dp = 0.;
            for (int idPoint = 1; idPoint < pointList.size(); idPoint++) {
                if (pointList.get(idPoint).type == PointPath.POINT_TYPE.REFL){
                    srSegment.d += CGAlgorithms3D.distance(ini, pointList.get(idPoint).coordinate);
                    ini = pointList.get(idPoint).coordinate;

                    srSegment.dp += CGAlgorithms3D.distance(iniGround, projectPointOnVector(pointList.get(idPoint).coordinate,srSegment.meanGdPlane,srSegment.pInit));
                    iniGround = projectPointOnVector(pointList.get(idPoint).coordinate,srSegment.meanGdPlane,srSegment.pInit);
                }
            }
            srSegment.d  += CGAlgorithms3D.distance(ini, srSegment.r);
            srSegment.dp += CGAlgorithms3D.distance(iniGround, srSegment.rMeanPlane);
        }

        srSegment.dc = (favorable) ? getRayCurveLength(srSegment.d,srSegment.d): srSegment.d;
        srSegment.dPath = srSegment.d;
        if (difVPoints.size()>0) {
            double gPath = 0;
            double dpSegments = 0;

            for (int idSegment = 0; idSegment < segmentList.size(); idSegment++) {
                gPath += segmentList.get(idSegment).gPath*segmentList.get(idSegment).dp;
                dpSegments += segmentList.get(idSegment).dp;
            }


            for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                dPath += CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate);
            }

            if (pointList.size()>3){
                srSegment.eLength = dPath;
            }
            srSegment.dPath = dPath
                    + CGAlgorithms3D.distance(srSegment.s, pointList.get(1).coordinate)
                    + CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate, srSegment.r);
            srSegment.dc = srSegment.d;

            double convex = 1; // if path is convex, delta is positive, otherwise negative

            srSegment.gPath = gPath/srSegment.dPath;

            srSegment.dp = srSegment.dPath;

            // todo handle with unconvex path
            //if (Vector3D.dot(S,R,S,pointList.get(difVPoints.get(0)).coordinate)<0){convex = -1;}
            srSegment.delta = convex * (srSegment.dPath - srSegment.d);
        }

        // diffraction on horizontal edges
        if (difHPoints.size()>0) {

            dPath = 0;

            // Symmetric coordinates to the gound mean plane see Figure 2.5.c
            Coordinate SGroundSeg = this.segmentList.get(0).sMeanPlane;
            Coordinate RGroundSeg = this.segmentList.get(segmentList.size()-1).rMeanPlane;
            Coordinate Sprime = new Coordinate(2 * SGroundSeg.x - srSegment.s.x, 2 * SGroundSeg.y - srSegment.s.y, 2 * SGroundSeg.z - srSegment.s.z);
            Coordinate Rprime = new Coordinate(2 * RGroundSeg.x - srSegment.r.x, 2 * RGroundSeg.y - srSegment.r.y, 2 * RGroundSeg.z - srSegment.r.z);

            double gpath = srSegment.gPath;
            SegmentPath SRp = new SegmentPath(gpath, new Vector3D(srSegment.s, Rprime),srSegment.pInit);
            SegmentPath SpR = new SegmentPath(gpath, new Vector3D(Sprime, srSegment.r),Sprime);

            SpR.d = dist2D(Sprime, srSegment.r);
            SRp.d = dist2D(srSegment.s, Rprime);

            SRp.dp = srSegment.dp;
            SpR.dp = srSegment.dp;

            if (!this.favorable){
                for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                    dPath += dist2D(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate);
                }

                if (pointList.size()>3){
                    srSegment.eLength = dPath;
                    SpR.eLength = dPath;
                    SRp.eLength = dPath;
                }
                srSegment.dPath = dPath
                        + dist2D(srSegment.s, pointList.get(1).coordinate)
                        + dist2D(pointList.get(pointList.size()-2).coordinate,srSegment.r);
                SpR.dPath = dPath
                        + dist2D(Sprime, pointList.get(1).coordinate)
                        + dist2D(pointList.get(pointList.size()-2).coordinate,srSegment.r);
                SRp.dPath = dPath
                        + dist2D(srSegment.s, pointList.get(1).coordinate)
                        + dist2D(pointList.get(pointList.size()-2).coordinate, Rprime);

                SpR.dc = SpR.d;
                SRp.dc = SRp.d;
                srSegment.dc = srSegment.d;

                // if path is convex, delta is positive, otherwise negative
                double convex = Vector3D.dot(srSegment.s, srSegment.r, srSegment.s, pointList.get(difHPoints.get(0)).coordinate)<0 ? -1 : 1;

                srSegment.delta = convex * (srSegment.dPath - srSegment.d);
                SRp.delta = convex * (SRp.dPath - SRp.d);
                SpR.delta = convex * (SpR.dPath - SpR.d);
            }
            else
            {

                // if the straight sound ray SR is masked by the obstacle (1st and 2nd case in Figure 2.5.e)
                for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                    dPath += getRayCurveLength(CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate), srSegment.d);
                }

                if (difHPoints.size()>1){
                    double dDif = CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate,pointList.get(difHPoints.get(difHPoints.size()-1)).coordinate);
                    srSegment.eLength = getRayCurveLength(dDif,srSegment.d);
                    SpR.eLength = srSegment.eLength;
                    SRp.eLength = srSegment.eLength;
                }

                srSegment.dPath = dPath
                        + getRayCurveLength(CGAlgorithms3D.distance(srSegment.s, pointList.get(1).coordinate), srSegment.d)
                        + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate, srSegment.r), srSegment.d);
                srSegment.dc = getRayCurveLength(srSegment.d, srSegment.d);

                if (difHPoints.size()>0) {
                    SpR.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(1).coordinate), srSegment.d)
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, srSegment.r), srSegment.d);
                    SpR.dc = getRayCurveLength(SpR.d, srSegment.d);

                    SRp.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(srSegment.s, pointList.get(1).coordinate), srSegment.d)
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, Rprime), srSegment.d);
                    SRp.dc = getRayCurveLength(SRp.d, srSegment.d);
                }

                // todo for the multiple diffractions in favourable conditions: Eq. 2.5.28

                // Iif the straight sound ray SR is not masked by the obstacle (3rd case in Figure 2.5.e)
                if (Vector3D.dot(srSegment.s, srSegment.r , srSegment.s, pointList.get(difHPoints.get(0)).coordinate)<0) {
                    Coordinate A = projectPointOnVector(pointList.get(difHPoints.get(0)).coordinate,srSegment.meanGdPlane, srSegment.pInit);
                    double SA = getRayCurveLength(CGAlgorithms3D.distance(srSegment.s, A), srSegment.d);
                    double AR = getRayCurveLength(CGAlgorithms3D.distance(A, srSegment.r), srSegment.d);
                    double SO = getRayCurveLength(CGAlgorithms3D.distance(srSegment.s, pointList.get(difHPoints.get(0)).coordinate), srSegment.d);
                    double OR = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, srSegment.r), srSegment.d);
                    double SpA = getRayCurveLength(CGAlgorithms3D.distance(Sprime, A), srSegment.d);
                    double ARp = getRayCurveLength(CGAlgorithms3D.distance(A, Rprime), srSegment.d);
                    double SpO = getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(difHPoints.get(0)).coordinate), srSegment.d);
                    double ORp = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, Rprime), srSegment.d);
                    srSegment.delta =  2*SA+2*AR-SO-OR-srSegment.dc; // Eq. 2.5.27
                    SRp.delta =  2*SA+2*ARp-SO-ORp-SRp.dc;
                    SpR.delta = 2*SpA+2*AR-SpO-OR-SpR.dc;
                }else {
                    srSegment.delta =  srSegment.dPath - srSegment.dc; // Eq. 2.5.26
                    SRp.delta = SRp.dPath - SRp.dc;
                    SpR.delta = SpR.dPath - SpR.dc;
                }
            }
            //this.srList.add(SpR);
            //this.srList.add(SRp);
        }

        // see Point 5.3 Equivalent heights in AFNOR document
        if (srSegment.zsH <=0){srSegment.zsH = 0.000000001;}
        if (srSegment.zrH <=0){srSegment.zrH = 0.000000001;}


        double testForm = srSegment.dp / (30 * (srSegment.zsH + srSegment.zrH)); // if <= 1, then the distinction between the type of ground located near the source and the type of ground located near the receiver is negligible.
        srSegment.testFormH = testForm;

        double gPathPrime;

        // if dp <= 30(zs + zr), then the distinction between the type of ground located near the source and the type of ground located near the receiver is negligible.
        // Eq. 2.5.14
        if (testForm <= 1) {
            srSegment.gPathPrime = testForm * srSegment.gPath + (1 - testForm) * getGs();
        } else {
            srSegment.gPathPrime = srSegment.gPath;
        }

        //this.srList.set(0,SR);

        // Compute PRIME zs, zr and testForm
        double zsPrime= srSegment.getZsPrime(this, srSegment);
        double zrPrime = srSegment.getZrPrime(this, srSegment);

        srSegment.testFormF = srSegment.dp / (30 * (zsPrime + zrPrime));
    }

/*
    void computeAugmentedSegments() {
        for (int idSegment = 0; idSegment < segmentList.size(); idSegment++) {

            SegmentPath seg = segmentList.get(idSegment);

            seg.idPtStart = idSegment;
            seg.idPtFinal = idSegment+1;

            // see Point 5.3 Equivalent heights in AFNOR document
            if (seg.zsH <=0){seg.zsH = 0.000000001;}
            if (seg.zrH <=0){seg.zrH = 0.000000001;}

            seg.dc = favorable ? getRayCurveLength(seg.d, seg.d) : seg.d;

            double gs = getGs();

            seg.testFormH = seg.dp / (30 * (seg.zsH + seg.zrH));

            // Compute PRIME zs, zr and testForm
            double zsPrime= seg.getZsPrime(this, seg );
            double zrPrime = seg.getZrPrime(this, seg);

            seg.testFormF = seg.dp / (30 * (zsPrime + zrPrime));

            double gPathPrime;

            if (seg.testFormH <= 1) {
                gPathPrime = seg.testFormH * seg.gPath + (1 - seg.testFormH) * gs;
            } else {
                gPathPrime = seg.gPath;
            }
            seg.gPathPrime = gPathPrime;

        }

    }
*/
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
        public double[] aAtm;
        public double[] aDiv;
        public double[] aRef;
        public double[] aBoundaryH;
        public double[] aBoundaryF;
        public double[] aGlobalH;
        public double[] aGlobalF;
        public double[] aDifH;
        public double[] aDifF;
        public double[] aGlobal;
        public double[] aSource; // directivity attenuation

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
