package org.noise_planet.noisemodelling.pathfinder.utils;

public class JVMMemoryMetric implements ProfilerThread.Metric {
    @Override
    public String[] getColumnNames() {
        return new String[] {"jvm_used_heap_mb", "jvm_max_heap_mb"};
    }

    @Override
    public String[] getCurrentValues() {
        Runtime r = Runtime.getRuntime();
        return new String[] {Long.toString((r.totalMemory() - r.freeMemory()) / 1048576L),
                Long.toString(r.totalMemory() / 1048576L)};
    }

    @Override
    public void tick(long currentMillis) {

    }
}
