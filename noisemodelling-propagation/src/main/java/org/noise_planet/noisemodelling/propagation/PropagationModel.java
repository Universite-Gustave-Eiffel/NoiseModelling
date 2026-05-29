/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.util.List;

/**
 * Interface for point to point propagation models. Retrieve paths and
 * attenuation for a given geometrical cross-section / cut profile and scene
 * @author Martin Glesser
 */
public interface PropagationModel {
    public CutProfile cutProfile = new CutProfile();
    public SceneWithAttenuation scene = new SceneWithAttenuation();


    /**
     * Compute the attenuation
     *
     * @param path path used for the attenuation computation
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    double[] computeAttenuation(CnossosPath path, AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix);

    /**
     * Compute the paths
     *
     * @return {List<CnossosPath>} Paths
     */
    List<CnossosPath> computePaths();
}
