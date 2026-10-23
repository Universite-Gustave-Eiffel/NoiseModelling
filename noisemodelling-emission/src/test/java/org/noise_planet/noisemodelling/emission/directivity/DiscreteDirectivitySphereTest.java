/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.emission.directivity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;

/**
 * Test the Directivity Sphere classes
 * @author Nicolas Fortin, Université Gustave Eiffel
 */

public class DiscreteDirectivitySphereTest {
    final static double[] freqTest = new double[]{125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    @Test
    public void testInsert() {
        DiscreteDirectivitySphere d = new DiscreteDirectivitySphere(1, freqTest);

        RailwayCnossosDirectivitySphere att = new RailwayCnossosDirectivitySphere(new LineSource("TRACTIONB"));

        for (int yaw = 0; yaw < 360; yaw += 5) {
            float phi = (float) Math.toRadians(yaw);
            for (int pitch = -85; pitch < 90; pitch += 5) {
                float theta = (float) Math.toRadians(pitch);
                double[] attSpectrum = new double[freqTest.length];
                for (int idFreq = 0; idFreq < freqTest.length; idFreq++) {
                    attSpectrum[idFreq] = att.getAttenuation(freqTest[idFreq], phi, theta);
                }
                d.addDirectivityRecord(theta, phi,
                        attSpectrum);
            }
        }

        // test nearest neighbors

        assertEquals(new DirectivityRecord((float) Math.toRadians(30),
                        (float) Math.toRadians(25), null),
                d.getRecord((float) Math.toRadians(31), (float) Math.toRadians(26), 0));

        assertEquals(new DirectivityRecord((float) Math.toRadians(85),
                        (float) Math.toRadians(0), null),
                d.getRecord((float) Math.toRadians(88), (float) Math.toRadians(358), 0));

        assertEquals(new DirectivityRecord((float) Math.toRadians(-85),
                        (float) Math.toRadians(0), null),
                d.getRecord((float) Math.toRadians(-89), (float) Math.toRadians(2), 0));


        // Test bilinear interpolation
        DirectivityRecord r = d.getRecord((float) Math.toRadians(26),
                (float) Math.toRadians(31), 1);
        assertEquals(new DirectivityRecord((float) Math.toRadians(26),
                (float) Math.toRadians(31), null), r);
        assertArrayEquals(new double[]{-5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63}, r.getAttenuation(),
                0.1);

        // check for non-existing frequency
        assertArrayEquals(new double[]{-5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63,
                        -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63},
                d.getAttenuationArray(new double[]{125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0,
                                1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0},
                        (float) Math.toRadians(31), (float) Math.toRadians(26)), 0.1);


        assertEquals(-5.63, d.getAttenuation(freqTest[0], (float) Math.toRadians(31),
                (float) Math.toRadians(26)), 0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[1], (float) Math.toRadians(31),
                (float) Math.toRadians(26)), 0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[1] + 1, (float) Math.toRadians(31),
                (float) Math.toRadians(26)), 0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[0] - 1, (float) Math.toRadians(31),
                (float) Math.toRadians(26)), 0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[freqTest.length - 1] + 1, (float) Math.toRadians(31),
                (float) Math.toRadians(26)), 0.1);

    }

    /**
     * A pattern that depends only on the elevation must give the same attenuation at every azimuth
     */
    @Test
    public void testElevationOnlyPatternIsIndependentOfAzimuth() {
        DiscreteDirectivitySphere d = new DiscreteDirectivitySphere(1, new double[]{1000});
        // 15° grid; 0 dB at or below the horizon, -20 dB at the zenith
        for (int pitch = -90; pitch <= 90; pitch += 15) {
            for (int yaw = 0; yaw < 360; yaw += 15) {
                d.addDirectivityRecord(Math.toRadians(pitch), Math.toRadians(yaw),
                        new double[]{-20.0 * Math.max(0, pitch) / 90.0});
            }
        }
        // Energy bilinear interpolation between 0 dB (elevation 0°) and -3.333 dB (elevation 15°) at elevation 10°
        for (int yaw : new int[]{0, 45, 80, 85, 89, 90, 95, 100, 180, 270, 275}) {
            assertEquals(-1.919, d.getAttenuation(1000, Math.toRadians(yaw), Math.toRadians(10)), 0.01,
                    "azimuth " + yaw);
        }
        // Nearest neighbour
        assertEquals(new DirectivityRecord(0, Math.toRadians(90), null),
                d.getRecord(Math.toRadians(1), Math.toRadians(89), 0));
    }

    /**
     * Next to the downward pole the azimuth must still be interpolated
     */
    @Test
    public void testAzimuthInterpolationNextToPole() {
        DiscreteDirectivitySphere d = new DiscreteDirectivitySphere(1, new double[]{1000});
        // 15° grid; 0 dB at azimuths 0°, 30°..., -10 dB at azimuths 15°, 45°...
        for (int pitch = -90; pitch <= 90; pitch += 15) {
            for (int yaw = 0; yaw < 360; yaw += 15) {
                d.addDirectivityRecord(Math.toRadians(pitch), Math.toRadians(yaw),
                        new double[]{yaw % 30 == 0 ? 0 : -10});
            }
        }
        // Halfway between azimuths 0° and 15°: 10*log10(0.5 * 1 + 0.5 * 0.1)
        assertEquals(-2.596, d.getAttenuation(1000, Math.toRadians(7.5), Math.toRadians(-80)), 0.01);
    }

}