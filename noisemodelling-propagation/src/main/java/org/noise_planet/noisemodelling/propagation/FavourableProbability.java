/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.locationtech.jts.geom.Coordinate;

/**
 * Compute the probability of favourable propagation probability along different noise propagation directions
 */
public interface FavourableProbability {
    /**
     * Returns the favourable probability for a given propagation direction.
     * @param angle Propagation angle in radians from source to receiver, 0 = north, increasing clockwise
     *              (pi / 2 = east, pi = south, 3 * pi / 2 = west)
     * @return Favourable propagation condition probability [0-1]
     */
    double getFavourableProbability(double angle);

    /**
     * Returns the favourable probability along the path from source to receiver.
     * @param source source coordinate
     * @param receiver receiver coordinate
     * @return Favourable propagation condition probability [0-1]
     */
    default double getFavourableProbability(Coordinate source, Coordinate receiver) {
        return getFavourableProbability(getPropagationAngle(source, receiver));
    }

    /**
     * Compute the propagation angle from source to receiver.
     * @param source source coordinate
     * @param receiver receiver coordinate
     * @return angle in radians in [0, 2 * pi), 0 = north, increasing clockwise (pi / 2 = east)
     */
    static double getPropagationAngle(Coordinate source, Coordinate receiver) {
        double angle = Math.atan2(receiver.getX() - source.getX(), receiver.getY() - source.getY());
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    /**
     * @return For the AtmosphericSettings table, returns the settings for the favourable probability model
     */
    String getFavourableProbabilitySettings();
}
