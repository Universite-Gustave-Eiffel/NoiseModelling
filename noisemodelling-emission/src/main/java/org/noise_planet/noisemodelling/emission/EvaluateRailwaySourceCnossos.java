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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import static java.lang.Math.min;
import static org.noise_planet.noisemodelling.emission.Utils.Vperhour2NoiseLevel;
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
 * @author Olivier Chiello, Univ Gustave Eiffel
 */

public class EvaluateRailwaySourceCnossos {
    private JsonNode CnossosRailWayData = parse(EvaluateRailwaySourceCnossos.class.getResourceAsStream("coefficient_Railway_Cnossos_SNCF.json"));
    private JsonNode CnossosRailWayData2020 = parse(EvaluateRailwaySourceCnossos.class.getResourceAsStream("coefficients_Railway_Cnossos_2020.json"));
    private JsonNode CnossosRailWayDataSncf = parse(EvaluateRailwaySourceCnossos.class.getResourceAsStream("coefficients_Railway_Cnossos_SNCF.json"));
    private JsonNode CnossosVehicleData = parse(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Vehicles_SNCF_2021.json"));
    private JsonNode CnossosTrainData = parse(EvaluateRailwaySourceCnossos.class.getResourceAsStream("Rail_Train_SNCF_2021.json"));

    public void setEvaluateRailwaySourceCnossos(InputStream cnossosVehicleData,InputStream cnossosTrainData ) {
        this.CnossosVehicleData = parse(cnossosVehicleData);
        this.CnossosTrainData = parse(cnossosTrainData);
    }

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }

    public JsonNode getCnossosRailWayData(int spectreVer){
        if (spectreVer==1){
            return CnossosRailWayData2020;
        } else if (spectreVer==2) {
            return CnossosRailWayDataSncf;
        } else {
            return CnossosRailWayData;
        }
    }

    public static<T> Iterable<T> iteratorToIterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    public JsonNode getCnossosVehicleNode(String typeVehicle) {
        JsonNode vehicle = getCnossosVehicleData().get(typeVehicle);
        if(vehicle == null) {
            throw new IllegalArgumentException(String.format("Vehicle %s not found must be one of :\n -%s", typeVehicle,
                    String.join("\n -", iteratorToIterable(getCnossosVehicleData().fieldNames()))));
        }
        return vehicle;
    }

    public JsonNode getCnossosVehicleData(){
        return CnossosVehicleData;
    }

    public JsonNode getCnossosTrainData(){
        return CnossosTrainData;
    }

    public Map<String, Integer> getVehicleFromTrain(String trainName){
        Map<String, Integer> vehicles = null;
        for (Iterator<Map.Entry<String, JsonNode>> it = CnossosTrainData.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> elt = it.next();
            if (trainName.equals(elt.getKey()))
            {

                ObjectMapper mapper = new ObjectMapper();
                vehicles = mapper.convertValue(elt.getValue(),new TypeReference<Map<String, Integer>>(){});
                break;
            }
        }
        return vehicles;
    }



    public boolean isInVehicleList(String trainName) {
        boolean inlist = false;
        for (Iterator<Map.Entry<String, JsonNode>> it = CnossosVehicleData.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> elt = it.next();
            if (trainName.equals(elt.getKey())) {
                inlist = true;
                break;
            }
        }
        return inlist;
    }

