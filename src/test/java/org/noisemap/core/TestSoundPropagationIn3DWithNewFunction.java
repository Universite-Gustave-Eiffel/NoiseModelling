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
          
           MeshBuilder mesh= new MeshBuilder();          


           mesh.addGeometry(building1,5.);
           mesh.addGeometry(building2,4.);

           mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
           FastObstructionTest ft=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());           
           
           System.out.println("----------------TEST 1 test path between source and receiver----------------");
           System.out.println("-----no building but have one cross triangle-----");
           Double[]lt=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(10,15,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Difrraction_Distance]);
           
           System.out.println("-----no building but have one cross triangle finished-----");
           
           System.out.println("----------TEST diffraction with 2 buildings(building1 and building2)----- ");
           
           lt=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Difrraction_Distance]);
           System.out.println("-----------exchange source receiver------------");
           Double[]lt1=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt1[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt1[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt1[ft.Full_Difrraction_Distance]);
           assertTrue("Exchange source receiver got the different resultat",lt[ft.Delta_Distance]-lt1[ft.Delta_Distance]<=FastObstructionTest.epsilon
                      &&lt[ft.E_Length]-lt1[ft.E_Length]<=FastObstructionTest.epsilon&&lt[ft.Full_Difrraction_Distance]-lt1[ft.Full_Difrraction_Distance]<=FastObstructionTest.epsilon);
           System.out.println("----------TEST diffraction with 2 buildings(building1 and building2) finished----- ");
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
          
           MeshBuilder mesh= new MeshBuilder();          


           mesh.addGeometry(building1,5.);
           mesh.addGeometry(building2,4.);
           System.out.println("----------------TEST  buildings are glued----------------");
           mesh.testMergeGetPolygonWithHeight(); 
           System.out.println("----------------TEST  buildings are glued Finished----------------");
           
           System.out.println("----------------TEST  buildings are glued and get the path----------------");
           mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
           FastObstructionTest ft= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());           
           
           
           Double[]lt=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Difrraction_Distance]);
           System.out.println("-----------exchange source receiver------------");
           Double[]lt1=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
           System.out.println("----deltadistance----");
           System.out.println(lt1[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt1[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt1[ft.Full_Difrraction_Distance]);
           assertTrue("Exchange source receiver got the different resultat",lt[ft.Delta_Distance]-lt1[ft.Delta_Distance]<=FastObstructionTest.epsilon
                      &&lt[ft.E_Length]-lt1[ft.E_Length]<=FastObstructionTest.epsilon&&lt[ft.Full_Difrraction_Distance]-lt1[ft.Full_Difrraction_Distance]<=FastObstructionTest.epsilon);
           
           }
    
    
    
}