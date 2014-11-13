package org.orbisgis.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import org.jdelaunay.delaunay.geometries.DTriangle;

import java.util.List;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestJDelaunayTriangleDirectionChanged extends TestCase{
    public void testTriangleDirectionChangedByJDelaunay() throws LayerDelaunayError{
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           LayerJDelaunay jDelaunay=new LayerJDelaunay();
           jDelaunay.addPolygon(building1, true, 1);
           jDelaunay.setMinAngle(0.);
	   jDelaunay.setRetrieveNeighbors(true);
	   jDelaunay.processDelaunay();
           List<DTriangle> triangle=jDelaunay.gettriangletest();
           //-1 no triangle
           //0 triangle ccw
           //1 triangle cw
           //2 one building, triangles have different directions
           int ccw=-1;                                
           for(DTriangle t:triangle){
               Coordinate [] ring = new Coordinate []{t.getPoint(0).getCoordinate(),t.getPoint(1).getCoordinate(),t.getPoint(2).getCoordinate(),t.getPoint(0).getCoordinate()};
               if(ccw==-1){
                   if(CGAlgorithms.isCCW(ring)){
                       ccw=0;
                   }
                   else{
                       ccw=1;
                   }
               }
               else if(ccw!=-1){
                   if(ccw==0&&!CGAlgorithms.isCCW(ring)||ccw==1&&CGAlgorithms.isCCW(ring)){
                       ccw=2;
                       break;
                   }
                   
               }
           }
           System.out.println("triangle directions are:");
           switch(ccw){
               case 0:
                   System.out.println("CCW");
                   break;
               case 1:
                   System.out.println("CW");
                   break;
               case 2:
                   System.out.println("Constraint's segment modified by JDelaunay");
                   break;
           }
          
           
           

    
    }
}
