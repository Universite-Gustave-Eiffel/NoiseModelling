package org.noise_planet.noisemodelling.jdbc.utils;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import static org.junit.jupiter.api.Assertions.*;

public class CornerZCollectorTest {

    /**
     * Each corner of the expanded envelope must take the Z of its nearest DEM point
     */
    @Test
    public void testCornersTakeNearestPointZ() {
        CornerZCollector collector = new CornerZCollector();
        // 3x3 DEM grid, Z = 10 * x + y
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                collector.addCoordinate(new Coordinate(x, y, 10 * x + y));
            }
        }
        collector.expandEnvelope(10);
        Coordinate[] expected = new Coordinate[] {new Coordinate(-10, -10, 0), new Coordinate(12, -10, 20),
                new Coordinate(12, 12, 22), new Coordinate(-10, 12, 2)};
        Coordinate[] corners = collector.getCorners();
        for (int i = 0; i < 4; i++) {
            assertTrue(expected[i].equals3D(corners[i]), "Corner " + i + ": " + corners[i]);
        }
    }
}
