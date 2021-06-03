/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.noise_planet.noisemodelling.emission.DirectionAttributes;
import org.noise_planet.noisemodelling.emission.DiscreteDirectionAttributes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Fetch directivity from database table
 */
public class DirectivityTableLoader {

    /**
     * The table shall contain the following fields :
     * DIR_ID : identifier of the directivity sphere (INTEGER)
     * THETA : Horizontal angle in degree. 0° front and 90° right (0-360) (FLOAT)
     * PHI : Vertical angle in degree. 0° front and 90° top -90° bottom (-90 - 90) (FLOAT)
     * LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000 : attenuation levels in dB for each octave or third octave (FLOAT)
     * @param connection
     * @param tableName
     * @param defaultInterpolation
     * @return
     */
    public static Map<Integer, DiscreteDirectionAttributes> loadTable(Connection connection, String tableName, int defaultInterpolation) throws SQLException {
        Map<Integer, DiscreteDirectionAttributes> directionAttributes = new HashMap<>();
        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), tableName);
        // fetch provided frequencies
        List<String> frequenciesFields = new ArrayList<>();
        for(String field : fields) {
            if(field.toUpperCase(Locale.ROOT).startsWith("LW")) {
                frequenciesFields.add(field);
            }
        }
        if(frequenciesFields.isEmpty()) {
            return directionAttributes;
        }
        double[] frequencies = new double[frequenciesFields.size()];
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencies[idFrequency] = Double.parseDouble(frequenciesFields.get(idFrequency).substring(2));
        }
        StringBuilder sb = new StringBuilder("SELECT DIR_ID, THETA, PHI");
        for(String frequency : frequenciesFields) {
            sb.append(", ");
            sb.append(frequency);
        }
        sb.append(" FROM ");
        sb.append(tableName);
        sb.append(" ORDER BY DIR_ID");
        try(Statement st = connection.createStatement()) {
            try(ResultSet rs = st.executeQuery(sb.toString())) {
                List<DiscreteDirectionAttributes.DirectivityRecord> rows = new ArrayList<>();
                int lastDirId = Integer.MIN_VALUE;
                while (rs.next()) {
                    int dirId = rs.getInt(1);
                    if(lastDirId != dirId && !rows.isEmpty()) {
                        DiscreteDirectionAttributes attributes = new DiscreteDirectionAttributes(lastDirId, frequencies);
                        attributes.setInterpolationMethod(defaultInterpolation);
                        attributes.addDirectivityRecords(rows);
                        directionAttributes.put(lastDirId, attributes);
                        rows.clear();
                    }
                    lastDirId = dirId;
                    float theta = (float)Math.toRadians(rs.getFloat(2));
                    float phi = (float)Math.toRadians(rs.getFloat(3));
                    double[] att = new double[frequencies.length];
                    for(int freqColumn = 0; freqColumn < frequencies.length; freqColumn++) {
                        att[0] = rs.getDouble(freqColumn + 4);
                    }
                    DiscreteDirectionAttributes.DirectivityRecord r = new DiscreteDirectionAttributes.DirectivityRecord(theta, phi, att);
                    rows.add(r);
                }
                if(!rows.isEmpty()) {
                    DiscreteDirectionAttributes attributes = new DiscreteDirectionAttributes(lastDirId, frequencies);
                    attributes.setInterpolationMethod(defaultInterpolation);
                    attributes.addDirectivityRecords(rows);
                    directionAttributes.put(lastDirId, attributes);
                }
            }
        }
        return directionAttributes;
    }
}
