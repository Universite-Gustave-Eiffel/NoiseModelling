/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able
 * to manipulate and create vector and raster spatial information. OrbisGIS
 * is distributed under GPL 3 license. It is produced  by the geo-informatic team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/>, CNRS FR 2488:
 *    Erwan BOCHER, scientific researcher,
 *    Thomas LEDUC, scientific researcher,
 *    Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OrbisGIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult:
 *    <http://orbisgis.cerma.archi.fr/>
 *    <http://sourcesup.cru.fr/projects/orbisgis/>
 *
 * or contact directly:
 *    erwan.bocher _at_ ec-nantes.fr
 *    fergonco _at_ gmail.com
 *    thomas.leduc _at_ cerma.archi.fr
 */
package lcpc_son;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.spatial.geometry.AbstractSpatialFunction;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
/**
 * 
 * @author Nicolas Fortin
 * @brief Make a set of points that lies on the line of the geom with a specified maximum distance between each point.
 * 
 * If the length of the line(s) is smaller than delta then the interior point will be returned
 */
public class ST_SplitLineInPoints extends AbstractSpatialFunction {
	static public Coordinate[] SplitMultiPointsInRegularPoints(Coordinate[] points,double delta)
	{
		GeometryFactory gf = new GeometryFactory();
		if(points.length==0)
			return null;
		if(points.length==1)
			return points;
		//If the distance between the first and the last point is smaller than delta, then return the avg point
		LineString Line=gf.createLineString(points);
		Double length=Line.getLength();
		if(length<delta)
		{
			if(points.length==2)
			{
				Coordinate[] coords={new Coordinate((points[1].x+points[0].x)/2.,
						(points[1].y+points[0].y)/2.,
						(points[1].y+points[0].y)/2.)};
				return coords;
			}else{
				return Line.getInteriorPoint().getCoordinates();
			}
		}
		//We can't set an absolute value, but we can provide a way to compute
		//a delta value that will build a regular set of points with the constraint of a maximum delta value.
		Double ModifiedDelta=length/Math.ceil(length/delta);
		//The line length is greater than Delta, then we will create a set of points where each point lies at the distance of the modified delta
		int nbpts=(int) Math.ceil(length/delta);
		Coordinate[] deltaPoints=new Coordinate[nbpts];
		int pts=0;
		int cursorPts=1; //This is the navigation cursor inside the points array
		Coordinate refpt=points[0];
		Double distToNextPt=ModifiedDelta;
		while(pts<nbpts)
		{
			Coordinate nextbldpts;
			Double distToNextControlPt=refpt.distance(points[cursorPts]);
			while(distToNextControlPt<distToNextPt && points.length!=cursorPts+1 )
			{
				//The distance with the next control point is not sufficient to build a new point
				distToNextPt-=distToNextControlPt;
				refpt=points[cursorPts];
				cursorPts++;
				distToNextControlPt=refpt.distance(points[cursorPts]);
			}
			if(distToNextControlPt>=distToNextPt)
			{
				//AB Normalized vector
				Coordinate A=refpt;
				Coordinate B=points[cursorPts];
				Double ABlen=B.distance(A);
				Coordinate AB=new Coordinate((B.x-A.x)/ABlen,
						(B.y-A.y)/ABlen,
						(B.z-A.z)/ABlen);
				//Compute intermediate P vector
				Coordinate P=new Coordinate(A.x+AB.x*distToNextPt,
						A.y+AB.y*distToNextPt,
						A.z+AB.z*distToNextPt);
				nextbldpts=P;
			}else{
				nextbldpts=points[cursorPts];
			}
			refpt=nextbldpts;
			deltaPoints[pts]=nextbldpts;
			pts++;
			distToNextPt=ModifiedDelta;				
		}
		return deltaPoints;
	}

	public Value evaluate(final Value... args) throws FunctionException {
		if (args[0].isNull()) {
			return ValueFactory.createNullValue();
		} else {
			GeometryFactory gf = new GeometryFactory();
			final Geometry geom = args[0].getAsGeometry();
			final Double delta = args[1].getAsDouble();
			//Get the point first and last point of the geometry
			Coordinate[] points=geom.getCoordinates();
			return ValueFactory.createValue(gf.createMultiPoint(SplitMultiPointsInRegularPoints(points,delta)));
		}
	}

	public String getName() {
		return "ST_SplitLineInPoints";
	}

	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(Argument.GEOMETRY,Argument.NUMERIC) };
	}

	public boolean isAggregate() {
		return false;
	}

	public String getDescription() {
		return "Split lines in multiple points with a distance step parameter between points.";
	}

	public String getSqlOrder() {
		return "select ST_SplitLineInPoints(the_geom,delta) from myTable;";
	}

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args)
			throws FunctionException {
		// TODO Auto-generated method stub
		return null;
	}

}