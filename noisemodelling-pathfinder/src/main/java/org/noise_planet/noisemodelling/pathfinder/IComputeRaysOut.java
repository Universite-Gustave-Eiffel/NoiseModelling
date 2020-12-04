package org.noise_planet.noisemodelling.pathfinder;

import java.util.List;

public interface IComputeRaysOut {

    /**
     * Add propagation path
     * @param sourceId Source identifier
     * @param sourceLi Source power per meter coefficient ( > 1.0 for line source segments with greater length than 1 meter)
     * @param propagationPath Propagation path result
     * @return Optional minimal energetic contribution per frequency band or empty array
     */
    double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath);

    /**
     * No more propagation paths will be pushed for this receiver identifier
     * @param receiverId
     */
    void finalizeReceiver(long receiverId);
    /**
     * If the implementation does not support thread concurrency, this method is called to return an instance
     * @param receiverStart
     * @param receiverEnd
     * @return
     */
    IComputeRaysOut subProcess(int receiverStart, int receiverEnd);
}
