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
    public void Test_X_TER_bicaisse_D() { // Todo train = vehicule / Rail = Track
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
        double[][] lWRailWay = new double[24][];
        for(int i=0;i<2;i++){
            int sourceHeight = i;

            for (int idFreq = 0; idFreq < FREQUENCIES.length; idFreq++) {

            VehiculeParametersCnossos vehiculeParameters = new VehiculeParametersCnossos(vehCat, "", vehPerTrain,
                    vehicleSpeed, vehiclePerHour, 0, 0, sourceHeight, FREQUENCIES[idFreq]);

            TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                    impactNoise, bridgeConstant, curvate, FREQUENCIES[idFreq]);

            lWRailWay[idFreq] = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
            }
            // TODO extract lWRailWay class getLWRoll()
            // LW lW = new LW();
            // lW.Rolling=LWRolling;
            // TODO optimize for(freq)
            // TODO add ref calcul exemple LWRoll = CNOSSOS p.19 (2.3.7)
            // TODO add Bridge Calcul
        }
    }
}
/*
    @Test
    public void Test_Plamade_TGV_DUPLEX(){
        // N_FERROVIAIRE_TRAFIC
        String veh1 = "TGV-DUPLEX-motrice";
        String veh2 = "TGV-DUPLEX-voiture-1";
        double VMAX = 320;
        int NBVOIWAG = 2;
        double TDIURNE = 1;
        double TSOIR = 1;
        double TNUIT = 1;

        // N_FERROVIAIRE_TRONCON_L
        double VMAXINFRA = 320;
        String BASEVOIE = "B"; // CHAR or STRING plamade convert to int
        String RUGOSITE = "M"; // CHAR or STRING plamade
        String SEMELLE = "M"; // CHAR or STRING plamade
        String PROTCTSUP = "N"; // CHAR or STRING plamade
        String JOINTRAIL = "N"; // CHAR or STRING plamade
        String COURBURE = "N"; // CHAR or STRING plamade
        // convert plamade format to NoiseModelling format

        int trackBase;
        switch (BASEVOIE) {
            case "B":trackBase = 0;break;
            case "S":trackBase = 0;break;
            case "L":trackBase = 0;break;
            case "N":trackBase = 0;break;
            case "T":trackBase = 0;break;
            case "O":trackBase = 0;break;
            default:trackBase = 0;
        }
        int railRoughness;
        switch (RUGOSITE) {
            case "E":railRoughness = 0;break;
            case "M":railRoughness = 0;break;
            case "N":railRoughness = 0;break;
            case "B":railRoughness = 0;break;
            default:railRoughness = 0;
        }
        int railPad;
        switch (SEMELLE) {
            case "S":railPad = 0;break;
            case "M":railPad = 0;break;
            case "N":railPad = 0;break;
            default:railPad = 0;
        }
        int additionalMeasures;
        switch (PROTCTSUP) {
            case "N":additionalMeasures = 0;break;
            case "D":additionalMeasures = 0;break;
            case "B":additionalMeasures = 0;break;
            case "A":additionalMeasures = 0;break;
            case "E":additionalMeasures = 0;break;
            case "O":additionalMeasures = 0;break;
            default:additionalMeasures = 0;
        }
        int railJoints;
        switch (JOINTRAIL) {
            case "N":railJoints = 0;break;
            case "S":railJoints = 0;break;
            case "D":railJoints = 0;break;
            case "M":railJoints = 0;break;
            default:railJoints = 0;
        }
        int curvate;
        switch (COURBURE) {
            case "N":curvate = 0;break;
            case "L":curvate = 0;break;
            case "M":curvate = 0;break;
            case "H":curvate = 0;break;
            default:curvate = 0;
        }

        double[][] LW = new double[24][];
        for (int idFreq = 0; idFreq < FREQUENCIES.length; idFreq++) {
            int sourceHeight = 0;

            //todo for(i=1:2;) veh1 / 2
            TrainParametersCnossos trainParameters = new TrainParametersCnossos(ENGMOTEUR, TYPVOITWAG, NBVOIWAG,
                    VMAX, TDIURNE, TSOIR, TNUIT, sourceHeight, FREQUENCIES[idFreq]);

            RailParametersCnossos railParameters = new RailParametersCnossos(VMAXINFRA, trackBase, railRoughness,
                    railPad, additionalMeasures, railJoints, curvate,FREQUENCIES[idFreq]);

            LW[idFreq] = EvaluateTrainSourceCnossos.evaluate(trainParameters, railParameters);

        }
    }

}*/