/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

/**
 * A factory interface for creating objects that compute rays out for noise map computation.
 */
public interface IComputeRaysOutFactory {
    /**
     * Creates an object that computes paths out for noise map computation.
     *
     * @param threadData       the scene data for the current computation thread.
     * @param pathDataDay      the attenuation parameters for daytime computation.
     * @param pathDataEvening  the attenuation parameters for evening computation.
     * @param pathDataNight    the attenuation parameters for nighttime computation.
     * @return an object that computes paths out for noise map computation.
     */
    IComputePathsOut create(Scene threadData, AttenuationCnossosParameters pathDataDay,
                            AttenuationCnossosParameters pathDataEvening, AttenuationCnossosParameters pathDataNight);
}
