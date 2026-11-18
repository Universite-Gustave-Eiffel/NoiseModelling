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
import java.nio.file.Files;
import java.nio.file.Path;
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
     * Opens a safe input stream for the given URI string.
     * <p>
     * Resources referenced with the {@code file} scheme are read directly from the local
     * filesystem, while the {@code http} and {@code https} schemes are fetched through the
     * modern HttpClient API. Any other scheme (or a malformed URI) is rejected.
     *
     * @param urlString the URI of the resource to open; must use an allowed scheme
     *                  (http, https or file)
     * @return An InputStream of the resource content
     * @throws IOException If the URI is invalid, the scheme is not allowed, a network error
     *                     occurs, the server returns a non-200 code, the local file cannot be
     *                     read, or the operation is interrupted
     */
    public static InputStream openSafeStream(String urlString) throws IOException {
        final URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI: " + urlString, e);
        }
        final String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IOException("URI has no scheme: " + urlString);
        }
        switch (scheme.toLowerCase()) {
            case "file":
                return Files.newInputStream(Path.of(uri));
            case "http":
            case "https":
                return openHttpStream(uri, urlString);
            default:
                throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * Fetches an HTTP(S) resource as an InputStream using the modern HttpClient API.
     * This method validates the HTTP status code before returning the stream.
     *
     * @param uri The validated http or https URI of the resource.
     * @param urlString The original URL string, used for error reporting.
     * @return An InputStream of the response body.
     * @throws IOException If a network error occurs, or the server returns a non-200 code or the operation is interrupted.
     */
    private static InputStream openHttpStream(URI uri, String urlString) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
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