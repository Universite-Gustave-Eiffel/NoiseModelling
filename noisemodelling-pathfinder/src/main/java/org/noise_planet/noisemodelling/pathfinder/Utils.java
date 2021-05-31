package org.noise_planet.noisemodelling.pathfinder;

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

    public static double[] dbaToW(double[] dBA) {
        double[] ret = new double[dBA.length];
        for (int i = 0; i < dBA.length; i++) {
            ret[i] = dbaToW(dBA[i]);
        }
        return ret;
    }
}
