package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.Tuple;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.noise_planet.noisemodelling.emission.EvaluateRailwaySourceCnossos;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.emission.RailwayTrackParametersCnossos;
import org.noise_planet.noisemodelling.emission.RailwayVehicleParametersCnossos;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static org.noise_planet.noisemodelling.jdbc.MakeParallelLines.MakeParallelLine;



public class RailWayLWIterator implements Iterator<RailWayLWIterator.RailWayLWGeom> {
    private final EvaluateRailwaySourceCnossos evaluateRailwaySourceCnossos = new EvaluateRailwaySourceCnossos();
    private Connection connection;
    private RailWayLW railWayLWsum;
    private RailWayLW railWayLWsumDay;
    private RailWayLW railWayLWsumEvening;
    private RailWayLW railWayLWsumNight;
    private RailWayLWGeom railWayLWCurrent = null;
    private String tableTrackGeometry;
    private String tableTrainTraffic;
    private int nbTrack;
    private String idSection ="";
    private int currentIdTrack = -1;
    List<LineString> railWayGeoms;
    double gs = 1.0;
    private SpatialResultSet spatialResultSet;
    public double distance = 2;
    public Map<String, Integer> sourceFields = null;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }


    /**
     * Generate sound source for train (with train source directivity) from traffic and geometry tracks tables
     * @param connection
     * @param tableTrackGeometry Track geometry and metadata
     * @param tableTrainTraffic Train traffic associated with tracks
     */
    public RailWayLWIterator(Connection connection, String tableTrackGeometry, String tableTrainTraffic) {
        this.connection = connection;
        this.tableTrackGeometry = tableTrackGeometry;
        this.tableTrainTraffic = tableTrainTraffic;
        railWayLWCurrent = fetchNext();
    }

    @Override
    public boolean hasNext() {
        return railWayLWCurrent != null;
    }

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

    @Override
    public RailWayLWGeom next() {
        RailWayLWGeom current = railWayLWCurrent;
        railWayLWCurrent = fetchNext();
        return current;
    }

    public RailWayLWGeom current() {
        return railWayLWCurrent;
    }

    private RailWayLWGeom fetchNext() {
        try {
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

                railWayLWsum = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                railWayLWsumDay = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                railWayLWsumEvening = getRailwayEmissionFromResultSet(spatialResultSet, "EVENING");
                railWayLWsumNight = getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT");

                nbTrack = spatialResultSet.getInt("NTRACK");
                idSection = spatialResultSet.getString("IDSECTION");
                if (hasColumn(spatialResultSet, "GS")) {
                    gs = spatialResultSet.getDouble("GS");
                }

                currentIdTrack = spatialResultSet.getInt("trackid");
                railWayGeoms = splitGeometry(spatialResultSet.getGeometry());
            }
            if(currentIdTrack == -1) {
                return null;
            }
            RailWayLWGeom railWayLWNext = new RailWayLWGeom(railWayLWsum, railWayLWsumDay, railWayLWsumEvening, railWayLWsumNight, railWayGeoms, currentIdTrack, nbTrack, distance, gs);
            railWayLWNext.setIdSection(idSection);
            currentIdTrack = -1;
            while (spatialResultSet.next()) {
                if (railWayLWNext.pk == spatialResultSet.getInt("trackid")) {
                    railWayLWsum = RailWayLW.sumRailWayLW(railWayLWsum, getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    railWayLWsumDay = RailWayLW.sumRailWayLW(railWayLWsumDay, getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    railWayLWsumEvening = RailWayLW.sumRailWayLW(railWayLWsumEvening, getRailwayEmissionFromResultSet(spatialResultSet, "EVENING"));
                    railWayLWsumNight = RailWayLW.sumRailWayLW(railWayLWsumNight, getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT"));
                } else {
                    // finalize current value
                    railWayLWNext.setRailWayLW(railWayLWsum);
                    railWayLWNext.setRailWayLWDay(railWayLWsumDay);
                    railWayLWNext.setRailWayLWEvening(railWayLWsumEvening);
                    railWayLWNext.setRailWayLWNight(railWayLWsumNight);
                    railWayLWNext.setIdSection(idSection);
                    // read next instance attributes for the next() call
                    railWayGeoms = splitGeometry(spatialResultSet.getGeometry());
                    currentIdTrack = spatialResultSet.getInt("trackid");
                    nbTrack = spatialResultSet.getInt("NTRACK");
                    idSection = spatialResultSet.getString("IDSECTION");
                    if (hasColumn(spatialResultSet, "GS")) {
                        gs = spatialResultSet.getDouble("GS");
                    }
                    railWayLWsum = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                    railWayLWsumDay = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                    railWayLWsumEvening = getRailwayEmissionFromResultSet(spatialResultSet, "EVENING");
                    railWayLWsumNight = getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT");
                    break;
                }
            }
            return railWayLWNext;
        } catch (SQLException | IOException throwables) {
            throw new NoSuchElementException(throwables.getMessage());
        }
    }

    /**
     * @param rs     result set of source
     * @param period D or E or N
     * @return Emission spectrum in dB
     */
    public RailWayLW getRailwayEmissionFromResultSet(ResultSet rs, String period) throws SQLException, IOException {

        if (sourceFields == null) {
            sourceFields = new HashMap<>();
            int fieldId = 1;
            for (String fieldName : JDBCUtilities.getColumnNames(rs.getMetaData())) {
                sourceFields.put(fieldName.toUpperCase(), fieldId++);
            }
        }

        String train = "FRET";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int rollingCondition = 0;
        double idlingTime = 0;
        int trackTransfer = 4;
        double trackSpacing = 1.0;
        int impactNoise = 0;
        int bridgeTransfert = 0;
        int curvature = 0;
        int railRoughness = 1;
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
        if (sourceFields.containsKey("TRACKSPC")) {
            trackSpacing = rs.getDouble("TRACKSPC");
            setDistance(trackSpacing);
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


        RailWayLW  lWRailWay = new RailWayLW();

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, commercialSpeed, isTunnel, nbTrack);

        Map<String, Integer> vehicles = evaluateRailwaySourceCnossos.getVehicleFromTrain(train);

        if (vehicles!=null){
            int i = 0;
            for (Map.Entry<String,Integer> entry : vehicles.entrySet()){
                String typeTrain = entry.getKey();
                vehiclePerHour = vehiclePerHour * entry.getValue();
                if (vehiclePerHour>0) {
                    RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(typeTrain, vehicleSpeed,
                            vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);

                    if (i == 0) {
                        lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
                    } else {
                        lWRailWay = RailWayLW.sumRailWayLW(lWRailWay, evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters));
                    }
                }
                i++;
            }

        }else if (evaluateRailwaySourceCnossos.isInVehicleList(train)){
            if (vehiclePerHour>0) {
                RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(train, vehicleSpeed,
                        vehiclePerHour / (double) nbTrack, rollingCondition, idlingTime);
                lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
            }
        }

        return lWRailWay;
    }


    public static class RailWayLWGeom {
        private RailWayLW railWayLW;
        private RailWayLW railWayLWDay;
        private RailWayLW railWayLWEvening;
        private RailWayLW railWayLWNight;
        private List<LineString> geometry;
        private int pk;
        private int nbTrack;
        private String idSection;
        private double distance;

        public RailWayLWGeom(RailWayLW railWayLW, RailWayLW railWayLWDay, RailWayLW railWayLWEvening, RailWayLW railWayLWNight, List<LineString> geometry, int pk, int nbTrack, double distance, double gs) {
            this.railWayLW = railWayLW;
            this.railWayLWDay = railWayLWDay;
            this.railWayLWEvening = railWayLWEvening;
            this.railWayLWNight = railWayLWNight;
            this.geometry = geometry;
            this.pk = pk;
            this.nbTrack = nbTrack;
            this.distance = distance;
            this.gs = gs;
        }

        public double getGs() {
            return gs;
        }

        public void setGs(double gs) {
            this.gs = gs;
        }

        private double gs;

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public RailWayLW getRailWayLW() {
            return railWayLW;
        }

        public void setRailWayLW(RailWayLW railWayLW) {
            this.railWayLW = railWayLW;
        }
        public RailWayLW getRailWayLWDay() {
            return railWayLWDay;
        }

        public void setRailWayLWDay(RailWayLW railWayLWDay) {
            this.railWayLWDay = railWayLWDay;
        }
        public RailWayLW getRailWayLWEvening() {
            return railWayLWEvening;
        }

        public void setRailWayLWEvening(RailWayLW railWayLWEvening) {
            this.railWayLWEvening = railWayLWEvening;
        }
        public RailWayLW getRailWayLWNight() {
            return railWayLWNight;
        }

        public void setRailWayLWNight(RailWayLW railWayLWNight) {
            this.railWayLWNight = railWayLWNight;
        }

        public int getNbTrack() {
            return nbTrack;
        }

        public String getIdSection() {
            return idSection;
        }

        public void setIdSection(String idSection) {
            this.idSection = idSection;
        }
        public void setNbTrack(int nbTrack) {
            this.nbTrack = nbTrack;
        }

        public List<LineString> getGeometry() {
            return  geometry;
        }


        public int getPK() {
            return pk;
        }

        public int setPK(int pk) {
            return this.pk=pk;
        }

        public void setGeometry(List<LineString> geometry) {
            this.geometry = geometry;
        }



        public List<LineString> getRailWayLWGeometry() {
            List<LineString> geometries = new ArrayList<>();


            boolean even = false;
            if (nbTrack % 2 == 0) even = true;

            if (nbTrack == 1) {
                geometries.addAll(getGeometry());
                return geometries;
            }else {

                if (even) {
                    for (int j=0; j < nbTrack/2 ; j++){
                        for (LineString subGeom : getGeometry()) {
                            geometries.add( MakeParallelLine(subGeom, ( distance / 2) + distance * j));
                            geometries.add(MakeParallelLine(subGeom, -((distance / 2) + distance * j)));
                        }
                    }
                } else {
                    for (int j=1; j <= ((nbTrack-1)/2) ; j++) {
                        for (LineString subGeom : getGeometry()) {
                            geometries.add( MakeParallelLine(subGeom,  distance * j));
                            geometries.add(MakeParallelLine(subGeom, -( distance * j)));
                        }
                    }
                    LineMerger centerLine = new LineMerger();
                    centerLine.add(getGeometry());
                    geometries.addAll(centerLine.getMergedLineStrings());
                }
                return geometries;
            }
        }

    }
}


