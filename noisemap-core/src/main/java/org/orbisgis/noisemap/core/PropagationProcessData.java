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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid).
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class PropagationProcessData {
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
    public List<Coordinate> receivers;
    /** FreeField test */
    public FastObstructionTest freeFieldFinder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex;
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries;
    /** Sound level of source. By frequency band, energetic */
    public List<ArrayList<Double>> wj_sources;
    /** Frequency bands values, by third octave */
    public List<Integer> freq_lvl;
    /** Maximum reflexion order */
    public int reflexionOrder;
    /** Maximum diffraction order */
    public int diffractionOrder;
    /** Maximum source distance */
    public double maxSrcDist;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist;
    /** Minimum distance between source and receiver */
    public double minRecDist;
    /** Wall alpha [0-1] */
    public double wallAlpha;
    /** probability occurrence favourable condition */
    public double[] windRose;
    /** Forget Sound Source in dB */
    public double forgetSource;
    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    public List<GeoWithSoilType> geoWithSoilType;
    /** True will compute vertical diffraction */
    public boolean computeVerticalDiffraction;
    /** Temperature in celsius */
    double temperature = 15;
    double celerity = 340;
    double humidity = 70;
    double pressure = Pref;

    public PropagationProcessData(List<Coordinate> receivers, FastObstructionTest freeFieldFinder,
                                  QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries,
                                  List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl, int reflexionOrder,
                                  int diffractionOrder, double maxSrcDist, double maxRefDist, double minRecDist,
                                  double wallAlpha, double[] windRose, double forgetSource, int cellId, ProgressVisitor cellProg,
                                  List<GeoWithSoilType> geoWithSoilType, boolean computeVerticalDiffraction) {
        this.receivers = receivers;
        this.freeFieldFinder = freeFieldFinder;
        this.sourcesIndex = sourcesIndex;
        this.sourceGeometries = sourceGeometries;
        this.wj_sources = wj_sources;
        this.freq_lvl = freq_lvl;
        this.reflexionOrder = reflexionOrder;
        this.diffractionOrder = diffractionOrder;
        this.maxSrcDist = maxSrcDist;
        this.maxRefDist = maxRefDist;
        this.minRecDist = minRecDist;
        this.wallAlpha = wallAlpha;
        this.windRose = windRose;
        this.forgetSource = forgetSource;
        this.cellId = cellId;
        this.cellProg = cellProg;
        this.geoWithSoilType = geoWithSoilType;
        this.computeVerticalDiffraction = computeVerticalDiffraction;
        this.celerity = computeCelerity(temperature+K_0);
    }

    /**
     * Set relative humidity in percentage.
     * @param humidity relative humidity in percentage. 0-100
     */
    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    /**
     * @param pressure Atmospheric pressure in pa. 1 atm is PropagationProcessData.Pref
     */
    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    /**
     * Compute sound celerity in air ISO 9613-1:1993(F)
     * @param k Temperature in kelvin
     * @return Sound celerity in m/s
     */
    static double computeCelerity(double k) {
        return 343.2 * Math.sqrt(k/Kref);
    }

    /**
     * @param temperature Temperature in ° celsius
     */
    public PropagationProcessData setTemperature(double temperature) {
        this.temperature = temperature;
        this.celerity = computeCelerity(temperature + K_0);
        return this;
    }

    /**
     * This function calculates the atmospheric attenuation coefficient of sound in air
     * ISO 9613-1:1993(F)
     * @param frequency acoustic frequency (Hz)
     * @param humidity relative humidity (in %) (0-100)
     * @param pressure atmospheric pressure (in Pa)
     * @param tempKelvin Temperature in Kelvin (in K)
     * @return atmospheric attenuation coefficient (db/km)
     * @author Judicaël Picaut, UMRAE
     */
    public static double getCoefAttAtmos(double frequency, double humidity, double pressure, double tempKelvin) {
        // Sound celerity
        double cson = computeCelerity(tempKelvin);

        // Calculation of the molar fraction of water vapour
        double C = -6.8346 * Math.pow(K01 / tempKelvin, 1.261) + 4.6151;
        double Ps = Pref * Math.pow(10., C);
        double hmol = humidity * Ps / Pref;

        // Classic and rotational absorption
        double Acr = (Pref / pressure) * (1.60E-10) * Math.sqrt(tempKelvin / Kref) * Math.pow(frequency, 2);

        // Vibratory oxygen absorption:!!123
        double Fr = (pressure / Pref) * (24. + 4.04E4 * hmol * (0.02 + hmol) / (0.391 + hmol));
        double Am = 1.559 * PropagationProcessData.FmolO * Math.exp(-KvibO / tempKelvin) * Math.pow(KvibO / tempKelvin, 2);
        double AvibO = Am * (frequency / cson) * 2. * (frequency / Fr) / (1 + Math.pow(frequency / Fr, 2));

        // Vibratory nitrogen absorption
        Fr = (pressure / Pref) * Math.sqrt(Kref / tempKelvin) * (9. + 280. * hmol * Math.exp(-4.170 * (Math.pow(tempKelvin / Kref, -1. / 3.) - 1)));
        Am = 1.559 * FmolN * Math.exp(-KvibN / tempKelvin) * Math.pow(KvibN / tempKelvin, 2);
        double AvibN = Am * (frequency / cson) * 2. * (frequency / Fr) / (1 + Math.pow(frequency / Fr, 2));

        // Total absorption in dB/m
        double alpha = (Acr + AvibO + AvibN);

        return alpha * 1000;
    }


}
