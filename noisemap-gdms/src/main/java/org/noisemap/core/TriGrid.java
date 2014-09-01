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

import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
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
import org.orbisgis.noisemap.core.GeoWithSoilType;
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
 * Compute the delaunay grid and evaluate at each vertices the sound level.
 * The user don't have to set the receiver position. This function is usefull to make noise maps.
 */

public class TriGrid {
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = Logger.getLogger(TriGrid.class.getName());
    // Timing sum in millisec
    private long totalParseBuildings = 0;
    private long totalDelaunay = 0;
    private static final String heightField = "height";
    public void setLogger(Logger logger) {
        this.logger = logger;
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

    private Geometry merge(LinkedList<Geometry> toUnite, double bufferSize) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geoArray[] = new Geometry[toUnite.size()];
        toUnite.toArray(geoArray);
        GeometryCollection polygonCollection = geometryFactory
                .createGeometryCollection(geoArray);
        BufferOp bufferOp = new BufferOp(polygonCollection, new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_SQUARE,
                BufferParameters.JOIN_MITRE, BufferParameters.DEFAULT_MITRE_LIMIT));
        return bufferOp.getResultGeometry(bufferSize);
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
                              double minRecDist, double srcPtDist, double triangleSide) throws DriverException,
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
            logger.info("Merge buildings");
            Geometry bufferBuildings = merge(toUnite, BUILDING_BUFFER);
            // Remove small artifacts due to buildings buffer
            if(triangleSide > 0) {
                bufferBuildings = Densifier.densify(bufferBuildings, triangleSide);
            }
            toUniteFinal.add(bufferBuildings); // Add buildings to triangulation
        }

        // Merge roads
        if (minRecDist > 0.01) {
            LinkedList<Geometry> toUniteRoads = new LinkedList<Geometry>(delaunaySegments);
            if (!toUniteRoads.isEmpty()) {
                // Build Polygons buffer from roads lines
                logger.info("Merge roads");
                Geometry bufferRoads = merge(toUniteRoads, minRecDist / 2);
                // Remove small artifacts due to multiple buffer crosses
                bufferRoads = TopologyPreservingSimplifier.simplify(bufferRoads,
                        minRecDist / 2);
                // Densify roads to set more receiver near roads.
                if(srcPtDist > 0){
                    bufferRoads = Densifier.densify(bufferRoads, srcPtDist);
                } else if (triangleSide > 0) {
                    bufferRoads = Densifier.densify(bufferRoads, triangleSide);
                }
                //Add points buffer to the final triangulation, this will densify sound level extraction near
                //toUniteFinal.add(makeBufferSegmentsNearRoads(toUniteRoads,srcPtDist));
                //roads, and helps to reduce over estimation due to inappropriate interpolation.
                toUniteFinal.add(bufferRoads); // Merge roads with minRecDist m
                // buffer
            }
        }
        logger.info("Merge roads and buildings");
        Geometry union = merge(toUniteFinal, 0.); // Merge roads and buildings
        // together
        // Remove geometries out of the bounding box
        logger.info("Remove roads and buildings outside study area");
        union = union.intersection(boundingBox);
        explodeAndAddPolygon(union, delaunayTool, boundingBox);
        logger.info("Feed delaunay in "+(System.currentTimeMillis() - beginfeed)+ " ms");
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
     * @param maximumArea
     * @throws DriverException
     * @throws LayerDelaunayError
     */
    public void computeFirstPassDelaunay(MeshBuilder cellMesh,
                                         Envelope mainEnvelope, int cellI, int cellJ, int cellIMax,
                                         int cellJMax, double cellWidth, double cellHeight,
                                         double maxSrcDist, DataSet sdsBuildings,
                                         DataSet sdsSources, int spatialBuildingsFieldIndex, int spatialSourceFieldIndex, double minRecDist,
                                         double srcPtDist, double maximumArea)
            throws DriverException, LayerDelaunayError {

        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI, cellJ,
                cellIMax, cellJMax, cellWidth, cellHeight);
        Geometry cellEnvelopeGeometry = new GeometryFactory().toGeometry(cellEnvelope);

                // new
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
                        cellMesh.addGeometry(cellEnvelopeGeometry.intersection(pt.buffer(minRecDist, BufferParameters.CAP_SQUARE)));
                    } else {

                        if (pt instanceof LineString) {
                            delaunaySegments.add((LineString) (pt));
                        } else if (pt instanceof MultiLineString) {
                            int nblinestring = pt.getNumGeometries();
                            for (int idlinestring = 0; idlinestring < nblinestring; idlinestring++) {
                                delaunaySegments.add((LineString) (pt
                                        .getGeometryN(idlinestring)));
                            }
                        }
                    }
                }
            }
        }

        // Compute equilateral triangle side from Area
        double triangleSide = (2*Math.pow(maximumArea, 0.5)) / Math.pow(3, 0.25);
        feedDelaunay(sdsBuildings, spatialBuildingsFieldIndex, cellMesh, cellEnvelope, maxSrcDist, delaunaySegments,
                minRecDist, srcPtDist, triangleSide);

        // Process delaunay

        long beginDelaunay = System.currentTimeMillis();
        logger.info("Begin delaunay");
        cellMesh.setComputeNeighbors(false);
        if (maximumArea > 1) {
            cellMesh.setInsertionEvaluator(new MeshRefinement(maximumArea,0.02,
                    MeshRefinement.DEFAULT_QUALITY, cellMesh));
            Geometry densifiedEnvelope = Densifier.densify(new GeometryFactory().toGeometry(cellEnvelope), triangleSide);
            cellMesh.finishPolygonFeeding(densifiedEnvelope);

        } else {
            cellMesh.finishPolygonFeeding(cellEnvelope);
        }
        // Maximum 5x steinerpt than input point, this limits avoid infinite
        // loop, or memory consuming triangulation
        logger.info("End delaunay");
        totalDelaunay += System.currentTimeMillis() - beginDelaunay;

    }

    public static Double DbaToW(Double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public void evaluate(DiskBufferDriver driver,DataSourceFactory dsf,String dbField, double maxSrcDist,
                         double maxRefDist,int subdivLvl, double minRecDist, double srcPtDist, double maximumArea,
                         int reflexionOrder, int diffractionOrder,double wallAlpha,final DataSet buildingsTable,
                         final DataSet sourceTable, ProgressMonitor pm) throws FunctionException {
        evaluate(driver, dsf, dbField, maxSrcDist, maxRefDist, subdivLvl, minRecDist, srcPtDist, maximumArea,
                reflexionOrder, diffractionOrder, wallAlpha, buildingsTable, sourceTable,null,null, pm);
    }

    public void evaluate(DiskBufferDriver driver,DataSourceFactory dsf,String dbField, double maxSrcDist,
                            double maxRefDist,int subdivLvl, double minRecDist, double srcPtDist, double maximumArea,
                            int reflexionOrder, int diffractionOrder,double wallAlpha,final DataSet buildingsTable,
                            final DataSet sourceTable,final DataSet topoTable,final DataSet soilAreas, ProgressMonitor pm) throws FunctionException {
        boolean doMultiThreading = true;
        assert (maxSrcDist > maxRefDist); //Maximum Source-Receiver
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
            long nbreceivers = 0;
            // extract spatial field index of four input tables
            int spatialBuildingsFieldIndex = MetadataUtilities.getSpatialFieldIndex(buildingsTable.getMetadata());
            int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sourceTable.getMetadata());
            int spatialsdsSoilAreasFieldIndex = -1;
            if(soilAreas != null) {
                MetadataUtilities.getSpatialFieldIndex(soilAreas.getMetadata());
            }
            int spatialTopoPtsFieldIndex = -1;
            if(topoTable != null) {
                MetadataUtilities.getSpatialFieldIndex(topoTable.getMetadata());
            }
            // 1 Step - Evaluation of the main bounding box (sources)
            Envelope mainEnvelope = GetGlobalEnvelope(sourceTable, pm);
            // Split domain into 4^subdiv cells

            int gridDim = (int) Math.pow(2, subdivLvl);

            // Initialization frequency declared in source Table
            ArrayList<Integer> db_field_ids = new ArrayList<Integer>();
            ArrayList<Integer> db_field_freq = new ArrayList<Integer>();
            int fieldid = 0;
            for (String fieldName : sourceTable.getMetadata().getFieldNames()) {
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

            int nbcell = gridDim * gridDim;
            if (nbcell == 1) {
                doMultiThreading = false;
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
                        return;
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

                    long rowCount = sourceTable.getRowCount();
                    int fieldCount = sourceTable.getMetadata().getFieldCount();
                    Integer idsource = 0;
                    for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {

                        //Value[] row = sdsSources.getRow(rowIndex);
                        //Geometry geo = sdsSources.getGeometry(rowIndex);
                        final Value[] row = new Value[fieldCount];
                        for (int j = 0; j < fieldCount; j++) {
                            row[j] = sourceTable.getFieldValue(rowIndex, j);
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


                    rowCount = buildingsTable.getRowCount();
                    int heightFieldIndex = buildingsTable.getMetadata().getFieldIndex(heightField);
                    for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                        final Geometry geometry = buildingsTable.getFieldValue(rowIndex, spatialBuildingsFieldIndex).getAsGeometry();
                        Envelope geomEnv = geometry.getEnvelopeInternal();
                        if (expandedCellEnvelop.intersects(geomEnv)) {
                            //if we dont have height of building
                            if (heightFieldIndex == -1) {
                                mesh.addGeometry(geometry);
                            } else {
                                double height = buildingsTable.getFieldValue(rowIndex, heightFieldIndex).getAsDouble();
                                mesh.addGeometry(geometry, height);
                            }
                        }
                    }
                    //if we have topographic points data
                    if(spatialTopoPtsFieldIndex!=-1){
                        for (long rowIndex = 0; rowIndex < topoTable.getRowCount(); rowIndex++) {
                            final Geometry geometry = topoTable.getFieldValue(rowIndex, spatialTopoPtsFieldIndex).getAsGeometry();
                            Envelope geomEnv = geometry.getEnvelopeInternal();
                            if (expandedCellEnvelop.intersects(geomEnv)&&geometry instanceof Point) {
                                mesh.addTopographicPoint(geometry.getCoordinate());
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
                            maxSrcDist, buildingsTable, sourceTable, spatialBuildingsFieldIndex, spatialSourceFieldIndex, minRecDist,
                            srcPtDist, maximumArea);
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
                    // Fetch soil areas
                    List<GeoWithSoilType> geoWithSoil = new ArrayList<GeoWithSoilType>();
                    if(spatialsdsSoilAreasFieldIndex!=-1 && soilAreas!=null){
                        int GFieldIndex = soilAreas.getMetadata().getFieldIndex("G");
                        for(int i=0;i<soilAreas.getRowCount();i++){
                            Geometry soilGeo = soilAreas.getFieldValue(i, spatialsdsSoilAreasFieldIndex).getAsGeometry();
                            if(expandedCellEnvelop.intersects(soilGeo.getEnvelopeInternal())){
                                double soilType = 0.;
                                if (GFieldIndex!=-1){
                                    soilAreas.getFieldValue(i, GFieldIndex).getAsDouble();
                                }
                                geoWithSoil.add(new GeoWithSoilType(soilGeo, soilType));
                            }
                        }
                    }
                    if(geoWithSoil.isEmpty()){
                        geoWithSoil = null;
                    }
                    PropagationProcessData threadData = new PropagationProcessData(
                            vertices, null, triangles, freeFieldFinder, sourcesIndex,
                            sourceGeometries, wj_sources, db_field_freq,
                            reflexionOrder, diffractionOrder, maxSrcDist, maxRefDist,
                            minRecDist, wallAlpha, ij,
                            pmManager.nextSubProcess(vertices.size()), geoWithSoil);
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
                                return;
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
                    return;
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
                    return;
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
}