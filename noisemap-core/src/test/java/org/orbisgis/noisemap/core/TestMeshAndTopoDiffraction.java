
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
package org.orbisgis.noisemap.core;

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
            mesh.addTopographicPoint(topoPoint1);

            mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
            FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
            System.out.println("----------TEST#1 diffraction with 2 buildings(building1 and building2)----- ");
            
            DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));
            Double[]lt=diffraData.getDiffractionData();
            System.out.println("----deltadistance----");
            System.out.println(lt[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt[nfot.Full_Diffraction_Distance]);
            
            
            
            System.out.println("-----------exchange source receiver------------");
            diffraData=nfot.getPath(new Coordinate(5,15,8), new Coordinate(48,25,7));
            Double[]lt1=diffraData.getDiffractionData();
            System.out.println("----deltadistance----");
            System.out.println(lt1[nfot.Delta_Distance]);
            System.out.println("----e----");
            System.out.println(lt1[nfot.E_Length]);
            System.out.println("----distancepath----");
            System.out.println(lt1[nfot.Full_Diffraction_Distance]);
            
            
            assertTrue("Exchange source receiver got the different resultat",lt[nfot.Delta_Distance]-lt1[nfot.Delta_Distance]<=FastObstructionTest.epsilon
                       &&lt[nfot.E_Length]-lt1[nfot.E_Length]<=FastObstructionTest.epsilon&&lt[nfot.Full_Diffraction_Distance]-lt1[nfot.Full_Diffraction_Distance]<=FastObstructionTest.epsilon);
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
             mesh.addTopographicPoint(topoPoint1);

             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             System.out.println("----------TEST#2 diffraction blocked by TopoPoint----- ");
             DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,4), new Coordinate(5,15,3.7));
             Double[] lt=diffraData.getDiffractionData();
             System.out.println("----deltadistance----");
             System.out.println(lt[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt[nfot.Full_Diffraction_Distance]);
             
            
            
            
             System.out.println("-----------exchange source receiver------------");
             diffraData=nfot.getPath(new Coordinate(5,15,3.7), new Coordinate(48,25,4));
             Double[]lt1=diffraData.getDiffractionData();
             System.out.println("----deltadistance----");
             System.out.println(lt1[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt1[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt1[nfot.Full_Diffraction_Distance]);


            assertTrue("testTopoPointsBlockSourceAndReceiver failed! ",lt[nfot.Delta_Distance].equals(lt1[nfot.Delta_Distance])
                       &&lt[nfot.E_Length].equals(lt1[nfot.E_Length])&&lt[nfot.Full_Diffraction_Distance].equals(lt1[nfot.Full_Diffraction_Distance])&&lt[nfot.Delta_Distance].equals(-1.0));
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
             mesh.addTopographicPoint(topoPoint1);

             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             System.out.println("----------TEST#3 diffraction with 2 buildings(building1 and building2)----- ");
             DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,1.5), new Coordinate(5,15,2));
             Double[] lt=diffraData.getDiffractionData();
             System.out.println("----deltadistance----");
             System.out.println(lt[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt[nfot.Full_Diffraction_Distance]);
             
            
            
            
             System.out.println("-----------exchange source receiver------------");
             diffraData=nfot.getPath(new Coordinate(5,15,2), new Coordinate(48,25,1.5));
             Double[]lt1=diffraData.getDiffractionData();
             System.out.println("----deltadistance----");
             System.out.println(lt1[nfot.Delta_Distance]);
             System.out.println("----e----");
             System.out.println(lt1[nfot.E_Length]);
             System.out.println("----distancepath----");
             System.out.println(lt1[nfot.Full_Diffraction_Distance]);
            
            
             assertTrue("testIfSourceOrReceiverAreImporerWithTopographic failed! ",lt[nfot.Delta_Distance].equals(lt1[nfot.Delta_Distance])
                       &&lt[nfot.E_Length].equals(lt1[nfot.E_Length])&&lt[nfot.Full_Diffraction_Distance].equals(lt1[nfot.Full_Diffraction_Distance])&&lt[nfot.Delta_Distance].equals(-1.0));
            
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
             FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             
             

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
             mesh.addTopographicPoint(topoPoint1);
             mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
             FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
             
             
             System.out.println("---------------------Test#5 isFreeField blocked by TopoPoint----------------------");
             assertFalse("isFreeField Failed",nfot.isFreeField(new Coordinate(40.,3.,2.), new Coordinate(39.,50.,3.)));

             assertTrue("isFreeField Failed",nfot.isFreeField(new Coordinate(40.,3.,9.), new Coordinate(40.,50.,10.)));
             System.out.println("---------------------Test#5 isFreeField blocked by TopoPoint finished----------------------");
             
             
            
            
         }         
         
}
