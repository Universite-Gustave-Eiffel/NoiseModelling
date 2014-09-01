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
package org.orbisgis.noisemap.core;

/**
 * Return the dB(A) value corresponding to the parameters.You can specify from 3 to 10 parameters.
 * loadSpeed,lightVehicleCount,heavyVehicleCount[,junction speed,speedMax,roadType[,Zbegin,Zend,roadLength[,isqueue]]]
 * @author Nicolas Fortin
 */
public class EvalRoadSource {

    private static Double getNoiseLvl(Double base, Double adj, Double speed,
                               Double speedBase) {
        return base + adj * Math.log(speed / speedBase);
    }

    private static Double sumDba(Double dBA1, Double dBA2) {
        return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2));
    }

    private static double getVPl(double vvl, double speedmax, int type, int subtype) throws IllegalArgumentException {
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
        throw new IllegalArgumentException("Unknown road type, please check (type="
                + type + ",subtype=" + subtype + ").");
    }

    /**
     * Motor noise Sound level correction corresponding to a slope percentage
     *
     * @param slope Slope percentage
     * @return Correction in dB(A)
     */
    private static Double GetCorrection(double slope) {
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

    /**
     * Simplest road noise evaluation
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param surface_category Defining surface categories
     * @param surface_age Defining surface age
     * @return Noise level in dB(A)
     */
    public static double evaluate(double speed_load, int vl_per_hour, int pl_per_hour, int surface_category, int surface_age) {
        return evaluate(vl_per_hour, pl_per_hour, speed_load, speed_load, 0, surface_category, surface_age);
    }

    /**
     * Compute the slope
     * @param beginZ Z start
     * @param endZ Z end
     * @param road_length_2d Road length (do not take account of Z)
     * @return Slope percentage
     */
    public static double computeSlope(double beginZ, double endZ, double road_length_2d) {
        return (endZ - beginZ) / road_length_2d * 100.;
    }

    /**
     * Road noise evaluation.Evaluate speed of heavy vehicle.
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param begin_z Road start height
     * @param end_z Road end height
     * @param road_length_2d Road length (do not take account of Z)
     * @param is_queue If true use speed_junction in speed_load
     * @param surface_category Defining surface categories
     * @param surface_age Defining surface age
     * @return Noise level in dB(A)
     */
    public static double evaluate(double speed_load, int vl_per_hour, int pl_per_hour, double speed_junction, double speed_max,
                                  int copound_roadtype, double begin_z,double end_z, double road_length_2d, boolean is_queue, int surface_category, int surface_age) {
        double speed;
        double speed_pl;
        // Separation of main index and sub index
        final int roadtype = copound_roadtype / 10;
        final int roadsubtype = copound_roadtype - (roadtype * 10);

        // Compute the slope
        // final double
        // ground_dist=Math.sqrt(Math.pow(road_length,2)-Math.pow(end_z-begin_z,2));
        // JTS return the length without the Z data; then we don't need
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
        double slope_perc = Math.min(6., Math.max(-6., computeSlope(begin_z, end_z, road_length_2d)));
        return evaluate(vl_per_hour, pl_per_hour, speed, speed_pl, slope_perc, surface_category, surface_age);
    }

    public static final int STEADY_SPEED = 1;
    public static final int ACCELERATION = 2;
    public static final int DECELERATION = 2;

    /**
     * Road noise evaluation.
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     * @param speed Average vehicle speed
     * @param speed_pl Average heavy vehicle speed
     * @param slope_perc Slope percentage, will be bounded to [-6 6]
     * @param surface_category Defining surfaces categories
     * @param surface_age Defining surface age
     * @return Noise level in dB(A)
     */
    public static double evaluate(int vl_per_hour, int pl_per_hour, double speed, double speed_pl, double slope_perc, int surface_category, int surface_age) {
        // ///////////////////////
        // Noise road/tire
        // cf. NMPB 2008 1 - Calculating sound emissions from road traffic
        // p. 18 at 20
        // The R2 surface is use by default

        double vl_road_lvl;
        double pl_road_lvl;

        // surface category -> R1
        if (surface_category == 1){
            vl_road_lvl = getNoiseLvl(53.4, 21., speed, 90.);
            pl_road_lvl = getNoiseLvl(61.5, 20., speed_pl, 80.);
            // check surface age
            if (surface_age < 2){
                vl_road_lvl = vl_road_lvl-4.;
                pl_road_lvl = pl_road_lvl-2.4;
            } else if ( surface_age >= 2 && surface_age < 10){
                vl_road_lvl = vl_road_lvl + 0.50 * (surface_age - 10);
                pl_road_lvl = pl_road_lvl + 0.30 * (surface_age - 10);
            }
        }
        // surface category -> R3
        else if (surface_category == 3){
            vl_road_lvl = getNoiseLvl(57.5, 21.4, speed, 90.);
            pl_road_lvl = getNoiseLvl(64.2, 20., speed_pl, 80.);
            // check surface age
            if (surface_age < 2){
                vl_road_lvl = vl_road_lvl-1.6;
                pl_road_lvl = pl_road_lvl-1.;
            } else if ( surface_age >= 2 && surface_age < 10){
                vl_road_lvl = vl_road_lvl + 0.20*(surface_age - 10);
                pl_road_lvl = pl_road_lvl + 0.12*(surface_age - 10);
            }
        }
        // surface category -> R2 or default
        else {
            vl_road_lvl = getNoiseLvl(55.4, 20.1, speed, 90.);
            pl_road_lvl = getNoiseLvl(63.4, 20., speed_pl, 80.);
            // check surface age                                                                                        // TODO default surface age 2 at 10 years. so need to initialize default value
            if (surface_age < 2) {
                vl_road_lvl = vl_road_lvl - 2.;
                pl_road_lvl = pl_road_lvl - 1.2;
            } else if (surface_age >= 2 && surface_age < 10) {
                vl_road_lvl = vl_road_lvl + 0.25 * (surface_age - 10);
                pl_road_lvl = pl_road_lvl + 0.15 * (surface_age - 10);
            }
        }

        // Bound slope
        slope_perc = Math.min(6., Math.max(-6.,slope_perc));

        // ///////////////////////
        // Noise motor

        // TODO need to initiate default speed for vl & pl

       /**
        * Calculate the emission powers of motors lights vehicles.
        * @param condition_vl initialization of conditions sections. True for starting condition and false for stopping condition. ----> (default : true)
        * @param pace_vl initialization of vehicle pace : 1 for steady speed ; 2 for acceleration ; 3 for decelerating.       ----> (default : 1)
        * @return vl motor noise level
        */

        boolean condition_vl = true;
        int pace_vl = 1;
        double vl_motor_lvl = 0.;


        if (speed < 25.) {                  // check speed the lvl.                                                     // TODO for check speed the lvl, need to initiate default speed
            if (condition_vl == false){     // stopping condition.
                vl_motor_lvl = 44.5;
            } else {                        // default or starting condition.
                vl_motor_lvl = 51.1;
            }
        } else if (speed >= 25.) {          // check speed the lvl.                                                     // TODO can initialize default at 25 km/h ?
            if (pace_vl == 2){              // accelerated pace.
                if (speed <= 100.) {         // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(46.1, -10., Math.max(20, speed), 90.);                                   // TODO Math.max(20, speed) -> max(25, speed) ?
                } else {                    // default speed lvl for accelerated pace
                    vl_motor_lvl = getNoiseLvl(44.3, 28.6, speed, 90.);
                }
            } else if (pace_vl == 3){       // decelerated pace.
                if (speed <= 80.) {          // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(42.1, -4.5, Math.max(20, speed), 90.);
                } else if (speed > 80. && speed <= 110.) {
                    vl_motor_lvl = getNoiseLvl(42.4, 2., speed, 90.);
                } else {                    // default speed lvl for decelerated pace.
                    vl_motor_lvl = getNoiseLvl(40.7, 21.3, speed, 90.);
                }
            } else {                        // default or steady speed.
                if (speed <= 30.) {          // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(36.7, -10., Math.max(20, speed), 90.);
                } else if (speed > 30. && speed <= 110. ) {
                    vl_motor_lvl = getNoiseLvl(42.4, 2., speed, 90.);
                } else {                    // default speed for steady speed pace.
                    vl_motor_lvl = getNoiseLvl(40.7, 21.3, speed, 90.);
                }
            }
        }



        /**
         * Calculate the emission powers of heavies goods vehicles.
         * @param condition_pl initialization of conditions sections. True for starting condition and false for stopping condition. ----> (default : true)
         * @param pace_pl      initialization of vehicle pace : 1 for steady speed ; 2 for acceleration ; 3 for deceleration.       ----> (default : 1)
         * @return vl motor noise level
         */


        boolean condition_pl = true;
        int pace_pl = STEADY_SPEED;
        double pl_motor_lvl;

        if (speed_pl <= 25.) {                                                                                           // TODO initialize default speed
            if (condition_pl != true){                      // stopping condition
                if (slope_perc < 0){                        // downward slope
                    slope_perc = - slope_perc;
                    pl_motor_lvl = 58.0 + (slope_perc - 2);
                }
                else {
                    pl_motor_lvl = 58.0;
                }
            } else {                                        // restart condition                                        // TODO optimization -> if (slope_perc > 2) ... else ... end
                if (slope_perc < 0) {                       // downward slope
                    pl_motor_lvl = 62.4;
                } else if (slope_perc > 2) {
                     pl_motor_lvl = 62.4 + Math.max(0, 2 *( slope_perc - 4.5));
                } else {
                        pl_motor_lvl = 62.4;
                 }
            }
        } else {                                                                                                        // TODO initialize default speed
            if (speed_pl > 25. && speed_pl <= 70.) {
                pl_motor_lvl = getNoiseLvl(49.6, -10., Math.max(20, speed_pl), 80.);
                if (slope_perc < 0){
                    slope_perc = - slope_perc;
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5 ;
                    } else {
                        pl_motor_lvl = pl_motor_lvl + 1*(slope_perc -2);
                    }
                } else if (slope_perc > 2){
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5 + Math.max(2*(slope_perc-4.5),0) ;
                    } else if(pace_pl != DECELERATION) {
                        pl_motor_lvl = pl_motor_lvl + 2 * (slope_perc - 2);
                    }
                } else {
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5.;
                    }
                }
            } else {
                pl_motor_lvl = getNoiseLvl(50.4, 3., speed_pl, 80.);
                if (slope_perc < 0){
                    slope_perc = - slope_perc;
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5 ;
                    } else {
                        pl_motor_lvl = pl_motor_lvl + 1*(slope_perc -2);
                    }
                } else if (slope_perc > 2){
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5 + Math.max(2*(slope_perc-4.5),0) ;
                    } else if(pace_pl != DECELERATION) {
                        pl_motor_lvl = pl_motor_lvl + 2 * (slope_perc - 2);
                    }
                } else {
                    if (pace_pl == ACCELERATION){
                        pl_motor_lvl = pl_motor_lvl + 5.;
                    }
                }
            }
            pl_motor_lvl += GetCorrection(slope_perc); // Slope correction of Lmw,m,PL
        }

        // ////////////////////////
        // Energetic SUM
        double vl_lvl = sumDba(vl_road_lvl, vl_motor_lvl) + 10
                * Math.log10(vl_per_hour);
        double pl_lvl = sumDba(pl_road_lvl, pl_motor_lvl) + 10
                * Math.log10(pl_per_hour);

        return sumDba(vl_lvl, pl_lvl);
    }
}