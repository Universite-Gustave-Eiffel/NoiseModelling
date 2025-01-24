/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Common parameters for sql processing of input data
 */
public class NoiseMapParameters {

    public String buildingsTableName = "BUILDINGS";
    public String sourcesTableName = "SOURCES";
    public String receiverTableName = "RECEIVERS";
    protected String soilTableName = "GROUND";

    public boolean exportAttenuationMatrix;
    public boolean exportProfileInRays = false;
    public boolean keepAbsorption = false; // in rays, keep store detailed absorption data
    public int maximumRaysOutputCount = 0; // if export rays, do not keep more than this number of rays (0 infinite)
    public enum ExportRaysMethods {TO_RAYS_TABLE, TO_MEMORY, NONE}
    public ExportRaysMethods exportRaysMethod = ExportRaysMethods.NONE;

    // Cnossos revisions have multiple coefficients for road emission formulae
    // this parameter will be removed when the final version of Cnossos will be published
    public int coefficientVersion = 2;

    // Process status
    public boolean exitWhenDone = false;
    public boolean aborted = false;

    public int geojsonColumnSizeLimit = 1000000; // sql column size limitation for geojson

    public int outputMaximumQueue = 50000;

    /**
     * Number of threads used for processing, 0 (default) is using all available cpu threads
     */
    private int threadCount = 0;

    public boolean mergeSources = true;

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

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

}
