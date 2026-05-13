/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 * <p>
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Locale;

public class AssumptionLoggerExtension implements TestWatcher {

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        // This method is called when an Assumption fails
        System.err.printf(Locale.ROOT, "Test '%s' cancelled (Assumption failed) : %s%n", context.getDisplayName(),
                cause.getMessage());
    }
}