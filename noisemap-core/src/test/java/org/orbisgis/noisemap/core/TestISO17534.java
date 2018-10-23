/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import junit.framework.TestCase;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/***
 * Sound propagation evaluation using NMPB validation scenarios Doesn't work !!
 <<<<<<< HEAD
 */
public class TestISO17534 extends TestCase {
    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));
    private static final double ERROR_EPSILON_TEST_T = 0.2;

    private double[] splCompute(PropagationProcess propManager, Coordinate receiverPosition) {
        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        propManager.computeSoundLevelAtPosition(receiverPosition, energeticSum, debug);
        return energeticSum;
    }

    private void splCompare(double[] resultW, String testName, double[] expectedLevel, double splEpsilon) {
        for (int i = 0; i < resultW.length; i++) {
            double dba = PropagationProcess.wToDba(resultW[i]);
            double expected = expectedLevel[i];
            assertEquals("Unit test " + testName + " failed at " + freqLvl.get(i) + " Hz", expected, dba, splEpsilon);
        }
    }

    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for (double db_m : dbValues) {
            ret.add(PropagationProcess.dbaToW(db_m));
        }
        return ret;
    }

    /**
     * Sound propagation
     * T01
     * Horizontal ground with homogeneous properties, close receiver - Reflective ground (G=0)
     *
     * @throws LayerDelaunayError
     */
    public void testT01() throws LayerDelaunayError {
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
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -50, 50)), 0.));
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

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(15);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(200, 0, 4)), "Test T01", new double[]{26.0, 35.9, 40.8, 45.5, 45.2, 44.2, 35.7, 17.0}, ERROR_EPSILON_TEST_T);
    }


    /**
     * Sound propagation
     * T02
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
     *
     * @throws LayerDelaunayError
     */
    public void testT02() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -50, 50)), 1.));
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
        double[] favrose = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(200, 0, 4)), "Test T02", new double[]{23.0, 32.9, 37.4, 39.4, 32.5, 36.7, 29.7, 11.2}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T03
     * Horizontal ground with homogeneous properties, road source - Non compacted ground (G=0.7)
     *
     * @throws LayerDelaunayError
     */
    public void testT03() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -50, 50)), 0.7));
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
        double[] favrose = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(7.5, 0, 4)), "Test T03", new double[]{53.3, 63.3, 68.3, 73.3, 73.3, 73.3, 68.1, 62.5}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T04H
     * Horizontal ground with homogeneous acoustic properties Homogeneous Meteo
     *
     * @throws LayerDelaunayError
     */
    public void testT04H() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 20, -20, 40)), 0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(20, 40, -20, 40)), 1.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(40, 60, -20, 40)), 0.3));
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
        double[] favrose = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(15);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 0, 2)), "Test T04H", new double[]{36.9, 46.9, 51.8, 56.8, 56.7, 56.3, 42.8, 32.6}, 0.1);
    }

    /**
     * Sound propagation
     * T04F
     * Horizontal ground with spatially varying acoustic properties Favourable Meteo
     *
     * @throws LayerDelaunayError
     */
    public void testT04F() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 20, -20, 40)), 0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(20, 40, -20, 40)), 1.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(40, 60, -20, 40)), 0.3));
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
        double[] favrose = new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(15);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 0, 2)), "Test T04F", new double[]{36.9, 46.9, 51.8, 56.8, 56.7, 53.3, 48.8, 42.1}, 0.1);
    }

    /**
     * Sound propagation
     * T05
     * Diffraction on vertical adges - building
     *
     * @throws LayerDelaunayError
     */
    public void testT05() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(50, 250, 0),
                new Coordinate(100, 250, 0),
                new Coordinate(100, -250, 0),
                new Coordinate(50, -250, 0),
                new Coordinate(50, 250, 0)}), 10);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(15);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(150, 0, 2)), "Test T05", new double[]{16.7, 22.9, 24.5, 26.2, 25.4, 24.7, 17.0, 1.8}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T06
     * Horizontal ground with homogeneous properties, road source - Non compacted ground (G=0.7) with a small slope (test relief)
     *
     * @throws LayerDelaunayError
     */
    public void testT06() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -250, 250)), 0.7));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topo
        mesh.addTopographicPoint(new Coordinate(0, -250, 0));
        mesh.addTopographicPoint(new Coordinate(0, 250, 0));
        mesh.addTopographicPoint(new Coordinate(150, -250, 2));
        mesh.addTopographicPoint(new Coordinate(150, 250, 2));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0.7, favrose, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(15);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(150, 0, 10)), "Test T06", new double[]{27.1, 37.1, 42.0, 46.8, 46.5, 43.4, 35.9, 20.8}, 0.1);
    }

    /**
     * Sound propagation
     * T10
     * Ground with spatially varying heights and acoustic properties - Strong enbankment - Slope Attenuation
     *
     * @throws LayerDelaunayError
     */

    public void testT10() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(3.5, 10, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(53.1, 54.1, 56.1, 59.1, 61.1, 64.1, 66.1, 69.1, 69.1, 72.1, 73.1, 72.1, 70.1, 67.1, 64.1, 62.1, 59.1, 57.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 9.5, 0, 20)), 0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(9.5, 15.5, 0, 20)), 1.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(15.5, 60, 0, 20)), 1.));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(15.5, 20, 0),
                new Coordinate(60, 20, 0),
                new Coordinate(60, 0, 0),
                new Coordinate(15.5, 0, 0),
                new Coordinate(15.5, 20, 0)}), 4.);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(40, 10, 9)), "Test T10", new double[]{10.59, 11.59, 13.58, 16.57, 18.56, 21.54, 23.53, 26.51, 26.49, 29.47, 30.45, 29.41, 27.36, 24.27, 21.14, 18.94, 15.61, 13.1}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T11
     * Ground with spatially varying heights and acoustic properties - Strong enbankment - Ground effect
     *
     * @throws LayerDelaunayError
     */
    public void testT11() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(3.5, 10, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(53.1, 54.1, 56.1, 59.1, 61.1, 64.1, 66.1, 69.1, 69.1, 72.1, 73.1, 72.1, 70.1, 67.1, 64.1, 62.1, 59.1, 57.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 9.5, 0, 20)), 0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(9.5, 15.5, 0, 20)), 1.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(15.5, 60, 0, 20)), 1.));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(15.5, 20, 0),
                new Coordinate(60, 20, 0),
                new Coordinate(60, 0, 0),
                new Coordinate(15.5, 0, 0),
                new Coordinate(15.5, 20, 0)}), 1.5);
        mesh.finishPolygonFeeding(cellEnvelope);
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(20, 10, 6.5)), "Test T11", new double[]{19.98, 20.98, 22.98, 25.97, 27.97, 30.96, 32.95, 35.94, 35.94, 38.93, 39.91, 38.9, 36.87, 33.83, 30.77, 28.67, 25.52, 23.28}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T12
     * Ground with spatially varying heights and acoustic properties - Strong enbankment - Ground and diffraction effect
     *
     * @throws LayerDelaunayError
     */
    public void testT12() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(3.5, 10, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(53.1, 54.1, 56.1, 59.1, 61.1, 64.1, 66.1, 69.1, 69.1, 72.1, 73.1, 72.1, 70.1, 67.1, 64.1, 62.1, 59.1, 57.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 9.5, 0, 20)), 0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(9.5, 15.5, 0, 20)), 1.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(15.5, 60, 0, 20)), 1.));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(15.5, 20, 0),
                new Coordinate(60, 20, 0),
                new Coordinate(60, 0, 0),
                new Coordinate(15.5, 0, 0),
                new Coordinate(15.5, 20, 0)}), 1.5);
        mesh.finishPolygonFeeding(cellEnvelope);
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(40, 10, 6.5)), "Test T12", new double[]{7.97, 9.07, 11.26, 14.52, 16.86, 20.2, 22.73, 26.48, 27.94, 32.1, 33.07, 32.04, 29.98, 26.9, 23.6, 20.89, 16.32, 12.93}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T13
     * Site with a flat ground having homogeneous properties and a long barrier
     *
     * @throws LayerDelaunayError
     */
    public void testT13() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(53.1, 54.1, 56.1, 59.1, 61.1, 64.1, 66.1, 69.1, 69.1, 72.1, 73.1, 72.1, 70.1, 67.1, 64.1, 62.1, 59.1, 57.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -20, 40)), 1.));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 60, 0),
                new Coordinate(100.01, 60, 0),
                new Coordinate(200.01, -40, 0),
                new Coordinate(200, -40, 0),
                new Coordinate(100, 60, 0)}), 6);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(200, 10, 4)), "Test T13", new double[]{-9.57, -8.89, -7.3, -4.74, -3.23, -0.81, 0.54, 2.87, 1.42, 1.01, -1.16, -4.87, -8.94, -12.61, -15.34, -16.92, -22.06, -27.36}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T14
     * Site with homogeneous ground properties and a large and tall building
     *
     * @throws LayerDelaunayError
     */
    public void testT14() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 0.05)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(53.1, 54.1, 56.1, 59.1, 61.1, 64.1, 66.1, 69.1, 69.1, 72.1, 73.1, 72.1, 70.1, 67.1, 64.1, 62.1, 59.1, 57.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 250, -250, 50)), 1.));
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(105, -250, 0),
                new Coordinate(175, 50, 0),
                new Coordinate(245, -250, 0),
                new Coordinate(245, -250, 0),
                new Coordinate(105, -250, 0)}), 1000);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 1, 250, 250, 1., 0., favrose, 0, null, geoWithSoilTypeList, false);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(200, 10, 4)), "Test T14", new double[]{-28.53, -27.56, -25.6, -22.65, -20.72, -19.79, -19.23, -17.83, -20.1, -19.7, -20.49, -21.97, -22.97, -24.4, -26, -29.22, -34.08, -38.92}, ERROR_EPSILON_TEST_T);
    }


    /**
     * Sound propagation
     * One source, One receiver, no buildings, two ground area and no topography.
     *
     * @throws LayerDelaunayError
     */
    public void testScene7() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 50, -50, 50)), 0.6));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 100, -50, 50)), 0.9));
        // rose of favourable conditions
        double[] favrose = new double[]{0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};

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

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200, 200, 1., 0., favrose, 0, null, geoWithSoilTypeList, false);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(100, 20, 1)), "Scene 7", new double[]{22.8, 23.8, 25.8, 28.7,
                30.7, 33.7, 35.6, 34.5, 28.7, 25.4, 23.0, 22.7, 24.0, 23.8, 23.2, 23.3, 21.9, 20.8}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and no topography.
     *
     * @throws LayerDelaunayError
     */
    public void testScene8() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 35, -100, 100)), 0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(35, 100, -100, 100)), 1));
        // rose of favourable conditions
        double[] favrose = new double[]{0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(20, 10, 0),
                new Coordinate(30, 10, 0),
                new Coordinate(30, 30, 0),
                new Coordinate(20, 30, 0),
                new Coordinate(20, 10, 0)}), 5);
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(35, 10, 0),
                new Coordinate(40, 10, 0),
                new Coordinate(40, 30, 0),
                new Coordinate(35, 30, 0),
                new Coordinate(35, 10, 0)}), 5.5);
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(45, 10, 0),
                new Coordinate(50, 10, 0),
                new Coordinate(50, 30, 0),
                new Coordinate(45, 30, 0),
                new Coordinate(45, 10, 0)}), 5);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        //TODO clarify, is the unit test result require the computation of Vertical diffraction + Horizontal diffraction ?
        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200, 200, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(75, 20, 2)), "Scene 8", new double[]{19.9, 19.8, 20.5,
                        22.2, 22.8, 24.4, 25.0, 26.7, 25.4, 27.2, 27.1, 25.0, 22.8, 19.7, 16.5, 14.2, 10.8, 8.1},
                ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and no topography.
     *
     * @throws LayerDelaunayError
     */
    public void testScene9() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 35, -100, 100)), 0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(35, 100, -100, 100)), 1));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(15, 10, 0),
                new Coordinate(35, 10, 0),
                new Coordinate(35, 30, 0),
                new Coordinate(15, 30, 0),
                new Coordinate(15, 10, 0)}), 5);
        mesh.finishPolygonFeeding(cellEnvelope);
        // rose of favourable conditions
        double[] favrose = new double[]{0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200, 200, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 20, 2)), "Scene 9", new double[]{18.1, 17.8, 18.3, 20.0,
                        20.7, 22.5, 23.3, 25.2, 24.1, 26.0, 26.0, 24.8, 22.8, 19.7, 16.5, 14.2, 10.8, 8.1},
                ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and topography.
     *
     * @throws LayerDelaunayError
     */
    public void testScene10() throws LayerDelaunayError {
        double groundHeight = 1.5;
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 10, -100, 100)), 0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(10, 100, -100, 100)), 1));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(20, 10, groundHeight),
                new Coordinate(30, 10, groundHeight),
                new Coordinate(30, 30, groundHeight),
                new Coordinate(20, 30, groundHeight),
                new Coordinate(20, 10, groundHeight)}), 5);
        // Add topographic points
        // Left of scene
        mesh.addTopographicPoint(new Coordinate(-50, 100, 0));
        mesh.addTopographicPoint(new Coordinate(-50, 30, 0));
        mesh.addTopographicPoint(new Coordinate(-50, 10, 0));
        mesh.addTopographicPoint(new Coordinate(-50, -100, 0));
        // bottom hill
        mesh.addTopographicPoint(new Coordinate(10, 100, 0));
        mesh.addTopographicPoint(new Coordinate(10, 30, 0));
        mesh.addTopographicPoint(new Coordinate(10, 10, 0));
        mesh.addTopographicPoint(new Coordinate(10, -100, 0));
        // top hill
        mesh.addTopographicPoint(new Coordinate(15, 100, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15, 30, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15, 10, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15, -100, groundHeight));
        // Right of scene
        mesh.addTopographicPoint(new Coordinate(100, 100, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100, 30, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100, 10, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100, -100, groundHeight));
        // Triangulation of scenery
        mesh.finishPolygonFeeding(cellEnvelope);
        // rose of favourable conditions
        double[] favrose = new double[]{0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200, 200, 1., 0., favrose, 0, null, geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 20, 2)), "Scene 10", new double[]{19.7, 19.6, 20.3, 22.0,
                        22.6, 24.2, 24.7, 26.5, 25.2, 27.0, 26.8, 24.8, 22.6, 19.4, 16.3, 14.0, 10.6, 7.9},
                ERROR_EPSILON_TEST_T);
    }
}
