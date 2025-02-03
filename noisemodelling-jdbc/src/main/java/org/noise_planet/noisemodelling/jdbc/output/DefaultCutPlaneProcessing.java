package org.noise_planet.noisemodelling.jdbc.output;

import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultCutPlaneProcessing implements NoiseMapByReceiverMaker.IComputeRaysOutFactory {
    ResultsCache resultsCache = new ResultsCache();
    final NoiseMapDatabaseParameters noiseMapDatabaseParameters;
    NoiseMapWriter noiseMapWriter;
    Thread tableWriterThread;
    Connection connection;
    // Process status
    AtomicBoolean exitWhenDone;
    AtomicBoolean aborted;
    NoiseMapByReceiverMaker noiseMapByReceiverMaker;

    /**
     * @param noiseMapDatabaseParameters Database settings
     * @param exitWhenDone Tell table writer thread to empty current stacks then stop waiting for new data
     * @param aborted If true, all processing are aborted and all threads will be shutdown
     */
    public DefaultCutPlaneProcessing(NoiseMapDatabaseParameters noiseMapDatabaseParameters, AtomicBoolean exitWhenDone, AtomicBoolean aborted) {
        this.noiseMapDatabaseParameters = noiseMapDatabaseParameters;
        this.exitWhenDone = exitWhenDone;
        this.aborted = aborted;
    }

    /**
     *  Creates a new instance of IComputePathsOut using the provided Scene data and AttenuationCnossosParameters for different time periods.
     * @param scene       the scene data for the current computation thread.
     * @return A new instance of IComputePathsOut initialized with the provided parameters.
     */
    @Override
    public CutPlaneVisitor create(SceneWithEmission scene) {
        return new AttenuationOutputMultiThread(scene, resultsCache, noiseMapDatabaseParameters, exitWhenDone, aborted);
    }

    @Override
    public void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException {
        this.connection = connection;
        this.noiseMapByReceiverMaker = noiseMapByReceiverMaker;
    }

    /**
     * Start creating and filling database tables.
     * This method is blocked until the computation is completed or if there is an issue
     */
    public void start() {
        noiseMapWriter = new NoiseMapWriter(connection, noiseMapByReceiverMaker, resultsCache, exitWhenDone, aborted);
        exitWhenDone.set(false);
        tableWriterThread = new Thread(noiseMapWriter);
        tableWriterThread.start();
        while (!noiseMapWriter.started && !aborted.get()) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }

    /**
     * Write the last results and stop the sql writing thread
     * This method is blocked until the data is written or if there is an issue
     */
    public void stop() {
        exitWhenDone.set(true);
        while (tableWriterThread != null && tableWriterThread.isAlive()) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }
}
