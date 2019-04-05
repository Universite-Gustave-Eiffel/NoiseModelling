package org.noise_planet.noisemodelling.propagation;

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
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

        Envelope bbox = mesh.getGeometriesBoundingBox();

        bbox.expandBy(5);

        mesh.finishPolygonFeeding(bbox);

        Coordinate p1 = new Coordinate(10, 20, 1.5);
        Coordinate p2 = new Coordinate(40, 20, 1.5);
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
        double expectedLength = CGAlgorithms3D.distance(p1, new Coordinate(15,20, manager.getBuildingRoofZ(1))) +
                CGAlgorithms3D.distance(new Coordinate(15, 20, manager.getBuildingRoofZ(1)), new Coordinate(30, 20 ,manager.getBuildingRoofZ(1))) +
                CGAlgorithms3D.distance(new Coordinate(30, 20, manager.getBuildingRoofZ(1)), p2);
        DiffractionWithSoilEffetZone diff = manager.getPath(p1, p2);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);

        // Do other way
        diff = manager.getPath(p2, p1);
        assertEquals(expectedLength, diff.getFullDiffractionDistance(), 1e-3);
    }

    @Test
    public void testTwoBuildingsDiffractionHorizontalEdges() throws ParseException, LayerDelaunayError {
        WKTReader wktReader = new WKTReader();
        Geometry building1 = wktReader.read("MULTIPOLYGON (((223245.10954126046 6757870.685251366, 223254.3576750219 6757883.725402858, 223265.81413628225 6757875.6004328905, 223256.56738751417 6757862.559298952, 223245.10954126046 6757870.685251366)))");
        Geometry building2 = wktReader.read("MULTIPOLYGON (((223243.47480791857 6757871.842867576, 223178.29477526256 6757917.929971755, 223183.61783164035 6757925.556583197, 223248.83068443503 6757879.444709641, 223243.47480791857 6757871.842867576)))");

        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(building1, 14);
        mesh.addGeometry(building2, 14);

        mesh.finishPolygonFeeding(new Envelope(new Coordinate(223127.3190158577, 6757833.636902214),
                new Coordinate(223340.4209403399, 6757947.187562704)));


        Coordinate p1 = new Coordinate(223252.85137195565, 6757859.903564689, 1.5);
        Coordinate p2 = new Coordinate(223230.85137195565, 6757927.403564689, 0.05);

        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());

        DiffractionWithSoilEffetZone diff = manager.getPath(p1, p2);

        assertEquals(80.2, diff.getFullDiffractionDistance(), 0.1);
    }
}
