/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

public class BR_EvalSourceV4 implements Function {
	private Double getNoiseLvl(Double base, Double adj, Double speed,
			Double speedBase) {
		return base + adj * Math.log(speed / speedBase);
	}

	private Double dbaToW(Double dBA) {
		return Math.pow(10., dBA / 10.);
	}

	private Double wToDba(Double W) {
		return 10 * Math.log10(W);
	}

	private Double sumDba(Double dBA1, Double dBA2) {
		return wToDba(dbaToW(dBA1) + dbaToW(dBA2));
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
		case 3:
			switch (subtype) {
			case 1:
				return vvl; // Interchange ramp
			case 2:
				return vvl; // Off boulevard roundabout circular junction
			case 7:
				return vvl; // inside-boulevard roundabout circular junction
			}
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
		}
		throw new FunctionException("Unknown road type, please check (type="
				+ type + ",subtype=" + subtype + ").");
	}

	private Double GetCorrection(double slope) {
		// Limitation of slope
		slope = Math.max(-6., slope);
		slope = Math.min(6., slope);
		// Computation of the correction
		if (slope > 2.) {
			return 2 * (slope - 2);
		} else if (slope < -2.) {
			return slope - 2;
		} else {
			return 0.;
		}
	}

	@Override
	public Value evaluate(DataSourceFactory dsf, Value... args)
			throws FunctionException {
		if (args.length < 10) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 10) {
			throw new FunctionException("Too many parameters !");
		} else {
			final double speed_load = args[0].getAsDouble();
			final double speed_junction = args[1].getAsDouble();
			final double speed_max = args[2].getAsDouble();
			final int vl_per_hour = args[3].getAsInt();
			final int pl_per_hour = args[4].getAsInt();
			final int copound_roadtype = args[5].getAsInt();
			final double begin_z = args[6].getAsDouble();
			final double end_z = args[7].getAsDouble();
			final double road_length = args[8].getAsDouble();
			final boolean is_queue = args[9].getAsBoolean();

			// Separation of main index and sub index
			final int roadtype = copound_roadtype / 10;
			final int roadsubtype = copound_roadtype - (roadtype * 10);

			// Compute the slope
			// final double
			// ground_dist=Math.sqrt(Math.pow(road_length,2)-Math.pow(end_z-begin_z,2));
			// OrbisGis return the length without the Z data; then we don't need
			// to compute the zero level distance
			final double ground_dist = road_length;
			final double slope_perc = (end_z - begin_z) / ground_dist * 100.;

			// Computation of the traffic speed
			double speed = 0.;
			if (speed_junction > 0. && is_queue) {
				speed = speed_junction;
			} else if (speed_load > 0.) {
				speed = speed_load;
			} else {
				speed = speed_max;
			}

			double speed_pl = getVPl(speed, speed_max, roadtype, roadsubtype);

			// ///////////////////////
			// Noise road/tire
			// Use R2 surface
			double vl_road_lvl = getNoiseLvl(55.4, 20.1, speed, 90.);
			double pl_road_lvl = getNoiseLvl(63.4, 20., speed_pl, 80.);

			// ///////////////////////
			// Noise motor
			double vl_motor_lvl = 0.;
			if (speed < 30.) {
				vl_motor_lvl = getNoiseLvl(36.7, -10., speed, 90.);
			} else if (speed < 110.) {
				vl_motor_lvl = getNoiseLvl(42.4, 2., speed, 90.);
			} else {
				vl_motor_lvl = getNoiseLvl(40.7, 21.3, speed, 90.);
			}

			double pl_motor_lvl = 0.;
			if (speed_pl < 70.) {
				pl_motor_lvl = getNoiseLvl(49.6, -10., speed_pl, 80.);
			} else {
				pl_motor_lvl = getNoiseLvl(50.4, 3., speed_pl, 80.);
			}
			pl_motor_lvl += GetCorrection(slope_perc); // Slope correction of
														// Lmw,m,PL

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
		return "BR_EvalSourceV4";
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
		return new Arguments[] { new Arguments(Argument.NUMERIC, // load speed
				Argument.NUMERIC, // junction speed
				Argument.NUMERIC, // speed limit
				Argument.NUMERIC, // light vehicle
				Argument.NUMERIC, // heavy vehicle
				Argument.NUMERIC, // Road Type XY
				Argument.NUMERIC, // Z begin
				Argument.NUMERIC, // Z end
				Argument.NUMERIC, // Road length(m) (without taking account of
									// the Z coordinate)
				Argument.BOOLEAN // Is queue (use junction speed as effective
									// speed)
		) };
	}

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the road and light and heavy vehicle parameters.";
	}

	@Override
	public String getSqlOrder() {
		return "select BR_EvalSourceV4(loadSpeed,junction,speedMax,lightVehicleCount,heavyVehicleCount,roadType,Zbegin,Zend,roadLength,isqueue) from myTable;";
	}

	@Override
	public Value getAggregateResult() {
		return null;
	}

}