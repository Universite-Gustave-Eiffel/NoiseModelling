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
import org.orbisgis.noisemap.core.EvaluateRoadSource;
import org.orbisgis.noisemap.core.RSParameters;

/**
 * Return the dB(A) value corresponding to the parameters.You can specify from 3 to 10 parameters.
 * @author Nicolas Fortin
 */
public class BR_EvalSource extends DeterministicScalarFunction {

    public BR_EvalSource() {
        addProperty(PROP_REMARKS, "## BR_EvalSource\n" +
                "\n" +
                "Return the dB(A) global value of equivalent source power of combined light and heavy traffic.\n" +
                "\n" +
                "1. BR_EvalSource(double speed_load, int vl_per_hour, int pl_per_hour, [SurfacingCategory, engineState])\n" +
                "2. BR_EvalSource(double speed_load, int vl_per_hour, int pl_per_hour, double beginZ, double endZ," +
                "double road_length_2d, [SurfacingCategory, engineState)\n" +
                "3. BR_EvalSource(double speed_load, int vl_per_hour, int pl_per_hour, double speed_junction, " +
                "double speed_max,int compound_road_type,  double beginZ, double endZ, double road_length_2d, " +
                "boolean isQueue, [SurfacingCategory, engineState)\n" +
                "4. BR_EvalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, " +
                "double endZ,double road_length_2d)\n" +
                "\n" +
                "The function 3 evaluate the hv_speed using speed_junction, speed_max, " +
                "copound_roadtype and isQueue.\n" +
                "\n" +
                "The function 4 is the complete evaluation function without default parameters.\n" +
                "\n" +
                "Parameters:\n" +
                "\n" +
                " - **speed_load** Average speed of vehicles.\n" +
                " - **vl_per_hour** Average light vehicle by hour\n" +
                " - **pl_per_hour** Average heavy vehicle by hour\n" +
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
                " - **hv_speed** Average heavy vehicle speed\n" +
                " - **SurfacingCategory** 1 to 3 :" +
                "        `R1` Less noisy, BRUM 0/6 or BBDR0/10 or BBTM 0/6 or BBTM 0/10\n" +
                "        `R2` Average noisy BBSG 0/10 BBTL 0/10 - type 1 or BRUM 0/10 or ECF\n" +
                "        `R3` Much noisy BBDG 0/14 or BBTM 0/14 or E S6/10 or BC or ES 10/14" +
                " - **engineState** Traffic flow type. One of SteadySpeed, Acceleration, Deceleration, Starting, Stopping.\n" +
                "                    Note that Starting, Stopping states does not use the speed and surfacing category\n");
    }

    @Override
    public String getJavaStaticMethod() {
        return "evalSource";
    }

    private static RSParameters.EngineState engineStateFromString(String flowState) throws IllegalArgumentException {
        flowState = flowState.trim().toLowerCase();
        if(flowState.startsWith("ste")) {
            return RSParameters.EngineState.SteadySpeed;
        } else if(flowState.startsWith("acc")) {
            return RSParameters.EngineState.Acceleration;
        } else if(flowState.startsWith("dec")) {
            return RSParameters.EngineState.Deceleration;
        } else if(flowState.startsWith("sta")) {
            return RSParameters.EngineState.Starting;
        } else if(flowState.startsWith("sto")) {
            return RSParameters.EngineState.Stopping;
        } else {
            throw new IllegalArgumentException("Got traffic flow type "+flowState+", it must be one of SteadySpeed," +
                    " Acceleration, Deceleration, Starting, Stopping.");
        }
    }

