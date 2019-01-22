package org.orbisgis.noisemap.core;

import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.Vector3D;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;



public class TestComputeRays {
    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));

    private static final double ERROR_EPSILON_TEST_T = 0.2;



    private void splCompare(double[] resultW, String testName, double[] expectedLevel, double splEpsilon) {
        for (int i = 0; i < resultW.length; i++) {
            double dba = resultW[i];
            double expected = expectedLevel[i];
            assertEquals("Unit test " + testName + " failed at " + freqLvl.get(i) + " Hz", expected, dba, splEpsilon);
        }
    }

    private void writeVTKmesh(String filename,ComputeRaysOut propDataOut,MeshBuilder mesh) throws IOException {

        int lengthPolygon = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates().length;

        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        fileWriter.write("POINTS " + String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size()+2*lengthPolygon) +  " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (PropagationPath.PointPath p:propDataOut.propagationPaths.get(0).getPointList())
        {
            coordinates.add(p.coordinate);
            fileWriter.write(String.valueOf(p.coordinate.x)+" "+String.valueOf(p.coordinate.y)+ " "+ String.valueOf(p.coordinate.z)+"\n");
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);
        mesh.getPolygonWithHeight().get(0).geo.getCoordinate();
        for (int j = 0; j < lengthPolygon; j++)
        {
            double x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
            double y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
            double z = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].z;
            fileWriter.write(String.valueOf(x)+" "+String.valueOf(y)+ " "+ String.valueOf(z)+"\n");
             x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
             y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
             z = mesh.getPolygonWithHeight().get(0).getHeight();
            fileWriter.write(String.valueOf(x)+" "+String.valueOf(y)+ " "+ String.valueOf(z)+"\n");
        }


        out.println(wktWriter.write(factoryLineString));

        fileWriter.write("LINES 1\n");
        fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size()));
        int i=0;
        for (PropagationPath.PointPath p:propDataOut.propagationPaths.get(0).getPointList()) {
            fileWriter.write(" " + String.valueOf(i));
            i++;
        }
        fileWriter.write("\n");

        fileWriter.write("POLYGONS 1 " + String.valueOf(2* lengthPolygon+1)+ "\n");

        fileWriter.write(String.valueOf(2* lengthPolygon));
       for (int j = 0; j < 2*lengthPolygon; j++) {
            fileWriter.write(" " +String.valueOf(j+i));
        }
        fileWriter.write("\n");

        fileWriter.close();
    }

    private void writeVTK(String filename,ComputeRaysOut propDataOut) throws IOException {


        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        int nbPoints = 0;
        for (int j=0;j <propDataOut.propagationPaths.size(); j++) {
            nbPoints=nbPoints+propDataOut.propagationPaths.get(j).getPointList().size();
        }
        fileWriter.write("\n");
        fileWriter.write("POINTS " + String.valueOf(nbPoints) +  " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int j=0;j <propDataOut.propagationPaths.size(); j++) {
            for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(j).getPointList()) {
                coordinates.add(p.coordinate);
                fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
            }
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);
        out.println(wktWriter.write(factoryLineString));
        fileWriter.write("\n");
        fileWriter.write("LINES "+ String.valueOf(propDataOut.propagationPaths.size())+ " "+String.valueOf(nbPoints+propDataOut.propagationPaths.size()) +"\n");
        int i = 0;
        for (int j=0;j <propDataOut.propagationPaths.size(); j++) {
            fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(j).getPointList().size()));

            for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(j).getPointList()) {
                fileWriter.write(" " + String.valueOf(i));
                i++;
            }
            fileWriter.write("\n");
        }



        fileWriter.close();
    }


    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for (double db_m : dbValues) {
            ret.add(PropagationProcess.dbaToW(db_m));
        }
        return ret;
    }

      /**
     * Test Direct Field
     */
    @Test
    public void DirectRay() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 50)), 0.));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 0, 4), energeticSum, debug);


        /*PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propDataOut.propagationPaths.get(0), propData), "Test T01", new double[]{-54, -54.1, -54.2, -54.5, -54.8, -55.8, -59.3, -73.0}, ERROR_EPSILON_TEST_T);
*/
        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), energeticSum, debug);

        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC06 -- ???
     */
    @Test
    public void TC06() throws LayerDelaunayError {
        // TODO Rayleigh stuff

        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 500, -20, 80)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), energeticSum, debug);
        assertEquals(true,false);
    }



    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.01, 240, 0),
                new Coordinate(265.01, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 250, 250, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), energeticSum, debug);

        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 300, 300, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), energeticSum, debug);

        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short
     * barrier
     */
    @Test
    public void TC09() throws LayerDelaunayError {
        // Impossible shape for NoiseModelling
        assertEquals(true, false);
    }
    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at low height
     */
    @Test
    public void TC10() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 4), energeticSum, debug);
        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at large height
     */
    @Test
    public void TC11() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 2, 400, 400, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 15), energeticSum, debug);
        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }





    /**
     * Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
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

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 400, 400, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), energeticSum, debug);

        String filename = "D:/aumond/Desktop/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}