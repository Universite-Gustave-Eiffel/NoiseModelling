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
public class EvaluateRoadSource {

    private static Double getNoiseLvl(Double base, Double adj, Double speed,
                               Double speedBase) {
        return base + adj * Math.log(speed / speedBase);
    }

    private static Double sumDba(Double dBA1, Double dBA2) {
        return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB(A)
     */
    public static double evaluate(EvaluateRoadSourceParameter parameters) {
        // ///////////////////////
        // Noise road/tire
        // cf. NMPB 2008 1 - Calculating sound emissions from road traffic
        // p. 18 at 20
        // The R2 surface is use by default
        // 2.7.2.3 - Emission power per metre of line-source depending on the age of the surface

        double vl_road_lvl;
        double pl_road_lvl;

        // surface category -> R1
        if (parameters.getSurfaceCategory() == EvaluateRoadSourceParameter.SurfaceCategory.R1){
            vl_road_lvl = getNoiseLvl(53.4, 21., parameters.getSpeedVl(), 90.);
            pl_road_lvl = getNoiseLvl(61.5, 20., parameters.getSpeedPl(), 80.);
            // check surface age
            if (parameters.getSurfaceAge() < 2){
                vl_road_lvl = vl_road_lvl-4.;
                pl_road_lvl = pl_road_lvl-2.4;
            } else {
                vl_road_lvl = vl_road_lvl + 0.50 * (parameters.getSurfaceAge() - 10);
                pl_road_lvl = pl_road_lvl + 0.30 * (parameters.getSurfaceAge() - 10);
            }
        }
        // surface category -> R3
        else if (parameters.getSurfaceCategory() == EvaluateRoadSourceParameter.SurfaceCategory.R3){
            vl_road_lvl = getNoiseLvl(57.5, 21.4, parameters.getSpeedVl(), 90.);
            pl_road_lvl = getNoiseLvl(64.2, 20., parameters.getSpeedPl(), 80.);
            // check surface age
            if (parameters.getSurfaceAge() < 2){
                vl_road_lvl = vl_road_lvl-1.6;
                pl_road_lvl = pl_road_lvl-1.;
            } else {
                vl_road_lvl = vl_road_lvl + 0.20*(parameters.getSurfaceAge() - 10);
                pl_road_lvl = pl_road_lvl + 0.12*(parameters.getSurfaceAge() - 10);
            }
        } else {
            // surface category -> R2 or default
            vl_road_lvl = getNoiseLvl(55.4, 20.1, parameters.getSpeedVl(), 90.);
            pl_road_lvl = getNoiseLvl(63.4, 20., parameters.getSpeedPl(), 80.);
            // check surface age                                                                                        // TODO default surface age 2 at 10 years. so need to initialize default value
            if (parameters.getSurfaceAge() < 2) {
                vl_road_lvl = vl_road_lvl - 2.;
                pl_road_lvl = pl_road_lvl - 1.2;
            } else {
                vl_road_lvl = vl_road_lvl + 0.25 * (parameters.getSurfaceAge() - 10);
                pl_road_lvl = pl_road_lvl + 0.15 * (parameters.getSurfaceAge() - 10);
            }
        }

        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles.
        double vl_motor_lvl = 0.;

        if (parameters.getSpeedVl() < 25.) {                  // check speed the lvl.
            if (parameters.getVlState() == EvaluateRoadSourceParameter.EngineState.Deceleration){     // stopping condition.
                vl_motor_lvl = 44.5;
            } else {                        // default or starting condition.
                vl_motor_lvl = 51.1;
            }
        } else {
            // check speed the lvl.
            // TODO can initialize default at 25 km/h ?
            if (parameters.getVlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                // accelerated pace.
                if (parameters.getSpeedVl() <= 100.) {         // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(46.1, -10., Math.max(20, parameters.getSpeedVl()), 90.);
                    // TODO Math.max(20, speed) -> max(25, speed) ?
                } else {
                    // default speed lvl for accelerated pace
                    vl_motor_lvl = getNoiseLvl(44.3, 28.6, parameters.getSpeedVl(), 90.);
                }
            } else if (parameters.getVlState() == EvaluateRoadSourceParameter.EngineState.Deceleration){
                // decelerated pace.
                if (parameters.getSpeedVl() <= 80.) {          // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(42.1, -4.5, Math.max(20, parameters.getSpeedVl()), 90.);
                } else if (parameters.getSpeedVl() > 80. && parameters.getSpeedVl() <= 110.) {
                    vl_motor_lvl = getNoiseLvl(42.4, 2., parameters.getSpeedVl(), 90.);
                } else {                    // default speed lvl for decelerated pace.
                    vl_motor_lvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedVl(), 90.);
                }
            } else {
                // default or steady speed.
                if (parameters.getSpeedVl() <= 30.) {
                    // check again the speed lvl.
                    vl_motor_lvl = getNoiseLvl(36.7, -10., Math.max(20, parameters.getSpeedVl()), 90.);
                } else if (parameters.getSpeedVl() > 30. && parameters.getSpeedVl() <= 110. ) {
                    vl_motor_lvl = getNoiseLvl(42.4, 2., parameters.getSpeedVl(), 90.);
                } else {
                    // default speed for steady speed pace.
                    vl_motor_lvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedVl(), 90.);
                }
            }
        }



