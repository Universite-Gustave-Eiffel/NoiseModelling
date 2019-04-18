package org.noise_planet.noisemodelling.propagation;

import java.util.List;

public interface IComputeRaysOut {

    /**
     * Add propagation path
     * @param sourceId Source identifier
     * @param sourceLi Source power per meter coefficient ( > 1.0 for line source segments with greater length than 1 meter)
     * @param propagationPath Propagation path result
     * @return Optional minimal energetic contribution per frequency band or empty array
     */
    double[] addPropagationPaths(int sourceId, double sourceLi, int receiverId, List<PropagationPath> propagationPath);

    /**
     * If the implementation does not support thread concurrency, this method is called to return an instance
     * @param receiverStart
     * @param receiverEnd
     * @return
     */
    IComputeRaysOut subProcess(int receiverStart, int receiverEnd);
}
