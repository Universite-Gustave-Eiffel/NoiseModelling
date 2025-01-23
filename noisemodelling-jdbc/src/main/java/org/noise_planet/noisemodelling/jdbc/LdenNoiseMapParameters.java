/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;


import org.checkerframework.checker.units.qual.A;
import org.h2gis.utilities.JDBCUtilities;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Configuration of NoiseModelling computation based on database data using standard Lden outputs
 */
public class LdenNoiseMapParameters extends NoiseMapParameters {
    public enum TIME_PERIOD {DAY, EVENING, NIGHT}
    // This field is initialised when {@link PointNoiseMap#initialize} is called
    AttenuationCnossosParameters attenuationCnossosParametersDay = null;
    AttenuationCnossosParameters attenuationCnossosParametersEvening = null;
    AttenuationCnossosParameters attenuationCnossosParametersNight = null;

    // Output config
    boolean computeLDay = true;
    boolean computeLEvening = true;
    boolean computeLNight = true;
    boolean computeLDEN = true;

    String lDayTable = "LDAY_RESULT";
    String lEveningTable = "LEVENING_RESULT";
    String lNightTable = "LNIGHT_RESULT";
    String lDenTable = "LDEN_RESULT";

    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = 0;

    public enum INPUT_MODE { INPUT_MODE_TRAFFIC_FLOW, INPUT_MODE_LW_DEN, INPUT_MODE_PROBA}

    final INPUT_MODE input_mode;

    public LdenNoiseMapParameters(INPUT_MODE inputMode) {
        input_mode = inputMode;
    }

    /**
     * Initialize the settings according to input tables
     * @param connection Active JDBC connection
     * @param ldenNoiseMapLoader
     * @throws SQLException
     */
    public void initialize(Connection connection, LdenNoiseMapLoader ldenNoiseMapLoader) throws SQLException {
        if(input_mode == LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Fetch source fields
            List<String> sourceField = JDBCUtilities.getColumnNames(connection, ldenNoiseMapLoader.getSourcesTableName());
            String period = "";
            if (computeLDay || computeLDEN) {
                period = "D";
            } else if (computeLEvening) {
                period = "E";
            } else if (computeLNight) {
                period = "N";
            }
            String freqField = lwFrequencyPrepend + period;
            frequencyArray = new ArrayList<>();
            exactFrequencyArray = new ArrayList<>();
            aWeightingArray = new ArrayList<>();
            if (!period.isEmpty()) {
                for (String fieldName : sourceField) {
                    if (fieldName.toUpperCase(Locale.ROOT).startsWith(freqField)) {
                        int freq = Integer.parseInt(fieldName.substring(freqField.length()));
                        int index = Arrays.binarySearch(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE, freq);
                        if (index >= 0) {
                            frequencyArray.add(freq);
                        }
                    }
                }
            }
            if(frequencyArray.isEmpty()) {
                throw new SQLException("Source table "+ ldenNoiseMapLoader.getSourcesTableName()+" does not contains any frequency bands");
            }

            ProfileBuilder.initializeFrequencyArrayFromReference(frequencyArray, exactFrequencyArray, aWeightingArray);

            // Instance of PropagationProcessPathData maybe already set
            for(LdenNoiseMapParameters.TIME_PERIOD timePeriod : LdenNoiseMapParameters.TIME_PERIOD.values()) {
                if (ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod) == null) {
                    AttenuationCnossosParameters attenuationCnossosParameters = new AttenuationCnossosParameters(frequencyArray, exactFrequencyArray, aWeightingArray);
                    setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                    ldenNoiseMapLoader.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                } else {
                    ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod).setFrequencies(frequencyArray);
                    ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod).setFrequenciesExact(exactFrequencyArray);
                    ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod).setFrequenciesAWeighting(aWeightingArray);
                    setPropagationProcessPathData(timePeriod, ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod));
                }
            }
        } else {
            for(LdenNoiseMapParameters.TIME_PERIOD timePeriod : LdenNoiseMapParameters.TIME_PERIOD.values()) {
                if (ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod) == null) {
                    // Traffic flow cnossos frequencies are octave bands from 63 to 8000 Hz
                    AttenuationCnossosParameters attenuationCnossosParameters = new AttenuationCnossosParameters(false);
                    setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                    ldenNoiseMapLoader.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                } else {
                    setPropagationProcessPathData(timePeriod, ldenNoiseMapLoader.getPropagationProcessPathData(timePeriod));
                }
            }
        }
    }

    public int getMaximumRaysOutputCount() {
        return maximumRaysOutputCount;
    }

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
     * Retrieves the propagation process path data for the specified time period.
     * @param time_period the time period for which to retrieve the propagation process path data.
     * @return the attenuation Cnossos parameters for the specified time period.
     */
    public AttenuationCnossosParameters getPropagationProcessPathData(TIME_PERIOD time_period) {
        switch (time_period) {
            case DAY:
                return attenuationCnossosParametersDay;
            case EVENING:
                return attenuationCnossosParametersEvening;
            default:
                return attenuationCnossosParametersNight;
        }
    }

    /**
     * Sets the propagation process path data for the specified time period.
     * @param time_period the time period for which to set the propagation process path data.
     * @param attenuationCnossosParameters the attenuation Cnossos parameters to set.
     */
    public void setPropagationProcessPathData(TIME_PERIOD time_period, AttenuationCnossosParameters attenuationCnossosParameters) {
        switch (time_period) {
            case DAY:
                attenuationCnossosParametersDay = attenuationCnossosParameters;
            case EVENING:
                attenuationCnossosParametersEvening = attenuationCnossosParameters;
            default:
                attenuationCnossosParametersNight = attenuationCnossosParameters;
        }
    }

    public void setComputeLDay(boolean computeLDay) {
        this.computeLDay = computeLDay;
    }

    public void setComputeLEvening(boolean computeLEvening) {
        this.computeLEvening = computeLEvening;
    }

    public void setComputeLNight(boolean computeLNight) {
        this.computeLNight = computeLNight;
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
     * @param computeLDEN IF true create LDEN_GEOM table
     */
    public void setComputeLDEN(boolean computeLDEN) {
        this.computeLDEN = computeLDEN;
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

    public void setlDayTable(String lDayTable) {
        this.lDayTable = lDayTable;
    }

    public void setlEveningTable(String lEveningTable) {
        this.lEveningTable = lEveningTable;
    }

    /**
     */
    public void setlNightTable(String lNightTable) {
        this.lNightTable = lNightTable;
    }

    public void setlDenTable(String lDenTable) {
        this.lDenTable = lDenTable;
    }

    public String getlDayTable() {
        return lDayTable;
    }

    public String getlEveningTable() {
        return lEveningTable;
    }

    public String getlNightTable() {
        return lNightTable;
    }

    public String getlDenTable() {
        return lDenTable;
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

    public boolean isComputeLDay() {
        return computeLDay;
    }

    public boolean isComputeLEvening() {
        return computeLEvening;
    }

    public boolean isComputeLNight() {
        return computeLNight;
    }

    public boolean isComputeLDEN() {
        return computeLDEN;
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
        public double [] dayLevels = new double[0];
        public double [] eveningLevels = new double[0];
        public double [] nightLevels = new double[0];

        public TimePeriodParameters(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                    double[] dayLevels, double[] eveningLevels, double[] nightLevels) {
            this.source = source;
            this.receiver = receiver;
            this.dayLevels = dayLevels;
            this.eveningLevels = eveningLevels;
            this.nightLevels = nightLevels;
        }

        public TimePeriodParameters() {
        }
    }
}
