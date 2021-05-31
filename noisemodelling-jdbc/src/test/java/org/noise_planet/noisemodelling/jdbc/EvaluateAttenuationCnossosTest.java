package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.EvaluateAttenuationCnossos;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.aWeighting;
import static org.noise_planet.noisemodelling.jdbc.Utils.addArray;
import static org.noise_planet.noisemodelling.pathfinder.ComputeRays.dbaToW;

public class EvaluateAttenuationCnossosTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateAttenuationCnossosTest.class);
    // TODO reduce error epsilon
    private static final double ERROR_EPSILON_high = 3;
    private static final double ERROR_EPSILON_very_high = 15;
    private static final double ERROR_EPSILON_medium = 1;
    private static final double ERROR_EPSILON_low = 0.5;
    private static final double ERROR_EPSILON_very_low = 0.2;

    private static final double[] HOM_WIND_ROSE = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] FAV_WIND_ROSE = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};



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

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{39.95,39.89,39.77,39.60,39.26,38.09,33.61,17.27},L, ERROR_EPSILON_very_low);
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
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{38.07,38.01,37.89,36.79,34.29,36.21,31.73,15.39},L, ERROR_EPSILON_very_low);
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
        rayData.setGs(1.0);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{36.21,36.16,35.31,29.71,33.70,34.36,29.87,13.54},L, ERROR_EPSILON_very_low);
    }
    
    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     */
    @Test
    public void TC04()  throws LayerDelaunayError , IOException {
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
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.2));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.9));
        rayData.setGs(0.2);

        rayData.setComputeVerticalDiffraction(true);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        //attData.setWindRose(FAV_WIND_ROSE);

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{37.91,37.85,37.73,36.37,34.23,36.06,31.57,15.24},L, ERROR_EPSILON_very_low); // p=0.5
     }


    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{37.26,37.21,37.08,36.91,36.57,35.41,30.91,14.54},L, ERROR_EPSILON_very_low); // p=0.5

    }


    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 11.5));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));

        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{37.53,37.47,37.33,34.99,36.60,35.67,31.18,14.82},L, ERROR_EPSILON_low); // p=0.5

    }

    /**
     * Test TC07h -- Flat ground with spatially varying acoustic properties and long barrier - METEO HOM
     */
    @Test
    public void TC07h()  throws LayerDelaunayError , IOException {
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
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setWindRose(HOM_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals( new double[]{32.54,31.32,29.60,27.37,22.22,20.76,13.44,-5.81},L, ERROR_EPSILON_very_low);//HOM
    }

    /**
     * Test TC07f -- Flat ground with spatially varying acoustic properties and long barrier -  METEO FAV
     */
    @Test
    public void TC07f()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.001, 240, 0),
                new Coordinate(265.001, -180, 0),
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
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setWindRose(FAV_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
         assertArrayEquals(  new double[]{32.85,31.83,30.35,28.36,25.78,22.06,14.81,-4.41},L, ERROR_EPSILON_very_low);//FAV

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
                new Coordinate(100.001, 240, 0),
                new Coordinate(265.001, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        //attData.setWindRose(FAV_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{32.70,31.58,29.99,27.89,24.36,21.46,14.18,-5.05},L, ERROR_EPSILON_very_low);//p=0.5

    }

    /**
     * Test TC08_vp -- Flat ground with spatially varying acoustic properties and short barrier - vertical plane
     */
    @Test
    public void TC08_vp()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{6.49,15.47,21.37,24.67,24.32,22.62,15.14,-6.19},L, ERROR_EPSILON_very_low);//p=0.5
    }
