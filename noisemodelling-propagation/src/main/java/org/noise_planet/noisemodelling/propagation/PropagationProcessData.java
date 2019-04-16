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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid).
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 * @author Adrien Le Bellec
 */
public class PropagationProcessData {
    public static double DEFAULT_MAX_PROPAGATION_DISTANCE = 1200;
    public static double DEFAULT_MAXIMUM_REF_DIST = 50;
    public static double DEFAULT_RECEIVER_DIST = 1.0;
    // Thermodynamic constants
	static final double K_0 = 273.15;	// Absolute zero in Celsius
    static final  double Pref = 101325;	// Standard atmosphere atm (Pa)
    static final  double Kref = 293.15;	// Reference ambient atmospheric temperature (K)
    static final  double FmolO = 0.209;	// Mole fraction of oxygen
    static final  double FmolN = 0.781;	// Mole fraction of nitrogen
    static final  double KvibO = 2239.1;// Vibrational temperature of oxygen (K)
    static final  double KvibN = 3352.0;// Vibrational temperature of the nitrogen (K)
    static final  double K01 = 273.16;  // Isothermal temperature at the triple point (K)

    /** coordinate of receivers */
    public List<Coordinate> receivers = new ArrayList<>();
    /** FreeField test */
    public FastObstructionTest freeFieldFinder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex = new QueryRTree();
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries = new ArrayList<>();
    /** Optional Maximal Sound level of source.energetic */
    public List<Double> wj_sources = new ArrayList<>();
    /** Frequency bands values, by third octave */
    public double freq_lvl[] = new double[] {63 ,   125 ,   250 ,   500 ,  1000 ,  2000 ,  4000 ,  8000};
    /** Maximum reflexion order */
    public int reflexionOrder = 1;
    /** Compute diffraction rays over vertical edges */
    private boolean computeHorizontalDiffraction = true;
    /** Maximum source distance */
    public double maxSrcDist = DEFAULT_MAX_PROPAGATION_DISTANCE;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist = DEFAULT_MAXIMUM_REF_DIST;
    /** Minimum distance between source and receiver */
    public double minRecDist = DEFAULT_RECEIVER_DIST;
    /** probability occurrence favourable condition */
    public double[] windRose;
    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = Double.NEGATIVE_INFINITY;
    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    private List<GeoWithSoilType> soilList = new ArrayList<>();
    /** True will compute vertical diffraction */
    private boolean computeVerticalDiffraction;

//    public PropagationProcessData(List<Coordinate> receivers, FastObstructionTest freeFieldFinder,
//                                  QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries,
//                                  List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl, int reflexionOrder,
//                                  boolean computeHorizontalDiffraction, double maxSrcDist, double maxRefDist, double minRecDist,
//                                  double defaultWallApha, double maximumError, int cellId, ProgressVisitor cellProg,
//                                  List<GeoWithSoilType> soilList, boolean computeVerticalDiffraction) {
//        this.receivers = receivers;
//        this.freeFieldFinder = freeFieldFinder;
//        this.sourcesIndex = sourcesIndex;
//        this.sourceGeometries = sourceGeometries;
//        this.wj_sources = new ArrayList<>();
//        //TODO compute global level
//        this.freq_lvl = freq_lvl;
//        this.reflexionOrder = reflexionOrder;
//        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
//        this.maxSrcDist = maxSrcDist;
//        this.maxRefDist = maxRefDist;
//        this.minRecDist = minRecDist;
//        this.defaultWallApha = defaultWallApha;
//        this.windRose = windRose;
//        this.maximumError = maximumError;
//        this.cellId = cellId;
//        this.cellProg = cellProg;
//        this.soilList = soilList;
//        this.computeVerticalDiffraction = computeVerticalDiffraction;
//        this.celerity = computeCelerity(temperature+K_0);
//    }
    public PropagationProcessData(FastObstructionTest freeFieldFinder) {
        this.freeFieldFinder = freeFieldFinder;
    }

    public void addSource(Geometry geom) {
        sourceGeometries.add(geom);
        sourcesIndex.appendGeometry(geom, sourceGeometries.size() - 1);
    }

    public void addSource(Geometry geom, double wj) {
        sourceGeometries.add(geom);
        sourcesIndex.appendGeometry(geom, sourceGeometries.size() - 1);
        wj_sources.add(wj);
    }

    public void setSources(List<Geometry> sourceGeometries) {
        int i = 0;
        for(Geometry source : sourceGeometries) {
            sourcesIndex.appendGeometry(source, i++);
        }
        this.sourceGeometries = sourceGeometries;
    }

