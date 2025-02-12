package org.noise_planet.noisemodelling.jdbc.output;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitorFactory;
import org.noise_planet.noisemodelling.pathfinder.ThreadPool;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.JVMMemoryMetric;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProgressMetric;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ReceiverStatsMetric;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultCutPlaneProcessing implements NoiseMapByReceiverMaker.IComputeRaysOutFactory {
    public int DEFAULT_END_WRITING_THREAD_TIMEOUT = 30; // timeout for write thread stop in seconds
    ResultsCache resultsCache = new ResultsCache();
    final NoiseMapDatabaseParameters noiseMapDatabaseParameters;
    NoiseMapWriter noiseMapWriter;
    ProfilerThread profilerThread;
    Connection connection;
    // Process status
    AtomicBoolean exitWhenDone;
    AtomicBoolean aborted;
    NoiseMapByReceiverMaker noiseMapByReceiverMaker;
    ThreadPool postProcessingThreadPool = new ThreadPool();
    Future<Boolean> noiseMapWriterFuture;

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
     *  Creates a new instance of IComputePathsOut using the provided Scene data and AttenuationParameters for different time periods.
     * @param scene       the scene data for the current computation thread.
     * @return A new instance of IComputePathsOut initialized with the provided parameters.
     */
    @Override
    public CutPlaneVisitorFactory create(SceneWithEmission scene) {
        return new AttenuationOutputMultiThread(scene, resultsCache, noiseMapDatabaseParameters, exitWhenDone, aborted);
    }

    @Override
    public void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException {
        this.connection = connection;
        this.noiseMapByReceiverMaker = noiseMapByReceiverMaker;
        if(noiseMapDatabaseParameters.CSVProfilerOutputPath != null) {
            profilerThread = new ProfilerThread(noiseMapDatabaseParameters.CSVProfilerOutputPath);
            profilerThread.addMetric(resultsCache);
            profilerThread.addMetric(new JVMMemoryMetric());
            profilerThread.addMetric(new ReceiverStatsMetric());
            profilerThread.setWriteInterval(noiseMapDatabaseParameters.CSVProfilerWriteInterval);
            profilerThread.setFlushInterval(noiseMapDatabaseParameters.CSVProfilerWriteInterval);
        }
    }

    /**
     * Start creating and filling database tables.
     */
    @Override
    public void start(ProgressVisitor progressLogger) throws SQLException {
        noiseMapWriter = new NoiseMapWriter(connection, noiseMapByReceiverMaker, resultsCache, exitWhenDone, aborted);
        exitWhenDone.set(false);
        if(profilerThread != null) {
            profilerThread.addMetric(new ProgressMetric(progressLogger));
            postProcessingThreadPool.submit(profilerThread);
        }
        try {
            noiseMapWriter.init();
        } catch (Exception ex) {
            throw new SQLException(ex);
        }
        noiseMapWriterFuture = postProcessingThreadPool.submitBlocking(noiseMapWriter);
    }

    /**
     * Write the last results and stop the sql writing thread
     * This method is blocked until the data is written or if there is an issue
     */
    @Override
    public void stop() throws SQLException {
        exitWhenDone.set(true);
        try {
            noiseMapWriterFuture.get(DEFAULT_END_WRITING_THREAD_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
