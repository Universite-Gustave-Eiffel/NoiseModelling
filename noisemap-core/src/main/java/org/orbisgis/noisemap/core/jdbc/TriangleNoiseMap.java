package org.orbisgis.noisemap.core.jdbc;

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
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.h2gis.h2spatialapi.ProgressVisitor;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.jdelaunay.delaunay.evaluator.TriangleQuality;
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
import org.orbisgis.noisemap.core.ThreadPool;
import org.orbisgis.noisemap.core.Triangle;
import org.orbisgis.noisemap.core.TriangleConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
public class TriangleNoiseMap {
    // When computing cell size, try to keep propagation distance away from the cell
    // inferior to this ratio (in comparison with cell width)
    private static final double MINIMAL_BUFFER_RATIO = 0.3;
    private String destinationTable = "";
    private String buildingsTableName = "";
    private String sourcesTableName = "";
    private String soilTableName = "";
    // Digital elevation model table. (Contains points or triangles)
    private String demTable = "";
    private String sound_lvl_field = "DB_M";
    private double maximumPropagationDistance = 750;
    private double maximumReflectionDistance = 400;
    private int subdivisionLevel = -1; // TODO Guess it from maximumPropagationDistance and source extent
    private int soundReflectionOrder = 2;
    private int soundDiffractionOrder = 1;
    private double wallAbsorption = 0.05;
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = LoggerFactory.getLogger(TriangleNoiseMap.class);
    private String heightField = "";
    private GeometryFactory geometryFactory = new GeometryFactory();
    private boolean doMultiThreading = true;
    private double roadWidth = 2;
    private double sourceDensification = 4;
    private double maximumArea = 75;
    // Initialised attributes
    private int gridDim = 0;
    private Envelope mainEnvelope = new Envelope();
    private List<Integer> db_field_ids = new ArrayList<>();
    private List<Integer> db_field_freq = new ArrayList<>();
    private long nbreceivers = 0;


    /**
     *
     * @param destinationTable Where to write result
     * @param buildingsTableName Buildings table
     * @param sourcesTableName Source table name
     */
    public TriangleNoiseMap(String destinationTable, String buildingsTableName, String sourcesTableName) {
        this.destinationTable = destinationTable;
        this.buildingsTableName = buildingsTableName;
        this.sourcesTableName = sourcesTableName;
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
    private static Envelope getCellEnv(Envelope mainEnvelope, int cellI, int cellJ,
                                      int cellIMax, int cellJMax, double cellWidth, double cellHeight) {
        return new Envelope(mainEnvelope.getMinX() + cellI * cellWidth,
                mainEnvelope.getMinX() + cellI * cellWidth + cellWidth,
                mainEnvelope.getMinY() + cellHeight * cellJ,
                mainEnvelope.getMinY() + cellHeight * cellJ + cellHeight);
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
        LinkedList<Geometry> toUniteFinal = new LinkedList<Geometry>();
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
     * @param cellIMax I cell count
     * @param cellJMax J cell count
     * @param cellWidth Cell width meter
     * @param cellHeight Cell height meter
     * @param maxSrcDist Maximum propagation distance
     * @param minRecDist Minimal distance receiver-source
     * @param srcPtDist Densification distance of sources pts
     * @param maximumArea Maximum area of triangles
     * @throws LayerDelaunayError
     */
    public void computeFirstPassDelaunay(MeshBuilder cellMesh,
                                         Envelope mainEnvelope, int cellI, int cellJ, int cellIMax,
                                         int cellJMax, double cellWidth, double cellHeight,
                                         double maxSrcDist, Collection<Geometry> buildings,
                                         Collection<Geometry> sources, double minRecDist,
                                         double srcPtDist, double maximumArea)
            throws LayerDelaunayError {

        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI, cellJ,
                cellIMax, cellJMax, cellWidth, cellHeight);
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
            cellMesh.setInsertionEvaluator(new TriangleConstraint(maximumArea));
            Geometry densifiedEnvelope = Densifier.densify(new GeometryFactory().toGeometry(cellEnvelope), triangleSide);
            cellMesh.finishPolygonFeeding(densifiedEnvelope);
        } else {
            cellMesh.finishPolygonFeeding(cellEnvelope);
        }
    }


