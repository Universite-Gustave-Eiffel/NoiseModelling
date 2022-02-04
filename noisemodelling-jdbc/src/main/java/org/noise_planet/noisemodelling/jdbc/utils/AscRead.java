/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <http://www.h2database.com>. H2GIS is developed by CNRS
 * <http://www.cnrs.fr/>.
 *
 * This code is part of the H2GIS project. H2GIS is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; version 3.0 of
 * the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 */
package org.noise_planet.noisemodelling.jdbc.utils;

import org.h2.value.*;
import org.h2gis.api.AbstractFunction;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.api.ScalarFunction;
import org.h2gis.functions.io.utility.PRJUtil;
import org.h2gis.utilities.URIUtilities;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SQL function to import ESRI ASCII Raster file as points or polygons table.
 *
 * @author Nicolas Fortin (Université Gustave Eiffel 2020)
 */
public class AscRead extends AbstractFunction implements ScalarFunction {

    public AscRead() {
        addProperty(PROP_REMARKS, "Import ESRI ASCII Raster file as point geometries\n"
                + "Pixels are converted into PointZ with Z as the pixel value\n"
                + "CALL ASCREAD('dem.asc');\n"
                + "CALL ASCREAD('dem.asc',TYPE);\n"
                + "TYPE of z data 1 for integer, 2 for double (default 2)\n"
                + "CALL ASCREAD('dem.asc', 'MYTABLE');\n"
                + "CALL ASCREAD('dem.asc', 'MYTABLE', TYPE);\n"
                + "TYPE of z data 1 for integer, 2 for double (default 2)"
                + "CALL ASCREAD('dem.asc', 'MYTABLE', GEOM_FILTER, DOWNSCALE_INT, AS_POLYGONS);\n"
                + "GEOM_FILTER - Extract only pixels that intersects the provided geometry envelope, null to disable filter\n"
                + "DOWNSCALE_INT - Coefficient used for exporting less cells (1 all cells, 2 for size / 2)\n"
                + "AS_POLYGONS - If true pixels are converted to polygons. (default false return points)\n");
    }

    @Override
    public String getJavaStaticMethod() {
        return "readAscii";
    }

    /**
     * Read the ASCII file.
     *
     * @param connection
     * @param fileName
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName) throws IOException, SQLException {
        final String name = URIUtilities.fileFromString(fileName).getName();
        String tableName = name.substring(0, name.lastIndexOf(".")).toUpperCase().replace(".", "_");
        if (tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            readAscii(connection, fileName, ValueVarchar.get(tableName));
        } else {
            throw new SQLException("The file name contains unsupported characters");
        }
    }

    /**
     * Read the ASCII file.
     *
     * @param connection
     * @param fileName
     * @param option
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName, Value option) throws IOException, SQLException {
        int zType = 2;
        String tableReference = null;
        boolean deletTable = false;
        Geometry envelope = null;
        if (option instanceof ValueInteger) {
            zType = option.getInt();
            if (!(zType == 1 || zType == 2)) {
                throw new SQLException("Please use 1 for integer or 2 for double conversion");
            }
        } else if (option instanceof ValueVarchar) {
            tableReference = option.getString();
        } else if (option instanceof ValueBoolean) {
            deletTable = option.getBoolean();
        } else if (option instanceof ValueGeometry) {
            envelope = ((ValueGeometry) option).getGeometry();
        } else if (!(option instanceof ValueNull)) {
            throw new SQLException("Supported optional parameter is integer for z type or varchar for table name");
        }
        File outputFile = URIUtilities.fileFromString(fileName);
        if (tableReference == null) {
            final String name = outputFile.getName();
            String tableName = name.substring(0, name.lastIndexOf(".")).replace(".", "_").toUpperCase();
            if (tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                tableReference = tableName;
            } else {
                throw new SQLException("The file name contains unsupported characters");
            }
        }
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        if (envelope != null && !envelope.isEmpty()) {
            ascReaderDriver.setExtractEnvelope(envelope.getEnvelopeInternal());
        }
        ascReaderDriver.setZType(zType);
        ascReaderDriver.setDeleteTable(deletTable);
        importFile(connection, tableReference, outputFile, new EmptyProgressVisitor(), ascReaderDriver);
    }

    /**
     * Read the ASCII file.
     *
     * @param connection
     * @param fileName
     * @param tableReference
     * @param option
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName, String tableReference, Value option) throws IOException, SQLException {
        int zType = 2;
        boolean deletTable = false;
        Geometry envelope = null;
        if (option instanceof ValueInteger) {
            zType = option.getInt();
            if (!(zType == 1 || zType == 2)) {
                throw new SQLException("Please use 1 for integer or 2 for double conversion");
            }
        } else if (option instanceof ValueBoolean) {
            deletTable = option.getBoolean();
        } else if (option instanceof ValueGeometry) {
            envelope = ((ValueGeometry) option).getGeometry();
        } else if (!(option instanceof ValueNull)) {
            throw new SQLException("Supported optional parameter is integer for z type or varchar for table name");
        }
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        if (envelope != null && !envelope.isEmpty()) {
            ascReaderDriver.setExtractEnvelope(envelope.getEnvelopeInternal());
        }
        ascReaderDriver.setAs3DPoint(true);
        ascReaderDriver.setZType(zType);
        ascReaderDriver.setDeleteTable(deletTable);
        importFile(connection, tableReference, URIUtilities.fileFromString(fileName), new EmptyProgressVisitor(), ascReaderDriver);
    }

    /**
     * Import the file
     * @param connection
     * @param tableReference
     * @param outputFile
     * @param progress
     * @param ascReaderDriver
     * @throws IOException
     * @throws SQLException
     */
    private static void importFile(Connection connection, String tableReference, File outputFile, ProgressVisitor progress, AscReaderDriver ascReaderDriver) throws IOException, SQLException {
        int srid = 0;
        String filePath = outputFile.getAbsolutePath();
        final int dotIndex = filePath.lastIndexOf('.');
        final String fileNamePrefix = filePath.substring(0, dotIndex);
        File prjFile = new File(fileNamePrefix + ".prj");
        if (prjFile.exists()) {
            srid = PRJUtil.getSRID(prjFile);
        }
        ascReaderDriver.read(connection, outputFile, progress, tableReference, srid);
    }

