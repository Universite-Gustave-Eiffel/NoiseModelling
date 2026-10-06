/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.propagation;

/**
 * Compute the probability of favourable propagation probability along different noise propagation directions
 */
public interface FavourableProbability {
    /**
     * Returns the favourable probability for a given angle.
     * @param angle Propagation angle (radians) from source to receiver where 0: north to south, pi/2: east to west (south clockwise convention)
     * @return Favourable propagation condition probability [0-1]
     */
    double getFavourableProbability(double angle);

    /**
     * @return For the AtmosphericSettings table, returns the settings for the favourable probability model
     */
    String getFavourableProbabilitySettings();
}
