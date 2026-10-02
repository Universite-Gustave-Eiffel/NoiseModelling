/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.output;

import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.ReceiverNoiseLevel;
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Store results in memory, awaiting writing on sql database
 */
public class ResultsCache implements ProfilerThread.Metric {
    /**
     * As reading the size of the queue is a O(n) operation, this attribute store the current number of elements in the stacks
     */
    public final AtomicLong queueSize = new AtomicLong(0);
    public final AtomicLong totalRaysInserted = new AtomicLong(0);
    public final ConcurrentLinkedDeque<ReceiverNoiseLevel> receiverLevels = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<AttenuationOutput> attenuationOutputs = new ConcurrentLinkedDeque<>();
    public final AtomicLong statisticsSumDistanceCutSource = new AtomicLong(0);
    public final AtomicInteger statisticsSumDistanceCutSourceCount = new AtomicInteger(0);
    public final AtomicLong statisticsSumSourcesCount = new AtomicLong(0);
    public final AtomicLong statisticsSumSourcesCountIgnored = new AtomicLong(0);

    @Override
    public String[] getColumnNames() {
        return new String[] {"jdbc_stack", "average_cut_source_distance"};
    }

    @Override
    public String[] getCurrentValues() {
        // Metric that return unprocessed data (not yet recorded in the database)
        return new String[] {Long.toString(queueSize.get()), Double.toString(getAverageCutSourceDistance())};
    }

    /**
     * Get the average distance at which noise sources are ignored for all receivers
     * @return the average distance at which noise sources are ignored for all receivers
     */
    public double getAverageCutSourceDistance() {
        long count = statisticsSumDistanceCutSourceCount.get();
        long sum = statisticsSumDistanceCutSource.get();
        return count > 0 ? (double) sum / count : 0;
    }

    /**
     * Get the percentage of ignored sources.
     * @return Percentage of ignored sources.
     */
    public double getPercentageIgnoredSources() {
        long totalSources = statisticsSumSourcesCount.get();
        long ignoredSources = statisticsSumSourcesCountIgnored.get();
        return totalSources > 0 ? (double) ignoredSources / totalSources * 100 : 0;
    }

    @Override
    public void tick(long currentMillis) {

    }

}
