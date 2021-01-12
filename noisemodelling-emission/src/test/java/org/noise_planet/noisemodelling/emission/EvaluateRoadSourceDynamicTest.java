package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        String RoadSurface = "DEF";
        boolean Stud = true;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "1";
        int acc_type = 1;
        double LwStd = 1;
        int VehId = 10;

        RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId);
        rsParameters.setSlopePercentage(0);
        rsParameters.setCoeffVer(2);
        assertEquals(91.66, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void testRoadNoise2_speed0() {
        double speed = 0;
        int acc = 1;
        int FreqParam = 500;
        double Temperature = 0;
        String RoadSurface = "DEF";
        boolean Stud = false;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "3";
        int acc_type = 1;
        double LwStd = 1;
        int VehId = 10;
        RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId);
        rsParameters.setSlopePercentage(0);
        assertEquals(100.08, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
    }


    @Test
    public void testRoadNoise3_speed60() {
        int FreqParam = 8000;
        //int[] f = {63, 125, 250, 500, 1000, 2000, 4000, 8000};
        //for (int FreqParam : f) {
        double speed = 60;
        int acc = 0;

        double Temperature = 15;
        String RoadSurface = "NL08";
        boolean Stud = false;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "1";
        int acc_type = 1;
        double LwStd = 0;
        int VehId = 1;
        RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId);
        rsParameters.setSlopePercentage(0);

        assertEquals(78.62, EvaluateRoadSourceDynamic.evaluate(rsParameters), EPSILON_TEST1);
        // }
    }




}