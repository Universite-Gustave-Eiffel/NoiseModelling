package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.functions.spatial.convert.ST_Force3D;
import org.h2gis.functions.spatial.edit.ST_UpdateZ;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.railway.RailWayParameters;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossos;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossosParameters;
import org.noise_planet.noisemodelling.emission.utils.Utils;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWGeom;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.dBToW;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumArray;

/**
 * Create emission table from traffic data (RAIL or ROADS)
 */
public class EmissionTableGenerator {
    public static final List<Integer> roadOctaveFrequencyBands = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public static final String DEN_PERIOD = "DEN";

    public enum STANDARD_PERIOD {DAY, EVENING, NIGHT}
    public static final String[] STANDARD_PERIOD_VALUE = new String[] {"D", "E", "N"};

    public static final double DAY_RATIO = 12. / 24.;
    public static final double EVENING_RATIO = 4. / 24. * dBToW(5.0);
    public static final double NIGHT_RATIO = 8. / 24. * dBToW(10.0);
    public static final double[] RATIOS = new double[] {DAY_RATIO, EVENING_RATIO, NIGHT_RATIO};

    /**
     * Cache table fields in upper case in Map
     * @param sourceFieldsCache map
     * @param rs table to load
     * @throws SQLException If error
     */
    public static void cacheFields(Map<String, Integer> sourceFieldsCache, ResultSet rs) throws SQLException {
        if (sourceFieldsCache.isEmpty()) {
            int fieldId = 1;
            for (String fieldName : JDBCUtilities.getColumnNames(rs.getMetaData())) {
                sourceFieldsCache.put(fieldName.toUpperCase(), fieldId++);
            }
        }
    }
    /**
     * Retrieves the emissions for the specified period from the given result set
     * @param rs result set of source
     * @param period optional column name to add after attribute fields _D or _E or _N
     * @param slope Default, gradient percentage of road from -12 % to 12 %
     * @param coefficientVersion Cnossos coefficient version  (1 = 2015, 2 = 2020)
     * @return Emission spectrum in dB
     */
    public static double[] getEmissionFromTrafficTable(ResultSet rs, String period, double slope, int coefficientVersion, Map<String, Integer> sourceFieldsCache) throws SQLException {
        cacheFields(sourceFieldsCache, rs);
        // Set default values
        double tv = 0; // old format "total vehicles"
        double hv = 0; // old format "heavy vehicles"
        double lv_speed = 0;
        double mv_speed = 0;
        double hgv_speed = 0;
        double wav_speed = 0;
        double wbv_speed = 0;
        double lvPerHour = 0;
        double mvPerHour = 0;
        double hgvPerHour = 0;
        double wavPerHour = 0;
        double wbvPerHour = 0;
        double temperature = 20.0;
        String roadSurface = "NL08";
        double tsStud = 0;
        double pmStud = 0;
        double junctionDistance = 100; // no acceleration of deceleration changes with dist >= 100
        int junctionType = 2;
        int way = 3; // default value 2-way road

        // Read fields
        if(sourceFieldsCache.containsKey("LV_SPD"+period)) {
            lv_speed = rs.getDouble(sourceFieldsCache.get("LV_SPD"+period));
        }
        if(sourceFieldsCache.containsKey("MV_SPD"+period)) {
            mv_speed = rs.getDouble(sourceFieldsCache.get("MV_SPD"+period));
        }
        if(sourceFieldsCache.containsKey("HGV_SPD"+period)) {
            hgv_speed = rs.getDouble(sourceFieldsCache.get("HGV_SPD"+period));
        }
        if(sourceFieldsCache.containsKey("WAV_SPD"+period)) {
            wav_speed = rs.getDouble(sourceFieldsCache.get("WAV_SPD"+period));
        }
        if(sourceFieldsCache.containsKey("WBV_SPD"+period)) {
            wbv_speed = rs.getDouble(sourceFieldsCache.get("WBV_SPD"+period));
        }
        if(sourceFieldsCache.containsKey("LV"+period)) {
            lvPerHour = rs.getDouble(sourceFieldsCache.get("LV"+period));
        }
        if(sourceFieldsCache.containsKey("MV"+period)) {
            mvPerHour = rs.getDouble(sourceFieldsCache.get("MV"+period));
        }
        if(sourceFieldsCache.containsKey("HGV"+period)) {
            hgvPerHour = rs.getDouble(sourceFieldsCache.get("HGV"+period));
        }
        if(sourceFieldsCache.containsKey("WAV"+period)) {
            wavPerHour = rs.getDouble(sourceFieldsCache.get("WAV"+period));
        }
        if(sourceFieldsCache.containsKey("WBV"+period)) {
            wbvPerHour = rs.getDouble(sourceFieldsCache.get("WBV"+period));
        }
        if(sourceFieldsCache.containsKey("PVMT")) {
            roadSurface= rs.getString(sourceFieldsCache.get("PVMT"));
        }
        if(sourceFieldsCache.containsKey("TEMP"+period)) {
            temperature = rs.getDouble(sourceFieldsCache.get("TEMP"+period));
        }
        if(sourceFieldsCache.containsKey("TS_STUD")) {
            tsStud = rs.getDouble(sourceFieldsCache.get("TS_STUD"));
        }
        if(sourceFieldsCache.containsKey("PM_STUD")) {
            pmStud = rs.getDouble(sourceFieldsCache.get("PM_STUD"));
        }
        if(sourceFieldsCache.containsKey("JUNC_DIST")) {
            junctionDistance = rs.getDouble(sourceFieldsCache.get("JUNC_DIST"));
        }
        if(sourceFieldsCache.containsKey("JUNC_TYPE")) {
            junctionType = rs.getInt(sourceFieldsCache.get("JUNC_TYPE"));
        }

        if(sourceFieldsCache.containsKey("WAY")) {
            way = rs.getInt(sourceFieldsCache.get("WAY"));
        }

        if(sourceFieldsCache.containsKey("SLOPE")) {
            slope = rs.getDouble(sourceFieldsCache.get("SLOPE"));
        }else{
            way = 3;
        }


        // old fields
        if(sourceFieldsCache.containsKey("TV"+period)) {
            tv = rs.getDouble(sourceFieldsCache.get("TV"+period));
        }
        if(sourceFieldsCache.containsKey("HV"+period)) {
            hv = rs.getDouble(sourceFieldsCache.get("HV"+period));
        }
        if(sourceFieldsCache.containsKey("HV_SPD"+period)) {
            hgv_speed = rs.getDouble(sourceFieldsCache.get("HV_SPD"+period));
        }

        if(tv > 0) {
            lvPerHour = tv - (hv + mvPerHour + hgvPerHour + wavPerHour + wbvPerHour);
        }
        if(hv > 0) {
            hgvPerHour = hv;
        }
        // Compute emission
        double[] lvl = new double[roadOctaveFrequencyBands.size()];
        for (int idFreq = 0; idFreq < roadOctaveFrequencyBands.size(); idFreq++) {
            int freq = roadOctaveFrequencyBands.get(idFreq);
            RoadCnossosParameters rsParametersCnossos = new RoadCnossosParameters(lv_speed, mv_speed, hgv_speed, wav_speed,
                    wbv_speed, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, temperature,
                    roadSurface, tsStud, pmStud, junctionDistance, junctionType);
            rsParametersCnossos.setSlopePercentage(slope);
            rsParametersCnossos.setWay(way);
            rsParametersCnossos.setFileVersion(coefficientVersion);
            try {
                lvl[idFreq] = RoadCnossos.evaluate(rsParametersCnossos);
            } catch (IOException ex) {
                throw new SQLException(ex);
            }
        }
        return lvl;
    }

