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

package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.utilities.JDBCUtilities
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.noise_planet.noisemodelling.jdbc.utils.PostgisConnectionWrapper
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid
import org.noise_planet.noisemodelling.wps.Database_Manager.DatabaseHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

import java.sql.Connection
import java.sql.DriverManager

/**
 * PostGIS integration tests for WPS scripts.
 * These tests use Testcontainers to spin up a PostGIS Docker container.
 * Tests are skipped if Docker is not available.
 */
class TestPostGIS extends GroovyTestCase {
    static final Logger LOGGER = LoggerFactory.getLogger(TestPostGIS.class)
    static PostgreSQLContainer<?> POSTGIS_CONTAINER
    Connection connection

    @Before
    void setUp() {
        try {
            POSTGIS_CONTAINER = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:15-3.3")
                            .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("noisemodelling_test")
                    .withUsername("nm")
                    .withPassword("nm")
            POSTGIS_CONTAINER.start()
        } catch (Exception e) {
            LOGGER.warn("Docker not available, skipping PostGIS tests: {}", e.getMessage())
            Assume.assumeTrue("Docker not available", false)
            return
        }

        Connection rawConnection = DriverManager.getConnection(
                POSTGIS_CONTAINER.getJdbcUrl(),
                POSTGIS_CONTAINER.getUsername(),
                POSTGIS_CONTAINER.getPassword())

        // Enable PostGIS extensions
        rawConnection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS postgis")

        // Wrap connection for JTS geometry type support
        connection = new PostgisConnectionWrapper(rawConnection)
    }

    @After
    void tearDown() {
        if (connection != null && !connection.isClosed()) {
            connection.close()
        }
        if (POSTGIS_CONTAINER != null && POSTGIS_CONTAINER.isRunning()) {
            POSTGIS_CONTAINER.stop()
        }
    }

    /**
     * Test DatabaseHelper utility methods on PostGIS connection.
     */
    @Test
    void testDatabaseHelper() {
        assertTrue(DatabaseHelper.isPostgreSQL(connection))
        assertEquals("SERIAL", DatabaseHelper.autoIncrement(connection))
        assertEquals("DOUBLE PRECISION", DatabaseHelper.doublePrecision(connection))

        Connection resolved = DatabaseHelper.resolveConnection(connection)
        assertNotNull(resolved)
    }

    /**
     * Test importing a shapefile and computing road emissions on PostGIS.
     */
    @Test
    void testRoadEmissionPostGIS() {
        // Import roads
        new Import_File().exec(connection,
                ["pathFile": TestPostGIS.getResource("ROADS2.shp").getPath()])

        // Verify table was created (PostgreSQL uses lowercase for unquoted identifiers)
        assertTrue(JDBCUtilities.tableExists(connection, "roads2"))

        // Compute road emissions
        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        assertEquals("Calculation Done ! The table LW_ROADS has been created.", res)

        // Verify LW_ROADS table exists and has expected columns (PostgreSQL uses lowercase)
        assertTrue(JDBCUtilities.tableExists(connection, "lw_roads"))
        def fieldNames = JDBCUtilities.getColumnNames(connection, "lw_roads")
        assertTrue(fieldNames.contains("the_geom"))
        assertTrue(fieldNames.contains("hzd63"))
        assertTrue(fieldNames.contains("hzn8000"))
    }

    /**
     * Test importing buildings and creating a receiver grid on PostGIS.
     */
    @Test
    void testDelaunayGridPostGIS() {
        // Import buildings and roads
        new Import_File().exec(connection,
                ["pathFile": TestPostGIS.getResource("buildings.shp").getPath()])

        new Import_File().exec(connection,
                ["pathFile": TestPostGIS.getResource("ROADS2.shp").getPath()])

        assertTrue(JDBCUtilities.tableExists(connection, "buildings"))
        assertTrue(JDBCUtilities.tableExists(connection, "roads2"))

        // Create Delaunay receiver grid
        String res = new Delaunay_Grid().exec(connection,
                ["sourcesTableName": "ROADS2",
                 "tableBuilding"   : "BUILDINGS"])

        // Verify receivers table was created (PostgreSQL uses lowercase)
        assertTrue(JDBCUtilities.tableExists(connection, "receivers"))
        int receiverCount = JDBCUtilities.getRowCount(connection, "receivers")
        assertTrue("Expected receivers to be created", receiverCount > 0)
        LOGGER.info("Created {} receivers on PostGIS", receiverCount)
    }

    /**
     * Test full noise level computation pipeline on PostGIS.
     */
    @Test
    void testNoiseLevelFromTrafficPostGIS() {
        // Import buildings and roads
        new Import_File().exec(connection,
                ["pathFile": TestPostGIS.getResource("buildings.shp").getPath()])

        new Import_File().exec(connection,
                ["pathFile": TestPostGIS.getResource("ROADS2.shp").getPath()])

        // Road emissions
        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        // Delaunay grid (PostgreSQL uses lowercase table names)
        new Delaunay_Grid().exec(connection,
                ["sourcesTableName": "LW_ROADS",
                 "tableBuilding"   : "BUILDINGS"])

        assertTrue(JDBCUtilities.tableExists(connection, "receivers"))

        // Noise level computation
        new Noise_level_from_traffic().exec(connection,
                                ["tableRoads" : "LW_ROADS",
                 "tableReceivers": "RECEIVERS",
                 "tableBuilding" : "BUILDINGS"])

                assertTrue(JDBCUtilities.tableExists(connection, "receivers_level"))
                int resultCount = JDBCUtilities.getRowCount(connection, "receivers_level")
        assertTrue("Expected noise level results", resultCount > 0)
        LOGGER.info("Computed {} noise level results on PostGIS", resultCount)
    }
}