    private static int getFreqInd(int freq){
        int Freq_ind;
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
                Freq_ind=3;
                break;
            case 125:
                Freq_ind=4;
                break;
            case 160:
                Freq_ind=5;
                break;
            case 200:
                Freq_ind=6;
                break;
            case 250:
                Freq_ind=7;
                break;
            case 315:
                Freq_ind=8;
                break;
            case 400:
                Freq_ind=9;
                break;
            case 500:
                Freq_ind=10;
                break;
            case 630:
                Freq_ind=11;
                break;
            case 800:
                Freq_ind=12;
                break;
            case 1000:
                Freq_ind=13;
                break;
            case 1250:
                Freq_ind=14;
                break;
            case 1600:
                Freq_ind=15;
                break;
            case 2000:
                Freq_ind=16;
                break;
            case 2500:
                Freq_ind=17;
                break;
            case 3150:
                Freq_ind=18;
                break;
            case 4000:
                Freq_ind=19;
                break;
            case 5000:
                Freq_ind=20;
                break;
            case 8000:
                Freq_ind=21;
                break;
            case 10000:
                Freq_ind=22;
                break;
            default:
                Freq_ind=0;
        }
        return Freq_ind;
    }



    public Double getLambdaValue(String typeVehicle, String refType, int spectreVer, int lambdaId) { //
        int refId = getCnossosVehicleNode(typeVehicle).get(refType).intValue();
        String ref = "";
        if(refType.equals("RefRoughness")){ref = "WheelRoughness";}
        else if(refType.equals("RefContact")){ref = "ContactFilter";}
        return getCnossosRailWayData(spectreVer).get("Vehicle").get(ref).get( String.valueOf(refId)).get("Values").get(lambdaId).doubleValue();
    }
    public Double getTrackRoughness(int trackRoughnessId, int spectreVer, int lambdaId) { //
        return getCnossosRailWayData(spectreVer).get("Track").get("RailRoughness").get( String.valueOf(trackRoughnessId)).get("Values").get(lambdaId).doubleValue();
    }
    public double getAxlesPerVeh(String typeVehicle) { //
        return getCnossosVehicleNode(typeVehicle).get("NbAxlePerVeh").doubleValue();
    }

    public int getNbCoach(String typeVehicle) { //
        int nbCoach ;
        try {
            nbCoach = getCnossosVehicleData().get(typeVehicle).get("NbCoach").intValue();
        } catch (Exception e) {
            nbCoach = 1;
        }

        return nbCoach;
    }

    public double getSpectre(String typeVehicle, String ref, int runningCondition,String sourceHeight, int spectreVer, int freqId) { //
        int refId = getCnossosVehicleNode(typeVehicle).get(ref).intValue();
        if(ref.equals("RefTraction")) {
            double tractionSpectre=0;
            String condition= "ConstantSpeed";
            if (refId != 0) {
                switch(runningCondition){
                    case 0 :
                        condition = "ConstantSpeed";
                        break;
                    case 1 :
                        condition = "AccelerationSpeed";
                        break;
                    case 3 :
                        condition = "DecelerationSpeed";
                        break;
                    case 4 :
                        condition = "IdlingSpeed";
                        break;
                }
                tractionSpectre = getCnossosRailWayData(spectreVer).get("Vehicle").get(condition).get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
            }
            return tractionSpectre;
        }else if(ref.equals("RefAerodynamic") ){
            double aerodynamicNoise;
            aerodynamicNoise = getCnossosRailWayData(spectreVer).get("Vehicle").get("AerodynamicNoise").get(String.valueOf(refId)).get("Values").get(sourceHeight).get(freqId).doubleValue();
            return aerodynamicNoise;
        }else{
            return 0;
        }
    }
    public double getAeroV0Alpha(String typeVehicle, String ref, int spectreVer, String aeroInf){
        int refId = getCnossosVehicleNode(typeVehicle).get(ref).intValue();
        return Double.parseDouble(getCnossosRailWayData(spectreVer).get("Vehicle").get("AerodynamicNoise").get(String.valueOf(refId)).get(aeroInf).asText());
    }
    public Double getBridgeStructural(int bridgeId, int spectreVer, int freqId){
        return getCnossosRailWayData(spectreVer).get("Track").get("BridgeConstant").get(String.valueOf(bridgeId)).get("Values").get(freqId).doubleValue();
    }

    public Double getTrackTransfer(int trackTransferId, int spectreVer, int freqId) { //
        return getCnossosRailWayData(spectreVer).get("Track").get("TrackTransfer").get(String.valueOf(trackTransferId)).get("Spectre").get(freqId).doubleValue();
    }
    public Double getImpactNoise(int impactNoiseId, int spectreVer, int freqId) { //
        return getCnossosRailWayData(spectreVer).get("Track").get("ImpactNoise").get(String.valueOf(impactNoiseId)).get("Values").get(freqId).doubleValue();
    }

    public Double getVehTransfer(String typeVehicle, int spectreVer, int freqId) {
        int RefTransfer = getCnossosVehicleNode(typeVehicle).get("RefTransfer").intValue();
        return getCnossosRailWayData(spectreVer).get("Vehicle").get("Transfer").get(String.valueOf(RefTransfer)).get("Spectre").get(freqId).doubleValue();

    }
    public Double getLRoughness(String typeVehicle, int trackRoughnessId, int spectreVer, int idLambda) { //
        double wheelRoughness = getLambdaValue(typeVehicle, "RefRoughness",spectreVer, idLambda);
        //double contactFilter = getLambdaValue(typeVehicle, "RefContact",spectreVer, idLambda);
        double trackRoughness = getTrackRoughness(trackRoughnessId, spectreVer, idLambda);
        return 10 * Math.log10(Math.pow(10,wheelRoughness/10) + Math.pow(10,trackRoughness/10) ) ;//+ contactFilter;
    }

    private double[] checkNanValue(double[] roughnessLtot) {
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

    /**
    * Track noise evaluation.
    * @param vehicleParameters Vehicle Noise emission parameters
    * @param trackParameters Track Noise emission parameters
    * constant speed
    
    * @return LWRoll / LWTraction A & B / LWAerodynamic A & B / LWBridge level in dB
    **/
    public RailWayLW evaluate(RailwayVehicleParametersCnossos vehicleParameters, RailwayTrackParametersCnossos trackParameters) {

        final int spectreVer = vehicleParameters.getSpectreVer();

        String typeVehicle = vehicleParameters.getTypeVehicle();
        double speedVehicle = vehicleParameters.getSpeedVehicle();
        double vehPerHour = vehicleParameters.getNumberVehicle();
        double axlesPerVeh = getAxlesPerVeh(typeVehicle);
        int runningCondition = vehicleParameters.getRunningCondition();

        double speedTrack = trackParameters.getSpeedTrack();
        double speedCommercial = trackParameters.getSpeedCommercial();
        int trackRoughnessId = trackParameters.getRailRoughness();
        int trackTransferId = trackParameters.getTrackTransfer();
        int impactId = trackParameters.getImpactNoise();
        int bridgeId = trackParameters.getBridgeTransfert();
        int curvature = trackParameters.getCurvature();

        // get speed of the vehicle
        double speed = min(speedVehicle,min(speedTrack, speedCommercial));

        boolean isTunnel = false ;//trackParameters.getIsTunnel();
        // %% Take into account the number of coach and the number of units
        // 10*log10(NbUnit*NbCoach);


        if(isTunnel){
            double [] lWSpectre = new double[24];
            for(int idFreq = 0; idFreq < 24; idFreq++) {
                lWSpectre[idFreq] =-99;
            }
            RailWayLW lWRailWay = new RailWayLW(lWSpectre, lWSpectre, lWSpectre, lWSpectre, lWSpectre, lWSpectre);
            return lWRailWay;
        }else {
            //  Rolling noise calcul
            double[] lWRolling = evaluateLWroughness("Rolling", typeVehicle, trackRoughnessId, impactId, bridgeId, curvature, speed, trackTransferId, spectreVer, axlesPerVeh);
            // Traction noise calcul
            double[] lWTractionA = evaluateLWSpectre(typeVehicle, "RefTraction", runningCondition, speed, 0, spectreVer);
            double[] lWTractionB = evaluateLWSpectre(typeVehicle, "RefTraction", runningCondition, speed, 1, spectreVer);
            // Aerodynamic noise calcul
            double[] lWAerodynamicA = evaluateLWSpectre(typeVehicle, "RefAerodynamic", runningCondition, speed, 0, spectreVer);
            double[] lWAerodynamicB = evaluateLWSpectre(typeVehicle, "RefAerodynamic", runningCondition, speed, 1, spectreVer);
            // Bridge noise calcul
            double[] lWBridge = evaluateLWroughness("Bridge", typeVehicle, trackRoughnessId, impactId, bridgeId, curvature, speed, trackTransferId, spectreVer, axlesPerVeh);

            for (int i=0;i<lWRolling.length;i++) {
                lWRolling[i] = Vperhour2NoiseLevel(lWRolling[i], vehPerHour*getNbCoach(typeVehicle), speed) ;
                lWTractionA[i] = Vperhour2NoiseLevel(lWTractionA[i], vehPerHour*getNbCoach(typeVehicle), speed);
                lWTractionB[i] = Vperhour2NoiseLevel(lWTractionB[i], vehPerHour*getNbCoach(typeVehicle), speed);
                lWAerodynamicA[i] = Vperhour2NoiseLevel(lWAerodynamicA[i], vehPerHour*getNbCoach(typeVehicle), speed);
                lWAerodynamicB[i] = Vperhour2NoiseLevel(lWAerodynamicB[i], vehPerHour*getNbCoach(typeVehicle), speed);
                lWBridge[i] = Vperhour2NoiseLevel(lWBridge[i], vehPerHour*getNbCoach(typeVehicle), speed);
            }

            RailWayLW lWRailWay = new RailWayLW(lWRolling, lWTractionA, lWTractionB, lWAerodynamicA, lWAerodynamicB, lWBridge);
            return lWRailWay;
        }
    }

    /**
     * traction or Aerodynamic Level.
     * @param typeVehicle vehicle data base
     * @param ref "Traction" "Aerodynamic"
     * @param speed min speed between vehicle and track
     * @param height height source
     * @return lWSpectre(freq) (Traction or Aerodynamic)
     **/
    private double[] evaluateLWSpectre(String typeVehicle,String ref,int runningCondition, double speed, int height,int spectreVer) {
        double [] lWSpectre = new double[24];

        for(int idFreq = 0; idFreq < 24; idFreq++) {
            if(!ref.equals("RefAerodynamic")) {
                if (height == 0) {
                    lWSpectre[idFreq] = getSpectre(typeVehicle, ref, runningCondition, "A", spectreVer, idFreq);
                } else if (height == 1) {
                    lWSpectre[idFreq] = getSpectre(typeVehicle, ref, runningCondition, "B", spectreVer, idFreq);
                }
            }else{
                int refId = getCnossosVehicleNode(typeVehicle).get(ref).intValue();
                if(speed<200  || refId==0){
                    lWSpectre[idFreq] =-99;
                }else{
                    if (height == 0) {
                        lWSpectre[idFreq] = getSpectre(typeVehicle, ref, runningCondition, "A", spectreVer, idFreq);
                    } else if (height == 1) {
                        lWSpectre[idFreq] = getSpectre(typeVehicle, ref, runningCondition, "B", spectreVer, idFreq);
                    }
                    double v0Aero = getAeroV0Alpha(typeVehicle,ref, spectreVer, "V0");
                    double alphaAero = getAeroV0Alpha(typeVehicle,ref, spectreVer, "Alpha");
                    lWSpectre[idFreq] = lWSpectre[idFreq]+ alphaAero*Math.log10(speed/v0Aero);
                }
            }
        }
        return lWSpectre;
    }

    /**
     * Rolling Level.
     * @param typeVehicle vehicle data base
     * @param trackRoughnessId track Roughness reference
     * @param impactId  impact reference
     * @param speed  min speed between vehicle and track
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

    private double[] evaluateLWroughness(String ref, String typeVehicle, int trackRoughnessId, int impactId, int bridgeId, int curvature, double speed,int trackTransferId, int spectreVer, double axlesPerVeh) {
        double [] trackTransfer = new double[24];
        double [] lWTr = new double[24];
        double [] vehTransfer = new double[24];
        double [] lWVeh = new double[24];
        double [] lW = new double[24];

        // roughnessLtot = CNOSSOS p.19 (2.3.7)
        double[] roughnessLtot = checkNanValue(evaluateRoughnessLtotFreq(typeVehicle, trackRoughnessId, impactId,speed, spectreVer));
        if(ref.equals("Rolling")) {
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                // lWTr = CNOSSOS p.20 (2.3.8)
                trackTransfer[idFreq] = getTrackTransfer(trackTransferId, spectreVer, idFreq);
                lWTr[idFreq] = roughnessLtot[idFreq] + trackTransfer[idFreq] + 10 * Math.log10(axlesPerVeh);

                // lWVeh = CNOSSOS p.20 (2.3.9)
                vehTransfer[idFreq] = getVehTransfer(typeVehicle, spectreVer, idFreq);
                lWVeh[idFreq] = roughnessLtot[idFreq] + vehTransfer[idFreq] + 10 * Math.log10(axlesPerVeh);
                // lWRoll = CNOSSOS p.19 (2.3.7)
                lW[idFreq] = 10 * Math.log10(Math.pow(10, lWTr[idFreq] / 10) + Math.pow(10, lWVeh[idFreq] / 10));
                if (curvature == 1) {
                    lW[idFreq] = lW[idFreq] + 5;
                } else if (curvature == 2) {
                    lW[idFreq] = lW[idFreq] + 8;
                } else if (curvature == 3) {
                    lW[idFreq] = lW[idFreq] + 8;
                }
                if (spectreVer==2){
                    if (bridgeId == 2) {
                        lW[idFreq] = lW[idFreq] + 5;
                    }
                }
            }
        }else if(ref.equals("Bridge")){
            double [] lWBridge= new double[24];
            for(int idFreq = 0; idFreq < 24; idFreq++) {
                lW[idFreq] = -99;
            }
            if(spectreVer==1) {
                if (bridgeId == 3 || bridgeId == 4) {
                    for (int idFreq = 0; idFreq < 24; idFreq++) {
                        lWBridge[idFreq] = getBridgeStructural(bridgeId, spectreVer, idFreq);
                        lW[idFreq] = roughnessLtot[idFreq] + lWBridge[idFreq] + 10 * Math.log10(axlesPerVeh);
                    }
                }
            }
        }
        return lW;
    }

    /**
     * Roughness Level.
     * linear interpolation wavelength to frequency
     * @param typeVehicle vehicle data base
     * @param trackRoughnessId track Roughness reference
     * @param impactId  impact reference
     * @param speed  impact reference
     * @return Lroughness(freq)
     **/
    private double[] evaluateRoughnessLtotFreq(String typeVehicle, int trackRoughnessId,int impactId, double speed, int spectreVer) {

        double[] roughnessTotLambda = new double[35];
        double[] roughnessLtot = new double[35];
        double[] contactFilter = new double[35];
        double[] lambdaToFreqLog= new double[35];
        double[] freqMedLog = new double[24];
        double[] Lambda = new double[35];

        double m = 33;
        for(int idLambda = 0; idLambda < 35; idLambda++){
            Lambda[idLambda]= Math.pow(10,m/10);
            lambdaToFreqLog[idLambda] = Math.log10(speed/Lambda[idLambda]*1000/3.6);

            roughnessTotLambda[idLambda]= Math.pow(10,getLRoughness(typeVehicle, trackRoughnessId,spectreVer, idLambda)/10);
            if(impactId!=0) {
                roughnessTotLambda[idLambda] = roughnessTotLambda[idLambda] + Math.pow(10, getImpactNoise(impactId, spectreVer, idLambda) / 10);
            }

            contactFilter[idLambda] = getLambdaValue(typeVehicle, "RefContact",spectreVer, idLambda);
            roughnessLtot[idLambda] = 10*Math.log10(roughnessTotLambda[idLambda])+contactFilter[idLambda];
            roughnessLtot[idLambda] = Math.pow(10,roughnessLtot[idLambda]/10);
            m --;
        }
        for(int idFreqMed = 0; idFreqMed < 24; idFreqMed++){
            freqMedLog[idFreqMed]= Math.log10(Math.pow(10,(17+Double.valueOf(idFreqMed))/10));
        }

        double[] roughnessLtotFreq = interpLinear(lambdaToFreqLog, roughnessLtot, freqMedLog);

        for(int idRoughnessLtotFreq = 0; idRoughnessLtotFreq < 24; idRoughnessLtotFreq++){
            roughnessLtotFreq[idRoughnessLtotFreq]= 10*Math.log10(roughnessLtotFreq[idRoughnessLtotFreq]);
        }
        return roughnessLtotFreq;
    }

}


