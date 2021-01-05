package org.noise_planet.noisemodelling.emission;

import org.junit.Test;
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos.sumDba;

public class EvaluateTrainSourceCNOSSOSTest {
    private static final double EPSILON_TEST1 = 0.01;
    private static final int[] FREQUENCIES = new int[]{50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000};

    @Test
    public void Test_X_TER_bicaisse_D() {
        String vehCat = "X-TER-bicaisse-D";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int vehPerTrain = 2;

        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeConstant = 0;
        int curvate = 0;
        int railRoughness = 4;

        double vMaxInfra = 160;
        LWRailWay lWRailWayA = null;
        LWRailWay lWRailWayB = null;

        for (int sourceHeight = 0; sourceHeight < 2; sourceHeight++) {
            VehiculeParametersCnossos vehiculeParameters = new VehiculeParametersCnossos(vehCat, "", vehPerTrain,
                    vehicleSpeed, vehiclePerHour, 0, 0, sourceHeight);
            TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                    impactNoise, bridgeConstant, curvate);
            if (sourceHeight == 0) {
                lWRailWayA = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
            } else if (sourceHeight == 1) {
                lWRailWayB = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
            }
            // TODO add ref calcul exemple LWRoll = CNOSSOS p.19 (2.3.7)
        }
        /*
            double[] LWRolling = lWRailWayA.getLWRolling();
            double[] LWTractionA = lWRailWayA.getLWTraction();
            double[] LWTractionB = lWRailWayB.getLWRolling();
            double[] LWAerodynamicA = lWRailWayA.getLWAerodynamic();
            double[] LWAerodynamicB = lWRailWayB.getLWAerodynamic();
        */
    }
}
