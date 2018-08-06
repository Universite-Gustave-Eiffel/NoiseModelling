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
 * Sound propagation evaluation using NMPB validation scenarios
 */
public class TestCnossos extends TestCase {
    private static final List<Integer> freqLvl= Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));
    private static final double ERROR_EPSILON_TEST_T = 0.05;

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
     * flat ground - 100m.xml
     * @throws LayerDelaunayError
     */
    public void test1() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst=new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 0.5)));
        //Scene dimension
        Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(54.8, 74.4, 86.6, 96.9, 100.0, 101.2,95.2, 87.6));
        //srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0 ));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0,5.,-50,50)),0.));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(5.,100,-50,50)),1.));
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
                freqLvl, 0, 0, 250,250, 1., 0.,0.5, 0, null,geoWithSoilTypeList, true);
        PropagationProcessOut propDataOut=new PropagationProcessOut();
        PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
        propManager.initStructures();

        //Run test
        splCompare(splCompute(propManager, new Coordinate(100, 0, 2.5)), "Test1", new double[]{3.9,23.5,35.6,40.9,42.1,48.4,42.4,27.3}, ERROR_EPSILON_TEST_T);
    }


}
