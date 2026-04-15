/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.secure;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Generator of Json Web Token used when a user as validated its identity with another method
 * Java Web Token will manage the identification process for each secure web page
 */
public class JWTProviderFactory {

    /**
     * @return Random secret token
     */
    public static String generateServerSecretToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key for HS256
        secureRandom.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }

    /**
     * Create the instance of the generator of Json Web Token
     * @param serverSecretToken Private static signature key of the server
     * @return instance of the generator
     */
    public static JWTProvider<User> createHMAC512(String serverSecretToken) {
        JWTGenerator<User> generator = (user, alg) -> {
            JWTCreator.Builder token = JWT.create()
                    .withClaim("user_identifier", user.identifier)
                    .withIssuer("NoiseModelling")
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            return token.sign(alg);
        };

        Algorithm algorithm = Algorithm.HMAC256(serverSecretToken);
        JWTVerifier verifier = JWT.require(algorithm).build();

        return new JWTProvider<>(algorithm, generator, verifier);
    }

}
