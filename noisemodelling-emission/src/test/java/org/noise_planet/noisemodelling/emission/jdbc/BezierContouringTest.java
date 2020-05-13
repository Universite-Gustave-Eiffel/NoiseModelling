package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class BezierContouringTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(BezierContouringTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }


    @Test
    public void testBezierContouring() throws SQLException, IOException {
        GeoJsonRead.readGeoJson(connection, BezierContouringTest.class.getResource("lden_geom.geojson").getFile());
        GeoJsonRead.readGeoJson(connection, BezierContouringTest.class.getResource("triangles.geojson").getFile());
        try(Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE LDEN_GEOM ALTER COLUMN IDRECEIVER INTEGER NOT NULL");
            st.execute("ALTER TABLE LDEN_GEOM ADD PRIMARY KEY (IDRECEIVER)");
            st.execute("ALTER TABLE TRIANGLES ALTER COLUMN PK INTEGER NOT NULL");
            st.execute("ALTER TABLE TRIANGLES ADD PRIMARY KEY (PK)");
            st.execute("CREATE INDEX ON TRIANGLES(CELL_ID)");
        }

        long start = System.currentTimeMillis();
        BezierContouring bezierContouring = new BezierContouring(BezierContouring.NF31_133_ISO, 2154);
        bezierContouring.setPointTable("LDEN_GEOM");
        bezierContouring.setPointTableField("LAEQ");
        bezierContouring.createTable(connection);
        System.out.println("Contouring done in " + (System.currentTimeMillis() - start) + " ms");

        assertTrue(JDBCUtilities.tableExists(connection, "CONTOURING_NOISE_MAP"));

        List<String> fieldValues = JDBCUtilities.getUniqueFieldValues(connection, "CONTOURING_NOISE_MAP", "ISOLVL");
        assertTrue(fieldValues.contains("0"));
        assertTrue(fieldValues.contains("1"));
        assertTrue(fieldValues.contains("2"));
        assertTrue(fieldValues.contains("3"));
        assertTrue(fieldValues.contains("4"));
        assertTrue(fieldValues.contains("5"));
        assertTrue(fieldValues.contains("6"));
        assertTrue(fieldValues.contains("7"));
        assertTrue(fieldValues.contains("8"));
        assertTrue(fieldValues.contains("9"));
    }

    @Test
    public void testBezierCurve() throws ParseException {
        String poly = "POLYGON ((15 15, 30 60, 80 50, 80 20, 60 40, 60 10, 30 10, 15 15))";
        WKTReader wktReader = new WKTReader();
        Geometry geom = wktReader.read(poly);
        Coordinate[] coordinates = geom.getCoordinates();
        Coordinate[] res = BezierContouring.interpolate(coordinates, 1.0);
        GeometryFactory factory = new GeometryFactory();
        LineString polyRes = factory.createLineString(res);
        System.out.println(polyRes.toString());
    }
}