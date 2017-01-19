/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
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
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */

package org.orbisgis.noisemap.core.jdbc;

import org.h2.util.StringUtils;
import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.h2gis.h2spatialapi.EmptyProgressVisitor;
import org.h2gis.h2spatialext.CreateSpatialExtension;
import org.h2gis.utilities.SFSUtilities;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orbisgis.noisemap.core.PropagationDebugInfo;
import org.orbisgis.noisemap.core.PropagationProcess;
import org.orbisgis.noisemap.core.PropagationProcessData;
import org.orbisgis.noisemap.core.PropagationProcessOut;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Nicolas Fortin
 */
public class TestPointNoiseMap {
    private static Connection connection;

    @BeforeClass
    public static void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(SpatialH2UT.createSpatialDataBase(TestPointNoiseMap.class.getSimpleName(), false, ""));
        CreateSpatialExtension.initSpatialExtension(connection);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(TestPointNoiseMap.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+StringUtils.quoteStringSQL(resourceFile.getPath());
    }

    /**
     * DEM is 22m height between sources and receiver. Sound level should be 0 dB(A) in direct field.
     * @throws SQLException
     */
    @Test
    public void testDem() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("scene_with_dem.sql"));
            st.execute("DELETE FROM sound_source WHERE GID = 1");
            st.execute("UPDATE sound_source SET THE_GEOM = 'POINT(120 -18 1.6)' WHERE GID = 2");
            st.execute("DROP TABLE IF EXISTS RECEIVERS");
            st.execute("CREATE TABLE RECEIVERS(the_geom POINT, GID SERIAL)");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-275 -18 20)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-275 -18 1.6)')");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "SOUND_SOURCE", "RECEIVERS");
            pointNoiseMap.setSoundDiffractionOrder(0);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.setDemTable("DEM");
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            List<PropagationResultPtRecord> result =
                    new ArrayList<>(pointNoiseMap.evaluateCell(connection, 0, 0, new EmptyProgressVisitor()));
            assertEquals(2, result.size());
            assertEquals(47.75, 10*Math.log10(result.get(0).getReceiverLvl()), 1e-2);
            assertEquals(0, 10*Math.log10(result.get(1).getReceiverLvl()), 1e-2);
        }
    }

    /**
     * DEM is 22m height between sources and receiver. There is a direct field propagation over the building
     * @throws SQLException
     */
    @Test
    public void testDemTopOfBuilding() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("scene_with_dem.sql"));
            st.execute("TRUNCATE TABLE BUILDINGS");
            st.execute("INSERT INTO buildings VALUES (" +
                    "'MULTIPOLYGON (((80 -30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 -30 0,80 -30 0)))',4)");
            st.execute("DELETE FROM sound_source WHERE GID = 1");
            st.execute("UPDATE sound_source SET THE_GEOM = 'POINT(200 -18 1.6)' WHERE GID = 2");
            st.execute("DROP TABLE IF EXISTS RECEIVERS");
            st.execute("CREATE TABLE RECEIVERS(the_geom POINT, GID SERIAL)");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-72 41 11)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-9 41 1.6)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(70 11 7)')");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "SOUND_SOURCE", "RECEIVERS");
            pointNoiseMap.setSoundDiffractionOrder(0);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.setDemTable("DEM");
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            List<PropagationResultPtRecord> result =
                    new ArrayList<>(pointNoiseMap.evaluateCell(connection, 0, 0, new EmptyProgressVisitor()));
            assertEquals(3, result.size());
            assertEquals(51.20, 10*Math.log10(result.get(0).getReceiverLvl()), 1e-2);
            assertEquals(0, 10*Math.log10(result.get(1).getReceiverLvl()), 1e-2);
            assertEquals(58.23, 10*Math.log10(result.get(2).getReceiverLvl()), 1e-2);
        }
    }

    /**
     * Check if sound reflection is bounds by building height.
     * @throws SQLException
     */
    @Test
    public void testReflectionZBounds() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("scene_with_dem.sql"));
            st.execute("TRUNCATE TABLE BUILDINGS");
            st.execute("INSERT INTO buildings VALUES (" +
                    "'MULTIPOLYGON (((80 -30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 -30 0,80 -30 0)))',4)");
            st.execute("DELETE FROM sound_source WHERE GID = 1");
            st.execute("UPDATE sound_source SET THE_GEOM = 'POINT(200 -18 1.6)' WHERE GID = 2");
            st.execute("DROP TABLE IF EXISTS RECEIVERS");
            st.execute("CREATE TABLE RECEIVERS(the_geom POINT, GID SERIAL)");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-72 41 11)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-9 41 1.6)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(70 11 7)')");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "SOUND_SOURCE", "RECEIVERS");
            pointNoiseMap.setSoundDiffractionOrder(0);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.setDemTable("DEM");
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            List<PropagationResultPtRecord> result =
                    new ArrayList<>(pointNoiseMap.evaluateCell(connection, 0, 0, new EmptyProgressVisitor()));
            assertEquals(3, result.size());
            assertEquals(51.20, 10*Math.log10(result.get(0).getReceiverLvl()), 1e-2);
            assertEquals(0, 10*Math.log10(result.get(1).getReceiverLvl()), 1e-2);
            assertEquals(58.23, 10*Math.log10(result.get(2).getReceiverLvl()), 1e-2);
        }
    }

    @Test
    public void testReflection() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("scene_without_dem.sql"));
            PointNoiseMap nm = new PointNoiseMap("BUILDINGS", "SOUND_SOURCE", "RECEIVERS");
            nm.setHeightField("HEIGHT");
            nm.setSoundDiffractionOrder(0);
            nm.setSoundReflectionOrder(2);
            nm.setComputeVerticalDiffraction(false);
            List<PropagationDebugInfo> debugInfo = new ArrayList<>();
            nm.initialize(connection, new EmptyProgressVisitor());
            PropagationProcessData propInput = nm.prepareCell(connection, 0, 0, new EmptyProgressVisitor(), new ArrayList<Long>());
            PropagationProcessOut threadDataOut = new PropagationProcessOut();
            PropagationProcess propaProcess = new PropagationProcess(
                    propInput, threadDataOut);
            propaProcess.runDebug(debugInfo);
            assertEquals(4, debugInfo.size());
        }
    }

}
