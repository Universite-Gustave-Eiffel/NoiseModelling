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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.util.List;

/**
 * Interface for point to point propagation models.
 * @author Martin Glesser
 */
public interface PropagationModel {
    /**
     * Compute the attenuation for a given path
     *
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {List<double[]>} Attenuation
     */
    List<double[]> computeAttenuation(AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix);

    /**
     * Compute the paths for a given geometrical cross-section / cut profile
     * @return {List<CnossosPath>} Paths
     */
    List<CnossosPath> computePaths();

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    double[] computeDirectAttenuation(AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix);

    /**
     * Getter for scene attribute
     *
     * @return {SceneWithAttenuation} Global geometrical information
     */
    SceneWithAttenuation getScene();

    /**
     * Setter for scene attribute
     *
     * @param scene Global geometrical information
     */
    void setScene(SceneWithAttenuation scene);

    /**
     * Setter for cutProfile attribute
     *
     * @param cutProfile Geometrical cut profile
     */
    void setCutProfile(CutProfile cutProfile);

    /**
     * Setter for cutProfile attribute
     *
     * @param source source point information
     * @param receiver receiver point information
     */
    void setCutProfile(PathFinder.SourcePointInfo source,
                              PathFinder.ReceiverPointInfo receiver);

}
