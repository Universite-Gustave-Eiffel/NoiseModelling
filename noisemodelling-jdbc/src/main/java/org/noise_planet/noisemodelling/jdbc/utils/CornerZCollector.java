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


/**
 * This class collects coordinates and keeps the Z values of points nearest to each corner of the envelope.
 * It stores only the 4 best candidates found so far, avoiding a full list in memory.
 */
public class CornerZCollector {

    private final Envelope envelope = new Envelope();
    // Stores the current best point (the one that provided the Z value) for each corner
    private final Coordinate[] bestPoints = new Coordinate[4];
    private final double[] minDistances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

    // Cached corner points
    private final Coordinate[] corners = new Coordinate[] {
            new Coordinate(), new Coordinate(), new Coordinate(), new Coordinate()
    };

    /**
     * Adds a coordinate to the collector.
     * @param c The coordinate to add.
     */
    public void addCoordinate(Coordinate c) {
        if (envelope.isNull()) {
            envelope.init(c);
            updateCorners();
            // Initialize best points with the first point
            for (int i = 0; i < 4; i++) {
                bestPoints[i] = c;
                minDistances[i] = corners[i].distance(c);
            }
        } else if (!envelope.contains(c)) {
            envelope.expandToInclude(c);
            updateCorners();
            // Re-evaluate distances for all points as corners have moved
            for (int i = 0; i < 4; i++) {
                double dist = corners[i].distance(c);
                if (dist < minDistances[i]) {
                    minDistances[i] = dist;
                    bestPoints[i] = c;
                }
            }
        } else {
            // Envelope unchanged, only compare with current cached corners
            for (int i = 0; i < 4; i++) {
                double dist = corners[i].distance(c);
                if (dist < minDistances[i]) {
                    minDistances[i] = dist;
                    bestPoints[i] = c;
                }
            }
        }
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
        if (bestPoints[0] == null) return null;

        return new Coordinate[] {
                new Coordinate(corners[0].x, corners[0].y, bestPoints[0].getZ()),
                new Coordinate(corners[1].x, corners[1].y, bestPoints[1].getZ()),
                new Coordinate(corners[2].x, corners[2].y, bestPoints[2].getZ()),
                new Coordinate(corners[3].x, corners[3].y, bestPoints[3].getZ())
        };
    }
}
