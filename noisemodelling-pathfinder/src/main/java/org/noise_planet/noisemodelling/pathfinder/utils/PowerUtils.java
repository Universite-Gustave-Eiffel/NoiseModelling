package org.noise_planet.noisemodelling.pathfinder.utils;

public class PowerUtils {

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


    /**
     * Eq 2.5.9
     * The ‘long-term’ sound level along a path starting from a given point source is
     * obtained from the logarithmic sum of the weighted sound energy
     * in homogeneous conditions and the sound energy in favourable conditions.
     * @param array1
     * @param array2
     * @param p the mean occurrence p of favourable conditions in the direction of the path (S,R)
     * @return
     */
    public static double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(p * dbaToW(array1[i]) + (1 - p) * dbaToW(array2[i]));
        }
        return sum;
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

    /**
     * Multiply component of two same size array
     *
     * @param array1
     * @param array2
     * @return
     */
    public static double[] multArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = array1[i] * array2[i];
        }
        return sum;
    }

    public static double sumArray(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    public static double sumArray(double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < energeticSum.length; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    /**
     * Element wise sum array without any other operations.
     *
     * @param array1 First array
     * @param array2 Second array
     * @return Sum of the two arrays
     */
    public static double[] sumArray(double array1[], double array2[]) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Arrays with different size");
        }
        double[] ret = new double[array1.length];
        for (int idfreq = 0; idfreq < array1.length; idfreq++) {
            ret[idfreq] = array1[idfreq] + array2[idfreq];
        }
        return ret;
    }
}
