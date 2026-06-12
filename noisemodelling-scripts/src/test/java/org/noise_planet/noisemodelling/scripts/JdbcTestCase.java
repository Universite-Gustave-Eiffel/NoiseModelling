/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 * <p>
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.scripts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.log4j.PropertyConfigurator;
import org.h2.Driver;
import org.h2.util.OsgiDataSourceFactory;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.postgis_jts.ConnectionWrapper;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.noise_planet.noisemodelling.VersionUtils;
import org.noise_planet.noisemodelling.runner.PostGISJTSDataSource;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class JdbcTestCase {

    DataSource dataSource;
    Connection connection;
    boolean isH2GISDatabase = false;

    static Logger LOG = LoggerFactory.getLogger(JdbcTestCase.class);

    /**
     * Retrieves PostgreSQL connection parameters from environment variables.
     * @return a {@link PostgisParameters} object containing the parameters or null if POSTGRES_HOST environment variable is not set
     */
    public static PostgisParameters getPostGISParametersFromEnv() {
        if(System.getenv("POSTGRES_HOST") == null) {
            return null;
        }
        String pgUser = Optional.ofNullable(System.getenv("POSTGRES_USER")).orElse("noisemodelling");
        String pgPass = Optional.ofNullable(System.getenv("POSTGRES_PASSWORD")).orElse("noisemodelling");
        String pgPort = Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432");
        String pgDb = Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db");
        String pgHost = Optional.ofNullable(System.getenv("POSTGRES_HOST")).orElse("localhost");
        return new PostgisParameters(pgUser, pgPass, pgPort, pgDb, pgHost);
    }

    /**
     * Creates a DataSource for PostgreSQL using the provided parameters.
     * @param params the PostgreSQL connection parameters
     * @return a DataSource configured for PostgreSQL
     * @throws SQLException if an error occurs while creating the DataSource
     */
    public static DataSource createPostgisDataSource(PostgisParameters params) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setUsername(params.user);
        config.setPassword(params.password);
        config.setDataSourceClassName(PostGISJTSDataSource.class.getCanonicalName());
        config.addDataSourceProperty("portNumbers", Integer.parseInt(params.port));
        config.addDataSourceProperty("databaseName", params.database);
        config.addDataSourceProperty("serverNames", params.host);
        return new HikariDataSource(config);
    }

    /**
     * Creates a DataSource for PostgreSQL from environment variables.
     * @return a DataSource configured for PostgreSQL
     * @throws SQLException if an error occurs while creating the DataSource
     */
    public static DataSource createPostgisDataSourceFromEnv() throws SQLException {
        return createPostgisDataSource(getPostGISParametersFromEnv());
    }

    public static DataSource createDataSource(String user, String password, boolean debug) throws SQLException {
        // Create H2 memory DataSource
        Driver driver = Driver.load();
        OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
        Properties properties = new Properties();
        String databasePath = "jdbc:h2:mem:junit"+System.currentTimeMillis();
        properties.setProperty(DataSourceFactory.JDBC_URL, databasePath);
        properties.setProperty(DataSourceFactory.JDBC_USER, user);
        properties.setProperty(DataSourceFactory.JDBC_PASSWORD, password);
        if (debug) {
            properties.setProperty("TRACE_LEVEL_FILE", "3"); // enable debug
        }
        return dataSourceFactory.createDataSource(properties);
    }

    @BeforeEach
    void initConnection() throws SQLException {
        dataSource = createDataSource("sa", "sa", false);
        isH2GISDatabase = !(dataSource instanceof HikariDataSource);
        if(isH2GISDatabase) {
            connection = JDBCUtilities.wrapConnection(dataSource.getConnection());
            H2GISFunctions.load(connection);
        } else {
            connection = new ConnectionWrapper(dataSource.getConnection());
        }
    }

    @AfterEach
    void closeConnection() throws SQLException {
        connection.close();
        try {
            // close connection pool, we are supposed to have a single connection pool
            HikariDataSource hds = dataSource.unwrap(HikariDataSource.class);
            hds.close();
        } catch (SQLException e) {
            // ignore
        }
    }

    @BeforeAll
    public static void init() {
        PropertyConfigurator.configure(
                Objects.requireNonNull(VersionUtils.class.getResource("log4j_tests.properties")));
    }
}
