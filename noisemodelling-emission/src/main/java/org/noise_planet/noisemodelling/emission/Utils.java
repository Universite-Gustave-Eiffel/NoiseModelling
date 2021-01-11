package org.noise_planet.noisemodelling.emission;

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
