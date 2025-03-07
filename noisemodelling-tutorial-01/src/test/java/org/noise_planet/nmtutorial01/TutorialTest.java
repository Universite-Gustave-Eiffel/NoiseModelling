package org.noise_planet.nmtutorial01;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.postgis_jts_osgi.DataSourceFactoryImpl;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TutorialTest {
    Logger LOGGER = LoggerFactory.getLogger(TutorialTest.class);

    @Test
    public void testPostgisNoiseModelling1() throws Exception {
        DataSourceFactoryImpl dataSourceFactory = new DataSourceFactoryImpl();
        Properties p = new Properties();
        p.setProperty("serverName", "localhost");
        p.setProperty("portNumber", "5432");
        p.setProperty("databaseName", "noisemodelling_db");
        p.setProperty("user", "noisemodelling");
        p.setProperty("password", "noisemodelling");
        try(Connection connection = JDBCUtilities.wrapConnection(dataSourceFactory.createDataSource(p).getConnection())) {
            connection.createStatement().execute("DROP TABLE IF EXISTS receivers_level");
            connection.createStatement().execute("DROP TABLE IF EXISTS contouring_noise_map");
            NoiseMapByReceiverMaker map = Main.mainWithConnection(connection, "target/postgis");
            String receiverTable = TableLocation.capsIdentifier(
                    NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME, DBTypes.POSTGIS);
            assertTrue(JDBCUtilities.tableExists(connection.unwrap(Connection.class), receiverTable));
            assertTrue(JDBCUtilities.hasField(connection.unwrap(Connection.class), receiverTable, "period"));
            assertTrue(JDBCUtilities.tableExists(connection.unwrap(Connection.class), "contouring_noise_map"));
            assertTrue(JDBCUtilities.hasField(connection.unwrap(Connection.class), "contouring_noise_map", "period"));


            int receiversRowCount = JDBCUtilities.getRowCount(connection, "RECEIVERS");

            int resultRowCount = JDBCUtilities.getRowCount(connection,
                    receiverTable);

            // D E N and DEN, should be 4 more rows than receivers
            assertEquals(receiversRowCount * 4, resultRowCount);

            assertEquals(3, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().size());
            assertEquals(20, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("D").temperature);
            assertEquals(16, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("E").temperature);
            assertEquals(10, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("N").temperature);

        } catch (PSQLException psqlException) {
            if(!(psqlException.getCause() instanceof ConnectException)) {
                throw psqlException;
            } else {
                // Ignore connection exception, we may not be inside the unit test of github workflow
                LOGGER.warn(psqlException.getLocalizedMessage(), psqlException);
            }
        }
    }

    @Test
    public void testH2gisNoiseModelling() throws Exception {
        try(Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase(TutorialTest.class.getSimpleName(),
                        true, ""));) {
            NoiseMapByReceiverMaker map = Main.mainWithConnection(connection, "target/h2gis");

            String receiverTable = TableLocation.capsIdentifier(
                    NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME, DBTypes.H2GIS);
            assertTrue(JDBCUtilities.tableExists(connection.unwrap(Connection.class), receiverTable));
            assertTrue(JDBCUtilities.hasField(connection.unwrap(Connection.class), receiverTable, "period"));
            assertTrue(JDBCUtilities.tableExists(connection.unwrap(Connection.class), "contouring_noise_map"));
            assertTrue(JDBCUtilities.hasField(connection.unwrap(Connection.class), "contouring_noise_map", "period"));


            int receiversRowCount = JDBCUtilities.getRowCount(connection, "RECEIVERS");

            int resultRowCount = JDBCUtilities.getRowCount(connection,
                    receiverTable);

            // D E N and DEN, should be 4 more rows than receivers
            assertEquals(receiversRowCount * 4, resultRowCount);

            assertEquals(3, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().size());
            assertEquals(20, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("D").temperature);
            assertEquals(16, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("E").temperature);
            assertEquals(10, ((DefaultTableLoader)map.getTableLoader()).getCnossosParametersPerPeriod().get("N").temperature);

        } catch (PSQLException psqlException) {
            if(!(psqlException.getCause() instanceof ConnectException)) {
                throw psqlException;
            } else {
                // Ignore connection exception, we may not be inside the unit test of github workflow
                LOGGER.warn(psqlException.getLocalizedMessage(), psqlException);
            }
        }
    }
}
