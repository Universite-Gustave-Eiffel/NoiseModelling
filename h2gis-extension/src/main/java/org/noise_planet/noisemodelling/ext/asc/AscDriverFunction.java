/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.noise_planet.noisemodelling.ext.asc;

import org.h2gis.api.DriverFunction;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.utility.PRJUtil;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.TableLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * GeoJSON driver to import a GeoJSON file and export a spatial table in a
 * GeoJSON 1.0 file.
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
        return new String[]{"asc"};
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
    public void exportTable(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException, IOException{

    }

    @Override
    public void exportTable(Connection connection, String tableReference, File fileName, ProgressVisitor progress, String encoding) throws SQLException, IOException{

    }

    @Override
    public void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress)
            throws SQLException, IOException {
        AscReaderDriver ascReaderDriver = new AscReaderDriver();
        int srid = 0;
        String filePath = fileName.getAbsolutePath();
        final int dotIndex = filePath.lastIndexOf('.');
        final String fileNamePrefix = filePath.substring(0, dotIndex);
        File prjFile = new File(fileNamePrefix+".prj");
        if(prjFile.exists()) {
            srid = PRJUtil.getSRID(prjFile);
        }
        try(FileInputStream fos = new FileInputStream(fileName)) {
            ascReaderDriver.read(connection, fos, progress, tableReference, srid);
        }
    }

    @Override
    public void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress,
                           String options) throws SQLException, IOException {
        importFile(connection, tableReference, fileName, progress);
    }

    @Override
    public void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress,
                           boolean deleteTables) throws SQLException, IOException {

        if(deleteTables) {
            final boolean isH2 = JDBCUtilities.isH2DataBase(connection.getMetaData());
            TableLocation requestedTable = TableLocation.parse(tableReference, isH2);
            Statement stmt = connection.createStatement();
            stmt.execute("DROP TABLE IF EXISTS " + requestedTable);
            stmt.close();
        }

        importFile(connection, tableReference, fileName, progress);
    }
}
