package org.orbisgis.noisemap.core.jdbc;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.ComputeRays;
import org.orbisgis.noisemap.core.ComputeRaysOut;
import org.orbisgis.noisemap.core.FastObstructionTest;
import org.orbisgis.noisemap.core.GeoWithSoilType;
import org.orbisgis.noisemap.core.LayerDelaunayError;
import org.orbisgis.noisemap.core.MeshBuilder;
import org.orbisgis.noisemap.core.PropagationProcessData;
import org.orbisgis.noisemap.core.PropagationProcessPathData;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.orbisgis.noisemap.core.QueryGeometryStructure;
import org.orbisgis.noisemap.core.QueryQuadTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Compute noise propagation at specified receiver points.
 * @author Nicolas Fortin
 */
public class PointNoiseMap extends JdbcNoiseMap {
    private final String receiverTableName;
    private Logger logger = LoggerFactory.getLogger(PointNoiseMap.class);
    private PropagationProcessPathData propagationProcessPathData = new PropagationProcessPathData();

    public PointNoiseMap(String buildingsTableName, String sourcesTableName, String receiverTableName) {
        super(buildingsTableName, sourcesTableName);
        this.receiverTableName = receiverTableName;
    }

    /**
     * Initialisation of data structures needed for sound propagation.
     * @param connection JDBC Connection
     * @param cellI Cell I [0-{@link #getGridDim()}]
     * @param cellJ Cell J [0-{@link #getGridDim()}]
     * @param progression Progression info
     * @param receiversPk [out] receivers primary key extraction
     * @return Data input for cell evaluation
     * @throws SQLException
     */
    public PropagationProcessData prepareCell(Connection connection,int cellI, int cellJ,
                                              ProgressVisitor progression, List<Long> receiversPk, Set<Long> skipReceivers) throws SQLException {
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
        fetchCellBuildings(connection, expandedCellEnvelop, buildingsGeometries,null, mesh);
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
        ArrayList<Geometry> sourceGeometries = new ArrayList<>();
        ArrayList<ArrayList<Double>> wj_sources = new ArrayList<>();
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();

        // Fetch all source located in expandedCellEnvelop
        fetchCellSource(connection, expandedCellEnvelop, null, sourceGeometries, wj_sources, sourcesIndex);

        // Fetch soil areas
        List<GeoWithSoilType> geoWithSoil = new ArrayList<>();
        fetchCellSoilAreas(connection, expandedCellEnvelop, geoWithSoil);
        if(geoWithSoil.isEmpty()){
            geoWithSoil = null;
        }

        // Fetch receivers

        List<Coordinate> receivers = new ArrayList<>();
        String receiverGeomName = SFSUtilities.getGeometryFields(connection,
                TableLocation.parse(receiverTableName)).get(0);
        int intPk = JDBCUtilities.getIntegerPrimaryKey(connection, receiverTableName);
        String pkSelect = "";
        if(intPk >= 1) {
            pkSelect = ", " + JDBCUtilities.getFieldName(connection.getMetaData(), receiverTableName, intPk);
        } else {
            throw new SQLException(String.format("Table %s missing primary key for receiver identification", receiverTableName));
        }
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName) + pkSelect + " FROM " +
                        receiverTableName + " WHERE " +
                        TableLocation.quoteIdentifier(receiverGeomName) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(cellEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    long receiverPk = rs.getLong(2);
                    if(skipReceivers.contains(receiverPk)) {
                        continue;
                    }
                    Geometry pt = rs.getGeometry();
                    if(pt != null && !pt.isEmpty()) {
                        receiversPk.add(receiverPk);
                        receivers.add(pt.getCoordinate());
                    }
                }
            }
        }

        return new PropagationProcessData(
                receivers, freeFieldFinder, sourcesIndex,
                sourceGeometries, null, db_field_freq,
                soundReflectionOrder, computeHorizontalDiffraction, maximumPropagationDistance, maximumReflectionDistance,
                0, wallAbsorption, DEFAULT_WIND_ROSE, forgetSource, ij,
                progression.subProcess(receivers.size()), geoWithSoil, computeVerticalDiffraction);
    }

    @Override
    protected Envelope getComputationEnvelope(Connection connection) throws SQLException {
        return SFSUtilities.getTableEnvelope(connection, TableLocation.parse(receiverTableName), "");
    }

    /**
     * Launch sound propagation
     * @param connection
     * @param cellI
     * @param cellJ
     * @param progression
     * @return
     * @throws SQLException
     */
    public Collection<PropagationResultPtRecord> evaluateCell(Connection connection, int cellI, int cellJ,
                                                              ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException {
        List<Long> receiversPk = new ArrayList<>();
        PropagationProcessData threadData = prepareCell(connection, cellI, cellJ, progression, receiversPk, skipReceivers);

        ComputeRaysOut computeRaysOut = new ComputeRaysOut(false, propagationProcessPathData);

        ComputeRays computeRays = new ComputeRays(threadData);

        if(!absoluteZCoordinates) {
            computeRays.makeRelativeZToAbsolute();
        }
        computeRays.run(computeRaysOut);

        return new cellResult(computeRaysOut.getVerticesSoundLevel(), threadData.cellId);
    }

    private static class cellResult extends AbstractCollection<PropagationResultPtRecord> {
        private List<ComputeRaysOut.verticeSL> verticeSLS;
        private int cellId;

        public cellResult(List<ComputeRaysOut.verticeSL> verticeSLS, int cellId) {
            this.verticeSLS = verticeSLS;
            this.cellId = cellId;
        }

        @Override
        public Iterator<PropagationResultPtRecord> iterator() {
            return new cellResultIterator(verticeSLS, cellId);
        }

        @Override
        public int size() {
            return 0;
        }
    }

    private static class cellResultIterator implements Iterator<PropagationResultPtRecord> {
        private List<ComputeRaysOut.verticeSL> verticeSLS;
        private int cellId;
        private int index = 0;

        public cellResultIterator(List<ComputeRaysOut.verticeSL> verticeSLS, int cellId) {
            this.verticeSLS = verticeSLS;
            this.cellId = cellId;
        }

        @Override
        public boolean hasNext() {
            return index < verticeSLS.size();
        }

        @Override
        public PropagationResultPtRecord next() {
            ComputeRaysOut.verticeSL v = verticeSLS.get(index++);
            return new PropagationResultPtRecord(v.receiverId, v.sourceId, cellId, v.value);
        }
    }
}
