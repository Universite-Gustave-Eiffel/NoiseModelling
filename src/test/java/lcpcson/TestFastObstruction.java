package lcpcson;

import java.util.ArrayList;

import org.gdms.data.DataSourceFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import junit.framework.TestCase;

public class TestFastObstruction extends TestCase {
	//public static DataSourceFactory dsf = new DataSourceFactory();

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

	public void testCollision() throws LayerDelaunayError {
		FastObstructionTest manager = new FastObstructionTest("./");
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] building1Coords = { new Coordinate(15., 0.),
				new Coordinate(30., 0.), new Coordinate(30., 30.),
				new Coordinate(15., 30.), new Coordinate(15., 0.) };
		Polygon building1 = factory.createPolygon(
				factory.createLinearRing(building1Coords), null);
		manager.addGeometry(building1);

		//manager.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.),
		//		new Coordinate(45., 45.)));

	}
}