    /**
     * Import a small subset of ASC file.
     *
     * @param connection
     * @param fileName
     * @param tableReference
     * @param envelope Extract only pixels that intersects the provided geometry
     * envelope, null to disable filter
     * @param downScale Coefficient used for exporting less cells (1 all cells,
     * 2 for size / 2)
     * @param extractAsPolygons If true pixels are converted to polygon.
     * (default false)
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName, String tableReference, Geometry envelope, int downScale, boolean extractAsPolygons) throws IOException, SQLException {
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        if (envelope != null && !envelope.isEmpty()) {
            ascReaderDriver.setExtractEnvelope(envelope.getEnvelopeInternal());
        }
        if (downScale > 1) {
            ascReaderDriver.setDownScale(downScale);
        }
        ascReaderDriver.setAs3DPoint(!extractAsPolygons);
        importFile(connection, tableReference, URIUtilities.fileFromString(fileName), new EmptyProgressVisitor(), ascReaderDriver);
    }

    /**
     * Import a small subset of ASC file.
     *
     * @param connection
     * @param fileName
     * @param tableReference
     * @param envelope Extract only pixels that intersects the provided geometry
     * envelope, null to disable filter
     * @param downScale Coefficient used for exporting less cells (1 all cells,
     * 2 for size / 2)
     * @param extractAsPolygons If true pixels are converted to polygon.
     * (default false)
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName, String tableReference, Geometry envelope, int downScale, boolean extractAsPolygons, boolean deleteTable) throws IOException, SQLException {
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        if (envelope != null && !envelope.isEmpty()) {
            ascReaderDriver.setExtractEnvelope(envelope.getEnvelopeInternal());
        }
        if (downScale > 1) {
            ascReaderDriver.setDownScale(downScale);
        }
        ascReaderDriver.setAs3DPoint(!extractAsPolygons);
        ascReaderDriver.setDeleteTable(deleteTable);
        importFile(connection, tableReference, URIUtilities.fileFromString(fileName), new EmptyProgressVisitor(), ascReaderDriver);
    }

    /**
     * Import a small subset of ASC file.
     *
     * @param connection
     * @param fileName
     * @param tableReference
     * @param envelope Extract only pixels that intersects the provided geometry
     * envelope, null to disable filter
     * @param downScale Coefficient used for exporting less cells (1 all cells,
     * 2 for size / 2)
     * @param extractAsPolygons If true pixels are converted to polygon.
     * (default false)
     * @throws IOException
     * @throws SQLException
     */
    public static void readAscii(Connection connection, String fileName, String tableReference, Geometry envelope, int downScale, boolean extractAsPolygons, boolean deleteTable, String encoding, int zType) throws IOException, SQLException {
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        if (envelope != null && !envelope.isEmpty()) {
            ascReaderDriver.setExtractEnvelope(envelope.getEnvelopeInternal());
        }
        if (downScale > 1) {
            ascReaderDriver.setDownScale(downScale);
        }
        ascReaderDriver.setAs3DPoint(!extractAsPolygons);
        ascReaderDriver.setEncoding(encoding);
        ascReaderDriver.setZType(zType);
        ascReaderDriver.setDeleteTable(deleteTable);
        importFile(connection, tableReference, URIUtilities.fileFromString(fileName), new EmptyProgressVisitor(), ascReaderDriver);
    }
}
