package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;
import static org.h2gis.utilities.GeometryTableUtilities.getSRID;
import static org.noise_planet.noisemodelling.pathfinder.utils.AlphaUtils.getWallAlpha;

/**
 * Common attributes for propagation of sound sources.
 * @author Nicolas Fortin
 */
public abstract class JdbcNoiseMap {
    // When computing cell size, try to keep propagation distance away from the cell
    // inferior to this ratio (in comparison with cell width)
    PropagationProcessPathData propagationProcessPathDataDay = new PropagationProcessPathData();

    HashMap<String , PropagationProcessPathData > propagationProcessPathDataT= new HashMap<String,PropagationProcessPathData >();
    PropagationProcessPathData propagationProcessPathDataEvening = new PropagationProcessPathData();
    PropagationProcessPathData propagationProcessPathDataNight = new PropagationProcessPathData();
    Logger logger = LoggerFactory.getLogger(JdbcNoiseMap.class);
    private static final int DEFAULT_FETCH_SIZE = 300;
    protected int fetchSize = DEFAULT_FETCH_SIZE;
    protected static final double MINIMAL_BUFFER_RATIO = 0.3;
    private String alphaFieldName = "G";
    protected final String buildingsTableName;
    protected String sourcesTableName;
    protected String soilTableName = "";
    // Digital elevation model table. (Contains points or triangles)
    protected String demTable = "";
    protected String sound_lvl_field = "DB_M";
    // True if Z of sound source and receivers are relative to the ground
    protected boolean receiverHasAbsoluteZCoordinates = false;
    protected boolean sourceHasAbsoluteZCoordinates = false;
    protected double maximumPropagationDistance = 750;
    protected double maximumReflectionDistance = 100;
    protected double gs = 0;
    /** if true take into account z value on Buildings Polygons
     * In this case, z represent the altitude (from the sea to the top of the wall) */
    protected boolean zBuildings = false;
    // Soil areas are splited by the provided size in order to reduce the propagation time
    protected double groundSurfaceSplitSideLength = 200;
    protected int soundReflectionOrder = 2;

    protected boolean bodyBarrier = false; // it needs to be true if train propagation is computed (multiple reflection between the train and a screen)
    public boolean verbose = true;
    protected boolean computeHorizontalDiffraction = true;
    protected boolean computeVerticalDiffraction = true;
    /** TODO missing reference to the SIGMA value of materials */
    protected double wallAbsorption = 100000;
    /** maximum dB Error, stop calculation if the sum of further sources contributions are smaller than this value */
    public double maximumError = Double.NEGATIVE_INFINITY;

    /** stop calculation if the sum of further sources contributions are smaller than this value */
    public double noiseFloor = Double.NEGATIVE_INFINITY;

    protected String heightField = "HEIGHT";
    protected GeometryFactory geometryFactory;
    protected int parallelComputationCount = 0;
    // Initialised attributes
    protected int gridDim = 0;
    protected Envelope mainEnvelope = new Envelope();

    public JdbcNoiseMap(String buildingsTableName, String sourcesTableName) {
        this.buildingsTableName = buildingsTableName;
        this.sourcesTableName = sourcesTableName;
    }

    public PropagationProcessPathData getPropagationProcessPathData(LDENConfig.TIME_PERIOD time_period) {
        switch (time_period) {
            case DAY:
                return propagationProcessPathDataDay;
            case EVENING:
                return propagationProcessPathDataEvening;
            default:
                return propagationProcessPathDataNight;
        }
    }

    public PropagationProcessPathData getPropagationProcessPathData(String time_period) {
        return propagationProcessPathDataT.get(time_period);
    }

    public void setPropagationProcessPathData(LDENConfig.TIME_PERIOD time_period, PropagationProcessPathData propagationProcessPathData) {
        switch (time_period) {
            case DAY:
                propagationProcessPathDataDay = propagationProcessPathData;
            case EVENING:
                propagationProcessPathDataEvening = propagationProcessPathData;
            default:
                propagationProcessPathDataNight = propagationProcessPathData;
        }
    }

