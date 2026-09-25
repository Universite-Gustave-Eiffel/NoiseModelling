/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.PathFinderProcessor;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

import java.util.List;

/**
 * Interface for point to point propagation models.
 * Note : the instances of the different implementations of
 * the interface must be thread-safe.
 * @author Martin Glesser
 */
public interface PropagationModel {

    /**
     * Called each time a new cut profile is detected
     */
    void initialize();

    /**
     * Launches the path finding methods (direct path, diffracted path and/or reflected path)
     *
     * @param src source point information
     * @param rcv receiver point information
     * @param receiverMirrorIndex reflexion information
     * @param propagationProcess PathFinder instance
     * @param computationProcessor object launching the computations performed at different steps of the path finding
     * @return Search strategy for the next steps of the path finding
     */
    PathFinderProcessor.PathSearchStrategy callRcvSrcPropagationMethod(PathFinder.SourcePointInfo src,
                                                                       PathFinder.ReceiverPointInfo rcv,
                                                                       MirrorReceiversCompute receiverMirrorIndex,
                                                                       PathFinder propagationProcess,
                                                                       PathFinderProcessor computationProcessor);

    /**
     * Compute the attenuation for a given cut-profile
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param attenuationParameters parameters of the computation

     * @param isExportAttenuationMatrix if true, store intermediate values in AttenuationOutput for debugging purpose
     * @return List of AttenuationOutput objects (some propagation models return several AttenuationOutput per cutProfile)
     */
    List<AttenuationOutput> computeAttenuation(SceneWithAttenuation scene, CutProfile cutProfile,
                                      AttenuationParameters attenuationParameters,
                                      boolean isExportAttenuationMatrix);

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param source source point information
     * @param receiver receiver point information
     * @param scene Geometrical information about the propagation scene
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return AttenuationOutput object
     */
    AttenuationOutput computeDirectAttenuation(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                      SceneWithAttenuation scene, AttenuationParameters attenuationParameters,
                                      boolean isExportAttenuationMatrix);

}
