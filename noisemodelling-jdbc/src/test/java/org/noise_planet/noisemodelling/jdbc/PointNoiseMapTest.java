package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.functions.io.shp.internal.SHPDriver;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCComputeRaysOut;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCPropagationData;
import org.noise_planet.noisemodelling.pathfinder.GeoWithSoilType;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

            List<ComputeRaysOut.VerticeSL> allLevels = new ArrayList<>();
            ArrayList<PropagationPath> propaMap = new ArrayList<>();
            Set<Long> receivers = new HashSet<>();
            pointNoiseMap.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(pointNoiseMap.getGridDim() * pointNoiseMap.getGridDim(), true, 5);
            for(int i=0; i < pointNoiseMap.getGridDim(); i++) {
                for(int j=0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof ComputeRaysOut) {
                        allLevels.addAll(((ComputeRaysOut) out).getVerticesSoundLevel());
                        propaMap.addAll(((ComputeRaysOut) out).getPropagationPaths());
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
                    if(out instanceof ComputeRaysOut) {
                        ComputeRaysOut rout = (ComputeRaysOut) out;
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


}