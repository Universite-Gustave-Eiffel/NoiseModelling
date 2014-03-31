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
package org.noisemap.core;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.spatial.geometry.AbstractScalarSpatialFunction;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @brief Make a set of points that lies on the line of the geom with a
 *        specified maximum distance between each point.
 * 
 *        If the length of the line(s) is smaller than delta then the interior
 *        point will be returned
 * @author Nicolas Fortin
 */
public class ST_SplitLineInPoints extends AbstractScalarSpatialFunction {

	@Override
	public String getName() {
		return "ST_SplitLineInPoints";
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
		return "Split lines in multiple points with a distance step parameter between points.";
	}

	@Override
	public String getSqlOrder() {
		return "select ST_SplitLineInPoints(the_geom,delta) from myTable;";
	}

	@Override
	public Value evaluate(DataSourceFactory dsf,Value... args) throws FunctionException {
		if (args[0].isNull()) {
			return ValueFactory.createNullValue();
		} else {
			GeometryFactory gf = new GeometryFactory();
			final Geometry geom = args[0].getAsGeometry();
			final Double delta = args[1].getAsDouble();
			// Get the point first and last point of the geometry
			Coordinate[] points = geom.getCoordinates();
			return ValueFactory.createValue(gf
					.createMultiPoint(JTSUtility.splitMultiPointsInRegularPoints(points,
							delta)));
		}
	}

}