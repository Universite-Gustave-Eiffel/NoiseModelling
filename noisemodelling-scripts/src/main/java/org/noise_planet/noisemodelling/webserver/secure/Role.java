/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver.secure;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    /** Non registered user */
    ANYONE,
    /** Can launch/cancel its own job  */
    RUNNER,
    /** Can add/remove users, see cancel any jobs */
    ADMINISTRATOR}
