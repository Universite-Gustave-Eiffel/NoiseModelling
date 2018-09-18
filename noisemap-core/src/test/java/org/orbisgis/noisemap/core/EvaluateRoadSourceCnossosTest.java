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
        String RoadSurface = "NL01";
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
        String RoadSurface = "NL02";
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
        String RoadSurface = "NL03";
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
        String RoadSurface = "NL03";
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
        String RoadSurface = "NL01";
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
        String VehCat = "2";
        final Double coeff = getCoeff("ar",Freq, VehCat);
        assertEquals(88.7 , coeff, EPSILON_TEST1);

    }

}