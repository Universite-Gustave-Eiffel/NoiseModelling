/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.emission.utils;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.*;

import java.util.Arrays;

/**
 * Linear Interpolation
 */

public class interpLinear {

    /**
     *
     * @param x arguments of interpolation function
     * @param y values of interpolation function
     * @param xi arguments to interpolate the function to
     * @return array with interpolated values at locations of xi. NaN if value of xi is not in range of x
     * @throws NullArgumentException
     * @throws NumberIsTooSmallException
     * @throws DimensionMismatchException
     * @throws NonMonotonicSequenceException
     */
    public static final double[] interpLinear(double[] x, double[] y, double[] xi) throws NullArgumentException,
            NumberIsTooSmallException, DimensionMismatchException, NonMonotonicSequenceException {

        LinearInterpolator interpolator = new LinearInterpolator();
        PolynomialSplineFunction interpolation = interpolator.interpolate(x, y);
        double[] yi = Arrays.stream(xi)
                .map(q -> {
                    try {
                        return interpolation.value(q);
                    } catch (OutOfRangeException e) {
                        return Double.NaN;
                    }
                })
                .toArray();
        return yi;
    }
}