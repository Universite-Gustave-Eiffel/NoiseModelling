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

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.factory.H2GISFunctions;
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
        connection = H2GISDBFactory.createSpatialDataBase(ScalarFunctionTest.class.getSimpleName(), true);
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_EvalSource(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BR_SpectrumRepartition(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BTW_EvalSource(), "");
        H2GISFunctions.registerFunction(connection.createStatement(), new BTW_SpectrumRepartition(), "");
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
        assertEquals(82.96, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource6Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15,0, 15, 50)");
        assertTrue(rs.next());
        assertEquals(83.21, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource6Down() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15, 15, 0, 50)");
        assertTrue(rs.next());
        assertEquals(83.03, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource7Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 30, 1500, 15,0, 15, 50)");
        assertTrue(rs.next());
        assertEquals(83.23, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_EvalSource10Up() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_EvalSource(50, 1500, 15, 15, 50, 52, 0, 15, 50, false)");
        assertTrue(rs.next());
        assertEquals(83.21, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBR_SpectrumRepartition() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BR_SpectrumRepartition(1000,1,83)");
        assertTrue(rs.next());
        assertEquals(76, rs.getDouble(1), 0.01);
    }

    @Test(expected = SQLException.class)
    public void testBR_SpectrumRepartitionErr() throws SQLException {
        st.executeQuery("SELECT BR_SpectrumRepartition(1000,0,83)");
    }

    @Test(expected = SQLException.class)
    public void testBR_SpectrumRepartitionErr2() throws SQLException {
        st.executeQuery("SELECT BR_SpectrumRepartition(1001,1,83)");
    }

    @Test
    public void testBTW_EvalSource() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BTW_EvalSource(40,10,1,false)");
        assertTrue(rs.next());
        assertEquals(79, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBTW_EvalSourceAntiVib() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BTW_EvalSource(40,10,1,true)");
        assertTrue(rs.next());
        assertEquals(77, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBTW_EvalSourceGrass() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BTW_EvalSource(40,10,0,false)");
        assertTrue(rs.next());
        assertEquals(76, rs.getDouble(1), 0.01);
    }

    @Test
    public void testBTW_SpectrumRepartition() throws SQLException {
        ResultSet rs = st.executeQuery("SELECT BTW_SpectrumRepartition(1000,83)");
        assertTrue(rs.next());
        assertEquals(71.7, rs.getDouble(1), 0.01);
    }

    @Test(expected = SQLException.class)
    public void testBTW_SpectrumRepartitionErr() throws SQLException {
        st.executeQuery("SELECT BR_SpectrumRepartition(1001,83)");
    }

}
