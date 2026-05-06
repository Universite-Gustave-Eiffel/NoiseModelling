/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.secure;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Optional;

/**
 * Derived from
 * <a href="https://github.com/kmehrunes/javalin-jwt">...</a>
 */
public class JWTProvider<T> {
    private final Algorithm algorithm;
    private final JWTGenerator<T> generator;
    private final JWTVerifier verifier;

    public JWTProvider(Algorithm algorithm, JWTGenerator<T> generator, JWTVerifier verifier) {
        this.algorithm = algorithm;
        this.generator = generator;
        this.verifier = verifier;
    }

    public String generateToken(T obj) {
        return generator.generate(obj, algorithm);
    }

    public Optional<DecodedJWT> validateToken(String token) {
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }
}
