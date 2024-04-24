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


import org.noise_planet.noisemodelling.emission.railway.RailwayTrackParameters;

/**
 * Parameters Track
 */

public class RailwayTrackCnossosParameters extends RailwayTrackParameters {
    //set default value
    private int trackTransfer;
    private int railRoughness;
    private int curvature;
    private int impactNoise;
    private int bridgeTransfert;
    private String fileVersion = "FR"; // version of cnossos coefficient, if 2 == amendments 2019

    public RailwayTrackCnossosParameters(double speedTrack, int trackTransfer, int railRoughness, int impactNoise,
                                         int bridgeTransfert, int curvature, double speedCommercial, boolean isTunnel, int nTrack) {

        setSpeedTrack(speedTrack);
        setSpeedCommercial(speedCommercial);
        setIsTunnel(isTunnel);
        setNTrack(nTrack);
        setTrackTransfer(trackTransfer);
        setRailRoughness(railRoughness);
        setImpactNoise(impactNoise);
        setCurvature(curvature);
        setBridgeTransfert(bridgeTransfert);

    }


    public String getFileVersion() {
        return this.fileVersion;
    }

    public void setFileVersion(String fileVersion) {
        this.fileVersion = fileVersion;
    }


    public int getTrackTransfer() {
        return trackTransfer;
    }

    public void setTrackTransfer(int trackTransfer) {
        this.trackTransfer = trackTransfer;
    }

    public int getRailRoughness() {
        return railRoughness;
    }

    public void setRailRoughness(int railRoughness) {
        this.railRoughness = railRoughness;
    }

    public int getImpactNoise() {
        return impactNoise;
    }

    public void setImpactNoise(int impactNoise) {
        this.impactNoise = impactNoise;
    }

    public int getBridgeTransfert() {
        return bridgeTransfert;
    }

    public void setBridgeTransfert(int bridgeTransfert) {
        this.bridgeTransfert = bridgeTransfert;
    }

    public int getCurvature() {
        return curvature;
    }

    public void setCurvature(int curvature) {
        this.curvature = curvature;
    }


}
