/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.road.cnossos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;

import static org.noise_planet.noisemodelling.emission.utils.Utils.*;

/**
 * Compute the emission sound level of a road segment in dB/m
 * Reference document is "Commission Directive (EU) 2015/996 of 19 May 2015 establishing common noise assessment methods according to Directive 2002/49/EC of the European Parliament and of the Council (Text with EEA relevance)" 2015
 * including its 2020 amendments
 * https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=OJ:JOL_2015_168_R_0001 for 2015 version
 * https://eur-lex.europa.eu/legal-content/GA/TXT/?uri=CELEX:32020L0367 for amendments
 *  @author Nicolas Fortin, Université Gustave Eiffel
 *  @author Pierre Aumond, Université Gustave Eiffel
 */

public class RoadCnossos {
    private static JsonNode RoadCnossos_2015 = parse(RoadCnossos.class.getResourceAsStream("RoadCnossos_2015.json")); // old coefficients in 2015 amendments
    private static JsonNode cnossosData2020 =parse(RoadCnossos.class.getResourceAsStream("RoadCnossos_2020.json")); // new coefficients in 2020 amendments

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }

    /**
     * Get the CNOSSOS coefficients from a specific file version.
     * @param fileVersion 1=RailwayCnossosEU_2020.json; other = RailwayCnossosSNCF_2021.json
     * @return
     */
    public static JsonNode getCnossosData(int fileVersion) {
        if (fileVersion == 1) {
            return RoadCnossos_2015; // old coefficients in 2015 amendments
        } else {
            return cnossosData2020; // new coefficients in 2020 amendments
        }
    }

    /**
     * Get "a" road surface coefficient (also called spectrum) for a frequency value
     * @param Freq Frequency in Hz (octave band)
     * @param vehCat Vehicle category (1,2,3,4a,4b,5)
     * @param roadSurface Road surface identifier - The list is given in the following file : src/main/resources/org/noise_planet/noisemodelling/emission/RoadCnossos_2020.json
     *                    search for NL01 or FR_R2 for example
     * @param fileVersion 2015 or 2019 coefficients version
     * @return a Road Coeff
     */
    public static Double getA_RoadSurfaceCoeff(int Freq, String vehCat, String roadSurface, int fileVersion) throws IOException {
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
        if (getCnossosData(fileVersion).get("roads").get(roadSurface)==null)  throw new IOException("Error : the pavement "+roadSurface + " doesn't exist in the database.");
        return getCnossosData(fileVersion).get("roads").get(roadSurface).get("ref").get(vehCat).get("spectrum").get(Freq_ind).doubleValue();
    }

    /**
     * Get "b" road surface coefficient (also called ßm) for a frequency value
     * @param vehCat Vehicle category (1,2,3,4a,4b,5)
     * @param roadSurface Road surface identifier - The list is given in the following file : src/main/resources/org/noise_planet/noisemodelling/emission/RoadCnossos_2020.json
     *                    search for NL01 or FR_R2 for example
     * @param fileVersion 2015 or 2019 coefficients version
     * @return a Road Coeff
     */
    public static Double getB_RoadSurfaceCoeff(String vehCat, String roadSurface, int fileVersion) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        return getCnossosData(fileVersion).get("roads").get(roadSurface).get("ref").get(vehCat).get("ßm").doubleValue();
    }

    /**
     * Get "Cr" coefficient related to the decrease in rolling noise near an intersection (due to deceleration and acceleration phases).
     * @param vehCat Vehicle category (1,2,3,4a,4b,5)
     * @param k k=1 Crossing lights, k=2 roundabout
     * @param fileVersion 2015 or 2019 coefficients version
     * @return Cr coefficient
     */
    public static double getCr(String vehCat, int k, int fileVersion) {
        return getCnossosData(fileVersion).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cr").doubleValue();
    }

    /**
     * Get "Cp" coefficient related to the increase in propulsion noise near an intersection (due to deceleration and acceleration phases).
     * @param vehCat Vehicle category
     * @param k k=1 Crossing lights, k=2 roundabout
     * @param fileVersion 2015 or 2019 coefficients version
     * @return Cp coefficient
     */
    public static double getCp(String vehCat, int k, int fileVersion) {
        return getCnossosData(fileVersion).get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cp").doubleValue();
    }

    /**
     * get vehicle emission values coefficients
     * @param coeff ar,br,ap,bp,a,b (ar, br, rolling noise / ap,bp, propulsion noise / a,b, studded tyres)
     * @param freq Frequency in Hz (octave band)
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
     * Get rolling or motor sound level in dB at a specific speed
     * @param base coeff A
     * @param adj coeff B
     * @param speed vm in km/h
     * @param speedBase vref in km/h
     * @return
     */
    public static Double getNoiseLvl(double base, double adj, double speed,
                                     double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }

    /**
     * Correction for studded tyres - Eq. 2.2.6
     * only for light vehicles
     * if speed is over 50 km/h or below 90 km/h the correction is limited.
     * @param roadCnossosParameters every others parameters linked to RoadCnossosParameters class (e.g. speed on the road segment)
     * @param Pm_stud proportion of vehicle equipped of studded tyres
     * @param Ts_stud number of months they are equipped with studded tires
     * @param freq Frequency in Hz (octave band)
     * @param fileVersion
     * @return
     */
    private static Double getDeltaStuddedTyres(RoadCnossosParameters roadCnossosParameters, double Pm_stud, double Ts_stud,
                                               int freq, int fileVersion, double vRef) throws IOException {
        double speed = roadCnossosParameters.getSpeedLv();
        double ps = Pm_stud * Ts_stud / 12;  // Eq. 2.2.7 yearly average proportion of vehicles equipped with studded tyres
        speed = (speed >= 90) ? 90 : speed;
        speed = (speed <= 50) ? 50 : speed;
        double deltastud = getNoiseLvl(getCoeff("a", freq, "1", fileVersion), getCoeff("b", freq, "1", fileVersion), speed, vRef);
        return 10 * Math.log10((1 - ps) + ps * Math.pow(10, deltastud / 10)); // Eq. 2.2.8
        // Only for light vehicles (Eq.2.2.9)
    }

    /**
     * Get the correction due to the air temperature
     * @param Temperature temperature in °C
     * @param vehicleCategory 1,2,3,4a,4b..
     * @return
     */
    private static Double getDeltaTemperature(double Temperature, String vehicleCategory) {
        double K = 0.08;
        double tempRef = 20;
        switch (vehicleCategory) {
            case "1":
                K = 0.08;
                break;
            case "2":
                K = 0.04;
                break;
            case "3":
                K = 0.04;
                break;
        }

        return K * (tempRef - Temperature);
    }

    /**
     * Get the correction due to the slope
     * Effects on vehicles 1,2 or 3 (light, medium and heavy vehicles)
     * @param roadCnossosParameters every others parameters linked to RoadCnossosParameters class (e.g. speed on the road segment)
     * @param vehicleCategory 1,2,3,4a,4b..
     * @param slopeDirection slope direction in °
     * @return
     * @throws IOException
     */
    private static Double getDeltaSlope(RoadCnossosParameters roadCnossosParameters, String vehicleCategory, double slopeDirection) throws IOException {

        double deltaSlope = 0;
        double slope = slopeDirection * roadCnossosParameters.getSlopePercentage();
        switch (vehicleCategory) {
            case "1":
                if (slope < -6) {
                    deltaSlope = (Math.min(12, -slope) - 6);
                } else if (slope <= 2) {
                    deltaSlope = 0.;
                } else {
                    deltaSlope = ((roadCnossosParameters.getSpeedLv() / 100) * ((Math.min(12, slope) - 2) / 1.5));
                }
                break;
            case "2":
                // Medium and Heavy vehicles (cat 2 and 3) - Eq 2.2.14 and 2.2.15
                if (slope < -4) {
                    deltaSlope = ((roadCnossosParameters.getSpeedMv() - 20) / 100) * (Math.min(12, -slope) - 4) / 0.7;
                } else if (slope <= 0) {
                    deltaSlope = 0.;
                } else {
                    deltaSlope = (roadCnossosParameters.getSpeedMv() / 100) * (Math.min(12, slope));
                }
                break;
            case "3":
                // Medium and Heavy vehicles (cat 2 and 3) - Eq 2.2.14 and 2.2.15
                if (slope < -4) {
                    deltaSlope = ((roadCnossosParameters.getSpeedHgv() - 10) / 100) * (Math.min(12, -slope) - 4) / 0.5;
                } else if (slope <= 0) {
                    deltaSlope = deltaSlope + 0.;
                } else {
                    deltaSlope = deltaSlope + (roadCnossosParameters.getSpeedHgv() / 100) * (Math.min(12, slope)) / 0.8;
                }
                break;
        }
        // no effects on cat 4 vehicles Eq. 2.2.16

        return deltaSlope;
    }

    /**
     * Return the noise emission level of a road segment in dB/m
     * @param roadCnossosParameters every parameters linked to RoadCnossosParameters class (e.g. speed on the road segment)
     * @return Noise level in dB
     */

    public static double evaluate(RoadCnossosParameters roadCnossosParameters) throws IOException {
        final int freqParam = roadCnossosParameters.getFrequency();
        final double Temperature = roadCnossosParameters.getTemperature();
        final double Ts_stud = roadCnossosParameters.getTsStud();
        final double Pm_stud = roadCnossosParameters.getqStudRatio();
        final double Junc_dist = roadCnossosParameters.getJunc_dist();
        final int Junc_type = roadCnossosParameters.getJunc_type();
        final String roadSurface = roadCnossosParameters.getRoadSurface();
        final int coeffVer = roadCnossosParameters.getFileVersion();
        double vRef = 70.;

        /**
         * Rolling Noise
         */
        // Rolling noise level Eq. 2.2.4
        double lvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "1", coeffVer), getCoeff("br", freqParam, "1", coeffVer), roadCnossosParameters.getSpeedLv(), vRef);
        double medRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "2", coeffVer), getCoeff("br", freqParam, "2", coeffVer), roadCnossosParameters.getSpeedMv(), vRef);
        double hgvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam, "3", coeffVer), getCoeff("br", freqParam, "3", coeffVer), roadCnossosParameters.getSpeedHgv(), vRef);
        // Rolling noise is only for categories 1, 2 and 3

        // Correction for studded tyres - Eq. 2.2.6
        if (Pm_stud > 0 && Ts_stud > 0) {
            lvRoadLvl = lvRoadLvl + getDeltaStuddedTyres(roadCnossosParameters, Pm_stud, Ts_stud, freqParam, coeffVer, vRef);
        }

        // Effect of air temperature on rolling noise correction Eq 2.2.10
        lvRoadLvl = lvRoadLvl + getDeltaTemperature(Temperature, "1"); // K = 0.08
        medRoadLvl = medRoadLvl + getDeltaTemperature(Temperature, "2"); // K = 0.04
        hgvRoadLvl = hgvRoadLvl + getDeltaTemperature(Temperature, "3"); // K = 0.04

        /**
         * Propulsion Noise
         */
        // General equation - Eq. 2.2.11
        double lvMotorLvl = getCoeff("ap", freqParam, "1", coeffVer) + getCoeff("bp", freqParam, "1", coeffVer) * (roadCnossosParameters.getSpeedLv() - vRef) / vRef;
        double medMotorLvl = getCoeff("ap", freqParam, "2", coeffVer) + getCoeff("bp", freqParam, "2", coeffVer) * (roadCnossosParameters.getSpeedMv() - vRef) / vRef;
        double hgvMotorLvl = getCoeff("ap", freqParam, "3", coeffVer) + getCoeff("bp", freqParam, "3", coeffVer) * (roadCnossosParameters.getSpeedHgv() - vRef) / vRef;
        double wheelaMotorLvl = getCoeff("ap", freqParam, "4a", coeffVer) + getCoeff("bp", freqParam, "4a", coeffVer) * (roadCnossosParameters.getSpeedWav() - vRef) / vRef;
        double wheelbMotorLvl = getCoeff("ap", freqParam, "4b", coeffVer) + getCoeff("bp", freqParam, "4b", coeffVer) * (roadCnossosParameters.getSpeedWbv() - vRef) / vRef;

        // Effect of road gradients
        // This correction implicitly includes the effect of slope on speed.
        // Light vehicles (cat 1) - Eq 2.2.13
        double sign = 1;
        boolean twoWay = false;
        switch ((int) roadCnossosParameters.getWay()) {
            case 1:
                sign = 1;
                break;
            case 2:
                sign = -1;
                break;
            case 3:
                twoWay = true;
        }

        lvMotorLvl = lvMotorLvl + getDeltaSlope(roadCnossosParameters, "1", sign);
        medMotorLvl = medMotorLvl + getDeltaSlope(roadCnossosParameters, "2", sign);
        hgvMotorLvl = hgvMotorLvl + getDeltaSlope(roadCnossosParameters, "3", sign);

        /**
         * Mixed effects (Rolling & Propulsion)
         */
        // Effect of the acceleration and deceleration of vehicles
        // Todo Here, we should get the Junc_dist by another way that we are doing now to be more precise issue #524
        double coefficientJunctionDistance = Math.max(1 - Math.abs(Junc_dist) / 100, 0);
        // Effect of the acceleration and deceleration of vehicles - Rolling Noise Eq 2.2.17
        lvRoadLvl = lvRoadLvl + getCr("1", Junc_type, coeffVer) * coefficientJunctionDistance;
        medRoadLvl = medRoadLvl + getCr("2", Junc_type, coeffVer) * coefficientJunctionDistance;
        hgvRoadLvl = hgvRoadLvl + getCr("3", Junc_type, coeffVer) * coefficientJunctionDistance;
        // Effect of the acceleration and deceleration of vehicles - Propulsion Noise Eq 2.2.18
        lvMotorLvl = lvMotorLvl + getCp("1", Junc_type, coeffVer) * coefficientJunctionDistance;
        medMotorLvl = medMotorLvl + getCp("2", Junc_type, coeffVer) * coefficientJunctionDistance;
        hgvMotorLvl = hgvMotorLvl + getCp("3", Junc_type, coeffVer) * coefficientJunctionDistance;
        wheelaMotorLvl = wheelaMotorLvl + getCp("4a", Junc_type, coeffVer) * coefficientJunctionDistance;
        wheelbMotorLvl = wheelbMotorLvl + getCp("4b", Junc_type, coeffVer) * coefficientJunctionDistance;

        // Effect of the type of road surface - Eq. 2.2.19
        lvRoadLvl = lvRoadLvl + getNoiseLvl(getA_RoadSurfaceCoeff(freqParam, "1", roadCnossosParameters.getRoadSurface(), coeffVer), getB_RoadSurfaceCoeff("1", roadSurface, coeffVer), roadCnossosParameters.getSpeedLv(), 70.);
        medRoadLvl = medRoadLvl + getNoiseLvl(getA_RoadSurfaceCoeff(freqParam, "2", roadCnossosParameters.getRoadSurface(), coeffVer), getB_RoadSurfaceCoeff("2", roadSurface, coeffVer), roadCnossosParameters.getSpeedMv(), 70.);
        hgvRoadLvl = hgvRoadLvl + getNoiseLvl(getA_RoadSurfaceCoeff(freqParam, "3", roadCnossosParameters.getRoadSurface(), coeffVer), getB_RoadSurfaceCoeff("3", roadSurface, coeffVer), roadCnossosParameters.getSpeedHgv(), 70.);

        // Correction road on propulsion noise - Eq. 2.2.20
        lvMotorLvl = lvMotorLvl + Math.min(getA_RoadSurfaceCoeff(freqParam, "1", roadSurface, coeffVer), 0.);
        medMotorLvl = medMotorLvl + Math.min(getA_RoadSurfaceCoeff(freqParam, "2", roadSurface, coeffVer), 0.);
        hgvMotorLvl = hgvMotorLvl + Math.min(getA_RoadSurfaceCoeff(freqParam, "3", roadSurface, coeffVer), 0.);
        wheelaMotorLvl = wheelaMotorLvl + Math.min(getA_RoadSurfaceCoeff(freqParam, "4a", roadSurface, coeffVer), 0.);
        wheelbMotorLvl = wheelbMotorLvl + Math.min(getA_RoadSurfaceCoeff(freqParam, "4b", roadSurface, coeffVer), 0.);

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
        double lvLvl = Vperhour2NoiseLevel(lvCompound, roadCnossosParameters.getLvPerHour(), roadCnossosParameters.getSpeedLv());
        double medLvl = Vperhour2NoiseLevel(medCompound, roadCnossosParameters.getMvPerHour(), roadCnossosParameters.getSpeedMv());
        double hgvLvl = Vperhour2NoiseLevel(hgvCompound, roadCnossosParameters.getHgvPerHour(), roadCnossosParameters.getSpeedHgv());
        double wheelaLvl = Vperhour2NoiseLevel(wheelaCompound, roadCnossosParameters.getWavPerHour(), roadCnossosParameters.getSpeedWav());
        double wheelbLvl = Vperhour2NoiseLevel(wheelbCompound, roadCnossosParameters.getWbvPerHour(), roadCnossosParameters.getSpeedWbv());

        // In the case of a bi-directional traffic flow, it is necessary to split the flow into two components and correct half for uphill and half for downhill.
        if (twoWay && roadCnossosParameters.getSlopePercentage() != 0) {
            lvRoadLvl = lvRoadLvl - getDeltaSlope(roadCnossosParameters, "1", sign) + getDeltaSlope(roadCnossosParameters, "1", -sign);
            medRoadLvl = medRoadLvl - getDeltaSlope(roadCnossosParameters, "2", sign) + getDeltaSlope(roadCnossosParameters, "2", -sign);
            hgvRoadLvl = hgvRoadLvl - getDeltaSlope(roadCnossosParameters, "3", sign) + getDeltaSlope(roadCnossosParameters, "3", -sign);
            double lvCompound_InverseSlope = sumDbValues(lvRoadLvl, lvMotorLvl);
            double medCompound_InverseSlope = sumDbValues(medRoadLvl, medMotorLvl);
            double hgvCompound_InverseSlope = sumDbValues(hgvRoadLvl, hgvMotorLvl);

            lvLvl = sumDbValues(Vperhour2NoiseLevel(lvCompound, roadCnossosParameters.getLvPerHour() / 2, roadCnossosParameters.getSpeedLv()), Vperhour2NoiseLevel(lvCompound_InverseSlope, roadCnossosParameters.getLvPerHour() / 2, roadCnossosParameters.getSpeedLv()));
            medLvl = sumDbValues(Vperhour2NoiseLevel(medCompound, roadCnossosParameters.getMvPerHour() / 2, roadCnossosParameters.getSpeedMv()), Vperhour2NoiseLevel(medCompound_InverseSlope, roadCnossosParameters.getMvPerHour() / 2, roadCnossosParameters.getSpeedMv()));
            hgvLvl = sumDbValues(Vperhour2NoiseLevel(hgvCompound, roadCnossosParameters.getHgvPerHour() / 2, roadCnossosParameters.getSpeedHgv()), Vperhour2NoiseLevel(hgvCompound_InverseSlope, roadCnossosParameters.getHgvPerHour() / 2, roadCnossosParameters.getSpeedHgv()));
        }

        return sumDb5(lvLvl, medLvl, hgvLvl, wheelaLvl, wheelbLvl);
    }
}