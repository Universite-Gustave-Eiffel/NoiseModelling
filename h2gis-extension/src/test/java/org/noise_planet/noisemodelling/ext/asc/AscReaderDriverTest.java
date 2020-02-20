package org.noise_planet.noisemodelling.ext.asc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
    import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
            assertEquals(234, rs.getInt("CELL_VAL"));
        }

        // Check last read cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-172.604,-89.867))")) {
            assertTrue(rs.next());
            assertEquals(114, rs.getInt("CELL_VAL"));
        }

        // Check nodata cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-177.438, -84.077))")) {
            assertTrue(rs.next());
            assertNull(rs.getObject("CELL_VAL"));
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
            assertEquals(234, rs.getInt("CELL_VAL"));
        }

        // Check last read cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-173.213, -89.771))")) {
            assertTrue(rs.next());
            assertEquals(114, rs.getInt("CELL_VAL"));
        }

        // Check nodata cell
        st = connection.createStatement();
        try(ResultSet rs = st.executeQuery("SELECT * FROM PRECIP30MIN WHERE ST_INTERSECTS(THE_GEOM, ST_MAKEPOINT(-177.3831, -84.5793))")) {
            assertTrue(rs.next());
            assertNull(rs.getObject("CELL_VAL"));
        }
    }
}