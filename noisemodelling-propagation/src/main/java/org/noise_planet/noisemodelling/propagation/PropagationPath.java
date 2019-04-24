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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector3D;
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

    public boolean isInitialized() {
        return initialized;
    }

    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public static class PointPath {
        // given by user
        public Coordinate coordinate; // coordinate (absolute)
        public double altitude; // altitude of relief (exact)
        public double gs;       // only if POINT_TYPE = SRCE or RECV, G coefficient right above the point
        public double alphaWall; // only if POINT_TYPE = REFL, alpha coefficient
        public int buildingId; // only if POINT_TYPE = REFL
        public POINT_TYPE type; // type of point
        public enum POINT_TYPE {
            SRCE,
            REFL,
            DIFV,
            DIFH,
            RECV
        }

        /**
         * parameters given by user
         * @param coordinate
         * @param altitude
         * @param gs
         * @param alphaWall
         * @param buildingId
         * @param type
         */
        public PointPath(Coordinate coordinate, double altitude, double gs, double alphaWall, int buildingId, POINT_TYPE type) {
            this.coordinate = coordinate;
            this.altitude = altitude;
            this.gs = gs;
            this.alphaWall = alphaWall;
            this.buildingId = buildingId;
            this.type = type;
        }

        public PointPath() {

        }

        public void setType(POINT_TYPE type) {
            this.type =  type;
        }

        public void setBuildingId(int buildingId) {
            this.buildingId =  buildingId;
        }

        public void setAlphaWall(double alphaWall) {
            this.alphaWall =  alphaWall;
        }

        public int getBuildingId() {
            return buildingId;
        }


        public void setCoordinate(Coordinate coordinate) {
            this.coordinate =  coordinate;
        }
    }

    public static class SegmentPath {
        //  given by user
        public double gPath;          // G coefficient for the considered path segment
        public Vector3D vector3D;     // mean Plane for the considered path segment
        public Coordinate pInit;     // init point to compute the mean Plane

        // computed in AugmentedSegments
        public int idPtStart;               //start point indice for the considered path segment
        public int idPtFinal;               //final point indice for the considered path segment

        public Double gPathPrime = null;    //Gpath prime , calculated from Gpath and geometry
        public Double gw = null;
        public Double gm = null;
        public Double zs = null;
        public Double zr = null;
        public Double zsPrime = null;
        public Double zrPrime = null;
        public Double testForm = null;
        public Double testFormPrime = null;

        public Double dPath; // pass by points
        public Double d ; // direct ray between source and receiver
        public Double dc; // direct ray sensible to meteorological conditions (can be curve) between source and receiver
        public Double dp; // distance on mean plane between source and receiver
        public Double eLength = 0.0; // distance between first and last diffraction point
        public Double delta; // distance between first and last diffraction point

        /**
         * @param gPath
         */

        public SegmentPath(double gPath, Vector3D vector3D, Coordinate pInit) {
            this.gPath = gPath;
            this.vector3D = vector3D;
            this.pInit = pInit;
        }


        public void setGw(double g) {
            this.gw = g;
        }

        public void setGm(double g) {
            this.gm = g;
        }

        public Double getgPathPrime(PropagationPath path) {
            if(gPathPrime == null) {
                path.computeAugmentedSegments();
            }
            return gPathPrime;
        }

        public Double getGw() {
            return gw;
        }

        public Double getGm() {
            return gm;
        }

        public Double getZs(PropagationPath path, SegmentPath segmentPath) {
            if(zs == null) {
                zs = path.computeZs(segmentPath);
            }
            return zs;
        }

        public Double getZr(PropagationPath path, SegmentPath segmentPath) {
            if(zr == null) {
                zr = path.computeZr(segmentPath);
            }
            return zr;
        }

        public Double getZsPrime(PropagationPath path, SegmentPath segmentPath) {
            if(zsPrime == null) {
                zsPrime = path.computeZsPrime(segmentPath);
            }
            return zsPrime;
        }

        public Double getZrPrime(PropagationPath path, SegmentPath segmentPath) {
            if(zrPrime == null) {
                zrPrime = path.computeZrPrime(segmentPath);
            }
            return zrPrime;
        }

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
            SegmentPath SRp = new SegmentPath(gpath, new Vector3D(S, Rprime),new Coordinate(0,0,0));
            SegmentPath SpR = new SegmentPath(gpath, new Vector3D(Sprime, R),new Coordinate(0,0,0));

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


    }


    private void computeAugmentedSegments() {
        for (int idSegment = 0; idSegment < segmentList.size(); idSegment++) {

            segmentList.get(idSegment).idPtStart = idSegment;
            segmentList.get(idSegment).idPtFinal = idSegment+1;

            double zs= segmentList.get(idSegment).getZs(this, this.segmentList.get(idSegment));
            segmentList.get(idSegment).zs  =zs;

            double zr = segmentList.get(idSegment).getZr(this, this.segmentList.get(idSegment));
            segmentList.get(idSegment).zr = zr;

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
            segmentList.get(idSegment).zs  =zs;

            double zrPrime = segmentList.get(idSegment).getZrPrime(this, this.segmentList.get(idSegment));
            segmentList.get(idSegment).zr = zr;

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


    private double computeZs(SegmentPath segmentPath) {
        return pointList.get(segmentPath.idPtStart).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtStart).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
    }

    private double computeZr(SegmentPath segmentPath) {
        return pointList.get(segmentPath.idPtFinal).coordinate.z - projectPointonSegment(pointList.get(segmentPath.idPtFinal).coordinate,segmentPath.vector3D,segmentPath.pInit).z;
    }

    private double computeZsPrime(SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazs = alpha0 * Math.pow((segmentPath.zs / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zs + deltazs + deltazt;
    }

    private double computeZrPrime(SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zs + segmentPath.zr);
        double deltazr = alpha0 * Math.pow((segmentPath.zr / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zr + deltazr + deltazt;
    }


    private double getRayCurveLength(double d) {
        double gamma = Math.max(1000,8*d);
        return 2*gamma*Math.asin(d/(2*gamma));

    }






}
