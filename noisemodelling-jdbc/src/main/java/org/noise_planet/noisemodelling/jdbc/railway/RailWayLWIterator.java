/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.railway;

import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.Tuple;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.Locale;


public class RailWayLWIterator implements Iterator<RailWayLWGeom> {
    private RailwayCnossos railway = new RailwayCnossos();
    private Connection connection;
    private RailWayLWGeom railWayLWComplete = null;
    private RailWayLWGeom railWayLWIncomplete = new RailWayLWGeom();
    private String tableTrackGeometry;
    private String tableTrainTraffic;
    private ResultSet spatialResultSet;
    private String geometryColumnName = null;
    private DBTypes dbType = null;
    public Map<String, Integer> sourceFields = null;


    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * @param connection
     * @param tableTrackGeometry Track geometry and metadata
     * @param tableTrainTraffic Train traffic associated with tracks
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic) {
        this.railway.setVehicleDataFile("RailwayVehiclesCnossos.json");
        this.railway.setTrainSetDataFile("RailwayTrainsets.json");
        this.railway.setRailwayDataFile("RailwayCnossosSNCF_2021.json");
        this.connection = connection;
        this.tableTrackGeometry = tableTrackGeometry;
        this.tableTrainTraffic = tableTrainTraffic;
        railWayLWComplete = fetchNext(railWayLWIncomplete);
    }


    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * @param connection
     * @param tableTrackGeometry Track geometry and metadata
     * @param tableTrainTraffic Train traffic associated with tracks
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic, String vehicleDataFile, String trainSetDataFile, String railwayDataFile) {
       this.railway.setVehicleDataFile(vehicleDataFile);
        this.railway.setTrainSetDataFile(trainSetDataFile);
        this.railway.setRailwayDataFile(railwayDataFile);
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
     * Check if a specified column exists in the given ResultSet
     * @param rs result set to inspect
     * @param columnName column to look up
     * @return {@code true} if the specified column name exists in the result set; {@code false} otherwise.
     * @throws SQLException if metadata retrieval fails
     */
    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsMetaData = rs.getMetaData();
        int columnCount = rsMetaData.getColumnCount();
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            if (columnName.equalsIgnoreCase(rsMetaData.getColumnName(columnIndex))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the next RailWayLWGeom object in the sequence
     * @return the current RailWayLWGeom object.
     */
    @Override
    public RailWayLWGeom next() {
        RailWayLWGeom current = railWayLWComplete;
        railWayLWComplete = fetchNext(railWayLWIncomplete);
        return current;
    }

    public RailWayLWGeom current() {
        return railWayLWComplete;
    }

    /**
     * Fetches the next RailWayLWGeom object from the spatial result set
     * @param incompleteRecord
     * @return the next complete RailWayLWGeom object, or null if there are no more records.
     */
    private RailWayLWGeom fetchNext(RailWayLWGeom incompleteRecord) {
        RailWayLWGeom completeRecord = null;
        try {
            boolean hasNext = false;
            if (spatialResultSet == null) {
        Tuple<String, Integer> trackKey = resolveIntegerPrimaryKey(connection, tableTrackGeometry);
        // Properly quote table names for cross-database compatibility
        dbType = DBUtils.getDBType(connection);
        TableLocation trackGeomLoc = TableLocation.parse(tableTrackGeometry, dbType);
        TableLocation trainTrafficLoc = TableLocation.parse(tableTrainTraffic, dbType);
        String quotedTrackKey = TableLocation.quoteIdentifier(trackKey.first(), dbType);
        
        // Get geometry column name for cross-database geometry reading
        List<String> geomColumns = GeometryTableUtilities.getGeometryColumnNames(connection, trackGeomLoc);
        if (geomColumns.isEmpty()) {
            throw new SQLException("No geometry column found in table " + tableTrackGeometry);
        }
        geometryColumnName = geomColumns.get(0);
        
        ResultSet rawResultSet = connection.createStatement().executeQuery(
            "SELECT r1." + quotedTrackKey + " trackid, r1.*, r2.* FROM " + trackGeomLoc + " r1, " +
                trainTrafficLoc + " r2 WHERE r1.IDSECTION=R2.IDSECTION ORDER BY R1." + quotedTrackKey);
        spatialResultSet = rawResultSet;
        if (rawResultSet instanceof SpatialResultSet) {
            spatialResultSet = (SpatialResultSet) rawResultSet;
        }
                if(!spatialResultSet.next()) {
                    return null;
                }
                hasNext = true;
                if (sourceFields == null) {
                    sourceFields = new HashMap<>();
                    int fieldId = 1;
                    for (String fieldName : JDBCUtilities.getColumnNames(spatialResultSet.getMetaData())) {
                        sourceFields.put(fieldName.toUpperCase(), fieldId++);
                    }
                }
                if (sourceFields.containsKey("TRACKSPC")) {
                    incompleteRecord.distance = spatialResultSet.getDouble("TRACKSPC");
                }
                incompleteRecord.setRailWayLW(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                incompleteRecord.setRailWayLWDay(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                incompleteRecord.setRailWayLWEvening(getRailwayEmissionFromResultSet(spatialResultSet, "EVENING"));
                incompleteRecord.setRailWayLWNight(getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT"));
                incompleteRecord.nbTrack = spatialResultSet.getInt("NTRACK");
                incompleteRecord.idSection = spatialResultSet.getString("IDSECTION");
                if (hasColumn(spatialResultSet, "GS")) {
                    incompleteRecord.gs = spatialResultSet.getDouble("GS");
                }
                incompleteRecord.pk = spatialResultSet.getInt("trackid");
                incompleteRecord.geometry = splitGeometry(GeometrySqlHelper.getGeometry(spatialResultSet, geometryColumnName, dbType));
            }
            if(incompleteRecord.pk == -1) {
                return null;
            }
            while (spatialResultSet.next()) {
                hasNext = true;
                if (incompleteRecord.pk == spatialResultSet.getInt("trackid")) {
                    incompleteRecord.setRailWayLW(RailWayCnossosParameters.sumRailwaySource(incompleteRecord.railWayLW, getRailwayEmissionFromResultSet(spatialResultSet, "DAY")));
                    incompleteRecord.setRailWayLWDay(RailWayCnossosParameters.sumRailwaySource(incompleteRecord.railWayLWDay, getRailwayEmissionFromResultSet(spatialResultSet, "DAY")));
                    incompleteRecord.setRailWayLWEvening(RailWayCnossosParameters.sumRailwaySource(incompleteRecord.railWayLWEvening, getRailwayEmissionFromResultSet(spatialResultSet, "EVENING")));
                    incompleteRecord.setRailWayLWNight(RailWayCnossosParameters.sumRailwaySource(incompleteRecord.railWayLWNight, getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT")));
                } else {
                    // railWayLWIncomplete is complete
                    completeRecord = new RailWayLWGeom(incompleteRecord);
                    // read next (incomplete) instance attributes for the next() call
                    incompleteRecord.geometry = splitGeometry(GeometrySqlHelper.getGeometry(spatialResultSet, geometryColumnName, dbType));
                    if (sourceFields.containsKey("TRACKSPC")) {
                        incompleteRecord.distance = spatialResultSet.getDouble("TRACKSPC");
                    }
                    // initialize incomplete record
                    incompleteRecord.setRailWayLW(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    incompleteRecord.setRailWayLWDay(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    incompleteRecord.setRailWayLWEvening(getRailwayEmissionFromResultSet(spatialResultSet, "EVENING"));
                    incompleteRecord.setRailWayLWNight(getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT"));
                    incompleteRecord.nbTrack = spatialResultSet.getInt("NTRACK");
                    incompleteRecord.idSection = spatialResultSet.getString("IDSECTION");
                    if (hasColumn(spatialResultSet, "GS")) {
                        incompleteRecord.gs = spatialResultSet.getDouble("GS");
                    }
                    incompleteRecord.pk = spatialResultSet.getInt("trackid");
                    incompleteRecord.geometry = splitGeometry(GeometrySqlHelper.getGeometry(spatialResultSet, geometryColumnName, dbType));
                    break;
                }
            }
            if(!hasNext) {
                incompleteRecord.pk = -1;
            } else {
                if (completeRecord == null) {
                    completeRecord = new RailWayLWGeom(incompleteRecord);
                }
            }
            return completeRecord;
        } catch (SQLException | IOException throwables) {
            throw new NoSuchElementException(throwables.getMessage());
        }
    }

    /**
     * Resolve the integer primary key column for a table in a cross-database safe way.
     * Falls back to the legacy JDBCUtilities implementation for H2GIS while handling
     * PostgreSQL schema scoping explicitly.
     */
    private Tuple<String, Integer> resolveIntegerPrimaryKey(Connection connection, String tableName) throws SQLException {
        // First try to detect database type from metadata (more reliable for wrapped connections)
        DBTypes dbType = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (metaData != null) {
                String product = metaData.getDatabaseProductName();
                if (product != null) {
                    String productLower = product.toLowerCase(Locale.ROOT);
                    if (productLower.contains("postgres")) {
                        dbType = DBTypes.POSTGIS;
                    } else if (productLower.contains("h2")) {
                        dbType = DBTypes.H2GIS;
                    }
                }
            }
        } catch (SQLException ignored) {
            // metadata lookup failed, fall back to DBUtils
        }
        
        // Fall back to DBUtils if metadata check didn't determine type
        if (dbType == null) {
            dbType = DBUtils.getDBType(connection);
        }
        
        TableLocation tableLocation = TableLocation.parse(tableName, dbType);

        if (dbType == DBTypes.POSTGRESQL || dbType == DBTypes.POSTGIS) {
            String schema = tableLocation.getSchema();
            String table = tableLocation.getTable();
            if (table == null || table.isEmpty()) {
                table = tableName;
            }

            String cleanedSchema = schema == null ? null : schema.replace('"', ' ').trim();
            String cleanedTable = table.replace('"', ' ').trim();

            DatabaseMetaData metaData = connection.getMetaData();
            String[] tableCandidates = new String[] {
                    cleanedTable,
                    cleanedTable.toLowerCase(Locale.ROOT),
                    cleanedTable.toUpperCase(Locale.ROOT)
            };

            Set<String> schemaCandidates = new LinkedHashSet<>();
            if (cleanedSchema != null && !cleanedSchema.isEmpty()) {
                schemaCandidates.add(cleanedSchema);
                schemaCandidates.add(cleanedSchema.toLowerCase(Locale.ROOT));
                schemaCandidates.add(cleanedSchema.toUpperCase(Locale.ROOT));
            }
            try {
                String currentSchema = connection.getSchema();
                if (currentSchema != null && !currentSchema.isEmpty()) {
                    schemaCandidates.add(currentSchema);
                    schemaCandidates.add(currentSchema.toLowerCase(Locale.ROOT));
                    schemaCandidates.add(currentSchema.toUpperCase(Locale.ROOT));
                }
            } catch (SQLException ignore) {
                // ignore schema lookup issues and fall back to metadata search without schema hint
            }

            schemaCandidates.add(null);

            String pkColumnName = null;
            String resolvedSchema = null;
            String resolvedTable = null;
            outer:
            for (String schemaCandidate : schemaCandidates) {
                for (String candidate : tableCandidates) {
                    try (ResultSet rs = metaData.getPrimaryKeys(null, schemaCandidate, candidate)) {
                        if (rs.next()) {
                            pkColumnName = rs.getString("COLUMN_NAME");
                            resolvedSchema = schemaCandidate;
                            resolvedTable = candidate;
                            break outer;
                        }
                    }
                }
            }

            if (pkColumnName == null || resolvedTable == null) {
                throw new SQLException("Table " + cleanedTable + " not found or does not have an integer primary key");
            }

            // Resolve schema if metadata lookup returned null (e.g., relying on search_path)
            if (resolvedSchema == null || resolvedSchema.isEmpty()) {
                resolvedSchema = cleanedSchema;
                if (resolvedSchema == null || resolvedSchema.isEmpty()) {
                    try {
                        resolvedSchema = connection.getSchema();
                    } catch (SQLException ignore) {
                        resolvedSchema = null;
                    }
                }
            }

            TreeMap<Integer, String> columnsByOrdinal = new TreeMap<>();
            String[] schemaAttempts = new String[] {
                    resolvedSchema,
                    resolvedSchema != null ? resolvedSchema.toLowerCase(Locale.ROOT) : null,
                    resolvedSchema != null ? resolvedSchema.toUpperCase(Locale.ROOT) : null
            };
            String[] tableAttempts = new String[] {
                    resolvedTable,
                    resolvedTable.toLowerCase(Locale.ROOT),
                    resolvedTable.toUpperCase(Locale.ROOT)
            };

            boolean metadataLoaded = false;
            for (String schemaAttempt : schemaAttempts) {
                if (metadataLoaded) {
                    break;
                }
                for (String tableAttempt : tableAttempts) {
                    try (ResultSet columnRs = metaData.getColumns(null, schemaAttempt, tableAttempt, null)) {
                        while (columnRs.next()) {
                            String columnName = columnRs.getString("COLUMN_NAME");
                            int ordinal = columnRs.getInt("ORDINAL_POSITION");
                            if (columnName != null) {
                                columnsByOrdinal.put(ordinal, columnName);
                            }
                        }
                        if (!columnsByOrdinal.isEmpty()) {
                            metadataLoaded = true;
                            break;
                        }
                    }
                }
            }

            List<String> columns;
            if (!columnsByOrdinal.isEmpty()) {
                columns = new ArrayList<>(columnsByOrdinal.values());
            } else {
                // Fallback to querying information_schema when DatabaseMetaData lookup fails (e.g. schema search path issues)
                String schemaForQuery = resolvedSchema;
                if (schemaForQuery == null || schemaForQuery.isEmpty()) {
                    schemaForQuery = cleanedSchema;
                }
                if (schemaForQuery == null || schemaForQuery.isEmpty()) {
                    try {
                        schemaForQuery = connection.getSchema();
                    } catch (SQLException ignore) {
                        schemaForQuery = null;
                    }
                }

                String normalizedTable = resolvedTable.toLowerCase(Locale.ROOT);
                String normalizedSchema = schemaForQuery != null ? schemaForQuery.toLowerCase(Locale.ROOT) : null;

                StringBuilder infoSchemaSql = new StringBuilder(
                        "SELECT column_name, ordinal_position FROM information_schema.columns WHERE table_name = ?");
                if (normalizedSchema != null && !normalizedSchema.isEmpty()) {
                    infoSchemaSql.append(" AND table_schema = ?");
                }
                infoSchemaSql.append(" ORDER BY ordinal_position");

                try (PreparedStatement columnStmt = connection.prepareStatement(infoSchemaSql.toString())) {
                    columnStmt.setString(1, normalizedTable);
                    if (normalizedSchema != null && !normalizedSchema.isEmpty()) {
                        columnStmt.setString(2, normalizedSchema);
                    }
                    try (ResultSet columnRs = columnStmt.executeQuery()) {
                        while (columnRs.next()) {
                            String columnName = columnRs.getString("column_name");
                            int ordinal = columnRs.getInt("ordinal_position");
                            if (columnName != null) {
                                columnsByOrdinal.put(ordinal, columnName);
                            }
                        }
                    }
                }

                if (!columnsByOrdinal.isEmpty()) {
                    columns = new ArrayList<>(columnsByOrdinal.values());
                } else {
                    columns = JDBCUtilities.getColumnNames(connection, TableLocation.parse(tableName, dbType));
                }
            }
            for (int idx = 0; idx < columns.size(); idx++) {
                String column = columns.get(idx);
                if (column.equalsIgnoreCase(pkColumnName)) {
                    return new Tuple<>(column, idx + 1);
                }
            }

            throw new SQLException("Primary key column " + pkColumnName + " not found in table " + cleanedTable);
        }

        return JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection, tableLocation);
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
        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeTransfert = 0;
        int curvature = 0;
        int railRoughness = 1;
        int nbTrack = 2;
        double vMaxInfra = 160;
        double commercialSpeed = 160;
        boolean isTunnel = false;
        String idSection = "";

        // Read fields
        if (sourceFields.containsKey("TRAINSPD")) {
            vehicleSpeed = rs.getDouble("TRAINSPD");
        }
        if (sourceFields.containsKey("T" + period)) {
            vehiclePerHour = rs.getDouble("T" + period);
        }
        if (sourceFields.containsKey("ROLLINGCONDITION")) {
            rollingCondition = rs.getInt("ROLLINGCONDITION");
        }
        if (sourceFields.containsKey("IDLINGTIME")) {
            idlingTime = rs.getDouble("IDLINGTIME");
        }
        if (sourceFields.containsKey("TRANSFER")) {
            trackTransfer = rs.getInt("TRANSFER");
        }
        if (sourceFields.containsKey("ROUGHNESS")) {
            railRoughness = rs.getInt("ROUGHNESS");
        }

        if (sourceFields.containsKey("IMPACT")) {
            impactNoise = rs.getInt("IMPACT");
        }
        if (sourceFields.containsKey("BRIDGE")) {
            bridgeTransfert = rs.getInt("BRIDGE");
        }
        if (sourceFields.containsKey("CURVATURE")) {
            curvature = rs.getInt("CURVATURE");
        }

        if (sourceFields.containsKey("TRACKSPD")) {
            vMaxInfra = rs.getDouble("TRACKSPD");
        }

        if (sourceFields.containsKey("COMSPD")) {
            commercialSpeed = rs.getDouble("COMSPD");
        }
        if (sourceFields.containsKey("TRAINTYPE")) {
            train = rs.getString("TRAINTYPE");
        }

        if (sourceFields.containsKey("TYPETRAIN")) {
            train = rs.getString("TYPETRAIN");
        }

        if (sourceFields.containsKey("ISTUNNEL")) {
            isTunnel = rs.getBoolean("ISTUNNEL");
        }

        if (sourceFields.containsKey("IDTUNNEL")) {
            String idTunnel = rs.getString("IDTUNNEL");
            isTunnel = idTunnel != null && !idTunnel.trim().isEmpty();
        }

        if (sourceFields.containsKey("NTRACK")) {
            nbTrack = rs.getInt("NTRACK");
        }


        RailWayCnossosParameters  lWRailWay = new RailWayCnossosParameters();

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, commercialSpeed, isTunnel, nbTrack);

        Map<String, Integer> vehicles = railway.getVehicleFromTrainset(train);
       // double vehiclePerHouri=vehiclePerHour;
        if (vehicles!=null){
            int i = 0;
            for (Map.Entry<String,Integer> entry : vehicles.entrySet()){
                String typeTrain = entry.getKey();
                double vehiclePerHouri = vehiclePerHour * entry.getValue();
                if (vehiclePerHouri>0) {
                    RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(typeTrain, vehicleSpeed,
                            vehiclePerHouri / (double) nbTrack, rollingCondition, idlingTime);

                    if (i == 0) {
                        lWRailWay = railway.evaluate(vehicleParameters, trackParameters);
                    } else {
                        lWRailWay = RailWayCnossosParameters.sumRailwaySource(lWRailWay, railway.evaluate(vehicleParameters, trackParameters));
                    }
                }
                i++;
            }

        }else if (railway.isInVehicleList(train)){
            if (vehiclePerHour>0) {
                RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(train, vehicleSpeed,
                        vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);
                lWRailWay = railway.evaluate(vehicleParameters, trackParameters);
            }
        }

        return lWRailWay;
    }



}