    /**
     * Computes the sound levels (Lw) for different periods based on the provided spatial result set.
     * @param rs Result set on a road record
     * @param coefficientVersion Cnossos coefficient version  (1 = 2015, 2 = 2020)
     * @param sourceFieldsCache SQL Fields cache
     * @return a two-dimensional array containing the sound levels (Ld, Le, Ln) for each frequency level.
     * @throws SQLException Exception while evaluating the lw
     */
    public static double[][] computeLw(SpatialResultSet rs, int coefficientVersion, Map<String, Integer> sourceFieldsCache) throws SQLException {
        double slope = getSlope(rs);
        // Day
        double[] ld = AcousticIndicatorsFunctions.dBToW(getEmissionFromTrafficTable(rs, "_D", slope, coefficientVersion, sourceFieldsCache));

        // Evening
        double[] le = AcousticIndicatorsFunctions.dBToW(getEmissionFromTrafficTable(rs, "_E", slope, coefficientVersion, sourceFieldsCache));

        // Night
        double[] ln = AcousticIndicatorsFunctions.dBToW(getEmissionFromTrafficTable(rs, "_N", slope, coefficientVersion, sourceFieldsCache));

        return new double[][] {ld, le, ln};
    }

    public static double getSlope(SpatialResultSet rs) {
        double slope = 0;
        try {
            Geometry g = rs.getGeometry();
            if(g != null && !g.isEmpty()) {
                Coordinate[] c = g.getCoordinates();
                if(c.length >= 2) {
                    double z0 = c[0].z;
                    double z1 = c[1].z;
                    if(!Double.isNaN(z0) && !Double.isNaN(z1)) {
                        slope = Utils.computeSlope(z0, z1, g.getLength());
                    }
                }
            }
        } catch (SQLException ex) {
            // ignore
        }
        return slope;
    }

