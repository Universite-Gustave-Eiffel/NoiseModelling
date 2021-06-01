package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiscreteDirectionAttributesTest {
    final static double[] freqTest = new double[] {125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    @Test
    public void testInsert() {
        DiscreteDirectionAttributes d = new DiscreteDirectionAttributes(1, freqTest);

        RailWayLW.TrainAttenuation att = new RailWayLW.TrainAttenuation(RailWayLW.TrainNoiseSource.TRACTIONB);

        for(float theta = 0; theta < 2 * Math.PI; theta += Math.toRadians(15)) {
            for(float phi = 0; phi < Math.PI / 2; phi += Math.toRadians(15)) {
                double[] attSpectrum = new double[freqTest.length];
                for (int idFreq = 0; idFreq < freqTest.length; idFreq++) {
                    attSpectrum[idFreq] = att.getAttenuation(freqTest[idFreq], phi, theta);
                }
                d.addDirectivityRecord(theta, phi,
                        attSpectrum);
            }
        }

        System.out.println(d.getRecord((float)Math.toRadians(26), (float)Math.toRadians(31), 0));

    }

}