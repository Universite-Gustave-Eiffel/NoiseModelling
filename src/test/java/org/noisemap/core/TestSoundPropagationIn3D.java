package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestSoundPropagationIn3D extends TestCase {

    public TestSoundPropagationIn3D(){
    } 
    
    public void test(){
    
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
				new Coordinate(30., 5.,2.), new Coordinate(30., 30.,2.),
				new Coordinate(15., 30.,2.), new Coordinate(15., 5.,2.) };
           Coordinate[] building2Coords = { new Coordinate(25., 5.,0.),
				new Coordinate(30., 5.,5.), new Coordinate(30., 30.,5.),
				new Coordinate(15., 30.,5.), new Coordinate(25., 5.,5.) };
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null);     
           FastObstructionTest ft= new FastObstructionTest();
           ft.addGeometry(building1, 2.0);
           ft.addGeometry(building2, 5.0);
           
    }
}