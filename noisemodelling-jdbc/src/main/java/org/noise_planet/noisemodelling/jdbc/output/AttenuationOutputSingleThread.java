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
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.ReceiverNoiseLevel;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossos;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPathBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.wToDb;


/**
 * Managed by a single thread, process all incoming vertical profile, compute attenuation and push on appropriate stack
 * for exporting result values in a thread safe way. It processes the receiver one at a time.
 */
public class AttenuationOutputSingleThread implements CutPlaneVisitor {
    private static final int UNKNOWN_SOURCE_ID = -1;
    AttenuationOutputMultiThread multiThread;
    NoiseMapDatabaseParameters dbSettings;
    public List<CnossosPath> cnossosPaths = new ArrayList<>();

    /**
     * Collected attenuation/noise level on the current receiver
     */
    Map<Integer, TimePeriodParameters> receiverAttenuationList = new HashMap<>();

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

    ProgressVisitor progressVisitor;

    /**
     * Constructs a NoiseMapInStack object with a multithreaded parent NoiseMap instance.
     * This class is not thread-safe
     * @param multiThreadParent
     */
    public AttenuationOutputSingleThread(AttenuationOutputMultiThread multiThreadParent, ProgressVisitor progressVisitor) {
        this.multiThread = multiThreadParent;
        this.dbSettings = multiThreadParent.noiseMapDatabaseParameters;
        this.progressVisitor = progressVisitor;
    }

    private void addGlobalReceiverLevel(double[] wjLevel) {
        if(wjAtReceiver.length != wjLevel.length) {
            wjAtReceiver = wjLevel.clone();
        } else {
            wjAtReceiver = AcousticIndicatorsFunctions.sumArray(wjAtReceiver, wjLevel);
        }
    }

    /**
     *
     * @param sourceInfo
     * @param receiverInfo
     * @param cnossosParameters
     * @return Attenuation in dB
     */
    private static double[] computeFastAttenuation(PathFinder.SourcePointInfo sourceInfo,
                                                   PathFinder.ReceiverPointInfo receiverInfo, AttenuationCnossosParameters cnossosParameters) {
        // For the quick attenuation evaluation
        // only take account of geometric dispersion and atmospheric attenuation
        double distance = Math.max(1.0, sourceInfo.position.distance3D(receiverInfo.position));
        // 3 dB gain as we consider source G path is equal to 0
        double attenuationDivGeom = AttenuationCnossos.getADiv(distance) - 3;
        return AcousticIndicatorsFunctions.multiplicationArray(AcousticIndicatorsFunctions.sumArray(
                    AttenuationCnossos.aAtm(cnossosParameters.getAlpha_atmo(), distance),
                    attenuationDivGeom), -1);
    }

    private double[] processAndStoreAttenuation(AttenuationCnossosParameters data, CnossosPath proPathParameters, String period) {
        double[] attenuation = AttenuationCnossos.computeCnossosAttenuation(data, proPathParameters, multiThread.sceneWithEmission,
                multiThread.noiseMapDatabaseParameters.exportAttenuationMatrix);
        if(multiThread.noiseMapDatabaseParameters.exportRaysMethod == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE &&
                multiThread.noiseMapDatabaseParameters.exportAttenuationMatrix) {
            CnossosPath cnossosPath = new CnossosPath(proPathParameters);
            cnossosPath.setTimePeriod(period);
            cnossosPaths.add(cnossosPath);
        }
        return attenuation;
    }

