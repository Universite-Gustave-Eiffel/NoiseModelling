package org.orbisgis.noisemap.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos.getCoeff;

/**
 * @author Pierre Aumond / Arnaud Can - 21/08/2018
 */

public class EvaluateRoadSourceDynamicTest {
    private static final double EPSILON_TEST1 = 0.01;

    @Test
    public void testRoadNoise1() {
        double speed = 20;
        int acc = 1;
        int FreqParam = 125;
        double Temperature = 20;
        int RoadSurface = 0;
        double Pm_stud = 0;
        double Ts_stud = 3;
        double Junc_dist = 50;
        int Junc_type = 1;
        int veh_type = 3;
        int acc_type= 1;
        RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  veh_type, acc_type, FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud , Junc_dist, Junc_type);
        rsParameters.setSlopePercentage(0);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(102.960, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
    }



}