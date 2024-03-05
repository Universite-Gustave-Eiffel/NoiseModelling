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
import java.util.HashMap;
import java.util.List;

/**
 * Configuration of NoiseModelling computation based on database data using standard Lden outputs
 */
public class LTConfig {
    public String timestep;
    public List<String> timesteps;



    public enum INPUT_MODE {INPUT_MODE_LWT}
    final INPUT_MODE input_mode;

    // This field is initialised when {@link PointNoiseMap#initialize} is called


    HashMap<String , PropagationProcessPathData > propagationProcessPathDataT= new HashMap<String,PropagationProcessPathData >();


    // Cnossos revisions have multiple coefficients for road emission formulae
    // this parameter will be removed when the final version of Cnossos will be published
    int coefficientVersion = 2;

    boolean fixAttenuation = true;

    // Process status
    boolean exitWhenDone = false;
    boolean aborted = false;

    // Output config
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
    int maximumRaysOutputCount = 0; // if export rays, do not keep more than this number of rays (0 infinite)
    // Maximum result stack to be inserted in database
    // if the stack is full, the computation core is waiting
    int outputMaximumQueue = 50000;

    boolean mergeSources = true;

    String lTTable = "LT_RESULT";

    String raysTable = "RAYS";

    String lwFrequencyPrepend = "HZ";

    File sqlOutputFile;
    Boolean sqlOutputFileCompression = true;
    Boolean dropResultsTable = true;



    public PropagationProcessPathData getPropagationProcessPathData() {
        return propagationProcessPathDataT.entrySet().iterator().next().getValue();
    }

    public PropagationProcessPathData getPropagationProcessPathData(String timestep) {
        return propagationProcessPathDataT.get(timestep);
    }

    /**
     * @return if export rays, do not keep more than this number of rays (0 infinite)
     */
    public int getMaximumRaysOutputCount() {
        return maximumRaysOutputCount;
    }

    /**
     * @param maximumRaysOutputCount if export rays, do not keep more than this number of rays per computation area (0 infinite)
     */
    public void setMaximumRaysOutputCount(int maximumRaysOutputCount) {
        this.maximumRaysOutputCount = maximumRaysOutputCount;
    }

    public void setPropagationProcessPathData(String timestep, PropagationProcessPathData propagationProcessPathData) {
        propagationProcessPathDataT.put(timestep, propagationProcessPathData);
    }



    public LTConfig() {
        input_mode = null;
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


    public void setMergeSources(boolean mergeSources) {
        this.mergeSources = mergeSources;
    }

    public void setlTTable(String lTTable) {
        this.lTTable = lTTable;
    }

    public String getlTTable() {
        return lTTable;
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

    public boolean isMergeSources() {
        return mergeSources;
    }
}
