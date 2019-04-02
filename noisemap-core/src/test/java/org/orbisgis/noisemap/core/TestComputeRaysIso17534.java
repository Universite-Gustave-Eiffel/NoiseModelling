package org.orbisgis.noisemap.core;

import org.h2gis.functions.spatial.volume.GeometryExtrude;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.orbisgis.noisemap.core.ComputeRays.dbaToW;
import static org.orbisgis.noisemap.core.ComputeRays.wToDba;


public class TestComputeRaysIso17534 {
    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));

    private static final double ERROR_EPSILON_TEST_T01 = 0.3; // 8khz issue
    private static final double ERROR_EPSILON_TEST_T02 = 0.3; // 8khz issue
    private static final double ERROR_EPSILON_TEST_T03 = 0.3; // 8khz issue
    private static final double ERROR_EPSILON_TEST_T04 = 0.3; // 8khz issue
    private static final double ERROR_EPSILON_TEST_T = 0.2;

    private double[] sumArrayWithPonderation(double[] array1, double[] array2, double p) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(p*dbaToW(array1[i])+ (1-p)*dbaToW(array2[i]));
        }
        return sum;
    }

    private double[] sumArray(double[] array1, double[] array2) {
        double[] sum = new double[array1.length];
        for (int i = 0; i < array1.length; i++) {
            sum[i] = wToDba(dbaToW(array1[i])+ dbaToW(array2[i]));
        }
        return sum;
    }

    private double[] computeWithMeteo(ComputeRaysOut propDataOut) {
        double[] aGlobal = new double[]{-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000,-10^1000};
        for (ComputeRaysOut.verticeSL lvl: propDataOut.getVerticesSoundLevel()) {
            aGlobal = sumArray(aGlobal, lvl.value);
        }
        return aGlobal;
    }

    private void splCompare(double[] resultW, String testName, double[] expectedLevel, double splEpsilon) {
        for (int i = 0; i < resultW.length; i++) {
            double dba = resultW[i];
            double expected = expectedLevel[i];
            assertEquals("Unit test " + testName + " failed at " + freqLvl.get(i) + " Hz", expected, dba, splEpsilon);
        }
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

    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for (double db_m : dbValues) {
            ret.add(ComputeRays.dbaToW(db_m));
        }
        return ret;
    }

    public double[] runTest(List<Coordinate> receivers, List<MeshBuilder.PolygonWithHeight> buildings, List<Coordinate> topoPoints, List<Coordinate> srcPoints,
                        List<GeoWithSoilType> geoWithSoilTypeList, double[] favrose, int reflectionOrder,
                            boolean doHorizontalDiffraction, boolean doVerticalDiffraction) throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope();

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();

        List<Geometry> srclst = new ArrayList<>();
        for (int idsrc = 0; idsrc < srcPoints.size(); idsrc++) {
            Geometry src = factory.createPoint(srcPoints.get(idsrc));
            srclst.add(src);
            cellEnvelope.expandToInclude(src.getEnvelopeInternal());
            sourcesIndex.appendGeometry(src, idsrc);
        }

        MeshBuilder mesh = new MeshBuilder();

        for(MeshBuilder.PolygonWithHeight building : buildings) {
            mesh.addGeometry(building.geo, building.getHeight(), building.getAlpha());
            cellEnvelope.expandToInclude(building.geo.getEnvelopeInternal());
        }

        for(Coordinate pt : topoPoints) {
            cellEnvelope.expandToInclude(pt);
            mesh.addTopographicPoint(pt);
        }

        for(Coordinate receiver : receivers) {
            cellEnvelope.expandToInclude(receiver);
        }

        cellEnvelope.expandBy(150);

        mesh.finishPolygonFeeding(cellEnvelope);

        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(receivers, manager, sourcesIndex, srclst, new ArrayList<>(),
                freqLvl, reflectionOrder, doHorizontalDiffraction, 1200, 1200, 1.,
                0., favrose, 0, 0, null, geoWithSoilTypeList, doVerticalDiffraction);

        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(10);
        propData.setHumidity(70);

        ComputeRaysOut propDataOut = new ComputeRaysOut(false, propData);

        ComputeRays computeRays = new ComputeRays(rayData);

        computeRays.setThreadCount(1);

        computeRays.run(propDataOut);

        return computeWithMeteo(propDataOut);
    }

    /**
     * TC01-TC03 - Flat ground with homogeneous acoustic properties
     */
    @Test
    public void TC01() throws LayerDelaunayError, IOException {
        List<Coordinate> receivers = Arrays.asList(new Coordinate(200, 50, 4));

        List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();

        List<Coordinate> topoPoints = new ArrayList<>();

        List<Coordinate> srcList = Arrays.asList(new Coordinate(10, 10, 1));

        List<GeoWithSoilType> geoWithSoilTypes = new ArrayList<>();

        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        double[] aGlobal = runTest(receivers, buildings, topoPoints, srcList, geoWithSoilTypes, favrose, 0, false, false);

        splCompare(aGlobal, "Test T01", new double[]{-53.05,-53.11,-53.23,-53.4,-53.74,-54.91,-59.39,-75.73}, ERROR_EPSILON_TEST_T01);
    }



    @Test
    public void TC02() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        List<Coordinate> receivers = Arrays.asList(new Coordinate(200, 50, 4));

        List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();

        List<Coordinate> topoPoints = new ArrayList<>();

        List<Coordinate> srcList = Arrays.asList(new Coordinate(10, 10, 1));

        List<GeoWithSoilType> geoWithSoilTypes = new ArrayList<>();
        geoWithSoilTypes.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 50)), 0.5));

        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        double[] aGlobal = runTest(receivers, buildings, topoPoints, srcList, geoWithSoilTypes, favrose, 0, false, true);

        splCompare(aGlobal, "Test T02", new double[]{-54.93,-54.99,-55.11,-56.21,-58.71,-56.79,-61.27,-77.61}, ERROR_EPSILON_TEST_T02);

    }



    @Test
    public void TC03() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        List<Coordinate> receivers = Arrays.asList(new Coordinate(200, 50, 4));

        List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();

        List<Coordinate> topoPoints = new ArrayList<>();

        List<Coordinate> srcList = Arrays.asList(new Coordinate(10, 10, 1));

        List<GeoWithSoilType> geoWithSoilTypes = new ArrayList<>();
        geoWithSoilTypes.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 50)), 1));

        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        double[] aGlobal = runTest(receivers, buildings, topoPoints, srcList, geoWithSoilTypes, favrose, 0, false, true);

        splCompare(aGlobal, "Test T03", new double[]{-56.79,-56.84,-57.69,-63.29,-59.3,-58.64,-63.13,-79.46}, ERROR_EPSILON_TEST_T03);

    }



    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     * The aim
     */
    @Test
    public void TC04() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        List<Coordinate> receivers = Arrays.asList(new Coordinate(200, 50, 4));

        List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();

        List<Coordinate> topoPoints = new ArrayList<>();

        List<Coordinate> srcList = Arrays.asList(new Coordinate(10, 10, 1));

        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.2));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.9));

        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        double[] aGlobal = runTest(receivers, buildings, topoPoints, srcList, geoWithSoilTypeList, favrose, 0, false, true);

        splCompare(aGlobal, "Test T04", new double[]{-55.09,-55.15,-55.27,-56.63,-58.77,-56.94,-61.43,-77.76}, ERROR_EPSILON_TEST_T04);

    }

    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        List<Coordinate> receivers = Arrays.asList(new Coordinate(200, 10, 14));

        List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();

        List<Coordinate> topoPoints = new ArrayList<>();
        topoPoints.addAll(Arrays.asList(new Coordinate(0, 80, 0),
                new Coordinate(225, 80, 0),
                new Coordinate(225, -20, 0),
                new Coordinate(0, -20, 0),
                new Coordinate(120, -20, 0),
                new Coordinate(185, -5, 10),
                new Coordinate(205, -5, 10),
                new Coordinate(205, 75, 10),
                new Coordinate(185, 75, 10),
                new Coordinate(225, 80, 0),
                new Coordinate(225, -20, 0),
                new Coordinate(0, -20, 0),
                new Coordinate(0, 80, 0),
                new Coordinate(120, 80, 0),
                new Coordinate(205, -5, 10),
                new Coordinate(205, 75, 10),
                new Coordinate(185, 75, 10),
                new Coordinate(185, -5, 10)));

        List<Coordinate> srcList = Arrays.asList(new Coordinate(10, 10, 1));

        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));


        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        double[] aGlobal = runTest(receivers, buildings, topoPoints, srcList, geoWithSoilTypeList, favrose, 0, false, true);

        splCompare(aGlobal, "Test T05", new double[]{-55.74,-55.79,-55.92,-56.09,-56.43,-57.59,-62.09,-78.46}, ERROR_EPSILON_TEST_T);
        // not the same definition of the mean plane (from where to where ?)... Also ISO keep the vertical for zs but in directive "The  equivalent height of a  point is  its  orthogonal
        //height in  relation to the mean ground plane".
    }

