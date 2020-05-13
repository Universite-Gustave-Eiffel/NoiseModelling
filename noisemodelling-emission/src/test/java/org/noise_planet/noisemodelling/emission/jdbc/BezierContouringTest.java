package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
    }
}