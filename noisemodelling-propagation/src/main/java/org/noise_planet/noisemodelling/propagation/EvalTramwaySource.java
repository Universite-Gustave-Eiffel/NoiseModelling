/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

/**
 * Evaluation of Tramway sound source
 * @author Nicolas Fortin
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