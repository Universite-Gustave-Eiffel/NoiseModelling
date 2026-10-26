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
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    public void fetchCellBridgesCachesExtentsCorrectlyAcrossMultipleCellsOnTheSameLoader() throws Exception {
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

            // one loader instance reused across two different cells, as happens for real during a
            // noise map run - the bridge-extents cache must stay correct for both
            DefaultTableLoader loader = new DefaultTableLoader();
            loader.noiseMapByReceiverMaker = maker;

            ProfileBuilder farBuilder = new ProfileBuilder();
            Envelope farEnvelope = new Envelope(1000, 1040, 0, 40);
            loader.fetchCellBridges(connection, farEnvelope, farBuilder);
            farBuilder.finishFeeding();
            assertNull(farBuilder.getBridgeByPk(1), "a cell far from the bridge should not load it");

            ProfileBuilder nearBuilder = new ProfileBuilder();
            Envelope nearEnvelope = new Envelope(150, 300, 0, 40);
            loader.fetchCellBridges(connection, nearEnvelope, nearBuilder);
            nearBuilder.finishFeeding();
            Bridge bridge = nearBuilder.getBridgeByPk(1);
            assertNotNull(bridge, "a later cell that does overlap the bridge should still load it correctly "
                    + "after the cached extents were computed for an earlier, unrelated cell");
            assertEquals(nPoints, bridge.getBridgePointCount());
        }
    }

    @Test
    public void initializeClearsTheBridgeExtentsCacheSoAMovedBridgeIsFoundAtItsNewLocation() throws Exception {
        try (Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase(DefaultTableLoaderBridgeTest.class.getSimpleName(), true, ""))) {
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
                // bridge 1, originally at x in [0, 90]
                for (int i = 0; i < 2; i++) {
                    st.execute("INSERT INTO BRIDGE_POINTS VALUES(" + i + ", 1, 'POINT(" + (i * 90) + " 20)', " +
                            "10.0, NULL, 0.5, 5.0, 5.0, 1.0, 1.0, 'CENTER', 'STEEL_BOX', 'STEEL')");
                }
            }

            NoiseMapByReceiverMaker maker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            maker.setBridgePointsTableName("BRIDGE_POINTS");
            maker.initialize(connection);

            // the loader under test here is a separate instance from maker's own internal
            // tableLoader, so its initialize() must be called directly - this is still the same
            // real lifecycle as NoiseMapByReceiverMaker.run(), which calls initialize() on the one
            // loader instance it holds before every run
            DefaultTableLoader loader = new DefaultTableLoader();
            loader.initialize(connection, maker);

            // first "run": warm the cache with the bridge at its original location
            ProfileBuilder firstBuilder = new ProfileBuilder();
            loader.fetchCellBridges(connection, new Envelope(0, 90, 0, 40), firstBuilder);
            firstBuilder.finishFeeding();
            assertNotNull(firstBuilder.getBridgeByPk(1), "sanity check: the bridge is found at its original location");

            // the bridge's points move far away, as if a second, separate run started after an
            // edit to the BRIDGE_POINTS table
            try (Statement st = connection.createStatement()) {
                st.execute("UPDATE BRIDGE_POINTS SET THE_GEOM = 'POINT(' || (1000 + (PK * 90)) || ' 20)' WHERE BRIDGE_PK = 1");
            }

            // NoiseMapByReceiverMaker.run() calls initialize() again on the very same loader
            // instance for every run; this must pick up the moved bridge, not the cached old extent
            loader.initialize(connection, maker);

            ProfileBuilder oldLocationBuilder = new ProfileBuilder();
            loader.fetchCellBridges(connection, new Envelope(0, 90, 0, 40), oldLocationBuilder);
            oldLocationBuilder.finishFeeding();
            assertNull(oldLocationBuilder.getBridgeByPk(1), "the bridge moved away, so its old location should be empty now");

            ProfileBuilder newLocationBuilder = new ProfileBuilder();
            loader.fetchCellBridges(connection, new Envelope(1000, 1090, 0, 40), newLocationBuilder);
            newLocationBuilder.finishFeeding();
            assertNotNull(newLocationBuilder.getBridgeByPk(1),
                    "the bridge should be found at its new location after initialize() re-reads the table, "
                            + "not stuck with the extent cached from before it moved");
        }
    }
}
