/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.secure;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.noise_planet.noisemodelling.webserver.Configuration;
import org.noise_planet.noisemodelling.webserver.UserController;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * Handle auth
 */
public class Auth {
    JWTProvider<User> provider;
    DataSource serverDataSource;
    Configuration configuration;
    Logger logger = LoggerFactory.getLogger(Auth.class);

    public Auth(JWTProvider<User> provider, DataSource serverDataSource, Configuration configuration) {
        this.provider = provider;
        this.serverDataSource = serverDataSource;
        this.configuration = configuration;
    }

    /**
     *
     * @param ctx Javalin Web context
     * @param serverDataSource server database connection
     * @return User or null if not connected
     * @throws SQLException JDBC exception
     */
    public static User getUserFromContext(Context ctx, DataSource serverDataSource,
                                          JWTProvider<User> provider) throws SQLException {
        int userId = JavalinJWT.getUserIdentifierFromContext(ctx, provider);
        if(userId > 0) {
            return DatabaseManagement.getUser(serverDataSource, userId);
        }
        return null;
    }

    /**
     * Check visitor credentials using Json Web Token.
     * Redirect user if non-authorized to the login page
     * @param ctx Javalin Web context
     */
    public void handleAccess(Context ctx) {
        var permittedRoles = ctx.routeRoles();
        if(configuration.isUnsecure()) {
            ctx.attribute("user", new User(1, DatabaseManagement.ADMIN_EMAIL,
                    new HashSet<>(Arrays.asList(Role.values())), ""));
            return; // anyone can access
        }
        if (permittedRoles.contains(Role.ANYONE)) {
            return; // anyone can access
        }
        // Read visitor token
        int userIdentifier = JavalinJWT.getUserIdentifierFromContext(ctx, provider);
        if(userIdentifier >= 0) {
            try {
                User user = DatabaseManagement.getUser(serverDataSource, userIdentifier);
                if(!user.registerToken.isEmpty()) {
                    // The administrator has reset the TOTP code
                    // User must validate the new TOTP code to be able to log in
                    ctx.attribute("messages",
                            String.format("Unauthorized access <a href=\"%s\">please login</a> before proceeding",
                                    ctx.contextPath()+"/login"));
                    throw new UnauthorizedResponse();
                }
                if (user.roles.stream().noneMatch(permittedRoles::contains)) {
                    ctx.attribute("messages", "You do not have the minimal authorization access to see this page");
                    throw new UnauthorizedResponse();
                }
                // Give access to the user to thymeleaf and controllers
                ctx.attribute("user", user);
            } catch (SQLException e) {
                ctx.attribute("messages", "Exception while authenticating the user");
                throw new UnauthorizedResponse();
            }
        } else {
            ctx.attribute("messages",
                    String.format("Unauthorized access <a href=\"%s\">please login</a> before proceeding",
                            ctx.contextPath()+"/login"));
            throw new UnauthorizedResponse();
        }
    }
}