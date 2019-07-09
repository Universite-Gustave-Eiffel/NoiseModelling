package org.noise_planet.nmtutorial01;

import org.noise_planet.noisemodelling.propagation.FastObstructionTest;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;

class TrafficPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessData(freeFieldFinder);
    }
}

