/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.template;

import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.PathFinderProcessor;
import org.noise_planet.noisemodelling.pathfinder.path.MirrorReceiversCompute;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Template of propagation model. To be used as a basis
 * for new propagation model implementation.
 * @author Martin Glesser
 */
public class TemplatePropagationModel implements PropagationModel {

    /**
     * Constructor for TemplatePropagationModel objects
     */
    public TemplatePropagationModel(){}

    /**
     * Initialize the attenuation computation each time a new cut plane is detected
     */
    @Override
    public void initialize() {

    }

    /**
     * Call the PathFinder method implementing the appropriate
     * rcv to src propagation strategy for Template propagation model
     *
     * @param src source point information
     * @param rcv receiver point information
     * @param receiverMirrorIndex reflexion information
     * @param computationProcessor object launching the computations performed at different steps of the path finding
     * @return Search strategy for the next steps of the path finding
     */
    @Override
    public PathFinderProcessor.PathSearchStrategy callRcvSrcPropagationMethod(PathFinder.SourcePointInfo src,
                                                                              PathFinder.ReceiverPointInfo rcv,
                                                                              MirrorReceiversCompute receiverMirrorIndex,
                                                                              PathFinder propagationProcess,
                                                                              PathFinderProcessor computationProcessor) {
        // CNOSSOS propagation strategy is used, but another strategy can be
        // defined in PathFinder and called from here
        return propagationProcess.cnossosRcvSrcPropagation(src, rcv, computationProcessor, receiverMirrorIndex);
    }

    /**
     * Compute the attenuation for a given cut-profile
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in attenuationOutput for debugging purpose
     * @return List of AttenuationOutput objects
     */
    public List<AttenuationOutput> computeAttenuation(SceneWithAttenuation scene, CutProfile cutProfile,
                                                      AttenuationParameters attenuationParameters,
                                                      boolean isExportAttenuationMatrix) {
        // Attenuation computation here
        List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
        AttenuationOutput attenuationOutput = new AttenuationOutput(cutProfile); // Store propagation path
        attenuationOutput.setMeteoType(MeteoType.CUSTOM); // Store meteo type
        attenuationOutput.aGlobal = new double[]{0, 0, 0, 0, 0, 0, 0, 0};
        attenuationOutput.lineString = cutProfile.getPropagationPath();
        attenuationOutputs.add(attenuationOutput);
        //
        return attenuationOutputs;
    }

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param source source point information
     * @param receiver receiver point information
     * @param scene Geometrical information about the propagation scene
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in attenuationOutput for debugging purpose
     * @return Attenuation
     */
    public AttenuationOutput computeDirectAttenuation(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                             SceneWithAttenuation scene, AttenuationParameters attenuationParameters,
                                             boolean isExportAttenuationMatrix){
        // Direct attenuation computation here
        AttenuationOutput attenuationOutput = new AttenuationOutput();
        attenuationOutput.aGlobal = new double[0];
        //
        return attenuationOutput;
    }

}
