package org.orbisgis.noisemap.core.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.orbisgis.noisemap.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static org.orbisgis.noisemap.core.ComputeRays.dbaToW;
import static org.orbisgis.noisemap.core.ComputeRays.wToDba;

/**
 * Compute noise propagation at specified receiver points - Get Transfer Matrix
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */

public class PointNoiseMap_Cnossos_Att extends JdbcNoiseMap {
    private final String receiverTableName;
    private final String sourcesTableName;
    private Logger logger = LoggerFactory.getLogger(PointNoiseMap_Cnossos_Att.class);

    public PointNoiseMap_Cnossos_Att(String buildingsTableName, String sourcesTableName, String receiverTableName) {
        super(buildingsTableName, sourcesTableName);
        this.receiverTableName = receiverTableName;
        this.sourcesTableName = sourcesTableName;
    }

    /**
     * Initialisation of data structures needed for sound propagation.
     *
     * @param connection  JDBC Connection
     * @param cellI       Cell I [0-{@link #getGridDim()}]
     * @param cellJ       Cell J [0-{@link #getGridDim()}]
     * @param progression Progression info
     * @param receiversPk [out] receivers primary key extraction
     * @param sourcePk    [out] sources primary key extraction
     * @return Data input for cell evaluation
     * @throws SQLException
     */
    public PropagationProcessData prepareCell(Connection connection, int cellI, int cellJ,
                                              ProgressVisitor progression, List<Long> receiversPk, List<Long> sourcePk, Set<Long> skipReceivers) throws SQLException {
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
            logger.info("Problem with this polygon: " + expandedCellEnvelop + " build");
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
        fetchCellSource_withindex(connection, expandedCellEnvelop, null, sourceGeometries, sourcePk, wj_sources, sourcesIndex);

        // Fetch soil areas
        List<GeoWithSoilType> geoWithSoil = new ArrayList<>();
        fetchCellSoilAreas(connection, expandedCellEnvelop, geoWithSoil);
        if (geoWithSoil.isEmpty()) {
            geoWithSoil = null;
        }

        // Fetch receivers
        List<Coordinate> receivers = new ArrayList<>();
        String receiverGeomName = SFSUtilities.getGeometryFields(connection,
                TableLocation.parse(receiverTableName)).get(0);
        int intPk = JDBCUtilities.getIntegerPrimaryKey(connection, receiverTableName);
        String pkSelect = "";
        if (intPk >= 1) {
            pkSelect = ", " + JDBCUtilities.getFieldName(connection.getMetaData(), receiverTableName, intPk);
        }
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName) + pkSelect + " FROM " +
                        receiverTableName + " WHERE " +
                        TableLocation.quoteIdentifier(receiverGeomName) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(cellEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    if (!pkSelect.isEmpty()) {
                        long receiverPk = rs.getLong(2);
                        if (skipReceivers.contains(receiverPk)) {
                            continue;
                        }
                        receiversPk.add(receiverPk);
                    }
                    Geometry pt = rs.getGeometry();
                    if (pt != null) {
                        receivers.add(pt.getCoordinate());
                    }

                }
            }
        }


        return new PropagationProcessData_Att(
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

    private double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(p*dbaToW(array1[i])+ (1-p)*dbaToW(array2[i]));
        }
        return sum;
    }

    private double[] sumArray(double[] array1, double[] array2) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(dbaToW(array1[i])+ dbaToW(array2[i]));
        }
        return sum;
    }
    private double[] computeWithMeteo(PropagationProcessPathData propData,ComputeRaysOut propDataOut, double p) {
        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        double[] aGlobalMeteo = new double[8];
        double[] aGlobal = new double[]{-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000};
        for (PropagationPath propath:propDataOut.propagationPaths) {
            propath.setFavorable(false);
            evaluateAttenuationCnossos.evaluate(propath, propData);
            aGlobalMeteo = evaluateAttenuationCnossos.getaGlobal();
            propath.setFavorable(true);
            evaluateAttenuationCnossos.evaluate(propath, propData);
            aGlobalMeteo = sumArrayWithPonderation(aGlobalMeteo, evaluateAttenuationCnossos.getaGlobal(),p);
            aGlobal = sumArray(aGlobal,aGlobalMeteo);
        }
        return aGlobal;
    }

    /**
     * Launch sound propagation
     *
     * @param connection
     * @param cellI
     * @param cellJ
     * @param progression
     * @return
     * @throws SQLException
     */
    public Collection<PropagationResultPtRecord_Att_f> evaluateCell(Connection connection, int cellI, int cellJ,
                                                                    ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException {
        PropagationProcessOut_Att_f threadDataOut = new PropagationProcessOut_Att_f();

        List<Long> receiversPk = new ArrayList<>();
        List<Long> sourcesPk = new ArrayList<>();

        PropagationProcessData threadData = prepareCell(connection, cellI, cellJ, progression, receiversPk, sourcesPk, skipReceivers);


        //PropagationProcess_Att_f propaProcess = new PropagationProcess_Att_f(
        //        threadData, threadDataOut);

        //PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
         //       freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
        PropagationProcessData rayData = prepareCell(connection, cellI, cellJ, progression, receiversPk, sourcesPk, skipReceivers);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[8];
        List<PropagationDebugInfo> debug = new ArrayList<>();

        if (!absoluteZCoordinates) {
            // TODO get back this stuff
            //ComputeRays.makeRelativeZToAbsolute();
        }

        //propaProcess.run();
        List<PropagationProcessOut_Att_f.verticeSL> verticesSoundLevel = threadDataOut.getVerticesSoundLevel();
        Stack<PropagationResultPtRecord_Att_f> toDriver = new Stack<>();

        //Vertices output type
        if (receiversPk.isEmpty()) {
            for (PropagationProcessOut_Att_f.verticeSL result : verticesSoundLevel) {

                /*computeRays.computeRaysAtPosition(new Coordinate(200, 10, 14), energeticSum, debug);


                double p = 0.5; // probability favourable conditions
                PropagationProcessPathData propData = new PropagationProcessPathData();
                propData.setTemperature(10);
                propData.setHumidity(70);
                propData.setPrime2520(true);

                double[] aGlobal = computeWithMeteo(propData, propDataOut, p);
                splCompare(aGlobal, "Test T05", new double[]{-55.74,-55.79,-55.92,-56.09,-56.43,-57.59,-62.09,-78.46}, ERROR_EPSILON_TEST_T);*/

                toDriver.add(new PropagationResultPtRecord_Att_f(result.receiverId, result.sourceId, threadData.cellId, result.value[0],
                        result.value[1], result.value[2], result.value[3], result.value[4], result.value[5], result.value[6], result.value[7],
                        result.value[8]));
            }
        } else {
            for (PropagationProcessOut_Att_f.verticeSL result : verticesSoundLevel) {
                toDriver.add(new PropagationResultPtRecord_Att_f(receiversPk.get(result.receiverId), sourcesPk.get(result.sourceId), threadData.cellId, result.value[0],
                        result.value[1], result.value[2], result.value[3], result.value[4], result.value[5], result.value[6], result.value[7],
                        result.value[8]));
            }
        }


        return toDriver;
    }
}
