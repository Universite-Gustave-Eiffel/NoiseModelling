package org.noise_planet.noisemodelling.emission;

public class TrainParametersCnossos {
    private String typeEng;
    private String typeWag;
    private int nbWag;

    private double speed;

    private double tDay;
    private double tEvenig;
    private double tNight;

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

    public void setTypeEng(String typeEng) {
        this.typeEng = typeEng;
    }
    public void setTypeWag(String typeWag) {
        this.typeWag = typeWag;
    }
    public void setNbWg(int nbWag) {
        this.nbWag = nbWag;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
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

    public String getTypeEng() {
        return typeEng;
    }
    public String getTypeWag() {
        return typeWag;
    }
    public int getNbWg() {
        return nbWag;
    }

    public double getSpeed() {
        return speed;
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

    public int getHeight() {
        return height;
    }

    public int getFreqParam() {
        return FreqParam;
    }

    public TrainParametersCnossos(String tpeEngine, String typeWag, int nbWag, double speed,
                                  double tDay, double tEvenig, double tNight, int height,int freqParam) {

        setTypeEng(tpeEngine);
        setTypeWag(typeWag);
        setNbWg(nbWag);

        setSpeed(speed);

        setTDay(tDay);
        setTEvening(tEvenig);
        setTNight(tNight);

        setHeight(height);
        this.FreqParam = Math.max(0, freqParam);
    }
}
