/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway.cnossos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.railway.Railway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

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
 * @author Adrien Le Bellec, Université Gustave Eiffel
 * @author Olivier Chiello, Université Gustave Eiffel
 */

public class RailwayCnossos extends Railway {

    public RailwayCnossos() {
    }

    /**
     * Helper to read a Ref field from a vehicle JSON node.
     * Supports both string (new merged file) and integer (legacy files) JSON values.
     * @param vehicleNode the JSON node of the vehicle
     * @param fieldName the Ref field name (e.g. "RefRoughness")
     * @return the string value of the reference
     */
    private static String getRefValue(JsonNode vehicleNode, String fieldName) {
        JsonNode value = vehicleNode.get(fieldName);
        if (value.isTextual()) {
            return value.textValue();
        }
        return String.valueOf(value.intValue());
    }

    /**
     * get Wheel Roughness by wavenumber - Only CNOSSOS
     * @param typeVehicle
     * @param lambdaId
     * @return
     */
    public Double getWheelRoughness(String typeVehicle, int lambdaId) { //
        String refId = getRefValue(getVehicleNode(typeVehicle), "RefRoughness");
        return fetchNode(getRailWayData().get("Vehicle").get("WheelRoughness"), refId).get("Values").get(lambdaId).doubleValue();
    }

    /**
     * get Contact Filter by wavenumber - Only CNOSSOS
     * @param typeVehicle type of a vehicle, for example : SNCF2
     * @param lambdaId wavenumber (between 1 and 34 corresponding the normalised third octave bands from 2000mm to 0.8mm).
     * @return contact filter
     */
    public Double getContactFilter(String typeVehicle, int lambdaId) { //
        String refId = getRefValue(getVehicleNode(typeVehicle), "RefContact");
        return getRailWayData().get("Vehicle").get("ContactFilter").get(refId).get("Values").get(lambdaId).doubleValue();
    }

    /**
     * get Track Roughness Filter by wavenumber - Only CNOSSOS
     * @param trackRoughnessId
     * @param lambdaId
     * @return
     */
    public Double getTrackRoughness(String trackRoughnessId, int lambdaId) { //
        return fetchNode(getRailWayData().get("Track").get("RailRoughness"), trackRoughnessId).get("Values").get(lambdaId).doubleValue();
    }

    /**
     * Axles number by vehicles
     * @param typeVehicle
     * @return
     */
    public double getAxlesPerVeh(String typeVehicle) { //
        return getVehicleNode(typeVehicle).get("NbAxlePerVeh").doubleValue();
    }

    /**
     * get Nb of coach by vehicle
     * @param typeVehicle
     * @return
     */
    public int getNbCoach(String typeVehicle) { //
        int nbCoach;
        try {
            nbCoach = getVehicleData().get(typeVehicle).get("NbCoach").intValue();
        } catch (Exception e) {
            nbCoach = 1;
        }

        return nbCoach;
    }

    /**
     * Get the reflecting barrier effect flag for a vehicle (0 or 1).
     * Open flat freight wagons have 0 (no body to reflect sound), all others have 1.
     * @param typeVehicle Vehicle type identifier (e.g. "SNCF2")
     * @return 0 or 1
     */
    public int getReflectingBarrierEffect(String typeVehicle) {
        try {
            return getVehicleNode(typeVehicle).get("ReflectingBarrierEffect").intValue();
        } catch (Exception e) {
            return 1; // default: reflecting barrier effect is active
        }
    }

    /**
     * Get the total length of a vehicle in meters.
     * @param typeVehicle Vehicle type identifier (e.g. "SNCF2")
     * @return Vehicle length in meters
     */
    public double getVehicleLength(String typeVehicle) {
        try {
            return getVehicleNode(typeVehicle).get("Length").doubleValue();
        } catch (Exception e) {
            return 1.0; // default fallback
        }
    }

