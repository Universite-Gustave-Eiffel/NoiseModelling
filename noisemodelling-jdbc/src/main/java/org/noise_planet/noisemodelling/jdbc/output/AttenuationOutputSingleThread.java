/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.output;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.jdbc.NoiseEmissionMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;
import org.noise_planet.noisemodelling.propagation.ReceiverNoiseLevel;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.wToDba;


/**
 * Managed by a single thread, process all incoming vertical profile, compute attenuation and push on appropriate stack
 * for exporting result values in a thread safe way. It processes the receiver one at a time.
 */
public class AttenuationOutputSingleThread implements IComputePathsOut {
    AttenuationOutputMultiThread multiThread;
    NoiseMapDatabaseParameters noiseMapDatabaseParameters;
    public List<CnossosPath> pathParameters = new ArrayList<CnossosPath>();

    /**
     * Collected attenuation/noise level on the current receiver
     */
    List<ReceiverNoiseLevel> receiverAttenuationList = new LinkedList<>();

    /**
     * MaxError DB Processing variable
     * Current, minimal power at receiver (minimal among time periods), only used to stop looking for far sources
     */
    double[] wjAtReceiver = new double[0];

    /**
     * MaxError DB Processing variable
     * Favorable Free Field cumulated global power at receiver, only used to stop looking for far sources
     * Key source index
     * Value maximum expected noise level in w
     */
    Map<Integer, Double> maximumWjExpectedSplAtReceiver = new HashMap<>();
    /**
     * MaxError DB Processing variable
     * Next Free Field cumulated global power at receiver, only used to stop looking for far sources
     */
    double sumMaximumRemainingWjExpectedSplAtReceiver = 0;

    public AtomicInteger cutProfileCount = new AtomicInteger(0);

    /**
     * Constructs a NoiseMapInStack object with a multithreaded parent NoiseMap instance.
     * This class is not thread-safe
     * @param multiThreadParent
     */
    public AttenuationOutputSingleThread(AttenuationOutputMultiThread multiThreadParent) {
        this.multiThread = multiThreadParent;
        this.noiseMapDatabaseParameters = multiThreadParent.noiseMapDatabaseParameters;
    }
//
//    /**
//     * Energetic sum of VerticeSL attenuation with WJ sources
//     * @param wjSources
//     * @param receiverAttenuationLevels
//     * @return
//     */
//    double[] sumLevels(List<double[]> wjSources, List<ReceiverNoiseLevel> receiverAttenuationLevels) {
//        double[] levels = new double[0];
//        for (ReceiverNoiseLevel lvl : receiverAttenuationLevels) {
//            if(wjSources.size() > lvl.source.sourceIndex && lvl.source.sourceIndex >= 0) {
//                levels = sumArray(levels,
//                        dbaToW(sumArray(wToDba(wjSources.get(lvl.source.sourceIndex)), lvl.levels)));
//            }
//        }
//        return levels;
//    }

