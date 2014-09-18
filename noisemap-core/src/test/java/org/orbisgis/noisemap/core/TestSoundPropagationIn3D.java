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
public class TestSoundPropagationIn3D extends TestCase {


    public void test() throws LayerDelaunayError{
    
           GeometryFactory factory = new GeometryFactory();
           Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
           Coordinate[] building2Coords = { new Coordinate(40., 5.,0.),
				new Coordinate(45., 5.,0.), new Coordinate(45., 30.,0.),
				new Coordinate(40., 30.,0.), new Coordinate(40., 5.,0.) };
           Polygon building1 = factory.createPolygon(
			factory.createLinearRing(building1Coords), null);
           Polygon building2 = factory.createPolygon(
			factory.createLinearRing(building2Coords), null);     
           MeshBuilder mesh= new MeshBuilder();
           //add building with height
           mesh.addGeometry(building1,5.);
           mesh.addGeometry(building2,4.);
           mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
           FastObstructionTest ft=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
           
           assertTrue("Intersection test isFreeField #1 failed",ft.isFreeField(new Coordinate(10,5), new Coordinate(12,45)));
           assertFalse("Intersection test isFreeField #2 failed",ft.isFreeField(new Coordinate(10,5), new Coordinate(32,15)));
           // Direct field propagation on top of the building
           // Ray is at 6.22m and building height is 5m, then there is a free field.
           assertTrue("Intersection test isFreeField #2 failed",ft.isFreeField(new Coordinate(10,5,6.0), new Coordinate(32,15,7.0)));
           System.out.println("----------------TEST path between source and receiver----------------");
           System.out.println("-----no building-----");
           DiffractionWithSoilEffetZone diffraData=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(10,15,0.5));
           Double[]lt=diffraData.getDiffractionData();
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Diffraction_Distance]);
           System.out.println("----------TEST with 2 building----- ");
           diffraData=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           lt=diffraData.getDiffractionData();
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Diffraction_Distance]);
           System.out.println("-----------exchange source receiver------------");
           diffraData=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));
           lt=diffraData.getDiffractionData();
           System.out.println("----deltadistance----");
           System.out.println(lt[ft.Delta_Distance]);
           System.out.println("----e----");
           System.out.println(lt[ft.E_Length]);
           System.out.println("----distancepath----");
           System.out.println(lt[ft.Full_Diffraction_Distance]);
          // LinkedList<Coordinate> lt=ft.getPath(new Coordinate(4,4,0.5), new Coordinate(31,31,1.5));
          // before change fastobstruction.get path return data type LinkedList<Segment>; 
           /*
           double distance=0.0;
           double distanceforRandS=lt.get(0).p0.distance(lt.get(lt.size()-1).p1);
           for(int i=0;i<lt.size();i++){
               if(i!=lt.size()-1){
                    System.out.println("point"+i+":" +lt.get(i).p0.toString());
               }
               else{    
                    System.out.println("point"+i+":" +lt.get(i).p0.toString());
                    System.out.println("point"+(i+1)+":" +lt.get(i).p1.toString());
               }
               
               distance=lt.get(i).getLength()+distance;
           }
           double deltdistance1=distance-distanceforRandS;
           
           
           System.out.println("distance="+distance);
           System.out.println("distanceRandS="+distanceforRandS);
           System.out.println("Delt distance="+deltdistance1);
           
           
           System.out.println("----------same situation but exchange source and receiver----- ");
           lt=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));

           double distanceforRandS2=lt.get(0).p0.distance(lt.get(lt.size()-1).p1);
           double distance2=0.0;
           for(int i=0;i<lt.size();i++){
               if(i!=lt.size()-1){
                    System.out.println("point"+i+":" +lt.get(i).p0.toString());
               }
               else{    
                    System.out.println("point"+i+":" +lt.get(i).p0.toString());
                    System.out.println("point"+(i+1)+":" +lt.get(i).p1.toString());
               }
               
               distance2=lt.get(i).getLength()+distance2;
           }
           double deltdistance2=distance2-distanceforRandS2;
           System.out.println("distance="+distance2);
           System.out.println("distanceRandS="+distanceforRandS2);
           System.out.println("Delt distance="+deltdistance2);
           
           
      
           //deltdistance1 and deltdistance2 may have the same resultat
           assertTrue("getPath #1 failed", deltdistance2==deltdistance1);
           
           
           //this fonction "distance" is just use the x and y, so the resultat is not same, and the distanceRandS is the right one
           System.out.println(new Coordinate(5,15,1.5).distance(new Coordinate(48,25,0.5)));
           assertTrue("distance failed", distanceforRandS2==new Coordinate(5,15,1.5).distance(new Coordinate(48,25,0.5)));
           
           
           */
           
           /*
           for(int i=0 ; i<lt.size();i++){
               System.out.println("Triangle "+ (i+1));
               System.out.println(lt.get(i)[0]+ "--" + lt.get(i)[1] + "--" + lt.get(i)[2]);
               System.out.println((ft.getTriBuildingHeight()).get(i));
              
           }
           
           System.out.println("------------------Test intersection---------------");
           
           LinkedList<Coordinate> pointsIntersection=ft.getIntersection();
           for(Coordinate point:pointsIntersection){
               System.out.println(point.toString());
           
           }
           
           System.out.println("----------TEST with 1 buildings other side----- ");
           ft.getPath(new Coordinate(32,15,0.5), new Coordinate(47,15,1.0));
           
           lt=ft.getTriBuildingCoordinate();
           
           for(int i=0 ; i<lt.size();i++){
               System.out.println("Triangle "+ (i+1));
               System.out.println(lt.get(i)[0]+ "--" + lt.get(i)[1] + "--" + lt.get(i)[2]);
               System.out.println((ft.getTriBuildingHeight()).get(i));
              
           }
           pointsIntersection=ft.getIntersection();
           for(Coordinate point:pointsIntersection){
               System.out.println(point.toString());
           
           }
           
           System.out.println("----------TEST with special points ----- ");
           ft.getPath(new Coordinate(1,16,0.5), new Coordinate(17,32,1.0));
           
           lt=ft.getTriBuildingCoordinate();
           
           for(int i=0 ; i<lt.size();i++){
               System.out.println("Triangle "+ (i+1));
               System.out.println(lt.get(i)[0]+ "--" + lt.get(i)[1] + "--" + lt.get(i)[2]);
               System.out.println("Height:"+(ft.getTriBuildingHeight()).get(i));
              
           }
           System.out.println("------------------Test intersection---------------");
           pointsIntersection=ft.getIntersection();
           for(Coordinate point:pointsIntersection){
               System.out.println(point.toString());
           }
           
           
           System.out.println("----------TEST with 2 buildings----- ");
           ft.getPath(new Coordinate(5,15), new Coordinate(48,25));
           
           lt=ft.getTriBuildingCoordinate();
           
           for(int i=0 ; i<lt.size();i++){
               System.out.println("Triangle "+ (i+1));
               System.out.println(lt.get(i)[0]+ "--" + lt.get(i)[1] + "--" + lt.get(i)[2]);
               System.out.println("Height:"+(ft.getTriBuildingHeight()).get(i));
              
           }
           System.out.println("------------------Test intersection---------------");
           pointsIntersection=ft.getIntersection();
           for(Coordinate point:pointsIntersection){
               System.out.println(point.toString());
           }

           
           
           System.out.println("----------------TEST Finished----------------");
         /*
           LineSegment a=new LineSegment(); 
           ft.setListofIntersection();
           ft.getListofIntersection(new Coordinate(10,5), new Coordinate(32,15));
         
           */

           
           }
}