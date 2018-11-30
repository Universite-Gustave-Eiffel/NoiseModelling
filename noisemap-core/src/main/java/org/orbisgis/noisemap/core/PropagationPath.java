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

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;

/**
 * PropagationPath work for FastObstructionTest,
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class PropagationPath {
    private boolean favorable;
    private List<PointPath> pointList;
    private List<SegmentPath> segmentList;

    public Double dPath = null; // pass by points
    public Double d = null; // direct ray
    public Double dc = null; // direct ray sensible to meteorological conditions (can be curve)
    public Double dp = null; // distance on mean plane
    public Double eLength = null;


    /**
     * @param favorable
     * @param pointList
     * @param segmentList
     */
    public PropagationPath(boolean favorable, List<PointPath> pointList, List<SegmentPath> segmentList) {
        this.favorable = favorable;
        this.pointList = pointList;
        this.segmentList = segmentList;
    }

    public static class PointPath {
        public final Coordinate coordinate;
        public final double altitude;
        public final double gs;
        public final double alphaWall;
        public final boolean diffraction;

        /**
         * @param coordinate
         * @param altitude
         * @param gs
         * @param alphaWall
         * @param diffraction
         */
        public PointPath(org.locationtech.jts.geom.Coordinate coordinate, double altitude, double gs, double alphaWall, boolean diffraction) {
            this.coordinate = coordinate;
            this.altitude = altitude;
            this.gs = gs;
            this.alphaWall = alphaWall;
            this.diffraction = diffraction;
        }
    }

    public static class SegmentPath {
        public final double gPath;
        private Double gPathPrime = null;
        private Double gw = null;
        private Double gm = null;
        public Double zs = null;
        public Double zr = null;
        public Double zsPrime = null;
        public Double zrPrime = null;
        // todo compute distances for segments, direct without conditions and direct with conditions
        public Double d = null; // direct ray
        public Double dc = null; // direct ray sensible to meteorological conditions

        /**
         * @param gPath
         */
        public SegmentPath(double gPath) {
            this.gPath = gPath;
        }

        public void setGw(double g) {
            this.gw = g;
        }

        public void setGm(double g) {
            this.gm = g;
        }

        public Double getgPathPrime(PropagationPath path) {
            if(gPathPrime == null) {
                gPathPrime = path.computeGPathPrime(path,this);
            }
            return gPathPrime;
        }

        public Double getGw() {
            return gw;
        }

        public Double getGm() {
            return gm;
        }

        public Double getZs(PropagationPath path) {
            if(zs == null) {
                zs = path.computeZs(path.pointList);
            }
            return zs;
        }

        public Double getZr(PropagationPath path) {
            if(zr == null) {
                zr = path.computeZr(path.pointList);
            }
            return zr;
        }

        public Double getZsPrime(PropagationPath path, SegmentPath segmentPath) {
            if(zsPrime == null) {
                zsPrime = path.computeZsPrime(path,segmentPath);
            }
            return zsPrime;
        }

        public Double getZrPrime(PropagationPath path, SegmentPath segmentPath) {
            if(zrPrime == null) {
                zrPrime = path.computeZrPrime(path,segmentPath);
            }
            return zrPrime;
        }

        public Double getD(PropagationPath path, SegmentPath segmentPath) {
            if(d == null) {
                d = path.computeDistanceD(path,segmentPath);
            }
            return d;
        }

        public Double getDc(PropagationPath path, SegmentPath segmentPath) {
            if(dc == null) {
                dc = path.computeDistanceC(path,segmentPath);
            }
            return dc;
        }


    }




    public List<PointPath> getPointList() {
        return pointList;
    }

    public List<SegmentPath> getSegmentList() {
        return segmentList;
    }

    public PropagationPath(List<SegmentPath> segmentList) {
        this.segmentList = segmentList;
    }

    public boolean isFavorable() {
        return favorable;
    }


    public Double getDPath(PropagationPath path) {
        if(dPath == null) {
            computeDistances(path);
        }
        return dPath;
    }

    public Double getD(PropagationPath path) {
        if(d == null) {
            computeDistances(path);
        }
        return d;
    }

    public Double getDc(PropagationPath path) {
        if(dc == null) {
            computeDistances(path);
        }
        return dc;
    }

    public Double getDp(PropagationPath path) {
        if(dp == null) {
            computeDistances(path);
        }
        return dp;
    }

    public Double geteLength(PropagationPath path) {
        if(eLength == null) {
            computeDistances(path);
        }
        return eLength;
    }

    public void computeDistances(PropagationPath path) {
        List<PropagationPath.PointPath> pointPath = getPointList();
        double dPath = 0;
        double dc = 0;
        double zs = pointList.get(0).altitude+ pointList.get(0).coordinate.z;
        double zr = pointList.get(pointList.size()-1).altitude+ pointList.get(pointList.size()-1).coordinate.z;

        Coordinate SGround = pointPath.get(0).coordinate;
        Coordinate RGround = pointPath.get(pointPath.size()-1).coordinate;
        Coordinate S = pointPath.get(0).coordinate;
        Coordinate R = pointPath.get(pointPath.size()-1).coordinate;
        SGround.z = SGround.z - zs;
        RGround.z = RGround.z - zr;

        double dp = CGAlgorithms3D.distance(SGround, RGround);
        double d = CGAlgorithms3D.distance(S, R);

        if (!path.favorable){
            for (int idPoint = 1; idPoint < pointPath.size(); idPoint++) {
                dPath += CGAlgorithms3D.distance(pointPath.get(idPoint - 1).coordinate, pointPath.get(idPoint).coordinate);
            }
            dc = d;
        }
        else
        {
            for (int idPoint = 1; idPoint < pointPath.size(); idPoint++) {
                dPath += getRayCurveLength(CGAlgorithms3D.distance(pointPath.get(idPoint - 1).coordinate, pointPath.get(idPoint).coordinate));
            }
            dc = getRayCurveLength(d);
        }

        // todo compute eLength
        double eLength = dPath;
        this.dPath = dPath;
        this.d = d;
        this.dc = dc;
        this.dp = dp;
        this.eLength = eLength;
    }

    private double computeGPathPrime(PropagationPath path, SegmentPath segmentPath) {

        double zs = segmentPath.getZs(path);
        double zr = segmentPath.getZr(path);
        double gs = pointList.get(0).gs;
        double testForm = path.dp / (30 * (zs + zr));
        double gPathPrime;

        if (testForm <= 1) {
            gPathPrime = testForm * segmentPath.gPath + (1 - testForm) * gs;
        } else {
            gPathPrime = segmentPath.gPath;
        }
        return gPathPrime;
    }

    private double computeZs(List<PointPath> pointList) {
        double zs = pointList.get(0).altitude+ pointList.get(0).coordinate.z;
        return zs;
    }

    private double computeZr(List<PointPath> pointList) {
        double zr = pointList.get(pointList.size()-1).altitude+ pointList.get(pointList.size()-1).coordinate.z;
        return zr;
    }

    private double computeZsPrime(PropagationPath path, SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * path.dp / (segmentPath.zs + segmentPath.zr);
        double deltazs = alpha0 * Math.pow((segmentPath.zs / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(path.dp, 2) / 2);
        return segmentPath.zs + deltazs + deltazt;
    }

    private double computeZrPrime(PropagationPath path, SegmentPath segmentPath) {
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * path.dp / (segmentPath.zs + segmentPath.zr);
        double deltazr = alpha0 * Math.pow((segmentPath.zr / (segmentPath.zs + segmentPath.zr)), 2) * (Math.pow(path.dp, 2) / 2);
        return segmentPath.zr + deltazr + deltazt;
    }

    // todo compute distance of segments
    private double computeDistanceD(PropagationPath path, SegmentPath segmentPath) {
        return 0;
    }
    private double computeDistanceC(PropagationPath path, SegmentPath segmentPath) {
        return 0;
    }


    private double getRayCurveLength(double d) {

        double gamma = Math.max(1000,8*d);
        return 2*gamma*Math.asin(d/(2*gamma));

    }


}
