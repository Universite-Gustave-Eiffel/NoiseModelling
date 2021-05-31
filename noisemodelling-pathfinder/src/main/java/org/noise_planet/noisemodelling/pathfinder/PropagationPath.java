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
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector3D;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PropagationPath
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */

// todo get out all the useless computations and functions
// todo please revise public, private, etc.

public class PropagationPath {
    // given by user
    private List<SegmentPath> srList; // list of source-receiver path (including prime path)
    private List<PointPath> pointList; // list of points (source, receiver or diffraction and reflection points)
    private List<SegmentPath> segmentList; // list of segments [S,O1] and [On-1,R] (O1 and On-1 are respectively the first diffraction point and On-1 the last diffration point)
    private boolean favorable; // if true, favorable meteorological condition path
    int idSource;
    int idReceiver;
    Orientation sourceOrientation =
            new Orientation(0,0,0);
    private boolean initialized = false;
    // computed in Augmented Path
    public List<Integer> difHPoints = new ArrayList<Integer>(); // diffraction points indices on horizontal edges
    public List<Integer> difVPoints = new ArrayList<Integer>(); // diffraction points indices on vertical edges
    public List<Integer> refPoints = new ArrayList<Integer>(); // reflection points indices

    /**
     * parameters given by user
     * @param favorable
     * @param pointList
     * @param segmentList
     */
    public PropagationPath(boolean favorable, List<PointPath> pointList, List<SegmentPath> segmentList , List<SegmentPath> srList) {
        this.favorable = favorable;
        this.pointList = pointList;
        this.segmentList = segmentList;
        this.srList = srList;
    }

    public PropagationPath() {

    }

    public Orientation getSourceOrientation() {
        return sourceOrientation;
    }

    public void setSourceOrientation(Orientation sourceOrientation) {
        this.sourceOrientation = sourceOrientation;
    }

    /**
     * @return Propagation path as a geometry object
     */
    public LineString asGeom() {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[pointList.size()];
        int i=0;
        for(PointPath pointPath : pointList) {
            coordinates[i++] = new Coordinate(pointPath.coordinate);
        }
        return geometryFactory.createLineString(coordinates);
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
    public void writeStream( DataOutputStream out ) throws IOException {
        out.writeBoolean(favorable);
        out.writeInt(idSource);
        out.writeFloat(sourceOrientation.yaw);
        out.writeFloat(sourceOrientation.pitch);
        out.writeFloat(sourceOrientation.roll);
        out.writeInt(idReceiver);
        out.writeInt(pointList.size());
        for(PointPath pointPath : pointList) {
            pointPath.writeStream(out);
        }
        out.writeInt(segmentList.size());
        for(SegmentPath segmentPath : segmentList) {
            segmentPath.writeStream(out);
        }
        out.writeInt(srList.size());
        for(SegmentPath segmentPath : srList) {
            segmentPath.writeStream(out);
        }
    }

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
        int srListSize = in.readInt();
        srList = new ArrayList<>(srListSize);
        for(int i=0; i < srListSize; i++) {
            SegmentPath segmentPath = new SegmentPath();
            segmentPath.readStream(in);
            srList.add(segmentPath);
        }
    }


    public boolean isInitialized() {
        return initialized;
    }

    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public List<PointPath> getPointList() {return pointList;}

    public List<SegmentPath> getSegmentList() {return segmentList;}

    public List<SegmentPath> getSRList() {return srList;}

    public void setPointList(List<PointPath> pointList) {this.pointList = pointList;}

    public void setSegmentList(List<SegmentPath>  segmentList) {this.segmentList = segmentList;}

    public void setSRList(List<SegmentPath> srList) {this.srList = srList;}


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


    public Coordinate projectPointonSegment(Coordinate P, Vector3D vector, Coordinate pInit) {
        Coordinate A = new Coordinate(pInit.x, pInit.y,pInit.z);
        Coordinate B = new Coordinate(vector.getX()+pInit.x, vector.getY()+pInit.y,vector.getZ()+pInit.z);

        return new Coordinate(A.x+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getX(),
                A.y+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getY(),
                A.z+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getZ());
    }


    public Coordinate projectPointonVector(Coordinate P, Vector3D vector,Coordinate pInit) {
        Coordinate A = new Coordinate(pInit.x, pInit.y,pInit.z);
        Coordinate B = new Coordinate(vector.getX()+pInit.x, vector.getY()+pInit.y,vector.getZ()+pInit.z);
        return new Coordinate(A.x+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getX(),
                A.y+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getY(),
                A.z+(Vector3D.dot(A,P,A,B) / Vector3D.dot(A,B,A,B))*vector.getZ());
    }

