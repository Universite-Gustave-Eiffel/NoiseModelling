/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.script;

import org.jetbrains.annotations.NotNull;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manage pool of Job Threads.
 */
public class JobExecutorService {
    protected final Map<Integer, Job<?>> jobs = new ConcurrentHashMap<>();
    protected final ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(JobExecutorService.class);

    public JobExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit) {
        this.executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                new SynchronousQueue<>(), Executors.defaultThreadFactory());
    }

    public <T> Future<T> submitJob(Job<T> job) {
        if (jobs.containsKey(job.getId())) {
            throw new IllegalArgumentException(String.format("Job with ID %d already exists.", job.getId()));
        }
        jobs.put(job.getId(), job);
        Future<T> futureTask = executorService.submit(job);
        job.setFuture(futureTask);
        return futureTask;
    }

    /**
     * @param id Job identifier
     * @return The job instance or null if it does not exist
     */
    public Job<?> getJob(int id) {
        return jobs.get(id);
    }

    /**
     * Cancel a job
     * @param jobId Job identifier
     * @return true if the job was found and canceled, false otherwise
     */
    public boolean cancelJob(int jobId) {
        Job<?> job = jobs.get(jobId);
        if (job != null) {
            job.cancel();
            jobs.remove(jobId);
            return true;
        } else {
            logger.error("Job with ID {} not found.", jobId);
            return false;
        }
    }

    /**
     * Remove a job from the service. This does not cancel the job if it is still running,
     * it just removes it from the tracking map. See cancelJob(int jobId) to cancel a job before removing it.
     * @param jobId Job identifier
     * @return The removed job instance or null if it did not exist
     */
    public Job<?> removeJob(int jobId) {
        return jobs.remove(jobId);
    }

    public void shutdown() {
        executorService.shutdown();
    }

}