    /**
     * Update internal map with new attenuation
     * @param noiseLevel
     */
    private void processNoiseLevel(ReceiverNoiseLevel noiseLevel) {
        int keyToUpdate = UNKNOWN_SOURCE_ID;
        if(!dbSettings.isMergeSources()) {
            keyToUpdate = noiseLevel.source.sourceIndex;
        }
        TimePeriodParameters periodParameters = new TimePeriodParameters(
                dbSettings.isMergeSources() ? new PathFinder.SourcePointInfo() : noiseLevel.source,
                noiseLevel.period, noiseLevel.levels);

        receiverAttenuationList.merge(keyToUpdate, periodParameters,
                TimePeriodParameters::update);
    }

    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        cutProfileCount.addAndGet(1);
        PathSearchStrategy strategy = PathSearchStrategy.CONTINUE;
        final SceneWithEmission scene = multiThread.sceneWithEmission;
        CnossosPath cnossosPath = CnossosPathBuilder.computeCnossosPathFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, scene.defaultGroundAttenuation);
        if(cnossosPath != null) {
            multiThread.cnossosPathCount.addAndGet(1);
            CutPointSource source = cutProfile.getSource();
            CutPointReceiver receiver = cutProfile.getReceiver();

            long sourcePk = source.sourcePk == -1 ? source.id : source.sourcePk;

            // export path if required
            if(multiThread.noiseMapDatabaseParameters.exportRaysMethod == NoiseMapDatabaseParameters.ExportRaysMethods
                    .TO_RAYS_TABLE && !multiThread.noiseMapDatabaseParameters.exportAttenuationMatrix) {
                // Use only one ray as the ray is the same if we not keep absorption values
                // Copy path content in order to keep original ids for other method calls
                this.cnossosPaths.add(cnossosPath);
            }
            if(scene.wjSources.isEmpty()) {
                // No emission push only attenuation for each period
                if(!scene.cnossosParametersPerPeriod.isEmpty()) {
                    for (Map.Entry<String, AttenuationCnossosParameters> cnossosParametersEntry :
                            scene.cnossosParametersPerPeriod.entrySet()) {
                        double[] attenuation = dBToW(processAndStoreAttenuation(cnossosParametersEntry.getValue(),
                                cnossosPath, cnossosParametersEntry.getKey()));
                        ReceiverNoiseLevel receiverNoiseLevel =
                                new ReceiverNoiseLevel(new PathFinder.SourcePointInfo(source),
                                        new PathFinder.ReceiverPointInfo(receiver), cnossosParametersEntry.getKey(),
                                        attenuation);
                        processNoiseLevel(receiverNoiseLevel);
                    }
                } else {
                    double[] attenuation = dBToW(processAndStoreAttenuation(scene.defaultCnossosParameters, cnossosPath, ""));
                    ReceiverNoiseLevel receiverNoiseLevel =
                            new ReceiverNoiseLevel(new PathFinder.SourcePointInfo(source),
                                    new PathFinder.ReceiverPointInfo(receiver), "",
                                    attenuation);
                    processNoiseLevel(receiverNoiseLevel);
                }
            } else {
                // Apply period attenuation to emission for each time period covered by the source emission
                if(scene.wjSources.containsKey(sourcePk)) {
                    ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePk);
                    double[] minimalPowerForAllPeriods = new double[0];
                    double minimalPowerSum = Double.MAX_VALUE;
                    for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                        String period = periodEmission.period;
                        double [] attenuation = new double[0];
                        // look for specific atmospheric settings for this period
                        if(scene.cnossosParametersPerPeriod.containsKey(period)) {
                            attenuation = dBToW(processAndStoreAttenuation(scene.cnossosParametersPerPeriod.get(period),
                                    cnossosPath, period));
                        } else {
                            // None ? ok fallback to default settings
                            attenuation = dBToW(processAndStoreAttenuation(scene.defaultCnossosParameters,
                                    cnossosPath, ""));
                        }
                        double[] levels = multiplicationArray(attenuation, periodEmission.emission);
                        ReceiverNoiseLevel receiverNoiseLevel =
                                new ReceiverNoiseLevel(new PathFinder.SourcePointInfo(source),
                                        new PathFinder.ReceiverPointInfo(receiver), period, levels);
                        processNoiseLevel(receiverNoiseLevel);
                        if(dbSettings.maximumError > 0) {
                            double powerSum = sumArray(levels);
                            if(powerSum < minimalPowerSum) {
                                minimalPowerSum = powerSum;
                                minimalPowerForAllPeriods = levels;
                            }
                        }
                    }
                    if(dbSettings.maximumError > 0) {
                        // Add minimal power
                        addGlobalReceiverLevel(minimalPowerForAllPeriods);
                    }
                }
            }
            if(dbSettings.maximumError > 0 && !scene.wjSources.isEmpty()) {
                // power at receiver using minimal emission of all periods
                double minimalCurrentLevelAtReceiver = AcousticIndicatorsFunctions.wToDb(sumArray(wjAtReceiver));

                // replace unknown value of expected power for this source
                int sourceHashCode = source.coordinate.hashCode();
                if(maximumWjExpectedSplAtReceiver.containsKey(sourceHashCode)) {
                    sumMaximumRemainingWjExpectedSplAtReceiver -= maximumWjExpectedSplAtReceiver.get(sourceHashCode);
                    maximumWjExpectedSplAtReceiver.remove(sourceHashCode);
                }
                double maximumExpectedLevelInDb = AcousticIndicatorsFunctions.wToDb(sumArray(wjAtReceiver) + sumMaximumRemainingWjExpectedSplAtReceiver);
                double dBDiff = maximumExpectedLevelInDb - minimalCurrentLevelAtReceiver;
                if (dBDiff < dbSettings.maximumError) {
                    strategy = PathSearchStrategy.PROCESS_SOURCE_BUT_SKIP_RECEIVER;
                }
            }
        }
        return strategy;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList, AtomicInteger cutProfileCount) {
        this.cutProfileCount = cutProfileCount;
        wjAtReceiver = new double[0];
        // Quickly evaluate the maximum expected power level at receiver location
        // using all nearby sources maximum emission in reflective direct field
        if(dbSettings.getMaximumError() > 0 && !multiThread.sceneWithEmission.wjSources.isEmpty()) {
            maximumWjExpectedSplAtReceiver.clear();
            final SceneWithEmission scene = multiThread.sceneWithEmission;
            for (PathFinder.SourcePointInfo sourcePointInfo : sourceList) {
                int sourceHashCode = sourcePointInfo.getCoord().hashCode();
                double[] attenuation = dBToW(computeFastAttenuation(sourcePointInfo, receiver, scene.defaultCnossosParameters));
                if(scene.wjSources.containsKey(sourcePointInfo.sourcePk)) {
                    ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePointInfo.sourcePk);
                    for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                        double[] wjAtReceiver = multiplicationArray(attenuation, periodEmission.emission);
                        double sumPower = sumArray(wjAtReceiver);
                        double previousPowerAtLocation = maximumWjExpectedSplAtReceiver.getOrDefault(sourceHashCode, 0.0);
                        if(sumPower > previousPowerAtLocation) {
                            maximumWjExpectedSplAtReceiver.put(sourceHashCode, sumPower);
                        }
                    }
                }
            }
            sumMaximumRemainingWjExpectedSplAtReceiver = maximumWjExpectedSplAtReceiver.values()
                    .stream().reduce(Double::sum).orElse(0.0);
        }
    }

    /**
     * Pushes attenuation data into a concurrent linked deque.
     * @param stack Stack to feed
     * @param data receiver noise level in dB
     */
    public void pushInStack(ConcurrentLinkedDeque<ReceiverNoiseLevel> stack, ReceiverNoiseLevel data) {
        while(multiThread.resultsCache.queueSize.get() > dbSettings.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                multiThread.aborted.set(true);
                break;
            }
            if(multiThread.aborted.get()) {
                progressVisitor.cancel();
                return;
            }
        }
        stack.add(data);
        multiThread.resultsCache.queueSize.incrementAndGet();
    }

    /**
     * Adds Cnossos paths to a concurrent stack while maintaining the maximum stack size.
     * @param stack Stack to feed
     * @param data rays
     */
    public void pushInStack(ConcurrentLinkedDeque<CnossosPath> stack, Collection<CnossosPath> data) {
        while(multiThread.resultsCache.queueSize.get() > dbSettings.outputMaximumQueue) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                multiThread.aborted.set(true);
                break;
            }
            if(multiThread.aborted.get()) {
                progressVisitor.cancel();
                return;
            }
        }
        if(dbSettings.getMaximumRaysOutputCount() == 0 || multiThread.resultsCache.totalRaysInserted.get() < dbSettings.getMaximumRaysOutputCount()) {
            long newTotalRays = multiThread.resultsCache.totalRaysInserted.addAndGet(data.size());
            if(dbSettings.getMaximumRaysOutputCount() > 0 && newTotalRays > dbSettings.getMaximumRaysOutputCount()) {
                // too many rays, remove unwanted rays
                int newListSize = data.size() - (int)(newTotalRays - dbSettings.getMaximumRaysOutputCount());
                if(newListSize > 0) {
                    List<CnossosPath> subList = new ArrayList<CnossosPath>(newListSize);
                    for (CnossosPath pathParameters : data) {
                        subList.add(pathParameters);
                        if (subList.size() >= newListSize) {
                            break;
                        }
                    }
                    data = subList;
                } else {
                    data = Collections.emptyList();
                }
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
        if(!this.cnossosPaths.isEmpty()) {
            if(dbSettings.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE) {
                // Push propagation rays
                pushInStack(multiThread.resultsCache.cnossosPaths, this.cnossosPaths);
            }
        }
        // Convert to dB then pushed cached entries for this receiver into multi-thread instance
        for (Map.Entry<Integer, TimePeriodParameters> periodParametersEntry : receiverAttenuationList.entrySet()) {
            TimePeriodParameters periodParameters = periodParametersEntry.getValue();
            for (Map.Entry<String, double[]> levelsAtPeriod : periodParameters.levelsPerPeriod.entrySet()) {
                pushInStack(multiThread.resultsCache.receiverLevels, new ReceiverNoiseLevel(periodParameters.source,
                        receiver, levelsAtPeriod.getKey(),
                        AcousticIndicatorsFunctions.wToDb(levelsAtPeriod.getValue())));
            }
        }
        receiverAttenuationList.clear();
        maximumWjExpectedSplAtReceiver.clear();
        sumMaximumRemainingWjExpectedSplAtReceiver = 0;
        wjAtReceiver = new double[0];
        this.cnossosPaths.clear();
    }


    /**
     * representing the noise levels for different time periods.
     */
    public static class TimePeriodParameters {
        public PathFinder.SourcePointInfo source = null;
        /**
         * Map of attenuation (attenuation not in dB but w)
         */
        public Map<String, double[]> levelsPerPeriod = new HashMap<>();

        public TimePeriodParameters(PathFinder.SourcePointInfo source) {
            this.source = source;
        }

        public TimePeriodParameters(PathFinder.SourcePointInfo source, String period, double[] attenuation) {
            this.source = source;
            levelsPerPeriod.put(period, attenuation);
        }

        public TimePeriodParameters() {
        }

        /**
         * merge attenuation/noise level in w
         * @param other
         * @return
         */
        public TimePeriodParameters update(TimePeriodParameters other) {
            for (Map.Entry<String, double[]> entry : other.levelsPerPeriod.entrySet()) {
                levelsPerPeriod.merge(entry.getKey(), entry.getValue(),
                        AcousticIndicatorsFunctions::sumArray);
            }
            return this;
        }
    }
}
