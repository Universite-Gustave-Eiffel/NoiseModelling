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

import org.h2gis.h2spatialapi.DeterministicScalarFunction;
import org.h2gis.h2spatialapi.Function;
import org.orbisgis.noisemap.core.EvalRoadSource;
import org.osgi.service.component.annotations.Component;

/**
 * Return the dB(A) value corresponding to the parameters.You can specify from 3 to 10 parameters.
 * @author Nicolas Fortin
 */
@Component(service = Function.class)
public class BR_EvalSource extends DeterministicScalarFunction {

    public BR_EvalSource() {
        addProperty(PROP_REMARKS, "Return the dB(A) value corresponding to the parameters.\n" +
                "SELECT BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount);\n" +
                "SELECT BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount,Zbegin,Zend,roadLength);\n" +
                "SELECT BR_EvalSource(lightVehicleSpeed,heavyVehicleSpeed,lightVehicleCount,heavyVehicleCount,Zbegin,Zend,roadLength);\n" +
                "SELECT BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount,junction speed,speedMax,roadType,Zbegin,Zend,roadLength,isQueue);");
    }

    @Override
    public String getJavaStaticMethod() {
        return "evalSource";
    }

    /**
     * Simplest road noise evaluation
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @return Noise level in dB(A)
     */
    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour) {
        return EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour);
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
        double slope = EvalRoadSource.computeSlope(beginZ, endZ, road_length_2d);
        return EvalRoadSource.evaluate(vl_per_hour, pl_per_hour, speed_load, speed_load, slope);
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
        double slope = EvalRoadSource.computeSlope(beginZ, endZ, road_length_2d);
        return EvalRoadSource.evaluate(vl_per_hour, pl_per_hour, lv_speed, hv_speed, slope);
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
        return EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour,
                speed_junction, speed_max, copound_roadtype, beginZ, endZ, roadLength2d, isQueue);
    }
}
