/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.h2;

import org.locationtech.jts.geom.Polygon;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Fortin
 */
public class TriGridTest {
    private static Connection connection;
    private Statement st;


    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(TriGridTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+ StringUtils.quoteStringSQL(resourceFile.getPath());
    }

    @BeforeClass
    public static void tearUpClass() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(TriGridTest.class.getSimpleName(), false, "MV_STORE=FALSE"));
        org.h2gis.functions.factory.H2GISFunctions.load(connection);
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_TriGrid(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_TriGrid3D(), "");
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

    @Test
    public void testFreeField() throws SQLException {
        // Create empty buildings table
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(the_geom POLYGON)");
        // Create a single sound source
        st.execute("DROP TABLE IF EXISTS roads_src_global");
        st.execute("CREATE TEMPORARY TABLE roads_src_global(the_geom POINT, db_m double)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(0 0)'::geometry, 85)");
        // INSERT 2 points to set the computation area
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(-20 -20)'::geometry, 0)");
        st.execute("INSERT INTO roads_src_global VALUES ('POINT(20 20)'::geometry, 0)");
        // Compute spectrum repartition
        st.execute("drop table if exists roads_src;\n" +
                "CREATE TABLE roads_src AS SELECT the_geom,\n" +
                "BR_SpectrumRepartition(100,1,db_m) as db_m100,\n" +
                "BR_SpectrumRepartition(125,1,db_m) as db_m125,\n" +
                "BR_SpectrumRepartition(160,1,db_m) as db_m160,\n" +
                "BR_SpectrumRepartition(200,1,db_m) as db_m200,\n" +
                "BR_SpectrumRepartition(250,1,db_m) as db_m250,\n" +
                "BR_SpectrumRepartition(315,1,db_m) as db_m315,\n" +
                "BR_SpectrumRepartition(400,1,db_m) as db_m400,\n" +
                "BR_SpectrumRepartition(500,1,db_m) as db_m500,\n" +
                "BR_SpectrumRepartition(630,1,db_m) as db_m630,\n" +
                "BR_SpectrumRepartition(800,1,db_m) as db_m800,\n" +
                "BR_SpectrumRepartition(1000,1,db_m) as db_m1000,\n" +
                "BR_SpectrumRepartition(1250,1,db_m) as db_m1250,\n" +
                "BR_SpectrumRepartition(1600,1,db_m) as db_m1600,\n" +
                "BR_SpectrumRepartition(2000,1,db_m) as db_m2000,\n" +
                "BR_SpectrumRepartition(2500,1,db_m) as db_m2500,\n" +
                "BR_SpectrumRepartition(3150,1,db_m) as db_m3150,\n" +
                "BR_SpectrumRepartition(4000,1,db_m) as db_m4000,\n" +
                "BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;");
        // Compute noise map
        ResultSet rs = st.executeQuery("SELECT * FROM BR_TRIGRID('buildings', 'roads_src', 'DB_M','', 700, 100,1,3,50,2,1,0.2)");
        try {
            assertTrue(rs.next());
            do {
                assertTrue(rs.getDouble("W_V1") >= 1);
                assertTrue(rs.getDouble("W_V2") >= 1);
                assertTrue(rs.getDouble("W_V3") >= 1);
                assertNotNull(rs.getObject("THE_GEOM"));
                assertTrue(rs.getObject("THE_GEOM") instanceof Polygon);
                assertEquals(4, ((Polygon) rs.getObject("THE_GEOM")).getNumPoints());
            } while (rs.next());
        } finally {
            rs.close();
        }
    }


    @Test
    public void testNoSource() throws SQLException {
        // Create empty buildings table
        st.execute("DROP TABLE IF EXISTS BUILDINGS");
        st.execute("CREATE TABLE BUILDINGS(the_geom geometry)");
        st.execute("INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((5 20 0,20 20 0,20 60 0,0 60 0,5 20 0)))'))");
        // Create a single sound source
        st.execute("DROP TABLE IF EXISTS roads_src_global");
        st.execute("CREATE TABLE roads_src_global(the_geom POINT, db_m double)");
        // Compute spectrum repartition
        st.execute("drop table if exists roads_src;\n" +
                "CREATE TABLE roads_src AS SELECT the_geom,\n" +
                "BR_SpectrumRepartition(100,1,db_m) as db_m100,\n" +
                "BR_SpectrumRepartition(125,1,db_m) as db_m125,\n" +
                "BR_SpectrumRepartition(160,1,db_m) as db_m160,\n" +
                "BR_SpectrumRepartition(200,1,db_m) as db_m200,\n" +
                "BR_SpectrumRepartition(250,1,db_m) as db_m250,\n" +
                "BR_SpectrumRepartition(315,1,db_m) as db_m315,\n" +
                "BR_SpectrumRepartition(400,1,db_m) as db_m400,\n" +
                "BR_SpectrumRepartition(500,1,db_m) as db_m500,\n" +
                "BR_SpectrumRepartition(630,1,db_m) as db_m630,\n" +
                "BR_SpectrumRepartition(800,1,db_m) as db_m800,\n" +
                "BR_SpectrumRepartition(1000,1,db_m) as db_m1000,\n" +
                "BR_SpectrumRepartition(1250,1,db_m) as db_m1250,\n" +
                "BR_SpectrumRepartition(1600,1,db_m) as db_m1600,\n" +
                "BR_SpectrumRepartition(2000,1,db_m) as db_m2000,\n" +
                "BR_SpectrumRepartition(2500,1,db_m) as db_m2500,\n" +
                "BR_SpectrumRepartition(3150,1,db_m) as db_m3150,\n" +
                "BR_SpectrumRepartition(4000,1,db_m) as db_m4000,\n" +
                "BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;");
        // Compute noise map
        ResultSet rs = st.executeQuery("SELECT * FROM BR_TRIGRID((SELECT ST_ENVELOPE(the_geom) from buildings),'buildings', 'roads_src', 'DB_M','', 700, 100,1,3,50,2,1,0.2)");
        try {
            assertTrue(rs.next());
            do {
                assertTrue(rs.getDouble("W_V1") >= 1);
                assertTrue(rs.getDouble("W_V2") >= 1);
                assertTrue(rs.getDouble("W_V3") >= 1);
                assertNotNull(rs.getObject("THE_GEOM"));
                assertTrue(rs.getObject("THE_GEOM") instanceof Polygon);
                assertEquals(4, ((Polygon) rs.getObject("THE_GEOM")).getNumPoints());
            } while (rs.next());
        } finally {
            rs.close();
        }
    }
    @Test
    public void testMultipleBuildings() throws Exception {
        st.execute(getRunScriptRes("multiple_buildings.sql"));
        st.execute("drop table if exists tri_lvl");
        ResultSet rs = st.executeQuery("select * from BR_TRIGRID('BUILDINGS', 'SOUND_SOURCE', 'DB_M', '', 1000, 100, 2, 3, 0, 2, 1, 0.2)");
        try {
            assertTrue(rs.next());
            do {
                assertTrue(rs.getDouble("W_V1") >= 1);
                assertTrue(rs.getDouble("W_V2") >= 1);
                assertTrue(rs.getDouble("W_V3") >= 1);
                assertNotNull(rs.getObject("THE_GEOM"));
                assertTrue(rs.getObject("THE_GEOM") instanceof Polygon);
                assertEquals(4, ((Polygon) rs.getObject("THE_GEOM")).getNumPoints());
            } while (rs.next());
        } finally {
            rs.close();
        }
    }
}
