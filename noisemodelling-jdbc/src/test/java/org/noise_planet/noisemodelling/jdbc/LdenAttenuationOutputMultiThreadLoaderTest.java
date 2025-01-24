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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCComputeRaysOut;
import org.noise_planet.noisemodelling.jdbc.Utils.JDBCPropagationData;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.GroundAbsorption;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

import java.sql.Connection;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.getRunScriptRes;

public class LdenAttenuationOutputMultiThreadLoaderTest {

    private Connection connection;

    @BeforeEach
    public void tearUp() throws Exception {
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(LdenAttenuationOutputMultiThreadLoaderTest.class.getSimpleName(), true, ""));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testGroundSurface() throws Exception {
        try(Statement st = connection.createStatement()) {
            st.execute(String.format("CALL SHPREAD('%s', 'LANDCOVER2000')", LdenAttenuationOutputMultiThreadLoaderTest.class.getResource("landcover2000.shp").getFile()));
            st.execute(getRunScriptRes("scene_with_landcover.sql"));
            LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            ldenNoiseMapLoader.setComputeHorizontalDiffraction(true);
            ldenNoiseMapLoader.setComputeVerticalDiffraction(true);
            ldenNoiseMapLoader.setSoundReflectionOrder(1);
            ldenNoiseMapLoader.setReceiverHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setSourceHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setHeightField("HEIGHT");
            ldenNoiseMapLoader.setSoilTableName("LAND_G");
            ldenNoiseMapLoader.setComputeVerticalDiffraction(true);
            ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());

            ldenNoiseMapLoader.setComputeRaysOutFactory(new JDBCComputeRaysOut(false));
            ldenNoiseMapLoader.setPropagationProcessDataFactory(new JDBCPropagationData());

            Set<Long> receivers = new HashSet<>();
            ldenNoiseMapLoader.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(ldenNoiseMapLoader.getGridDim() * ldenNoiseMapLoader.getGridDim(), true, 5);
            double expectedMaxArea = Math.pow(ldenNoiseMapLoader.getGroundSurfaceSplitSideLength(), 2);
            for(int i = 0; i < ldenNoiseMapLoader.getGridDim(); i++) {
                for(int j = 0; j < ldenNoiseMapLoader.getGridDim(); j++) {
                    IComputePathsOut out = ldenNoiseMapLoader.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof AttenuationComputeOutput) {
                        AttenuationComputeOutput rout = (AttenuationComputeOutput) out;
                        for(GroundAbsorption soil : rout.inputData.profileBuilder.getGroundEffects()) {
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
            st.execute(String.format("CALL SHPREAD('%s', 'LANDCOVER2000')", LdenAttenuationOutputMultiThreadLoaderTest.class.getResource("landcover2000.shp").getFile()));
            st.execute(getRunScriptRes("scene_with_landcover.sql"));
            DelaunayReceiversMaker noisemap = new DelaunayReceiversMaker("BUILDINGS", "ROADS_GEOM");
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
        AttenuationCnossosParameters data = new AttenuationCnossosParameters(false);
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
                    4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223915.72 6757490.22 0.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223925.72 6757480.22 0.0)');");
            LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            ldenNoiseMapLoader.setComputeHorizontalDiffraction(false);
            ldenNoiseMapLoader.setComputeVerticalDiffraction(false);
            ldenNoiseMapLoader.setSoundReflectionOrder(0);
            ldenNoiseMapLoader.setReceiverHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setMaximumPropagationDistance(1000);
            ldenNoiseMapLoader.setSourceHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setHeightField("HEIGHT");

            LdenNoiseMapParameters ldenNoiseMapParameters = new LdenNoiseMapParameters(LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenNoiseMapParameters.setExportRaysMethod(LdenNoiseMapParameters.ExportRaysMethods.TO_MEMORY);

            ldenNoiseMapParameters.setCoefficientVersion(1);
            NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, ldenNoiseMapParameters);
            // Use train directivity functions instead of discrete directivity
            noiseMapMaker.insertTrainDirectivity();

            ldenNoiseMapLoader.setPropagationProcessDataFactory(noiseMapMaker);
            ldenNoiseMapLoader.setComputeRaysOutFactory(noiseMapMaker);

            ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());
            Set<Long> receivers = new HashSet<>();
            ldenNoiseMapLoader.setThreadCount(1);
            ldenNoiseMapLoader.setGridDim(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor((long) ldenNoiseMapLoader.getGridDim() * ldenNoiseMapLoader.getGridDim(), true, 5);
            //System.out.println("size = "+ noiseMapByReceiverMaker.getGridDim());
            for(int i = 0; i < ldenNoiseMapLoader.getGridDim(); i++) {
                for(int j = 0; j < ldenNoiseMapLoader.getGridDim(); j++) {
                    IComputePathsOut out = ldenNoiseMapLoader.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof AttenuationOutputMultiThread) {
                        AttenuationOutputMultiThread rout = (AttenuationOutputMultiThread) out;

                        AttenuationComputeOutput.SourceReceiverAttenuation sl = rout.attenuatedPaths.lDenLevels.pop();
                        assertEquals(1, sl.receiver.receiverPk);
                        assertEquals(73.3, sl.value[0], 1);
                        sl = rout.attenuatedPaths.lDenLevels.pop();
                        assertEquals(2, sl.receiver.receiverPk);
                        assertEquals(53.3, sl.value[0], 1);
                        assertTrue(rout.attenuatedPaths.lDenLevels.isEmpty());

                        List<CnossosPath> pathsParameters = rout.getPropagationPaths();
                        assertEquals(2 , pathsParameters.size());

                        CnossosPath pathParameters = pathsParameters.remove(0);
                        assertEquals(1, pathParameters.getIdReceiver());
                        assertEquals(new Orientation(90, 15, 0), pathParameters.getSourceOrientation());
                        pathParameters = pathsParameters.remove(0);
                        assertEquals(2, pathParameters.getIdReceiver());
                        assertEquals(new Orientation(90, 15, 0), pathParameters.getSourceOrientation());

                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }

    public static void assertOrientationEquals(Orientation orientationA, Orientation orientationB, double epsilon) {
        assertArrayEquals(new double[]{orientationA.yaw, orientationA.pitch, orientationA.roll},
                new double[]{orientationB.yaw, orientationB.pitch, orientationB.roll}, epsilon, orientationA+" != "+orientationB);
    }


    @Test
    public void testLineDirectivity() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE BUILDINGS(pk serial PRIMARY KEY, the_geom geometry, height real)");
            st.execute(createSource(new GeometryFactory().createLineString(
                    new Coordinate[]{new Coordinate(223915.72,6757480.22 ,5),
                            new Coordinate(223920.72,6757485.22, 5.1 )}), 91,
                    new Orientation(0,0,0),4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(pointZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (223922.55 6757495.27 4.0)');" +
                    "insert into receivers(the_geom) values ('POINTZ (223936.42 6757471.91 4.0)');");
            LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            ldenNoiseMapLoader.setComputeHorizontalDiffraction(false);
            ldenNoiseMapLoader.setComputeVerticalDiffraction(false);
            ldenNoiseMapLoader.setSoundReflectionOrder(0);
            ldenNoiseMapLoader.setReceiverHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setMaximumPropagationDistance(1000);
            ldenNoiseMapLoader.setSourceHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setHeightField("HEIGHT");

            LdenNoiseMapParameters ldenNoiseMapParameters = new LdenNoiseMapParameters(LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenNoiseMapParameters.setCoefficientVersion(1);
            ldenNoiseMapParameters.setExportAttenuationMatrix(false);
            ldenNoiseMapParameters.setExportRaysMethod(LdenNoiseMapParameters.ExportRaysMethods.TO_MEMORY);
            NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, ldenNoiseMapParameters);
            // Use train directivity functions instead of discrete directivity
            noiseMapMaker.insertTrainDirectivity();
            ldenNoiseMapLoader.setPropagationProcessDataFactory(noiseMapMaker);
            ldenNoiseMapLoader.setComputeRaysOutFactory(noiseMapMaker);

            Set<Long> receivers = new HashSet<>();
            ldenNoiseMapLoader.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(ldenNoiseMapLoader.getGridDim() * ldenNoiseMapLoader.getGridDim(), true, 5);

            ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());

            Envelope compEnv = new Envelope(new Coordinate(223915.72,6757480.22 ,5));
            compEnv.expandBy(500);
            ldenNoiseMapLoader.setMainEnvelope(compEnv);

            ldenNoiseMapLoader.setGridDim(1);

            for(int i = 0; i < ldenNoiseMapLoader.getGridDim(); i++) {
                for(int j = 0; j < ldenNoiseMapLoader.getGridDim(); j++) {
                    //System.out.println("here");
                    IComputePathsOut out = ldenNoiseMapLoader.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof AttenuationOutputMultiThread) {
                        AttenuationOutputMultiThread rout = (AttenuationOutputMultiThread) out;

                        assertEquals(2, rout.attenuatedPaths.lDenLevels.size());

                        AttenuationComputeOutput.SourceReceiverAttenuation sl = rout.attenuatedPaths.lDenLevels.pop();
                        assertEquals(1, sl.receiver.receiverPk);
                        assertEquals(68.3, sl.value[0], 1);
                        sl = rout.attenuatedPaths.lDenLevels.pop();
                        assertEquals(2, sl.receiver.receiverPk);
                        assertEquals(70.8, sl.value[0], 1);

                        assertEquals(3 , rout.pathParameters.size());
                        List<CnossosPath> pathsParameters = rout.getPropagationPaths();

                        CnossosPath pathParameters = pathsParameters.remove(0);
                        assertEquals(1, pathParameters.getIdReceiver());
                        assertEquals(0, new Coordinate(0, 5.07).distance(pathParameters.getPointList().get(0).coordinate), 0.01);
                        // This is source orientation, not relevant to receiver position
                        assertOrientationEquals(new Orientation(45, 0.81, 0), pathParameters.getSourceOrientation(), 0.01);
                        assertOrientationEquals(new Orientation(330.2084079818916,-5.947213381005439,0.0), pathParameters.raySourceReceiverDirectivity, 0.01);

                        pathParameters = pathsParameters.remove(0);;
                        assertEquals(1, pathParameters.getIdReceiver());
                        assertEquals(0, new Coordinate(0, 5.02).
                                distance(pathParameters.getPointList().get(0).coordinate), 0.01);
                        assertOrientationEquals(new Orientation(45, 0.81, 0), pathParameters.getSourceOrientation(), 0.01);
                        assertOrientationEquals(new Orientation(336.9922375343167,-4.684918495003125,0.0), pathParameters.raySourceReceiverDirectivity, 0.01);

                        pathParameters = pathsParameters.remove(0);
                        assertEquals(2, pathParameters.getIdReceiver());
                        assertOrientationEquals(new Orientation(45, 0.81, 0), pathParameters.getSourceOrientation(), 0.01);
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
                    91, new Orientation(90,0,0),4));
            st.execute("create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));\n" +
                    "insert into receivers(the_geom) values ('POINTZ (4.5 3 1.0)');" + //front
                    "insert into receivers(the_geom) values ('POINTZ (2.5 3 1.0)');" + //behind
                    "insert into receivers(the_geom) values ('POINTZ (3.5 2 1.0)');" + //right
                    "insert into receivers(the_geom) values ('POINTZ (3.5 4 1.0)');"); //left
            LdenNoiseMapLoader ldenNoiseMapLoader = new LdenNoiseMapLoader("BUILDINGS", "ROADS_GEOM", "RECEIVERS");
            ldenNoiseMapLoader.setComputeHorizontalDiffraction(false);
            ldenNoiseMapLoader.setComputeVerticalDiffraction(false);
            ldenNoiseMapLoader.setSoundReflectionOrder(0);
            ldenNoiseMapLoader.setReceiverHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setMaximumPropagationDistance(1000);
            ldenNoiseMapLoader.setSourceHasAbsoluteZCoordinates(false);
            ldenNoiseMapLoader.setHeightField("HEIGHT");

            LdenNoiseMapParameters ldenNoiseMapParameters = new LdenNoiseMapParameters(LdenNoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN);
            ldenNoiseMapParameters.setCoefficientVersion(1);
            ldenNoiseMapParameters.setExportAttenuationMatrix(false);
            ldenNoiseMapParameters.setExportRaysMethod(LdenNoiseMapParameters.ExportRaysMethods.TO_MEMORY);
            NoiseMapMaker noiseMapMaker = new NoiseMapMaker(connection, ldenNoiseMapParameters);
            // Use train directivity functions instead of discrete directivity
            noiseMapMaker.insertTrainDirectivity();

            ldenNoiseMapLoader.setPropagationProcessDataFactory(noiseMapMaker);
            ldenNoiseMapLoader.setComputeRaysOutFactory(noiseMapMaker);

            ldenNoiseMapLoader.initialize(connection, new EmptyProgressVisitor());

            Set<Long> receivers = new HashSet<>();
            ldenNoiseMapLoader.setThreadCount(1);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(ldenNoiseMapLoader.getGridDim() * ldenNoiseMapLoader.getGridDim(), true, 5);
            for(int i = 0; i < ldenNoiseMapLoader.getGridDim(); i++) {
                for(int j = 0; j < ldenNoiseMapLoader.getGridDim(); j++) {
                    IComputePathsOut out = ldenNoiseMapLoader.evaluateCell(connection, i, j, progressVisitor, receivers);
                    if(out instanceof AttenuationOutputMultiThread) {
                        AttenuationOutputMultiThread rout = (AttenuationOutputMultiThread) out;
                        List<CnossosPath> pathsParameters = rout.getPropagationPaths();
                        assertEquals(4 , pathsParameters.size());
                        CnossosPath pathParameters = pathsParameters.remove(0);
                        assertEquals(1, pathParameters.getIdReceiver());
                        // receiver is front of source
                        assertEquals(new Orientation(0, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
                        pathParameters = pathsParameters.remove(0);
                        assertEquals(2, pathParameters.getIdReceiver());
                        // receiver is behind of the source
                        assertEquals(new Orientation(180, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
                        pathParameters = pathsParameters.remove(0);
                        assertEquals(3, pathParameters.getIdReceiver());
                        // receiver is on the right of the source
                        assertEquals(new Orientation(90, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
                        pathParameters = pathsParameters.remove(0);
                        assertEquals(4, pathParameters.getIdReceiver());
                        // receiver is on the left of the source
                        assertEquals(new Orientation(360-90, 0, 0), pathParameters.getRaySourceReceiverDirectivity());
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }
}