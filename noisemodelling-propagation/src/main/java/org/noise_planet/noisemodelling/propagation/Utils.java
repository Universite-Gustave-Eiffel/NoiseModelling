/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class Utils {
    /**
     * Convert Decibel to Watt
     * @param dB
     * @return watt value
     */
    public static double dbToW(double dB) {
        return Math.pow(10., dB / 10.);
    }

    /**
     * Convert Watt to Decibel
     * @param w
     * @return decibel value
     */
    public static double wToDb(double w) {
        return 10 * Math.log10(w);
    }
}
