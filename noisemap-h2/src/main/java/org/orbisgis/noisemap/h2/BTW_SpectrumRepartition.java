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
package org.orbisgis.noisemap.h2;

import org.h2gis.api.DeterministicScalarFunction;
import org.orbisgis.noisemap.core.TramSpectrumRepartition;

/**
 * Return the dB(A) value corresponding to the parameters.
 * @author Nicolas Fortin
 */
public class BTW_SpectrumRepartition extends DeterministicScalarFunction {

    public BTW_SpectrumRepartition() {
        addProperty(PROP_REMARKS, "\n" +
                "## BTW_SpectrumRepartition\n" +
                "Return the dB(A) level of third-octave frequency band using tramway emission spectrum.\n" +
                "BTW_SpectrumRepartition(int freqBand, double level)\n" +
                " - **freqBand** One of 100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000," +
                "5000\n" +
                " - **level** Global dB(A) value\n");
    }

    @Override
    public String getJavaStaticMethod() {
        return "spectrumRepartition";
    }

    /**
     * Return the dB(A) value corresponding to the the third octave frequency band.
     * @param freqBand Frequency band one of [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000]
     * @param level Global dB(A) value
     * @return third octave frequency band attenuation
     */
    public static double spectrumRepartition(int freqBand, double level) throws IllegalArgumentException {
        return level + TramSpectrumRepartition.getAttenuatedValue(freqBand);
    }
}
