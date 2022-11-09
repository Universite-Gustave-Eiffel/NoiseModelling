/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway.cnossos;

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
 * @author Adrien Le Bellec, Université Gustave Eiffel
 * @author Olivier Chiello, Université Gustave Eiffel
 */

import org.noise_planet.noisemodelling.emission.railway.RailwayVehicleParameters;

/**
 * Parameters Vehicule
 */

public class RailwayVehicleCnossosParameters extends RailwayVehicleParameters {

    // set default value
    private int runningCondition = 0; // 0 = constand speed, 1 = acceleration , 2 =decceleration, 3 = idling
    private double idlingTime = 0; // if idling, idling time (seconds)
    private String fileVersion = "FR"; // version of cnossos coefficient, if 2 == amendments 2019

    public RailwayVehicleCnossosParameters(String typeVehicle, double speedVehicle, double vehiclePerHour, int runningCondition, double idlingTime) {
        setTypeVehicle(typeVehicle);
        setSpeedVehicle(speedVehicle);
        setVehiclePerHour(vehiclePerHour);

        setRunningCondition(runningCondition);
        setIdlingTime(idlingTime);
    }

    public String getFileVersion() {
        return this.fileVersion;
    }

    public void setFileVersion(String fileVersion) {
        this.fileVersion = fileVersion;
    }

    public int getRunningCondition() {
        return runningCondition;
    }

    public void setRunningCondition(int runningCondition) {
        this.runningCondition = runningCondition;
    }

    public double getIdlingTime() {
        return idlingTime;
    }

    public void setIdlingTime(double idlingTime) {
        this.idlingTime = idlingTime;
    }
}
