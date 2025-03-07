package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.PathFinder;

/**
 * Attenuation or noise level value at receiver location
 * May be linked with a source
 * May be linked with a period
 */
public class ReceiverNoiseLevel {
    public PathFinder.SourcePointInfo source = null;
    public PathFinder.ReceiverPointInfo receiver = null;
    public String period = "";
    public double [] levels = new double[0];

    public ReceiverNoiseLevel(PathFinder.SourcePointInfo source,
                                PathFinder.ReceiverPointInfo receiver,
                                String period,
                                double[] levels) {
        this.levels = levels;
        this.period = period;
        this.receiver = receiver;
        this.source = source;
    }

    public ReceiverNoiseLevel() {
    }
}
