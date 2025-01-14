/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.Attenuation;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.wToDba;


public class NoiseMapInStack implements IComputePathsOut {
    NoiseMap noiseMapComputeRaysOut;
    NoiseMapParameters noiseMapParameters;
    public List<CnossosPath> pathParameters = new ArrayList<CnossosPath>();
    /**
     * Collected attenuation per receiver
     */
    Map<Integer, NoiseMapParameters.TimePeriodParameters> receiverAttenuationPerSource = new HashMap<>();
    /**
     * Cumulated global power at receiver, only used to stop looking for far sources
     */
    double[] wjAtReceiver = new double[0];
    /**
     * Favorable Free Field cumulated global power at receiver, only used to stop looking for far sources
     */
    Map<Integer, Double> maximumWjExpectedSplAtReceiver = new HashMap<>();
    double sumMaximumRemainingWjExpectedSplAtReceiver = 0;
    //
    public static final double DAY_RATIO = 12. / 24.;
    public static final double EVENING_RATIO = 4. / 24. * dbaToW(5.0);
    public static final double NIGHT_RATIO = 8. / 24. * dbaToW(10.0);

    /**
     * Constructs a NoiseMapInStack object with a multi-threaded parent NoiseMap instance.
     * @param multiThreadParent
     */
    public NoiseMapInStack(NoiseMap multiThreadParent) {
        this.noiseMapComputeRaysOut = multiThreadParent;
        this.noiseMapParameters = multiThreadParent.noiseEmissionMaker.noiseMapParameters;
    }

    /**
     * Energetic sum of VerticeSL attenuation with WJ sources
     * @param wjSources
     * @param receiverAttenuationLevels
     * @return
     */
    double[] sumLevels(List<double[]> wjSources, List<Attenuation.SourceReceiverAttenuation> receiverAttenuationLevels) {
        double[] levels = new double[noiseMapComputeRaysOut.dayPathData.freq_lvl.size()];
        for (Attenuation.SourceReceiverAttenuation lvl : receiverAttenuationLevels) {
            if(wjSources.size() > lvl.source.sourceIndex && lvl.source.sourceIndex >= 0) {
                levels = sumArray(levels,
                        dbaToW(sumArray(wToDba(wjSources.get(lvl.source.sourceIndex)), lvl.value)));
            }
        }
        return levels;
    }

    private void addGlobalReceiverLevel(double[] wjLevel) {
        if(wjAtReceiver.length != wjLevel.length) {
            wjAtReceiver = wjLevel.clone();
        } else {
            wjAtReceiver = AcousticIndicatorsFunctions.sumArray(wjAtReceiver, wjLevel);
        }
    }

    private NoiseMapParameters.TimePeriodParameters computeLdenAttenuation(CnossosPath cnossosPath) {
        PathFinder.SourcePointInfo sourceInfo = new PathFinder.SourcePointInfo(cnossosPath.getCutProfile().getSource());
        PathFinder.ReceiverPointInfo receiverInfo = new PathFinder.ReceiverPointInfo(cnossosPath.getCutProfile().getReceiver());
        NoiseMapParameters.TimePeriodParameters denWAttenuation =
                new NoiseMapParameters.TimePeriodParameters(sourceInfo,
                        receiverInfo, new double[0], new double[0], new double[0]);
        CutPointSource source = cnossosPath.getCutProfile().getSource();
        List<CnossosPath> cnossosPaths = Collections.singletonList(cnossosPath);
        if (noiseMapParameters.computeLDay || noiseMapParameters.computeLDEN) {
            denWAttenuation.dayLevels = dbaToW(noiseMapComputeRaysOut.computeCnossosAttenuation(
                    noiseMapComputeRaysOut.dayPathData,
                    source.id,
                    source.li,
                    cnossosPaths));
        }
        if (noiseMapParameters.computeLEvening || noiseMapParameters.computeLDEN) {
            denWAttenuation.eveningLevels = dbaToW(noiseMapComputeRaysOut.computeCnossosAttenuation(
                    noiseMapComputeRaysOut.eveningPathData,
                    source.id,
                    source.li,
                    cnossosPaths));
        }
        if (noiseMapParameters.computeLNight || noiseMapParameters.computeLDEN) {
            denWAttenuation.nightLevels = dbaToW(noiseMapComputeRaysOut.computeCnossosAttenuation(
                    noiseMapComputeRaysOut.nightPathData,
                    source.id,
                    source.li,
                    cnossosPaths));
        }
        return denWAttenuation;
    }

