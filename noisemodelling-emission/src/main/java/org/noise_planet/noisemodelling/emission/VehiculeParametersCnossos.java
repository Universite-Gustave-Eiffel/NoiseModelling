package org.noise_planet.noisemodelling.emission;

public class VehiculeParametersCnossos {
    private String typeVehicule;
    private String typeWag;
    private int nbWag;

    private double speedVehicule;

    private double tDay;
    private double tEvenig;
    private double tNight;

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