    /**
     *
     * @param typeVehicle
     * @param runningCondition
     * @param sourceHeightId
     * @param fileVersion
     * @param freqId
     * @return
     */
    public double getTractionNoise(String typeVehicle, int runningCondition, String sourceHeightId, String fileVersion, int freqId) { //
        String refId = getRefValue(getVehicleNode(typeVehicle), "RefTraction");
        double tractionSpectrum = 0;

        if (refId != null && !refId.isEmpty()) {
            String condition = "ConstantSpeed";
            switch (runningCondition) {
                case 0:
                    condition = "ConstantSpeed";
                    break;
                case 1:
                    condition = "AccelerationSpeed";
                    break;
                case 3:
                    condition = "DecelerationSpeed";
                    break;
                case 4:
                    condition = "IdlingSpeed";
                    break;
            }
            // Resolve traction node dynamically from JSON data
            JsonNode tractionNode = getRailWayData().get("Vehicle").get(condition).get(refId);
            if (tractionNode != null) {
                try {
                    tractionSpectrum = tractionNode.get("Values").get(sourceHeightId).get(freqId).doubleValue();
                } catch (NullPointerException ex) {
                    throw new IllegalArgumentException(String.format(Locale.ROOT, "Could not find traction spectrum for the following parameters " +
                            "getRailWayData(%s).get(\"Vehicle\").get(%s).get" +
                            "(%s).get(\"Values\").get(%s).get(%d)", fileVersion, condition, refId, sourceHeightId, freqId));
                }
            }
        }
        return tractionSpectrum;
    }

    /**
     * retrieve the aerodynamic noise value for a specific type of vehicle, source height, file version, and frequency ID
     * by accessing the corresponding data from the vehicle node, railway data, and noise values.
     * @param typeVehicle
     * @param sourceHeightId
     * @param fileVersion
     * @param freqId
     * @return
     */
    public double getAerodynamicNoise(String typeVehicle, String sourceHeightId, String fileVersion, int freqId) { //
        String refId = getRefValue(getVehicleNode(typeVehicle), "RefAerodynamic");
        return getRailWayData().get("Vehicle").get("AerodynamicNoise").get(refId).get("Values").get(sourceHeightId).get(freqId).doubleValue();
    }

    /**
     * retrieves the structural constant for a specific bridge and frequency ID
     * by accessing the corresponding data from the railway track's bridge constants.
     * @param bridgeId
     * @param freqId
     * @return
     */
    public Double getBridgeStructural(String bridgeId, int freqId) {
        return getRailWayData().get("Track").get("BridgeConstant").get(bridgeId).get("Values").get(freqId).doubleValue();
    }


    /**
     * fetches and returns the transfer value from the railway data for a specific track transfer ID and frequency ID.
     * @param trackTransferId
     * @param freqId
     * @return
     */
    public Double getTrackTransfer(String trackTransferId,  int freqId) { //
        return fetchNode(getRailWayData().get("Track").get("TrackTransfer"), trackTransferId).get("Spectre").get(freqId).doubleValue();
    }

    /**
     *  fetches and returns the impact noise value from the railway data for a specific impact noise ID and frequency ID.
     * @param impactNoiseId
     * @param freqId
     * @return
     */
    public Double getImpactNoise(String impactNoiseId,  int freqId) { //
        return fetchNode(getRailWayData().get("Track").get("ImpactNoise"), impactNoiseId).get("Values").get(freqId).doubleValue();
    }

    /**
     * retrieve and return the transfer value associated with a given vehicle type and a specific frequency ID.
     * @param typeVehicle
     * @param freqId
     * @return
     */
    public Double getVehTransfer(String typeVehicle,  int freqId) {
        String refTransfer = getRefValue(getVehicleNode(typeVehicle), "RefTransfer");
        return getRailWayData().get("Vehicle").get("Transfer").get(refTransfer).get("Spectre").get(freqId).doubleValue();

    }