    public static double[] computeLden(NoiseMapParameters.TimePeriodParameters denWAttenuation,
                                       double[] wjSourcesD, double[] wjSourcesE, double[] wjSourcesN, NoiseMapParameters.TimePeriodParameters denWLevel) {
        double[] ldenLevel = new double[0];
        denWLevel.receiver = denWAttenuation.receiver;
        denWLevel.source = denWAttenuation.source;
        if (wjSourcesD.length > 0) {
            // Apply attenuation on source level
            denWLevel.dayLevels = multiplicationArray(denWAttenuation.dayLevels,
                    wjSourcesD);
            ldenLevel = multiplicationArray(denWLevel.dayLevels, DAY_RATIO);
        }
        if (wjSourcesE.length > 0) {
            // Apply attenuation on source level
            denWLevel.eveningLevels = multiplicationArray(denWAttenuation.eveningLevels,
                    wjSourcesE);
            ldenLevel = sumArray(ldenLevel, multiplicationArray(denWLevel.eveningLevels, EVENING_RATIO));
        }
        if (wjSourcesN.length > 0) {
            // Apply attenuation on source level
            denWLevel.nightLevels = multiplicationArray(denWAttenuation.nightLevels,
                    wjSourcesN);
            ldenLevel = sumArray(ldenLevel, multiplicationArray(denWLevel.nightLevels, NIGHT_RATIO));
        }
        return ldenLevel;
    }

    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        PathSearchStrategy strategy = PathSearchStrategy.CONTINUE;
        final Scene scene = noiseMapComputeRaysOut.inputData;
        CnossosPath cnossosPath = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.freq_lvl, scene.gS);
        if(cnossosPath != null) {
            CutPointSource source = cutProfile.getSource();
            CutPointReceiver receiver = cutProfile.getReceiver();

            long receiverPk = receiver.receiverPk == -1 ? receiver.id : receiver.receiverPk;
            long sourcePk = source.sourcePk == -1 ? source.id : source.sourcePk;

            // export path if required
            noiseMapComputeRaysOut.rayCount.addAndGet(1);
            if(noiseMapComputeRaysOut.exportPaths && !noiseMapComputeRaysOut.exportAttenuationMatrix) {
                // Use only one ray as the ray is the same if we not keep absorption values
                // Copy path content in order to keep original ids for other method calls
                cnossosPath.setIdReceiver(receiverPk);
                cnossosPath.setIdSource(sourcePk);
                this.pathParameters.add(cnossosPath);
            }
            // Compute attenuation for each time period
            NoiseMapParameters.TimePeriodParameters denWAttenuation = computeLdenAttenuation(cnossosPath);
            if(noiseMapParameters.maximumError > 0) {
                // Add power to evaluate potential error if ignoring remaining sources
                NoiseEmissionMaker noiseEmissionMaker = noiseMapComputeRaysOut.noiseEmissionMaker;
                double[] lden = computeLden(denWAttenuation,
                        getSpectrum(noiseEmissionMaker.wjSourcesD, source.id),
                        getSpectrum(noiseEmissionMaker.wjSourcesE, source.id),
                        getSpectrum(noiseEmissionMaker.wjSourcesN, source.id), new NoiseMapParameters.TimePeriodParameters());
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
                if (dBDiff < noiseMapParameters.maximumError) {
                    strategy = PathSearchStrategy.PROCESS_SOURCE_BUT_SKIP_RECEIVER;
                }
            }
            // apply attenuation to global attenuation
            // push or merge attenuation level
            receiverAttenuationPerSource.merge(source.id, denWAttenuation,
                    (timePeriodParameters, timePeriodParameters2) ->
                            new NoiseMapParameters.TimePeriodParameters( timePeriodParameters.source, timePeriodParameters.receiver,
                                    sumArray(timePeriodParameters.dayLevels, timePeriodParameters2.dayLevels),
                                    sumArray(timePeriodParameters.eveningLevels, timePeriodParameters2.eveningLevels),
                                    sumArray(timePeriodParameters.nightLevels, timePeriodParameters2.nightLevels)));

        }
        return strategy;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList) {
        if(noiseMapParameters.getMaximumError() > 0) {
            maximumWjExpectedSplAtReceiver.clear();
            sumMaximumRemainingWjExpectedSplAtReceiver = 0;
            final Scene scene = noiseMapComputeRaysOut.inputData;
            CutPointReceiver pointReceiver = new CutPointReceiver(receiver);
            NoiseEmissionMaker noiseEmissionMaker = noiseMapComputeRaysOut.noiseEmissionMaker;
            for (PathFinder.SourcePointInfo sourcePointInfo : sourceList) {
                CutProfile cutProfile = new CutProfile(new CutPointSource(sourcePointInfo), pointReceiver);
                CnossosPath cnossosPath = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, scene.isBodyBarrier(),
                        scene.freq_lvl, scene.gS);
                if (cnossosPath != null) {
                    double[] wjReceiver = computeLden(computeLdenAttenuation(cnossosPath),
                            getSpectrum(noiseEmissionMaker.wjSourcesD, sourcePointInfo.sourceIndex),
                            getSpectrum(noiseEmissionMaker.wjSourcesE, sourcePointInfo.sourceIndex),
                            getSpectrum(noiseEmissionMaker.wjSourcesN, sourcePointInfo.sourceIndex),
                            new NoiseMapParameters.TimePeriodParameters());
                    double globalReceiver = sumArray(wjReceiver);
                    sumMaximumRemainingWjExpectedSplAtReceiver += globalReceiver;
                    maximumWjExpectedSplAtReceiver.merge(sourcePointInfo.getCoord().hashCode(), globalReceiver, Double::sum);
                }
            }
        }
    }

    /**
     * Pushes attenuation data into a concurrent linked deque.
     * @param stack Stack to feed
     * @param data receiver noise level in dB
     */
    public void pushInStack(ConcurrentLinkedDeque<Attenuation.SourceReceiverAttenuation> stack, Attenuation.SourceReceiverAttenuation data) {
        while(noiseMapComputeRaysOut.attenuatedPaths.queueSize.get() > noiseMapParameters.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                noiseMapParameters.aborted = true;
                break;
            }
            if(noiseMapParameters.aborted) {
                if(noiseMapComputeRaysOut != null && this.noiseMapComputeRaysOut.inputData != null &&
                        this.noiseMapComputeRaysOut.inputData.cellProg != null) {
                    this.noiseMapComputeRaysOut.inputData.cellProg.cancel();
                }
                return;
            }
        }
        stack.add(data);
        noiseMapComputeRaysOut.attenuatedPaths.queueSize.incrementAndGet();
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess() {
        return null;
    }

    /**
     * Adds Cnossos paths to a concurrent stack while maintaining the maximum stack size.
     * @param stack Stack to feed
     * @param data rays
     */
    public void pushInStack(ConcurrentLinkedDeque<CnossosPath> stack, Collection<CnossosPath> data) {
        while(noiseMapComputeRaysOut.attenuatedPaths.queueSize.get() > noiseMapParameters.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                noiseMapParameters.aborted = true;
                break;
            }
            if(noiseMapParameters.aborted) {
                if(noiseMapComputeRaysOut != null && this.noiseMapComputeRaysOut.inputData != null &&
                        this.noiseMapComputeRaysOut.inputData.cellProg != null) {
                    this.noiseMapComputeRaysOut.inputData.cellProg.cancel();
                }
                return;
            }
        }
        if(noiseMapParameters.getMaximumRaysOutputCount() == 0 || noiseMapComputeRaysOut.attenuatedPaths.totalRaysInserted.get() < noiseMapParameters.getMaximumRaysOutputCount()) {
            long newTotalRays = noiseMapComputeRaysOut.attenuatedPaths.totalRaysInserted.addAndGet(data.size());
            if(noiseMapParameters.getMaximumRaysOutputCount() > 0 && newTotalRays > noiseMapParameters.getMaximumRaysOutputCount()) {
                // too many rays, remove unwanted rays
                int newListSize = data.size() - (int)(newTotalRays - noiseMapParameters.getMaximumRaysOutputCount());
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
            noiseMapComputeRaysOut.attenuatedPaths.queueSize.addAndGet(data.size());
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
            if(noiseMapParameters.getExportRaysMethod() == NoiseMapParameters.ExportRaysMethods.TO_RAYS_TABLE) {
                // Push propagation rays
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.rays, this.pathParameters);
            } else if(noiseMapParameters.getExportRaysMethod() == NoiseMapParameters.ExportRaysMethods.TO_MEMORY
                    && (noiseMapParameters.getMaximumRaysOutputCount() == 0 ||
                    noiseMapComputeRaysOut.propagationPathsSize.get() < noiseMapParameters.getMaximumRaysOutputCount())){
                int newRaysSize = noiseMapComputeRaysOut.propagationPathsSize.addAndGet(this.pathParameters.size());
                if(noiseMapParameters.getMaximumRaysOutputCount() > 0 && newRaysSize > noiseMapParameters.getMaximumRaysOutputCount()) {
                    // remove exceeded elements of the array
                    this.pathParameters = this.pathParameters.subList(0,
                            this.pathParameters.size() - Math.min( this.pathParameters.size(),
                                    newRaysSize - noiseMapParameters.getMaximumRaysOutputCount()));
                }
                noiseMapComputeRaysOut.pathParameters.addAll(this.pathParameters);
            }
            this.pathParameters.clear();
        }
        NoiseEmissionMaker noiseEmissionMaker = noiseMapComputeRaysOut.noiseEmissionMaker;
        Map<Integer, NoiseMapParameters.TimePeriodParameters> attenuationPerSource = receiverAttenuationPerSource;

        final int spectrumSize = noiseMapComputeRaysOut.inputData.freq_lvl.size();
        double[] mergedLdenSpectrum = new double[spectrumSize];
        NoiseMapParameters.TimePeriodParameters mergedLden = new NoiseMapParameters.TimePeriodParameters(
                new PathFinder.SourcePointInfo(), receiver, new double[spectrumSize], new double[spectrumSize],
                new double[spectrumSize]);
        for (Map.Entry<Integer, NoiseMapParameters.TimePeriodParameters> timePeriodParametersEntry :
                attenuationPerSource.entrySet()) {
            int sourceId = timePeriodParametersEntry.getKey();
            NoiseMapParameters.TimePeriodParameters denValues = new NoiseMapParameters.TimePeriodParameters();
            NoiseMapParameters.TimePeriodParameters denAttenuation = timePeriodParametersEntry.getValue();
            // Apply attenuation to emission level
            double[] ldenSpectrum = computeLden(denAttenuation,
                    getSpectrum(noiseEmissionMaker.wjSourcesD, sourceId),
                    getSpectrum(noiseEmissionMaker.wjSourcesE, sourceId),
                    getSpectrum(noiseEmissionMaker.wjSourcesN, sourceId),
                    denValues);
            // Store receiver level in appropriate stack
            if (noiseMapParameters.mergeSources) {
                mergedLdenSpectrum = sumArray(mergedLdenSpectrum, ldenSpectrum);
                mergedLden.dayLevels = sumArray(mergedLden.dayLevels, denValues.dayLevels);
                mergedLden.eveningLevels = sumArray(mergedLden.eveningLevels, denValues.eveningLevels);
                mergedLden.nightLevels = sumArray(mergedLden.nightLevels, denValues.nightLevels);
            } else {
                if (noiseMapParameters.computeLDay) {
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lDayLevels,
                        new Attenuation.SourceReceiverAttenuation(receiver,
                                denAttenuation.source,
                                wToDba(denValues.dayLevels)
                        ));
                }
                if (noiseMapParameters.computeLEvening) {
                    pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lEveningLevels,
                            new Attenuation.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(denValues.eveningLevels)
                            ));
                }
                if (noiseMapParameters.computeLNight) {
                    pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lNightLevels,
                            new Attenuation.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(denValues.nightLevels)
                            ));
                }
                if (noiseMapParameters.computeLDEN) {
                    pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lDenLevels,
                            new Attenuation.SourceReceiverAttenuation(denAttenuation.receiver,
                                    denAttenuation.source,
                                    wToDba(ldenSpectrum)
                            ));
                }
            }
        }
        if (noiseMapParameters.mergeSources) {
            if (noiseMapParameters.computeLDay) {
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lDayLevels,
                        new Attenuation.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.dayLevels)
                        ));
            }
            if (noiseMapParameters.computeLEvening) {
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lEveningLevels,
                        new Attenuation.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.eveningLevels)
                        ));
            }
            if (noiseMapParameters.computeLNight) {
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lNightLevels,
                        new Attenuation.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLden.nightLevels)
                        ));
            }
            if (noiseMapParameters.computeLDEN) {
                pushInStack(noiseMapComputeRaysOut.attenuatedPaths.lDenLevels,
                        new Attenuation.SourceReceiverAttenuation(mergedLden.receiver,
                                mergedLden.source,
                                wToDba(mergedLdenSpectrum)
                        ));
            }
        }
        receiverAttenuationPerSource.clear();
    }
}
