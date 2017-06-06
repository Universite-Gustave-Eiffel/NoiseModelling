package org.orbisgis.noisemap.core;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos.*;

/**
 * @author Pierre Aumond - 27/04/2017.
 */

public class EvaluateRoadSourceCnossosTest {
    private static final double EPSILON_TEST1 = 0.01;
    // TODO Add more unitary tests
    @Test
    public void testRoadNoise1() {
        double lv_speed = 20;
        int lv_per_hour = 1000;
        double mv_speed = 4;
        int mv_per_hour = 4;
        double hgv_speed = 1;
        int hgv_per_hour = 1;
        double wav_speed = 30;
        int wav_per_hour = 10;
        double wbv_speed = 30;
        int wbv_per_hour = 20;
        int FreqParam = 125;
        double Temperature = 0;
        int RoadSurface = 0;
        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface );
        rsParameters.setFlowState(RSParametersCnossos.EngineState.SteadySpeed);
        rsParameters.setSurfaceAge(10);
        rsParameters.setSlopePercentage(-15);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(74.57 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void TestTableIII() {
        int Freq = 125;
        int VehCat = 2;
        int Coeff = 0;
        final Double coeff = getCoeff(Coeff,Freq, VehCat);
        //System.out.println(coeff);
        assertEquals(88.7 , coeff, EPSILON_TEST1);

    }

}