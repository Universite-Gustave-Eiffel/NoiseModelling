/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * Unit test of ST_TriangleContouring
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
