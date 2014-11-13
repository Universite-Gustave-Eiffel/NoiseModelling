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

import org.h2.tools.SimpleResultSet;
import org.h2.tools.SimpleRowSource;
import org.h2gis.h2spatial.TableFunctionUtil;
import org.h2gis.h2spatialapi.AbstractFunction;
import org.h2gis.h2spatialapi.EmptyProgressVisitor;
import org.h2gis.h2spatialapi.ScalarFunction;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.orbisgis.noisemap.core.jdbc.PointNoiseMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sound propagation from punctual sound sources to punctual receivers created by a delaunay triangulation of specified
 * buildings geometry
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class BR_PtGrid extends AbstractFunction implements ScalarFunction {

    public BR_PtGrid() {
        addProperty(PROP_REMARKS , "Sound propagation from punctual sound sources to defined punctual receivers.\n" +
                "select * from BR_PtGridBR_PtGrid(buildings VARCHAR,sources VARCHAR,receivers_table VARCHAR," +
                "sound_lvl_field VARCHAR,ground_type_table VARCHAR, maximum_propagation_distance DOUBLE(meter)," +
                "maximum_reflection_distance DOUBLE(meter),subdivision_level int," +
                " sound_reflection_order int, sound_diffraction_order int, wall_absorption double)");
    }

    @Override
    public String getJavaStaticMethod() {
        return "noisePropagation";
    }

    /**
     * Construct a ResultSet using parameter and core noise-map.
     *
     * @param connection                   Active connection, never closed (provided and hidden by H2)
     * @param buildings                    Buildings table name (polygons)
     * @param sources                      Source table table (linestring or point)
     * @param receivers_table              Receiver table, has a numeric primary key (optional) and a point column.
     * @param sound_lvl_field              Field name to extract from sources table. Frequency is added on right.
     * @param ground_type                  Optional (empty if not available) Soil category. This is a table with a
     *                                     polygon column and a column 'G' double.
     * @param maximum_propagation_distance Propagation distance limitation.
     * @param maximum_reflection_distance  Maximum reflection distance from the source-receiver propagation line.
     * @param sound_reflection_order       Sound reflection order on walls.
     * @param sound_diffraction_order      Source diffraction order on corners.
     * @param wall_absorption              Wall absorption coefficient.
     * @return A table with 3 columns GID(extracted from receivers table), W energy receiver by receiver,
     * cellid cell identifier.
     * @throws java.sql.SQLException
     */
    public static ResultSet noisePropagation(Connection connection, String buildings,
                                             String sources, String receivers_table, String sound_lvl_field,
                                             String ground_type, double maximum_propagation_distance, double maximum_reflection_distance,
                                             int sound_reflection_order, int sound_diffraction_order,
                                             double wall_absorption) throws SQLException {
        return BR_PtGrid3D.noisePropagation(connection, buildings, "", sources, receivers_table, sound_lvl_field,
                ground_type,"", maximum_propagation_distance, maximum_reflection_distance, sound_reflection_order,
                sound_diffraction_order, wall_absorption);
    }
}
