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
package org.noisemap.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.DriverUtilities;
import org.gdms.driver.DataSet;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.gdms.driver.DiskBufferDriver;
import org.grap.utilities.EnvelopeUtil;
import org.orbisgis.noisemap.core.FastObstructionTest;
import org.orbisgis.noisemap.core.LayerDelaunayError;
import org.orbisgis.noisemap.core.MeshRefinement;
import org.orbisgis.noisemap.core.MeshBuilder;
import org.orbisgis.noisemap.core.PropagationProcess;
import org.orbisgis.noisemap.core.PropagationProcessData;
import org.orbisgis.noisemap.core.PropagationProcessOut;
import org.orbisgis.noisemap.core.PropagationResultTriRecord;
import org.orbisgis.noisemap.core.QueryGeometryStructure;
import org.orbisgis.noisemap.core.QueryQuadTree;
import org.orbisgis.noisemap.core.Triangle;
import org.orbisgis.progress.ProgressMonitor;


import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.gdms.data.schema.MetadataUtilities;

/**
 * 
 * @author Nicolas Fortin
 */
class NodeList {
	public final LinkedList<Coordinate> nodes = new LinkedList<Coordinate>();
}

/**
 * Compute the delaunay grid and evaluate at each vertices the sound level.
 * The user don't have to set the receiver position. This function is usefull to make noise maps.
 */

public class BR_TriGrid extends AbstractTableFunction {
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = Logger.getLogger(BR_TriGrid.class.getName());
    // Timing sum in millisec
    private long totalParseBuildings = 0;
    private long totalDelaunay = 0;
    private static final String heightField = "height";

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    int getCellId(int row, int col, int cols) {
        return row * cols + col;
    }

    /**
     * Compute the envelope of sdsSource
     *
     * @param sdsSource
     * @param pm
     * @return
     * @throws FunctionException
     */
    public static Envelope GetGlobalEnvelope(DataSet sdsSource, ProgressMonitor pm) throws FunctionException {
        // The region of interest is only where we can find sources
        // Then we keep only the region where the area is covered by sources
        Envelope mainEnvelope;

        try {
            mainEnvelope = DriverUtilities.getFullExtent(sdsSource);
        } catch (DriverException e) {
            throw new FunctionException(e);
        }
        return mainEnvelope;

    }

