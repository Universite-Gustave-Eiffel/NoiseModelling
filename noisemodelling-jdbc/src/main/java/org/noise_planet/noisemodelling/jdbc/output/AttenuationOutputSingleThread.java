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
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;

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
    PropagationModel propagationModel;
    public List<AttenuationOutput> attenuationOutputs = new ArrayList<>();

    /**
     * Collected attenuation/noise level on the current receiver
     */
    Map<Integer, TimePeriodParameters> receiverAttenuationList = new HashMap<>();

    /**
     * MaxError DB Processing variable
     * Current, power at receiver, only used to stop looking for far sources
     */
    Map<String, Double> wjAtReceiver = new HashMap<>();

    /**
     * MaxError DB Processing variable
     * Favourable Free Field cumulated global power at receiver, only used to stop looking for far sources
     * Key unique source point identifier
     * Value maximum expected noise level in w
     */
    Map<String, HashMap<SourcePointKey, Double>> maximumWjExpectedSplAtReceiver = new HashMap<>();

    public AtomicInteger cutProfileCount = new AtomicInteger(0);

    ProgressVisitor progressVisitor;

    /**
     * Constructs a AttenuationOutputSingleThread object with a multithreaded parent
     * AttenuationOutputMultiThread instance.
     * This class is not thread-safe
     *
     * @param multiThreadParent multi thread cell computation manager
     * @param progressVisitor progress information
     */
    public AttenuationOutputSingleThread(AttenuationOutputMultiThread multiThreadParent, ProgressVisitor progressVisitor) {
        this.multiThread = multiThreadParent;
        this.dbSettings = multiThreadParent.noiseMapDatabaseParameters;
        this.progressVisitor = progressVisitor;
    }

    /**
     * The maximumError shortcut stops the path finder before all farther sources are visited.
     * When rays are explicitly exported, users expect the rays table to describe the complete
     * propagation search, so the shortcut must stay disabled for that diagnostic output.
     */
    private boolean isMaximumErrorPruningEnabled() {
        return dbSettings.maximumError > 0 &&
                dbSettings.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.NONE;
    }

    /**
     * Compute the attenuation for a given geometrical cross-section and period and store the
     * results.
     *
     * @param cutProfile geometrical cross-section
     * @param data attenuation computation parameters
     * @param period period identifier
     * @param emission source emission levels for the period
     * @param sourcePk source identifier
     * @return path search strategy
     */
    private PathSearchStrategy processAndStoreAttenuation(CutProfile cutProfile, AttenuationParameters data,
                                                          String period, double[] emission, long sourcePk) {
        return processAndStoreAttenuation(cutProfile, data, period, emission, sourcePk, new ArrayList<>());
    }

    /**
     * Compute the attenuation for a given geometrical cross-section and period and store the
     * results.
     *
     * @param cutProfile geometrical cross-section
     * @param data attenuation computation parameters
     * @param period period identifier
     * @param emission source emission levels for the period
     * @param sourcePk source identifier
     * @param defaultAttenuation attenuation computed with default parameters
     * @return path search strategy
     */
    private PathSearchStrategy processAndStoreAttenuation(CutProfile cutProfile, AttenuationParameters data,
                                                          String period, double[] emission, long sourcePk,
                                                          List<AttenuationOutput> defaultAttenuation) {
        PathSearchStrategy strategy = PathSearchStrategy.CONTINUE;
        final SceneWithEmission scene = multiThread.sceneWithEmission;
        // Avoid multiple attenuation computation with default attenuation parameters
        List<AttenuationOutput> attenuationList;
        if(!defaultAttenuation.isEmpty()){
            attenuationList = defaultAttenuation;
        } else {
            attenuationList = propagationModel.computeAttenuation(scene, cutProfile, data,
                    multiThread.noiseMapDatabaseParameters.exportAttenuationMatrix);
            // export attenuation output per period if required
            if(multiThread.noiseMapDatabaseParameters.exportRaysMethod == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE &&
                    multiThread.noiseMapDatabaseParameters.exportAttenuationMatrix) {
                for (AttenuationOutput attenuationOutput : attenuationList) {
                    attenuationOutput.setTimePeriod(period);
                    this.attenuationOutputs.add(attenuationOutput);
                }
            }
            defaultAttenuation.addAll(attenuationList);
        }
        // export attenuation output (only the rays/propagation path export is requested)
        if(multiThread.noiseMapDatabaseParameters.exportRaysMethod == NoiseMapDatabaseParameters.ExportRaysMethods
                .TO_RAYS_TABLE && this.attenuationOutputs.isEmpty()) {
            // Use only one ray as the ray is the same if we not keep absorption values
            this.attenuationOutputs.addAll(attenuationList);
        }
        for (AttenuationOutput attenuationOutput : attenuationList) {
            double[] attenuationDb = attenuationOutput.getaGlobal();
            double[] attenuation = dBToW(attenuationDb);
            double[] levels;
            if(emission.length != 0 ) {
                levels = multiplicationArray(attenuation, emission);
                if (isMaximumErrorPruningEnabled()) {
                    double powerSum = sumArray(levels);
                    wjAtReceiver.merge(period, powerSum, Double::sum);
                }
            } else {
                levels = attenuation;
            }
            CutPointSource source = cutProfile.getSource();
            CutPointReceiver receiver = cutProfile.getReceiver();
            ReceiverNoiseLevel receiverNoiseLevel =
                    new ReceiverNoiseLevel(new PathFinder.SourcePointInfo(source),
                            new PathFinder.ReceiverPointInfo(receiver), period,
                            levels);
            processNoiseLevel(receiverNoiseLevel);

            // To reduce the computation time, we evaluate the potential remaining power
            // at the receiver and stop processing further sources if we are already close enough to
            // the expected final level at the receiver (if maximumError is defined in dbSettings)
            if(isMaximumErrorPruningEnabled() && scene.wjSources.containsKey(sourcePk)) {
                boolean keepRunning = false;
                // Update remaining expected max power for each source period.
                // We remove the currently processed source point from the precomputed budget.
                ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePk);
                SourcePointKey sourcePointKey = new SourcePointKey(source);
                for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                    final String periodLabel = periodEmission.period;
                    if (maximumWjExpectedSplAtReceiver.containsKey(periodLabel)) {
                        maximumWjExpectedSplAtReceiver.get(periodLabel).remove(sourcePointKey);
                        if (maximumWjExpectedSplAtReceiver.get(periodLabel).isEmpty()) {
                            maximumWjExpectedSplAtReceiver.remove(periodLabel);
                        }
                    }
                }
                for (Map.Entry<String, Double> entry : wjAtReceiver.entrySet()) {
                    final double levelAtReceiver = entry.getValue();

                    if (!maximumWjExpectedSplAtReceiver.containsKey(period)) {
                        // Nothing to evaluate here, as there is no expected further power for this period.
                        continue;
                    }

                    // Evaluate the current noise level at receiver compared to the final
                    // expected noise level at the receiver.
                    double nonProcessedPower = maximumWjExpectedSplAtReceiver.get(period).values().stream()
                            .reduce(Double::sum).orElse(0.0);
                    double maximumExpectedLevelInDb = AcousticIndicatorsFunctions.wToDb(levelAtReceiver + nonProcessedPower);
                    double dBDiff = maximumExpectedLevelInDb - wToDb(levelAtReceiver);
                    if (dBDiff > dbSettings.maximumError) {
                        // For this period we still expect to see some significant sources further away.
                        keepRunning = true;
                        break;
                    }
                }
                if(!keepRunning) {
                    strategy = PathSearchStrategy.PROCESS_SOURCE_BUT_SKIP_RECEIVER;
                }
            }
        }
        return strategy;
    }

    /**
     * Update internal map with new attenuation
     * @param noiseLevel receiver noise level
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

    /**
     * Manage attenuation computation each time a cutProfile is found.
     * Note: in the case of CNOSSOS propagation model, a new instance of PropagationModel needs to be
     * created for each cutProfile to ensure a new computation of the cnossosPaths.
     *
     * @param cutProfile vertical profile
     * @return Search strategy
     */
    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        // Create a PropagationModel instance
        propagationModel = multiThread.propagationModelCreator.create();
        PathSearchStrategy strategy = PathSearchStrategy.CONTINUE;
        multiThread.cutProfileCount.addAndGet(1);
        final SceneWithEmission scene = multiThread.sceneWithEmission;
        if(scene.getCloseReceiverReflectionWallDistance() > 0
                && cutProfile.hasCloseReflectionBeforeReceiver(scene.getCloseReceiverReflectionWallDistance())) {
            return strategy;
        }
        CutPointSource source = cutProfile.getSource();
        long sourcePk = source.sourcePk == -1 ? source.id : source.sourcePk;
        if(scene.wjSources.isEmpty()) {
            // No emission push only attenuation for each period
            if(!scene.cnossosParametersPerPeriod.isEmpty()) {
                for (Map.Entry<String, AttenuationParameters> propagationParametersEntry :
                        scene.cnossosParametersPerPeriod.entrySet()) {
                    strategy = processAndStoreAttenuation(cutProfile, propagationParametersEntry.getValue(),
                            propagationParametersEntry.getKey(), new double[0], sourcePk);
                }
            } else {
                strategy = processAndStoreAttenuation(cutProfile, scene.defaultCnossosParameters,
                        "", new double[0], sourcePk);
            }
        } else {
            // Apply period attenuation to emission for each time period covered by the source emission
            if(scene.wjSources.containsKey(sourcePk)) {
                ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePk);
                List<AttenuationOutput>  defaultAttenuation = new ArrayList<>();
                for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                    String period = periodEmission.period;
                    // look for specific atmospheric settings for this period
                    if(scene.cnossosParametersPerPeriod.containsKey(period)) {
                        strategy = processAndStoreAttenuation(cutProfile,
                                scene.cnossosParametersPerPeriod.get(period), period,
                                periodEmission.emission, sourcePk);
                    } else {
                        strategy = processAndStoreAttenuation(cutProfile, scene.defaultCnossosParameters,
                                period, periodEmission.emission, sourcePk, defaultAttenuation);
                    }
                }
            }
        }
        return strategy;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList,
            AtomicInteger cutProfileCount) {
        this.cutProfileCount = cutProfileCount;
        // Create a PropagationModel instance
        propagationModel = multiThread.propagationModelCreator.create();
        // Quickly evaluate the maximum expected power level at receiver location
        // using all nearby sources maximum emission in reflective direct field
        if(isMaximumErrorPruningEnabled() && !multiThread.sceneWithEmission.wjSources.isEmpty()) {
            wjAtReceiver = new HashMap<>(multiThread.sceneWithEmission.periodSet.size());
            for (String period : multiThread.sceneWithEmission.periodSet) {
                wjAtReceiver.put(period, 0.0);
            }
            maximumWjExpectedSplAtReceiver.clear();

            final SceneWithEmission scene = multiThread.sceneWithEmission;
            for (PathFinder.SourcePointInfo sourcePointInfo : sourceList) {
                // Create a fake CutProfile with direct field view between source and receiver
                double[] attenuation = dBToW(propagationModel.computeDirectAttenuation(sourcePointInfo, receiver,
                        scene, scene.defaultCnossosParameters,false).getaGlobal());
                // For line source apply a gain on the attenuation
                if(sourcePointInfo.li > 1) {
                    attenuation = multiplicationArray(attenuation, sourcePointInfo.li);
                }
                if(scene.wjSources.containsKey(sourcePointInfo.sourcePk)) {
                    ArrayList<SceneWithEmission.PeriodEmission> emissions = scene.wjSources.get(sourcePointInfo.sourcePk);
                    for (SceneWithEmission.PeriodEmission periodEmission : emissions) {
                        // Use period-specific attenuation settings when available so the remaining-power
                        // budget matches the actual period being evaluated by the maxError algorithm.
                        AttenuationParameters parameters = scene.cnossosParametersPerPeriod.getOrDefault(
                                periodEmission.period, scene.defaultCnossosParameters);
                        double[] attenuationPerPeriod = attenuation;
                        if(parameters != scene.defaultCnossosParameters) {
                            attenuationPerPeriod = dBToW(propagationModel.computeDirectAttenuation(sourcePointInfo,
                                    receiver, scene, parameters,false).getaGlobal());
                            if(sourcePointInfo.li > 1) {
                                attenuationPerPeriod = multiplicationArray(attenuationPerPeriod, sourcePointInfo.li);
                            }
                        }
                        double[] wjAtReceiver = multiplicationArray(attenuationPerPeriod, periodEmission.emission);
                        double sumPower = sumArray(wjAtReceiver);
                        HashMap<SourcePointKey, Double> sourceLevel;
                        if(!maximumWjExpectedSplAtReceiver.containsKey(periodEmission.period)) {
                            sourceLevel = new HashMap<>();
                            maximumWjExpectedSplAtReceiver.put(periodEmission.period, sourceLevel);
                        } else {
                            sourceLevel = maximumWjExpectedSplAtReceiver.get(periodEmission.period);
                        }
                        sourceLevel.merge(new SourcePointKey(sourcePointInfo), sumPower, Double::sum);
                    }
                }
            }
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
    public void pushInStack(ConcurrentLinkedDeque<AttenuationOutput> stack, List<AttenuationOutput> data) {
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
                    data = new ArrayList<>(data.subList(0, newListSize));
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
     * @param receiver attributes of the receiver point
     */
    @Override
    public void finalizeReceiver(PathFinder.ReceiverPointInfo receiver) {
        // Push propagation rays (only in case of Cnossos propagation model)
        if(!this.attenuationOutputs.isEmpty()) {
            if(dbSettings.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE) {
                pushInStack(multiThread.resultsCache.attenuationOutputs, this.attenuationOutputs);
            }
        }
        // Convert to dB then pushed cached entries for this receiver into multi-thread instance
        boolean computeLden = isComputeLden();
        Set<String> collectedPeriod = new HashSet<>();
        for (Map.Entry<Integer, TimePeriodParameters> periodParametersEntry : receiverAttenuationList.entrySet()) {
            TimePeriodParameters periodParameters = periodParametersEntry.getValue();
            for (Map.Entry<String, double[]> levelsAtPeriod : periodParameters.levelsPerPeriod.entrySet()) {
                pushInStack(multiThread.resultsCache.receiverLevels, new ReceiverNoiseLevel(periodParameters.source,
                        receiver, levelsAtPeriod.getKey(),
                        AcousticIndicatorsFunctions.wToDb(levelsAtPeriod.getValue())));
                if(dbSettings.isMergeSources()) {
                    collectedPeriod.add(levelsAtPeriod.getKey());
                }
            }
            if(computeLden) {
                double[] lden = new double[0];
                for (EmissionTableGenerator.STANDARD_PERIOD period : EmissionTableGenerator.STANDARD_PERIOD.values()) {
                    double[] levels = periodParameters.levelsPerPeriod.getOrDefault(
                            EmissionTableGenerator.STANDARD_PERIOD_VALUE[period.ordinal()], new double[0]);
                    // Apply period gain
                    lden = AcousticIndicatorsFunctions.sumArray(lden,
                            AcousticIndicatorsFunctions.multiplicationArray(levels,
                                    EmissionTableGenerator.RATIOS[period.ordinal()]));
                }
                pushInStack(multiThread.resultsCache.receiverLevels, new ReceiverNoiseLevel(periodParameters.source,
                        receiver, EmissionTableGenerator.DEN_PERIOD,
                        AcousticIndicatorsFunctions.wToDb(lden)));
                if(dbSettings.isMergeSources()) {
                    collectedPeriod.add(EmissionTableGenerator.DEN_PERIOD);
                }
            }
        }
        if (dbSettings.isMergeSources()) {
            // If Merge source is activated, the following code will push empty values (-99dB) when no rays have reached the receiver
            Set<String> difference = new HashSet<>(multiThread.sceneWithEmission.periodSet);
            if(computeLden) {
                difference.add(EmissionTableGenerator.DEN_PERIOD);
            }
            difference.removeAll(collectedPeriod);
            // add missing periods levels for this receiver
            double[] levels = new double[multiThread.sceneWithEmission.profileBuilder.frequencyArray.size()];
            Arrays.fill(levels, dbSettings.noSourceNoiseLevel);
            for (String period : difference) {
                pushInStack(multiThread.resultsCache.receiverLevels,
                        new ReceiverNoiseLevel(new PathFinder.SourcePointInfo(), receiver, period, levels));
            }
        }
        receiverAttenuationList.clear();
        maximumWjExpectedSplAtReceiver.clear();
        wjAtReceiver.clear();
        this.attenuationOutputs.clear();
    }

    private boolean isComputeLden() {
        SceneDatabaseInputSettings.INPUT_MODE inputMode =
                multiThread.sceneWithEmission.sceneDatabaseInputSettings.getInputMode();

        // Some input use convention source Day Evening and Night, and the expected result must also include
        // DEN which is a special mix of the three periods
        return inputMode.equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW_DEN) ||
                inputMode.equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
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
         * @param other noise level as a function of the period
         * @return noise level as a function of the period
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
