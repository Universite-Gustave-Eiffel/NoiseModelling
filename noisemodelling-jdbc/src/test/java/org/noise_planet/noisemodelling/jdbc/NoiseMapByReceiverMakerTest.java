/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.NoiseMapWriter;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWGeom;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.jdbc.utils.IsoSurface;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CoordinateMixin;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.GroundAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosAttenuationOutput;
import org.noise_planet.noisemodelling.propagation.cnossos.PointPath;

import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class NoiseMapByReceiverMakerTest {

    private Connection connection;

    private static class DemTableLoader extends DefaultTableLoader {
        public void fetchDem(Connection connection, org.locationtech.jts.geom.Envelope envelope) throws SQLException {
            fetchCellDem(connection, envelope, new org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder());
        }
    }

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

    private DemTableLoader initDemTableLoader(String demGeometryType, String demWkt) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(ID INTEGER PRIMARY KEY, THE_GEOM GEOMETRY(POLYGONZ))");
            st.execute("INSERT INTO BUILDINGS VALUES(1, 'POLYGONZ ((0 0 0, 10 0 0, 10 10 0, 0 10 0, 0 0 0))')");
            st.execute("CREATE TABLE RECEIVERS(ID INTEGER PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ))");
            st.execute("INSERT INTO RECEIVERS VALUES(1, 'POINTZ (5 5 1)')");
            st.execute("CREATE TABLE DEM(ID INTEGER PRIMARY KEY, THE_GEOM GEOMETRY(" + demGeometryType + "))");
            st.execute("INSERT INTO DEM VALUES(1, '" + demWkt + "')");
        }
        DemTableLoader tableLoader = new DemTableLoader();
        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS", "", "RECEIVERS");
        noiseMapByReceiverMaker.setPropagationProcessDataFactory(tableLoader);
        noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        noiseMapByReceiverMaker.setDemTable("DEM");
        noiseMapByReceiverMaker.initialize(connection);
        return tableLoader;
    }

    private void assertDemWithoutZThrows(String demGeometryType, String demWkt) throws SQLException {
        DemTableLoader tableLoader = initDemTableLoader(demGeometryType, demWkt);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tableLoader.fetchDem(connection, new org.locationtech.jts.geom.Envelope(-1, 20, -1, 20)));
        assertTrue(exception.getMessage().contains("DEM"), exception.getMessage());
        assertTrue(exception.getMessage().contains("without Z ordinate"), exception.getMessage());
    }

    @Test
    public void testDemPointWithoutZThrows() throws Exception {
        assertDemWithoutZThrows("POINT", "POINT (1 1)");
    }

    @Test
    public void testDemLineStringWithoutZThrows() throws Exception {
        assertDemWithoutZThrows("LINESTRING", "LINESTRING (1 1, 2 2)");
    }

    @Test
    public void testDemPointWithZSucceeds() throws Exception {
        DemTableLoader tableLoader = initDemTableLoader("POINTZ", "POINTZ (1 1 12)");
        assertDoesNotThrow(() -> tableLoader.fetchDem(connection, new org.locationtech.jts.geom.Envelope(-1, 20, -1, 20)));
    }

    /**
     * Check if the altitude of the roofs of buildings are well constructed from the DEM when height is provided
     * but the buildings polygons are in 2D.
     */
    @Test
    public void testBuildingsAltitudeFromDemAndHeight() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/BUILD_GRID2.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'LW_ROADS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/SourceSi.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'DEM')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/DEM_Fence.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'RECEIVERS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/RCVS20.shp").getFile()));
            st.execute("ALTER TABLE BUILDINGS ALTER COLUMN THE_GEOM GEOMETRY;");
            st.execute("UPDATE BUILDINGS SET THE_GEOM = ST_SetSRID(ST_Force2D(THE_GEOM), 2154);");
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
            noiseMapByReceiverMaker.setDemTable("DEM");
            noiseMapByReceiverMaker.setGridDim(1);
            noiseMapByReceiverMaker.initialize(connection);

            NoiseMapByReceiverMaker.TableLoader tableLoader = noiseMapByReceiverMaker.getPropagationProcessDataFactory();
            SceneWithEmission sceneWithEmission = tableLoader.create(connection, new CellIndex(0, 0), new HashSet<>());

            assertFalse(sceneWithEmission.profileBuilder.getBuildings().isEmpty());
            for (Building building : sceneWithEmission.profileBuilder.getBuildings()) {
                // Check altitude of the building
                assertTrue(building.getAverageZ() > 100);
            }
        }
    }

    /**
     * Check if the altitude of the roofs of buildings are well read from the geometry, ignoring the HEIGHT field of the buildings table
     */
    @Test
    public void testBuildingsAltitudeFromBuildingsGeometry() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/BUILD_GRID2.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'LW_ROADS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/SourceSi.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'DEM')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/DEM_Fence.shp").getFile()));
            st.execute(String.format("CALL SHPREAD('%s', 'RECEIVERS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/RCVS20.shp").getFile()));
            st.execute("DELETE FROM BUILDINGS WHERE ST_ZMIN(THE_GEOM) < 0");
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS", "LW_ROADS", "RECEIVERS");
            noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
            noiseMapByReceiverMaker.setDemTable("DEM");
            noiseMapByReceiverMaker.setGridDim(1);
            noiseMapByReceiverMaker.initialize(connection);

            NoiseMapByReceiverMaker.TableLoader tableLoader = noiseMapByReceiverMaker.getPropagationProcessDataFactory();
            SceneWithEmission sceneWithEmission = tableLoader.create(connection, new CellIndex(0, 0), new HashSet<>());

            assertFalse(sceneWithEmission.profileBuilder.getBuildings().isEmpty());
            for (Building building : sceneWithEmission.profileBuilder.getBuildings()) {
                // Check altitude of the building
                assertTrue(building.getAverageZ() > 100);
            }
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
            noiseMapByReceiverMaker.initialize(connection);

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
                assertEquals("D", scene.wjSources.get(1L).getFirst().period);
            }
        }
    }

    private static String createSource(Geometry source, double lvl, Orientation sourceOrientation, int directivityId) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ROADS_GEOM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY, YAW REAL, PITCH REAL, ROLL REAL, DIR_ID INT");
        StringBuilder values = new StringBuilder("(row_number() over())::int, ST_SETSRID('");
        values.append(new WKTWriter(3).write(source));
        values.append("', ").append(source.getSRID()).append(") THE_GEOM, ");
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

    /**
     * Deserialize CnossosAttenuationOutput object.
     *
     * @param json The serialized CnossosAttenuationOutput
     * @return Deserialized CnossosAttenuationOutput object
     * @throws JsonProcessingException if the deserialization fails
     */
    public static CnossosAttenuationOutput jsonToCnossosAttenuationOutput(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Coordinate.class, CoordinateMixin.class);
        mapper.registerModule(new JtsModule());
        return mapper.readValue(json, CnossosAttenuationOutput.class);

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
    public void testRecordProfile() throws Exception {
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
            noiseMapByReceiverMaker.getSceneInputSettings().setUseTrainDirectivity(true);

            File profileFile = File.createTempFile("profile", ".csv");
            profileFile.deleteOnExit();
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().CSVProfilerOutputPath = profileFile;

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            List<String> lines = Files.readAllLines(profileFile.toPath());
            assertTrue(lines.size() >= 2, "The profiler should have written at least one data row");
            List<String> headers = Arrays.asList(lines.getFirst().split(","));
            Set<String> expectedColumns = new HashSet<>(Arrays.asList("time","jdbc_stack","average_cut_source_distance","cut_profile_count",
                    "jvm_used_heap_mb","jvm_max_heap_mb","receiver_min_milliseconds","receiver_median_milliseconds",
                    "receiver_mean_milliseconds","receiver_max_milliseconds",
                    "receiver_collect_sources_max_milliseconds","receiver_precompute_reflection_max_milliseconds",
                    "receiver_processed_sources_percentage_mean","receiver_median_point_sources_in_range",
                    "progression"));
            // Check if expected columns are in the header
            for(String columnHeader : expectedColumns) {
                assertTrue(headers.contains(columnHeader), "Column not found: " + columnHeader);
            }
            int cutProfileCount = Integer.parseInt(lines.getLast().split(",")[headers.indexOf("cut_profile_count")]);
            assertEquals(2, cutProfileCount, "Two receivers, one point source, should have 2 cut profiles");
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
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationOutput = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = true;
            noiseMapByReceiverMaker.setThreadCount(1);

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

            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH, IDSOURCE FROM " + parameters.raysTable + " WHERE PERIOD='D' AND METEO='homogeneous' ORDER BY IDRECEIVER, IDSOURCE")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                AttenuationOutput attenuationOutput = NoiseMapWriter.jsonToAttenuationOutput(rs.getString(2));
                // This is source orientation, not relevant to receiver position
                assertEquals(1, rs.getInt("IDSOURCE"));
                assertOrientationEquals(new Orientation(45, 0.81, 0), attenuationOutput.cutProfile.getSourceOrientation(), 0.01);
                assertOrientationEquals(new Orientation(330.2084079818916,-5.947213381005439,0.0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity(), 0.01);
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
                assertEquals(1, rs.getInt("IDSOURCE"));
                attenuationOutput = NoiseMapWriter.jsonToAttenuationOutput(rs.getString(2));
                assertOrientationEquals(new Orientation(45, 0.81, 0), attenuationOutput.cutProfile.getSourceOrientation(), 0.01);
                assertOrientationEquals(new Orientation(336.9922375343167,-4.684918495003125,0.0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity(), 0.01);
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
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationOutput = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = true;

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

            List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH FROM " + parameters.raysTable + " WHERE PERIOD='D' AND METEO='homogeneous' ORDER BY IDRECEIVER")) {
                while (rs.next()) {
                    CnossosAttenuationOutput attenuationOutput = jsonToCnossosAttenuationOutput(rs.getString("PATH"));
                    attenuationOutputs.add(attenuationOutput);
                }
            }
            assertEquals(4 , attenuationOutputs.size());
            AttenuationOutput attenuationOutput = attenuationOutputs.removeFirst();
            assertEquals(1, attenuationOutput.getCutProfile().getReceiver().receiverPk);
            // receiver is front of source
            assertEquals(new Orientation(0, 0, 0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity());
            attenuationOutput = attenuationOutputs.removeFirst();
            assertEquals(2, attenuationOutput.getCutProfile().getReceiver().receiverPk);
            // receiver is behind of the source
            assertEquals(new Orientation(180, 0, 0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity());
            attenuationOutput = attenuationOutputs.removeFirst();
            assertEquals(3, attenuationOutput.getCutProfile().getReceiver().receiverPk);
            // receiver is on the right of the source
            assertEquals(new Orientation(90, 0, 0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity());
            attenuationOutput = attenuationOutputs.removeFirst();
            assertEquals(4, attenuationOutput.getCutProfile().getReceiver().receiverPk);
            // receiver is on the left of the source
            assertEquals(new Orientation(360-90, 0, 0), attenuationOutput.getCutProfile().getRaySourceReceiverDirectivity());

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
            delaunayReceiversMaker.setMaximumArea(0);
            delaunayReceiversMaker.setGridDim(1);
            delaunayReceiversMaker.run(connection, "RECEIVERS", isoSurface.getTriangleTable(), new EmptyProgressVisitor());

            // Create noise map for 4 periods
            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "SOURCES_GEOM", "RECEIVERS");

            noiseMapByReceiverMaker.setMaximumPropagationDistance(100);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(false);
            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportReceiverPosition = true;
            noiseMapByReceiverMaker.setGridDim(1);
            noiseMapByReceiverMaker.setSourcesEmissionTableName("SOURCES_EMISSION");
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().setMaximumError(3);

            noiseMapByReceiverMaker.run(connection, new RootProgressVisitor(1, true, 5));

            int receiversRowCount = JDBCUtilities.getRowCount(connection, "RECEIVERS");

            int resultRowCount = JDBCUtilities.getRowCount(connection,
                    noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().receiversLevelTable);

            // D E N and DEN, should be 4 more rows than receivers
            assertEquals(receiversRowCount * 4, resultRowCount);
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
            delaunayReceiversMaker.setMaximumArea(0);
            delaunayReceiversMaker.setGridDim(1);
            delaunayReceiversMaker.run(connection, "RECEIVERS", isoSurface.getTriangleTable(), new EmptyProgressVisitor());

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

            // D E N and DEN, should be 4 more rows than receivers
            assertEquals(receiversRowCount * 4, resultRowCount);
        }
    }




    @Test
    public void testPointDem() throws Exception {
        try (Statement st = connection.createStatement()) {
            // Import shape file
            // org/noise_planet/noisemodelling/jdbc/PointSource/DEM_Fence.shp
            st.execute(String.format("CALL SHPREAD('%s', 'DEM')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/DEM_Fence.shp").getFile()));
            // Import buildings
            // org/noise_planet/noisemodelling/jdbc/PointSource/BUILD_GRID2.shp
            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/BUILD_GRID2.shp").getFile()));

            // create source point direction east->90°
            st.execute(createSource(new GeometryFactory(new PrecisionModel(), 2154).createPoint(new Coordinate(759520.99,6299434.84,1.0 )),
                    91, new Orientation(90,0,0),0));

            //SRID=2154;Point Z (759155.20419493981171399 6299238.93822849541902542 0)
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ))");
            st.execute("insert into receivers(the_geom) values ('SRID=2154; POINTZ (759155.204 6299238.93 1.6)')");

            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "ROADS_GEOM", "RECEIVERS");

            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);
            noiseMapByReceiverMaker.setSourcesZIsAltitude(false);
            noiseMapByReceiverMaker.setReceiversZIsAltitude(false);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setMaximumPropagationDistance(1000);
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().raysTable = "RAYS";
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationOutput = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = false;
            noiseMapByReceiverMaker.setDemTable("DEM");

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

            List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH FROM " + parameters.raysTable + " WHERE METEO='homogeneous' AND PERIOD='D' ORDER BY IDRECEIVER")) {
                while (rs.next()) {
                    CnossosAttenuationOutput attenuationOutput = jsonToCnossosAttenuationOutput(rs.getString("PATH"));
                    attenuationOutputs.add(attenuationOutput);
                }
            }
            assertEquals(1 , attenuationOutputs.size());
            // Check source coordinates
            AttenuationOutput attenuationOutput = attenuationOutputs.getFirst();
            assertEquals(200.53, attenuationOutput.getCutProfile().getSource().coordinate.z, 0.1);
            // Check receiver coordinates
            assertEquals(189.30, attenuationOutput.getCutProfile().getReceiver().coordinate.z, 0.1);
            // Check CNOSSOS path points
            // One diffraction on horizontal edge of building
            assertInstanceOf(CnossosAttenuationOutput.class, attenuationOutput);
            CnossosAttenuationOutput cnossosAttenuationOutput = (CnossosAttenuationOutput) attenuationOutput;
            assertEquals(3, cnossosAttenuationOutput.propagationPath.getPointList().size());
            assertEquals(200.53, cnossosAttenuationOutput.propagationPath.getPointList().getFirst().coordinate.y, 0.1);
            assertEquals(189.30, cnossosAttenuationOutput.propagationPath.getPointList().getLast().coordinate.y, 0.1);
        }
    }


    /**
     * Place a point source into a building to check if the source is still computed but a warning will be logged
     * @throws Exception
     */
    @Test
    public void testSourceInBuilding() throws Exception {
        try (Statement st = connection.createStatement()) {
            // Import shape file
            // org/noise_planet/noisemodelling/jdbc/PointSource/DEM_Fence.shp
            st.execute(String.format("CALL SHPREAD('%s', 'DEM')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/DEM_Fence.shp").getFile()));
            // Import buildings
            // org/noise_planet/noisemodelling/jdbc/PointSource/BUILD_GRID2.shp
            st.execute(String.format("CALL SHPREAD('%s', 'BUILDINGS')", NoiseMapByReceiverMakerTest.class.getResource("PointSource/BUILD_GRID2.shp").getFile()));

            // create source point direction east->90°
            st.execute(createSource(new GeometryFactory(new PrecisionModel(), 2154).createPoint(new Coordinate(759502.135,6299460.753,1.0 )),
                    91, new Orientation(90,0,0),0));

            //SRID=2154;Point Z (759155.20419493981171399 6299238.93822849541902542 0)
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ))");
            st.execute("insert into receivers(the_geom) values ('SRID=2154; POINTZ (759155.204 6299238.93 1.6)')");

            NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BUILDINGS",
                    "ROADS_GEOM", "RECEIVERS");

            noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
            noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);
            noiseMapByReceiverMaker.setSourcesZIsAltitude(false);
            noiseMapByReceiverMaker.setReceiversZIsAltitude(false);
            noiseMapByReceiverMaker.setSoundReflectionOrder(0);
            noiseMapByReceiverMaker.setMaximumPropagationDistance(1000);
            noiseMapByReceiverMaker.setHeightField("HEIGHT");
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportRaysMethod = NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().raysTable = "RAYS";
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationOutput = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportAttenuationMatrix = true;
            noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().mergeSources = false;
            noiseMapByReceiverMaker.setDemTable("DEM");

            noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

            NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

            List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
            try(ResultSet rs = st.executeQuery("SELECT IDRECEIVER, PATH FROM " + parameters.raysTable + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
                while (rs.next()) {
                    CnossosAttenuationOutput attenuationOutput = jsonToCnossosAttenuationOutput(rs.getString("PATH"));
                    attenuationOutputs.add(attenuationOutput);
                }
            }
            // Diffraction over the walls of the building, but no direct path
            assertEquals(2 , attenuationOutputs.size());
            // Homogenous path with diffraction over the building wall
            assertInstanceOf(CnossosAttenuationOutput.class, attenuationOutputs.getFirst());
            CnossosAttenuationOutput cnossosAttenuationOutput0 = (CnossosAttenuationOutput) attenuationOutputs.getFirst();
            assertEquals(PointPath.POINT_TYPE.DIFH, cnossosAttenuationOutput0.propagationPath.getPointList().get(1).type);
            // Favorable path with diffraction over the building wall
            assertInstanceOf(CnossosAttenuationOutput.class, attenuationOutputs.get(1));
            CnossosAttenuationOutput cnossosAttenuationOutput1 = (CnossosAttenuationOutput) attenuationOutputs.get(1);
            assertEquals(PointPath.POINT_TYPE.DIFH, cnossosAttenuationOutput1.propagationPath.getPointList().get(1).type);
        }
    }

    /**
     * Test that bodyBarrier (train/screen multi-reflection) produces different results
     * compared to standard computation without body barrier.
     * Uses a simple programmatic geometry: source line at x=0.5, screen at x=3, receiver at x=25.
     */
    @Test
    public void testBodyBarrierRailJDBC() throws SQLException, IOException {
        // Create a simple LW source table with one source line at x=0.5
        // Columns: PK, THE_GEOM, GS, HRAIL, + frequency columns for D/E/N periods
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE LW_RAILWAY(PK INT PRIMARY KEY, THE_GEOM GEOMETRY, DIR_ID INT, GS DOUBLE, HRAIL DOUBLE, CREF DOUBLE");
        int[] frequencies = {50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
                800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000};
        for (String period : new String[]{"D", "E", "N"}) {
            for (int freq : frequencies) {
                createSql.append(", HZ").append(period).append(freq).append(" DOUBLE");
            }
        }
        createSql.append(")");
        connection.createStatement().execute(createSql.toString());

        // Insert a source line at x=0.5, z=0.68 (ROLLING: 0.5m above rail at 0.18m), from y=-10 to y=10
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO LW_RAILWAY VALUES(1, ");
        insertSql.append("ST_SetSRID(ST_GeomFromText('LINESTRING Z(0.5 -10 0.68, 0.5 10 0.68)'), 2154)");
        insertSql.append(", 1, 0.0, 0.18, 0.0");
        // Set 90 dB for all frequencies and periods
        for (int i = 0; i < 3 * frequencies.length; i++) {
            insertSql.append(", 90.0");
        }
        insertSql.append(")");
        connection.createStatement().execute(insertSql.toString());

        // Create a screen wall at x=3, height 2.5, g=0 (reflective)
        connection.createStatement().execute(
                "CREATE TABLE SCREENS(PK INT PRIMARY KEY, THE_GEOM GEOMETRY, HEIGHT DOUBLE, G DOUBLE)");
        connection.createStatement().execute(
                "INSERT INTO SCREENS VALUES(1, " +
                        "ST_SetSRID(ST_Buffer(ST_GeomFromText('LINESTRING(3 -100, 3 100)'), 0.1, 'join=mitre endcap=flat'), 2154)" +
                        ", 2.5, 0.0)");

        // Create a receiver at x=25, z=4
        connection.createStatement().execute(
                "CREATE TABLE RECEPTEURS(PK INT PRIMARY KEY, THE_GEOM GEOMETRY)");
        connection.createStatement().execute(
                "INSERT INTO RECEPTEURS VALUES(1, " +
                        "ST_SetSRID(ST_GeomFromText('POINT Z(25 0 4)'), 2154))");

        // --- Case A: CREF = 0 (no body barrier) ---
        NoiseMapByReceiverMaker noiseMapNoBody = new NoiseMapByReceiverMaker("SCREENS", "LW_RAILWAY", "RECEPTEURS");
        noiseMapNoBody.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapNoBody.run(connection, new EmptyProgressVisitor());

        DefaultTableLoader loaderNoBody = (DefaultTableLoader) noiseMapNoBody.getTableLoader();
        List<String> frequenciesFields = loaderNoBody.frequencyArray.stream()
                .map(frequency -> noiseMapNoBody.getFrequencyFieldPrepend() + frequency)
                .collect(Collectors.toList());

        double[] levelsNoBody;
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "
                + noiseMapNoBody.getNoiseMapDatabaseParameters().receiversLevelTable
                + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
            assertTrue(rs.next());
            levelsNoBody = frequenciesFields.stream().mapToDouble(field -> {
                try { return rs.getDouble(field); } catch (SQLException e) { throw new RuntimeException(e); }
            }).toArray();
        }

        connection.createStatement().execute("DROP TABLE IF EXISTS RECEIVERS_LEVEL");

        // --- Case B: CREF = 1 (body barrier active) ---
        connection.createStatement().execute("UPDATE LW_RAILWAY SET CREF = 1.0");
        NoiseMapByReceiverMaker noiseMapWithBody = new NoiseMapByReceiverMaker("SCREENS", "LW_RAILWAY", "RECEPTEURS");
        noiseMapWithBody.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapWithBody.run(connection, new EmptyProgressVisitor());

        double[] levelsWithBody;
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "
                + noiseMapWithBody.getNoiseMapDatabaseParameters().receiversLevelTable
                + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
            assertTrue(rs.next());
            levelsWithBody = frequenciesFields.stream().mapToDouble(field -> {
                try { return rs.getDouble(field); } catch (SQLException e) { throw new RuntimeException(e); }
            }).toArray();
        }

        // Body barrier (multi-reflection) should produce different levels
        boolean different = false;
        for (int i = 0; i < levelsNoBody.length; i++) {
            if (Math.abs(levelsNoBody[i] - levelsWithBody[i]) > 0.01) {
                different = true;
                break;
            }
        }
        assertTrue(different, "Body barrier ON vs OFF should produce different receiver levels");
    }

    /**
     * Test that using a different platform (testPlatform with h2=0) on a rail section
     * produces different propagation levels compared to DEFAULT platform (h2=0.18),
     * because source heights change with hRail.
     */
    @Test
    public void testPlatformChangeAffectsPropagation() throws SQLException, IOException {
        // Import rail section + traffic
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        // Add PLATFORM column to rail section (initially DEFAULT)
        connection.createStatement().execute("ALTER TABLE Rail_Section2 ADD COLUMN PLATFORM VARCHAR(50)");
        connection.createStatement().execute("UPDATE Rail_Section2 SET PLATFORM = 'DEFAULT'");

        // Emission with DEFAULT platform (hRail=0.18)
        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");

        // Setup buildings (empty) and receiver
        connection.createStatement().execute("CREATE TABLE BUILDINGS(PK INT PRIMARY KEY, THE_GEOM GEOMETRY, HEIGHT DOUBLE)");
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Recepteurs.shp").getFile());
        connection.createStatement().execute("SELECT UpdateGeometrySRID('RECEPTEURS', 'THE_GEOM', 2154)");
        connection.createStatement().execute("UPDATE RECEPTEURS SET THE_GEOM = ST_UPDATEZ(THE_GEOM, 4.0)");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154)");

        // Propagation with DEFAULT platform
        NoiseMapByReceiverMaker noiseMapDefault = new NoiseMapByReceiverMaker("BUILDINGS",
                "LW_RAILWAY", "RECEPTEURS");
        noiseMapDefault.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapDefault.run(connection, new EmptyProgressVisitor());

        DefaultTableLoader loaderDefault = (DefaultTableLoader) noiseMapDefault.getTableLoader();
        List<String> freqFields = loaderDefault.frequencyArray.stream()
                .map(f -> noiseMapDefault.getFrequencyFieldPrepend() + f)
                .collect(Collectors.toList());

        double[] levelsDefault;
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "
                + noiseMapDefault.getNoiseMapDatabaseParameters().receiversLevelTable
                + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
            assertTrue(rs.next());
            levelsDefault = freqFields.stream().mapToDouble(f -> {
                try { return rs.getDouble(f); } catch (SQLException e) { throw new RuntimeException(e); }
            }).toArray();
        }

        // Cleanup output + emission tables
        connection.createStatement().execute("DROP TABLE IF EXISTS RECEIVERS_LEVEL");
        connection.createStatement().execute("DROP TABLE IF EXISTS LW_RAILWAY");

        // Switch to testPlatform (h2=0, so hRail=0 => different source heights)
        connection.createStatement().execute("UPDATE Rail_Section2 SET PLATFORM = 'testPlatform'");

        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154)");

        // Propagation with testPlatform
        NoiseMapByReceiverMaker noiseMapTest = new NoiseMapByReceiverMaker("BUILDINGS",
                "LW_RAILWAY", "RECEPTEURS");
        noiseMapTest.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapTest.run(connection, new EmptyProgressVisitor());

        double[] levelsTest;
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM "
                + noiseMapTest.getNoiseMapDatabaseParameters().receiversLevelTable
                + " WHERE PERIOD='D' ORDER BY IDRECEIVER")) {
            assertTrue(rs.next());
            levelsTest = freqFields.stream().mapToDouble(f -> {
                try { return rs.getDouble(f); } catch (SQLException e) { throw new RuntimeException(e); }
            }).toArray();
        }

        // Different platform => different source heights => different levels
        boolean different = false;
        for (int i = 0; i < levelsDefault.length; i++) {
            if (Math.abs(levelsDefault[i] - levelsTest[i]) > 0.01) {
                different = true;
                break;
            }
        }
        assertTrue(different, "Changing platform (DEFAULT vs testPlatform) should produce different receiver levels");
    }

    @Test
    public void testNoiseEmissionRailWayForPropa() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");

        // Get Class to compute LW
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"Rail_Section2", "Rail_Traffic");
        RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        List<LineString> geometries = v.getRailWayLWGeometry();
        assertEquals(geometries.size(),2);

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Recepteurs.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Buildings.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_protect.shp").getFile());

        // ICI POUR CHANGER HAUTEUR ET G ECRAN
        connection.createStatement().execute("CREATE TABLE SCREENS AS SELECT the_geom , pk as pk, 12.0 as height, 0.2 as g FROM Rail_protect");

        // ICI HAUTEUR RECPTEUR
        connection.createStatement().execute("SELECT UpdateGeometrySRID('RECEPTEURS', 'THE_GEOM', 2154);");
        connection.createStatement().execute("SELECT UpdateGeometrySRID('LW_RAILWAY', 'THE_GEOM', 2154);");

        connection.createStatement().execute("UPDATE RECEPTEURS SET THE_GEOM = ST_UPDATEZ(THE_GEOM,4.0);");

        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("SCREENS", "LW_RAILWAY",
                "RECEPTEURS");

        NoiseMapDatabaseParameters parameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();

        noiseMapByReceiverMaker.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_LW_DEN);
        noiseMapByReceiverMaker.setMaximumPropagationDistance(150);
        noiseMapByReceiverMaker.setGridDim(1);
        noiseMapByReceiverMaker.setThreadCount(1);

        // Use train directivity functions instead of discrete directivity
        DefaultTableLoader defaultTableLoader = ((DefaultTableLoader) noiseMapByReceiverMaker.getPropagationProcessDataFactory());
        defaultTableLoader.insertTrainDirectivity();

        parameters.setRaysTable("RAYS");
        parameters.setExportRaysMethod(NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE);
        parameters.exportAttenuationMatrix = true;
        parameters.exportAttenuationOutput = true;
        parameters.keepAbsorption = true;

        noiseMapByReceiverMaker.run(connection, new EmptyProgressVisitor());

        try(Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT IDRECEIVER, IDSOURCE, PATH, METEO FROM RAYS ORDER BY IDRECEIVER, IDSOURCE, METEO")) {
            int numberOfPropagationLinesWithDeltaBodyScreen = 0;
            while (resultSet.next()) {
                CnossosAttenuationOutput attenuationOutput = jsonToCnossosAttenuationOutput(resultSet.getString("PATH"));
                if(Arrays.stream(attenuationOutput.deltaBodyScreen).anyMatch(x -> x > 0)) {
                    numberOfPropagationLinesWithDeltaBodyScreen++;
                }
            }
            assertNotEquals(0, numberOfPropagationLinesWithDeltaBodyScreen, "No propagation lines found with a gain from Train Body/Wall");
        }
    }
}
