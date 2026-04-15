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
import org.noise_planet.noisemodelling.webserver.NoiseModellingServerHttpTest;
import org.osgi.service.jdbc.DataSourceFactory;
import org.postgresql.ds.PGSimpleDataSource;
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

    static DataSource createDataSource(String user, String password, boolean debug) throws SQLException {
        HikariConfig config = new HikariConfig();
        boolean pgHostConfigurationDefined = System.getenv().containsKey ("POSTGRES_HOST");
        pgHostConfigurationDefined = false; // We do not use Postgis Database locally
        if(pgHostConfigurationDefined) {
            config.setUsername(System.getenv("POSTGRES_USER"));
            config.setPassword(System.getenv("POSTGRES_PASSWORD"));
            config.setDataSourceClassName(PGSimpleDataSource.class.getCanonicalName());
            config.addDataSourceProperty("portNumbers", Integer.parseInt(Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432")));
            config.addDataSourceProperty("databaseName",
                    Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db"));
            config.addDataSourceProperty("serverNames",
                    Optional.ofNullable(System.getenv("POSTGRES_HOST")).orElse("localhost"));
            return new HikariDataSource(config);
        } else {
            // Create H2 memory DataSource
            Driver driver = Driver.load();
            OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
            Properties properties = new Properties();
            String databasePath = "jdbc:h2:mem:junit"+System.currentTimeMillis();
            LOG.warn("POSTGRES_HOST is not configured, fallback to H2GIS database: \n${databasePath}");
            properties.setProperty(DataSourceFactory.JDBC_URL, databasePath);
            properties.setProperty(DataSourceFactory.JDBC_USER, user);
            properties.setProperty(DataSourceFactory.JDBC_PASSWORD, password);
            if (debug) {
                properties.setProperty("TRACE_LEVEL_FILE", "3"); // enable debug
            }
            return dataSourceFactory.createDataSource(properties);
        }
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
                Objects.requireNonNull(NoiseModellingServerHttpTest.class.getResource("log4j.properties")));
    }
}
