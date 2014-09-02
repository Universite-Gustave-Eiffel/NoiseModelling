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
public class EvaluateRoadSourceParameter {
    private final double speed_load;
    private final int vl_per_hour;
    private final int pl_per_hour;

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
        Deceleration
    }

    private SurfaceCategory surfaceCategory = SurfaceCategory.R2;
    private int surfaceAge = 10; // Default value means no correction of road level
    private double slopePercentage = 0;
    private double speedVl;
    private double speedPl;
    private EngineState vlState = EngineState.SteadySpeed;
    private EngineState plState = EngineState.SteadySpeed;

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
     * Compute {@link EvaluateRoadSourceParameter#speedPl}
     * and {@link EvaluateRoadSourceParameter#speedVl} from theses parameters
     * @param speed_junction Speed in the junction section
     * @param speed_max Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param is_queue If true use speed_junction in speed_load
     */
    public void setSpeedFromRoadCaracteristics(double speed_junction, boolean is_queue, double speed_max,int copound_roadtype) {
        // Separation of main index and sub index
        final int roadtype = copound_roadtype / 10;
        final int roadSubType = copound_roadtype - (roadtype * 10);
        if (speed_junction > 0. && is_queue) {
            speedVl = speed_junction;
        } else if (speed_load > 0.) {
            speedVl = speed_load;
        } else {
            speedVl = speed_max;
        }
        speedPl = getVPl(speedVl, speed_max, roadtype, roadSubType);
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
     * @param speed_load Average vehicle speed
     * @param vl_per_hour Average light vehicle per hour
     * @param pl_per_hour Average heavy vehicle per hour
     */
    public EvaluateRoadSourceParameter(double speed_load, int vl_per_hour, int pl_per_hour) {
        this.speed_load = speed_load;
        this.vl_per_hour = vl_per_hour;
        this.pl_per_hour = pl_per_hour;
        setSpeedVl(speed_load);
        setSpeedPl(speed_load);
    }

    public void setSurfaceCategory(SurfaceCategory surfaceCategory) {
        this.surfaceCategory = surfaceCategory;
    }

    public void setSpeedVl(double speedVl) {
        // Validity discussed 3.5.3.2 - Speed validity of results P.45 of Road Noise Prediction
        this.speedVl = Math.min(130, Math.max(5, speedVl));
    }

    public void setSpeedPl(double speedPl) {
        // Validity discussed 3.5.3.2 - Speed validity of results P.45 of Road Noise Prediction
        this.speedPl = Math.min(100, Math.max(5, speedPl));
    }

    public void setVlState(EngineState vlState) {
        this.vlState = vlState;
    }

    public void setPlState(EngineState plState) {
        this.plState = plState;
    }

    public double getSpeed_load() {
        return speed_load;
    }

    public int getVl_per_hour() {
        return vl_per_hour;
    }

    public int getPl_per_hour() {
        return pl_per_hour;
    }

    public SurfaceCategory getSurfaceCategory() {
        return surfaceCategory;
    }

    public double getSlopePercentage() {
        return slopePercentage;
    }

    public double getSpeedVl() {
        return speedVl;
    }

    public double getSpeedPl() {
        return speedPl;
    }

    public EngineState getVlState() {
        return vlState;
    }

    public EngineState getPlState() {
        return plState;
    }

}
