/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package lcpcson;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

public class BTW_EvalSourceV1 implements Function {

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
		if (args.length!=2) {
			return ValueFactory.createNullValue();
		} else {
			final double speed = args[0].getAsDouble();
			final int tw_per_hour = args[1].getAsInt();
			
			/////////////////////////
			// Noise Tramway
			double tw_lvl=50+2.*Math.log(speed/30.)+10*Math.log10(tw_per_hour);

			return ValueFactory.createValue(tw_lvl);
		}
	}

        @Override
	public String getName() {
		return "BTW_EvalSourceV1";
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
		return new Arguments[] { new Arguments(Argument.NUMERIC,
				Argument.NUMERIC) };
	}

        @Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to speed,tramway by hour parameters.";
	}

        @Override
	public String getSqlOrder() {
		return "select BTW_EvalSourceV1(loadSpeed,tramway_count) from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}

}