package org.noise_planet.nmtutorial01;

import org.h2gis.postgis_jts_osgi.DataSourceFactoryImpl;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.Properties;

public class PostgisTest {
    Logger LOGGER = LoggerFactory.getLogger(PostgisTest.class);

    @Test
    public void testPostgisNoiseModelling1() throws Exception {
        DataSourceFactoryImpl dataSourceFactory = new DataSourceFactoryImpl();
        Properties p = new Properties();
        p.setProperty("serverName", "localhost");
        p.setProperty("portNumber", "5432");
        p.setProperty("databaseName", "postgres");
        p.setProperty("user", "postgres");
        p.setProperty("password", "");
        try(Connection connection = dataSourceFactory.createDataSource(p).getConnection()) {

        } catch (PSQLException ex) {
            if (ex.getCause() instanceof ConnectException) {
                // Connection issue ignore
                LOGGER.warn("Connection error to local PostGIS, ignored", ex);
            } else {
                throw ex;
            }
        }
    }
}
