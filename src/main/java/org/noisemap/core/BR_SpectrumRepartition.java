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

import java.util.HashMap;


import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;


import org.gdms.sql.function.BasicFunctionSignature;

import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.AbstractScalarFunction;

/**
 * 
 * @author Nicolas Fortin
 */
public class BR_SpectrumRepartition extends AbstractScalarFunction {

	private HashMap<Integer, Integer> freqToIndex = new HashMap<Integer, Integer>();
	private double[] non_pervious_att = { -27, -26, -24, -21, -19, -16, -14,
			-11, -11, -8, -7, -8, -10, -13, -16, -18, -21, -23 };

	public BR_SpectrumRepartition() {
		super();
		freqToIndex.put(100, 0);
		freqToIndex.put(125, 1);
		freqToIndex.put(160, 2);
		freqToIndex.put(200, 3);
		freqToIndex.put(250, 4);
		freqToIndex.put(315, 5);
		freqToIndex.put(400, 6);
		freqToIndex.put(500, 7);
		freqToIndex.put(630, 8);
		freqToIndex.put(800, 9);
		freqToIndex.put(1000, 10);
		freqToIndex.put(1250, 11);
		freqToIndex.put(1600, 12);
		freqToIndex.put(2000, 13);
		freqToIndex.put(2500, 14);
		freqToIndex.put(3150, 15);
		freqToIndex.put(4000, 16);
		freqToIndex.put(5000, 17);
	}

	public double getAttenuatedValue(int freq) throws FunctionException {
		if (freqToIndex.containsKey(freq)) {
			return non_pervious_att[freqToIndex.get(freq)];
		} else {
			throw new FunctionException("The frequency " + freq
					+ " Hz is unknown !");
		}
	}

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
		if (args.length < 3) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 3) {
			throw new FunctionException("Too many parameters !");
		} else {
			return ValueFactory.createValue(args[2].getAsDouble()
					+ getAttenuatedValue(args[0].getAsInt()));
		}
	}

	@Override
	public String getName() {
		return "BR_SpectrumRepartition";
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
                    		ScalarArgument.INT, // Category of road surface [Pervious, Non
							// Pervious]
                    		ScalarArgument.DOUBLE // Global SPL value (dBA)
                    		)
            };
    }
                    		
	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the the third octave frequency band. First parameter is Frequency band one of [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000], second parameter is the category of the road surface [1:Pervious,2:Non Pervious], third parameter is the global dB(A) Spl Value.";
	}

	@Override
	public String getSqlOrder() {
		return "select BR_SpectrumRepartition(100,1,dbA) as dbA_100 from myTable;";
	}

}
