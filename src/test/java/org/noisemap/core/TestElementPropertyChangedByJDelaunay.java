/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.geometries.DPoint;

import java.util.List;


import junit.framework.TestCase;

/**
 *
 * @author 008
 */
public class TestElementPropertyChangedByJDelaunay extends TestCase{
    public void testElementPropertyChangedByJDelaunay() throws LayerDelaunayError{
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
           Coordinate[] building2Coords = { new Coordinate(30., 5.,0.),
				new Coordinate(45., 5.,0.), new Coordinate(45., 45.,0.),
				new Coordinate(30., 45.,0.), new Coordinate(30., 5.,0.) };
                  
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null);
           LayerJDelaunay jDelaunay=new LayerJDelaunay();
           jDelaunay.addPolygon(building1, true, 1);
           jDelaunay.addPolygon(building2, true, 2);
           jDelaunay.setMinAngle(0.);
	   jDelaunay.setRetrieveNeighbors(true);
	   jDelaunay.processDelaunay();
           List<DTriangle> triangle=jDelaunay.gettriangletest();                           
           for(DTriangle t:triangle){
               for(DPoint p:t.getPoints()){
                   if(p.getProperty()>=2){
                       System.out.println("exception");
                   }
               }
           }
          
           
           

    
    }
    
}
