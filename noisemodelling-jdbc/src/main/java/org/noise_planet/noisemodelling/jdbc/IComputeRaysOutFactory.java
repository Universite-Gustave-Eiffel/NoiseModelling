package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitorFactory;
import org.noise_planet.noisemodelling.propagation.PropagationModelCreator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A factory interface for creating objects that compute rays out for noise map computation.
 */
public interface IComputeRaysOutFactory {
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
     * @throws SQLException
     */
    void stop() throws SQLException;

    /**
     * Creates an object that computes paths out for noise map computation.
     *
     * @param cellData the scene data for the current computation cell
     * @return an object that computes paths out for noise map computation.
     */
    CutPlaneVisitorFactory create(SceneWithEmission cellData);

    /**
     * Setter for propagationModelCreator
     *
     * @param propagationModelCreator interface for PropagationModel creation
     */
    void setPropagationModelCreator(PropagationModelCreator propagationModelCreator);

    /**
     * Getter for propagationModelCreator
     *
     * @return interface for PropagationModel creation
     */
    PropagationModelCreator getPropagationModelCreator();
}
