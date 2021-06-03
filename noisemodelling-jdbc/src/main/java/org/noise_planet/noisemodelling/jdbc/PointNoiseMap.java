/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;
import org.noise_planet.noisemodelling.pathfinder.ComputeRays;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.pathfinder.FastObstructionTest;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.MeshBuilder;
import org.noise_planet.noisemodelling.pathfinder.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Compute noise propagation at specified receiver points.
 * @author Nicolas Fortin
 */
public class PointNoiseMap extends JdbcNoiseMap {
    private final String receiverTableName;
    private PropagationProcessDataFactory propagationProcessDataFactory;
    private IComputeRaysOutFactory computeRaysOutFactory;
    private Logger logger = LoggerFactory.getLogger(PointNoiseMap.class);
    private int threadCount = 0;

    public PointNoiseMap(String buildingsTableName, String sourcesTableName, String receiverTableName) {
        super(buildingsTableName, sourcesTableName);
        this.receiverTableName = receiverTableName;
    }

    public void setComputeRaysOutFactory(IComputeRaysOutFactory computeRaysOutFactory) {
        this.computeRaysOutFactory = computeRaysOutFactory;
    }

    public void setPropagationProcessDataFactory(PropagationProcessDataFactory propagationProcessDataFactory) {
        this.propagationProcessDataFactory = propagationProcessDataFactory;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Initialisation of data structures needed for sound propagation.
     * @param connection JDBC Connection
     * @param cellI Cell I [0-{@link #getGridDim()}]
     * @param cellJ Cell J [0-{@link #getGridDim()}]
     * @param progression Progression info
     * @return Data input for cell evaluation
     * @throws SQLException
     */
    public PropagationProcessData prepareCell(Connection connection,int cellI, int cellJ,
                                              ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException, IOException {
        boolean isH2 = JDBCUtilities.isH2DataBase(connection.getMetaData());
        MeshBuilder mesh = new MeshBuilder();
        int ij = cellI * gridDim + cellJ + 1;
        if(verbose) {
            logger.info("Begin processing of cell " + ij + " / " + gridDim * gridDim);
        }
        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                cellJ, getCellWidth(), getCellHeight());


        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maximumPropagationDistance);

        // //////////////////////////////////////////////////////
        // feed freeFieldFinder for fast intersection query
        // optimization
        // Fetch buildings in extendedEnvelope
        fetchCellBuildings(connection, expandedCellEnvelop, mesh);
        //if we have topographic points data
        fetchCellDem(connection, expandedCellEnvelop, mesh);

        // Data fetching for collision test is done.
        Envelope meshEnvelope = new Envelope(expandedCellEnvelop);
        // Expand again envelope for sound sources or buildings sides that are close to the edge
        meshEnvelope.expandBy(10);
        try {
            mesh.finishPolygonFeeding(expandedCellEnvelop);
        } catch (LayerDelaunayError ex) {
            throw new SQLException(ex.getLocalizedMessage(), ex);
        }
        FastObstructionTest freeFieldFinder = new FastObstructionTest(mesh.getPolygonWithHeight(),
                mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());


        PropagationProcessData propagationProcessData;
        if(propagationProcessDataFactory != null) {
            propagationProcessData = propagationProcessDataFactory.create(freeFieldFinder);
        } else {
            propagationProcessData = new PropagationProcessData(freeFieldFinder);
        }
        propagationProcessData.reflexionOrder = soundReflectionOrder;
        propagationProcessData.maximumError = getMaximumError();
        propagationProcessData.maxRefDist = maximumReflectionDistance;
        propagationProcessData.maxSrcDist = maximumPropagationDistance;
        propagationProcessData.setComputeVerticalDiffraction(computeVerticalDiffraction);
        propagationProcessData.setComputeHorizontalDiffraction(computeHorizontalDiffraction);

        // Fetch all source located in expandedCellEnvelop
        fetchCellSource(connection, expandedCellEnvelop, propagationProcessData);

        propagationProcessData.cellId = ij;

        // Fetch soil areas
        fetchCellSoilAreas(connection, expandedCellEnvelop, propagationProcessData.getSoilList());

        // Fetch receivers

        String receiverGeomName = SFSUtilities.getGeometryFields(connection,
                TableLocation.parse(receiverTableName)).get(0);
        int intPk = JDBCUtilities.getIntegerPrimaryKey(connection, receiverTableName);
        String pkSelect = "";
        if(intPk >= 1) {
            pkSelect = ", " + TableLocation.quoteIdentifier(JDBCUtilities.getFieldName(connection.getMetaData(), receiverTableName, intPk), isH2);
        } else {
            throw new SQLException(String.format("Table %s missing primary key for receiver identification", receiverTableName));
        }
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName, isH2) + pkSelect + " FROM " +
                        receiverTableName + " WHERE " +
                        TableLocation.quoteIdentifier(receiverGeomName, isH2) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(cellEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    long receiverPk = rs.getLong(2);
                    if(skipReceivers.contains(receiverPk)) {
                        continue;
                    } else {
                        skipReceivers.add(receiverPk);
                    }
                    Geometry pt = rs.getGeometry();
                    if(pt != null && !pt.isEmpty()) {
                        propagationProcessData.addReceiver(receiverPk, pt.getCoordinate(), rs);
                    }
                }
            }
        }
        if(progression != null) {
            propagationProcessData.cellProg = progression.subProcess(propagationProcessData.receivers.size());
        }
        return propagationProcessData;
    }

    @Override
    protected Envelope getComputationEnvelope(Connection connection) throws SQLException {
        return SFSUtilities.getTableEnvelope(connection, TableLocation.parse(receiverTableName), "");
    }

    /**
     * Fetch all receivers and compute cells that contains receivers
     * @param connection
     * @return Cell index with number of receivers
     * @throws SQLException
     */
    public Map<CellIndex, Integer> searchPopulatedCells(Connection connection) throws SQLException {
        if(mainEnvelope == null) {
            throw new IllegalStateException("Call initialize before calling searchPopulatedCells");
        }
        Map<CellIndex, Integer> cellIndices = new HashMap<>();
        List<String> geometryFields = SFSUtilities.getGeometryFields(connection, TableLocation.parse(receiverTableName));
        String geometryField;
        if(geometryFields.isEmpty()) {
            throw new SQLException("The table "+receiverTableName+" does not contain a Geometry field, then the extent " +
                    "cannot be computed");
        }
        geometryField = geometryFields.get(0);
        ResultSet rs = connection.createStatement().executeQuery("SELECT " + geometryField + " FROM " + receiverTableName);
        // Construct RTree with cells envelopes
        STRtree rtree = new STRtree();
        for(int i = 0; i < gridDim; i++) {
            for(int j = 0; j < gridDim; j++) {
                Envelope refEnv = getCellEnv(mainEnvelope, i,
                        j, getCellWidth(), getCellHeight());
                rtree.insert(refEnv, new CellIndex(j, i));
            }
        }
        // Iterate over receivers and look for intersecting cells
        try (SpatialResultSet srs = rs.unwrap(SpatialResultSet.class)) {
            while (srs.next()) {
                Geometry pt = srs.getGeometry();
                if(pt instanceof Point && !pt.isEmpty()) {
                    Coordinate ptCoord = pt.getCoordinate();
                    List queryResult = rtree.query(new Envelope(ptCoord));
                    for(Object o : queryResult) {
                        if(o instanceof CellIndex) {
                            cellIndices.merge((CellIndex) o, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return cellIndices;
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
    public IComputeRaysOut evaluateCell(Connection connection, int cellI, int cellJ,
                                        ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException, IOException {
        PropagationProcessData threadData = prepareCell(connection, cellI, cellJ, progression, skipReceivers);

        if(verbose) {
            logger.info(String.format("This computation area contains %d receivers %d sound sources and %d buildings",
                    threadData.receivers.size(), threadData.sourceGeometries.size(),
                    threadData.freeFieldFinder.getBuildingCount()));
        }
        IComputeRaysOut computeRaysOut;
        if(computeRaysOutFactory == null) {
            computeRaysOut = new ComputeRaysOutAttenuation(false, propagationProcessPathData, threadData);
        } else {
            computeRaysOut = computeRaysOutFactory.create(threadData, propagationProcessPathData);
        }

        ComputeRays computeRays = new ComputeRays(threadData);

        if(threadCount > 0) {
            computeRays.setThreadCount(threadCount);
        }

        if(!receiverHasAbsoluteZCoordinates) {
            computeRays.makeReceiverRelativeZToAbsolute();
        }

        if(!sourceHasAbsoluteZCoordinates) {
            computeRays.makeSourceRelativeZToAbsolute();
        }

        computeRays.run(computeRaysOut);

        return computeRaysOut;
    }

    @Override
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        super.initialize(connection, progression);
        if(propagationProcessDataFactory != null) {
            propagationProcessDataFactory.initialize(connection, this);
        }
    }

    public interface PropagationProcessDataFactory {
        PropagationProcessData create(FastObstructionTest freeFieldFinder);

        void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException;
    }

    public interface IComputeRaysOutFactory {
        IComputeRaysOut create(PropagationProcessData threadData, PropagationProcessPathData pathData);
    }

    /**
     * Cell metadata computed from receivers table
     */
    public static class CellIndex implements Comparable<CellIndex> {
        int longitudeIndex;
        int latitudeIndex;

        public CellIndex(int longitudeIndex, int latitudeIndex) {
            this.longitudeIndex = longitudeIndex;
            this.latitudeIndex = latitudeIndex;
        }

        public int getLongitudeIndex() {
            return longitudeIndex;
        }

        public int getLatitudeIndex() {
            return latitudeIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CellIndex cellIndex = (CellIndex) o;
            return longitudeIndex == cellIndex.longitudeIndex && latitudeIndex == cellIndex.latitudeIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(longitudeIndex, latitudeIndex);
        }

        @Override
        public int compareTo(CellIndex o) {
            int comp = Integer.compare(latitudeIndex, o.latitudeIndex);
            if(comp != 0) {
                return comp;
            } else {
                return Integer.compare(longitudeIndex, o.longitudeIndex);
            }
        }
    }
}
