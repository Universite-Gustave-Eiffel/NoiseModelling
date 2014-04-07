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
import org.orbisgis.noisemap.core.TramSpectrumRepartition;

/**
 * 
 * @author Nicolas Fortin
 */
public class BTW_SpectrumRepartition extends AbstractScalarFunction {

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
		if (args.length < 2) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 2) {
			throw new FunctionException("Too many parameters !");
		} else {
			return ValueFactory.createValue(args[1].getAsDouble()
					+ TramSpectrumRepartition.getAttenuatedValue(args[0].getAsInt()));
		}
	}

	@Override
	public String getName() {
		return "BTW_SpectrumRepartition";
	}


	@Override
	public int getType(int[] types) {
		return Type.DOUBLE;
	}

	   
    @Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.INT,  // Frequency
							// [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000]
                    		ScalarArgument.DOUBLE // Global SPL value (dBA)
                    		)
            };
    }

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the the third octave frequency band. First parameter is Frequency band one of [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000] third parameter is the global dB(A) Spl Value.";
	}

	@Override
	public String getSqlOrder() {
		return "select BTW_SpectrumRepartition(100,dbA) as dbA_100 from myTable;";
	}

}
