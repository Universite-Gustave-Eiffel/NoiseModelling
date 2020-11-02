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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static java.lang.Math.min;


/**
 * Return the dB value corresponding to the parameters
 * @author Adrien Le Bellec - 27/10/2020
 */


public class EvaluateTrainSourceCnossos {
// Todo evaluation du niveau sonore d'un train
    private static JsonNode CnossosTraindata = parse(EvaluateTrainSourceCnossos.class.getResourceAsStream("coefficients_train_cnossos.json"));

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }
    public static JsonNode getCnossosTrainData(int spectreVer){
        if (spectreVer==1){
            return CnossosTraindata;
        }
        else {
            return CnossosTraindata;
        }
    }
    public static String getTypeTrain(String typeTrain, int spectreVer) { //
        String typeTrainUse;
        if (getCnossosTrainData(spectreVer).get("Train").has(typeTrain)) {
            typeTrainUse = typeTrain;
        }else{
            typeTrainUse="Empty";
        }
        return typeTrainUse;
    }

//    public static Double getTrainVmax(String typeTrain, int spectreVer) { //
//        return getCnossosTrainData(spectreVer).get("Train").get(typeTrain).get("Vmax").doubleValue();
//    }

    private static int getFreqInd(int freq){// Todo freq [24] / [18]
        int Freq_ind = 0;
        switch (freq) {
            case 50:
                Freq_ind=0;
                break;
            case 63:
                Freq_ind=1;
                break;
            case 80:
                Freq_ind=2;
                break;
            case 100:
                Freq_ind=0;
                break;
            case 125:
                Freq_ind=1;
                break;
            case 160:
                Freq_ind=2;
                break;
            case 200:
                Freq_ind=3;
                break;
            case 250:
                Freq_ind=4;
                break;
            case 315:
                Freq_ind=5;
                break;
            case 400:
                Freq_ind=6;
                break;
            case 500:
                Freq_ind=7;
                break;
            case 630:
                Freq_ind=8;
                break;
            case 800:
                Freq_ind=9;
                break;
            case 1000:
                Freq_ind=10;
                break;
            case 1250:
                Freq_ind=11;
                break;
            case 1600:
                Freq_ind=12;
                break;
            case 2000:
                Freq_ind=13;
                break;
            case 2500:
                Freq_ind=14;
                break;
            case 3150:
                Freq_ind=15;
                break;
            case 4000:
                Freq_ind=16;
                break;
            case 5000:
                Freq_ind=17;
                break;
            case 8000:
                Freq_ind=17;
                break;
            case 10000:
                Freq_ind=17;
                break;
            default:
                Freq_ind=0;
        }
        return Freq_ind;
    }

    // Rolling Noise
    public static Double getWheelRoughness(String typeTrain, int spectreVer, int Lambda_ind) { //
        int RefRoughness = getCnossosTrainData(spectreVer).get("Train").get("Definition").get(typeTrain).get("RefRoughness").intValue();
        return getCnossosTrainData(spectreVer).get("Train").get("WheelRoughness").get(String.valueOf(RefRoughness)).get("Values").get(Lambda_ind).doubleValue();
    }
    public static Double getContactFilter(String typeTrain, int spectreVer, int Lambda_ind) { //
        int RefContact = getCnossosTrainData(spectreVer).get("Train").get("Definition").get(typeTrain).get("RefContact").intValue();
        return getCnossosTrainData(spectreVer).get("Train").get("ContactFilter").get(String.valueOf(RefContact)).get("Values").get(Lambda_ind).doubleValue();
    }
    public static Double getRailRoughness(int railRoughnessId, int spectreVer, int Lambda_ind) { //
        return getCnossosTrainData(spectreVer).get("Rail").get("RailRoughness").get(String.valueOf(railRoughnessId)).get("Values").get(Lambda_ind).doubleValue();
    }
    public static Double getTrackTransfer(int trackTransferId, int spectreVer, int Freq_ind) { //
        return getCnossosTrainData(spectreVer).get("TrackTransfer").get(String.valueOf(trackTransferId)).get("Spectre").get(Freq_ind).doubleValue();
    }
    public static Double getLRoughness(String typeTrain, int railRoughnessId, int spectreVer, int idLambda) { //
        double wheelRoughness = getWheelRoughness(typeTrain, spectreVer, idLambda);
        double contactFilter = getContactFilter(typeTrain, spectreVer, idLambda);
        double railRoughness = getRailRoughness(railRoughnessId, spectreVer, idLambda);
        return 10 * Math.log10(Math.pow(10,wheelRoughness/10) + Math.pow(10,railRoughness/10) ) + contactFilter;
    }

    private static double getLambdaToFreq(double speed,int idLambda) {
        int n=0;
        double[] Lambda = new double[32];
        for(double m = 30; m > -2 ; m--){
            Lambda[n]= Math.pow(10,m/10);
            n ++;
        }
        return speed/Lambda[idLambda]*1000/3.6; // km/h - m/s || mm - m
    }

    /** get noise level source from speed **/
    private static Double getNoiseLvl(double base, double speed,
                                      double speedRef, double speedIncrement) {
        return base + speedIncrement * Math.log10(speed / speedRef);
    }


    private static Double getNoiseLvldBa(double NoiseLvl,  int freq){
        double LvlCorrectionA;
        switch (freq) {
            case 100:
                LvlCorrectionA=-19.1;
                break;
            case 125:
                LvlCorrectionA=-16.1;
                break;
            case 160:
                LvlCorrectionA=-13.4;
                break;
            case 200:
                LvlCorrectionA=-10.9;
                break;
            case 250:
                LvlCorrectionA=-8.6;
                break;
            case 315:
                LvlCorrectionA=-6.6;
                break;
            case 400:
                LvlCorrectionA=-4.8;
                break;
            case 500:
                LvlCorrectionA=-3.2;
                break;
            case 630:
                LvlCorrectionA=-1.9;
                break;
            case 800:
                LvlCorrectionA=-0.8;
                break;
            case 1000:
                LvlCorrectionA=0;
                break;
            case 1250:
                LvlCorrectionA=0.6;
                break;
            case 1600:
                LvlCorrectionA=1;
                break;
            case 2000:
                LvlCorrectionA=1.2;
                break;
            case 2500:
                LvlCorrectionA=1.3;
                break;
            case 3150:
                LvlCorrectionA=1.2;
                break;
            case 4000:
                LvlCorrectionA=1;
                break;
            case 5000:
                LvlCorrectionA=0.5;
                break;
            default:
                LvlCorrectionA=0;
        }
        return NoiseLvl+LvlCorrectionA;
    }


    /** compute Noise Level from flow_rate and speed **/
    private static Double Vperhour2NoiseLevel(double NoiseLevel, double vperhour, double speed) {
        if (speed > 0 && vperhour !=0) {
            return NoiseLevel + 10 * Math.log10(vperhour / (1000 * speed));
        }else{
            return 0.;
        }
    }


    /** get noise level source from number of vehicule **/
    private static Double getNoiseLvlFinal(double base, double numbersource, int numVeh) {
        return base + 10 * Math.log10(numbersource*numVeh);
    }
    public static final double[] interpLinear(double[] x, double[] y, double[] xi) throws IllegalArgumentException {

        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                yi[i] = Double.NaN;
            }
            else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                }
                else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }

    /**
     * Rail noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */

    public static double evaluate(TrainParametersCnossos parameters) {
        final int freqParam = parameters.getFreqParam();
        final int spectreVer = parameters.getSpectreVer();
        int Freq_ind = getFreqInd(freqParam);

        String typeTrain = parameters.getTypeTrain();
        int railRoughnessId = parameters.getRailRoughness();
        int trackTransferId = parameters.getTrackTransfer();
        double speed = parameters.getSpeed();

        double[] roughnessLtot = evaluateRoughnessLtotFreq(typeTrain, railRoughnessId, speed, Freq_ind, spectreVer); // evaluate L_r_Tot_f
        // Todo niveau de puissance de la contribution voie et de la roue, par véhicule


        double  trackTransfer = getTrackTransfer(trackTransferId,spectreVer, Freq_ind);


        // Todo Traction noise calcul


        // Todo Aerodynamic noise calcul



//        double base = getbase(typeTrain,spectreVer, freqParam , parameters.getHeight());
//        trainLWv =getNoiseLvl(base,speed,speedRef,speedIncrement);
//        trainLWvm= Vperhour2NoiseLevel(trainLWv , parameters.getVehPerHour(), speed);
//        trainLWvm = getNoiseLvlFinal(trainLWvm, numSource, parameters.getNumVeh());

        return roughnessLtot[Freq_ind]; // Todo niveau global(hauteur)
    }

    private static double[] evaluateRoughnessLtotFreq(String typeTrain, int railRoughnessId, double speed, int Freq_ind, int spectreVer) {

        double[] roughnessLtotLambda = new double[32];
        double[] lambdaToFreqLog= new double[32];
        double[] FreqMedLog = new double[24];

        for(int idLambda = 0; idLambda < 32; idLambda++){
            roughnessLtotLambda[idLambda]= Math.pow(10,getLRoughness(typeTrain, railRoughnessId,spectreVer, idLambda)/10); // Lambda
            lambdaToFreqLog[idLambda] = Math.log10(getLambdaToFreq(speed,idLambda));
        }
        for(int idFreqMed = 0; idFreqMed < 24; idFreqMed++){
            FreqMedLog[idFreqMed]= Math.log10(Math.pow(10,(17+Double.valueOf(idFreqMed))/10));
        }
        double[] roughnessLtotFreq = interpLinear(lambdaToFreqLog, roughnessLtotLambda, FreqMedLog);

        for(int idRoughnessLtotFreq = 0; idRoughnessLtotFreq < 24; idRoughnessLtotFreq++){
            roughnessLtotFreq[idRoughnessLtotFreq]= 10*Math.log10(roughnessLtotFreq[idRoughnessLtotFreq]);
        }
        return roughnessLtotFreq;
    }

    public static double evaluateRollingNoise(int test){
        double L_W_roll =0;
        return L_W_roll;
    }




//    public static double evaluateSpeed(String typeEng, String typeWag, Double speed){
//
//        double speedMaxEng = getTrainVmax(typeEng,2);
//        double speedMaxWag = getTrainVmax(typeWag,2);
//
//        double speedTrain = Math.min(speedMaxEng, speedMaxWag);
//        speed = Math.min(speed, speedTrain);
//
//        return speed;
//    }

}


