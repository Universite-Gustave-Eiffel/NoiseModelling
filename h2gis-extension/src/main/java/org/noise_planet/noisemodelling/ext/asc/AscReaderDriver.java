/*
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <http://www.h2database.com>. H2GIS is developed by CNRS
 * <http://www.cnrs.fr/>.
 *
 * This code is part of the H2GIS project. H2GIS is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 */

package org.noise_planet.noisemodelling.ext.asc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.locationtech.jts.geom.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Driver to import ESRI ASCII Raster file as polygons
 *
 * This class is written to directly access the ESRI ascii grid format.
 *
 * The ASCII grid data file format comprises a few lines of header data followed
 * by lists of cell values. The header data includes the following keywords and
 * values:
 *
 * ncols : number of columns in the data set.
 *
 * nrows : number of rows in the data set.
 *
 * xllcorner : x-coordinate of the west border of the LowerLeft corner.
 *
 * yllcorner : y-coordinate of the south border of the LowerLeft corner.
 *
 * cellsize : size of the square cell of the data set.
 *
 * NODATA_value : arbitrary value assigned to unknown cells.
 *
 * @author Nicolas Fortin (Université Gustave Eiffel 2020)
 * @author Erwan Bocher, CNRS, 2020
 */
public class AscReaderDriver {
    private static final int BATCH_MAX_SIZE = 100;
    private static final int BUFFER_SIZE = 16384;
    private boolean as3DPoint = false;
    private Envelope extractEnvelope = null;
    private int downScale = 1;
    private String lastWord = "";

    private int nrows;
    private int ncols;
    private double cellSize;
    private double yValue;
    private double xValue;
    private boolean readFirst;
    private double noData;
    private int zType = 2;
    private boolean deleteTable = false;
    private String encoding = "UTF-8";
    private boolean importNodata = false;

    /**
     * @return If true ASC is imported as 3D points cloud, Raster is imported in
     * pixel polygons otherwise.
     */
    public boolean isAs3DPoint() {
        return as3DPoint;
    }

    /**
     * @param as3DPoint If true ASC is imported as 3D points cloud, Raster is
     * imported in pixel polygons otherwise.
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
     * @param extractEnvelope Imported geometries are filtered using this
     * optional envelope. Set Null object for no filtering.
     */
    public void setExtractEnvelope(Envelope extractEnvelope) {
        this.extractEnvelope = extractEnvelope;
    }

    /**
     * @return Coefficient used for exporting less cells (1 all cells, 2 for
     * size / 2)
     */
    public int getDownScale() {
        return downScale;
    }

    /**
     * @param downScale Coefficient used for exporting less cells (1 all cells,
     * 2 for size / 2)
     */
    public void setDownScale(int downScale) {
        this.downScale = downScale;
    }

    private void readHeader(Scanner scanner) throws IOException {
        // NCOLS
        lastWord = scanner.next();
        if (!lastWord.equalsIgnoreCase("NCOLS")) {
            throw new IOException("Unexpected word " + lastWord);
        }
        // XXX
        lastWord = scanner.next();
        ncols = Integer.parseInt(lastWord);
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
        nrows = Integer.parseInt(lastWord);
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
        xValue = Double.parseDouble(lastWord);

        // YLLCENTER or YLLCORNER
        lastWord = scanner.next();
        if (!(lastWord.equalsIgnoreCase("YLLCENTER") || lastWord.equalsIgnoreCase("YLLCORNER"))) {
            throw new IOException("Unexpected word " + lastWord);
        }
        boolean isYCenter = lastWord.equalsIgnoreCase("YLLCENTER");
        // XXX
        lastWord = scanner.next();
        yValue = Double.parseDouble(lastWord);

        // CELLSIZE
        lastWord = scanner.next();
        if (!lastWord.equalsIgnoreCase("CELLSIZE")) {
            throw new IOException("Unexpected word " + lastWord);
        }
        // XXX
        lastWord = scanner.next();
        cellSize = Double.parseDouble(lastWord);
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
        readFirst = false;
        noData = -9999;
        if (lastWord.equalsIgnoreCase("NODATA_VALUE")) {
            readFirst = true;
            // XXX
            lastWord = scanner.next();
            noData = Double.parseDouble(lastWord);

        }
    }

    /**
     * Read the ascii file from inpustream
     *
     * @param connection
     * @param inputStream
     * @param progress
     * @param outputTable
     * @param srid
     * @throws UnsupportedEncodingException
     * @throws SQLException
     * @return output table name
     */

