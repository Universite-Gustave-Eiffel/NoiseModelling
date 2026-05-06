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

/**
 * Represents information about a library, including its name,
 * last modified date, version, and commit hash.
 */
public class LibraryInfo {
    private final String name;
    private final String lastModified;
    private final String version;
    private final String commit;
    private final long lastModifiedTimeStamp;

    public LibraryInfo(String name, String lastModified, String version, String commit, long lastModifiedTimeStamp) {
        this.name = name != null ? name : "Unknown";
        this.lastModified = lastModified != null ? lastModified : " - ";
        this.version = version != null ? version : " - ";
        this.commit = commit != null ? commit : " - ";
        this.lastModifiedTimeStamp = lastModifiedTimeStamp;
    }

    public String getName() { return name; }
    public String getLastModified() { return lastModified; }
    public String getVersion() { return version; }
    public String getCommit() { return commit; }
    public long getLastModifiedTimeStamp() { return lastModifiedTimeStamp; }
}
