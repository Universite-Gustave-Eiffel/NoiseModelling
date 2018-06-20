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
import org.locationtech.jts.geom.LineSegment;

import java.util.List;

/**
 * DiffractionWithGroundEffectZone work for FastObstructionTest,
 * aims to keep the 3D diffraction, first diffraction zone and last diffraction zone data, 
 * to give them to propagation process data
 * @author SU Qi
 * @author Nicolas Fortin
 */
public class DiffractionWithSoilEffetZone {
    private LineSegment rOZone;//receiver-first intersection zone for 3D diffraction
    private LineSegment oSZone;//last intersection-source zone for 3D diffraction
    private double deltaDistance;
    private double eLength;
    private double fullDiffractionDistance;
    private List<Coordinate> rOgroundCoordinates;
    private List<Coordinate> oSgroundCoordinates;

    /**
     *
     * @param rOZone Segment from receiver to first diffraction corner
     * @param oSZone Segment from last diffraction corner to source
     * @param deltaDistance Direct field distance between R and S minus fullDiffractionDistance
     * @param eLength Length from first diffraction corner to last diffraction corner
     * @param fullDiffractionDistance Full path distance from receiver to source
     */
    public DiffractionWithSoilEffetZone(LineSegment rOZone, LineSegment oSZone,
                                        double deltaDistance, double eLength, double fullDiffractionDistance,
                                        List<Coordinate> rOgroundCoordinates, List<Coordinate> oSgroundCoordinates) {
        this.rOZone = rOZone;
        this.oSZone = oSZone;
        this.deltaDistance = deltaDistance;
        this.eLength = eLength;
        this.fullDiffractionDistance = fullDiffractionDistance;
        this.rOgroundCoordinates = rOgroundCoordinates;
        this.oSgroundCoordinates = oSgroundCoordinates;
    }

    /**
     * @return Ground segments between Receiver and first diffraction. The first coordinate is the receiver ground position.
     */
    public List<Coordinate> getrOgroundCoordinates() {
        return rOgroundCoordinates;
    }

    /**
     * @return Ground segments between first diffraction and source. The last coordinate is the source ground position.
     */
    public List<Coordinate> getoSgroundCoordinates() {
        return oSgroundCoordinates;
    }

    /**
     * @return Direct field distance between R and S minus fullDiffractionDistance
     */
    public double getDeltaDistance() {
        return deltaDistance;
    }

    /**
     * @return Length from first diffraction corner to last diffraction corner
     */
    public double geteLength() {
        return eLength;
    }

    /**
     * @return Full path distance from receiver to source
     */
    public double getFullDiffractionDistance() {
        return fullDiffractionDistance;
    }

    /**
     * @return Segment from receiver to first diffraction corner
     */
    public LineSegment getROZone() {
        return this.rOZone;
    }

    /**
     * @return Segment from last diffraction corner to source
     */
    public LineSegment getOSZone() {
        return this.oSZone;
    }
}
