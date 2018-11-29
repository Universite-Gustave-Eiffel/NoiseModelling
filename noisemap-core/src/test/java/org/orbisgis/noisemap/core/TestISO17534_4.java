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
public class TestISO17534_4 extends TestCase {
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
     * Test ISO/PDTR 17534-4:2018(E)
     * Sound propagation
     * TC01
     * Flat ground with homogeneous acoustic properties - Reflective ground (G=0)
     *
     * @throws LayerDelaunayError
     */
    public void testTC01() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(93.0-26.2, 93.0-16.1, 93.0-8.6, 93.0-3.2, 93.0, 93.0+1.2, 93.0+1.0, 93.0-1.1));
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
        double[] favrose = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};

        PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 250, 250, 1., 0., favrose,0.1, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(10);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        // 2 dB d'erreur sur alpha atm a 8000 Hz
        splCompare(splCompute(propManager, new Coordinate(200, 50, 4)), "Test TC01", new double[]{13.75, 23.79, 31.17, 36.4, 39.26, 39.29, 34.61, 16.17}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Test ISO/PDTR 17534-4:2018(E)
     * Sound propagation
     * TC02
     * Flat ground with homogeneous acoustic properties - Mixed ground (G=0.5)
     *
     * @throws LayerDelaunayError
     */
    public void testTC02() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(93.0-26.2, 93.0-16.1, 93.0-8.6, 93.0-3.2, 93.0, 93.0+1.2, 93.0+1.0, 93.0-1.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -50, 50)), 0.5));
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
                freqLvl, 0, 0, 250, 250, 1., 0., favrose, 0.5,0, null, geoWithSoilTypeList, true);
        propData.setTemperature(10);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        // 2 dB d'erreur sur alpha atm a 8000 Hz
        splCompare(splCompute(propManager, new Coordinate(200, 50, 4)), "Test TC02", new double[]{11.87, 21.91, 29.29, 33.59, 34.29, 37.41, 32.73, 14.29}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Test ISO/PDTR 17534-4:2018(E)
     * Sound propagation
     * TC03
     * Flat ground with homogeneous acoustic properties - Porous ground (G=1)
     *
     * @throws LayerDelaunayError
     */
    public void testTC03() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(93.0-26.2, 93.0-16.1, 93.0-8.6, 93.0-3.2, 93.0, 93.0+1.2, 93.0+1.0, 93.0-1.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50, 250, -50, 50)), 0.5));
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
                freqLvl, 0, 0, 250, 250, 1., 0., favrose,0.5, 0, null, geoWithSoilTypeList, true);
        propData.setTemperature(10);
        propData.setHumidity(70);
        PropagationProcessOut propDataOut = new PropagationProcessOut();
        PropagationProcess propManager = new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        // 2 dB d'erreur sur alpha atm a 8000 Hz
        splCompare(splCompute(propManager, new Coordinate(200, 50, 4)), "Test TC03", new double[]{10.01, 20.06, 26.71, 26.51, 33.70, 35.56, 30.87, 12.44}, ERROR_EPSILON_TEST_T);
    }




}
