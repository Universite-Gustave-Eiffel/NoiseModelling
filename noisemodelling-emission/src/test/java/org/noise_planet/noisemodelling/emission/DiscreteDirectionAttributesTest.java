package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiscreteDirectionAttributesTest {
    final static double[] freqTest = new double[] {125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    @Test
    public void testInsert() {
        DiscreteDirectionAttributes d = new DiscreteDirectionAttributes(1, freqTest);

        RailWayLW.TrainAttenuation att = new RailWayLW.TrainAttenuation(RailWayLW.TrainNoiseSource.TRACTIONB);

        for(int yaw = 0; yaw < 360; yaw += 5) {
            float phi = (float)Math.toRadians(yaw);
            for(int pitch = -85; pitch < 90; pitch += 5) {
                float theta = (float)Math.toRadians(pitch);
                double[] attSpectrum = new double[freqTest.length];
                for (int idFreq = 0; idFreq < freqTest.length; idFreq++) {
                    attSpectrum[idFreq] = att.getAttenuation(freqTest[idFreq], phi, theta);
                }
                d.addDirectivityRecord(theta, phi,
                        attSpectrum);
            }
        }

        // test nearest neighbors

        assertEquals(new DiscreteDirectionAttributes.DirectivityRecord((float)Math.toRadians(30),
                (float)Math.toRadians(25), null),
                d.getRecord((float)Math.toRadians(31), (float)Math.toRadians(26), 0));

        assertEquals(new DiscreteDirectionAttributes.DirectivityRecord((float)Math.toRadians(85),
                        (float)Math.toRadians(0), null),
                d.getRecord((float)Math.toRadians(88), (float)Math.toRadians(358), 0));

        assertEquals(new DiscreteDirectionAttributes.DirectivityRecord((float)Math.toRadians(-85),
                (float)Math.toRadians(0), null),
                d.getRecord((float)Math.toRadians(-89), (float)Math.toRadians(2), 0));


        // Test bilinear interpolation
        DiscreteDirectionAttributes.DirectivityRecord r = d.getRecord((float)Math.toRadians(26),
                (float)Math.toRadians(31), 1);
        assertEquals(new DiscreteDirectionAttributes.DirectivityRecord((float)Math.toRadians(26),
                        (float)Math.toRadians(31), null), r);
        assertArrayEquals(new double[]{-5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63, -5.63}, r.getAttenuation(),
                0.1);


        assertEquals(-5.63, d.getAttenuation(freqTest[0], (float)Math.toRadians(31),
                (float)Math.toRadians(26)),0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[1], (float)Math.toRadians(31),
                (float)Math.toRadians(26)),0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[1] + 1, (float)Math.toRadians(31),
                (float)Math.toRadians(26)),0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[0] - 1, (float)Math.toRadians(31),
                (float)Math.toRadians(26)),0.1);
        assertEquals(-5.63, d.getAttenuation(freqTest[freqTest.length - 1] + 1, (float)Math.toRadians(31),
                (float)Math.toRadians(26)),0.1);
    }

}