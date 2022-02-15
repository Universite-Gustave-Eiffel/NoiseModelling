package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class BezierContouringJDBCTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(BezierContouringJDBCTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testBezierContouring() throws SQLException, IOException {
        GeoJsonRead.importTable(connection, BezierContouringJDBCTest.class.getResource("lden_geom.geojson").getFile());
        GeoJsonRead.importTable(connection, BezierContouringJDBCTest.class.getResource("triangles.geojson").getFile());
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
        bezierContouring.setSmooth(true);
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

        SHPWrite.exportTable(connection, "target/contouring.shp", "CONTOURING_NOISE_MAP","UTF-8",true);
    }
}