package org.orbisgis.noisemap.core.jdbc;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.FastObstructionTest;
import org.orbisgis.noisemap.core.GeoWithSoilType;
import org.orbisgis.noisemap.core.LayerDelaunayError;
import org.orbisgis.noisemap.core.MeshBuilder;
import org.orbisgis.noisemap.core.PropagationProcess;
import org.orbisgis.noisemap.core.PropagationProcessData;
import org.orbisgis.noisemap.core.PropagationProcessOut;
import org.orbisgis.noisemap.core.PropagationResultTriRecord;
import org.orbisgis.noisemap.core.QueryGeometryStructure;
import org.orbisgis.noisemap.core.QueryQuadTree;
import org.orbisgis.noisemap.core.Triangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Create noise map using JDBC connection. SQL syntax is compatible with H2 and PostGIS.
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class TriangleNoiseMap extends JdbcNoiseMap {
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = LoggerFactory.getLogger(TriangleNoiseMap.class);
    private double roadWidth = 2;
    private double sourceDensification = 4;
    private double maximumArea = 75;
    private long nbreceivers = 0;
    private double receiverHeight = 1.6;


    /**
     * @param buildingsTableName Buildings table
     * @param sourcesTableName Source table name
     */
    public TriangleNoiseMap(String buildingsTableName, String sourcesTableName) {
        super(buildingsTableName, sourcesTableName);
    }

    private void explodeAndAddPolygon(Geometry intersectedGeometry,
                                      MeshBuilder delaunayTool)
            throws LayerDelaunayError {
        if (intersectedGeometry instanceof GeometryCollection) {
            for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
                Geometry subGeom = intersectedGeometry.getGeometryN(j);
                explodeAndAddPolygon(subGeom, delaunayTool);
            }
        } else {
            delaunayTool.addGeometry(intersectedGeometry);
        }
    }

    private Geometry merge(LinkedList<Geometry> toUnite, double bufferSize) {
        Geometry geoArray[] = new Geometry[toUnite.size()];
        toUnite.toArray(geoArray);
        GeometryCollection polygonCollection = geometryFactory
                .createGeometryCollection(geoArray);
        BufferOp bufferOp = new BufferOp(polygonCollection,
                new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_SQUARE,
                BufferParameters.JOIN_MITRE, BufferParameters.DEFAULT_MITRE_LIMIT));
        return bufferOp.getResultGeometry(bufferSize);
    }

    private void feedDelaunay(Collection<Geometry> buildings, MeshBuilder delaunayTool, Envelope boundingBoxFilter,
                              double srcDistance, LinkedList<LineString> delaunaySegments, double minRecDist,
                              double srcPtDist, double triangleSide) throws LayerDelaunayError {
        Envelope extendedEnvelope = new Envelope(boundingBoxFilter);
        extendedEnvelope.expandBy(srcDistance * 2.);
        Geometry linearRing = geometryFactory.toGeometry(boundingBoxFilter);
        if (!(linearRing instanceof Polygon)) {
            return;
        }
        Polygon boundingBox = (Polygon)linearRing;
        LinkedList<Geometry> toUnite = new LinkedList<>();
        Envelope fetchBox = new Envelope(boundingBoxFilter);
        fetchBox.expandBy(BUILDING_BUFFER);
        Geometry fetchGeometry = geometryFactory.toGeometry(fetchBox);
        for(Geometry building : buildings) {
            if(building.intersects(fetchGeometry)) {
                toUnite.add(building);
            }
        }
        // Reduce small artifacts to avoid, shortest geometry to be
        // over-triangulated
        LinkedList<Geometry> toUniteFinal = new LinkedList<>();
        if (!toUnite.isEmpty()) {
            Geometry bufferBuildings = merge(toUnite, BUILDING_BUFFER);
            // Remove small artifacts due to buildingsTableName buffer
            if(triangleSide > 0) {
                bufferBuildings = Densifier.densify(bufferBuildings, triangleSide);
            }
            toUniteFinal.add(bufferBuildings); // Add buildingsTableName to triangulation
        }
        // Merge roads
        if (minRecDist > 0.01) {
            LinkedList<Geometry> toUniteRoads = new LinkedList<Geometry>(delaunaySegments);
            if (!toUniteRoads.isEmpty()) {
                // Build Polygons buffer from roads lines
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
        Geometry union = merge(toUniteFinal, 0.); // Merge roads and buildingsTableName
        // together
        // Remove geometries out of the bounding box
        union = union.intersection(boundingBox);
        explodeAndAddPolygon(union, delaunayTool);
    }


    /**
     * Delaunay triangulation of Sub-Domain
     *
     * @param cellMesh Final mesh target
     * @param mainEnvelope Global envelope
     * @param cellI I cell index
     * @param cellJ J cell index
     * @param maxSrcDist Maximum propagation distance
     * @param minRecDist Minimal distance receiver-source
     * @param srcPtDist Densification distance of sources pts
     * @param maximumArea Maximum area of triangles
     * @throws LayerDelaunayError
     */
    public void computeDelaunay(MeshBuilder cellMesh,
                                Envelope mainEnvelope, int cellI, int cellJ, double maxSrcDist,
                                Collection<Geometry> buildings, Collection<Geometry> sources,
                                double minRecDist, double srcPtDist, double maximumArea)
            throws LayerDelaunayError {

        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI, cellJ,
                getCellWidth(), getCellHeight());
        Geometry cellEnvelopeGeometry = new GeometryFactory().toGeometry(cellEnvelope);

        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maxSrcDist);

        // Build delaunay triangulation from buildings inside the extended
        // bounding box

        // /////////////////////////////////////////////////
        // Add roads into delaunay tool
        LinkedList<LineString> delaunaySegments = new LinkedList<>();
        if (minRecDist > 0.1) {
            for (Geometry pt : sources) {
                Envelope ptEnv = pt.getEnvelopeInternal();
                if (ptEnv.intersects(expandedCellEnvelop)) {
                    if (pt instanceof Point) {
                        // Add square in rendering
                        cellMesh.addGeometry(cellEnvelopeGeometry.intersection(pt.buffer(minRecDist, BufferParameters.CAP_SQUARE)));
                    } else {
                        if (pt instanceof LineString) {
                            delaunaySegments.add((LineString) (pt));
                        } else if (pt instanceof MultiLineString) {
                            int nbLineString = pt.getNumGeometries();
                            for (int idLineString = 0; idLineString < nbLineString; idLineString++) {
                                delaunaySegments.add((LineString) (pt
                                        .getGeometryN(idLineString)));
                            }
                        }
                    }
                }
            }
        }

        // Compute equilateral triangle side from Area
        double triangleSide = (2*Math.pow(maximumArea, 0.5)) / Math.pow(3, 0.25);
        feedDelaunay(buildings, cellMesh, cellEnvelope, maxSrcDist, delaunaySegments,
                minRecDist, srcPtDist, triangleSide);

        // Process delaunay
        logger.info("Begin delaunay");
        cellMesh.setComputeNeighbors(false);
        if (maximumArea > 1) {
            cellMesh.setMaximumArea(maximumArea);
            Geometry densifiedEnvelope = Densifier.densify(new GeometryFactory().toGeometry(cellEnvelope), triangleSide);
            cellMesh.finishPolygonFeeding(densifiedEnvelope);
        } else {
            cellMesh.finishPolygonFeeding(cellEnvelope);
        }
        logger.info("End delaunay");
    }

    @Override
    protected Envelope getComputationEnvelope(Connection connection) throws SQLException {
        return SFSUtilities.getTableEnvelope(connection, TableLocation.parse(sourcesTableName), "");
    }

    public Collection<PropagationResultTriRecord> evaluateCell(Connection connection,int cellI, int cellJ, ProgressVisitor progression) throws SQLException {
        PropagationProcessOut threadDataOut = new PropagationProcessOut();
        MeshBuilder mesh = new MeshBuilder();
        int ij = cellI * gridDim + cellJ;
        logger.info("Begin processing of cell " + (cellI + 1) + ","
                + (cellJ + 1) + " of the " + gridDim + "x" + gridDim
                + "  grid..");
        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                cellJ, getCellWidth(), getCellHeight());


        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maximumPropagationDistance);

        // //////////////////////////////////////////////////////
        // feed freeFieldFinder for fast intersection query
        // optimization
        // Fetch buildings in extendedEnvelope
        ArrayList<Geometry> buildingsGeometries = new ArrayList<>();
        fetchCellBuildings(connection, expandedCellEnvelop, buildingsGeometries, mesh);
        //if we have topographic points data
        fetchCellDem(connection, expandedCellEnvelop, mesh);

        // Data fetching for collision test is done.
        try {
            mesh.finishPolygonFeeding(expandedCellEnvelop);
        } catch (LayerDelaunayError ex) {
            throw new SQLException(ex.getLocalizedMessage(), ex);
        }
        FastObstructionTest freeFieldFinder = new FastObstructionTest(mesh.getPolygonWithHeight(),
                mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        // //////////////////////////////////////////////////////
        // Make source index for optimization
        ArrayList<Geometry> sourceDelaunayGeometries = new ArrayList<>();
        ArrayList<Geometry> sourceGeometries = new ArrayList<>();
        ArrayList<ArrayList<Double>> wj_sources = new ArrayList<>();
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();

        // Fetch all source located in expandedCellEnvelop
        fetchCellSource(connection, expandedCellEnvelop, sourceDelaunayGeometries, sourceGeometries, wj_sources,
                sourcesIndex);
        // Compute the first pass delaunay mesh
        // The first pass doesn't take account of additional
        // vertices of neighbor cells at the borders
        // then, there are discontinuities in iso surfaces at each
        // border of cell
        MeshBuilder cellMesh = new MeshBuilder();
        try {
            computeDelaunay(cellMesh, mainEnvelope, cellI,
                    cellJ,
                    maximumPropagationDistance, buildingsGeometries, sourceDelaunayGeometries, roadWidth,
                    sourceDensification, maximumArea);
        } catch (LayerDelaunayError err) {
            throw new SQLException(err.getLocalizedMessage(), err);
        }
        sourceDelaunayGeometries.clear();
        // Make a structure to keep the following information
        // Triangle list with 3 vertices(int), and 3 neighbor
        // triangle ID
        // Vertices list

        // The evaluation of sound level must be done where the
        // following vertices are
        List<Coordinate> vertices = new ArrayList<>(cellMesh.getVertices().size());
        for(Coordinate vertex : cellMesh.getVertices()) {
            Coordinate translatedVertex = new Coordinate(vertex);
            double z = receiverHeight;
            if(!demTable.isEmpty()) {
                z = freeFieldFinder.getHeightAtPosition(translatedVertex) + receiverHeight;
            }
            translatedVertex.setOrdinate(2, z);
            vertices.add(translatedVertex);
        }
        List<Triangle> triangles = new ArrayList<>();
        for(Triangle triangle : cellMesh.getTriangles()) {
            if(triangle.getAttribute() == 0) {
                triangles.add(triangle);
            }
        }
        nbreceivers += vertices.size();


        // Fetch soil areas
        List<GeoWithSoilType> geoWithSoil = new ArrayList<>();
        fetchCellSoilAreas(connection, expandedCellEnvelop, geoWithSoil);
        if(geoWithSoil.isEmpty()){
            geoWithSoil = null;
        }
        PropagationProcessData threadData = new PropagationProcessData(
                vertices, freeFieldFinder, sourcesIndex,
                sourceGeometries, wj_sources, db_field_freq,
                soundReflectionOrder, soundDiffractionOrder, maximumPropagationDistance, maximumReflectionDistance,
                roadWidth, wallAbsorption, ij,
                progression.subProcess(vertices.size()), geoWithSoil, computeVerticalDiffraction);
        PropagationProcess propaProcess = new PropagationProcess(
                threadData, threadDataOut);
        propaProcess.run();
        Stack<PropagationResultTriRecord> toDriver = new Stack<>();
        int tri_id = 0;
        double[] verticesSoundLevel = threadDataOut.getVerticesSoundLevel();
        for (Triangle tri : triangles) {
            Coordinate pverts[] = {vertices.get(tri.getA()),
                    vertices.get(tri.getB()),
                    vertices.get(tri.getC()),
                    vertices.get(tri.getA())};
            toDriver.add(new PropagationResultTriRecord(
                    geometryFactory.createPolygon(geometryFactory.createLinearRing(pverts), null),
                    verticesSoundLevel[tri.getA()],
                    verticesSoundLevel[tri.getB()],
                    verticesSoundLevel[tri.getC()],
                    ij,
                    tri_id));
            tri_id++;
        }
        return toDriver;
    }

    public double getRoadWidth() {
        return roadWidth;
    }

    public void setRoadWidth(double roadWidth) {
        this.roadWidth = roadWidth;
    }

    public double getSourceDensification() {
        return sourceDensification;
    }

    public void setSourceDensification(double sourceDensification) {
        this.sourceDensification = sourceDensification;
    }

    public double getMaximumArea() {
        return maximumArea;
    }

    public void setMaximumArea(double maximumArea) {
        this.maximumArea = maximumArea;
    }

    public double getReceiverHeight() {
        return receiverHeight;
    }

    public void setReceiverHeight(double receiverHeight) {
        this.receiverHeight = receiverHeight;
    }
}
