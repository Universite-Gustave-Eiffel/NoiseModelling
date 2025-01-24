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
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.h2gis.utilities.wrapper.ConnectionWrapper;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.directivity.DirectivityRecord;
import org.noise_planet.noisemodelling.emission.directivity.DiscreteDirectivitySphere;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Wall;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.WallAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;
import static org.h2gis.utilities.GeometryTableUtilities.getSRID;
/**
 * Common attributes for subdivision of the domain
 * @author Nicolas Fortin
 */
public abstract class NoiseMapLoader {
    // When computing cell size, try to keep propagation distance away from the cell
    // inferior to this ratio (in comparison with cell width)
    Logger logger = LoggerFactory.getLogger(NoiseMapLoader.class);
    private static final int DEFAULT_FETCH_SIZE = 300;
    protected int fetchSize = DEFAULT_FETCH_SIZE;
    protected static final double MINIMAL_BUFFER_RATIO = 0.3;
    private String alphaFieldName = "G";
    protected final String buildingsTableName;
    protected final String sourcesTableName;
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

    protected String heightField = "HEIGHT";
    protected GeometryFactory geometryFactory;
    protected ProfilerThread profilerThread;

    // Initialised attributes
    /**
     *  Side computation cell count (same on X and Y)
     */
    protected int gridDim = 0;
    protected Envelope mainEnvelope = new Envelope();

    public NoiseMapLoader(String buildingsTableName, String sourcesTableName) {
        this.buildingsTableName = buildingsTableName;
        this.sourcesTableName = sourcesTableName;
    }

