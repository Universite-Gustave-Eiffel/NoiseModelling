package lcpcson;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.HashMap;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

public class BTW_SpectrumRepartition implements Function {

	private HashMap<Integer, Integer> freqToIndex = new HashMap<Integer, Integer>();
	private double[] non_pervious_att = { -27, -26, -24, -21, -19, -16, -14,
			-11, -11, -8, -7, -8, -10, -13, -16, -18, -21, -23 };

	public BTW_SpectrumRepartition() {
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
	public Value evaluate(DataSourceFactory dsf, Value... args)
			throws FunctionException {
		if (args.length < 2) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 2) {
			throw new FunctionException("Too many parameters !");
		} else {
			return ValueFactory.createValue(args[1].getAsDouble()
					+ getAttenuatedValue(args[0].getAsInt()));
		}
	}

	@Override
	public String getName() {
		return "BTW_SpectrumRepartition";
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
		return new Arguments[] { new Arguments(Argument.INT, // Frequency
																// [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000]
				Argument.DOUBLE // Global SPL value (dBA)
		) };
	}

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the the third octave frequency band. First parameter is Frequency band one of [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000], second parameter is the category of the road surface [1:Pervious,2:Non Pervious], third parameter is the global dB(A) Spl Value.";
	}

	@Override
	public String getSqlOrder() {
		return "select BTW_SpectrumRepartition(100,1,dbA) as dbA_100 from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}
}
