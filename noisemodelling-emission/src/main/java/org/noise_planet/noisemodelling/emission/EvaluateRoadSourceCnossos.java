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
package org.noise_planet.noisemodelling.emission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.io.IOException;
import java.io.InputStream;

/**
 * Return the dB value corresponding to the parameters
 * Reference document is Common Noise Assessment Methods in Europe(CNOSSOS-EU), 2012
 * Stylianos Kephalopoulos, Marco Paviotti, Fabienne Anfosso-Ledee
 * https://ec.europa.eu/jrc/sites/jrcsh/files/cnossos-eu%2520jrc%2520reference%2520report_final_on%2520line%2520version_10%2520august%25202012.pdf
 * @author Nicolas Fortin
 * @author Pierre Aumond - 27/04/2017.
 */

public class EvaluateRoadSourceCnossos {

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
    private static Double getNoiseLvl(double base, double adj, double speed,
                                      double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }

    /** compute Noise Level from flow_rate and speed **/
    private static Double Vperhour2NoiseLevel(double NoiseLevel, double vperhour, double speed) {
        if (speed > 0) {
            return NoiseLevel + 10 * Math.log10(vperhour / (1000 * speed));
        }else{
            return 0.;
        }
    }



    /** get sum dBa **/
    public static Double sumDba(Double dBA1, Double dBA2) {
        return ComputeRays.wToDba(ComputeRays.dbaToW(dBA1) + ComputeRays.dbaToW(dBA2));
    }

