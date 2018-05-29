/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import junit.framework.TestCase;

/***
 * Sound propagation evaluation using NMPB validation scenarios
 */
public class TestSoundPropagationValidation extends TestCase {
    private static final List<Integer> freqLvl= Collections.unmodifiableList(Arrays.asList(100, 125, 160, 200, 250, 315,
            400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000));
    private static final double ERROR_EPSILON_TEST7 = 0.57;
    private static final double ERROR_EPSILON_TEST8 = 0.79;
    private static final double ERROR_EPSILON_TEST9 = 0.59;
    private static final double ERROR_EPSILON_TEST10 = 3.2;

	private double[] splCompute(PropagationProcess propManager,Coordinate receiverPosition) {
		double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
		propManager.computeSoundLevelAtPosition(receiverPosition, energeticSum, debug);
		return energeticSum;
	}

	private void splCompare(double[] resultW,String testName,double[] expectedLevel, double splEpsilon) {
        for(int i=0; i<resultW.length; i++) {
            double dba = PropagationProcess.wToDba(resultW[i]);
            double expected = expectedLevel[i];
            assertEquals("Unit test "+testName+" failed at "+freqLvl.get(i)+" Hz",expected, dba,splEpsilon);
        }
	}

    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for(double db_m : dbValues) {
            ret.add(PropagationProcess.dbaToW(db_m));
        }
        return ret;
    }

    /**
     * Sound propagation
     * One source, One receiver, no buildings, two ground area and no topography.
     * @throws LayerDelaunayError
     */
	public void testScene7() throws LayerDelaunayError {
		GeometryFactory factory = new GeometryFactory();
		////////////////////////////////////////////////////////////////////////////
		//Add road source as one point
		List<Geometry> srclst=new ArrayList<Geometry>(); 
		srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
		//Scene dimension
		Envelope cellEnvelope=new Envelope(new Coordinate(-170., -170.,0.),new Coordinate(170, 170,0.));
		//Add source sound level
		List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,50,-50,50)),0.6));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50,100,-50,50)),0.9));
		//Build query structure for sources
		QueryGeometryStructure sourcesIndex = new QueryQuadTree();
		int idsrc=0;
		for(Geometry src : srclst) {
			sourcesIndex.appendGeometry(src, idsrc);
			idsrc++;
		}
		//Create obstruction test object
		MeshBuilder mesh = new MeshBuilder();
		mesh.finishPolygonFeeding(cellEnvelope);
		
		//Retrieve Delaunay triangulation of scene
		List<Coordinate> vert=mesh.getVertices();
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

		PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200,200, 1., 0., 0, null,geoWithSoilTypeList, false);
		PropagationProcessOut propDataOut=new PropagationProcessOut();
		PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
		propManager.initStructures();

		//Run test
		splCompare(splCompute(propManager, new Coordinate(100, 20, 1)), "Scene 7", new double[]{22.8,23.8,25.8,28.7,
                30.7,33.7,35.6,34.5,28.7,25.4,23.0,22.7,24.0,23.8,23.2,23.3,21.9,20.8}, ERROR_EPSILON_TEST7);
	}

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and no topography.
     * @throws LayerDelaunayError
     */
    public void testScene8() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst=new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope=new Envelope(new Coordinate(-170., -170.,0.),new Coordinate(170, 170,0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum=new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,35,-100,100)),0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(35,100,-100,100)),1));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc=0;
        for(Geometry src : srclst) {
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
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert=mesh.getVertices();
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        //TODO clarify, is the unit test result require the computation of Vertical diffraction + Horizontal diffraction ?
        PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200,200, 1., 0., 0, null,geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut=new PropagationProcessOut();
        PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 20, 2)), "Scene 8", new double[]{19.9, 19.8, 20.5,
                        22.2, 22.8, 24.4, 25.0, 26.7, 25.4, 27.2, 27.1, 25.0, 22.8, 19.7, 16.5, 14.2, 10.8, 8.1},
                ERROR_EPSILON_TEST8);
    }

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and no topography.
     * @throws LayerDelaunayError
     */
    public void testScene9() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst=new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope=new Envelope(new Coordinate(-170., -170.,0.),new Coordinate(170, 170,0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum=new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,35,-100,100)),0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(35,100,-100,100)),1));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc=0;
        for(Geometry src : srclst) {
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

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert=mesh.getVertices();
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200,200, 1., 0., 0, null,geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut=new PropagationProcessOut();
        PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 20, 2)), "Scene 9", new double[]{18.1, 17.8, 18.3, 20.0,
                        20.7, 22.5, 23.3, 25.2, 24.1, 26.0, 26.0, 24.8, 22.8, 19.7, 16.5, 14.2, 10.8, 8.1},
                ERROR_EPSILON_TEST9);
    }

    /**
     * Sound propagation
     * One source, One receiver, one buildings, two ground area and topography.
     * @throws LayerDelaunayError
     */
    public void testScene10() throws LayerDelaunayError {
        double groundHeight = 1.5;
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst=new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 20, 0.5)));
        //Scene dimension
        Envelope cellEnvelope=new Envelope(new Coordinate(-170., -170.,0.),new Coordinate(170, 170,0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum=new ArrayList<>();
        srcSpectrum.add(asW(73.1, 74.1, 76.1, 79.1, 81.1, 84.1, 86.1, 89.1, 89.1, 92.1, 93.1, 92.1, 90.1, 87.1, 84.1,
                82.1, 79.1, 77.1));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,10,-100,100)),0));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(10,100,-100,100)),1));
        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc=0;
        for(Geometry src : srclst) {
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
        mesh.addTopographicPoint(new Coordinate(-50,100, 0));
        mesh.addTopographicPoint(new Coordinate(-50,30, 0));
        mesh.addTopographicPoint(new Coordinate(-50,10, 0));
        mesh.addTopographicPoint(new Coordinate(-50,-100, 0));
        // bottom hill
        mesh.addTopographicPoint(new Coordinate(10,100, 0));
        mesh.addTopographicPoint(new Coordinate(10,30, 0));
        mesh.addTopographicPoint(new Coordinate(10,10, 0));
        mesh.addTopographicPoint(new Coordinate(10,-100, 0));
        // top hill
        mesh.addTopographicPoint(new Coordinate(15,100, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15,30, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15,10, groundHeight));
        mesh.addTopographicPoint(new Coordinate(15,-100, groundHeight));
        // Right of scene
        mesh.addTopographicPoint(new Coordinate(100,100, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100,30, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100,10, groundHeight));
        mesh.addTopographicPoint(new Coordinate(100,-100, groundHeight));
        // Triangulation of scenery
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert=mesh.getVertices();
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 200,200, 1., 0., 0, null,geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut=new PropagationProcessOut();
        PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(50, 20, 2)), "Scene 10", new double[]{19.7, 19.6, 20.3, 22.0,
                        22.6, 24.2, 24.7, 26.5, 25.2, 27.0, 26.8, 24.8, 22.6, 19.4, 16.3, 14.0, 10.6, 7.9},
                ERROR_EPSILON_TEST10);
    }
}
