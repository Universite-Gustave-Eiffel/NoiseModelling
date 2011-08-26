/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.AbstractScalarFunction;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;

public class BTW_EvalSource  extends AbstractScalarFunction {
	public static final int ground_type_grass=0;
	public static final  int ground_type_rigid=1;
	public static final  double grass_a_factor=26.;
	public static final  double rigid_a_factor=26.;
	public static final  double grass_b_factor=75.;
	public static final  double rigid_b_factor=78.;
	public static final  double speed_reference=40.;
	@Override
	public Value evaluate(SQLDataSourceFactory dsf,Value... args) throws FunctionException {
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
			}else if(ground_type==ground_type_grass) {
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
	public Type getType(Type[] types) {
		return TypeFactory.createType(Type.DOUBLE);
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