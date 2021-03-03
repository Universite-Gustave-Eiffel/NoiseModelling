package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.Triangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Create input receivers built from Delaunay for contructing a NoiseMap rendering.
 * SQL syntax is compatible with H2 and PostGIS.
 * @author Nicolas Fortin
 * @author SU Qi
 */
public class TriangleNoiseMap extends JdbcNoiseMap {
    private static final int BATCH_MAX_SIZE = 100;
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = LoggerFactory.getLogger(TriangleNoiseMap.class);
    private double roadWidth = 2;
    private double sourceDensification = 4;
    private double maximumArea = 75;
    private long nbreceivers = 0;
    private double receiverHeight = 1.6;
    private double buildingBuffer = 2;


    /**
     * @param buildingsTableName Buildings table
     * @param sourcesTableName Source table name
     */
    public TriangleNoiseMap(String buildingsTableName, String sourcesTableName) {
        super(buildingsTableName, sourcesTableName);
    }

    /**
     * @return Do not add receivers closer to specified distance
     */
    public double getBuildingBuffer() {
        return buildingBuffer;
    }

    /**
     * Do not add receivers closer to specified distance
     * @param buildingBuffer Distance in meters
     */
    public void setBuildingBuffer(double buildingBuffer) {
        this.buildingBuffer = buildingBuffer;
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
                              double srcPtDist, double triangleSide, double buildingBuffer) throws LayerDelaunayError {
        Envelope extendedEnvelope = new Envelope(boundingBoxFilter);
        extendedEnvelope.expandBy(srcDistance * 2.);
        Geometry linearRing = geometryFactory.toGeometry(boundingBoxFilter);
        if (!(linearRing instanceof Polygon)) {
            return;
        }
        Polygon boundingBox = (Polygon)linearRing;
        LinkedList<Geometry> toUnite = new LinkedList<>();
        Envelope fetchBox = new Envelope(boundingBoxFilter);
        fetchBox.expandBy(buildingBuffer);
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
            Geometry bufferBuildings = merge(toUnite, buildingBuffer);
            bufferBuildings = TopologyPreservingSimplifier.simplify(bufferBuildings,
                    minRecDist / 2);
            // Remove small artifacts due to buildingsTableName buffer
            if(triangleSide > 0) {
                bufferBuildings = Densifier.densify(bufferBuildings, triangleSide);
            }
            toUniteFinal.add(bufferBuildings); // Add buildingsTableName to triangulation
        }
        Geometry geom1 = geometryFactory.createPolygon();
        Geometry geom2 = geometryFactory.createPolygon();
        try {
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
            geom1 = union;
            geom2 = boundingBox;
            union = union.intersection(boundingBox);
            explodeAndAddPolygon(union, delaunayTool);
        } catch (TopologyException ex) {
            WKTWriter wktWriter = new WKTWriter(3);
            logger.error(String.format("Error with input geometries\n%s\n%s",wktWriter.write(geom1),wktWriter.write(geom2)), ex);
            throw ex;
        }
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
                                Envelope mainEnvelope, int cellI, int cellJ, double maxSrcDist, Collection<Geometry> sources,
                                double minRecDist, double srcPtDist, double maximumArea, double buildingBuffer)
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
        List<Geometry> buildings = new ArrayList<>(cellMesh.getPolygonWithHeight().size());
        for(MeshBuilder.PolygonWithHeight poly : cellMesh.getPolygonWithHeight()) {
            buildings.add(poly.getGeometry());
        }
        cellMesh.clearBuildings();
        feedDelaunay(buildings, cellMesh, cellEnvelope, maxSrcDist, delaunaySegments,
                minRecDist, srcPtDist, triangleSide, buildingBuffer);

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

    public void generateReceivers(Connection connection, int cellI, int cellJ, String receiverTableName, String trianglesTableName, AtomicInteger receiverPK) throws SQLException, LayerDelaunayError, IOException {
        // Compute the first pass delaunay mesh
        // The first pass doesn't take account of additional
        // vertices of neighbor cells at the borders
        // then, there are discontinuities in iso surfaces at each
        // border of cell
        MeshBuilder cellMesh = new MeshBuilder();
        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                cellJ, getCellWidth(), getCellHeight());
        // Fetch all source located in expandedCellEnvelop
        PropagationProcessData data = new PropagationProcessData(null);
        fetchCellSource(connection, cellEnvelope, data);

        List<Geometry> sourceDelaunayGeometries = data.sourceGeometries;
        fetchCellBuildings(connection, cellEnvelope, cellMesh);

        MeshBuilder demMesh = new MeshBuilder();
        FastObstructionTest freeFieldFinder = null;
        if(!demTable.isEmpty()) {
            fetchCellDem(connection, cellEnvelope, demMesh);
            demMesh.finishPolygonFeeding(cellEnvelope);
            freeFieldFinder = new FastObstructionTest(demMesh.getPolygonWithHeight(),
                    demMesh.getTriangles(), demMesh.getTriNeighbors(), demMesh.getVertices());
        }
        try {
            computeDelaunay(cellMesh, mainEnvelope, cellI,
                    cellJ,
                    maximumPropagationDistance, sourceDelaunayGeometries, roadWidth,
                    sourceDensification, maximumArea, buildingBuffer);
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
            if(freeFieldFinder != null) {
                z = freeFieldFinder.getHeightAtPosition(translatedVertex) + receiverHeight;
            }
            translatedVertex.setOrdinate(2, z);
            vertices.add(translatedVertex);
        }
        // Do not add triangles associated with buildings
        List<Triangle> triangles = new ArrayList<>();
        for(Triangle triangle : cellMesh.getTriangles()) {
            if(triangle.getAttribute() == 0) {
                triangles.add(triangle);
            }
        }
        nbreceivers += vertices.size();

        if(!JDBCUtilities.tableExists(connection, receiverTableName)) {
            Statement st = connection.createStatement();
            st.execute("CREATE TABLE "+TableLocation.parse(receiverTableName)+"(pk serial NOT NULL, the_geom geometry not null, PRIMARY KEY (PK))");
        }
        if(!JDBCUtilities.tableExists(connection, trianglesTableName)) {
            Statement st = connection.createStatement();
            st.execute("CREATE TABLE "+TableLocation.parse(trianglesTableName)+"(pk serial NOT NULL, the_geom geometry , PK_1 integer not null, PK_2 integer not null, PK_3 integer not null, cell_id integer not null, PRIMARY KEY (PK))");
        }
        int receiverPkOffset = receiverPK.get();
        // Add vertices to receivers
        PreparedStatement ps = connection.prepareStatement("INSERT INTO "+TableLocation.parse(receiverTableName)+" VALUES (?, ST_MAKEPOINT(?,?,?));");
        int batchSize = 0;
        for(Coordinate v : vertices) {
            ps.setInt(1, receiverPK.getAndAdd(1));
            ps.setDouble(2, v.x);
            ps.setDouble(3, v.y);
            ps.setDouble(4, v.z);
            ps.addBatch();
            batchSize++;
            if (batchSize >= BATCH_MAX_SIZE) {
                ps.executeBatch();
                ps.clearBatch();
                batchSize = 0;
            }
        }
        if (batchSize > 0) {
            ps.executeBatch();
        }
        // Add triangles
        ps = connection.prepareStatement("INSERT INTO "+TableLocation.parse(trianglesTableName)+"(the_geom, PK_1, PK_2, PK_3, CELL_ID) VALUES (?, ?, ?, ?, ?);");
        batchSize = 0;
        for(Triangle t : triangles) {
            ps.setObject(1, geometryFactory.createPolygon(new Coordinate[]{vertices.get(t.getA()),
                    vertices.get(t.getB()), vertices.get(t.getC()), vertices.get(t.getA())}));
            ps.setInt(2, t.getA() + receiverPkOffset);
            ps.setInt(3, t.getC() + receiverPkOffset);
            ps.setInt(4, t.getB() + receiverPkOffset);
            ps.setInt(5, cellI * gridDim + cellJ);
            ps.addBatch();
            batchSize++;
            if (batchSize >= BATCH_MAX_SIZE) {
                ps.executeBatch();
                ps.clearBatch();
                batchSize = 0;
            }
        }
        if (batchSize > 0) {
            ps.executeBatch();
        }
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

    public long getNbreceivers() {
        return nbreceivers;
    }
}
