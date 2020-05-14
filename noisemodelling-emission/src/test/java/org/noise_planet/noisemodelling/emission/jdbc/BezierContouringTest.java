package org.noise_planet.noisemodelling.emission.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import static org.junit.Assert.assertTrue;

public class BezierContouringTest {

    @Test
    public void testBezierCurve() throws ParseException {
        String poly = "POLYGON ((15 15, 30 60, 80 50, 80 20, 60 40, 60 10, 30 10, 15 15))";
        WKTReader wktReader = new WKTReader();
        Geometry geom = wktReader.read(poly);
        Coordinate[] coordinates = geom.getCoordinates();

        Coordinate[] res = BezierContouring.interpolate(coordinates, 0.5, 1.0);

        GeometryFactory factory = new GeometryFactory();
        LineString polyRes = factory.createLineString(res);
        System.out.println(polyRes.toString());

        assertTrue(coordinates.length < res.length);

        // All points of input geom must be present in output geom
        for(Coordinate c : coordinates) {
            boolean found = false;
            for(Coordinate p : res) {
                if(p.distance(c) < 1e-6) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
}
