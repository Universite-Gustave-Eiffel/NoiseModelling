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
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;


/**
 * Return the dB value corresponding to the parameters
 * @author Adrien Le Bellec - 13/05/2020
 */


public class EvaluateTrainSourceCnossos {
// Todo evaluation du niveau sonore d'un train
    private static JsonNode nmpbTraindata = parse(EvaluateTrainSourceCnossos.class.getResourceAsStream("coefficients_train_NMPB.json"));

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }
    public static JsonNode getnmpbTraindata(int spectreVer){
        if (spectreVer==1){
            return nmpbTraindata;
        }
        else {
            return nmpbTraindata;
        }
    }

    public static String getTypeTrain(String typeTrain, int spectreVer) { //
        String typeTrainUse;
        if (getnmpbTraindata(spectreVer).get("Train").has(typeTrain)) {
            typeTrainUse = typeTrain;
        }else{
            typeTrainUse="TGV00-38-100"; // TODO a modifier -> Geostandard
        }
        return typeTrainUse;
    }
    public static Double getSpeedIncrement(String typeTrain, int spectreVer) { //
        return getnmpbTraindata(spectreVer).get("Train").get(typeTrain).get("Source").get("Speed-increment").doubleValue();
    }

    public static Double getTrainVmax(String typeTrain, int spectreVer) { //
        return getnmpbTraindata(spectreVer).get("Train").get(typeTrain).get("Vmax").doubleValue();
    }
    public static Double getTrainVref(String typeTrain, int spectreVer) { //
        return getnmpbTraindata(spectreVer).get("Train").get(typeTrain).get("Vref").doubleValue();
    }
    public static int getNumberSource(String typeTrain, int spectreVer) { //
        return getnmpbTraindata(spectreVer).get("Train").get(typeTrain).get("Source").get("Source-number-0cm").intValue();
    }

    public static Double getbase(String typeTrain, int spectreVer, int freq, double height) { //
        int Freq_ind;
        switch (freq) {
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
            default:
                Freq_ind=0;
        }
        String heightSource;
        if (height==1){
            heightSource="Spectrum50cm";
        }else if (height==2){
            heightSource="Spectrum400cm";
        }
        else {
            heightSource="Spectrum0cm";
        }
        return getnmpbTraindata(spectreVer).get("Train").get(typeTrain).get(heightSource).get(Freq_ind).doubleValue();
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

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(TrainParametersCnossos parameters) {
        final int freqParam = parameters.getFreqParam();
        final int spectreVer = parameters.getSpectreVer();
        double trainLWvm; // LW(v)/m (1 veh/h)
        double trainLWv; // LW(v)
        String typeTrain = getTypeTrain(parameters.getTypeTrain(),spectreVer);
        double speedIncrement = getSpeedIncrement(typeTrain,spectreVer);
        double speedRef = getTrainVref(typeTrain,spectreVer);
        double speedMax = getTrainVmax(typeTrain,spectreVer);
        int numSource = getNumberSource(typeTrain,spectreVer);

        double speed = Math.min(parameters.getSpeed(), speedMax);


        double base = getbase(typeTrain,spectreVer, freqParam , parameters.getHeight());
        trainLWv =getNoiseLvl(base,speed,speedRef,speedIncrement);
        trainLWvm= Vperhour2NoiseLevel(trainLWv , parameters.getVehPerHour(), speed);
        trainLWvm = getNoiseLvlFinal(trainLWvm, numSource, parameters.getNumVeh());
        return trainLWvm;
    }
    public static double evaluateSpeed(String typeEng, String typeWag, Double speed){

        double speedMaxEng = getTrainVmax(typeEng,2);
        double speedMaxWag = getTrainVmax(typeWag,2);

        double speedTrain = Math.min(speedMaxEng, speedMaxWag);
        speed = Math.min(speed, speedTrain);

        return speed;
    }

}


