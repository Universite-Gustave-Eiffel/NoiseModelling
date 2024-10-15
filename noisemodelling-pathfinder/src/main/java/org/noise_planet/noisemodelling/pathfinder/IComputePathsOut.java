/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder;

import org.noise_planet.noisemodelling.pathfinder.cnossos.CnossosPath;

import java.util.List;

public interface IComputePathsOut {

    /**
     * Get propagation path result
     * @param sourceId Source identifier
     * @param sourceLi Source power per meter coefficient
     * @param pathParameters Propagation path result
     */
    double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<CnossosPath> pathParameters);

    /**
     * No more propagation paths will be pushed for this receiver identifier
     * @param receiverId
     */
    void finalizeReceiver(long receiverId);
    /**
     * If the implementation does not support thread concurrency, this method is called to return an instance
     * @return
     */
    IComputePathsOut subProcess();
}
