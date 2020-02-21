package org.noise_planet.noisemodelling.ext.asc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
    import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import static org.junit.Assert.*;

public class AscReaderDriverTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(AscReaderDriverTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testReadPrecip() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();
        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }

        // Check database content

        // Check first read cell
        Statement st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-179.74,-80.18))")) {
            assertTrue(rs.next());
            assertEquals(234, rs.getInt("Z"));
        }

        // Check last read cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-172.604,-89.867))")) {
            assertTrue(rs.next());
            assertEquals(114, rs.getInt("Z"));
        }

        // Check nodata cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-177.438, -84.077))")) {
            assertTrue(rs.next());
            assertNull(rs.getObject("Z"));
        }
    }



    @Test
    public void testReadPrecipCenter() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();
        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min_center.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }

        // Check database content

        // Check first read cell
        Statement st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-180.1454, -80.303))")) {
            assertTrue(rs.next());
            assertEquals(234, rs.getInt("Z"));
        }

        // Check last read cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-173.213, -89.771))")) {
            assertTrue(rs.next());
            assertEquals(114, rs.getInt("Z"));
        }

        // Check nodata cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-177.3831, -84.5793))")) {
            assertTrue(rs.next());
            assertNull(rs.getObject("Z"));
        }
    }
    @Test
    public void testReadPrecipPoint() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();
        reader.setAs3DPoint(true);
        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }

        // Check database content

        // Check first read cell
        Statement st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT ST_Z(THE_GEOM) Z FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_EXPAND(ST_MAKEPOINT(-179.74,-80.18), 0.25, 0.25))")) {
            assertTrue(rs.next());
            assertEquals(234, rs.getInt("Z"));
        }

        // Check last read cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT ST_Z(THE_GEOM) Z FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_EXPAND( ST_MAKEPOINT(-172.604,-89.867), 0.25, 0.25))")) {
            assertTrue(rs.next());
            assertEquals(114, rs.getInt("Z"));
        }

        // Check nodata cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT  ST_Z(THE_GEOM) Z FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_EXPAND( ST_MAKEPOINT(-177.438, -84.077), 0.25, 0.25))")) {
            assertFalse(rs.next());
        }
    }


    @Test
    public void testReadPrecipEnvelope() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();

        reader.setExtractEnvelope(new Envelope(-178.242, -174.775, -89.707, -85.205));

        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }

        // Check database content

        // Check number of extracted cells
        Statement st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT COUNT(*) CPT FROM PRECIP30MIN")) {
            assertTrue(rs.next());
            assertEquals(90, rs.getInt("CPT"));
        }

    }

    @Test
    public void testReadPrecipLimit() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();
        reader.setLimit(50);

        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }

        // Check database content

        // Check number of extracted cells
        Statement st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT COUNT(*) CPT FROM PRECIP30MIN")) {
            assertTrue(rs.next());
            assertEquals(50, rs.getInt("CPT"));
        }
    }



//    @Test
//    public void testReadBigFile() throws IOException, SQLException {
//        AscReaderDriver reader = new AscReaderDriver();
//        reader.setExtractEnvelope(new Envelope(606084.78, 625191.882, 6868551.62, 6885046.96));
//        long start = System.currentTimeMillis();
//        try(InputStream inputStream = new FileInputStream("IDF_TOUT_asc.asc")) {
//            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 2154);
//        }
//        System.out.println(String.format(Locale.ROOT, "Done in %.3f s",(System.currentTimeMillis() - start)/1e3));
//        // Check number of extracted cells
//        SHPWrite.exportTable(connection, "export.shp", "PRECIP30MIN");
//    }
}