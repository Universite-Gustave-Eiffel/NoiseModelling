/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.utils;

import java.util.Arrays;
import java.util.Locale;

/**
 * A utility class providing acoustic indicator functions for computations,
 * including conversions between decibels and energy, summation and multiplication of arrays,
 * and operations specific to octave bands.
 */
public class AcousticIndicatorsFunctions {

    /**
     * Convert Decibel to Watt
     * @param dB Sound power spectrum in dB (or dBa , no weighting is done here)
     * @return Watt value
     */
    public static double dBToW(double dB) {
        return Math.pow(10., dB / 10.);
    }

    /**
     * Convert Decibel to Watt
     * @param dB Sound power spectrum in dB (or dBa , no weighting is done here)
     * @return Watt value
     */
    public static double[] dBToW(double[] dB) {
        double[] ret = new double[dB.length];
        for (int i = 0; i < dB.length; i++) {
            ret[i] = dBToW(dB[i]);
        }
        return ret;
    }

    /**
     * Convert Watt to Decibel
     * @param w
     * @return Decibel value
     */
    public static double wToDb(double w) {
        return 10 * Math.log10(w);
    }


    /**
     * Convert Watt to Decibel
     * @param w
     * @return Decibel value
     */
    public static double[] wToDb(double[] w) {
        double[] ret = new double[w.length];
        for (int i = 0; i < w.length; i++) {
            ret[i] = wToDb(w[i]);
        }
        return ret;
    }

    public static double[] twoDgtAftrComma(double[] valeurs) {
        return Arrays.stream(valeurs)
                .map(nombre -> Double.parseDouble(String.format(Locale.US, "%.2f", nombre)))
                .toArray();
    }




    /**
     * Eq 2.5.9
     * The ‘long-term’ sound level along a path starting from a given point source is
     * obtained from the logarithmic sum of the weighted sound energy
     * in homogeneous conditions and the sound energy in favourable conditions.
     * @param array1 double array
     * @param array2 double array
     * @param p the mean occurrence p of favourable conditions in the direction of the path (S,R)
     * @return
     */
    public static double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDb(p * dBToW(array1[i]) + (1 - p) * dBToW(array2[i]));
        }
        return sum;
    }

    /**
     * energetic Sum of two same size dB array
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
            sum[i] = wToDb(dBToW(array1[i]) + dBToW(array2[i]));
        }
        return sum;
    }

    /**
     * Sum of all the decibel components of this given list
     * @param array1
     * @return the sum in decibel
     */
    public static double sumDbArray(double[] array1) {

        double sum = dBToW(array1[0]);
        for (int i = 1; i < array1.length; i++) {
            sum = dBToW(array1[i]) + sum;
        }

        return wToDb(sum);
    }

    /**
     * Multiply component of two same size array
     *
     * @param array1
     * @param array2
     * @return
     */
    public static double[] multiplicationArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array");
        }
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = array1[i] * array2[i];
        }
        return sum;
    }

    /**
     * Multiply component of two same size array
     *
     * @param array Array input
     * @param coefficient number to multiply at each index
     * @return Array multiplied
     */
    public static double[] multiplicationArray(double[] array, double coefficient) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] * coefficient;
        }
        return result;
    }

    /**
     * sum the first nbfreq values in a given array
     * @param nbfreq
     * @param energeticSum
     * @return the sum value
     */
    public static double sumArray(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }

    /**
     * Sum of all the components of this given list
     * @param energeticSum
     * @return sum value
     */
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
            if(array1.length == 0) {
                return array2;
            }
            if(array2.length == 0) {
                return array1;
            }
            throw new IllegalArgumentException("Arrays with different size");
        }
        double[] ret = new double[array1.length];
        for (int idfreq = 0; idfreq < array1.length; idfreq++) {
            ret[idfreq] = array1[idfreq] + array2[idfreq];
        }
        return ret;
    }

    
    public static double[] sumArray(double[] array, double number) {
        double[] ret = new double[array.length];
        for (int idfreq = 0; idfreq < array.length; idfreq++) {
            ret[idfreq] = array[idfreq] + number;
        }
        return ret;
    }


    /**
     * Create new array by taking middle third octave bands
     *
     * @param thirdOctaveBands Third octave bands array
     * @return Octave bands array
     */
    public static Double[] asOctaveBands(Double[] thirdOctaveBands) {
        Double[] octaveBands = new Double[thirdOctaveBands.length / 3];
        int j = 0;
        for (int i = 1; i < thirdOctaveBands.length - 1; i += 3) {
            octaveBands[j++] = thirdOctaveBands[i];
        }
        return octaveBands;
    }

    /**
     * Create new array by taking middle third octave bands
     *
     * @param thirdOctaveBands Third octave bands array
     * @return Octave bands array
     */
    public static Integer[] asOctaveBands(Integer[] thirdOctaveBands) {
        Integer[] octaveBands = new Integer[thirdOctaveBands.length / 3];
        int j = 0;
        for (int i = 1; i < thirdOctaveBands.length - 1; i += 3) {
            octaveBands[j++] = thirdOctaveBands[i];
        }
        return octaveBands;
    }

    /**
     * Create new array by taking middle third octave bands
     *
     * @param thirdOctaveBands Third octave bands array
     * @return Octave bands array
     */
    public static Integer[] asOctaveBands(int[] thirdOctaveBands) {
        Integer[] octaveBands = new Integer[thirdOctaveBands.length / 3];
        int j = 0;
        for (int i = 1; i < thirdOctaveBands.length - 1; i += 3) {
            octaveBands[j++] = thirdOctaveBands[i];
        }
        return octaveBands;
    }
}
