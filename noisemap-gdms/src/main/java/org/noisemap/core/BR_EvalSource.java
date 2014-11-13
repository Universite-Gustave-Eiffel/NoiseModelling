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
import org.gdms.data.types.Type;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.AbstractScalarFunction;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.orbisgis.noisemap.core.EvalRoadSource;

/**
 * 
 * @author Nicolas Fortin
 */
public class BR_EvalSource  extends AbstractScalarFunction {

	@Override
	public Value evaluate(DataSourceFactory dsf,Value... args) throws FunctionException {
		if (args.length < 3) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 10) {
			throw new FunctionException("Too many parameters !");
		} else {
			// Basic arguments
			double speed_load = args[0].getAsDouble();
			int vl_per_hour = args[1].getAsInt();
			int pl_per_hour = args[2].getAsInt();
            if(args.length == 10) {
                double speed_junction = args[3].getAsDouble();
                double speed_max = args[4].getAsDouble();
                int copound_roadtype = args[5].getAsInt();
                double begin_z = args[6].getAsDouble();
                double end_z = args[7].getAsDouble();
                double road_length = args[8].getAsDouble();
                boolean is_queue = args[9].getAsBoolean();
                return ValueFactory.createValue(EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour,
                        speed_junction, speed_max, copound_roadtype, begin_z, end_z, road_length, is_queue));
            } else if(args.length == 6) {
                double slope = EvalRoadSource.computeSlope(args[3].getAsDouble(),args[4].getAsDouble(),
                        args[5].getAsDouble());
                return ValueFactory.createValue(EvalRoadSource.evaluate(vl_per_hour, pl_per_hour, speed_load, speed_load,
                        slope));
            } else {
                return ValueFactory.createValue(EvalRoadSource.evaluate(speed_load, vl_per_hour, pl_per_hour));
			}
		}
	}

	@Override
	public String getName() {
		return "BR_EvalSource";
	}

	@Override
	public int getType(int[] types) {
		return Type.DOUBLE;
	}
	@Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE  // heavy vehicle
                    		
                    ),
                    new BasicFunctionSignature(getType(null),
                            ScalarArgument.DOUBLE, // load speed
                            ScalarArgument.DOUBLE,    // light vehicle
                            ScalarArgument.DOUBLE,    // heavy vehicle
                            ScalarArgument.DOUBLE, // Z begin
                            ScalarArgument.DOUBLE, // Z end
                            ScalarArgument.DOUBLE  // Road length(m) (without taking account of
                                                   // the Z coordinate)

                    ),new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE, // heavy vehicle
                    		ScalarArgument.DOUBLE, // junction speed
                    		ScalarArgument.DOUBLE, // speed limit
                    		ScalarArgument.DOUBLE, // Road Type XY
                    		ScalarArgument.DOUBLE, // Z begin
                    		ScalarArgument.DOUBLE, // Z end
                    		ScalarArgument.DOUBLE, // Road length(m) (without taking account of
    											   // the Z coordinate)
                    		ScalarArgument.BOOLEAN // Is queue (use junction speed as effective
												   // speed)
                    )
            };
    }

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the parameters.You can specify from 3 to 10 parameters.";
	}

	@Override
	public String getSqlOrder() {
		return "BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount[,junction speed,speedMax,roadType[,Zbegin,Zend,roadLength[,isqueue]]])";
	}
}