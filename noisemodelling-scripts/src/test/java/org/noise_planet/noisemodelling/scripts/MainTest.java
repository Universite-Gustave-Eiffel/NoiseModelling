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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.AssumptionLoggerExtension;
import org.noise_planet.noisemodelling.runner.Main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AssumptionLoggerExtension.class)
public class MainTest {
    String dbName = UUID.randomUUID().toString().replace("-", "");

    @Test
    public void testSetHeight(@TempDir File temp) throws Exception {
        String receiverPath = MainTest.class.getResource("receivers.shp").getPath();
        Main.main("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--pathFile", receiverPath);

        Main.main("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Geometric_Tools/Set_Height.groovy",
                "--tableName", "RECEIVERS", "--height" , "1.5");
    }

    @AfterEach
    public void cleanup() throws Exception {
        // Close logging handlers
        LogManager.shutdown();

        // Give system time to release file
        Thread.sleep(125);
    }



    @Test
    public void testConnectionToPostGIS(@TempDir File temp) throws Exception {
        String pgHost = System.getenv("POSTGRES_HOST");
        Assumptions.assumeTrue(pgHost != null && !pgHost.isEmpty(), "POSTGRES_HOST is not defined, skipping PostGIS test");

        String pgUser = Optional.ofNullable(System.getenv("POSTGRES_USER")).orElse("noisemodelling");
        String pgPass = Optional.ofNullable(System.getenv("POSTGRES_PASSWORD")).orElse("noisemodelling");
        String pgPort = Optional.ofNullable(System.getenv("POSTGRES_PORT")).orElse("5432");
        String pgDb = Optional.ofNullable(System.getenv("POSTGRES_DB")).orElse("noisemodelling_db");

        String buildingsPath = MainTest.class.getResource("buildings.shp").getPath();

        Main.main("-w", temp.getAbsolutePath(),
                "-d", pgDb,
                "-u", pgUser,
                "-p", pgPass,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--port", pgPort,
                "--host", pgHost,
                "--pathFile", buildingsPath,
                "--tableName", "BUILDINGS");

        try (Connection connection = JdbcTestCase.createPostgisDataSourceFromEnv().getConnection()) {
            assertTrue(JDBCUtilities.tableExists(connection, "BUILDINGS"), "Table BUILDINGS should exist in PostGIS");
        }
    }
}
