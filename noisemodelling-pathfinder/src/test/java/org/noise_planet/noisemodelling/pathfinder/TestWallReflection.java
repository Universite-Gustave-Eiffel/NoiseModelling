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
package org.noise_planet.noisemodelling.pathfinder;

import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.noise_planet.noisemodelling.pathfinder.ProfileBuilder.IntersectionType.BUILDING;
import static org.noise_planet.noisemodelling.pathfinder.ProfileBuilder.IntersectionType.WALL;

public class TestWallReflection extends TestCase {

    @Test
	public void testWallVisibility () {
		// 
		// a0_____a1
		// |      |    
		// |build1|      
		// |      |    b0____b1
		// |______|   /      /
		// a3     a2 /buil2d/    c2
		//          /______/b2  /
		//          b3         /
		//                    /
		// c0________________/c1
		Coordinate a0=new Coordinate(0,9);
		Coordinate a1=new Coordinate(4,9);
		Coordinate a2=new Coordinate(4,4);
		Coordinate a3=new Coordinate(0,4);
		Coordinate b0=new Coordinate(7,5);
		Coordinate b1=new Coordinate(11,5);
		Coordinate b2=new Coordinate(9,0);
		Coordinate b3=new Coordinate(5,0);
		
		Coordinate c0=new Coordinate(0,-2);
		Coordinate c1=new Coordinate(12,-2);
		Coordinate c2=new Coordinate(16,3);
		
		
		ProfileBuilder.Wall a01=new ProfileBuilder.Wall(a0,a1,0,WALL);
        ProfileBuilder.Wall a12=new ProfileBuilder.Wall(a1,a2,0,WALL);
        ProfileBuilder.Wall a23=new ProfileBuilder.Wall(a2,a3,0,WALL);
        ProfileBuilder.Wall a30=new ProfileBuilder.Wall(a3,a0,0,WALL);

        ProfileBuilder.Wall b01=new ProfileBuilder.Wall(b0,b1,0,WALL);
        ProfileBuilder.Wall b12=new ProfileBuilder.Wall(b1,b2,0,WALL);
        ProfileBuilder.Wall b23=new ProfileBuilder.Wall(b2,b3,0,WALL);
        ProfileBuilder.Wall b30=new ProfileBuilder.Wall(b3,b0,0,WALL);

        ProfileBuilder.Wall c01=new ProfileBuilder.Wall(c0,c1,0,WALL);
        ProfileBuilder.Wall c12=new ProfileBuilder.Wall(c1,c2,0,WALL);


		//Test cases Walls face to face
		assertTrue(MirrorReceiverIterator.wallWallTest(b30, a23));
		assertTrue(MirrorReceiverIterator.wallWallTest(b30, a12));
		assertTrue(MirrorReceiverIterator.wallWallTest(b01, a12));
		assertTrue(MirrorReceiverIterator.wallWallTest(c01, c12));
		assertTrue(MirrorReceiverIterator.wallWallTest(c12, a12));
		
		//Test cases Walls hidden
		assertFalse(MirrorReceiverIterator.wallWallTest(b30, b12));
		assertFalse(MirrorReceiverIterator.wallWallTest(b30, a01));
		assertFalse(MirrorReceiverIterator.wallWallTest(b30, a30));
		assertFalse(MirrorReceiverIterator.wallWallTest(b23, a23));
		assertFalse(MirrorReceiverIterator.wallWallTest(b12, a23));
		assertFalse(MirrorReceiverIterator.wallWallTest(b30, b01));
		assertFalse(MirrorReceiverIterator.wallWallTest(b30, b23));
		
	}

	public static List<MirrorReceiverResult> getReceiverImages(Coordinate receiver, Coordinate source, List<ProfileBuilder.Wall> walls, int order) {
        MirrorReceiverIterator.It mirrorReceiverResults =
                new MirrorReceiverIterator.It(receiver, walls, order);

        List<MirrorReceiverResult> res = new ArrayList<>();
        for(MirrorReceiverResult r : mirrorReceiverResults) {
            res.add(r);
        }
        return res;
    }

