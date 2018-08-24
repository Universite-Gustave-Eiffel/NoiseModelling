package org.orbisgis.noisemap.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos.getCoeff;

/**
 * @author Pierre Aumond / Arnaud Can - 21/08/2018
 */

public class EvaluateRoadSourceDynamicTest {
    private static final double EPSILON_TEST1 = 0.1;

    @Test
    public void testRoadNoise1() {
        double speed = 50;
        int acc = 1;
        int FreqParam = 500;
        double Temperature = 0;
        int RoadSurface = 0;
        boolean Stud = true;
        double Junc_dist = 200;
        int Junc_type = 1;
        int veh_type = 1;
        int acc_type= 1;
        double LwStd= 1;
        int VehId = 10;
        RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  veh_type, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,VehId);
        rsParameters.setSlopePercentage(0);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(91.71, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void testRoadNoise2_speed0() {
        double speed = 0;
        int acc = 1;
        int FreqParam = 500;
        double Temperature = 0;
        int RoadSurface = 0;
        boolean Stud = false;
        double Junc_dist = 200;
        int Junc_type = 1;
        int veh_type = 3;
        int acc_type= 1;
        double LwStd= 1;
        int VehId = 10;
        RSParametersDynamic rsParameters = new RSParametersDynamic(speed,  acc,  veh_type, acc_type, FreqParam,  Temperature,  RoadSurface,Stud, Junc_dist, Junc_type,LwStd,VehId);
        rsParameters.setSlopePercentage(0);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(98.18, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
    }




}