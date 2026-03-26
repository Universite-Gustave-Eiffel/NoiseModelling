/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.emission.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

public class UriUtils {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "file");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Validates if a string is a well-formed URI and uses an approved scheme
     * (http, https, or file).
     *
     * @param uriString the string to validate; can be null or empty
     * @return {@code true} if the string is a valid URI with an allowed scheme,
     *         {@code false} otherwise
     */
    public static boolean isValidUri(String uriString) {
        if (uriString == null || uriString.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(uriString);
            String scheme = uri.getScheme();
            return scheme != null && ALLOWED_SCHEMES.contains(scheme.toLowerCase());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Fetches a resource as an InputStream using the modern HttpClient API.
     * This method validates the HTTP status code before returning the stream.
     *
     * @param urlString The full URL of the resource.
     * @return An InputStream of the response body.
     * @throws IOException If a network error occurs, or the server returns a non-200 code or the operation is interrupted.
     */
    public static InputStream openSafeStream(String urlString) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch " + urlString + ". HTTP Status: " + response.statusCode());
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }
}