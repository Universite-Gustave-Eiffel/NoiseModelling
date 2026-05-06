/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.secure;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Derived from
 * <a href="https://github.com/kmehrunes/javalin-jwt">...</a>
 */
public class JWTAccessManager implements Handler {
    private final String userRoleClaim;
    private final Map<String, RouteRole> rolesMapping;
    private final RouteRole defaultRole;

    public JWTAccessManager(String userRoleClaim, Map<String, RouteRole> rolesMapping, RouteRole defaultRole) {
        this.userRoleClaim = userRoleClaim;
        this.rolesMapping = rolesMapping;
        this.defaultRole = defaultRole;
    }

    private RouteRole extractRole(Context context) {
        if (!JavalinJWT.containsJWT(context)) {
            return defaultRole;
        }

        DecodedJWT jwt = JavalinJWT.getDecodedFromContext(context);
        String userLevel = jwt.getClaim(userRoleClaim).asString();

        return Optional.ofNullable(rolesMapping.get(userLevel)).orElse(defaultRole);
    }

    @Override
    public void handle(@NotNull Context context) {
        RouteRole role = extractRole(context);
        Set<RouteRole> permittedRoles = context.routeRoles();
        if (!permittedRoles.contains(role)) {
            throw new UnauthorizedResponse();
        }
    }
}