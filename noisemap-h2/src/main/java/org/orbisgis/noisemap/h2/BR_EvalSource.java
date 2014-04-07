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

    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour) {
        return EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour);
    }

    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d) {
        double slope = EvalRoadSource.computeSlope(beginZ, endZ, road_length_2d);
        return EvalRoadSource.evaluate(vl_per_hour, pl_per_hour, speed_load, speed_load, slope);
    }

    public static double evalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, double endZ,
                                    double road_length_2d) {
        double slope = EvalRoadSource.computeSlope(beginZ, endZ, road_length_2d);
        return EvalRoadSource.evaluate(vl_per_hour, pl_per_hour, lv_speed, hv_speed, slope);
    }

    public static double evalSource(double speed_load, int vl_per_hour, int pl_per_hour, double speed_junction, double speed_max,
                                    int copound_roadtype,  double beginZ, double endZ, double roadLength2d, boolean isQueue) {
        return EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour,
                speed_junction, speed_max, copound_roadtype, beginZ, endZ, roadLength2d, isQueue);
    }
}
