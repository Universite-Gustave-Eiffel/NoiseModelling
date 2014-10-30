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
import java.sql.SQLException;

/**
 * Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified
 * buildings geometry
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class BR_PtGrid extends AbstractFunction implements ScalarFunction {
    public BR_PtGrid() {
        addProperty(PROP_REMARKS , "Sound propagation from ponctual sound sources to defined ponctual receivers.\n" +
                "CALL BR_PtGridBR_PtGrid(buildings VARCHAR,sources VARCHAR,receivers_table VARCHAR," +
                "sound_lvl_field VARCHAR,maximum_propagation_distance DOUBLE(meter)," +
                "maximum_reflection_distance DOUBLE(meter),subdivision_level int," +
                " sound_reflection_order int, sound_diffraction_order int, wall_absorption double)");
    }

    @Override
    public String getJavaStaticMethod() {
        return "noisePropagation";
    }

    public static void noisePropagation(Connection connection, String destinationTable, String buildings ,
                                        String sources,String receivers_table,String sound_lvl_field,
                                        double maximum_propagation_distance, double maximum_reflection_distance,
                                        int subdivision_level, int sound_reflection_order,int sound_diffraction_order,
                                        double wall_absorption) throws SQLException {
        if(maximum_propagation_distance < maximum_reflection_distance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }

    }
}
