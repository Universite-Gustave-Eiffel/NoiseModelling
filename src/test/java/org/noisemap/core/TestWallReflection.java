package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import junit.framework.TestCase;

public class TestWallReflection extends TestCase {

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
		
		
		LineSegment a01=new LineSegment(a0,a1);
		LineSegment a12=new LineSegment(a1,a2);
		LineSegment a23=new LineSegment(a2,a3);
		LineSegment a30=new LineSegment(a3,a0);
		
		LineSegment b01=new LineSegment(b0,b1);
		LineSegment b12=new LineSegment(b1,b2);
		LineSegment b23=new LineSegment(b2,b3);
		LineSegment b30=new LineSegment(b3,b0);

		LineSegment c01=new LineSegment(c0,c1);
		LineSegment c12=new LineSegment(c1,c2);
		//Test cases Walls face to face
		assertTrue(PropagationProcess.wallWallTest(b30, a23));
		assertTrue(PropagationProcess.wallWallTest(b30, a12));
		assertTrue(PropagationProcess.wallWallTest(b01, a12));
		assertTrue(PropagationProcess.wallWallTest(c01, c12));
		assertTrue(PropagationProcess.wallWallTest(c12, a12));
		
		//Test cases Walls hidden
		assertFalse(PropagationProcess.wallWallTest(b30, b12));
		assertFalse(PropagationProcess.wallWallTest(b30, a01));
		assertFalse(PropagationProcess.wallWallTest(b30, a30));
		assertFalse(PropagationProcess.wallWallTest(b23, a23));
		assertFalse(PropagationProcess.wallWallTest(b12, a23));
		assertFalse(PropagationProcess.wallWallTest(b30, b01));
		assertFalse(PropagationProcess.wallWallTest(b30, b23));
		
	}
}
