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

import java.io.IOException;

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

    /**
     * Compute Noise Level from flow_rate and speed Eq 2.2.1 from Directive 2015/2019
     * @param LWim LW,i,m is the directional sound power of a single vehicle and is expressed in dB (re. 10–12 W/m).
     * @param Qm Traffic flow data Qm shall be expressed as yearly average per hour, per time period (day-evening-night), per vehicle class and per source line.
     * @param vm The speed vm is a representative speed per vehicle category: in most cases the lower of the maximum legal speed for the section of road and the maximum legal speed for the vehicle category. If measurement data is unavailable, the maximum legal speed for the vehicle category shall be used.
     * @return
     * @throws IOException if speed < 0 km/h
     */
    public static Double Vperhour2NoiseLevel(double LWim, double Qm, double vm){
        return LWim + 10 * Math.log10(Qm / (1000 * vm));
    }

    /**
     * Compute Noise Level from flow_rate and speed Eq 2.2.1 from Directive 2015/2019
     * @param LWimf LW,i,m is the directional sound power of a single vehicle and is expressed in dB (re. 10–12 W/m) and for every freq
     * @param Qm Traffic flow data Qm shall be expressed as yearly average per hour, per time period (day-evening-night), per vehicle class and per source line.
     * @param vm The speed vm is a representative speed per vehicle category: in most cases the lower of the maximum legal speed for the section of road and the maximum legal speed for the vehicle category. If measurement data is unavailable, the maximum legal speed for the vehicle category shall be used.
     * @return
     * @throws IOException if speed < 0 km/h
     */
    public static double[] Vperhour2NoiseLevelAllFreq(double[] LWimf, double Qm, double vm){
        double[] LWimf_return = new double[LWimf.length];
        for (int f = 1 ;f< LWimf.length;f++) {
             LWimf_return[f] = LWimf[f] + 10 * Math.log10(Qm / (1000 * vm));
        }
        return LWimf_return;
    }


    /**
     * energetic Sum of dBA array
     *
     * @param array1
     * @param array2
     * @return
     */
    public static double[] sumDbArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(dbaToW(array1[i]) + dbaToW(array2[i]));
        }
        return sum;
    }

    public static double dbaToW(double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public static double[] dbaToW(double[] dBA) {
        double[] ret = new double[dBA.length];
        for (int i = 0; i < dBA.length; i++) {
            ret[i] = dbaToW(dBA[i]);
        }
        return ret;
    }

    public static double wToDba(double w) {
        return 10 * Math.log10(w);
    }

    public static double[] wToDba(double[] w) {
        double[] ret = new double[w.length];
        for (int i = 0; i < w.length; i++) {
            ret[i] = wToDba(w[i]);
        }
        return ret;
    }

}