    private static Double DbaToW(Double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public Collection<PropagationResultTriRecord> evaluateCell(Connection connection,int cellI, int cellJ, ProgressVisitor progression) throws SQLException {
        Stack<PropagationResultTriRecord> toDriver = new Stack<>();
        PropagationProcessOut threadDataOut = new PropagationProcessOut(
                toDriver, null);

        MeshBuilder mesh = new MeshBuilder();
        int ij = cellI * gridDim + cellJ;
        logger.info("Begin processing of cell " + (cellI + 1) + ","
                + (cellJ + 1) + " of the " + gridDim + "x" + gridDim
                + "  grid..");

        double cellWidth = mainEnvelope.getWidth() / gridDim;
        double cellHeight = mainEnvelope.getHeight() / gridDim;
        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                cellJ, gridDim, gridDim, cellWidth, cellHeight);


        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maximumPropagationDistance);
        // //////////////////////////////////////////////////////
        // Make source index for optimization
        ArrayList<Geometry> sourceGeometries = new ArrayList<Geometry>();
        ArrayList<ArrayList<Double>> wj_sources = new ArrayList<ArrayList<Double>>();
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();

        // Fetch all source located in expandedCellEnvelop
        int idsource = 0;
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        String sourceGeomName = SFSUtilities.getGeometryFields(connection, sourceTableIdentifier).get(0);
        try (PreparedStatement st = connection.prepareStatement("SELECT * FROM " + sourcesTableName + " WHERE "
                + TableLocation.quoteIdentifier(sourceGeomName) + " && ?")) {
            st.setObject(1, geometryFactory.toGeometry(expandedCellEnvelop));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    Geometry geo = rs.getGeometry();
                    if (geo != null) {
                        sourcesIndex.appendGeometry(geo, idsource);
                        ArrayList<Double> wj_spectrum = new ArrayList<>();
                        wj_spectrum.ensureCapacity(db_field_ids.size());
                        for (Integer idcol : db_field_ids) {
                            wj_spectrum
                                    .add(DbaToW(rs.getDouble(idcol)));
                        }
                        wj_sources.add(wj_spectrum);
                        sourceGeometries.add(geo);
                        idsource++;
                    }
                }
            }
        }

        // //////////////////////////////////////////////////////
        // feed freeFieldFinder for fast intersection query
        // optimization
        // Fetch buildings in extendedEnvelope
        String queryHeight = "";
        if(!heightField.isEmpty()) {
            queryHeight = ", " + TableLocation.quoteIdentifier(heightField);
        }
        ArrayList<Geometry> buildingsGeometries = new ArrayList<>();
        String buildingGeomName = SFSUtilities.getGeometryFields(connection,
                TableLocation.parse(buildingsTableName)).get(0);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(buildingGeomName) + queryHeight + " FROM " +
                        buildingsTableName + " WHERE " +
                        TableLocation.quoteIdentifier(buildingGeomName) + " && ?")) {
            st.setObject(1, geometryFactory.toGeometry(expandedCellEnvelop));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    //if we don't have height of building
                    Geometry building = rs.getGeometry();
                    if(building != null) {
                        buildingsGeometries.add(building);
                        if (heightField.isEmpty()) {
                            mesh.addGeometry(building);
                        } else {
                            mesh.addGeometry(building, rs.getDouble(heightField));
                        }
                    }
                }
            }
        }
        //if we have topographic points data
        if(!demTable.isEmpty()) {
            String topoGeomName = SFSUtilities.getGeometryFields(connection,
                    TableLocation.parse(demTable)).get(0);
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(topoGeomName) + queryHeight + " FROM " +
                            demTable + " WHERE " +
                            TableLocation.quoteIdentifier(topoGeomName) + " && ?")) {
                st.setObject(1, geometryFactory.toGeometry(expandedCellEnvelop));
                try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                    while (rs.next()) {
                        Geometry pt = rs.getGeometry();
                        if(pt != null) {
                            mesh.addTopographicPoint(pt.getCoordinate());
                        }
                    }
                }
            }
        }
        try {
            mesh.finishPolygonFeeding(expandedCellEnvelop);
        } catch (LayerDelaunayError ex) {
            throw new SQLException(ex.getLocalizedMessage(), ex);
        }
        FastObstructionTest freeFieldFinder = new FastObstructionTest(mesh.getPolygonWithHeight(),
                mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        // Compute the first pass delaunay mesh
        // The first pass doesn't take account of additional
        // vertices of neighbor cells at the borders
        // then, there are discontinuities in iso surfaces at each
        // border of cell
        MeshBuilder cellMesh = new MeshBuilder();
        try {
            computeFirstPassDelaunay(cellMesh, mainEnvelope, cellI,
                    cellJ, gridDim, gridDim, cellWidth, cellHeight,
                    maximumPropagationDistance, buildingsGeometries, sourceGeometries, roadWidth,
                    sourceDensification, maximumArea);
        } catch (LayerDelaunayError err) {
            throw new SQLException(err.getLocalizedMessage(), err);
        }                    // Make a structure to keep the following information
        // Triangle list with 3 vertices(int), and 3 neighbor
        // triangle ID
        // Vertices list

        // The evaluation of sound level must be done where the
        // following vertices are
        List<Coordinate> vertices = cellMesh.getVertices();
        // TODO use topographic data to define Z of receivers (offset defined by user default to )
        List<Triangle> triangles = new ArrayList<>();
        for(Triangle triangle : cellMesh.getTriangles()) {
            if(triangle.getBuidlingID() == 0) {
                triangles.add(triangle);
            }
        }
        nbreceivers += vertices.size();


        // Fetch soil areas
        List<GeoWithSoilType> geoWithSoil = new ArrayList<>();
        if(!soilTableName.isEmpty()){
            String soilGeomName = SFSUtilities.getGeometryFields(connection,
                    TableLocation.parse(soilTableName)).get(0);
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(soilGeomName) + queryHeight + " FROM " +
                            soilTableName + " WHERE " +
                            TableLocation.quoteIdentifier(soilGeomName) + " && ?")) {
                st.setObject(1, geometryFactory.toGeometry(expandedCellEnvelop));
                try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                    while (rs.next()) {
                        Geometry poly = rs.getGeometry();
                        if(poly != null) {
                            geoWithSoil.add(new GeoWithSoilType(poly, rs.getDouble("G")));
                        }
                    }
                }
            }
        }
        if(geoWithSoil.isEmpty()){
            geoWithSoil = null;
        }
        PropagationProcessData threadData = new PropagationProcessData(
                vertices, null, triangles, freeFieldFinder, sourcesIndex,
                sourceGeometries, wj_sources, db_field_freq,
                soundReflectionOrder, soundDiffractionOrder, maximumPropagationDistance, maximumReflectionDistance,
                roadWidth, wallAbsorption, ij,
                progression.subProcess(vertices.size()), geoWithSoil);
        PropagationProcess propaProcess = new PropagationProcess(
                threadData, threadDataOut);
        propaProcess.run();
        return toDriver;
    }

    /**
     *
     * @param connection Active connection
     * @throws SQLException
     */
    public void execute(Connection connection, ProgressVisitor progression) throws SQLException {
        if(maximumPropagationDistance < maximumReflectionDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        if(sourcesTableName.isEmpty()) {
            throw new SQLException("A sound source table must be provided");
        }
        ThreadPool threadManager = null;
        // Steps of execution
        // Evaluation of the main bounding box (sourcesTableName+buildingsTableName)
        // Split domain into 4^subdiv cells
        // For each cell :
        // Expand bounding box cell by maxSrcDist
        // Build delaunay triangulation from buildingsTableName polygon processed by
        // intersection with non extended bounding box
        // Save the list of sourcesTableName index inside the extended bounding box
        // Save the list of buildingsTableName index inside the extended bounding box
        // Make a structure to keep the following information
        // Triangle list with the 3 vertices index
        // Vertices list (as receivers)
        // For each vertices within the cell bounding box (not the extended
        // one)
        // Find all sourcesTableName within maxSrcDist
        // For All found sourcesTableName
        // Test if there is a gap(no building) between source and receiver
        // if not then append the distance attenuated sound level to the
        // receiver
        // Save the triangle geometry with the db_m value of the 3 vertices
        // 1 Step - Evaluation of the main bounding box (sources)
        mainEnvelope = SFSUtilities.getTableEnvelope(connection, TableLocation.parse(sourcesTableName), "");
        // Split domain into 4^subdiv cells
        // Compute subdivision level using envelope and maximum propagation distance
        double greatestSideLength = mainEnvelope.maxExtent();
        subdivisionLevel = 0;
        while(maximumPropagationDistance / (greatestSideLength / Math.pow(2, subdivisionLevel)) < MINIMAL_BUFFER_RATIO) {
            subdivisionLevel++;
        }
        gridDim = (int) Math.pow(2, subdivisionLevel);

        // Initialization frequency declared in source Table
        db_field_ids = new ArrayList<>();
        db_field_freq = new ArrayList<>();
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        try(ResultSet rs = connection.getMetaData().getColumns(sourceTableIdentifier.getCatalog(),
            sourceTableIdentifier.getSchema(), sourceTableIdentifier.getTable(), null)) {
            while(rs.next()) {
                String fieldName = rs.getString("COLUMN_NAME");
                if (fieldName.startsWith(sound_lvl_field)) {
                    String sub = fieldName.substring(sound_lvl_field.length());
                    db_field_ids.add(rs.getInt("ORDINAL_POSITION"));
                    if (sub.length() > 0) {
                        int freq = Integer.parseInt(sub);
                        db_field_freq.add(freq);
                    } else {
                        db_field_freq.add(0);
                    }
                }
            }
        }
    }

    public void setDestinationTable(String destinationTable) {
        this.destinationTable = destinationTable;
    }

    public void setBuildingsTableName(String buildingsTableName) {
        this.buildingsTableName = buildingsTableName;
    }

    public void setSourcesTableName(String sourcesTableName) {
        this.sourcesTableName = sourcesTableName;
    }

    public void setSound_lvl_field(String sound_lvl_field) {
        this.sound_lvl_field = sound_lvl_field;
    }

    public void setMaximumPropagationDistance(double maximumPropagationDistance) {
        this.maximumPropagationDistance = maximumPropagationDistance;
    }

    public void setMaximumReflectionDistance(double maximumReflectionDistance) {
        this.maximumReflectionDistance = maximumReflectionDistance;
    }

    public void setSubdivisionLevel(int subdivisionLevel) {
        this.subdivisionLevel = subdivisionLevel;
    }

    public void setSoundReflectionOrder(int soundReflectionOrder) {
        this.soundReflectionOrder = soundReflectionOrder;
    }

    public void setSoundDiffractionOrder(int soundDiffractionOrder) {
        this.soundDiffractionOrder = soundDiffractionOrder;
    }

    public void setWallAbsorption(double wallAbsorption) {
        this.wallAbsorption = wallAbsorption;
    }

    public void setHeightField(String heightField) {
        this.heightField = heightField;
    }

    public void setMaximumArea(double maximumArea) {
        this.maximumArea = maximumArea;
    }
}
