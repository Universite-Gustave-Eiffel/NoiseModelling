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
import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.Attenuation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;

/**
 * Split the domain according to an input receiver grid
 */
public class ReceiverGridNoiseMapLoader extends NoiseMapLoader  {
    /** Frequency bands values, by octave or third octave */
    public List<Integer> frequencyArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public List<Double> exactFrequencyArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE));
    public List<Double> aWeightingArray = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE));

    protected final NoiseMapParameters noiseMapParameters;
    List<String> noiseSource = Arrays.asList("ROLLING","TRACTIONA", "TRACTIONB","AERODYNAMICA","AERODYNAMICB","BRIDGE");

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();

    public ReceiverGridNoiseMapLoader(NoiseMapParameters noiseMapParameters) {
        super(noiseMapParameters.buildingsTableName, noiseMapParameters.sourcesTableName);
        this.noiseMapParameters = noiseMapParameters;
    }

    /**
     * Inserts directivity attributes for noise sources for trains into the directionAttributes map.
     */
    public void insertTrainDirectivity() {
        directionAttributes.clear();
        directionAttributes.put(0, new OmnidirectionalDirection());
        int i=1;
        for(String typeSource : noiseSource) {
            directionAttributes.put(i, new RailwayCnossosDirectivitySphere(new LineSource(typeSource)));
            i++;
        }
    }

    /**
     * Creates a scene object with the given profile builder.
     * @param builder the profile builder used to construct the scene.
     * @return the created scene object.
     */
    Scene create(ProfileBuilder builder) {
        Scene scene = new Scene(builder);
        scene.set
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
    public Scene prepareCell(Connection connection, int cellI, int cellJ,
                             ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException, IOException {
        DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
        ProfileBuilder builder = new ProfileBuilder();
        builder.setFrequencyArray(frequencyArray);
        int ij = cellI * gridDim + cellJ + 1;
        Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
                cellJ, getCellWidth(), getCellHeight());
        if(verbose) {
            WKTWriter roundWKTWriter = new WKTWriter();
            roundWKTWriter.setPrecisionModel(new PrecisionModel(1.0));
            logger.info("Begin processing of cell {}/{} Compute domain is:\n {}", ij, gridDim * gridDim,
                    roundWKTWriter.write(geometryFactory.toGeometry(cellEnvelope)));
        }
        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maximumPropagationDistance + 2 * maximumReflectionDistance);

        // //////////////////////////////////////////////////////
        // feed freeFieldFinder for fast intersection query
        // optimization
        // Fetch buildings in extendedEnvelope
        fetchCellBuildings(connection, expandedCellEnvelop, builder);
        //if we have topographic points data
        fetchCellDem(connection, expandedCellEnvelop, builder);

        // Fetch soil areas
        fetchCellSoilAreas(connection, expandedCellEnvelop, builder);

        builder.finishFeeding();

        expandedCellEnvelop = new Envelope(cellEnvelope);
        expandedCellEnvelop.expandBy(maximumPropagationDistance);

        Scene scene;
        if(propagationProcessDataFactory != null) {
            scene = propagationProcessDataFactory.create(builder);
        } else {
            scene = new Scene(builder);
        }
        scene.reflexionOrder = soundReflectionOrder;
        scene.setBodyBarrier(bodyBarrier);
        scene.maxRefDist = maximumReflectionDistance;
        scene.maxSrcDist = maximumPropagationDistance;
        scene.gS = getGs();
        scene.setComputeVerticalDiffraction(computeVerticalDiffraction);
        scene.setComputeHorizontalDiffraction(computeHorizontalDiffraction);

        // Fetch all source located in expandedCellEnvelop
        fetchCellSource(connection, expandedCellEnvelop, scene, true);

        scene.cellId = ij;

        // Fetch receivers

        String receiverGeomName = GeometryTableUtilities.getGeometryColumnNames(connection,
                TableLocation.parse(noiseMapParameters.receiverTableName)).get(0);
        int intPk = JDBCUtilities.getIntegerPrimaryKey(connection.unwrap(Connection.class), TableLocation.parse(noiseMapParameters.receiverTableName, dbType));
        String pkSelect = "";
        if(intPk >= 1) {
            pkSelect = ", " + TableLocation.quoteIdentifier(JDBCUtilities.getColumnName(connection, noiseMapParameters.receiverTableName, intPk), dbType);
        } else {
            throw new SQLException(String.format("Table %s missing primary key for receiver identification", noiseMapParameters.receiverTableName));
        }
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName, dbType ) + pkSelect + " FROM " +
                        noiseMapParameters.receiverTableName + " WHERE " +
                        TableLocation.quoteIdentifier(receiverGeomName, dbType) + " && ?::geometry")) {
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
                        // check z value
                        if(pt.getCoordinate().getZ() == Coordinate.NULL_ORDINATE) {
                            throw new IllegalArgumentException("The table " + noiseMapParameters.receiverTableName +
                                    " contain at least one receiver without Z ordinate." +
                                    " You must specify X,Y,Z for each receiver");
                        }
                        scene.addReceiver(receiverPk, pt.getCoordinate(), rs);
                    }
                }
            }
        }
        if(progression != null) {
            scene.cellProg = progression.subProcess(scene.receivers.size());
        }
        return scene;
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
        List<String> geometryFields = GeometryTableUtilities.getGeometryColumnNames(connection, TableLocation.parse(noiseMapParameters.receiverTableName));
        String geometryField;
        if(geometryFields.isEmpty()) {
            throw new SQLException("The table "+noiseMapParameters.receiverTableName+" does not contain a Geometry field, then the extent " +
                    "cannot be computed");
        }
        logger.info("Collect all receivers in order to localize populated cells");
        geometryField = geometryFields.get(0);
        ResultSet rs = connection.createStatement().executeQuery("SELECT " + geometryField + " FROM " + noiseMapParameters.receiverTableName);
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
     * @param connection
     * @param cellI
     * @param cellJ
     * @param progression
     * @return
     * @throws SQLException
     */
    public IComputePathsOut evaluateCell(Connection connection, int cellI, int cellJ,
                                         ProgressVisitor progression, Set<Long> skipReceivers) throws SQLException, IOException {
        Scene threadData = prepareCell(connection, cellI, cellJ, progression, skipReceivers);

        if(verbose) {
            logger.info(String.format("This computation area contains %d receivers %d sound sources and %d buildings",
                    threadData.receivers.size(), threadData.sourceGeometries.size(),
                    threadData.profileBuilder.getBuildingCount()));
        }
        IComputePathsOut computeRaysOut;
        if(computeRaysOutFactory == null) {
            computeRaysOut = new Attenuation(false, attenuationCnossosParametersDay, threadData);
        } else {
            computeRaysOut = computeRaysOutFactory.create(threadData, attenuationCnossosParametersDay,
                    attenuationCnossosParametersEvening, attenuationCnossosParametersNight);
        }

        PathFinder computeRays = new PathFinder(threadData);

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
     * Extracted from NMPB 2008-2 7.3.2
     * Soil areas POLYGON, with a dimensionless coefficient G:
     *  - Law, meadow, field of cereals G=1
     *  - Undergrowth (resinous or decidious) G=1
     *  - Compacted earth, track G=0.3
     *  - Road surface G=0
     *  - Smooth concrete G=0
     * @param soilTableName Table name of grounds properties
     */
    public void setSoilTableName(String soilTableName) {
        this.soilTableName = soilTableName;
    }

    /**
     * Extracted from NMPB 2008-2 7.3.2
     * Soil areas POLYGON, with a dimensionless coefficient G:
     *  - Law, meadow, field of cereals G=1
     *  - Undergrowth (resinous or decidious) G=1
     *  - Compacted earth, track G=0.3
     *  - Road surface G=0
     *  - Smooth concrete G=0
     * @return Table name of grounds properties
     */
    public String getSoilTableName() {
        return soilTableName;
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


    @Override
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        super.initialize(connection, progression);
        if(propagationProcessDataFactory != null) {
            propagationProcessDataFactory.initialize(connection, this);
        }
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
     * Fetches soil areas data for the specified cell envelope and adds them to the profile builder.
     * @param connection         the database connection to use for querying the soil areas data.
     * @param fetchEnvelope      the envelope representing the cell to fetch soil areas data for.
     * @param builder            the profile builder to which the soil areas data will be added.
     * @throws SQLException      if an SQL exception occurs while fetching the soil areas data.
     */
    protected void fetchCellSoilAreas(Connection connection, Envelope fetchEnvelope, ProfileBuilder builder)
            throws SQLException {
        if(!soilTableName.isEmpty()){
            DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
            double startX = Math.floor(fetchEnvelope.getMinX() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            double startY = Math.floor(fetchEnvelope.getMinY() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            String soilGeomName = getGeometryColumnNames(connection,
                    TableLocation.parse(soilTableName, dbType)).get(0);
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(soilGeomName, dbType) + ", G FROM " +
                            soilTableName + " WHERE " +
                            TableLocation.quoteIdentifier(soilGeomName, dbType) + " && ?::geometry")) {
                st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
                try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                    while (rs.next()) {
                        Geometry mainPolygon = rs.getGeometry();
                        if(mainPolygon != null) {
                            for (int idPoly = 0; idPoly < mainPolygon.getNumGeometries(); idPoly++) {
                                Geometry poly = mainPolygon.getGeometryN(idPoly);
                                if (poly instanceof Polygon) {
                                    PreparedPolygon preparedPolygon = new PreparedPolygon((Polygon) poly);
                                    // Split soil by square
                                    Envelope geoEnv = poly.getEnvelopeInternal();
                                    double startXGeo = Math.max(startX, Math.floor(geoEnv.getMinX() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength);
                                    double startYGeo = Math.max(startY, Math.floor(geoEnv.getMinY() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength);
                                    double xCursor = startXGeo;
                                    double g = rs.getDouble("G");
                                    double maxX = Math.min(fetchEnvelope.getMaxX(), geoEnv.getMaxX());
                                    double maxY = Math.min(fetchEnvelope.getMaxY(), geoEnv.getMaxY());
                                    while (xCursor < maxX) {
                                        double yCursor = startYGeo;
                                        while (yCursor < maxY) {
                                            Envelope cellEnv = new Envelope(xCursor, xCursor + groundSurfaceSplitSideLength, yCursor, yCursor + groundSurfaceSplitSideLength);
                                            Geometry envGeom = geometryFactory.toGeometry(cellEnv);
                                            if(preparedPolygon.intersects(envGeom)) {
                                                try {
                                                    Geometry inters = poly.intersection(envGeom);
                                                    if (!inters.isEmpty() && (inters instanceof Polygon || inters instanceof MultiPolygon)) {
                                                        builder.addGroundEffect(inters, g);
                                                    }
                                                } catch (TopologyException | IllegalArgumentException ex) {
                                                    // Ignore
                                                }
                                            }
                                            yCursor += groundSurfaceSplitSideLength;
                                        }
                                        xCursor += groundSurfaceSplitSideLength;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
        if(propagationProcessDataFactory != null) {
            propagationProcessDataFactory.initialize(connection, this);
        }
    }
}
