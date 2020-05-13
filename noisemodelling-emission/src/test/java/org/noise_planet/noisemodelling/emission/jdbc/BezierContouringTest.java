package org.noise_planet.noisemodelling.emission.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.Arrays;

public class BezierContouringTest {

    @Test
    public void testBezierCurve() throws ParseException {
        String poly = "POLYGON ((15 15, 30 60, 80 50, 80 20, 60 40, 60 10, 30 10, 15 15))";
        WKTReader wktReader = new WKTReader();
        Geometry geom = wktReader.read(poly);
        Coordinate[] coordinates = geom.getCoordinates();
        Coordinate[] res = BezierContouring.interpolate(Arrays.copyOfRange(coordinates, 0, coordinates.length -1), 1.0);
        GeometryFactory factory = new GeometryFactory();
        LineString polyRes = factory.createLineString(res);
        System.out.println(polyRes.toString());
    }
}
