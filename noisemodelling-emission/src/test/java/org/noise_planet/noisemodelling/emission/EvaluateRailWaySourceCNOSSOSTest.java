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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void Test_Cnossos_Rail_emission_section_1() {
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
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial,isTunnel, nTracks);
        lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_3() {
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

            lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);


            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValuesLWRolling[idFreq], lWRailWay.getLWRolling()[idFreq], EPSILON_TEST1);
            }
        }

    }

   /* @Test
    public void Test_Cnossos_Rail_emission_section_6() {
        String vehCat = "SNCF-BB66400";

        double vehicleSpeed = 80;
        double tDay = 1;
        double tEvening = 0.3;
        double tNight = 0.25;


        int rollingCondition = 0;
        double idlingTime = 0;

        int  nTracks=2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = true;

        double vMaxInfra = 160;
        double vehicleCommercial= 120;

        int vehiclePerHour = (int) (1000*vehicleSpeed);

        RailWayLW lWRailWay;

        double[] expectedValuesLWRolling = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWTractionA = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWTractionB = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWAerodynamicA = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWAerodynamicB = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        double[] expectedValuesLWBridge = new double[]{-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99,-99};
        RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(vehCat, vehicleSpeed,vehiclePerHour, rollingCondition,idlingTime);
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
    }*/
}
