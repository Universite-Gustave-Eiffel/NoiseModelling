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

import org.locationtech.jts.geom.Geometry;
import org.h2.tools.SimpleResultSet;
import org.h2.tools.SimpleRowSource;
import org.h2gis.utilities.TableUtilities;
import org.h2gis.api.AbstractFunction;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ScalarFunction;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.PropagationResultTriRecord;
import org.orbisgis.noisemap.core.jdbc.TriangleNoiseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sound propagation from punctual sound sources to ponctual receivers created by a delaunay triangulation of specified
 * buildings geometry. User can specify Height of buildings, and topology.
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class BR_TriGrid3D extends AbstractFunction implements ScalarFunction {
    private static final int COLUMN_COUNT = 6;
    private static final Logger LOGGER = LoggerFactory.getLogger("gui."+BR_TriGrid3D.class);

    public BR_TriGrid3D() {
        addProperty(PROP_REMARKS , "## BR_TriGrid3D\n" +
                "\n" +
                "Table function.Sound propagation in 3 dimension. Return 6 columns. TRI_ID integer,THE_GEOM polygon,W_V1 double,W_V2 double,W_V3 double,CELL_ID integer.\n" +
                "\n" +
                "BR_TriGrid3D(String buildingsTable, String heightFieldName, String sourcesTable, " +
                "String sourcesTableSoundFieldName, String groundTypeTable, String demTable, " +
                "double maximumPropagationDistance, double maximumWallSeekingDistance, double roadsWidth, " +
                "double receiversDensification, double maximumAreaOfTriangle, int soundReflectionOrder, " +
                "int soundDiffractionOrder, double wallAlpha)\n" +
                " \n" +
                " - **buildingsTable** table identifier that contain a geometry column of type POLYGON. Polygon Z " +
                "value is the ground level.\n" +
                " - **heightFieldName** column identifier in the buildings table that hold building height in meter" +
                ".\n" +
                " - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The " +
                "table must contain the sound emission level in dB(A).\n" +
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
                " POINT with X,Y and Z value. \n" +
                " - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter " +
                "are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and " +
                "memory usage.\n" +
                " - **maximumWallSeekingDistance** From the direct propagation line source-receiver, " +
                "wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts " +
                "performance.\n" +
                " - **roadsWidth** Start creating receivers from this distance. Should be superior than 1 meter.\n" +
                " - **receiversDensification** Create additional receivers at this distance from sources. (0 to " +
                "disable)\n" +
                " - **maximumAreaOfTriangle** Maximum area for noise map triangular mesh. Smaller area means more " +
                "receivers. Impacts performance.\n" +
                " - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. " +
                "Recommended value is 2.\n" +
                " - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended " +
                "value is 1.\n" +
                " - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete.");
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
     * @param heightFieldName            Optional (empty if not available) Field name of building height on buildingsTable
     * @param sourcesTable               Source table table (linestring or point)
     * @param sourcesTableSoundFieldName Field name to extract from sources table. Frequency is added on right.
     * @param groundTypeTable            Optional (empty if not available) Soil category. This is a table with a polygon column and a column 'G' double.
     * @param demTable                   Optional (empty if not available) Point table of digital elevation model.
     * @param maximumPropagationDistance Propagation distance limitation.
     * @param maximumWallSeekingDistance Maximum reflection distance from the source-receiver propagation line.
     * @param roadsWidth                 Buffer without receivers applied on roads on final noise map.
     * @param soundReflectionOrder       Sound reflection order on walls.
     * @param soundDiffractionOrder      Source diffraction order on corners.
     * @param wallAlpha                  Wall absorption coefficient.
     * @return A table with 3 columns GID(extracted from receivers table), W energy receiver by receiver, cellid cell identifier.
     * @throws java.sql.SQLException
     */
    public static ResultSet noisePropagation(Connection connection, String buildingsTable, String heightFieldName,
                                             String sourcesTable, String sourcesTableSoundFieldName,
                                             String groundTypeTable, String demTable,
                                             double maximumPropagationDistance, double maximumWallSeekingDistance,
                                             double roadsWidth, double receiversDensification,
                                             double maximumAreaOfTriangle, int soundReflectionOrder,
                                             int soundDiffractionOrder, double wallAlpha) throws SQLException {
        if (maximumPropagationDistance < maximumWallSeekingDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }        SimpleResultSet rs;
        if(TableUtilities.isColumnListConnection(connection)) {
            // Only rs columns is necessary
            rs = new SimpleResultSet();
            feedColumns(rs);
        } else {
            connection = SFSUtilities.wrapConnection(connection);
            TriangleNoiseMap noiseMap = new TriangleNoiseMap(TableLocation.capsIdentifier(buildingsTable, true),
                    TableLocation.capsIdentifier(sourcesTable, true));
            noiseMap.setHeightField(heightFieldName);
            noiseMap.setSoilTableName(groundTypeTable);
            noiseMap.setDemTable(demTable);
            noiseMap.setSound_lvl_field(sourcesTableSoundFieldName);
            noiseMap.setMaximumPropagationDistance(maximumPropagationDistance);
            noiseMap.setMaximumReflectionDistance(maximumWallSeekingDistance);
            noiseMap.setSoundReflectionOrder(soundReflectionOrder);
            noiseMap.setSoundDiffractionOrder(soundDiffractionOrder);
            noiseMap.setMaximumArea(maximumAreaOfTriangle);
            noiseMap.setSourceDensification(receiversDensification);
            noiseMap.setRoadWidth(roadsWidth);
            noiseMap.setWallAbsorption(wallAlpha);
            noiseMap.initialize(connection, new EmptyProgressVisitor());
            rs = new SimpleResultSet(new TriangleRowSource(noiseMap, connection));
            feedColumns(rs);
        }
        return rs;
    }

    /**
     * Construct a ResultSet using parameter and core noise-map.
     *
     * @param connection                 Active connection, never closed (provided and hidden by H2)
     * @param computationEnvelope        Computation area
     * @param buildingsTable             Buildings table name (polygons)
     * @param heightFieldName            Optional (empty if not available) Field name of building height on buildingsTable
     * @param sourcesTable               Source table table (linestring or point)
     * @param sourcesTableSoundFieldName Field name to extract from sources table. Frequency is added on right.
     * @param groundTypeTable            Optional (empty if not available) Soil category. This is a table with a polygon column and a column 'G' double.
     * @param demTable                   Optional (empty if not available) Point table of digital elevation model.
     * @param maximumPropagationDistance Propagation distance limitation.
     * @param maximumWallSeekingDistance Maximum reflection distance from the source-receiver propagation line.
     * @param roadsWidth                 Buffer without receivers applied on roads on final noise map.
     * @param soundReflectionOrder       Sound reflection order on walls.
     * @param soundDiffractionOrder      Source diffraction order on corners.
     * @param wallAlpha                  Wall absorption coefficient.
     * @return A table with 3 columns GID(extracted from receivers table), W energy receiver by receiver, cellid cell identifier.
     * @throws java.sql.SQLException
     */
    public static ResultSet noisePropagation(Connection connection,Geometry computationEnvelope, String buildingsTable, String heightFieldName,
                                             String sourcesTable, String sourcesTableSoundFieldName,
                                             String groundTypeTable, String demTable,
                                             double maximumPropagationDistance, double maximumWallSeekingDistance,
                                             double roadsWidth, double receiversDensification,
                                             double maximumAreaOfTriangle, int soundReflectionOrder,
                                             int soundDiffractionOrder, double wallAlpha) throws SQLException {
        if (maximumPropagationDistance < maximumWallSeekingDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }        SimpleResultSet rs;
        if(TableUtilities.isColumnListConnection(connection)) {
            // Only rs columns is necessary
            rs = new SimpleResultSet();
            feedColumns(rs);
        } else {
            connection = SFSUtilities.wrapConnection(connection);
            TriangleNoiseMap noiseMap = new TriangleNoiseMap(TableLocation.capsIdentifier(buildingsTable, true),
                    TableLocation.capsIdentifier(sourcesTable, true));
            noiseMap.setMainEnvelope(computationEnvelope.getEnvelopeInternal());
            noiseMap.setHeightField(heightFieldName);
            noiseMap.setSoilTableName(groundTypeTable);
            noiseMap.setDemTable(demTable);
            noiseMap.setSound_lvl_field(sourcesTableSoundFieldName);
            noiseMap.setMaximumPropagationDistance(maximumPropagationDistance);
            noiseMap.setMaximumReflectionDistance(maximumWallSeekingDistance);
            noiseMap.setSoundReflectionOrder(soundReflectionOrder);
            noiseMap.setSoundDiffractionOrder(soundDiffractionOrder);
            noiseMap.setMaximumArea(maximumAreaOfTriangle);
            noiseMap.setSourceDensification(receiversDensification);
            noiseMap.setRoadWidth(roadsWidth);
            noiseMap.setWallAbsorption(wallAlpha);
            noiseMap.initialize(connection, new EmptyProgressVisitor());
            rs = new SimpleResultSet(new TriangleRowSource(noiseMap, connection));
            feedColumns(rs);
        }
        return rs;
    }

    private static void feedColumns(SimpleResultSet rs) {
        rs.addColumn("TRI_ID", Types.INTEGER, 10, 11);
        rs.addColumn("THE_GEOM", Types.JAVA_OBJECT, "GEOMETRY", 0, 0);
        rs.addColumn("W_V1", Types.DOUBLE, 17, 24);
        rs.addColumn("W_V2", Types.DOUBLE, 17, 24);
        rs.addColumn("W_V3", Types.DOUBLE, 17, 24);
        rs.addColumn("CELL_ID", Types.INTEGER, 10, 11);
    }

    private static class TriangleRowSource implements SimpleRowSource {
        private Deque<PropagationResultTriRecord> records = new ArrayDeque<PropagationResultTriRecord>();
        private int cellI = -1;
        private int cellJ = 0;
        private TriangleNoiseMap noiseMap;
        private Connection connection;

        private TriangleRowSource(TriangleNoiseMap noiseMap, Connection connection) {
            this.noiseMap = noiseMap;
            this.connection = connection;
        }

        @Override
        public Object[] readRow() throws SQLException {
            if(records.isEmpty()) {
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
                    LOGGER.info("Evaluate cell " + (cellI + cellJ * noiseMap.getGridDim() + 1) + "/" + ((int)Math.pow
                            (noiseMap.getGridDim(), 2)));
                    records.addAll(noiseMap.evaluateCell(connection, cellI, cellJ, new ProgressLogger()));
                } while (records.isEmpty());
            }
            // Consume cell
            PropagationResultTriRecord record = records.pop();
            Object[] row = new Object[COLUMN_COUNT];
            row[0] = record.getTriId();
            row[1] = record.getTriangle();
            row[2] = record.getV1();
            row[3] = record.getV2();
            row[4] = record.getV3();
            row[5] = record.getCellId();
            return row;
        }

        @Override
        public void close() {
            records.clear();
        }

        @Override
        public void reset() throws SQLException {
            cellI = -1;
            cellJ = 0;
            records.clear();
        }
    }
}
