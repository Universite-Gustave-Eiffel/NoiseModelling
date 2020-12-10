package org.noise_planet.noisemodelling.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.Densifier3D;
import org.noise_planet.noisemodelling.pathfinder.utils.GeoJSONDocument;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.noise_planet.noisemodelling.jdbc.Utils.addArray;


public class TestComputeRaysFull {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestComputeRaysFull.class);
    private boolean storeGeoJSONRays = false;


     /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01()  throws LayerDelaunayError, IOException {
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
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{39.95,39.89,39.77,39.60,39.26,38.09,33.61,17.27},L, 0.3);
    }

    /**
     * Test TC02 -- Mixed ground (G = 0.5)
     */
    @Test
    public void TC02()  throws LayerDelaunayError , IOException {
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
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -20, 80)), 0.5));
        rayData.setComputeVerticalDiffraction(true);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{38.07,38.01,37.89,36.79,34.29,36.21,31.73,15.39},L, 0.3);
    }

    /**
     * Test TC03 -- Porous ground (G = 1)
     */
    @Test
    public void TC03()  throws LayerDelaunayError , IOException {
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
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -20, 80)), 1));
        rayData.setComputeVerticalDiffraction(true);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{36.21,36.16,35.31,29.71,33.70,34.36,29.87,13.54},L, 0.3);
    }

    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07()  throws LayerDelaunayError , IOException {
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
        attData.setWindRose(new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5});
        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{32.70,31.58,29.99,27.89,24.36,21.46,14.18,-5.05},L, 3);

    }
