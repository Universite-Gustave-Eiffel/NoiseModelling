package org.noise_planet.noisemodelling.propagation;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.propagation.utils.Densifier3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EvaluateAttenuationCnossosTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateAttenuationCnossosTest.class);
    // TODO reduce error epsilon
    private static final double ERROR_EPSILON = 5;

    /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();


        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -20, 80)), 0));
        rayData.setComputeVerticalDiffraction(true);
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertArrayEquals(new double[]{-53.05, -53.11, -53.23, -53.4, -53.74, -54.91, -59.39, -75.73}, propDataOut.getVerticesSoundLevel().get(0).value, ERROR_EPSILON);
    }

    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.1, 240, 0),
                new Coordinate(265.1, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertArrayEquals(new double[]{-60.3, -61.42, -63.01, -65.11, -68.64, -71.54, -78.82, -98.05}, propDataOut.getVerticesSoundLevel().get(0).value, ERROR_EPSILON);


    }

    /**
     * Test optimisation feature {@link PropagationProcessData#maximumError}
     */
    @Test
    public void testIgnoreNonSignificantSources() throws LayerDelaunayError {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = ComputeRays.dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(0, 0, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), roadLvl);
        rayData.addSource(factory.createPoint(new Coordinate(1100, 1100, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;
        rayData.maximumError = 3; // 3 dB error max

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        // Second source has not been computed because at best it would only increase the received level of only 0.0004 dB
        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        assertEquals(44.07, ComputeRays.wToDba(ComputeRays.sumArray(roadLvl.length, ComputeRays.dbaToW(propDataOut.receiversAttenuationLevels.get(0).value))), 0.1);
    }

    @Test
    public void testRoseIndex() {
        double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;
        double angleStart = Math.PI / 2 - angle_section / 2;
        for(int i = 0; i < PropagationProcessPathData.DEFAULT_WIND_ROSE.length; i++) {
            double angle = angleStart - angle_section * i - angle_section / 3;
            int index = ComputeRaysOut.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);angle = angleStart - angle_section * i - angle_section * 2.0/3.0;
            index = ComputeRaysOut.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);
        }
    }

    /**
     * Check if Li coefficient computation and line source subdivision are correctly done
     * @throws LayerDelaunayError
     */
    @Test
    public void testSourceLines()  throws LayerDelaunayError, IOException, ParseException {

        // First Compute the scene with only point sources at 1m each
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        LineString geomSource = (LineString)wktReader.read("LINESTRING (51 40.5 0.05, 51 55.5 0.05)");
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = ComputeRays.dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(50, 50, 0.05));
        rayData.addReceiver(new Coordinate(48, 50, 4));
        rayData.addReceiver(new Coordinate(44, 50, 4));
        rayData.addReceiver(new Coordinate(40, 50, 4));
        rayData.addReceiver(new Coordinate(20, 50, 4));
        rayData.addReceiver(new Coordinate(0, 50, 4));

        List<Coordinate> srcPtsRef = new ArrayList<>();
        ComputeRays.splitLineStringIntoPoints(geomSource, 1.0, srcPtsRef);
        for(Coordinate srcPtRef : srcPtsRef) {
            rayData.addSource(factory.createPoint(srcPtRef), roadLvl);
        }

        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.makeRelativeZToAbsoluteOnlySources();
        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);


        // Second compute the same scene but with a line source
        rayData.clearSources();
        rayData.addSource(geomSource, roadLvl);
        RayOut propDataOutTest = new RayOut(true, attData, rayData);
        computeRays.run(propDataOutTest);

        // Merge levels for each receiver for point sources
        Map<Long, double[]> levelsPerReceiver = new HashMap<>();
        for(ComputeRaysOut.verticeSL lvl : propDataOut.receiversAttenuationLevels) {
            if(!levelsPerReceiver.containsKey(lvl.receiverId)) {
                levelsPerReceiver.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiver.put(lvl.receiverId, ComputeRays.sumDbArray(levelsPerReceiver.get(lvl.receiverId),
                        lvl.value));
            }
        }


        // Merge levels for each receiver for lines sources
        Map<Long, double[]> levelsPerReceiverLines = new HashMap<>();
        for(ComputeRaysOut.verticeSL lvl : propDataOutTest.receiversAttenuationLevels) {
            if(!levelsPerReceiverLines.containsKey(lvl.receiverId)) {
                levelsPerReceiverLines.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiverLines.put(lvl.receiverId, ComputeRays.sumDbArray(levelsPerReceiverLines.get(lvl.receiverId),
                        lvl.value));
            }
        }

        assertEquals(6, levelsPerReceiverLines.size());
        assertEquals(6, levelsPerReceiver.size());

//        KMLDocument.exportScene("target/testSourceLines.kml", manager, propDataOutTest);
//        KMLDocument.exportScene("target/testSourceLinesPt.kml", manager, propDataOut);
//        // Uncomment for printing maximum error
//        for(int i = 0; i < levelsPerReceiver.size(); i++) {
//            LOGGER.info(String.format("%d error %.2f", i,  getMaxError(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i))));
//        }

        for(int i = 0; i < levelsPerReceiver.size(); i++) {
            assertArrayEquals(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i), 0.2);
        }
    }

    private static double getMaxError(double[] ref, double[] result) {
        assertEquals(ref.length, result.length);
        double max = Double.MIN_VALUE;
        for(int i=0; i < ref.length; i++) {
            max = Math.max(max, Math.abs(ref[i] - result[i]));
        }
        return max;
    }

    private static class RayOut extends ComputeRaysOut {
        private DirectPropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, DirectPropagationProcessData processData) {
            super(keepRays, pathData);
            this.processData = processData;
        }

        @Override
        public double[] doAddPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] attenuation = super.doAddPropagationPaths(sourceId, sourceLi, receiverId, propagationPath);
            double[] soundLevel = ComputeRays.wToDba(ComputeRays.multArray(processData.wjSources.get((int)sourceId), ComputeRays.dbaToW(attenuation)));
            return soundLevel;
        }
    }

    private static class DirectPropagationProcessData extends PropagationProcessData {
        private List<double[]> wjSources = new ArrayList<>();

        public DirectPropagationProcessData(FastObstructionTest freeFieldFinder) {
            super(freeFieldFinder);
        }

        public void addSource(Geometry geom, double[] spectrum) {
            super.addSource(geom);
            wjSources.add(spectrum);
        }

        public void clearSources() {
            wjSources.clear();
            sourceGeometries.clear();
            sourcesIndex = new QueryRTree();
        }

        @Override
        public double[] getMaximalSourcePower(int sourceId) {
            return wjSources.get(sourceId);
        }
    }
}