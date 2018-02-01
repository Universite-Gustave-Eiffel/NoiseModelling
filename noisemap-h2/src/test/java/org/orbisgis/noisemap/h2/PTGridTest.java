package org.orbisgis.noisemap.h2;

import org.h2.util.StringUtils;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Fortin
 */
public class PTGridTest {
    private static Connection connection;
    private Statement st;

    @BeforeClass
    public static void tearUpClass() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PTGridTest.class.getSimpleName(), false, "MV_STORE=FALSE"));
        org.h2gis.functions.factory.H2GISFunctions.load(connection);
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid3D(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid_f(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid3D_f(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid_Att_f(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_PtGrid3D_Att_f(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_SpectrumRepartition(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_EvalSource(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_SpectrumRepartition(), "");
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


    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(PTGridTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM " + StringUtils.quoteStringSQL(resourceFile.getPath());
    }


//
//    @Test
//    public void testFreeField() throws SQLException {
//        // Create empty buildings table
//        st.execute("DROP TABLE IF EXISTS BUILDINGS");
//        st.execute("CREATE TABLE BUILDINGS(the_geom POLYGON)");
//        // Create a single sound source
//        st.execute("DROP TABLE IF EXISTS roads_src_global");
//        st.execute("CREATE TEMPORARY TABLE roads_src_global(the_geom POINT, db_m double)");
//        // INSERT 2 points to set the computation area
//        st.execute("INSERT INTO roads_src_global VALUES ('POINT(15 0)'::geometry, 70)");
//        st.execute("INSERT INTO roads_src_global VALUES ('POINT(16 0)'::geometry, 70)");
//        st.execute("INSERT INTO roads_src_global VALUES ('POINT(17 0)'::geometry, 70)");
//        st.execute("INSERT INTO roads_src_global VALUES ('POINT(18 0)'::geometry, 70)");
//        // Compute spectrum repartition
//        st.execute("drop table if exists roads_src;\n" +
//                "CREATE TABLE roads_src AS SELECT the_geom,\n" +
//                "100 as db_m63,\n" +
//                "100 as db_m125,\n" +
//                "100 as db_m250,\n" +
//                "100 as db_m500,\n" +
//                "100 as db_m1000,\n" +
//                "100 as db_m2000,\n" +
//                "100 as db_m4000,\n" +
//                "100 as db_m8000 from roads_src_global;");
//        // Create receivers points
//        st.execute("DROP TABLE IF EXISTS RECEIVERS");
//        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
//        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(0 0)')");
//        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(-5 0)')");
//        // Compute noise map
//        st.execute("DROP TABLE IF EXISTS TEST");
//        ResultSet rs = st.executeQuery("SELECT * FROM BR_PtGrid_Att_f('buildings', 'roads_src','receivers', 'DB_M','', 100,100, 2,2,0.2)");
//        try {
//            while(rs.next())
//            {
//                System.out.print(rs.getInt(1));
//                System.out.print("\t");
//                System.out.print(rs.getInt(2));
//                System.out.print("\t");
//                System.out.print(rs.getInt(3));
//                System.out.print("\t");
//                System.out.print(rs.getDouble(4));
//                System.out.print("\n");
//            }
//        } finally {
//            rs.close();
//        }
//    }


    @Test
    public void testDemTopBuilding() throws Exception {
        st.execute(getRunScriptRes("dem.sql"));
        st.execute("drop table if exists sound_source;\n" +
                "create table sound_source(the_geom geometry, db_m100 double,db_m125 double,db_m160 double," +
                "db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,\n" +
                "db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double," +
                "db_m3150 double,db_m4000 double,db_m5000 double);\n" +
                "insert into sound_source VALUES ('POINT(68 60 5.5)', 100, 100, 100, 100, 100, 100, 100, 100, 100, 100," +
                " 100, 100, 100, 100, 100, 100, 100, 100);");
        st.execute("drop table if exists pt_lvl");
        // Create receivers points
        st.execute("DROP TABLE IF EXISTS RECEIVERS");
        st.execute("CREATE TABLE RECEIVERS(ID SERIAL, THE_GEOM POINT)");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 1.)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 0.2)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (-157 60 11.6)')");
        st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT (59 60 1.6)')");
        ResultSet rs = st.executeQuery("select * from BR_PTGRID3D('BUILDINGS', 'HEIGHT', 'SOUND_SOURCE','RECEIVERS', 'DB_M', '','DEM' ,1000,500, 2, 1, 0.2)");
        //  W must be equal to 1
        try {
            assertTrue(rs.next());
            // First receiver is hidden by the hill
            assertEquals(1, rs.getDouble("W"), 0.01);
            assertTrue(rs.next());
            // Second receiver is under the ground
            assertEquals(1, rs.getDouble("W"), 0.01);
            // Third receiver is higher than the hill
            assertTrue(rs.next());
            assertEquals(28.24, 10 * Math.log10(rs.getDouble("W")), 0.1);
            // Fourth receiver is just behind the wall of building. Only horizontal diffraction on edge is contributing
            assertTrue(rs.next());
            assertEquals(65.63, 10 * Math.log10(rs.getDouble("W")), 0.1);
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void testGroundReflectionPropagation() throws Exception {
        st.execute("DROP TABLE IF EXISTS LANDCOVER2000");
        st.execute("CALL SHPREAD('" + TriGridTest.class.getResource("landcover2000.shp").getFile() + "', 'LANDCOVER2000')");
        st.execute(getRunScriptRes("ground-effect.sql"));
        ResultSet rs = st.executeQuery("SELECT * FROM PT_LVL");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("GID"));
        assertEquals(56.91, 10 * Math.log10(rs.getDouble("W")), 0.1);
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("GID"));
        assertEquals(55.21, 10 * Math.log10(rs.getDouble("W")), 0.1);
        assertFalse(rs.next());
    }
}
