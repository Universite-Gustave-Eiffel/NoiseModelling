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

import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.geom.GeometryFactory;

import java.sql.SQLException;
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
    public static final double DEFAULT_MAX_PROPAGATION_DISTANCE = 1200;
    public static final double DEFAULT_MAXIMUM_REF_DIST = 50;
    public static final double DEFAULT_RECEIVER_DIST = 1.0;
    public static final double DEFAULT_GS = 0.0;

    public List<Long> receiversPk = new ArrayList<>();
    public List<Long> sourcesPk = new ArrayList<>();
    /** coordinate of receivers */
    public List<Coordinate> receivers = new ArrayList<>();
    /** FreeField test */
    public FastObstructionTest freeFieldFinder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex = new QueryRTree();
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries = new ArrayList<>();
    /** Frequency bands values, by third octave */
    public double freq_lvl[] = new double[] {63 ,   125 ,   250 ,   500 ,  1000 ,  2000 ,  4000 ,  8000};
    /** Maximum reflexion order */
    public int reflexionOrder = 1;
    /** Compute diffraction rays over vertical edges */
    protected boolean computeHorizontalDiffraction = true;
    /** Maximum source distance */
    public double maxSrcDist = DEFAULT_MAX_PROPAGATION_DISTANCE;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist = DEFAULT_MAXIMUM_REF_DIST;
    /** Minimum distance between source and receiver */
    public double minRecDist = DEFAULT_RECEIVER_DIST;
    /** Source factor absorption */
    public double gS = DEFAULT_GS;

    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = Double.NEGATIVE_INFINITY;
    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    protected List<GeoWithSoilType> soilList = new ArrayList<>();
    /** True will compute vertical diffraction */
    protected boolean computeVerticalDiffraction;



    public PropagationProcessData(FastObstructionTest freeFieldFinder) {
        this.freeFieldFinder = freeFieldFinder;
    }

    public void addSource(Geometry geom) {
        sourceGeometries.add(geom);
        sourcesIndex.appendGeometry(geom, sourceGeometries.size() - 1);
    }

    public void addSource(Long pk, Geometry geom) {
        addSource(geom);
        sourcesPk.add(pk);
    }
    /**
     * Add geometry with additional attributes
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param rs Additional attributes fetched from database
     */
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        addSource(pk, geom);
    }

    public void setSources(List<Geometry> sourceGeometries) {
        int i = 0;
        for(Geometry source : sourceGeometries) {
            sourcesIndex.appendGeometry(source, i++);
        }
        this.sourceGeometries = sourceGeometries;
    }

    /**
     * Optional - Return the maximal power spectrum of the sound source
     * @param sourceId Source identifier (index in {@link PropagationProcessData#sourceGeometries})
     * @return maximal power spectrum or empty array
     */
    public double[] getMaximalSourcePower(int sourceId) {
        return new double[0];
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

    public void addReceiver(long pk, Coordinate position) {
        receivers.add(position);
        receiversPk.add(pk);
    }

    public void addReceiver(long pk, Coordinate position, SpatialResultSet rs) {
        addReceiver(pk, position);
    }

    public void setComputeHorizontalDiffraction(boolean computeHorizontalDiffraction) {
        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
    }

    public void setComputeVerticalDiffraction(boolean computeVerticalDiffraction) {
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }

    public void setGs(double gS) {
        this.gS = gS;
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
            if(coordinates.length > 1) {
                this.sourceGeometries.set(k, factory.createLineString(coordinates));
            } else {
                this.sourceGeometries.set(k, factory.createPoint(coordinates[0]));
            }
        }


    }

}