    public void setSources(List<Geometry> sourceGeometries, List<Double> wj) {
        int i = 0;
        for(Geometry source : sourceGeometries) {
            sourcesIndex.appendGeometry(source, i++);
        }
        this.sourceGeometries = sourceGeometries;
        this.wj_sources = wj;
    }

    public void addSoilType(GeoWithSoilType soilType) {
        soilList.add(soilType);
    }

    public void addSoilType(Envelope region, double type) {
        soilList.add(new GeoWithSoilType(new GeometryFactory().toGeometry(region), type));
    }

    public void addSoilType(Geometry geo, double type) {
        soilList.add(new GeoWithSoilType(geo, type));
    }

    public void setSoilList(List<GeoWithSoilType> soilList) {
        this.soilList = soilList;
    }

    public List<GeoWithSoilType> getSoilList() {
        return soilList;
    }

    public void addReceiver(Coordinate... receiver) {
        receivers.addAll(Arrays.asList(receiver));
    }

    public void setComputeHorizontalDiffraction(boolean computeHorizontalDiffraction) {
        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
    }

    public void setComputeVerticalDiffraction(boolean computeVerticalDiffraction) {
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }

    public boolean isComputeHorizontalDiffraction() {
        return computeHorizontalDiffraction;
    }

    public boolean isComputeVerticalDiffraction() {
        return computeVerticalDiffraction;
    }

    /**
     * Get WallAlpha
     */
    public static double getWallAlpha(double wallAlpha, double freq_lvl)
    {
        double sigma = 0;
        if(wallAlpha >= 0 && wallAlpha <= 1) {
            sigma = 20000 * Math.pow (10., -2 * Math.pow (wallAlpha, 3./5.)) ;
        } else {
            sigma = Math.min(20000, Math.max(20, wallAlpha));
        }
        double value = GetWallImpedance(sigma,freq_lvl);
        return value;

    }

    public static double GetWallImpedance(double sigma, double freq_l)
    {
        double s = Math.log(freq_l / sigma);
        double x = 1. + 9.08 * Math.exp(-.75 * s);
        double y = 11.9 * Math.exp(-0.73 * s);
        ComplexNumber Z = new ComplexNumber(x, y);

        // Delany-Bazley method, not used in NoiseModelling for the moment
        /*double layer = 0.05; // Let user Choose
        if (layer > 0 && sigma < 1000)
        {
            s = 1000 * sigma / freq;
            double c = 340;
            double RealK= 2 * Math.PI * freq / c *(1 + 0.0858 * Math.pow(s, 0.70));
            double ImgK=2 * Math.PI * freq / c *(0.175 * Math.pow(s, 0.59));
            ComplexNumber k = ComplexNumber.multiply(new ComplexNumber(2 * Math.PI * freq / c,0) , new ComplexNumber(1 + 0.0858 * Math.pow(s, 0.70),0.175 * Math.pow(s, 0.59)));
            ComplexNumber j = new ComplexNumber(-0, -1);
            ComplexNumber m = ComplexNumber.multiply(j,k);
            Z[i] = ComplexNumber.divide(Z[i], (ComplexNumber.exp(m)));
        }*/

        return GetTrueWallAlpha(Z);
    }

   static double GetTrueWallAlpha(ComplexNumber impedance)         // TODO convert impedance to alpha
    {
        double alpha ;
        ComplexNumber z = ComplexNumber.divide(new ComplexNumber(1.0,0), impedance) ;
        double x = z.getRe();
        double y = z.getIm();
        double a1 = (x * x - y * y) / y ;
        double a2 = y / (x * x + y * y + x) ;
        double a3 = ((x + 1) *(x + 1) + y * y) / (x * x + y * y) ;
        alpha = 8 * x * (1 + a1 * Math.atan(a2) - x * Math.log(a3)) ;
        return alpha ;
    }



    /**
     * Update ground Z coordinates of sound sources and receivers absolute to sea levels
     */
    public void makeRelativeZToAbsoluteOnlySources() {

        for (int k=0; k< this.sourceGeometries.size(); k++) {
            Geometry source = this.sourceGeometries.get(k);
            GeometryFactory factory = new GeometryFactory();
            Coordinate[] coordinates = source.getCoordinates();
            for (int i = 0; i < coordinates.length; i++){
                Coordinate pt = coordinates[i];
                Double zGround = this.freeFieldFinder.getHeightAtPosition(pt);
                pt.setOrdinate(2, zGround + (Double.isNaN(pt.getOrdinate(2)) ? 0 : pt.getOrdinate(2)));
                coordinates[i] = pt;
            }

            this.sourceGeometries.set(k,factory.createLineString(coordinates));
        }


    }

}


