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
package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;
import static junit.framework.Assert.assertFalse;

import static junit.framework.Assert.assertTrue;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class Test3DPropagation extends TestCase{
    
    public void test3Dpropagation() throws LayerDelaunayError{
        GeometryFactory factory = new GeometryFactory();
	Coordinate[] building1Coords = { new Coordinate(0., 20., 0.),
				new Coordinate(20., 20., 0.), new Coordinate(20., 60., 0.),
				new Coordinate(0., 60., 0.), new Coordinate(0.,20., 0.) };
        
        Coordinate[] building2Coords = { new Coordinate(20., 0., 0.),
				new Coordinate(100., 0., 0.), new Coordinate(100., 20., 0.),
				new Coordinate(20., 20., 0.), new Coordinate(20., 0., 0.) };
        
        Coordinate[] building3Coords = { new Coordinate(80., 30., 0.),
				new Coordinate(80., 90., 0.), new Coordinate(-10., 90., 0.),
				new Coordinate(-10., 70., 0.), new Coordinate(60., 70., 0.), 
                                new Coordinate(60., 30., 0.), new Coordinate(80., 30., 0.) };
 
        Coordinate[] building4Coords = { new Coordinate(137., 89., 0.),
				new Coordinate(137., 109., 0.), new Coordinate(153., 109., 0.),
				new Coordinate(153., 89., 0.), new Coordinate(137., 89., 0.)};
        
        Coordinate[] building5Coords = { new Coordinate(140., 0., 0.),
				new Coordinate(230., 0., 0.), new Coordinate(230., 60., 0.),
				new Coordinate(140., 60., 0.), new Coordinate(140., 40., 0.), 
                                new Coordinate(210., 40., 0.), new Coordinate(210., 20., 0.),
                                new Coordinate(140., 20., 0), new Coordinate(140., 0., 0.)};
        
        Polygon[] building1Poly = new Polygon[]{factory.createPolygon(building1Coords)};
        Polygon[] building2Poly = new Polygon[]{factory.createPolygon(building2Coords)};
        Polygon[] building3Poly = new Polygon[]{factory.createPolygon(building3Coords)};
        
        Polygon[] building5Poly = new Polygon[]{factory.createPolygon(building5Coords)};
	MultiPolygon building1 = factory.createMultiPolygon(building1Poly);
        MultiPolygon building2 = factory.createMultiPolygon(building2Poly);
        MultiPolygon building3 = factory.createMultiPolygon(building3Poly);
        Polygon      building4 = factory.createPolygon(building4Coords);
        MultiPolygon building5 = factory.createMultiPolygon(building5Poly);
        
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(building1, 4.);
        mesh.addGeometry(building2, 5.);
        mesh.addGeometry(building3, 4.);
        mesh.addGeometry(building4, 5.);
        mesh.addGeometry(building5, 4.);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(-300., -300.,0.),
				new Coordinate(300., 300.,0.)));
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
        manager.getPath(new Coordinate(19.99292893234268, 20.007071067966407, 0.0), new Coordinate(125.0, 44.5, 3.0));
        manager.isFreeField(new Coordinate(19.99292893234268, 20.007071067966407, 0.0), new Coordinate(125.0, 44.5, 3.0));
    }
}
