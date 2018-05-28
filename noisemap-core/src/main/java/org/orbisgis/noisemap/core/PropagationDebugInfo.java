package org.orbisgis.noisemap.core;

import org.locationtech.jts.geom.Coordinate;

import java.util.List;

/**
 * Store propagation path and related noise contribution.
 * @author Nicolas Fortin
 */
public class PropagationDebugInfo {
    private List<Coordinate> propagationPath;
    private double[] noiseContribution;

    public PropagationDebugInfo(List<Coordinate> propagationPath, double[] noiseContribution) {
        this.propagationPath = propagationPath;
        this.noiseContribution = noiseContribution;
    }

    public List<Coordinate> getPropagationPath() {
        return propagationPath;
    }

    public void addNoiseContribution(int idFreq, double noiseLevel) {
        noiseContribution[idFreq] += noiseLevel;
    }

    public double[] getNoiseContribution() {
        return noiseContribution;
    }
}
