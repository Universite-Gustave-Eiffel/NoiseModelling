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

import com.vividsolutions.jts.algorithm.CGAlgorithms3D;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author Nicolas Fortin
 */
public class TestDiffractionPath {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDiffractionPath.class);

    @Test
    public void testPathOneBuilding() throws LayerDelaunayError {
        //Build Scene with One Building
        double height = 5;
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = {new Coordinate(15., 5., 0.),
                new Coordinate(30., 5., 0.), new Coordinate(30., 30., 0.),
                new Coordinate(15., 30., 0.), new Coordinate(15., 5., 0.)};
        Polygon building1 = factory.createPolygon(factory.createLinearRing(building1Coords));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(building1, height);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0., 0.),
                new Coordinate(45., 45., 0.)));
        Coordinate p1 = new Coordinate(10, 20, 1);
        Coordinate p2 = new Coordinate(40, 20, 1);
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
        double expectedLength = CGAlgorithms3D.distance(p1, new Coordinate(15,20, height)) +
                CGAlgorithms3D.distance(new Coordinate(15, 20, height), new Coordinate(30, 20 ,height)) +
                CGAlgorithms3D.distance(new Coordinate(30, 20, height), p2);
        DiffractionWithSoilEffetZone diff = manager.getPath(p1, p2);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);
        assertEquals(height, diff.getROZone().p1.z, 1e-12);
        assertEquals(height, diff.getOSZone().p0.z, 1e-12);
        // Do other way
        diff = manager.getPath(p2, p1);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);
        assertEquals(height, diff.getROZone().p1.z, 1e-12);
        assertEquals(height, diff.getOSZone().p0.z, 1e-12);
    }

    @Test
    public void testPathOneBuildingWithTopo() throws LayerDelaunayError {
        //Build Scene with One Building
        double height = 5;
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = {new Coordinate(15., 5., 0.),
                new Coordinate(30., 5., 0.), new Coordinate(30., 30., 0.),
                new Coordinate(15., 30., 0.), new Coordinate(15., 5., 0.)};
        Polygon building1 = factory.createPolygon(factory.createLinearRing(building1Coords));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(building1, height);
        // Add topo
        mesh.addTopographicPoint(new Coordinate(0, 0, 0));
        mesh.addTopographicPoint(new Coordinate(0, 45, 0));
        mesh.addTopographicPoint(new Coordinate(32, 0, 0));
        mesh.addTopographicPoint(new Coordinate(32, 45, 0));
        mesh.addTopographicPoint(new Coordinate(36, 0, 1));
        mesh.addTopographicPoint(new Coordinate(36, 20, 1));
        mesh.addTopographicPoint(new Coordinate(36, 45, 1));
        mesh.addTopographicPoint(new Coordinate(50, 0, 1));
        mesh.addTopographicPoint(new Coordinate(50, 20, 1));
        mesh.addTopographicPoint(new Coordinate(50, 45, 1));

        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0., 0.),
                new Coordinate(45., 45., 0.)));
        Coordinate p1 = new Coordinate(10, 20, 1.5);
        Coordinate p2 = new Coordinate(40, 20, 1.5);
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
        double expectedLength = CGAlgorithms3D.distance(p1, new Coordinate(15,20, height)) +
                CGAlgorithms3D.distance(new Coordinate(15, 20, height), new Coordinate(30, 20 ,height)) +
                CGAlgorithms3D.distance(new Coordinate(30, 20, height), p2);
        DiffractionWithSoilEffetZone diff = manager.getPath(p1, p2);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);

        // Do other way
        diff = manager.getPath(p2, p1);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);
    }
}
