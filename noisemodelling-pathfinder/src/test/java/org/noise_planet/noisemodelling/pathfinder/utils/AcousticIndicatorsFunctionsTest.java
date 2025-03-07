/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcousticIndicatorsFunctionsTest {


    @Test
    public void testAcousticIndicators() {
        double[] power = new double[]{93, 93, 93, 93, 93, 93, 93, 93};
        double[] absOne = new double[] {-43.56, -50.59, -54.49, -56.14, -55.31, -49.77, -26.37, -25.98};
        double[] absTwo = new double[] {-74.24, -78.34, -81.99, -85.43, -88.61, -92.80, -100.35, -119.88};

        double[] sumAbs = AcousticIndicatorsFunctions.sumArray(AcousticIndicatorsFunctions.dBToW(absOne),
                AcousticIndicatorsFunctions.dBToW(absTwo));
        double[] noiseResult = AcousticIndicatorsFunctions.wToDb(
                AcousticIndicatorsFunctions.multiplicationArray(sumAbs, AcousticIndicatorsFunctions.dBToW(power)));

        double[] wSum = AcousticIndicatorsFunctions.dBToW(AcousticIndicatorsFunctions.sumArray(power,
                absOne));
        wSum = AcousticIndicatorsFunctions.sumArray( wSum,AcousticIndicatorsFunctions.dBToW(AcousticIndicatorsFunctions.sumArray(power,
                absTwo)));

        assertArrayEquals(noiseResult, AcousticIndicatorsFunctions.wToDb(wSum), 0.01);
    }
}