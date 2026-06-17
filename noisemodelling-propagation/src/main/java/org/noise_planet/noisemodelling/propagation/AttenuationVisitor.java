/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receive vertical cut plane, compute the attenuation corresponding to this plane
 */
public class AttenuationVisitor implements CutPlaneVisitor {
    public AttenuationComputeOutput multiThreadParent;
    public List<ReceiverNoiseLevel> receiverAttenuationLevels = new ArrayList<>();
    public List<CnossosPath> pathParameters = new ArrayList<>();
    public boolean keepRays;

    /**
     * Constructor for AttenuationVisitor object
     *
     * @param multiThreadParent Multithread data class
     */
    public AttenuationVisitor(AttenuationComputeOutput multiThreadParent) {
        this.multiThreadParent = multiThreadParent;
        this.keepRays = multiThreadParent.exportPaths;
    }

    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        multiThreadParent.cutProfileCount.addAndGet(1);
        final SceneWithAttenuation scene = multiThreadParent.scene;
        if(scene.getCloseReceiverReflectionWallDistance() > 0
                && cutProfile.hasCloseReflectionBeforeReceiver(scene.getCloseReceiverReflectionWallDistance())) {
            return PathSearchStrategy.CONTINUE;
        }
        // Push attenuation for each period
        if(!multiThreadParent.scene.cnossosParametersPerPeriod.isEmpty()) {
            for (Map.Entry<String, AttenuationParameters> cnossosParametersEntry :
                    multiThreadParent.scene.cnossosParametersPerPeriod.entrySet()) {
                processAndStoreAttenuation(scene, cutProfile, cnossosParametersEntry.getKey(),
                        cnossosParametersEntry.getValue());
            }
        } else {
            processAndStoreAttenuation(scene, cutProfile, "",
                    multiThreadParent.scene.defaultCnossosParameters);
        }

        return PathSearchStrategy.CONTINUE;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList,
                              AtomicInteger cutProfileCount) {

    }

    /**
     * Compute and store attenuation
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param period Period identifier
     * @param AttenuationParameters parameters of the propagation computation
     */
    private void processAndStoreAttenuation(SceneWithAttenuation scene, CutProfile cutProfile,
                                            String period, AttenuationParameters AttenuationParameters) {
        PropagationModel propagationModel = multiThreadParent.propagationModel;
        List<CnossosPath> paths = propagationModel.computePaths(scene, cutProfile);
        List<double[]> attenuationList = propagationModel.computeAttenuation(scene, cutProfile, paths,
                AttenuationParameters,multiThreadParent.exportAttenuationMatrix);
        for (double[] aGlobalMeteo : attenuationList) {
            if (aGlobalMeteo != null && aGlobalMeteo.length > 0) {
                receiverAttenuationLevels.add(new ReceiverNoiseLevel(
                        new PathFinder.SourcePointInfo(cutProfile.getSource()),
                        new PathFinder.ReceiverPointInfo(cutProfile.getReceiver()),
                        period, aGlobalMeteo));
            }
        }
        if(keepRays) {
            pathParameters.addAll(paths);
        }
    }

    /**
     * No more propagation paths will be pushed for this receiver identifier
     *
     * @param receiver receiver point information
     */
    @Override
    public void finalizeReceiver(PathFinder.ReceiverPointInfo receiver) {
        if(keepRays && !pathParameters.isEmpty()) {
            multiThreadParent.pathParameters.addAll(this.pathParameters);
            multiThreadParent.propagationPathsSize.addAndGet(pathParameters.size());
            this.pathParameters.clear();
        }
        if(multiThreadParent.receiversAttenuationLevels != null) {
            // Push merged sources into multi-thread parent
            // Merge levels for each receiver for lines sources
            Map<PathFinder.SourcePointInfo, double[]> levelsPerSourceLines = new HashMap<>();
            for (ReceiverNoiseLevel lvl : receiverAttenuationLevels) {
                if (!levelsPerSourceLines.containsKey(lvl.source)) {
                    levelsPerSourceLines.put(lvl.source, lvl.levels);
                } else {
                    // merge
                    levelsPerSourceLines.put(lvl.source,
                            AcousticIndicatorsFunctions.sumDbArray(levelsPerSourceLines.get(lvl.source),
                            lvl.levels));
                }
            }
            for (Map.Entry<PathFinder.SourcePointInfo, double[]> entry : levelsPerSourceLines.entrySet()) {
                multiThreadParent.receiversAttenuationLevels.add(
                        new ReceiverNoiseLevel(entry.getKey(), receiver , "", entry.getValue()));
            }
        }
        receiverAttenuationLevels.clear();
    }

}
