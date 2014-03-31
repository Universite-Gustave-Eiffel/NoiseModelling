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
package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import junit.framework.TestCase;
import org.gdms.sql.function.FunctionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test of ST_TriangleContouring.
 */
public class ST_TriangleContouringTest  extends TestCase{
    
    public ST_TriangleContouringTest() {
    }

    @Test
    public void testContouringTriangle() throws FunctionException {
        int subdividedTri = 0;
        //Input Data, a Triangle
        TriMarkers triangleData = new TriMarkers(new Coordinate(7,2),
                                                 new Coordinate(13,4),
                                                 new Coordinate(5,7),
                                                 2885245,2765123,12711064
                                                );
        //Iso ranges
        String isolevels_str = "31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20";
        LinkedList<Double> iso_lvls = new LinkedList<Double>();
        for (String isolvl : isolevels_str.split(",")) {
                iso_lvls.add(Double.valueOf(isolvl));
        }
        //Split the triangle into multiple triangles
        HashMap<Short,LinkedList<TriMarkers>> triangleToDriver=ST_TriangleContouring.processTriangle(triangleData, iso_lvls);
        for(Map.Entry<Short,LinkedList<TriMarkers>> entry : triangleToDriver.entrySet()) {
           subdividedTri+=entry.getValue().size();
        }
        assertTrue(subdividedTri==5);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
}
