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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    EvaluateRailwaySourceCnossos evaluateRailwaySourceCnossos = new EvaluateRailwaySourceCnossos();

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownVehicle() {
        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));
        String vehCat = "notsupported";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle

        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setSpectreVer(1);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);

        evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_1() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        int  nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle

        double[] expectedValuesLWRolling = new double[]{98.6704,99.6343,101.5298,102.8865,100.3316,99.6011,100.4072,105.7262,107.2207,108.4848,109.4223,110.1035,111.8706,111.4956,108.5828,104.2152,106.5525,105.2982,103.1594,100.7729,101.1764,100.6417,100.6287,102.1869};
        double[] expectedValuesLWTractionA = new double[]{98.8613,94.7613,92.5613,94.5613,92.7613,92.7613,92.9613,94.7613,94.5613,95.6613,95.5613,98.5613,95.1613,95.0613,95.0613,94.0613,94.0613,99.3613,92.4613,89.4613,86.9613,84.0613,81.4613,79.1613};
        double[] expectedValuesLWTractionB = new double[]{103.1613,99.9613,95.4613,93.9613,93.2613,93.5613,92.8613,92.6613,92.3613,92.7613,92.7613,96.7613,92.6613,92.9613,92.8613,93.0613,93.1613,98.2613,91.4613,88.6613,85.9613,83.3613,80.8613,78.6613};
        double[] expectedValuesLWAerodynamicA = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWAerodynamicB = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWBridge = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};

        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(1);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

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

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        int  nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 3;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle

        RailWayLW lWRailWay = null;

        double[] expectedValuesLWRolling = new double[]{100.8970,101.8434,104.6603,107.0239,104.6611,104.0827,105.2341,109.9994,110.1740,110.1183,110.2914,110.7347,112.4299,111.8073,108.7535,104.3038,106.6040,105.3350,103.1827,100.7862,101.1828,100.6431,100.6290,102.1868};
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(1);

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_3() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));


        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        int  nTracks=4;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 3;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        RailWayLW lWRailWay = null;

        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle

        double[] expectedValuesLWRolling = new double[]{98.6704,99.6343,101.5298,102.8865,100.3316,99.6011,100.4072,105.7262,107.2207,108.4848,109.4223,110.1035,111.8706,111.4956,108.5828,104.2152,106.5525,105.2982,103.1594,100.7729,101.1764,100.6417,100.6287,102.1869};
        double[] expectedValuesLWBridge = new double[]{108.4579,109.4015,111.344,112.4959,111.1415,110.9017,105.8236,109.7833,111.592,115.0733,116.7548,116.8658,117.8667,116.2709,113.2686,102.3774,96.9285,95.8390,85.6001,75.2583,70.6990,62.9177,57.9386,54.5294};

        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(1);

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
            assertEquals(expectedValuesLWBridge[idFreq], lWRailWay.getLWBridge()[idFreq], EPSILON_TEST1);
        }
    }


    @Test
    public void Test_Cnossos_Rail_emission_section_4 () {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));


        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 200;
        int rollingCondition = 0;
        double idlingTime = 0;

        int  nTracks = 2;
        int trackTransfer = 9;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 200;
        double vehicleCommercial = 200;

        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle

        RailWayLW lWRailWay = null;
        double[] expectedValuesLWRolling = new double[]{98.66110, 100.5681, 104.3908, 107.5565, 106.7391, 106.4557, 105.5468, 109.3204, 108.2763, 109.2614, 111.1101, 112.5186, 117.3340, 121.3800, 123.5189, 122.9000, 126.9115, 122.9414, 117.8516, 111.8003, 107.3142, 105.8263, 103.9313, 102.2640};
        double[] expectedValuesLWTractionA = new double[]{98.8613, 94.7613, 92.5613, 94.5613, 92.7613, 92.7613, 92.9613, 94.7613, 94.5613, 95.6613, 95.5613, 98.5613, 95.1613, 95.0613, 95.0613, 94.0613, 94.0613, 99.3613, 92.4613, 89.4613, 86.9613, 84.0613, 81.4613, 79.1613};
        double[] expectedValuesLWTractionB = new double[]{103.1613, 99.96130, 95.46130, 93.96130, 93.26130, 93.56130, 92.86130, 92.66130, 92.36130, 92.76130, 92.76130, 96.76130, 92.66130, 92.96130, 92.86130, 93.06130, 93.16130, 98.26130, 91.46130, 88.66130, 85.96130, 83.36130, 80.86130, 78.6613};
        double[] expectedValuesLWAerodynamicA = new double[]{103.7732, 104.3619, 106.8712, 108.5805, 106.4589, 106.1681, 106.0774, 107.5763, 107.0650, 107.4712, 107.3712, 106.3712, 106.9712, 106.8712, 106.8712, 105.8712, 105.8712, 106.1712, 105.7403, 104.2867, 103.2811, 101.8249, 100.7712, 100.0176};
        double[] expectedValuesLWAerodynamicB = new double[]{27.9057, 29.7057, 30.2057, 28.7057, 28.0057, 28.30570, 27.60570, 27.40570, 27.10570, 27.5057, 27.5057, 27.5057, 27.4057, 27.7057, 27.6057, 96.4057, 101.5057, 101.6057, 96.80570, 28.40570, 28.70570, 29.10570, 29.60570, 30.4057};
        double[] expectedValuesLWBridge = new double[]{-99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99};


        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(1);

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

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
    public void Test_Cnossos_Rail_emission_section_5() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2015.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));


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
        double  vehiclePerHour = tDay1;

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

        int  nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 4;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial, isTunnel, nTracks);

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
                    vehiclePerHour = (1000*vehicleSpeed);
                    expectedValuesLWRolling = expectedValuesLWRolling1;
                    break;
                case 2:
                    vehCat = vehCat2;
                    vehicleSpeed = vehicleSpeed2;
                    tDay = tDay2;
                    tEvening = tEvening2;
                    tNight = tNight2;
                    vehiclePerHour = (1000*vehicleSpeed);
                    expectedValuesLWRolling = expectedValuesLWRolling2;
                    break;
                case 3:
                    vehCat = vehCat3;
                    vehicleSpeed = vehicleSpeed3;
                    tDay = tDay3;
                    tEvening = tEvening3;
                    tNight = tNight3;
                    vehiclePerHour = (int) (1000*vehicleSpeed);
                    expectedValuesLWRolling = expectedValuesLWRolling3;
                    break;
                default:
                    vehCat = "";
                    vehicleSpeed = 0;
                    tDay = 0;
                    tEvening = 0;
                    tNight = 0;
                    vehiclePerHour = (1000*vehicleSpeed);
                    expectedValuesLWRolling = new double[]{-99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99, -99};
            }

            RailWayLW lWRailWay = null;
            RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
            vehicleParameters.setSpectreVer(1);

            lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);


            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
            }
        }

    }

    @Test
    public void Test_Cnossos_Rail_emission_section_6() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        // Set : Take into account the number of units
        double nBUnit = 2;

        int  nTracks=2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        double tDay = 1;
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);

        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        //


        double[] expectedValuesLWRolling = new double[]{48.5606,49.5910,51.5763,56.4920,55.5831,55.4623,55.2207,56.4663,56.3912,
                56.7350,56.4837,59.9648,62.8745,62.6585,59.6990,57.3252,59.7649,61.1898,60.5615,58.7027,55.6686,49.3786,48.0600,45.9195};
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            // Compute sound powers per track meter
            lWRailWay.getLWRolling()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq] , EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC1() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        int  nTracks=2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;


        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{48.5605583070706,49.591094876553,51.5763357483898,56.4920337262433,55.5830577481227,55.4623526520535,55.2207102517505,56.4663370258577,56.3912193484322,56.7349538148708,56.4837006351883,59.9647735129824,62.8744986704598,62.6584506699844,59.6990085689524,57.3251971450722,59.7649235057108,61.1897948776294,60.5615433617675,58.7027221858109,55.6685855421802,49.378644576175,48.0600152459358,45.9195251325526};
        double[] expectedValuesLWRollingE = new double[]{48.5605583070706,49.591094876553,51.5763357483898,56.4920337262433,55.5830577481227,55.4623526520535,55.2207102517505,56.4663370258577,56.3912193484322,56.7349538148708,56.4837006351883,59.9647735129824,62.8744986704598,62.6584506699844,59.6990085689524,57.3251971450722,59.7649235057108,61.1897948776294,60.5615433617675,58.7027221858109,55.6685855421802,49.378644576175,48.0600152459358,45.9195251325526};
        double[] expectedValuesLWRollingN = new double[]{48.5605583070706,49.591094876553,51.5763357483898,56.4920337262433,55.5830577481227,55.4623526520535,55.2207102517505,56.4663370258577,56.3912193484322,56.7349538148708,56.4837006351883,59.9647735129824,62.8744986704598,62.6584506699844,59.6990085689524,57.3251971450722,59.7649235057108,61.1897948776294,60.5615433617675,58.7027221858109,55.6685855421802,49.378644576175,48.0600152459358,45.9195251325526};

        double[] expectedValuesLW_Traction_A_D = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};
        double[] expectedValuesLW_Traction_A_E = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};
        double[] expectedValuesLW_Traction_A_N = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};

        double[] expectedValuesL_Traction_B_D = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};
        double[] expectedValuesL_Traction_B_E = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};
        double[] expectedValuesL_Traction_B_N = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tractionA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tractionB , EPSILON_TEST1);

            //Evening
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tractionA, EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tractionB , EPSILON_TEST1);

            //Night
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            assertEquals(expectedValuesLWRollingN[idFreq], rolling, EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tractionB , EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC2() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        int  nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 2;
        int impactNoise = 1;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{43.8903627967019,44.9655697489009,48.9478577301374,55.5479496503529,55.435644216131,56.2891894988337,57.7651753624208,59.8571890905986,60.9673183839427,61.9895897144138,61.6677155247581,64.860508108409,67.7908958462555,65.626269983869,62.1831439289956,59.579681366753,61.0864098301681,62.2076187832561,61.491437018795,59.6104246527745,56.5374755835627,50.4190128973858,49.0315804220768,46.7955035486733};
        double[] expectedValuesLWRollingE = new double[]{43.8903627967019,44.9655697489009,48.9478577301374,55.5479496503529,55.435644216131,56.2891894988337,57.7651753624208,59.8571890905986,60.9673183839427,61.9895897144138,61.6677155247581,64.860508108409,67.7908958462555,65.626269983869,62.1831439289956,59.579681366753,61.0864098301681,62.2076187832561,61.491437018795,59.6104246527745,56.5374755835627,50.4190128973858,49.0315804220768,46.7955035486733};
        double[] expectedValuesLWRollingN = new double[]{43.8903627967019,44.9655697489009,48.9478577301374,55.5479496503529,55.435644216131,56.2891894988337,57.7651753624208,59.8571890905986,60.9673183839427,61.9895897144138,61.6677155247581,64.860508108409,67.7908958462555,65.626269983869,62.1831439289956,59.579681366753,61.0864098301681,62.2076187832561,61.491437018795,59.6104246527745,56.5374755835627,50.4190128973858,49.0315804220768,46.7955035486733};

        double[] expectedValuesLW_Traction_A_D = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};
        double[] expectedValuesLW_Traction_A_E = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};
        double[] expectedValuesLW_Traction_A_N = new double[]{49.8691001300806,49.7691001300806,49.6691001300806,50.1691001300806,47.0691001300806,54.1691001300806,50.7691001300806,44.2691001300806,46.8691001300806,47.0691001300806,45.6691001300806,44.9691001300806,43.4691001300806,44.5691001300806,41.5691001300806,41.1691001300806,38.9691001300806,37.3691001300806,36.4691001300806,35.3691001300806,31.7691001300806,27.6691001300806,22.4691001300806,20.4691001300806};

        double[] expectedValuesL_Traction_B_D = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};
        double[] expectedValuesL_Traction_B_E = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};
        double[] expectedValuesL_Traction_B_N = new double[]{-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572,-39.030899869572};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tractionA= 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], rolling, EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tractionB , EPSILON_TEST1);

            //Evening
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tractionA, EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tractionB , EPSILON_TEST1);

            //Night
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            assertEquals(expectedValuesLWRollingN[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tractionB , EPSILON_TEST1);

        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC3() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        int  nTracks=4;
        int trackTransfer = 8;
        int railRoughness = 2;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{59.3823815386426,59.7740042542256,61.6645074704619,66.5651015825184,64.9236256151963,63.9195646659437,63.0633891427656,63.1703959172873,62.0842218312365,61.5507094539991,59.9458124597247,62.0451549860138,63.5063034095735,61.62742572976,57.591356100339,54.8777141322954,57.2745023554145,58.7784762578773,58.2320686591241,56.3534564010822,53.2240832593016,46.6986552124813,45.4591519061876,43.3326357315674};
        double[] expectedValuesLWRollingE = new double[]{59.3823815386426,59.7740042542256,61.6645074704619,66.5651015825184,64.9236256151963,63.9195646659437,63.0633891427656,63.1703959172873,62.0842218312365,61.5507094539991,59.9458124597247,62.0451549860138,63.5063034095735,61.62742572976,57.591356100339,54.8777141322954,57.2745023554145,58.7784762578773,58.2320686591241,56.3534564010822,53.2240832593016,46.6986552124813,45.4591519061876,43.3326357315674};
        double[] expectedValuesLWRollingN = new double[]{59.3823815386426,59.7740042542256,61.6645074704619,66.5651015825184,64.9236256151963,63.9195646659437,63.0633891427656,63.1703959172873,62.0842218312365,61.5507094539991,59.9458124597247,62.0451549860138,63.5063034095735,61.62742572976,57.591356100339,54.8777141322954,57.2745023554145,58.7784762578773,58.2320686591241,56.3534564010822,53.2240832593016,46.6986552124813,45.4591519061876,43.3326357315674};

        double[] expectedValuesLW_Traction_A_D = new double[]{46.8588001734408,46.7588001734407,46.6588001734408,47.1588001734408,44.0588001734407,51.1588001734408,47.7588001734407,41.2588001734407,43.8588001734408,44.0588001734407,42.6588001734408,41.9588001734407,40.4588001734407,41.5588001734407,38.5588001734407,38.1588001734408,35.9588001734407,34.3588001734408,33.4588001734407,32.3588001734408,28.7588001734407,24.6588001734408,19.4588001734407,17.4588001734407};
        double[] expectedValuesLW_Traction_A_E = new double[]{46.8588001734408,46.7588001734407,46.6588001734408,47.1588001734408,44.0588001734407,51.1588001734408,47.7588001734407,41.2588001734407,43.8588001734408,44.0588001734407,42.6588001734408,41.9588001734407,40.4588001734407,41.5588001734407,38.5588001734407,38.1588001734408,35.9588001734407,34.3588001734408,33.4588001734407,32.3588001734408,28.7588001734407,24.6588001734408,19.4588001734407,17.4588001734407};
        double[] expectedValuesLW_Traction_A_N = new double[]{46.8588001734408,46.7588001734407,46.6588001734408,47.1588001734408,44.0588001734407,51.1588001734408,47.7588001734407,41.2588001734407,43.8588001734408,44.0588001734407,42.6588001734408,41.9588001734407,40.4588001734407,41.5588001734407,38.5588001734407,38.1588001734408,35.9588001734407,34.3588001734408,33.4588001734407,32.3588001734408,28.7588001734407,24.6588001734408,19.4588001734407,17.4588001734407};

        double[] expectedValuesL_Traction_B_D = new double[]{-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644};
        double[] expectedValuesL_Traction_B_E = new double[]{-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644};
        double[] expectedValuesL_Traction_B_N = new double[]{-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644,-42.0411998258644};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tB , EPSILON_TEST1);

            //Evening
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tB , EPSILON_TEST1);

            //Night
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            assertEquals(expectedValuesLWRollingN[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tB , EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC4() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        int  nTracks=2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{44.5811582203502,45.6116947898327,47.5969356616694,52.5126336395229,51.6036576614023,51.4829525653331,51.2413101650301,52.4869369391373,52.4118192617118,52.7555537281504,52.5043005484679,55.985373426262,58.8950985837394,58.679050583264,55.7196084822321,53.3457970583518,55.7855234189905,57.210394790909,56.5821432750471,54.7233220990906,51.6891854554598,45.3992444894546,44.0806151592154,41.9401250458322};
        double[] expectedValuesLWRollingE = new double[]{43.3317708542672,44.3623074237497,46.3475482955864,51.2632462734399,50.3542702953193,50.2335651992501,49.9919227989471,51.2375495730543,51.1624318956288,51.5061663620674,51.2549131823849,54.735986060179,57.6457112176564,57.429663217181,54.4702211161491,52.0964096922688,54.5361360529075,55.961007424826,55.3327559089641,53.4739347330076,50.4397980893768,44.1498571233716,42.8312277931324,40.6907376797492};
        double[] expectedValuesLWRollingN = new double[]{42.5399583937909,43.5704949632734,45.5557358351102,50.4714338129637,49.562457834843,49.4417527387739,49.2001103384709,50.445737112578,50.3706194351526,50.7143539015912,50.4631007219087,53.9441735997028,56.8538987571802,56.6378507567048,53.6784086556728,51.3045972317925,53.7443235924312,55.1691949643497,54.5409434484879,52.6821222725313,49.6479856289006,43.3580446628953,42.0394153326562,39.898925219273};

        double[] expectedValuesLW_Traction_A_D = new double[]{45.8897000433602,45.7897000433602,45.6897000433602,46.1897000433602,43.0897000433602,50.1897000433602,46.7897000433602,40.2897000433602,42.8897000433602,43.0897000433602,41.6897000433602,40.9897000433602,39.4897000433602,40.5897000433602,37.5897000433602,37.1897000433602,34.9897000433602,33.3897000433602,32.4897000433602,31.3897000433602,27.7897000433602,23.6897000433602,18.4897000433602,16.4897000433602};
        double[] expectedValuesLW_Traction_A_E = new double[]{44.6403126772772,44.5403126772772,44.4403126772772,44.9403126772772,41.8403126772772,48.9403126772772,45.5403126772772,39.0403126772772,41.6403126772772,41.8403126772772,40.4403126772772,39.7403126772772,38.2403126772772,39.3403126772772,36.3403126772772,35.9403126772772,33.7403126772772,32.1403126772772,31.2403126772772,30.1403126772772,26.5403126772772,22.4403126772772,17.2403126772772,15.2403126772772};
        double[] expectedValuesLW_Traction_A_N = new double[]{43.8485002168009,43.7485002168009,43.6485002168009,44.1485002168009,41.0485002168009,48.1485002168009,44.7485002168009,38.2485002168009,40.8485002168009,41.0485002168009,39.6485002168009,38.9485002168009,37.4485002168009,38.5485002168009,35.5485002168009,35.1485002168009,32.9485002168009,31.3485002168009,30.4485002168009,29.3485002168009,25.7485002168009,21.6485002168009,16.4485002168009,14.4485002168009};

        double[] expectedValuesL_Traction_B_D = new double[]{-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712,-43.0102999557712};
        double[] expectedValuesL_Traction_B_E = new double[]{-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647,-44.2596873215647};
        double[] expectedValuesL_Traction_B_N = new double[]{-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093,-45.0514997818093};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tB , EPSILON_TEST1);

            //Evening
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tB , EPSILON_TEST1);

            //Night
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aA= 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            assertEquals(expectedValuesLWRollingN[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tA, EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tB , EPSILON_TEST1);

        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC5() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));
        // TGV-D-U2
        String vehCat1 = "SNCF2";
        double vehicleSpeed1 = 80;
        int rollingCondition1 = 0;
        double idlingTime1 = 0;
        double nBUnit1 = 2;
        double tDay1 = 0.4;
        double tEvening1 = 0.3;
        double tNight1 = 0.25;

        // Corail
        String vehCat2 = "SNCF6";
        double vehicleSpeed2 = 120;
        int rollingCondition2 = 0;
        double idlingTime2 = 0;
        double nBUnit2 = 1;
        double tDay2 = 0.5;
        double tEvening2 = 0.5;
        double tNight2 = 0.2;

        String vehCat3 = "SNCF73";
        double vehicleSpeed3 = 120;
        int rollingCondition3 = 0;
        double idlingTime3 = 0;
        double nBUnit3 = 9;
        double tDay3 = 0.5;
        double tEvening3 = 0.5;
        double tNight3 = 0.2;

        // V2N Train
        String vehCat4 = "SNCF8";
        double vehicleSpeed4 = 100;
        int rollingCondition4 = 0;
        double idlingTime4 = 0;
        double nBUnit4 = 1;
        double tDay4 = 0.2;
        double tEvening4 = 0.3;
        double tNight4 = 0.7;

        String vehCat5 = "SNCF69";
        double vehicleSpeed5 = 100;
        int rollingCondition5 = 0;
        double idlingTime5 = 0;
        double nBUnit5 = 4;
        double tDay5 = 0.2;
        double tEvening5 = 0.3;
        double tNight5 = 0.7;

        // Section
        int  nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour1 = (1000*vehicleSpeed1); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour1*nTracks);
        double deltaTDay1 = 10*Math.log10(tDay1)-deltaL0;
        double deltaTEvening1 = 10*Math.log10(tEvening1)-deltaL0;
        double deltaTNight1 = 10*Math.log10(tNight1)-deltaL0;

        double vehiclePerHour2 = (1000*vehicleSpeed2); //for one vehicle
        deltaL0 = 10*Math.log10(vehiclePerHour2*nTracks);
        double deltaTDay2 = 10*Math.log10(tDay2)-deltaL0;
        double deltaTEvening2 = 10*Math.log10(tEvening2)-deltaL0;
        double deltaTNight2 = 10*Math.log10(tNight2)-deltaL0;

        double vehiclePerHour3 = (1000*vehicleSpeed3); //for one vehicle
        deltaL0 = 10*Math.log10(vehiclePerHour3*nTracks);
        double deltaTDay3 = 10*Math.log10(tDay3)-deltaL0;
        double deltaTEvening3 = 10*Math.log10(tEvening3)-deltaL0;
        double deltaTNight3 = 10*Math.log10(tNight3)-deltaL0;

        double vehiclePerHour4 = (1000*vehicleSpeed4); //for one vehicle
        deltaL0 = 10*Math.log10(vehiclePerHour4*nTracks);
        double deltaTDay4 = 10*Math.log10(tDay4)-deltaL0;
        double deltaTEvening4 = 10*Math.log10(tEvening4)-deltaL0;
        double deltaTNight4 = 10*Math.log10(tNight4)-deltaL0;

        double vehiclePerHour5 = (1000*vehicleSpeed5); //for one vehicle
        deltaL0 = 10*Math.log10(vehiclePerHour5*nTracks);
        double deltaTDay5 = 10*Math.log10(tDay5)-deltaL0;
        double deltaTEvening5 = 10*Math.log10(tEvening5)-deltaL0;
        double deltaTNight5 = 10*Math.log10(tNight5)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{47.1552955205404,48.8498299106887,52.6506665062211,59.9189540729727,57.9682560825282,58.5872885355725,59.0528160959685,60.1904875304058,60.6442541026116,62.4955801250664,64.9184365575419,70.5236596616762,75.9091586312107,76.6780384718047,74.8656580571925,71.8258438346925,69.0422555458139,66.2590037381391,64.4635730861252,62.3174294364882,58.0523015636309,51.1818941714616,49.4692864131497,47.5870164960235};
        double[] expectedValuesLWRollingE = new double[]{46.7153189292102,48.540003543594,52.5625340892052,60.0787091329295,57.9923893388288,58.6495173688799,59.0916185428417,60.1979886666328,60.6185205324881,62.4960038970257,64.9683137433641,70.562548216149,75.9313127158166,76.6869876714216,74.8713872507045,71.8271267251379,69.0163596754784,66.1966324248387,64.3988216929349,62.2298468109704,57.9544897520346,51.0604922061408,49.3019210916908,47.4515242894244};
        double[] expectedValuesLWRollingN = new double[]{46.1270508967803,48.0388064828929,51.9977848913093,59.7778707051652,57.4505710153709,58.0747874408452,58.2265981950651,59.2354414276101,59.2675554683469,60.7056375004388,62.8199452089267,67.8798770364391,72.7517670066939,73.2280095787096,71.3038487930857,68.2530122348682,65.6895763573932,63.6625773432251,62.317581482955,60.0794037396615,56.4441459388305,49.9352912430575,48.0608827756958,46.1859326090793};

        double[] expectedValuesLW_Traction_A_D = new double[]{46.6113350513868,46.5361930551615,46.4507560297314,46.4615638226975,43.7430615177986,50.3108176641904,47.0669699562295,41.9733820154675,44.9476552863602,44.0601109307387,43.5845906890585,42.5291935606719,44.0678692086462,42.2820604493695,39.3273037341639,38.7228940509159,37.701456345155,35.0584081257409,34.9065665976502,32.1860323878995,28.9788221314718,24.6576825124615,19.3206918485612,17.4371753071292};
        double[] expectedValuesLW_Traction_A_E = new double[]{45.8328681049925,45.786234654668,45.7429051220906,45.3887520516725,42.8767932132583,49.1270860066919,45.9523201972128,41.2845923821496,44.272113341395,43.2015176915916,42.9875064075318,41.9345847780754,43.7642442442831,41.6353141126725,38.7319900260274,37.9848318047791,37.2510674836558,34.3066693732319,34.2589449165509,31.1934631044681,28.0984026857766,23.7802040911827,18.466879142774,16.6411101334843};
        double[] expectedValuesLW_Traction_A_N = new double[]{46.0848410660751,46.1332009230439,46.2608016902272,44.9756706211064,42.8060094745255,48.4199531690002,45.2738703819419,40.3948530934846,42.8148965285004,42.5567543659976,42.3409101395776,41.8331316056441,41.8487195283546,40.999669853935,38.3337368784949,37.035478413884,36.3931579683877,33.005287899018,32.377048539335,30.045709663381,26.8475690712121,23.0216616855918,18.1249028285137,16.4062831110709};

        double[] expectedValuesL_Traction_B_D = new double[]{28.1206003941747,27.4206004782821,24.7206009653633,22.9206015056726,21.5206021113937,22.6206016195594,24.0206011493702,28.2206003832282,28.7206003321217,23.6206012686259,27.6206004528527,25.1206008727914,23.6206012686259,24.4206010406076,23.4206013325014,22.3206017415914,21.0206023796041,20.020603018198,18.9206039131889,15.1206095083875,12.9206158371834,11.0206245764616,5.12069586340405,3.82072934593712};
        double[] expectedValuesL_Traction_B_E = new double[]{29.8815127708764,29.181512817581,26.4815130880562,24.6815133880888,23.2815137244444,24.3815134513299,25.7815131902348,29.9815127647978,30.4815127364185,25.3815132564572,29.3815128034602,26.8815130366512,25.3815132564572,26.1815131298392,25.1815132919272,24.0815135190939,22.7815138733811,21.7815142279909,20.6815147249775,16.8815178319809,14.6815213463517,12.7815261992633,6.88156578498707,5.58158437797714};
        double[] expectedValuesL_Traction_B_N = new double[]{33.5612804391897,32.8612804536025,30.1612805370699,28.3612806296585,26.9612807334563,28.0612806491744,29.4612805686017,33.6612804373138,34.1612804285561,29.0612805890376,33.0612804492449,30.5612805212065,29.0612805890376,29.8612805499639,28.8612805999835,27.7612806700861,26.4612807794175,25.4612808888484,24.3612810422162,20.5612820010234,18.361283085543,16.4612845831314,10.561296799165,9.26130253693161};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters1 = new RailwayVehicleParametersCnossos(vehCat1, vehicleSpeed1,vehiclePerHour1, rollingCondition1,idlingTime1);
        RailwayVehicleParametersCnossos vehicleParameters2 = new RailwayVehicleParametersCnossos(vehCat2, vehicleSpeed2,vehiclePerHour2, rollingCondition2,idlingTime2);
        RailwayVehicleParametersCnossos vehicleParameters3 = new RailwayVehicleParametersCnossos(vehCat3, vehicleSpeed3,vehiclePerHour3, rollingCondition3,idlingTime3);
        RailwayVehicleParametersCnossos vehicleParameters4 = new RailwayVehicleParametersCnossos(vehCat4, vehicleSpeed4,vehiclePerHour4, rollingCondition4,idlingTime4);
        RailwayVehicleParametersCnossos vehicleParameters5 = new RailwayVehicleParametersCnossos(vehCat5, vehicleSpeed5,vehiclePerHour5, rollingCondition5,idlingTime5);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay1 = evaluateRailwaySourceCnossos.evaluate(vehicleParameters1, trackParameters);
        RailWayLW lWRailWay2 = evaluateRailwaySourceCnossos.evaluate(vehicleParameters2, trackParameters);
        RailWayLW lWRailWay3 = evaluateRailwaySourceCnossos.evaluate(vehicleParameters3, trackParameters);
        RailWayLW lWRailWay4 = evaluateRailwaySourceCnossos.evaluate(vehicleParameters4, trackParameters);
        RailWayLW lWRailWay5 = evaluateRailwaySourceCnossos.evaluate(vehicleParameters5, trackParameters);
        RailWayLW lWRailWay = new RailWayLW();
        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double rolling = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWRolling()[idFreq]+10 * Math.log10(nBUnit1)+deltaTDay1)/10)+Math.pow(10,(lWRailWay2.getLWRolling()[idFreq]+10 * Math.log10(nBUnit2)+deltaTDay2)/10)+Math.pow(10,(lWRailWay3.getLWRolling()[idFreq]+10 * Math.log10(nBUnit3)+deltaTDay3)/10)+Math.pow(10,(lWRailWay4.getLWRolling()[idFreq]+10 * Math.log10(nBUnit4)+deltaTDay4)/10)+Math.pow(10,(lWRailWay5.getLWRolling()[idFreq]+10 * Math.log10(nBUnit5)+deltaTDay5)/10));
            double tractionA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTDay1)/10)+Math.pow(10,(lWRailWay2.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTDay2)/10)+Math.pow(10,(lWRailWay3.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTDay3)/10)+Math.pow(10,(lWRailWay4.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTDay4)/10)+Math.pow(10,(lWRailWay5.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTDay5)/10));
            double tractionB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTDay1)/10)+Math.pow(10,(lWRailWay2.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTDay2)/10)+Math.pow(10,(lWRailWay3.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTDay3)/10)+Math.pow(10,(lWRailWay4.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTDay4)/10)+Math.pow(10,(lWRailWay5.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTDay5)/10));
            double aeroA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTDay1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTDay2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTDay3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTDay4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTDay5)/10));
            double aeroB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTDay1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTDay2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTDay3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTDay4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTDay5)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tractionB , EPSILON_TEST1);

            //Evening
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWRolling()[idFreq]+10 * Math.log10(nBUnit1)+deltaTEvening1)/10)+Math.pow(10,(lWRailWay2.getLWRolling()[idFreq]+10 * Math.log10(nBUnit2)+deltaTEvening2)/10)+Math.pow(10,(lWRailWay3.getLWRolling()[idFreq]+10 * Math.log10(nBUnit3)+deltaTEvening3)/10)+Math.pow(10,(lWRailWay4.getLWRolling()[idFreq]+10 * Math.log10(nBUnit4)+deltaTEvening4)/10)+Math.pow(10,(lWRailWay5.getLWRolling()[idFreq]+10 * Math.log10(nBUnit5)+deltaTEvening5)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTEvening1)/10)+Math.pow(10,(lWRailWay2.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTEvening2)/10)+Math.pow(10,(lWRailWay3.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTEvening3)/10)+Math.pow(10,(lWRailWay4.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTEvening4)/10)+Math.pow(10,(lWRailWay5.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTEvening5)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTEvening1)/10)+Math.pow(10,(lWRailWay2.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTEvening2)/10)+Math.pow(10,(lWRailWay3.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTEvening3)/10)+Math.pow(10,(lWRailWay4.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTEvening4)/10)+Math.pow(10,(lWRailWay5.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTEvening5)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTEvening1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTEvening2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTEvening3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTEvening4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTEvening5)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTEvening1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTEvening2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTEvening3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTEvening4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTEvening5)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tractionB , EPSILON_TEST1);

            //Night
            rolling = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWRolling()[idFreq]+10 * Math.log10(nBUnit1)+deltaTNight1)/10)+Math.pow(10,(lWRailWay2.getLWRolling()[idFreq]+10 * Math.log10(nBUnit2)+deltaTNight2)/10)+Math.pow(10,(lWRailWay3.getLWRolling()[idFreq]+10 * Math.log10(nBUnit3)+deltaTNight3)/10)+Math.pow(10,(lWRailWay4.getLWRolling()[idFreq]+10 * Math.log10(nBUnit4)+deltaTNight4)/10)+Math.pow(10,(lWRailWay5.getLWRolling()[idFreq]+10 * Math.log10(nBUnit5)+deltaTNight5)/10));
            tractionA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTNight1)/10)+Math.pow(10,(lWRailWay2.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTNight2)/10)+Math.pow(10,(lWRailWay3.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTNight3)/10)+Math.pow(10,(lWRailWay4.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTNight4)/10)+Math.pow(10,(lWRailWay5.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTNight5)/10));
            tractionB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTNight1)/10)+Math.pow(10,(lWRailWay2.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTNight2)/10)+Math.pow(10,(lWRailWay3.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTNight3)/10)+Math.pow(10,(lWRailWay4.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTNight4)/10)+Math.pow(10,(lWRailWay5.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTNight5)/10));
            aeroA = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit1)+deltaTNight1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit2)+deltaTNight2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit3)+deltaTNight3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit4)+deltaTNight4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit5)+deltaTNight5)/10));
            aeroB = 10*Math.log10(Math.pow(10,(lWRailWay1.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit1)+deltaTNight1)/10)+Math.pow(10,(lWRailWay2.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit2)+deltaTNight2)/10)+Math.pow(10,(lWRailWay3.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit3)+deltaTNight3)/10)+Math.pow(10,(lWRailWay4.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit4)+deltaTNight4)/10)+Math.pow(10,(lWRailWay5.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit5)+deltaTNight5)/10));
           assertEquals(expectedValuesLWRollingN[idFreq], rolling , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tractionA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tractionB , EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC6() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        int  nTracks=2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 1;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial= 120;
        boolean isTunnel = true;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140};
        double[] expectedValuesLW_Traction_A_D = new double[]{-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140};
        double[] expectedValuesL_Traction_B_D = new double[]{-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140};
        double[] expectedValuesLW_Aero_A_D = new double[]{-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140};
        double[] expectedValuesLW_Aero_B_D = new double[]{-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140,-140};

        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            lWRailWay.getLWRolling()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            lWRailWay.getLWTractionA()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            lWRailWay.getLWTractionB()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            lWRailWay.getLWAerodynamicA()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            lWRailWay.getLWAerodynamicB()[idFreq] = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertTrue(expectedValuesLWRollingD[idFreq] > lWRailWay.getLWRolling()[idFreq]);
            assertTrue(expectedValuesLW_Traction_A_D[idFreq]> lWRailWay.getLWTractionA()[idFreq] );
            assertTrue(expectedValuesL_Traction_B_D[idFreq]> lWRailWay.getLWTractionB()[idFreq] );
            assertTrue(expectedValuesLW_Aero_A_D[idFreq]> lWRailWay.getLWAerodynamicA()[idFreq] );
            assertTrue(expectedValuesLW_Aero_B_D[idFreq]> lWRailWay.getLWAerodynamicB()[idFreq] );

        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC7() {

        evaluateRailwaySourceCnossos.setEvaluateRailwaySourceCnossos(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"), EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

        String vehCat = "SNCF2";
        double vehicleSpeed = 300;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        int  nTracks=2;
        int trackTransfer = 5;
        int railRoughness = 2;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 350;
        double vehicleCommercial= 300;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000*vehicleSpeed); //for one vehicle
        double deltaL0 = 10*Math.log10(vehiclePerHour*nTracks);
        double deltaTDay = 10*Math.log10(tDay)-deltaL0;
        double deltaTEvening = 10*Math.log10(tEvening)-deltaL0;
        double deltaTNight = 10*Math.log10(tNight)-deltaL0;

        // Expected values
        double[] expectedValuesLWRollingD = new double[]{33.6733596010837,34.95169576625,38.2539731975929,44.1801059537931,42.888610835611,44.0148743871629,45.6800277990472,47.6677743937925,48.7556304666653,51.3814838437463,52.4049926613164,57.631720998909,63.3357285948357,66.1337582772167,66.4000139556384,66.2606389650649,67.9407445637443,68.7526472395718,67.7497360005367,64.0167245325835,58.7017301999714,52.2571748232612,52.5279874725351,50.9834474448221};
        double[] expectedValuesLWRollingE = new double[]{32.4239722350007,33.702308400167,37.0045858315099,42.9307185877101,41.639223469528,42.7654870210799,44.4306404329642,46.4183870277095,47.5062431005823,50.1320964776633,51.1556052952334,56.382333632826,62.0863412287527,64.8843709111337,65.1506265895554,65.0112515989819,66.6913571976613,67.5032598734888,66.5003486344537,62.7673371665005,57.4523428338884,51.0077874571782,51.2786001064521,49.7340600787391};
        double[] expectedValuesLWRollingN = new double[]{31.6321597745245,32.9104959396908,36.2127733710337,42.1389061272339,40.8474110090518,41.9736745606036,43.638827972488,45.6265745672333,46.714430640106,49.340284017187,50.3637928347571,55.5905211723497,61.2945287682765,64.0925584506574,64.3588141290791,64.2194391385057,65.899544737185,66.7114474130125,65.7085361739774,61.9755247060243,56.6605303734121,50.2159749967019,50.4867876459759,48.9422476182628};

        double[] expectedValuesLW_Traction_A_D = new double[]{40.149387366083,40.049387366083,39.949387366083,40.449387366083,37.349387366083,44.449387366083,41.049387366083,34.549387366083,37.149387366083,37.349387366083,35.949387366083,35.249387366083,33.749387366083,34.849387366083,31.849387366083,31.449387366083,29.249387366083,27.649387366083,26.749387366083,25.649387366083,22.049387366083,17.949387366083,12.749387366083,10.749387366083};
        double[] expectedValuesLW_Traction_A_E = new double[]{38.9,38.8,38.7,39.2,36.1,43.2,39.8,33.3,35.9,36.1,34.7,34,32.5,33.6,30.6,30.2,28,26.4,25.5,24.4,20.8,16.7,11.5,9.50000000000001};
        double[] expectedValuesLW_Traction_A_N = new double[]{38.1081875395238,38.0081875395237,37.9081875395238,38.4081875395238,35.3081875395237,42.4081875395238,39.0081875395237,32.5081875395237,35.1081875395238,35.3081875395237,33.9081875395238,33.2081875395237,31.7081875395237,32.8081875395237,29.8081875395237,29.4081875395238,27.2081875395237,25.6081875395238,24.7081875395237,23.6081875395238,20.0081875395237,15.9081875395238,10.7081875395238,8.70818753952375};

        double[] expectedValuesL_Traction_B_D = new double[]{-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598,-48.7506126306598};
        double[] expectedValuesL_Traction_B_E = new double[]{-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571,-49.9999999956571};
        double[] expectedValuesL_Traction_B_N = new double[]{-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647,-50.7918124552647};

        double[] expectedValuesLW_Aero_A_D = new double[]{52.449387366083,53.649387366083,52.149387366083,52.449387366083,51.649387366083,52.049387366083,54.649387366083,57.349387366083,58.049387366083,58.349387366083,59.149387366083,57.549387366083,41.049387366083,38.449387366083,35.549387366083,32.449387366083,28.849387366083,26.249387366083,20.049387366083,-19.5506126339131,-62.7506125520996,-62.650612553962,-64.4506125129002,-64.5506125100814};
        double[] expectedValuesLW_Aero_A_E = new double[]{51.2,52.4,50.9,51.2,50.4,50.8,53.4,56.1,56.8,57.1,57.9,56.3,39.8,37.2,34.3,31.2,27.6,25,18.8,-20.7999999999948,-63.9999998909102,-63.8999998933933,-65.6999998386443,-65.7999998348859};
        double[] expectedValuesLW_Aero_A_N = new double[]{50.4081875395238,51.6081875395238,50.1081875395238,50.4081875395238,49.6081875395238,50.0081875395237,52.6081875395238,55.3081875395237,56.0081875395237,56.3081875395237,57.1081875395238,55.5081875395237,39.0081875395237,36.4081875395238,33.5081875395237,30.4081875395238,26.8081875395237,24.2081875395237,18.0081875395237,-21.59181246047,-64.7918123295684,-64.6918123325483,-66.4918122668494,-66.5918122623393};

        double[] expectedValuesLW_Aero_B_D = new double[]{61.449387366083,62.549387366083,63.849387366083,64.949387366083,63.949387366083,62.349387366083,62.349387366083,61.449387366083,62.049387366083,62.349387366083,62.249387366083,60.649387366083,58.549387366083,57.349387366083,51.849387366083,48.949387366083,42.849387366083,16.349387366083,-34.9506126337812,-56.5506126142904,-56.3506126151737,-55.7506126175923,-61.7506125689271,-62.4506125575606};
        double[] expectedValuesLW_Aero_B_E = new double[]{60.2,61.3,62.6,63.7,62.7,61.1,61.1,60.2,60.8,61.1,61,59.4,57.3,56.1,50.6,47.7,41.6,15.1,-36.199999999819,-57.7999999738312,-57.599999975009,-56.9999999782337,-62.9999999133469,-63.6999998981914};
        double[] expectedValuesLW_Aero_B_N = new double[]{59.4081875395238,60.5081875395237,61.8081875395237,62.9081875395238,61.9081875395238,60.3081875395237,60.3081875395237,59.4081875395238,60.0081875395237,60.3081875395237,60.2081875395237,58.6081875395238,56.5081875395237,55.3081875395237,49.8081875395237,46.9081875395238,40.8081875395237,14.3081875395237,-36.991812460259,-58.5918124290736,-58.391812430487,-57.7918124343567,-63.7918123564925,-64.491812338306};

                        // Compute
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
        vehicleParameters.setSpectreVer(2);
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        RailWayLW lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {

            //Day
            double r  = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            double aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTDay)/10));
            assertEquals(expectedValuesLWRollingD[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_D[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_D[idFreq], tB, EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_A_D[idFreq], aA, EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_B_D[idFreq], aB , EPSILON_TEST1);

            //Evening
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            tB  = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            aB  = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTEvening)/10));
            assertEquals(expectedValuesLWRollingE[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_E[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_E[idFreq], tB, EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_A_E[idFreq], aA , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_B_E[idFreq], aB , EPSILON_TEST1);

            //Night
            r = 10*Math.log10(Math.pow(10,(lWRailWay.getLWRolling()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            tB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWTractionB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aA = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicA()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            aB = 10*Math.log10(Math.pow(10,(lWRailWay.getLWAerodynamicB()[idFreq]+10 * Math.log10(nBUnit)+deltaTNight)/10));
            assertEquals(expectedValuesLWRollingN[idFreq], r , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Traction_A_N[idFreq], tA , EPSILON_TEST1);
            assertEquals(expectedValuesL_Traction_B_N[idFreq], tB , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_A_N[idFreq], aA , EPSILON_TEST1);
            assertEquals(expectedValuesLW_Aero_B_N[idFreq], aB , EPSILON_TEST1);

        }
    }
}
