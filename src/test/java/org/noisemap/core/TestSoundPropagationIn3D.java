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
    
    public void test() throws LayerDelaunayError{
    
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
           Coordinate[] building2Coords = { new Coordinate(32., 5.,0.),
				new Coordinate(34., 5.,0.), new Coordinate(34., 34.,0.),
				new Coordinate(32., 34.,0.), new Coordinate(32., 5.,0.) };
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null);     
           FastObstructionTest ft= new FastObstructionTest();
           ft.addGeometry(building1,5.);
           ft.addGeometry(building2,4.);
           ft.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(45., 45.,0.)));
           
    }
}