        // Calculate the emission powers of heavies goods vehicles.
        double pl_motor_lvl;

        if (parameters.getSpeedPl() <= 25.) {
            // TODO initialize default speed
            if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Deceleration){
                // stopping condition
                if (parameters.getSlopePercentage() < 2){
                    // downward slope
                    pl_motor_lvl = 58.0 + (-parameters.getSlopePercentage() - 2);
                } else {
                    pl_motor_lvl = 58.0;
                }
            } else {
                // restart condition
                if (parameters.getSlopePercentage() < 2) {
                    // downward slope
                    pl_motor_lvl = 62.4;
                } else {
                    pl_motor_lvl = 62.4 + Math.max(0, 2 *( parameters.getSlopePercentage() - 4.5));
                }
            }
        } else if(parameters.getSpeedPl() <= 70.) {
            pl_motor_lvl = getNoiseLvl(49.6, -10., Math.max(20, parameters.getSpeedPl()), 80.);
            if (parameters.getSlopePercentage() < 0){
                if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                    pl_motor_lvl = pl_motor_lvl + 5 ;
                } else {
                    pl_motor_lvl = pl_motor_lvl + 1*(-parameters.getSlopePercentage() -2);
                }
            } else if (parameters.getSlopePercentage() > 2){
                if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                    pl_motor_lvl = pl_motor_lvl + 5 + Math.max(2*(parameters.getSlopePercentage()-4.5),0) ;
                } else if(parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.SteadySpeed) {
                    pl_motor_lvl = pl_motor_lvl + 2 * (parameters.getSlopePercentage() - 2);
                }
            } else {
                if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                    pl_motor_lvl = pl_motor_lvl + 5.;
                }
            }
        } else {
            // 70 km/h to 100 km/h
            // Table 2.11
            pl_motor_lvl = getNoiseLvl(50.4, 3., parameters.getSpeedPl(), 80.);
            if(parameters.getSlopePercentage() < 2) {
                if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                    pl_motor_lvl = pl_motor_lvl + 5 ;
                } else if(parameters.getSlopePercentage() <= -2) {
                    pl_motor_lvl = pl_motor_lvl + (-parameters.getSlopePercentage() -2);
                }
            } else {
                if (parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.Acceleration){
                    pl_motor_lvl = pl_motor_lvl + 5 + Math.max(2*(parameters.getSlopePercentage()-4.5),0) ;
                } else if(parameters.getPlState() == EvaluateRoadSourceParameter.EngineState.SteadySpeed) {
                    pl_motor_lvl = pl_motor_lvl + 2 * (parameters.getSlopePercentage() - 2);
                }
            }
        }


        // ////////////////////////
        // Energetic SUM
        double vl_lvl = sumDba(vl_road_lvl, vl_motor_lvl) + 10
                * Math.log10(parameters.getVl_per_hour());
        double pl_lvl = sumDba(pl_road_lvl, pl_motor_lvl) + 10
                * Math.log10(parameters.getPl_per_hour());

        return sumDba(vl_lvl, pl_lvl);
    }
}