    public void setPropagationProcessPathData(String time_period, PropagationProcessPathData propagationProcessPathData) {
        propagationProcessPathDataT.put(time_period, propagationProcessPathData);
    }
    public PropagationProcessPathData getPropagationProcessPathDataDay() {
        return propagationProcessPathDataDay;
    }

    public void setPropagationProcessPathDataDay(PropagationProcessPathData propagationProcessPathDataDay) {
        this.propagationProcessPathDataDay = propagationProcessPathDataDay;
    }

    public PropagationProcessPathData getPropagationProcessPathDataEvening() {
        return propagationProcessPathDataEvening;
    }

    public void setPropagationProcessPathDataEvening(PropagationProcessPathData propagationProcessPathDataEvening) {
        this.propagationProcessPathDataEvening = propagationProcessPathDataEvening;
    }

    public PropagationProcessPathData getPropagationProcessPathDataNight() {
        return propagationProcessPathDataNight;
    }

    public void setPropagationProcessPathDataNight(PropagationProcessPathData propagationProcessPathDataNight) {
        this.propagationProcessPathDataNight = propagationProcessPathDataNight;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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
     * Compute the envelope corresping to parameters
     *
     * @param mainEnvelope Global envelope
     * @param cellI        I cell index
     * @param cellJ        J cell index
     * @param cellWidth    Cell width meter
     * @param cellHeight   Cell height meter
     * @return Envelope of the cell
     */
    public static Envelope getCellEnv(Envelope mainEnvelope, int cellI, int cellJ, double cellWidth,
                                      double cellHeight) {
        return new Envelope(mainEnvelope.getMinX() + cellI * cellWidth,
                mainEnvelope.getMinX() + cellI * cellWidth + cellWidth,
                mainEnvelope.getMinY() + cellHeight * cellJ,
                mainEnvelope.getMinY() + cellHeight * cellJ + cellHeight);
    }

    public double getGroundSurfaceSplitSideLength() {
        return groundSurfaceSplitSideLength;
    }

    public void setGroundSurfaceSplitSideLength(double groundSurfaceSplitSideLength) {
        this.groundSurfaceSplitSideLength = groundSurfaceSplitSideLength;
    }

    protected void fetchCellDem(Connection connection, Envelope fetchEnvelope, ProfileBuilder mesh) throws SQLException {
        if(!demTable.isEmpty()) {
            List<String> geomFields = getGeometryColumnNames(connection,
                    TableLocation.parse(demTable));
            if(geomFields.isEmpty()) {
                throw new SQLException("Digital elevation model table \""+demTable+"\" must exist and contain a POINT field");
            }
            String topoGeomName = geomFields.get(0);
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(topoGeomName) + " FROM " +
                            demTable + " WHERE " +
                            TableLocation.quoteIdentifier(topoGeomName) + " && ?::geometry")) {
                st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
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
    }

    protected void fetchCellSoilAreas(Connection connection, Envelope fetchEnvelope, ProfileBuilder builder)
            throws SQLException {
        if(!soilTableName.isEmpty()){
            double startX = Math.floor(fetchEnvelope.getMinX() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            double startY = Math.floor(fetchEnvelope.getMinY() / groundSurfaceSplitSideLength) * groundSurfaceSplitSideLength;
            String soilGeomName = getGeometryColumnNames(connection,
                    TableLocation.parse(soilTableName)).get(0);
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(soilGeomName) + ", G FROM " +
                            soilTableName + " WHERE " +
                            TableLocation.quoteIdentifier(soilGeomName) + " && ?::geometry")) {
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


    void fetchCellBuildings(Connection connection, Envelope fetchEnvelope, ProfileBuilder builder) throws SQLException {
        ArrayList<ProfileBuilder.Building> buildings = new ArrayList<>();
        fetchCellBuildings(connection, fetchEnvelope, buildings);
        for(ProfileBuilder.Building building : buildings) {
            builder.addBuilding(building);
        }
    }

    void fetchCellBuildings(Connection connection, Envelope fetchEnvelope, List<ProfileBuilder.Building> buildings) throws SQLException {
        Geometry envGeo = geometryFactory.toGeometry(fetchEnvelope);
        boolean fetchAlpha = JDBCUtilities.hasField(connection, buildingsTableName, alphaFieldName);
        String additionalQuery = "";
        if(!heightField.isEmpty()) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(heightField);
        }
        if(fetchAlpha) {
            additionalQuery += ", " + alphaFieldName;
        }
        String pkBuilding = "";
        final int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection, new TableLocation(buildingsTableName));
        if(indexPk > 0) {
            pkBuilding = JDBCUtilities.getColumnName(connection, buildingsTableName, indexPk);
            additionalQuery += ", " + pkBuilding;
        }
        String buildingGeomName = getGeometryColumnNames(connection,
                TableLocation.parse(buildingsTableName)).get(0);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(buildingGeomName) + additionalQuery + " FROM " +
                        buildingsTableName + " WHERE " +
                        TableLocation.quoteIdentifier(buildingGeomName) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                int columnIndex = 0;
                if(!pkBuilding.isEmpty()) {
                    columnIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), pkBuilding);
                }
                double oldAlpha = wallAbsorption;
                List<Double> alphaList = new ArrayList<>(propagationProcessPathDataDay.freq_lvl.size());
                for(double freq : propagationProcessPathDataDay.freq_lvl_exact) {
                    alphaList.add(getWallAlpha(oldAlpha, freq));
                }
                while (rs.next()) {
                    //if we don't have height of building
                    Geometry building = rs.getGeometry();
                    if(building != null) {
                        Geometry intersectedGeometry = null;
                        try {
                            intersectedGeometry = building.intersection(envGeo);
                        } catch (TopologyException ex) {
                            WKTWriter wktWriter = new WKTWriter(3);
                            logger.error(String.format("Error with input buildings geometry\n%s\n%s",wktWriter.write(building),wktWriter.write(envGeo)), ex);
                        }
                        if(intersectedGeometry instanceof Polygon || intersectedGeometry instanceof MultiPolygon) {
                            if(fetchAlpha && Double.compare(rs.getDouble(alphaFieldName), oldAlpha) != 0 ) {
                                // Compute building absorption value
                                alphaList.clear();
                                oldAlpha = rs.getDouble(alphaFieldName);
                                for(double freq : propagationProcessPathDataDay.freq_lvl_exact) {
                                    alphaList.add(getWallAlpha(oldAlpha, freq));
                                }
                            }

                            int pk = -1;
                            if(columnIndex != 0) {
                                pk = rs.getInt(columnIndex);
                            }
                            for(int i=0; i<intersectedGeometry.getNumGeometries(); i++) {
                                Geometry geometry = intersectedGeometry.getGeometryN(i);
                                if(geometry instanceof Polygon && !geometry.isEmpty()) {
                                    ProfileBuilder.Building poly = new ProfileBuilder.Building((Polygon) geometry,
                                            heightField.isEmpty() ? Double.MAX_VALUE : rs.getDouble(heightField),
                                            alphaList, pk, iszBuildings());
                                    buildings.add(poly);
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
     * @param propagationProcessData (Out) Propagation process input data
     * @throws SQLException
     */
    public void fetchCellSource(Connection connection,Envelope fetchEnvelope, CnossosPropagationData propagationProcessData, boolean doIntersection)
            throws SQLException, IOException {
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        List<String> geomFields = getGeometryColumnNames(connection, sourceTableIdentifier);
        if(geomFields.isEmpty()) {
            throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier));
        }
        String sourceGeomName =  geomFields.get(0);
        Geometry domainConstraint = geometryFactory.toGeometry(fetchEnvelope);
        int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, new TableLocation(sourcesTableName));
        if(pkIndex < 1) {
            throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier));
        }
        try (PreparedStatement st = connection.prepareStatement("SELECT * FROM " + sourcesTableName + " WHERE "
                + TableLocation.quoteIdentifier(sourceGeomName) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
            st.setFetchSize(fetchSize);
            boolean autoCommit = connection.getAutoCommit();
            if(autoCommit) {
                connection.setAutoCommit(false);
            }
            st.setFetchDirection(ResultSet.FETCH_FORWARD);
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    Geometry geo = rs.getGeometry();
                    if (geo != null) {
                        if(doIntersection) {
                            geo = domainConstraint.intersection(geo);
                        }
                        if(!geo.isEmpty()) {
                            Coordinate[] coordinates = geo.getCoordinates();
                            for(Coordinate coordinate : coordinates) {
                                // check z value
                                if(coordinate.getZ() == Coordinate.NULL_ORDINATE) {
                                    throw new IllegalArgumentException("The table " + sourcesTableName +
                                            " contain at least one source without Z ordinate." +
                                            " You must specify X,Y,Z for each source");
                                }
                            }
                            propagationProcessData.addSource(rs.getLong(pkIndex), geo, rs);
                        }
                    }
                }
            } finally {
                if (autoCommit) {
                    connection.setAutoCommit(true);
                }
            }
        }
    }


    /**
     * Fetch source geometries and power
     * @param connection Active connection
     * @throws SQLException
     */
    public List<Integer> fetchSourceNumber(Connection connection)
            throws SQLException, IOException {
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        Statement st  = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT DISTINCT(PK) PK FROM " + sourcesTableName +";");
        // Create a List to store the values
        List<Integer> distinctSources = new ArrayList<>();


        // Iterate through the ResultSet and add values to the List
        while (rs.next()) {
            Integer value = rs.getInt("PK"); // Change "IT" to the actual column name
            distinctSources.add(value);
        }

        List<Integer> distinctTimesteps = new ArrayList<>();

        rs = st.executeQuery("SELECT DISTINCT(IT) IT FROM " + sourcesTableName +";");

        // Iterate through the ResultSet and add values to the List
        while (rs.next()) {
            Integer value = rs.getInt("IT"); // Change "IT" to the actual column name
            distinctTimesteps.add(value);
        }

        return distinctSources;
    }

    public void createSourcesTable(Connection connection, List<Integer> frequencyValues)
            throws SQLException, IOException {
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        Statement st  = connection.createStatement();

        st.execute("CREATE TABLE LW_SOURCES_130DB AS SELECT DISTINCT PK, THE_GEOM  FROM " + sourcesTableName +";");

        for (int freq : frequencyValues) {
            st.execute("ALTER TABLE LW_SOURCES_130DB ADD HZ"+ freq +" DOUBLE NOT NULL DEFAULT 130.0;");
        }

        st.execute("ALTER TABLE LW_SOURCES_130DB ALTER COLUMN PK INTEGER NOT NULL;");
        st.execute("ALTER TABLE LW_SOURCES_130DB ADD CONSTRAINT PK PRIMARY KEY (PK);");



    }

    public List<String> fetchTimeSteps(Connection connection)
            throws SQLException, IOException {
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName);
        Statement st  = connection.createStatement();

        List<String> distinctTimesteps = new ArrayList<>();

        ResultSet rs = st.executeQuery("SELECT DISTINCT(IT) IT FROM " + sourcesTableName +";");

        // Iterate through the ResultSet and add values to the List
        while (rs.next()) {
            String value = rs.getString("IT"); // Change "IT" to the actual column name
            distinctTimesteps.add(value);
        }

        return distinctTimesteps;
    }

    /**
     * true if train propagation is computed (multiple reflection between the train and a screen)
     * @param bodyBarrier
     */
    public void setBodyBarrier(boolean bodyBarrier) {
        this.bodyBarrier = bodyBarrier;
    }

    protected double getCellWidth() {
        return mainEnvelope.getWidth() / gridDim;
    }

    protected double getCellHeight() {
        return mainEnvelope.getHeight() / gridDim;
    }

    protected static Double DbaToW(Double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    abstract protected Envelope getComputationEnvelope(Connection connection) throws SQLException;

    /**
     * Fetch scene attributes, compute best computation cell size.
     * @param connection Active connection
     * @throws java.sql.SQLException
     */
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        if(soundReflectionOrder > 0 && maximumPropagationDistance < maximumReflectionDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        int srid = 0;
        DBTypes dbTypes = DBUtils.getDBType(connection);
        if(!sourcesTableName.isEmpty()) {
            srid = getSRID(connection, TableLocation.parse(sourcesTableName, dbTypes));
        }
        if(srid == 0) {
            srid = getSRID(connection, TableLocation.parse(buildingsTableName, dbTypes));
        }
        geometryFactory = new GeometryFactory(new PrecisionModel(), srid);

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
        if(mainEnvelope.isNull()) {
            // 1 Step - Evaluation of the main bounding box (sources)
            setMainEnvelope(getComputationEnvelope(connection));
        }
    }

    /**
     * @return Side computation cell count (same on X and Y)
     */
    public int getGridDim() {
        return gridDim;
    }

    public void setGridDim(int gridDim) {
        this.gridDim = gridDim;
    }

    /**
     * This table must contain a POLYGON column, where Z values are wall bottom position relative to sea level.
     * It may also contain a height field (0-N] average building height from the ground.
     * @return Table name that contains buildings
     */
    public String getBuildingsTableName() {
        return buildingsTableName;
    }

    /**
     * This table must contain a POINT or LINESTRING column, and spectrum in dB(A).
     * Spectrum column name must be {@link #sound_lvl_field}HERTZ. Where HERTZ is a number [100-5000]
     * @return Table name that contain linear and/or punctual sound sources.     *
     */
    public String getSourcesTableName() {
        return sourcesTableName;
    }

    public void setSourcesTableName(String sourcesTableName) {
        this.sourcesTableName = sourcesTableName;
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
     * @return True if provided Z value are sea level (false for relative to ground level)
     */
    public boolean isReceiverHasAbsoluteZCoordinates() {
        return receiverHasAbsoluteZCoordinates;
    }

    /**
     *
     * @param receiverHasAbsoluteZCoordinates True if provided Z value are sea level (false for relative to ground level)
     */
    public void setReceiverHasAbsoluteZCoordinates(boolean receiverHasAbsoluteZCoordinates) {
        this.receiverHasAbsoluteZCoordinates = receiverHasAbsoluteZCoordinates;
    }

    /**
     * @return True if provided Z value are sea level (false for relative to ground level)
     */
    public boolean isSourceHasAbsoluteZCoordinates() {
        return sourceHasAbsoluteZCoordinates;
    }

    /**
     * @param sourceHasAbsoluteZCoordinates True if provided Z value are sea level (false for relative to ground level)
     */
    public void setSourceHasAbsoluteZCoordinates(boolean sourceHasAbsoluteZCoordinates) {
        this.sourceHasAbsoluteZCoordinates = sourceHasAbsoluteZCoordinates;
    }


    public boolean iszBuildings() {
        return zBuildings;
    }

    public void setzBuildings(boolean zBuildings) {
        this.zBuildings = zBuildings;
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
     * Digital Elevation model table name. Currently only a table with POINTZ column is supported.
     * DEM points too close with buildings are not fetched.
     * @return Digital Elevation model table name
     */
    public String getDemTable() {
        return demTable;
    }

    /**
     * Digital Elevation model table name. Currently only a table with POINTZ column is supported.
     * DEM points too close with buildings are not fetched.
     * @param demTable Digital Elevation model table name
     */
    public void setDemTable(String demTable) {
        this.demTable = demTable;
    }

    /**
     * Field name of the {@link #sourcesTableName}HERTZ. Where HERTZ is a number [100-5000].
     * Without the hertz value.
     * @return Hertz field prefix
     */
    public String getSound_lvl_field() {
        return sound_lvl_field;
    }

    /**
     * Field name of the {@link #sourcesTableName}HERTZ. Where HERTZ is a number [100-5000].
     * Without the hertz value.
     * @param sound_lvl_field Hertz field prefix
     */
    public void setSound_lvl_field(String sound_lvl_field) {
        this.sound_lvl_field = sound_lvl_field;
    }

    /**
     * @return Sound propagation stop at this distance, default to 750m.
     * Computation cell size if proportional with this value.
     */
    public double getMaximumPropagationDistance() {
        return maximumPropagationDistance;
    }

    /**
     * @param maximumPropagationDistance  Sound propagation stop at this distance, default to 750m.
     * Computation cell size if proportional with this value.
     */
    public void setMaximumPropagationDistance(double maximumPropagationDistance) {
        this.maximumPropagationDistance = maximumPropagationDistance;
    }

    /**
     *
     * @param gs ground factor above the sound source
     */
    public void setGs(double gs) {
        this.gs = gs;
    }

    public double getGs() {
        return this.gs;
    }

    public double getNoiseFloor() {
        return noiseFloor;
    }

    public void setNoiseFloor(double noiseFloor) {
        this.noiseFloor = noiseFloor;
    }

    /**
     * @return maximum dB Error, stop calculation if the maximum sum of further sources contributions are smaller than this value
     */
    public double getMaximumError() {
        return maximumError;
    }

    /**
     * @param maximumError maximum dB Error, stop calculation if the maximum sum of further sources contributions are smaller than this value
     */
    public void setMaximumError(double maximumError) {
        this.maximumError = maximumError;
    }

    /**
     * @return Reflection and diffraction maximum search distance, default to 400m.
     */
    public double getMaximumReflectionDistance() {
        return maximumReflectionDistance;
    }

    /**
     * @param maximumReflectionDistance Reflection and diffraction seek walls and corners up to X meters
     *                                  from the direct propagation line. Default to 100m.
     */
    public void setMaximumReflectionDistance(double maximumReflectionDistance) {
        this.maximumReflectionDistance = maximumReflectionDistance;
    }

    /**
     * @return Sound reflection order. 0 order mean 0 reflection depth.
     * 2 means propagation of rays up to 2 collision with walls.
     */
    public int getSoundReflectionOrder() {
        return soundReflectionOrder;
    }

    /**
     * @param soundReflectionOrder Sound reflection order. 0 order mean 0 reflection depth.
     * 2 means propagation of rays up to 2 collision with walls.
     */
    public void setSoundReflectionOrder(int soundReflectionOrder) {
        this.soundReflectionOrder = soundReflectionOrder;
    }

    /**
     * @return True if diffraction rays will be computed on vertical edges (around buildings)
     */
    public boolean isComputeHorizontalDiffraction() {
        return computeHorizontalDiffraction;
    }

    /**
     * @param computeHorizontalDiffraction True if diffraction rays will be computed on vertical edges (around buildings)
     */
    public void setComputeHorizontalDiffraction(boolean computeHorizontalDiffraction) {
        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
    }

    /**
     * @return Global default wall absorption on sound reflection.
     */
    public double getWallAbsorption() {
        return wallAbsorption;
    }

    /**
     * @param wallAbsorption Set default global wall absorption on sound reflection.
     */
    public void setWallAbsorption(double wallAbsorption) {
        this.wallAbsorption = wallAbsorption;
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

    /**
     * @return True if multi-threading is activated.
     */
    public boolean isDoMultiThreading() {
        return parallelComputationCount != 1;
    }

    /**
     * @return Parallel computations, 0 for using all available cores (1 single core)
     */
    public int getParallelComputationCount() {
        return parallelComputationCount;
    }

    /**
     * @param parallelComputationCount Parallel computations, 0 for using all available cores  (1 single core)
     */
    public void setParallelComputationCount(int parallelComputationCount) {
        this.parallelComputationCount = parallelComputationCount;
    }

    /**
     * @return The envelope of computation area.
     */
    public Envelope getMainEnvelope() {
        return mainEnvelope;
    }

    /**
     * Set computation area. Update the property subdivisionLevel and gridDim.
     * @param mainEnvelope Computation area
     */
    public void setMainEnvelope(Envelope mainEnvelope) {
        this.mainEnvelope = mainEnvelope;
        // Split domain into 4^subdiv cells
        // Compute subdivision level using envelope and maximum propagation distance
        double greatestSideLength = mainEnvelope.maxExtent();
        int subdivisionLevel = 0;
        while(maximumPropagationDistance / (greatestSideLength / Math.pow(2, subdivisionLevel)) < MINIMAL_BUFFER_RATIO) {
            subdivisionLevel++;
        }
        gridDim = (int) Math.pow(2, subdivisionLevel);
    }

    /**
     * @return True if diffraction of horizontal edges is computed.
     */
    public boolean isComputeVerticalDiffraction() {
        return computeVerticalDiffraction;
    }

    /**
     * Activate of deactivate diffraction of horizontal edges. Height of buildings must be provided.
     * @param computeVerticalDiffraction New value
     */
    public void setComputeVerticalDiffraction(boolean computeVerticalDiffraction) {
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }

}
