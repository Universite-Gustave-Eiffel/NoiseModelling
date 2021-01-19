package org.noise_planet.noisemodelling.propagation;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class Utils {
    public static double dbToW(double dB) {
        return Math.pow(10., dB / 10.);
    }

    public static double wToDb(double w) {
        return 10 * Math.log10(w);
    }
}
