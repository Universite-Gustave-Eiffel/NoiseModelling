package org.orbisgis.noisemap.h2;

import org.h2gis.h2spatial.CreateSpatialExtension;
import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Fortin
 */
public class ScalarFunctionTest {
    private static Connection connection;
    private Statement st;

    @BeforeClass
    public static void tearUpClass() throws Exception {
        connection = SpatialH2UT.createSpatialDataBase(ScalarFunctionTest.class.getSimpleName(), true);
        CreateSpatialExtension.registerFunction(connection.createStatement(), new BR_EvalSource(), "");
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        connection.close();
    }

    @Before
    public void setUp() throws Exception {
        st = connection.createStatement();
    }

    @After
    public void tearDown() throws Exception {
        st.close();
    }

    @Test
    public void testBR_EvalSource() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15)");
        assertTrue(rs.next());
        assertEquals(77.91, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource6Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15,0, 15, 50)");
        assertTrue(rs.next());
        assertEquals(79.21, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource6Down() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15, 15, 0, 50)");
        assertTrue(rs.next());
        assertEquals(77.67, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource7Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 30, 1500, 15,0, 15, 50)");
        assertTrue(rs.next());
        assertEquals(81.36, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource10Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15, 15, 50, 52, 0, 15, 50, false)");
        assertTrue(rs.next());
        assertEquals(79.21, rs.getDouble(1), 0.01);
    }
}
