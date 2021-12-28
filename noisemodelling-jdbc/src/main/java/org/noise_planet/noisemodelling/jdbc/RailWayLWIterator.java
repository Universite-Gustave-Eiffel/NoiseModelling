package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
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
    private RailWayLWGeom railWayLWfinal = new RailWayLWGeom();
    private String tableTrain;
    private String tableTrack;

    private int nbTrack = 1;
    private LDENConfig ldenConfig;
    private SpatialResultSet spatialResultSet;
    private int currentIdSection = -1;
    public double distance = 2;
    public Map<String, Integer> sourceFields = null;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }


    public RailWayLWIterator(Connection connection, String tableTrain, String tableTrack, LDENConfig ldenConfig) {
        this.connection = connection;
        this.tableTrain = tableTrain;
        this.tableTrack = tableTrack;
        this.ldenConfig = ldenConfig;
    }

    @Override
    public boolean hasNext() {
        return railWayLWfinal != null;
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
        try {
            if (spatialResultSet == null) {
                spatialResultSet = connection.createStatement().executeQuery("SELECT r1.*, r2.* FROM " + tableTrain + " r1, " + tableTrack + " r2 WHERE r1.IDSECTION= R2.IDSECTION ; ").unwrap(SpatialResultSet.class);
                spatialResultSet.next();

                railWayLWsum = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                railWayLWsumDay = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                railWayLWsumEvening = getRailwayEmissionFromResultSet(spatialResultSet, "EVENING");
                railWayLWsumNight = getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT");

                railWayLWfinal.setNbTrack(spatialResultSet.getInt("NTRACK"));
                if (hasColumn(spatialResultSet, "GS")) railWayLWfinal.setGs(spatialResultSet.getDouble("GS"));

                currentIdSection = spatialResultSet.getInt("PK");
                railWayLWfinal.setPK(currentIdSection);
                railWayLWfinal.setGeometry(splitGeometry(spatialResultSet.getGeometry()));
            }
            while (spatialResultSet.next()) {
                if (currentIdSection == spatialResultSet.getInt("PK")) {
                    railWayLWsum = RailWayLW.sumRailWayLW(railWayLWsum, getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    railWayLWsumDay = RailWayLW.sumRailWayLW(railWayLWsumDay, getRailwayEmissionFromResultSet(spatialResultSet, "DAY"));
                    railWayLWsumEvening = RailWayLW.sumRailWayLW(railWayLWsumEvening, getRailwayEmissionFromResultSet(spatialResultSet, "EVENING"));
                    railWayLWsumNight = RailWayLW.sumRailWayLW(railWayLWsumNight, getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT"));
                } else {
                    railWayLWfinal.setRailWayLW(railWayLWsum);
                    railWayLWfinal.setRailWayLWDay(railWayLWsumDay);
                    railWayLWfinal.setRailWayLWEvening(railWayLWsumEvening);
                    railWayLWfinal.setRailWayLWNight(railWayLWsumNight);
                    RailWayLWGeom previousRailWayLW = railWayLWfinal;
                    railWayLWfinal = new RailWayLWGeom();
                    railWayLWfinal.setGeometry(splitGeometry(spatialResultSet.getGeometry()));
                    railWayLWfinal.setPK(spatialResultSet.getInt("PK"));
                    railWayLWfinal.setNbTrack(spatialResultSet.getInt("NTRACK"));
                    if (hasColumn(spatialResultSet, "GS")) railWayLWfinal.setGs(spatialResultSet.getDouble("GS"));
                    railWayLWsum = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                    railWayLWsumDay = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                    railWayLWsumEvening = getRailwayEmissionFromResultSet(spatialResultSet, "EVENING");
                    railWayLWsumNight = getRailwayEmissionFromResultSet(spatialResultSet, "NIGHT");
                    railWayLWfinal.setRailWayLW(railWayLWsum);
                    railWayLWfinal.setRailWayLWDay(railWayLWsumDay);
                    railWayLWfinal.setRailWayLWEvening(railWayLWsumEvening);
                    railWayLWfinal.setRailWayLWNight(railWayLWsumNight);
                    currentIdSection = spatialResultSet.getInt("PK");
                    return previousRailWayLW;
                }
            }

            RailWayLWGeom previousRailWayLW = railWayLWfinal;
            railWayLWfinal=null;
            return previousRailWayLW;

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
            for (String fieldName : JDBCUtilities.getFieldNames(rs.getMetaData())) {
                sourceFields.put(fieldName.toUpperCase(), fieldId++);
            }
        }
        double[] lvl = new double[ldenConfig.propagationProcessPathData.freq_lvl.size()];

        String typeTrain = "FRET";
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

        // Read fields
        if (sourceFields.containsKey("TRAINSPD")) {
            vehicleSpeed = rs.getDouble(sourceFields.get("TRAINSPD"));
        }
        if (sourceFields.containsKey("T" + period)) {
            vehiclePerHour = rs.getInt(sourceFields.get("T" + period));
        }
        if (sourceFields.containsKey("ROLLINGCONDITION")) {
            rollingCondition = rs.getInt(sourceFields.get("ROLLINGCONDITION"));
        }
        if (sourceFields.containsKey("IDLINGTIME")) {
            idlingTime = rs.getDouble(sourceFields.get("IDLINGTIME"));
        }
        if (sourceFields.containsKey("TRANSFER")) {
            trackTransfer = rs.getInt(sourceFields.get("TRANSFER"));
        }
        if (sourceFields.containsKey("ROUGHNESS")) {
            railRoughness = rs.getInt(sourceFields.get("ROUGHNESS"));
        }

        if (sourceFields.containsKey("IMPACT")) {
            impactNoise = rs.getInt(sourceFields.get("IMPACT"));
        }
        if (sourceFields.containsKey("BRIDGE")) {
            bridgeTransfert = rs.getInt(sourceFields.get("BRIDGE"));
        }
        if (sourceFields.containsKey("CURVATURE")) {
            curvature = rs.getInt(sourceFields.get("CURVATURE"));
        }

        if (sourceFields.containsKey("TRACKSPD")) {
            vMaxInfra = rs.getDouble(sourceFields.get("TRACKSPD"));
        }
        if (sourceFields.containsKey("TRACKSPC")) {
            trackSpacing = rs.getDouble(sourceFields.get("TRACKSPC"));
            setDistance(trackSpacing);
        }

        if (sourceFields.containsKey("COMSPD")) {
            commercialSpeed = rs.getDouble(sourceFields.get("COMSPD"));
        }
        if (sourceFields.containsKey("TRAINTYPE")) {
            typeTrain = rs.getString(sourceFields.get("TRAINTYPE"));
        }

        if (sourceFields.containsKey("ISTUNNEL")) {
            isTunnel = rs.getBoolean(sourceFields.get("ISTUNNEL"));
        }

        if (sourceFields.containsKey("IDTUNNEL")) {

            if (rs.getString(sourceFields.get("IDTUNNEL")) ==  null) {
                isTunnel = false;
            } else {
                isTunnel = !rs.getString(sourceFields.get("IDTUNNEL")).isEmpty();
            }
        }

        if (sourceFields.containsKey("NTRACK")) {
            nbTrack = rs.getInt(sourceFields.get("NTRACK"));
        }




        RailWayLW  lWRailWay = new RailWayLW();

        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, commercialSpeed, isTunnel, nbTrack);

        Map<String, Integer> vehicles = evaluateRailwaySourceCnossos.getVehicleFromTrain(typeTrain);

        if (vehicles!=null){
            int i = 0;
            for (Map.Entry<String,Integer> entry : vehicles.entrySet()){
                typeTrain = entry.getKey();
                vehiclePerHour = vehiclePerHour * entry.getValue();
                RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(typeTrain, vehicleSpeed,
                        vehiclePerHour/(double) nbTrack, rollingCondition, idlingTime);

                if (i==0){
                    lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
                }
                else {
                    lWRailWay = RailWayLW.sumRailWayLW(lWRailWay, evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters));
                }
                i++;
            }

        }else if (evaluateRailwaySourceCnossos.isInVehicleList(typeTrain)){
            RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(typeTrain, vehicleSpeed,
                    vehiclePerHour/(double)nbTrack, rollingCondition, idlingTime);
            lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
        }

        return lWRailWay;
    }


    public class RailWayLWGeom {
        private RailWayLW railWayLW;
        private RailWayLW railWayLWDay;
        private RailWayLW railWayLWEvening;
        private RailWayLW railWayLWNight;
        private List<LineString> geometry;
        private int pk;
        private int nbTrack;

        public double getGs() {
            return gs;
        }

        public void setGs(double gs) {
            this.gs = gs;
        }

        private double gs;


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


