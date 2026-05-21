package org.noise_planet.noisemodelling.propagation.cnossos;

import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Retrieve attenuation computed according to CNOSSOS P2P propagation
 * model for a given cut profile.
 * @author Martin Glesser
 */
public class CnossosPropagationModel implements PropagationModelFactory{
    public AttenuationComputeOutput multiThreadParent;
    public boolean keepRays;
    public List<CnossosPath> pathParameters = new ArrayList<>();
    public List<ReceiverNoiseLevel> receiverAttenuationLevels = new ArrayList<>();

    public CnossosPropagationModel(AttenuationComputeOutput multiThreadParent) {
        this.multiThreadParent = multiThreadParent;
        this.keepRays = multiThreadParent.exportPaths;
    }

    /**
     * Compute the attenuation for a given cut profile
     *
     * @param cutProfile geometrical cross-section
     */
    public List<ReceiverNoiseLevel> computeAttenuation(CutProfile cutProfile) {
        final SceneWithAttenuation scene = multiThreadParent.scene;
        // Source surface reflectivity
        double gs = scene.sourceGs.getOrDefault(cutProfile.getSource().sourcePk, SceneWithAttenuation.DEFAULT_GS);
        for(CnossosPath cnossosPath : CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, gs)) {
            computeAttenuation(cnossosPath);
        }

        return receiverAttenuationLevels;
    }

    public List<CnossosPath> getPaths(){
        return pathParameters;
    }

    private void processPath(String period, AttenuationParameters AttenuationParameters, CnossosPath path) {
        double[] aGlobalMeteo = AttenuationCnossos.computeCnossosAttenuation(AttenuationParameters, path,
                multiThreadParent.scene, multiThreadParent.exportAttenuationMatrix);
        if (aGlobalMeteo != null && aGlobalMeteo.length > 0) {
            multiThreadParent.cnossosPathCount.addAndGet(1);
            if(keepRays) {
                pathParameters.add(path);
            }
            receiverAttenuationLevels.add(new ReceiverNoiseLevel(
                    new PathFinder.SourcePointInfo(path.getCutProfile().getSource()),
                    new PathFinder.ReceiverPointInfo(path.getCutProfile().getReceiver()),
                    period, aGlobalMeteo));
        }
    }

    /**
     * Process Cnossos propagation path to compute attenuation
     * @param path Propagation path result
     */
    public void computeAttenuation(CnossosPath path) {
        if(!multiThreadParent.scene.cnossosParametersPerPeriod.isEmpty()) {
            for (Map.Entry<String, AttenuationParameters> cnossosParametersEntry :
                    multiThreadParent.scene.cnossosParametersPerPeriod.entrySet()) {
                processPath(cnossosParametersEntry.getKey(), cnossosParametersEntry.getValue(), path);
            }
        } else {
            processPath("", multiThreadParent.scene.defaultCnossosParameters, path);
        }
    }

}
