package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.functions.spatial.mesh.DelaunayData;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.TableUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.LayerTinfour;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
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

    }

    @Test
    public void testContouring3D() throws SQLException, IOException, LayerDelaunayError {
        // Will create elevation iso from DEM table
        GeoJsonRead.importTable(connection, Paths.get(Paths.get(System.getProperty("user.dir")).getParent().toString(),
                "wps_scripts/src/test/resources/org/noise_planet/noisemodelling/wps/dem.geojson").toString());
        LayerTinfour delaunayTool = new LayerTinfour();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT the_geom FROM DEM")) {
            try (SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
                while (rs.next()) {
                    Geometry pt = rs.getGeometry();
                    if(pt != null) {
                        delaunayTool.addVertex(pt.getCoordinate());
                    }
                }
            }
        }
        delaunayTool.processDelaunay();
        TriangleNoiseMap.generateResultTable(connection, "RECEIVERS", "TRIANGLES",
                new AtomicInteger(), delaunayTool.getVertices(), new GeometryFactory(), delaunayTool.getTriangles(),
                0, 0, 1);
        try(Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE RECEIVERS ADD COLUMN HEIGHT FLOAT");
            st.execute("UPDATE RECEIVERS SET HEIGHT = ST_Z(THE_GEOM)");
        }
        long start = System.currentTimeMillis();
        BezierContouring bezierContouring = new BezierContouring(Arrays.asList(0.,5.,10.,15.,20.,25.,30.,35.), 2154);
        bezierContouring.setPointTable("RECEIVERS");
        bezierContouring.setPointTableField("HEIGHT");
        bezierContouring.setSmooth(false);
        bezierContouring.setMergeTriangles(false);
        bezierContouring.createTable(connection);
        System.out.println("Contouring done in " + (System.currentTimeMillis() - start) + " ms");

        assertTrue(JDBCUtilities.tableExists(connection, "CONTOURING_NOISE_MAP"));

        // Check Z values in CONTOURING_NOISE_MAP
        try(Statement st = connection.createStatement()) {
            try(ResultSet rs = st.executeQuery("SELECT MAX(ST_ZMAX(THE_GEOM)) MAXZ, MIN(ST_ZMIN(THE_GEOM)) MINZ FROM CONTOURING_NOISE_MAP")) {
                assertTrue(rs.next());
                assertEquals(33.2, rs.getDouble("MAXZ"), 0.01);
                assertEquals(-1.79, rs.getDouble("MINZ"), 0.01);
            }
        }

    }
}