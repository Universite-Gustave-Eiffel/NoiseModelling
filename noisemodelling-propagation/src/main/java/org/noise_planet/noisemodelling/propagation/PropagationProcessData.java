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
    /** Optional Sound level of source.energetic */
    public List<Double> wj_sources = new ArrayList<>();
    /** Frequency bands values, by third octave */
    public double freq_lvl[] = new double[] {63 ,   125 ,   250 ,   500 ,  1000 ,  2000 ,  4000 ,  8000};
    /** Maximum reflexion order */
    public int reflexionOrder = 1;
    /** Compute diffraction rays over vertical edges */
    public boolean computeHorizontalDiffraction = true;
    /** Maximum source distance */
    public double maxSrcDist = DEFAULT_MAX_PROPAGATION_DISTANCE;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist = DEFAULT_MAXIMUM_REF_DIST;
    /** Minimum distance between source and receiver */
    public double minRecDist = DEFAULT_RECEIVER_DIST;
    /** probability occurrence favourable condition */
    public double[] windRose;
    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError;
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
    double celerity ;
    double humidity = 70;
    double pressure = Pref;

//    public PropagationProcessData(List<Coordinate> receivers, FastObstructionTest freeFieldFinder,
//                                  QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries,
//                                  List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl, int reflexionOrder,
//                                  boolean computeHorizontalDiffraction, double maxSrcDist, double maxRefDist, double minRecDist,
//                                  double defaultWallApha, double maximumError, int cellId, ProgressVisitor cellProg,
//                                  List<GeoWithSoilType> geoWithSoilType, boolean computeVerticalDiffraction) {
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
//        this.geoWithSoilType = geoWithSoilType;
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

    public void setSources(QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries) {
        this.sourcesIndex = sourcesIndex;
        this.sourceGeometries = sourceGeometries;
    }

    public void addReceiver(Coordinate... receiver) {
        receivers.addAll(Arrays.asList(receiver));
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
     *
     * @param frequency Frequency (Hz)
     * @param humidity Humidity %
     * @param pressure Pressure in pascal
     * @param T_kel Temperature in kelvin
     * @return Atmospheric absorption dB/km
     */
    public static double getCoefAttAtmos2(double frequency, double humidity, double pressure, double T_kel) {

            final double Kelvin = 273.15;	//For converting to Kelvin
            final double e = 2.718282;

            double T_ref = Kelvin + 20;     //Reference temp = 20 degC
            double T_rel = T_kel / T_ref;   //Relative temp
            double T_01 = Kelvin + 0.01;    //Triple point isotherm temperature (Kelvin)
            double P_ref = 101.325;         //Reference atmospheric P = 101.325 kPa
            double P_rel = (pressure / 1e3) / P_ref;       //Relative pressure

            //Get Molecular Concentration of water vapour
            double P_sat_over_P_ref = Math.pow(10,((-6.8346 * Math.pow((T_01 / T_kel), 1.261)) + 4.6151));
            double H = humidity * (P_sat_over_P_ref/P_rel); 		// h from ISO 9613-1, Annex B, B.1

            //fro from ISO 9613-1, 6.2, eq.3
            double Fro = P_rel * (24 + 40400 * H * (0.02 + H) / (0.391 + H));

            //frn from ISO 9613-1, 6.2, eq.4
            double Frn = P_rel / Math.sqrt(T_rel) * (9 + 280 * H * Math.pow(e,(-4.17 * (Math.pow(T_rel,(-1.0/3.0)) - 1))));

            //xc, xo and xn from ISO 9613-1, 6.2, part of eq.5
            double Xc = 0.0000000000184 / P_rel * Math.sqrt(T_rel);
            double Xo = 0.01275 * Math.pow(e,(-2239.1 / T_kel)) * Math.pow((Fro + (frequency*frequency / Fro)), -1);
            double Xn = 0.1068 * Math.pow(e,(-3352.0 / T_kel)) * Math.pow((Frn + (frequency*frequency / Frn)), -1);

            //alpha from ISO 9613-1, 6.2, eq.5
            double Alpha = 20 * Math.log10(e) * frequency * frequency * (Xc + Math.pow(T_rel,(-5.0/2.0)) * (Xo + Xn));

        return Alpha * 1000;
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


    /**
     * @return Computation parameters for PropagationProcessPath
     */
    public PropagationProcessPathData newPropagationProcessPathData() {
        PropagationProcessPathData attenuationWithMeteo = new PropagationProcessPathData();
        attenuationWithMeteo.setWindRose(windRose);
        attenuationWithMeteo.setTemperature(temperature);
        attenuationWithMeteo.setHumidity(humidity);
        attenuationWithMeteo.setPressure(pressure);
        return attenuationWithMeteo;
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


