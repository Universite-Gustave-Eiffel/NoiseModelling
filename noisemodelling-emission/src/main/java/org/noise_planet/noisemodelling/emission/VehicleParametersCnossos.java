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

public class VehicleParametersCnossos {

    // TODO set default value
    private String typeVehicle="X-TER-bicaisse-D";
    private double speedVehicle;
    private int runningCondition=0;
    private double idlingTime=0;
    private double tDay=1;
    private double tEvenig=1;
    private double tNight=1;


    private int spectreVer = 2;

    /**
     * @param spectreVer
     */
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
    public void setRunningCondition(int runningCondition) {
        this.runningCondition = runningCondition;
    }
    public void setIdlingTime(double idlingTime) {
        this.idlingTime = idlingTime;
    }
    public void setTDay(double tDay) {
        this.tDay = tDay;
    }
    public void setTEvening(double tEvenig) {
        this.tEvenig = tEvenig;
    }
    public void setTNight(double tNight) {
        this.tNight = tNight;
    }

    public String getTypeVehicle() {
        return typeVehicle;
    }
    public double getSpeedVehicle() {
        return speedVehicle;
    }
    public int getRunningCondition() {
        return runningCondition;
    }
    public double getIdlingTime() {
        return idlingTime;
    }
    public double getTDay() {
        return tDay;
    }
    public double getTEvening() {
        return tEvenig;
    }
    public double getTNight() {
        return tNight;
    }

    public VehicleParametersCnossos(String typeVehicle, double speedVehicle, int runningCondition,double idlingTime,
                                  double tDay, double tEvening, double tNight) {

        setTypeVehicle(typeVehicle);
        setSpeedVehicle(speedVehicle);
        setRunningCondition(runningCondition);
        setIdlingTime(idlingTime);
        setTDay(tDay);
        setTEvening(tEvening);
        setTNight(tNight);
    }
}
