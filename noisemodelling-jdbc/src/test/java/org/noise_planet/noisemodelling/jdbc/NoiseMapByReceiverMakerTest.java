/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.NoiseMapWriter;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.jdbc.utils.IsoSurface;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.GroundAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class NoiseMapByReceiverMakerTest {

    private Connection connection;

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(NoiseMapByReceiverMakerTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    /**
     * Check if ground surface are split according to {@link GridMapMaker#groundSurfaceSplitSideLength}
     * @throws Exception
     */
    @Test
    public void testGroundSurface() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'LANDCOVER2000')", NoiseMapByReceiverMakerTest.class.getResource("landcover2000.shp").getFile()));
            st.execute(getRunScriptRes("scene_with_landcover.sql"));
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.setSoilTableName("LAND_G");
            noiseMapByReceiverMaker.setFrequencyFieldPrepend("DB_M");
            noiseMapByReceiverMaker.initialize(connection, new EmptyProgressVisitor());

            Set<Long> processedReceivers = new HashSet<>();
            Map<CellIndex, Integer> populatedCells = noiseMapByReceiverMaker.searchPopulatedCells(connection);
            double expectedMaxArea = Math.pow(noiseMapByReceiverMaker.getGroundSurfaceSplitSideLength(), 2);
            assertFalse(populatedCells.isEmpty());
            for (Map.Entry<CellIndex, Integer> indexIntegerEntry : populatedCells.entrySet()) {
                SceneWithEmission scene = noiseMapByReceiverMaker.prepareCell(connection, indexIntegerEntry.getKey(), processedReceivers);
                assertFalse(scene.profileBuilder.getGroundEffects().isEmpty());
                for(GroundAbsorption soil : scene.profileBuilder.getGroundEffects()) {
                    assertTrue(soil.getGeometry().getArea() < expectedMaxArea);
                }
                assertEquals(3, scene.wjSources.size());
                assertEquals(1, scene.wjSources.get(1L).size());
                assertEquals("D", scene.wjSources.get(1L).get(0).period);
            }
        }
    }

    private static String createSource(Geometry source, double lvl, Orientation sourceOrientation, int directivityId) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ROADS_GEOM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY, YAW REAL, PITCH REAL, ROLL REAL, DIR_ID INT");
        StringBuilder values = new StringBuilder("(row_number() over())::int, ST_SETSRID('");
        values.append(new WKTWriter(3).write(source));
        values.append("', 2154) THE_GEOM, ");
        values.append(sourceOrientation.yaw);
        values.append(" YAW, ");
        values.append(sourceOrientation.pitch);
        values.append(" PITCH, ");
        values.append(sourceOrientation.roll);
        values.append(" ROLL, ");
        values.append(directivityId);
        values.append(" DIR_ID");
        AttenuationParameters data = new AttenuationParameters(false);
        for(String period : new String[] {"D", "E", "N"}) {
            for (int freq : data.getFrequencies()) {
                String fieldName = "HZ" + period + freq;
                sb.append(", ");
                sb.append(fieldName);
                sb.append(" real");
                values.append(", ");
                values.append(String.format(Locale.ROOT, "%.2f", lvl));
                values.append(" ");
                values.append(fieldName);
            }
        }
        sb.append(") AS select ");
        sb.append(values.toString());
        return sb.toString();
    }


    @Test
    public void testPointDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");
            st.execute(createSource(new GeometryFactory().createPoint(new Coordinate(223915.72,6757480.22,0.0 )),
                    91,
                    new Orientation(90,15,0),
                    4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223915.72 6757490.22 0.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223925.72 6757480.22 0.0)');");
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "ROADS_GEOM", "RECEIVERS");
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(false);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setMaximumPropagationDistance(1000);
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setCoefficientVersion(1);

            // Use train directivity functions instead of discrete directivity
            noiseMapByReceiverMaker.getSceneInputSettings().setUseTrainDirectivity(true);

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

            try(ResultSet rs = st.executeQuery("SELECT HZ63 FROM " + parameters.receiversLevelTable + " WHERE PERIOD='DEN' ORDER BY IDRECEIVER")) {
                assertTrue(rs.next());
                assertEquals(73.3, rs.getDouble(1), 0.1);
                assertTrue(rs.next());
                assertEquals(53.3, rs.getDouble(1), 0.1);
                assertFalse(rs.next());
            }
        }
    }



    @Test
    public void testLineDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");
            st.execute(createSource(new GeometryFactory().createLineString(
                    new Coordinate[]{new Coordinate(223915.72,6757480.22 ,5),
                            new Coordinate(223920.72,6757485.22, 5.1 )}), 91,
                    new Orientation(0,0,0),4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(pointZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223922.55 6757495.27 4.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223936.42 6757471.91 4.0)');");
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "ROADS_GEOM", "RECEIVERS");
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(false);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setMaximumPropagationDistance(1000);
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setCoefficientVersion(1);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportCnossosPathWithAttenuation = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = true;
            noiseMapByReceiverMaker.setBodyBarrier(true);

            // Use train directivity functions instead of discrete directivity
            DefaultTableLoader defaultTableLoader = ((DefaultTableLoader) noiseMapByReceiverMaker.getPropagationProcessDataFactory());
            defaultTableLoader.insertTrainDirectivity();
            AttenuationParameters daySettings = new AttenuationParameters();
            daySettings.setTemperature(20);
            AttenuationParameters eveningSettings = new AttenuationParameters();
            eveningSettings.setTemperature(18);
            AttenuationParameters nightSettings = new AttenuationParameters();
            nightSettings.setTemperature(16);
            defaultTableLoader.cnossosParametersPerPeriod.put("D", daySettings);
            defaultTableLoader.cnossosParametersPerPeriod.put("E", eveningSettings);
            defaultTableLoader.cnossosParametersPerPeriod.put("N", nightSettings);

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, HZ63 FROM " + parameters.receiversLevelTable + " WHERE PERIOD='DEN' ORDER BY IDRECEIVER")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals(68.3, rs.getDouble(2), 1);
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                assertEquals(70.8, rs.getDouble(2), 1);
                assertFalse(rs.next());
            }

            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH FROM " + parameters.raysTable + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                CnossosPath cnossosPath = NoiseMapWriter.jsonToPropagationPath(rs.getString(2));
                // This is source orientation, not relevant to receiver position
                assertOrientationEquals(new Orientation(45, 0.81, 0), cnossosPath.getSourceOrientation(), 0.01);
                assertOrientationEquals(new Orientation(330.2084079818916,-5.947213381005439,0.0), cnossosPath.raySourceReceiverDirectivity, 0.01);
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                cnossosPath = NoiseMapWriter.jsonToPropagationPath(rs.getString(2));
                assertOrientationEquals(new Orientation(45, 0.81, 0), cnossosPath.getSourceOrientation(), 0.01);
                assertOrientationEquals(new Orientation(336.9922375343167,-4.684918495003125,0.0), cnossosPath.raySourceReceiverDirectivity, 0.01);
            }
        }
    }


    public static void assertOrientationEquals(Orientation orientationA, Orientation orientationB, double epsilon) {
        assertArrayEquals(new double[]{orientationA.yaw, orientationA.pitch, orientationA.roll},
                new double[]{orientationB.yaw, orientationB.pitch, orientationB.roll}, epsilon, orientationA+" != "+orientationB);
    }

    @Test
    public void testPointRayDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");
            // create source point direction east->90°
            st.execute(createSource(new GeometryFactory().createPoint(new Coordinate(3.5,3,1.0 )),
                    91, new Orientation(90,0,0),4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (4.5 3 1.0)');" + //front
                    "insert into receivers(the_geom) values ('POINTZ (2.5 3 1.0)');" + //behind
                    "insert into receivers(the_geom) values ('POINTZ (3.5 2 1.0)');" + //right
                    "insert into receivers(the_geom) values ('POINTZ (3.5 4 1.0)');"); //left
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "ROADS_GEOM", "RECEIVERS");
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(false);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setMaximumPropagationDistance(1000);
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setCoefficientVersion(1);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportCnossosPathWithAttenuation = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = true;
            noiseMapByReceiverMaker.setBodyBarrier(true);

            // Use train directivity functions instead of discrete directivity
            DefaultTableLoader defaultTableLoader = ((DefaultTableLoader) noiseMapByReceiverMaker.getPropagationProcessDataFactory());
            defaultTableLoader.insertTrainDirectivity();
            AttenuationParameters daySettings = new AttenuationParameters();
            daySettings.setTemperature(20);
            AttenuationParameters eveningSettings = new AttenuationParameters();
            eveningSettings.setTemperature(18);
            AttenuationParameters nightSettings = new AttenuationParameters();
            nightSettings.setTemperature(16);
            defaultTableLoader.cnossosParametersPerPeriod.put("D", daySettings);
            defaultTableLoader.cnossosParametersPerPeriod.put("E", eveningSettings);
            defaultTableLoader.cnossosParametersPerPeriod.put("N", nightSettings);

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

            List<CnossosPath> pathsParameters = new ArrayList<>();
            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH FROM " + parameters.raysTable + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
                while (rs.next()) {
                    CnossosPath cnossosPath = NoiseMapWriter.jsonToPropagationPath(rs.getString("PATH"));
                    pathsParameters.add(cnossosPath);
                }
            }
            assertEquals(4 , pathsParameters.size());
            CnossosPath pathParameters = pathsParameters.remove(0);
            assertEquals(1, pathParameters.getCutProfile().getReceiver().receiverPk);
            // receiver is front of source
            assertEquals(new Orientation(0, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
            pathParameters = pathsParameters.remove(0);
            assertEquals(2, pathParameters.getCutProfile().getReceiver().receiverPk);
            // receiver is behind of the source
            assertEquals(new Orientation(180, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
            pathParameters = pathsParameters.remove(0);
            assertEquals(3, pathParameters.getCutProfile().getReceiver().receiverPk);
            // receiver is on the right of the source
            assertEquals(new Orientation(90, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
            pathParameters = pathsParameters.remove(0);
            assertEquals(4, pathParameters.getCutProfile().getReceiver().receiverPk);
            // receiver is on the left of the source
            assertEquals(new Orientation(360-90, 0, 0), pathParameters.getRaySourceReceiverDirectivity());

        }
    }



    @Test
    public void testEmissionTrafficTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'ROADS_TRAFF')", NoiseMapByReceiverMakerTest.class.getResource("roads_traff.shp").getFile()));
            st.execute("CREATE TABLE SOURCES_GEOM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY) AS SELECT PK, THE_GEOM FROM ROADS_TRAFF");
            st.execute("CREATE TABLE SOURCES_EMISSION(PERIOD VARCHAR, IDSOURCE INT, TV REAL, HV REAL, LV_SPD REAL, HV_SPD REAL, PVMT VARCHAR)");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'D', PK, TV_D, HV_D, LV_SPD_D, HV_SPD_D, PVMT FROM ROADS_TRAFF");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'E', PK, TV_E, HV_E, LV_SPD_E, HV_SPD_E, PVMT FROM ROADS_TRAFF");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'N', PK, TV_N, HV_N, LV_SPD_N, HV_SPD_N, PVMT FROM ROADS_TRAFF");

            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("buildings.shp").getFile()));

            int srid = org.h2gis.utilities.GeometryTableUtilities.getSRID(connection, "BUILDINGS");
            IsoSurface isoSurface = new IsoSurface(IsoSurface.NF31_133_ISO, srid);
            // Generate delaunay triangulation
            DelaunayReceiversMaker delaunayReceiversMaker = new DelaunayReceiversMaker("BUILDINGS", "ROADS_TRAFF");
            delaunayReceiversMaker.setMaximumArea(800);
            delaunayReceiversMaker.setGridDim(1);
            delaunayReceiversMaker.run(connection, "RECEIVERS", isoSurface.getTriangleTable());

            // Create noise map for 4 periods
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "SOURCES_GEOM", "RECEIVERS");

            noiseMapByReceiverMaker.setMaximumPropagationDistance(100);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportReceiverPosition = true;
            noiseMapByReceiverMaker.setGridDim(1);
            noiseMapByReceiverMaker.setSourcesEmissionTableName("SOURCES_EMISSION");
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setMaximumError(3);

            noiseMapByReceiverMaker.run(connection, new RootProgressVisitor(1, true, 5));

            int receiversRowCount = JDBCUtilities.getRowCount(connection, "RECEIVERS");

            int resultRowCount = JDBCUtilities.getRowCount(connection,
                    noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().receiversLevelTable);

            // D E N, should be 3 more rows than receivers
            assertEquals(receiversRowCount * 3, resultRowCount);
        }
    }


    @Test
    public void testEmissionLwTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'LW_ROADS')", NoiseMapByReceiverMakerTest.class.getResource("lw_roads.shp").getFile()));
            st.execute("CREATE TABLE SOURCES_GEOM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY) AS SELECT PK, THE_GEOM FROM LW_ROADS");
            st.execute("CREATE TABLE SOURCES_EMISSION(PERIOD VARCHAR, IDSOURCE INT, HZ63 REAL, LW125 REAL, LW250 REAL, LW500 REAL, LW1000 REAL, LW2000 REAL, LW4000 REAL, LW8000 REAL)");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'D', PK, LWD63, LWD125, LWD250, LWD500, LWD1000, LWD2000, LWD4000, LWD8000 FROM LW_ROADS");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'E', PK, LWE63, LWE125, LWE250, LWE500, LWE1000, LWE2000, LWE4000, LWE8000 FROM LW_ROADS");
            st.execute("INSERT INTO SOURCES_EMISSION SELECT 'N', PK, LWN63, LWN125, LWN250, LWN500, LWN1000, LWN2000, LWN4000, LWN8000 FROM LW_ROADS");

            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("buildings.shp").getFile()));

            int srid = org.h2gis.utilities.GeometryTableUtilities.getSRID(connection, "BUILDINGS");
            IsoSurface isoSurface = new IsoSurface(IsoSurface.NF31_133_ISO, srid);
            // Generate delaunay triangulation
            DelaunayReceiversMaker delaunayReceiversMaker = new DelaunayReceiversMaker("BUILDINGS", "SOURCES_GEOM");
            delaunayReceiversMaker.setMaximumArea(800);
            delaunayReceiversMaker.setGridDim(1);
            delaunayReceiversMaker.run(connection, "RECEIVERS", isoSurface.getTriangleTable());

            // Create noise map for 4 periods
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "SOURCES_GEOM", "RECEIVERS");

            noiseMapByReceiverMaker.setFrequencyFieldPrepend("LW");
            noiseMapByReceiverMaker.setMaximumPropagationDistance(100);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportReceiverPosition = true;
            noiseMapByReceiverMaker.setGridDim(1);
            noiseMapByReceiverMaker.setSourcesEmissionTableName("SOURCES_EMISSION");
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setMaximumError(3);

            noiseMapByReceiverMaker.run(connection, new RootProgressVisitor(1, true, 5));

            int receiversRowCount = JDBCUtilities.getRowCount(connection, "RECEIVERS");

            int resultRowCount = JDBCUtilities.getRowCount(connection,
                    noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().receiversLevelTable);

            // D E N, should be 3 more rows than receivers
            assertEquals(receiversRowCount * 3, resultRowCount);
        }
    }
}