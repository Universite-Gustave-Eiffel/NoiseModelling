package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import static junit.framework.Assert.assertTrue;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestSoundPropagationIn3DWithNewFunction extends TestCase {


    public void testDiffraction3DNormailBuilding() throws LayerDelaunayError{
    
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(15., 5.,0.) };
           Coordinate[] building2Coords = { new Coordinate(40., 5.,0.),
				new Coordinate(45., 5.,0.), new Coordinate(45., 30.,0.),
				new Coordinate(40., 30.,0.), new Coordinate(40., 5.,0.) };
                  
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null); 
          
           FastObstructionTest ft= new FastObstructionTest();          


           ft.addGeometry(building1,5.);
           ft.addGeometry(building2,4.);

           ft.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
                      
           System.out.println("----------------TEST path between source and receiver----------------");
           System.out.println("-----no building but have one cross triangle-----");
           Double[]lt=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(10,15,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[0]);
           System.out.println("----e----");
           System.out.println(lt[1]);
           System.out.println("----distancepath----");
           System.out.println(lt[3]);
           System.out.println("----------TEST diffraction with 2 buildings(building1 and building2)----- ");
           
           lt=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[0]);
           System.out.println("----e----");
           System.out.println(lt[1]);
           System.out.println("----distancepath----");
           System.out.println(lt[3]);
           System.out.println("-----------exchange source receiver------------");
           Double[]lt1=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt1[0]);
           System.out.println("----e----");
           System.out.println(lt1[1]);
           System.out.println("----distancepath----");
           System.out.println(lt1[3]);
           assertTrue("Exchange source receiver got the different resultat",lt[0].equals(lt1[0])&&lt[1].equals(lt1[1])&&lt[3].equals(lt1[3]));
           
           }
   
    public void testDiffraction3DSpecialBuilding() throws LayerDelaunayError{
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(15., 5.,0.) };
           Coordinate[] building2Coords = { new Coordinate(30., 5.,0.),
				new Coordinate(45., 5.,0.), new Coordinate(45., 45.,0.),
				new Coordinate(30., 45.,0.), new Coordinate(30., 5.,0.) };
                  
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null); 
          
           FastObstructionTest ft= new FastObstructionTest();          


           ft.addGeometry(building1,5.);
           ft.addGeometry(building2,4.);
           System.out.println("----------------TEST 2 buildings are glued----------------");
           ft.testMergeGetPolygonWithHeight(); 
           System.out.println("----------------TEST 2 buildings are glued Finished----------------");
           ft.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
                      
           
           
           Double[]lt=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[0]);
           System.out.println("----e----");
           System.out.println(lt[1]);
           System.out.println("----distancepath----");
           System.out.println(lt[3]);
           System.out.println("-----------exchange source receiver------------");
           Double[]lt1=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt1[0]);
           System.out.println("----e----");
           System.out.println(lt1[1]);
           System.out.println("----distancepath----");
           System.out.println(lt1[3]);
           assertTrue("Exchange source receiver got the different resultat",lt[0].equals(lt1[0])&&lt[1].equals(lt1[1])&&lt[3].equals(lt1[3]));
           
           }
    
    
    
}