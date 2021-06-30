package org.noise_planet.nmtutorial01;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.postgis_jts_osgi.DataSourceFactoryImpl;
import org.h2gis.utilities.SFSUtilities;
import org.junit.Test;
import org.noise_planet.noisemodelling.jdbc.LDENConfig;
import org.noise_planet.noisemodelling.jdbc.LDENPointNoiseMapFactory;
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostgisTest {
    Logger LOGGER = LoggerFactory.getLogger(PostgisTest.class);

    @Test
    public void testPostgisNoiseModelling1() throws Exception {
        DataSourceFactoryImpl dataSourceFactory = new DataSourceFactoryImpl();
        Properties p = new Properties();
        p.setProperty("serverName", "localhost");
        p.setProperty("portNumber", "5432");
        p.setProperty("databaseName", "postgres");
        p.setProperty("user", "orbisgis");
        p.setProperty("password", "orbisgis");
        try(Connection connection = SFSUtilities.wrapConnection(dataSourceFactory.createDataSource(p).getConnection())) {
            Statement sql = connection.createStatement();

            // Clean DB

            sql.execute("DROP TABLE IF EXISTS BUILDINGS");
            sql.execute("DROP TABLE IF EXISTS LW_ROADS");
            sql.execute("DROP TABLE IF EXISTS RECEIVERS");
            sql.execute("DROP TABLE IF EXISTS DEM");

            // Import BUILDINGS

            LOGGER.info("Import buildings");

            GeoJsonRead.readGeoJson(connection, Main.class.getResource("buildings.geojson").getFile(), "BUILDINGS");

            // Import noise source

            LOGGER.info("Import noise source");

            GeoJsonRead.readGeoJson(connection, Main.class.getResource("lw_roads.geojson").getFile(), "lw_roads");
            // Set primary key
            sql.execute("ALTER TABLE lw_roads ADD CONSTRAINT lw_roads_pk PRIMARY KEY (\"PK\");");

            // Import BUILDINGS

            LOGGER.info("Import evaluation coordinates");

            GeoJsonRead.readGeoJson(connection, Main.class.getResource("receivers.geojson").getFile(), "receivers");
            // Set primary key
            sql.execute("ALTER TABLE receivers ADD CONSTRAINT RECEIVERS_pk PRIMARY KEY (\"PK\");");

            // Import MNT

            LOGGER.info("Import digital elevation model");

            GeoJsonRead.readGeoJson(connection, Main.class.getResource("dem_lorient.geojson").getFile(), "dem");

            // Init NoiseModelling
            PointNoiseMap pointNoiseMap = new PointNoiseMap("buildings", "lw_roads", "receivers");

            pointNoiseMap.setMaximumPropagationDistance(160.0d);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setComputeHorizontalDiffraction(true);
            pointNoiseMap.setComputeVerticalDiffraction(true);
            // Building height field name
            pointNoiseMap.setHeightField("HEIGHT");
            // Point cloud height above sea level POINT(X Y Z)
            pointNoiseMap.setDemTable("DEM");
            // Do not propagate for low emission or far away sources.
            // error in dB
            pointNoiseMap.setMaximumError(0.1d);

            // Init custom input in order to compute more than just attenuation
            // LW_ROADS contain Day Evening Night emission spectrum
            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

            ldenConfig.setComputeLDay(true);
            ldenConfig.setComputeLEvening(true);
            ldenConfig.setComputeLNight(true);
            ldenConfig.setComputeLDEN(true);

            LDENPointNoiseMapFactory tableWriter = new LDENPointNoiseMapFactory(connection, ldenConfig);

            tableWriter.setKeepRays(true);

            pointNoiseMap.setPropagationProcessDataFactory(tableWriter);
            pointNoiseMap.setComputeRaysOutFactory(tableWriter);

            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            // force the creation of a 2x2 cells
            pointNoiseMap.setGridDim(2);


            // Set of already processed receivers
            Set<Long> receivers = new HashSet<>();
            ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim());
            LOGGER.info("start");
            long start = System.currentTimeMillis();

            // Iterate over computation areas
            try {
                tableWriter.start();
                for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
                    for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                        // Run ray propagation
                        IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    }
                }
            } finally {
                tableWriter.stop();
            }
            long computationTime = System.currentTimeMillis() - start;
            LOGGER.info(String.format(Locale.ROOT, "Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));

            int nbReceivers = 0;
            try(ResultSet rs = sql.executeQuery("SELECT COUNT(*) cpt FROM RECEIVERS")) {
                assertTrue(rs.next());
                nbReceivers = rs.getInt(1);
            }
            try(ResultSet rs = sql.executeQuery("SELECT COUNT(*) cpt FROM LDAY_RESULT")) {
                assertTrue(rs.next());
                assertEquals(nbReceivers, rs.getInt(1));
            }
            try(ResultSet rs = sql.executeQuery("SELECT COUNT(*) cpt FROM LEVENING_RESULT")) {
                assertTrue(rs.next());
                assertEquals(nbReceivers, rs.getInt(1));
            }
            try(ResultSet rs = sql.executeQuery("SELECT COUNT(*) cpt FROM LNIGHT_RESULT")) {
                assertTrue(rs.next());
                assertEquals(nbReceivers, rs.getInt(1));
            }
            try(ResultSet rs = sql.executeQuery("SELECT COUNT(*) cpt FROM LDEN_RESULT")) {
                assertTrue(rs.next());
                assertEquals(nbReceivers, rs.getInt(1));
            }
        } catch (PSQLException ex) {
            if (ex.getCause() == null || ex.getCause() instanceof ConnectException) {
                // Connection issue ignore
                LOGGER.warn("Connection error to local PostGIS, ignored", ex);
            } else {
                throw ex;
            }
        } catch (SQLException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex.getNextException());
            throw ex;
        }
    }
}
