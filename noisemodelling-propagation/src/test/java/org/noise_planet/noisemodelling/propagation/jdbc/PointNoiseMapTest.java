package org.noise_planet.noisemodelling.propagation.jdbc;

import org.junit.Test;

import static org.junit.Assert.*;

public class PointNoiseMapTest {

    @Test
    public void testSplitting() {
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS", "RECEIVERS");

    }

    public void testLdenPointNoiseMap() {
        // Test optimisation on lden scenario
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS", "RECEIVERS");
    }
}