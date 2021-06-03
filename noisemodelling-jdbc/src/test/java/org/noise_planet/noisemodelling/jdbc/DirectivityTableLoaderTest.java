package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.emission.DirectionAttributes;
import org.noise_planet.noisemodelling.emission.DiscreteDirectionAttributes;
import org.noise_planet.noisemodelling.emission.RailWayLW;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.Assert.*;

public class DirectivityTableLoaderTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(DirectivityTableLoaderTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testFetch() throws SQLException {
        double[] freqTest = new double[] {63, 125, 250, 500, 1000, 2000, 4000, 8000};
        DiscreteDirectionAttributes d = new DiscreteDirectionAttributes(1, freqTest);
        try(Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE DIRTEST(DIR_ID INTEGER, THETA FLOAT, PHI FLOAT, LW63 FLOAT, LW125 FLOAT, LW250 FLOAT, LW500 FLOAT, LW1000 FLOAT, LW2000 FLOAT, LW4000 FLOAT, LW8000 FLOAT)");
        }
        try(PreparedStatement st = connection.prepareStatement("INSERT INTO DIRTEST VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            RailWayLW.TrainAttenuation att = new RailWayLW.TrainAttenuation(RailWayLW.TrainNoiseSource.TRACTIONB);
            for(int yaw = 0; yaw < 360; yaw += 5) {
                float theta = (float) Math.toRadians(yaw);
                for (int pitch = -85; pitch < 90; pitch += 5) {
                    float phi = (float)Math.toRadians(pitch);
                    st.setInt(1, 1);
                    st.setFloat(2, yaw);
                    st.setFloat(3, pitch);
                    double[] attSpectrum = new double[freqTest.length];
                    for (int idFreq = 0; idFreq < freqTest.length; idFreq++) {
                        attSpectrum[idFreq] = att.getAttenuation(freqTest[idFreq], phi, theta);
                        st.setFloat(4 + idFreq, (float)attSpectrum[idFreq]);
                    }
                    st.executeUpdate();
                    d.addDirectivityRecord(theta, phi,
                            attSpectrum);
                }
            }
        }

        // Data is inserted now fetch it from the database
        Map<Integer, DiscreteDirectionAttributes> directivities = DirectivityTableLoader.loadTable(connection, "DIRTEST", 1);

        assertEquals(1, directivities.size());

        assertTrue(directivities.containsKey(d.getDirectionIdentifier()));

        assertEquals(d.getRecordsTheta().size(), directivities.get(d.getDirectionIdentifier()).getRecordsTheta().size());

        assertEquals(d.getRecordsTheta().get(0), directivities.get(d.getDirectionIdentifier()).getRecordsTheta().get(0));

        assertArrayEquals(d.getRecordsTheta().get(0).getAttenuation(), directivities.get(d.getDirectionIdentifier()).getRecordsTheta().get(0).getAttenuation(), 0.1);

    }

}