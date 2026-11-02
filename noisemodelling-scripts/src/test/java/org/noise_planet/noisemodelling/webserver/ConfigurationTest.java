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

package org.noise_planet.noisemodelling.webserver;

import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.VersionUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationTest {

    @Test
    public void testShortVersionFlag() {
        assertVersionFlag("-v");
    }

    @Test
    public void testLongVersionFlag() {
        assertVersionFlag("--version");
    }

    @Test
    public void testConfigurationParsesWithoutVersionFlag() {
        Configuration configuration = Configuration.createConfigurationFromArguments(
                new String[]{"-s", "scripts/", "-p", "9001"});
        assertNotNull(configuration);
        assertEquals("scripts/", configuration.scriptPath);
        assertEquals(9001, configuration.port);
    }

    @Test
    public void testVersionFlagWithLauncherArguments() {
        assertVersionFlag("-u", "-s", "scripts/", "-r", "", "--version");
    }

    private static void assertVersionFlag(String... args) {
        String output = captureStdout(() ->
                assertNull(Configuration.createConfigurationFromArguments(args)));
        assertTrue(output.contains(VersionUtils.getVersionDescription()));
    }

    private static String captureStdout(Runnable runnable) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(printStream);
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8);
    }
}
