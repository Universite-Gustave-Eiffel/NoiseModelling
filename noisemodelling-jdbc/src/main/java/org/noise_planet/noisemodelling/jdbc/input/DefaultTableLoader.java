package org.noise_planet.noisemodelling.jdbc.input;

import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivityRecord;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.DiscreteDirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.jdbc.NoiseEmissionMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.WallAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;

/**
 *  Default implementation for initializing input propagation process data for noise map computation.
 */
public class DefaultTableLoader implements NoiseMapByReceiverMaker.PropagationProcessDataFactory {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultTableLoader.class);
    int srid = 0;
    NoiseMapDatabaseParameters noiseMapDatabaseParameters = new NoiseMapDatabaseParameters();

    public List<Integer> frequencyArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public List<Double> exactFrequencyArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE));
    public List<Double> aWeightingArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE));


    public enum INPUT_MODE {
        /** Test */
        INPUT_MODE_TRAFFIC_FLOW,
        INPUT_MODE_LW,
        INPUT_MODE_ATTENUATION }

    /**
     * When fetching tables data, expect the columns according to this mode
     */
    public INPUT_MODE inputMode = INPUT_MODE.INPUT_MODE_ATTENUATION;

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
     * @throws SQLException
     */

    @Override
    public void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException {
        if(JDBCUtilities.tableExists(connection, noiseMapByReceiverMaker.getSourcesTableName())) {
            this.srid = GeometryTableUtilities.getSRID(connection, noiseMapByReceiverMaker.getSourcesTableName());
        }
        if(inputMode == INPUT_MODE.INPUT_MODE_LW) {
            // Load expected frequencies used for computation
            // Fetch source fields
            List<String> sourceField = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesEmissionTableName());
            List<Integer> frequencyValues = readFrequenciesFromLwTable(noiseMapByReceiverMaker, sourceField);
            frequencyArray = new ArrayList<>(frequencyValues);
            exactFrequencyArray = new ArrayList<>();
            aWeightingArray = new ArrayList<>();
            ProfileBuilder.initializeFrequencyArrayFromReference(frequencyValues, exactFrequencyArray, aWeightingArray);
        }
    }

    private List<Integer> readFrequenciesFromLwTable(NoiseMapByReceiverMaker noiseMapByReceiverMaker, List<String> sourceField) throws SQLException {
        List<Integer> frequencyValues = new ArrayList<>();
        String freqField = noiseMapDatabaseParameters.lwFrequencyPrepend;
        for (String fieldName : sourceField) {
            if (fieldName.toUpperCase(Locale.ROOT).startsWith(freqField)) {
                try {
                    int freq = Integer.parseInt(fieldName.substring(freqField.length()));
                    int index = Arrays.binarySearch(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE, freq);
                    if (index >= 0) {
                        frequencyValues.add(freq);
                    }
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }
        if(frequencyValues.isEmpty()) {
            throw new SQLException("Source emission table "+ noiseMapByReceiverMaker.getSourcesTableName()+" does not contains any frequency bands");
        }
        return frequencyValues;
    }

    @Override
    public SceneWithEmission create(Connection connection, CellIndex cellIndex, Envelope expandedCellEnvelop) {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.setFrequencyArray(frequencyArray);
        SceneWithEmission scene = new SceneWithEmission(profileBuilder);
        scene.setDirectionAttributes(directionAttributes);
        return scene;
    }

    /**
     * The table shall contain the following fields :
     * DIR_ID : identifier of the directivity sphere (INTEGER)
     * THETA : Horizontal angle in degree. 0° front and 90° right (0-360) (FLOAT)
     * PHI : Vertical angle in degree. 0° front and 90° top -90° bottom (-90 - 90) (FLOAT)
     * LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000 : attenuation levels in dB for each octave or third octave (FLOAT)
     * @param connection
     * @param tableName
     * @param defaultInterpolation
     * @return
     */
    public static Map<Integer, DiscreteDirectivitySphere> fetchDirectivity(Connection connection, String tableName, int defaultInterpolation) throws SQLException {
        Map<Integer, DiscreteDirectivitySphere> directionAttributes = new HashMap<>();
        List<String> fields = JDBCUtilities.getColumnNames(connection, tableName);
        // fetch provided frequencies
        List<String> frequenciesFields = new ArrayList<>();
        for(String field : fields) {
            if(field.toUpperCase(Locale.ROOT).startsWith("LW")) {
                try {
                    double frequency = Double.parseDouble(field.substring(2));
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
        boolean fetchAlpha = JDBCUtilities.hasField(connection, buildingTableParameters.buildingsTableName,
                buildingTableParameters.alphaFieldName);
        String additionalQuery = "";
        DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
        if(!buildingTableParameters.heightField.isEmpty()) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(buildingTableParameters.heightField, dbType);
        }
        if(fetchAlpha) {
            additionalQuery += ", " + buildingTableParameters.alphaFieldName;
        }
        String pkBuilding = "";
        final int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection.unwrap(Connection.class),
                new TableLocation(buildingTableParameters.buildingsTableName, dbType));
        if(indexPk > 0) {
            pkBuilding = JDBCUtilities.getColumnName(connection, buildingTableParameters.buildingsTableName, indexPk);
            additionalQuery += ", " + pkBuilding;
        }
        String buildingGeomName = getGeometryColumnNames(connection,
                TableLocation.parse(buildingTableParameters.buildingsTableName, dbType)).get(0);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(buildingGeomName) + additionalQuery + " FROM " +
                        buildingTableParameters.buildingsTableName + " WHERE " +
                        TableLocation.quoteIdentifier(buildingGeomName, dbType) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                int columnIndex = 0;
                if(!pkBuilding.isEmpty()) {
                    columnIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), pkBuilding);
                }
                double oldAlpha = buildingTableParameters.defaultWallAbsorption;
                while (rs.next()) {
                    //if we don't have height of building
                    Geometry building = rs.getGeometry();
                    if(building != null) {
                        Geometry intersectedGeometry = null;
                        try {
                            intersectedGeometry = building.intersection(envGeo);
                        } catch (TopologyException ex) {
                            WKTWriter wktWriter = new WKTWriter(3);
                            LOGGER.error(String.format("Error with input buildings geometry\n%s\n%s",wktWriter.write(building),wktWriter.write(envGeo)), ex);
                        }
                        if(intersectedGeometry instanceof Polygon || intersectedGeometry instanceof MultiPolygon || intersectedGeometry instanceof LineString) {
                            if(fetchAlpha) {
                                oldAlpha = rs.getDouble(buildingTableParameters.alphaFieldName);
                            }

                            long pk = -1;
                            if(columnIndex != 0) {
                                pk = rs.getLong(columnIndex);
                            }
                            for(int i=0; i<intersectedGeometry.getNumGeometries(); i++) {
                                Geometry geometry = intersectedGeometry.getGeometryN(i);
                                if(geometry instanceof Polygon && !geometry.isEmpty()) {
                                    Building poly = new Building((Polygon) geometry,
                                            buildingTableParameters.heightField.isEmpty() ?
                                                    Double.MAX_VALUE :
                                                    rs.getDouble(buildingTableParameters.heightField),
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
                                        wall.setHeight(buildingTableParameters.heightField.isEmpty() ?
                                                Double.MAX_VALUE : rs.getDouble(buildingTableParameters.heightField));
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
}
