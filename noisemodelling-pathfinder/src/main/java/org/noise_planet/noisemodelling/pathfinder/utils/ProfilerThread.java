package org.noise_planet.noisemodelling.pathfinder.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilerThread  implements Runnable {
    public Logger log = LoggerFactory.getLogger(ProfilerThread.class);
    public AtomicLong timeTracker = new AtomicLong(System.currentTimeMillis());
    public AtomicBoolean doRun = new AtomicBoolean(true);
    private ConcurrentLinkedDeque<ReceiverProfile> receiverProfiles = new ConcurrentLinkedDeque<>();
    private DescriptiveStatistics stats = new DescriptiveStatistics();
    private static final int PROCESSING_TIMEOUT = 5000;

    @Override
    public void run() {
        while (doRun.get() || !receiverProfiles.isEmpty()) {
            timeTracker.set(System.currentTimeMillis());
            try {
                if(!receiverProfiles.isEmpty()) {
                    while (!receiverProfiles.isEmpty()) {
                        ReceiverProfile receiverProfile = receiverProfiles.pop();
                        stats.addValue(receiverProfile.computationTime);
                    }
                } else {
                    Thread.sleep(2);
                }
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    public void printStats() {
        if (doRun.get()) {
            return;
        }
        long start = System.currentTimeMillis();
        while (!receiverProfiles.isEmpty()) {
            try {
                if( System.currentTimeMillis() - start > PROCESSING_TIMEOUT) {
                    return;
                }
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                return;
            }
        }
        double mean = stats.getMean();
        double max = stats.getMax();
        double min = stats.getMin();
        double median = stats.getPercentile(50);
        log.info(String.format(Locale.ROOT, "Receiver computation statistics: " +
                "mean: %d ms  min: %d ms max: %d ms  median: %d ms",
                (int) mean, (int) min, (int) max, (int) median));
    }

    public void clearStats() {
        stats.clear();
        receiverProfiles.clear();
    }

    public void onEndComputation(int receiverId, int computationTime) {
        receiverProfiles.add(new ReceiverProfile(receiverId, computationTime));
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
