/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */
package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.io.File;

/**
 * Configuration of NoiseModelling computation based on database data using standard Lden outputs
 */
public class LDENConfig {
    public enum TIME_PERIOD {TIME_PERIOD_DAY, TIME_PERIOD_EVENING, TIME_PERIOD_NIGHT}
    public enum INPUT_MODE { INPUT_MODE_TRAFFIC_FLOW, INPUT_MODE_LW_DEN, INPUT_MODE_PROBA}
    final INPUT_MODE input_mode;

    // This field is initialised when {@link PointNoiseMap#initialize} is called
    PropagationProcessPathData propagationProcessPathDataDay = null;
    PropagationProcessPathData propagationProcessPathDataEvening = null;
    PropagationProcessPathData propagationProcessPathDataNight = null;

    // Cnossos revisions have multiple coefficients for road emission formulae
    // this parameter will be removed when the final version of Cnossos will be published
    int coefficientVersion = 2;

    // Process status
    boolean exitWhenDone = false;
    boolean aborted = false;

    // Output config
    boolean computeLDay = true;
    boolean computeLEvening = true;
    boolean computeLNight = true;
    boolean computeLDEN = true;
    public int geojsonColumnSizeLimit = 1000000; // sql column size limitation for geojson

    public boolean isComputeLAEQOnly() {
        return computeLAEQOnly;
    }

    public void setComputeLAEQOnly(boolean computeLAEQOnly) {
        this.computeLAEQOnly = computeLAEQOnly;
    }

    boolean computeLAEQOnly = false;

    public enum ExportRaysMethods {TO_RAYS_TABLE, TO_MEMORY, NONE}
    ExportRaysMethods exportRaysMethod = ExportRaysMethods.NONE;

    boolean exportProfileInRays = false;
    boolean keepAbsorption = false; // in rays, keep store detailed absorption data
    // Maximum result stack to be inserted in database
    // if the stack is full, the computation core is waiting
    int outputMaximumQueue = 50000;

    boolean mergeSources = true;

    String lDayTable = "LDAY_RESULT";
    String lEveningTable = "LEVENING_RESULT";
    String lNightTable = "LNIGHT_RESULT";
    String lDenTable = "LDEN_RESULT";
    String raysTable = "RAYS";

    String lwFrequencyPrepend = "LW";

    File sqlOutputFile;
    Boolean sqlOutputFileCompression = true;
    Boolean dropResultsTable = true;

    public LDENConfig(INPUT_MODE input_mode) {
        this.input_mode = input_mode;
    }


    public PropagationProcessPathData getPropagationProcessPathData(TIME_PERIOD time_period) {
        switch (time_period) {
            case TIME_PERIOD_DAY:
                return propagationProcessPathDataDay;
            case TIME_PERIOD_EVENING:
                return propagationProcessPathDataEvening;
            default:
                return propagationProcessPathDataNight;
        }
    }

    public void setPropagationProcessPathData(TIME_PERIOD time_period, PropagationProcessPathData propagationProcessPathData) {
        switch (time_period) {
            case TIME_PERIOD_DAY:
                propagationProcessPathDataDay = propagationProcessPathData;
            case TIME_PERIOD_EVENING:
                propagationProcessPathDataEvening = propagationProcessPathData;
            default:
                propagationProcessPathDataNight = propagationProcessPathData;
        }
    }

    public String getLwFrequencyPrepend() {
        return lwFrequencyPrepend;
    }

    public void setLwFrequencyPrepend(String lwFrequencyPrepend) {
        this.lwFrequencyPrepend = lwFrequencyPrepend;
    }

    /**
     * @return The filePath of results outputs as sql commands.
     */
    public File getSqlOutputFile() {
        return sqlOutputFile;
    }

    /**
     * @return Drop previous results tables before inserting results
     */
    public Boolean getDropResultsTable() {
        return dropResultsTable;
    }

    public Boolean getSqlOutputFileCompression() {
        return sqlOutputFileCompression;
    }

    public void setSqlOutputFileCompression(Boolean sqlOutputFileCompression) {
        this.sqlOutputFileCompression = sqlOutputFileCompression;
    }

    /**
     * @param dropResultsTable Drop previous results tables before inserting results
     */
    public void setDropResultsTable(Boolean dropResultsTable) {
        this.dropResultsTable = dropResultsTable;
    }

    /**
     * @param sqlOutputFile
     */
    public void setSqlOutputFile(File sqlOutputFile) {
        this.sqlOutputFile = sqlOutputFile;
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

    /**
     * @return For each ray export the ground profile under it as a geojson column (take large amount of disk)
     */
    public boolean isExportProfileInRays() {
        return exportProfileInRays;
    }

    /**
     * @param  exportProfileInRays For each ray export the ground profile under it as a geojson column (take large amount of disk)
     */
    public void setExportProfileInRays(boolean exportProfileInRays) {
        this.exportProfileInRays = exportProfileInRays;
    }

    public boolean isKeepAbsorption() {
        return keepAbsorption;
    }

    /**
     * @param keepAbsorption If true store absorption values in propagation path objects
     * @see #setKeepAbsorption(boolean)
     */
    public void setKeepAbsorption(boolean keepAbsorption) {
        this.keepAbsorption = keepAbsorption;
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
     * @param lNightTable Change name of created table (must be set before running computation)
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
     * @param raysTable Table name that will contain rays dump (profile)
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
}
