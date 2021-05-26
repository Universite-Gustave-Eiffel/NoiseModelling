package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.math.Vector3D;

import static org.junit.Assert.assertEquals;

public class TestOrientation {

    @Test
    public void testOrientationOps() {
        Orientation orientationA = new Orientation(0, 0, 0);
        Orientation rotated = Orientation.rotate(orientationA, new Vector3D(1,0,0).normalize());
        assertEquals(0, rotated.bearing, 1e-6);
        assertEquals(0, rotated.inclination, 1e-6);

        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize());
        assertEquals(90, rotated.bearing, 1e-6);
        assertEquals(0, rotated.inclination, 1e-6);

        orientationA = new Orientation(90, 0, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize());
        assertEquals(180, rotated.bearing, 1e-6);
        assertEquals(0, rotated.inclination, 1e-6);

        orientationA = new Orientation(0, 45, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(1,0,0).normalize());
        assertEquals(0, rotated.bearing, 1e-6);
        assertEquals(45, rotated.inclination, 1e-6);
    }


    @Test
    public void testOrientationTranspose() {
        Orientation orientationA = new Orientation(90, 0, 0);
        Orientation rotated = Orientation.rotate(orientationA, new Vector3D(0, 1, 0).normalize(), true);
        assertEquals(0, rotated.bearing, 1e-6);
        assertEquals(0, rotated.inclination, 1e-6);

        orientationA = new Orientation(0, -45, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(1,0,0).normalize(), true);
        assertEquals(0, rotated.bearing, 1e-6);
        assertEquals(45, rotated.inclination, 1e-6);

        long deb = System.currentTimeMillis();
        for(int i = 0; i < 1e6; i++) {
            orientationA = new Orientation(0, 0, 45);
            rotated = Orientation.rotate(orientationA, new Vector3D(0, 1, 1).normalize(), true);
            assertEquals(90, rotated.bearing, 1e-6);
            assertEquals(0, rotated.inclination, 1e-6);
        }
        System.out.println(String.format("Computation time: %d ms", (int)((System.currentTimeMillis() - deb))));
    }
}
