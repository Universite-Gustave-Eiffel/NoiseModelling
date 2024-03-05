package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

public class LTComputeRaysOut extends ComputeRaysOutAttenuation {
    LtData ltData;
    LTPropagationProcessData ltPropagationProcessData;
    public PropagationProcessPathData PathData;
    public LTConfig ltConfig;

    public LTComputeRaysOut(PropagationProcessPathData PathData, LTPropagationProcessData inputData,
                            LtData ltData, LTConfig ltConfig) {
        super(inputData.ltConfig.exportRaysMethod != LTConfig.ExportRaysMethods.NONE, null, inputData);
        this.keepAbsorption = inputData.ltConfig.keepAbsorption;
        this.ltData = ltData;
        this.ltPropagationProcessData = inputData;
        this.PathData = PathData;
        this.ltConfig = ltConfig;
    }

    public LtData getLtData() {
        return ltData;
    }

    @Override
    public IComputeRaysOut subProcess() {
        return new ThreadComputeRaysOut(this);
    }

    public static class Attenuation {
        public HashMap<String, double []> Levels = null;
        public double[] getTimePeriodLevel(String timestep) {
            return Levels.get(timestep);
        }
        public void setTimePeriodLevel(String timestep, double [] levels) {
            Levels.put(timestep, levels);
        }
    }

    public static class ThreadComputeRaysOut implements IComputeRaysOut {
        LTComputeRaysOut ltComputeRaysOut;
        LTConfig ltConfig;

        ThreadRaysOut[] lTThreadRaysOut = new ThreadRaysOut[1];
        public List<PropagationPath> propagationPaths = new ArrayList<PropagationPath>();

        public ThreadComputeRaysOut(LTComputeRaysOut multiThreadParent) {
            this.ltComputeRaysOut = multiThreadParent;
            this.ltConfig = multiThreadParent.ltPropagationProcessData.ltConfig;
            lTThreadRaysOut[0] = new ThreadRaysOut(multiThreadParent, multiThreadParent.PathData);
            for (ThreadRaysOut threadRaysOut : lTThreadRaysOut) {
                threadRaysOut.keepRays = false;
            }

        }


        /**
         * Energetic sum of VerticeSL attenuation with WJ sources
         * @param wjSources
         * @param receiverAttenuationLevels
         * @return
         */
        double[] sumLevels(List<double[]> wjSources,List<VerticeSL> receiverAttenuationLevels) {
            double[] levels = new double[ltComputeRaysOut.PathData.freq_lvl.size()];
            for (VerticeSL lvl : receiverAttenuationLevels) {
                levels = sumArray(levels,
                        dbaToW(sumArray(wToDba(wjSources.get((int) lvl.sourceId)), lvl.value)));
            }
            return levels;
        }

        double[] processAndPushResult(long receiverPK, List<double[]> wjSources,List<VerticeSL> receiverAttenuationLevels, ConcurrentLinkedDeque<VerticeSL> result, boolean feedStack) {
            double[] levels = sumLevels(wjSources, receiverAttenuationLevels);
            if(feedStack) {
                pushInStack(result, new VerticeSL(receiverPK, -1, wToDba(levels)));
            }
            return levels;
        }


        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPathsParameter) {
            ltComputeRaysOut.rayCount.addAndGet(propagationPathsParameter.size());
            if(ltComputeRaysOut.keepRays && !ltComputeRaysOut.keepAbsorption) {
                for(PropagationPath propagationPath : propagationPathsParameter) {
                    // Use only one ray as the ray is the same if we not keep absorption values
                    if (ltComputeRaysOut.inputData != null && sourceId < ltComputeRaysOut.inputData.sourcesPk.size() && receiverId < ltComputeRaysOut.inputData.receiversPk.size()) {
                        // Copy path content in order to keep original ids for other method calls
                        PropagationPath pathPk = new PropagationPath(propagationPath);
                        pathPk.setIdReceiver(ltComputeRaysOut.inputData.receiversPk.get((int) receiverId).intValue());
                        pathPk.setIdSource(ltComputeRaysOut.inputData.sourcesPk.get((int) sourceId).intValue());
                        this.propagationPaths.add(pathPk);
                    } else {
                        this.propagationPaths.add(propagationPath);
                    }
                }
            }
            double[] globalLevel = null;
            for(String timestep : ltConfig.timesteps) {
                for(PropagationPath propagationPath : propagationPathsParameter) {
                    if (globalLevel == null) {


                        globalLevel = lTThreadRaysOut[0].addPropagationPaths(sourceId, sourceLi,
                                receiverId, Collections.singletonList(propagationPath));
                    } else {
                        globalLevel = PowerUtils.sumDbArray(globalLevel, lTThreadRaysOut[0].addPropagationPaths(sourceId, sourceLi,
                                receiverId, Collections.singletonList(propagationPath)));
                    }
                    propagationPath.setTimePeriod(timestep);
                    if(ltComputeRaysOut.keepRays && ltComputeRaysOut.keepAbsorption) {
                        // copy ray for each time period because absorption is different for each period
                        if (ltComputeRaysOut.inputData != null && sourceId < ltComputeRaysOut.inputData.sourcesPk.size() && receiverId < ltComputeRaysOut.inputData.receiversPk.size()) {
                            // Copy path content in order to keep original ids for other method calls
                            PropagationPath pathPk = new PropagationPath(propagationPath);
                            pathPk.setIdReceiver(ltComputeRaysOut.inputData.receiversPk.get((int) receiverId).intValue());
                            pathPk.setIdSource(ltComputeRaysOut.inputData.sourcesPk.get((int) sourceId).intValue());
                            this.propagationPaths.add(pathPk);
                        } else {
                            this.propagationPaths.add(propagationPath);
                        }
                    }
                }
            }
            return globalLevel;
        }

