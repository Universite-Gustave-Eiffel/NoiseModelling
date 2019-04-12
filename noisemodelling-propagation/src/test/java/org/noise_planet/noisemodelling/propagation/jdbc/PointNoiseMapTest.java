package org.noise_planet.noisemodelling.propagation.jdbc;

import org.h2.util.StringUtils;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.SFSUtilities;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.noise_planet.noisemodelling.propagation.PropagationResultPtRecord;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;

public class PointNoiseMapTest {

    private static Connection connection;

    @BeforeClass
    public static void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PointNoiseMapTest.class.getSimpleName(), true, ""));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(PointNoiseMapTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+StringUtils.quoteStringSQL(resourceFile.getPath());
    }

    @Test
    public void testSplitting() {
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS", "RECEIVERS");

    }

    public void testLdenPointNoiseMap() {
        // Test optimisation on lden scenario
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS", "RECEIVERS");
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
            pointNoiseMap.setComputeHorizontalDiffraction(true);
            pointNoiseMap.setComputeVerticalDiffraction(true);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.setDemTable("DEM");
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            Collection<PropagationResultPtRecord> result =
                    pointNoiseMap.evaluateCell(connection, 0, 0, new EmptyProgressVisitor(), new HashSet<Long>());

//            assertEquals(3, result.size());
//            assertEquals(51.20, 10*Math.log10(result.get(0).getReceiverLvl()), 1e-2);
//            assertEquals(0, 10*Math.log10(result.get(1).getReceiverLvl()), 1e-2);
//            assertEquals(58.23, 10*Math.log10(result.get(2).getReceiverLvl()), 1e-2);
        }
    }
}