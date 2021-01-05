package org.noise_planet.noisemodelling.propagation;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class Utils {
    public static double dbaToW(double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public static double wToDba(double w) {
        return 10 * Math.log10(w);
    }
}
