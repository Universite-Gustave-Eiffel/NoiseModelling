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
    public static String getVersion() {
        try (InputStream input = VersionUtils.class.getResourceAsStream("version.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                return "Unknown";
            }
            prop.load(input);
            return prop.getProperty("project.version");
        } catch (Exception ex) {
            LoggerFactory.getLogger(VersionUtils.class).error("Error while reading version.properties", ex);
            return "Unknown";
        }
    }
}