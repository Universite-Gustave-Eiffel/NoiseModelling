package org.orbisgis.noisemap.core.jdbc;

import org.h2.util.StringUtils;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.orbisgis.noisemap.core.PropagationResultTriRecord;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestTriangleNoiseMap {

    private static Connection connection;

    @BeforeClass
    public static void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(TestPointNoiseMap.class.getSimpleName(), false, ""));
        H2GISFunctions.load(connection);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    private static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(TestPointNoiseMap.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+ StringUtils.quoteStringSQL(resourceFile.getPath());
    }


    /**
     * DEM is 22m height between sources and receiver. Sound level should be 0 dB(A) in direct field.
     * @throws SQLException
     */
    @Test
    public void testProblematicBuildings() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(getRunScriptRes("scene_buildings_issues.sql"));
            TriangleNoiseMap triangleNoiseMap = new TriangleNoiseMap("BUILDINGS", "ROADS");
            triangleNoiseMap.setSoundDiffractionOrder(0);
            triangleNoiseMap.setSoundReflectionOrder(0);
            triangleNoiseMap.setHeightField("HEIGHT");
            triangleNoiseMap.setRoadWidth(2.5);
            triangleNoiseMap.setMaximumArea(75.0);
            triangleNoiseMap.setMaximumPropagationDistance(750);
            triangleNoiseMap.initialize(connection, new EmptyProgressVisitor());
            List<PropagationResultTriRecord> result =
                    new ArrayList<>(triangleNoiseMap.evaluateCell(connection, 0, 0, new EmptyProgressVisitor()));
            assertFalse(result.isEmpty());
        }
    }

}
