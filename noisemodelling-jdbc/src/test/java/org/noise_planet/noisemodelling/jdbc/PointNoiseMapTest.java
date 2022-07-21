package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCComputeRaysOut;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCPropagationData;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class PointNoiseMapTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PointNoiseMapTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
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
            st.execute("DROP TABLE IF EXISTS RECEIVERS");
            st.execute("CREATE TABLE RECEIVERS(the_geom GEOMETRY(POINTZ), GID SERIAL PRIMARY KEY)");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINTZ(-72 41 11)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINTZ(-9 41 1.6)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINTZ(70 11 7)')");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "SOUND_SOURCE", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(true);
            pointNoiseMap.setComputeVerticalDiffraction(true);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(true);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");

            pointNoiseMap.setDemTable("DEM");
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            pointNoiseMap.setComputeRaysOutFactory(new JDBCComputeRaysOut(true));
            pointNoiseMap.setPropagationProcessDataFactory(new JDBCPropagationData());

            List<ComputeRaysOutAttenuation.VerticeSL> allLevels = new ArrayList<>();
            ArrayList<PropagationPath> propaMap = new ArrayList<>();
            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof ComputeRaysOutAttenuation) {
                        allLevels.addAll(((ComputeRaysOutAttenuation) out).getVerticesSoundLevel());
                        propaMap.addAll(((ComputeRaysOutAttenuation) out).getPropagationPaths());
                    }
                }
            }


            DataOutputStream outputBin = new DataOutputStream(new FileOutputStream("./target/test-resources/propaMap.bin"));
            //PropagationPath.writePropagationPathListStream(outputBin, propaMap);
            propaMap.clear();
            DataInputStream input = new DataInputStream(new FileInputStream("./target/test-resources/propaMap.bin"));
            //PropagationPath.readPropagationPathListStream(input, propaMap);


            assertEquals(3, allLevels.size());
        }
    }

    @Test
    public void testGroundSurface() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'LANDCOVER2000')", PointNoiseMapTest.class.getResource("landcover2000.shp").getFile()));
            st.execute(getRunScriptRes("scene_with_landcover.sql"));
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(true);
            pointNoiseMap.setComputeVerticalDiffraction(true);
            pointNoiseMap.setSoundReflectionOrder(1);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.setSoilTableName("LAND_G");
            pointNoiseMap.setComputeVerticalDiffraction(true);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            pointNoiseMap.setComputeRaysOutFactory(new JDBCComputeRaysOut(false));
            pointNoiseMap.setPropagationProcessDataFactory(new JDBCPropagationData());

            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            double expectedMaxArea = Math.pow(pointNoiseMap.getGroundSurfaceSplitSideLength(), 2);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof ComputeRaysOutAttenuation) {
                        ComputeRaysOutAttenuation rout = (ComputeRaysOutAttenuation) out;
                        for(ProfileBuilder.GroundEffect soil : rout.inputData.profileBuilder.getGroundEffects()) {
                            assertTrue(soil.getGeometry().getArea() < expectedMaxArea);
                        }
                    }
                }
            }

        }
    }

    @Test
    public void testNoiseMapBuilding() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'LANDCOVER2000')", PointNoiseMapTest.class.getResource("landcover2000.shp").getFile()));
            st.execute(getRunScriptRes("scene_with_landcover.sql"));
            TriangleNoiseMap noisemap = new TriangleNoiseMap("BUILDINGS", "ROADS_GEOM");
            noisemap.setReceiverHasAbsoluteZCoordinates(false);
            noisemap.setSourceHasAbsoluteZCoordinates(false);
            noisemap.setHeightField("HEIGHT");
            noisemap.initialize(connection, new EmptyProgressVisitor());

            AtomicInteger pk = new AtomicInteger(0);
            for(int i=0; i < noisemap.getGridDim(); i++) {
                for(int j=0; j < noisemap.getGridDim(); j++) {
                    noisemap.generateReceivers(connection, i, j, "NM_RECEIVERS", "TRIANGLES", pk);
                }
            }
            assertNotSame(0, pk.get());
        }
    }

    //    @Test
    //    public void testNoiseMapBuilding2() throws Exception {
    //        try(Statement st = connection.createStatement()) {
    //            SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("roads_traff.shp").getFile(), "ROADS_GEOM");
    //            SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile(), " BUILDINGS");
    //            TriangleNoiseMap noisemap = new TriangleNoiseMap("BUILDINGS", "ROADS_GEOM");
    //            noisemap.setReceiverHasAbsoluteZCoordinates(false);
    //            noisemap.setSourceHasAbsoluteZCoordinates(false);
    //            noisemap.setHeightField("HEIGHT");
    //            noisemap.setMaximumArea(300);
    //            noisemap.setBuildingBuffer(0);
    //            noisemap.setMaximumPropagationDistance(800);
    //
    //
    //
    //            noisemap.initialize(connection, new EmptyProgressVisitor());
    //            AtomicInteger pk = new AtomicInteger(0);
    //            for(int i=0; i < noisemap.getGridDim(); i++) {
    //                for(int j=0; j < noisemap.getGridDim(); j++) {
    //                    noisemap.generateReceivers(connection, i, j, "NM_RECEIVERS", "TRIANGLES", pk);
    //                }
    //            }
    //            assertNotSame(0, pk.get());
    //            SHPWrite.exportTable(connection, "target/triangle.shp", "TRIANGLES");
    //        }
    //    }

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
        PropagationProcessPathData data = new PropagationProcessPathData(false);
        for(String period : new String[] {"D", "E", "N"}) {
            for (int freq : data.freq_lvl) {
                String fieldName = "LW" + period + freq;
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
                    91, new Orientation(90,15,0),
                    RailWayLW.TrainNoiseSource.TRACTIONB.ordinal() + 1));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223915.72 6757490.22 0.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223925.72 6757480.22 0.0)');");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setMaximumPropagationDistance(1000);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");

            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_MEMORY);
            ldenConfig.setCoefficientVersion(1);
            LDENPointNoiseMapFactory ldenPointNoiseMapFactory = new LDENPointNoiseMapFactory(connection, ldenConfig);
            // Use train directivity functions instead of discrete directivity
            ldenPointNoiseMapFactory.insertTrainDirectivity();

            pointNoiseMap.setPropagationProcessDataFactory(ldenPointNoiseMapFactory);
            pointNoiseMap.setComputeRaysOutFactory(ldenPointNoiseMapFactory);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            pointNoiseMap.setGridDim(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof LDENComputeRaysOut) {
                        LDENComputeRaysOut rout = (LDENComputeRaysOut) out;

                        ComputeRaysOutAttenuation.VerticeSL sl = rout.ldenData.lDenLevels.pop();
                        assertEquals(1, sl.receiverId);
                        assertEquals(73.3, sl.value[0], 1);
                        sl = rout.ldenData.lDenLevels.pop();
                        assertEquals(2, sl.receiverId);
                        assertEquals(53.3, sl.value[0], 1);
                        assertTrue(rout.ldenData.lDenLevels.isEmpty());

                        assertEquals(2 , rout.propagationPaths.size());
                        PropagationPath path = rout.propagationPaths.remove(0);
                        assertEquals(1, path.getIdReceiver());
                        assertEquals(new Orientation(90, 15, 0), path.getSourceOrientation());
                        path = rout.propagationPaths.remove(0);
                        assertEquals(2, path.getIdReceiver());
                        assertEquals(new Orientation(90, 15, 0), path.getSourceOrientation());

                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }

    public static void assertOrientationEquals(Orientation orientationA, Orientation orientationB, double epsilon) {
        assertEquals(orientationA.pitch, orientationB.pitch, epsilon);
        assertEquals(orientationA.roll, orientationB.roll, epsilon);
        assertEquals(orientationA.yaw, orientationB.yaw, epsilon);
    }


    @Test
    public void testLineDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial PRIMARY KEY, the_geom geometry, height real)");
            st.execute(createSource(new GeometryFactory().createLineString(
                    new Coordinate[]{new Coordinate(223915.72,6757480.22 ,5),
                            new Coordinate(223920.72,6757485.22, 5.1 )}), 91,
                    new Orientation(0,0,0),
                    RailWayLW.TrainNoiseSource.TRACTIONB.ordinal() + 1));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(pointZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223922.55 6757495.27 0.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223936.42 6757471.91 0.0)');");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setMaximumPropagationDistance(1000);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");

            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenConfig.setCoefficientVersion(1);
            ldenConfig.setKeepAbsorption(true);
            ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_MEMORY);
            LDENPointNoiseMapFactory ldenPointNoiseMapFactory = new LDENPointNoiseMapFactory(connection, ldenConfig);
            // Use train directivity functions instead of discrete directivity
            ldenPointNoiseMapFactory.insertTrainDirectivity();
            pointNoiseMap.setPropagationProcessDataFactory(ldenPointNoiseMapFactory);
            pointNoiseMap.setComputeRaysOutFactory(ldenPointNoiseMapFactory);

            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            Envelope compEnv = new Envelope(new Coordinate(223915.72,6757480.22 ,5));
            compEnv.expandBy(500);
            pointNoiseMap.setMainEnvelope(compEnv);

            pointNoiseMap.setGridDim(1);

            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof LDENComputeRaysOut) {
                        LDENComputeRaysOut rout = (LDENComputeRaysOut) out;

                        assertEquals(2, rout.ldenData.lDenLevels.size());

                        ComputeRaysOutAttenuation.VerticeSL sl = rout.ldenData.lDenLevels.pop();
                        assertEquals(1, sl.receiverId);
                        assertEquals(68.3, sl.value[0], 1);
                        sl = rout.ldenData.lDenLevels.pop();
                        assertEquals(2, sl.receiverId);
                        assertEquals(70.8, sl.value[0], 1);

                        assertEquals(3 , rout.propagationPaths.size());
                        PropagationPath path = rout.propagationPaths.remove(0);
                        assertEquals(1, path.getIdReceiver());
                        assertEquals(0, new Coordinate(0, 5.07).
                                distance(path.getPointList().get(0).coordinate), 0.1);
                        // This is source orientation, not relevant to receiver position
                        assertOrientationEquals(new Orientation(45, 0.81, 0), path.getSourceOrientation(), 0.01);
                        assertOrientationEquals(new Orientation(330.07, -24.12, 0.0), path.raySourceReceiverDirectivity, 0.01);
                        assertEquals(-5.9, path.absorptionData.aSource[0], 0.1);

                        path = rout.propagationPaths.remove(0);;
                        assertEquals(1, path.getIdReceiver());
                        assertEquals(0, new Coordinate(0, 5.02).
                                distance(path.getPointList().get(0).coordinate), 0.1);
                        assertOrientationEquals(new Orientation(45, 0.81, 0), path.getSourceOrientation(), 0.01);
                        assertOrientationEquals(new Orientation(336.90675972385696, -19.398969693698437, 0), path.raySourceReceiverDirectivity, 0.01);
                        assertEquals(-7.8, path.absorptionData.aSource[0], 0.1);
                        path = rout.propagationPaths.remove(0);
                        assertEquals(2, path.getIdReceiver());
                        assertOrientationEquals(new Orientation(45, 0.81, 0), path.getSourceOrientation(), 0.01);
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }


    @Test
    public void testPointRayDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial  PRIMARY KEY, the_geom geometry, height real)");
            // create source point direction east->90°
            st.execute(createSource(new GeometryFactory().createPoint(new Coordinate(3.5,3,1.0 )),
                    91, new Orientation(90,0,0),
                    RailWayLW.TrainNoiseSource.TRACTIONB.ordinal() + 1));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (4.5 3 1.0)');" + //front
                    "insert into receivers(the_geom) values ('POINTZ (2.5 3 1.0)');" + //behind
                    "insert into receivers(the_geom) values ('POINTZ (3.5 2 1.0)');" + //right
                    "insert into receivers(the_geom) values ('POINTZ (3.5 4 1.0)');"); //left
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setMaximumPropagationDistance(1000);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");

            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenConfig.setCoefficientVersion(1);
            ldenConfig.setKeepAbsorption(true);
            ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_MEMORY);
            LDENPointNoiseMapFactory ldenPointNoiseMapFactory = new LDENPointNoiseMapFactory(connection, ldenConfig);
            // Use train directivity functions instead of discrete directivity
            ldenPointNoiseMapFactory.insertTrainDirectivity();

            pointNoiseMap.setPropagationProcessDataFactory(ldenPointNoiseMapFactory);
            pointNoiseMap.setComputeRaysOutFactory(ldenPointNoiseMapFactory);

            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof LDENComputeRaysOut) {
                        LDENComputeRaysOut rout = (LDENComputeRaysOut) out;
                        assertEquals(4 , rout.propagationPaths.size());
                        PropagationPath path = rout.propagationPaths.remove(0);
                        assertEquals(1, path.getIdReceiver());
                        // receiver is front of source
                        assertEquals(new Orientation(0, 0, 0), path.getRaySourceReceiverDirectivity());
                        path = rout.propagationPaths.remove(0);
                        assertEquals(2, path.getIdReceiver());
                        // receiver is behind of the source
                        assertEquals(new Orientation(180, 0, 0), path.getRaySourceReceiverDirectivity());
                        path = rout.propagationPaths.remove(0);
                        assertEquals(3, path.getIdReceiver());
                        // receiver is on the right of the source
                        assertEquals(new Orientation(90, 0, 0), path.getRaySourceReceiverDirectivity());
                        path = rout.propagationPaths.remove(0);
                        assertEquals(4, path.getIdReceiver());
                        // receiver is on the left of the source
                        assertEquals(new Orientation(360-90, 0, 0), path.getRaySourceReceiverDirectivity());
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }
}