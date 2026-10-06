package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import java.util.List;

/**
 * Services implementing this interface must initialize any frequency-dependent
 * data structures they own.
 */
public interface FrequencyInitializable {
    /**
     * Initialize frequency-dependent data using the provided exact frequency list.
     *
     * @param exactFrequencyArray list of exact frequency values
     */
    void initializeFrequencyDependentData(List<Double> exactFrequencyArray);
}
