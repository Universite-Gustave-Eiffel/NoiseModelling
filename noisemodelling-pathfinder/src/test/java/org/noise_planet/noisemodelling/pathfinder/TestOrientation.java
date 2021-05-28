package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.math.Vector3D;

import static org.junit.Assert.assertEquals;

public class TestOrientation {

    @Test
    public void testOrientationOps() {
        // North Vector
        Orientation orientationA = new Orientation(0, 0, 0);
        Orientation rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize());
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(0, rotated.pitch, 1e-6);


        rotated = Orientation.rotate(orientationA, new Vector3D(1, 0,0).normalize());
        assertEquals(90, rotated.yaw, 1e-6);
        assertEquals(0, rotated.pitch, 1e-6);

        orientationA = new Orientation(90, 0, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize());
        assertEquals(90, rotated.yaw, 1e-6);
        assertEquals(0, rotated.pitch, 1e-6);

        orientationA = new Orientation(0, 45, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize());
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(45, rotated.pitch, 1e-6);

        orientationA = new Orientation(0, 0, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,1).normalize());
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(45, rotated.pitch, 1e-6);

        rotated = Orientation.rotate(new Orientation(0, 45, 0), new Vector3D(0,1,0).normalize());
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(45, rotated.pitch, 1e-6);
    }


    @Test
    public void testOrientationTranspose() {
        Orientation orientationA = new Orientation(90, 0, 0);
        Orientation rotated = Orientation.rotate(orientationA, new Vector3D(1, 0, 0).normalize(), true);
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(0, rotated.pitch, 1e-6);

        orientationA = new Orientation(0, -45, 0);
        rotated = Orientation.rotate(orientationA, new Vector3D(0,1,0).normalize(), true);
        assertEquals(0, rotated.yaw, 1e-6);
        assertEquals(45, rotated.pitch, 1e-6);

        orientationA = new Orientation(0, 0, 45);
        rotated = Orientation.rotate(orientationA, new Vector3D(1, 0, 1).normalize(), true);
        assertEquals(90, rotated.yaw, 1e-6);
        assertEquals(0, rotated.pitch, 1e-6);
    }
}
