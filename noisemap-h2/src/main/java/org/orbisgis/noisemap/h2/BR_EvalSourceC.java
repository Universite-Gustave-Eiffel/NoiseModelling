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

import org.h2gis.h2spatialapi.DeterministicScalarFunction;
import org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos;
import org.orbisgis.noisemap.core.RSParametersCnossos;

/**
 * Return the dB value corresponding to the parameters.You can specify from 3 to 10 parameters.
 * @author Nicolas Fortin
 * @author Pierre Aumond 09/05/2017
 */
public class BR_EvalSourceC extends DeterministicScalarFunction {

    public BR_EvalSourceC() {
        addProperty(PROP_REMARKS, "## BR_EvalSourceC\n" +
                "\n" +
                "Return the dB value per frequency of equivalent source power of combined light, medium, heavy, light and heavy two-wheels traffic.\n" +
                "\n" +
                "1. BR_EvalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, " +
                "double endZ,double road_length_2d, freq)\n" +
                "\n" +
                "The function 3 evaluate the v_speed using speed_junction, speed_max, " +
                "copound_roadtype and isQueue.\n" +
                "\n" +
                "The function 4 is the complete evaluation function without default parameters.\n" +
                "\n" +
                "Parameters:\n" +
                "\n" +
                " - **speed_load** Average speed of vehicles.\n" +
                " - **vl_per_hour** Average light vehicle by hour\n" +
                " - **ml_per_hour** Average medium vehicle by hour\n" +
                " - **pl_per_hour** Average heavy vehicle by hour\n" +
                " - **wa_per_hour** Average light two-wheels vehicle by hour\n" +
                " - **wb_per_hour** Average heavy two-wheels vehicle by hour\n" +
                " - **beginZ** Beginning of road height. Used to compute slope.\n" +
                " - **endZ** End of road height. Used to compute slope.\n" +
                " - **road_length_2d** 2D length of road. Used to compute slope.\n" +
                " - **speed_junction** Speed of vehicle at road junction.\n" +
                " - **speed_max** Legal maximum speed of the road.\n" +
                " - **copound_roadtype** Road type:\n" +
                "`10` Highway 2x2 130 km/h\n" +
                "`21` 2x2 way 110 km/h\n" +
                "`22` 2x2 way 90km/h off belt-way\n" +
                "`23` Belt-way\n" +
                "`31` Interchange ramp\n" +
                "`32` Off boulevard roundabout circular junction\n" +
                "`37` Inside-boulevard roundabout circular junction\n" +
                "`41` lower level 2x1 way 7m 90km/h\n" +
                "`42` Standard 2x1 way 90km/h\n" +
                "`43` 2x1 way\n" +
                "`51` extra boulevard 70km/h\n" +
                "`52` extra boulevard 50km/h\n" +
                "`53` extra boulevard Street 50km/h\n" +
                "`54` extra boulevard Street <50km/h\n" +
                "`56` in boulevard 70km/h\n" +
                "`57` in boulevard 50km/h\n" +
                "`58` in boulevard Street 50km/h\n" +
                "`59` in boulevard Street <50km/h\n" +
                "`61` Bus-way boulevard 70km/h\n" +
                "`62` Bus-way boulevard 50km/h\n" +
                "`63` Bus-way extra boulevard Street\n" +
                "`64` Bus-way extra boulevard Street\n" +
                "`68` Bus-way in boulevard Street 50km/h\n" +
                "`69` Bus-way in boulevard Street <50km/h\n" +
                " - **isQueue** If this segment of road is behind a traffic light. If vehicles behavior is to stop at" +
                " the end of the road.\n" +
                " - **lv_speed** Average light vehicle speed\n" +
                " - **mv_speed** Average medium vehicle speed\n" +
                " - **hv_speed** Average heavy vehicle speed\n" +
                " - **wav_speed** Average light two-wheels vehicle speed\n" +
                " - **wbv_speed** Average heavy two-wheels vehicle speed\n" +
                " - **SurfacingCategory** 0 to 14 :" +
                "        see CNOSSOS documentation" +
                " - **freq** Frequency, only octave band are accepted from 63 hz to 8 khz.\n");
    }

    @Override
    public String getJavaStaticMethod() {
        return "evalSourceC";
    }

    private static RSParametersCnossos.EngineState engineStateFromString(String flowState) throws IllegalArgumentException {
        flowState = flowState.trim().toLowerCase();
        if (flowState.startsWith("ste")) {
            return RSParametersCnossos.EngineState.SteadySpeed;
        } else if (flowState.startsWith("acc")) {
            return RSParametersCnossos.EngineState.Acceleration;
        } else if (flowState.startsWith("dec")) {
            return RSParametersCnossos.EngineState.Deceleration;
        } else if (flowState.startsWith("sta")) {
            return RSParametersCnossos.EngineState.Starting;
        } else if (flowState.startsWith("sto")) {
            return RSParametersCnossos.EngineState.Stopping;
        } else {
            throw new IllegalArgumentException("Got traffic flow type " + flowState + ", it must be one of SteadySpeed," +
                    " Acceleration, Deceleration, Starting, Stopping.");
        }
    }

    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     * @param lv_speed Average vehicle speed
     * @param mv_speed Average vehicle speed
     * @param hv_speed Average vehicle speed
     * @param wav_speed Average vehicle speed
     * @param wbv_speed Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param ml_per_hour Average medium vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param wa_per_hour Average light 2w vehicle per hour
     * @param wb_per_hour Average heavy 2w vehicle per hour
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param roadLength2d Road length (do not take account of Z)
     * @param isQueue If true use speed_junction in speed_load
     * @param FreqParam Studied Frequency
     * @param Temperature Temperature(Celsius)
     * @param RoadSurface Road surface between 0 and 14
     * @return Noise level in dB
     */
    public static double evalSourceC(double lv_speed, double mv_speed, double hv_speed, double wav_speed, double wbv_speed,
                                     int vl_per_hour, int ml_per_hour, int pl_per_hour, int wa_per_hour, int wb_per_hour,
                                     double speed_junction, boolean isQueue, double speed_max, int copound_roadtype,
                                     double beginZ, double endZ, double roadLength2d,
                                     int RoadSurface, double Temperature, int FreqParam) {
        //checkRoadSurface(roadSurface);
        RSParametersCnossos srcParameters = new RSParametersCnossos(lv_speed, mv_speed, hv_speed, wav_speed, wbv_speed,
                vl_per_hour, ml_per_hour, pl_per_hour, wa_per_hour, wb_per_hour,
                FreqParam, Temperature, RoadSurface);
        srcParameters.setSpeedFromRoadCaracteristics(lv_speed, speed_junction, isQueue, speed_max, copound_roadtype);
        srcParameters.setSlopePercentage(RSParametersCnossos.computeSlope(beginZ, endZ, roadLength2d));
        return EvaluateRoadSourceCnossos.evaluate(srcParameters);
    }
}
