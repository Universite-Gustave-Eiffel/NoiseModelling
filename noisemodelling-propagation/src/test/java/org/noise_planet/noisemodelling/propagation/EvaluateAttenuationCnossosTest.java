package org.noise_planet.noisemodelling.propagation;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

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
        rayData.setHumidity(70);
        rayData.setTemperature(10);
        PropagationProcessPathData attData = new PropagationProcessPathData();
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
        rayData.setHumidity(70);
        rayData.setTemperature(10);
        PropagationProcessPathData attData = new PropagationProcessPathData();
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

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(0, 0, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), ComputeRays.dbaToW(84));
        rayData.addSource(factory.createPoint(new Coordinate(1100, 1100, 1)), ComputeRays.dbaToW(84));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setHumidity(70);
        rayData.setTemperature(10);
        rayData.maxSrcDist = 2000;
        rayData.maximumError = 3; // 3 dB error max

        PropagationProcessPathData attData = new PropagationProcessPathData();
        ComputeRaysOut propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        // Second source has not been computed because at best it would only increase the received level of only 0.00042 dB
        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
    }



    private static class RayOut extends ComputeRaysOut {
        private PropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, PropagationProcessData processData) {
            super(keepRays, pathData);
            this.processData = processData;
        }

        @Override
        public double addPropagationPaths(int sourceId, int receiverId, List<PropagationPath> propagationPath) {
            double global_attenuation = super.addPropagationPaths(sourceId, receiverId, propagationPath);
            return processData.wj_sources.get(sourceId) * global_attenuation;
        }
    }
}