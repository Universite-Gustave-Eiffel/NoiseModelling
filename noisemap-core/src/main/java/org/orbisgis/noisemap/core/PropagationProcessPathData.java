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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data input for a propagation Path process.
 *@author Pierre Aumond
 */
public class PropagationProcessPathData {
    // Thermodynamic constants
	static final double K_0 = 273.15;	// Absolute zero in Celsius
    static final  double Pref = 101325;	// Standard atmosphere atm (Pa)
    static final  double Kref = 293.15;	// Reference ambient atmospheric temperature (K)
    static final  double FmolO = 0.209;	// Mole fraction of oxygen
    static final  double FmolN = 0.781;	// Mole fraction of nitrogen
    static final  double KvibO = 2239.1;// Vibrational temperature of oxygen (K)
    static final  double KvibN = 3352.0;// Vibrational temperature of the nitrogen (K)
    static final  double K01 = 273.16;  // Isothermal temperature at the triple point (K)
    /** Frequency bands values, by third octave */
    static final List<Integer> freq_lvl = Arrays.asList(63, 125, 250, 500, 1000, 2000, 4000, 8000);

    /** Temperature in celsius */
    private double temperature = 15;
    private double celerity = 340;
    private double humidity = 70;
    private double pressure = Pref;
    private double[] alpha_atmo = getAtmoCoeffArray(freq_lvl,  temperature,  pressure,  humidity);
    private double defaultOccurance = 0.5;

    private boolean gDisc = true;     // choose between accept G discontinuity or not
    private boolean prime2520 = false; // choose to use prime values to compute eq. 2.5.20
    /** probability occurrence favourable condition */
    private double[] windRose  = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

    /**
     * Set relative humidity in percentage.
     * @param humidity relative humidity in percentage. 0-100
     */
    public PropagationProcessPathData setHumidity(double humidity) {

        this.humidity = humidity;
        this.alpha_atmo = getAtmoCoeffArray(freq_lvl,  temperature,  pressure,  humidity);
        return this;
    }

    /**
     * @param pressure Atmospheric pressure in pa. 1 atm is PropagationProcessData.Pref
     */
    public PropagationProcessPathData setPressure(double pressure) {
        this.pressure = pressure;
        this.alpha_atmo = getAtmoCoeffArray(freq_lvl,  temperature,  pressure,  humidity);
        return this;
    }

    public double[] getWindRose() {
        return windRose;
    }

    public void setWindRose(double[] windRose) {
        this.windRose = windRose;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getCelerity() {
        return celerity;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getPressure() {
        return pressure;
    }

    public boolean isPrime2520() {
        return prime2520;
    }

    public boolean isgDisc() {
        return gDisc;
    }

    public void setgDisc(boolean gDisc) {
        this.gDisc = gDisc;
    }

    /**
     * @return Default favorable probability (0-1)
     */
    public double getDefaultOccurance() {
        return defaultOccurance;
    }

    /**
     * @param defaultOccurance Default favorable probability (0-1)
     */
    public void setDefaultOccurance(double defaultOccurance) {
        this.defaultOccurance = defaultOccurance;
    }

    public PropagationProcessPathData setGDisc(boolean gDisc) {
        this.gDisc = gDisc;
        return this;
    }

    public PropagationProcessPathData setPrime2520(boolean prime2520) {
        this.prime2520 = prime2520;
        return this;
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
    public PropagationProcessPathData setTemperature(double temperature) {
        this.temperature = temperature;
        this.celerity = computeCelerity(temperature + K_0);
        this.alpha_atmo = getAtmoCoeffArray(freq_lvl,  temperature,  pressure,  humidity);
        return this;
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

    public static double[] getAtmoCoeffArray(List<Integer> freq_lvl, double temperature, double pressure, double humidity){
        double[] alpha_atmo;
        // Compute atmospheric alpha value by specified frequency band
        alpha_atmo = new double[freq_lvl.size()];
        for (int idfreq = 0; idfreq < freq_lvl.size(); idfreq++) {
            alpha_atmo[idfreq] = getAlpha(freq_lvl.get(idfreq), temperature, pressure, humidity);
        }
        return alpha_atmo;
    }


    public double[] getAlpha_atmo() {
        return alpha_atmo;
    }



}
