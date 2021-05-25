package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.math.Vector3D;

import static org.junit.Assert.assertEquals;

public class TestOrientation {

    @Test
    public void testOrientationYaw() {
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
    }
}
