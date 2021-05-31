package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class MakeParallelLinesTest {

    @Test
    public void makeParallelLine() throws ParseException {
        WKTReader wktReader  = new WKTReader();
        LineString line = (LineString)wktReader.read("LINESTRING(0 0,5 0,10 0)");

        LineString line2 = MakeParallelLines.MakeParallelLine(line, 0.1);

        LineString line3 = MakeParallelLines.MakeParallelLine(line, -0.1);

        System.out.println(new GeometryFactory().createMultiLineString(new LineString[]{line, line2, line3}));
    }




    @Test
    public void makeParallelLine2() throws ParseException {
        WKTReader wktReader  = new WKTReader();
        LineString line = (LineString)wktReader.read("LINESTRING(4 5,5 8,6 6,6 8,9 8,10 5,12 6)");

        LineString line2 = MakeParallelLines.MakeParallelLine(line, 0.5);

        System.out.println(new GeometryFactory().createMultiLineString(new LineString[]{line, line2}));
    }
}

