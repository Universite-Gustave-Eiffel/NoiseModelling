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

package org.noise_planet.noisemodelling.emission;

/**
 * Evaluation of Tramway sound source
 * @Author Nicolas Fortin
 */

public class EvalTramwaySource {
    /** Utility class */
    private EvalTramwaySource() {}
    public enum GROUND_TYPE {GRASS, RIGID}
	public static final  double grass_a_factor=26.;
	public static final  double rigid_a_factor=26.;
	public static final  double grass_b_factor=75.;
	public static final  double rigid_b_factor=78.;
	public static final  double speed_reference=40.;


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
        final double a_factor,b_factor;
        switch (ground_type) {
            case GRASS:
                a_factor=grass_a_factor;
                b_factor=grass_b_factor;
                break;
            case RIGID:
                a_factor=rigid_a_factor;
                b_factor=rigid_b_factor;
                break;
            default:
                throw new IllegalArgumentException("Unknown ground type");
        }
        double delta_corr=0;
        if(has_anti_vibration) {
            delta_corr+=-2;
        }
        // ///////////////////////
        // Noise Tramway
        return a_factor * Math.log10(speed / speed_reference) +
                b_factor +
                delta_corr +
                Math.log10(tw_per_hour);
    }
}