    private void explodeAndAddPolygon(Geometry intersectedGeometry,
                                      MeshBuilder delaunayTool, Geometry boundingBox)
            throws DriverException, LayerDelaunayError {
        long beginAppendPolygons = System.currentTimeMillis();
        if (intersectedGeometry instanceof GeometryCollection) {
            for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
                Geometry subGeom = intersectedGeometry.getGeometryN(j);
                explodeAndAddPolygon(subGeom, delaunayTool, boundingBox);
            }
        } else {
            delaunayTool.addGeometry(intersectedGeometry);
        }
        totalDelaunay += System.currentTimeMillis() - beginAppendPolygons;
    }

    /**
     * This function compute buffer polygon near roads, densify, then add points to the delaunayTriangulation
     *
     * @param toUnite
     * @param bufferSize
     * @param delaunayTool
     * @throws LayerDelaunayError
     */
    private void makeBufferPointsNearRoads(List<Geometry> toUnite, double bufferSize, Envelope filter, MeshBuilder delaunayTool) throws LayerDelaunayError {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geoArray[] = new Geometry[toUnite.size()];
        toUnite.toArray(geoArray);
        GeometryCollection polygonCollection = geometryFactory
                .createGeometryCollection(geoArray);
        Geometry polygon = polygonCollection.buffer(bufferSize, 4,
                BufferParameters.CAP_SQUARE);
        polygon = TopologyPreservingSimplifier.simplify(polygon,
                bufferSize / 2.);
        polygon = Densifier.densify(polygon, bufferSize);
        delaunayTool.addGeometry(polygon);
    }

    private Geometry merge(LinkedList<Geometry> toUnite, double bufferSize) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geoArray[] = new Geometry[toUnite.size()];
        toUnite.toArray(geoArray);
        GeometryCollection polygonCollection = geometryFactory
                .createGeometryCollection(geoArray);
        return polygonCollection.buffer(bufferSize, 0,
                BufferParameters.CAP_SQUARE);
    }

    /**
     * Compute the envelope corresping to parameters
     *
     * @param mainEnvelope Global envelope
     * @param cellI        I cell index
     * @param cellJ        J cell index
     * @param cellIMax     I cell count
     * @param cellJMax     J cell count
     * @param cellWidth    Cell width meter
     * @param cellHeight   Cell height meter
     * @return Envelope of the cell
     */
    public static Envelope getCellEnv(Envelope mainEnvelope, int cellI, int cellJ,
                                      int cellIMax, int cellJMax, double cellWidth, double cellHeight) {
        return new Envelope(mainEnvelope.getMinX() + cellI * cellWidth,
                mainEnvelope.getMinX() + cellI * cellWidth + cellWidth,
                mainEnvelope.getMinY() + cellHeight * cellJ,
                mainEnvelope.getMinY() + cellHeight * cellJ + cellHeight);
    }

    private void feedDelaunay(DataSet polygonDatabase, int spatialBuildingsFieldIndex,
                              MeshBuilder delaunayTool, Envelope boundingBoxFilter,
                              double srcDistance, LinkedList<LineString> delaunaySegments,
                              double minRecDist, double srcPtDist) throws DriverException,
            LayerDelaunayError {
        Envelope extendedEnvelope = new Envelope(boundingBoxFilter);
        extendedEnvelope.expandBy(srcDistance * 2.);
        long oldtotalDelaunay = totalDelaunay;
        long beginfeed = System.currentTimeMillis();
        Geometry linearRing = EnvelopeUtil.toGeometry(boundingBoxFilter);
        if (!(linearRing instanceof LinearRing)) {
            return;
        }
        GeometryFactory factory = new GeometryFactory();
        Polygon boundingBox = new Polygon((LinearRing) linearRing, null,
                factory);

        LinkedList<Geometry> toUnite = new LinkedList<Geometry>();
        final long rowCount = polygonDatabase.getRowCount();
        for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            final Geometry geometry = polygonDatabase.getFieldValue(rowIndex, spatialBuildingsFieldIndex).getAsGeometry();
            Envelope geomEnv = geometry.getEnvelopeInternal();
            geomEnv.expandBy(BUILDING_BUFFER);
            if (boundingBoxFilter.intersects(geomEnv)) {
                // Add polygon to union array
                toUnite.add(geometry);
            }
        }
        // Reduce small artifacts to avoid, shortest geometry to be
        // over-triangulated
        LinkedList<Geometry> toUniteFinal = new LinkedList<Geometry>();
        if (!toUnite.isEmpty()) {
            Geometry bufferBuildings = merge(toUnite, BUILDING_BUFFER);
            // Remove small artifacts due to buildings buffer
            bufferBuildings = TopologyPreservingSimplifier.simplify(
                    bufferBuildings, BUILDING_BUFFER * 2);
            // Densify receiver near buildings
            // bufferBuildings=Densifier.densify(bufferBuildings,srcPtDist);

            toUniteFinal.add(bufferBuildings); // Add buildings to triangulation
        }

        // Merge roads
        if (minRecDist > 0.01) {
            LinkedList<Geometry> toUniteRoads = new LinkedList<Geometry>(delaunaySegments);
            if (!toUniteRoads.isEmpty()) {
                // Build Polygons buffer from roads lines
                Geometry bufferRoads = merge(toUniteRoads, minRecDist);
                // Remove small artifacts due to multiple buffer crosses
                bufferRoads = TopologyPreservingSimplifier.simplify(bufferRoads,
                        minRecDist / 2);
                // Densify roads to set more receiver near roads.
                //bufferRoads = Densifier.densify(bufferRoads, srcPtDist);
                //Add points buffer to the final triangulation, this will densify sound level extraction near
                //toUniteFinal.add(makeBufferSegmentsNearRoads(toUniteRoads,srcPtDist));
                makeBufferPointsNearRoads(toUniteRoads, srcPtDist, boundingBoxFilter, delaunayTool);
                //roads, and helps to reduce over estimation due to inapropriate interpolation.
                toUniteFinal.add(bufferRoads); // Merge roads with minRecDist m
                // buffer
            }
        }
        Geometry union = merge(toUniteFinal, 0.); // Merge roads and buildings
        // together
        // Remove geometries out of the bounding box
        union = union.intersection(boundingBox);
        explodeAndAddPolygon(union, delaunayTool, boundingBox);

        totalParseBuildings += System.currentTimeMillis() - beginfeed
                - (totalDelaunay - oldtotalDelaunay);
    }

    /**
     * Delaunay triangulation of Sub-Domain
     *
     * @param cellMesh
     * @param mainEnvelope
     * @param cellI
     * @param cellJ
     * @param cellIMax
     * @param cellJMax
     * @param cellWidth
     * @param cellHeight
     * @param maxSrcDist
     * @param sdsSources
     * @param minRecDist
     * @param srcPtDist
     * @param firstPassResults
     * @param neighborsBorderVertices
     * @param maximumArea
     * @throws DriverException
     * @throws LayerDelaunayError
     */
    public void computeFirstPassDelaunay(MeshBuilder cellMesh,
                                         Envelope mainEnvelope, int cellI, int cellJ, int cellIMax,
                                         int cellJMax, double cellWidth, double cellHeight,
                                         double maxSrcDist, DataSet sdsBuildings,
                                         DataSet sdsSources, int spatialBuildingsFieldIndex, int spatialSourceFieldIndex, double minRecDist,
                                         double srcPtDist, String[] firstPassResults,
                                         NodeList[] neighborsBorderVertices, double maximumArea)
            throws DriverException, LayerDelaunayError {

        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI, cellJ,
                cellIMax, cellJMax, cellWidth, cellHeight);// new
        // Envelope(mainEnvelope.getMinX()+cellI*cellWidth,
        // mainEnvelope.getMinX()+cellI*cellWidth+cellWidth,
        // mainEnvelope.getMinY()+cellHeight*cellJ,
        // mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maxSrcDist);

        // Build delaunay triangulation from buildings inside the extended
        // bounding box

        // /////////////////////////////////////////////////
        // Add roads into delaunay tool
        LinkedList<LineString> delaunaySegments = new LinkedList<LineString>();
        if (minRecDist > 0.1) {
            long rowCount = sdsSources.getRowCount();
            for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                Geometry pt = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
                Envelope ptEnv = pt.getEnvelopeInternal();
                if (ptEnv.intersects(expandedCellEnvelop)) {
                    if (pt instanceof Point) {
                        // Add square in rendering
                        cellMesh.addGeometry(pt.buffer(minRecDist, BufferParameters.CAP_SQUARE));
                    } else {

                        if (pt instanceof LineString) {
                            delaunaySegments.add((LineString) (pt));
                        } else if (pt instanceof MultiLineString) {
                            int nblinestring = ((MultiLineString) pt)
                                    .getNumGeometries();
                            for (int idlinestring = 0; idlinestring < nblinestring; idlinestring++) {
                                delaunaySegments.add((LineString) (pt
                                        .getGeometryN(idlinestring)));
                            }
                        }
                    }
                }
            }
        }
        feedDelaunay(sdsBuildings, spatialBuildingsFieldIndex, cellMesh, cellEnvelope, maxSrcDist, delaunaySegments,
                minRecDist, srcPtDist);

        // Process delaunay

        long beginDelaunay = System.currentTimeMillis();
        logger.info("Begin delaunay");
        if (maximumArea > 1) {
            cellMesh.setInsertionEvaluator(new MeshRefinement(maximumArea,0.02,
                    MeshRefinement.DEFAULT_QUALITY));
        }
        // Maximum 5x steinerpt than input point, this limits avoid infinite
        // loop, or memory consuming triangulation
        cellMesh.finishPolygonFeeding(cellEnvelope);
        logger.info("End delaunay");
        totalDelaunay += System.currentTimeMillis() - beginDelaunay;

    }

    public static Double DbaToW(Double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    @Override
    public DataSet evaluate(DataSourceFactory dsf, DataSet[] tables,
                            Value[] values, ProgressMonitor pm) throws FunctionException {
        String tmpdir = dsf.getTempDir().getAbsolutePath();
        if (values.length < 10) {
            throw new FunctionException("Not enough parameters !");
        } else if (values.length > 10) {
            throw new FunctionException("Too many parameters !");
        }
        String dbField = values[0].toString();
        double maxSrcDist = values[1].getAsDouble();
        double maxRefDist = values[2].getAsDouble();
        int subdivLvl = values[3].getAsInt();
        double minRecDist = values[4].getAsDouble(); /*
                                                     * <! Minimum distance
													 * between source and
													 * receiver
													 */
        double srcPtDist = values[5].getAsDouble(); /*
													 * <! Complexity distance of
													 * roads
													 */
        double maximumArea = values[6].getAsDouble();
        int reflexionOrder = values[7].getAsInt();
        int diffractionOrder = values[8].getAsInt();
        double wallAlpha = values[9].getAsDouble();
        boolean forceSinglePass = false;
        boolean doMultiThreading = true;
        assert (maxSrcDist > maxRefDist); //Maximum Source-Receiver
        //distance must be superior than
        //maximum Receiver-Wall distance
        DiskBufferDriver driver = null;
        ThreadPool threadManager = null;
        ProgressionOrbisGisManager pmManager = null;
        PropagationProcessDiskWriter driverManager = null;
        try {
            // Steps of execution
            // Evaluation of the main bounding box (sources+buildings)
            // Split domain into 4^subdiv cells
            // For each cell :
            // Expand bounding box cell by maxSrcDist
            // Build delaunay triangulation from buildings polygon processed by
            // intersection with non extended bounding box
            // Save the list of sources index inside the extended bounding box
            // Save the list of buildings index inside the extended bounding box
            // Make a structure to keep the following information
            // Triangle list with the 3 vertices index
            // Vertices list (as receivers)
            // For each vertices within the cell bounding box (not the extended
            // one)
            // Find all sources within maxSrcDist
            // For All found sources
            // Test if there is a gap(no building) between source and receiver
            // if not then append the distance attenuated sound level to the
            // receiver
            // Save the triangle geometry with the db_m value of the 3 vertices

            int tableBuildings = 0;
            int tableSources = 1;
            long nbreceivers = 0;


            // Load Sources, Buildings, Topo Points and Soil Areas table drivers
            if (tables.length != 2) {
                throw new FunctionException("Table lenght must be 2 !");
            }
            final DataSet sds = tables[tableBuildings];
            final DataSet sdsSources = tables[tableSources];

            // extract spatial field index of four input tables
            int spatialBuildingsFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
            int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSources.getMetadata());

            // 1 Step - Evaluation of the main bounding box (sources)
            Envelope mainEnvelope = GetGlobalEnvelope(sdsSources, pm);
            // Split domain into 4^subdiv cells

            int gridDim = (int) Math.pow(2, subdivLvl);

            // Initialization frequency declared in source Table
            ArrayList<Integer> db_field_ids = new ArrayList<Integer>();
            ArrayList<Integer> db_field_freq = new ArrayList<Integer>();
            int fieldid = 0;
            for (String fieldName : sdsSources.getMetadata().getFieldNames()) {
                if (fieldName.startsWith(dbField)) {
                    String sub = fieldName.substring(dbField.length());
                    if (sub.length() > 0) {
                        int freq = Integer.parseInt(sub);
                        db_field_ids.add(fieldid);
                        db_field_freq.add(freq);
                    } else {
                        db_field_ids.add(fieldid);
                        db_field_freq.add(0);
                    }
                }
                fieldid++;
            }

            double cellWidth = mainEnvelope.getWidth() / gridDim;
            double cellHeight = mainEnvelope.getHeight() / gridDim;

            String[] firstPassResults = new String[gridDim * gridDim];
            NodeList[] neighborsBorderVertices = new NodeList[gridDim * gridDim];

            driver = new DiskBufferDriver(dsf, getMetadata(null));

            int nbcell = gridDim * gridDim;
            if (nbcell == 1) {
                doMultiThreading = false;
                forceSinglePass = true;
            }

            Runtime runtime = Runtime.getRuntime();
            threadManager = new ThreadPool(
                    runtime.availableProcessors(),
                    runtime.availableProcessors() + 1, Long.MAX_VALUE,
                    TimeUnit.SECONDS);

            pmManager = new ProgressionOrbisGisManager(
                    nbcell, pm);
            Stack<PropagationResultTriRecord> toDriver = new Stack<PropagationResultTriRecord>();
            driverManager = new PropagationProcessDiskWriter(
                    toDriver, null, driver, null);
            driverManager.start();
            pmManager.start();
            PropagationProcessOut threadDataOut = new PropagationProcessOut(
                    toDriver, null);

            for (int cellI = 0; cellI < gridDim; cellI++) {
                for (int cellJ = 0; cellJ < gridDim; cellJ++) {
                    MeshBuilder mesh = new MeshBuilder();
                    int ij = cellI * gridDim + cellJ;
                    logger.info("Begin processing of cell " + (cellI + 1) + ","
                            + (cellJ + 1) + " of the " + gridDim + "x" + gridDim
                            + "  grid..");
                    if (pm != null && pm.isCancelled()) {
                        driver.writingFinished();
                        return driver.getTable("main");
                    }
                    Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                            cellJ, gridDim, gridDim, cellWidth, cellHeight);// new
                    // Envelope(mainEnvelope.getMinX()+cellI*cellWidth,
                    // mainEnvelope.getMinX()+cellI*cellWidth+cellWidth,
                    // mainEnvelope.getMinY()+cellHeight*cellJ,
                    // mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
                    Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
                    expandedCellEnvelop.expandBy(maxSrcDist);
                    // Build delaunay triangulation from buildings inside the
                    // extended bounding box

                    // //////////////////////////////////////////////////////
                    // Make source index for optimization
                    ArrayList<Geometry> sourceGeometries = new ArrayList<Geometry>();
                    ArrayList<ArrayList<Double>> wj_sources = new ArrayList<ArrayList<Double>>();
                    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
                    // QueryGeometryStructure<Integer> sourcesIndex=new
                    // QueryQuadTree<Integer>();

                    long rowCount = sdsSources.getRowCount();
                    int fieldCount = sdsSources.getMetadata().getFieldCount();
                    Integer idsource = 0;
                    for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {

                        //Value[] row = sdsSources.getRow(rowIndex);
                        //Geometry geo = sdsSources.getGeometry(rowIndex);
                        final Value[] row = new Value[fieldCount];
                        for (int j = 0; j < fieldCount; j++) {
                            row[j] = sdsSources.getFieldValue(rowIndex, j);
                        }
                        Geometry geo = row[spatialSourceFieldIndex].getAsGeometry();

                        Envelope ptEnv = geo.getEnvelopeInternal();
                        if (ptEnv.intersects(expandedCellEnvelop)) {
                            sourcesIndex.appendGeometry(geo, idsource);
                            ArrayList<Double> wj_spectrum = new ArrayList<Double>();
                            wj_spectrum.ensureCapacity(db_field_ids.size());
                            for (Integer idcol : db_field_ids) {
                                wj_spectrum
                                        .add(DbaToW(row[idcol].getAsDouble()));
                            }
                            wj_sources.add(wj_spectrum);
                            sourceGeometries.add(geo);
                            idsource++;
                        }
                    }

                    // //////////////////////////////////////////////////////
                    // feed freeFieldFinder for fast intersection query
                    // optimization


                    rowCount = sds.getRowCount();
                    for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                        final Geometry geometry = sds.getFieldValue(rowIndex, spatialBuildingsFieldIndex).getAsGeometry();
                        double height = 0.;

                        //if exist height
                        if (sds.getMetadata().getFieldIndex(heightField) != -1) {
                            height = sds.getFieldValue(rowIndex, sds.getMetadata().getFieldIndex(heightField)).getAsDouble();
                        }
                        Envelope geomEnv = geometry.getEnvelopeInternal();
                        if (expandedCellEnvelop.intersects(geomEnv)) {
                            //if we dont have height of building
                            if (Double.compare(height, 0.) == 0) {
                                mesh.addGeometry(geometry);
                            } else {
                                mesh.addGeometry(geometry, height);
                            }
                        }
                    }

                    mesh.finishPolygonFeeding(expandedCellEnvelop);
                    FastObstructionTest freeFieldFinder = new FastObstructionTest(mesh.getPolygonWithHeight(),
                            mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
                    // Compute the first pass delaunay mesh
                    // The first pass doesn't take account of additional
                    // vertices of neighbor cells at the borders
                    // then, there are discontinuities in iso surfaces at each
                    // border of cell
                    MeshBuilder cellMesh = new MeshBuilder();


                    computeFirstPassDelaunay(cellMesh, mainEnvelope, cellI,
                            cellJ, gridDim, gridDim, cellWidth, cellHeight,
                            maxSrcDist, sds, sdsSources, spatialBuildingsFieldIndex, spatialSourceFieldIndex, minRecDist,
                            srcPtDist, firstPassResults,
                            neighborsBorderVertices, maximumArea);
                    // Make a structure to keep the following information
                    // Triangle list with 3 vertices(int), and 3 neighbor
                    // triangle ID
                    // Vertices list

                    // The evaluation of sound level must be done where the
                    // following vertices are
                    List<Coordinate> vertices = cellMesh.getVertices();
                    List<Triangle> triangles = new ArrayList<Triangle>();
                    for(Triangle triangle : cellMesh.getTriangles()) {
                        if(triangle.getBuidlingID() == 0) {
                            triangles.add(triangle);
                        }
                    }
                    nbreceivers += vertices.size();


                    PropagationProcessData threadData = new PropagationProcessData(
                            vertices, null, triangles, freeFieldFinder, sourcesIndex,
                            sourceGeometries, wj_sources, db_field_freq,
                            reflexionOrder, diffractionOrder, maxSrcDist, maxRefDist,
                            minRecDist, wallAlpha, ij,
                            pmManager.nextSubProcess(vertices.size()), null);
                    PropagationProcess propaProcess = new PropagationProcess(
                            threadData, threadDataOut);

                    if (doMultiThreading) {
                        logger.info("Wait for free Thread to begin propagation of cell "
                                + (cellI + 1)
                                + ","
                                + (cellJ + 1)
                                + " of the "
                                + gridDim
                                + "x" + gridDim + "  grid..");
                        // threadManager.executeBlocking(propaProcess);
                        while (!threadManager.hasAvaibleQueueSlot()) {
                            if (pm != null && pm.isCancelled()) {
                                driver.writingFinished();
                                return driver.getTable("main");
                            }
                            Thread.sleep(100);
                        }
                        threadManager.execute(propaProcess);
                        logger.info("Processing enqueued"); // enqueued
                    } else {
                        propaProcess.run();
                    }
                }
            }
            // Wait termination of processes
            logger.info("Wait for termination of the lasts propagation process..");
            // threadManager.getRemainingTasks()>0
            Thread.sleep(100);
            while (threadDataOut.getCellComputed() < nbcell && doMultiThreading) {
                if (pm != null && pm.isCancelled()) {
                    driver.writingFinished();
                    return driver.getTable("main");
                }
                Thread.sleep(100);
            }
            // Wait for rows stack to be empty
            driverManager.stopWatchingStack();
            pmManager.stop();
            logger.info("Wait for termination of writing to the driver..");
            while (driverManager.isRunning()) {
                if (pm != null && pm.isCancelled()) {
                    driver.writingFinished();
                    return driver.getTable("main");
                }
                Thread.sleep(10);
            }
            threadManager.shutdown();
            driver.writingFinished();
            driver.open();
            logger.info("Parse polygons time:" + this.totalParseBuildings
                    + " ms");
            logger.info("Delaunay time:" + this.totalDelaunay + " ms");
            logger.info("Min Max Avg computation time by receiver : " + (threadDataOut.getMinimalReceiverComputationTime() / 1e6) + " ms to " + (threadDataOut.getMaximalReceiverComputationTime() / 1e6) + " ms. Avg :" + (threadDataOut.getSumReceiverComputationTime() / (nbreceivers * 1e6)) + " ms.");
            logger.info("Receiver count:" + nbreceivers);
            logger.info("Receiver-Source count:"
                    + threadDataOut.getNb_couple_receiver_src());
            logger.info("Receiver image (reflections):"
                    + threadDataOut.getNb_image_receiver());
            logger.info("Receiver-Sources specular reflection path count:"
                    + threadDataOut.getNb_reflexion_path());
            logger.info("Receiver-Source diffraction path count:" + threadDataOut.getNb_diffraction_path());
            logger.info("Buildings obstruction test count:"
                    + threadDataOut.getNb_obstr_test());
            return driver.getTable("main");
        } catch (DriverLoadException e) {
            throw new FunctionException(e);
        } catch (DriverException e) {
            throw new FunctionException(e);
        } catch (LayerDelaunayError e) {
            throw new FunctionException(e);
        } catch (InterruptedException e) {
            throw new FunctionException(e);
        } finally {
            //Stop threads if there are not stoped
            if (pmManager != null) {
                pmManager.stop();
            }
            if (threadManager != null) {
                threadManager.shutdown();
            }
            if (driverManager != null) {
                driverManager.stopWatchingStack();
            }
        }
    }

    @Override
    public Metadata getMetadata(Metadata[] tables) throws DriverException {
        Type meta_type[] = {TypeFactory.createType(Type.GEOMETRY),
                TypeFactory.createType(Type.FLOAT),
                TypeFactory.createType(Type.FLOAT),
                TypeFactory.createType(Type.FLOAT),
                TypeFactory.createType(Type.INT),
                TypeFactory.createType(Type.INT)};
        String meta_name[] = {"the_geom", "db_v1", "db_v2", "db_v3",
                "cellid", "triid"};
        return new DefaultMetadata(meta_type, meta_name);
    }

    @Override
    public FunctionSignature[] getFunctionSignatures() {
        // Builds geom , sources.the_geom, sources.db_m ,max propa dist , subdiv
        // lev ,roads width , receiv road dis,max tri area ,sound refl o,sound
        // dif order,wall alpha
        return new FunctionSignature[]{
                new TableFunctionSignature(TableDefinition.GEOMETRY,
                        new TableArgument(TableDefinition.GEOMETRY),//buildings
                        new TableArgument(TableDefinition.GEOMETRY),//src
                        ScalarArgument.STRING,
                        ScalarArgument.DOUBLE,
                        ScalarArgument.DOUBLE,
                        ScalarArgument.INT,
                        ScalarArgument.DOUBLE,
                        ScalarArgument.DOUBLE,
                        ScalarArgument.DOUBLE,
                        ScalarArgument.INT,
                        ScalarArgument.INT,
                        ScalarArgument.DOUBLE
                )
        };
    }

    @Override
    public String getName() {
        return "BR_TriGrid";
    }

    @Override
    public String getSqlOrder() {
        return "create table result as select * from BR_TriGrid( buildings_table, sound_sources_table,'source db field name',searchSourceLimit,searchReflectionWallLimit,subdivlevel,roadwith(1.8),densification_receiver(5),max triangle area(300),reflection order(2),diffraction order(1),wall absorption(0.1));";
    }

    @Override
    public String getDescription() {
        return "BR_TriGrid(buildings(polygons),sources(points),sound lvl field name(string),maximum propagation distance (double meter),maximum wall seeking distance (double meter),subdivision level 4^n cells(int), roads width (meter), densification of receivers near roads (meter), maximum area of triangle, sound reflection order, sound diffraction order, alpha of walls ) Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified buildings geometry.";
    }


}