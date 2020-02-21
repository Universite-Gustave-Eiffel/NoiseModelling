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

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.locationtech.jts.geom.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Driver to import ESRI ASCII Raster file as polygons
 *
 * @author Nicolas Fortin (Université Gustave Eiffel 2020)
 */
public class AscReaderDriver {
    private static final int BATCH_MAX_SIZE = 100;
    private static final int BUFFER_SIZE = 16384;
    private boolean as3DPoint = false;
    private Envelope extractEnvelope = null;
    private int limit = -1;

    /**
     * @return If true ASC is imported as 3D points cloud, Raster is imported in pixel polygons otherwise.
     */
    public boolean isAs3DPoint() {
        return as3DPoint;
    }

    /**
     * @param as3DPoint If true ASC is imported as 3D points cloud, Raster is imported in pixel polygons otherwise.
     */
    public void setAs3DPoint(boolean as3DPoint) {
        this.as3DPoint = as3DPoint;
    }

    /**
     * @return Imported geometries are filtered using this optional envelope
     */
    public Envelope getExtractEnvelope() {
        return extractEnvelope;
    }

    /**
     * @param extractEnvelope Imported geometries are filtered using this optional envelope. Set Null object for no filtering.
     */
    public void setExtractEnvelope(Envelope extractEnvelope) {
        this.extractEnvelope = extractEnvelope;
    }

