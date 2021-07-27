package org.noise_planet.noisemodelling.pathfinder.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Generate stats about receiver computation time
 */
public class ReceiverStatsMetric implements ProfilerThread.Metric {
    public static final String RECEIVER_COMPUTATION_TIME = "RECEIVER_COMPUTATION_TIME";
    private ConcurrentLinkedDeque<ReceiverProfile> receiverProfiles = new ConcurrentLinkedDeque<>();
    private DescriptiveStatistics stats = new DescriptiveStatistics();

    public ReceiverStatsMetric() {
    }

    @Override
    public void tick(long currentMillis) {
        while (!receiverProfiles.isEmpty()) {
            ReceiverProfile receiverProfile = receiverProfiles.pop();
            stats.addValue(receiverProfile.computationTime);
        }
    }

    @Override
    public String[] getColumnNames() {
        return new String[] {"receiver_min","receiver_median","receiver_mean","receiver_max"};
    }

    public void onEndComputation(int receiverId, int computationTime) {
        receiverProfiles.add(new ReceiverProfile(receiverId, computationTime));
    }

    @Override
    public String[] getCurrentValues() {
        String[] res = new String[] {
                Integer.toString((int)stats.getMin()),
                Integer.toString((int)stats.getPercentile(50)),
                Integer.toString((int)stats.getMean()),
                Integer.toString((int)stats.getMax())
        };
        stats.clear();
        return res;
    }

    public static class ReceiverProfile {
        public int receiverId;
        public int computationTime;

        public ReceiverProfile(int receiverId, int computationTime) {
            this.receiverId = receiverId;
            this.computationTime = computationTime;
        }
    }
}