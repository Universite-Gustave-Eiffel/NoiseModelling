/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.input;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Bridge;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A bridge spanning several cells must not be truncated at a cell's fetch envelope: all of its
 * points should load as soon as one of them falls inside the envelope. Otherwise a long bridge
 * gets a spurious deck-end edge mid-span, and road points beyond it find no deck under them.
 */
public class DefaultTableLoaderBridgeTest {

    @Test
    public void fetchCellBridgesLoadsTheWholeBridgeEvenWhenTheEnvelopeOnlyCoversPartOfIt() throws Exception {
        try (Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase(DefaultTableLoaderBridgeTest.class.getSimpleName(), true, ""))) {
            int nPoints = 10;
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE BUILDINGS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON), HEIGHT DOUBLE)");
                st.execute("CREATE TABLE LW_ROADS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRING))");
                st.execute("INSERT INTO LW_ROADS(THE_GEOM) VALUES ('LINESTRING(0 20, 450 20)')");
                st.execute("CREATE TABLE RECEIVERS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POINT))");
                st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(225 60)')");
                st.execute("CREATE TABLE BRIDGE_POINTS(PK INT, BRIDGE_PK INT, THE_GEOM GEOMETRY(POINT), " +
                        "ABSOLUTE_DECK_HEIGHT DOUBLE, RELATIVE_DECK_HEIGHT DOUBLE, DECK_THICKNESS DOUBLE, " +
                        "RIGHT_WIDTH DOUBLE, LEFT_WIDTH DOUBLE, RIGHT_BARRIER_HEIGHT DOUBLE, LEFT_BARRIER_HEIGHT DOUBLE, " +
                        "POSITION VARCHAR, GIRDER_TYPE VARCHAR, SLAB_TYPE VARCHAR)");
                // a 450 m long, straight deck: points every 50 m along x, from x=0 to x=450
                for (int i = 0; i < nPoints; i++) {
                    st.execute("INSERT INTO BRIDGE_POINTS VALUES(" + i + ", 1, 'POINT(" + (i * 50) + " 20)', " +
                            "10.0, NULL, 0.5, 5.0, 5.0, 1.0, 1.0, 'CENTER', 'STEEL_BOX', 'STEEL')");
                }
            }

            NoiseMapByReceiverMaker maker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            maker.setBridgePointsTableName("BRIDGE_POINTS");
            maker.initialize(connection);

            DefaultTableLoader loader = new DefaultTableLoader();
            loader.noiseMapByReceiverMaker = maker;

            ProfileBuilder builder = new ProfileBuilder();
            // a cell envelope covering only the middle third of the bridge (x in [150, 300])
            Envelope cellEnvelope = new Envelope(150, 300, 0, 40);
            loader.fetchCellBridges(connection, cellEnvelope, builder);
            builder.finishFeeding();

            Bridge bridge = builder.getBridgeByPk(1);
            assertNotNull(bridge, "the bridge has points inside the cell envelope, so it should be loaded");
            assertEquals(nPoints, bridge.getBridgePointCount(),
                    "every point of the bridge should load, not just the ones inside the cell envelope");
        }
    }

    @Test
    public void fetchCellBridgesLoadsABridgeThatSpansACellWithNoPointOfItsOwnInside() throws Exception {
        try (Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase(DefaultTableLoaderBridgeTest.class.getSimpleName(), true, ""))) {
            int nPoints = 10;
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE BUILDINGS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON), HEIGHT DOUBLE)");
                st.execute("CREATE TABLE LW_ROADS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRING))");
                st.execute("INSERT INTO LW_ROADS(THE_GEOM) VALUES ('LINESTRING(0 20, 450 20)')");
                st.execute("CREATE TABLE RECEIVERS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POINT))");
                st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(225 60)')");
                st.execute("CREATE TABLE BRIDGE_POINTS(PK INT, BRIDGE_PK INT, THE_GEOM GEOMETRY(POINT), " +
                        "ABSOLUTE_DECK_HEIGHT DOUBLE, RELATIVE_DECK_HEIGHT DOUBLE, DECK_THICKNESS DOUBLE, " +
                        "RIGHT_WIDTH DOUBLE, LEFT_WIDTH DOUBLE, RIGHT_BARRIER_HEIGHT DOUBLE, LEFT_BARRIER_HEIGHT DOUBLE, " +
                        "POSITION VARCHAR, GIRDER_TYPE VARCHAR, SLAB_TYPE VARCHAR)");
                // a 450 m long, straight deck: points every 50 m along x, from x=0 to x=450
                for (int i = 0; i < nPoints; i++) {
                    st.execute("INSERT INTO BRIDGE_POINTS VALUES(" + i + ", 1, 'POINT(" + (i * 50) + " 20)', " +
                            "10.0, NULL, 0.5, 5.0, 5.0, 1.0, 1.0, 'CENTER', 'STEEL_BOX', 'STEEL')");
                }
            }

            NoiseMapByReceiverMaker maker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            maker.setBridgePointsTableName("BRIDGE_POINTS");
            maker.initialize(connection);

            DefaultTableLoader loader = new DefaultTableLoader();
            loader.noiseMapByReceiverMaker = maker;

            ProfileBuilder builder = new ProfileBuilder();
            // a narrow cell strictly between two bridge points (100 and 150): the bridge's own
            // span crosses it, but no BRIDGE_POINTS row falls inside it
            Envelope cellEnvelope = new Envelope(120, 130, 0, 40);
            loader.fetchCellBridges(connection, cellEnvelope, builder);
            builder.finishFeeding();

            Bridge bridge = builder.getBridgeByPk(1);
            assertNotNull(bridge, "the bridge spans this cell even though none of its own points fall inside it");
            assertEquals(nPoints, bridge.getBridgePointCount(), "every point of the bridge should load");
        }
    }
}
