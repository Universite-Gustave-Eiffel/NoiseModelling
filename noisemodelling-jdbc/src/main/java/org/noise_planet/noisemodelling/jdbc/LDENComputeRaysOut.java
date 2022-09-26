package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

public class LDENComputeRaysOut extends ComputeRaysOutAttenuation {
    LdenData ldenData;
    LDENPropagationProcessData ldenPropagationProcessData;
    public PropagationProcessPathData dayPathData;
    public PropagationProcessPathData eveningPathData;
    public PropagationProcessPathData nightPathData;
    public LDENConfig ldenConfig;

    public LDENComputeRaysOut(PropagationProcessPathData dayPathData, PropagationProcessPathData eveningPathData,
                              PropagationProcessPathData nightPathData, LDENPropagationProcessData inputData,
                              LdenData ldenData, LDENConfig ldenConfig) {
        super(inputData.ldenConfig.exportRaysMethod != LDENConfig.ExportRaysMethods.NONE, null, inputData);
        this.keepAbsorption = inputData.ldenConfig.keepAbsorption;
        this.ldenData = ldenData;
        this.ldenPropagationProcessData = inputData;
        this.dayPathData = dayPathData;
        this.eveningPathData = eveningPathData;
        this.nightPathData = nightPathData;
        this.ldenConfig = ldenConfig;
    }

    public LdenData getLdenData() {
        return ldenData;
    }

    @Override
    public IComputeRaysOut subProcess() {
        return new ThreadComputeRaysOut(this);
    }

    public static class DENAttenuation {
        public double [] dayLevels = null;
        public double [] eveningLevels = null;
        public double [] nightLevels = null;

        public double[] getTimePeriodLevel(LDENConfig.TIME_PERIOD timePeriod) {
            switch (timePeriod) {
                case DAY:
                    return dayLevels;
                case EVENING:
                    return eveningLevels;
                default:
                    return nightLevels;
            }
        }
        public void setTimePeriodLevel(LDENConfig.TIME_PERIOD timePeriod, double [] levels) {
            switch (timePeriod) {
                case DAY:
                    dayLevels = levels;
                case EVENING:
                    eveningLevels = levels;
                default:
                    nightLevels = levels;
            }
        }
    }

    public static class ThreadComputeRaysOut implements IComputeRaysOut {
        LDENComputeRaysOut ldenComputeRaysOut;
        LDENConfig ldenConfig;
        ThreadRaysOut[] lDENThreadRaysOut = new ThreadRaysOut[3];
        public List<PropagationPath> propagationPaths = new ArrayList<PropagationPath>();

