package org.noise_planet.nmtutorial01;


import org.noise_planet.noisemodelling.propagation.ComputeRays;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.GeoJSONDocument;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationPath;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 */
class PropagationPathStorage extends ComputeRaysOut {
    // Thread safe queue object
    protected TrafficPropagationProcessData inputData;

    PropagationPathStorage(PropagationProcessData inputData, PropagationProcessPathData pathData) {
        super(false, pathData, inputData);
        this.inputData = (TrafficPropagationProcessData)inputData;
    }

    @Override
    public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath);
        double[] soundLevel = ComputeRays.wToDba(ComputeRays.multArray(inputData.wjSourcesD.get((int)sourceId), ComputeRays.dbaToW(attenuation)));
        return soundLevel;
    }
}
