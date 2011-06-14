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
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionValidator;

// select id, Log10(id) from points;

public class Log10 implements Function {

	public Value evaluate(final Value... args) throws FunctionException {
		if (args[0].isNull()) {
			return ValueFactory.createNullValue();
		} else {
			final double value = args[0].getAsDouble();
			return ValueFactory.createValue(Math.log10(value));
		}
	}

	@Override
	public String getName() {
		return "Log10";
	}

	@Override
	public boolean isAggregate() {
		return false;
	}

	@Override
	public Type getType(Type[] types) {
		return TypeFactory.createType(Type.DOUBLE);
	}

	@Override
	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(Argument.NUMERIC) };
	}

	public void validateTypes(Type[] argumentsTypes) {
		FunctionValidator.failIfBadNumberOfArguments(this, argumentsTypes, 1);
		FunctionValidator.failIfNotNumeric(this, argumentsTypes[0]);
	}

	@Override
	public String getDescription() {
		return "Returns the base 10 logarithm of a double value";
	}

	@Override
	public String getSqlOrder() {
		return "select Log10(myNumericField) from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args)
			throws FunctionException {
		// TODO Auto-generated method stub
		return null;
	}

}