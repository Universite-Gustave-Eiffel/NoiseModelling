/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.emission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.noise_planet.noisemodelling.emission.Utils.dbaToW;
import static org.noise_planet.noisemodelling.emission.Utils.wToDba;

/**
 * Return the dB value corresponding to the parameters
 * Reference document is Common Noise Assessment Methods in Europe(CNOSSOS-EU), 2012
 * Stylianos Kephalopoulos, Marco Paviotti, Fabienne Anfosso-Ledee
 * https://ec.europa.eu/jrc/sites/jrcsh/files/cnossos-eu%2520jrc%2520reference%2520report_final_on%2520line%2520version_10%2520august%25202012.pdf
 * @author Nicolas Fortin
 * @author Pierre Aumond - 27/04/2017 - 21/08/2018
 * @author Arnaud Can - 27/02/2018 - 21/08/2018
 */

public class EvaluateRoadSourceDynamic {

    private static JsonNode cnossosData = parse(EvaluateRoadSourceCnossos.class.getResourceAsStream("coefficients_cnossos.json"));
    private static JsonNode cnossosData2019 = parse(EvaluateRoadSourceCnossos.class.getResourceAsStream("coefficients_cnossos2019.json"));

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }

    public static JsonNode getCnossosData(int coeffVer){
        if (coeffVer==1){
            return cnossosData;
        }
        else {
            return cnossosData2019;
        }
    }

    /** Get a Road Coeff by Freq **/
    public static Double getA_Roadcoeff(int Freq, String vehCat, String RoadSurface, int coeffVer) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        int Freq_ind;
        int VehCat_ind;
        double out_value;
        switch (Freq) {
            case 63:
                Freq_ind=0;
                break;
            case 125:
                Freq_ind=1;
                break;
            case 250:
                Freq_ind=2;
                break;
            case 500:
                Freq_ind=3;
                break;
            case 1000:
                Freq_ind=4;
                break;
            case 2000:
                Freq_ind=5;
                break;
            case 4000:
                Freq_ind=6;
                break;
            case 8000:
                Freq_ind=7;
                break;
            default:
                Freq_ind=0;
        }
        return getCnossosData(coeffVer).get("roads").get(RoadSurface).get("ref").get(vehCat).get("spectrum").get(Freq_ind).doubleValue();
    }

    /** Get b Road Coeff by Freq **/
    public static Double getB_Roadcoeff(String vehCat, String roadSurface, int coeffVer) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        return getCnossosData(coeffVer).get("roads").get(roadSurface).get("ref").get(vehCat).get("ßm").doubleValue();
    }

    public static double getCr(String vehCat, int k, int coeffVer) {
        return getCnossosData(coeffVer).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cr").doubleValue();
    }

    public static double getCp(String vehCat, int k, int coeffVer) {
        return getCnossosData(coeffVer).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cp").doubleValue();
    }

    /**
     * Vehicle emission values coefficients
     * @param coeff ar,br,ap,bp,a,b
     * @param freq 0 = 63 Hz, 1 = 125 Hz, etc.
     * @param vehicleCategory 1,2,3,4a,4b..
     * @return
     */
    public static Double getCoeff(String coeff, int freq, String vehicleCategory, int coeffVer) {
        int Freq_ind;
        switch (freq) {
            case 63:
                Freq_ind=0;
                break;
            case 125:
                Freq_ind=1;
                break;
            case 250:
                Freq_ind=2;
                break;
            case 500:
                Freq_ind=3;
                break;
            case 1000:
                Freq_ind=4;
                break;
            case 2000:
                Freq_ind=5;
                break;
            case 4000:
                Freq_ind=6;
                break;
            case 8000:
                Freq_ind=7;
                break;
            default:
                Freq_ind=0;
        }
        return getCnossosData(coeffVer).get("vehicles").get(vehicleCategory).get(coeff).get(Freq_ind).doubleValue();
    }

    /** get noise level from speed **/
    private static Double getNoiseLvl(Double base, Double adj, Double speed,
                                      Double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }


    /** get sum dBa **/
    private static Double sumDba(Double dBA1, Double dBA2) {
        return wToDba(dbaToW(dBA1) + dbaToW(dBA2));
    }

    private static Double sumDba_5(Double dBA1, Double dBA2, Double dBA3, Double dBA4, Double dBA5) {
        return wToDba(dbaToW(dBA1) + dbaToW(dBA2) + dbaToW(dBA3) + dbaToW(dBA4) + dbaToW(dBA5));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(RSParametersDynamic parameters) {
        final double Compound;
        final boolean Stud = parameters.getStud();
        final double Junc_dist = parameters.getJunc_dist();
        final int Junc_type = parameters.getJunc_type();
        final int acc_type = parameters.getAcc_type();
        final String veh_type = parameters.getVeh_type();
        final int VehId = parameters.getVehId();
        final double acceleration = parameters.getAcceleration();
        double speed = parameters.getSpeed();

        final int freqParam = parameters.getFreqParam();
        final double Temperature = parameters.getTemperature();
        final String roadSurface = parameters.getRoadSurface();
        final int coeffVer = parameters.getCoeffVer();

        // ///////////////////////
        // Noise road/tire CNOSSOS
        double RoadLvl; // Lw/m (1 veh/h)

        // Noise level
        // Noise level
        RoadLvl = getNoiseLvl(getCoeff("ap", freqParam , veh_type  ,coeffVer), getCoeff("bp", freqParam , veh_type  ,coeffVer), speed, 70.);

        // Correction by temperature p. 36
        switch (veh_type) {
            case "1":
                RoadLvl = RoadLvl + 0.08 * (20 - Temperature); // K = 0.08  p. 36
                break;
            case "2":
            case "3":
                RoadLvl = RoadLvl + 0.04 * (20 - Temperature); // K = 0.04 p. 36
                break;
            default:
                break;
        }


        // Rolling noise acceleration correction
        double coefficientJunctionDistance = Math.max(1 - Math.abs(Junc_dist) / 100, 0);
        RoadLvl = RoadLvl + getCr(veh_type, Junc_type,coeffVer) * coefficientJunctionDistance;


        //Studied tyres
        if (veh_type.equals("1")) { // because studded tyres are only on Cat 1 vehicle
            if (Stud) {
                double speedStud  = (speed >= 90) ? 90 : speed;
                speedStud = (speedStud <= 50) ? 50 : speedStud;
                double deltaStud = getNoiseLvl(getCoeff( "a" , freqParam , veh_type  ,coeffVer), getCoeff("b", freqParam , veh_type ,coeffVer), speedStud, 70.);
                RoadLvl = RoadLvl + Math.pow(10, deltaStud / 10);
            }
        }

        //Road surface correction on rolling noise
        RoadLvl = RoadLvl +getNoiseLvl(getA_Roadcoeff(freqParam,veh_type, roadSurface,coeffVer), getB_Roadcoeff(veh_type,roadSurface,coeffVer), speed, 70.);


        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles and heavies goods vehicles.
        double MotorLvl;
        RoadLvl = (speed <= 20) ? 0 : RoadLvl;
        speed = (speed <= 20) ? 20 : speed; // Because when vehicles are stopped they still emit motor sounds.
        // default or steady speed.
        MotorLvl =getCoeff("ap", freqParam , veh_type ,coeffVer ) + getCoeff("bp", freqParam , veh_type ,coeffVer ) * (speed-70)/70 ;

        // Propulsion noise acceleration correction


        // Propulsion noise acceleration correction
        double aMax;
        switch (acc_type) {
            case 1:
                if (veh_type.equals("1") || veh_type.equals("2") || veh_type.equals("3") ) {
                    MotorLvl = MotorLvl + getCp(veh_type, Junc_type,coeffVer) * coefficientJunctionDistance;
                }
                break;
            case 2:
                switch (veh_type) {
                    case "1":
                        aMax = 2;
                        if (acceleration >= -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 5.0;}
                        if (acceleration >= -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 2.0;}
                        if (acceleration < -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case "2":
                    case "3":
                        aMax = 1;
                        if (acceleration >= -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 7.0;}
                        if (acceleration >= -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 3.0;}
                        if (acceleration < -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    case "4a":
                    case "4b":
                        aMax = 4;
                        if (acceleration >= -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 5.0;}
                        if (acceleration >= -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 2.0;}
                        if (acceleration < -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    default:
                        break;
                }
                break;
            case 3:
                switch (veh_type) {
                    case "1":
                    case "4a":
                    case "4b":
                        aMax = 10;
                        if (acceleration >= -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 5.0;}
                        if (acceleration >= -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 2.0;}
                        if (acceleration < -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case "2":
                    case "3":
                        aMax = 10;
                        if (acceleration >= -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 7.0;}
                        if (acceleration >= -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,aMax) * 3.0;}
                        if (acceleration < -1 & freqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & freqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }


        // Correction gradient
        switch (veh_type) {
            case "1":
                if (parameters.getSlopePercentage() < -6) {
                    // downwards 2% <= p <= 6%
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + (Math.min(12, -parameters.getSlopePercentage()) - 6) / 1;
                } else if (parameters.getSlopePercentage() <= 2) {
                    // 0% <= p <= 2%
                    MotorLvl = MotorLvl + 0.;
                } else {
                    // upwards 2% <= p <= 6%
                    MotorLvl = MotorLvl + ((speed / 100) * ((Math.min(12, parameters.getSlopePercentage()) - 2) / 1.5));
                }
                break;
            case "2":
                if (parameters.getSlopePercentage() < -4) {
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + ((speed - 20) / 100) * (Math.min(12, -1 * parameters.getSlopePercentage()) - 4) / 0.7;
                } else if (parameters.getSlopePercentage() <= 0) {
                    MotorLvl = MotorLvl + 0.;

                } else {
                    MotorLvl = MotorLvl + (speed / 100) * (Math.min(12, parameters.getSlopePercentage())) / 1;
                }
                break;
            case "3":
                if (parameters.getSlopePercentage() < -4) {
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + ((speed - 10) / 100) * (Math.min(12, -1 * parameters.getSlopePercentage()) - 4) / 0.5;
                } else if (parameters.getSlopePercentage() <= 0) {
                    MotorLvl = MotorLvl + 0.;
                } else {
                    MotorLvl = MotorLvl + (speed / 100) * (Math.min(12, parameters.getSlopePercentage())) / 0.8;
                }
                break;
            default:
                break;
        }


        // Correction road on propulsion noise
        MotorLvl = MotorLvl+ Math.min(getA_Roadcoeff(freqParam ,veh_type,roadSurface,coeffVer), 0.);

        Random r = new Random(VehId);
        double deltaLwdistrib = 0.115*Math.pow(parameters.getLwStd(),2.0); // Gozalo, G. R., Aumond, P., & Can, A. (2020). Variability in sound power levels: Implications for static and dynamic traffic models. Transportation Research Part D: Transport and Environment, 84, 102339.

        Compound = sumDba(RoadLvl, MotorLvl) - deltaLwdistrib +  r.nextGaussian()*parameters.getLwStd();

        return Compound;
    }


}
