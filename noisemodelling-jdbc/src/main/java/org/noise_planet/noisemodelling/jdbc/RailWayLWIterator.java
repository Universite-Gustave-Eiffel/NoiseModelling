package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.noise_planet.noisemodelling.emission.EvaluateRailwaySourceCnossos;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.emission.RailwayTrackParametersCnossos;
import org.noise_planet.noisemodelling.emission.RailwayVehicleParametersCnossos;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class RailWayLWIterator implements Iterator<RailWayLW> {

    private Connection connection;
    private RailWayLW railWayLW;
    private RailWayLW railWayLWsum;
    private RailWayLWGeom railWayLWfinal = new RailWayLWGeom();
    private String tableTrain;
    private String tableTrack;
    private int nbTrack = 1;
    private LDENConfig ldenConfig;
    private SpatialResultSet spatialResultSet;
    private int currentIdSection = -1;

    public Map<String, Integer> sourceFields = null;

    public List<Geometry> getParrallelLines(Coordinate[] coordinates){
        GeometryFactory geometryFactory = new GeometryFactory();

        LineMerger plLinesLeft = new LineMerger();
        LineMerger plLinesRight = new LineMerger();
        List<Geometry> geometries = new ArrayList<>();

        boolean left = true;
        for (int i =0;i<coordinates.length-1;i++){

            Coordinate[] coordinates1 = new Coordinate[]{coordinates[i], coordinates[i+1]};
            Geometry linestring = geometryFactory.createLineString(coordinates1);
            if (!linestring.intersects(railWayLWfinal.getGeometry())){
                if (left) {
                    plLinesLeft.add(linestring);
                }else{
                    plLinesRight.add(linestring);
                }
            }else{
                left = !left;
            }

        }
        geometries.addAll(plLinesLeft.getMergedLineStrings());
        geometries.addAll(plLinesRight.getMergedLineStrings());
        return geometries;
    }

    public List<Geometry> getRailWayLWGeometry( double distance) {
        List<Geometry> geometries = new ArrayList<>();

        boolean even = false;
        if (nbTrack % 2 == 0) even = true;

        if (nbTrack == 1) {
            geometries.add(railWayLWfinal.getGeometry());
            return geometries;
        }else {
            BufferParameters bufferParameters = new BufferParameters();
            bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
            BufferOp bufOp = new BufferOp(railWayLWfinal.getGeometry(), bufferParameters);

            if (even) {
                for (int j=1; j <= nbTrack/2 ; j++){
                    Coordinate[] coordinates = bufOp.getResultGeometry( (distance/2) +distance* j).getCoordinates();
                    geometries.addAll(getParrallelLines(coordinates));
                }
            } else {
                for (int j=1; j <= ((nbTrack-1)/2) ; j++) {
                    Coordinate[] coordinates = bufOp.getResultGeometry(distance*j).getCoordinates();
                    geometries.addAll(getParrallelLines(coordinates));
                }
                LineMerger centerLine = new LineMerger();
                centerLine.add(railWayLWfinal.getGeometry());
                geometries.addAll(centerLine.getMergedLineStrings());
            }
            return geometries;
        }
    }

    public RailWayLW getRailWayLW() {
        return railWayLWfinal.getRailWayLW();
    }

    public int getRailWayLWPK() {
        return railWayLWfinal.getPK();
    }


    public RailWayLWIterator(Connection connection, String tableTrain, String tableTrack, LDENConfig ldenConfig, int nbTrack) {
        this.connection = connection;
        this.tableTrain = tableTrain;
        this.tableTrack = tableTrack;
        this.ldenConfig = ldenConfig;
        this.nbTrack = nbTrack;
    }

    @Override
    public boolean hasNext() {
        return railWayLW != null;
    }

    @Override
    public RailWayLWGeom next() {
        try {
            if (spatialResultSet == null) {
                spatialResultSet = connection.createStatement().executeQuery("SELECT r1.*, r2.* FROM "+tableTrain+" r1, "+tableTrack+" r2 WHERE r1.IDSECTION= R2.IDSECTION; ").unwrap(SpatialResultSet.class);
                spatialResultSet.next();
                railWayLW = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                railWayLWsum = railWayLW;
                currentIdSection = spatialResultSet.getInt(1);
            }
            while (spatialResultSet.next()) {
                if (currentIdSection == spatialResultSet.getInt(1)) {
                    railWayLWsum = RailWayLW.sumRailWayLW(railWayLWsum,railWayLW);
                } else {
                    railWayLWfinal.setRailWayLW(railWayLWsum);
                    railWayLWfinal.setGeometry(spatialResultSet.getGeometry());
                    railWayLWfinal.setPK(spatialResultSet.getInt("PK"));
                    railWayLW = getRailwayEmissionFromResultSet(spatialResultSet, "DAY");
                    railWayLWsum = railWayLW;
                    currentIdSection = spatialResultSet.getInt(1);
                    return railWayLWfinal;
                }

            }
        } catch (SQLException | IOException throwables) {
            throw new NoSuchElementException(throwables.getMessage());
        }
        return null;
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
        int vehiclePerHour = 1;
        int rollingCondition = 0;
        double idlingTime = 0;
        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeTransfert = 0;
        int curvature = 0;
        int railRoughness = 4;
        double vMaxInfra = 160;
        double vehicleCommercial = 160;

        // Read fields
        if (sourceFields.containsKey("SPEEDVEHIC")) {
            vehicleSpeed = rs.getDouble(sourceFields.get("SPEEDVEHIC"));
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
        if (sourceFields.containsKey("TRACKTRANS")) {
            trackTransfer = rs.getInt(sourceFields.get("TRACKTRANS"));
        }
        if (sourceFields.containsKey("RAILROUGHN")) {
            railRoughness = rs.getInt(sourceFields.get("RAILROUGHN"));
        }

        if (sourceFields.containsKey("IMPACTNOIS")) {
            impactNoise = rs.getInt(sourceFields.get("IMPACTNOIS"));
        }
        if (sourceFields.containsKey("BRIDGETRAN")) {
            bridgeTransfert = rs.getInt(sourceFields.get("BRIDGETRAN"));
        }
        if (sourceFields.containsKey("CURVATURE")) {
            curvature = rs.getInt(sourceFields.get("CURVATURE"));
        }

        if (sourceFields.containsKey("SPEEDTRACK")) {
            vMaxInfra = rs.getDouble(sourceFields.get("SPEEDTRACK"));
        }
        if (sourceFields.containsKey("SPEEDCOMME")) {
            vehicleCommercial = rs.getDouble(sourceFields.get("SPEEDCOMME"));
        }
        if (sourceFields.containsKey("TYPETRAIN")) {
            typeTrain = rs.getString(sourceFields.get("TYPETRAIN"));
        }

        EvaluateRailwaySourceCnossos evaluateRailwaySourceCnossos = new EvaluateRailwaySourceCnossos();
        RailWayLW  lWRailWay = new RailWayLW();
        RailwayTrackParametersCnossos trackParameters = new RailwayTrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial);

        Map<String, Integer> vehicles = evaluateRailwaySourceCnossos.getVehicleFromTrain(typeTrain);
        if (vehicles!=null){
            int i = 0;
            for (Map.Entry<String,Integer> entry : vehicles.entrySet()){
                typeTrain = entry.getKey();
                vehiclePerHour = vehiclePerHour * entry.getValue();
                RailwayVehicleParametersCnossos vehicleParameters = new RailwayVehicleParametersCnossos(typeTrain, vehicleSpeed,
                        vehiclePerHour/nbTrack, rollingCondition, idlingTime);

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
                    vehiclePerHour/nbTrack, rollingCondition, idlingTime);
             lWRailWay = evaluateRailwaySourceCnossos.evaluate(vehicleParameters, trackParameters);
        }

        return lWRailWay;
    }
}


class RailWayLWGeom extends RailWayLW {
    private RailWayLW railWayLW;
    private Geometry geometry;
    private int pk;

    public RailWayLW getRailWayLW() {
        return railWayLW;
    }

    public void setRailWayLW(RailWayLW railWayLW) {
        this.railWayLW = railWayLW;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public int getPK() {
        return pk;
    }

    public int setPK(int pk) {
        return this.pk;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
