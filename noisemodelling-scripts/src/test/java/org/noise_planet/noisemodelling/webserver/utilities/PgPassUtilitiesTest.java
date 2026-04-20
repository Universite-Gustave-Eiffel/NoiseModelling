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
package org.noise_planet.noisemodelling.webserver.utilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PgPassUtilitiesTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParsePgPass() throws IOException {
        Path pgPassPath = tempDir.resolve(".pgpass");
        String content = "localhost:5432:mydb:myuser:mypassword\n" +
                         "escaped\\:host:5432:db:user:pass\n" +
                         "host2:*:db2:user2:pass2\n" +
                         "*:5432:*:*:otherpass\n";
        Files.write(pgPassPath, content.getBytes());

        File pgPassFile = pgPassPath.toFile();

        // Exact match
        PgPassUtilities.PgPassEntry entry = PgPassUtilities.getCredentials(pgPassFile, "localhost", "5432", "mydb", "myuser");
        assertNotNull(entry);
        assertEquals("mypassword", entry.password);

        // Escaped character match
        entry = PgPassUtilities.getCredentials(pgPassFile, "escaped:host", "5432", "db", "user");
        assertNotNull(entry);
        assertEquals("pass", entry.password);

        // Wildcard match (port)
        entry = PgPassUtilities.getCredentials(pgPassFile, "host2", "1234", "db2", "user2");
        assertNotNull(entry);
        assertEquals("pass2", entry.password);

        // Wildcard match (host, database, username) - this one should match the last line
        entry = PgPassUtilities.getCredentials(pgPassFile, "anyhost", "5432", "anydb", "anyuser");
        assertNotNull(entry);
        assertEquals("otherpass", entry.password);

        // No match
        entry = PgPassUtilities.getCredentials(pgPassFile, "localhost", "5433", "mydb", "myuser");
        assertNull(entry);
    }
}
