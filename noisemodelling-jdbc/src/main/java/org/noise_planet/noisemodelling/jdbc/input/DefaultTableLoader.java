/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.input;

import org.h2gis.utilities.*;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivityRecord;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.DiscreteDirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;

/**
 *  Default implementation for initializing input propagation process data for noise map computation.
 */
public class DefaultTableLoader implements NoiseMapByReceiverMaker.TableLoader {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultTableLoader.class);
    NoiseMapByReceiverMaker noiseMapByReceiverMaker;
    // Soil areas are split by the provided size in order to reduce the propagation time
    protected double groundSurfaceSplitSideLength = 200;
    public List<Integer> frequencyArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public List<Double> exactFrequencyArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE));
    public List<Double> aWeightingArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE));
    /**
     * Define attenuation settings to apply for each period
     */
    public Map<String, AttenuationParameters> cnossosParametersPerPeriod = new HashMap<>();
    public AttenuationParameters defaultParameters = new AttenuationParameters();

    public static final int DEFAULT_FETCH_SIZE = 300;
    protected int fetchSize = DEFAULT_FETCH_SIZE;

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();

    /**
     * Inserts directivity attributes for noise sources for trains into the directionAttributes map.
     */
    public void insertTrainDirectivity() {
        directionAttributes.clear();
        directionAttributes.put(0, new OmnidirectionalDirection());
        int i=1;
        for(String typeSource : RailWayCnossosParameters.sourceType) {
            directionAttributes.put(i, new RailwayCnossosDirectivitySphere(new LineSource(typeSource)));
            i++;
        }
    }

    /**
     * Initializes the NoiseMap parameters and attenuation data based on the input mode specified in the NoiseMap parameters.
     * @param connection   the database connection to be used for initialization.
     * @param noiseMapByReceiverMaker the noise map by receiver maker object associated with the computation process.
     * @throws SQLException if a database access error occurs during initialization.
     */
    @Override
    public void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException {
        this.noiseMapByReceiverMaker = noiseMapByReceiverMaker;
        SceneDatabaseInputSettings inputSettings = noiseMapByReceiverMaker.getSceneInputSettings();
        if(inputSettings.inputMode == SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_GUESS) {
            // Check fields to find appropriate expected data
            inputSettings.inputMode = SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION;
            if(!inputSettings.sourcesEmissionTableName.isEmpty()) {
                List<String> sourceFields = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesEmissionTableName());
                Set<String> normalizedSourceFields = normalizeColumnNames(sourceFields);
                if(normalizedSourceFields.contains("LV_SPD")) {
                    inputSettings.inputMode = SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW;
                } else {
                    inputSettings.inputMode = SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW;
                }
            } else {
                List<String> sourceFields = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesTableName());
                Set<String> normalizedSourceFields = normalizeColumnNames(sourceFields);
                for (EmissionTableGenerator.STANDARD_PERIOD period : EmissionTableGenerator.STANDARD_PERIOD.values()) {
                    String periodFieldName = EmissionTableGenerator.STANDARD_PERIOD_VALUE[period.ordinal()];
                    List<Integer> frequencyValues = readFrequenciesFromLwTable(
                            noiseMapByReceiverMaker.getFrequencyFieldPrepend()+
                                    periodFieldName, sourceFields);
                    if(!frequencyValues.isEmpty()) {
                        inputSettings.inputMode = SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN;
                        break;
                    } else {
                        if(normalizedSourceFields.contains("LV_SPD_" + periodFieldName)) {
                            inputSettings.inputMode = SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW_DEN;
                            break;
                        }
                    }
                }
            }
        }

        if(inputSettings.inputMode == SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW) {
            // Load expected frequencies used for computation
            // Fetch source fields
            List<String> sourceField = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesEmissionTableName());
            List<Integer> frequencyValues = readFrequenciesFromLwTable(noiseMapByReceiverMaker.getFrequencyFieldPrepend(), sourceField);
            if(frequencyValues.isEmpty()) {
                throw new SQLException("Source emission table "+ noiseMapByReceiverMaker.getSourcesTableName()+" does not contains any frequency bands");
            }
            frequencyArray = new ArrayList<>(frequencyValues);
            exactFrequencyArray = new ArrayList<>();
            aWeightingArray = new ArrayList<>();
            ProfileBuilder.initializeFrequencyArrayFromReference(frequencyArray, exactFrequencyArray, aWeightingArray);
        } else if (inputSettings.inputMode == SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN) {
            List<String> sourceFields = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesTableName());
            Set<Integer> frequencySet = new HashSet<>();
            for (EmissionTableGenerator.STANDARD_PERIOD period : EmissionTableGenerator.STANDARD_PERIOD.values()) {
                String periodFieldName = EmissionTableGenerator.STANDARD_PERIOD_VALUE[period.ordinal()];
                frequencySet.addAll(readFrequenciesFromLwTable(noiseMapByReceiverMaker.getFrequencyFieldPrepend()+periodFieldName, sourceFields));
            }
            frequencyArray = new ArrayList<>(frequencySet);
            exactFrequencyArray = new ArrayList<>();
            aWeightingArray = new ArrayList<>();
            ProfileBuilder.initializeFrequencyArrayFromReference(frequencyArray, exactFrequencyArray, aWeightingArray);
        }
        defaultParameters.setFrequencies(frequencyArray);
        // Load atmospheric data from database
        if(!inputSettings.periodAtmosphericSettingsTableName.isEmpty()) {
            loadAtmosphericTableSettings(connection, inputSettings.periodAtmosphericSettingsTableName);
        }
        // apply expected frequency to each atmospheric data
        for(AttenuationParameters parameters : cnossosParametersPerPeriod.values()) {
            parameters.setFrequencies(frequencyArray);
        }
        // Load source directivity
        if(inputSettings.useTrainDirectivity) {
            insertTrainDirectivity();
        } else if (!inputSettings.directivityTableName.isEmpty()) {
            directionAttributes = fetchDirectivity(connection, inputSettings.directivityTableName, 1, noiseMapByReceiverMaker.getFrequencyFieldPrepend());
            if(noiseMapByReceiverMaker.isVerbose()) {
                LOGGER.info("Loaded {} directivities from the database", directionAttributes.size());
            }
        }
    }

    private void loadAtmosphericTableSettings(Connection connection, String atmosphericSettingsTableName) throws SQLException {
        String query = "SELECT * FROM " + atmosphericSettingsTableName;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                // Placeholder for processing the results
                AttenuationParameters.readFromDatabase(resultSet, cnossosParametersPerPeriod);
            }
        }
    }

    /**
     * Retrieves the frequency array used within the class.
     *
     * @return a list of integers representing the frequency values in the array.
     */
    public List<Integer> getFrequencyArray() {
        return frequencyArray;
    }

    /**
     * Retrieves the exact frequency array used within the class.
     *
     * @return a list of doubles representing the exact frequency values in the array.
     */
    public List<Double> getExactFrequencyArray() {
        return exactFrequencyArray;
    }

    /**
     * Retrieves the A-weighting correction array used within the class.
     * A-weighting is applied to account for the varying sensitivity of
     * human hearing to different frequencies, commonly used in acoustic measurements.
     *
     * @return a list of doubles representing the A-weighting correction values.
     */
    public List<Double> getaWeightingArray() {
        return aWeightingArray;
    }

    /**
     * Retrieves the parameters defined for different time periods.
     *
     * @return a map where the keys represent the time periods (e.g., "D", "E", "N") as strings,
     *         and the values are instances of {@link AttenuationParameters} representing the corresponding parameters.
     */
    public Map<String, AttenuationParameters> getCnossosParametersPerPeriod() {
        return cnossosParametersPerPeriod;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public Map<Integer, DirectivitySphere> getDirectionAttributes() {
        return directionAttributes;
    }

    private static List<Integer> readFrequenciesFromLwTable(String frequencyPrepend, List<String> sourceField) throws SQLException {
        List<Integer> frequencyValues = new ArrayList<>();
        String normalizedPrefix = frequencyPrepend == null ? "" : frequencyPrepend.toUpperCase(Locale.ROOT);
        for (String fieldName : sourceField) {
            if (fieldName.toUpperCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                try {
                    int freq = Integer.parseInt(fieldName.substring(normalizedPrefix.length()));
                    int index = Arrays.binarySearch(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE, freq);
                    if (index >= 0) {
                        frequencyValues.add(freq);
                    }
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }
        return frequencyValues;
    }

    private static Set<String> normalizeColumnNames(List<String> columnNames) {
        Set<String> normalized = new HashSet<>();
        for (String columnName : columnNames) {
            if (columnName != null) {
                normalized.add(columnName.toUpperCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    @Override
    public SceneWithEmission create(Connection connection, CellIndex cellIndex,
                                    Set<Long> skipReceivers) throws SQLException {
        DBTypes dbType = DBUtils.getDBType(org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.resolveConnection(connection));
        GeometryFactory geometryFactory = noiseMapByReceiverMaker.getGeometryFactory();

        Envelope cellEnvelope = noiseMapByReceiverMaker.getCellEnv(cellIndex);
        Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
        double maximumPropagationDistance = noiseMapByReceiverMaker.getMaximumPropagationDistance();
        double maximumReflectionDistance = noiseMapByReceiverMaker.getMaximumReflectionDistance();

        // We have to fetch input data at least at this distance from the receivers in order to have continuity
        // between subdomains
        expandedCellEnvelop.expandBy(maximumPropagationDistance + 2 * maximumReflectionDistance);

        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.setFrequencyArray(frequencyArray);
        SceneWithEmission scene = new SceneWithEmission(profileBuilder, noiseMapByReceiverMaker.getSceneInputSettings());
        scene.setDirectionAttributes(directionAttributes);
        scene.cnossosParametersPerPeriod = cnossosParametersPerPeriod;
        scene.defaultCnossosParameters = defaultParameters;
        scene.periodSet.addAll(cnossosParametersPerPeriod.keySet());


        // //////////////////////////////////////////////////////
        // feed freeFieldFinder for fast intersection query
        // optimization
        // Fetch buildings in extendedEnvelope
        fetchCellBuildings(connection, noiseMapByReceiverMaker.getBuildingTableParameters(), expandedCellEnvelop,
                scene.profileBuilder, geometryFactory);

        //if we have topographic points data
        fetchCellDem(connection, expandedCellEnvelop, scene.profileBuilder);

        // Fetch soil areas
        fetchCellSoilAreas(connection, expandedCellEnvelop, scene.profileBuilder);

        scene.profileBuilder.finishFeeding();

        scene.reflexionOrder = noiseMapByReceiverMaker.getSoundReflectionOrder();
        scene.setBodyBarrier(noiseMapByReceiverMaker.isBodyBarrier());
        scene.maxRefDist = maximumReflectionDistance;
        scene.maxSrcDist = maximumPropagationDistance;
        scene.setComputeVerticalDiffraction(noiseMapByReceiverMaker.isComputeVerticalDiffraction());
        scene.setComputeHorizontalDiffraction(noiseMapByReceiverMaker.isComputeHorizontalDiffraction());

        // Fetch all source located in expandedCellEnvelop
        fetchCellSource(connection, expandedCellEnvelop, scene, true);

        // Fetch receivers
        String receiverTableName = noiseMapByReceiverMaker.getReceiverTableName();
        String receiverGeomName = GeometryTableUtilities.getGeometryColumnNames(connection,
                TableLocation.parse(receiverTableName)).get(0);
        
        // Get PK column name using cross-database helper
        Tuple<String, Integer> pkInfo = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getIntegerPrimaryKey(
                connection, TableLocation.parse(receiverTableName, dbType));
        if (pkInfo == null || pkInfo.first() == null || pkInfo.first().isEmpty()) {
            throw new SQLException(String.format("Table %s missing primary key for receiver identification", receiverTableName));
        }
        String pkColumnName = pkInfo.first();
        
        String pkSelect = ", " + TableLocation.quoteIdentifier(pkColumnName, dbType);
        String receiverTableSql = TableLocation.parse(receiverTableName, dbType).toString();
        String receiverPredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                connection, TableLocation.quoteIdentifier(receiverGeomName, dbType));
        Geometry receiverEnvelope = geometryFactory.toGeometry(cellEnvelope);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(receiverGeomName, dbType ) + pkSelect + " FROM " +
                        receiverTableSql + " WHERE " + receiverPredicate)) {
            org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, receiverEnvelope, dbType);
            try (SpatialResultSet rs = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.unwrapSpatialResultSet(st.executeQuery(), dbType)) {
                while (rs.next()) {
                    long receiverPk = rs.getLong(2);
                    if(skipReceivers.contains(receiverPk)) {
                        continue;
                    } else {
                        skipReceivers.add(receiverPk);
                    }
                    // With PostgisConnectionWrapper, PostgreSQL returns JTS Geometry directly
                    Geometry pt = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getGeometry(rs, receiverGeomName);
                    if(pt != null && !pt.isEmpty()) {
                        if(pt.getCoordinate().getZ() == Coordinate.NULL_ORDINATE) {
                            throw new IllegalArgumentException("The table " + receiverTableName +
                                    " contain at least one receiver without Z ordinate." +
                                    " You must specify X,Y,Z for each receiver");
                        }
                        scene.addReceiver(receiverPk, pt.getCoordinate(), rs);
                    }
                }
            }
        }

        return scene;
    }

    /**
     * The table shall contain the following fields :
     * DIR_ID : identifier of the directivity sphere (INTEGER)
     * THETA : Horizontal angle in degree. 0° front and 90° right (0-360) (FLOAT)
     * PHI : Vertical angle in degree. 0° front and 90° top -90° bottom (-90 - 90) (FLOAT)
     * HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000 : attenuation levels in dB for each octave or third octave (FLOAT)
     * @param connection Connection
     * @param tableName Table name
     * @param defaultInterpolation Interpolation if applicable
     * @param frequencyFieldPrepend Frequency field name ex. HZ for HZ1000
     * @return Map of directivity spheres
     */
    public static Map<Integer, DirectivitySphere> fetchDirectivity(Connection connection, String tableName, int defaultInterpolation, String frequencyFieldPrepend) throws SQLException {
        Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();
        List<String> fields = JDBCUtilities.getColumnNames(connection, tableName);
        // fetch provided frequencies
        List<String> frequenciesFields = new ArrayList<>();
        for(String field : fields) {
            if(field.toUpperCase(Locale.ROOT).startsWith(frequencyFieldPrepend)) {
                try {
                    double frequency = Double.parseDouble(field.substring(frequencyFieldPrepend.length()));
                    if (frequency > 0) {
                        frequenciesFields.add(field);
                    }
                } catch (NumberFormatException ex) {
                    //ignore column
                }
            }
        }
        if(frequenciesFields.isEmpty()) {
            return directionAttributes;
        }
        double[] frequencies = new double[frequenciesFields.size()];
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencies[idFrequency] = Double.parseDouble(frequenciesFields.get(idFrequency).substring(2));
        }
        StringBuilder sb = new StringBuilder("SELECT DIR_ID, THETA, PHI");
        for(String frequency : frequenciesFields) {
            sb.append(", ");
            sb.append(frequency);
        }
        sb.append(" FROM ");
        sb.append(tableName);
        sb.append(" ORDER BY DIR_ID");
        try(Statement st = connection.createStatement()) {
            try(ResultSet rs = st.executeQuery(sb.toString())) {
                List<DirectivityRecord> rows = new ArrayList<>();
                int lastDirId = Integer.MIN_VALUE;
                while (rs.next()) {
                    int dirId = rs.getInt(1);
                    if(lastDirId != dirId && !rows.isEmpty()) {
                        DiscreteDirectivitySphere attributes = new DiscreteDirectivitySphere(lastDirId, frequencies);
                        attributes.setInterpolationMethod(defaultInterpolation);
                        attributes.addDirectivityRecords(rows);
                        directionAttributes.put(lastDirId, attributes);
                        rows.clear();
                    }
                    lastDirId = dirId;
                    double theta = Math.toRadians(rs.getDouble(2));
                    double phi = Math.toRadians(rs.getDouble(3));
                    double[] att = new double[frequencies.length];
                    for(int freqColumn = 0; freqColumn < frequencies.length; freqColumn++) {
                        att[freqColumn] = rs.getDouble(freqColumn + 4);
                    }
                    DirectivityRecord r = new DirectivityRecord(theta, phi, att);
                    rows.add(r);
                }
                if(!rows.isEmpty()) {
                    DiscreteDirectivitySphere attributes = new DiscreteDirectivitySphere(lastDirId, frequencies);
                    attributes.setInterpolationMethod(defaultInterpolation);
                    attributes.addDirectivityRecords(rows);
                    directionAttributes.put(lastDirId, attributes);
                }
            }
        }
        return directionAttributes;
    }


    /**
     * Fetches buildings data for the specified cell envelope and adds them to the profile builder.
     * @param connection     the database connection to use for querying the buildings data.
     * @param buildingTableParameters Database settings for the building table
     * @param fetchEnvelope  the envelope representing the cell to fetch buildings data for.
     * @param builder        the profile builder to which the buildings data will be added.
     * @param geometryFactory geometry factory instance with SRID set.
     * @throws SQLException  if an SQL exception occurs while fetching the buildings data.
     */
    public static void fetchCellBuildings(Connection connection, BuildingTableParameters buildingTableParameters,
                                          Envelope fetchEnvelope, ProfileBuilder builder,
                                          GeometryFactory geometryFactory) throws SQLException {
        List<Building> buildings = new LinkedList<>();
        List<Wall> walls = new LinkedList<>();
        fetchCellBuildings(connection,buildingTableParameters, fetchEnvelope, buildings, walls, geometryFactory);
        for(Building building : buildings) {
            builder.addBuilding(building);
        }
        for (Wall wall : walls) {
            builder.addWall(wall);
        }
    }

    /**
     * Fetches building data for the specified cell envelope and adds them to the provided list of buildings.
     * @param connection      the database connection to use for querying the building data.
     * @param buildingTableParameters Database settings for the building table
     * @param fetchEnvelope   the envelope representing the cell to fetch building data for.
     * @param buildings       the list to which the fetched buildings will be added.
     * @param walls Wall list to feed
     * @param geometryFactory geometry factory instance with SRID set.
     * @throws SQLException   if an SQL exception occurs while fetching the building data.
     */
    public static void fetchCellBuildings(Connection connection,
                                          BuildingTableParameters buildingTableParameters,
                                          Envelope fetchEnvelope,
                                          List<Building> buildings,
                                          List<Wall> walls,
                                          GeometryFactory geometryFactory) throws SQLException {
        Geometry envGeo = geometryFactory.toGeometry(fetchEnvelope);
        String additionalQuery = "";
        DBTypes dbType = GeometrySqlHelper.resolveDbType(connection);

        // Resolve column names once taking into account Postgres lowercase normalisation
        String heightFieldName = buildingTableParameters.heightField;
        String alphaFieldName = buildingTableParameters.alphaFieldName;
        if (GeometrySqlHelper.isPostgreSQL(dbType)) {
            if (heightFieldName != null && heightFieldName.equals(heightFieldName.toUpperCase(Locale.ROOT))
                    && heightFieldName.matches("[A-Z_]+")) {
                heightFieldName = heightFieldName.toLowerCase(Locale.ROOT);
            }
            if (alphaFieldName != null && alphaFieldName.equals(alphaFieldName.toUpperCase(Locale.ROOT))
                    && alphaFieldName.matches("[A-Z_]+")) {
                alphaFieldName = alphaFieldName.toLowerCase(Locale.ROOT);
            }
        }

        boolean fetchAlpha = false;
        if (alphaFieldName != null && !alphaFieldName.isEmpty()) {
            fetchAlpha = JDBCUtilities.hasField(connection, buildingTableParameters.buildingsTableName,
                    alphaFieldName);
            // On PostgreSQL try the original identifier as a fallback when metadata still uses uppercase
            if (!fetchAlpha && GeometrySqlHelper.isPostgreSQL(dbType)
                    && !alphaFieldName.equals(buildingTableParameters.alphaFieldName)) {
                fetchAlpha = JDBCUtilities.hasField(connection, buildingTableParameters.buildingsTableName,
                        buildingTableParameters.alphaFieldName);
                if (fetchAlpha) {
                    alphaFieldName = buildingTableParameters.alphaFieldName;
                }
            }
        }
        
        if(!buildingTableParameters.heightField.isEmpty()) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(heightFieldName, dbType);
        }
        if(fetchAlpha) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(alphaFieldName, dbType);
        }
        String pkBuilding = "";
        Tuple<String, Integer> pkInfo = GeometrySqlHelper.getIntegerPrimaryKey(connection,
                TableLocation.parse(buildingTableParameters.buildingsTableName, dbType));
        int indexPk = 0;
        if(pkInfo != null) {
            pkBuilding = pkInfo.first();
            indexPk = pkInfo.second();
        }
        if(indexPk > 0) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(pkBuilding, dbType);
        }
        String buildingGeomName = getGeometryColumnNames(connection,
                TableLocation.parse(buildingTableParameters.buildingsTableName, dbType)).get(0);
        String buildingTableSql = TableLocation.parse(buildingTableParameters.buildingsTableName, dbType).toString();
        String buildingPredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                connection, TableLocation.quoteIdentifier(buildingGeomName, dbType));
    Geometry fetchGeometry = geometryFactory.toGeometry(fetchEnvelope);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(buildingGeomName, dbType) + additionalQuery + " FROM " +
                        buildingTableSql + " WHERE " + buildingPredicate)) {
            org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, fetchGeometry, dbType);
            try (ResultSet rs = st.executeQuery()) {
                int columnIndex = 0;
                if(!pkBuilding.isEmpty()) {
                    columnIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), pkBuilding);
                }
                double oldAlpha = buildingTableParameters.defaultWallAbsorption;
                while (rs.next()) {
                    //if we don't have height of building
                    Geometry building = GeometrySqlHelper.getGeometry(rs, buildingGeomName);
                    if(building != null) {
                        Geometry intersectedGeometry = null;
                        try {
                            intersectedGeometry = building.intersection(envGeo);
                        } catch (TopologyException ex) {
                            WKTWriter wktWriter = new WKTWriter(3);
                            LOGGER.error(String.format("Error with input buildings geometry\n%s\n%s",wktWriter.write(building),wktWriter.write(envGeo)), ex);
                        }
                        if(intersectedGeometry instanceof Polygon || intersectedGeometry instanceof MultiPolygon || intersectedGeometry instanceof LineString) {
                            double buildingHeightValue = buildingTableParameters.heightField.isEmpty() ?
                                    Double.MAX_VALUE : rs.getDouble(heightFieldName);
                            if(fetchAlpha) {
                                oldAlpha = rs.getDouble(alphaFieldName);
                            }

                            long pk = -1;
                            if(columnIndex != 0) {
                                pk = rs.getLong(columnIndex);
                            }
                            for(int i=0; i<intersectedGeometry.getNumGeometries(); i++) {
                                Geometry geometry = intersectedGeometry.getGeometryN(i);
                                if(geometry instanceof Polygon && !geometry.isEmpty()) {
                                    Building poly = new Building((Polygon) geometry,
                                            buildingHeightValue,
                                            oldAlpha, pk, buildingTableParameters.zBuildings);
                                    buildings.add(poly);
                                } else if (geometry instanceof LineString) {
                                    // decompose linestring into segments
                                    LineString lineString = (LineString) geometry;
                                    Coordinate[] coordinates = lineString.getCoordinates();
                                    for(int vertex=0; vertex < coordinates.length - 1; vertex++) {
                                        Wall wall = new Wall(new LineSegment(coordinates[vertex], coordinates[vertex+1]),
                                                -1, ProfileBuilder.IntersectionType.WALL);
                                        wall.setG(oldAlpha);
                                        wall.setPrimaryKey(pk);
                                        wall.setHeight(buildingHeightValue);
                                        walls.add(wall);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static class BuildingTableParameters {
        public String buildingsTableName;
        public String heightField = "HEIGHT";
        public String alphaFieldName = "G";
        public double defaultWallAbsorption = 100000;
        /** if true take into account z value on Buildings Polygons
         * In this case, z represent the altitude (from the sea to the top of the wall) */
        public boolean zBuildings = false;

        public BuildingTableParameters() {
        }


        /**
         * @return Get building absorption coefficient column name
         */

        public String getAlphaFieldName() {
            return alphaFieldName;
        }

        /**
         * @param alphaFieldName Set building absorption coefficient column name (default is ALPHA)
         */

        public void setAlphaFieldName(String alphaFieldName) {
            this.alphaFieldName = alphaFieldName;
        }


        /**
         * @return {@link #buildingsTableName} table field name for buildings height above the ground.
         */
        public String getHeightField() {
            return heightField;
        }

        /**
         * @param heightField {@link #buildingsTableName} table field name for buildings height above the ground.
         */
        public void setHeightField(String heightField) {
            this.heightField = heightField;
        }

    }

    /**
     * Fetches digital elevation model (DEM) data for the specified cell envelope and adds it to the mesh.
     * @param connection the database connection to use for querying the DEM data.
     * @param fetchEnvelope  the envelope representing the cell to fetch DEM data for.
     * @param profileBuilder the profile builder mesh to which the DEM data will be added.
     * @throws SQLException if an SQL exception occurs while fetching the DEM data.
     */
    protected void fetchCellDem(Connection connection, Envelope fetchEnvelope, ProfileBuilder profileBuilder) throws SQLException {
        String demTable = noiseMapByReceiverMaker.getDemTable();
        if(!demTable.isEmpty()) {
            GeometryFactory geometryFactory = noiseMapByReceiverMaker.getGeometryFactory();
            DBTypes dbType = DBUtils.getDBType(org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.resolveConnection(connection));
            List<String> geomFields = getGeometryColumnNames(connection,
                    TableLocation.parse(demTable, dbType));
            if(geomFields.isEmpty()) {
                throw new SQLException("Digital elevation model table \""+ demTable +"\" must exist and contain a POINT field");
            }
            String topoGeomName = geomFields.get(0);
            double sumZ = 0;
            int topoCount = 0;
        String demTableSql = TableLocation.parse(demTable, dbType).toString();
        String topoPredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                connection, TableLocation.quoteIdentifier(topoGeomName, dbType));
        Geometry demEnvelope = geometryFactory.toGeometry(fetchEnvelope);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(topoGeomName, dbType) + " FROM " +
                        demTableSql + " WHERE " + topoPredicate)) {
            org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, demEnvelope, dbType);
                try (SpatialResultSet rs = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.unwrapSpatialResultSet(st.executeQuery(), dbType)) {
                    while (rs.next()) {
                        Geometry pt = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getGeometry(rs, topoGeomName);
                        if(pt != null) {
                            Coordinate ptCoordinate = pt.getCoordinate();
                            profileBuilder.addTopographicPoint(ptCoordinate);
                            if(!Double.isNaN(ptCoordinate.z)) {
                                sumZ+=ptCoordinate.z;
                                topoCount+=1;
                            }
                        }
                    }
                }
                double averageZ = 0;
                if(topoCount > 0) {
                    averageZ = sumZ / topoCount;
                }
                // add corners of envelope to guaranty topography continuity
                Envelope extentedEnvelope = new Envelope(fetchEnvelope);
                extentedEnvelope.expandBy(fetchEnvelope.getDiameter());
                Coordinate[] coordinates = geometryFactory.toGeometry(extentedEnvelope).getCoordinates();
                for (int i = 0; i < coordinates.length - 1; i++) {
                    Coordinate coordinate = coordinates[i];
                    profileBuilder.addTopographicPoint(new Coordinate(coordinate.x, coordinate.y, averageZ));
                }
            }
        }
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
        String soilTableName = noiseMapByReceiverMaker.getSoilTableName();
        if(!soilTableName.isEmpty()){
            GeometryFactory geometryFactory = noiseMapByReceiverMaker.getGeometryFactory();
            DBTypes dbType = DBUtils.getDBType(org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.resolveConnection(connection));
            double startX = Math.floor(fetchEnvelope.getMinX() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            double startY = Math.floor(fetchEnvelope.getMinY() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            String soilGeomName = getGeometryColumnNames(connection,
                    TableLocation.parse(soilTableName, dbType)).get(0);
        String soilTableSql = TableLocation.parse(soilTableName, dbType).toString();
        String soilPredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                connection, TableLocation.quoteIdentifier(soilGeomName, dbType));
        Geometry soilEnvelope = geometryFactory.toGeometry(fetchEnvelope);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(soilGeomName, dbType) + ", G FROM " +
                        soilTableSql + " WHERE " + soilPredicate)) {
            org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, soilEnvelope, dbType);
                try (SpatialResultSet rs = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.unwrapSpatialResultSet(st.executeQuery(), dbType)) {
                    while (rs.next()) {
                        Geometry mainPolygon = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getGeometry(rs, soilGeomName);
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
     * Fetch source geometries and power
     * @param connection Active connection
     * @param fetchEnvelope Fetch envelope
     * @param scene (Out) Propagation process input data
     * @throws SQLException
     */
    public void fetchCellSource(Connection connection, Envelope fetchEnvelope, SceneWithEmission scene, boolean doIntersection)
            throws SQLException {
        String sourcesTableName = noiseMapByReceiverMaker.getSourcesTableName();
        GeometryFactory geometryFactory = noiseMapByReceiverMaker.getGeometryFactory();
        DBTypes dbType = DBUtils.getDBType(org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.resolveConnection(connection));
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName, dbType);
        List<String> geomFields = getGeometryColumnNames(connection, sourceTableIdentifier);
        if (geomFields.isEmpty()) {
            throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier));
        }
        String sourceGeomName = geomFields.get(0);
        Geometry domainConstraint = geometryFactory.toGeometry(fetchEnvelope);
        Tuple<String, Integer> primaryKey = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getIntegerPrimaryKey(
                GeometrySqlHelper.resolveConnection(connection), TableLocation.parse(sourcesTableName, dbType));
        if (primaryKey == null) {
            throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier));
        }
        int pkIndex = primaryKey.second();
        Geometry envelopeGeometry = geometryFactory.toGeometry(fetchEnvelope);
        String sourcePredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                connection, TableLocation.quoteIdentifier(sourceGeomName, dbType));
        try (PreparedStatement st = connection.prepareStatement("SELECT * FROM " + sourceTableIdentifier.toString() + " WHERE "
                + sourcePredicate)) {
            org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, envelopeGeometry, dbType);
            st.setFetchSize(fetchSize);
            boolean autoCommit = connection.getAutoCommit();
            if (autoCommit) {
                connection.setAutoCommit(false);
            }
            st.setFetchDirection(ResultSet.FETCH_FORWARD);
            try (SpatialResultSet rs = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.unwrapSpatialResultSet(st.executeQuery(), dbType)) {
                while (rs.next()) {
                    Geometry geo = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.getGeometry(rs, sourceGeomName);
                    if (geo != null) {
                        if (doIntersection) {
                            geo = domainConstraint.intersection(geo);
                        }
                        if (!geo.isEmpty()) {
                            Coordinate[] coordinates = geo.getCoordinates();
                            for (Coordinate coordinate : coordinates) {
                                // check z value
                                if (coordinate.getZ() == Coordinate.NULL_ORDINATE) {
                                    throw new IllegalArgumentException("The table " + sourcesTableName +
                                            " contain at least one source without Z ordinate." +
                                            " You must specify X,Y,Z for each source");
                                }
                            }
                            scene.addSource(rs.getLong(pkIndex), geo, rs);
                        }
                    }
                }
            } finally {
                if (autoCommit) {
                    connection.setAutoCommit(true);
                }
            }
        }
        // Fetch emission table data for the sources in this area
        String emissionTableName = scene.sceneDatabaseInputSettings.sourcesEmissionTableName;
        if (!emissionTableName.isEmpty()) {
            String emissionPredicate = org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.buildEnvelopePredicate(
                    connection, "S." + TableLocation.quoteIdentifier(sourceGeomName, dbType));
            try (PreparedStatement st = connection.prepareStatement("SELECT E.* FROM " + sourceTableIdentifier.toString() +
                    " S INNER JOIN " + TableLocation.parse(emissionTableName, dbType) + " E ON S." + primaryKey.first() + " = E." +
                    scene.sceneDatabaseInputSettings.sourceEmissionPrimaryKeyField + " WHERE " + emissionPredicate)) {
                org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper.setGeometryParameter(st, 1, envelopeGeometry, dbType);
                st.setFetchSize(fetchSize);
                boolean autoCommit = connection.getAutoCommit();
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                st.setFetchDirection(ResultSet.FETCH_FORWARD);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        scene.addSourceEmission(rs.getLong(scene.sceneDatabaseInputSettings.sourceEmissionPrimaryKeyField), rs);
                    }
                } finally {
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }

        }
    }
}
