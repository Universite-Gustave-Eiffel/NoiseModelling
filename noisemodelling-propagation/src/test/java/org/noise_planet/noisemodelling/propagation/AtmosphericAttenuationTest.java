/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Test atmospheric attenuation with ISO 9313-1:1993 P.5-12 as reference values
 */
public class AtmosphericAttenuationTest {
    private static final double EPSILON = 0.1;
    private static final List<Double> freq_lvl_exact = Arrays.asList(PropagationProcessPathData.asOctaveBands(PropagationProcessPathData.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE));

    @Test
    public void atmoTestMinus20degree() {
        double temperature = -20;
        double humidity = 70;
        double pressure = 101325;
        final double[] expected = new double[] {0.173,0.514,1.73,5.29,11.5,16.6,20.2,27.8};
        for(int idfreq=0;idfreq< expected.length;idfreq++) {
            double freq = freq_lvl_exact.get(idfreq);
            double coefAttAtmos = PropagationProcessPathData.getCoefAttAtmos(freq, humidity,pressure,temperature+PropagationProcessPathData.K_0);
            assertEquals(expected[idfreq], coefAttAtmos, EPSILON);
        }
    }

    @Test
    public void atmoTestMinus15degree() {
        double temperature = -15;
        double humidity = 50;
        double pressure = 101325;
        final double[] expected = new double[] {0.188,0.532,1.76,5.61,13.2,20.5,25.2,33.2};
        for(int idfreq=0;idfreq< expected.length;idfreq++) {
            double freq = freq_lvl_exact.get(idfreq);
            double coefAttAtmos = PropagationProcessPathData.getCoefAttAtmos(freq, humidity,pressure,temperature+PropagationProcessPathData.K_0);
            assertEquals(expected[idfreq], coefAttAtmos, EPSILON);
        }
    }

    @Test
    public void atmoTest0degree() {
        double temperature = 0;
        double humidity = 60;
        double pressure = 101325;
        final double[] expected = new double[] {0.165,0.401,0.779,1.78,5.5,19.3,63.3,154.4};
        for(int idfreq=0;idfreq< expected.length;idfreq++) {
            double freq = freq_lvl_exact.get(idfreq);
            double coefAttAtmos = PropagationProcessPathData.getCoefAttAtmos(freq, humidity,pressure,temperature+PropagationProcessPathData.K_0);
            assertEquals(expected[idfreq], coefAttAtmos, EPSILON);
        }
    }

    @Test
    public void atmoTest20degree() {
        double temperature = 20;
        double humidity = 80;
        double pressure = 101325;
        final double[] expected = new double[] {0.079,0.302,1.04,2.77,5.15,8.98,21.3,68.6};
        for(int idfreq=0;idfreq< expected.length;idfreq++) {
            double freq = freq_lvl_exact.get(idfreq);
            double coefAttAtmos = PropagationProcessPathData.getCoefAttAtmos(freq, humidity,pressure,temperature+PropagationProcessPathData.K_0);
            assertEquals(expected[idfreq], coefAttAtmos, EPSILON);
        }
    }


    @Test
    public void atmoTestCnossos() {
        double temperature = 10;
        double humidity = 70;
        double pressure = 101325;
        final double[] expected = new double[] {0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        for(int idfreq=0;idfreq< expected.length;idfreq++) {
            double freq = freq_lvl_exact.get(idfreq);
            double coefAttAtmos = PropagationProcessPathData.getCoefAttAtmos(freq, humidity,pressure,temperature+PropagationProcessPathData.K_0);
            assertEquals(expected[idfreq], coefAttAtmos, EPSILON);
        }
    }
}
