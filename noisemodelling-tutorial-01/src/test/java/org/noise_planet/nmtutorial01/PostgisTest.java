package org.noise_planet.nmtutorial01;

import org.h2gis.postgis_jts_osgi.DataSourceFactoryImpl;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.*;

public class PostgisTest {
    Logger LOGGER = LoggerFactory.getLogger(PostgisTest.class);

    @Test
    public void testPostgisNoiseModelling1() throws Exception {
        DataSourceFactoryImpl dataSourceFactory = new DataSourceFactoryImpl();
        Properties p = new Properties();
        p.setProperty("serverName", "localhost");
        p.setProperty("portNumber", "5432");
        p.setProperty("databaseName", "orbisgis_db");
        p.setProperty("user", "orbisgis");
        p.setProperty("password", "orbisgis");
        try(Connection connection = JDBCUtilities.wrapConnection(dataSourceFactory.createDataSource(p).getConnection())) {
            Main.mainWithConnection(connection, "target");
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
