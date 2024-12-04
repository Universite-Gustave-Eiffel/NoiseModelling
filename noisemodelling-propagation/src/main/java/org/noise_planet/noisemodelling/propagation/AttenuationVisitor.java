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
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumDbArray;

/**
 * ToDo descripion
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


    /**
     * Get propagation path result
     * @param sourceId Source identifier
     * @param sourceLi Source power per meter coefficient
     * @param path Propagation path result
     */
    @Override
    public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<CnossosPath> path) {
        double[] aGlobalMeteo = multiThreadParent.computeCnossosAttenuation(attenuationCnossosParameters, sourceId, sourceLi, receiverId, path);
        multiThreadParent.rayCount.addAndGet(path.size());
        if(keepRays) {
            if(multiThreadParent.inputData != null && sourceId < multiThreadParent.inputData.sourcesPk.size() &&
                    receiverId < multiThreadParent.inputData.receiversPk.size()) {
                for(CnossosPath pathParameter : path) {
                    // Copy path content in order to keep original ids for other method calls
                    //CnossosPathParameters pathParametersPk = new CnossosPathParameters(pathParameter);
                    pathParameter.setIdReceiver(multiThreadParent.inputData.receiversPk.get((int)receiverId).intValue());
                    pathParameter.setIdSource(multiThreadParent.inputData.sourcesPk.get((int)sourceId).intValue());
                    pathParameter.setSourceOrientation(pathParameter.getSourceOrientation());
                    pathParameter.setGs(pathParameter.getGs());
                    pathParameters.add(pathParameter);
                }
            } else {
                pathParameters.addAll(path);
            }
        }
        if (aGlobalMeteo != null) {
            receiverAttenuationLevels.add(new Attenuation.SourceReceiverAttenuation(receiverId, sourceId, aGlobalMeteo));
            return aGlobalMeteo;
        } else {
            return new double[0];
        }
    }

    /**
     *
     * @param receiverId
     * @param sourceId
     * @param level
     */
    protected void pushResult(long receiverId, long sourceId, double[] level) {
        multiThreadParent.receiversAttenuationLevels.add(new Attenuation.SourceReceiverAttenuation(receiverId, sourceId, level));
    }


    /**
     * No more propagation paths will be pushed for this receiver identifier
     * @param receiverId
     */
    @Override
    public void finalizeReceiver(final long receiverId) {
        if(keepRays && !pathParameters.isEmpty()) {
            multiThreadParent.pathParameters.addAll(this.pathParameters);
            multiThreadParent.propagationPathsSize.addAndGet(pathParameters.size());
            this.pathParameters.clear();
        }
        long receiverPK = receiverId;
        if(multiThreadParent.inputData != null) {
            if(receiverId < multiThreadParent.inputData.receiversPk.size()) {
                receiverPK = multiThreadParent.inputData.receiversPk.get((int)receiverId);
            }
        }
        multiThreadParent.finalizeReceiver(receiverId);
        if(multiThreadParent.receiversAttenuationLevels != null) {
            // Push merged sources into multi-thread parent
            // Merge levels for each receiver for lines sources
            Map<Long, double[]> levelsPerSourceLines = new HashMap<>();
            for (Attenuation.SourceReceiverAttenuation lvl : receiverAttenuationLevels) {
                if (!levelsPerSourceLines.containsKey(lvl.sourceId)) {
                    levelsPerSourceLines.put(lvl.sourceId, lvl.value);
                } else {
                    // merge
                    levelsPerSourceLines.put(lvl.sourceId, sumDbArray(levelsPerSourceLines.get(lvl.sourceId),
                            lvl.value));
                }
            }
            long sourcePK;
            for (Map.Entry<Long, double[]> entry : levelsPerSourceLines.entrySet()) {
                final long sourceId = entry.getKey();
                sourcePK = sourceId;
                if(multiThreadParent.inputData != null) {
                    // Retrieve original identifier
                    if(entry.getKey() < multiThreadParent.inputData.sourcesPk.size()) {
                        sourcePK = multiThreadParent.inputData.sourcesPk.get((int)sourceId);
                    }
                }
                pushResult(receiverPK, sourcePK, entry.getValue());
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