//    /**
//     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
//     * This test
//     */
//    @Test
//    public void TC06()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 11.5));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T06.geojson", propDataOut);
//            exportScene("target/T06.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T06.geojson"), propDataOut);
//        }
//
//
//    }
//
//    /**
//     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
//     */
//    @Test
//    public void TC07()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 240, 0),
//                new Coordinate(100.1, 240, 0),
//                new Coordinate(265.1, -180, 0),
//                new Coordinate(265, -180, 0),
//                new Coordinate(100, 240, 0)}), 6);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T07.geojson", propDataOut);
//            exportScene("target/T07.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T07.geojson"), propDataOut);
//        }
//
//
//    }
//
//    /**
//     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short
//     * barrier
//     */
//    public void TC09() throws LayerDelaunayError {
//        // Impossible shape for NoiseModelling
//    }
//
//
//    /**
//     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
//     */
//    @Test
//    public void TC08()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 50, 0),
//                new Coordinate(175.01, 50, 0),
//                new Coordinate(190.01, 10, 0),
//                new Coordinate(190, 10, 0),
//                new Coordinate(175, 50, 0)}), 6);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T08.geojson", propDataOut);
//            exportScene("target/T08.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T08.geojson"), propDataOut);
//        }
//
//
//    }
//
//    /**
//     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at low height
//     */
//    @Test
//    public void TC10()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(55, 5, 0),
//                new Coordinate(65, 5, 0),
//                new Coordinate(65, 15, 0),
//                new Coordinate(55, 15, 0),
//                new Coordinate(55, 5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(70, 10, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T10.geojson", propDataOut);
//            exportScene("target/T10.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T10.geojson"), propDataOut);
//        }
//
//
//    }
//    /**
//     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at large height
//     */
//    @Test
//    public void TC11() throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(55, 5,0),
//                new Coordinate(65, 5,0),
//                new Coordinate(65, 15,0),
//                new Coordinate(55, 15,0),
//                new Coordinate(55, 5,0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(70, 10, 15));
//        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T11.geojson", propDataOut);
//            exportScene("target/T11.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T11.geojson"), propDataOut);
//        }
//
//
//    }
//
//    /**
//     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at low height
//     */
//    @Test
//    public void TC12() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(11., 15.5, 0),
//                new Coordinate(12., 13, 0),
//                new Coordinate(14.5, 12, 0),
//                new Coordinate(17.0, 13, 0),
//                new Coordinate(18.0, 15.5, 0),
//                new Coordinate(17.0, 18, 0),
//                new Coordinate(14.5, 19, 0),
//                new Coordinate(12.0, 18, 0),
//                new Coordinate(11, 15.5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(30, 20, 6));
//        rayData.addSource(factory.createPoint(new Coordinate(0, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.5));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T12.geojson", propDataOut);
//            exportScene("target/T12.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T12.geojson"), propDataOut);
//        }
//
//    }
//
//    /**
//     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
//     * building
//     */
//    @Test
//    public void TC13() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(169.4, 41.0, 0),
//                new Coordinate(172.5, 33.5, 0),
//                new Coordinate(180.0, 30.4, 0),
//                new Coordinate(187.5, 33.5, 0),
//                new Coordinate(190.6, 41.0, 0),
//                new Coordinate(187.5, 48.5, 0),
//                new Coordinate(180.0, 51.6, 0),
//                new Coordinate(172.5, 48.5, 0),
//                new Coordinate(169.4, 41.0, 0)}), 30);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 28.5));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T13.geojson", propDataOut);
//            exportScene("target/T13.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T13.geojson"), propDataOut);
//        }
//    }
//    /**
//     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at large height
//     */
//    @Test
//    public void TC14() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(11., 15.5, 0),
//                new Coordinate(12., 13, 0),
//                new Coordinate(14.5, 12, 0),
//                new Coordinate(17.0, 13, 0),
//                new Coordinate(18.0, 15.5, 0),
//                new Coordinate(17.0, 18, 0),
//                new Coordinate(14.5, 19, 0),
//                new Coordinate(12.0, 18, 0),
//                new Coordinate(11, 15.5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(25, 20, 23));
//        rayData.addSource(factory.createPoint(new Coordinate(8, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T14.geojson", propDataOut);
//            exportScene("target/T14.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T14.geojson"), propDataOut);
//        }
//    }
//    /**
//     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
//     */
//    @Test
//    public void TC15() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(55.0, 5.0, 0),
//                new Coordinate(65.0, 5.0, 0),
//                new Coordinate(65.0, 15.0, 0),
//                new Coordinate(55.0, 15.0, 0),
//                new Coordinate(55.0, 5.0, 0)}), 8);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(70, 14.5, 0),
//                new Coordinate(80.0, 10.2, 0),
//                new Coordinate(80.0, 20.2, 0),
//                new Coordinate(70, 14.5, 0)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(90.1, 19.5, 0),
//                new Coordinate(93.3, 17.8, 0),
//                new Coordinate(87.3, 6.6, 0),
//                new Coordinate(84.1, 8.3, 0),
//                new Coordinate(90.1, 19.5, 0)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(94.9, 14.1, 0),
//                new Coordinate(98.02, 12.37, 0),
//                new Coordinate(92.03, 1.2, 0),
//                new Coordinate(88.86, 2.9, 0),
//                new Coordinate(94.9, 14.1, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(100, 15, 5));
//        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.setComputeVerticalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T15.geojson", propDataOut);
//            exportScene("target/T15.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T15.geojson"), propDataOut);
//        }
//    }
//
//
//
//    /**
//     * Reflecting barrier on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC16() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 14));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//        if(storeGeoJSONRays) {
//            exportRays("target/T16.geojson", propDataOut);
//            exportScene("target/T16.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T16.geojson"), propDataOut);
//        }
//    }
//
//
//    /**
//     * Reflecting two barrier on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC16b() throws LayerDelaunayError, IOException  {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 20);
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 12, 0),
//                new Coordinate(170, 30, 0),
//                new Coordinate(170, 32, 0),
//                new Coordinate(114, 14, 0),
//                new Coordinate(114, 12, 0)}), 20);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 15));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T16b.geojson", propDataOut);
//            exportScene("target/T16b.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T16b.geojson"), propDataOut);
//        }
//
//    }
//
//
//    /**
//     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
//     * reduced receiver height
//     */
//    @Test
//    public void TC17() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 11.5));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T17.geojson", propDataOut);
//            exportScene("target/T17.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T17.geojson"), propDataOut);
//        }
//    }
//
//
//    /**
//     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC18() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52),
//                new Coordinate(170, 60),
//                new Coordinate(170, 61),
//                new Coordinate(114, 53),
//                new Coordinate(114, 52)}), 15);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(87, 50),
//                new Coordinate(92, 32),
//                new Coordinate(92, 33),
//                new Coordinate(87, 51),
//                new Coordinate(87, 50)}), 12);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 12));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T18.geojson", propDataOut);
//            exportScene("target/T18.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T18.geojson"), propDataOut);
//        }
//
//    }
//
//    /**
//     * TC18b - Screening and reflecting barrier on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC18b() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52),
//                new Coordinate(170, 60),
//                new Coordinate(170, 61),
//                new Coordinate(114, 53),
//                new Coordinate(114, 52)}), 15);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(87, 50),
//                new Coordinate(92, 32),
//                new Coordinate(92, 33),
//                new Coordinate(87, 51),
//                new Coordinate(87, 50)}), 12);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 50, 12+ manager.getHeightAtPosition(new Coordinate(200, 50, 12))));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T18b.geojson", propDataOut);
//            exportScene("target/T18b.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T18b.geojson"), propDataOut);
//        }
//
//    }
//
//
//
//     /**
//     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC19() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(156, 28),
//                new Coordinate(145, 7),
//                new Coordinate(145, 8),
//                new Coordinate(156, 29),
//                new Coordinate(156, 28)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 35),
//                new Coordinate(188, 19),
//                new Coordinate(188, 20),
//                new Coordinate(175, 36),
//                new Coordinate(175, 35)}), 14.5);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 24),
//                new Coordinate(118, 24),
//                new Coordinate(118, 30),
//                new Coordinate(100, 30),
//                new Coordinate(100, 24)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 15.1),
//                new Coordinate(118, 15.1),
//                new Coordinate(118, 23.9),
//                new Coordinate(100, 23.9),
//                new Coordinate(100, 15.1)}), 7);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 9),
//                new Coordinate(118, 9),
//                new Coordinate(118, 15),
//                new Coordinate(100, 15),
//                new Coordinate(100, 9)}), 12);
//
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(200, 30, 14));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T19.geojson", propDataOut);
//            exportScene("target/T19.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T19.geojson"), propDataOut);
//        }
//    }
//
//
//    /**
//     * TC21 - Building on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC21() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(167.2, 39.5),
//                new Coordinate(151.6, 48.5),
//                new Coordinate(141.1, 30.3),
//                new Coordinate(156.7, 21.3),
//                new Coordinate(159.7, 26.5),
//                new Coordinate(151.0, 31.5),
//                new Coordinate(155.5, 39.3),
//                new Coordinate(164.2, 34.3),
//                new Coordinate(167.2, 39.5)}), 11.5);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(187.05, 25, 14));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T21.geojson", propDataOut);
//            exportScene("target/T21.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T21.geojson"), propDataOut);
//        }
//
//    }
//
//
//    /**
//     * TC22 - Building with receiver backside on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC22() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(197, 36.0, 0),
//                new Coordinate(179, 36, 0),
//                new Coordinate(179, 15, 0),
//                new Coordinate(197, 15, 0),
//                new Coordinate(197, 21, 0),
//                new Coordinate(187, 21, 0),
//                new Coordinate(187, 30, 0),
//                new Coordinate(197, 30, 0),
//                new Coordinate(197, 36, 0)}), 20);
//
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(187.05, 25, 14));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T22.geojson", propDataOut);
//            exportScene("target/T22.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T22.geojson"), propDataOut);
//        }
//
//    }
//
//
//    /**
//     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
//     * properties
//     */
//    @Test
//    public void TC23() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(75, 34, 0),
//                new Coordinate(110, 34, 0),
//                new Coordinate(110, 26, 0),
//                new Coordinate(75, 26, 0),
//                new Coordinate(75, 34, 0)}), 9);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(83, 18, 0),
//                new Coordinate(118, 18, 0),
//                new Coordinate(118, 10, 0),
//                new Coordinate(83, 10, 0),
//                new Coordinate(83, 18, 0)}), 8);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(59.6, -9.87, 0));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 0));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 0));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 0));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 0));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 5));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 5));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 0));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 5));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 5));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 5));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.39, 5));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 0));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 0));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 0));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 0));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 5));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 5));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 0));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 5));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 5));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 0));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.93, 5));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 5));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(107, 25.95, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -100, 100)), 0.));
//
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T23.geojson", propDataOut);
//            exportScene("target/T23.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T23.geojson"), propDataOut);
//        }
//
//    }
//
//    /**
//     * TC28 Propagation over a large distance with many buildings between source and
//     * receiver
//     */
//    @Test
//    public void TC28() throws LayerDelaunayError, IOException {
//        double upKml = 100.;
//        GeometryFactory factory = new GeometryFactory();
//
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500, 1500, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(113, 10, 0+upKml),
//                new Coordinate(127, 16, 0+upKml),
//                new Coordinate(102, 70, 0+upKml),
//                new Coordinate(88, 64, 0+upKml),
//                new Coordinate(113, 10, 0+upKml)}), 6);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(176, 19, 0+upKml),
//                new Coordinate(164, 88, 0+upKml),
//                new Coordinate(184, 91, 0+upKml),
//                new Coordinate(196, 22, 0+upKml),
//                new Coordinate(176, 19, 0+upKml)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(250, 70, 0+upKml),
//                new Coordinate(250, 180, 0+upKml),
//                new Coordinate(270, 180, 0+upKml),
//                new Coordinate(270, 70, 0+upKml),
//                new Coordinate(250, 70, 0+upKml)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(332, 32, 0+upKml),
//                new Coordinate(348, 126, 0+upKml),
//                new Coordinate(361, 108, 0+upKml),
//                new Coordinate(349, 44, 0+upKml),
//                new Coordinate(332, 32, 0+upKml)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(400, 5, 0+upKml),
//                new Coordinate(400, 85, 0+upKml),
//                new Coordinate(415, 85, 0+upKml),
//                new Coordinate(415, 5, 0+upKml),
//                new Coordinate(400, 5, 0+upKml)}), 9);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(444, 47, 0+upKml),
//                new Coordinate(436, 136, 0+upKml),
//                new Coordinate(516, 143, 0+upKml),
//                new Coordinate(521, 89, 0+upKml),
//                new Coordinate(506, 87, 0+upKml),
//                new Coordinate(502, 127, 0+upKml),
//                new Coordinate(452, 123, 0+upKml),
//                new Coordinate(459, 48, 0+upKml),
//                new Coordinate(444, 47, 0+upKml)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(773, 12, 0+upKml),
//                new Coordinate(728, 90, 0+upKml),
//                new Coordinate(741, 98, 0+upKml),
//                new Coordinate(786, 20, 0+upKml),
//                new Coordinate(773, 12, 0+upKml)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(972, 82, 0+upKml),
//                new Coordinate(979, 121, 0+upKml),
//                new Coordinate(993, 118, 0+upKml),
//                new Coordinate(986, 79, 0+upKml),
//                new Coordinate(972, 82, 0+upKml)}), 8);
//
//        //x2
//        mesh.addTopographicPoint(new Coordinate(-1300, -1300, 0+upKml));
//        mesh.addTopographicPoint(new Coordinate(1300, 1300, 0+upKml));
//        mesh.addTopographicPoint(new Coordinate(-1300, 1300, 0+upKml));
//        mesh.addTopographicPoint(new Coordinate(1300, -1300, 0+upKml));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        PropagationProcessData rayData = new PropagationProcessData(manager);
//        rayData.addReceiver(new Coordinate(1000, 100, 1+upKml));
//        rayData.addSource(factory.createPoint(new Coordinate(0, 50, 4+upKml)));
//        rayData.setComputeHorizontalDiffraction(true);
//        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));
//        rayData.maxSrcDist = 1500;
//        rayData.setComputeVerticalDiffraction(true);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        if(storeGeoJSONRays) {
//            exportRays("target/T28.geojson", propDataOut);
//            exportScene("target/T28.kml", manager, propDataOut);
//        } else {
//            assertRaysEquals(TestComputeRaysFull.class.getResourceAsStream("T28.geojson"), propDataOut);
//        }
//
//
//
//    }


    private void exportRays(String name, ComputeRaysOut result) throws IOException {
        FileOutputStream outData = new FileOutputStream(name);
        GeoJSONDocument jsonDocument = new GeoJSONDocument(outData);
        jsonDocument.setRounding(1);
        jsonDocument.writeHeader();
        for(PropagationPath propagationPath : result.getPropagationPaths()) {
            jsonDocument.writeRay(propagationPath);
        }
        jsonDocument.writeFooter();
    }

    private void exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
        try {
            Coordinate proj = new Coordinate( 351714.794877, 6685824.856402, 0);
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
            kmlDocument.setOffset(proj);
            kmlDocument.writeHeader();
            if(manager != null) {
                kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices());
            }
            if(result != null) {
                kmlDocument.writeRays(result.getPropagationPaths());
            }
            if(manager != null && manager.isHasBuildingWithHeight()) {
                kmlDocument.writeBuildings(manager);
            }
            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }

    private void assertRaysEquals(InputStream expected, ComputeRaysOut result) throws IOException {
        // Parse expected
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(expected);
        // Generate result
        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        GeoJSONDocument jsonDocument = new GeoJSONDocument(outData);
        jsonDocument.setRounding(1);
        jsonDocument.writeHeader();
        for(PropagationPath propagationPath : result.getPropagationPaths()) {
            jsonDocument.writeRay(propagationPath);
        }
        jsonDocument.writeFooter();
        JsonNode resultNode = mapper.readTree(outData.toString());
        // Check equality
        assertEquals(rootNode, resultNode);
    }

    private static Geometry addGround(MeshBuilder mesh) throws IOException {
        List<LineSegment> lineSegments = new ArrayList<>();
        lineSegments.add(new LineSegment(new Coordinate(0, 80, 0), new Coordinate(225, 80, 0)));
        lineSegments.add(new LineSegment(new Coordinate(225, 80, 0), new Coordinate(225, -20, 0)));
        lineSegments.add(new LineSegment(new Coordinate(225, -20, 0 ), new Coordinate(0, -20, 0)));
        lineSegments.add(new LineSegment(new Coordinate(0, -20, 0), new Coordinate(0, 80, 0)));
        lineSegments.add(new LineSegment(new Coordinate(120, -20, 0), new Coordinate(120, 80, 0)));
        lineSegments.add(new LineSegment(new Coordinate(185, -15, 10), new Coordinate(205, -15, 10)));
        lineSegments.add(new LineSegment(new Coordinate(205,-15, 10), new Coordinate(205, 75, 10)));
        lineSegments.add(new LineSegment(new Coordinate(205, 75, 10), new Coordinate(185, 75, 10)));
        lineSegments.add(new LineSegment(new Coordinate(185, 75, 10), new Coordinate(185, -15, 10)));
        lineSegments.add(new LineSegment(new Coordinate(120, 80, 0), new Coordinate(185, 75, 10)));
        lineSegments.add(new LineSegment(new Coordinate(120,-20 ,0), new Coordinate(185, -15, 10)));
        lineSegments.add(new LineSegment(new Coordinate(205, 75, 10), new Coordinate(225, 80, 0)));
        lineSegments.add(new LineSegment(new Coordinate(205, -15, 10), new Coordinate(225, -20, 0)));

        GeometryFactory factory = new GeometryFactory();
        LineString[] segments = new LineString[lineSegments.size()];
        int i = 0;
        for(LineSegment segment : lineSegments) {
            segments[i++] = factory.createLineString(new Coordinate[]{segment.p0, segment.p1});
        }
        Geometry geo = factory.createMultiLineString(segments);
        geo = geo.union();
        geo = Densifier3D.densify(geo, 4);
        for(Coordinate pt : geo.getCoordinates()) {
            mesh.addTopographicPoint(pt);
        }
//        for(int idGeo = 0; idGeo < geo.getNumGeometries(); idGeo++) {
//            Geometry line = geo.getGeometryN(idGeo);
//            if(line instanceof LineString) {
//                mesh.addTopographicLine((LineString)line);
//            }
//        }
        return geo;
        /*
        MCIndexNoder mCIndexNoder = new MCIndexNoder();
        mCIndexNoder.setSegmentIntersector(new IntersectionAdder(new RobustLineIntersector()));
        List<SegmentString> nodes = new ArrayList<>();
        for(LineSegment segment : lineSegments) {
            nodes.add(new NodedSegmentString(new Coordinate[]{segment.p0, segment.p1}, 1));
        }
        mCIndexNoder.computeNodes(nodes);
        Collection nodedSubstring = mCIndexNoder.getNodedSubstrings();
        for(Object ob: nodedSubstring) {
            if(ob instanceof SegmentString) {
                SegmentString seg = (SegmentString)ob;
                mesh.addTopographicLine(factory.createLineString(seg.getCoordinates()));
            }
        }
        */
    }



    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */

    public void TC20() throws LayerDelaunayError {
        //Tables 221 – 222 are not shown in this draft.

        assertEquals(false, true);
    }


    /**
     * TC24 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties – receiver position modified
     */
    public void TC24() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC25 – Replacement of the earth-berm by a barrier
     */
    public void TC25() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC26 – Road source with influence of retrodiffraction
     */

    public void TC26() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC27 Source located in flat cut with retro-diffraction
     */
    public void TC27() throws LayerDelaunayError {

        assertEquals(true, false);

    }


}