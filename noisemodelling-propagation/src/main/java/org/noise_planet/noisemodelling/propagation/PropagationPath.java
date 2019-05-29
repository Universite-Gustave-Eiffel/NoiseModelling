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
package org.noise_planet.noisemodelling.propagation;

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.algorithm.CGAlgorithmsDD;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.Vector3D;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.math3.geometry.euclidean.threed.Vector3D.angle;

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
    private boolean initialized = false;
    // computed in Augmented Path
    public List<Integer> difHPoints = new ArrayList<Integer>(); // diffraction points indices
    public List<Integer> difVPoints = new ArrayList<Integer>(); // diffraction points indices
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


    public void initPropagationPath() {
        if(!isInitialized()) {
            computeAugmentedPath();
            computeAugmentedSegments();
            computeAugmentedSRPath();
            setInitialized(true);
        }
    }


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
        SR.dPath = CGAlgorithms3D.distance(S, R);

        if (refPoints.size()>0){ // case if only reflexion points
            for (int idPoint = 1; idPoint < pointList.size(); idPoint++) {
                dPath += CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate);
            }
            SR.dPath = dPath;
        }
        if (!this.favorable){
            SR.dc = SR.d;
        }else{
            SR.dc = getRayCurveLength(SR.d);
        }

        if (difVPoints.size()>0) {
            double gpath = SR.gPath;
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
            // todo handle with unconvex path
            //if (Vector3D.dot(S,R,S,pointList.get(difVPoints.get(0)).coordinate)<0){convex = -1;}
            SR.delta = convex * (SR.dPath - SR.dc);
        }
        if (difHPoints.size()>0) {
            // Symmetric coordinates
            Coordinate Sprime = new Coordinate(2 * SGround.x - S.x, 2 * SGround.y - S.y, 2 * SGround.z - S.z);
            Coordinate Rprime = new Coordinate(2 * RGround.x - R.x, 2 * RGround.y - R.y, 2 * RGround.z - R.z);
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
                double convex = 1; // if path is convex, delta is positive, otherwise negative
                if (Vector3D.dot(S,R,S,pointList.get(difHPoints.get(0)).coordinate)<0){convex = -1;}

                SR.delta = convex * (SR.dPath - SR.dc);
                SRp.delta = convex * (SRp.dPath - SRp.dc);
                SpR.delta = convex * (SpR.dPath - SpR.dc);
            }
            else
            {
                for (int idPoint = 2; idPoint < pointList.size()-1; idPoint++) {
                    dPath += getRayCurveLength(CGAlgorithms3D.distance(pointList.get(idPoint - 1).coordinate, pointList.get(idPoint).coordinate));
                }

                if (difHPoints.size()>1){
                    SR.eLength = CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate,pointList.get(difHPoints.get(difHPoints.size()-1)).coordinate);
                    SpR.eLength = SR.eLength;
                    SRp.eLength = SR.eLength;
                }

                SR.dPath = dPath
                        + getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(1).coordinate))
                        + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size()-2).coordinate, R));
                SR.dc = getRayCurveLength(SR.d);

                if (difHPoints.size()>0) {
                    SpR.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(1).coordinate))
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, R));
                    SpR.dc = getRayCurveLength(SpR.d);

                    SRp.dPath = dPath
                            + getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(1).coordinate))
                            + getRayCurveLength(CGAlgorithms3D.distance(pointList.get(pointList.size() - 2).coordinate, Rprime));
                    SRp.dc = getRayCurveLength(SRp.d);
                }


                if (Vector3D.dot(S,R,S,pointList.get(difHPoints.get(0)).coordinate)<0) {
                    Coordinate A = projectPointonVector(pointList.get(difHPoints.get(0)).coordinate,SR.vector3D, SR.pInit);
                    double SA = getRayCurveLength(CGAlgorithms3D.distance(S, A));
                    double AR = getRayCurveLength(CGAlgorithms3D.distance(A, R));
                    double SO = getRayCurveLength(CGAlgorithms3D.distance(S, pointList.get(difHPoints.get(0)).coordinate));
                    double OR = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, R));
                    double SpA = getRayCurveLength(CGAlgorithms3D.distance(Sprime, A));
                    double ARp = getRayCurveLength(CGAlgorithms3D.distance(A, Rprime));
                    double SpO = getRayCurveLength(CGAlgorithms3D.distance(Sprime, pointList.get(difHPoints.get(0)).coordinate));
                    double ORp = getRayCurveLength(CGAlgorithms3D.distance(pointList.get(difHPoints.get(0)).coordinate, Rprime));
                    SR.delta =  2*SA+2*AR-SO-OR-SR.dc;
                    SRp.delta =  2*SA+2*ARp-SO-ORp-SRp.dc;
                    SpR.delta = 2*SpA+2*AR-SpO-OR-SpR.dc;
                }else {
                    SR.delta =  SR.dPath - SR.dc;
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


        double gs = pointList.get(0).gs;

        double testForm = SR.dp / (30 * (SR.zs + SR.zr));
        SR.testForm = testForm;

        // Compute PRIME zs, zr and testForm
        double zsPrime= SR.getZsPrime(this,SR );
        double zrPrime = SR.getZrPrime(this, SR);
        double testFormPrime = SR.dp / (30 * (zsPrime + zrPrime));
        SR.testFormPrime = testFormPrime;

        double gPathPrime;
        if (testForm <= 1) {
            gPathPrime = testForm * SR.gPath + (1 - testForm) * gs;
        } else {
            gPathPrime = SR.gPath;
        }
        SR.gPathPrime = gPathPrime;

        this.srList.set(0,SR);
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


            double dp = CGAlgorithms3D.distance(SGround, RGround);
            segmentList.get(idSegment).dp = dp;

            double d = CGAlgorithms3D.distance(S, R);
            segmentList.get(idSegment).d = d;

            if (!this.favorable){
                segmentList.get(idSegment).dc = d;
            }
            else
            {
                segmentList.get(idSegment).dc = getRayCurveLength(d);
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
        return pointList.get(segmentPath.idPtStart).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtStart).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
    }

    public double computeZr(SegmentPath segmentPath) {
        return pointList.get(segmentPath.idPtFinal).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtFinal).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
    }

    public double computeZsPrime(SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazs = alpha0 * Math.pow((segmentPath.zs / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zs + deltazs + deltazt;
    }

    public double computeZrPrime(SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazr = alpha0 * Math.pow((segmentPath.zr / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zr + deltazr + deltazt;
    }


    private double getRayCurveLength(double d) {
        double gamma = Math.max(1000,8*d);
        return 2*gamma*Math.asin(d/(2*gamma));

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
