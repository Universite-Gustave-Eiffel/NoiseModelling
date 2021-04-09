package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;

import static org.junit.Assert.*;

public class LayerTinfourTest {

    @Test
    public void testPointDelaunay1() throws LayerDelaunayError {
        LayerTinfour layerTinfour = new LayerTinfour();
        layerTinfour.setRetrieveNeighbors(true);

        layerTinfour.addVertex(new Coordinate(0,0,0));
        layerTinfour.addVertex(new Coordinate(100,0,0));
        layerTinfour.addVertex(new Coordinate(100,100,0));
        layerTinfour.addVertex(new Coordinate(0,100,0));

        layerTinfour.addVertex(new Coordinate(25,51,0));
        layerTinfour.addVertex(new Coordinate(10,5,0));
        layerTinfour.addVertex(new Coordinate(85,78,0));

        layerTinfour.processDelaunay();


        List<Triangle> triangleList = layerTinfour.getTriangles();
        List<Triangle> neighbors = layerTinfour.getNeighbors();
        assertEquals(8, triangleList.size());
    }
}