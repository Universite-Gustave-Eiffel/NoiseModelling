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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Railway noise evaluation from Cnossos reference : COMMISSION DIRECTIVE (EU) 2015/996
 * of 19 May 2015 establishing common noise assessment methods according to Directive 2002/49/EC
 * of the European Parliament and of the Council
 *
 * amending, for the purposes of adapting to scientific and technical progress, Annex II to
 * Directive 2002/49/EC of the European Parliament and of the Council as regards
 * common noise assessment methods
 *
 * part 2.3. Railway noise
 *
 * Return the dB value corresponding to the parameters
 * @author Adrien Le Bellec - univ Gustave eiffel
 * @author Olivier Chiello, Univ Gustave Eiffel
 */

public class EvaluateRailWaySourceCNOSSOSTest {
    private static final double EPSILON_TEST1 = 0.0001;

    @Test
    public void Test_Cnossos_Rail_emission_section_1() {
        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        LWRailWay lWRailWay = null;

        double[] expectedValuesLWRolling = new double[]{98.6704,99.6343,101.5298,102.8865,100.3316,99.6011,100.4072,105.7262,107.2207,108.4848,109.4223,110.1035,111.8706,111.4956,108.5828,104.2152,106.5525,105.2982,103.1594,100.7729,101.1764,100.6417,100.6287,102.1869};
        double[] expectedValuesLWTractionA = new double[]{98.8613,94.7613,92.5613,94.5613,92.7613,92.7613,92.9613,94.7613,94.5613,95.6613,95.5613,98.5613,95.1613,95.0613,95.0613,94.0613,94.0613,99.3613,92.4613,89.4613,86.9613,84.0613,81.4613,79.1613};
        double[] expectedValuesLWTractionB = new double[]{103.1613,99.9613,95.4613,93.9613,93.2613,93.5613,92.8613,92.6613,92.3613,92.7613,92.7613,96.7613,92.6613,92.9613,92.8613,93.0613,93.1613,98.2613,91.4613,88.6613,85.9613,83.3613,80.8613,78.6613};
        double[] expectedValuesLWAerodynamicA = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWAerodynamicB = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWBridge = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};

        VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                tDay, tEvening, tNight, rollingCondition,idlingTime);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial,false,1);
        lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWTractionA[idFreq], lWRailWay.getLWTractionA()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWTractionB[idFreq], lWRailWay.getLWTractionB()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWAerodynamicA[idFreq], lWRailWay.getLWAerodynamicA()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWAerodynamicB[idFreq], lWRailWay.getLWAerodynamicB()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWBridge[idFreq], lWRailWay.getLWBridge()[idFreq], EPSILON_TEST1);

        }
    }
    @Test
    public void Test_Cnossos_Rail_emission_section_2() {
        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 3;
        int bridgeTrasnfert = 0;
        int curvature = 0;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        LWRailWay lWRailWay = null;

        double[] expectedValuesLWRolling = new double[]{100.8970,101.8434,104.6603,107.0239,104.6611,104.0827,105.2341,109.9994,110.1740,110.1183,110.2914,110.7347,112.4299,111.8073,108.7535,104.3038,106.6040,105.3350,103.1827,100.7862,101.1828,100.6431,100.6290,102.1868};
        VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                tDay, tEvening, tNight, rollingCondition,idlingTime);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial,false,1);
        lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
        }
    }
    @Test
    public void Test_Cnossos_Rail_emission_section_3() {
        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks=4;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        LWRailWay lWRailWay = null;

        double[] expectedValuesLWRolling = new double[]{98.6704,99.6343,101.5298,102.8865,100.3316,99.6011,100.4072,105.7262,107.2207,108.4848,109.4223,110.1035,111.8706,111.4956,108.5828,104.2152,106.5525,105.2982,103.1594,100.7729,101.1764,100.6417,100.6287,102.1869};
        VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                tDay, tEvening, tNight, rollingCondition,idlingTime);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial,false,1);
        lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
        }
    }
    @Test
    public void Test_Cnossos_Rail_emission_section_4() {
        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks=2;
        int trackTransfer = 9;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        LWRailWay lWRailWay = null;
        double[] expectedValuesLWRolling = new double[]{98.6611,99.6116,101.4768,102.7945,100.2227,99.0975,98.5652,103.9451,105.7615,110.0754,113.8617,113.7918,113.8773,112.1487,108.7419,103.7803,106.3539,105.1058,102.996,100.5999,101.0251,100.5285,100.5494,102.1402};
        VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                tDay, tEvening, tNight, rollingCondition,idlingTime);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial,false,1);
        lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
        }
    }
    @Test
    public void Test_Cnossos_Rail_emission_section_5() {
        String vehCat;
        double vehicleSpeed;
        double tDay;
        double tEvening;
        double tNight;

        String vehCat1 = "SNCF-BB66400";
        double vehicleSpeed1 = 80;
        double tDay1 = 0.4;
        double tEvening1 = 0.3;
        double tNight1 = 0.25;


        String vehCat2 = "RMR-Cat-1";
        double vehicleSpeed2 = 120;
        double tDay2 = 0.5;
        double tEvening2 = 0.5;
        double tNight2 = 0.5;

        String vehCat3 = "RMR-Cat-9-a";
        double vehicleSpeed3 = 100;
        double tDay3 = 0.2;
        double tEvening3 = 0.3;
        double tNight3 = 0.7;

        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 4;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial, false, 1);

        double[] expectedValuesLWRolling;
        double[] expectedValuesLWRolling1 = new double[]{97.1163,95.9284,97.6691,99.2991,97.1440,97.4508,99.2167,105.2093,107.0484,108.4798,109.5263,110.2781,112.0743,111.6404,108.6516,104.1867,106.4632,105.1549,102.952,100.4917,100.9441,100.4491,100.4702,102.0818};
        double[] expectedValuesLWRolling2 = new double[]{102.3819,101.3591,102.0322,102.2927,100.1769,99.86140,101.3709,104.9785,104.7817,105.1695,107.2118,111.4597,117.2867,118.8195,117.2068,114.3600,112.0048,109.4634,105.5892,104.2252,101.6110,100.1356,100.5837,100.3448};
        double[] expectedValuesLWRolling3 = new double[]{100.0997,98.93480,99.69780,100.8341,98.85870,98.98000,100.6952,104.5915,105.3080,106.4464,108.5190,111.5829,115.6512,115.9799,113.7622,110.2023,107.1972,105.9335,104.4154,102.0925,100.1482,100.1865,99.95900,100.3303};

        for (int i = 1; i < 4; i++) {
            switch (i) {
                case 1:
                    vehCat = vehCat1;
                    vehicleSpeed = vehicleSpeed1;
                    tDay = tDay1;
                    tEvening = tEvening1;
                    tNight = tNight1;
                    expectedValuesLWRolling = expectedValuesLWRolling1;
                    break;
                case 2:
                    vehCat = vehCat2;
                    vehicleSpeed = vehicleSpeed2;
                    tDay = tDay2;
                    tEvening = tEvening2;
                    tNight = tNight2;
                    expectedValuesLWRolling = expectedValuesLWRolling2;
                    break;
                case 3:
                    vehCat = vehCat3;
                    vehicleSpeed = vehicleSpeed3;
                    tDay = tDay3;
                    tEvening = tEvening3;
                    tNight = tNight3;
                    expectedValuesLWRolling = expectedValuesLWRolling3;
                    break;
                default:
                    vehCat = "";
                    vehicleSpeed = 0;
                    tDay = 0;
                    tEvening = 0;
                    tNight = 0;
                    expectedValuesLWRolling = new double[]{-99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99};
            }

            LWRailWay lWRailWay = null;
            VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                    tDay, tEvening, tNight, rollingCondition, idlingTime);
            lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);


            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
            }
        }

    }
}
