/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
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
    private int writeInterval = 60;
    private int flushInterval = 300;
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

    /**
     * @param flushInterval Time in seconds between each effective write on the hard drive
     */
    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    @Override
    public void run() {
        long lastWrite = 0;
        long lastFlush = 0;
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
            b.flush();
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
                    if((timeTracker.get() - lastFlush) / 1000.0 >= flushInterval ) {
                        lastFlush = timeTracker.get();
                        b.flush();
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
