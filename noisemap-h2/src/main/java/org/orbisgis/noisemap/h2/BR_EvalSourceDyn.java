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
import org.orbisgis.noisemap.core.EvaluateRoadSourceDynamic;
import org.orbisgis.noisemap.core.RSParametersDynamic;

/**
 * Return the dB value corresponding to the parameters using the CNOSSOS Method.
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond 06/08/2018 - 21/08/2018
 * @author Arnaud Can 21/08/2018
 */
public class BR_EvalSourceDyn extends DeterministicScalarFunction {
    public BR_EvalSourceDyn() {
        addProperty(PROP_REMARKS, "## BR_EvalSourceDyn\n" +
                "\n" +
                "Return the dB value per frequency of equivalent source power of an individual vehicle (light, medium, heavy, light and heavy two-wheels traffic).\n" +
                "\n" +
                "1. BR_EvalSourceDyn(double speed, double acceleration, int veh_type, double beginZ, double endZ, double roadLength2d,\n" +
                "                                       int RoadSurface, double Temperature, double Ts_stud, double Pm_stud, int acc_type, double Junc_dist, int Junc_type, int FreqParam)\n" +
                "\n" +
                "     * @param speed Vehicle speed\n" +
                "     * @param acceleration Vehicle acceleration (optional, used when acc_type > 1) \n" +
                "     * @param veh_type Vehicle type (CNOSSOS categories)\n" +
                "     * @param acc_type Acceleration mode (1 = Distance to Junction (CNOSSOS), 2= Correction from IMAGINE with bounds , 3 = Correction from IMAGINE without bounds)\n" +
                "     * @param beginZ Road start height\n" +
                "     * @param endZ Road end height\n" +
                "     * @param roadLength2d Road length (do not take account of Z)\n" +
                "     * @param FreqParam Studied Frequency\n" +
                "     * @param Temperature Temperature(Celsius)\n" +
                "     * @param RoadSurface Road surface between 0 and 14\n" +
                "     * @param Stud true = equipped with studded tyres\n" +
                "     * @param Junc_dist Distance to junction (optional, used when acc_type = 1)\n" +
                "     * @param Junc_type Type of junction; k = 1 for a crossing with traffic lights ; k = 2 for a roundabout (optional, used when acc_type = 1)\n" +
                "     * @param LwStd Standard Deviation of Lw");
    }

    @Override
    public String getJavaStaticMethod() {
        return "evalSourceDyn";
    }

    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     *
     * @param speed        Vehicle speed
     * @param acceleration Vehicle acceleration
     * @param veh_type     Vehicle type (CNOSSOS categories)
     * @param acc_type     Acceleration mode (1 = Distance to Junction (CNOSSOS), 2= Correction from IMAGINE with bounds , 3 = Correction from IMAGINE without bounds)
     * @param beginZ       Road start height
     * @param endZ         Road end height
     * @param roadLength2d Road length (do not take account of Z)
     * @param FreqParam    Studied Frequency
     * @param Temperature  Temperature(Celsius)
     * @param RoadSurface  Road surface between 0 and 14
     * @param Stud      True = vehicles equipped with studded tyres
     * @param Junc_dist    Distance to junction
     * @param Junc_type    Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     * @param LwStd Standard Deviation of Lw
     * @param VehId Vehicle ID used as a seed for LwStd
     * @return Noise level in dB
     */
    public static double evalSourceDyn(double speed, double acceleration, int veh_type, int acc_type, double beginZ, double endZ, double roadLength2d,
            int FreqParam, double Temperature, int RoadSurface, boolean Stud, double Junc_dist, int Junc_type, double LwStd, int VehId) {
        //checkRoadSurface(roadSurface);
        RSParametersDynamic srcParameters = new RSParametersDynamic(speed, acceleration, veh_type, acc_type,
                FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId);
        srcParameters.setSlopePercentage(RSParametersDynamic.computeSlope(beginZ, endZ, roadLength2d));
        return EvaluateRoadSourceDynamic.evaluate(srcParameters);
    }
}