    private void addGlobalReceiverLevel(double[] wjLevel) {
        if(wjAtReceiver.length != wjLevel.length) {
            wjAtReceiver = wjLevel.clone();
        } else {
            wjAtReceiver = AcousticIndicatorsFunctions.sumArray(wjAtReceiver, wjLevel);
        }
    }
//
//    private NoiseMapDatabaseParameters.TimePeriodParameters computeFastLdenAttenuation(PathFinder.SourcePointInfo sourceInfo,
//                                                                                       PathFinder.ReceiverPointInfo receiverInfo) {
//        // For the quick attenuation evaluation
//        // only take account of geometric dispersion and atmospheric attenuation
//        double distance = Math.max(1.0, sourceInfo.position.distance3D(receiverInfo.position));
//        // 3 dB gain as we consider source G path is equal to 0
//        double attenuationDivGeom = AttenuationCnossos.getADiv(distance) - 3;
//        NoiseMapDatabaseParameters.TimePeriodParameters denWAttenuation =
//                new NoiseMapDatabaseParameters.TimePeriodParameters(sourceInfo,
//                        receiverInfo, new double[0], new double[0], new double[0]);
//        if (noiseMapDatabaseParameters.computeLDay || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.dayLevels = dbaToW(AcousticIndicatorsFunctions.multiplicationArray(AcousticIndicatorsFunctions.sumArray(
//                    AttenuationCnossos.aAtm(attenuationOutputMultiThreadComputeRaysOut.dayPathData.getAlpha_atmo(), distance),
//                    attenuationDivGeom), -1));
//        }
//        if (noiseMapDatabaseParameters.computeLEvening || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.eveningLevels = dbaToW(AcousticIndicatorsFunctions.multiplicationArray(AcousticIndicatorsFunctions.sumArray(
//                    AttenuationCnossos.aAtm(attenuationOutputMultiThreadComputeRaysOut.eveningPathData.getAlpha_atmo(), distance),
//                    attenuationDivGeom), -1));
//        }
//        if (noiseMapDatabaseParameters.computeLNight || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.nightLevels = dbaToW(AcousticIndicatorsFunctions.multiplicationArray(AcousticIndicatorsFunctions.sumArray(
//                    AttenuationCnossos.aAtm(attenuationOutputMultiThreadComputeRaysOut.nightPathData.getAlpha_atmo(), distance),
//                    attenuationDivGeom), -1));
//        }
//        return denWAttenuation;
//    }
//
//
//    private NoiseMapDatabaseParameters.TimePeriodParameters computeLdenAttenuation(CnossosPath cnossosPath) {
//        PathFinder.SourcePointInfo sourceInfo = new PathFinder.SourcePointInfo(cnossosPath.getCutProfile().getSource());
//        PathFinder.ReceiverPointInfo receiverInfo = new PathFinder.ReceiverPointInfo(cnossosPath.getCutProfile().getReceiver());
//        NoiseMapDatabaseParameters.TimePeriodParameters denWAttenuation =
//                new NoiseMapDatabaseParameters.TimePeriodParameters(sourceInfo,
//                        receiverInfo, new double[0], new double[0], new double[0]);
//        CutPointSource source = cnossosPath.getCutProfile().getSource();
//        List<CnossosPath> cnossosPaths = Collections.singletonList(cnossosPath);
//        if (noiseMapDatabaseParameters.computeLDay || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.dayLevels = dbaToW(attenuationOutputMultiThreadComputeRaysOut.computeCnossosAttenuation(
//                    attenuationOutputMultiThreadComputeRaysOut.dayPathData,
//                    source.id,
//                    source.li,
//                    cnossosPaths));
//        }
//        if (noiseMapDatabaseParameters.computeLEvening || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.eveningLevels = dbaToW(attenuationOutputMultiThreadComputeRaysOut.computeCnossosAttenuation(
//                    attenuationOutputMultiThreadComputeRaysOut.eveningPathData,
//                    source.id,
//                    source.li,
//                    cnossosPaths));
//        }
//        if (noiseMapDatabaseParameters.computeLNight || noiseMapDatabaseParameters.computeLDEN) {
//            denWAttenuation.nightLevels = dbaToW(attenuationOutputMultiThreadComputeRaysOut.computeCnossosAttenuation(
//                    attenuationOutputMultiThreadComputeRaysOut.nightPathData,
//                    source.id,
//                    source.li,
//                    cnossosPaths));
//        }
//        return denWAttenuation;
//    }
//
//    public static double[] computeLden(NoiseMapDatabaseParameters.TimePeriodParameters denWAttenuation,
//                                       double[] wjSourcesD, double[] wjSourcesE, double[] wjSourcesN, NoiseMapDatabaseParameters.TimePeriodParameters denWLevel) {
//        double[] ldenLevel = new double[0];
//        denWLevel.receiver = denWAttenuation.receiver;
//        denWLevel.source = denWAttenuation.source;
//        if (wjSourcesD.length > 0) {
//            // Apply attenuation on source level
//            denWLevel.dayLevels = multiplicationArray(denWAttenuation.dayLevels,
//                    wjSourcesD);
//            ldenLevel = multiplicationArray(denWLevel.dayLevels, DAY_RATIO);
//        }
//        if (wjSourcesE.length > 0) {
//            // Apply attenuation on source level
//            denWLevel.eveningLevels = multiplicationArray(denWAttenuation.eveningLevels,
//                    wjSourcesE);
//            ldenLevel = sumArray(ldenLevel, multiplicationArray(denWLevel.eveningLevels, EVENING_RATIO));
//        }
//        if (wjSourcesN.length > 0) {
//            // Apply attenuation on source level
//            denWLevel.nightLevels = multiplicationArray(denWAttenuation.nightLevels,
//                    wjSourcesN);
//            ldenLevel = sumArray(ldenLevel, multiplicationArray(denWLevel.nightLevels, NIGHT_RATIO));
//        }
//        return ldenLevel;
//    }

    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        cutProfileCount.addAndGet(1);
        PathSearchStrategy strategy = PathSearchStrategy.CONTINUE;
        final SceneWithEmission scene = multiThread.sceneWithEmission;
        CnossosPath cnossosPath = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, scene.defaultGroundAttenuation);
        if(cnossosPath != null) {
            CutPointSource source = cutProfile.getSource();
            CutPointReceiver receiver = cutProfile.getReceiver();

            long receiverPk = receiver.receiverPk == -1 ? receiver.id : receiver.receiverPk;
            long sourcePk = source.sourcePk == -1 ? source.id : source.sourcePk;

            // export path if required
            multiThread.rayCount.addAndGet(1);
            if(multiThread.exportPaths && !multiThread.exportAttenuationMatrix) {
                // Use only one ray as the ray is the same if we not keep absorption values
                // Copy path content in order to keep original ids for other method calls
                cnossosPath.setIdReceiver(receiverPk);
                cnossosPath.setIdSource(sourcePk);
                this.pathParameters.add(cnossosPath);
            }
            // Compute attenuation for each time period
            if(scene.wjSources.containsKey(sourcePk)) {
                ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePk);
                for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                    String period = periodEmission.period;
                }
            }

            NoiseMapDatabaseParameters.TimePeriodParameters denWAttenuation = computeLdenAttenuation(cnossosPath);
            if(noiseMapDatabaseParameters.maximumError > 0) {
                // Add power to evaluate potential error if ignoring remaining sources
                NoiseEmissionMaker noiseEmissionMaker = multiThread.sceneWithEmission;
                double[] lden = computeLden(denWAttenuation,
                        getSpectrum(noiseEmissionMaker.wjSourcesD, source.id),
                        getSpectrum(noiseEmissionMaker.wjSourcesE, source.id),
                        getSpectrum(noiseEmissionMaker.wjSourcesN, source.id), new NoiseMapDatabaseParameters.TimePeriodParameters());
                addGlobalReceiverLevel(lden);

                double currentLevelAtReceiver = wToDba(sumArray(wjAtReceiver));
                // replace unknown value of expected power for this source
                int sourceHashCode = source.coordinate.hashCode();
                if(maximumWjExpectedSplAtReceiver.containsKey(sourceHashCode)) {
                    sumMaximumRemainingWjExpectedSplAtReceiver -= maximumWjExpectedSplAtReceiver.get(sourceHashCode);
                    maximumWjExpectedSplAtReceiver.remove(sourceHashCode);
                }
                double maximumExpectedLevelInDb = wToDba(sumArray(wjAtReceiver) + sumMaximumRemainingWjExpectedSplAtReceiver);
                double dBDiff = maximumExpectedLevelInDb - currentLevelAtReceiver;
                if (dBDiff < noiseMapDatabaseParameters.maximumError) {
                    strategy = PathSearchStrategy.PROCESS_SOURCE_BUT_SKIP_RECEIVER;
                }
            }
            // apply attenuation to global attenuation
            // push or merge attenuation level
            receiverAttenuationList.merge(source.id, denWAttenuation,
                    (timePeriodParameters, timePeriodParameters2) ->
                            new NoiseMapDatabaseParameters.TimePeriodParameters( timePeriodParameters.source, timePeriodParameters.receiver,
                                    sumArray(timePeriodParameters.dayLevels, timePeriodParameters2.dayLevels),
                                    sumArray(timePeriodParameters.eveningLevels, timePeriodParameters2.eveningLevels),
                                    sumArray(timePeriodParameters.nightLevels, timePeriodParameters2.nightLevels)));

        }
        return strategy;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList, AtomicInteger cutProfileCount) {
        this.cutProfileCount = cutProfileCount;
        wjAtReceiver = new double[0];
        if(noiseMapDatabaseParameters.getMaximumError() > 0) {
            maximumWjExpectedSplAtReceiver.clear();
            sumMaximumRemainingWjExpectedSplAtReceiver = 0;
            NoiseEmissionMaker noiseEmissionMaker = multiThread.sceneWithEmission;
            NoiseMapDatabaseParameters.TimePeriodParameters cachedValues = new NoiseMapDatabaseParameters.TimePeriodParameters();
            for (PathFinder.SourcePointInfo sourcePointInfo : sourceList) {
                NoiseMapDatabaseParameters.TimePeriodParameters ldenAttenuation = computeFastLdenAttenuation(sourcePointInfo, receiver);
                double[] wjReceiver = computeLden(ldenAttenuation,
                        AcousticIndicatorsFunctions.multiplicationArray(getSpectrum(noiseEmissionMaker.wjSourcesD,
                                sourcePointInfo.sourceIndex), sourcePointInfo.li),
                        AcousticIndicatorsFunctions.multiplicationArray(getSpectrum(noiseEmissionMaker.wjSourcesE,
                                sourcePointInfo.sourceIndex), sourcePointInfo.li),
                        AcousticIndicatorsFunctions.multiplicationArray(getSpectrum(noiseEmissionMaker.wjSourcesN,
                                sourcePointInfo.sourceIndex), sourcePointInfo.li),
                            cachedValues);
                    double globalReceiver = sumArray(wjReceiver);
                    sumMaximumRemainingWjExpectedSplAtReceiver += globalReceiver;
                    maximumWjExpectedSplAtReceiver.merge(sourcePointInfo.getCoord().hashCode(), globalReceiver, Double::sum);
            }
        }
    }

    /**
     * Pushes attenuation data into a concurrent linked deque.
     * @param stack Stack to feed
     * @param data receiver noise level in dB
     */
    public void pushInStack(ConcurrentLinkedDeque<ReceiverNoiseLevel> stack, ReceiverNoiseLevel data) {
        while(multiThread.resultsCache.queueSize.get() > noiseMapDatabaseParameters.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                multiThread.aborted.set(true);
                break;
            }
            if(multiThread.aborted.get()) {
                if(multiThread != null && this.multiThread.scene != null &&
                        this.multiThread.p != null) {
                    this.multiThread.scene.cellProg.cancel();
                }
                return;
            }
        }
        stack.add(data);
        multiThread.resultsCache.queueSize.incrementAndGet();
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess(ProgressVisitor visitor) {
        return null;
    }

    /**
     * Adds Cnossos paths to a concurrent stack while maintaining the maximum stack size.
     * @param stack Stack to feed
     * @param data rays
     */
    public void pushInStack(ConcurrentLinkedDeque<CnossosPath> stack, Collection<CnossosPath> data) {
        while(multiThread.resultsCache.queueSize.get() > noiseMapDatabaseParameters.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                multiThread.aborted.set(true);
                break;
            }
            if(multiThread.aborted.get()) {
                if(multiThread != null && this.multiThread.scene != null &&
                        this.multiThread.scene.cellProg != null) {
                    this.multiThread.scene.cellProg.cancel();
                }
                return;
            }
        }
        if(noiseMapDatabaseParameters.getMaximumRaysOutputCount() == 0 || multiThread.resultsCache.totalRaysInserted.get() < noiseMapDatabaseParameters.getMaximumRaysOutputCount()) {
            long newTotalRays = multiThread.resultsCache.totalRaysInserted.addAndGet(data.size());
            if(noiseMapDatabaseParameters.getMaximumRaysOutputCount() > 0 && newTotalRays > noiseMapDatabaseParameters.getMaximumRaysOutputCount()) {
                // too many rays, remove unwanted rays
                int newListSize = data.size() - (int)(newTotalRays - noiseMapDatabaseParameters.getMaximumRaysOutputCount());
                List<CnossosPath> subList = new ArrayList<CnossosPath>(newListSize);
                for(CnossosPath pathParameters : data) {
                    subList.add(pathParameters);
                    if(subList.size() >= newListSize) {
                        break;
                    }
                }
                data = subList;
            }
            stack.addAll(data);
            multiThread.resultsCache.queueSize.addAndGet(data.size());
        }
    }

    private static double[] getSpectrum(List<double[]> spectrum, int index) {
        if(index >= 0 && index < spectrum.size()) {
            return spectrum.get(index);
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
        if(!this.pathParameters.isEmpty()) {
            if(noiseMapDatabaseParameters.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE) {
                // Push propagation rays
                pushInStack(multiThread.resultsCache.rays, this.pathParameters);
            } else if(noiseMapDatabaseParameters.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.TO_MEMORY
                    && (noiseMapDatabaseParameters.getMaximumRaysOutputCount() == 0 ||
                    multiThread.propagationPathsSize.get() < noiseMapDatabaseParameters.getMaximumRaysOutputCount())){
                int newRaysSize = multiThread.propagationPathsSize.addAndGet(this.pathParameters.size());
                if(noiseMapDatabaseParameters.getMaximumRaysOutputCount() > 0 && newRaysSize > noiseMapDatabaseParameters.getMaximumRaysOutputCount()) {
                    // remove exceeded elements of the array
                    this.pathParameters = this.pathParameters.subList(0,
                            this.pathParameters.size() - Math.min( this.pathParameters.size(),
                                    newRaysSize - noiseMapDatabaseParameters.getMaximumRaysOutputCount()));
                }
                multiThread.pathParameters.addAll(this.pathParameters);
            }
            this.pathParameters.clear();
        }
        NoiseEmissionMaker noiseEmissionMaker = multiThread.sceneWithEmission;
        Map<Integer, NoiseMapDatabaseParameters.TimePeriodParameters> attenuationPerSource = receiverAttenuationList;

        final int spectrumSize = multiThread.scene.freq_lvl.size();
        double[] mergedLdenSpectrum = new double[spectrumSize];
        NoiseMapDatabaseParameters.TimePeriodParameters mergedLden = new NoiseMapDatabaseParameters.TimePeriodParameters(
                new PathFinder.SourcePointInfo(), receiver, new double[spectrumSize], new double[spectrumSize],
                new double[spectrumSize]);
        for (Map.Entry<Integer, NoiseMapDatabaseParameters.TimePeriodParameters> timePeriodParametersEntry :
                attenuationPerSource.entrySet()) {
            int sourceId = timePeriodParametersEntry.getKey();
            NoiseMapDatabaseParameters.TimePeriodParameters denValues = new NoiseMapDatabaseParameters.TimePeriodParameters();
            NoiseMapDatabaseParameters.TimePeriodParameters denAttenuation = timePeriodParametersEntry.getValue();
            // Apply attenuation to emission level
            double[] ldenSpectrum = computeLden(denAttenuation,
                    getSpectrum(noiseEmissionMaker.wjSourcesD, sourceId),
                    getSpectrum(noiseEmissionMaker.wjSourcesE, sourceId),
                    getSpectrum(noiseEmissionMaker.wjSourcesN, sourceId),
                    denValues);
            // Store receiver level in appropriate stack
            if (noiseMapDatabaseParameters.mergeSources) {
                mergedLdenSpectrum = sumArray(mergedLdenSpectrum, ldenSpectrum);
                mergedLden.dayLevels = sumArray(mergedLden.dayLevels, denValues.dayLevels);
                mergedLden.eveningLevels = sumArray(mergedLden.eveningLevels, denValues.eveningLevels);
                mergedLden.nightLevels = sumArray(mergedLden.nightLevels, denValues.nightLevels);
            } else {
                if (noiseMapDatabaseParameters.computeLDay) {
                pushInStack(multiThread.resultsCache.lDayLevels,
                        new AttenuationComputeOutput.SourceReceiverAttenuation(receiver,
                                denAttenuation.source,
                                wToDba(denValues.dayLevels)
                        ));
                }
                if (noiseMapDatabaseParameters.computeLEvening) {
                    pushInStack(multiThread.resultsCache.lEveningLevels,
                            new AttenuationComputeOutput.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(denValues.eveningLevels)
                            ));
                }
                if (noiseMapDatabaseParameters.computeLNight) {
                    pushInStack(multiThread.resultsCache.lNightLevels,
                            new AttenuationComputeOutput.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(denValues.nightLevels)
                            ));
                }
                if (noiseMapDatabaseParameters.computeLDEN) {
                    pushInStack(multiThread.resultsCache.lDenLevels,
                            new AttenuationComputeOutput.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(ldenSpectrum)
                            ));
                }
            }
        }
        if (noiseMapDatabaseParameters.mergeSources) {
            if (noiseMapDatabaseParameters.computeLDay) {
                pushInStack(multiThread.resultsCache.lDayLevels,
                        new AttenuationComputeOutput.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.dayLevels)
                        ));
            }
            if (noiseMapDatabaseParameters.computeLEvening) {
                pushInStack(multiThread.resultsCache.lEveningLevels,
                        new AttenuationComputeOutput.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.eveningLevels)
                        ));
            }
            if (noiseMapDatabaseParameters.computeLNight) {
                pushInStack(multiThread.resultsCache.lNightLevels,
                        new AttenuationComputeOutput.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.nightLevels)
                        ));
            }
            if (noiseMapDatabaseParameters.computeLDEN) {
                pushInStack(multiThread.resultsCache.lDenLevels,
                        new AttenuationComputeOutput.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLdenSpectrum)
                        ));
            }
        }
        receiverAttenuationList.clear();
        maximumWjExpectedSplAtReceiver.clear();
        sumMaximumRemainingWjExpectedSplAtReceiver = 0;
        wjAtReceiver = new double[0];
    }


    /**
     * representing the noise levels for different time periods.
     */
    public static class TimePeriodParameters {
        public PathFinder.SourcePointInfo source = null;
        public PathFinder.ReceiverPointInfo receiver = null;
        public Map<String, double[]> levelsPerPeriod = new HashMap<>();

        public TimePeriodParameters(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver) {
            this.source = source;
            this.receiver = receiver;
        }

        public TimePeriodParameters() {
        }
    }
}
