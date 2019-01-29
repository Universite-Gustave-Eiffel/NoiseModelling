package org.orbisgis.noisemap.core;

import org.h2gis.functions.spatial.create.ST_Extrude;
import org.h2gis.functions.spatial.distance.ST_ClosestPoint;
import org.h2gis.functions.spatial.properties.ST_Explode;
import org.h2gis.functions.spatial.volume.GeometryExtrude;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.Vector3D;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.*;
import java.util.*;
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

    private void writeVTKmesh(String filename, ComputeRaysOut propDataOut, MeshBuilder mesh) throws IOException {

        int lengthPolygon = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates().length;

        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        fileWriter.write("POINTS " + String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size() + 2 * lengthPolygon) + " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
            coordinates.add(p.coordinate);
            fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);
        mesh.getPolygonWithHeight().get(0).geo.getCoordinate();
        for (int j = 0; j < lengthPolygon; j++) {
            double x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
            double y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
            double z = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].z;
            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
            x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
            y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
            z = mesh.getPolygonWithHeight().get(0).getHeight();
            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
        }


        out.println(wktWriter.write(factoryLineString));

        fileWriter.write("LINES 1\n");
        fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size()));
        int i = 0;
        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
            fileWriter.write(" " + String.valueOf(i));
            i++;
        }
        fileWriter.write("\n");

        fileWriter.write("POLYGONS 1 " + String.valueOf(2 * lengthPolygon + 1) + "\n");

        fileWriter.write(String.valueOf(2 * lengthPolygon));
        for (int j = 0; j < 2 * lengthPolygon; j++) {
            fileWriter.write(" " + String.valueOf(j + i));
        }
        fileWriter.write("\n");

        fileWriter.close();
    }

    private static void addGeometry(List<Geometry> geom, Geometry polygon) {
        if (polygon instanceof Polygon) {
            geom.add((Polygon) polygon);
        } else {
            for (int i = 0; i < polygon.getNumGeometries(); i++) {
                addGeometry(geom, polygon.getGeometryN(i));
            }
        }

    }

    private void writePLY(String filename, MeshBuilder mesh) throws IOException, LayerDelaunayError {
        PointsMerge pointsMerge = new PointsMerge(0.01);
        List<Geometry> triVertices2 = new ArrayList<>();
        Map<String,Integer> vertices2 = new HashMap<>();
        List<Coordinate> vertices3 = new ArrayList<>();
        GeometryFactory geometryFactory = new GeometryFactory();
        int k=0;
        for (MeshBuilder.PolygonWithHeight polygon : mesh.getPolygonWithHeight()) {
            GeometryCollection buildingExtruded = GeometryExtrude.extrudePolygonAsGeometry((Polygon) polygon.getGeometry(), polygon.getHeight());
            addGeometry(triVertices2, buildingExtruded);
            for (Coordinate coordinate : buildingExtruded.getCoordinates()) {
                vertices2.put(coordinate.toString(),k);
                vertices3.add(coordinate);
                k++;
            }

        }
        int vertexCountG = mesh.getVertices().size();
        int vertexCountB = vertices3.size();
        int faceCountG = mesh.getTriangles().size();
        int faceCountB = triVertices2.size();
        int vertexCount = vertexCountG + vertexCountB;
        int faceCount = faceCountG + faceCountB;
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("ply\n");
        fileWriter.write("format ascii 1.0\n");
        fileWriter.write("element vertex " + vertexCount + "\n");
        fileWriter.write("property float x\n");
        fileWriter.write("property float y\n");
        fileWriter.write("property float z\n");
        fileWriter.write("property uchar green\n");
        fileWriter.write("property uchar red\n");
        fileWriter.write("property uchar blue\n");
        fileWriter.write("element face " + faceCount + "\n");
        fileWriter.write("property list uchar int vertex_index\n");
        fileWriter.write("end_header\n");

        for (int i = 0; i < vertexCountG; i++) {
            fileWriter.write(mesh.getVertices().get(i).x + " " + mesh.getVertices().get(i).y + " " + (mesh.getVertices().get(i).z) + " " + "255 0 0\n");
        }
        // Iterating over values only
        for (Coordinate vertice : vertices3) {
            //System.out.println("Value = " + value);
            fileWriter.write(vertice.x + " " + vertice.y + " " + (vertice.z) + " " + "0 0 255\n");
        }

        for (int i = 0; i < faceCountG; i++) {
            fileWriter.write("3 " + mesh.getTriangles().get(i).getA() + " " + mesh.getTriangles().get(i).getB() + " " + (mesh.getTriangles().get(i).getC()) + "\n");
        }
        for (int i=0;i<faceCountB;i++){
            Coordinate[] coordinates = triVertices2.get(i).getCoordinates();
            fileWriter.write(coordinates.length + " " );
            for (int j=0;j<coordinates.length;j++){
              fileWriter.write((vertexCountG+ vertices2.get(coordinates[j].toString()))+" ");
            }
            fileWriter.write("\n" );
        }
        fileWriter.close();
    }

    private void writeVTK(String filename, ComputeRaysOut propDataOut) throws IOException {


        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        int nbPoints = 0;
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
            nbPoints = nbPoints + propDataOut.propagationPaths.get(j).getPointList().size();
        }
        fileWriter.write("\n");
        fileWriter.write("POINTS " + String.valueOf(nbPoints) + " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
            for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(j).getPointList()) {
                coordinates.add(p.coordinate);
                fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
            }
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);
        out.println(wktWriter.write(factoryLineString));
        fileWriter.write("\n");
        fileWriter.write("LINES " + String.valueOf(propDataOut.propagationPaths.size()) + " " + String.valueOf(nbPoints + propDataOut.propagationPaths.size()) + "\n");
        int i = 0;
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
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
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T05.vtk";
        String filename2 = "D:/aumond/Desktop/T05.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     * This test
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
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), energeticSum, debug);
        assertEquals(true, false);
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
                new Coordinate(100.1, 240, 0),
                new Coordinate(265.1, -180, 0),
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
                freqLvl, 1, 0, 250, 250, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T07.vtk";
        String filename2 = "D:/aumond/Desktop/T07.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
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
                freqLvl, 1, 1, 300, 300, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T08.vtk";
        String filename2 = "D:/aumond/Desktop/T08.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
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
        String filename = "D:/aumond/Desktop/T09.vtk";
        String filename2 = "D:/aumond/Desktop/T09.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
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
                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 15), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T11.vtk";
        String filename2 = "D:/aumond/Desktop/T11.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at low height
     */
    @Test
    public void TC12() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 10, 1)));
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
        computeRays.computeRaysAtPosition(new Coordinate(30, 20, 6), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T12.vtk";
        String filename2 = "D:/aumond/Desktop/T12.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
     * building
     */
    @Test
    public void TC13() throws LayerDelaunayError {
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
                new Coordinate(169.4, 41.0, 0),
                new Coordinate(172.5, 33.5, 0),
                new Coordinate(180.0, 30.4, 0),
                new Coordinate(187.5, 33.5, 0),
                new Coordinate(190.6, 41.0, 0),
                new Coordinate(187.5, 48.5, 0),
                new Coordinate(180.0, 51.6, 0),
                new Coordinate(172.5, 48.5, 0),
                new Coordinate(169.4, 41.0, 0)}), 30);

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
                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 28.5), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T13.vtk";
        String filename2 = "D:/aumond/Desktop/T13.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(8, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));

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
        computeRays.computeRaysAtPosition(new Coordinate(25, 20, 23), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T14.vtk";
        String filename2 = "D:/aumond/Desktop/T14.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() throws LayerDelaunayError {
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
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));

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
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(100, 15, 5), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T15.vtk";
        String filename2 = "D:/aumond/Desktop/T15.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
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
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T16.vtk";
        String filename2 = "D:/aumond/Desktop/T16.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
     * reduced receiver height
     */
    @Test
    public void TC17() throws LayerDelaunayError {
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
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T17.vtk";
        String filename2 = "D:/aumond/Desktop/T17.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false); // because rayleigh distance
    }

    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC18() throws LayerDelaunayError {
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
                new Coordinate(170, 61, 0),
                new Coordinate(114, 53, 0),
                new Coordinate(114, 52, 0)}), 15);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(87, 50, 0),
                new Coordinate(92, 32, 0),
                new Coordinate(92, 33, 0),
                new Coordinate(87, 51, 0),
                new Coordinate(87, 50, 0)}), 12);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 12), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T18.vtk";
        String filename2 = "D:/aumond/Desktop/T18.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }

    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC19() throws LayerDelaunayError {
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
                new Coordinate(156, 28, 0),
                new Coordinate(145, 7, 0),
                new Coordinate(145, 8, 0),
                new Coordinate(156, 29, 0),
                new Coordinate(156, 28, 0)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 35, 0),
                new Coordinate(188, 19, 0),
                new Coordinate(188, 20, 0),
                new Coordinate(175, 36, 0),
                new Coordinate(175, 35, 0)}), 14.5);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 24, 0),
                new Coordinate(118, 24, 0),
                new Coordinate(118, 30, 0),
                new Coordinate(100, 30, 0),
                new Coordinate(100, 24, 0)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 15.1, 0),
                new Coordinate(118, 15.1, 0),
                new Coordinate(118, 23.9, 0),
                new Coordinate(100, 23.9, 0),
                new Coordinate(100, 15.1, 0)}), 7);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 9, 0),
                new Coordinate(118, 9, 0),
                new Coordinate(118, 15, 0),
                new Coordinate(100, 15, 0),
                new Coordinate(100, 9, 0)}), 12);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 30, 14), energeticSum, debug);

        String filename = "D:/aumond/Desktop/T19.vtk";
        String filename2 = "D:/aumond/Desktop/T19.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC20() throws LayerDelaunayError {
        //Tables 221 – 222 are not shown in this draft.

        assertEquals(false, true);
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC21() throws LayerDelaunayError {
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
                new Coordinate(167.2, 39.5, 0),
                new Coordinate(151.6, 48.5, 0),
                new Coordinate(141.1, 30.3, 0),
                new Coordinate(156.7, 21.3, 0),
                new Coordinate(159.7, 26.5, 0),
                new Coordinate(151.0, 31.5, 0),
                new Coordinate(155.5, 39.3, 0),
                new Coordinate(164.2, 34.3, 0),
                new Coordinate(167.2, 39.5, 0)}), 11.5);

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
                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T21.vtk";
        String filename2 = "D:/aumond/Desktop/T21.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);
    }


    /**
     * TC22 - Building with receiver backside on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC22() throws LayerDelaunayError {
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
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T22.vtk";
        String filename2 = "D:/aumond/Desktop/T22.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);

    }


    /**
     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties
     */
    @Test
    public void TC23() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(38, 14, 1)));
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

        //x1
        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
        mesh.addTopographicPoint(new Coordinate(59.6, -9.87, 0));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.39, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.93, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T23.vtk";
        String filename2 = "D:/aumond/Desktop/T23.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);

    }

    /**
     * TC24 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties – receiver position modified
     */
    @Test
    public void TC24() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC25 – Replacement of the earth-berm by a barrier
     */
    @Test
    public void TC25() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC26 – Road source with influence of retrodiffraction
     */
    @Test
    public void TC26() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC27 Source located in flat cut with retro-diffraction
     */
    @Test
    public void TC27() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC28 Propagation over a large distance with many buildings between source and
     * receiver
     */
    @Test
    public void TC28() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 50, 4)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500., 1500., 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));

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

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 1500, 1500, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(1000, 100, 1), energeticSum, debug);
        String filename = "D:/aumond/Desktop/T28.vtk";
        String filename2 = "D:/aumond/Desktop/T28.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, true);

    }


}