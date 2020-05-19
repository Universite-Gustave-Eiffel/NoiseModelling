package org.noise_planet.noisemodelling.emission.jdbc;

import org.noise_planet.noisemodelling.propagation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntToDoubleFunction;

public class LDENComputeRaysOut extends ComputeRaysOut {
    LdenData ldenData;
    LDENPropagationProcessData ldenPropagationProcessData;

    public LDENComputeRaysOut(boolean keepRays, PropagationProcessPathData pathData, LDENPropagationProcessData inputData, LdenData ldenData) {
        super(keepRays, pathData, inputData);
        this.ldenData = ldenData;
        this.ldenPropagationProcessData = inputData;
    }

    @Override
    public IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
        return new ThreadComputeRaysOut(this);
    }

    static class ThreadComputeRaysOut extends ComputeRaysOut.ThreadRaysOut {
        LDENComputeRaysOut ldenComputeRaysOut;
        LDENConfig ldenConfig;
        public ThreadComputeRaysOut(LDENComputeRaysOut multiThreadParent) {
            super(multiThreadParent);
            this.ldenComputeRaysOut = multiThreadParent;
            this.ldenConfig = multiThreadParent.ldenPropagationProcessData.ldenConfig;
        }

        void processAndPushResult(long receiverPK, List<double[]> wjSources, ConcurrentLinkedDeque<VerticeSL> result) {
            double[] levels = new double[PropagationProcessPathData.freq_lvl.size()];
            for (VerticeSL lvl : receiverAttenuationLevels) {
                levels = ComputeRays.sumArray(levels,
                        ComputeRays.dbaToW(ComputeRays.sumArray(ComputeRays.wToDba(wjSources.get((int) lvl.sourceId)), lvl.value)));
            }
            pushInStack(result, new VerticeSL(receiverPK, -1, ComputeRays.wToDba(levels)));
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
                    if(multiThreadParent != null && this.multiThreadParent.inputData != null &&
                            this.multiThreadParent.inputData.cellProg != null) {
                        this.multiThreadParent.inputData.cellProg.cancel();
                    }
                    return;
                }
            }
            stack.add(data);
            ldenComputeRaysOut.ldenData.queueSize.incrementAndGet();
        }

        @Override
        public void finalizeReceiver(final long receiverId) {
            long receiverPK = receiverId;
            if(ldenComputeRaysOut.inputData != null) {
                if(receiverId < ldenComputeRaysOut.inputData.receiversPk.size()) {
                    receiverPK = ldenComputeRaysOut.inputData.receiversPk.get((int)receiverId);
                }
            }
            if(!ldenConfig.mergeSources) {
                // Aggregate by source id
                Map<Long, double[]> levelsPerSourceLines = new HashMap<>();
                for (VerticeSL lvl : receiverAttenuationLevels) {
                    if (!levelsPerSourceLines.containsKey(lvl.sourceId)) {
                        levelsPerSourceLines.put(lvl.sourceId, lvl.value);
                    } else {
                        // merge
                        levelsPerSourceLines.put(lvl.sourceId, ComputeRays.sumDbArray(levelsPerSourceLines.get(lvl.sourceId),
                                lvl.value));
                    }
                }
                long sourcePK;
                for (Map.Entry<Long, double[]> entry : levelsPerSourceLines.entrySet()) {
                    final long sourceId = entry.getKey();
                    sourcePK = sourceId;
                    if(ldenComputeRaysOut.inputData != null) {
                        // Retrieve original source identifier
                        if(entry.getKey() < ldenComputeRaysOut.inputData.sourcesPk.size()) {
                            sourcePK = ldenComputeRaysOut.inputData.sourcesPk.get((int)sourceId);
                        }
                    }
                    if(ldenConfig.computeLDay) {
                        double[] levels = ComputeRays.sumArray(ComputeRays.wToDba(ldenComputeRaysOut.ldenPropagationProcessData.
                                wjSourcesD.get((int) sourceId)), entry.getValue());
                        pushInStack(ldenComputeRaysOut.ldenData.lDayLevels, new VerticeSL(receiverPK, sourcePK, levels));
                    }
                    if(ldenConfig.computeLEvening) {
                        double[] levels = ComputeRays.sumArray(ComputeRays.wToDba(ldenComputeRaysOut.ldenPropagationProcessData.
                                wjSourcesE.get((int) sourceId)), entry.getValue());
                        pushInStack(ldenComputeRaysOut.ldenData.lEveningLevels, new VerticeSL(receiverPK, sourcePK, levels));
                    }
                    if(ldenConfig.computeLNight) {
                        double[] levels = ComputeRays.sumArray(ComputeRays.wToDba(ldenComputeRaysOut.ldenPropagationProcessData.
                                wjSourcesN.get((int) sourceId)), entry.getValue());
                        pushInStack(ldenComputeRaysOut.ldenData.lNightLevels, new VerticeSL(receiverPK, sourcePK, levels));
                    }
                    if(ldenConfig.computeLDEN) {
                        double[] levels = ComputeRays.sumArray(ComputeRays.wToDba(ldenComputeRaysOut.ldenPropagationProcessData.
                                wjSourcesDEN.get((int) sourceId)), entry.getValue());
                        pushInStack(ldenComputeRaysOut.ldenData.lDenLevels, new VerticeSL(receiverPK, sourcePK, levels));
                    }
                }
            } else {
                // Merge all results
                if(ldenConfig.computeLDay) {
                    processAndPushResult(receiverPK, ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesD,
                            ldenComputeRaysOut.ldenData.lDayLevels);
                }
                if(ldenConfig.computeLEvening) {
                    processAndPushResult(receiverPK, ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesE,
                            ldenComputeRaysOut.ldenData.lEveningLevels);
                }
                if(ldenConfig.computeLNight) {
                    processAndPushResult(receiverPK, ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesD,
                            ldenComputeRaysOut.ldenData.lNightLevels);
                }
                if(ldenConfig.computeLDEN) {
                    processAndPushResult(receiverPK, ldenComputeRaysOut.ldenPropagationProcessData.wjSourcesDEN,
                            ldenComputeRaysOut.ldenData.lDenLevels);
                }
            }
            receiverAttenuationLevels.clear();
        }
    }

    static class LdenData {
        AtomicLong queueSize = new AtomicLong(0);
        public ConcurrentLinkedDeque<VerticeSL> lDayLevels = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<VerticeSL> lEveningLevels = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<VerticeSL> lNightLevels = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<VerticeSL> lDenLevels = new ConcurrentLinkedDeque<>();
    }
}
