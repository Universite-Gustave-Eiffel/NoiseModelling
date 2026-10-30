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

package org.noise_planet.noisemodelling;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class VersionUtilsTest {

    @Test
    public void testGetVersion() {
        String version = VersionUtils.getVersion();
        assertNotNull(version);
        assertNotEquals("Unknown", version);
    }

    @Test
    public void testGetCommit() {
        assertNotNull(VersionUtils.getCommit());
    }

    @Test
    public void testGetVersionDescription() {
        String description = VersionUtils.getVersionDescription();
        assertTrue(description.startsWith("NoiseModelling " + VersionUtils.getVersion()));
        assertTrue(description.contains("(commit " + VersionUtils.getCommit() + ")"));
    }

    @Test
    public void testResolveUnresolvedPlaceholder() {
        Properties properties = new Properties();
        properties.setProperty("project.commit", "${buildNumber}");
        assertEquals("Unknown", VersionUtils.resolveProperty(properties, "project.commit"));
    }

    @Test
    public void testResolveMissingOrBlankValue() {
        Properties properties = new Properties();
        assertEquals("Unknown", VersionUtils.resolveProperty(properties, "project.commit"));
        properties.setProperty("project.commit", "  ");
        assertEquals("Unknown", VersionUtils.resolveProperty(properties, "project.commit"));
        properties.setProperty("project.commit", "1a2b3c4");
        assertEquals("1a2b3c4", VersionUtils.resolveProperty(properties, "project.commit"));
    }
}
