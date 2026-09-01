/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end: a {@code BRIDGE_POINTS} table drives a bridge deck that shields a receiver.
 * Runs the same scene with and without the bridge table and checks the deck lowers the level.
 */
public class BridgeNoiseMapTest {

    private Connection connection;

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase(BridgeNoiseMapTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    private void createScene(Statement st) throws Exception {
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON), HEIGHT DOUBLE)");

        st.execute("DROP TABLE IF EXISTS DEM");
        st.execute("CREATE TABLE DEM(THE_GEOM GEOMETRY(POINTZ))");
        for (int x = -40; x <= 140; x += 30) {
            for (int y = -40; y <= 140; y += 30) {
                st.execute("INSERT INTO DEM VALUES ('POINTZ(" + x + " " + y + " 0)')");
            }
        }

        // one short line source, on the deck (absolute Z ~ 10.6), inside the deck footprint (strip at y=20)
        st.execute("DROP TABLE IF EXISTS LW_ROADS");
        StringBuilder cols = new StringBuilder("PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ)");
        StringBuilder vals = new StringBuilder("1, 'LINESTRING Z(40 20 10.6, 60 20 10.6)'");
        for (String p : new String[]{"D", "E", "N"}) {
            for (String f : new String[]{"63", "125", "250", "500", "1000", "2000", "4000", "8000"}) {
                cols.append(", LW").append(p).append(f).append(" DOUBLE");
                vals.append(", 90.0");
            }
        }
        st.execute("CREATE TABLE LW_ROADS(" + cols + ")");
        st.execute("INSERT INTO LW_ROADS VALUES(" + vals + ")");

        // receiver on the ground beyond the deck
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ))");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINTZ(50 75 4)')");

        // a straight deck at absolute altitude 10 m over the strip y in [15, 25]
        st.execute("DROP TABLE IF EXISTS BRIDGE_POINTS");
        st.execute("CREATE TABLE BRIDGE_POINTS(PK INT, BRIDGE_PK INT, THE_GEOM GEOMETRY(POINT), " +
                "ABSOLUTE_DECK_HEIGHT DOUBLE, RELATIVE_DECK_HEIGHT DOUBLE, DECK_THICKNESS DOUBLE, " +
                "RIGHT_WIDTH DOUBLE, LEFT_WIDTH DOUBLE, RIGHT_BARRIER_HEIGHT DOUBLE, LEFT_BARRIER_HEIGHT DOUBLE, " +
                "POSITION VARCHAR, GIRDER_TYPE VARCHAR, SLAB_TYPE VARCHAR)");
        for (int i = 0; i < 3; i++) {
            st.execute("INSERT INTO BRIDGE_POINTS VALUES(" + i + ", 100, 'POINT(" + (i * 50) + " 20)', " +
                    "10.0, NULL, 0.5, 5.0, 5.0, 1.0, 1.0, 'CENTER', 'STEEL_BOX', 'STEEL')");
        }
    }

    private double runAndGetLevel(boolean withBridge) throws Exception {
        try (Statement st = connection.createStatement()) {
            createScene(st);

            NoiseMapByReceiverMaker maker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            maker.sceneDatabaseInputSettings.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
            maker.setDemTable("DEM");
            maker.setSourcesZIsAltitude(true);
            maker.setReceiversZIsAltitude(true);
            maker.setMaximumPropagationDistance(250.0);
            maker.setSoundReflectionOrder(0);
            maker.setThreadCount(1);
            maker.setFrequencyFieldPrepend("LW");
            maker.setComputeHorizontalDiffraction(true);
            maker.setComputeVerticalDiffraction(true);
            if (withBridge) {
                maker.setBridgePointsTableName("BRIDGE_POINTS");
            }

            maker.run(connection, new EmptyProgressVisitor());

            String levelTable = maker.getNoiseMapDatabaseParameters().receiversLevelTable;
            try (ResultSet rs = st.executeQuery("SELECT LEQ FROM " + levelTable + " WHERE PERIOD='DEN'")) {
                assertTrue(rs.next(), "a level row is expected");
                return rs.getDouble(1);
            }
        }
    }

    @Test
    public void bridgeDeckLowersTheLevelAtAShieldedReceiver() throws Exception {
        double withoutBridge = runAndGetLevel(false);
        double withBridge = runAndGetLevel(true);
        assertTrue(withBridge < withoutBridge - 0.5,
                String.format("expected the deck to shield the receiver: without=%.2f dB, with=%.2f dB",
                        withoutBridge, withBridge));
    }
}
