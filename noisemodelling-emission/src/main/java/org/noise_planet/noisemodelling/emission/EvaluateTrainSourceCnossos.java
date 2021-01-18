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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;
import static org.noise_planet.noisemodelling.emission.utils.interpLinear.interpLinear;


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
 */


public class EvaluateTrainSourceCnossos {
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
        return Math.log10(speed/Lambda[idLambda]*1000/3.6); // km/h - m/s || mm - m
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
    * constant speed
    * todo add acceleration / deceleration / idling speed ?
    * @return LWRoll / LWTraction A & B / LWAerodynamic A & B / LWBridge level in dB
    **/
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
        double[] lWRolling = evaluateLW("Rolling", typeVehicule, trackRoughnessId, impactId, bridgeId, curvate, speed,trackTransferId,spectreVer,axlesPerVeh);

        // Traction noise calcul
        double[] lWTractionA = evaluateLWSpectre(typeVehicule,"RefTraction", speed, 0,spectreVer);
         double[] lWTractionB = evaluateLWSpectre(typeVehicule,"RefTraction", speed, 1,spectreVer);

        // Aerodynamic noise calcul
        double[] lWAerodynamicA = evaluateLWSpectre(typeVehicule,"RefAerodynamic", speed, 0,spectreVer);
        double[] lWAerodynamicB = evaluateLWSpectre(typeVehicule,"RefAerodynamic", speed, 1,spectreVer);

        // Bridge noise calcul
        double[] lWBridge = evaluateLW("Bridge", typeVehicule, trackRoughnessId, impactId, bridgeId, curvate, speed,trackTransferId,spectreVer,axlesPerVeh);


        LWRailWay lWRailWay= new LWRailWay(lWRolling, lWTractionA,lWTractionB, lWAerodynamicA,lWAerodynamicB,lWBridge);
        return lWRailWay;
    }

    /**
     * traction or Aerodynamic Level.
     * @param typeVehicule vehicule data base
     * @param ref "Traction" "Aerodynamic"
     * @param speed min speed between vehicule and track
     * @param height height source
     * @return lWSpectre(freq) (Traction or Aerodynamic)
     **/
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

    /**
     * Rolling Level.
     * @param typeVehicule vehicule data base
     * @param trackRoughnessId track Roughness reference
     * @param impactId  impact reference
     * @param speed  min speed between vehicule and track
     *
     *               Step 1
     * wavelength to frequecy (evaluateRoughnessLtotFreq)
     *               Step 2
     * calcul sound power of wheel and bogie emission
     * calcul sound power of rail sleeper and ballast/slab emission
     * todo add sound power of superstructure emission ?
     *
     * @return lWRoll(freq)
     **/
    private static double[] evaluateLW(String ref, String typeVehicule, int trackRoughnessId, int impactId, int bridgeId, int curvate, double speed,int trackTransferId, int spectreVer, int axlesPerVeh) {
        double [] trackTransfer = new double[24];
        double [] lWTr = new double[24];
        double [] vehTransfer = new double[24];
        double [] lWVeh = new double[24];
        double [] lW = new double[24];

        // roughnessLtot = CNOSSOS p.19 (2.3.7)
        double[] roughnessLtot = checkNanValue(evaluateRoughnessLtotFreq(typeVehicule, trackRoughnessId, impactId,speed, spectreVer));
        if(ref=="Rolling") {
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                // lWTr = CNOSSOS p.20 (2.3.8)
                trackTransfer[idFreq] = getTrackTransfer(trackTransferId, spectreVer, idFreq);
                lWTr[idFreq] = roughnessLtot[idFreq] + trackTransfer[idFreq] + 10 * Math.log10(axlesPerVeh);

                // lWVeh = CNOSSOS p.20 (2.3.9)
                vehTransfer[idFreq] = getVehTransfer(typeVehicule, spectreVer, idFreq);
                lWVeh[idFreq] = roughnessLtot[idFreq] + vehTransfer[idFreq] + 10 * Math.log10(axlesPerVeh);
                // lWRoll = CNOSSOS p.19 (2.3.7)
                lW[idFreq] = 10 * Math.log10(Math.pow(10, lWTr[idFreq] / 10) + Math.pow(10, lWVeh[idFreq] / 10));
                if (curvate == 1) {
                    lW[idFreq] = lW[idFreq] + 5;
                } else if (curvate == 2) {
                    lW[idFreq] = lW[idFreq] + 8;
                } else if (curvate == 3) {
                    lW[idFreq] = lW[idFreq] + 8;
                }
            }
        }else if(ref=="Bridge"){
            double [] lWBridge= new double[24];
            if(bridgeId==3 || bridgeId==4){
                for(int idFreq = 0; idFreq < 24; idFreq++) {
                    lWBridge[idFreq] = getBridgeStructural(bridgeId,spectreVer,idFreq);
                    lW[idFreq] = roughnessLtot[idFreq] + lWBridge[idFreq] + 10 * Math.log10(axlesPerVeh);
                }
            }
        }
        return lW;
    }

    /**
     * Roughness Level.
     * linear interpolation wavelength to frequency
     * @param typeVehicule vehicule data base
     * @param trackRoughnessId track Roughness reference
     * @param impactId  impact reference
     * @param speed  impact reference
     * @return Lroughness(freq)
     **/
    private static double[] evaluateRoughnessLtotFreq(String typeVehicule, int trackRoughnessId,int impactId, double speed, int spectreVer) {

        double[] roughnessLtotLambda = new double[32];
        double[] lambdaToFreqLog= new double[32];
        double[] freqMedLog = new double[24];
        double[] Lambda = new double[32];

        double m = 30;
        for(int idLambda = 0; idLambda < 32; idLambda++){
            Lambda[idLambda]= Math.pow(10,m/10);
            lambdaToFreqLog[idLambda] = Math.log10(speed/Lambda[idLambda]*1000/3.6);

            roughnessLtotLambda[idLambda]= Math.pow(10,getLRoughness(typeVehicule, trackRoughnessId,spectreVer, idLambda)/10);

            if(impactId!=0) {
                roughnessLtotLambda[idLambda] = roughnessLtotLambda[idLambda] + Math.pow(10, getImpactNoise(impactId, spectreVer, idLambda) / 10);
            }

            m --;
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