//    /**
//     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
//     */
//    @Test
//    public void TC05() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 0, false, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//        PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(10);
//        propData.setHumidity(70);
//        propData.setPrime2520(true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut(false ,propData);
//        ComputeRays computeRays = new ComputeRays(rayData);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 10, 14), 0, debug, propDataOut);
//
//
//        double p = 0.5; // probability favourable conditions
//
//        double[] aGlobal = computeWithMeteo(propData, propDataOut, p);
//        splCompare(aGlobal, "Test T05", new double[]{-55.74,-55.79,-55.92,-56.09,-56.43,-57.59,-62.09,-78.46}, ERROR_EPSILON_TEST_T);
//        // not the same definition of the mean plane (from where to where ?)... Also ISO keep the vertical for zs but in directive "The  equivalent height of a  point is  its  orthogonal
//        //height in  relation to the mean ground plane".
//
//
//    }
//
//    /**
//     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
//     * This test
//     */
//    @Test
//    public void TC06() throws LayerDelaunayError {
//        // TODO Rayleigh stuff
//
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 500, -20, 80)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 0, false, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut(false, new PropagationProcessPathData());
//        ComputeRays computeRays = new ComputeRays(rayData);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0, debug, propDataOut);
//        assertEquals(true, false);
//    }
//
//
//    /**
//     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
//     */
//    @Test
//    public void TC07() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, false, 250, 250, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut(false, p);
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0, energeticSum, debug);
//
//        double p = 0.5; // probability favourable conditions
//        PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(10);
//        propData.setHumidity(70);
//        propData.setPrime2520(true);
//
//        double[] aGlobal = computeWithMeteo(propData, propDataOut, p);
//        splCompare(aGlobal, "Test T07", new double[]{-60.3,-61.42,-73.01,-65.11,-68.64,-71.54,-78.82,-98.05}, ERROR_EPSILON_TEST_T);
//
//    }
//
//    /**
//     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
//     */
//    @Test
//    public void TC08() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 300, 300, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0, energeticSum, debug);
//
//        double p = 0.5; // probability favourable conditions
//        PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(10);
//        propData.setHumidity(70);
//        propData.setPrime2520(true);
//
//        double[] aGlobal = computeWithMeteo(propData, propDataOut, p);
//        splCompare(aGlobal, "Test T08", new double[]{-58.63,-60.04,-61.89,-64.34,-68.13,-70.76,-78.07,-97.33}, ERROR_EPSILON_TEST_T);
//
//
//    }
//
//    /**
//     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short
//     * barrier
//     */
//    @Test
//    public void TC09() throws LayerDelaunayError {
//        // Impossible shape for NoiseModelling
//        assertEquals(true, false);
//    }
//
//    /**
//     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at low height
//     */
//    @Test
//    public void TC10() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 4), 0, energeticSum, debug);
//
//        double p = 0.5; // probability favourable conditions
//        PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(10);
//        propData.setHumidity(70);
//
//        double[] aGlobal = computeWithMeteo(propData, propDataOut, p);
//        splCompare(aGlobal, "Test T10", new double[]{-46.91,-50.51,-54.56,-57.03,-58.33,-59.1,-59.91,-61.8}, ERROR_EPSILON_TEST_T);
//
//    }
//
//    /**
//     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at large height
//     */
//    @Test
//    public void TC11() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 15), 0, energeticSum, debug);
//        String filename = "target/T11.vtk";
//        String filename2 = "target/T11.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at low height
//     */
//    @Test
//    public void TC12() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(0, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(30, 20, 6), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T12.vtk";
//        String filename2 = "D:/aumond/Desktop/T12.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//
//    /**
//     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
//     * building
//     */
//    @Test
//    public void TC13() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 28.5), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T13.vtk";
//        String filename2 = "D:/aumond/Desktop/T13.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at large height
//     */
//    @Test
//    public void TC14() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(8, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(25, 20, 23), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T14.vtk";
//        String filename2 = "D:/aumond/Desktop/T14.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
//     */
//    @Test
//    public void TC15() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(100, 15, 5), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T15.vtk";
//        String filename2 = "D:/aumond/Desktop/T15.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * Reflecting barrier on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC16() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), 0, energeticSum, debug);
//
//        String filename = "D:/aumond/Desktop/T16.vtk";
//        String filename2 = "D:/aumond/Desktop/T16.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
//     * reduced receiver height
//     */
//    @Test
//    public void TC17() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0, energeticSum, debug);
//
//        String filename = "D:/aumond/Desktop/T17.vtk";
//        String filename2 = "D:/aumond/Desktop/T17.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false); // because rayleigh distance
//    }
//
//    /**
//     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC18() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 61, 0),
//                new Coordinate(114, 53, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(87, 50, 0),
//                new Coordinate(92, 32, 0),
//                new Coordinate(92, 33, 0),
//                new Coordinate(87, 51, 0),
//                new Coordinate(87, 50, 0)}), 12);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, true, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut(false, new PropagationProcessPathData());
//        ComputeRays computeRays = new ComputeRays(rayData);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 12), 0, debug, propDataOut);
//
//
//        PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(10);
//        propData.setHumidity(70);
//
//        double[] aGlobal = computeWithMeteo(propData, propDataOut, 0.5);
//
//        splCompare(aGlobal, "Test T18", new double[]{-53.05,-53.11,-53.23,-53.4,-53.74,-54.91,-59.39,-75.73}, ERROR_EPSILON_TEST_T);
//
//
//
//        assertEquals(true, true);
//    }
//
//    /**
//     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC19() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
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
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(156, 28, 0),
//                new Coordinate(145, 7, 0),
//                new Coordinate(145, 8, 0),
//                new Coordinate(156, 29, 0),
//                new Coordinate(156, 28, 0)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 35, 0),
//                new Coordinate(188, 19, 0),
//                new Coordinate(188, 20, 0),
//                new Coordinate(175, 36, 0),
//                new Coordinate(175, 35, 0)}), 14.5);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 24, 0),
//                new Coordinate(118, 24, 0),
//                new Coordinate(118, 30, 0),
//                new Coordinate(100, 30, 0),
//                new Coordinate(100, 24, 0)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 15.1, 0),
//                new Coordinate(118, 15.1, 0),
//                new Coordinate(118, 23.9, 0),
//                new Coordinate(100, 23.9, 0),
//                new Coordinate(100, 15.1, 0)}), 7);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 9, 0),
//                new Coordinate(118, 9, 0),
//                new Coordinate(118, 15, 0),
//                new Coordinate(100, 15, 0),
//                new Coordinate(100, 9, 0)}), 12);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 1, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 30, 14), 0, energeticSum, debug);
//
//        String filename = "D:/aumond/Desktop/T19.vtk";
//        String filename2 = "D:/aumond/Desktop/T19.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(true, true);
//    }
//
//    /**
//     * TC20 - Ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC20() throws LayerDelaunayError {
//        //Tables 221 – 222 are not shown in this draft.
//
//        assertEquals(false, true);
//    }
//
//    /**
//     * TC21 - Building on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC21() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(167.2, 39.5, 0),
//                new Coordinate(151.6, 48.5, 0),
//                new Coordinate(141.1, 30.3, 0),
//                new Coordinate(156.7, 21.3, 0),
//                new Coordinate(159.7, 26.5, 0),
//                new Coordinate(151.0, 31.5, 0),
//                new Coordinate(155.5, 39.3, 0),
//                new Coordinate(164.2, 34.3, 0),
//                new Coordinate(167.2, 39.5, 0)}), 11.5);
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
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T21.vtk";
//        String filename2 = "D:/aumond/Desktop/T21.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//    }
//
//
//    /**
//     * TC22 - Building with receiver backside on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC22() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 2, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T22.vtk";
//        String filename2 = "D:/aumond/Desktop/T22.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//
//    }
//
//
//    /**
//     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
//     * properties
//     */
//    @Test
//    public void TC23() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(38, 14, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
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
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.39, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.93, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T23.vtk";
//        String filename2 = "D:/aumond/Desktop/T23.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC24 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
//     * properties – receiver position modified
//     */
//    @Test
//    public void TC24() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC25 – Replacement of the earth-berm by a barrier
//     */
//    @Test
//    public void TC25() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC26 – Road source with influence of retrodiffraction
//     */
//    @Test
//    public void TC26() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC27 Source located in flat cut with retro-diffraction
//     */
//    @Test
//    public void TC27() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC28 Propagation over a large distance with many buildings between source and
//     * receiver
//     */
//    @Test
//    public void TC28() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(0, 50, 4)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500., 1500., 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(113, 10, 0),
//                new Coordinate(127, 16, 0),
//                new Coordinate(102, 70, 0),
//                new Coordinate(88, 64, 0),
//                new Coordinate(113, 10, 0)}), 6);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(176, 19, 0),
//                new Coordinate(164, 88, 0),
//                new Coordinate(184, 91, 0),
//                new Coordinate(196, 22, 0),
//                new Coordinate(176, 19, 0)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(250, 70, 0),
//                new Coordinate(250, 180, 0),
//                new Coordinate(270, 180, 0),
//                new Coordinate(270, 70, 0),
//                new Coordinate(250, 70, 0)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(332, 32, 0),
//                new Coordinate(348, 126, 0),
//                new Coordinate(361, 108, 0),
//                new Coordinate(349, 44, 0),
//                new Coordinate(332, 32, 0)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(400, 5, 0),
//                new Coordinate(400, 85, 0),
//                new Coordinate(415, 85, 0),
//                new Coordinate(415, 5, 0),
//                new Coordinate(400, 5, 0)}), 9);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(444, 47, 0),
//                new Coordinate(436, 136, 0),
//                new Coordinate(516, 143, 0),
//                new Coordinate(521, 89, 0),
//                new Coordinate(506, 87, 0),
//                new Coordinate(502, 127, 0),
//                new Coordinate(452, 123, 0),
//                new Coordinate(459, 48, 0),
//                new Coordinate(444, 47, 0)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(773, 12, 0),
//                new Coordinate(728, 90, 0),
//                new Coordinate(741, 98, 0),
//                new Coordinate(786, 20, 0),
//                new Coordinate(773, 12, 0)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(972, 82, 0),
//                new Coordinate(979, 121, 0),
//                new Coordinate(993, 118, 0),
//                new Coordinate(986, 79, 0),
//                new Coordinate(972, 82, 0)}), 8);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 1500, 1500, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(1000, 100, 1), 0, energeticSum, debug);
//        String filename = "D:/aumond/Desktop/T28.vtk";
//        String filename2 = "D:/aumond/Desktop/T28.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, true);
//
//    }


}