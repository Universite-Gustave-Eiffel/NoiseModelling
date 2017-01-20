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
 * @author Nicolas Fortin
 */
public class RSParameters {
    private final double lvPerHour;
    private final double hgvPerHour;

    /**
     * BBTS means very thin asphalt concrete.
     * BBUM means ultra-thin asphalt concrete
     * BBDR means drainage asphalt concrete
     * BBSG means dense asphalt concrete
     * ECF means cold mix
     * BC means cement concrete
     * ES is surface dressing
     * @link Setra Road_noise_prediction - Calculating sound emissions from road traffic - Figure 2.2 P.18
     */
    public enum SurfaceCategory {
        R1, //Less noisy, BRUM 0/6 or BBDR0/10 or BBTM 0/6 or BBTM 0/10
        R2, //Average noisy BBSG 0/10 BBTL 0/10 - type 1 or BRUM 0/10 or ECF
        R3  //Much noisy BBDG 0/14 or BBTM 0/14 or E S6/10 or BC or ES 10/14
    }

    public enum EngineState {
        SteadySpeed,
        Acceleration,
        Deceleration,
        Starting,
        Stopping
    }

    private SurfaceCategory surfaceCategory = SurfaceCategory.R2;
    private int surfaceAge = 10; // Default value means no correction of road level
    private double slopePercentage = 0;
    private double speedLv;
    private double speedHgv;
    private EngineState flowState = EngineState.SteadySpeed;

    private static double getVPl(double sLv, double speedmax, int type, int subtype) throws IllegalArgumentException {
        switch (type) {
            case 1:
                return Math.min(sLv, 100); // Highway 2x2 130 km/h
            case 2:
                switch (subtype) {
                    case 1:
                        return Math.min(sLv, 90); // 2x2 way 110 km/h
                    case 2:
                        return Math.min(sLv, 90); // 2x2 way 90km/h off belt-way
                    case 3:
                        if (speedmax < 80) {
                            return Math.min(sLv, 70);
                        } // Belt-way 70 km/h
                        else {
                            return Math.min(sLv, 85);
                        } // Belt-way 90 km/h
                }
                break;
            case 3:
                switch (subtype) {
                    case 1:
                        return sLv; // Interchange ramp
                    case 2:
                        return sLv; // Off boulevard roundabout circular junction
                    case 7:
                        return sLv; // inside-boulevard roundabout circular junction
                }
                break;
            case 4:
                switch (subtype) {
                    case 1:
                        return Math.min(sLv, 90); // lower level 2x1 way 7m 90km/h
                    case 2:
                        return Math.min(sLv, 90); // Standard 2x1 way 90km/h
                    case 3:
                        if (speedmax < 70) {
                            return Math.min(sLv, 60);
                        } // 2x1 way 60 km/h
                        else {
                            return Math.min(sLv, 80);
                        } // 2x1 way 80 km/h
                }
                break;
            case 5:
                switch (subtype) {
                    case 1:
                        return Math.min(sLv, 70); // extra boulevard 70km/h
                    case 2:
                        return Math.min(sLv, 50); // extra boulevard 50km/h
                    case 3:
                        return Math.min(sLv, 50); // extra boulevard Street 50km/h
                    case 4:
                        return Math.min(sLv, 50); // extra boulevard Street <50km/h
                    case 6:
                        return Math.min(sLv, 50); // in boulevard 70km/h
                    case 7:
                        return Math.min(sLv, 50); // in boulevard 50km/h
                    case 8:
                        return Math.min(sLv, 50); // in boulevard Street 50km/h
                    case 9:
                        return Math.min(sLv, 50); // in boulevard Street <50km/h
                }
                break;
            case 6:
                switch (subtype) {
                    case 1:
                        return Math.min(sLv, 50); // Bus-way boulevard 70km/h
                    case 2:
                        return Math.min(sLv, 50); // Bus-way boulevard 50km/h
                    case 3:
                        return Math.min(sLv, 50); // Bus-way extra boulevard Street
                    // 50km/h
                    case 4:
                        return Math.min(sLv, 50); // Bus-way extra boulevard Street
                    // <50km/h
                    case 8:
                        return Math.min(sLv, 50); // Bus-way in boulevard Street 50km/h
                    case 9:
                        return Math.min(sLv, 50); // Bus-way in boulevard Street <50km/h
                }
                break;
        }
        throw new IllegalArgumentException("Unknown road type, please check (type="
                + type + ",subtype=" + subtype + ").");
    }

    /**
     * Compute {@link RSParameters#speedHgv}
     * and {@link RSParameters#speedLv} from theses parameters
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param is_queue If true use speed_junction in speedLoad
     */
    public void setSpeedFromRoadCaracteristics(double speedLoad, double speed_junction, boolean is_queue, double speed_max,int copound_roadtype) {
        // Separation of main index and sub index
        final int roadtype = copound_roadtype / 10;
        final int roadSubType = copound_roadtype - (roadtype * 10);
        if (speed_junction > 0. && is_queue) {
            speedLv = speed_junction;
        } else if (speedLoad > 0.) {
            speedLv = speedLoad;
        } else {
            speedLv = speed_max;
        }
        speedHgv = getVPl(speedLv, speed_max, roadtype, roadSubType);
    }

    /**
     * @param slopePercentage Gradient percentage of road from -6 % to 6 %
     */
    public void setSlopePercentage(double slopePercentage) {
        this.slopePercentage = Math.min(6., Math.max(-6., slopePercentage));
    }

    /**
     * Compute the slope
     * @param beginZ Z start
     * @param endZ Z end
     * @param road_length_2d Road length (projected to Z axis)
     * @return Slope percentage
     */
    public static double computeSlope(double beginZ, double endZ, double road_length_2d) {
        return (endZ - beginZ) / road_length_2d * 100.;
    }

    /**
     * @param surfaceAge Road surface age in years, from 1 to 10 years.
     */
    public void setSurfaceAge(int surfaceAge) {
        this.surfaceAge = Math.max(1, Math.min(10, surfaceAge));
    }

    /**
     * @return Road surface age
     */
    public int getSurfaceAge() {
        return surfaceAge;
    }

    /**
     * Simplest road noise evaluation
     * @param lv_speed Average light vehicle speed
     * @param lv_speed Average heavy goods vehicle speed
     * @param lvPerHour Average light vehicle per hour
     * @param hgvPerHour Average heavy vehicle per hour
     */
    public RSParameters(double lv_speed, double hgv_speed, double lvPerHour, double hgvPerHour) {
        this.lvPerHour = lvPerHour;
        this.hgvPerHour = hgvPerHour;
        setSpeedLv(lv_speed);
        setSpeedHgv(hgv_speed);
    }

    public void setSurfaceCategory(SurfaceCategory surfaceCategory) {
        this.surfaceCategory = surfaceCategory;
    }

    public void setSpeedLv(double speedLv) {
        this.speedLv = speedLv;
    }

    public void setSpeedHgv(double speedHgv) {
        this.speedHgv = speedHgv;
    }

    /**
     * Set the engine state for vehicle.
     * @param flowState enum
     */
    public void setFlowState(EngineState flowState) {
        this.flowState = flowState;
    }

    public double getLvPerHour() {
        return lvPerHour;
    }

    public double getHgvPerHour() {
        return hgvPerHour;
    }

    public SurfaceCategory getSurfaceCategory() {
        return surfaceCategory;
    }

    public double getSlopePercentage() {
        return slopePercentage;
    }

    public double getSpeedLv() {
        return speedLv;
    }

    public double getSpeedHgv() {
        return speedHgv;
    }

    public EngineState getFlowState() {
        return flowState;
    }
}