    /**
     * Simplest road noise evaluation
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @return Noise level in dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour) {
        return EvaluateRoadSource.evaluate(new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour));
    }

    /**
     *
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param road_length_2d Road length (do not take account of Z)
     * @return Noise emission dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d) {
        RSParameters RSParameters = new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour);
        RSParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, road_length_2d));
        return EvaluateRoadSource.evaluate(RSParameters);
    }

    /**
     * @param lv_speed Average light vehicle speed
     * @param hv_speed Average heavy vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param road_length_2d Road length (do not take account of Z)
     * @return Noise emission dB(A)
     */
    public static double evalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d) {
        RSParameters rsParameters = new RSParameters(lv_speed, hv_speed, vl_per_hour, pl_per_hour);
        rsParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, road_length_2d));
        return EvaluateRoadSource.evaluate(rsParameters);
    }


    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param roadLength2d Road length (do not take account of Z)
     * @param isQueue If true use speed_junction in speed_load
     * @return Noise level in dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double speed_junction, double speed_max,
                                    int copound_roadtype,  double beginZ, double endZ, double roadLength2d, boolean isQueue) {
        RSParameters srcParameters = new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour);
        srcParameters.setSpeedFromRoadCaracteristics(speed_load, speed_junction, isQueue, speed_max, copound_roadtype);
        srcParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, roadLength2d));
        return EvaluateRoadSource.evaluate(srcParameters);
    }

    private static void checkRoadSurface(int roadSurface) {
        if(roadSurface < 1 || roadSurface > RSParameters.SurfaceCategory.values().length) {
            throw new IllegalArgumentException("Road surface must be between 1 and 3");
        }
    }



    /**
     * Simplest road noise evaluation
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param roadSurface Road surface between 1 and 3
     * @param flowState Flow state. One of SteadySpeed, Acceleration, Deceleration, Starting, Stopping.
     * @return Noise level in dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, int roadSurface, String flowState) {
        checkRoadSurface(roadSurface);
        RSParameters rsParameters = new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour);
        rsParameters.setFlowState(engineStateFromString(flowState));
        rsParameters.setSurfaceCategory(RSParameters.SurfaceCategory.values()[roadSurface - 1]);
        return EvaluateRoadSource.evaluate(rsParameters);
    }

    /**
     *
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param road_length_2d Road length (do not take account of Z)
     * @param roadSurface Road surface between 1 and 3
     * @param flowState Flow state. One of SteadySpeed, Acceleration, Deceleration, Starting, Stopping.
     * @return Noise emission dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d, int roadSurface, String flowState) {
        checkRoadSurface(roadSurface);
        RSParameters rsParameters = new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour);
        rsParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, road_length_2d));
        rsParameters.setFlowState(engineStateFromString(flowState));
        rsParameters.setSurfaceCategory(RSParameters.SurfaceCategory.values()[roadSurface - 1]);
        return EvaluateRoadSource.evaluate(rsParameters);
    }

    /**
     * @param lv_speed Average light vehicle speed
     * @param hv_speed Average heavy vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param road_length_2d Road length (do not take account of Z)
     * @param roadSurface Road surface between 1 and 3
     * @param flowState Flow state. One of SteadySpeed, Acceleration, Deceleration, Starting, Stopping.
     * @return Noise emission dB(A)
     */
    public static double evalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d, int roadSurface, String flowState) {
        checkRoadSurface(roadSurface);
        RSParameters rsParameters = new RSParameters(lv_speed,hv_speed, vl_per_hour, pl_per_hour);
        rsParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, road_length_2d));
        rsParameters.setFlowState(engineStateFromString(flowState));
        rsParameters.setSurfaceCategory(RSParameters.SurfaceCategory.values()[roadSurface - 1]);
        return EvaluateRoadSource.evaluate(rsParameters);
    }


    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param beginZ Road start height
     * @param endZ Road end height
     * @param roadLength2d Road length (do not take account of Z)
     * @param isQueue If true use speed_junction in speed_load
     * @param roadSurface Road surface between 1 and 3
     * @param flowState Flow state. One of SteadySpeed, Acceleration, Deceleration, Starting, Stopping.
     * @return Noise level in dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double speed_junction, double speed_max,
                                    int copound_roadtype,  double beginZ, double endZ, double roadLength2d, boolean isQueue,
                                    int roadSurface, String flowState) {
        checkRoadSurface(roadSurface);
        RSParameters srcParameters = new RSParameters(speed_load,speed_load, vl_per_hour, pl_per_hour);
        srcParameters.setSpeedFromRoadCaracteristics(speed_load, speed_junction, isQueue, speed_max, copound_roadtype);
        srcParameters.setSlopePercentage(RSParameters.computeSlope(beginZ, endZ, roadLength2d));
        srcParameters.setFlowState(engineStateFromString(flowState));
        srcParameters.setSurfaceCategory(RSParameters.SurfaceCategory.values()[roadSurface - 1]);
        return EvaluateRoadSource.evaluate(srcParameters);
    }
}
