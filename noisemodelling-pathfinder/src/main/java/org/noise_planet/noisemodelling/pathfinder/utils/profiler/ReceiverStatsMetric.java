/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder.utils.profiler;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Generate stats about receiver computation time
 */
public class ReceiverStatsMetric implements ProfilerThread.Metric {
    private ConcurrentLinkedDeque<ReceiverComputationTime> receiverComputationTimes = new ConcurrentLinkedDeque<>();
    private ConcurrentLinkedDeque<ReceiverCutProfiles> receiverCutProfilesDeque = new ConcurrentLinkedDeque<>();
    private DescriptiveStatistics computationTime = new DescriptiveStatistics();
    private DescriptiveStatistics computationCutProfiles = new DescriptiveStatistics();
    private DescriptiveStatistics computationProcessSourcesPercentage = new DescriptiveStatistics();
    private DescriptiveStatistics collectSourcesTime = new DescriptiveStatistics();
    private DescriptiveStatistics precomputeReflectionTime = new DescriptiveStatistics();

    public ReceiverStatsMetric() {
    }

    @Override
    public void tick(long currentMillis) {
        while (!receiverComputationTimes.isEmpty()) {
            ReceiverComputationTime receiverProfile = receiverComputationTimes.pop();
            computationTime.addValue(receiverProfile.computationTime);
        }
        while (!receiverCutProfilesDeque.isEmpty()) {
            ReceiverCutProfiles receiverProfile = receiverCutProfilesDeque.pop();
            computationCutProfiles.addValue(receiverProfile.numberOfRays);
            if(receiverProfile.numberOfSources > 0) {
                computationProcessSourcesPercentage.addValue(((double) receiverProfile.numberOfProcessSources / receiverProfile.numberOfSources) * 100);
            }
        }
    }

    @Override
    public String[] getColumnNames() {
        return new String[] {"receiver_min_milliseconds","receiver_median_milliseconds","receiver_mean_milliseconds","receiver_max_milliseconds", "receiver_collect_sources_max_milliseconds", "receiver_precompute_reflection_max_milliseconds", "receiver_median_profiles_count", "receiver_max_profiles_count", "receiver_processed_sources_percentage_mean"};
    }

    public void onEndComputation(ReceiverComputationTime receiverComputationTime) {
        receiverComputationTimes.add(receiverComputationTime);
    }

    public void onReceiverCutProfiles(int receiverId, int receiverCutProfiles, int numberOfSources,
                                      int numberOfProcessSources) {
        receiverCutProfilesDeque.add(new ReceiverCutProfiles(receiverId, receiverCutProfiles, numberOfSources,
                numberOfProcessSources));
    }

    @Override
    public String[] getCurrentValues() {
        String[] res = new String[] {
                Integer.toString((int) computationTime.getMin()),
                Integer.toString((int) computationTime.getPercentile(50)),
                Integer.toString((int) computationTime.getMean()),
                Integer.toString((int) computationTime.getMax()),
                Integer.toString((int) collectSourcesTime.getMax()),
                Integer.toString((int) precomputeReflectionTime.getMax()),
                Integer.toString((int) computationCutProfiles.getPercentile(50)),
                Integer.toString((int) computationCutProfiles.getMax()),
                Integer.toString((int) computationProcessSourcesPercentage.getMean()),
        };
        computationTime.clear();
        computationCutProfiles.clear();
        computationProcessSourcesPercentage.clear();
        collectSourcesTime.clear();
        precomputeReflectionTime.clear();
        return res;
    }

    public static class ReceiverComputationTime {
        public int receiverId;
        public int computationTime;
        public int reflectionPreprocessTime;
        public int sourceCollectTime;

        /**
         * Create the ReceiverComputationTime constructor
         *
         * @param receiverId
         * @param computationTime
         * @param reflectionPreprocessTime
         * @param sourceCollectTime
         */
        public ReceiverComputationTime(int receiverId, int computationTime, int reflectionPreprocessTime, int sourceCollectTime) {
            this.receiverId = receiverId;
            this.computationTime = computationTime;
            this.reflectionPreprocessTime = reflectionPreprocessTime;
            this.sourceCollectTime = sourceCollectTime;
        }
    }

    public static class ReceiverCutProfiles {
        public int receiverId;
        public int numberOfRays;
        int numberOfSources;
        int numberOfProcessSources;

        /**
         * Create the ReceiverCutProfiles constructor
         * @param receiverId
         * @param numberOfCutProfiles
         */
        public ReceiverCutProfiles(int receiverId, int numberOfCutProfiles, int numberOfSources, int numberOfProcessSources) {
            this.receiverId = receiverId;
            this.numberOfRays = numberOfCutProfiles;
            this.numberOfSources = numberOfSources;
            this.numberOfProcessSources = numberOfProcessSources;
        }
    }
}