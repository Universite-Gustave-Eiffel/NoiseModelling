/**
 * NoiseMap is a scientific computation plugin for OrbisGIS to quickly evaluate the
 * noise impact on European action plans and urban mobility plans. This model is
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
 * Copyright (C) 2011-1012 IRSTV (FR CNRS 2488)
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
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.AbstractScalarFunction;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;

/**
 * 
 * @author Nicolas Fortin
 */
public class BTW_EvalSource  extends AbstractScalarFunction {
	public static final int ground_type_grass=0;
	public static final  int ground_type_rigid=1;
	public static final  double grass_a_factor=26.;
	public static final  double rigid_a_factor=26.;
	public static final  double grass_b_factor=75.;
	public static final  double rigid_b_factor=78.;
	public static final  double speed_reference=40.;
	@Override
	public Value evaluate(DataSourceFactory dsf,Value... args) throws FunctionException {
		if (args.length != 4) {
			return ValueFactory.createNullValue();
		} else {
			final double speed = args[0].getAsDouble();
			final double tw_per_hour = args[1].getAsDouble();
			final int ground_type = args[2].getAsInt();
			final boolean has_anti_vibration = args[3].getAsBoolean();

			final double a_factor,b_factor;
			if(ground_type==ground_type_grass) {
				a_factor=grass_a_factor;
				b_factor=grass_b_factor;
			}else if(ground_type==ground_type_rigid) {
				a_factor=rigid_a_factor;
				b_factor=rigid_b_factor;				
			}else{
				throw new FunctionException("Unknown ground type");
			}
			double delta_corr=0;
			if(has_anti_vibration) {
				delta_corr+=-2;
			}
			// ///////////////////////
			// Noise Tramway
			double tw_lvl = a_factor * Math.log(speed / speed_reference) +
			                b_factor +
			                delta_corr +
			                Math.log10(tw_per_hour);
			
			return ValueFactory.createValue(tw_lvl);
		}
	}

	@Override
	public String getName() {
		return "BTW_EvalSource";
	}


	@Override
	public int getType(int[] types) {
		return Type.DOUBLE;
	}
	@Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE,  //Speed Km/h
                    		ScalarArgument.DOUBLE,  //Tramway per hour
                    		ScalarArgument.DOUBLE,  //Ground category
                    		ScalarArgument.BOOLEAN  //Anti-vibration system
                    		)
            };
	}


	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to real speed in km/h (not commercial speed),tramway by hour parameters. Ground category is one of theses (0:Grass,1:Rigid).Anti-vibration system can be floating panels placed under railways.";
	}

	@Override
	public String getSqlOrder() {
		return "select BTW_EvalSource(loadSpeed,tramway_count_per_hour,ground_category,has_anti_vibration_system) from myTable;";
	}

}