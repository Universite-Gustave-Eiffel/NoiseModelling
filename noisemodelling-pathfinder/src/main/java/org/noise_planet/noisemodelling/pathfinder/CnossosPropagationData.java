/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid).
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 * @author Adrien Le Bellec
 */
public class CnossosPropagationData {
    public static final double DEFAULT_MAX_PROPAGATION_DISTANCE = 1200;
    public static final double DEFAULT_MAXIMUM_REF_DIST = 700;
    public static final double DEFAULT_RECEIVER_DIST = 1.0;
    public static final double DEFAULT_GS = 0.0;
    public static final String YAW_DATABASE_FIELD = "YAW";
    public static final String PITCH_DATABASE_FIELD = "PITCH";
    public static final String ROLL_DATABASE_FIELD = "ROLL";
    public static final String DIRECTIVITY_DATABASE_FIELD = "DIR_ID";
    public static final String GS_DATABASE_FIELD = "GS";

    public List<Long> receiversPk = new ArrayList<>();
    public List<Long> sourcesPk = new ArrayList<>();
    /** coordinate of receivers */
    public List<Coordinate> receivers = new ArrayList<>();
    /** Profile builder */
    public ProfileBuilder profileBuilder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex = new QueryRTree();
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries = new ArrayList<>();
    /** Source orientation for emission computation */
    public Map<Long, Orientation> sourceOrientation = new HashMap<>();
    /**
     * Link between sources PK and direction attenuation index
     */
    public Map<Long, Integer> sourceDirection = new HashMap<>();




    /**
     * Link between sources PK and gs coefficient
     */
    public Map<Long, Double> sourceGs = new HashMap<>();


    /** Maximum reflexion order */
    public int reflexionOrder = 1;
    /** Compute horizontal diffraction rays over vertical edges */
    protected boolean computeHorizontalDiffraction = true;

    /** True will compute vertical diffraction over horizontal edges */
    protected boolean computeVerticalDiffraction;

    /** Maximum source distance */
    public double maxSrcDist = DEFAULT_MAX_PROPAGATION_DISTANCE;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist = DEFAULT_MAXIMUM_REF_DIST;
    /** Source factor absorption */
    public double gS = DEFAULT_GS;

    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = Double.NEGATIVE_INFINITY;

    /** stop calculation if the sum of further sources contributions are smaller than this value */
    public double noiseFloor = Double.NEGATIVE_INFINITY;


    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    protected List<GeoWithSoilType> soilList = new ArrayList<>();

    Map<String, Integer> sourceFieldNames = new HashMap<>();
    public static final Integer[] DEFAULT_FREQUENCIES_THIRD_OCTAVE = new Integer[] {50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000};
    public static final Double[] DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE = new Double[] {50.1187234, 63.0957344, 79.4328235, 100.0, 125.892541, 158.489319, 199.526231, 251.188643, 316.227766, 398.107171, 501.187234, 630.957344, 794.328235, 1000.0, 1258.92541, 1584.89319, 1995.26231, 2511.88643, 3162.27766, 3981.07171, 5011.87234, 6309.57344, 7943.28235, 10000.0};
    public static final Double[] DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE = new Double[] {-30.2, -26.2, -22.5, -19.1, -16.1, -13.4, -10.9, -8.6, -6.6, -4.8, -3.2, -1.9, -0.8, 0.0, 0.6, 1.0, 1.2, 1.3, 1.2, 1.0, 0.5, -0.1, -1.1, -2.5};

    public List<Integer> freq_lvl = Arrays.asList(asOctaveBands(DEFAULT_FREQUENCIES_THIRD_OCTAVE));


    public CnossosPropagationData(ProfileBuilder profileBuilder, List<Integer> freq_lvl) {
        this.profileBuilder = profileBuilder;
        this.freq_lvl = freq_lvl;
    }


    public CnossosPropagationData(ProfileBuilder profileBuilder) {
        this.profileBuilder = profileBuilder;
    }

    public void addSource(Geometry geom) {
        sourceGeometries.add(geom);
        sourcesIndex.appendGeometry(geom, sourceGeometries.size() - 1);
    }

    public void addSource(Long pk, Geometry geom) {
        addSource(geom);
        sourcesPk.add(pk);
    }

    public void addSource(Long pk, Geometry geom, Orientation orientation) {
        addSource(pk, geom);
        sourceOrientation.put(pk, orientation);
    }

    public void addSource(Long pk, Geometry geom, Double gs) {
        addSource(pk, geom);
        sourceGs.put(pk, gs);
    }

