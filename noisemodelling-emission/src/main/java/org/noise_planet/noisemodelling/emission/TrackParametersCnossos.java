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
 * DataBase Track
 */

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
