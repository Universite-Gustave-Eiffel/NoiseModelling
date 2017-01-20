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

/**
 * Unit test for noise emission
 */
public class EvaluateRoadSourceTest {
    private static final double EPSILON_TEST1 = 0.01;

    @Test
    public void testRoadNoise1() {
        double lv_speed = 110;
        int lv_per_hour = 3000;
        double hgv_speed = 100;
        int hgv_per_hour = 500;
        RSParameters rsParameters = new RSParameters(lv_speed, hgv_speed, lv_per_hour, hgv_per_hour);
        rsParameters.setFlowState(RSParameters.EngineState.SteadySpeed);
        rsParameters.setSurfaceCategory(RSParameters.SurfaceCategory.R2);
        rsParameters.setSurfaceAge(8);
        rsParameters.setSlopePercentage(0.5);
        assertEquals(94.9 , EvaluateRoadSource.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void testRoadNoise2() {
        double lv_speed = 110;
        int lv_per_hour = 0;
        double hgv_speed = 100;
        int hgv_per_hour = 0;
        RSParameters rsParameters = new RSParameters(lv_speed, hgv_speed, lv_per_hour, hgv_per_hour);
        rsParameters.setFlowState(RSParameters.EngineState.SteadySpeed);
        rsParameters.setSurfaceCategory(RSParameters.SurfaceCategory.R2);
        rsParameters.setSurfaceAge(8);
        rsParameters.setSlopePercentage(0.5);
        assertEquals(Double.NEGATIVE_INFINITY , EvaluateRoadSource.evaluate(rsParameters), EPSILON_TEST1);
    }
}