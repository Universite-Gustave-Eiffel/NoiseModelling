package org.noisemap.plugin;

import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.strategies.IncompatibleTypesException;
import org.noisemap.core.BR_EvalSource;
import org.noisemap.core.TestSoundPropagationValidation;

import junit.framework.TestCase;

public class TestTrafficNoiseComputation extends TestCase {
	private void doSplAssert(String parameters,double expecteddB,double computeddB) {
		assertTrue("Road noise estimation ("+parameters+") failed. Expected value is "+expecteddB+" dB(A), computed value is "+computeddB+" dB(A).", TestSoundPropagationValidation.isSameDbValues(computeddB, expecteddB));
	}
	public void testRoadVehicleNoise() throws IncompatibleTypesException, FunctionException {

		//3 Components traffic information
		BR_EvalSource noiseEval=new BR_EvalSource();
		double value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(1.),ValueFactory.createValue(0.)).getAsDouble();
		doSplAssert("1 vl/h @ 50km/h",45.573,value);
		value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(0.),ValueFactory.createValue(0.)).getAsDouble();
		doSplAssert("0 tv/h",Double.NEGATIVE_INFINITY,value);
		value=noiseEval.evaluate(null, ValueFactory.createValue(50.),ValueFactory.createValue(0.),ValueFactory.createValue(1.)).getAsDouble();
		doSplAssert("1 pl/h @ 50km/h",57.163,value);
		
		//6 Components traffic information 
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(100.),ValueFactory.createValue(0.),ValueFactory.createValue(32),ValueFactory.createValue(50),ValueFactory.createValue(53)).getAsDouble();
		doSplAssert("100 vl/h @ 40km/h (junct 32,max 50), extra boulevard Street",63.03,value);
		
		//10 Components traffic information
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(50.),ValueFactory.createValue(50.),ValueFactory.createValue(32),ValueFactory.createValue(50),ValueFactory.createValue(53),ValueFactory.createValue(0),ValueFactory.createValue(5),ValueFactory.createValue(45.),ValueFactory.createValue(false)).getAsDouble();
		doSplAssert("50 vl/h 50 pl/h @ 40km/h",81.69,value);
				
	}
}
