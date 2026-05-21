/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

import java.util.List;

/**
 * Interface for point to point propagation models. Retrieve
 * attenuation for a given geometrical cross-section / cut profile
 * @author Martin Glesser
 */
public interface PropagationModelFactory {
    /**
     * Compute the attenuation for a given geometrical cross-section
     *
     * @param cutProfile vertical profile / geometrical cross-section
     */
    List<ReceiverNoiseLevel> computeAttenuation(CutProfile cutProfile);
}
