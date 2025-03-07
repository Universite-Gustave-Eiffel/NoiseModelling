/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.*;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.DefaultCutPlaneProcessing;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitorFactory;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Compute noise propagation at specified receiver points.
 * @author Nicolas Fortin
 */
public class NoiseMapByReceiverMaker extends GridMapMaker {
    private final String receiverTableName;
    private TableLoader tableLoader = new DefaultTableLoader();
    /** Tell table writer thread to empty current stacks then stop waiting for new data */
    public AtomicBoolean exitWhenDone = new AtomicBoolean(false);
    /** If true, all processing are aborted and all threads will be shutdown */
    public AtomicBoolean aborted = new AtomicBoolean(false);
    private final NoiseMapDatabaseParameters noiseMapDatabaseParameters = new NoiseMapDatabaseParameters();
    private IComputeRaysOutFactory computeRaysOutFactory = new DefaultCutPlaneProcessing(noiseMapDatabaseParameters, exitWhenDone, aborted);
    private Logger logger = LoggerFactory.getLogger(NoiseMapByReceiverMaker.class);
    private int threadCount = 0;
    private ProfilerThread profilerThread;

    SceneDatabaseInputSettings sceneDatabaseInputSettings = new SceneDatabaseInputSettings();

    /** ?? for train source ? TODO is it related to sources ? if yes then provide a special column for this kind of source */

    public NoiseMapByReceiverMaker(String buildingsTableName, String sourcesTableName, String receiverTableName) {
        super(buildingsTableName, sourcesTableName);
        this.receiverTableName = receiverTableName;
    }

    /**
     * @return Settings of the database (expected tables names; fields, global settings of the computation)
     */
    public NoiseMapDatabaseParameters getNoiseMapDatabaseParameters() {
        return noiseMapDatabaseParameters;
    }


    /**
     * This table must contain a source identifier column named IDSOURCE, a **PERIOD** VARCHAR field,
     * and emission spectrum in dB(A).
     * Spectrum column name must be LW{@link #sound_lvl_field}. Where HERTZ is a number
     * @return Source emission table name*
     */
    public String getSourcesEmissionTableName() {
        return sceneDatabaseInputSettings.getSourcesEmissionTableName();
    }


    public SceneDatabaseInputSettings.INPUT_MODE getInputMode() {
        return sceneDatabaseInputSettings.getInputMode();
    }

    public void setInputMode(SceneDatabaseInputSettings.INPUT_MODE inputMode) {
        sceneDatabaseInputSettings.setInputMode(inputMode);
    }

    public String getSourceEmissionPrimaryKeyField() {
        return sceneDatabaseInputSettings.getSourceEmissionPrimaryKeyField();
    }

    public void setSourceEmissionPrimaryKeyField(String sourceEmissionPrimaryKeyField) {
        sceneDatabaseInputSettings.setSourceEmissionPrimaryKeyField(sourceEmissionPrimaryKeyField);
    }


    public String getFrequencyFieldPrepend() {
        return sceneDatabaseInputSettings.getFrequencyFieldPrepend();
    }

    /**
     * @param frequencyFieldPrepend Text preceding the frequency in source emission table (default LW)
     */
    public void setFrequencyFieldPrepend(String frequencyFieldPrepend) {
        sceneDatabaseInputSettings.setFrequencyFieldPrepend(frequencyFieldPrepend);
    }

    public SceneDatabaseInputSettings getSceneInputSettings() {
        return sceneDatabaseInputSettings;
    }

    /**
     * This table must contain a source identifier column named IDSOURCE, a **PERIOD** VARCHAR field,
     * and emission spectrum in dB(A) or road traffic information
     * Spectrum column name must be LW{@link #sound_lvl_field}. Where HERTZ is a number
     * @param sourcesEmissionTableName Source emission table name
     */
    public void setSourcesEmissionTableName(String sourcesEmissionTableName) {
        sceneDatabaseInputSettings.setSourcesEmissionTableName(sourcesEmissionTableName);
    }

    /**
     * true if train propagation is computed (multiple reflection between the train and a screen)
     */
    public boolean isBodyBarrier() {
        return bodyBarrier;
    }

    /**
     * Computation stacks and timing are collected by this class in order
     * to profile the execution of the simulation
     * @return Instance of ProfilerThread or null
     */
    public ProfilerThread getProfilerThread() {
        return profilerThread;
    }

    /**
     * @return Receiver table name
     */
    public String getReceiverTableName() {
        return receiverTableName;
    }

    /**
     * Computation stacks and timing are collected by this class in order
     * to profile the execution of the simulation
     * @param profilerThread Instance of ProfilerThread
     */
    public void setProfilerThread(ProfilerThread profilerThread) {
        this.profilerThread = profilerThread;
    }

    public void setComputeRaysOutFactory(IComputeRaysOutFactory computeRaysOutFactory) {
        this.computeRaysOutFactory = computeRaysOutFactory;
    }

    /**
     * Do not call this method after {@link #initialize(Connection, ProgressVisitor)} has been called
     * @param tableLoader Object that generate scene for each sub-cell using database data
     */
    public void setPropagationProcessDataFactory(TableLoader tableLoader) {
        this.tableLoader = tableLoader;
    }

