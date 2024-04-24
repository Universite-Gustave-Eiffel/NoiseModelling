package org.noise_planet.noisemodelling.jdbc.utils;

import org.h2gis.functions.spatial.convert.ST_Force3D;
import org.h2gis.functions.spatial.edit.ST_UpdateZ;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.railway.RailWayParameters;
import org.noise_planet.noisemodelling.jdbc.RailWayLWIterator;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
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
        StringBuilder createTableQuery = new StringBuilder("create table "+outputTable+" (PK_SECTION int," +
                " the_geom GEOMETRY, DIR_ID int, GS double");
        StringBuilder insertIntoQuery = new StringBuilder("INSERT INTO "+outputTable+"(PK_SECTION, the_geom," +
                " DIR_ID, GS");
        StringBuilder insertIntoValuesQuery = new StringBuilder("?,?,?,?");
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
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,railSectionTableName, railTrafficTableName);

        while (railWayLWIterator.hasNext()) {
            RailWayLWIterator.RailWayLWGeom railWayLWGeom = railWayLWIterator.next();

            RailWayParameters railWayLWDay = railWayLWGeom.getRailWayLWDay();
            RailWayParameters railWayLWEvening = railWayLWGeom.getRailWayLWEvening();
            RailWayParameters railWayLWNight = railWayLWGeom.getRailWayLWNight();
            List<LineString> geometries = railWayLWGeom.getRailWayLWGeometry();

            int pk = railWayLWGeom.getPK();
            double[] LWDay = new double[CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            double[] LWEvening = new double[CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            double[] LWNight = new double[CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE.length];
            Arrays.fill(LWDay, -99.00);
            Arrays.fill(LWEvening, -99.00);
            Arrays.fill(LWNight, -99.00);
            double heightSource = 0;
            int directivityId = 0;
            boolean day = (railWayLWDay.getRailwaySourceList().size()>0);
            boolean evening = (railWayLWEvening.getRailwaySourceList().size()>0);
            boolean night = (railWayLWNight.getRailwaySourceList().size()>0);
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
