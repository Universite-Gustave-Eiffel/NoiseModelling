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
 *  @Author Nicolas Fortin, Université Gustave Eiffel
 *  @Author Pierre Aumond, Université Gustave Eiffel
 */

public class Utils {
    /**
     * Convert dB to W
     * @param dB
     * @return
     */
    public static double dbToW(double dB) {
        return Math.pow(10., dB / 10.);
    }

    /**
     * Convert W to dB
     * @param w
     * @return
     */
    public static double wToDb(double w) {
        return 10 * Math.log10(w);
    }

    /**
     * Compute the slope
     * @param beginZ Z start
     * @param endZ Z end
     * @param road_length_2d Road length (projected to Z axis)
     * @return Slope percentage
     */
    public static double computeSlope(double beginZ, double endZ, double road_length_2d) {
        return (endZ - beginZ) / road_length_2d * 100.;
    }
}
