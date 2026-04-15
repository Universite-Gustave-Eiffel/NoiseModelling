/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.utilities;

import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.event.Level;

/**
 * Redirects output to a logger.
 */
public class LoggingOutputStream extends OutputStream {
    protected final Logger logger;
    protected final Level level;
    protected final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public LoggingOutputStream(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void write(int b) {
        // UTF-8 newline (LF) is always 0x0A, and it never appears as
        // part of a multi-byte character sequence, making this check safe.
        if (b == '\n') {
            flushBuffer();
        } else if (b != '\r') {
            buffer.write(b);
        }
    }

    protected void flushBuffer() {
        if (buffer.size() > 0) {
            // Convert the captured bytes to a UTF-8 String
            String message = buffer.toString(StandardCharsets.UTF_8);

            // SLF4J 2.0 Fluent API handles the dynamic level
            logger.atLevel(level).log(message);

            buffer.reset();
        }
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        super.close();
    }
}
