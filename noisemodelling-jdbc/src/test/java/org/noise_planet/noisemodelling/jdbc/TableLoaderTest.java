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
import org.h2gis.functions.io.dbf.DBFRead;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.railway.RailWayParameters;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWGeom;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator;
import org.noise_planet.noisemodelling.jdbc.railway.RailwayPlatform;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Building;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;

public class TableLoaderTest {

    private Connection connection;
    public static double[] aWeightingArray = Arrays.stream(
            asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE)).
            mapToDouble(value -> value).toArray();

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(TableLoaderTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    /**
     * A road that follows a DEM triangle edge must keep its geometry. Before the fix the DEM
     * profile zigzagged, increasing the line length and the number of source points (issue #1093).
     */
    @Test
    public void testLineSourceAlongDemTriangleEdge() throws Exception {
        int x0 = 650000;
        int y0 = 6860000;
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), HEIGHT DOUBLE)");
            st.execute("CREATE TABLE ROADS_1093(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154))");
            st.execute("INSERT INTO ROADS_1093 VALUES (1, ST_SetSRID('LINESTRING Z(" +
                    (x0 - 150) + " " + y0 + " 0.05, " + (x0 + 150) + " " + y0 + " 0.05)'::GEOMETRY, 2154))");
            st.execute("CREATE TABLE RECEIVERS_1093(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ, 2154))");
            st.execute("INSERT INTO RECEIVERS_1093 VALUES (1, ST_SetSRID(ST_MakePoint(" + x0 + ", " + (y0 + 20) + ", 4), 2154))");
            st.execute("CREATE TABLE DEM_1093(PK INT AUTO_INCREMENT PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ, 2154))");
            st.execute("INSERT INTO DEM_1093(THE_GEOM) SELECT ST_SetSRID(ST_MakePoint(" + x0 + " + a.X * 20, " +
                    y0 + " + b.X * 20, 0), 2154) FROM SYSTEM_RANGE(-10, 10) a, SYSTEM_RANGE(-10, 10) b");
        }
        NoiseMapByReceiverMaker noiseMap = new NoiseMapByReceiverMaker("BUILDINGS", "ROADS_1093", "RECEIVERS_1093");
        noiseMap.geometryFactory = new GeometryFactory();
        noiseMap.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        noiseMap.setDemTable("DEM_1093");
        DefaultTableLoader loader = (DefaultTableLoader) noiseMap.getTableLoader();
        loader.initialize(connection, noiseMap);

        ProfileBuilder profile = new ProfileBuilder();
        Envelope envelope = new Envelope(x0 - 200, x0 + 200, y0 - 200, y0 + 200);
        loader.fetchCellDem(connection, envelope, profile);
        profile.finishFeeding();
        SceneWithEmission scene = new SceneWithEmission(profile, noiseMap.getSceneInputSettings());
        loader.fetchCellSource(connection, envelope, scene, false);

        assertEquals(1, scene.sourceGeometries.size());
        Geometry source = scene.sourceGeometries.get(0);
        assertInstanceOf(LineString.class, source);
        assertEquals(300, source.getLength(), 1e-9);
        assertEquals(2, source.getNumPoints());
    }

    @Test
    public void testClippedLineSources() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE SOURCES(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)");
            st.execute("INSERT INTO SOURCES VALUES "
                    + "(1, 'LINESTRING Z (5 5 0.05, 12 8 0.05, 10 10 0.05)'),"
                    + "(2, 'MULTILINESTRING Z ((1 1 0.05, 2 2 0.05), (5 5 0.05, 12 8 0.05, 10 10 0.05))'),"
                    + "(3, 'LINESTRING Z (12 8 0.05, 10 10 0.05)'),"
                    + "(4, 'POINT Z (10 10 0.05)')");
        }
        NoiseMapByReceiverMaker noiseMap = new NoiseMapByReceiverMaker("", "SOURCES", "");
        noiseMap.geometryFactory = new GeometryFactory();
        noiseMap.setInputMode(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        DefaultTableLoader loader = (DefaultTableLoader) noiseMap.getTableLoader();
        loader.initialize(connection, noiseMap);

        for (boolean absoluteZ : new boolean[] {false, true}) {
            noiseMap.setSourcesZIsAltitude(absoluteZ);
            ProfileBuilder profile = new ProfileBuilder();
            profile.addTopographicPoint(new Coordinate(-100, -100, 100));
            profile.addTopographicPoint(new Coordinate(100, -100, 100));
            profile.addTopographicPoint(new Coordinate(0, 100, 100));
            profile.finishFeeding();
            assertTrue(profile.hasDem());
            SceneWithEmission scene = new SceneWithEmission(profile, noiseMap.getSceneInputSettings());
            loader.fetchCellSource(connection, new Envelope(0, 10, 0, 10), scene, true);

            assertEquals(3, scene.sourceGeometries.size());
            assertFalse(scene.sourcesPk.contains(3L), "A boundary touch must not become a point source");
            Geometry line = scene.sourceGeometries.get(scene.sourcesPk.indexOf(1L));
            assertInstanceOf(LineString.class, line);
            assertEquals(2, line.getNumPoints());
            assertEquals(5, line.getCoordinates()[0].x, 1e-9);
            assertEquals(10, line.getCoordinates()[1].x, 1e-9);
            assertEquals(50.0 / 7, line.getCoordinates()[1].y, 1e-9);
            Geometry multiLine = scene.sourceGeometries.get(scene.sourcesPk.indexOf(2L));
            assertInstanceOf(MultiLineString.class, multiLine);
            assertEquals(2, multiLine.getNumGeometries());
            assertInstanceOf(Point.class, scene.sourceGeometries.get(scene.sourcesPk.indexOf(4L)));
            for (Geometry source : scene.sourceGeometries) {
                for (Coordinate coordinate : source.getCoordinates()) {
                    assertEquals(absoluteZ ? 0.05 : 100.05, coordinate.getZ(), 1e-9);
                }
            }

            SceneWithEmission unclipped = new SceneWithEmission(profile, noiseMap.getSceneInputSettings());
            loader.fetchCellSource(connection, new Envelope(0, 10, 0, 10), unclipped, false);
            assertEquals(4, unclipped.sourceGeometries.size());
            Geometry road = unclipped.sourceGeometries.get(unclipped.sourcesPk.indexOf(1L));
            assertEquals(3, road.getNumPoints());
            assertEquals(12, road.getCoordinates()[1].x, 1e-9);
        }
    }

    @Test
    public void testNoiseEmissionRailWay() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());
        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN",RailWayLWIterator.RAILWAY_VEHICLES_CNOSSOS_JSON,RailWayLWIterator.RAILWAY_TRAINSETS_JSON, RailWayLWIterator.RAILWAY_EMISSION_CNOSSOS_JSON, RailWayLWIterator.RAILWAY_PLATFORMS_JSON);

        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWayTwoGeoms() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 2)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWaySingleGeom() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWaySingleGeomSingleTrain() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two track only
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK LIMIT 1)");
        connection.createStatement().execute("DELETE FROM RAILTRAIN WHERE PK NOT IN (SELECT R1.PK FROM RAILTRAIN R1, RAILTRACK R2 WHERE r1.IDSECTION = R2.IDSECTION LIMIT 1)");

        int expectedNumberOfRows;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM RAILTRACK")) {
            assertTrue(rs.next());
            expectedNumberOfRows = rs.getInt(1);
        }
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(expectedNumberOfRows, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWayLastSectionSingleTrain() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("RailTrain.dbf").getFile());

        // Test with two tracks, the last one with a single train
        connection.createStatement().execute("DELETE FROM RAILTRACK WHERE PK NOT IN (SELECT PK FROM RAILTRACK ORDER BY PK LIMIT 2)");
        connection.createStatement().execute("DELETE FROM RAILTRAIN WHERE IDSECTION = (SELECT IDSECTION FROM RAILTRACK ORDER BY PK DESC LIMIT 1)"
                + " AND PK <> (SELECT MIN(T.PK) FROM RAILTRAIN T, RAILTRACK R WHERE T.IDSECTION = R.IDSECTION"
                + " AND R.PK = (SELECT MAX(PK) FROM RAILTRACK))");

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        int numberOfRows = 0;
        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();
            assertNotNull(v);
            numberOfRows++;
        }
        assertEquals(2, numberOfRows);
    }

    @Test
    public void testNoiseEmissionRailWay_OC5() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrain.dbf").getFile());

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        v.setNbTrack(2);
        RailWayParameters railWayLW = v.getRailWayLW();
        List<LineString> geometries = v.getRailWayLWGeometry();

        v = railWayLWIterator.next();
        assertFalse(railWayLWIterator.hasNext());

    }

    /**
     * Test with SNCF-prefixed string track parameter keys (VARCHAR columns)
     * to validate that the merged RailwayEmissionCnossos.json works with properly prefixed keys
     * read directly from the database (e.g. "SNCF7", "SNCF1").
     */
    @Test
    public void testNoiseEmissionRailWay_OC5_SNCFPrefixed() throws SQLException, IOException {
        // Import geometry from existing OC test data
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrack.shp").getFile());

        // Re-create RAILTRACK with VARCHAR columns for TRANSFER, ROUGHNESS, IMPACT, BRIDGE
        // using SNCF-prefixed values matching the merged RailwayEmissionCnossos.json keys
        connection.createStatement().execute("ALTER TABLE RAILTRACK DROP COLUMN TRANSFER");
        connection.createStatement().execute("ALTER TABLE RAILTRACK DROP COLUMN ROUGHNESS");
        connection.createStatement().execute("ALTER TABLE RAILTRACK DROP COLUMN IMPACT");
        connection.createStatement().execute("ALTER TABLE RAILTRACK DROP COLUMN BRIDGE");
        connection.createStatement().execute("ALTER TABLE RAILTRACK ADD COLUMN TRANSFER VARCHAR(20)");
        connection.createStatement().execute("ALTER TABLE RAILTRACK ADD COLUMN ROUGHNESS VARCHAR(20)");
        connection.createStatement().execute("ALTER TABLE RAILTRACK ADD COLUMN IMPACT VARCHAR(20)");
        connection.createStatement().execute("ALTER TABLE RAILTRACK ADD COLUMN BRIDGE VARCHAR(20)");
        connection.createStatement().execute("UPDATE RAILTRACK SET TRANSFER='SNCF7', ROUGHNESS='SNCF1', IMPACT='', BRIDGE=''");

        // Import train traffic data from existing OC test
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/OC/RailTrain.dbf").getFile());

        // Use the merged RailwayEmissionCnossos.json with SNCF-prefixed keys
        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection, "RAILTRACK", "RAILTRAIN");
        RailWayLWGeom v = railWayLWIterator.next();
        assertNotNull(v);
        v.setNbTrack(2);
        RailWayParameters railWayLW = v.getRailWayLW();
        assertNotNull(railWayLW);
        assertFalse(railWayLW.getRailwaySourceList().isEmpty(), "Railway source list should not be empty with SNCF-prefixed keys");
        List<LineString> geometries = v.getRailWayLWGeometry();
        assertNotNull(geometries);

        v = railWayLWIterator.next();
        assertFalse(railWayLWIterator.hasNext());
    }

    @Test
    public void testNoiseEmissionRailWay_BM() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/BM/RailTrack.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/BM/RailTrain.dbf").getFile());

        HashMap<String, double[]> results = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAILTRACK", "RAILTRAIN");
        double resD,resE,resN;

        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();

            RailWayParameters railWayLW = v.getRailWayLWDay();
            double[] lW = new double[24];
            Arrays.fill(lW, -99.00);

            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }

            double[] LWA = sumArray(lW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            results.put(idSection,new double[]{resD, resE, resN});

        }
    }

    @Test
    public void testNoiseEmissionRailWay_Section556() throws SQLException, IOException {
        double[] dBA = new double[]{-30,-26.2,-22.5,-19.1,-16.1,-13.4,-10.9,-8.6,-6.6,-4.8,-3.2,-1.9,-0.8,0,0.6,1,1.2,1.3,1.2,1,0.5,-0.1,-1.1,-2.5};

        SHPRead.importTable(connection, TableLoaderTest.class.getResource("Test/556/RAIL_SECTIONS.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("Test/556/RAIL_TRAFIC.dbf").getFile());

        HashMap<String, double[]> results = new HashMap<>();

        RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection,"RAIL_SECTIONS", "RAIL_TRAFIC");

        double resD,resE,resN;

       // RailWayLWIterator.RailWayLWGeom v = railWayLWIterator.current();

        while (railWayLWIterator.hasNext()) {
            RailWayLWGeom v = railWayLWIterator.next();

            RailWayParameters railWayLW = v.getRailWayLWDay();

            double[] lW = new double[24];
            Arrays.fill(lW, -99.00);

            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }

            double[] LWA = sumArray(lW, dBA);
            resD = sumDbArray(LWA);

            railWayLW = v.getRailWayLWEvening();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resE = sumDbArray(LWA);

            railWayLW = v.getRailWayLWNight();
            Arrays.fill(lW, -99.00);
            if (!railWayLW.getRailwaySourceList().isEmpty()){
                for (Map.Entry<String, LineSource> railwaySourceEntry : railWayLW.getRailwaySourceList().entrySet()) {
                    double[]  lW1 = railwaySourceEntry.getValue().getlW();
                    lW = AcousticIndicatorsFunctions.sumDbArray(lW, lW1);
                }
            }
            LWA = sumArray(lW, dBA);
            resN = sumDbArray(LWA);

            String idSection = v.getIdSection();

            results.put(idSection,new double[]{resD, resE, resN});

        }

        // TODO check results


    }

    @Test
    public void testReadFrequencies() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("lw_roads.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("buildings.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("receivers.shp").getFile());

        try(Statement st = connection.createStatement()) {
            // drop all columns except 1000 Hz
            st.execute("ALTER TABLE lw_roads drop column LWD63");
            st.execute("ALTER TABLE lw_roads drop column LWD125");
            st.execute("ALTER TABLE lw_roads drop column LWD250");
            st.execute("ALTER TABLE lw_roads drop column LWD500");
            st.execute("ALTER TABLE lw_roads drop column LWD2000");
            st.execute("ALTER TABLE lw_roads drop column LWD4000");
            st.execute("ALTER TABLE lw_roads drop column LWD8000");
            st.execute("ALTER TABLE lw_roads drop column LWE63");
            st.execute("ALTER TABLE lw_roads drop column LWE125");
            st.execute("ALTER TABLE lw_roads drop column LWE250");
            st.execute("ALTER TABLE lw_roads drop column LWE500");
            st.execute("ALTER TABLE lw_roads drop column LWE1000");
            st.execute("ALTER TABLE lw_roads drop column LWE2000");
            st.execute("ALTER TABLE lw_roads drop column LWE4000");
            st.execute("ALTER TABLE lw_roads drop column LWE8000");
            st.execute("ALTER TABLE lw_roads drop column LWN63");
            st.execute("ALTER TABLE lw_roads drop column LWN125");
            st.execute("ALTER TABLE lw_roads drop column LWN250");
            st.execute("ALTER TABLE lw_roads drop column LWN500");
            st.execute("ALTER TABLE lw_roads drop column LWN1000");
            st.execute("ALTER TABLE lw_roads drop column LWN2000");
            st.execute("ALTER TABLE lw_roads drop column LWN4000");
            st.execute("ALTER TABLE lw_roads drop column LWN8000");
        }

        NoiseMapByReceiverMaker noiseMap = new NoiseMapByReceiverMaker("BUILDINGS",
                "LW_ROADS", "RECEIVERS");

        noiseMap.setFrequencyFieldPrepend("LW");

        noiseMap.initialize(connection);

        DefaultTableLoader tableLoader = (DefaultTableLoader)noiseMap.getTableLoader();

        assertEquals(1, tableLoader.frequencyArray.size());

        assertEquals(1000, (int) tableLoader.frequencyArray.get(0));
    }

    // Check regression of finding cell i,j that contains receivers
    @Test
    public void testRegression1() throws SQLException, IOException {
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/lw_roads_fence.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/bati_fence.shp").getFile());
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("regression1/receivers.shp").getFile());

        // Count receivers
        int nbReceivers = 0;
        try(ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) CPT FROM RECEIVERS")) {
            assertTrue(rs.next());
            nbReceivers = rs.getInt(1);
        }

        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker("BATI_FENCE",
                "LW_ROADS_FENCE",
                "RECEIVERS");

        noiseMapByReceiverMaker.initialize(connection);

        Map<CellIndex, Integer> populatedCells = noiseMapByReceiverMaker.searchPopulatedCells(connection);

        // Check if all receivers are found
        assertEquals(nbReceivers, populatedCells.values().stream().reduce(Integer::sum).orElse(0));
    }

    /**
     * Test that HRAIL column is properly created in LW_RAILWAY output
     * and that the default value matches the default platform hRail (h2 = 0.18).
     */
    @Test
    public void testRailWithScreenBodyBarrier() throws SQLException, IOException {
        // --- Common setup ---
        SHPRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Section2.shp").getFile());
        DBFRead.importTable(connection, TableLoaderTest.class.getResource("PropaRail/Rail_Traffic.dbf").getFile());

        EmissionTableGenerator.makeTrainLWTable(connection, "Rail_Section2", "Rail_Traffic",
                "LW_RAILWAY", "HZ");

        // Verify HRAIL column was created in LW_RAILWAY
        assertTrue(JDBCUtilities.hasField(connection, "LW_RAILWAY", "HRAIL"),
                "LW_RAILWAY should have HRAIL column");

        // Verify HRAIL value is the default platform hRail (h2 = 0.18)
        try (ResultSet rs = connection.createStatement().executeQuery(
                "SELECT HRAIL FROM LW_RAILWAY LIMIT 1")) {
            assertTrue(rs.next());
            assertEquals(0.18, rs.getDouble("HRAIL"), 0.001,
                    "Default HRAIL should be 0.18 (h2 = rail above ballast)");
        }
    }


    /**
     * Tests RailwayPlatform JSON loading, validates SNCF and SNCF_LGV platform properties,
     * verifies getHRail() returns h2, checks multi-track calculation methods (getD2, getD3, getD4),
     * and confirms DEFAULT_PLATFORM differs from SNCF.
     */
    @Test
    public void Test_Railway_Platform() throws Exception {
        // Load platforms from the default JSON file
        Map<String, RailwayPlatform> platforms = RailwayPlatform.loadFromJSON(RailWayLWIterator.RAILWAY_PLATFORMS_JSON);

        // Verify that loading succeeded
        assertFalse(platforms.isEmpty(), "RailwayPlatforms.json file must contain platforms");

        // Verify that SNCF and SNCF_LGV platforms exist
        assertTrue(platforms.containsKey("SNCF"), "SNCF platform must be present");
        assertTrue(platforms.containsKey("SNCF_LGV"), "SNCF_LGV platform must be present");

        // Retrieve instances
        RailwayPlatform sncfPlatform = platforms.get("SNCF");
        RailwayPlatform sncfLgvPlatform = platforms.get("SNCF_LGV");
        RailwayPlatform defaultPlatform = RailwayPlatform.DEFAULT_PLATFORM;

        // Verify SNCF properties
        assertNotNull(sncfPlatform, "SNCF must not be null");
        assertEquals(1.0, sncfPlatform.d1, 0.001, "SNCF d1 incorrect");
        assertEquals(3.0, sncfPlatform.d2_0, 0.001, "SNCF d2_0 incorrect");
        assertEquals(4.0, sncfPlatform.d3_0, 0.001, "SNCF d3_0 incorrect");
        assertEquals(8.0, sncfPlatform.d4_0, 0.001, "SNCF d4_0 incorrect");
        assertEquals(0.0, sncfPlatform.g1, 0.001, "SNCF g1 incorrect");
        assertEquals(1.0, sncfPlatform.g2, 0.001, "SNCF g2 incorrect");
        assertEquals(0.0, sncfPlatform.g3, 0.001, "SNCF g3 incorrect");
        assertEquals(0.0, sncfPlatform.h1, 0.001, "SNCF h1 incorrect");
        assertEquals(0.0, sncfPlatform.h2, 0.001, "SNCF h2 incorrect");
        assertEquals(3.67, sncfPlatform.trackspacing, 0.001, "SNCF trackspacing incorrect");
        // Verify SNCF_LGV properties (differs by trackspacing)
        assertNotNull(sncfLgvPlatform, "SNCF_LGV must not be null");
        assertEquals(4.5, sncfLgvPlatform.trackspacing, 0.001, "SNCF_LGV trackspacing must be 4.5");

        // Verify that getHRail returns h2
        assertEquals(sncfPlatform.h2, sncfPlatform.getHRail(), 0.001, "getHRail must return h2");

        // Test calculation methods with multiple tracks
        int nbTracks = 2;
        double trackSpacing = 3.67;
        assertEquals(3.0 + (nbTracks - 1) * trackSpacing, sncfPlatform.getD2(nbTracks, trackSpacing), 0.001, "getD2 calculation incorrect");
        assertEquals(4.0 + (nbTracks - 1) * trackSpacing, sncfPlatform.getD3(nbTracks, trackSpacing), 0.001, "getD3 calculation incorrect");
        assertEquals(8.0 + (nbTracks - 1) * trackSpacing, sncfPlatform.getD4(nbTracks, trackSpacing), 0.001, "getD4 calculation incorrect");

        // Verify that DEFAULT_PLATFORM has different values
        assertNotEquals(defaultPlatform.d1, sncfPlatform.d1, "DEFAULT_PLATFORM.d1 must differ from SNCF");
        assertEquals(1.8, defaultPlatform.d1, 0.001, "DEFAULT_PLATFORM.d1 incorrect");
    }

    @Test
    public void testRoadNoiseEmission() throws SQLException, IOException {
        try(Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS TRAFFIC_TEST (PK SERIAL, LV_D REAL, LV_SPD_D REAL, HGV_D REAL, HGV_SPD_D REAL, PVMT VARCHAR);\n"
                    + "INSERT INTO TRAFFIC_TEST(LV_D, LV_SPD_D, HGV_D, HGV_SPD_D, PVMT) VALUES (3747.23, 50, 479.02, 50, 'FR_R2');");
        }
        Map<String, Integer> sourceEmissionFieldsCache = new HashMap<>();
        double globalLevel = 0;
        try(SpatialResultSet rs = connection.createStatement().executeQuery("SELECT * FROM TRAFFIC_TEST").unwrap(SpatialResultSet.class)) {
            assertTrue(rs.next());
            // new double[][] {ld, le, ln}
            double[][] lw = EmissionTableGenerator.computeLw(rs, 1, sourceEmissionFieldsCache);
            double[] dbLw = AcousticIndicatorsFunctions.wToDb(lw[0]);
            double[] dbALw = AcousticIndicatorsFunctions.sumArray(dbLw, aWeightingArray);
            globalLevel = AcousticIndicatorsFunctions.sumArray(AcousticIndicatorsFunctions.dBToW(dbALw)); // add day value only
        }
        assertEquals(89.6, AcousticIndicatorsFunctions.wToDb(globalLevel), 0.1);
    }



}
