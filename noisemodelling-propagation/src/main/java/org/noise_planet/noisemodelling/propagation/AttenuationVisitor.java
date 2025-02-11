/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receive vertical cut plane, compute the attenuation corresponding to this plane
 */
public class AttenuationVisitor implements IComputePathsOut {
    public Attenuation multiThreadParent;
    public List<Attenuation.SourceReceiverAttenuation> receiverAttenuationLevels = new ArrayList<>();
    public List<CnossosPath> pathParameters = new ArrayList<CnossosPath>();
    public AttenuationCnossosParameters attenuationCnossosParameters;
    public boolean keepRays = false;

    public AttenuationVisitor(Attenuation multiThreadParent, AttenuationCnossosParameters attenuationCnossosParameters) {
        this.multiThreadParent = multiThreadParent;
        this.keepRays = multiThreadParent.exportPaths;
        this.attenuationCnossosParameters = attenuationCnossosParameters;
    }

    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        final Scene scene = multiThreadParent.inputData;
        CnossosPath cnossosPath = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.freq_lvl, scene.gS);
        if(cnossosPath != null) {
            addPropagationPaths(cutProfile.getSource(), cutProfile.getReceiver(), Collections.singletonList(cnossosPath));
        }
        return PathSearchStrategy.CONTINUE;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList, AtomicInteger cutProfileCount) {

    }

    /**
     * Get propagation path result
     * @param source Source identifier
     * @param receiver Receiver identifier
     * @param path Propagation path result
     */
    public double[] addPropagationPaths(CutPointSource source, CutPointReceiver receiver, List<CnossosPath> path) {
        double[] aGlobalMeteo = multiThreadParent.computeCnossosAttenuation(attenuationCnossosParameters, source.id, source.li, path);
        multiThreadParent.rayCount.addAndGet(path.size());
        if(keepRays) {
            pathParameters.addAll(path);
        }
        if (aGlobalMeteo != null) {
            receiverAttenuationLevels.add(new Attenuation.SourceReceiverAttenuation(new PathFinder.ReceiverPointInfo(receiver),
                    new PathFinder.SourcePointInfo(source), aGlobalMeteo));
            return aGlobalMeteo;
        } else {
            return new double[0];
        }
    }

    /**
     * No more propagation paths will be pushed for this receiver identifier
     *
     * @param receiver
     */
    @Override
    public void finalizeReceiver(PathFinder.ReceiverPointInfo receiver) {
        if(keepRays && !pathParameters.isEmpty()) {
            multiThreadParent.pathParameters.addAll(this.pathParameters);
            multiThreadParent.propagationPathsSize.addAndGet(pathParameters.size());
            this.pathParameters.clear();
        }
        multiThreadParent.finalizeReceiver(receiver);
        if(multiThreadParent.receiversAttenuationLevels != null) {
            // Push merged sources into multi-thread parent
            // Merge levels for each receiver for lines sources
            Map<PathFinder.SourcePointInfo, double[]> levelsPerSourceLines = new HashMap<>();
            for (Attenuation.SourceReceiverAttenuation lvl : receiverAttenuationLevels) {
                if (!levelsPerSourceLines.containsKey(lvl.source)) {
                    levelsPerSourceLines.put(lvl.source, lvl.value);
                } else {
                    // merge
                    levelsPerSourceLines.put(lvl.source,
                            AcousticIndicatorsFunctions.sumDbArray(levelsPerSourceLines.get(lvl.source),
                            lvl.value));
                }
            }
            for (Map.Entry<PathFinder.SourcePointInfo, double[]> entry : levelsPerSourceLines.entrySet()) {
                multiThreadParent.receiversAttenuationLevels.add(
                        new Attenuation.SourceReceiverAttenuation(receiver, entry.getKey(), entry.getValue()));
            }
        }
        receiverAttenuationLevels.clear();
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess() {
        return multiThreadParent.subProcess();
    }
}