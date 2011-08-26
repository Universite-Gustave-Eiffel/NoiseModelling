/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.spatial.geometry.AbstractScalarSpatialFunction;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author Nicolas Fortin
 * @brief Split StringLine (2 vertices) into MultiStringLine (2 String Line) if
 *        the length of the line is smaller than the split length parameter
 */
public class ST_SplitSegment extends AbstractScalarSpatialFunction {


	public Value evaluate(SQLDataSourceFactory dsf,Value... args) throws FunctionException {
		if (args[0].isNull()) {
			return ValueFactory.createNullValue();
		} else {
			GeometryFactory gf = new GeometryFactory();
			final Geometry geom = args[0].getAsGeometry();
			final Double endLength = args[1].getAsDouble();
			// Get the point first and last point of the geometry
			Coordinate[] points = geom.getCoordinates();
			if (points.length < 2) {
				return ValueFactory.createNullValue();
			}

			// If the distance between the first and the last point is smaller
			// than delta, then return the avg point
			LineString Line = gf.createLineString(points);
			Double length = Line.getLength();

			if (length <= endLength || endLength < 0.0000001) {
				return args[0];
			}
			final double delta = length - endLength;
			// The line length is greater than Delta, then we will create a set
			// of points where each point lies at the distance of the modified
			// delta
			int nbpts = (int) Math.ceil(length / delta);

			CoordinateList beginSegment = new CoordinateList();
			CoordinateList endSegment = new CoordinateList();
			int pts = 0;
			int cursorPts = 1; // This is the navigation cursor inside the
								// points array
			Coordinate refpt = points[0];
			Double distToNextPt = delta;
			boolean splitted = false;
			beginSegment.add(points[0], false);
			while (pts < nbpts) {
				Coordinate nextbldpts;
				Double distToNextControlPt = refpt.distance(points[cursorPts]);
				while (distToNextControlPt < distToNextPt
						&& points.length != cursorPts + 1) {
					// The distance with the next control point is not
					// sufficient to build a new point
					// Append the intermediate pt into the correct list
					if (splitted) {
						endSegment.add(points[cursorPts], false);
					} else {
						beginSegment.add(points[cursorPts], false);
					}
					distToNextPt -= distToNextControlPt;
					refpt = points[cursorPts];
					cursorPts++;
					distToNextControlPt = refpt.distance(points[cursorPts]);
				}
				if (distToNextControlPt >= distToNextPt) {
					// AB Normalized vector
					Coordinate A = refpt;
					Coordinate B = points[cursorPts];
					Double ABlen = B.distance(A);
					Coordinate AB = new Coordinate((B.x - A.x) / ABlen,
							(B.y - A.y) / ABlen, (B.z - A.z) / ABlen);
					// Compute intermediate P vector
					Coordinate P = new Coordinate(A.x + AB.x * distToNextPt,
							A.y + AB.y * distToNextPt, A.z + AB.z
									* distToNextPt);
					nextbldpts = P;
				} else {
					nextbldpts = points[cursorPts];
				}
				refpt = nextbldpts;
				if (splitted) {
					endSegment.add(nextbldpts, false);
				} else {
					beginSegment.add(nextbldpts, false);
					endSegment.add(nextbldpts, false);
				}
				pts++;
				distToNextPt = delta;
				splitted = true;
			}
			LineString[] linearray = {
					gf.createLineString(beginSegment.toCoordinateArray()),
					gf.createLineString(endSegment.toCoordinateArray()) };
			return ValueFactory
					.createValue(gf.createMultiLineString(linearray));
		}
	}
	@Override
	public String getName() {
		return "ST_SplitSegment";
	}


	@Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.GEOMETRY, 
                    		ScalarArgument.DOUBLE
                    		
                    )
            };
	}

	@Override
	public String getDescription() {
		return "Split StringLine (2 vertices) into MultiStringLine (2 String Line) if the length of the line is smaller than the split length parameter.";
	}

	@Override
	public String getSqlOrder() {
		return "select ST_SplitSegment(the_geom,splitLength) from myTable;";
	}
}