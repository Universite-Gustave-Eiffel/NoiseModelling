package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;

public class TestOrientation {

    @Test
    public void testOrientationYaw() {
        Orientation orientationA = new Orientation(90, 0, 0);
        Orientation rotated = orientationA.rotate(new Orientation(90, 0 ,0));
        System.out.println(rotated);
    }
}
