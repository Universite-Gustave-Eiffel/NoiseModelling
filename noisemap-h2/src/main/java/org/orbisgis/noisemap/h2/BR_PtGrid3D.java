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
import org.h2gis.utilities.TableUtilities;
import org.h2gis.api.AbstractFunction;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ScalarFunction;
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
import java.util.HashSet;

/**
 * Sound propagation from punctual sound sources to punctual receivers created by a delaunay triangulation of specified
 * buildings geometry
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class BR_PtGrid3D extends AbstractFunction implements ScalarFunction {
    private static final int COLUMN_COUNT = 3;

    public BR_PtGrid3D() {
        addProperty(PROP_REMARKS , "## BR_PtGrid3D\n" +
                "\n" +
                "BR_PtGrid3D(String buildingsTable, String heightFieldName,String sourcesTable, String receiversTable, String sourcesTableSoundFieldName, String groundTypeTable, String demTable, double maximumPropagationDistance, double maximumWallSeekingDistance, int soundReflectionOrder, int soundDiffractionOrder, double wallAlpha)\n" +
                "\n" +
                " - **buildingsTable** table identifier that contain a geometry column of type POLYGON.\n" +
                " - **heightFieldName** column identifier in the buildings table that hold building height in meter. " +
                "\n" +
                " - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The " +
                "table must contain the sound emission level in dB(A).\n" +
                " - **receiversTable** table identifier that contain the list of evaluation point of sound level. " +
                "This table must contains only POINT. And optionally an integer primary key.\n" +
                " - **sourcesTableSoundFieldName** prefix identifier of the emission level column. ex 'DB_M' for " +
                "columns 'DB_M100' to 'DB_M5000'.  \n" +
                " - **groundTypeTable** table identifier of the ground category table. This table must contain a " +
                "geometry field of type POLYGON. And a column 'G' of type double between 0 and 1.\n" +
                " dimensionless coefficient G:\n" +
                "    - Law, meadow, field of cereals G=1\n" +
                "    - Undergrowth (resinous or decidious) G=1\n" +
                "    - non-compacted earth G=0.7\n" +
                "    - Compacted earth, track G=0.3\n" +
                "    - Road surface G=0\n" +
                "    - Smooth concrete G=0\n" +
                " - **demTable** table identifier that contain the digital elevation model. A geometry column of type" +
                " POINT with X,Y and Z value.    \n" +
                " - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter " +
                "are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and " +
                "memory usage.\n" +
                " - **maximumWallSeekingDistance** From the direct propagation line source-receiver, " +
                "wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts " +
                "performance.\n" +
                " - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. " +
                "Recommended value is 2.\n" +
                " - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended " +
                "value is 1.\n" +
                " - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete." +
                "");
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
     * @param buildings_height_field       Optional (empty if not available) Field name of building height on
     *                                     buildingsTable
     * @param sources                      Source table table (linestring or point)
     * @param receivers_table              Receiver table, has a numeric primary key (optional) and a point column.
     * @param sound_lvl_field              Field name to extract from sources table. Frequency is added on right.
     * @param ground_type                  Optional (empty if not available) Soil category. This is a table with a
     *                                     polygon column and a column 'G' double.
     * @param dem_table                    Optional (empty if not available) Point table of digital elevation model.
     * @param maximum_propagation_distance Propagation distance limitation.
     * @param maximum_reflection_distance  Maximum reflection distance from the source-receiver propagation line.
     * @param sound_reflection_order       Sound reflection order on walls.
     * @param sound_diffraction_order      Source diffraction order on corners.
     * @param wall_absorption              Wall absorption coefficient.
     * @return A table with 3 columns GID(extracted from receivers table), W energy receiver by receiver,
     * cellid cell identifier.
     * @throws SQLException
     */
    public static ResultSet noisePropagation(Connection connection, String buildings, String buildings_height_field,
                                             String sources, String receivers_table, String sound_lvl_field,
                                             String ground_type, String dem_table,
                                             double maximum_propagation_distance, double maximum_reflection_distance,
                                             int sound_reflection_order, int sound_diffraction_order,
                                             double wall_absorption) throws SQLException {
        if (maximum_propagation_distance < maximum_reflection_distance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        SimpleResultSet rs;
        if(TableUtilities.isColumnListConnection(connection)) {
            // Only rs columns is necessary
            rs = new SimpleResultSet();
            feedColumns(rs);
        } else {
            connection = SFSUtilities.wrapConnection(connection);
                    PointNoiseMap noiseMap = new PointNoiseMap(TableLocation.capsIdentifier(buildings, true),
                            TableLocation.capsIdentifier(sources, true), TableLocation.capsIdentifier(receivers_table, true));
            noiseMap.setSound_lvl_field(sound_lvl_field);
            noiseMap.setMaximumPropagationDistance(maximum_propagation_distance);
            noiseMap.setSoilTableName(ground_type);
            noiseMap.setHeightField(buildings_height_field);
            noiseMap.setDemTable(dem_table);
            noiseMap.setMaximumReflectionDistance(maximum_reflection_distance);
            noiseMap.setSoundReflectionOrder(sound_reflection_order);
            noiseMap.setSoundDiffractionOrder(sound_diffraction_order);
            noiseMap.setWallAbsorption(wall_absorption);
            noiseMap.initialize(connection, new EmptyProgressVisitor());
            rs = new SimpleResultSet(new PointRowSource(noiseMap, connection));
            feedColumns(rs);
        }
        return rs;
    }

    private static void feedColumns(SimpleResultSet rs) {
        rs.addColumn("GID", Types.BIGINT, 19, 20);
        rs.addColumn("W", Types.DOUBLE, 17, 24);
        rs.addColumn("CELL_ID", Types.INTEGER, 10, 11);
    }

    private static class PointRowSource implements SimpleRowSource {
        private Deque<PropagationResultPtRecord> output = new ArrayDeque<PropagationResultPtRecord>();
        HashSet<Long> processedReceivers = new HashSet<Long>();
        private int cellI = -1;
        private int cellJ = 0;
        private PointNoiseMap noiseMap;
        private Connection connection;

        private PointRowSource(PointNoiseMap noiseMap, Connection connection) {
            this.noiseMap = noiseMap;
            this.connection = connection;
        }

        @Override
        public Object[] readRow() throws SQLException {
            if(output.isEmpty()) {
                do {
                    // Increment ids
                    if (cellI + 1 < noiseMap.getGridDim()) {
                        cellI++;
                    } else {
                        if (cellJ + 1 < noiseMap.getGridDim()) {
                            cellI = 0;
                            cellJ++;
                        } else {
                            return null;
                        }
                    }
                    // Fetch next cell
                    output.addAll(noiseMap.evaluateCell(connection, cellI, cellJ, new ProgressLogger(), processedReceivers));
                } while (output.isEmpty());
            }
            // Consume cell
            PropagationResultPtRecord record = output.pop();
            Object[] row = new Object[COLUMN_COUNT];
            row[0] = record.getReceiverRecordRow();
            processedReceivers.add(record.getReceiverRecordRow());
            row[1] = record.getReceiverLvl();
            row[2] = record.getCellId();
            return row;
        }

        @Override
        public void close() {
            output.clear();
        }

        @Override
        public void reset() throws SQLException {
            cellI = -1;
            cellJ = 0;
            output.clear();
        }
    }
}
