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
 * @author Adrien Le Bellec - univ Gustave eiffel
 * @author Olivier Chiello, Univ Gustave Eiffel
 */

/**
 * DataBase Vehicule
 */

public class RailwayVehicleParametersCnossos {

    // set default value
    private String typeVehicle=""; // name of the vehicles
    private double speedVehicle; // speed of the vehicles (km/h)
    private double vehiclePerHour = 1; // Average light vehicle per hour
    private int runningCondition=0; // 0 = constand speed, 1 = acceleration , 2 =decceleration, 3 = idling
    private double idlingTime=0; // if idling, idling time (seconds)

    private int spectreVer = 2; // version of cnossos coefficient, if 1 == amendments 2020 if 2 == 2021 SNCF


    public void setSpectreVer(int spectreVer) {
        this.spectreVer = spectreVer;
    }
    public int getSpectreVer() {
        return this.spectreVer;
    }


    public void setTypeVehicle(String typeVehicle) {
        this.typeVehicle = typeVehicle;
    }
    public void setSpeedVehicle(double speedVehicle) {
        this.speedVehicle = speedVehicle;
    }
    public void setVehiclePerHour(double vehiclePerHour) {
        this.vehiclePerHour = vehiclePerHour;
    }

    public void setRunningCondition(int runningCondition) {
        this.runningCondition = runningCondition;
    }
    public void setIdlingTime(double idlingTime) {
        this.idlingTime = idlingTime;
    }

    public String getTypeVehicle() {
        return typeVehicle;
    }
    public double getSpeedVehicle() {
        return speedVehicle;
    }
    public double getNumberVehicle() {
        return vehiclePerHour ;
    }

    public int getRunningCondition( ) {
        return runningCondition;
    }
    public double getIdlingTime() {
        return idlingTime;
    }

    public RailwayVehicleParametersCnossos(String typeVehicle, double speedVehicle, double vehiclePerHour, int runningCondition, double idlingTime) {


        setTypeVehicle(typeVehicle);
        setSpeedVehicle(speedVehicle);
        setVehiclePerHour(vehiclePerHour);

        setRunningCondition(runningCondition);
        setIdlingTime(idlingTime);
    }
}
