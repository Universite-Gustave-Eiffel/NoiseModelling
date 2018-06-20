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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestSoundPropagationIn3DWithNewFunction extends TestCase {


    public void testDiffraction3DNormailBuilding() throws LayerDelaunayError {

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = {new Coordinate(15., 5., 0.),
                new Coordinate(15., 30., 0.), new Coordinate(30., 30., 0.),
                new Coordinate(30., 5., 0.), new Coordinate(15., 5., 0.)};
        Coordinate[] building2Coords = {new Coordinate(40., 5., 0.),
                new Coordinate(45., 5., 0.), new Coordinate(45., 30., 0.),
                new Coordinate(40., 30., 0.), new Coordinate(40., 5., 0.)};

        Polygon building1 = factory.createPolygon(
                factory.createLinearRing(building1Coords), null);
        Polygon building2 = factory.createPolygon(
                factory.createLinearRing(building2Coords), null);

        MeshBuilder mesh = new MeshBuilder();


        mesh.addGeometry(building1, 5.);
        mesh.addGeometry(building2, 4.);

        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0., 0.), new Coordinate(60., 60., 0.)));
        FastObstructionTest ft = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
        DiffractionWithSoilEffetZone diffraData = ft.getPath(new Coordinate(5, 15, 1.5), new Coordinate(10, 15, 0.5));
        diffraData = ft.getPath(new Coordinate(48, 25, 0.5), new Coordinate(5, 15, 1.5));
        DiffractionWithSoilEffetZone diffraData2 = ft.getPath(new Coordinate(5, 15, 1.5), new Coordinate(48, 25, 0.5));
        assertTrue("Exchange source receiver got the different resultat",
                diffraData.getDeltaDistance() - diffraData2.getDeltaDistance() <= FastObstructionTest.epsilon
                        && diffraData.geteLength() - diffraData2.geteLength() <= FastObstructionTest.epsilon
                        && diffraData.getFullDiffractionDistance() - diffraData2.getFullDiffractionDistance() <= FastObstructionTest.epsilon);
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
           mesh.testMergeGetPolygonWithHeight();

           mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));
           FastObstructionTest ft= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());           
           
           DiffractionWithSoilEffetZone diffraData=ft.getPath(new Coordinate(48,25,0.5), new Coordinate(5,15,1.5));
           DiffractionWithSoilEffetZone diffraData2=ft.getPath(new Coordinate(5,15,1.5), new Coordinate(48,25,0.5));

           assertTrue("Exchange source receiver got the different resultat",
                diffraData.getDeltaDistance() - diffraData2.getDeltaDistance() <= FastObstructionTest.epsilon
                        && diffraData.geteLength() - diffraData2.geteLength() <= FastObstructionTest.epsilon
                        && diffraData.getFullDiffractionDistance() - diffraData2.getFullDiffractionDistance() <= FastObstructionTest.epsilon);
           
    }
}