    /**
     * @return Maximum number of database insertions (-1 no limitation)
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit Set the maximum of database insertion (-1 no limitation)
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Read asc stream
     *
     * @param connection
     * @param inputStream
     * @param progress
     * @param tableReference
     * @throws SQLException
     * @throws IOException
     */
    public void read(Connection connection, InputStream inputStream, ProgressVisitor progress, String tableReference,
                     int srid) throws SQLException, IOException {
        BufferedInputStream bof = new BufferedInputStream(inputStream, BUFFER_SIZE);
        String lastWord = "";
        try {
            Scanner scanner = new Scanner(bof);
            // Read HEADER
            // NCOLS
            lastWord = scanner.next();
            if (!lastWord.equalsIgnoreCase("NCOLS")) {
                throw new IOException("Unexpected word " + lastWord);
            }
            // XXX
            lastWord = scanner.next();
            int ncols = Integer.parseInt(lastWord);
            if (ncols <= 0) {
                throw new IOException("NCOLS <= 0");
            }
            // NROWS
            lastWord = scanner.next();
            if (!lastWord.equalsIgnoreCase("NROWS")) {
                throw new IOException("Unexpected word " + lastWord);
            }
            // XXX
            lastWord = scanner.next();
            int nrows = Integer.parseInt(lastWord);
            if (nrows <= 0) {
                throw new IOException("NROWS <= 0");
            }
            // XLLCENTER or XLLCORNER
            lastWord = scanner.next();
            if (!(lastWord.equalsIgnoreCase("XLLCENTER") || lastWord.equalsIgnoreCase("XLLCORNER"))) {
                throw new IOException("Unexpected word " + lastWord);
            }
            boolean isXCenter = lastWord.equalsIgnoreCase("XLLCENTER");
            // XXX
            lastWord = scanner.next();
            double xValue = Double.parseDouble(lastWord);

            // YLLCENTER or YLLCORNER
            lastWord = scanner.next();
            if (!(lastWord.equalsIgnoreCase("YLLCENTER") || lastWord.equalsIgnoreCase("YLLCORNER"))) {
                throw new IOException("Unexpected word " + lastWord);
            }
            boolean isYCenter = lastWord.equalsIgnoreCase("YLLCENTER");
            // XXX
            lastWord = scanner.next();
            double yValue = Double.parseDouble(lastWord);

            // CELLSIZE
            lastWord = scanner.next();
            if (!lastWord.equalsIgnoreCase("CELLSIZE")) {
                throw new IOException("Unexpected word " + lastWord);
            }
            // XXX
            lastWord = scanner.next();
            double cellSize = Double.parseDouble(lastWord);
            // Compute offsets
            if (isXCenter) {
                xValue = xValue - cellSize / 2;
            }
            if (isYCenter) {
                yValue = yValue + cellSize * nrows - cellSize / 2;
            } else {
                yValue = yValue + cellSize * nrows;
            }
            // Optional NODATA_VALUE
            lastWord = scanner.next();
            boolean readFirst = false;
            int noData = -9999;
            if (lastWord.equalsIgnoreCase("NODATA_VALUE")) {
                readFirst = true;
                // XXX
                lastWord = scanner.next();
                noData = Integer.parseInt(lastWord);
            }

            Statement st = connection.createStatement();
            PreparedStatement preparedStatement;
            if(as3DPoint) {
                st.execute("CREATE TABLE " + tableReference + "(PK SERIAL NOT NULL, THE_GEOM GEOMETRY, " + " CONSTRAINT ASC_PK PRIMARY KEY (PK))");
                preparedStatement = connection.prepareStatement("INSERT INTO " + tableReference +
                        "(the_geom) VALUES (?)");
            } else {
                st.execute("CREATE TABLE " + tableReference + "(PK SERIAL NOT NULL, THE_GEOM GEOMETRY,Z int, " + " CONSTRAINT ASC_PK PRIMARY KEY (PK))");
                preparedStatement = connection.prepareStatement("INSERT INTO " + tableReference +
                        "(the_geom, Z) VALUES (?, ?)");
            }
            // Read data
            GeometryFactory factory = new GeometryFactory();
            int batchSize = 0;
            int inserted = 0;
            int firstRow = 0;
            int firstCol = 0;
            int lastRow = nrows;
            int lastCol = ncols;
            // Compute envelope
            if(extractEnvelope != null) {
                firstCol = (int)Math.floor((extractEnvelope.getMinX() - xValue) / cellSize);
                lastCol = (int)Math.ceil((extractEnvelope.getMaxX() - xValue) / cellSize);
                firstRow = nrows - (int)Math.ceil((extractEnvelope.getMaxY() - (yValue - cellSize * nrows)) / cellSize);
                lastRow = nrows - (int)Math.ceil((extractEnvelope.getMinY() - (yValue - cellSize * nrows)) / cellSize);
            }
            ProgressVisitor cellProgress = new EmptyProgressVisitor();
            if (progress != null) {
                cellProgress = progress.subProcess(lastRow);
            }
            for (int i = 0; i < nrows && (limit == -1 || inserted < limit); i++) {
                for (int j = 0; j < ncols && (limit == -1 || inserted < limit); j++) {
                    if (readFirst) {
                        lastWord = scanner.next();
                    } else {
                        readFirst = true;
                    }
                    if(extractEnvelope == null || (i >= firstRow && i <= lastRow && j >= firstCol && j <= lastCol)) {
                        int data = Integer.parseInt(lastWord);
                        double x = xValue + j * cellSize;
                        double y = yValue - i * cellSize;
                        if (as3DPoint) {
                            if (data != noData) {
                                Point cell = factory.createPoint(new Coordinate(new Coordinate(x + cellSize / 2, y - cellSize / 2, data)));
                                cell.setSRID(srid);
                                preparedStatement.setObject(1, cell);
                                preparedStatement.addBatch();
                                inserted++;
                                batchSize++;
                            }
                        } else {
                            Polygon cell = factory.createPolygon(new Coordinate[]{new Coordinate(x, y), new Coordinate(x, y - cellSize), new Coordinate(x + cellSize, y - cellSize), new Coordinate(x + cellSize, y), new Coordinate(x, y)});
                            cell.setSRID(srid);
                            preparedStatement.setObject(1, cell);
                            if (data != noData) {
                                preparedStatement.setObject(2, data);
                            } else {
                                preparedStatement.setNull(2, Types.INTEGER);
                            }
                            preparedStatement.addBatch();
                            inserted++;
                            batchSize++;
                        }
                        if (batchSize >= BATCH_MAX_SIZE) {
                            preparedStatement.executeBatch();
                            preparedStatement.clearBatch();
                            batchSize = 0;
                        }
                    }
                }
                cellProgress.endStep();
                if(i > lastRow) {
                    break;
                }
            }
            if (batchSize > 0) {
                preparedStatement.executeBatch();
            }
        } catch (NoSuchElementException | NumberFormatException ex) {
            throw new SQLException("Unexpected word " + lastWord, ex);
        }
    }

}