    /**
     * calculates the total roughness level for a specific type of vehicle,
     * track roughness, vehicle file version, and lambda ID by retrieving the wheel roughness and track roughness
     * @param typeVehicle
     * @param trackRoughnessId
     * @param idLambda
     * @return
     */
    public Double getLRoughness(String typeVehicle, String trackRoughnessId, int idLambda) { //
        double wheelRoughness = getWheelRoughness(typeVehicle, idLambda);
        double trackRoughness = getTrackRoughness(trackRoughnessId, idLambda);
        return 10 * Math.log10(Math.pow(10, wheelRoughness / 10) + Math.pow(10, trackRoughness / 10));
    }

    /**
     * Check if it exists some NaN values in the Roughness sound level.
     * They appear when calculating the roughness in the frequency domain by interpolating the roughness in the wavelength domain
     * @param roughnessLtot
     * @return
     */
    private double[] checkNanValue(double[] roughnessLtot) {
        int indice_NaN = 0;
        for (int i = 0; i < roughnessLtot.length-2; i++) {
            if (Double.isNaN(roughnessLtot[i])) {
                indice_NaN++;
            }
        }
        for (int i = 0; i < indice_NaN; i++) {
            roughnessLtot[i] = roughnessLtot[indice_NaN + 1];
        }
        return roughnessLtot;
    }

    /**
     * Evaluate the sound level for one Vehicle
     * @param vehicleParameters Vehicle Noise emission parameters
     * @param trackParameters Track Noise emission parameters
     * constant speed
     *
     * @return LWRoll / LWTraction A and B / LWAerodynamic A and B / LWBridge level in dB
     **/
    public RailWayCnossosParameters evaluate(RailwayVehicleCnossosParameters vehicleParameters, RailwayTrackCnossosParameters trackParameters) throws IOException {

        String vehicleFileVersion = vehicleParameters.getFileVersion();
        String typeVehicle = vehicleParameters.getTypeVehicle();

        double speedVehicle = vehicleParameters.getSpeedVehicle();
        double vehPerHour = vehicleParameters.getNumberVehicle();
        double axlesPerVeh = getAxlesPerVeh(typeVehicle);
        int runningCondition = vehicleParameters.getRunningCondition();

        double speedTrack = trackParameters.getSpeedTrack();
        double speedCommercial = trackParameters.getSpeedCommercial();
        String trackRoughnessId = trackParameters.getRailRoughness();
        String trackTransferId = trackParameters.getTrackTransfer();
        String impactId = trackParameters.getImpactNoise();
        String bridgeId = trackParameters.getBridgeTransfert();
        int curvature = trackParameters.getCurvature();

        // get speed of the vehicle
        double speed = min(speedVehicle, min(speedTrack, speedCommercial));

        boolean isTunnel = trackParameters.getIsTunnel();
        // %% Take into account the number of coach and the number of units
        // 10*log10(NbUnit*NbCoach);

        RailWayCnossosParameters railWayParameters = new RailWayCnossosParameters();

        if (isTunnel) {
            return railWayParameters;
        } else {
            //  Rolling noise calcul
            double[] lW = getLWRolling(typeVehicle, trackRoughnessId, impactId,  curvature, speed, trackTransferId, axlesPerVeh);
            railWayParameters.addRailwaySource("ROLLING", new LineSource(lW,0.5, "ROLLING"));
            lW = getLWTraction(typeVehicle,  runningCondition,  "A", vehicleFileVersion);
            railWayParameters.addRailwaySource("TRACTIONA", new LineSource(lW,0.5, "TRACTIONA"));
            lW =  getLWTraction(typeVehicle,  runningCondition,  "B", vehicleFileVersion);
            railWayParameters.addRailwaySource("TRACTIONB", new LineSource(lW,4, "TRACTIONB"));
            lW = getLWAero(typeVehicle,   speed, "A", vehicleFileVersion);
            railWayParameters.addRailwaySource("AERODYNAMICA", new LineSource(lW,0.5, "AERODYNAMICA"));
            lW = getLWAero(typeVehicle,   speed, "B", vehicleFileVersion);
            railWayParameters.addRailwaySource("AERODYNAMICB", new LineSource(lW,4, "AERODYNAMICB"));
            lW =  getLWBridge(typeVehicle, trackRoughnessId, impactId, bridgeId, speed,axlesPerVeh);
            railWayParameters.addRailwaySource("BRIDGE", new LineSource(lW,0.5, "BRIDGE"));

            railWayParameters.appendVperHour(vehPerHour*getNbCoach(typeVehicle), speed);
            return railWayParameters;
        }
    }

