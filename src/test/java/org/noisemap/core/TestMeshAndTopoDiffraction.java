package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import static junit.framework.Assert.assertFalse;

import static junit.framework.Assert.assertTrue;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestMeshAndTopoDiffraction extends TestCase{
    
        public void testMergeBuildingAndTopoPoints() throws LayerDelaunayError{
            
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
            mesh.addTopograhicPoint(topoPoint1);

            mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
            NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
            System.out.println("----------TEST#1 diffraction with 2 buildings(building1 and building2)----- ");
           
            Double[]lt=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));
            System.out.println("----deltadistance----");
            System.out.println(lt[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt[nfot.Full_Difrraction_Distance]);
            
            
            
            System.out.println("-----------exchange source receiver------------");
            Double[]lt1=nfot.getPath(new Coordinate(5,15,8), new Coordinate(48,25,7));
            System.out.println("----deltadistance----");
            System.out.println(lt1[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt1[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt1[nfot.Full_Difrraction_Distance]);
            
            
            assertTrue("Exchange source receiver got the different resultat",lt[nfot.Delta_Distance]-lt1[nfot.Delta_Distance]<=FastObstructionTest.epsilon
                       &&lt[nfot.E_Length]-lt1[nfot.E_Length]<=FastObstructionTest.epsilon&&lt[nfot.Full_Difrraction_Distance]-lt1[nfot.Full_Difrraction_Distance]<=FastObstructionTest.epsilon);
            System.out.println("----------TEST#1 diffraction with 2 buildings(building1 and building2) finished----- ");


         }
         public void testTopoPointsBlockSourceAndReceiver() throws LayerDelaunayError{
            
             GeometryFactory factory = new GeometryFactory();

             Coordinate[] building1Coords = { new Coordinate(40., 5.,2.),
                                        new Coordinate(45., 5.,3.), new Coordinate(45., 45.,5.),
                                        new Coordinate(40., 45.,2.), new Coordinate(40., 5.,2.) };
            
             Coordinate topoPoint1 = new Coordinate(30.,15.,6.);

             Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);            
             MeshBuilder mesh= new MeshBuilder();

             mesh.addGeometry(building1,1.0);
             mesh.addTopograhicPoint(topoPoint1);

             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             System.out.println("----------TEST#2 diffraction blocked by TopoPoint----- ");
            
             Double[] lt=nfot.getPath(new Coordinate(48,25,4), new Coordinate(5,15,3.7));
             System.out.println("----deltadistance----");
             System.out.println(lt[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt[nfot.Full_Difrraction_Distance]);
             
            
            
            
             System.out.println("-----------exchange source receiver------------");
             Double[]lt1=nfot.getPath(new Coordinate(5,15,3.7), new Coordinate(48,25,4));
             System.out.println("----deltadistance----");
             System.out.println(lt1[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt1[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt1[nfot.Full_Difrraction_Distance]);
            
            
            assertTrue("testTopoPointsBlockSourceAndReceiver failed! ",lt[nfot.Delta_Distance].equals(lt1[nfot.Delta_Distance])
                       &&lt[nfot.E_Length].equals(lt1[nfot.E_Length])&&lt[nfot.Full_Difrraction_Distance].equals(lt1[nfot.Full_Difrraction_Distance])&&lt[nfot.Delta_Distance].equals(-1.0));
             System.out.println("----------TEST#2 diffraction blocked by TopoPoint finished----- ");
            

         }
         
         
         
         
         public void testIfSourceOrReceiverAreImporerWithTopographic() throws LayerDelaunayError{
            
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
             mesh.addTopograhicPoint(topoPoint1);

             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             System.out.println("----------TEST#3 diffraction with 2 buildings(building1 and building2)----- ");
            
             Double[] lt=nfot.getPath(new Coordinate(48,25,1.5), new Coordinate(5,15,2));
             System.out.println("----deltadistance----");
             System.out.println(lt[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt[nfot.Full_Difrraction_Distance]);
             
            
            
            
             System.out.println("-----------exchange source receiver------------");
             Double[]lt1=nfot.getPath(new Coordinate(5,15,2), new Coordinate(48,25,1.5));
             System.out.println("----deltadistance----");
             System.out.println(lt1[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt1[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt1[nfot.Full_Difrraction_Distance]);
            
            
             assertTrue("testIfSourceOrReceiverAreImporerWithTopographic failed! ",lt[nfot.Delta_Distance].equals(lt1[nfot.Delta_Distance])
                       &&lt[nfot.E_Length].equals(lt1[nfot.E_Length])&&lt[nfot.Full_Difrraction_Distance].equals(lt1[nfot.Full_Difrraction_Distance])&&lt[nfot.Delta_Distance].equals(-1.0));
            
             System.out.println("----------TEST#3 diffraction with 2 buildings(building1 and building2) finished----- ");

         }
             
         public void testPointsVisible() throws LayerDelaunayError{
             

             GeometryFactory factory = new GeometryFactory();
             Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                       new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                       new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
            
             Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
             MeshBuilder mesh= new MeshBuilder();
             mesh.addGeometry(building1,4.0);
             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             
             

             assertFalse("isFreeField Failed",nfot.isFreeField(new Coordinate(10.,10.), new Coordinate(25.,31.)));

             
             
            
            
         }

         public void testPointsVisibilityBlockedByTopoPoint() throws LayerDelaunayError{
             GeometryFactory factory = new GeometryFactory();
             Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                       new Coordinate(15., 30.,3.), new Coordinate(30., 30.,3.),
                                       new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
             Coordinate topoPoint1 = new Coordinate(40.,15.,8.);
             Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
             MeshBuilder mesh= new MeshBuilder();
             mesh.addGeometry(building1,4.0);
             mesh.addTopograhicPoint(topoPoint1);
             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             NewFastObstructionTest nfot= new NewFastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             
             
             System.out.println("---------------------Test#5 isFreeField blocked by TopoPoint----------------------");
             assertFalse("isFreeField Failed",nfot.isFreeField(new Coordinate(40.,3.,2.), new Coordinate(40.,50.,2.)));
             System.out.println("---------------------Test#5 isFreeField blocked by TopoPoint finished----------------------");
             
             
            
            
         }         
         
}
