/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

/**
 * Declares the factory method that returns PropagationModel objects
 * @author Martin Glesser
 */
public interface PropagationModelCreator {
    /**
     * Factory method that returns PropagationModel objects
     * @return PropagationModel object
     */
    PropagationModel create();
}
