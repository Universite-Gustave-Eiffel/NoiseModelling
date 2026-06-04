/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
