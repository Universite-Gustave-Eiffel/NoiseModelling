/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway.schall03;

/**
 * Compute sound emission levels of tramway sound source
 * The proposed model is a very simplified version, inspired by :
 * Moehler, U.; Liepert, M.; Kurze, U.J.; Onnich, H. The new German prediction model for railway noise 'Schall 03 2006'. In Noise and Vibration Mitigation for Rail Transportation Systems; Springer: Berlin, Germany, 2008; pp. 186–192.
 *
 * @author Nicolas Fortin, Université Gustave Eiffel
 */

public class TramwaySchall03 {

    // these factors are extracted from German directive Schall 03
    public static final double grass_a_factor = 26.;
    public static final double rigid_a_factor = 26.;
    public static final double grass_b_factor = 75.;
    public static final double rigid_b_factor = 78.;
    public static final double speed_reference = 40.;
    /** Utility class */
    private TramwaySchall03() {
    }

    /**
     * Evaluation of Tramway sound source
     * @param speed Average tramway speed
     * @param tw_per_hour Average tramway count per hour
     * @param ground_type Ground category
     * @param has_anti_vibration True if rails lies on anti-vibration system
     * @return Value in dB(A)
     */
    public static double evaluate(final double speed, final double tw_per_hour, final GROUND_TYPE ground_type,
                                  boolean has_anti_vibration) {
        final double a_factor, b_factor;
        switch (ground_type) {
            case GRASS:
                a_factor = grass_a_factor;
                b_factor = grass_b_factor;
                break;
            case RIGID:
                a_factor = rigid_a_factor;
                b_factor = rigid_b_factor;
                break;
            default:
                throw new IllegalArgumentException("Unknown ground type");
        }

        double delta_corr = 0;
        if (has_anti_vibration) {
            delta_corr += -2;
        }

        return a_factor * Math.log10(speed / speed_reference) +
                b_factor +
                delta_corr +
                Math.log10(tw_per_hour);
    }


    public enum GROUND_TYPE {GRASS, RIGID}
}