package org.orbisgis.noisemap.core.jdbc;

import com.vividsolutions.jts.geom.Coordinate;
import org.h2.util.StringUtils;
import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.h2gis.h2spatialapi.EmptyProgressVisitor;
import org.h2gis.h2spatialext.CreateSpatialExtension;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Nicolas Fortin
 */
public class TestPointNoiseMap {
    private static Connection connection;

    @BeforeClass
    public static void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(SpatialH2UT.createSpatialDataBase(TestPointNoiseMap.class.getSimpleName(), false));
        CreateSpatialExtension.initSpatialExtension(connection);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    /**
     * DEM is 22m height between sources and receiver. Sound level should be 0 dB(A) in direct field.
     * @throws SQLException
     */
    @Test
    public void testDem() throws SQLException {
        try(Statement st = connection.createStatement()) {
            URL scriptPath = TestPointNoiseMap.class.getResource("scene_with_dem.sql");
            st.execute("RUNSCRIPT FROM "+StringUtils.quoteStringSQL(scriptPath.toString()));
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
}
