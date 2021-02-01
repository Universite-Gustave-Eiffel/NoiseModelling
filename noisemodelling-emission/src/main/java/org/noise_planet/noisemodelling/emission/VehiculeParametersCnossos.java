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

public class VehiculeParametersCnossos {

    // TODO set default value
    private String typeVehicule="X-TER-bicaisse-D";
    private String typeWag;
    private int nbWag=1;

    private double speedVehicule;

    private double tDay=1;
    private double tEvenig=1;
    private double tNight=1;

    private int height;

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

    public void setTypeVehicule(String typeEng) {
        this.typeVehicule = typeEng;
    }
    /*public void setTypeWag(String typeWag) {
        this.typeWag = typeWag;
    }
    public void setNbWg(int nbWag) {
        this.nbWag = nbWag;
    }*/

    public void setSpeed(double speedVehicule) {
        this.speedVehicule = speedVehicule;
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


    public void setHeight(int height) {
        this.height = height;
    }

    public String getTypeVehicule() {
        return typeVehicule;
    }

    /*public String getTypeWag() {
        return typeWag;
    }
    public int getNbWg() {
        return nbWag;
    }*/

    public double getSpeed() {
        return speedVehicule;
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

    /*public int getHeight() {
        return height;
    }*/

// Todo update stratégie -> 1 vehciule (eng + wagon ? ) CNOSSOS / plamade EngVeh + WagonVah
    public VehiculeParametersCnossos(String typeVehicule, String typeWag, int nbWag, double speedVehicule,
                                  double tDay, double tEvening, double tNight) {

        setTypeVehicule(typeVehicule);
        //setTypeWag(typeWag);
        //setNbWg(nbWag);
        // Todo  / condition de circulation  + si stationnement temps de stationnement (defaut = 0)
        setSpeed(speedVehicule);

        setTDay(tDay);
        setTEvening(tEvening);
        setTNight(tNight);

        //setHeight(height);
    }
}
