/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;


/**
 * This class collects coordinates and gives each corner of their envelope the Z value of the nearest point.
 */
public class CornerZCollector {

    private final Envelope envelope = new Envelope();
    // Collected points, the nearest one to each final corner is searched in getCorners
    private final List<Coordinate> points = new ArrayList<>();

    // Cached corner points
    private final Coordinate[] corners = new Coordinate[] {
            new Coordinate(), new Coordinate(), new Coordinate(), new Coordinate()
    };

    /**
     * Adds a coordinate to the collector.
     * @param c The coordinate to add.
     */
    public void addCoordinate(Coordinate c) {
        envelope.expandToInclude(c);
        points.add(c);
    }

    /**
     * Updates the corner points based on the current envelope.
     */
    private void updateCorners() {
        corners[0].setCoordinate(new Coordinate(envelope.getMinX(), envelope.getMinY()));
        corners[1].setCoordinate(new Coordinate(envelope.getMaxX(), envelope.getMinY()));
        corners[2].setCoordinate(new Coordinate(envelope.getMaxX(), envelope.getMaxY()));
        corners[3].setCoordinate(new Coordinate(envelope.getMinX(), envelope.getMaxY()));
    }

    public void expandEnvelope(double distance) {
        envelope.expandBy(distance);
        updateCorners();
    }

    /**
     * Returns the coordinates of the corners with the best Z values.
     * @return An array of coordinates representing the corners with the best Z values or null if no points have been added.
     */
    public Coordinate[] getCorners() {
        if (points.isEmpty()) return null;
        updateCorners();
        Coordinate[] bestPoints = new Coordinate[4];
        double[] minDistances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        for (Coordinate c : points) {
            for (int i = 0; i < 4; i++) {
                double dist = corners[i].distance(c);
                if (dist < minDistances[i]) {
                    minDistances[i] = dist;
                    bestPoints[i] = c;
                }
            }
        }

        return new Coordinate[] {
                new Coordinate(corners[0].x, corners[0].y, bestPoints[0].getZ()),
                new Coordinate(corners[1].x, corners[1].y, bestPoints[1].getZ()),
                new Coordinate(corners[2].x, corners[2].y, bestPoints[2].getZ()),
                new Coordinate(corners[3].x, corners[3].y, bestPoints[3].getZ())
        };
    }
}
