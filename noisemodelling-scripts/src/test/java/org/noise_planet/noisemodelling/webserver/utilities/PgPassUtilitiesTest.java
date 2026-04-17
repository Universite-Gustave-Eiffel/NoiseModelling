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
