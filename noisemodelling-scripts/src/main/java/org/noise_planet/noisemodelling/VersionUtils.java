/*
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and
 * education, as well as by experts in a professional use.
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 *
 * Official webpage : http://noise-planet.org/noisemodelling.html
 *  Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling;

import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to read version.properties
 */
public class VersionUtils {
    private static final String UNKNOWN = "Unknown";

    private static Properties loadProperties() {
        Properties prop = new Properties();
        try (InputStream input = VersionUtils.class.getResourceAsStream("version.properties")) {
            if (input != null) {
                prop.load(input);
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(VersionUtils.class).error("Error while reading version.properties", ex);
        }
        return prop;
    }

    /**
     * Resolve a property value, falling back to {@link #UNKNOWN} for missing,
     * blank or unresolved maven placeholders (ex: build without git metadata).
     *
     * @param properties properties to read from
     * @param key        property key
     * @return resolved value or {@link #UNKNOWN}
     */
    static String resolveProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("${")) {
            return UNKNOWN;
        }
        return value;
    }

    private static String readProperty(String key) {
        return resolveProperty(loadProperties(), key);
    }

    /**
     * @return NoiseModelling maven project version
     */
    public static String getVersion() {
        return readProperty("project.version");
    }

    /**
     * @return Git commit identifier of the build
     */
    public static String getCommit() {
        return readProperty("project.commit");
    }

    /**
     * @return Human readable version and commit, ex: "NoiseModelling 6.0.2 (commit 1a2b3c4)"
     */
    public static String getVersionDescription() {
        return "NoiseModelling " + getVersion() + " (commit " + getCommit() + ")";
    }
}
