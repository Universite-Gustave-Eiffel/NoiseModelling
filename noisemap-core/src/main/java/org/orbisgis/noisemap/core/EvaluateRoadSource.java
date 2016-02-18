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
        return base + adj * Math.log10(speed / speedBase);
    }

    private static Double sumDba(Double dBA1, Double dBA2) {
        return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB(A)
     */
    public static double evaluate(RSParameters parameters) {
        // Fix vehicle speed to validity domains
        // Validity discussed 3.5.3.2 - Speed validity of results P.45 of Road Noise Prediction
        parameters.setSpeedLv(Math.min(130,
                Math.max(parameters.getLvState() == RSParameters.EngineState.SteadySpeed ? 20 : 5,
                        parameters.getSpeedLv())));
        // Validity discussed 3.5.3.2 - Speed validity of results P.45 of Road Noise Prediction
        parameters.setSpeedHgv(Math.min(100,
                Math.max(parameters.getHgvState() == RSParameters.EngineState.SteadySpeed ? 20 : 5,
                        parameters.getSpeedHgv())));
        // ///////////////////////
        // Noise road/tire
        // cf. NMPB 2008 1 - Calculating sound emissions from road traffic
        // p. 18 at 20
        // The R2 surface is use by default
        // 2.7.2.3 - Emission power per metre of line-source depending on the age of the surface

        double lvRoadLvl;
        double hgvRoadLvl;

        // surface category -> R1
        if (parameters.getSurfaceCategory() == RSParameters.SurfaceCategory.R1){
            lvRoadLvl = getNoiseLvl(53.4, 21., parameters.getSpeedLv(), 90.);
            hgvRoadLvl = getNoiseLvl(61.5, 20., parameters.getSpeedHgv(), 80.);
            // check surface age
            if (parameters.getSurfaceAge() < 2){
                lvRoadLvl = lvRoadLvl-4.;
                hgvRoadLvl = hgvRoadLvl-2.4;
            } else {
                lvRoadLvl = lvRoadLvl + 0.50 * (parameters.getSurfaceAge() - 10);
                hgvRoadLvl = hgvRoadLvl + 0.30 * (parameters.getSurfaceAge() - 10);
            }
        }
        // surface category -> R3
        else if (parameters.getSurfaceCategory() == RSParameters.SurfaceCategory.R3){
            lvRoadLvl = getNoiseLvl(57.5, 21.4, parameters.getSpeedLv(), 90.);
            hgvRoadLvl = getNoiseLvl(64.2, 20., parameters.getSpeedHgv(), 80.);
            // check surface age
            if (parameters.getSurfaceAge() < 2){
                lvRoadLvl = lvRoadLvl-1.6;
                hgvRoadLvl = hgvRoadLvl-1.;
            } else {
                lvRoadLvl = lvRoadLvl + 0.20*(parameters.getSurfaceAge() - 10);
                hgvRoadLvl = hgvRoadLvl + 0.12*(parameters.getSurfaceAge() - 10);
            }
        } else {
            // surface category -> R2 or default
            lvRoadLvl = getNoiseLvl(55.4, 20.1, parameters.getSpeedLv(), 90.);
            hgvRoadLvl = getNoiseLvl(63.4, 20., parameters.getSpeedHgv(), 80.);
            // check surface age                                                                                        // TODO default surface age 2 at 10 years. so need to initialize default value
            if (parameters.getSurfaceAge() < 2) {
                lvRoadLvl = lvRoadLvl - 2.;
                hgvRoadLvl = hgvRoadLvl - 1.2;
            } else {
                lvRoadLvl = lvRoadLvl + 0.25 * (parameters.getSurfaceAge() - 10);
                hgvRoadLvl = hgvRoadLvl + 0.15 * (parameters.getSurfaceAge() - 10);
            }
        }

        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles.
        double lvMotorLvl = 0.;
        // See 2.7.1.2 - Special case: "starting" and "stopping" sections
        // Speed lower than 25 km/h are considered as starting sections
        if (parameters.getSpeedLv() < 25. ||
                parameters.getLvState() == RSParameters.EngineState.Starting ||
                parameters.getLvState() == RSParameters.EngineState.Stopping) {
            if (parameters.getLvState() == RSParameters.EngineState.Stopping){
                lvMotorLvl = 44.5;
            } else {
                // Starting condition.
                lvMotorLvl = 51.1;
            }
        } else {
            // check speed the lvl.
            // TODO can initialize default at 25 km/h ?
            if (parameters.getLvState() == RSParameters.EngineState.Acceleration){
                // accelerated pace.
                if (parameters.getSpeedLv() <= 100.) {         // check again the speed lvl.
                    lvMotorLvl = getNoiseLvl(46.1, -10., Math.max(20, parameters.getSpeedLv()), 90.);
                } else {
                    // default speed lvl for accelerated pace
                    lvMotorLvl = getNoiseLvl(44.3, 28.6, parameters.getSpeedLv(), 90.);
                }
            } else if (parameters.getLvState() == RSParameters.EngineState.Deceleration){
                // decelerated pace.
                if (parameters.getSpeedLv() <= 80.) {          // check again the speed lvl.
                    lvMotorLvl = getNoiseLvl(42.1, -4.5, Math.max(20, parameters.getSpeedLv()), 90.);
                } else if (parameters.getSpeedLv() > 80. && parameters.getSpeedLv() <= 110.) {
                    lvMotorLvl = getNoiseLvl(42.4, 2., parameters.getSpeedLv(), 90.);
                } else {                    // default speed lvl for decelerated pace.
                    lvMotorLvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedLv(), 90.);
                }
            } else {
                // default or steady speed.
                if (parameters.getSpeedLv() <= 30.) {
                    // check again the speed lvl.
                    lvMotorLvl = getNoiseLvl(36.7, -10., Math.max(20, parameters.getSpeedLv()), 90.);
                } else if (parameters.getSpeedLv() > 30. && parameters.getSpeedLv() <= 110. ) {
                    lvMotorLvl = getNoiseLvl(42.4, 2., parameters.getSpeedLv(), 90.);
                } else {
                    // default speed for steady speed pace.
                    lvMotorLvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedLv(), 90.);
                }
            }
        }



        // Calculate the emission powers of heavies goods vehicles.
        double hgvMotorLvl;

        if (parameters.getSpeedHgv() <= 25.) {
            // TODO initialize default speed
            if (parameters.getHgvState() == RSParameters.EngineState.Deceleration){
                // stopping condition
                if (parameters.getSlopePercentage() < 2){
                    // downward slope
                    hgvMotorLvl = 58.0 + (-parameters.getSlopePercentage() - 2);
                } else {
                    hgvMotorLvl = 58.0;
                }
            } else {
                // restart condition
                if (parameters.getSlopePercentage() < 2) {
                    // downward slope
                    hgvMotorLvl = 62.4;
                } else {
                    hgvMotorLvl = 62.4 + Math.max(0, 2 *( parameters.getSlopePercentage() - 4.5));
                }
            }
        } else if(parameters.getSpeedHgv() <= 70.) {
            hgvMotorLvl = getNoiseLvl(49.6, -10., Math.max(20, parameters.getSpeedHgv()), 80.);
            if (parameters.getSlopePercentage() < 0){
                if (parameters.getHgvState() == RSParameters.EngineState.Acceleration){
                    hgvMotorLvl = hgvMotorLvl + 5 ;
                } else {
                    hgvMotorLvl = hgvMotorLvl + 1*(-parameters.getSlopePercentage() -2);
                }
            } else if (parameters.getSlopePercentage() > 2){
                if (parameters.getHgvState() == RSParameters.EngineState.Acceleration){
                    hgvMotorLvl = hgvMotorLvl + 5 + Math.max(2*(parameters.getSlopePercentage()-4.5),0) ;
                } else if(parameters.getHgvState() == RSParameters.EngineState.SteadySpeed) {
                    hgvMotorLvl = hgvMotorLvl + 2 * (parameters.getSlopePercentage() - 2);
                }
            } else {
                if (parameters.getHgvState() == RSParameters.EngineState.Acceleration){
                    hgvMotorLvl = hgvMotorLvl + 5.;
                }
            }
        } else {
            // 70 km/h to 100 km/h
            // Table 2.11
            hgvMotorLvl = getNoiseLvl(50.4, 3., parameters.getSpeedHgv(), 80.);
            if(parameters.getSlopePercentage() < 2) {
                if (parameters.getHgvState() == RSParameters.EngineState.Acceleration){
                    hgvMotorLvl = hgvMotorLvl + 5 ;
                } else if(parameters.getSlopePercentage() <= -2) {
                    hgvMotorLvl = hgvMotorLvl + (-parameters.getSlopePercentage() -2);
                }
            } else {
                if (parameters.getHgvState() == RSParameters.EngineState.Acceleration){
                    hgvMotorLvl = hgvMotorLvl + 5 + Math.max(2*(parameters.getSlopePercentage()-4.5),0) ;
                } else if(parameters.getHgvState() == RSParameters.EngineState.SteadySpeed) {
                    hgvMotorLvl = hgvMotorLvl + 2 * (parameters.getSlopePercentage() - 2);
                }
            }
        }


        // ////////////////////////
        // Energetic SUM
        double lvLvl = sumDba(lvRoadLvl, lvMotorLvl) + 10
                * Math.log10(parameters.getLvPerHour());
        double hgvLvl = sumDba(hgvRoadLvl, hgvMotorLvl) + 10
                * Math.log10(parameters.getHgvPerHour());

        return sumDba(lvLvl, hgvLvl);
    }
}