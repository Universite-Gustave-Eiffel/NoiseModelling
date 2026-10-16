/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.emission.railway;
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


import java.io.IOException;

/**
 * Parameters Track
 */

public class RailwayTrackParameters {
    //set default value
    private double speedTrack; // maximum speed on the track (km/h)
    private double speedCommercial; // commercial speed on the track (km/h)
    private int nTrack;
    private boolean isTunnel;
    private static final double MAXSPEED = 500.;

    public RailwayTrackParameters() {}

    public boolean getIsTunnel() {
        return isTunnel;
    }

    public void setIsTunnel(boolean tunnel) {
        isTunnel = tunnel;
    }

    public double getNTrack() {
        return nTrack;
    }

    public void setNTrack(int nTrack) {
        if (nTrack <= 0)  throw new IllegalArgumentException("The number of tracks is less than one on one section.");
        this.nTrack = nTrack;
    }


    public double getSpeedTrack() {
        return speedTrack;
    }

    public void setSpeedTrack(double speedTrack) {
        if (speedTrack <= 0.)  throw new IllegalArgumentException("The track speed is zero or less on one section.");
        this.speedTrack = Math.min(speedTrack, MAXSPEED);
    }


    public double getSpeedCommercial() {
        return speedCommercial;
    }

    public void setSpeedCommercial(double speedCommercial) {
        if (speedCommercial <= 0.)  throw new IllegalArgumentException("The commercial speed is zero or less on one section.");
        this.speedCommercial = Math.min(speedCommercial, MAXSPEED);
    }
}
