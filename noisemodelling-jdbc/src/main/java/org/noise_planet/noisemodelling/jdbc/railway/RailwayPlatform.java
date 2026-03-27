/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.railway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.noise_planet.noisemodelling.emission.railway.Railway;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Describes a railway platform geometry and ground properties.
 * These parameters define the cross-section of the railway track platform
 * used for noise emission and propagation calculations.
 * Platform definitions are loaded from a JSON resource file (RailwayPlatforms.json).
 */
public class RailwayPlatform {

    /** Track gauge - distance between rails (m) */
    public double d1 = 1.8;

    /** Top width of the ballast shoulder (m) */
    public double d2_0 = 3.3;

    /** Bottom width of the ballast shoulder (m) */
    public double d3_0 = 4.8;

    /** Total platform width (m) */
    public double d4_0 = 8.3;

    /** Ground factor of the platform area (0=hard, 1=soft) */
    public double g1 = 0;

    /** Ground factor of the ballast shoulder (0=hard, 1=soft) */
    public double g2 = 1;

    /** Ground factor between the rails (0=hard, 1=soft) */
    public double g3 = 1;

    /** Height of the ballast shoulder (m) */
    public double h1 = 0.5;

    /** Height of the rail above the ballast - hRail (m) */
    public double h2 = 0.18;

    /** Default platform with standard SNCF values */
    public static final RailwayPlatform DEFAULT_PLATFORM = new RailwayPlatform();

    public RailwayPlatform() {
    }

    public RailwayPlatform(RailwayPlatform other) {
        this.d1 = other.d1;
        this.d2_0 = other.d2_0;
        this.d3_0 = other.d3_0;
        this.d4_0 = other.d4_0;
        this.g1 = other.g1;
        this.g2 = other.g2;
        this.g3 = other.g3;
        this.h1 = other.h1;
        this.h2 = other.h2;
    }

    /**
     * @return Height of the rail above the ballast (h2 only). Ballast is considered as the ground reference,
     *         so hRail represents the rail protruding above it. Used in body barrier calculation and source height positioning.
     */
    public double getHRail() {
        return h2;
    }

    /**
     * @param nTrack number of tracks in the section
     * @param trackSpacing spacing between tracks (m)
     * @return Top width of the ballast shoulder adjusted for multi-track: d2_0 + (nTrack - 1) * trackSpacing
     */
    public double getD2(int nTrack, double trackSpacing) {
        return d2_0 + (nTrack - 1) * trackSpacing;
    }

    /**
     * @param nTrack number of tracks in the section
     * @param trackSpacing spacing between tracks (m)
     * @return Bottom width of the ballast shoulder adjusted for multi-track: d3_0 + (nTrack - 1) * trackSpacing
     */
    public double getD3(int nTrack, double trackSpacing) {
        return d3_0 + (nTrack - 1) * trackSpacing;
    }

    /**
     * @param nTrack number of tracks in the section
     * @param trackSpacing spacing between tracks (m)
     * @return Total platform width adjusted for multi-track: d4_0 + (nTrack - 1) * trackSpacing
     */
    public double getD4(int nTrack, double trackSpacing) {
        return d4_0 + (nTrack - 1) * trackSpacing;
    }

    /**
     * Load platform definitions from a JSON resource file.
     * The JSON is located next to the Railway class resources, or if the filename is an URL, it is opened.
     * @param fileName name of the JSON file (e.g. "RailwayPlatforms.json") or JSON url (e.g. "file:///path/to/RailwayPlatforms.json")
     * @return map of platform name to RailwayPlatform
     */
    public static Map<String, RailwayPlatform> loadFromJSON(String fileName) {
        Map<String, RailwayPlatform> platforms = new HashMap<>();
        try (InputStream is = Railway.getStreamFromResourceString(fileName)) {
            if (is == null) {
                return platforms;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode node = root.get(name);
                RailwayPlatform p = new RailwayPlatform();
                if (node.has("d1")) p.d1 = node.get("d1").asDouble();
                if (node.has("d2_0")) p.d2_0 = node.get("d2_0").asDouble();
                if (node.has("d3_0")) p.d3_0 = node.get("d3_0").asDouble();
                if (node.has("d4_0")) p.d4_0 = node.get("d4_0").asDouble();
                if (node.has("g1")) p.g1 = node.get("g1").asDouble();
                if (node.has("g2")) p.g2 = node.get("g2").asDouble();
                if (node.has("g3")) p.g3 = node.get("g3").asDouble();
                if (node.has("h1")) p.h1 = node.get("h1").asDouble();
                if (node.has("h2")) p.h2 = node.get("h2").asDouble();
                platforms.put(name.toUpperCase(), p);
            }
        } catch (IOException e) {
            // fallback: empty map
        }
        return platforms;
    }

}