    /**
     * traction or Aerodynamic Level.
     * @param typeVehicle vehicle data base
     * @param height height source
     * @return lWSpectre(freq) (Traction or Aerodynamic)
     **/
    private double[] getLWTraction(String typeVehicle, int runningCondition, String height, String fileVersion) {
        double[] lWSpectre = new double[24];
        for (int idFreq = 0; idFreq < 24; idFreq++) {
            lWSpectre[idFreq] = getTractionNoise(typeVehicle, runningCondition, height, fileVersion, idFreq);
        }
        return lWSpectre;
    }

    /**
     * traction or Aerodynamic Level.
     * @param typeVehicle vehicle data base
     * @param speed min speed between vehicle and track
     * @param height height source
     * @return lWSpectre(freq) (Traction or Aerodynamic)
     **/
    private double[] getLWAero(String typeVehicle, double speed, String height, String fileVersion) {
        double[] lWSpectre = new double[24];

        String refId = getRefValue(getVehicleNode(typeVehicle), "RefAerodynamic");

        // Resolve the aerodynamic noise node from JSON data
        JsonNode aeroNode = (refId != null && !refId.isEmpty()) ?
                getRailWayData().get("Vehicle").get("AerodynamicNoise").get(refId) : null;

        // Check V0 value from JSON - if 0 or node not found, no aerodynamic noise
        double v0Aero = 0;
        double alphaAero = 0;
        if (aeroNode != null) {
            v0Aero = Double.parseDouble(aeroNode.get("V0").asText());
            alphaAero = Double.parseDouble(aeroNode.get("Alpha").asText());
        }

        if (speed < 200 || aeroNode == null || v0Aero == 0) {
            Arrays.fill(lWSpectre, -99);
        } else {
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                lWSpectre[idFreq] = getAerodynamicNoise(typeVehicle, height, fileVersion, idFreq)
                        + alphaAero * Math.log10(speed / v0Aero);
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


    private double[] getLWRolling(String typeVehicle, String trackRoughnessId, String impactId, int curvature, double speed, String trackTransferId, double axlesPerVeh) {

        double[] trackTransfer = new double[24];
        double[] lWTr = new double[24];
        double[] vehTransfer = new double[24];
        double[] lWVeh = new double[24];
        double[] lW = new double[24];

        // roughnessLtot = CNOSSOS p.19 (2.3.7)
        double[] roughnessLtot = checkNanValue(getLWRoughness(typeVehicle, trackRoughnessId, impactId, speed));

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            // lWTr = CNOSSOS p.20 (2.3.8)
            trackTransfer[idFreq] = getTrackTransfer(trackTransferId,  idFreq);
            lWTr[idFreq] = roughnessLtot[idFreq] + trackTransfer[idFreq] + 10 * Math.log10(axlesPerVeh);

            // lWVeh = CNOSSOS p.20 (2.3.9)
            vehTransfer[idFreq] = getVehTransfer(typeVehicle,  idFreq);
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
        }

        return lW;
    }


    /**
     * method calculates the overall sound power level for a specific type of vehicle, track roughness,
     * impact ID, bridge ID, speed, track file version, and number of axles per vehicle
     * @param typeVehicle
     * @param trackRoughnessId
     * @param impactId
     * @param bridgeId
     * @param speed
     * @param axlesPerVeh
     * @return
     */
    private double[] getLWBridge(String typeVehicle, String trackRoughnessId, String impactId, String bridgeId, double speed, double axlesPerVeh) {

        double[] lW = new double[24];

        // roughnessLtot = CNOSSOS p.19 (2.3.7)
        double[] roughnessLtot = checkNanValue(getLWRoughness(typeVehicle, trackRoughnessId, impactId, speed));

        for (int idFreq = 0; idFreq < 24; idFreq++) {
            lW[idFreq] = -99;
        }

        // Resolve bridge node dynamically from JSON data
        JsonNode bridgeNode = (bridgeId != null && !bridgeId.isEmpty()) ?
                getRailWayData().get("Track").get("BridgeConstant").get(bridgeId) : null;

        if (bridgeNode != null) {
            // Check "Values" first, then "Value" (singular)
            JsonNode valuesNode = bridgeNode.get("Values");
            if (valuesNode == null) {
                valuesNode = bridgeNode.get("Value");
            }
            if (valuesNode != null) {
                if (valuesNode.isArray() && valuesNode.size() >= 24) {
                    // Frequency-dependent bridge structural spectrum (e.g. EU3, EU4)
                    for (int idFreq = 0; idFreq < 24; idFreq++) {
                        lW[idFreq] = roughnessLtot[idFreq] + valuesNode.get(idFreq).doubleValue() + 10 * Math.log10(axlesPerVeh);
                    }
                } else if (valuesNode.isNumber() && valuesNode.doubleValue() != 0) {
                    // Uniform bridge constant (e.g. EU2, SNCF2)
                    double bridgeConstant = valuesNode.doubleValue();
                    for (int idFreq = 0; idFreq < 24; idFreq++) {
                        lW[idFreq] = roughnessLtot[idFreq] + bridgeConstant + 10 * Math.log10(axlesPerVeh);
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
    private double[] getLWRoughness(String typeVehicle, String trackRoughnessId, String impactId, double speed) {

        double[] roughnessTotLambda = new double[35];
        double[] roughnessLtot = new double[35];
        double[] contactFilter = new double[35];
        double[] lambdaToFreqLog = new double[35];
        double[] freqMedLog = new double[24];
        double[] Lambda = new double[35];

        // Resolve impact noise node once before the loop
        boolean hasImpactNoise = false;
        if (impactId != null && !impactId.isEmpty()) {
            JsonNode impactNode = getRailWayData().get("Track").get("ImpactNoise").get(impactId);
            if (impactNode != null) {
                // Check JoinDensity: if present and null, this is an empty sentinel entry
                JsonNode joinDensity = impactNode.get("JoinDensity");
                hasImpactNoise = joinDensity == null || !joinDensity.isNull();
            }
        }

        double m = 33;
        for (int idLambda = 0; idLambda < 35; idLambda++) {
            Lambda[idLambda] = Math.pow(10, m / 10);
            lambdaToFreqLog[idLambda] = Math.log10(speed / Lambda[idLambda] * 1000 / 3.6);

            roughnessTotLambda[idLambda] = Math.pow(10, getLRoughness(typeVehicle, trackRoughnessId, idLambda) / 10);

            contactFilter[idLambda] = getContactFilter(typeVehicle,  idLambda);
            if (hasImpactNoise) {
                roughnessLtot[idLambda] =  10 * Math.log10(roughnessTotLambda[idLambda]+ Math.pow(10, getImpactNoise(impactId,  idLambda) / 10))+ contactFilter[idLambda];
            } else {
                roughnessLtot[idLambda] = 10 * Math.log10(roughnessTotLambda[idLambda]) + contactFilter[idLambda];
            }
            roughnessLtot[idLambda] = Math.pow(10, roughnessLtot[idLambda] / 10);
            m--;
        }
        for (int idFreqMed = 0; idFreqMed < 24; idFreqMed++) {
            freqMedLog[idFreqMed] = Math.log10(Math.pow(10, (17 + Double.valueOf(idFreqMed)) / 10));
        }

        double[] roughnessLtotFreq = interpLinear(lambdaToFreqLog, roughnessLtot, freqMedLog);

        for (int idRoughnessLtotFreq = 0; idRoughnessLtotFreq < 24; idRoughnessLtotFreq++) {
            roughnessLtotFreq[idRoughnessLtotFreq] = 10 * Math.log10(roughnessLtotFreq[idRoughnessLtotFreq]);
        }
        return roughnessLtotFreq;
    }

}