    public static double getSlope(Geometry g) {
        double slope = 0;
        if(g != null && !g.isEmpty()) {
            Coordinate[] c = g.getCoordinates();
            if (c.length >= 2) {
                double z0 = c[0].z;
                double z1 = c[1].z;
                if (!Double.isNaN(z0) && !Double.isNaN(z1)) {
                    slope = Utils.computeSlope(z0, z1, g.getLength());
                }
            }
        }
        return slope;
    }

    /**
     * Generate Train emission from train geometry tracks and train traffic
     * @param connection
     * @param railSectionTableName
     * @param railTrafficTableName
     * @param outputTable
     * @throws SQLException
     */
    public static void makeTrainLWTable(Connection connection, String railSectionTableName, String railTrafficTableName, String outputTable, String frequencyPrepend) throws SQLException {

        // drop table LW_RAILWAY if exists and the create and prepare the table
        connection.createStatement().execute("drop table if exists " + outputTable);

        // Build and execute queries
        StringBuilder createTableQuery = new StringBuilder("create table "+outputTable+" (PK_SECTION int," +
                " the_geom GEOMETRY, DIR_ID int, GS double");
        StringBuilder insertIntoQuery = new StringBuilder("INSERT INTO "+outputTable+"(PK_SECTION, the_geom," +
                " DIR_ID, GS");
        StringBuilder insertIntoValuesQuery = new StringBuilder("?,?,?,?");
        for(int thirdOctave : ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", ").append(frequencyPrepend).append("D");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", ").append(frequencyPrepend).append("D");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", ").append(frequencyPrepend).append("E");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", ").append(frequencyPrepend).append("E");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", ").append(frequencyPrepend).append("N");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", ").append(frequencyPrepend).append("N");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }

        createTableQuery.append(")");
        insertIntoQuery.append(") VALUES (");
        insertIntoQuery.append(insertIntoValuesQuery);
        insertIntoQuery.append(")");
        connection.createStatement().execute(createTableQuery.toString());

        // Get Class to compute HZ
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,railSectionTableName, railTrafficTableName);

        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom railWayLWGeom = railWayLWIterator.next();

            RailWayParameters railWayLWDay = railWayLWGeom.getRailWayLWDay();
            RailWayParameters railWayLWEvening = railWayLWGeom.getRailWayLWEvening();
            RailWayParameters railWayLWNight = railWayLWGeom.getRailWayLWNight();
            List<LineString> geometries = railWayLWGeom.getRailWayLWGeometry();

            int pk = railWayLWGeom.getPK();
            double[] LWDay = new double[ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            double[] LWEvening = new double[ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            double[] LWNight = new double[ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            Arrays.fill(LWDay, -99.00);
            Arrays.fill(LWEvening, -99.00);
            Arrays.fill(LWNight, -99.00);
            double heightSource = 0;
            int directivityId = 0;
            boolean day = (!railWayLWDay.getRailwaySourceList().isEmpty());
            boolean evening = (!railWayLWEvening.getRailwaySourceList().isEmpty());
            boolean night = (!railWayLWNight.getRailwaySourceList().isEmpty());
            for (int iSource = 0; iSource < 6; iSource++) {

                heightSource = 0;
                switch (iSource) {
                    case 0:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("ROLLING").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("ROLLING").getlW();
                        if (night) LWNight = railWayLWNight.getRailwaySourceList().get("ROLLING").getlW();
                        if (day) heightSource = 4; //railWayLWDay.getRailwaySourceList().get("ROLLING").getSourceHeight();
                        directivityId = 1;
                        break;
                    case 1:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("TRACTIONA").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("TRACTIONA").getlW();
                        if (night) LWNight = railWayLWNight.getRailwaySourceList().get("TRACTIONA").getlW();
                        heightSource = 0.5;
                        directivityId = 2;
                        break;
                    case 2:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("TRACTIONB").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("TRACTIONB").getlW();
                        if (night) LWNight = railWayLWNight.getRailwaySourceList().get("TRACTIONB").getlW();
                        heightSource = 4;
                        directivityId = 3;
                        break;
                    case 3:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("AERODYNAMICA").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("AERODYNAMICA").getlW();
                        if (night)  LWNight = railWayLWNight.getRailwaySourceList().get("AERODYNAMICA").getlW();
                        heightSource = 0.5;
                        directivityId = 4;
                        break;
                    case 4:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("AERODYNAMICB").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("AERODYNAMICB").getlW();
                        if (night)  LWNight = railWayLWNight.getRailwaySourceList().get("AERODYNAMICB").getlW();
                        heightSource = 4;
                        directivityId = 5;
                        break;
                    case 5:
                        if (day) LWDay = railWayLWDay.getRailwaySourceList().get("BRIDGE").getlW();
                        if (evening) LWEvening = railWayLWEvening.getRailwaySourceList().get("BRIDGE").getlW();
                        if (night)  LWNight = railWayLWNight.getRailwaySourceList().get("BRIDGE").getlW();
                        heightSource = 0.5;
                        directivityId = 6;
                        break;
                }

                PreparedStatement ps = connection.prepareStatement(insertIntoQuery.toString());
                for (Geometry trackGeometry : geometries) {

                    Geometry sourceGeometry = ST_UpdateZ.updateZ(ST_Force3D.force3D(trackGeometry), heightSource).copy() ;

                    int cursor = 1;
                    ps.setInt(cursor++, pk);
                    ps.setObject(cursor++, sourceGeometry);
                    ps.setInt(cursor++, directivityId);
                    ps.setDouble(cursor++, railWayLWGeom.getGs());
                    for (double v : LWDay) {
                        ps.setDouble(cursor++, v);
                    }
                    for (double v : LWEvening) {
                        ps.setDouble(cursor++, v);
                    }
                    for (double v : LWNight) {
                        ps.setDouble(cursor++, v);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

        }

        // Add primary key to the LW table
        connection.createStatement().execute("ALTER TABLE "+outputTable+" ADD PK INT AUTO_INCREMENT PRIMARY KEY;");
    }


}
