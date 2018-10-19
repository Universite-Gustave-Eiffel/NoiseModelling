/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.h2;

import org.h2gis.api.DeterministicScalarFunction;
import org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos;
import org.orbisgis.noisemap.core.RSParametersCnossos;

/**
 * Return the dB value corresponding to the parameters using the CNOSSOS Method.
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond 06/08/2018
 */
public class BR_EvalSourceC extends DeterministicScalarFunction {

    public BR_EvalSourceC() {
        addProperty(PROP_REMARKS, "## BR_EvalSourceC\n" +
                "\n" +
                "Return the dB value per frequency of equivalent source power of combined light, medium, heavy, light and heavy two-wheels traffic.\n" +
                "\n" +
                "1. BR_EvalSource(double lv_speed, double mv_speed, double hv_speed, double wav_speed, double wbv_speed,\n" +
                "                                     int vl_per_hour, int ml_per_hour, int pl_per_hour, int wa_per_hour, int wb_per_hour,\n" +
                "                                     double beginZ, double endZ, double roadLength2d,\n" +
                "                                     int RoadSurface, double Temperature, double Ts_stud, double Pm_stud,  double Junc_dist, int Junc_type, int FreqParam)\n" +
                "\n" +
                "    * @param lv_speed Average vehicle speed\n" +
                "        * @param mv_speed Average vehicle speed\n" +
                "* @param hv_speed Average vehicle speed\n" +
                "* @param wav_speed Average light 2w vehicle speed\n" +
                "* @param wbv_speed Average heavy 2w  vehicle speed\n" +
                "* @param vl_per_hour Average light vehicle per hour\n" +
                "* @param ml_per_hour Average medium vehicle per hour\n" +
                "* @param pl_per_hour Average heavy vehicle per hour\n" +
                "* @param wa_per_hour Average light 2w vehicle per hour\n" +
                "* @param wb_per_hour Average heavy 2w vehicle per hour\n" +
                "* @param beginZ Road start height\n" +
                "* @param endZ Road end height\n" +
                "* @param roadLength2d Road length (do not take account of Z)\n" +
                "* @param FreqParam Studied Frequency\n" +
                " * @param Temperature Temperature(Celsius)\n" +
                " * @param RoadSurface Road surface between 0 and 14\n" +
                " * @param Ts_stud A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres and during .\n" +
                "* @param Pm_stud Average proportion of vehicles equipped with studded tyres\n" +
                "* @param Junc_dist Distance to junction\n" +
                "* @param Junc_type Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)");
    }

    @Override
    public String getJavaStaticMethod() {
        return "evalSourceC";
    }

    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     *
     * @param lv_speed     Average vehicle speed
     * @param mv_speed     Average vehicle speed
     * @param hv_speed     Average vehicle speed
     * @param wav_speed    Average light 2w vehicle speed
     * @param wbv_speed    Average heavy 2w  vehicle speed
     * @param vl_per_hour  Average light vehicle per hour
     * @param ml_per_hour  Average medium vehicle per hour
     * @param pl_per_hour  Average heavy vehicle per hour
     * @param wa_per_hour  Average light 2w vehicle per hour
     * @param wb_per_hour  Average heavy 2w vehicle per hour
     * @param beginZ       Road start height
     * @param endZ         Road end height
     * @param roadLength2d Road length (do not take account of Z)
     * @param FreqParam    Studied Frequency
     * @param Temperature  Temperature(Celsius)
     * @param RoadSurface  Road surface name ex: NL01
     * @param Ts_stud      A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres and during .
     * @param Pm_stud      Average proportion of vehicles equipped with studded tyres
     * @param Junc_dist    Distance to junction
     * @param Junc_type    Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     * @return Noise level in dB
     */
    public static double evalSourceC(double lv_speed, double mv_speed, double hv_speed, double wav_speed, double wbv_speed,
                                     int vl_per_hour, int ml_per_hour, int pl_per_hour, int wa_per_hour, int wb_per_hour,
                                     double beginZ, double endZ, double roadLength2d,
                                     String RoadSurface, double Temperature, double Ts_stud, double Pm_stud, double Junc_dist, int Junc_type, int FreqParam) {
        RSParametersCnossos srcParameters = new RSParametersCnossos(lv_speed, mv_speed, hv_speed, wav_speed, wbv_speed,
                vl_per_hour, ml_per_hour, pl_per_hour, wa_per_hour, wb_per_hour,
                FreqParam, Temperature, RoadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
        srcParameters.setSlopePercentage(RSParametersCnossos.computeSlope(beginZ, endZ, roadLength2d));
        return EvaluateRoadSourceCnossos.evaluate(srcParameters);
    }
}
