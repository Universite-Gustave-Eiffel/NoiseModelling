
/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and
 * education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Utility class to handle PostgreSQL .pgpass file.
 * See <a href="https://www.postgresql.org/docs/current/libpq-pgpass.html">PostGIS help</a>
 */
public class PgPassUtilities {

    public static final String PGPASS_FILENAME = ".pgpass";

    /**
     * Represents a line in the .pgpass file.
     */
    public static class PgPassEntry {
        public final String hostname;
        public final String port;
        public final String database;
        public final String username;
        public final String password;

        public PgPassEntry(String hostname, String port, String database, String username, String password) {
            this.hostname = hostname;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Fetch credentials from a specific file.
     * See <a href="https://www.postgresql.org/docs/current/libpq-pgpass.html">PostGIS help page</a>
     * @param pgPassFile The file to read
     * @param hostname Hostname to match
     * @param database Database name to match
     * @return PgPassEntry if found, null otherwise
     */
    public static PgPassEntry getCredentials(File pgPassFile, String hostname, String port, String database, String username) {
        if (pgPassFile == null || !pgPassFile.exists() || !pgPassFile.canRead()) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(pgPassFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("(?<!\\\\):");
                if (parts.length == 5) {
                    String pgHost = unescape(parts[0]);
                    String pgPort = unescape(parts[1]);
                    String pgDb = unescape(parts[2]);
                    String pgUser = unescape(parts[3]);
                    String pgPass = unescape(parts[4]);

                    if (match(pgHost, hostname) && match(pgPort, port) && match(pgDb, database) && match(pgUser, username)) {
                        return new PgPassEntry(pgHost, pgPort, pgDb, pgUser, pgPass);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return null;
    }

    private static boolean match(String pgValue, String value) {
        return "*".equals(pgValue) || pgValue.equals(value);
    }

    private static String unescape(String value) {
        return value.replace("\\:", ":").replace("\\\\", "\\");
    }

    /**
     * Returns the .pgpass file path.
     * @return File containing the PostGIS database credentials
     */
    public static File getPgPassFile() {
        String pgPassEnv = System.getenv("PGPASSFILE");
        if (pgPassEnv != null && !pgPassEnv.isEmpty()) {
            return new File(pgPassEnv);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            // On Windows
            Path pgPassPath = Path.of(appData, "postgresql", "pgpass.conf");
            if (pgPassPath.toFile().exists()) {
                return pgPassPath.toFile();
            }
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            return new File(userHome, PGPASS_FILENAME);
        }
        return null;
    }
}
