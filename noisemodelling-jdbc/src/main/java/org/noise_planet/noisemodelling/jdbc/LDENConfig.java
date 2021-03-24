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

/**
 * Configuration of NoiseModelling computation based on database data using standard Lden outputs
 */
public class LDENConfig {
    public enum INPUT_MODE { INPUT_MODE_TRAFFIC_FLOW,INPUT_MODE_RAILWAY_FLOW, INPUT_MODE_LW_DEN, INPUT_MODE_PROBA}
    final INPUT_MODE input_mode;

    // This field is initialised when {@link PointNoiseMap#initialize} is called
    PropagationProcessPathData propagationProcessPathData = null;

    // Cnossos revisions have multiple coefficients for road emission formulae
    // this parameter will be removed when the final version of Cnossos will be published
    int coefficientVersion = 2;

    // Process status
    boolean exitWhenDone = false;
    boolean aborted = false;

    // Output config
    boolean computeLDay = false;
    boolean computeLEvening = false;
    boolean computeLNight = false;
    boolean computeLDEN = true;
    boolean exportRays = false;
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

    public LDENConfig(INPUT_MODE input_mode) {
        this.input_mode = input_mode;
    }

    public PropagationProcessPathData getPropagationProcessPathData() {
        return propagationProcessPathData;
    }

    public String getLwFrequencyPrepend() {
        return lwFrequencyPrepend;
    }

    public void setLwFrequencyPrepend(String lwFrequencyPrepend) {
        this.lwFrequencyPrepend = lwFrequencyPrepend;
    }

    public void setPropagationProcessPathData(PropagationProcessPathData propagationProcessPathData) {
        this.propagationProcessPathData = propagationProcessPathData;
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

    /**
     * Export rays in table
     * @return True if exported, false (default) otherwise
     */
    public boolean isExportRays() {
        return exportRays;
    }

    /**
     * Export rays in table (beware this could take a lot of storage space)
     * @param exportRays True to export rays in table RAYS (by default)
     */
    public void setExportRays(boolean exportRays) {
        this.exportRays = exportRays;
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
