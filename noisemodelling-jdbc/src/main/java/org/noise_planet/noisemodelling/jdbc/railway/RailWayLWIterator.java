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
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.Map;


public class RailWayLWIterator implements Iterator<RailWayLWGeom> {
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
    public Map<String, Integer> sourceFields = null;
    private Map<String, RailwayPlatform> platformMap;


    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * @param connection
     * @param tableTrackGeometry Track geometry and metadata
     * @param tableTrainTraffic Train traffic associated with tracks
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic) throws IOException {
        this.railway.setVehicleDataFile(RAILWAY_VEHICLES_CNOSSOS_JSON);
        this.railway.setTrainSetDataFile(RAILWAY_TRAINSETS_JSON);
        this.railway.setRailwayDataFile(RAILWAY_EMISSION_CNOSSOS_JSON);
        this.setPlatformDataFile(RAILWAY_PLATFORMS_JSON);
        this.connection = connection;
        this.tableTrackGeometry = tableTrackGeometry;
        this.tableTrainTraffic = tableTrainTraffic;
        railWayLWComplete = fetchNext(railWayLWIncomplete);
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
                Tuple<String, Integer> trackKey = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection,
                        TableLocation.parse(tableTrackGeometry, DBUtils.getDBType(connection)));
                spatialResultSet = connection.createStatement().executeQuery(
                        "SELECT r1."+trackKey.first()+" trackid, r1.*, r2.* FROM " + tableTrackGeometry + " r1, " +
                                tableTrainTraffic + " r2 WHERE r1.IDSECTION=R2.IDSECTION ORDER BY R1." + trackKey.first())
                        .unwrap(SpatialResultSet.class);
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
                incompleteRecord.cref = incompleteRecord.railWayLWDay.getCref();
                incompleteRecord.nbTrack = spatialResultSet.getInt("NTRACK");
                incompleteRecord.idSection = spatialResultSet.getString("IDSECTION");
                incompleteRecord.platform = readPlatform(spatialResultSet);
                // GS from explicit column, otherwise use platform g3 (ground factor between rails)
                if (hasColumn(spatialResultSet, "GS")) {
                    incompleteRecord.gs = spatialResultSet.getDouble("GS");
                } else {
                    incompleteRecord.gs = incompleteRecord.platform.g3;
                }
                incompleteRecord.pk = spatialResultSet.getInt("trackid");
                incompleteRecord.geometry = splitGeometry(spatialResultSet.getGeometry());
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
                    // railWayLWIncomplete is complete — sync cref from accumulated emissions
                    incompleteRecord.cref = incompleteRecord.railWayLWDay.getCref();
                    completeRecord = new RailWayLWGeom(incompleteRecord);
                    // read next (incomplete) instance attributes for the next() call
                    incompleteRecord.geometry = splitGeometry(spatialResultSet.getGeometry());
                    if (sourceFields.containsKey("TRACKSPC")) {
                        incompleteRecord.distance = spatialResultSet.getDouble("TRACKSPC");
                    }
                    // initialize incomplete record
                    incompleteRecord.setRailWayLW(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    incompleteRecord.setRailWayLWDay(getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    incompleteRecord.setRailWayLWEvening(getRailwayEmissionFromResultSet(spatialResultSet, "EVENING"));
                    incompleteRecord.setRailWayLWNight(getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT"));
                    incompleteRecord.cref = incompleteRecord.railWayLWDay.getCref();
                    incompleteRecord.nbTrack = spatialResultSet.getInt("NTRACK");
                    incompleteRecord.idSection = spatialResultSet.getString("IDSECTION");
                    incompleteRecord.platform = readPlatform(spatialResultSet);
                    // GS from explicit column, otherwise use platform g3 (ground factor between rails)
                    if (hasColumn(spatialResultSet, "GS")) {
                        incompleteRecord.gs = spatialResultSet.getDouble("GS");
                    } else {
                        incompleteRecord.gs = incompleteRecord.platform.g3;
                    }
                    incompleteRecord.pk = spatialResultSet.getInt("trackid");
                    incompleteRecord.geometry = splitGeometry(spatialResultSet.getGeometry());
                    break;
                }
            }
            if(!hasNext) {
                incompleteRecord.pk = -1;
            } else {
                if (completeRecord == null) {
                    // Sync cref from accumulated emissions before creating final record
                    incompleteRecord.cref = incompleteRecord.railWayLWDay.getCref();
                    completeRecord = new RailWayLWGeom(incompleteRecord);
                }
            }
            return completeRecord;
        } catch (SQLException | IOException throwables) {
            throw new NoSuchElementException(throwables.getMessage());
        }
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
            trackTransferStr = rs.getString("TRANSFER");
        }
        if (sourceFields.containsKey("ROUGHNESS")) {
            railRoughnessStr = rs.getString("ROUGHNESS");
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

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransferStr, railRoughnessStr,
                impactNoiseStr, bridgeTransfertStr, curvature, commercialSpeed, isTunnel, nbTrack);

        Map<String, Integer> vehicles = railway.getVehicleFromTrainset(train);
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

        }else if (railway.isInVehicleList(train)){
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