    public String read(Connection connection, InputStream inputStream, ProgressVisitor progress, String outputTable,
                           int srid) throws UnsupportedEncodingException, SQLException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream, BUFFER_SIZE), encoding));
        try {
            Scanner scanner = new Scanner(reader);
            // Read HEADER
            readHeader(scanner);
            // Read values
            Statement st = connection.createStatement();
            PreparedStatement preparedStatement;

            if (!JDBCUtilities.tableExists(connection,outputTable)){
                if (as3DPoint) {
                    if (zType == 1) {
                        st.execute("CREATE TABLE " + outputTable + "(PK SERIAL PRIMARY KEY, THE_GEOM POINT, Z integer)");
                    } else {
                        st.execute("CREATE TABLE " + outputTable + "(PK SERIAL PRIMARY KEY, THE_GEOM POINT, Z double precision)");
                    }
                } else {
                    if (zType == 1) {
                        st.execute("CREATE TABLE " + outputTable + "(PK SERIAL PRIMARY KEY, THE_GEOM POLYGON, Z integer)");
                    } else {
                        st.execute("CREATE TABLE " + outputTable + "(PK SERIAL PRIMARY KEY, THE_GEOM POLYGON, Z double precision)");
                    }
                }
            }

            preparedStatement = connection.prepareStatement("INSERT INTO " + outputTable
                    + "(the_geom, Z) VALUES (?, ?)");
            // Read data
            GeometryFactory factory = new GeometryFactory(new PrecisionModel(),srid);
            int batchSize = 0;
            int firstRow = 0;
            int firstCol = 0;
            int lastRow = nrows;
            int lastCol = ncols;
            // Compute envelope
            if (extractEnvelope != null) {
                firstCol = (int) Math.floor((extractEnvelope.getMinX() - xValue) / cellSize);
                lastCol = (int) Math.ceil((extractEnvelope.getMaxX() - xValue) / cellSize);
                firstRow = nrows - (int) Math.ceil((extractEnvelope.getMaxY() - (yValue - cellSize * nrows)) / cellSize);
                lastRow = nrows - (int) Math.ceil((extractEnvelope.getMinY() - (yValue - cellSize * nrows)) / cellSize);
            }
            ProgressVisitor cellProgress = new EmptyProgressVisitor();
            if (progress != null) {
                cellProgress = progress.subProcess(lastRow);
            }
            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    if (readFirst) {
                        lastWord = scanner.next();
                    } else {
                        readFirst = true;
                    }

                    if ((downScale == 1 || (i % downScale == 0 && j % downScale == 0)) && (extractEnvelope == null || (i >= firstRow && i <= lastRow && j >= firstCol && j <= lastCol))) {
                        double z = Double.parseDouble(lastWord);
                        double x = xValue + j * cellSize;
                        double y = yValue - i * cellSize;
                        if (as3DPoint) {
                            Point cell = factory.createPoint(new Coordinate(x + cellSize / 2, y - cellSize / 2, z));
                            cell.setSRID(srid);
                            if (Math.abs(noData - z) != 0) {
                                preparedStatement.setObject(1, cell);
                                preparedStatement.setObject(2, z);
                                preparedStatement.addBatch();
                                batchSize++;
                            } else if (importNodata) {
                                preparedStatement.setObject(1, cell);
                                preparedStatement.setObject(2, noData);
                                preparedStatement.addBatch();
                                batchSize++;
                            }
                        } else {
                            Polygon cell = factory.createPolygon(new Coordinate[]{new Coordinate(x, y, z), new Coordinate(x, y - cellSize * downScale, z), new Coordinate(x + cellSize * downScale, y - cellSize * downScale, z), new Coordinate(x + cellSize * downScale, y, z), new Coordinate(x, y, z)});
                            cell.setSRID(srid);
                            if (Math.abs(noData - z) != 0) {
                                preparedStatement.setObject(1, cell);
                                preparedStatement.setObject(2, z);
                                preparedStatement.addBatch();
                                batchSize++;
                            } else if (importNodata) {
                                preparedStatement.setObject(1, cell);
                                preparedStatement.setObject(2, noData);
                                preparedStatement.addBatch();
                                batchSize++;
                            }
                        }
                        if (batchSize >= BATCH_MAX_SIZE) {
                            preparedStatement.executeBatch();
                            preparedStatement.clearBatch();
                            batchSize = 0;
                        }
                    }
                }
                cellProgress.endStep();
                if (i > lastRow) {
                    break;
                }
            }
            if (batchSize > 0) {
                preparedStatement.executeBatch();
            }
            return outputTable;
        } catch (NoSuchElementException | NumberFormatException | IOException | SQLException ex) {
            throw new SQLException("Unexpected word " + lastWord, ex);
        }
    }

    /**
     * Use to set the z conversion type 1 = integer 2 = double
     *
     * @param zType
     */
    public void setZType(int zType) {
        this.zType = zType;
    }

    /**
     * Set true to delete the input table if exists
     *
     * @param deleteTable
     */
    public void setDeleteTable(boolean deleteTable) {
        this.deleteTable = deleteTable;
    }

    /**
     * Set encoding
     *
     * @param encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Set to true if nodata must be imported. Default is false
     *
     * @param importNodata
     */
    public void setImportNodata(boolean importNodata) {
        this.importNodata = importNodata;
    }
}
