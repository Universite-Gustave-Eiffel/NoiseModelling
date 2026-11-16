/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.utils;

import org.h2.util.ScriptReader;
import org.h2gis.api.ProgressVisitor;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class DataBaseUtilities {
    /**
     * Checks if the given SRID uses meters as its unit by querying the SPATIAL_REF_SYS table.
     *
     * @param connection A live JDBC connection
     * @param srid       The SRID to check
     * @return true if the units are metric (meters)
     */
    public static boolean isSridMetric(Connection connection, int srid) throws SQLException {
        String sql = "SELECT PROJ4TEXT FROM SPATIAL_REF_SYS WHERE SRID = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, srid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String proj = rs.getString("PROJ4TEXT");
                    if (proj == null || proj.isEmpty()) {
                        return false;
                    }

                    // Normalize to lowercase for reliable matching
                    proj = proj.toLowerCase();

                    /*
                     * Logic for Proj4 metric detection:
                     * 1. Check for "+units=m".
                     *    We ensure it's not "+units=mm" (millimeters).
                     * 2. Check for "+to_meter=1" or "+to_meter=1.0".
                     */
                    boolean hasMeterUnit = proj.contains("+units=m") && !proj.contains("+units=mm");
                    boolean hasMeterFactor = proj.contains("+to_meter=1") || proj.contains("+to_meter=1.0");

                    return hasMeterUnit || hasMeterFactor;
                }
            }
        } catch (SQLException e) {
            // Log the exception according to your project's logging policy
            System.err.println("Error querying SPATIAL_REF_SYS: " + e.getMessage());
        }

        return false;
    }

    /**
     * Parses a script containing multiple SQL statements and executes each of them on the given connection,
     * reporting progress through the provided visitor.
     *
     * @param sqlInstructions The SQL instructions to execute
     * @param connection      A live JDBC connection
     * @param progressVisitor Visitor used to report progress
     * @param logger          Logger used to report SQL comments
     */
    public static void parseScript(String sqlInstructions, Connection connection, ProgressVisitor progressVisitor, Logger logger) throws SQLException {
        InputStream is = new ByteArrayInputStream(sqlInstructions.getBytes());
        List<String> statementList = new LinkedList<>();
        try (Reader reader = new InputStreamReader(is)) {
            ScriptReader scriptReader = new ScriptReader(reader);
            scriptReader.setSkipRemarks(false);
            String statement = scriptReader.readStatement();
            while (statement != null && !statement.trim().isEmpty()) {
                statementList.add(statement);
                statement = scriptReader.readStatement();
            }
        } catch (java.io.IOException e) {
            throw new SQLException(e);
        }
        int idStatement = 0;
        final int nbStatements = statementList.size();
        ProgressVisitor evalProgress = progressVisitor.subProcess(nbStatements);
        for (String statement : statementList) {
            for (String subStatement : statement.split("\n")) {
                if (subStatement.trim().startsWith("--")) {
                    logger.info(String.format(Locale.ROOT, "%d/%d %s", idStatement + 1, nbStatements, subStatement.replace("--", "")));
                }
            }
            idStatement++;
            try (Statement st = connection.createStatement()) {
                st.execute(statement);
            }
            evalProgress.endStep();
            if (evalProgress.isCanceled()) {
                throw new SQLException("Canceled by user");
            }
        }
    }
}
