/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;


import java.io.File;

/**
 * Global configuration of NoiseModelling computation based on database data
 * This is input only, these settings are never updated by org.noise_planet.noisemodelling.jdbc class
 */
public class NoiseMapDatabaseParameters {
    public boolean exportAttenuationMatrix;
    public static final String DEFAULT_RECEIVERS_LEVEL_TABLE_NAME = "RECEIVERS_LEVEL";
    /**
     * Noise level on the receiver for each period if mergeSources is true and no sound source were found
     */
    public double noSourceNoiseLevel = -99;

    public NoiseMapDatabaseParameters() {
    }

    /**
     * Path to write the computation time and other statistics in a csv file
     */
    public File CSVProfilerOutputPath = null;
    /**
     * Create a new csv line after this time in seconds
     */
    public int CSVProfilerWriteInterval = 60;

    /**
     * With attenuation export also the json of the related cnossos path, for debugging purpose
     */
    public boolean exportCnossosPathWithAttenuation = false;
    public boolean keepAbsorption = false; // in rays, keep store detailed absorption data
    public int maximumRaysOutputCount = 0; // if export rays, do not keep more than this number of rays (0 infinite)

    public enum ExportRaysMethods {TO_RAYS_TABLE, NONE}
    public ExportRaysMethods exportRaysMethod = ExportRaysMethods.NONE;
    /** Cnossos revisions have multiple coefficients for road emission formulae
     * this parameter will be removed when the final version of Cnossos will be published
     */
    public int coefficientVersion = 2;

    // Output config

    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = 0;

    public int geojsonColumnSizeLimit = 1000000; // sql column size limitation for geojson

    public int getMaximumRaysOutputCount() {
        return maximumRaysOutputCount;
    }
    public int outputMaximumQueue = 50000;

    public boolean mergeSources = true;

    public String receiversLevelTable = DEFAULT_RECEIVERS_LEVEL_TABLE_NAME;
    public String raysTable = "RAYS";

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


    public void setExportCnossosPathWithAttenuation(boolean exportCnossosPathWithAttenuation) {
        this.exportCnossosPathWithAttenuation = exportCnossosPathWithAttenuation;
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
     * @return Output table with noise level per receiver/source
     */
    public String getReceiversLevelTable() {
        return receiversLevelTable;
    }

    /**
     * @param receiversLevelTable Output table with noise level per receiver/source
     */
    public void setReceiversLevelTable(String receiversLevelTable) {
        this.receiversLevelTable = receiversLevelTable;
    }
}
