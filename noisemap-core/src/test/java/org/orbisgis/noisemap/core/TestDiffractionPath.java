package org.orbisgis.noisemap.core;

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
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
