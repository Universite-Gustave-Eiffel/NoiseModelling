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
package org.orbisgis.noisemap.core;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid).
 *
 * @author Nicolas Fortin
 */
public class PropagationProcessData {
    /** Coordinate of receivers */
    public List<Coordinate> receivers;
    /** FreeField test */
    public FastObstructionTest freeFieldFinder;
    /** Source Index */
    public QueryGeometryStructure sourcesIndex;
    /** Sources geometries. Can be LINESTRING or POINT */
    public List<Geometry> sourceGeometries;
    /** Sound level of source. By frequency band, energetic */
    public List<ArrayList<Double>> wj_sources;
    /** Frequency bands values, by third octave */
    public List<Integer> freq_lvl;
    /** Maximum reflexion order */
    public int reflexionOrder;
    /** Maximum diffraction order */
    public int diffractionOrder;
    /** Maximum source distance */
    public double maxSrcDist;
    /** Maximum reflection wall distance from receiver->source line */
    public double maxRefDist;
    /** Minimum distance between source and receiver */
    public double minRecDist;
    /** Wall alpha [0-1] */
    public double wallAlpha;
    /** cellId only used in output data */
    public int cellId;
    /** Progression information */
    public ProgressVisitor cellProg;
    /** list Geometry of soil and the type of this soil */
    public List<GeoWithSoilType> geoWithSoilType;
    /** True will compute vertical diffraction */
    public boolean computeVerticalDiffraction;

    public PropagationProcessData(List<Coordinate> receivers, FastObstructionTest freeFieldFinder,
                                  QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries,
                                  List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl, int reflexionOrder,
                                  int diffractionOrder, double maxSrcDist, double maxRefDist, double minRecDist,
                                  double wallAlpha, int cellId, ProgressVisitor cellProg,
                                  List<GeoWithSoilType> geoWithSoilType, boolean computeVerticalDiffraction) {
        this.receivers = receivers;
        this.freeFieldFinder = freeFieldFinder;
        this.sourcesIndex = sourcesIndex;
        this.sourceGeometries = sourceGeometries;
        this.wj_sources = wj_sources;
        this.freq_lvl = freq_lvl;
        this.reflexionOrder = reflexionOrder;
        this.diffractionOrder = diffractionOrder;
        this.maxSrcDist = maxSrcDist;
        this.maxRefDist = maxRefDist;
        this.minRecDist = minRecDist;
        this.wallAlpha = wallAlpha;
        this.cellId = cellId;
        this.cellProg = cellProg;
        this.geoWithSoilType = geoWithSoilType;
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }
}
