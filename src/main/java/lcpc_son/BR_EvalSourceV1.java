/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able
 * to manipulate and create vector and raster spatial information. OrbisGIS
 * is distributed under GPL 3 license. It is produced  by the geo-informatic team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/>, CNRS FR 2488:
 *    Erwan BOCHER, scientific researcher,
 *    Thomas LEDUC, scientific researcher,
 *    Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OrbisGIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult:
 *    <http://orbisgis.cerma.archi.fr/>
 *    <http://sourcesup.cru.fr/projects/orbisgis/>
 *
 * or contact directly:
 *    erwan.bocher _at_ ec-nantes.fr
 *    fergonco _at_ gmail.com
 *    thomas.leduc _at_ cerma.archi.fr
 */
/***********************************
 * ANR EvalPDU
 * Lcpc 28_05_2010
 * @author Nicolas Fortin
 ***********************************/

package lcpc_son;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

public class BR_EvalSourceV1 implements Function {
	private Double GetNoiseLvl(Double base,Double adj,Double speed,Double speedBase)
	{
		return base+adj*Math.log(speed/speedBase);
	}
	private Double DbaToW(Double dBA){
		return Math.pow(10.,dBA/10.);
	}
	private Double WToDba(Double W){
		return 10*Math.log10(W);
	}
	private Double SumDba(Double dBA1,Double dBA2){
		return WToDba(DbaToW(dBA1)+DbaToW(dBA2));
	}
	public Value evaluate(Value... args) throws FunctionException {
		if (args.length!=3) {
			return ValueFactory.createNullValue();
		} else {
			final double speed = args[0].getAsDouble();
			final int vl_per_hour = args[1].getAsInt();
			final int pl_per_hour = args[2].getAsInt();
			
			/////////////////////////
			// Noise road/tire
			//Use R2 surface
			double vl_road_lvl=GetNoiseLvl(55.4,20.1,speed,90.);
			double pl_road_lvl=GetNoiseLvl(63.4,20.,speed,80.);
			
			/////////////////////////
			// Noise motor
			double vl_motor_lvl=0.;
			if(speed<30.){
				vl_motor_lvl=GetNoiseLvl(36.7,-10.,speed,90.);
			}else if(speed<110.){
				vl_motor_lvl=GetNoiseLvl(42.4,2.,speed,90.);
			}else{
				vl_motor_lvl=GetNoiseLvl(40.7,21.3,speed,90.);				
			}
			
			double pl_motor_lvl=0.;
			if(speed<70.){
				pl_motor_lvl=GetNoiseLvl(49.6,-10.,speed,80.);
			}else{
				pl_motor_lvl=GetNoiseLvl(50.4,3.,speed,80.);
			}
			
			//////////////////////////
			// Energetic SUM
			double vl_lvl=SumDba(vl_road_lvl,vl_motor_lvl)+10*Math.log10(vl_per_hour);
			double pl_lvl=SumDba(pl_road_lvl,pl_motor_lvl)+10*Math.log10(pl_per_hour);

			double sum_lvl=SumDba(vl_lvl,pl_lvl);
			return ValueFactory.createValue(sum_lvl);
		}
	}

	public String getName() {
		return "BR_EvalSourceV1";
	}

	public boolean isAggregate() {
		return false;
	}

	public Type getType(Type[] types) {
		return TypeFactory.createType(Type.DOUBLE);
	}

	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(Argument.NUMERIC,
				Argument.NUMERIC,
				Argument.NUMERIC) };
	}

	public String getDescription() {
		return "Return the dB(A) value corresponding to speed,light and heavy vehicle parameters.";
	}

	public String getSqlOrder() {
		return "select BR_EvalSourceV1(loadSpeed,lightVehicleCount,heavyVehicleCount) from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}
	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args)
			throws FunctionException {
		// TODO Auto-generated method stub
		return null;
	}

}