package org.noise_planet.noisemodelling.emission;

public class TrainParametersCnossos {
    private String typeTrain;
    private double vehPerHour;

    private double speed;
    private int numVeh;
    private int height;
    private final int FreqParam;

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

    public void setTypeTrain(String typeTrain) {
        this.typeTrain = typeTrain;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public void setVehPerHour(double vehPerHour) {
        this.vehPerHour = vehPerHour;
    }
    public void setNumVeh(int numVeh) {
        this.numVeh = numVeh;
    }
    public void setHeight(int height) {
        this.height = height;
    }


    public String getTypeTrain() {
        return typeTrain;
    }
    public double getVehPerHour() {
        return vehPerHour;
    }
    public int getNumVeh() {
        return numVeh;
    }
    public double getSpeed() {
        return speed;
    }
    public int getHeight() {
        return height;
    }
    public int getFreqParam() {
        return FreqParam;
    }

    public TrainParametersNMPB(String typeTrain, double speed,  double vehPerHour, int numVeh,int height,int freqParam) {

       setTypeTrain(typeTrain);
       this.FreqParam = Math.max(0, freqParam);

       setSpeed(speed);
       setHeight(height);
       setVehPerHour(vehPerHour);
       setNumVeh(numVeh);
    }
}
