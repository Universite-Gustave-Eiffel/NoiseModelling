package org.noisemap.plugin;

import org.gdms.data.values.ValueFactory;
import org.gdms.data.types.IncompatibleTypesException;
import org.gdms.sql.function.FunctionException;
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


                //BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount
                //              [,junction speed,speedMax,roadType
                //              [,Zbegin,Zend,roadLength
                //              [,isqueue]]])
		//10 Components traffic information
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(50.),ValueFactory.createValue(50.),ValueFactory.createValue(32),ValueFactory.createValue(50),ValueFactory.createValue(53),ValueFactory.createValue(0),ValueFactory.createValue(5),ValueFactory.createValue(45.),ValueFactory.createValue(false)).getAsDouble();
		doSplAssert("50 vl/h 50 pl/h @ 40km/h",81.69,value);

                //Other 10 Components, restart engine < 25 km/h
		value=noiseEval.evaluate(null, ValueFactory.createValue(40.),ValueFactory.createValue(793.),ValueFactory.createValue(49.),ValueFactory.createValue(1.75),ValueFactory.createValue(40),ValueFactory.createValue(58),ValueFactory.createValue(7.9),ValueFactory.createValue(7.3),ValueFactory.createValue(8.61),ValueFactory.createValue(true)).getAsDouble();
		doSplAssert("793 vl/h 49 pl/h @ 1.72km/h",82.72,value);

                //Another
                value=noiseEval.evaluate(null, ValueFactory.createValue(20.),ValueFactory.createValue(239.),ValueFactory.createValue(13.),ValueFactory.createValue(20.),ValueFactory.createValue(20.),ValueFactory.createValue(59),ValueFactory.createValue(7.7),ValueFactory.createValue(12.),ValueFactory.createValue(4.14),ValueFactory.createValue(false)).getAsDouble();
		doSplAssert("239 vl/h 13 pl/h @ 20 km/h",81.98,value);
	}
}