    /**
     * Initialise the propagation path
     */
    public void initPropagationPath() {
        if(!isInitialized()) {
            computeAugmentedPath();
            computeAugmentedSegments();
            computeAugmentedSRPath();
            setInitialized(true);
        }
    }

    /**
     * Initialise all values that depends of the global path
     * as distances, Gpath, etc.
     */
    public void computeAugmentedSRPath() {
        double dPath =0 ;

        SegmentPath SR = this.srList.get(0);

        SR.idPtStart = 0;
        SR.idPtFinal = pointList.size()-1;

        // Original absolute coordinates
        Coordinate S = (Coordinate) pointList.get(0).coordinate.clone();
        Coordinate R = (Coordinate) pointList.get(pointList.size()-1).coordinate.clone();

        // Projected source and receiver on MeanPlane
        Coordinate SGround = projectPointonVector(S,SR.vector3D,SR.pInit);
        Coordinate RGround = projectPointonVector(R,SR.vector3D,SR.pInit);

        SR.d = CGAlgorithms3D.distance(S, R);
        SR.dp = CGAlgorithms3D.distance(SGround, RGround);

        // In case of reflections the slanted path passes through the image sources.
        if (refPoints.size()>0){
            Coordinate ini = S;
            Coordinate iniGround = SGround;
            SR.d =0.;
            SR.dp = 0.;
            for (int idPoint = 1; idPoint < pointList.size(); idPoint++) {
                if (pointList.get(idPoint).type == PointPath.POINT_TYPE.REFL){
                    SR.d += CGAlgorithms3D.distance(ini, pointList.get(idPoint).coordinate);
                    ini = pointList.get(idPoint).coordinate;

                    SR.dp += CGAlgorithms3D.distance(iniGround, projectPointonVector(pointList.get(idPoint).coordinate,SR.vector3D,SR.pInit));
                    iniGround = projectPointonVector(pointList.get(idPoint).coordinate,SR.vector3D,SR.pInit);
                }
            }
            SR.d  += CGAlgorithms3D.distance(ini, R);
            SR.dp += CGAlgorithms3D.distance(iniGround, RGround);
        }

        SR.dc = (favorable) ? getRayCurveLength(SR.d,SR.d): SR.d;
        SR.dPath = SR.d;
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
                SR.eLength = dPath;
            }
            SR.dPath = dPath
                    + CGAlgorithms3D.distance(S, pointList.get(1).coordinate)
                    + CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate,R);
            SR.dc = SR.d;

            double convex = 1; // if path is convex, delta is positive, otherwise negative

            SR.gPath = gPath/SR.dPath;

            SR.dp = SR.dPath;

            // todo handle with unconvex path
            //if (Vector3D.dot(S,R,S,pointList.get(difVPoints.get(0)).coordinate)<0){convex = -1;}
            SR.delta = convex * (SR.dPath - SR.d);
        }

        // diffraction on horizontal edges
        if (difHPoints.size()>0) {

            dPath = 0;

            // Symmetric coordinates to the gound mean plane see Figure 2.5.c
            Coordinate SGroundSeg = this.segmentList.get(0).sGround;
            Coordinate RGroundSeg = this.segmentList.get(segmentList.size()-1).rGround;
            Coordinate Sprime = new Coordinate(2 * SGroundSeg.x - S.x, 2 * SGroundSeg.y - S.y, 2 * SGroundSeg.z - S.z);
            Coordinate Rprime = new Coordinate(2 * RGroundSeg.x - R.x, 2 * RGroundSeg.y - R.y, 2 * RGroundSeg.z - R.z);

            double gpath = SR.gPath;
            SegmentPath SRp = new SegmentPath(gpath, new Vector3D(S, Rprime),SR.pInit);
            SegmentPath SpR = new SegmentPath(gpath, new Vector3D(Sprime, R),Sprime);

            SpR.d = CGAlgorithms3D.distance(Sprime, R);
            SRp.d = CGAlgorithms3D.distance(S, Rprime);

            SRp.dp = SR.dp;
            SpR.dp = SR.dp;

            if (!this.favorable){
                for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                    dPath += CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate);
                }

                if (pointList.size()>3){
                    SR.eLength = dPath;
                    SpR.eLength = dPath;
                    SRp.eLength = dPath;
                }
                SR.dPath = dPath
                        + CGAlgorithms3D.distance(S, pointList.get(1).coordinate)
                        + CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate,R);
                SpR.dPath = dPath
                        + CGAlgorithms3D.distance(Sprime, pointList.get(1).coordinate)
                        + CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate,R);
                SRp.dPath = dPath
                        + CGAlgorithms3D.distance(S, pointList.get(1).coordinate)
                        + CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate,Rprime);

                SpR.dc = SpR.d;
                SRp.dc = SRp.d;
                SR.dc = SR.d;

                // if path is convex, delta is positive, otherwise negative
                double convex = Vector3D.dot(S,R,S,pointList.get(difHPoints.get(0)).coordinate)<0 ? -1 : 1;

                SR.delta = convex * (SR.dPath - SR.d);
                SRp.delta = convex * (SRp.dPath - SRp.d);
                SpR.delta = convex * (SpR.dPath - SpR.d);
            }
            else
            {

                // if the straight sound ray SR is masked by the obstacle (1st and 2nd case in Figure 2.5.e)
                for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                    dPath += getRayCurveLength(CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate), SR.d);
                }

                if (difHPoints.size()>1){
                    double dDif = CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate,pointList.get(difHPoints.get(difHPoints.size()-1)).coordinate);
                    SR.eLength = getRayCurveLength(dDif,SR.d);
                    SpR.eLength = SR.eLength;
                    SRp.eLength = SR.eLength;
                }

                SR.dPath = dPath
                        + getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(1).coordinate), SR.d)
                        + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate, R), SR.d);
                SR.dc = getRayCurveLength(SR.d, SR.d);

                if (difHPoints.size()>0) {
                    SpR.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(1).coordinate), SR.d)
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, R), SR.d);
                    SpR.dc = getRayCurveLength(SpR.d, SR.d);

                    SRp.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(1).coordinate), SR.d)
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, Rprime), SR.d);
                    SRp.dc = getRayCurveLength(SRp.d, SR.d);
                }

                // todo for the multiple diffractions in favourable conditions: Eq. 2.5.28

                // Iif the straight sound ray SR is not masked by the obstacle (3rd case in Figure 2.5.e)
                if (Vector3D.dot(S,R,S,pointList.get(difHPoints.get(0)).coordinate)<0) {
                    Coordinate A = projectPointonVector(pointList.get(difHPoints.get(0)).coordinate,SR.vector3D, SR.pInit);
                    double SA = getRayCurveLength(CGAlgorithms3D.distance(S, A), SR.d);
                    double AR = getRayCurveLength(CGAlgorithms3D.distance(A, R), SR.d);
                    double SO = getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(difHPoints.get(0)).coordinate), SR.d);
                    double OR = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, R), SR.d);
                    double SpA = getRayCurveLength(CGAlgorithms3D.distance(Sprime, A), SR.d);
                    double ARp = getRayCurveLength(CGAlgorithms3D.distance(A, Rprime), SR.d);
                    double SpO = getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(difHPoints.get(0)).coordinate), SR.d);
                    double ORp = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, Rprime), SR.d);
                    SR.delta =  2*SA+2*AR-SO-OR-SR.dc; // Eq. 2.5.27
                    SRp.delta =  2*SA+2*ARp-SO-ORp-SRp.dc;
                    SpR.delta = 2*SpA+2*AR-SpO-OR-SpR.dc;
                }else {
                    SR.delta =  SR.dPath - SR.dc; // Eq. 2.5.26
                    SRp.delta = SRp.dPath - SRp.dc;
                    SpR.delta = SpR.dPath - SpR.dc;
                }
            }
            this.srList.add(SpR);
            this.srList.add(SRp);
        }


        SR.zs  =SR.getZs(this, SR);
        SR.zr  =SR.getZr(this, SR);

        // see Point 5.3 Equivalent heights in AFNOR document
        if (SR.zs<=0){SR.zs = 0.000000001;}
        if (SR.zr<=0){SR.zr = 0.000000001;}


        double testForm = SR.dp / (30 * (SR.zs + SR.zr)); // if <= 1, then the distinction between the type of ground located near the source and the type of ground located near the receiver is negligible.
        SR.testForm = testForm;

        double gPathPrime;

        // if dp <= 30(zs + zr), then the distinction between the type of ground located near the source and the type of ground located near the receiver is negligible.
        // Eq. 2.5.14
        if (testForm <= 1) {
            SR.gPathPrime = testForm * SR.gPath + (1 - testForm) * pointList.get(0).gs;
        } else {
            SR.gPathPrime = SR.gPath;
        }

        this.srList.set(0,SR);

        // Compute PRIME zs, zr and testForm
        double zsPrime= SR.getZsPrime(this,SR );
        double zrPrime = SR.getZrPrime(this, SR);

        double testFormPrime = SR.dp / (30 * (zsPrime + zrPrime));
        SR.testFormPrime = testFormPrime;
    }


    void computeAugmentedSegments() {
        for (int idSegment = 0; idSegment < segmentList.size(); idSegment++) {

            segmentList.get(idSegment).idPtStart = idSegment;
            segmentList.get(idSegment).idPtFinal = idSegment+1;

            double zs = segmentList.get(idSegment).getZs(this, this.segmentList.get(idSegment));
            double zr = segmentList.get(idSegment).getZr(this, this.segmentList.get(idSegment));

            // see Point 5.3 Equivalent heights in AFNOR document
            if (zs<=0){zs = 0.000000001;}
            if (zr<=0){zr = 0.000000001;}

            this.segmentList.get(idSegment).zs  = zs;
            this.segmentList.get(idSegment).zr = zr;

            Coordinate S = (Coordinate) pointList.get(idSegment).coordinate.clone();
            Coordinate R = (Coordinate) pointList.get(idSegment+1).coordinate.clone();

            // Projected source and receiver on MeanPlane
            Coordinate SGround = projectPointonVector(S,segmentList.get(idSegment).vector3D,segmentList.get(idSegment).pInit);
            Coordinate RGround = projectPointonVector(R,segmentList.get(idSegment).vector3D,segmentList.get(idSegment).pInit);

            this.segmentList.get(idSegment).sGround = SGround;
            this.segmentList.get(idSegment).rGround = RGround;

            double dp = CGAlgorithms3D.distance(SGround, RGround);
            segmentList.get(idSegment).dp = dp;

            double d = CGAlgorithms3D.distance(S, R);
            segmentList.get(idSegment).d = d;

            if (!this.favorable){
                segmentList.get(idSegment).dc = d;
            }
            else
            {
                segmentList.get(idSegment).dc = getRayCurveLength(d, d);
            }



            double gs = pointList.get(0).gs;

            double testForm = dp / (30 * (zs + zr));
            segmentList.get(idSegment).testForm = testForm;

            // Compute PRIME zs, zr and testForm
            double zsPrime= segmentList.get(idSegment).getZsPrime(this,this.segmentList.get(idSegment) );
            double zrPrime = segmentList.get(idSegment).getZrPrime(this, this.segmentList.get(idSegment));

            double testFormPrime = dp / (30 * (zsPrime + zrPrime));
            segmentList.get(idSegment).testFormPrime = testFormPrime;

            double gPathPrime;

            if (testForm <= 1) {
                gPathPrime = testForm * segmentList.get(idSegment).gPath + (1 - testForm) * gs;
            } else {
                gPathPrime = segmentList.get(idSegment).gPath;
            }
            this.segmentList.get(idSegment).gPathPrime = gPathPrime;

        }

    }

    private void computeAugmentedPath() {
        difVPoints.clear();
        difHPoints.clear();
        refPoints.clear();
        for (int idPoint = 0; idPoint < pointList.size(); idPoint++) {

            if (pointList.get(idPoint).type==PointPath.POINT_TYPE.DIFV)
            {
                difVPoints.add(idPoint);
            }
            if (pointList.get(idPoint).type==PointPath.POINT_TYPE.DIFH)
            {
                difHPoints.add(idPoint);
            }

            if (pointList.get(idPoint).type==PointPath.POINT_TYPE.REFL)
            {
                refPoints.add(idPoint);
            }
        }

    }

    double computeZs(SegmentPath segmentPath) {
        double zs = pointList.get(segmentPath.idPtStart).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtStart).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
        return ((zs > 0) ? zs : 0); // Section 2.5.3 - If the equivalent height of a point becomes negative, i.e. if the point is located below the mean ground plane, a null height is retained, and the equivalent point is then identical with its possible image.
    }

    public double computeZr(SegmentPath segmentPath) {
        double zr = pointList.get(segmentPath.idPtFinal).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtFinal).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
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
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazs = alpha0 * Math.pow((segmentPath.zs / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zs + deltazs + deltazt;
    }

    /**
     * Eq 2.5.19
     * @param segmentPath
     * @return
     */
    public double computeZrPrime(SegmentPath segmentPath) {
        // The height corrections deltazs and deltazr convey the effect of the sound ray bending. deltazT accounts for the effect of the turbulence.
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazr = alpha0 * Math.pow((segmentPath.zr / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zr + deltazr + deltazt;
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
    public static void writePropagationPathListStream( DataOutputStream out, List<PropagationPath> propagationPaths ) throws IOException {
        out.writeInt(propagationPaths.size());
        for(PropagationPath propagationPath : propagationPaths) {
            propagationPath.writeStream(out);
        }
    }

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
}
