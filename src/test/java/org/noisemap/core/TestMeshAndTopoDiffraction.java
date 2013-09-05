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
public class TestMeshAndTopoDiffraction extends TestCase{
    
        public void testMergeBuildingAndToPoPoints() throws LayerDelaunayError{
            
            GeometryFactory factory = new GeometryFactory();
            Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                        new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                        new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
            Coordinate[] building2Coords = { new Coordinate(40., 5.,2.),
                                        new Coordinate(45., 5.,3.), new Coordinate(45., 45.,5.),
                                        new Coordinate(40., 45.,2.), new Coordinate(40., 5.,2.) };
            
            Coordinate topoPoint1 = new Coordinate(5.,10.,1.);
            Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
            Polygon building2 = factory.createPolygon(
                            factory.createLinearRing(building2Coords), null);            
            MeshBuilder mesh= new MeshBuilder();
            mesh.addGeometry(building1,4.0);
            mesh.addGeometry(building2,5.0);
            mesh.addTopograhic(topoPoint1);

            mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
            NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
            System.out.println("----------TEST diffraction with 2 buildings(building1 and building2)----- ");
           
            Double[]lt=nfot.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
            System.out.println("----deltadistance----");
            System.out.println(lt[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt[nfot.Full_Difrraction_Distance]);
            
            
            
            System.out.println("-----------exchange source receiver------------");
            Double[]lt1=nfot.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
            System.out.println("----deltadistance----");
            System.out.println(lt1[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt1[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt1[nfot.Full_Difrraction_Distance]);
            
            
            assertTrue("Exchange source receiver got the different resultat",lt[nfot.Delta_Distance]-lt1[nfot.Delta_Distance]<=FastObstructionTest.epsilon
                       &&lt[nfot.E_Length]-lt1[nfot.E_Length]<=FastObstructionTest.epsilon&&lt[nfot.Full_Difrraction_Distance]-lt1[nfot.Full_Difrraction_Distance]<=FastObstructionTest.epsilon);
            System.out.println("----------TEST diffraction with 2 buildings(building1 and building2) finished----- ");


         }
         public void testTopoPointsBlockSourceAndReceiver() throws LayerDelaunayError{
            
            GeometryFactory factory = new GeometryFactory();
            Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                        new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                        new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
            Coordinate[] building2Coords = { new Coordinate(40., 5.,2.),
                                        new Coordinate(45., 5.,3.), new Coordinate(45., 45.,5.),
                                        new Coordinate(40., 45.,2.), new Coordinate(40., 5.,2.) };
            
            Coordinate topoPoint1 = new Coordinate(5.,10.,4.);
            Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
            Polygon building2 = factory.createPolygon(
                            factory.createLinearRing(building2Coords), null);            
            MeshBuilder mesh= new MeshBuilder();
            mesh.addGeometry(building1,4.0);
            mesh.addGeometry(building2,5.0);
            mesh.addTopograhic(topoPoint1);

            mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
            NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
            System.out.println("----------TEST diffraction with 2 buildings(building1 and building2)----- ");
           
            Double[] lt=nfot.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
            System.out.println("----deltadistance----");
            System.out.println(lt[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt[nfot.Full_Difrraction_Distance]);
            
            
            
            
            System.out.println("-----------exchange source receiver------------");
            Double[]lt1=nfot.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
            System.out.println("----deltadistance----");
            System.out.println(lt1[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt1[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt1[nfot.Full_Difrraction_Distance]);
            
            
             assertTrue("Exchange source receiver got the different resultat",lt[nfot.Delta_Distance]-lt1[nfot.Delta_Distance]<=FastObstructionTest.epsilon
                       &&lt[nfot.E_Length]-lt1[nfot.E_Length]<=FastObstructionTest.epsilon&&lt[nfot.Full_Difrraction_Distance]-lt1[nfot.Full_Difrraction_Distance]<=FastObstructionTest.epsilon);
            System.out.println("----------TEST diffraction with 2 buildings(building1 and building2) finished----- ");
            System.out.println("-----the intersections points------");


         }        
}
