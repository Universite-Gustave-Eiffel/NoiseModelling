package org.noise_planet.noisemodelling.propagation.cnossos;

import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.PropagationModel;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.util.List;

/**
 * Retrieve attenuation computed according to CNOSSOS P2P propagation
 * model for a given cut profile.
 * @author Martin Glesser
 */
public class CnossosPropagationModel implements PropagationModel {
    public CutProfile cutProfile;
    public SceneWithAttenuation scene;

    public CnossosPropagationModel(CutProfile cutProfile, SceneWithAttenuation scene) {
        this.cutProfile = cutProfile;
        this.scene = scene;
    }

    /**
     * Compute paths
     *
     * @return {List<CnossosPath>} Paths computed with the CNOSSOS method
     */
    public List<CnossosPath> computePaths(){
        double gs = scene.sourceGs.getOrDefault(cutProfile.getSource().sourcePk, SceneWithAttenuation.DEFAULT_GS);
        return CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, gs);
    }

    /**
     * Compute the attenuation
     *
     * @param path path used for the attenuation computation
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    public double[] computeAttenuation(CnossosPath path, AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix) {
        return AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, path,
                scene, isExportAttenuationMatrix);
    }
}
