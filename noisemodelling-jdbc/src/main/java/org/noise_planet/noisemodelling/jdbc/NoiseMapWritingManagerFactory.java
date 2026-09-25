package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.PathFinderProcessorManager;
import org.noise_planet.noisemodelling.propagation.PropagationModelFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A factory interface for creating objects that manages the noise map computations and writing.
 */
public interface NoiseMapWritingManagerFactory {
    /**
     * Called only once when the settings are set.
     *
     * @param connection              the database connection to be used for initialization.
     * @param noiseMapByReceiverMaker the noise map by receiver maker object associated with the computation process.
     * @throws SQLException if an SQL exception occurs while initializing the propagation process data factory.
     */
    void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException;

    /**
     * Called before the first sub cell is being computed
     *
     * @param progressLogger Main progression information, this method will not update the progression
     * @throws SQLException If an SQL exception occurs
     */
    void start(ProgressVisitor progressLogger) throws SQLException;

    /**
     * Called when all sub-cells have been processed
     *
     * @throws SQLException If an SQL exception occurs
     */
    void stop() throws SQLException;

    /**
     * Creates an object that will manage the computations performed at different steps
     * of the path finding.
     *
     * @param cellData the scene data for the current computation cell
     * @param propagationModelFactory the propagation model to be used
     * @return an object that manages the computations performed at different steps of the path finding
     */
    PathFinderProcessorManager createProcessorManager(SceneWithEmission cellData,
                                                      PropagationModelFactory propagationModelFactory);
}