    /**
     * @return Object that generate scene for each sub-cell using database data
     */
    public TableLoader getPropagationProcessDataFactory() {
        return tableLoader;
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
     * @param cellIndex Computation area index
     * @param skipReceivers Do not process the receivers primary keys in this set and once included add the new receivers primary in it
     * @return Data input for cell evaluation
     * @throws SQLException
     */
    public SceneWithEmission prepareCell(Connection connection, CellIndex cellIndex,
                                         Set<Long> skipReceivers) throws SQLException, IOException {

        Envelope cellEnvelope = getCellEnv(cellIndex);

        if(verbose) {
            int ij = cellIndex.getLatitudeIndex() * gridDim + cellIndex.getLongitudeIndex() + 1;
            WKTWriter roundWKTWriter = new WKTWriter();
            roundWKTWriter.setPrecisionModel(new PrecisionModel(1.0));
            logger.info("Begin processing of cell {}/{} Compute domain is:\n {}", ij, gridDim * gridDim,
                    roundWKTWriter.write(geometryFactory.toGeometry(cellEnvelope)));
        }

        return tableLoader.create(connection, cellIndex, skipReceivers);
    }

    /**
     * Retrieves the computation envelope based on data stored in the database tables.
     * @param connection the database connection.
     * @return the computation envelope containing the bounding box of the data stored in the specified tables.
     * @throws SQLException
     */
    @Override
    protected Envelope getComputationEnvelope(Connection connection) throws SQLException {
        DBTypes dbTypes = DBUtils.getDBType(connection);
        Envelope envelopeInternal = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(receiverTableName, dbTypes)).getEnvelopeInternal();
        envelopeInternal.expandBy(maximumPropagationDistance);
        return envelopeInternal;
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
        List<String> geometryFields = GeometryTableUtilities.getGeometryColumnNames(connection, TableLocation.parse(receiverTableName));
        String geometryField;
        if(geometryFields.isEmpty()) {
            throw new SQLException("The table "+receiverTableName+" does not contain a Geometry field, then the extent " +
                    "cannot be computed");
        }
        logger.info("Collect all receivers in order to localize populated cells");
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
                if(pt != null && !pt.isEmpty()) {
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
     * @param connection JDBC Connection
     * @param cellIndex Computation area index
     * @param progression Progression info
     * @param skipReceivers Do not process the receivers primary keys in this set and once included add the new receivers primary in it
     * @return Output data instance for this cell
     * @throws SQLException Sql exception instance
     */
    public CutPlaneVisitorFactory evaluateCell(Connection connection, CellIndex cellIndex,
                                        ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException, IOException {
        SceneWithEmission scene = prepareCell(connection, cellIndex, skipReceivers);

        if(verbose) {
            logger.info(String.format("This computation area contains %d receivers %d sound sources and %d buildings",
                    scene.receivers.size(), scene.sourceGeometries.size(),
                    scene.profileBuilder.getBuildingCount()));
        }

        CutPlaneVisitorFactory computeRaysOut = computeRaysOutFactory.create(scene);

        PathFinder computeRays = new PathFinder(scene, progression);

        if(profilerThread != null) {
            computeRays.setProfilerThread(profilerThread);
        }

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

    /**
     * @return Class used to load tables for input data
     */
    public TableLoader getTableLoader() {
        return tableLoader;
    }

    /**
     * Initializes the noise map computation process.
     * @param connection Active connection
     * @param progression
     * @throws SQLException
     */
    @Override
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        super.initialize(connection, progression);
        tableLoader.initialize(connection, this);
        computeRaysOutFactory.initialize(connection, this);
    }

    /**
     * Run NoiseModelling with provided parameters, return when computation is done
     */
    public void run(Connection connection, ProgressVisitor progressLogger) throws SQLException {
        initialize(connection, progressLogger);

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        // Fetch cell identifiers with receivers
        Map<CellIndex, Integer> cells = searchPopulatedCells(connection);
        ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());

        try {
            computeRaysOutFactory.start(progressVisitor);
            for (CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                try {
                    evaluateCell(connection, cellIndex, progressVisitor, receivers);
                } catch (IOException ex) {
                    throw new SQLException(ex);
                }
            }
        } finally {
            computeRaysOutFactory.stop();
        }
    }

    /**
     * A factory interface for initializing input propagation process data for noise map computation.
     */
    public interface TableLoader {

        /**
         * Called only once when the settings are set.
         * @param connection             the database connection to be used for initialization.
         * @param noiseMapByReceiverMaker the noise map by receiver maker object associated with the computation process.
         * @throws SQLException if an SQL exception occurs while initializing the propagation process data factory.
         */
        void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException;

        /**
         * Called on each sub-domain in order to create cell input data.
         *
         * @param connection          Active connection
         * @param cellIndex           Active cell covering the computation
         * @param skipReceivers Do not process the receivers primary keys in this set and once included add the new receivers primary in it
         * @return Scene to feed the data
         */
        SceneWithEmission create(Connection connection, CellIndex cellIndex, Set<Long> skipReceivers) throws SQLException;
    }

    /**
     * A factory interface for creating objects that compute rays out for noise map computation.
     */
    public interface IComputeRaysOutFactory {

        /**
         * Called only once when the settings are set.
         * @param connection             the database connection to be used for initialization.
         * @param noiseMapByReceiverMaker the noise map by receiver maker object associated with the computation process.
         * @throws SQLException if an SQL exception occurs while initializing the propagation process data factory.
         */
        void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException;

        /**
         * Called before the first sub cell is being computed
         * @param progressLogger Main progression information, this method will not update the progression
         * @throws SQLException
         */
        void start(ProgressVisitor progressLogger) throws SQLException;

        /**
         * Called when all sub-cells have been processed
         * @throws SQLException
         */
        void stop() throws SQLException;

        /**
         * Creates an object that computes paths out for noise map computation.
         * @param cellData the scene data for the current computation cell
         * @return an object that computes paths out for noise map computation.
         */
        CutPlaneVisitorFactory create(SceneWithEmission cellData);
    }


}
