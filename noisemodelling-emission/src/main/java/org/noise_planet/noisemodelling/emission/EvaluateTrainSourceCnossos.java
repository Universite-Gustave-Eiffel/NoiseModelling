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

import static java.lang.Math.log10;
import static java.lang.Math.min;
import static org.noise_planet.noisemodelling.emission.utils.interpLinear.interpLinear;


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
    public static String getTypeVehicule(String typeVehicule, int spectreVer) { //
        String typeVehiculeUse;
        if (getCnossosTrainData(spectreVer).get("Vehicule").has(typeVehicule)) {
            typeVehiculeUse = typeVehicule;
        }else{
            typeVehiculeUse="Empty";
        }
        return typeVehiculeUse;
    }

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

    // Rolling Noise TODO rename getLambdaValue ?
    public static Double getLambdaValue(String typeVehicule, String refType, int spectreVer, int lambdaId) { //
        int refId = getCnossosTrainData(spectreVer).get("Vehicule").get("Definition").get(typeVehicule).get(refType).intValue();
        String ref = "";
        if(refType=="RefRoughness"){ref = "WheelRoughness";}
        else if(refType=="RefContact"){ref = "ContactFilter";}
        return getCnossosTrainData(spectreVer).get("Vehicule").get(ref).get(String.valueOf(refId)).get("Values").get(lambdaId).doubleValue();
    }
    public static Double getTrackRoughness(int trackRoughnessId, int spectreVer, int lambdaId) { //
        return getCnossosTrainData(spectreVer).get("Track").get("RailRoughness").get(String.valueOf(trackRoughnessId)).get("Values").get(lambdaId).doubleValue();
    }
    public static int getAxlesPerVeh(String typeVehicule, int spectreVer) { //
        return getCnossosTrainData(spectreVer).get("Vehicule").get("Definition").get(typeVehicule).get("Axles").intValue();
    }
    public static double getSpectre(String typeVehicule, String ref, String sourceHeight, int spectreVer, int freqId) { //
        int refId = getCnossosTrainData(spectreVer).get("Vehicule").get("Definition").get(typeVehicule).get(ref).intValue();
        if(ref=="RefTraction") {
            double constantSpeed;
            double accelerationSpeed;
            double decelerationSpeed;
            double idlingSpeed;
            if (refId != 0) {
                constantSpeed = getCnossosTrainData(spectreVer).get("Vehicule").get("ConstantSpeed").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
                accelerationSpeed = getCnossosTrainData(spectreVer).get("Vehicule").get("AccelerationSpeed").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
                decelerationSpeed = getCnossosTrainData(spectreVer).get("Vehicule").get("DecelerationSpeed").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
                idlingSpeed = getCnossosTrainData(spectreVer).get("Vehicule").get("IdlingSpeed").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
            } else {
                constantSpeed = -100;
                accelerationSpeed = -100;
                decelerationSpeed = -100;
                idlingSpeed = -100;
            }
            return constantSpeed;
        }else if(ref=="RefAerodynamic"){
            double aerodynamicNoise;
            aerodynamicNoise = getCnossosTrainData(spectreVer).get("Vehicule").get("AerodynamicNoise").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
            return aerodynamicNoise;
        }else{
            return 0;
        }
    }
    public static double getAeroV0Alpha(String typeVehicule, String ref, int spectreVer, String aeroInf){
        int refId = getCnossosTrainData(spectreVer).get("Vehicule").get("Definition").get(typeVehicule).get(ref).intValue();
        return Double.parseDouble(getCnossosTrainData(spectreVer).get("Vehicule").get("AerodynamicNoise").get(String.valueOf(refId)).get(aeroInf).asText());
    }
    public static Double getBridgeStructural(int bridgeId, int spectreVer, int freqId){
        return getCnossosTrainData(spectreVer).get("Track").get("BridgeConstant").get(String.valueOf(bridgeId)).get("Values").get(freqId).doubleValue();
    }

    public static Double getTrackTransfer(int trackTransferId, int spectreVer, int freqId) { //
        return getCnossosTrainData(spectreVer).get("Track").get("TrackTransfer").get(String.valueOf(trackTransferId)).get("Spectre").get(freqId).doubleValue();
    }
    public static Double getImpactNoise(int impactNoiseId, int spectreVer, int freqId) { //
        return getCnossosTrainData(spectreVer).get("Track").get("ImpactNoise").get(String.valueOf(impactNoiseId)).get("Values").get(freqId).doubleValue();
    }

    public static Double getVehTransfer(String typeVehicule, int spectreVer, int freqId) {
        int RefTransfer = getCnossosTrainData(spectreVer).get("Vehicule").get("Definition").get(typeVehicule).get("RefTransfer").intValue();
        return getCnossosTrainData(spectreVer).get("Vehicule").get("Transfer").get(String.valueOf(RefTransfer)).get("Spectre").get(freqId).doubleValue();

    }
    public static Double getLRoughness(String typeVehicule, int trackRoughnessId, int spectreVer, int idLambda) { //
        double wheelRoughness = getLambdaValue(typeVehicule, "RefRoughness",spectreVer, idLambda);
        double contactFilter = getLambdaValue(typeVehicule, "RefContact",spectreVer, idLambda);
        double trackRoughness = getTrackRoughness(trackRoughnessId, spectreVer, idLambda);
        return 10 * Math.log10(Math.pow(10,wheelRoughness/10) + Math.pow(10,trackRoughness/10) ) + contactFilter;
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

    private static double[] checkNanValue(double[] roughnessLtot) {
        int indice_NaN= 0;
        for(int i = 0; i < 24 ; i++) {
            if (Double.isNaN(roughnessLtot[i])) {
                indice_NaN++;
            }
        }
        for(int i = 0; i < indice_NaN ; i++) {
            roughnessLtot[i] = roughnessLtot[indice_NaN+1];
        }
        return roughnessLtot;
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

    /** get noise level source from number of vehicule **/
    private static Double getNoiseLvlFinal(double base, double numbersource, int numVeh) {
        return base + 10 * Math.log10(numbersource*numVeh);
    }

    /**
     * Track noise evaluation.
     * @param vehiculeParameters Vehicule Noise emission parameters
     * @param trackParameters Track Noise emission parameters
     * @return LWRoll / LWTraction A & B / LWAerodynamic A & B / LWBridge level in dB
     */

     static LWRailWay evaluate(VehiculeParametersCnossos vehiculeParameters, TrackParametersCnossos trackParameters) {

        final int spectreVer = vehiculeParameters.getSpectreVer();

        String typeVehicule = vehiculeParameters.getTypeVehicule();
        double speedVehicule = vehiculeParameters.getSpeed();

        double speedTrack = trackParameters.getSpeed();
        int trackRoughnessId = trackParameters.getRailRoughness();
        int trackTransferId = trackParameters.getTrackTransfer();
        int impactId = trackParameters.getImpactNoise();
        int bridgeId = trackParameters.getBridgeConstant();
        int curvate = trackParameters.getCurvate();

        int axlesPerVeh = getAxlesPerVeh(typeVehicule,spectreVer);
        double speed = min(speedVehicule,speedTrack);

        //  Rolling noise calcul
        double[] lWRolling = evaluateLWRoll(typeVehicule, trackRoughnessId, impactId, speed,trackTransferId,spectreVer,axlesPerVeh);

         double lWSqueal = 0; // correction rolling

        // Traction noise calcul
        double[] lWTractionA = evaluateLWSpectre(typeVehicule,"RefTraction", speed, 0,spectreVer);
         double[] lWTractionB = evaluateLWSpectre(typeVehicule,"RefTraction", speed, 1,spectreVer);

        // Aerodynamic noise calcul
        double[] lWAerodynamicA = evaluateLWSpectre(typeVehicule,"RefAerodynamic", speed, 0,spectreVer);
        double[] lWAerodynamicB = evaluateLWSpectre(typeVehicule,"RefAerodynamic", speed, 1,spectreVer);

        double[] lWBridge = evaluateLWBridge(bridgeId,spectreVer);

        LWRailWay lWRailWay= new LWRailWay(lWRolling, lWTractionA,lWTractionB, lWAerodynamicA,lWAerodynamicB,lWBridge);
        return lWRailWay;
    }

    private static double[] evaluateLWBridge(int bridgeId, int spectreVer) {
        double [] lWBridge= new double[24];
        if(bridgeId==3 || bridgeId==4){
            for(int idFreq = 0; idFreq < 24; idFreq++) {
                lWBridge[idFreq] = getBridgeStructural(bridgeId,spectreVer,idFreq);
            }
        }
        return lWBridge;
    }

    private static double[] evaluateLWSpectre(String typeVehicule,String ref, double speed, int height,int spectreVer) {
        double [] lWSpectre = new double[24];
        for(int idFreq = 0; idFreq < 24; idFreq++) {
            if(height==0){
                lWSpectre[idFreq] = getSpectre(typeVehicule,ref,"A",spectreVer,idFreq);}
            else if(height==1) {
                lWSpectre[idFreq] = getSpectre(typeVehicule, ref, "B", spectreVer, idFreq);
            }
            if(ref=="RefAerodynamic"){
                if(speed<200){
                    lWSpectre[idFreq] =0;
                }else{
                double v0Aero = getAeroV0Alpha(typeVehicule,ref, spectreVer, "V0");
                double alphaAero = getAeroV0Alpha(typeVehicule,ref, spectreVer, "Alpha");
                lWSpectre[idFreq] = lWSpectre[idFreq]+ alphaAero*Math.log10(speed/v0Aero);
                }
            }
        }
        return lWSpectre;
    }

    private static double[] evaluateLWRoll(String typeVehicule, int trackRoughnessId, int impactId, double speed,int trackTransferId, int spectreVer, int axlesPerVeh) {
        double [] trackTransfer = new double[24];
        double [] lWTr = new double[24];
        double [] vehTransfer = new double[24];
        double [] lWVeh = new double[24];
        double [] lWRoll = new double[24];

        // roughnessLtot = CNOSSOS p.19 (2.3.7)
        double[] roughnessLtot = checkNanValue(evaluateRoughnessLtotFreq(typeVehicule, trackRoughnessId, impactId,speed, spectreVer));

        for(int idFreq = 0; idFreq < 24; idFreq++){
            // lWTr = CNOSSOS p.20 (2.3.8)
            trackTransfer[idFreq]= getTrackTransfer(trackTransferId,spectreVer,idFreq);
            // TODO PB LWTR
            lWTr[idFreq] = roughnessLtot[idFreq]+trackTransfer[idFreq]+10*Math.log10(axlesPerVeh);

            // lWVeh = CNOSSOS p.20 (2.3.9)
            vehTransfer[idFreq]= getVehTransfer(typeVehicule,spectreVer,idFreq);

            // TODO PB LWVEH
            lWVeh[idFreq] = roughnessLtot[idFreq]+vehTransfer[idFreq]+10*Math.log10(axlesPerVeh);


            lWRoll[idFreq] = 10*Math.log10(Math.pow(10,lWTr[idFreq]/10)+Math.pow(10,lWVeh[idFreq]/10));
        }
        return lWRoll;
    }
    private static double[] evaluateRoughnessLtotFreq(String typeVehicule, int trackRoughnessId,int impactId, double speed, int spectreVer) {

        double[] roughnessLtotLambda = new double[32];
        double[] lambdaToFreqLog= new double[32];
        double[] freqMedLog = new double[24];

        for(int idLambda = 0; idLambda < 32; idLambda++){
            roughnessLtotLambda[idLambda]= Math.pow(10,getLRoughness(typeVehicule, trackRoughnessId,spectreVer, idLambda)/10); // Lambda
            roughnessLtotLambda[idLambda]= roughnessLtotLambda[idLambda]+Math.pow(10,getImpactNoise(impactId,spectreVer, idLambda)/10); // add impact
            lambdaToFreqLog[idLambda] = Math.log10(getLambdaToFreq(speed,idLambda));
        }
        for(int idFreqMed = 0; idFreqMed < 24; idFreqMed++){
            freqMedLog[idFreqMed]= Math.log10(Math.pow(10,(17+Double.valueOf(idFreqMed))/10));
        }
        double[] roughnessLtotFreq = interpLinear(lambdaToFreqLog, roughnessLtotLambda, freqMedLog);

        for(int idRoughnessLtotFreq = 0; idRoughnessLtotFreq < 24; idRoughnessLtotFreq++){
            roughnessLtotFreq[idRoughnessLtotFreq]= 10*Math.log10(roughnessLtotFreq[idRoughnessLtotFreq]);
        }
        return roughnessLtotFreq;
    }


//    /** compute Noise Level from flow_rate and speed @return**/
//    public static double evaluateLm(double Lw, double Q, double speed, int idFreq) {
//        return Lw+ 10*Math.log10(Q/(1000*speed));
//    }


}


