package org.noise_planet.noisemodelling.jdbc.utils;

import org.h2gis.functions.spatial.convert.ST_Force3D;
import org.h2gis.functions.spatial.edit.ST_UpdateZ;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.jdbc.LDENConfig;
import org.noise_planet.noisemodelling.jdbc.RailWayLWIterator;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Functions to generate Sound source table from traffic tables
 */
public class MakeLWTable {

    /**
     * Generate Train emission from train geometry tracks and train traffic
     * @param connection
     * @param railSectionTableName
     * @param railTrafficTableName
     * @param outputTable
     * @throws SQLException
     */
    public static void makeTrainLWTable(Connection connection, String railSectionTableName, String railTrafficTableName, String outputTable) throws SQLException {

        // drop table LW_RAILWAY if exists and the create and prepare the table
        connection.createStatement().execute("drop table if exists " + outputTable);

        // Build and execute queries
        StringBuilder createTableQuery = new StringBuilder("create table "+outputTable+" (ID_SECTION int," +
                " the_geom GEOMETRY, DIR_ID int");
        StringBuilder insertIntoQuery = new StringBuilder("INSERT INTO "+outputTable+"(ID_SECTION, the_geom," +
                " DIR_ID");
        StringBuilder insertIntoValuesQuery = new StringBuilder("?,?,?");
        for(int thirdOctave : CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWD");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWD");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWE");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWE");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }
        for(int thirdOctave : CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
            createTableQuery.append(", LWN");
            createTableQuery.append(thirdOctave);
            createTableQuery.append(" double precision");
            insertIntoQuery.append(", LWN");
            insertIntoQuery.append(thirdOctave);
            insertIntoValuesQuery.append(", ?");
        }

        createTableQuery.append(")");
        insertIntoQuery.append(") VALUES (");
        insertIntoQuery.append(insertIntoValuesQuery);
        insertIntoQuery.append(")");
        connection.createStatement().execute(createTableQuery.toString());

        // Get Class to compute LW
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_RAILWAY_FLOW);
        ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData());
        ldenConfig.setCoefficientVersion(2);
        ldenConfig.setExportRays(true);
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,railSectionTableName, railTrafficTableName, ldenConfig);

        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom railWayLWGeom = railWayLWIterator.next();

            RailWayLW railWayLWDay = railWayLWGeom.getRailWayLWDay();
            RailWayLW railWayLWEvening = railWayLWGeom.getRailWayLWEvening();
            RailWayLW railWayLWNight = railWayLWGeom.getRailWayLWNight();
            List<LineString> geometries = railWayLWGeom.getRailWayLWGeometry();

            int pk = railWayLWGeom.getPK();
            double[] LWDay = new double[0];
            double[] LWEvening = new double[0];
            double[] LWNight = new double[0];
            double heightSource = 0;
            int directivityId = 0;
            for (int iSource = 0; iSource < 6; iSource++) {
                switch (iSource) {
                    case 0:
                        LWDay = railWayLWDay.getLWRolling();
                        LWEvening = railWayLWEvening.getLWRolling();
                        LWNight = railWayLWNight.getLWRolling();
                        heightSource = 0.5;
                        directivityId = 1;
                        break;
                    case 1:
                        LWDay = railWayLWDay.getLWTractionA();
                        LWEvening = railWayLWEvening.getLWTractionA();
                        LWNight = railWayLWNight.getLWTractionA();
                        heightSource = 0.5;
                        directivityId = 2;
                        break;
                    case 2:
                        LWDay = railWayLWDay.getLWTractionB();
                        LWEvening = railWayLWEvening.getLWTractionB();
                        LWNight = railWayLWNight.getLWTractionB();
                        heightSource = 4;
                        directivityId = 3;
                        break;
                    case 3:
                        LWDay = railWayLWDay.getLWAerodynamicA();
                        LWEvening = railWayLWEvening.getLWAerodynamicA();
                        LWNight = railWayLWNight.getLWAerodynamicA();
                        heightSource = 0.5;
                        directivityId = 4;
                        break;
                    case 4:
                        LWDay = railWayLWDay.getLWAerodynamicB();
                        LWEvening = railWayLWEvening.getLWAerodynamicB();
                        LWNight = railWayLWNight.getLWAerodynamicB();
                        heightSource = 4;
                        directivityId = 5;
                        break;
                    case 5:
                        LWDay = railWayLWDay.getLWBridge();
                        LWEvening = railWayLWEvening.getLWBridge();
                        LWNight = railWayLWNight.getLWBridge();
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
