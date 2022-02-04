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

import org.h2gis.api.DriverFunction;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.DriverManager;
import org.h2gis.functions.io.utility.PRJUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Asc driver to import ESRI ASCII Raster file as polygons
 *
 * @author Nicolas Fortin (Université Gustave Eiffel 2020)
 */
public class AscDriverFunction implements DriverFunction {

    @Override
    public IMPORT_DRIVER_TYPE getImportDriverType() {
        return IMPORT_DRIVER_TYPE.COPY;
    }

    @Override
    public String[] getImportFormats() {
        return new String[]{"asc", "asc.gz"};
    }

    @Override
    public String[] getExportFormats() {
        return new String[]{};
    }

    @Override
    public String getFormatDescription(String format) {
        if (format.equalsIgnoreCase("asc")) {
            return "ESRI ASCII Raster format";
        } else {
            return "";
        }
    }

    @Override
    public boolean isSpatialFormat(String extension) {
        return extension != null && extension.equalsIgnoreCase("asc");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, boolean deleteFiles, ProgressVisitor progress) throws SQLException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, String options, boolean deleteFiles, ProgressVisitor progress) throws SQLException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] exportTable(Connection connection, String tableReference, File fileName, String encoding, ProgressVisitor progress) throws SQLException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress)
            throws SQLException, IOException {
        return importFile(connection, tableReference, fileName, null,false, progress);
    }

    @Override
    public String[] importFile(Connection connection, String tableReference, File fileName, String options, ProgressVisitor progress
    ) throws SQLException, IOException {
        return importFile(connection, tableReference, fileName, progress);
    }

    @Override
    public String[] importFile(Connection connection, String tableReference, File fileName, boolean deleteTables, ProgressVisitor progress
    ) throws SQLException, IOException {
        return importFile(connection, tableReference, fileName, null,deleteTables, progress);
    }

    @Override
    public String[] importFile(Connection connection, String tableReference, File fileName, String encoding, boolean deleteTables, ProgressVisitor progress) throws SQLException, IOException {
        DriverManager.check(connection,tableReference,fileName,progress);
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        ascReaderDriver.setDeleteTable(deleteTables);
        ascReaderDriver.setEncoding(encoding);
        int srid = 0;
        String filePath = fileName.getAbsolutePath();
        final int dotIndex = filePath.lastIndexOf('.');
        final String fileNamePrefix = filePath.substring(0, dotIndex);
        File prjFile = new File(fileNamePrefix + ".prj");
        if (prjFile.exists()) {
            srid = PRJUtil.getSRID(prjFile);
        }
        return ascReaderDriver.read(connection, fileName, progress, tableReference, srid);
    }
}
