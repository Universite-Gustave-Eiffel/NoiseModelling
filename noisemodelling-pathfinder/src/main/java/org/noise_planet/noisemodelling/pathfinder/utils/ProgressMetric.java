package org.noise_planet.noisemodelling.pathfinder.utils;

import org.h2gis.api.ProgressVisitor;

import java.util.Locale;

/**
 * Metric that write progression value in percentage
 */
public class ProgressMetric implements ProfilerThread.Metric {
    private ProgressVisitor progressVisitor;

    public ProgressMetric(ProgressVisitor progressVisitor) {
        this.progressVisitor = progressVisitor;
    }

    @Override
    public String[] getColumnNames() {
        return new String[] {"progression"};
    }

    @Override
    public String[] getCurrentValues() {
        return new String[] {String.format(Locale.ROOT, "%.2f", progressVisitor.getProgression() *  100.0)};
    }

    @Override
    public void tick(long currentMillis) {

    }
}
