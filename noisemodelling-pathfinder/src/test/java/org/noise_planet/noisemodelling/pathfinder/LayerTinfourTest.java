package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.functions.io.osm.OSMDriverFunction;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

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

    @Test
    public void testPolygonHole() throws ParseException, LayerDelaunayError {
        WKTReader wktReader = new WKTReader();
        GeometryFactory factory = new GeometryFactory();
        Polygon[] polygons = {
                (Polygon) wktReader.read("POLYGON((222677.439091769 6758514.42927814 0,222671.451152259 6758525.56045846 0,222666.53640143 6758534.70500279 0,222660.568467509 6758545.82623747 0,222680.275526724 6758556.23812336 0,222682.923201221 6758551.44635515 0,222687.825144212 6758553.96433641 0,222688.6946897 6758553.91607613 0,222689.594539629 6758553.69797565 0,222708.564257407 6758552.76566163 0,222718.050422367 6758536.28473477 0,222718.049359588 6758536.28416282 0,222715.538763539 6758534.93305632 0,222715.534664596 6758534.94067328 0,222713.49856221 6758538.72427529 0,222712.138657153 6758539.12136151 0,222705.459934079 6758535.53060206 0,222705.062847945 6758534.17068046 0,222707.088948575 6758530.38705905 0,222707.089519954 6758530.38599728 0,222696.179880457 6758524.51484693 0,222696.177391059 6758524.51947287 0,222694.151290324 6758528.30309409 0,222692.791365757 6758528.71018202 0,222686.102658034 6758525.11938618 0,222685.695570154 6758523.75946162 0,222687.731672806 6758519.97586009 0,222687.734820884 6758519.97003873 0,222677.439091769 6758514.42927814 0)))"),
                (Polygon) wktReader.read("POLYGON((222718.050422367 6758536.28473477 0,222726.944985841 6758520.82200926 0,222711.291580801 6758512.32681897 0,222709.875972122 6758510.54519262 0,222707.961134851 6758508.53272968 0,222706.494573643 6758507.25068349 0,222704.937863906 6758506.07838239 0,222704.57854509 6758505.84781922 0,222704.889271526 6758505.37873581 0,222705.348389209 6758505.67944089 0,222706.541930339 6758503.47318075 0,222706.002942329 6758503.13234521 0,222704.775286101 6758502.34044378 0,222703.717046283 6758501.77873438 0,222702.848494026 6758501.3273335 0,222702.379276882 6758501.07656837 0,222701.091151516 6758500.54439877 0,222699.957711378 6758502.67082755 0,222700.416829049 6758502.97153264 0,222700.215942338 6758503.49080695 0,222698.837944834 6758502.91847076 0,222696.450902373 6758502.18426504 0,222693.773683978 6758501.63937878 0,222691.535579581 6758501.3951585 0,222690.536302091 6758501.35323118 0,222685.884034415 6758498.91570387 0,222684.890461927 6758501.07237841 0,222679.744696108 6758505.69935698 0,222678.532224271 6758512.47266004 0,222678.080905977 6758513.2912627 0,222677.439091769 6758514.42927814 0,222687.734820884 6758519.97003873 0,222689.77776067 6758516.19227811 0,222691.117714616 6758515.78515118 0,222697.816387942 6758519.38595153 0,222698.213493564 6758520.73587122 0,222696.179880457 6758524.51484693 0,222707.089519954 6758530.38599728 0,222709.125050975 6758526.60345721 0,222710.474970686 6758526.20635152 0,222717.163678848 6758529.79714675 0,222717.570750355 6758531.15707127 0,222715.538763539 6758534.93305632 0,222718.049359588 6758536.28416282 0,222718.050422367 6758536.28473477 0)))"),
                (Polygon) wktReader.read("POLYGON((222705.696340536 6758557.74687277 0,222724.075919557 6758557.07325136 0,222740.531595117 6758528.34377304 0,222741.230128632 6758528.86480505 0,222743.686623899 6758524.74224218 0,222742.888076702 6758524.26098912 0,222744.171861162 6758521.90499592 0,222732.249628577 6758511.59826499 0,222726.944985841 6758520.82200926 0,222718.050422367 6758536.28473477 0,222708.564257407 6758552.76566163 0,222705.696340536 6758557.74687277 0))")
        };
        Polygon merged = (Polygon)factory.createMultiPolygon(polygons).buffer(0);
        System.out.println(merged.toString());
        LayerTinfour layerTinfour = new LayerTinfour();
        layerTinfour.setRetrieveNeighbors(true);

        layerTinfour.addPolygon(merged, 55);

        layerTinfour.processDelaunay();

        List<Triangle> triangleList = layerTinfour.getTriangles();
    }
}