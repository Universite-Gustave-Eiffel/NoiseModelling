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
package org.orbisgis.noisemap.h2;

import org.h2gis.h2spatialapi.AbstractFunction;
import org.h2gis.h2spatialapi.ScalarFunction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified
 * buildings geometry
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class BR_TriGrid extends AbstractFunction implements ScalarFunction {

    public BR_TriGrid() {
        addProperty(PROP_REMARKS , "Sound propagation from ponctual sound sources to ponctual receivers created by a " +
                "delaunay triangulation of specified buildings geometry.\n" +
                "CALL BR_TriGrid(buildingsTable VARCHAR, sourcesTable VARCHAR, sourcesTableSoundFieldName VARCHAR," +
                " maximumPropagationDistance DOUBLE, maximumWallSeekingDistance DOUBLE, cellWidth DOUBLE," +
                " roadsWidth DOUBLE, receiversDensification DOUBLE, maximumAreaOfTriangle DOUBLE," +
                " soundReflectionOrder INT, soundDiffractionOrder DOUBLE, wallAlpha DOUBLE)");
    }

    @Override
    public String getJavaStaticMethod() {
        return "noisePropagation";
    }

    /**
     * Construct a ResultSet using parameter and core noise-map.
     *
     * @param connection                 Active connection, never closed (provided and hidden by H2)
     * @param buildingsTable             Buildings table name (polygons)
     * @param sourcesTable               Source table table (linestring or point)
     * @param sourcesTableSoundFieldName Field name to extract from sources table. Frequency is added on right.
     * @param groundTypeTable            Optional (empty if not available) Soil category. This is a table with a
     *                                   polygon column and a column 'G' [0-1] double.
     * @param maximumPropagationDistance Propagation distance limitation.
     * @param maximumWallSeekingDistance Maximum reflection distance from the source-receiver propagation line.
     * @param roadsWidth                 Buffer without receivers applied on roads on final noise map.
     * @param soundReflectionOrder       Sound reflection order on walls.
     * @param soundDiffractionOrder      Source diffraction order on corners.
     * @param wallAlpha                  Wall absorption coefficient.
     * @return A table with 3 columns GID(extracted from receivers table), W energy receiver by receiver,
     * cellid cell identifier.
     * @throws SQLException
     */
    public static ResultSet noisePropagation(Connection connection, String buildingsTable, String sourcesTable,
                                             String sourcesTableSoundFieldName, String groundTypeTable,
                                             double maximumPropagationDistance, double maximumWallSeekingDistance,
                                             double roadsWidth, double receiversDensification,
                                             double maximumAreaOfTriangle, int soundReflectionOrder,
                                             int soundDiffractionOrder, double wallAlpha) throws SQLException {
        return BR_TriGrid3D.noisePropagation(connection, buildingsTable, "", sourcesTable,
                sourcesTableSoundFieldName, groundTypeTable, "", maximumPropagationDistance,
                maximumWallSeekingDistance, roadsWidth, receiversDensification, maximumAreaOfTriangle,
                soundReflectionOrder, soundDiffractionOrder, wallAlpha);
    }
}