    /**
     * Computation stacks and timing are collected by this class in order
     * to profile the execution of the simulation
     * @param profilerThread Instance of ProfilerThread
     */
    public void setProfilerThread(ProfilerThread profilerThread) {
        this.profilerThread = profilerThread;
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
     * Compute the envelope corresponding to parameters
     * @param cellIndex Cell location
     * @return Envelope of the cell
     */
    public Envelope getCellEnv(CellIndex cellIndex) {
        return  getCellEnv(mainEnvelope, cellIndex.getLatitudeIndex(),
                cellIndex.getLongitudeIndex(), getCellWidth(), getCellHeight());
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

    /**
     * Fetches digital elevation model (DEM) data for the specified cell envelope and adds it to the mesh.
     * @param connection the database connection to use for querying the DEM data.
     * @param fetchEnvelope  the envelope representing the cell to fetch DEM data for.
     * @param mesh the profile builder mesh to which the DEM data will be added.
     * @throws SQLException if an SQL exception occurs while fetching the DEM data.
     */
    protected void fetchCellDem(Connection connection, Envelope fetchEnvelope, ProfileBuilder mesh) throws SQLException {
        if(!demTable.isEmpty()) {
            DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
            List<String> geomFields = getGeometryColumnNames(connection,
                    TableLocation.parse(demTable, dbType));
            if(geomFields.isEmpty()) {
                throw new SQLException("Digital elevation model table \""+demTable+"\" must exist and contain a POINT field");
            }
            String topoGeomName = geomFields.get(0);
            double sumZ = 0;
            int topoCount = 0;
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT " + TableLocation.quoteIdentifier(topoGeomName, dbType) + " FROM " +
                            demTable + " WHERE " +
                            TableLocation.quoteIdentifier(topoGeomName, dbType) + " && ?::geometry")) {
                st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
                try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                    while (rs.next()) {
                        Geometry pt = rs.getGeometry();
                        if(pt != null) {
                            Coordinate ptCoordinate = pt.getCoordinate();
                            mesh.addTopographicPoint(ptCoordinate);
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
                    mesh.addTopographicPoint(new Coordinate(coordinate.x, coordinate.y, averageZ));
                }
            }
        }
    }

    /**
     * Fetches buildings data for the specified cell envelope and adds them to the profile builder.
     * @param connection     the database connection to use for querying the buildings data.
     * @param fetchEnvelope  the envelope representing the cell to fetch buildings data for.
     * @param builder        the profile builder to which the buildings data will be added.
     * @throws SQLException  if an SQL exception occurs while fetching the buildings data.
     */
    void fetchCellBuildings(Connection connection, Envelope fetchEnvelope, ProfileBuilder builder) throws SQLException {
        List<Building> buildings = new LinkedList<>();
        List<Wall> walls = new LinkedList<>();
        fetchCellBuildings(connection, fetchEnvelope, buildings, walls);
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
     * @param fetchEnvelope   the envelope representing the cell to fetch building data for.
     * @param buildings       the list to which the fetched buildings will be added.
     * @throws SQLException   if an SQL exception occurs while fetching the building data.
     */
    void fetchCellBuildings(Connection connection, Envelope fetchEnvelope, List<Building> buildings, List<Wall> walls) throws SQLException {
        Geometry envGeo = geometryFactory.toGeometry(fetchEnvelope);
        boolean fetchAlpha = JDBCUtilities.hasField(connection, buildingsTableName, alphaFieldName);
        String additionalQuery = "";
        DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
        if(!heightField.isEmpty()) {
            additionalQuery += ", " + TableLocation.quoteIdentifier(heightField, dbType);
        }
        if(fetchAlpha) {
            additionalQuery += ", " + alphaFieldName;
        }
        String pkBuilding = "";
        final int indexPk = JDBCUtilities.getIntegerPrimaryKey(connection.unwrap(Connection.class), new TableLocation(buildingsTableName, dbType));
        if(indexPk > 0) {
            pkBuilding = JDBCUtilities.getColumnName(connection, buildingsTableName, indexPk);
            additionalQuery += ", " + pkBuilding;
        }
        String buildingGeomName = getGeometryColumnNames(connection,
                TableLocation.parse(buildingsTableName, dbType)).get(0);
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT " + TableLocation.quoteIdentifier(buildingGeomName) + additionalQuery + " FROM " +
                        buildingsTableName + " WHERE " +
                        TableLocation.quoteIdentifier(buildingGeomName, dbType) + " && ?::geometry")) {
            st.setObject(1, geometryFactory.toGeometry(fetchEnvelope));
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                int columnIndex = 0;
                if(!pkBuilding.isEmpty()) {
                    columnIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), pkBuilding);
                }
                double recordGValue = this.wallAbsorption;
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
                        if(intersectedGeometry instanceof Polygon || intersectedGeometry instanceof MultiPolygon || intersectedGeometry instanceof LineString) {
                            if(fetchAlpha) {
                                recordGValue = rs.getDouble(alphaFieldName);
                            }
                            long pk = -1;
                            if(columnIndex != 0) {
                                pk = rs.getLong(columnIndex);
                            }
                            for(int i=0; i<intersectedGeometry.getNumGeometries(); i++) {
                                Geometry geometry = intersectedGeometry.getGeometryN(i);
                                if(geometry instanceof Polygon && !geometry.isEmpty()) {
                                    Building poly = new Building((Polygon) geometry,
                                            heightField.isEmpty() ? Double.MAX_VALUE : rs.getDouble(heightField),
                                            recordGValue, pk, iszBuildings());
                                    buildings.add(poly);
                                } else if (geometry instanceof LineString) {
                                    // decompose linestring into segments
                                    LineString lineString = (LineString) geometry;
                                    Coordinate[] coordinates = lineString.getCoordinates();
                                    for(int vertex=0; vertex < coordinates.length - 1; vertex++) {
                                        Wall wall = new Wall(new LineSegment(coordinates[vertex], coordinates[vertex+1]),
                                                -1, ProfileBuilder.IntersectionType.WALL);
                                        wall.setG(recordGValue);
                                        wall.setPrimaryKey(pk);
                                        wall.setHeight(heightField.isEmpty() ? Double.MAX_VALUE : rs.getDouble(heightField));
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


    /**
     * Fetch source geometries and power
     * @param connection Active connection
     * @param fetchEnvelope Fetch envelope
     * @param propagationProcessData (Out) Propagation process input data
     * @throws SQLException
     */
    public void fetchCellSource(Connection connection, Envelope fetchEnvelope, Scene propagationProcessData, boolean doIntersection)
            throws SQLException, IOException {
        DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));
        TableLocation sourceTableIdentifier = TableLocation.parse(sourcesTableName, dbType);
        List<String> geomFields = getGeometryColumnNames(connection, sourceTableIdentifier);
        if(geomFields.isEmpty()) {
            throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier));
        }
        String sourceGeomName =  geomFields.get(0);
        Geometry domainConstraint = geometryFactory.toGeometry(fetchEnvelope);
        int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection.unwrap(Connection.class), new TableLocation(sourcesTableName, dbType));
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
     * true if train propagation is computed (multiple reflection between the train and a screen)
     */
    public void setBodyBarrier(boolean bodyBarrier) {
        this.bodyBarrier = bodyBarrier;
    }

    public double getCellWidth() {
        return mainEnvelope.getWidth() / gridDim;
    }

    public double getCellHeight() {
        return mainEnvelope.getHeight() / gridDim;
    }

    abstract protected Envelope getComputationEnvelope(Connection connection) throws SQLException;

    /**
     * Fetch scene attributes, compute the best computation cell size.
     * @param connection Active connection
     * @throws java.sql.SQLException
     */
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        if(soundReflectionOrder > 0 && maximumPropagationDistance < maximumReflectionDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        int srid = 0;
        DBTypes dbTypes = DBUtils.getDBType(connection.unwrap(Connection.class));
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
     */
    public void setGs(double gs) {
        this.gs = gs;
    }

    public double getGs() {
        return this.gs;
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
