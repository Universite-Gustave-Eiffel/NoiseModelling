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
import org.orbisgis.noisemap.core.PropagationProcess;

/**
 * 
 * @author Nicolas Fortin
 */
public class BR_EvalSource  extends AbstractScalarFunction {
	private Double getNoiseLvl(Double base, Double adj, Double speed,
			Double speedBase) {
		return base + adj * Math.log(speed / speedBase);
	}



	private Double sumDba(Double dBA1, Double dBA2) {
		return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2));
	}

	private double getVPl(double vvl, double speedmax, int type, int subtype)
			throws FunctionException {
		switch (type) {
		case 1:
			return Math.min(vvl, 100); // Highway 2x2 130 km/h
		case 2:
			switch (subtype) {
			case 1:
				return Math.min(vvl, 90); // 2x2 way 110 km/h
			case 2:
				return Math.min(vvl, 90); // 2x2 way 90km/h off belt-way
			case 3:
				if (speedmax < 80) {
					return Math.min(vvl, 70);
				} // Belt-way 70 km/h
				else {
					return Math.min(vvl, 85);
				} // Belt-way 90 km/h
			}
                        break;
		case 3:
			switch (subtype) {
			case 1:
				return vvl; // Interchange ramp
			case 2:
				return vvl; // Off boulevard roundabout circular junction
			case 7:
				return vvl; // inside-boulevard roundabout circular junction
			}
                        break;
		case 4:
			switch (subtype) {
			case 1:
				return Math.min(vvl, 90); // lower level 2x1 way 7m 90km/h
			case 2:
				return Math.min(vvl, 90); // Standard 2x1 way 90km/h
			case 3:
				if (speedmax < 70) {
					return Math.min(vvl, 60);
				} // 2x1 way 60 km/h
				else {
					return Math.min(vvl, 80);
				} // 2x1 way 80 km/h
			}
                        break;
		case 5:
			switch (subtype) {
			case 1:
				return Math.min(vvl, 70); // extra boulevard 70km/h
			case 2:
				return Math.min(vvl, 50); // extra boulevard 50km/h
			case 3:
				return Math.min(vvl, 50); // extra boulevard Street 50km/h
			case 4:
				return Math.min(vvl, 50); // extra boulevard Street <50km/h
			case 6:
				return Math.min(vvl, 50); // in boulevard 70km/h
			case 7:
				return Math.min(vvl, 50); // in boulevard 50km/h
			case 8:
				return Math.min(vvl, 50); // in boulevard Street 50km/h
			case 9:
				return Math.min(vvl, 50); // in boulevard Street <50km/h
			}
                        break;
		case 6:
			switch (subtype) {
			case 1:
				return Math.min(vvl, 50); // Bus-way boulevard 70km/h
			case 2:
				return Math.min(vvl, 50); // Bus-way boulevard 50km/h
			case 3:
				return Math.min(vvl, 50); // Bus-way extra boulevard Street
											// 50km/h
			case 4:
				return Math.min(vvl, 50); // Bus-way extra boulevard Street
											// <50km/h
			case 8:
				return Math.min(vvl, 50); // Bus-way in boulevard Street 50km/h
			case 9:
				return Math.min(vvl, 50); // Bus-way in boulevard Street <50km/h
			}
                        break;
		}
		throw new FunctionException("Unknown road type, please check (type="
				+ type + ",subtype=" + subtype + ").");
	}
        /**
         * Motor noise Sound level correction corresponding to a slope percentage
         * @param slope Slope percentage
         * @return Correction in dB(A)
         */
	private Double GetCorrection(double slope) {
		// Limitation of slope
		double rslope = Math.max(-6., slope);
		rslope = Math.min(6., rslope);
		// Computation of the correction
		if (rslope > 2.) {
			return 2 * (rslope - 2);
		} else if (rslope < -2.) {
			return rslope - 2;
		} else {
			return 0.;
		}
	}

	@Override
	public Value evaluate(DataSourceFactory dsf,Value... args) throws FunctionException {
		if (args.length < 3) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 10) {
			throw new FunctionException("Too many parameters !");
		} else {
			// Basic arguments
			double speed_load = args[0].getAsDouble();
			int vl_per_hour = args[1].getAsInt();
			int pl_per_hour = args[2].getAsInt();
			double speed_junction = speed_load;
			double speed_max = speed_load;
			int copound_roadtype = 58;
			double begin_z = 0;
			double end_z = 0;
			double road_length = 10;
			boolean is_queue = false;
			if (args.length >= 6) {
				speed_junction = args[3].getAsDouble();
				speed_max = args[4].getAsDouble();
				copound_roadtype = args[5].getAsInt();
			}
			if (args.length >= 9) {
				begin_z = args[6].getAsDouble();
				end_z = args[7].getAsDouble();
				road_length = args[8].getAsDouble();
			}
			if (args.length >= 10) {
				is_queue = args[9].getAsBoolean();
			}
			double speed;
			double speed_pl;
			if(args.length >= 6) {
				// Separation of main index and sub index
				final int roadtype = copound_roadtype / 10;
				final int roadsubtype = copound_roadtype - (roadtype * 10);
	
				// Compute the slope
				// final double
				// ground_dist=Math.sqrt(Math.pow(road_length,2)-Math.pow(end_z-begin_z,2));
				// OrbisGis return the length without the Z data; then we don't need
				// to compute the zero level distance

				// Computation of the traffic speed
				if (speed_junction > 0. && is_queue) {
					speed = speed_junction;
				} else if (speed_load > 0.) {
					speed = speed_load;
				} else {
					speed = speed_max;
				}
	
				speed_pl = getVPl(speed, speed_max, roadtype, roadsubtype);
			}else{
				speed = speed_load;
				speed_pl = speed_load;
			}
			// ///////////////////////
			// Noise road/tire
			// Use R2 surface
			double vl_road_lvl = getNoiseLvl(55.4, 20.1, speed, 90.);
			double pl_road_lvl = getNoiseLvl(63.4, 20., speed_pl, 80.);

			// ///////////////////////
			// Noise motor
			double vl_motor_lvl = 0.;
                        if (speed < 25.) { //restart
                                vl_motor_lvl = 51.1;
                        }else if (speed < 30.) {
				vl_motor_lvl = getNoiseLvl(36.7, -10., Math.max(20,speed), 90.);
			} else if (speed < 110.) {
				vl_motor_lvl = getNoiseLvl(42.4, 2., speed, 90.);
			} else {
				vl_motor_lvl = getNoiseLvl(40.7, 21.3, speed, 90.);
			}


                        double ground_dist = road_length; //distance is not hypotenuse
			double slope_perc = 0;
                        double pl_motor_lvl = 0.;
			if (args.length >= 9) {
                            //Taking account of slope percentage
                            slope_perc =  Math.min(6.,Math.max(-6.,(end_z - begin_z) / ground_dist * 100.));
           		}
                        if (speed < 25.) { //restart
                            if(slope_perc > 2) {
                                pl_motor_lvl = 62.4 + Math.max(0,2*slope_perc-4.5);
                            } else {
                                pl_motor_lvl = 62.4;
                            }
                        } else {
                            if (speed_pl < 70.) {
                                    pl_motor_lvl = getNoiseLvl(49.6, -10., Math.max(20,speed_pl), 80.);
                            } else {
                                    pl_motor_lvl = getNoiseLvl(50.4, 3., speed_pl, 80.);
                            }
                            pl_motor_lvl += GetCorrection(slope_perc); // Slope correction of Lmw,m,PL
                        }

			// ////////////////////////
			// Energetic SUM
			double vl_lvl = sumDba(vl_road_lvl, vl_motor_lvl) + 10
					* Math.log10(vl_per_hour);
			double pl_lvl = sumDba(pl_road_lvl, pl_motor_lvl) + 10
					* Math.log10(pl_per_hour);

			double sum_lvl = sumDba(vl_lvl, pl_lvl);
			return ValueFactory.createValue(sum_lvl);
		}
	}

	@Override
	public String getName() {
		return "BR_EvalSource";
	}

	@Override
	public int getType(int[] types) {
		return Type.DOUBLE;
	}
	@Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE  // heavy vehicle
                    		
                    ),new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE,  // heavy vehicle
                    		ScalarArgument.DOUBLE, // junction speed
                    		ScalarArgument.DOUBLE, // speed limit
                    		ScalarArgument.DOUBLE // Road Type XY
                    		
                    ),new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE, // heavy vehicle
                    		ScalarArgument.DOUBLE, // junction speed
                    		ScalarArgument.DOUBLE, // speed limit
                    		ScalarArgument.DOUBLE, // Road Type XY
                    		ScalarArgument.DOUBLE, // Z begin
                    		ScalarArgument.DOUBLE, // Z end
                    		ScalarArgument.DOUBLE  // Road length(m) (without taking account of
    											   // the Z coordinate)
                    		
                    ),new BasicFunctionSignature(getType(null),
                    		ScalarArgument.DOUBLE, // load speed
                    		ScalarArgument.DOUBLE, // light vehicle
                    		ScalarArgument.DOUBLE, // heavy vehicle
                    		ScalarArgument.DOUBLE, // junction speed
                    		ScalarArgument.DOUBLE, // speed limit
                    		ScalarArgument.DOUBLE, // Road Type XY
                    		ScalarArgument.DOUBLE, // Z begin
                    		ScalarArgument.DOUBLE, // Z end
                    		ScalarArgument.DOUBLE, // Road length(m) (without taking account of
    											   // the Z coordinate)
                    		ScalarArgument.BOOLEAN // Is queue (use junction speed as effective
												   // speed)
                    )
            };
    }

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the parameters.You can specify from 3 to 10 parameters.";
	}

	@Override
	public String getSqlOrder() {
		return "BR_EvalSource(loadSpeed,lightVehicleCount,heavyVehicleCount[,junction speed,speedMax,roadType[,Zbegin,Zend,roadLength[,isqueue]]])";
	}




}