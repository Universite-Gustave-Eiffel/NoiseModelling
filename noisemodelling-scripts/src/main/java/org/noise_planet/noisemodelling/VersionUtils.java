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

    private static String readProperty(String key) {
        try (InputStream input = VersionUtils.class.getResourceAsStream("version.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                return UNKNOWN;
            }
            prop.load(input);
            String value = prop.getProperty(key);
            // Unresolved maven placeholder (ex: build without git metadata)
            if (value == null || value.isBlank() || value.startsWith("${")) {
                return UNKNOWN;
            }
            return value;
        } catch (Exception ex) {
            LoggerFactory.getLogger(VersionUtils.class).error("Error while reading version.properties", ex);
            return UNKNOWN;
        }
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
