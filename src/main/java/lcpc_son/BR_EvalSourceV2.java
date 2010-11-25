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


public class BR_EvalSourceV2 implements Function {
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
	private double getVPl(double vvl,double speedmax,int type,int subtype) throws FunctionException
	{
		switch(type)
		{
			case 1:
				return Math.min(vvl, 100); //Highway 2x2 130 km/h
			case 2:
				switch(subtype)
				{
					case 1:
						return Math.min(vvl,90); //2x2 way 110 km/h
					case 2:
						return Math.min(vvl,90); //2x2 way 90km/h off belt-way
					case 3:
						if(speedmax<80)
							return Math.min(vvl,70); //Belt-way 70 km/h
						else
							return Math.min(vvl,85); //Belt-way 90 km/h
				}
			case 3:
				switch(subtype)
				{
					case 1:
						return vvl; //Interchange ramp
					case 2:
						return vvl; //Off boulevard roundabout circular junction 
					case 7:
						return vvl; //inside-boulevard roundabout circular junction 
				}
			case 4:
				switch(subtype)
				{
					case 1:
						return Math.min(vvl,90); //lower level 2x1 way 7m 90km/h 
					case 2:
						return Math.min(vvl,90); //Standard 2x1 way 90km/h
					case 3:
						if(speedmax<70)
							return Math.min(vvl,60); //2x1 way 60 km/h
						else
							return Math.min(vvl,80); //2x1 way 80 km/h
				}
			case 5:
				switch(subtype)
				{
					case 1:
						return Math.min(vvl,70); //extra boulevard 70km/h 
					case 2:
						return Math.min(vvl,50); //extra boulevard 50km/h
					case 3:
						return Math.min(vvl,50); //extra boulevard Street 50km/h
					case 4:
						return Math.min(vvl,50); //extra boulevard Street <50km/h
					case 6:
						return Math.min(vvl,50); //in boulevard 70km/h 
					case 7:
						return Math.min(vvl,50); //in boulevard 50km/h 
					case 8:
						return Math.min(vvl,50); //in boulevard Street 50km/h
					case 9:
						return Math.min(vvl,50); //in boulevard Street <50km/h
				}
			case 6:
				switch(subtype)
				{
					case 1:
						return Math.min(vvl,50); //Bus-way boulevard 70km/h 
					case 2:
						return Math.min(vvl,50); //Bus-way boulevard 50km/h
					case 3:
						return Math.min(vvl,50); //Bus-way extra boulevard Street 50km/h
					case 4:
						return Math.min(vvl,50); //Bus-way extra boulevard Street <50km/h
					case 7:
						return Math.min(vvl,50); //?? Unknown
					case 8:
						return Math.min(vvl,50); //Bus-way in boulevard Street 50km/h
					case 9:
						return Math.min(vvl,50); //Bus-way in boulevard Street <50km/h
				}
		}
		throw new FunctionException("Unknown road type, please check (type="+type+",subtype="+subtype+").");
	}
	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
		if (args.length!=6) {
			throw new FunctionException("Not enough parameters !");
		} else {
			final double speed_load = args[0].getAsDouble();
			final double speed_junction = args[1].getAsDouble();
			final double speed_max = args[2].getAsDouble();
			final int vl_per_hour = args[3].getAsInt();
			final int pl_per_hour = args[4].getAsInt();
			final int copound_roadtype = args[5].getAsInt();
			// Separation of main index and sub index
			final int roadtype=copound_roadtype/10;
			final int roadsubtype=copound_roadtype-(roadtype*10);
			//Computation of traffic speed
			double speed=0.;
			if(speed_junction>0.)
			{
				speed=speed_junction;
			}else if(speed_load>0.){
				speed=speed_load;
			}else{
				speed=speed_max;
			}

			double speed_pl=getVPl(speed,speed_max,roadtype,roadsubtype);

			/////////////////////////
			// Noise road/tire
			//Use R2 surface
			double vl_road_lvl=GetNoiseLvl(55.4,20.1,speed,90.);
			double pl_road_lvl=GetNoiseLvl(63.4,20.,speed_pl,80.);
			
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
			if(speed_pl<70.){
				pl_motor_lvl=GetNoiseLvl(49.6,-10.,speed_pl,80.);
			}else{
				pl_motor_lvl=GetNoiseLvl(50.4,3.,speed_pl,80.);
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
		return "BR_EvalSourceV2";
	}

	public boolean isAggregate() {
		return false;
	}

	public Type getType(Type[] types) {
		return TypeFactory.createType(Type.DOUBLE);
	}

	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(
				Argument.NUMERIC, 		//load speed
				Argument.NUMERIC, 		//junction speed
				Argument.NUMERIC, 		//speed limit
				Argument.NUMERIC, 		//light vehicle
				Argument.NUMERIC,		//heavy vehicle
				Argument.NUMERIC		//Road Type XY
		)}; 	
	}

	public String getDescription() {
		return "Return the dB(A) value corresponding to the road and light and heavy vehicle parameters.";
	}

	public String getSqlOrder() {
		return "select BR_EvalSourceV2(loadSpeed,junction,speedMax,lightVehicleCount,heavyVehicleCount,roadType) from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}

}