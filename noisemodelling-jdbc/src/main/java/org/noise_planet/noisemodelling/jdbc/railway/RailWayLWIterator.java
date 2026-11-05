/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.railway;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.Tuple;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.railway.Railway;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map;


public class RailWayLWIterator implements Iterator<RailWayLWGeom>, AutoCloseable {
    public static final String RAILWAY_VEHICLES_CNOSSOS_JSON = "RailwayVehiclesCnossos.json";
    public static final String RAILWAY_TRAINSETS_JSON = "RailwayTrainsets.json";
    public static final String RAILWAY_EMISSION_CNOSSOS_JSON = "RailwayEmissionCnossos.json";
    public static final String RAILWAY_PLATFORMS_JSON = "RailwayPlatforms.json";
    private RailwayCnossos railway = new RailwayCnossos();
    private Connection connection;
    private RailWayLWGeom railWayLWComplete = null;
    private RailWayLWGeom railWayLWIncomplete = new RailWayLWGeom();
    private String tableTrackGeometry;
    private String tableTrainTraffic;
    private SpatialResultSet spatialResultSet;
    private Statement statement;
    private boolean finished = false;
    public Map<String, Integer> sourceFields = null;
    private Map<String, RailwayPlatform> platformMap;


    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * @param connection
     * @param tableTrackGeometry Track geometry and metadata
     * @param tableTrainTraffic Train traffic associated with tracks
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic) throws IOException {
        this(connection, tableTrackGeometry, tableTrainTraffic, RAILWAY_VEHICLES_CNOSSOS_JSON, RAILWAY_TRAINSETS_JSON, RAILWAY_EMISSION_CNOSSOS_JSON, RAILWAY_PLATFORMS_JSON);
    }

    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * Constructs a RailWayLWIterator instance to process railway data by reading from a database connection
     * and associated data files. Initializes the railway metadata and fetches the first incomplete railway geometry.
     *
     * @param connection          Database connection used to fetch railway and traffic data.
     * @param tableTrackGeometry  Table name containing track geometry and metadata.
     * @param tableTrainTraffic   Table name containing train traffic details associated with tracks.
     * @param vehicleDataFile     File path Url or resource filename (from org.noise_planet.noisemodelling.emission.railway package) for vehicle data configuration.
     * @param trainSetDataFile    File path Url or resource filename (from org.noise_planet.noisemodelling.emission.railway package) for train set data configuration.
     * @param railwayEmissionDataFile     File path Url or resource filename (from org.noise_planet.noisemodelling.emission.railway package) for railway metadata configuration.
     * @throws IOException        If an error occurs during file reading or parsing the specified data files.
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic, String vehicleDataFile, String trainSetDataFile, String railwayEmissionDataFile, String platformDataFile) throws IOException {
       this.railway.setVehicleDataFile(vehicleDataFile);
        this.railway.setTrainSetDataFile(trainSetDataFile);
        this.railway.setRailwayDataFile(railwayEmissionDataFile);
        this.setPlatformDataFile(platformDataFile);
        this.connection = connection;
        this.tableTrackGeometry = tableTrackGeometry;
        this.tableTrainTraffic = tableTrainTraffic;
        railWayLWComplete = fetchNext(railWayLWIncomplete);
    }
    @Override
    public boolean hasNext() {
        return railWayLWComplete != null;
    }

    /**
     * Set a custom platform data file (JSON resource next to Railway class).
     * @param platformDataFile name of the JSON file (e.g. "RailwayPlatforms.json")
     */
    public void setPlatformDataFile(String platformDataFile) {
        this.platformMap = RailwayPlatform.loadFromJSON(platformDataFile);
    }

    /**
     * Read platform info for a rail section from the result set.
     * Looks up the PLATFORM column (name -> JSON lookup), defaults to DEFAULT if no column present.
     * @throws IllegalArgumentException if the platform name is not found in the JSON library
     */
    private RailwayPlatform readPlatform(SpatialResultSet rs) throws SQLException {
        if (sourceFields.containsKey("PLATFORM")) {
            String platformName = rs.getString("PLATFORM");
            if (!rs.wasNull() && platformName != null && !platformName.isEmpty()) {
                RailwayPlatform p = platformMap.get(platformName.toUpperCase());
                if (p == null) {
                    throw new IllegalArgumentException(
                            "Platform '" + platformName + "' not found in platform data file. " +
                            "Available platforms: " + platformMap.keySet());
                }
                return p;
            }
        }
        return RailwayPlatform.DEFAULT_PLATFORM;
    }


    /**
     * Split the input geometry into a list of LineString objects.
     * @param geometry
     * @return a list of LineString objects extracted from the input geometry.
     */
    private List<LineString> splitGeometry(Geometry geometry){
        List<LineString> inputLineStrings = new ArrayList<>();
        for (int id = 0; id < geometry.getNumGeometries(); id++) {
            Geometry subGeom = geometry.getGeometryN(id);
            if (subGeom instanceof LineString) {
                inputLineStrings.add((LineString) subGeom);
            }
        }
        return inputLineStrings;
    }

    /**
     * Check if a specified column exists in the given SpatialResultSet
     * @param rs
     * @param columnName
     * @return "true" if the specified column name exists in the result set; "false" otherwise.
     * @throws SQLException
     */
    public static boolean hasColumn(SpatialResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the next RailWayLWGeom object in the sequence
     * @return the current RailWayLWGeom object.
     * @throws NoSuchElementException if the iterator has no more records.
     */
    @Override
    public RailWayLWGeom next() {
        if (railWayLWComplete == null) {
            throw new NoSuchElementException("No more railway sections to read");
        }
        RailWayLWGeom current = railWayLWComplete;
        railWayLWComplete = fetchNext(railWayLWIncomplete);
        return current;
    }

    public RailWayLWGeom current() {
        return railWayLWComplete;
    }

    /**
     * Release the JDBC resources used by this iterator. The iterator can no longer be advanced
     * after this call.
     */
    @Override
    public void close() throws SQLException {
        finished = true;
        closeResultSet();
    }

    private void closeResultSet() throws SQLException {
        SQLException closeException = null;
        if (spatialResultSet != null) {
            try {
                spatialResultSet.close();
            } catch (SQLException ex) {
                closeException = ex;
            } finally {
                spatialResultSet = null;
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                if (closeException == null) {
                    closeException = ex;
                } else {
                    closeException.addSuppressed(ex);
                }
            } finally {
                statement = null;
            }
        }
        if (closeException != null) {
            throw closeException;
        }
    }

    private void closeResultSetQuietly() {
        try {
            closeResultSet();
        } catch (SQLException ignored) {
            // The read failure is the useful exception to report
        }
    }

    private void openResultSet() throws SQLException {
        Tuple<String, Integer> trackKey = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection,
                TableLocation.parse(tableTrackGeometry, DBUtils.getDBType(connection)));
        statement = connection.createStatement();
        spatialResultSet = statement.executeQuery(
                "SELECT r1."+trackKey.first()+" trackid, r1.*, r2.* FROM " + tableTrackGeometry + " r1, " +
                        tableTrainTraffic + " r2 WHERE r1.IDSECTION=R2.IDSECTION ORDER BY R1." + trackKey.first())
                .unwrap(SpatialResultSet.class);
        if (sourceFields == null) {
            sourceFields = new HashMap<>();
            int fieldId = 1;
            for (String fieldName : JDBCUtilities.getColumnNames(spatialResultSet.getMetaData())) {
                sourceFields.put(fieldName.toUpperCase(), fieldId++);
            }
        }
    }

    /**
     * Read the attributes of a new rail section from the current row.
     */
    private void readSection(RailWayLWGeom record, SpatialResultSet rs) throws SQLException, IOException {
        record.nbTrack = readNbTrack(rs);
        record.idSection = rs.getString("IDSECTION");
        record.platform = readPlatform(rs);
        // GS from explicit column, otherwise use platform g3 (ground factor between rails)
        if (hasColumn(rs, "GS")) {
            record.gs = readDouble(rs, "GS", record.platform.g3);
        } else {
            record.gs = record.platform.g3;
        }
        record.pk = rs.getInt("trackid");
        record.geometry = splitGeometry(rs.getGeometry());
        // Reset the spacing so a NULL TRACKSPC cannot inherit the previous section value
        record.distance = RailWayLWGeom.DEFAULT_DISTANCE;
        if (sourceFields.containsKey("TRACKSPC")) {
            record.distance = readDouble(rs, "TRACKSPC", RailWayLWGeom.DEFAULT_DISTANCE);
        }
        readSources(record, rs);
    }

    /**
     * Initialize the source spectra of a section from its first traffic row.
     */
    private void readSources(RailWayLWGeom record, SpatialResultSet rs) throws SQLException, IOException {
        record.setRailWayLW(getRailwayEmissionFromResultSet(rs, "DAY"));
        record.setRailWayLWDay(getRailwayEmissionFromResultSet(rs, "DAY"));
        record.setRailWayLWEvening(getRailwayEmissionFromResultSet(rs, "EVENING"));
        record.setRailWayLWNight(getRailwayEmissionFromResultSet(rs, "NIGHT"));
        record.cref = record.railWayLWDay.getCref();
    }

    /**
     * Add one traffic row to the source spectra of the current section.
     */
    private void addSources(RailWayLWGeom record, SpatialResultSet rs) throws SQLException, IOException {
        record.setRailWayLW(RailWayCnossosParameters.sumRailwaySource(record.railWayLW,
                getRailwayEmissionFromResultSet(rs, "DAY")));
        record.setRailWayLWDay(RailWayCnossosParameters.sumRailwaySource(record.railWayLWDay,
                getRailwayEmissionFromResultSet(rs, "DAY")));
        record.setRailWayLWEvening(RailWayCnossosParameters.sumRailwaySource(record.railWayLWEvening,
                getRailwayEmissionFromResultSet(rs, "EVENING")));
        record.setRailWayLWNight(RailWayCnossosParameters.sumRailwaySource(record.railWayLWNight,
                getRailwayEmissionFromResultSet(rs, "NIGHT")));
    }

    private RailWayLWGeom completeRecord(RailWayLWGeom record) {
        // Sync cref from accumulated emissions before creating final record
        record.cref = record.railWayLWDay.getCref();
        return new RailWayLWGeom(record);
    }

    /**
     * Fetches the next RailWayLWGeom object from the spatial result set
     * @param incompleteRecord
     * @return the next complete RailWayLWGeom object, or null if there are no more records.
     */
    private RailWayLWGeom fetchNext(RailWayLWGeom incompleteRecord) {
        if (finished) {
            return null;
        }
        try {
            if (spatialResultSet == null) {
                openResultSet();
                if (!spatialResultSet.next()) {
                    finished = true;
                    closeResultSetQuietly();
                    return null;
                }
                readSection(incompleteRecord, spatialResultSet);
            }
            if (incompleteRecord.pk == -1) {
                return null;
            }
            while (spatialResultSet.next()) {
                if (incompleteRecord.pk == spatialResultSet.getInt("trackid")) {
                    addSources(incompleteRecord, spatialResultSet);
                } else {
                    RailWayLWGeom completeRecord = completeRecord(incompleteRecord);
                    readSection(incompleteRecord, spatialResultSet);
                    return completeRecord;
                }
            }
            // End of the result set: emit the pending record, even if no row was read in this call
            RailWayLWGeom completeRecord = completeRecord(incompleteRecord);
            incompleteRecord.pk = -1;
            finished = true;
            closeResultSetQuietly();
            return completeRecord;
        } catch (SQLException | IOException throwables) {
            closeResultSetQuietly();
            throw new RuntimeException("Failed to read railway sections", throwables);
        } catch (RuntimeException ex) {
            closeResultSetQuietly();
            throw ex;
        }
    }

    private String describeRow(ResultSet rs) {
        try {
            return "section '" + rs.getString("IDSECTION") + "', track " + rs.getInt("trackid");
        } catch (SQLException ex) {
            return "unknown section/track";
        }
    }

    private double readDouble(ResultSet rs, String columnName, double defaultValue) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? defaultValue : value;
    }

    private int readInt(ResultSet rs, String columnName, int defaultValue) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? defaultValue : value;
    }

    private String readNonBlankString(ResultSet rs, String columnName, String defaultValue) throws SQLException {
        String value = rs.getString(columnName);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private int readNbTrack(ResultSet rs) throws SQLException {
        if (!sourceFields.containsKey("NTRACK")) {
            return 2;
        }
        int trackCount = rs.getInt("NTRACK");
        if (rs.wasNull() || trackCount <= 0) {
            throw new IllegalArgumentException("Invalid NTRACK value for " + describeRow(rs) +
                    ": expected a positive number of tracks");
        }
        return trackCount;
    }

    /**
     * Retrieves railway emission parameters from the given ResultSet for a specified period.
     * @param rs     result set of source
     * @param period Day or Evening or Night
     * @return Emission spectrum in dB
     */
    public RailWayCnossosParameters getRailwayEmissionFromResultSet(ResultSet rs, String period) throws SQLException, IOException {
        String train = "FRET";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int rollingCondition = 0;
        double idlingTime = 0;
        String trackTransferStr = "SNCF4";
        String impactNoiseStr = "";
        String bridgeTransfertStr = "";
        int curvature = 0;
        String railRoughnessStr = "SNCF1";
        int nbTrack = 2;
        double vMaxInfra = 160;
        double commercialSpeed = Railway.NO_SPEED_LIMIT;
        boolean isTunnel = false;

        // Read fields
        if (sourceFields.containsKey("TRAINSPD")) {
            vehicleSpeed = readDouble(rs, "TRAINSPD", vehicleSpeed);
        }
        if (sourceFields.containsKey("T" + period)) {
            vehiclePerHour = readDouble(rs, "T" + period, 0);
        }
        if (sourceFields.containsKey("ROLLINGCONDITION")) {
            rollingCondition = readInt(rs, "ROLLINGCONDITION", rollingCondition);
        }
        if (sourceFields.containsKey("IDLINGTIME")) {
            idlingTime = readDouble(rs, "IDLINGTIME", idlingTime);
        }
        if (sourceFields.containsKey("TRANSFER")) {
            trackTransferStr = readNonBlankString(rs, "TRANSFER", trackTransferStr);
        }
        if (sourceFields.containsKey("ROUGHNESS")) {
            railRoughnessStr = readNonBlankString(rs, "ROUGHNESS", railRoughnessStr);
        }

        if (sourceFields.containsKey("IMPACT")) {
            String val = rs.getString("IMPACT");
            impactNoiseStr = val != null ? val : "";
        }
        if (sourceFields.containsKey("BRIDGE")) {
            String val = rs.getString("BRIDGE");
            bridgeTransfertStr = val != null ? val : "";
        }
        if (sourceFields.containsKey("CURVATURE")) {
            curvature = readInt(rs, "CURVATURE", curvature);
        }

        if (sourceFields.containsKey("TRACKSPD")) {
            vMaxInfra = readDouble(rs, "TRACKSPD", vMaxInfra);
        }

        if (sourceFields.containsKey("COMSPD")) {
            double commercialSpeedValue = readDouble(rs, "COMSPD", 0);
            // COMSPD is optional: NULL/zero means "not provided", keep the default value
            if (commercialSpeedValue > 0.) {
                commercialSpeed = commercialSpeedValue;
            }
        }
        if (sourceFields.containsKey("TRAINTYPE")) {
            train = rs.getString("TRAINTYPE");
        }

        if (sourceFields.containsKey("TYPETRAIN")) {
            train = rs.getString("TYPETRAIN");
        }

        if (train == null || train.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing train type (TRAINTYPE/TYPETRAIN) for " + describeRow(rs));
        }
        train = train.trim();

        if (sourceFields.containsKey("ISTUNNEL")) {
            isTunnel = rs.getBoolean("ISTUNNEL");
        }

        if (sourceFields.containsKey("IDTUNNEL")) {
            String idTunnel = rs.getString("IDTUNNEL");
            isTunnel = idTunnel != null && !idTunnel.trim().isEmpty();
        }

        nbTrack = readNbTrack(rs);

        Map<String, Integer> vehicles = railway.getVehicleFromTrainset(train);
        boolean isVehicle = railway.isInVehicleList(train);
        if (vehicles == null && !isVehicle) {
            throw new IllegalArgumentException("Unknown train type '" + train + "' for " + describeRow(rs) +
                    ". Check TRAINTYPE/TYPETRAIN against the train set and vehicle data files");
        }

        RailWayCnossosParameters  lWRailWay = new RailWayCnossosParameters();

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransferStr, railRoughnessStr,
                impactNoiseStr, bridgeTransfertStr, curvature, commercialSpeed, isTunnel, nbTrack);

        if (vehicles!=null){
            // Two-pass approach:
            // Pass 1: evaluate each vehicle type and collect emissions + RBE values
            List<RailWayCnossosParameters> vehicleEmissions = new ArrayList<>();
            List<Integer> vehicleRbe = new ArrayList<>();
            for (Map.Entry<String,Integer> entry : vehicles.entrySet()){
                String typeTrain = entry.getKey();
                int unitCount = entry.getValue();
                double vehiclePerHouri = vehiclePerHour * unitCount;
                int rbe = railway.getReflectingBarrierEffect(typeTrain);
                RailWayCnossosParameters vehEmission = new RailWayCnossosParameters();
                if (vehiclePerHouri>0) {
                    RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(typeTrain, vehicleSpeed,
                            vehiclePerHouri / (double) nbTrack, rollingCondition, idlingTime);
                    vehEmission = railway.evaluate(vehicleParameters, trackParameters);
                }
                vehicleEmissions.add(vehEmission);
                vehicleRbe.add(rbe);
            }

            // Pass 2: compute power-weighted Cref using low-source acoustic power
            double rbeWeightedSum = 0;
            double totalPowerWeight = 0;
            for (int i = 0; i < vehicleEmissions.size(); i++) {
                double power = vehicleEmissions.get(i).computeLowSourcePower();
                rbeWeightedSum += vehicleRbe.get(i) * power;
                totalPowerWeight += power;
            }

            // Merge all vehicle emissions
            for (int i = 0; i < vehicleEmissions.size(); i++) {
                if (i == 0) {
                    lWRailWay = vehicleEmissions.get(i);
                } else {
                    lWRailWay = RailWayCnossosParameters.sumRailwaySource(lWRailWay, vehicleEmissions.get(i));
                }
            }

            // Set power-weighted Cref on the combined emission result
            if (totalPowerWeight > 0) {
                lWRailWay.setCref(rbeWeightedSum / totalPowerWeight);
                lWRailWay.setCrefTotalWeight(totalPowerWeight);
            }

        }else if (isVehicle){
            if (vehiclePerHour>0) {
                RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(train, vehicleSpeed,
                        vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);
                lWRailWay = railway.evaluate(vehicleParameters, trackParameters);
            }
            // Set Cref from single vehicle, weighted by low-source power
            int rbe = railway.getReflectingBarrierEffect(train);
            double power = lWRailWay.computeLowSourcePower();
            lWRailWay.setCref(rbe);
            lWRailWay.setCrefTotalWeight(power);
        }

        return lWRailWay;
    }



}


