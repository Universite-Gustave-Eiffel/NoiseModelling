/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.emission.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.emission.utils.Utils.*;

/*
 * Test basic utils
 */

class UtilsTest {

    @Test
    void sumTestDbValues() {
        double valA  = 50.0;
        double valB = 60.0;
        double result = sumDbValues(valA, valB);
        assertEquals(60.4139, result, 0.0001);
    }

    @Test
    void sumTestDb3() {
        double valA = 50.0;
        double valB = 55.0;
        double valC = 60.0;
        double result = sumDb3(valA, valB, valC);
        assertEquals(61.5113, result, 0.0001);
    }

    @Test
    void sumTestDb5() {
        double valA = 50.0;
        double valB = 51.0;
        double valC = 52.0;
        double valD = 53.0;
        double valE = 54.0;
        double result = sumDb5(valA, valB, valC, valD, valE);
        assertEquals(59.2174, result, 0.0001);
    }

    @Test
    void testTertsToOctave() {
        double[] terts = new double[] {10.0, 10.0, 10.0,
                                       15.0, 15.0, 15.0,
                                       20.0, 20.0, 20.0,
                                       25.0, 25.0, 25.0,
                                       30.0, 30.0, 30.0,
                                       35.0, 35.0, 35.0,
                                       40.0, 40.0, 40.0,
                                       45.0, 45.0, 45.0};
        double[] octave = tertsToOctave(terts);
        double[] expectation = new double[] {10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0};
        expectation = Arrays.stream(expectation).map(x -> x + 4.7712).toArray();
        assertArrayEquals(expectation, octave, 0.0001);



    }
}