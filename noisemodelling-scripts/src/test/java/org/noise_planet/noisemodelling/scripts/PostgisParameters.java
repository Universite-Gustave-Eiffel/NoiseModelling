/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.scripts;

/**
 * Represents the parameters for connecting to a PostgreSQL database.
 */
public class PostgisParameters {
    public final String user;
    public final String password;
    public final String port;
    public final String database;
    public final String host;

    PostgisParameters(String user, String password, String port, String database, String host) {
        this.user = user;
        this.password = password;
        this.port = port;
        this.database = database;
        this.host = host;
    }
}
