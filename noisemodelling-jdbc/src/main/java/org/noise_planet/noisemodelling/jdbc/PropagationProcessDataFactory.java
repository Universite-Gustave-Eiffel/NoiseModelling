/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A factory interface for creating propagation process data for noise map computation.
 */
public interface PropagationProcessDataFactory {


    /**
     * Initializes the propagation process data factory.
     * @param connection             the database connection to be used for initialization.
     * @param ldenNoiseMapLoader the noise map by receiver maker object associated with the computation process.
     * @throws SQLException if an SQL exception occurs while initializing the propagation process data factory.
     */
    void initialize(Connection connection, LdenNoiseMapLoader ldenNoiseMapLoader) throws SQLException;
}
