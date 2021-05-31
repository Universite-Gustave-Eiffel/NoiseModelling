/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Pierre Aumond, Univ. Gustave Eiffel
 */

public class EvaluateRoadSourceCnossosTest {
    private static final double EPSILON_TEST1 = 0.01;
    private static final int[] FREQUENCIES = new int[] {63,125,250,500,1000,2000,4000,8000};

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T02() throws IOException {
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
        String RoadSurface = "NL01";
        double Pm_stud = 0.5;
        double Ts_stud = 4;
        double Junc_dist = 200;
        int Junc_type = 1;
        RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud , Junc_dist, Junc_type);
        rsParameters.setSlopePercentage_without_limit(10);
        rsParameters.setCoeffVer(1);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(77.6711 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T03() throws IOException {
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
        String RoadSurface = "NL02";
        double Pm_stud = 0.5;
        double Ts_stud = 4;
        double Junc_dist = 200;
        int Junc_type = 1;
        RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud , Junc_dist, Junc_type);
        rsParameters.setSlopePercentage_without_limit(-5);
        rsParameters.setCoeffVer(1);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(79.6814 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T04() throws IOException {
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
        String RoadSurface = "NL03";
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 200;
        int Junc_type = 1;

        RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(-7);
        rsParameters.setCoeffVer(1);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(58.8222 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);

    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T05() throws IOException {
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
        String RoadSurface = "NL03";
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 50;
        int Junc_type = 1;
        RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(-7);
        rsParameters.setCoeffVer(1);
        //System.out.println(EvaluateRoadSourceCnossos.evaluate(rsParameters));
        assertEquals(82.785 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    /** based on CNOSSOS_Road_Output.csv and the CNOSSOS_DLL_CONSOLE.exe**/
    @Test
    public void T06() throws IOException {
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
        String RoadSurface = "NL01";
        double Pm_stud = 0.5;
        double Ts_stud = 4.;
        double Junc_dist = 30;
        int Junc_type = 2;
        RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(lv_speed,  mv_speed,  hgv_speed,  wav_speed,  wbv_speed,  lv_per_hour,  mv_per_hour,  hgv_per_hour,  wav_per_hour,  wbv_per_hour,  FreqParam,  Temperature,  RoadSurface,Ts_stud, Pm_stud, Junc_dist, Junc_type );
        rsParameters.setSlopePercentage_without_limit(10);
        rsParameters.setCoeffVer(1);
        assertEquals(85.4991 , EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void TestTableIII() {
        int Freq = 125;
        String VehCat = "2";
        final Double coeff = EvaluateRoadSourceCnossos.getCoeff("ar",Freq, VehCat, 1);
        assertEquals(88.7 , coeff, EPSILON_TEST1);

    }


    @Test
    public void CnossosEmissionTest() throws IOException {
        String vehCat="1";
        double vehiclePerHour = 1000;
        double vehicleSpeed = 20;
        double tsStud = 0.5;
        String surfRef = "NL01";
        double temperature = -5;
        double pmStud = 1;
        double slope = -15;
        double juncDist = 200;
        int juncType = 1;
        double[] expectedValues = new double[]{88.421,77.1136,75.5712,75.6919,73.6689,71.3471,68.1195,63.4796};
        for(int idFreq = 1; idFreq < FREQUENCIES.length; idFreq++) {
            RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(vehicleSpeed, vehicleSpeed, vehicleSpeed,
                    vehicleSpeed, vehicleSpeed, "1".equals(vehCat) ? vehiclePerHour : 0,
                    "2".equals(vehCat) ? vehiclePerHour : 0, "3".equals(vehCat) ? vehiclePerHour : 0,
                    "4a".equals(vehCat) ? vehiclePerHour : 0, "4b".equals(vehCat) ? vehiclePerHour : 0,
                    FREQUENCIES[idFreq], temperature, surfRef, tsStud, pmStud, juncDist, juncType);
            rsParameters.setSlopePercentage(slope);
            rsParameters.setCoeffVer(1);
            assertEquals(String.format("%d Hz", FREQUENCIES[idFreq]), expectedValues[idFreq], EvaluateRoadSourceCnossos.evaluate(rsParameters), EPSILON_TEST1);
        }
    }
    @Test
    public void CnossosEmissionTestwithSlope() throws IOException {
        String vehCat="1";
        double vehiclePerHour = 1000;
        double vehicleSpeed = 20;
        double tsStud = 0.5;
        String surfRef = "NL01";
        double temperature = -5;
        double pmStud = 1;
        double slope = -15;
        double juncDist = 200;
        int juncType = 1;
        double[] expectedValues = new double[]{88.421,77.09,75.54,75.01,72.79,71.13,68.07,63.44};
        for(int idFreq = 1; idFreq < FREQUENCIES.length; idFreq++) {
            RoadSourceParametersCnossos rsParameters = new RoadSourceParametersCnossos(vehicleSpeed, vehicleSpeed, vehicleSpeed,
                    vehicleSpeed, vehicleSpeed, "1".equals(vehCat) ? vehiclePerHour : 0,
                    "2".equals(vehCat) ? vehiclePerHour : 0, "3".equals(vehCat) ? vehiclePerHour : 0,
                    "4a".equals(vehCat) ? vehiclePerHour : 0, "4b".equals(vehCat) ? vehiclePerHour : 0,
                    FREQUENCIES[idFreq], temperature, surfRef, tsStud, pmStud, juncDist, juncType);
            rsParameters.setSlopePercentage(slope);
            rsParameters.setWay(3);
            rsParameters.setCoeffVer(1);
            double result = EvaluateRoadSourceCnossos.evaluate(rsParameters);
            assertEquals(String.format("%d Hz", FREQUENCIES[idFreq]), expectedValues[idFreq], result, EPSILON_TEST1);
        }
    }
}