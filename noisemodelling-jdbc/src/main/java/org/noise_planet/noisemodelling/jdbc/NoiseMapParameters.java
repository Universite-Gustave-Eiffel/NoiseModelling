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

    boolean exportAttenuationMatrix;
    /** Frequency bands values, by octave or third octave */
    public List<Integer> frequencyArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public List<Double> exactFrequencyArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE));
    public List<Double> aWeightingArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE));


    boolean exportProfileInRays = false;
    boolean keepAbsorption = false; // in rays, keep store detailed absorption data
    int maximumRaysOutputCount = 0; // if export rays, do not keep more than this number of rays (0 infinite)
    public enum ExportRaysMethods {TO_RAYS_TABLE, TO_MEMORY, NONE}
    ExportRaysMethods exportRaysMethod = ExportRaysMethods.NONE;
    // Cnossos revisions have multiple coefficients for road emission formulae
    // this parameter will be removed when the final version of Cnossos will be published
    int coefficientVersion = 2;

    // Process status
    boolean exitWhenDone = false;
    boolean aborted = false;

    public int geojsonColumnSizeLimit = 1000000; // sql column size limitation for geojson

    int outputMaximumQueue = 50000;

    public boolean mergeSources = true;

    String raysTable = "RAYS";

    String lwFrequencyPrepend = "LW";

    File sqlOutputFile;
    Boolean sqlOutputFileCompression = true;
    Boolean dropResultsTable = true;
    public boolean computeLAEQOnly = false;

    /**
     * If true the position of the receiver (with the altitude if available) will be exported into the results tables
     */
    boolean exportReceiverPosition = false;

}
