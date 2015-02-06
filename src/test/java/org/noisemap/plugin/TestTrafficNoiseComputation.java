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
package org.noisemap.plugin;

import org.gdms.data.values.ValueFactory;
import org.gdms.data.types.IncompatibleTypesException;
import org.gdms.sql.function.FunctionException;
import org.noisemap.core.BR_EvalSource;
import org.noisemap.core.TestSoundPropagationValidation;

import junit.framework.TestCase;

public class TestTrafficNoiseComputation extends TestCase {
        public static double splEpsilon=0.001;
	private void doSplAssert(String parameters,double expecteddB,double computeddB) {
		assertTrue("Road noise estimation ("+parameters+") failed. Expected value is "+expecteddB+" dB(A), computed value is "+computeddB+" dB(A).", TestSoundPropagationValidation.isSameDbValues(computeddB, expecteddB,splEpsilon));
	}
	public void testRoadVehicleNoise() throws IncompatibleTypesException, FunctionException {

		//3 Components traffic information
		BR_EvalSource noiseEval=new BR_EvalSource();
		double value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(1.),ValueFactory.createValue(0.)).getAsDouble();
		doSplAssert("1 vl/h @ 50km/h",50.858,value);
		value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(0.),ValueFactory.createValue(0.)).getAsDouble();
		doSplAssert("0 tv/h",Double.NEGATIVE_INFINITY,value);
		value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(0.),ValueFactory.createValue(1.)).getAsDouble();
		doSplAssert("1 pl/h @ 50km/h",60.002,value);
		
		//6 Components traffic information 
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(100.),ValueFactory.createValue(0.),ValueFactory.createValue(32),ValueFactory.createValue(50),ValueFactory.createValue(53)).getAsDouble();
		doSplAssert("100 vl/h @ 40km/h (junct 32,max 50), extra boulevard Street",63.03,value);


                //BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount
                //              [,junction speed,speedMax,roadType
                //              [,Zbegin,Zend,roadLength
                //              [,isqueue]]])
		//10 Components traffic information
                //                             loadSpeed                    ,lightVehicleCount            ,heavyVehicleCount            ,junction speed              ,speedMax                    ,roadType                    ,Zbegin                     ,Zend                       ,roadLength                   ,isqueue
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(50.),ValueFactory.createValue(50.),ValueFactory.createValue(32),ValueFactory.createValue(50),ValueFactory.createValue(53),ValueFactory.createValue(0),ValueFactory.createValue(5),ValueFactory.createValue(45.),ValueFactory.createValue(false)).getAsDouble();
		doSplAssert("50 vl/h 50 pl/h @ 40km/h",81.686,value);

                //Other 10 Components, restart engine < 25 km/h
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(793.),ValueFactory.createValue(49.),ValueFactory.createValue(1.75),ValueFactory.createValue(40),ValueFactory.createValue(58),ValueFactory.createValue(7.9),ValueFactory.createValue(7.3),ValueFactory.createValue(8.61),ValueFactory.createValue(true)).getAsDouble();
		doSplAssert("793 vl/h 49 pl/h @ 1.72km/h",82.725,value);

                //Another
                //                             loadSpeed                    ,lightVehicleCount             ,heavyVehicleCount            ,junction speed               ,speedMax                     ,roadType                    ,Zbegin                       ,Zend                         ,roadLength                    ,isqueue
                value=noiseEval.evaluate(null, ValueFactory.createValue(20.),ValueFactory.createValue(239.),ValueFactory.createValue(13.),ValueFactory.createValue(20.),ValueFactory.createValue(20.),ValueFactory.createValue(59),ValueFactory.createValue(7.7),ValueFactory.createValue(12.),ValueFactory.createValue(4.14),ValueFactory.createValue(false)).getAsDouble();
		doSplAssert("239 vl/h 13 pl/h @ 20 km/h",81.985,value);
	}
}
