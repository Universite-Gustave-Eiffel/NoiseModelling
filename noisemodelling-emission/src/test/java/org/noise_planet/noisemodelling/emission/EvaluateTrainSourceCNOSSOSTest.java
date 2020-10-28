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
    private static final int[] FREQUENCIES = new int[]{50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 8000, 10000};
    private static final double[] LAMBDA = new double[]{1000, 800, 630, 500, 400, 315, 250, 200, 160, 120, 100, 80, 63, 50, 40, 31.5, 25, 20, 16, 12, 10, 8, 6.3, 5, 4, 3.2, 2.5, 2, 1.6, 1.2, 1, 0.8};

    @Test
    public void Test_X_TER_bicaisse_D() {
        String vehCat = "X-TER-bicaisse-D";
        int vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int vehPerTrain = 2; // use in rolling noise

        int trackTransfer = 4;
        int railRoughness = 4;

        // double expectedValues = 75.9991;

        double[] LW0m = new double[24];
        double[] LW4m = new double[24];

        for (int idLambda = 0; idLambda < LAMBDA.length; idLambda++) {

        }

        for (int idFreq = 0; idFreq < FREQUENCIES.length; idFreq++) {
            int sourceHeight = 0;

            TrainParametersCnossos parameters = new TrainParametersCnossos(vehCat, vehicleSpeed, vehiclePerHour,
                    vehPerTrain, trackTransfer,railRoughness, sourceHeight, FREQUENCIES[idFreq]);
            LW0m[idFreq] = EvaluateTrainSourceCnossos.evaluate(parameters);

            sourceHeight = 1;
            parameters = new TrainParametersCnossos(vehCat, vehicleSpeed, vehiclePerHour,
                    vehPerTrain, trackTransfer,railRoughness, sourceHeight, FREQUENCIES[idFreq]);
            LW4m[idFreq] = EvaluateTrainSourceCnossos.evaluate(parameters);

        }
    }
}