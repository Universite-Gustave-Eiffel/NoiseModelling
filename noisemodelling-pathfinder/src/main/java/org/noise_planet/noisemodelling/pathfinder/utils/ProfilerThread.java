package org.noise_planet.noisemodelling.pathfinder.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilerThread  implements Runnable {
    public Logger log = LoggerFactory.getLogger(ProfilerThread.class);
    public AtomicLong timeTracker = new AtomicLong(System.currentTimeMillis());
    private AtomicBoolean doRun = new AtomicBoolean(true);
    private int writeInterval = 300;
    private File outputFile;
    long start;
    private Map<String, Integer> metricsIndex = new HashMap<>();
    private List<Metric> metrics = new ArrayList<>();

    public ProfilerThread(File outputFile) {
        this.outputFile = outputFile;
        start = System.currentTimeMillis();
        addMetric(new TimeMetric(timeTracker, start));
    }

    public void addMetric(Metric metric) {
        metrics.add(metric);
        metricsIndex.put(metric.getClass().getName(), metrics.size() - 1);
    }

    /**
     * @param writeInterval In seconds, intervals for writing new metrics in the csv file
     */
    public void setWriteInterval(int writeInterval) {
        this.writeInterval = writeInterval;
    }

    @Override
    public void run() {
        long lastWrite = 0;
        try(BufferedWriter b = new BufferedWriter(new FileWriter(outputFile))) {
            StringBuilder sb = new StringBuilder();
            for(Metric m : metrics) {
                for(String columnName : m.getColumnNames()) {
                    if(sb.length() != 0) {
                        sb.append(",");
                    }
                    sb.append(columnName);
                }
            }
            sb.append("\n");
            b.write(sb.toString());
            while (doRun.get()) {
                timeTracker.set(System.currentTimeMillis());
                for(Metric m : metrics) {
                    m.tick(timeTracker.get());
                }
                try {
                    if((timeTracker.get() - lastWrite) / 1000.0 >= writeInterval ) {
                        lastWrite = timeTracker.get();
                        sb = new StringBuilder();
                        for(Metric m : metrics) {
                            for(String metricValue : m.getCurrentValues()) {
                                if(sb.length() != 0) {
                                    sb.append(",");
                                }
                                sb.append(metricValue);
                            }
                        }
                        sb.append("\n");
                        b.write(sb.toString());
                    } else {
                        Thread.sleep(2);
                    }
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } catch (IOException ex) {
            log.error("Error while writing file", ex);
        }
    }

    public void stop() {
        doRun.set(false);
    }

    public <T extends Metric> T getMetric(Class<T> metricClass) {
        Integer mIndex = metricsIndex.get(metricClass.getName());
        if(mIndex != null) {
            Metric o = metrics.get(mIndex);
            if (metricClass.isInstance(o)) {
                return metricClass.cast(o);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private class TimeMetric implements Metric {
        AtomicLong timeTracker;
        long startTime;

        public TimeMetric(AtomicLong timeTracker, long startTime) {
            this.timeTracker = timeTracker;
            this.startTime = startTime;
        }

        @Override
        public String[] getColumnNames() {
            return new String[] {"time"};
        }

        @Override
        public String[] getCurrentValues() {
            return new String[] {String.format(Locale.ROOT, "%.2f", (timeTracker.get() - start) / 1e3)};
        }

        @Override
        public void tick(long currentMillis) {

        }
    }

    /**
     * Metric is a collection of statistics to write on the profile csv file
     */
    public interface Metric {
        String[] getColumnNames();
        String[] getCurrentValues();

        /**
         * Called with little intervals in order to process metrics on the same thread than
         * the call to getCurrentValues
         * @param currentMillis Time
         */
        void tick(long currentMillis);
    }
}
