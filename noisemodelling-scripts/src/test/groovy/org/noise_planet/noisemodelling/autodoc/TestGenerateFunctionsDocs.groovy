/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.autodoc

import org.junit.jupiter.api.Test
import org.noise_planet.noisemodelling.webserver.script.ScriptMetadata

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class TestGenerateFunctionsDocs {

    @Test
    void testEmptyDefaultValueRendering() {
        def resource = getClass().getResource('/scripts/Acoustic_Tools/Combine_Noise_Maps.groovy')
        assertTrue(resource != null)
        Path scriptPath = Path.of(resource.toURI())
        ScriptMetadata metadata = new ScriptMetadata('Acoustic_Tools', scriptPath.toUri(), scriptPath.parent.toUri())

        String rst = GenerateFunctionsDocs.generateFunctionRst('Combine_Noise_Maps', metadata)

        assertTrue(rst.contains('Default: (empty string)'))
        assertTrue(rst.contains('Default: ``difference``'))
        assertFalse(rst.contains('Default: ````'))
        assertEquals(4, countOccurrences(rst, 'Default:'))
    }

    private static int countOccurrences(String value, String token) {
        int count = 0
        int index = 0
        while ((index = value.indexOf(token, index)) >= 0) {
            count++
            index += token.length()
        }
        return count
    }
}
