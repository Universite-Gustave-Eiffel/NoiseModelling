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

import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid).
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 * @author Adrien Le Bellec
 */
public class PropagationProcessData {
    public static final double DEFAULT_MAX_PROPAGATION_DISTANCE = 1200;
    public static final double DEFAULT_MAXIMUM_REF_DIST = 50;
    public static final double DEFAULT_RECEIVER_DIST = 1.0;
    public static final double DEFAULT_GS = 0.0;

    public List<Long> receiversPk = new ArrayList<>();
    public List<Long> sourcesPk = new ArrayList<>();
    /** coordinate of receivers */
    public List<Coordinate> receivers = new ArrayList<>();
    /** FreeField test */
    public FastObstructionTest freeFieldFinder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex = new QueryRTree();
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries = new ArrayList<>();



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
    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    protected List<GeoWithSoilType> soilList = new ArrayList<>();



    public PropagationProcessData(FastObstructionTest freeFieldFinder) {
        this.freeFieldFinder = freeFieldFinder;
    }

    public void addSource(Geometry geom) {
        sourceGeometries.add(geom);
        sourcesIndex.appendGeometry(geom, sourceGeometries.size() - 1);
    }

    public void addSource(Long pk, Geometry geom) {
        addSource(geom);
        sourcesPk.add(pk);
    }
    /**
     * Add geometry with additional attributes
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param rs Additional attributes fetched from database
     */
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException, IOException {
        addSource(pk, geom);
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


    public void addSoilType(GeoWithSoilType soilType) {
        soilList.add(soilType);
    }

    public void addSoilType(Envelope region, double type) {
        soilList.add(new GeoWithSoilType(new GeometryFactory().toGeometry(region), type));
    }

    public void addSoilType(Geometry geo, double type) {
        soilList.add(new GeoWithSoilType(geo, type));
    }

    public void setSoilList(List<GeoWithSoilType> soilList) {
        this.soilList = soilList;
    }

    public List<GeoWithSoilType> getSoilList() {
        return soilList;
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


    public boolean isComputeHorizontalDiffraction() {
        return computeHorizontalDiffraction;
    }

    public boolean isComputeVerticalDiffraction() {
        return computeVerticalDiffraction;
    }


}


