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
    private String trackTransfer;
    private String railRoughness;
    private int curvature;
    private String impactNoise;
    private String bridgeTransfert;

    public RailwayTrackCnossosParameters(double speedTrack, String trackTransfer, String railRoughness, String impactNoise,
                                         String bridgeTransfert, int curvature, double speedCommercial, boolean isTunnel, int nTrack) {

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

    public String getTrackTransfer() {
        return trackTransfer;
    }

    public void setTrackTransfer(String trackTransfer) {
        this.trackTransfer = trackTransfer;
    }

    public String getRailRoughness() {
        return railRoughness;
    }

    public void setRailRoughness(String railRoughness) {
        this.railRoughness = railRoughness;
    }

    public String getImpactNoise() {
        return impactNoise;
    }

    public void setImpactNoise(String impactNoise) {
        this.impactNoise = impactNoise;
    }

    public String getBridgeTransfert() {
        return bridgeTransfert;
    }

    public void setBridgeTransfert(String bridgeTransfert) {
        this.bridgeTransfert = bridgeTransfert;
    }

    public int getCurvature() {
        return curvature;
    }

    public void setCurvature(int curvature) {
        this.curvature = curvature;
    }


}
