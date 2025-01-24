/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;


import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of NoiseModelling computation based on database data using standard Lden outputs
 */
public class NoiseMapParameters {
    public boolean exportAttenuationMatrix;

    public NoiseMapParameters(INPUT_MODE inputMode) {
        input_mode = inputMode;
    }

    public enum INPUT_MODE { INPUT_MODE_TRAFFIC_FLOW, INPUT_MODE_LW_DEN, INPUT_MODE_PROBA}
    public final INPUT_MODE input_mode;
    public boolean exportProfileInRays = false;
    public boolean keepAbsorption = false; // in rays, keep store detailed absorption data
    public int maximumRaysOutputCount = 0; // if export rays, do not keep more than this number of rays (0 infinite)
    // Environmental propagation conditions for each time period
    public Map<String, AttenuationCnossosParameters> attenuationCnossosParametersMap = new HashMap<>();
    /**
     * If a time period do not specify the propagation meteorological conditions, use this default settings
     */
    public AttenuationCnossosParameters defaultAttenuationCnossosParameters;

    public enum ExportRaysMethods {TO_RAYS_TABLE, TO_MEMORY, NONE}
    public ExportRaysMethods exportRaysMethod = ExportRaysMethods.NONE;
    /** Cnossos revisions have multiple coefficients for road emission formulae
     * this parameter will be removed when the final version of Cnossos will be published
     */
    public int coefficientVersion = 2;

    // Process status
    public boolean exitWhenDone = false;
    public boolean aborted = false;

    // Output config

    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = 0;

    public int geojsonColumnSizeLimit = 1000000; // sql column size limitation for geojson

    public int getMaximumRaysOutputCount() {
        return maximumRaysOutputCount;
    }
    public int outputMaximumQueue = 50000;

    public boolean mergeSources = true;

    public String receiversLevelTable = "RECEIVERS_LEVEL";
    public String raysTable = "RAYS";

    public String lwFrequencyPrepend = "LW";

    public File sqlOutputFile;
    public Boolean sqlOutputFileCompression = true;
    public Boolean dropResultsTable = true;
    public boolean computeLAEQOnly = false;

    /**
     * If true the position of the receiver (with the altitude if available) will be exported into the results tables
     */
    public boolean exportReceiverPosition = false;

    /**
     * @return If true the position of the receiver (with the altitude if available) will be exported into the results
     * tables
     */
    public boolean isExportReceiverPosition() {
        return exportReceiverPosition;
    }

    /**
     * @param exportReceiverPosition If true the position of the receiver (with the altitude if available) will be
     *                               exported into the results tables
     */
    public void setExportReceiverPosition(boolean exportReceiverPosition) {
        this.exportReceiverPosition = exportReceiverPosition;
    }

    /**
     * @param maximumRaysOutputCount if export rays, do not keep more than this number of rays per computation area (0 infinite)
     */
    public void setMaximumRaysOutputCount(int maximumRaysOutputCount) {
        this.maximumRaysOutputCount = maximumRaysOutputCount;
    }

    public boolean isComputeLAEQOnly() {
        return computeLAEQOnly;
    }

    public void setComputeLAEQOnly(boolean computeLAEQOnly) {
        this.computeLAEQOnly = computeLAEQOnly;
    }

    /**
     * If a time period do not specify the propagation meteorological conditions, use this default settings
     * @param defaultAttenuationCnossosParameters propagation meteorological conditions
     */
    public void setDefaultAttenuationCnossosParameters(AttenuationCnossosParameters defaultAttenuationCnossosParameters) {
        this.defaultAttenuationCnossosParameters = defaultAttenuationCnossosParameters;
    }

    /**
     * Retrieves the propagation process path data for the specified time period.
     * @param timePeriod the time period for which to retrieve the propagation process path data.
     * @return the attenuation Cnossos parameters for the specified time period.
     */
    public AttenuationCnossosParameters getPropagationProcessPathData(String timePeriod) {
        if(attenuationCnossosParametersMap.containsKey(timePeriod)) {
            return attenuationCnossosParametersMap.get(timePeriod);
        } else {
            return defaultAttenuationCnossosParameters;
        }
    }

    /**
     * Sets the propagation process path data for the specified time period.
     * @param timePeriod the time period for which to set the propagation process path data.
     * @param attenuationCnossosParameters the attenuation Cnossos parameters to set.
     */
    public void setPropagationProcessPathData(String timePeriod, AttenuationCnossosParameters attenuationCnossosParameters) {
        attenuationCnossosParametersMap.put(timePeriod, attenuationCnossosParameters);
    }

    public ExportRaysMethods getExportRaysMethod() {
        return exportRaysMethod;
    }

    /**
     * Export rays in table (beware this could take a lot of storage space) or keep on memory or do not keep
     * @param exportRaysMethod
     */
    public void setExportRaysMethod(ExportRaysMethods exportRaysMethod) {
        this.exportRaysMethod = exportRaysMethod;
    }


    public void setExportProfileInRays(boolean exportProfileInRays) {
        this.exportProfileInRays = exportProfileInRays;
    }

    public boolean isKeepAbsorption() {
        return keepAbsorption;
    }

    /**
     * @param exportAttenuationMatrix If true store absorption values in propagation path objects
     * @see #setExportAttenuationMatrix(boolean)
     */
    public void setExportAttenuationMatrix(boolean exportAttenuationMatrix) {
        this.exportAttenuationMatrix = exportAttenuationMatrix;
    }

    /**
     * @param coefficientVersion Cnossos revisions have multiple coefficients for road emission formulae this parameter
     *                          will be removed when the final version of Cnossos will be published
     */
    public void setCoefficientVersion(int coefficientVersion) {
        this.coefficientVersion = coefficientVersion;
    }

    public int getCoefficientVersion() {
        return coefficientVersion;
    }

    /**
     * Maximum result stack to be inserted in database
     * if the stack is full, the computation core is waiting
     * @param outputMaximumQueue Maximum number of elements in stack
    */
    public void setOutputMaximumQueue(int outputMaximumQueue) {
        this.outputMaximumQueue = outputMaximumQueue;
    }

    /**
     * @return maximum dB Error, stop calculation if the maximum sum of further sources contributions are smaller than this value
     */
    public double getMaximumError() {
        return maximumError;
    }

    /**
     * @param maximumError maximum dB Error, stop calculation if the maximum sum of further sources contributions
     *                    compared to the current level at the receiver position are smaller than this value
     */
    public void setMaximumError(double maximumError) {
        this.maximumError = maximumError;
    }

    public void setMergeSources(boolean mergeSources) {
        this.mergeSources = mergeSources;
    }

    /**
     * @return Table name that contains rays dump (profile)
     */
    public String getRaysTable() {
        return raysTable;
    }

    /**
     */
    public void setRaysTable(String raysTable) {
        this.raysTable = raysTable;
    }

    public boolean isMergeSources() {
        return mergeSources;
    }

    /**
     * representing the noise levels for different time periods.
     */
    public static class TimePeriodParameters {
        public PathFinder.SourcePointInfo source = null;
        public PathFinder.ReceiverPointInfo receiver = null;
        public String period = "";
        public double [] levels = new double[0];

        public TimePeriodParameters(PathFinder.SourcePointInfo source,
                                    PathFinder.ReceiverPointInfo receiver,
                                    String period,
                                    double[] levels) {
            this.levels = levels;
            this.period = period;
            this.receiver = receiver;
            this.source = source;
        }

        public TimePeriodParameters() {
        }
    }
}
