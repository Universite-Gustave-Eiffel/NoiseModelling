package org.orbisgis.noisemap.core.jdbc;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * Compute noise propagation at specified receiver points.
 * @author Nicolas Fortin
 * @author Pierre Aumond 07/06/2016
 */
public class PointNoiseMap_f extends JdbcNoiseMap {
    private final String receiverTableName;
    private Logger logger = LoggerFactory.getLogger(PointNoiseMap_f.class);

    public PointNoiseMap_f(String buildingsTableName, String sourcesTableName, String receiverTableName) {
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
                                              ProgressVisitor progression, List<Long> receiversPk) throws SQLException {
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
        }
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName) + pkSelect + " FROM " +
                        receiverTableName + " WHERE " +
                        TableLocation.quoteIdentifier(receiverGeomName) + " && ?")) {
            st.setObject(1, geometryFactory.toGeometry(cellEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    Geometry pt = rs.getGeometry();
                    if(pt != null) {
                        receivers.add(pt.getCoordinate());
                    }
                    if(!pkSelect.isEmpty()) {
                        receiversPk.add(rs.getLong(2));
                    }
                }
            }
        }

        return new PropagationProcessData(
                receivers, freeFieldFinder, sourcesIndex,
                sourceGeometries, wj_sources, db_field_freq,
                soundReflectionOrder, soundDiffractionOrder, maximumPropagationDistance, maximumReflectionDistance,
                0, wallAbsorption, ij,
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
    public Collection<PropagationResultPtRecord_f> evaluateCell(Connection connection,int cellI, int cellJ,
                                                              ProgressVisitor progression) throws SQLException {
        PropagationProcessOut_f threadDataOut = new PropagationProcessOut_f();
        List<Long> receiversPk = new ArrayList<>();
        PropagationProcessData threadData = prepareCell(connection, cellI, cellJ, progression, receiversPk);

        PropagationProcess_f propaProcess = new PropagationProcess_f(
                threadData, threadDataOut);
        propaProcess.run();


        double[] verticesSoundLevel = threadDataOut.getVerticesSoundLevel(0);
        double[] verticesSoundLevel63 = threadDataOut.getVerticesSoundLevel(63);
        double[] verticesSoundLevel125 = threadDataOut.getVerticesSoundLevel(125);
        double[] verticesSoundLevel250 = threadDataOut.getVerticesSoundLevel(250);
        double[] verticesSoundLevel500 = threadDataOut.getVerticesSoundLevel(500);
        double[] verticesSoundLevel1000 = threadDataOut.getVerticesSoundLevel(1000);
        double[] verticesSoundLevel2000 = threadDataOut.getVerticesSoundLevel(2000);
        double[] verticesSoundLevel4000 = threadDataOut.getVerticesSoundLevel(4000);
        double[] verticesSoundLevel8000 = threadDataOut.getVerticesSoundLevel(8000);

        Stack<PropagationResultPtRecord_f> toDriver = new Stack<>();
        //Vertices output type
        if(receiversPk.isEmpty()) {
            for (int receiverId = 0; receiverId < threadData.receivers.size(); receiverId++) {
                toDriver.add(new PropagationResultPtRecord_f(receiverId, threadData.cellId, verticesSoundLevel[receiverId],
                        verticesSoundLevel63[receiverId], verticesSoundLevel125[receiverId], verticesSoundLevel250[receiverId],
                        verticesSoundLevel500[receiverId], verticesSoundLevel1000[receiverId], verticesSoundLevel2000[receiverId], verticesSoundLevel4000[receiverId], verticesSoundLevel8000[receiverId]));
            }
        } else {
            for (int receiverId = 0; receiverId < threadData.receivers.size(); receiverId++) {
                toDriver.add(new PropagationResultPtRecord_f(receiversPk.get(receiverId), threadData.cellId, verticesSoundLevel[receiverId],
                        verticesSoundLevel63[receiverId], verticesSoundLevel125[receiverId], verticesSoundLevel250[receiverId],
                        verticesSoundLevel500[receiverId], verticesSoundLevel1000[receiverId], verticesSoundLevel2000[receiverId], verticesSoundLevel4000[receiverId], verticesSoundLevel8000[receiverId]));
            }
        }
        return toDriver;
    }
}
