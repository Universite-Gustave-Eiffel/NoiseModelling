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
import io.javalin.http.Handler;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import java.util.Optional;

/**
 * Derived from
 * <a href="https://github.com/kmehrunes/javalin-jwt">...</a>
 */
public class JavalinJWT {

    private final static String CONTEXT_ATTRIBUTE = "jwt";
    private final static String COOKIE_KEY = "jwt";


    public static boolean containsJWT(Context context) {
        return context.attribute(CONTEXT_ATTRIBUTE) != null;
    }

    public static Context addDecodedToContext(Context context, DecodedJWT jwt) {
        context.attribute(CONTEXT_ATTRIBUTE, jwt);
        return context;
    }

    public static DecodedJWT getDecodedFromContext(Context context) {
        Object attribute = context.attribute(CONTEXT_ATTRIBUTE);

        if (!(attribute instanceof DecodedJWT)) {
            throw new InternalServerErrorResponse("The context carried invalid object as JavalinJWT");
        }

        return (DecodedJWT) attribute;
    }

    public static Optional<String> getTokenFromHeader(Context context) {
        return Optional.ofNullable(context.header("Authorization"))
                .flatMap(header -> {
                    String[] split = header.split(" ");
                    if (split.length != 2 || !split[0].equals("Bearer")) {
                        return Optional.empty();
                    }

                    return Optional.of(split[1]);
                });
    }

    public static Optional<String> getTokenFromCookie(Context context) {
        return Optional.ofNullable(context.cookie(COOKIE_KEY));
    }

    public static Context addTokenToCookie(Context context, String token) {
        return context.cookie(COOKIE_KEY, token);
    }

    public static <T> Handler createHeaderDecodeHandler(JWTProvider<T> jwtProvider) {
        return context -> getTokenFromHeader(context)
                .flatMap(jwtProvider::validateToken)
                .ifPresent(jwt -> JavalinJWT.addDecodedToContext(context, jwt));
    }

    public static <T> Handler createCookieDecodeHandler(JWTProvider<T> jwtProvider) {
        return context -> getTokenFromCookie(context)
                .flatMap(jwtProvider::validateToken)
                .ifPresent(jwt -> JavalinJWT.addDecodedToContext(context, jwt));
    }


    /**
     * Return the user identifier from the web context (extracted from verified Json Web Token)
     * @param ctx Web context
     * @param provider Json Web Token verifier
     * @return User identifier or -1 if token is invalid
     */
    public static int getUserIdentifierFromContext(Context ctx, JWTProvider<User> provider) {
        // Read visitor token
        Optional<DecodedJWT> decodedJWT = JavalinJWT.getTokenFromCookie(ctx)
                .flatMap(provider::validateToken);
        if(decodedJWT.isPresent()) {
            return decodedJWT.get().getClaim("user_identifier").asInt();
        } else {
            return -1;
        }
    }
}
