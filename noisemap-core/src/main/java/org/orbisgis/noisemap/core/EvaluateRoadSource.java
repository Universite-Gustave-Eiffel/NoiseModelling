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
 * Return the dB(A) value corresponding to the parameters
 * Reference document is reference 0924-1A - © 2011 Sétra - ISRN No. : EQ-SETRA--11-ED13--FR+ENG
 * http://www.setra.developpement-durable.gouv.fr/IMG/pdf/0924-1A_Road_noise_prediction_v1.pdf
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
                Math.max(parameters.getFlowState() == RSParameters.EngineState.SteadySpeed ? 20 : 5,
                        parameters.getSpeedLv())));
        // Validity discussed 3.5.3.2 - Speed validity of results P.45 of Road Noise Prediction
        parameters.setSpeedHgv(Math.min(100,
                Math.max(parameters.getFlowState() == RSParameters.EngineState.SteadySpeed ? 20 : 5,
                        parameters.getSpeedHgv())));

        double lvCompound;
        double hgvCompound;

        // P 108. D.2.5 - Starting and stopping sections
        // There is no breakdown into engine and rolling noise components, the values below are expressed
        // directly in Lw/m(1 veh/h)
        if(parameters.getFlowState() == RSParameters.EngineState.Starting ||
                parameters.getFlowState() == RSParameters.EngineState.Stopping) {
            // Starting or stopping section
            if (parameters.getFlowState() == RSParameters.EngineState.Stopping){
                lvCompound = 44.5;
                if (parameters.getSlopePercentage() < 2) {
                    // downward slope
                    hgvCompound = 58.0 + (-parameters.getSlopePercentage() - 2);
                } else {
                    hgvCompound = 58.0;
                }
            } else {
                // Starting condition.
                lvCompound = 51.1;
                if (parameters.getSlopePercentage() < 2) {
                    // downward slope
                    hgvCompound = 62.4;
                } else {
                    hgvCompound = 62.4 + Math.max(0, 2 * (parameters.getSlopePercentage() - 4.5));
                }
            }
        } else {

            // ///////////////////////
            // Noise road/tire
            // cf. NMPB 2008 1 - Calculating sound emissions from road traffic
            // p. 18 at 20
            // 2.7.2.3 - Emission power per metre of line-source depending on the age of the surface
            double lvRoadLvl; // Lw/m (1 veh/h)
            double hgvRoadLvl;// Lw/m (1 veh/h)
            // surface category -> R1
            if (parameters.getSurfaceCategory() == RSParameters.SurfaceCategory.R1) {
                lvRoadLvl = getNoiseLvl(53.4, 21., parameters.getSpeedLv(), 90.);
                hgvRoadLvl = getNoiseLvl(61.5, 20., parameters.getSpeedHgv(), 80.);
                // check surface age
                if (parameters.getSurfaceAge() < 2) {
                    lvRoadLvl = lvRoadLvl - 4.;
                    hgvRoadLvl = hgvRoadLvl - 2.4;
                } else {
                    lvRoadLvl = lvRoadLvl + 0.50 * (parameters.getSurfaceAge() - 10);
                    hgvRoadLvl = hgvRoadLvl + 0.30 * (parameters.getSurfaceAge() - 10);
                }
            }
            // surface category -> R3
            else if (parameters.getSurfaceCategory() == RSParameters.SurfaceCategory.R3) {
                lvRoadLvl = getNoiseLvl(57.5, 21.4, parameters.getSpeedLv(), 90.);
                hgvRoadLvl = getNoiseLvl(64.2, 20., parameters.getSpeedHgv(), 80.);
                // check surface age
                if (parameters.getSurfaceAge() < 2) {
                    lvRoadLvl = lvRoadLvl - 1.6;
                    hgvRoadLvl = hgvRoadLvl - 1.;
                } else {
                    lvRoadLvl = lvRoadLvl + 0.20 * (parameters.getSurfaceAge() - 10);
                    hgvRoadLvl = hgvRoadLvl + 0.12 * (parameters.getSurfaceAge() - 10);
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
            double lvMotorLvl;
            if (parameters.getFlowState() == RSParameters.EngineState.Acceleration) {
                // accelerated pace.
                if(parameters.getSpeedLv() <= 20.) {
                    // 5-20
                    lvMotorLvl = getNoiseLvl(61.8, 14.1, parameters.getSpeedLv(), 90.);
                }else if (parameters.getSpeedLv() <= 100.) {
                    // 20-100
                    lvMotorLvl = getNoiseLvl(46.1, -10., parameters.getSpeedLv(), 90.);
                } else {
                    // 100-130
                    lvMotorLvl = getNoiseLvl(44.3, 28.6, parameters.getSpeedLv(), 90.);
                }
            } else if (parameters.getFlowState() == RSParameters.EngineState.Deceleration) {
                // decelerated pace.
                if(parameters.getSpeedLv() <= 10.) {
                    // 5-10
                    lvMotorLvl = getNoiseLvl(31.6, -10., parameters.getSpeedLv(), 90.);
                } else if(parameters.getSpeedLv() <= 25.) {
                    // 10-25
                    lvMotorLvl = getNoiseLvl(49.4, 8.7, parameters.getSpeedLv(), 90.);
                }else if (parameters.getSpeedLv() <= 80.) {
                    // 25-80
                    lvMotorLvl = getNoiseLvl(42.1, -4.5, Math.max(20, parameters.getSpeedLv()), 90.);
                } else if (parameters.getSpeedLv() <= 110.) {
                    // 80-110
                    lvMotorLvl = getNoiseLvl(42.4, 2., parameters.getSpeedLv(), 90.);
                } else {
                    // 110-130
                    lvMotorLvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedLv(), 90.);
                }
            } else {
                // default or steady speed.
                if (parameters.getSpeedLv() <= 30.) {
                    // check again the speed lvl.
                    lvMotorLvl = getNoiseLvl(36.7, -10., Math.max(20, parameters.getSpeedLv()), 90.);
                } else if (parameters.getSpeedLv() > 30. && parameters.getSpeedLv() <= 110.) {
                    lvMotorLvl = getNoiseLvl(42.4, 2., parameters.getSpeedLv(), 90.);
                } else {
                    // default speed for steady speed pace.
                    lvMotorLvl = getNoiseLvl(40.7, 21.3, parameters.getSpeedLv(), 90.);
                }
            }

            // Calculate the emission powers of heavies goods vehicles.
            double hgvMotorLvl;
            if (parameters.getSpeedHgv() <= 70.) {
                // P 109. D.17
                // 5-70
                hgvMotorLvl = getNoiseLvl(49.6, -10., parameters.getSpeedHgv(), 80.);
            } else {
                // P 109. D.17
                // 70 km/h to 100 km/h
                hgvMotorLvl = getNoiseLvl(50.4, 3., parameters.getSpeedHgv(), 80.);
            }
            // Correction of hgvMotorLvl independent of speed
            // P109. D.18
            if (parameters.getSlopePercentage() <= -2) {
                // downwards 2% <= p <= 6%
                if (parameters.getFlowState() == RSParameters.EngineState.Acceleration) {
                    hgvMotorLvl = hgvMotorLvl + 5;
                } else {
                    // Steady and deceleration, the same formulae
                    hgvMotorLvl = hgvMotorLvl + 1 * (-parameters.getSlopePercentage() - 2);
                }
            } else if (parameters.getSlopePercentage() < 2) {
                // 0% <= p <= 2%
                if (parameters.getFlowState() == RSParameters.EngineState.Acceleration) {
                    hgvMotorLvl = hgvMotorLvl + 5.;
                }
            } else {
                // upwards 2% <= p <= 6%
                if (parameters.getFlowState() == RSParameters.EngineState.Acceleration) {
                    hgvMotorLvl = hgvMotorLvl + Math.max(2 * (parameters.getSlopePercentage() - 2), 5);
                } else if (parameters.getFlowState() == RSParameters.EngineState.SteadySpeed) {
                    hgvMotorLvl = hgvMotorLvl + 2 * (parameters.getSlopePercentage() - 2);
                }
            }
            lvCompound = sumDba(lvRoadLvl, lvMotorLvl);
            hgvCompound = sumDba(hgvRoadLvl, hgvMotorLvl);
        }

        // ////////////////////////
        // Lw/m (1 veh/h) to ?
        double lvLvl = lvCompound  + 10
                * Math.log10(parameters.getLvPerHour());
        double hgvLvl = hgvCompound + 10
                * Math.log10(parameters.getHgvPerHour());


        return sumDba(lvLvl, hgvLvl);
    }
}