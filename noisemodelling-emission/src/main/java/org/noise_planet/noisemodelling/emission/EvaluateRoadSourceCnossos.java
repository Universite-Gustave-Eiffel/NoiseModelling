/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.emission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;

import static org.noise_planet.noisemodelling.emission.Utils.dbToW;
import static org.noise_planet.noisemodelling.emission.Utils.wToDb;

/**
 * Return the dB value corresponding to the parameters
 * Reference document is "Commission Directive (EU) 2015/996 of 19 May 2015 establishing common noise assessment methods according to Directive 2002/49/EC of the European Parliament and of the Council (Text with EEA relevance)" 2015
 * including its 2019 amendments
 * https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=OJ:JOL_2015_168_R_0001 for 2015 version
 *  @Author Nicolas Fortin, Université Gustave Eiffel
 *  @Author Pierre Aumond, Université Gustave Eiffel - 27/04/2017 - Update 12/01/2021
 */

public class EvaluateRoadSourceCnossos {

    private static JsonNode cnossosData = parse(EvaluateRoadSourceCnossos.class.getResourceAsStream("coefficients_cnossos.json"));
    private static JsonNode cnossosData2019 = parse(EvaluateRoadSourceCnossos.class.getResourceAsStream("coefficients_cnossos2019.json")); // new coefficients in 2019 amendments

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }

    public static JsonNode getCnossosData(int coeffVer) {
        if (coeffVer == 1) {
            return cnossosData;
        } else {
            return cnossosData2019;
        }
    }

    /**
     * Get a Road Coeff for a frequency value
     * @param Freq Frequency in Hz (ocrave band)
     * @param vehCat Vehicle category
     * @param RoadSurface Road surface identifier
     * @param coeffVer 2015 or 2019 coefficients version
     * @return a Road Coeff
     */
    public static Double getA_Roadcoeff(int Freq, String vehCat, String RoadSurface, int coeffVer) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - accessed on line 2017 at : https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        int Freq_ind;
        switch (Freq) {
            case 63:
                Freq_ind = 0;
                break;
            case 125:
                Freq_ind = 1;
                break;
            case 250:
                Freq_ind = 2;
                break;
            case 500:
                Freq_ind = 3;
                break;
            case 1000:
                Freq_ind = 4;
                break;
            case 2000:
                Freq_ind = 5;
                break;
            case 4000:
                Freq_ind = 6;
                break;
            case 8000:
                Freq_ind = 7;
                break;
            default:
                Freq_ind = 0;
        }
        return getCnossosData(coeffVer).get("roads").get(RoadSurface).get("ref").get(vehCat).get("spectrum").get(Freq_ind).doubleValue();
    }

    /**
     * Get b Road Coeff
     * @param vehCat Vehicle category
     * @param roadSurface Road surface identifier
     * @param coeffVer 2015 or 2019 coefficients version
     * @return b Road Coeff
     */
    public static Double getB_Roadcoeff(String vehCat, String roadSurface, int coeffVer) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        return getCnossosData(coeffVer).get("roads").get(roadSurface).get("ref").get(vehCat).get("ßm").doubleValue();
    }

    /**
     * Get Cr coefficient
     * @param vehCat Vehicle category
     * @param k k=1 Crossing lights, k=2 roundabout
     * @param coeffVer 2015 or 2019 coefficients version
     * @return Cr coefficient
     */
    public static double getCr(String vehCat, int k, int coeffVer) {
        return getCnossosData(coeffVer).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cr").doubleValue();
    }

    /**
     * Get Cp coefficient
     * @param vehCat Vehicle category
     * @param k k=1 Crossing lights, k=2 roundabout
     * @param coeffVer 2015 or 2019 coefficients version
     * @return Cp coefficient
     */
    public static double getCp(String vehCat, int k, int coeffVer) {
        return getCnossosData(coeffVer).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cp").doubleValue();
    }

    /**
     * get Vehicle emission values coefficients
     * @param coeff ar,br,ap,bp,a,b
     * @param freq 0 = 63 Hz, 1 = 125 Hz, etc.
     * @param vehicleCategory 1,2,3,4a,4b..
     * @return Vehicle emission values coefficients
     */
    public static Double getCoeff(String coeff, int freq, String vehicleCategory, int coeffVer) {
        int Freq_ind;
        switch (freq) {
            case 63:
                Freq_ind = 0;
                break;
            case 125:
                Freq_ind = 1;
                break;
            case 250:
                Freq_ind = 2;
                break;
            case 500:
                Freq_ind = 3;
                break;
            case 1000:
                Freq_ind = 4;
                break;
            case 2000:
                Freq_ind = 5;
                break;
            case 4000:
                Freq_ind = 6;
                break;
            case 8000:
                Freq_ind = 7;
                break;
            default:
                Freq_ind = 0;
        }
        return getCnossosData(coeffVer).get("vehicles").get(vehicleCategory).get(coeff).get(Freq_ind).doubleValue();
    }

    /**
     * Get rolling or motor sound level
     * @param base coeff A
     * @param adj coeff B
     * @param speed vm in km/h
     * @param speedBase vref in km/h
     * @return
     */
    private static Double getNoiseLvl(double base, double adj, double speed,
                                      double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }


    /**
     * Compute Noise Level from flow_rate and speed Eq 2.2.1 from Directive 2015/2019
     * @param LWim LW,i,m is the directional sound power of a single vehicle and is expressed in dB (re. 10–12 W/m).
     * @param Qm Traffic flow data Qm shall be expressed as yearly average per hour, per time period (day-evening-night), per vehicle class and per source line.
     * @param vm The speed vm is a representative speed per vehicle category: in most cases the lower of the maximum legal speed for the section of road and the maximum legal speed for the vehicle category. If measurement data is unavailable, the maximum legal speed for the vehicle category shall be used.
     * @return
     * @throws IOException if speed < 0 km/h
     */
    private static Double Vperhour2NoiseLevel(double LWim, double Qm, double vm){
       return LWim + 10 * Math.log10(Qm / (1000 * vm));
    }


    /**
     * Energetic sum of 2 dB values
     * @param dB1 First value in dB
     * @param dB2 Second value in dB
     * @return
     */
    private static Double sumDbValues(Double dB1, Double dB2) {
        return wToDb(dbToW(dB1) + dbToW(dB2));
    }

    /**
     * Energetic sum of 5 dB values
     * @param dB1 value in dB
     * @param dB2 value in dB
     * @param dB3 value in dB
     * @param dB4 value in dB
     * @param dB5 value in dB
     * @return
     */
    private static Double sumDb5(Double dB1, Double dB2, Double dB3, Double dB4, Double dB5) {
        return wToDb(dbToW(dB1) + dbToW(dB2) + dbToW(dB3) + dbToW(dB4) + dbToW(dB5));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(RSParametersCnossos parameters) throws IOException {
        final int freqParam = parameters.getFreqParam();
        final double Temperature = parameters.getTemperature();
        final double Ts_stud = parameters.getTsStud();
        final double Pm_stud = parameters.getqStudRatio();
        final double Junc_dist = parameters.getJunc_dist();
        final int Junc_type = parameters.getJunc_type();
        final String roadSurface = parameters.getRoadSurface();
        final int coeffVer = parameters.getCoeffVer();
        double vRef = 70.;

        /**
         * Rolling Noise
         */
        // Rolling noise level Eq. 2.2.4
        double lvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "1", coeffVer), getCoeff("br", freqParam, "1", coeffVer), parameters.getSpeedLv(), vRef);
        double medRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "2", coeffVer), getCoeff("br", freqParam, "2", coeffVer), parameters.getSpeedMv(), vRef);
        double hgvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "3", coeffVer), getCoeff("br", freqParam, "3", coeffVer), parameters.getSpeedHgv(), vRef);
        // Rolling noise is only for categories 1, 2 and 3

        // Correction for studded tyres - Eq. 2.2.6
        if (Pm_stud > 0 && Ts_stud > 0) {
            double deltastud;
            double speed = parameters.getSpeedLv();
            double ps = Pm_stud * Ts_stud / 12; // Eq. 2.2.7 yearly average proportion of vehicles equipped with studded tyres
            speed = (speed >= 90) ? 90 : speed;
            speed = (speed <= 50) ? 50 : speed;
            deltastud = getNoiseLvl(getCoeff("a", freqParam, "1", coeffVer), getCoeff("b", freqParam, "1", coeffVer), speed, vRef);
            lvRoadLvl = lvRoadLvl + 10 * Math.log10((1 - ps) + ps * Math.pow(10, deltastud / 10)); // Eq. 2.2.8
            // Only for light vehicles (Eq.2.2.9)
        }

        // Effect of air temperature on rolling noise correction Eq 2.2.10
        lvRoadLvl = lvRoadLvl + 0.08 * (20 - Temperature); // K = 0.08
        medRoadLvl = medRoadLvl + 0.04 * (20 - Temperature); // K = 0.04
        hgvRoadLvl = hgvRoadLvl + 0.04 * (20 - Temperature); // K = 0.04

        /**
         * Propulsion Noise
         */
        // General equation - Eq. 2.2.11
        double lvMotorLvl = getCoeff("ap", freqParam, "1", coeffVer) + getCoeff("bp", freqParam, "1", coeffVer) * (parameters.getSpeedLv() - vRef) / vRef;
        double medMotorLvl = getCoeff("ap", freqParam, "2", coeffVer) + getCoeff("bp", freqParam, "2", coeffVer) * (parameters.getSpeedMv() - vRef) / vRef;
        double hgvMotorLvl = getCoeff("ap", freqParam, "3", coeffVer) + getCoeff("bp", freqParam, "3", coeffVer) * (parameters.getSpeedHgv() - vRef) / vRef;
        double wheelaMotorLvl = getCoeff("ap", freqParam, "4a", coeffVer) + getCoeff("bp", freqParam, "4a", coeffVer) * (parameters.getSpeedWav() - vRef) / vRef;
        double wheelbMotorLvl = getCoeff("ap", freqParam, "4b", coeffVer) + getCoeff("bp", freqParam, "4b", coeffVer) * (parameters.getSpeedWbv() - vRef) / vRef;

        // Effect of road gradients
        // This correction implicitly includes the effect of slope on speed.
        // Light vehicles (cat 1) - Eq 2.2.13
        if (parameters.getSlopePercentage() < -6) {
            lvMotorLvl = lvMotorLvl + (Math.min(12, -parameters.getSlopePercentage()) - 6) / 1;
        } else if (parameters.getSlopePercentage() <= 2) {
            lvMotorLvl = lvMotorLvl + 0.;
        } else {
            lvMotorLvl = lvMotorLvl + ((parameters.getSpeedLv() / 100) * ((Math.min(12, parameters.getSlopePercentage()) - 2) / 1.5));
        }

        // Medium and Heavy vehicles (cat 2 and 3) - Eq 2.2.14 and 2.2.15
        if (parameters.getSlopePercentage() < -4) {
            medMotorLvl = medMotorLvl + ((parameters.getSpeedMv() - 20) / 100) * (Math.min(12, -parameters.getSlopePercentage()) - 4) / 0.7;
            hgvMotorLvl = hgvMotorLvl + ((parameters.getSpeedHgv() - 10) / 100) * (Math.min(12, -parameters.getSlopePercentage()) - 4) / 0.5;
        } else if (parameters.getSlopePercentage() <= 0) {
            medMotorLvl = medMotorLvl + 0.;
            hgvMotorLvl = hgvMotorLvl + 0.;
        } else {
            medMotorLvl = medMotorLvl + (parameters.getSpeedMv() / 100) * (Math.min(12, parameters.getSlopePercentage())) / 1;
            hgvMotorLvl = hgvMotorLvl + (parameters.getSpeedHgv() / 100) * (Math.min(12, parameters.getSlopePercentage())) / 0.8;
        }
        // no effects on cat 4 vehicles Eq. 2.2.16

        /**
         * Mixed effects (Rolling & Propulsion)
         */
        // Effect of the acceleration and deceleration of vehicles
        // Todo Here, we should get the Junc_dist by another way that we are doing now to be more precise issue #261
        double coefficientJunctionDistance = Math.max(1 - Math.abs(Junc_dist) / 100, 0);
        // Rolling Noise Eq 2.2.17
        lvRoadLvl = lvRoadLvl + getCr("1", Junc_type, coeffVer) * coefficientJunctionDistance;
        medRoadLvl = medRoadLvl + getCr("2", Junc_type, coeffVer) * coefficientJunctionDistance;
        hgvRoadLvl = hgvRoadLvl + getCr("3", Junc_type, coeffVer) * coefficientJunctionDistance;

        // Propulsion Noise Eq 2.2.18
        lvMotorLvl = lvMotorLvl + getCp("1", Junc_type, coeffVer) * coefficientJunctionDistance;
        medMotorLvl = medMotorLvl + getCp("2", Junc_type, coeffVer) * coefficientJunctionDistance;
        hgvMotorLvl = hgvMotorLvl + getCp("3", Junc_type, coeffVer) * coefficientJunctionDistance;
        wheelaMotorLvl = wheelaMotorLvl + getCp("4a", Junc_type, coeffVer) * coefficientJunctionDistance;
        wheelbMotorLvl = wheelbMotorLvl + getCp("4b", Junc_type, coeffVer) * coefficientJunctionDistance;

        // Effect of the type of road surface - Eq. 2.2.19
        lvRoadLvl = lvRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam, "1", parameters.getRoadSurface(), coeffVer), getB_Roadcoeff("1", roadSurface, coeffVer), parameters.getSpeedLv(), 70.);
        medRoadLvl = medRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam, "2", parameters.getRoadSurface(), coeffVer), getB_Roadcoeff("2", roadSurface, coeffVer), parameters.getSpeedMv(), 70.);
        hgvRoadLvl = hgvRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam, "3", parameters.getRoadSurface(), coeffVer), getB_Roadcoeff("3", roadSurface, coeffVer), parameters.getSpeedHgv(), 70.);

        // Correction road on propulsion noise - Eq. 2.2.20
        lvMotorLvl = lvMotorLvl + Math.min(getA_Roadcoeff(freqParam, "1", roadSurface, coeffVer), 0.);
        medMotorLvl = medMotorLvl + Math.min(getA_Roadcoeff(freqParam, "2", roadSurface, coeffVer), 0.);
        hgvMotorLvl = hgvMotorLvl + Math.min(getA_Roadcoeff(freqParam, "3", roadSurface, coeffVer), 0.);
        wheelaMotorLvl = wheelaMotorLvl + Math.min(getA_Roadcoeff(freqParam, "4a", roadSurface, coeffVer), 0.);
        wheelbMotorLvl = wheelbMotorLvl + Math.min(getA_Roadcoeff(freqParam, "4b", roadSurface, coeffVer), 0.);

        /**
         * Combine Propulsion and Rolling Noise - Eq. 2.2.2
         */
        final double lvCompound = sumDbValues(lvRoadLvl, lvMotorLvl);
        final double medCompound = sumDbValues(medRoadLvl, medMotorLvl);
        final double hgvCompound = sumDbValues(hgvRoadLvl, hgvMotorLvl);
        final double wheelaCompound = wheelaMotorLvl; // Eq. 2.2.3
        final double wheelbCompound = wheelbMotorLvl; // Eq. 2.2.3

        /**
         * Compute Noise Level from flow_rate and speed - Eq 2.2.1
         */
        double lvLvl = Vperhour2NoiseLevel(lvCompound, parameters.getLvPerHour(), parameters.getSpeedLv());
        double medLvl = Vperhour2NoiseLevel(medCompound, parameters.getMvPerHour(), parameters.getSpeedMv());
        double hgvLvl = Vperhour2NoiseLevel(hgvCompound, parameters.getHgvPerHour(), parameters.getSpeedHgv());
        double wheelaLvl = Vperhour2NoiseLevel(wheelaCompound, parameters.getWavPerHour(), parameters.getSpeedWav());
        double wheelbLvl = Vperhour2NoiseLevel(wheelbCompound, parameters.getWbvPerHour(), parameters.getSpeedWbv());
        return sumDb5(lvLvl, medLvl, hgvLvl, wheelaLvl, wheelbLvl);
    }
}