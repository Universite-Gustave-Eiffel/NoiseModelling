/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
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
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */

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
