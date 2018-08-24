package org.orbisgis.noisemap.core;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.orbisgis.noisemap.core.EvaluateRoadSourceCnossos.*;

/**
 * @author Pierre Aumond - 27/04/2017.
 */

public class EvaluateRoadSourceCnossosTest {
    private static final double EPSILON_TEST1 = 0.01;

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T02() {
        double lv_speed = 70;
        int lv_per_hour = 1000;
        double mv_speed = 70;
        int mv_per_hour = 1000;
        double hgv_speed = 70;
        int hgv_per_hour = 1000;
        double wav_speed = 70;
        int wav_per_hour = 1000;
        double wbv_speed = 70;
        int wbv_per_hour = 1000;
        int FreqParam = 8000;
        double Temperature = 15;
        int RoadSurface = 1;
        double Pm_stud = 0.5;
        double Ts_stud = 4;
        double Junc_dist = 200;
        int Junc_type = 1;
        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud , Junc_dist, Junc_type);
        rsParameters.setSlopePercentage_without_limit(10);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(77.6711 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T03() {
        double lv_speed = 40;
        int lv_per_hour = 582;
        double mv_speed = 43;
        int mv_per_hour = 500;
        double hgv_speed = 45;
        int hgv_per_hour = 400;
        double wav_speed = 35;
        int wav_per_hour = 1000;
        double wbv_speed = 32;
        int wbv_per_hour = 1100;
        int FreqParam = 1000;
        double Temperature = 5;
        int RoadSurface = 2;
        double Pm_stud = 0.5;
        double Ts_stud = 4;
        double Junc_dist = 200;
        int Junc_type = 1;
        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud , Junc_dist, Junc_type);
        rsParameters.setSlopePercentage_without_limit(-5);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(79.6814 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T04() {
        double lv_speed = 40;
        int lv_per_hour = 58;
        double mv_speed = 43;
        int mv_per_hour = 50;
        double hgv_speed = 45;
        int hgv_per_hour = 40;
        double wav_speed = 35;
        int wav_per_hour = 100;
        double wbv_speed = 32;
        int wbv_per_hour = 100;
        int FreqParam = 8000;
        double Temperature = -5;
        int RoadSurface = 3;
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 200;
        int Junc_type = 1;

        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(-7);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(58.8222 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T05() {
        double lv_speed = 40;
        int lv_per_hour = 58;
        double mv_speed = 43;
        int mv_per_hour = 50;
        double hgv_speed = 45;
        int hgv_per_hour = 40;
        double wav_speed = 35;
        int wav_per_hour = 100;
        double wbv_speed = 32;
        int wbv_per_hour = 100;
        int FreqParam = 63;
        double Temperature = -5;
        int RoadSurface = 3;
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 50;
        int Junc_type = 1;
        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(-7);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(82.785 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T06() {
        double lv_speed = 70;
        int lv_per_hour = 1000;
        double mv_speed = 70;
        int mv_per_hour = 1000;
        double hgv_speed = 70;
        int hgv_per_hour = 1000;
        double wav_speed = 70;
        int wav_per_hour = 1000;
        double wbv_speed = 70;
        int wbv_per_hour = 1000;
        int FreqParam = 4000;
        double Temperature = 15;
        int RoadSurface = 1;
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 30;
        int Junc_type = 2;
        RSParametersCnossos rsParameters = new RSParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(10);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(85.4991 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
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