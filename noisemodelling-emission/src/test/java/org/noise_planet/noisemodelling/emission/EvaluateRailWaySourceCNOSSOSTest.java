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
    private static final double EPSILON_TEST1 = 0.1;
    @Test
    public void Test_X_TER_bicaisse_D() {
        String vehCat = "X-TER-bicaisse-D";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int rollingCondition = 0;
        double idlingTime = 0;

        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeTrasnfert = 0;
        int curvature = 0;
        int railRoughness = 4;

        double vMaxInfra = 160;
        double vehicleCommercial= 160;

        LWRailWay lWRailWay = null;

        VehicleParametersCnossos vehicleParameters = new VehicleParametersCnossos(vehCat, vehicleSpeed,
                vehiclePerHour, 0, 0, rollingCondition,idlingTime);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial,false,1);
        lWRailWay = EvaluateRailWaySourceCnossos.evaluate(vehicleParameters, trackParameters);
    }
    @Test
    public void Test_Cnossos_Rail_emission_secion_1() {
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
                tDay, 0, 0, rollingCondition,idlingTime);
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
}