        public ThreadComputeRaysOut(LDENComputeRaysOut multiThreadParent) {
            this.ldenComputeRaysOut = multiThreadParent;
            this.ldenConfig = multiThreadParent.ldenPropagationProcessData.ldenConfig;
            lDENThreadRaysOut[0] = new ThreadRaysOut(multiThreadParent, multiThreadParent.dayPathData);
            lDENThreadRaysOut[1] = new ThreadRaysOut(multiThreadParent, multiThreadParent.eveningPathData);
            lDENThreadRaysOut[2] = new ThreadRaysOut(multiThreadParent, multiThreadParent.nightPathData);
            for (ThreadRaysOut threadRaysOut : lDENThreadRaysOut) {
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
            double[] levels = new double[ldenComputeRaysOut.dayPathData.freq_lvl.size()];
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
            ldenComputeRaysOut.rayCount.addAndGet(propagationPathsParameter.size());
            if(ldenComputeRaysOut.keepRays && !ldenComputeRaysOut.keepAbsorption) {
                for(PropagationPath propagationPath : propagationPathsParameter) {
                    // Use only one ray as the ray is the same if we not keep absorption values
                    if (ldenComputeRaysOut.inputData != null && sourceId < ldenComputeRaysOut.inputData.sourcesPk.size() && receiverId < ldenComputeRaysOut.inputData.receiversPk.size()) {
                        // Copy path content in order to keep original ids for other method calls
                        PropagationPath pathPk = new PropagationPath(propagationPath);
                        pathPk.setIdReceiver(ldenComputeRaysOut.inputData.receiversPk.get((int) receiverId).intValue());
                        pathPk.setIdSource(ldenComputeRaysOut.inputData.sourcesPk.get((int) sourceId).intValue());
                        this.propagationPaths.add(pathPk);
                    } else {
                        this.propagationPaths.add(propagationPath);
                    }
                }
            }
            double[] globalLevel = null;
            for(LDENConfig.TIME_PERIOD timePeriod : LDENConfig.TIME_PERIOD.values()) {
                for(PropagationPath propagationPath : propagationPathsParameter) {
                    if (globalLevel == null) {
                        globalLevel = lDENThreadRaysOut[timePeriod.ordinal()].addPropagationPaths(sourceId, sourceLi,
                                receiverId, Collections.singletonList(propagationPath));
                    } else {
                        globalLevel = PowerUtils.sumDbArray(globalLevel, lDENThreadRaysOut[timePeriod.ordinal()].addPropagationPaths(sourceId, sourceLi,
                                receiverId, Collections.singletonList(propagationPath)));
                    }
                    propagationPath.setTimePeriod(timePeriod.name());
                    if(ldenComputeRaysOut.keepRays && ldenComputeRaysOut.keepAbsorption) {
                        // copy ray for each time period because absorption is different for each period
                        if (ldenComputeRaysOut.inputData != null && sourceId < ldenComputeRaysOut.inputData.sourcesPk.size() && receiverId < ldenComputeRaysOut.inputData.receiversPk.size()) {
                            // Copy path content in order to keep original ids for other method calls
                            PropagationPath pathPk = new PropagationPath(propagationPath);
                            pathPk.setIdReceiver(ldenComputeRaysOut.inputData.receiversPk.get((int) receiverId).intValue());
                            pathPk.setIdSource(ldenComputeRaysOut.inputData.sourcesPk.get((int) sourceId).intValue());
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
            while(ldenComputeRaysOut.ldenData.queueSize.get() > ldenConfig.outputMaximumQueue) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    ldenConfig.aborted = true;
                    break;
                }
                if(ldenConfig.aborted) {
                    if(ldenComputeRaysOut != null && this.ldenComputeRaysOut.inputData != null &&
                            this.ldenComputeRaysOut.inputData.cellProg != null) {
                        this.ldenComputeRaysOut.inputData.cellProg.cancel();
                    }
                    return;
                }
            }
            stack.add(data);
            ldenComputeRaysOut.ldenData.queueSize.incrementAndGet();
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
            while(ldenComputeRaysOut.ldenData.queueSize.get() > ldenConfig.outputMaximumQueue) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    ldenConfig.aborted = true;
                    break;
                }
                if(ldenConfig.aborted) {
                    if(ldenComputeRaysOut != null && this.ldenComputeRaysOut.inputData != null &&
                            this.ldenComputeRaysOut.inputData.cellProg != null) {
                        this.ldenComputeRaysOut.inputData.cellProg.cancel();
                    }
                    return;
                }
            }
            if(ldenConfig.getMaximumRaysOutputCount() == 0 || ldenComputeRaysOut.ldenData.totalRaysInserted.get() < ldenConfig.getMaximumRaysOutputCount()) {
                long newTotalRays = ldenComputeRaysOut.ldenData.totalRaysInserted.addAndGet(data.size());
                if(ldenConfig.getMaximumRaysOutputCount() > 0 && newTotalRays > ldenConfig.getMaximumRaysOutputCount()) {
                    // too many rays, remove unwanted rays
                    int newListSize = data.size() - (int)(newTotalRays - ldenConfig.getMaximumRaysOutputCount());
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
                ldenComputeRaysOut.ldenData.queueSize.addAndGet(data.size());
            }
        }

        @Override
        public void finalizeReceiver(final long receiverId) {
            if(!propagationPaths.isEmpty()) {
                if(ldenConfig.getExportRaysMethod() == LDENConfig.ExportRaysMethods.TO_RAYS_TABLE) {
                    // Push propagation rays
                    pushInStack(ldenComputeRaysOut.ldenData.rays, propagationPaths);
                } else if(ldenConfig.getExportRaysMethod() == LDENConfig.ExportRaysMethods.TO_MEMORY
                && (ldenConfig.getMaximumRaysOutputCount() == 0 ||
                        ldenComputeRaysOut.propagationPathsSize.get() < ldenConfig.getMaximumRaysOutputCount())){
                    int newRaysSize = ldenComputeRaysOut.propagationPathsSize.addAndGet(propagationPaths.size());
                    if(newRaysSize > ldenConfig.getMaximumRaysOutputCount()) {
                        // remove exceeded elements of the array
                        propagationPaths = propagationPaths.subList(0,
                                propagationPaths.size() - Math.min( propagationPaths.size(),
                                        newRaysSize - ldenConfig.getMaximumRaysOutputCount()));
                    }
                    ldenComputeRaysOut.propagationPaths.addAll(propagationPaths);
                }
                propagationPaths.clear();
            }
            long receiverPK = receiverId;
            if(ldenComputeRaysOut.inputData != null) {
                if(receiverId < ldenComputeRaysOut.inputData.receiversPk.size()) {
                    receiverPK = ldenComputeRaysOut.inputData.receiversPk.get((int)receiverId);
                }
            }
            double[] dayLevels = new double[0], eveningLevels = new double[0], nightLevels = new double[0];
            if (!ldenConfig.mergeSources) {
                // Aggregate by source id
                Map<Long, DENAttenuation> levelsPerSourceLines = new HashMap<>();
                for (LDENConfig.TIME_PERIOD timePeriod : LDENConfig.TIME_PERIOD.values()) {
                    ThreadRaysOut threadRaysOut = lDENThreadRaysOut[timePeriod.ordinal()];
                    for (VerticeSL lvl : threadRaysOut.receiverAttenuationLevels) {
                        DENAttenuation denAttenuation;
                        if (!levelsPerSourceLines.containsKey(lvl.sourceId)) {
                            denAttenuation = new DENAttenuation();
                            levelsPerSourceLines.put(lvl.sourceId, denAttenuation);
                        } else {
                            denAttenuation = levelsPerSourceLines.get(lvl.sourceId);
                        }
                        if (denAttenuation.getTimePeriodLevel(timePeriod) == null) {
                            denAttenuation.setTimePeriodLevel(timePeriod, lvl.value);
                        } else {
                            // same receiver, same source already exists, merge attenuation
                            denAttenuation.setTimePeriodLevel(timePeriod, sumDbArray(
                                    denAttenuation.getTimePeriodLevel(timePeriod), lvl.value));
                        }
                    }
                }
                long sourcePK;
                for (Map.Entry<Long, DENAttenuation> entry : levelsPerSourceLines.entrySet()) {
                    final long sourceId = entry.getKey();
                    sourcePK = sourceId;
                    if (ldenComputeRaysOut.inputData != null) {
                        // Retrieve original source identifier
                        if (entry.getKey() < ldenComputeRaysOut.inputData.sourcesPk.size()) {
                            sourcePK = ldenComputeRaysOut.inputData.sourcesPk.get((int) sourceId);
                        }
                    }
                    if (ldenConfig.computeLDay || ldenConfig.computeLDEN) {
                        dayLevels = sumArray(wToDba(ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesD.get((int) sourceId)), entry.getValue().dayLevels);
                        if(ldenConfig.computeLDay) {
                            pushInStack(ldenComputeRaysOut.ldenData.lDayLevels, new VerticeSL(receiverPK, sourcePK, dayLevels));
                        }
                    }
                    if (ldenConfig.computeLEvening || ldenConfig.computeLDEN) {
                        eveningLevels = sumArray(wToDba(ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesE.get((int) sourceId)), entry.getValue().eveningLevels);
                        if(ldenConfig.computeLEvening) {
                            pushInStack(ldenComputeRaysOut.ldenData.lEveningLevels, new VerticeSL(receiverPK, sourcePK, eveningLevels));
                        }
                    }
                    if (ldenConfig.computeLNight || ldenConfig.computeLDEN) {
                        nightLevels = sumArray(wToDba(ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesN.get((int) sourceId)), entry.getValue().nightLevels);
                        if(ldenConfig.computeLNight) {
                            pushInStack(ldenComputeRaysOut.ldenData.lNightLevels, new VerticeSL(receiverPK, sourcePK, nightLevels));
                        }
                    }
                    if (ldenConfig.computeLDEN) {
                        double[] levels = new double[dayLevels.length];
                        for(int idFrequency = 0; idFrequency < levels.length; idFrequency++) {
                            levels[idFrequency] = (12 * dayLevels[idFrequency] +
                                    4 * dbaToW(wToDba(eveningLevels[idFrequency]) + 5) +
                                    8 * dbaToW(wToDba(nightLevels[idFrequency]) + 10)) / 24.0;
                        }
                        pushInStack(ldenComputeRaysOut.ldenData.lDenLevels, new VerticeSL(receiverPK, sourcePK, levels));
                    }
                }
            } else {
                // Merge all results
                if (ldenConfig.computeLDay || ldenConfig.computeLDEN) {
                    dayLevels = processAndPushResult(receiverPK,
                            ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesD,
                            lDENThreadRaysOut[0].receiverAttenuationLevels, ldenComputeRaysOut.ldenData.lDayLevels,
                            ldenConfig.computeLDay);
                }
                if (ldenConfig.computeLEvening || ldenConfig.computeLDEN) {
                    eveningLevels = processAndPushResult(receiverPK,
                            ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesE,
                            lDENThreadRaysOut[1].receiverAttenuationLevels, ldenComputeRaysOut.ldenData.lEveningLevels,
                            ldenConfig.computeLEvening);
                }
                if (ldenConfig.computeLNight || ldenConfig.computeLDEN) {
                    nightLevels = processAndPushResult(receiverPK,
                            ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesN,
                            lDENThreadRaysOut[2].receiverAttenuationLevels, ldenComputeRaysOut.ldenData.lNightLevels,
                            ldenConfig.computeLNight);
                }
                if (ldenConfig.computeLDEN) {
                    double[] levels = new double[dayLevels.length];
                    for(int idFrequency = 0; idFrequency < levels.length; idFrequency++) {
                        levels[idFrequency] = (12 * dayLevels[idFrequency] +
                                4 * dbaToW(wToDba(eveningLevels[idFrequency]) + 5) +
                                8 * dbaToW(wToDba(nightLevels[idFrequency]) + 10)) / 24.0;
                    }
                    pushInStack(ldenComputeRaysOut.ldenData.lDenLevels, new VerticeSL(receiverPK, -1, wToDba(levels)));
                }
            }
            for (ThreadRaysOut threadRaysOut : lDENThreadRaysOut) {
                threadRaysOut.receiverAttenuationLevels.clear();
            }
        }
    }

    public static class LdenData {
        public final AtomicLong queueSize = new AtomicLong(0);
        public final AtomicLong totalRaysInserted = new AtomicLong(0);
        public final ConcurrentLinkedDeque<VerticeSL> lDayLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lEveningLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lNightLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lDenLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<PropagationPath> rays = new ConcurrentLinkedDeque<>();
    }
}
