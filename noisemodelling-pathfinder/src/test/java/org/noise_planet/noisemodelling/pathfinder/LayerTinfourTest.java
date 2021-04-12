package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.functions.io.osm.OSMDriverFunction;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

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

    @Test
    public void testPolygonDelaunay1() throws LayerDelaunayError {
        LayerTinfour layerTinfour = new LayerTinfour();
        layerTinfour.setRetrieveNeighbors(true);

        GeometryFactory geometryFactory = new GeometryFactory();

        layerTinfour.addVertex(new Coordinate(0,0,0));
        layerTinfour.addVertex(new Coordinate(100,0,0));
        layerTinfour.addVertex(new Coordinate(100,100,0));
        layerTinfour.addVertex(new Coordinate(0,100,0));

        Coordinate[] building1 = new Coordinate[] {
          new Coordinate(40,20),
                new Coordinate(55,20),
                new Coordinate(55,45),
                new Coordinate(40,45),
                new Coordinate(40,20),
        };

        layerTinfour.addPolygon(geometryFactory.createPolygon(building1), 55);

        layerTinfour.processDelaunay();

        List<Triangle> triangleList = layerTinfour.getTriangles();
        int numbertri55 = 0;
        for(Triangle tri : triangleList) {
            if(tri.getAttribute() == 55) {
                numbertri55++;
            }
        }
        // 2 triangle inside a rectangular building
        assertEquals(2, numbertri55);
        List<Triangle> neighbors = layerTinfour.getNeighbors();
        assertEquals(10, triangleList.size());
    }

}