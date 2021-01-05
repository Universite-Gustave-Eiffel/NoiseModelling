package org.noise_planet.noisemodelling.emission;

public class TrackParametersCnossos{

    private double speedTrack;
    private int trackTransfer;
    private int railRoughness;
    private int curvate;
    private int impactNoise;
    private int bridgeConstant;

    private int spectreVer = 2;

    /**
     * @param spectreVer
     */
    public void setSpectreVer(int spectreVer) {
        this.spectreVer = spectreVer;
    }


    public void setSpeed(double speedTrack) {
        this.speedTrack = speedTrack;
    }
    public void setTrackTransfer(int trackTransfer) {
        this.trackTransfer = trackTransfer;
    }

    public void setRailRoughness(int railRoughness) {
        this.railRoughness = railRoughness;
    }
    public void setCurvate(int curvate) {
        this.curvate = curvate;
    }
    public void setImpactNoise(int impactNoise) {
        this.impactNoise = impactNoise;
    }
    public void setBridgeConstant(int bridgeConstant) {
        this.bridgeConstant = bridgeConstant;
    }

    public int getSpectreVer() {
        return this.spectreVer;
    }

    public double getSpeed() {
        return speedTrack;
    }
    public int getTrackTransfer() {
        return trackTransfer;
    }
    public int getRailRoughness() {
        return railRoughness;
    }
    public int getImpactNoise() {
        return impactNoise;
    }
    public int getBridgeConstant() {
        return bridgeConstant;
    }
    public int getCurvate() {
        return curvate;
    }

    public TrackParametersCnossos( double speedTrack, int trackTransfer, int railRoughness,int impactNoise,
                                   int bridgeConstant, int curvate) {

       setSpeed(speedTrack);
       setTrackTransfer(trackTransfer);
       setRailRoughness(railRoughness);
       setCurvate(curvate);
       setImpactNoise(impactNoise);
       setBridgeConstant(bridgeConstant);

    }
}
