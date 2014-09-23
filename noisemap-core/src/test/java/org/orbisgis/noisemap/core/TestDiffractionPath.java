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
        DiffractionWithSoilEffetZone diff = manager.getPath(p1, p2);
        double expectedLength = CGAlgorithms3D.distance(p1, new Coordinate(15,20, height)) +
                CGAlgorithms3D.distance(new Coordinate(15, 20, height), new Coordinate(30, 20 ,height)) +
                CGAlgorithms3D.distance(new Coordinate(30, 20, height), p2);
        assertEquals(expectedLength, diff.getDiffractionData()[DiffractionWithSoilEffetZone.FULL_DIFFRACTION_DISTANCE], 1e-12);
    }
}