    private static Double sumDba_5(Double dBA1, Double dBA2, Double dBA3, Double dBA4, Double dBA5) {
        return ComputeRays.wToDba(ComputeRays.dbaToW(dBA1) + ComputeRays.dbaToW(dBA2) + ComputeRays.dbaToW(dBA3) + ComputeRays.dbaToW(dBA4) + ComputeRays.dbaToW(dBA5));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(RSParametersCnossos parameters) {
        final int freqParam = parameters.getFreqParam();
        final double Temperature = parameters.getTemperature();
        final double Ts_stud = parameters.getTs_stud();
        final double Pm_stud = parameters.getPm_stud();
        final double Junc_dist = parameters.getJunc_dist();
        final int Junc_type = parameters.getJunc_type();
        final String roadSurface = parameters.getRoadSurface();
        final int coeffVer = parameters.getCoeffVer();

        // ///////////////////////
        // Noise road/tire CNOSSOS
        double lvRoadLvl; // Lw/m (1 veh/h)
        double medRoadLvl;// Lw/m (1 veh/h)
        double hgvRoadLvl;// Lw/m (1 veh/h)
        double wheelaRoadLvl;// Lw/m (1 veh/h)
        double wheelbRoadLvl;// Lw/m (1 veh/h)

        // Noise level
        lvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "1"  ,coeffVer), getCoeff("br", freqParam , "1"  ,coeffVer), parameters.getSpeedLv(), 70.);
        medRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "2" ,coeffVer ), getCoeff("br", freqParam , "2" ,coeffVer ), parameters.getSpeedMv(), 70.);
        hgvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "3"  ,coeffVer), getCoeff("br", freqParam , "3" ,coeffVer ), parameters.getSpeedHgv(), 70.);
        wheelaRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "4a" ,coeffVer ), getCoeff("br", freqParam , "4a" ,coeffVer ), parameters.getSpeedWav(), 70.);
        wheelbRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "4b"  ,coeffVer), getCoeff("br", freqParam , "4b" ,coeffVer ), parameters.getSpeedWbv(), 70.);

        // Correction by temperature p. 36
        lvRoadLvl = lvRoadLvl+ 0.08*(20-Temperature); // K = 0.08  p. 36
        medRoadLvl = medRoadLvl + 0.04*(20-Temperature); // K = 0.04 p. 36
        hgvRoadLvl = hgvRoadLvl + 0.04*(20-Temperature); // K = 0.04 p. 36


        // Rolling noise acceleration correction
        double coefficientJunctionDistance = Math.max(1 - Math.abs(Junc_dist) / 100, 0);
        lvRoadLvl = lvRoadLvl + getCr("1", Junc_type,coeffVer) * coefficientJunctionDistance;
        medRoadLvl = medRoadLvl + getCr("2", Junc_type,coeffVer)  * coefficientJunctionDistance;
        hgvRoadLvl = hgvRoadLvl + getCr("3", Junc_type,coeffVer)  * coefficientJunctionDistance;

        //Studied tyres
        if (Pm_stud >0 && Ts_stud > 0) {
            double deltastud = 0;
            double speed = parameters.getSpeedLv();
            double ps = Pm_stud * Ts_stud / 12; //yearly average proportion of vehicles equipped with studded tyres
            speed = (speed >= 90) ? 90 : speed;
            speed = (speed <= 50) ? 50 : speed;
            deltastud = getNoiseLvl(getCoeff("a", freqParam, "1",coeffVer), getCoeff("b", freqParam, "1",coeffVer), speed, 70.);
            lvRoadLvl = lvRoadLvl + 10 * Math.log10((1 - ps) + ps * Math.pow(10, deltastud / 10));
        }

        //Road surface correction on rolling noise
        lvRoadLvl = lvRoadLvl+ getNoiseLvl(getA_Roadcoeff(freqParam,"1", parameters.getRoadSurface(),coeffVer), getB_Roadcoeff("1",roadSurface,coeffVer), parameters.getSpeedLv(), 70.);
        medRoadLvl = medRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"2", parameters.getRoadSurface(),coeffVer), getB_Roadcoeff("2",roadSurface,coeffVer), parameters.getSpeedMv(), 70.);
        hgvRoadLvl = hgvRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"3", parameters.getRoadSurface(),coeffVer), getB_Roadcoeff("3",roadSurface,coeffVer), parameters.getSpeedHgv(), 70.);
        wheelaRoadLvl = wheelaRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"4a", parameters.getRoadSurface(),coeffVer), getB_Roadcoeff("4a",roadSurface,coeffVer), parameters.getSpeedWav(), 70.);
        wheelbRoadLvl = wheelbRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"4b", parameters.getRoadSurface(),coeffVer), getB_Roadcoeff("4b",roadSurface,coeffVer), parameters.getSpeedWbv(), 70.);

        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles and heavies goods vehicles.

        // default or steady speed.
        double lvMotorLvl = getCoeff("ap", freqParam , "1" ,coeffVer ) + getCoeff("bp", freqParam , "1" ,coeffVer ) * (parameters.getSpeedLv()-70)/70 ;
        double medMotorLvl =  getCoeff("ap", freqParam , "2" ,coeffVer ) + getCoeff("bp", freqParam , "2" ,coeffVer ) * (parameters.getSpeedMv()-70)/70 ;
        double hgvMotorLvl =  getCoeff("ap", freqParam , "3" ,coeffVer ) + getCoeff("bp", freqParam , "3" ,coeffVer ) * (parameters.getSpeedHgv()-70)/70 ;
        double wheelaMotorLvl =  getCoeff("ap", freqParam , "4a" ,coeffVer ) + getCoeff("bp", freqParam , "4a" ,coeffVer ) * (parameters.getSpeedWav()-70)/70 ;
        double wheelbMotorLvl =  getCoeff("ap", freqParam , "4b" ,coeffVer ) + getCoeff("bp", freqParam , "4b" ,coeffVer ) * (parameters.getSpeedWbv()-70)/70 ;


        // Propulsion noise acceleration correction

        lvMotorLvl = lvMotorLvl + getCp("1", Junc_type,coeffVer) * coefficientJunctionDistance;
        medMotorLvl = medMotorLvl + getCp("2", Junc_type,coeffVer)  * coefficientJunctionDistance;
        hgvMotorLvl = hgvMotorLvl + getCp("3", Junc_type,coeffVer)  * coefficientJunctionDistance;


        // Correction gradient for light vehicle
        if (parameters.getSlopePercentage() < -6) {
            // downwards 2% <= p <= 6%
            // Steady and deceleration, the same formulae
            lvMotorLvl = lvMotorLvl + (Math.min(12,-parameters.getSlopePercentage())-6)/1;
        }
        else if (parameters.getSlopePercentage() <= 2) {
            // 0% <= p <= 2%
            lvMotorLvl = lvMotorLvl + 0.;
        } else {
            // upwards 2% <= p <= 6%
            lvMotorLvl = lvMotorLvl + ((parameters.getSpeedLv()/100) * ((Math.min(12,parameters.getSlopePercentage())-2)/1.5));
        }
        // Correction gradient for trucks
        if (parameters.getSlopePercentage() < -4) {
            // Steady and deceleration, the same formulae
            medMotorLvl = medMotorLvl + ((parameters.getSpeedMv()-20)/100) * (Math.min(12,-1*parameters.getSlopePercentage())-4)/0.7;
            hgvMotorLvl = hgvMotorLvl + ((parameters.getSpeedHgv()-10)/100) * (Math.min(12,-1*parameters.getSlopePercentage())-4)/0.5;
        }
        else if (parameters.getSlopePercentage() <= 0) {
            medMotorLvl = medMotorLvl + 0.;
            hgvMotorLvl = hgvMotorLvl + 0.;
        } else {
            medMotorLvl = medMotorLvl + (parameters.getSpeedMv()/100) * (Math.min(12,parameters.getSlopePercentage()))/1;
            hgvMotorLvl = hgvMotorLvl + (parameters.getSpeedHgv()/100) * (Math.min(12,parameters.getSlopePercentage()))/0.8;
        }

        // Correction road on propulsion noise
        lvMotorLvl = lvMotorLvl+ Math.min(getA_Roadcoeff(freqParam ,"1",roadSurface,coeffVer), 0.);
        medMotorLvl = medMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"2",roadSurface,coeffVer), 0.);
        hgvMotorLvl = hgvMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"3",roadSurface,coeffVer), 0.);
        wheelaMotorLvl = wheelaMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"4a",roadSurface,coeffVer), 0.);
        wheelbMotorLvl = wheelbMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"4b",roadSurface,coeffVer), 0.);


        final double lvCompound = sumDba(lvRoadLvl, lvMotorLvl);
        final double medCompound = sumDba(medRoadLvl, medMotorLvl);
        final double hgvCompound = sumDba(hgvRoadLvl, hgvMotorLvl);
        final double wheelaCompound = sumDba(wheelaRoadLvl, wheelaMotorLvl);
        final double wheelbCompound = sumDba(wheelbRoadLvl, wheelbMotorLvl);


        // ////////////////////////
        // Lw/m (1 veh/h) to ?

        double lvLvl = Vperhour2NoiseLevel(lvCompound , parameters.getLvPerHour(), parameters.getSpeedLv());
        double medLvl =Vperhour2NoiseLevel(medCompound , parameters.getMvPerHour(), parameters.getSpeedMv());
        double hgvLvl =Vperhour2NoiseLevel(hgvCompound , parameters.getHgvPerHour(), parameters.getSpeedHgv());
        double wheelaLvl =Vperhour2NoiseLevel(wheelaCompound , parameters.getWavPerHour(), parameters.getSpeedWav());
        double wheelbLvl =Vperhour2NoiseLevel(wheelbCompound , parameters.getWbvPerHour(), parameters.getSpeedWbv());
        return sumDba_5(lvLvl, medLvl, hgvLvl, wheelaLvl, wheelbLvl);
    }
}
