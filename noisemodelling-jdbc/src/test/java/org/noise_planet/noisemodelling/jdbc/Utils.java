/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2.util.StringUtils;

import java.io.File;
import java.net.URISyntaxException;

public class Utils {
    public static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(NoiseMapByReceiverMakerTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+ StringUtils.quoteStringSQL(resourceFile.getPath());
    }
}
