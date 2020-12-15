package org.noise_planet.noisemodelling.emission;

public class RailParametersCnossos {

    private double speed;
    private int trackTransfer;
    private int railRoughness;
    private int railPad;
    private int additionalMeasures;
    private int railJoints;
    private int curvate;

    //private int superstructureTransfer;
    //private int impactNoise;
    //private int bridgeConstant;
    private final int FreqParam;

    private int spectreVer = 2;

    /**
     * @param spectreVer
     */
    public void setSpectreVer(int spectreVer) {
        this.spectreVer = spectreVer;
    }


    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public void setTrackTransfer(int trackTransfer) {
        this.trackTransfer = trackTransfer;
    }
    /*public void setSuperstructureTransfer(int superstructureTransfer) {
        this.superstructureTransfer = superstructureTransfer;
    }*/

    public void setRailRoughness(int railRoughness) {
        this.railRoughness = railRoughness;
    }
    public void setRailPad(int railPad) {
        this.railPad = railPad;
    }
    public void setAdditionalMeasures(int additionalMeasures) {
        this.additionalMeasures = additionalMeasures;
    }
    public void setRailJoints(int railJoints) {
        this.railJoints = railJoints;
    }
    public void setCurvate(int curvate) {
        this.curvate = curvate;
    }

    /*
    public void setImpactNoise(int impactNoise) {
        this.impactNoise = impactNoise;
    }
    public void setBridgeConstant(int bridgeConstant) {
        this.bridgeConstant = bridgeConstant;
    }*/


    public int getSpectreVer() {
        return this.spectreVer;
    }

    public double getSpeed() {
        return speed;
    }
    public int getTrackTransfer() {
        return trackTransfer;
    }
    public int getRailRoughness() {
        return railRoughness;
    }
    public int getRailPad() {
        return railPad;
    }
    public int getAdditionalMeasures() {
        return additionalMeasures;
    }
    public int getRailJoints() {
        return railJoints;
    }
    public int getCurvate() {
        return curvate;
    }

    /*public int getSuperstructureTransfer() {
        return superstructureTransfer;
    }
    public int getImpactNoise() {
        return impactNoise;
    }
    public int getBridgeConstant() {
        return bridgeConstant;
    }*/

    public int getFreqParam() {
        return FreqParam;
    }

    public RailParametersCnossos( double speed, int trackTransfer, int railRoughness,int railPad,
                                  int additionalMeasures, int railJoints, int curvate, int freqParam) {


       setSpeed(speed);
       setTrackTransfer(trackTransfer);
       setRailRoughness(railRoughness);
       setRailPad(railPad);
       setAdditionalMeasures(additionalMeasures);
       setRailJoints(railJoints);
       setCurvate(curvate);

       this.FreqParam = Math.max(0, freqParam);
       //setSuperstructureTransfer(superstructureTransfer);
       //setImpactNoise(impactNoise);
       // setBridgeConstant(bridgeConstant);

    }
}
