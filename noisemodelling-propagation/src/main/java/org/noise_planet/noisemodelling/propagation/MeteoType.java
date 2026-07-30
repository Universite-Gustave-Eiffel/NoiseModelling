/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of meteo parameters used by the propagation model.
 *
 * @author Martin Glesser
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MeteoType {
    // CNOSSOS
    FAVOURABLE("favorable"), // CNOSSOS Favourable condition
    HOMOGENEOUS("homogeneous"); // Null vertical gradient of effective sound celerity

    private final String description;

    // Constructor (runs once for each constant above)
    MeteoType(String description) {
        this.description = description;
    }
    @JsonValue
    public String getMeteoType() {
        return description;
    }
}
