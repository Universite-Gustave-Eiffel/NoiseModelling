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
package org.noise_planet.noisemodelling.propagation;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.algorithm.RectangleLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class TestFastObstruction extends TestCase {

	static boolean isBarelyEqual(double v1, double v2) {
		return Math.abs(v1 - v2) < 1e-7;
	}

	private void checkAngle(double angle1, double angle2) {
		assertTrue(Math.cos(angle1) + "!=" + Math.cos(angle2) + "(" + angle1
				+ "!=" + angle2 + ")",
				isBarelyEqual(Math.cos(angle1), Math.cos(angle2)));
		assertTrue(Math.sin(angle1) + "!=" + Math.sin(angle2) + "(" + angle1
				+ "!=" + angle2 + ")",
				isBarelyEqual(Math.sin(angle1), Math.sin(angle2)));
	}

	private void checkMerge(double[] ccw_values, double assertCCW1,
			double assertCCW2) {
		ArrayList<Double> assertShortcut;
		ArrayList<ArrayList<Double>> verticesAngle = new ArrayList<ArrayList<Double>>();
		for (int rangeid = 0; rangeid < ccw_values.length - 1; rangeid += 2) {
			verticesAngle.add(new ArrayList<Double>());
			FastObstructionTest.updateMinMax(
					0,
					new Coordinate(0, 0),
					new Coordinate(Math.cos(ccw_values[rangeid]), Math
							.sin(ccw_values[rangeid])),
					new Coordinate(Math.cos(ccw_values[rangeid + 1]), Math
							.sin(ccw_values[rangeid + 1])), verticesAngle);
		}
		assertShortcut = verticesAngle.get(0);
		assertTrue("Merging of open angle failed, too many elements !",
				assertShortcut.size() == 2);
		checkAngle(assertShortcut.get(0), assertCCW1);
		checkAngle(assertShortcut.get(1), assertCCW2);
	}

	/**
	 * Test classification/fusion of open angles (help to compute translation
	 * epsilon of diffraction edges)
	 */
	public void testAngleOrdering() {
		// Test case merge of angle ranges, cover all cases
		double[] values1 = { (7. / 4.) * Math.PI, (1. / 4.) * Math.PI,
				(1. / 4.) * Math.PI, (3. / 4.) * Math.PI, (3. / 4.) * Math.PI,
				(5. / 4.) * Math.PI };
		checkMerge(values1, values1[0], values1[5]);
		double[] values2 = { (7. / 4.) * Math.PI, (1. / 4.) * Math.PI,
				(3. / 4.) * Math.PI, (5. / 4.) * Math.PI, (1. / 4.) * Math.PI,
				(3. / 4.) * Math.PI };
		checkMerge(values2, values2[0], values2[3]);
		double[] values3 = { (7. / 4.) * Math.PI, (1. / 4.) * Math.PI,
				(3. / 4.) * Math.PI, (5. / 4.) * Math.PI, (1. / 4.) * Math.PI,
				(3. / 4.) * Math.PI, (5. / 4.) * Math.PI, (7. / 4.) * Math.PI };
		checkMerge(values3, values3[3], values3[3]);
		double[] values4 = { -1.08, -0.357, 1.86, -2.98, 0.424, 1.86, -0.357,
				0.424 };
		checkMerge(values4, values4[0], values4[3]);
		double[] values5 = { -1.932, -1.172, -2.693, -1.932, 1.847, 1.999,
				0.691, 1.847, -1.172, 0.396, 0.396, 0.691 };
		checkMerge(values5, values5[2], values5[5]);

	}
	public void testVoidScene() throws LayerDelaunayError {
		//Create obstruction test object
		MeshBuilder mesh = new MeshBuilder();
		mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.),
				new Coordinate(45., 45.,0.)));
                FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices()); 
		assertTrue("Void Intersection test #1 failed",manager.isFreeField(new Coordinate(5,20), new Coordinate(14,30)));
		manager.getWideAnglePoints(Math.PI * (1 + 1 / 16.0), Math.PI * (2 - (1 / 16.)));
	}
	
	public void testScene1() throws LayerDelaunayError {
		//Build Scene with One Building
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
		Polygon building1 = factory.createPolygon(
				factory.createLinearRing(building1Coords), null);
		
		//Create obstruction test object
		MeshBuilder mesh = new MeshBuilder();
		mesh.addGeometry(building1);
		mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.),
				new Coordinate(45., 45.,0.)));
                FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
                
		//Run intersection test
		collisionTask(manager);
		//Run wide angle detection
		List<Coordinate> openAngle=openAngleTask(manager,building1Coords);
		int freefieldcpt=0;
		Coordinate receiver=new Coordinate(5,20,0);
		for(Coordinate corner : openAngle)
		{
			if(manager.isFreeField(receiver, corner)) {
				freefieldcpt++;
			}
		}
		assertTrue("Missed visible corner or found more than visible",freefieldcpt==2);
	}
	private void collisionTask(FastObstructionTest manager) throws LayerDelaunayError {

		assertTrue("Intersection test #1 failed",manager.isFreeField(new Coordinate(5,20), new Coordinate(14,30)));
		assertFalse("Intersection test #2 failed",manager.isFreeField(new Coordinate(5,20), new Coordinate(16,31)));

	}
	private List<Coordinate> openAngleTask(FastObstructionTest manager,Coordinate[] buildingCoords) throws LayerDelaunayError {
		List<Coordinate> wideangle=manager.getWideAnglePoints(Math.PI * (1 + 1 / 16.0), Math.PI * (2 - (1 / 16.)));
		assertTrue("Too many corners found",wideangle.size()==4);
		for(Coordinate buildingCorner : buildingCoords)
		{
			boolean found=false;
			for(Coordinate widangl : wideangle) {
				double dist=widangl.distance(buildingCorner);
				if(dist>FastObstructionTest.wideAngleTranslationEpsilon-FastObstructionTest.epsilon && dist<FastObstructionTest.wideAngleTranslationEpsilon+FastObstructionTest.epsilon) {
					found=true;
					break;
				}
			}
			assertTrue("Corner at "+buildingCorner+" of building not found !",found);				
		}
		return wideangle;
	}

    /**
     * Sound propagation path over a building
     * @throws LayerDelaunayError
     */
    public void testOverBuilding() throws LayerDelaunayError {
        //Build Scene with One Building
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
                new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
                new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
        Polygon building1 = factory.createPolygon(factory.createLinearRing(building1Coords));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(building1, 5);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.),
                new Coordinate(45., 45.,0.)));
        FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
        // Over building
        assertTrue(manager.isFreeField(new Coordinate(5, 20, 5.5), new Coordinate(40, 20, 5.5)));
        // Top-down - intersection in (30,20,3.2)
        assertFalse(manager.isFreeField(new Coordinate(5, 20, 9), new Coordinate(40, 20, 1)));
        // Top-down - intersection in (21,20,4.8)
        assertFalse(manager.isFreeField(new Coordinate(5, 20, 8), new Coordinate(40, 20, 1)));
        // Top-down - intersection in (15,20,4.5)
        assertFalse(manager.isFreeField(new Coordinate(5, 20, 6), new Coordinate(40, 20, 1)));
        // Down-up - intersection in (15,20,3.2)
        assertFalse(manager.isFreeField(new Coordinate(5, 20, 1), new Coordinate(40, 20, 9)));

    }
	/**
	 * Sound propagation path over a building
	 * @throws LayerDelaunayError
	 */
	public void testOverBuilding2() throws LayerDelaunayError, ParseException {

		WKTReader wktReader = new WKTReader();
		Geometry building1 = wktReader.read("MULTIPOLYGON (((223245.10954126046 6757870.685251366, 223254.3576750219 6757883.725402858, 223265.81413628225 6757875.6004328905, 223256.56738751417 6757862.559298952, 223245.10954126046 6757870.685251366)))");
		Geometry building2 = wktReader.read("MULTIPOLYGON (((223243.47480791857 6757871.842867576, 223178.29477526256 6757917.929971755, 223183.61783164035 6757925.556583197, 223248.83068443503 6757879.444709641, 223243.47480791857 6757871.842867576)))");

		MeshBuilder mesh = new MeshBuilder();
		mesh.addGeometry(building1, 14);
		mesh.addGeometry(building2, 14);

		mesh.finishPolygonFeeding(new Envelope(new Coordinate(223127.3190158577, 6757833.636902214),
				new Coordinate(223340.4209403399, 6757947.187562704)));
		FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());

		// Over building
		assertTrue(manager.isFreeField(new Coordinate(223245.77914329473, 6757881.602448081, 14.0000001), new Coordinate(223230.85137195565, 6757927.403564689, 0.050000100000000006)));


	}

	@Test
	public void testIntersectionRayEnvelope() {
		Envelope env = new Envelope(new Coordinate(2, 3), new Coordinate(6, 6));
		RectangleLineIntersector rect = new RectangleLineIntersector(env);
		assertFalse(rect.intersects(new Coordinate(5, 1), new Coordinate(8, 5)));
	}
}
