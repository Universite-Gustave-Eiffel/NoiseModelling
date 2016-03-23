
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

            DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));
            DiffractionWithSoilEffetZone diffraData2=nfot.getPath(new Coordinate(5,15,8), new Coordinate(48,25,7));

            assertTrue("Exchange source receiver got the different resultat",
                    diffraData.getDeltaDistance() - diffraData2.getDeltaDistance() <= FastObstructionTest.epsilon
                       && diffraData.geteLength() - diffraData2.geteLength() <= FastObstructionTest.epsilon
                       && diffraData.getFullDiffractionDistance() - diffraData2.getFullDiffractionDistance() <= FastObstructionTest.epsilon);
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
             DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,4), new Coordinate(5,15,3.7));
             DiffractionWithSoilEffetZone diffraData2=nfot.getPath(new Coordinate(5,15,3.7), new Coordinate(48,25,4));
             assertEquals(diffraData.getDeltaDistance(), diffraData2.getDeltaDistance(), 1e-16);
             assertEquals(diffraData.geteLength(), diffraData2.geteLength(), 1e-16);
             assertEquals(diffraData.getFullDiffractionDistance(), diffraData2.getFullDiffractionDistance(), 1e-16);
             assertEquals(-1, diffraData.getDeltaDistance(), 1e-16);
            

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
             DiffractionWithSoilEffetZone diffraData=nfot.getPath(new Coordinate(48,25,1.5), new Coordinate(5,15,2));
             DiffractionWithSoilEffetZone diffraData2=nfot.getPath(new Coordinate(5,15,2), new Coordinate(48,25,1.5));
             assertEquals(diffraData.getDeltaDistance(), diffraData2.getDeltaDistance(), 1e-12);
             assertEquals(diffraData.geteLength(), diffraData2.geteLength(), 1e-12);
             assertEquals(diffraData.getFullDiffractionDistance(), diffraData2.getFullDiffractionDistance(), 1e-12);
             assertEquals(-1.0,diffraData.getDeltaDistance(), 1e-12);
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
             assertFalse("isFreeField Failed",nfot.isFreeField(new Coordinate(40.,3.,2.), new Coordinate(39.,50.,3.)));
             assertTrue("isFreeField Failed",nfot.isFreeField(new Coordinate(40.,3.,9.), new Coordinate(40.,50.,10.)));
         }         
         
}
