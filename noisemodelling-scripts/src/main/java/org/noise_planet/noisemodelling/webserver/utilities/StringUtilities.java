/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.utilities;


import org.apache.commons.text.StringEscapeUtils;

import java.time.Duration;
import java.util.Locale;

public class StringUtilities {

    public static final int DURATION_DAYS = 0, DURATION_HOURS = 1, DURATION_MINUTES = 2, DURATION_SECONDS = 3;

    public static int[] splitDuration(Duration duration) {
        long totalTimeSeconds = duration.getSeconds();
        long days = totalTimeSeconds / 86400L;
        totalTimeSeconds = totalTimeSeconds % 86400L;
        long hours = totalTimeSeconds / 3600L;
        totalTimeSeconds = totalTimeSeconds % 3600;
        long minutes = totalTimeSeconds / 60L;
        totalTimeSeconds = totalTimeSeconds % 60;
        return new int[] {(int) days, (int) hours, (int) minutes, (int) totalTimeSeconds};
    }

    public static String durationToString(Duration duration) {
        String durationString;
        int[] durationArray = splitDuration(duration);
        if (durationArray[DURATION_DAYS] > 0) {
            durationString = String.format(Locale.ROOT, "%dd %dh %dm %ds", durationArray[DURATION_DAYS],
                    durationArray[DURATION_HOURS], durationArray[DURATION_MINUTES],
                    durationArray[DURATION_SECONDS]);
        } else {
            durationString = String.format(Locale.ROOT, "%dh %dm %ds", durationArray[DURATION_HOURS],
                    durationArray[DURATION_MINUTES], durationArray[DURATION_SECONDS]);
        }
        return durationString;
    }

    public static String escapeHtml(String input) {
        return StringEscapeUtils.escapeHtml4(input);
    }
}