    public static int[] asWallArray(MirrorReceiverResult res) {
	    int depth = 0;
        MirrorReceiverResult cursor = res;
        while(cursor != null) {
            depth++;
            cursor = cursor.getParentMirror();
        }
        int[] walls = new int[depth];
        cursor = res;
        int i=0;
        while(cursor != null) {
            walls[(depth - 1) - (i++)] = cursor.getBuildingId();
            cursor = cursor.getParentMirror();
        }
        return walls;
    }

    @Test
    public void testWallReceiverImageOrder3() {
        List<ProfileBuilder.Wall> walls = new ArrayList<>();
        walls.add(new ProfileBuilder.Wall(new Coordinate(355265.87,6688353.34), new Coordinate(355267.89,6688335.39) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355267.89,6688335.39), new Coordinate(355080.59,6688318.03) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355091.25,6688308.90), new Coordinate(355268.15,6688325.84) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355079.33,6688338.38), new Coordinate(355265.87,6688353.34) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355080.59,6688318.03), new Coordinate(355079.33,6688338.38) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355270.96,6688300.54), new Coordinate(355093.28,6688287.69) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355093.28,6688287.69), new Coordinate(355091.25,6688308.90) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355268.15,6688325.84), new Coordinate(355270.96,6688300.54) , 1, BUILDING));
        Coordinate receiver = new Coordinate(355261.53293337114, 6688329.444505501, 1.6);
        Coordinate source = new Coordinate(355104.51057583705, 6688315.152817895, 0.05);

        List<MirrorReceiverResult> res = getReceiverImages(receiver, source, walls, 3);

        // expect 6 receiver image for 3 reflection order
        assertEquals(6, res.size());
        assertArrayEquals(new int[]{2} ,asWallArray(res.get(0)));
        assertArrayEquals(new int[]{2, 1} ,asWallArray(res.get(1)));
        assertArrayEquals(new int[]{2, 1, 2} ,asWallArray(res.get(2)));
        assertArrayEquals(new int[]{1} ,asWallArray(res.get(3)));
        assertArrayEquals(new int[]{1, 2} ,asWallArray(res.get(4)));
        assertArrayEquals(new int[]{1, 2, 1} ,asWallArray(res.get(5)));

    }

    @Test
    public void testCrossTableIterator() {
        MirrorReceiverIterator.CrossTableIterator crossTableIterator = new MirrorReceiverIterator.CrossTableIterator(3, 8);
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{0}, crossTableIterator.next().toArray(new Integer[]{}));
        crossTableIterator.skipLevel();
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1}, crossTableIterator.next().toArray(new Integer[]{}));
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 0}, crossTableIterator.next().toArray(new Integer[]{}));
        crossTableIterator.skipLevel();
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 2}, crossTableIterator.next().toArray(new Integer[]{}));
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 2, 0}, crossTableIterator.next().toArray(new Integer[]{}));
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 2, 1}, crossTableIterator.next().toArray(new Integer[]{}));
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 2, 3}, crossTableIterator.next().toArray(new Integer[]{}));
        crossTableIterator.skipLevel(); // skip but we are already at max level
        assertTrue(crossTableIterator.hasNext());
        assertArrayEquals(new Integer[]{1, 2, 4}, crossTableIterator.next().toArray(new Integer[]{}));
    }

    @Test
    public void testCrossTableIterator2() {
        MirrorReceiverIterator.CrossTableIterator crossTableIterator = new MirrorReceiverIterator.CrossTableIterator(3, 8);
        crossTableIterator.setNext(new ArrayList<>(Arrays.asList(1, 7, 0)), 2);
        crossTableIterator.skipLevel();
        assertArrayEquals(new Integer[]{2}, crossTableIterator.next().toArray(new Integer[]{}));
    }


    @Test
    public void testWallReceiverImageOrder4() {

        List<ProfileBuilder.Wall> walls = new ArrayList<>();
        walls.add(new ProfileBuilder.Wall(new Coordinate(355265.87,6688353.34), new Coordinate(355267.89,6688335.39) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355267.89,6688335.39), new Coordinate(355080.59,6688318.03) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355091.25,6688308.90), new Coordinate(355268.15,6688325.84) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355079.33,6688338.38), new Coordinate(355265.87,6688353.34) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355080.59,6688318.03), new Coordinate(355079.33,6688338.38) , 2, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355270.96,6688300.54), new Coordinate(355093.28,6688287.69) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355093.28,6688287.69), new Coordinate(355091.25,6688308.90) , 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(new Coordinate(355268.15,6688325.84), new Coordinate(355270.96,6688300.54) , 1, BUILDING));
        Coordinate receiver = new Coordinate(355261.53293337114, 6688329.444505501, 1.6);
        Coordinate source = new Coordinate(355104.51057583705, 6688315.152817895, 0.05);

        List<MirrorReceiverResult> res = getReceiverImages(receiver, source, walls, 4);

        // expect 8 receiver image for 4 reflection order
        assertEquals(8, res.size());
    }

    public void testWallReceiverImage() {
        Coordinate a = new Coordinate(2, 3);
        Coordinate b = new Coordinate(6, 3);
        Coordinate c = new Coordinate(2, 1);
        Coordinate d = new Coordinate(6, 1);
        Coordinate e = new Coordinate(3, 7);
        Coordinate f = new Coordinate(7, 7);
        Coordinate g = new Coordinate(3, 5);
        Coordinate h = new Coordinate(7, 5);
        List<ProfileBuilder.Wall> walls = new ArrayList<>(8);
        // Order of walls points must be counter clock wise (from exterior of building)
        // Building 1
        walls.add(new ProfileBuilder.Wall(a, b, 0, BUILDING));
        walls.add(new ProfileBuilder.Wall(b, d, 0, BUILDING));
        walls.add(new ProfileBuilder.Wall(d, c, 0, BUILDING));
        walls.add(new ProfileBuilder.Wall(c, a, 0, BUILDING));
        // Building 2
        walls.add(new ProfileBuilder.Wall(g, e, 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(h, g, 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(f, h, 1, BUILDING));
        walls.add(new ProfileBuilder.Wall(e, f, 1, BUILDING));
        Coordinate receiver = new Coordinate(0, 4);
        Coordinate source = new Coordinate(9, 4);

        MirrorReceiverIterator.It mirrorReceiverResults =
                new MirrorReceiverIterator.It(receiver, walls,  2);
        Iterator<MirrorReceiverResult> it = mirrorReceiverResults.iterator();
        wallTest(new Coordinate(0, 2), new int[]{0}, it.next());
        wallTest(new Coordinate(6, 2), new int[]{0, 4}, it.next());
        wallTest(new Coordinate(0, 8), new int[]{0, 5}, it.next());
        wallTest(new Coordinate(4, 4), new int[]{3}, it.next());
        wallTest(new Coordinate(6, 4), new int[]{4}, it.next());
        wallTest(new Coordinate(6, 2), new int[]{4, 0}, it.next());
        wallTest(new Coordinate(0, 6), new int[]{5}, it.next());
        wallTest(new Coordinate(0, 0), new int[]{5, 0}, it.next());
        assertFalse(it.hasNext());
    }

    private void wallTest(Coordinate expectedCoordinate,int[] expectedWalls,MirrorReceiverResult res) {
        int[] resultWalls = new int[expectedWalls.length];
        int id = 0;
        MirrorReceiverResult cursor = res;
        while(cursor != null) {
            if(expectedWalls.length - 1 - id >= 0) {
                resultWalls[expectedWalls.length - 1 - id] = cursor.getWallId();
            }
            cursor = cursor.getParentMirror();
            id++;
        }
        assertArrayEquals(expectedWalls, resultWalls);
        assertEquals(expectedCoordinate, res.getReceiverPos());
    }

    private void equalsTest(int[] expected, List<Integer> result) {
        for(int i=0;i<result.size();i++) {
            assertEquals(expected[i], result.get(i).intValue());
        }
    }

    @Test
    public void testWallIndexIt() {
        MirrorReceiverIterator.CrossTableIterator it = new MirrorReceiverIterator.CrossTableIterator(2, 3);
        equalsTest(new int[]{0}, it.next());
        equalsTest(new int[]{0,1}, it.next());
        equalsTest(new int[]{0,2}, it.next());
        equalsTest(new int[]{1}, it.next());
        equalsTest(new int[]{1,0}, it.next());
        equalsTest(new int[]{1,2}, it.next());
        equalsTest(new int[]{2}, it.next());
        equalsTest(new int[]{2,0}, it.next());
        equalsTest(new int[]{2,1}, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testWallIndexItSkip() {
        MirrorReceiverIterator.CrossTableIterator it = new MirrorReceiverIterator.CrossTableIterator(2, 3);
        equalsTest(new int[]{0}, it.next());
        equalsTest(new int[]{0,1}, it.next());
        equalsTest(new int[]{0, 2}, it.next());
        equalsTest(new int[]{1}, it.next());
        it.skipLevel();
        equalsTest(new int[]{2}, it.next());
        equalsTest(new int[]{2, 0}, it.next());
        it.skipLevel();
        equalsTest(new int[]{2, 1}, it.next());
    }

    public void testSingleWall() {
        MirrorReceiverIterator.CrossTableIterator it = new MirrorReceiverIterator.CrossTableIterator(2, 1);
        equalsTest(new int[]{0}, it.next());
        assertFalse(it.hasNext());
    }

    public void testPath() throws ParseException, LayerDelaunayError {

        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(wktReader.read("POLYGON ((316900.21711186244 6703891.837263795, 316903.24859771173 6703904.468454833, 316926.44405761914 6703898.451414739, 316925.433562336 6703889.678478417, 316914.1343878069 6703892.480306247, 316913.53727695777 6703890.367452473, 316906.78533120255 6703892.066921813, 316906.4178783723 6703890.32152087, 316900.21711186244 6703891.837263795))"), 11.915885805791621);
        builder.addBuilding(wktReader.read("POLYGON ((316886.41 6703903.61, 316888.31 6703910.59, 316899.79 6703907.69, 316897.99 6703900.71, 316886.41 6703903.61))"), 13.143551238469575);

        builder.finishFeeding();


        CnossosPropagationData data = new CnossosPropagationData(builder);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(data);
        data.reflexionOrder = 1;
	    Coordinate receiver = new Coordinate(316898.0027227718, 6703891.69841584, 4);
	    Coordinate source = new Coordinate(316900.8845049501,6703903.754851485, 0.05);
        List<PropagationPath> paths;
        paths = computeRays.computeReflexion(receiver, source, false);
        assertEquals(1, paths.size());
        List<PointPath> pts = paths.get(0).getPointList();
        assertEquals(3, pts.size());
        assertEquals(PointPath.POINT_TYPE.SRCE, pts.get(0).type);
        assertEquals(0, source.distance(pts.get(0).coordinate), 1e-6);
        assertEquals(PointPath.POINT_TYPE.REFL, pts.get(1).type);
        assertEquals(0, new Coordinate(316901.506, 6703897.22, 2.14).distance(pts.get(1).coordinate), 0.01);
        assertEquals(PointPath.POINT_TYPE.RECV, pts.get(2).type);
        assertEquals(0, receiver.distance(pts.get(2).coordinate), 1e-6);

        data.reflexionOrder = 2;
        paths = computeRays.computeReflexion(receiver, source, false);
        assertEquals(2, paths.size());
        pts = paths.get(0).getPointList();
        // 2 ref points
        assertEquals(4, pts.size());
        assertEquals(PointPath.POINT_TYPE.SRCE, pts.get(0).type);
        assertEquals(0, source.distance(pts.get(0).coordinate), 1e-6);
        assertEquals(PointPath.POINT_TYPE.REFL, pts.get(1).type);
        assertEquals(0, new Coordinate(316898.18, 6703901.42, 0.99).distance(pts.get(1).coordinate), 1);
        assertEquals(PointPath.POINT_TYPE.REFL, pts.get(2).type);
        assertEquals(0, new Coordinate(316900.80, 6703894.30, 3).distance(pts.get(2).coordinate), 1);
        assertEquals(PointPath.POINT_TYPE.RECV, pts.get(3).type);
        assertEquals(0, receiver.distance(pts.get(3).coordinate), 1e-6);
    }
}