        /**
         * @param stack Stack to feed
         * @param data receiver noise level in dB
         */
        public void pushInStack(ConcurrentLinkedDeque<VerticeSL> stack, VerticeSL data) {
            while(ltComputeRaysOut.ltData.queueSize.get() > ltConfig.outputMaximumQueue) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    ltConfig.aborted = true;
                    break;
                }
                if(ltConfig.aborted) {
                    if(ltComputeRaysOut != null && this.ltComputeRaysOut.inputData != null &&
                            this.ltComputeRaysOut.inputData.cellProg != null) {
                        this.ltComputeRaysOut.inputData.cellProg.cancel();
                    }
                    return;
                }
            }
            stack.add(data);
            ltComputeRaysOut.ltData.queueSize.incrementAndGet();
        }

        @Override
        public IComputeRaysOut subProcess() {
            return null;
        }

        /**
         * @param stack Stack to feed
         * @param data rays
         */
        public void pushInStack(ConcurrentLinkedDeque<PropagationPath> stack, Collection<PropagationPath> data) {
            while(ltComputeRaysOut.ltData.queueSize.get() > ltConfig.outputMaximumQueue) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    ltConfig.aborted = true;
                    break;
                }
                if(ltConfig.aborted) {
                    if(ltComputeRaysOut != null && this.ltComputeRaysOut.inputData != null &&
                            this.ltComputeRaysOut.inputData.cellProg != null) {
                        this.ltComputeRaysOut.inputData.cellProg.cancel();
                    }
                    return;
                }
            }
            if(ltConfig.getMaximumRaysOutputCount() == 0 || ltComputeRaysOut.ltData.totalRaysInserted.get() < ltConfig.getMaximumRaysOutputCount()) {
                long newTotalRays = ltComputeRaysOut.ltData.totalRaysInserted.addAndGet(data.size());
                if(ltConfig.getMaximumRaysOutputCount() > 0 && newTotalRays > ltConfig.getMaximumRaysOutputCount()) {
                    // too many rays, remove unwanted rays
                    int newListSize = data.size() - (int)(newTotalRays - ltConfig.getMaximumRaysOutputCount());
                    List<PropagationPath> subList = new ArrayList<PropagationPath>(newListSize);
                    for(PropagationPath propagationPath : data) {
                        subList.add(propagationPath);
                        if(subList.size() >= newListSize) {
                            break;
                        }
                    }
                    data = subList;
                }
                stack.addAll(data);
                ltComputeRaysOut.ltData.queueSize.addAndGet(data.size());
            }
        }

        @Override
        public void finalizeReceiver(final long receiverId) {
            if(!propagationPaths.isEmpty()) {
                if(ltConfig.getExportRaysMethod() == LTConfig.ExportRaysMethods.TO_RAYS_TABLE) {
                    // Push propagation rays
                    pushInStack(ltComputeRaysOut.ltData.rays, propagationPaths);
                } else if(ltConfig.getExportRaysMethod() == LTConfig.ExportRaysMethods.TO_MEMORY
                && (ltConfig.getMaximumRaysOutputCount() == 0 ||
                        ltComputeRaysOut.propagationPathsSize.get() < ltConfig.getMaximumRaysOutputCount())){
                    int newRaysSize = ltComputeRaysOut.propagationPathsSize.addAndGet(propagationPaths.size());
                    if(ltConfig.getMaximumRaysOutputCount() > 0 && newRaysSize > ltConfig.getMaximumRaysOutputCount()) {
                        // remove exceeded elements of the array
                        propagationPaths = propagationPaths.subList(0,
                                propagationPaths.size() - Math.min( propagationPaths.size(),
                                        newRaysSize - ltConfig.getMaximumRaysOutputCount()));
                    }
                    ltComputeRaysOut.propagationPaths.addAll(propagationPaths);
                }
                propagationPaths.clear();
            }
            long receiverPK = receiverId;
            if(ltComputeRaysOut.inputData != null) {
                if(receiverId < ltComputeRaysOut.inputData.receiversPk.size()) {
                    receiverPK = ltComputeRaysOut.inputData.receiversPk.get((int)receiverId);
                }
            }
            double[] tLevels = new double[0];
            if (!ltConfig.mergeSources) {
                // Aggregate by source id
                Map<Long, Attenuation> levelsPerSourceLines = new HashMap<>();
                for (String timestep : ltConfig.timesteps) {
                    ThreadRaysOut threadRaysOut = lTThreadRaysOut[0];
                    for (VerticeSL lvl : threadRaysOut.receiverAttenuationLevels) {
                        Attenuation attenuation;
                        if (!levelsPerSourceLines.containsKey(lvl.sourceId)) {
                            attenuation = new Attenuation();
                            levelsPerSourceLines.put(lvl.sourceId, attenuation);
                        } else {
                            attenuation = levelsPerSourceLines.get(lvl.sourceId);
                        }
                        if (attenuation.getTimePeriodLevel(timestep) == null) {
                            attenuation.setTimePeriodLevel(timestep, lvl.value);
                        } else {
                            // same receiver, same source already exists, merge attenuation
                            attenuation.setTimePeriodLevel(timestep, sumDbArray(
                                    attenuation.getTimePeriodLevel(timestep), lvl.value));
                        }
                    }
                }
                long sourcePK;
                for (Map.Entry<Long, Attenuation> entry : levelsPerSourceLines.entrySet()) {
                    final long sourceId = entry.getKey();
                    sourcePK = sourceId;
                    if (ltComputeRaysOut.inputData != null) {
                        // Retrieve original source identifier
                        if (entry.getKey() < ltComputeRaysOut.inputData.sourcesPk.size()) {
                            sourcePK = ltComputeRaysOut.inputData.sourcesPk.get((int) sourceId);
                        }
                    }

                    for (String timestep : ltConfig.timesteps) {
                        List<double[]> srcLvls = ltComputeRaysOut.ltPropagationProcessData.wjSourcesT.get(timestep);
                       // tLevels = sumArray(wToDba(srcLvls.get(int sourceId)), entry.getValue().Levels);
                    }
                    pushInStack(ltComputeRaysOut.ltData.lTLevels, new VerticeSL(receiverPK, sourcePK, tLevels));





                    double[] levels = new double[tLevels.length];
                    for(int idFrequency = 0; idFrequency < levels.length; idFrequency++) {
                        levels[idFrequency] = (12 * tLevels[idFrequency] +
                                4 * dbaToW(wToDba(tLevels[idFrequency]) + 5) +
                                8 * dbaToW(wToDba(tLevels[idFrequency]) + 10)) / 24.0;
                    }
                    pushInStack(ltComputeRaysOut.ltData.lTLevels, new VerticeSL(receiverPK, sourcePK, levels));

                }
            } else {
                // Merge all results

               /* tLevels = processAndPushResult(receiverPK,
                        ltComputeRaysOut.ltPropagationProcessData.wjSources,
                        lTThreadRaysOut[0].receiverAttenuationLevels, ltComputeRaysOut.ltData.lTLevels);


*/
                double[] levels = new double[tLevels.length];
                for(int idFrequency = 0; idFrequency < levels.length; idFrequency++) {
                    levels[idFrequency] = (12 * tLevels[idFrequency] +
                            4 * dbaToW(wToDba(tLevels[idFrequency]) + 5) +
                            8 * dbaToW(wToDba(tLevels[idFrequency]) + 10)) / 24.0;
                }
                pushInStack(ltComputeRaysOut.ltData.lTLevels, new VerticeSL(receiverPK, -1, wToDba(levels)));

            }
            for (ThreadRaysOut threadRaysOut : lTThreadRaysOut) {
                threadRaysOut.receiverAttenuationLevels.clear();
            }
        }
    }

    public static class LtData {
        public final AtomicLong queueSize = new AtomicLong(0);
        public final AtomicLong totalRaysInserted = new AtomicLong(0);
        public final ConcurrentLinkedDeque<VerticeSL> lTLevels = new ConcurrentLinkedDeque<>();

        public final ConcurrentLinkedDeque<PropagationPath> rays = new ConcurrentLinkedDeque<>();
    }
}