    /**
     * Add geometry with additional attributes
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param rs Additional attributes fetched from database
     */
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException, IOException {
        addSource(pk, geom);
        if(sourceFieldNames.isEmpty()) {
            List<String> fieldNames = JDBCUtilities.getColumnNames(rs.getMetaData());
            for(int idField = 0; idField < fieldNames.size(); idField++) {
                sourceFieldNames.put(fieldNames.get(idField).toUpperCase(Locale.ROOT), idField + 1);
            }
        }
        float yaw = 0;
        float pitch = 0;
        float roll = 0;
        boolean hasOrientation = false;
        if(sourceFieldNames.containsKey(YAW_DATABASE_FIELD)) {
            yaw = rs.getFloat(sourceFieldNames.get(YAW_DATABASE_FIELD));
            hasOrientation = true;
        }
        if(sourceFieldNames.containsKey(PITCH_DATABASE_FIELD)) {
            pitch = rs.getFloat(sourceFieldNames.get(PITCH_DATABASE_FIELD));
            hasOrientation = true;
        }
        if(sourceFieldNames.containsKey(ROLL_DATABASE_FIELD)) {
            roll = rs.getFloat(sourceFieldNames.get(ROLL_DATABASE_FIELD));
            hasOrientation = true;
        }
        int directivityField = JDBCUtilities.getFieldIndex(rs.getMetaData(), DIRECTIVITY_DATABASE_FIELD);
        if(sourceFieldNames.containsKey(DIRECTIVITY_DATABASE_FIELD)) {
            sourceDirection.put(pk, rs.getInt(directivityField));
        }
        if(hasOrientation) {
            sourceOrientation.put(pk, new Orientation(yaw, pitch, roll));
        }

        int gsField = JDBCUtilities.getFieldIndex(rs.getMetaData(), GS_DATABASE_FIELD);
        if(sourceFieldNames.containsKey(GS_DATABASE_FIELD)) {
            sourceGs.put(pk, rs.getDouble(gsField));
        }
    }

    public void setSources(List<Geometry> sourceGeometries) {
        int i = 0;
        for(Geometry source : sourceGeometries) {
            sourcesIndex.appendGeometry(source, i++);
        }
        this.sourceGeometries = sourceGeometries;
    }

    /**
     * Optional - Return the maximal power spectrum of the sound source
     * @param sourceId Source identifier (index in {@link PropagationProcessData#sourceGeometries})
     * @return maximal power spectrum or empty array
     */
    public double[] getMaximalSourcePower(int sourceId) {
        return new double[0];
    }

    public void addReceiver(Coordinate... receiver) {
        receivers.addAll(Arrays.asList(receiver));
    }

    public void addReceiver(long pk, Coordinate position) {
        receivers.add(position);
        receiversPk.add(pk);
    }

    public void addReceiver(long pk, Coordinate position, SpatialResultSet rs) {
        addReceiver(pk, position);
    }

    public int getReflexionOrder() {
        return reflexionOrder;
    }

    public void setReflexionOrder(int reflexionOrder) {
        this.reflexionOrder = reflexionOrder;
    }

    public void setComputeHorizontalDiffraction(boolean computeHorizontalDiffraction) {
        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
    }

    public void setComputeVerticalDiffraction(boolean computeVerticalDiffraction) {
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }

    public void setGs(double gS) {
        this.gS = gS;
    }


    public double getSourceGs(int srcIndex) {
        return this.gS;
    }

    public boolean isComputeVEdgeDiffraction() {
        return computeHorizontalDiffraction;
    }

    public boolean isComputeHEdgeDiffraction() {
        return computeVerticalDiffraction;
    }

    public boolean isComputeDiffraction() {
        return isComputeHEdgeDiffraction() || isComputeVEdgeDiffraction();
    }

    /**
     * Return directivity attenuation. Default implementation define only omnidirectional sources.
     * @param srcIndex Source index in the list sourceGeometries
     * @param frequency Frequency in Hertz
     * @param phi (0 2π) 0 is front
     * @param theta (-π/2 π/2) 0 is horizontal π is top
     * @return Attenuation in dB
     */
    public double getSourceAttenuation(int srcIndex, double frequency, float phi, float theta) {
        return 0;
    }

    /**
     * @param srcIndex Source index in the list sourceGeometries
     * @return True if the source is omnidirectional and so does not have orientation dependant attenuation.
     */
    public boolean isOmnidirectional(int srcIndex) {
        return true;
    }

    /**
     * Create new array by taking middle third octave bands
     *
     * @param thirdOctaveBands Third octave bands array
     * @return Octave bands array
     */
    public static Double[] asOctaveBands(Double[] thirdOctaveBands) {
        Double[] octaveBands = new Double[thirdOctaveBands.length / 3];
        int j = 0;
        for (int i = 1; i < thirdOctaveBands.length - 1; i += 3) {
            octaveBands[j++] = thirdOctaveBands[i];
        }
        return octaveBands;
    }
    /**
     * Create new array by taking middle third octave bands
     *
     * @param thirdOctaveBands Third octave bands array
     * @return Octave bands array
     */
    public static Integer[] asOctaveBands(Integer[] thirdOctaveBands) {
        Integer[] octaveBands = new Integer[thirdOctaveBands.length / 3];
        int j = 0;
        for (int i = 1; i < thirdOctaveBands.length - 1; i += 3) {
            octaveBands[j++] = thirdOctaveBands[i];
        }
        return octaveBands;
    }
}


