package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiscreteDirectionAttributesTest {
    final static double[] freqTest = new double[] {125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    @Test
    public void testInsert() {
        DiscreteDirectionAttributes d = new DiscreteDirectionAttributes(1, freqTest);

        d.addDirectivityRecord((float) Math.toRadians(15), (float) Math.toRadians(15),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});
        d.addDirectivityRecord((float) Math.toRadians(15), (float) Math.toRadians(30),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});
        d.addDirectivityRecord((float) Math.toRadians(15), (float) Math.toRadians(45),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});

        d.addDirectivityRecord((float) Math.toRadians(0), (float) Math.toRadians(15),
                new double[] {-20, -20, -20, -20, -20, -20, -20, -20});
        d.addDirectivityRecord((float) Math.toRadians(0), (float) Math.toRadians(45),
                new double[] {-20, -20, -20, -20, -20, -20, -20, -20});
        d.addDirectivityRecord((float) Math.toRadians(0), (float) Math.toRadians(30),
                new double[] {-20, -20, -20, -20, -20, -20, -20, -20});

        d.addDirectivityRecord((float) Math.toRadians(45), (float) Math.toRadians(15),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});
        d.addDirectivityRecord((float) Math.toRadians(45), (float) Math.toRadians(30),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});
        d.addDirectivityRecord((float) Math.toRadians(45), (float) Math.toRadians(45),
                new double[] {-8, -8, -8, -8, -8, -8, -8, -8});


        System.out.println(d.getRecord((float)Math.toRadians(22), (float)Math.toRadians(31), 0));

    }

}