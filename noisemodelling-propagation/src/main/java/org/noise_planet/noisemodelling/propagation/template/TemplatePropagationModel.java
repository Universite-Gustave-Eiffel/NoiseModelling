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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.PropagationModel;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Template of propagation model. To be used as a basis
 * for new P2P model implementation.
 * @author Martin Glesser
 */
public class TemplatePropagationModel implements PropagationModel {

    /**
     * Constructor for TemplatePropagationModel objects
     */
    public TemplatePropagationModel(){}

    /**
     * Compute the attenuation for a given cut-profile
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param paths List of propagation paths (Cnossos specific)
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return Attenuation for the homogeneous and favourable path
     */
    public List<double[]> computeAttenuation(SceneWithAttenuation scene, CutProfile cutProfile, List<CnossosPath> paths,
                                             AttenuationParameters attenuationParameters,
                                             boolean isExportAttenuationMatrix) {
        // Attenuation computation here
        List<double[]> attenuation = new ArrayList<>();
        attenuation.add(new double[]{0});
        attenuation.add(new double[]{0});
        //
        return attenuation;
    }

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param source source point information
     * @param receiver receiver point information
     * @param scene Geometrical information about the propagation scene
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return Attenuation
     */
    public double[] computeDirectAttenuation(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                             SceneWithAttenuation scene, AttenuationParameters attenuationParameters,
                                             boolean isExportAttenuationMatrix){
        // Direct attenuation computation here
        double[] attenuation = new double[]{0};
        //
        return attenuation;
    }

    /**
     * Compute the propagation paths for a given geometrical cross-section / cut profile
     * (Specific to Cnossos propagation model, will be removed after pathFinder module
     * refacto)
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @return List of Cnossos propagation paths
     */
    public List<CnossosPath> computePaths(SceneWithAttenuation scene, CutProfile cutProfile){
        return new ArrayList<>();
    }

}
