package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCComputeRaysOut;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCPropagationData;
import org.noise_planet.noisemodelling.pathfinder.*;
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
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(PointNoiseMapTest.class.getSimpleName(), true, ""));
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
            st.execute("CREATE TABLE RECEIVERS(the_geom POINT, GID SERIAL)");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-72 41 11)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-9 41 1.6)')");
            st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(70 11 7)')");
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
            PropagationPath.writePropagationPathListStream(outputBin, propaMap);
            propaMap.clear();
            DataInputStream input = new DataInputStream(new FileInputStream("./target/test-resources/propaMap.bin"));
            PropagationPath.readPropagationPathListStream(input, propaMap);


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
                        for(GeoWithSoilType soil : rout.inputData.getSoilList()) {
                            assertTrue(soil.getGeo().getArea() < expectedMaxArea);
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

    private static String createSource(Coordinate localisation, double lvl, Orientation sourceOrientation, int directivityId) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ROADS_GEOM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY, YAW REAL, PITCH REAL, ROLL REAL, DIR_ID INT");
        StringBuilder values = new StringBuilder("null, ST_SETSRID('");
        values.append(new WKTWriter(3).write(new GeometryFactory().createPoint(localisation)));
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
            SHPRead.readShape(connection, LDENPointNoiseMapFactoryTest.class.getResource("buildings.shp").getFile(), " BUILDINGS");
            st.execute(createSource(new Coordinate(223915.72,6757480.22 ), 91, new Orientation(0,0,0), RailWayLW.TrainNoiseSource.TRACTIONB.ordinal() + 1));
            st.execute("create table receivers(id serial, the_geom point);\n" +
                    "insert into receivers(the_geom) values ('POINT (223915.72 6757490.22)');" +
                    "insert into receivers(the_geom) values ('POINT (223925.72 6757480.22)');");
            PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            pointNoiseMap.setComputeHorizontalDiffraction(false);
            pointNoiseMap.setComputeVerticalDiffraction(false);
            pointNoiseMap.setSoundReflectionOrder(0);
            pointNoiseMap.setReceiverHasAbsoluteZCoordinates(false);
            pointNoiseMap.setMaximumPropagationDistance(1000);
            pointNoiseMap.setSourceHasAbsoluteZCoordinates(false);
            pointNoiseMap.setHeightField("HEIGHT");
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

            LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData());
            ldenConfig.setCoefficientVersion(1);
            LDENPointNoiseMapFactory ldenPointNoiseMapFactory = new LDENPointNoiseMapFactory(connection, ldenConfig);
            // Use train directivity functions instead of discrete directivity
            ldenPointNoiseMapFactory.insertTrainDirectivity();
            pointNoiseMap.setPropagationProcessDataFactory(ldenPointNoiseMapFactory);
            pointNoiseMap.setComputeRaysOutFactory(ldenPointNoiseMapFactory);

            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof ComputeRaysOutAttenuation) {
                        ComputeRaysOutAttenuation rout = (ComputeRaysOutAttenuation) out;

                    }
                }
            }
        }
    }

}