//
//    /**
//     * Test TC08_lph -- Flat ground with spatially varying acoustic properties and short barrier - lateral paths (homogeneous)
//     */
//    @Test
//    public void TC08_lph()  throws LayerDelaunayError , IOException {
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
//        rayData.setComputeVerticalDiffraction(false);
//        rayData.setGs(0.9);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//        attData.setWindRose(HOM_WIND_ROSE);
//        attData.setPrime2520(true);
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
//        assertArrayEquals(  new double[]{8.17,16.86,22.51,25.46,24.87,23.44,15.93,-5.43},L, ERROR_EPSILON_low);//p=0.5
//       // double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
//       // assertArrayEquals(  new double[]{27.91,25.83,23.28,17.92,9.92,13.14,5.68,-13.7},L, ERROR_EPSILON_low);//p=0.5
//        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions
//    }
//
//    /**
//     * Test TC08_lpf -- Flat ground with spatially varying acoustic properties and short barrier - lateral paths (favorable)
//     */
//    @Test
//    public void TC08_lpf()  throws LayerDelaunayError , IOException {
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
//        rayData.setComputeVerticalDiffraction(false);
//        rayData.setGs(0.9);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//        attData.setWindRose(FAV_WIND_ROSE);
//        attData.setPrime2520(true);
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
//        assertArrayEquals(new double[]{28.59,26.51,23.96,21.09,16.68,12.82,6.36,-12.02},L, ERROR_EPSILON_medium);//p=0.5
//        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions
//
//    }



    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6);

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
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{8.17,16.86,22.51,25.46,24.87,23.44,15.93,-5.43},L, ERROR_EPSILON_very_low);//p=0.5
        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions

    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short barrier
     */
    @Test
    public void TC09()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6.63);

        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        // impossible geometry in NoiseModelling
        assertArrayEquals(  new double[]{6.41,14.50,19.52,22.09,22.16,19.28,11.62,-9.31},L, ERROR_EPSILON_high);//p=0.5
    }


    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at low height
     */
    @Test
    public void TC10()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5, 0),
                new Coordinate(65, 5, 0),
                new Coordinate(65, 15, 0),
                new Coordinate(55, 15, 0),
                new Coordinate(55, 5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(70, 10, 4));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 100, -100, 100)), 0.5));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{46.09,42.49,38.44,35.97,34.67,33.90,33.09,31.20},L, ERROR_EPSILON_very_low);//p=0.5
    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at large height
     */
    @Test
    public void TC11() throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5,0),
                new Coordinate(65, 5,0),
                new Coordinate(65, 15,0),
                new Coordinate(55, 15,0),
                new Coordinate(55, 5,0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(70, 10, 15));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.5));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{21.28,28.39,32.47,34.51,34.54,33.37,32.14,27.73},L, ERROR_EPSILON_low);//p=0.5

    }


    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at low height
     */
    @Test
    public void TC12() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(30, 20, 6));
        rayData.addSource(factory.createPoint(new Coordinate(0, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.5));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{21.81,29.66,34.31,36.14,35.57,33.72,31.12,25.37},L, ERROR_EPSILON_very_low);//p=0.5

    }

    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
     * building
     */
    @Test
    public void TC13() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(169.4, 41.0, 0),
                new Coordinate(172.5, 33.5, 0),
                new Coordinate(180.0, 30.4, 0),
                new Coordinate(187.5, 33.5, 0),
                new Coordinate(190.6, 41.0, 0),
                new Coordinate(187.5, 48.5, 0),
                new Coordinate(180.0, 51.6, 0),
                new Coordinate(172.5, 48.5, 0),
                new Coordinate(169.4, 41.0, 0)}), 20);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 28.5));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
       // attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{5.14,12.29,16.39,18.47,18.31,15.97,9.72,-9.92},L, ERROR_EPSILON_high);//p=0.5

    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(25, 20, 23));
        rayData.addSource(factory.createPoint(new Coordinate(8, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.2);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{25.61,34.06,39.39,42.04,41.86,39.42,35.26,27.57},L, ERROR_EPSILON_very_low);//p=0.5
    }

    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55.0, 5.0, 0),
                new Coordinate(65.0, 5.0, 0),
                new Coordinate(65.0, 15.0, 0),
                new Coordinate(55.0, 15.0, 0),
                new Coordinate(55.0, 5.0, 0)}), 8);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(70, 14.5, 0),
                new Coordinate(80.0, 10.2, 0),
                new Coordinate(80.0, 20.2, 0),
                new Coordinate(70, 14.5, 0)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(90.1, 19.5, 0),
                new Coordinate(93.3, 17.8, 0),
                new Coordinate(87.3, 6.6, 0),
                new Coordinate(84.1, 8.3, 0),
                new Coordinate(90.1, 19.5, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(94.9, 14.1, 0),
                new Coordinate(98.02, 12.37, 0),
                new Coordinate(92.03, 1.2, 0),
                new Coordinate(88.86, 2.9, 0),
                new Coordinate(94.9, 14.1, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(100, 15, 5));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));

        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{10.75,16.57,20.81,24.51,26.55,26.78,25.04,18.50},L, ERROR_EPSILON_medium);
    }

    /**
     * Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15,new double[]{0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.5});


        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{13.62,23.58,30.71,35.68,38.27,38.01,32.98,15.00},L, ERROR_EPSILON_high);//p=0.5
    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
     * reduced receiver height
     */
    @Test
    public void TC17() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15, new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5});

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 11.5));

        PropagationProcessPathData attData = new PropagationProcessPathData();
        // Push source with sound level
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), dbaToW(aWeighting(Collections.nCopies(attData.freq_lvl.size(), 93d))));

        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertArrayEquals(  new double[]{14.02,23.84,30.95,33.86,38.37,38.27,33.25,15.28}, propDataOut.getVerticesSoundLevel().get(0).value, ERROR_EPSILON_medium);//p=0.5
    }


    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC18() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52),
                new Coordinate(170, 60),
                new Coordinate(170, 61),
                new Coordinate(114, 53),
                new Coordinate(114, 52)}), 15);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(87, 50),
                new Coordinate(92, 32),
                new Coordinate(92, 33),
                new Coordinate(87, 51),
                new Coordinate(87, 50)}), 12);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 12));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{11.69,21.77,28.93,32.71,36.83,36.83,32.12,13.66},L, ERROR_EPSILON_low);//p=0.5


    }


    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC19() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(156, 28),
                new Coordinate(145, 7),
                new Coordinate(145, 8),
                new Coordinate(156, 29),
                new Coordinate(156, 28)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 35),
                new Coordinate(188, 19),
                new Coordinate(188, 20),
                new Coordinate(175, 36),
                new Coordinate(175, 35)}), 14.5);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 24),
                new Coordinate(118, 24),
                new Coordinate(118, 30),
                new Coordinate(100, 30),
                new Coordinate(100, 24)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 15.1),
                new Coordinate(118, 15.1),
                new Coordinate(118, 23.9),
                new Coordinate(100, 23.9),
                new Coordinate(100, 15.1)}), 7);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 9),
                new Coordinate(118, 9),
                new Coordinate(118, 15),
                new Coordinate(100, 15),
                new Coordinate(100, 9)}), 12);


        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 30, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{6.72,14.66,19.34,21.58,21.84,19.00,11.42,-9.38},L, ERROR_EPSILON_very_high);//p=0.5
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC20() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 0);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        rayData.setComputeVerticalDiffraction(false);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{11.21,21.25,28.63,33.86,36.73,36.79,32.17,14},L, ERROR_EPSILON_very_low);//p=0.5
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC21() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 11.5);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));

        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setReflexionOrder(0);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{10.44,20.58,27.78,33.09,35.84,35.73,30.91,12.48},L, ERROR_EPSILON_very_high);// Because building height definition is not in accordance with ISO

    }

    /**
     * TC22 - Building with receiver backside on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC22() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(197, 36.0, 0),
                new Coordinate(179, 36, 0),
                new Coordinate(179, 15, 0),
                new Coordinate(197, 15, 0),
                new Coordinate(197, 21, 0),
                new Coordinate(187, 21, 0),
                new Coordinate(187, 30, 0),
                new Coordinate(197, 30, 0),
                new Coordinate(197, 36, 0)}), 20);


        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(187.05, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));

        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{-2.96,3.56,6.73,11.17,13.85,13.86,9.48,-7.64},L, ERROR_EPSILON_very_high); //because we don't take into account this rays

    }


    /**
     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties
     */
    @Test
    public void TC23() throws LayerDelaunayError, IOException {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9, buildingsAbs);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8, buildingsAbs);

        // Ground Surface

        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(30, -14, 0), // 1
                new Coordinate(122, -14, 0),// 1 - 2
                new Coordinate(122, 45, 0), // 2 - 3
                new Coordinate(30, 45, 0),  // 3 - 4
                new Coordinate(30, -14, 0) // 4
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(46.27, 36.28, 0), // 9
                new Coordinate(54.68, 37.59, 5), // 9-10
                new Coordinate(55.93, 37.93, 5), // 10-11
                new Coordinate(63.71, 41.16, 0) // 11
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 12
                new Coordinate(67.35, -6.83, 5), // 12-13
                new Coordinate(68.68, -6.49, 5), // 13-14
                new Coordinate(76.84, -5.28, 0) // 14
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(54.68, 37.59, 5), //15
                new Coordinate(67.35, -6.83, 5)
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(55.93, 37.93, 5), //16
                new Coordinate(68.68, -6.49, 5)
        }));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(107, 25.95, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        // Create porous surface as defined by the test:
        // The surface of the earth berm is porous (G = 1).
        rayData.addSoilType(new GeoWithSoilType(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }), 1.));

        rayData.setComputeVerticalDiffraction(true);
        rayData.setReflexionOrder(0);

        rayData.setGs(0.);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        //KMLDocument.exportScene("target/tc23.kml", manager, propDataOut);
        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93 - 26.2, 93 - 16.1,
                93 - 8.6, 93 - 3.2, 93, 93 + 1.2, 93 + 1.0, 93 - 1.1});
        assertArrayEquals(new double[]{12.7, 21.07, 27.66, 31.48, 31.42, 28.74, 23.75, 13.92}, L, ERROR_EPSILON_high);//p=0.5

    }

    /**
     * – Two buildings behind an earth-berm on flat ground with homogeneous acoustic properties – receiver position modified
     * @throws LayerDelaunayError
     * @throws IOException
     */
    @Test
    public void TC24() throws LayerDelaunayError, IOException {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9, buildingsAbs);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8, buildingsAbs);

        // Ground Surface

        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(30, -14, 0), // 1
                new Coordinate(122, -14, 0),// 1 - 2
                new Coordinate(122, 45, 0), // 2 - 3
                new Coordinate(30, 45, 0),  // 3 - 4
                new Coordinate(30, -14, 0) // 4
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(46.27, 36.28, 0), // 9
                new Coordinate(54.68, 37.59, 5), // 9-10
                new Coordinate(55.93, 37.93, 5), // 10-11
                new Coordinate(63.71, 41.16, 0) // 11
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 12
                new Coordinate(67.35, -6.83, 5), // 12-13
                new Coordinate(68.68, -6.49, 5), // 13-14
                new Coordinate(76.84, -5.28, 0) // 14
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(54.68, 37.59, 5), //15
                new Coordinate(67.35, -6.83, 5)
        }));
        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(55.93, 37.93, 5), //16
                new Coordinate(68.68, -6.49, 5)
        }));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(106, 18.5, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        // Create porus surface as defined by the test:
        // The surface of the earth berm is porous (G = 1).
        rayData.addSoilType(new GeoWithSoilType(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }), 1.));

        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        //KMLDocument.exportScene("target/tc24.kml", manager, propDataOut);
        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93 - 26.2, 93 - 16.1,
                93 - 8.6, 93 - 3.2, 93, 93 + 1.2, 93 + 1.0, 93 - 1.1});
        //todo IL Y A UNE ERREUR DANS LA NORME AVEC LE BATIMENT 2, SI ON LE SUPPRIME LES RESULTATS SONT EQUIVALENTS
        assertArrayEquals(new double[]{14.31, 21.69, 27.76, 31.52, 31.49, 29.18, 25.39, 16.58}, L, ERROR_EPSILON_very_high);

    }

    /**
     * – Replacement of the earth-berm by a barrier
     * @throws LayerDelaunayError
     * @throws IOException
     */
    @Test
    public void TC25() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();


        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8);

        // screen
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.19, 24.47, 0),
                new Coordinate(64.17, 6.95, 0),
                new Coordinate(64.171, 6.951, 0),
                new Coordinate(59.191, 24.471, 0),
                new Coordinate(59.19, 24.47, 0)}), 5);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(106, 18.5, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        //MANQUE DIFFRACTIONS HORIZONTALES
        assertArrayEquals(  new double[]{17.50,25.65,30.56,33.22,33.48,31.52,27.51,17.80},L, ERROR_EPSILON_very_high);//p=0.5
    }


    /**
     * TC26 – Road source with influence of retrodiffraction
     * @throws LayerDelaunayError
     * @throws IOException
     * */
    @Test
    public void TC26() throws LayerDelaunayError, IOException {


        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        // screen
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(74.0, 52.0, 0),
                new Coordinate(130.0, 60.0, 0),
                new Coordinate(130.01, 60.01, 0),
                new Coordinate(74.01, 52.01, 0),
                new Coordinate(74.0, 52.0, 0)}), 7); // not exacly the same


        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(120, 50, 8));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 0.05)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -10, 100)), 0.0));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -10, 100)), 0.5));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});


        assertArrayEquals(  new double[]{17.50,27.52,34.89,40.14,43.10,43.59,40.55,29.15},L, ERROR_EPSILON_high);//p=0.5
    }


    /**
     * TC27 – Road source with influence of retrodiffraction
     * @throws LayerDelaunayError
     * @throws IOException
     * */
    @Test
    public void TC27() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        // screen
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114.0, 52.0, 0),
                new Coordinate(170.0, 60.0, 0),
                new Coordinate(170.01, 60.01, 0),
                new Coordinate(114.01, 52.01, 0),
                new Coordinate(114.0, 52.0, 0)}), 4); // not exacly the same


        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(-200, -200, -0.5), // 5
                new Coordinate(110, -200, -0.5), // 5-6
                new Coordinate(110, 200, -0.5), // 6-7
                new Coordinate(-200, 200, -0.5), // 7-8
                new Coordinate(-200, -200, -0.5) // 8
        }));

        mesh.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(111, -200, 0), // 5
                new Coordinate(200, -200, 0), // 5-6
                new Coordinate(200, 200, 0), // 6-7
                new Coordinate(111, 200, 0), // 7-8
                new Coordinate(111, -200, 0) // 8
        }));


        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(105, 35, -0.45)));

        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(80, 110, 20, 80)), 0.0));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(110, 215, 20, 80)), 1.0));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{16.84,26.97,34.79,40.23,38.57,38.58,39.36,29.60},L, ERROR_EPSILON_very_high);// we don't take into account retrodiffraction

    }

    /**
     * TC28 Propagation over a large distance with many buildings between source and
     * receiver
     */
    @Test
    public void TC28() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500, 1500, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(113, 10, 0),
                new Coordinate(127, 16, 0),
                new Coordinate(102, 70, 0),
                new Coordinate(88, 64, 0),
                new Coordinate(113, 10, 0)}), 6);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(176, 19, 0),
                new Coordinate(164, 88, 0),
                new Coordinate(184, 91, 0),
                new Coordinate(196, 22, 0),
                new Coordinate(176, 19, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(250, 70, 0),
                new Coordinate(250, 180, 0),
                new Coordinate(270, 180, 0),
                new Coordinate(270, 70, 0),
                new Coordinate(250, 70, 0)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(332, 32, 0),
                new Coordinate(348, 126, 0),
                new Coordinate(361, 108, 0),
                new Coordinate(349, 44, 0),
                new Coordinate(332, 32, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(400, 5, 0),
                new Coordinate(400, 85, 0),
                new Coordinate(415, 85, 0),
                new Coordinate(415, 5, 0),
                new Coordinate(400, 5, 0)}), 9);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(444, 47, 0),
                new Coordinate(436, 136, 0),
                new Coordinate(516, 143, 0),
                new Coordinate(521, 89, 0),
                new Coordinate(506, 87, 0),
                new Coordinate(502, 127, 0),
                new Coordinate(452, 123, 0),
                new Coordinate(459, 48, 0),
                new Coordinate(444, 47, 0)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(773, 12, 0),
                new Coordinate(728, 90, 0),
                new Coordinate(741, 98, 0),
                new Coordinate(786, 20, 0),
                new Coordinate(773, 12, 0)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(972, 82, 0),
                new Coordinate(979, 121, 0),
                new Coordinate(993, 118, 0),
                new Coordinate(986, 79, 0),
                new Coordinate(972, 82, 0)}), 8);

        //x2
        mesh.addTopographicPoint(new Coordinate(-1300, -1300, 0));
        mesh.addTopographicPoint(new Coordinate(1300, 1300, 0));
        mesh.addTopographicPoint(new Coordinate(-1300, 1300, 0));
        mesh.addTopographicPoint(new Coordinate(1300, -1300, 0));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(1000, 100, 1));
        rayData.addSource(factory.createPoint(new Coordinate(0, 50, 4)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));
        rayData.maxSrcDist = 1500;
        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{150-26.2,150-16.1,150-8.6,150-3.2,150,150+1.2,150+1.0,150-1.1});
        assertArrayEquals(  new double[]{43.56,50.59,54.49,56.14,55.31,49.77,23.37,-59.98},L, ERROR_EPSILON_very_high);//p=0.5


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
            roadLvl[i] = dbaToW(roadLvl[i]);
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

        assertEquals(44.07, ComputeRays.wToDba(ComputeRays.sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
    }

    @Test
    public void testRoseIndex() {
        double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;
        double angleStart = Math.PI / 2 - angle_section / 2;
        for(int i = 0; i < PropagationProcessPathData.DEFAULT_WIND_ROSE.length; i++) {
            double angle = angleStart - angle_section * i - angle_section / 3;
            int index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);angle = angleStart - angle_section * i - angle_section * 2.0/3.0;
            index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
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
            roadLvl[i] = dbaToW(roadLvl[i]);
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
        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.makeRelativeZToAbsolute();
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);


        // Second compute the same scene but with a line source
        rayData.clearSources();
        rayData.addSource(geomSource, roadLvl);
        RayOut propDataOutTest = new RayOut(true, attData, rayData);
        computeRays.run(propDataOutTest);

        // Merge levels for each receiver for point sources
        Map<Long, double[]> levelsPerReceiver = new HashMap<>();
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOut.receiversAttenuationLevels) {
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
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOutTest.receiversAttenuationLevels) {
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





    /**
     * Test reported issue with receiver over building
     */
    @Test
    public void testReceiverOverBuilding() throws LayerDelaunayError, ParseException {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        WKTReader wktReader = new WKTReader();
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        mesh.addGeometry(wktReader.read("POLYGON ((-111 -35, -111 82, 70 82, 70 285, 282 285, 282 -35, -111 -35))"), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(162, 80, 150));
        rayData.addSource(factory.createPoint(new Coordinate(-150, 200, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        assertEquals(14.6, ComputeRays.wToDba(ComputeRays.sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
    }






    private static double getMaxError(double[] ref, double[] result) {
        assertEquals(ref.length, result.length);
        double max = Double.MIN_VALUE;
        for(int i=0; i < ref.length; i++) {
            max = Math.max(max, Math.abs(ref[i] - result[i]));
        }
        return max;
    }

    private static final class RayOut extends ComputeRaysOutAttenuation {
        private DirectPropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, DirectPropagationProcessData processData) {
            super(keepRays, pathData);
            this.processData = processData;
        }

        @Override
        public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath);
            double[] soundLevel = ComputeRays.wToDba(ComputeRays.multArray(processData.wjSources.get((int)sourceId), dbaToW(attenuation)));
            return soundLevel;
        }
    }

    private static final class DirectPropagationProcessData extends PropagationProcessData {
        private List<double[]> wjSources = new ArrayList<>();

        public DirectPropagationProcessData(FastObstructionTest freeFieldFinder) {
            super(freeFieldFinder);
        }

        public void addSource(Geometry geom, double[] spectrum) {
            super.addSource(geom);
            wjSources.add(spectrum);
        }

        public void addSource(Geometry geom, List<Double> spectrum) {
            super.addSource(geom);
            double[] wj = new double[spectrum.size()];
            for(int i=0; i < spectrum.size(); i++) {
                wj[i] = spectrum.get(i);
            }
            wjSources.add(wj);
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


    /**
     * Test NaN regression issue
     * ByteArrayOutputStream bos = new ByteArrayOutputStream();
     * propath.writeStream(new DataOutputStream(bos));
     * new String(Base64.getEncoder().encode(bos.toByteArray()));
     */
    @Test
    public void TestRegressionNaN() throws LayerDelaunayError, IOException {
        String path = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABkELTp9wo7AcQVnI2rXCgfo/qZmZmZmZmgAAAAAAAAAAAAAAAAAAAAAACH/4" +
                "AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAAEELUD" +
                "JSoUA3QVnItqDcGhJAJdiQBvXwS0AVTjoMf9fiAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gA" +
                "AAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELUoGFTOGrQVnIga50fzdANmqD/Me4pUActzMeCMRaAAAAAA" +
                "AAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////" +
                "AAAAA0ELUo/NRf1KQVnIgGcH8SZANmqD/Me4pUAe4TEhnNY1AAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AA" +
                "AAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELU1RrgqjDQVnIbssqD85AMNkgsNSQIkAlRqCv" +
                "boWkAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAA" +
                "AAAAD/////AAAAA0ELU3djM9QGQVnIa6l2eGhALdnXzMMRgUAl2dfMwxGBAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAA" +
                "AAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAABAAAAAU/wBvxnrf6hkBJLxOOJzAAwGIL" +
                "Ic/cAABAJRdmLHGkLkELTp9xTapHQVnI2rWzSOi/jTelQ3f5WD/hNUTOrUdwQFJ6WqIZaADAanpOelWAAD/odGaWtL+wQQtQMkh" +
                "BG1pBWci2ocn7h0ASDrQig+tyAAAAAAAAAAA/+uzqKo4AAMATSo/AwAAAP/Qml6qEHQRBC1J9yb/w6kFZyIIECGuLQBb5xfFciCQ" +
                "AAAAAAAAAAEA4e506acAAwFGKjYvwAABABNd1zN/3IEELUo8N2d3pQVnIgHgsr3lAIC98kZ14SQAAAAAAAAAAQBFrZiGsAADAKP" +
                "YLaTAAAD/SoWVrORNgQQtTU81vkgNBWchu2VI9a0AlRuFfAqJXAAAABT/TBsY8SUi2QGNg7UkPZADAe8S0CsKAAEAVMZUczyA2Q" +
                "QtOn1fPWpRBWcjat/vFwUAKJWO95msGP9MGxjxJSLZAY2CN2WJwAMB7xCtJq4AAQCDgvd4PLeBBC06fP717akFZyNq6I58WQBny" +
                "M91nx0E/0wbGPElItkBjYUy4vFgAwHvFPMvZgABAAUNc+v/JNkELTp9Xz1qUQVnI2rf7xcFACiVjveZrBj/TBsY8SUi2QGNfsm" +
                "l4cADAe8Lw2QoAAEAuAc8nHC4hQQtOn3aZdepBWcjatTnckL+z+60sjk/gP9MGxjxJSLZAY2GkR76YAMB7xbpDLYAAQBuZiTof" +
                "xetBC06fV89alEFZyNq3+8XBQAolY73mawY=";

        PropagationPath propPath = new PropagationPath();
        propPath.readStream(new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(path))));
        propPath.initPropagationPath();

//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        propPath.writeStream(new DataOutputStream(bos));
//        String newVersion  = new String(Base64.getEncoder().encode(bos.toByteArray()));
//        System.out.println(newVersion);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        PropagationProcessPathData pathData = new PropagationProcessPathData();
        evaluateAttenuationCnossos.evaluate(propPath, pathData);
        double[] aGlobalMeteoHom = evaluateAttenuationCnossos.getaGlobal();
        for (int i = 0; i < aGlobalMeteoHom.length; i++) {
            assertFalse(String.format("freq %d Hz with nan value", pathData.freq_lvl.get(i)),
                    Double.isNaN(aGlobalMeteoHom[i]));
        }

    }
}