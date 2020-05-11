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
package org.noise_planet.noisemodelling.emission;

/**
 * RoadSource parameters for CNOSSOS method
 * @author Nicolas Fortin
 * @author Pierre Aumond - 03/05/2017
 */
public class RSParametersCnossos {
    private final double lvPerHour;
    private final double mvPerHour;
    private final double hgvPerHour;
    private final double wavPerHour;
    private final double wbvPerHour;
    private final int FreqParam;
    private final double Temperature;
    private final String roadSurface;
    private final double Ts_stud;
    private final double Pm_stud;
    private final double Junc_dist;
    private final int Junc_type;

    private double slopePercentage;
    private double speedLv;
    private double speedMv;
    private double speedHgv;
    private double speedWav;
    private double speedWbv;

    private int coeffVer = 2;

    /**
     * @param coeffVer
     */
    public void setCoeffVer(int coeffVer) {
        this.coeffVer = coeffVer;
    }

    public int getCoeffVer() {
        return this.coeffVer;
    }


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
     * Compute {@link RSParametersCnossos#speedHgv}
     * and {@link RSParametersCnossos#speedLv} from theses parameters
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
        speedMv = getVPl(speedLv, speed_max, roadtype, roadSubType);
        speedWav = getVPl(speedLv, speed_max, roadtype, roadSubType);
        speedWbv = getVPl(speedLv, speed_max, roadtype, roadSubType);
    }

    /**
     * @param slopePercentage Gradient percentage of road from -12 % to 12 %
     */
    public void setSlopePercentage(double slopePercentage) {
        this.slopePercentage = Math.min(12., Math.max(-12., slopePercentage));
    }

    /**
     * @param slopePercentage Gradient percentage of road from -12 % to 12 %
     */
    public void setSlopePercentage_without_limit(double slopePercentage) {
        this.slopePercentage = slopePercentage;
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
     * Simplest road noise evaluation
     * Vehicles category Table 3 P.31 CNOSSOS_EU_JRC_REFERENCE_REPORT
     * lv : Passenger cars, delivery vans ≤ 3.5 tons, SUVs , MPVs including trailers and caravans
     * mv: Medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle
     * hgv: Heavy duty vehicles, touring cars, buses, with three or more axles
     * wav:  mopeds, tricycles or quads ≤ 50 cc
     * wbv:  motorcycles, tricycles or quads > 50 cc
     * @param lv_speed Average light vehicle speed
     * @param mv_speed Average medium vehicle speed
     * @param hgv_speed Average heavy goods vehicle speed
     * @param wav_speed Average light 2 wheels vehicle speed
     * @param wbv_speed Average heavy 2 wheels vehicle speed
     * @param lvPerHour Average light vehicle per hour
     * @param mvPerHour Average heavy vehicle per hour
     * @param hgvPerHour Average heavy vehicle per hour
     * @param wavPerHour Average heavy vehicle per hour
     * @param wbvPerHour Average heavy vehicle per hour
     * @param FreqParam Studied Frequency
     * @param Temperature Temperature (Celsius)
     * @param roadSurface roadSurface empty default, NL01 FR01 ..
     * @param Ts_stud A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres and during .
     * @param Pm_stud Average proportion of vehicles equipped with studded tyres
     * @param Junc_dist Distance to junction
     * @param Junc_type Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     */
    public RSParametersCnossos(double lv_speed, double mv_speed, double hgv_speed, double wav_speed, double wbv_speed, double lvPerHour, double mvPerHour, double hgvPerHour, double wavPerHour, double wbvPerHour, int FreqParam, double Temperature, String roadSurface, double Ts_stud, double Pm_stud, double Junc_dist, int Junc_type) {
        this.lvPerHour = Math.max(0, lvPerHour);
        this.mvPerHour = Math.max(0, mvPerHour);
        this.hgvPerHour = Math.max(0, hgvPerHour);
        this.wavPerHour = Math.max(0, wavPerHour);
        this.wbvPerHour = Math.max(0, wbvPerHour);
        this.FreqParam = Math.max(0, FreqParam);
        this.Temperature = Temperature;
        this.roadSurface = roadSurface;
        this.Ts_stud = Math.max(0, Math.min(12, Ts_stud));
        this.Pm_stud = Math.max(0, Math.min(1, Pm_stud));
        this.Junc_dist = Math.max(0, Junc_dist);
        this.Junc_type = Math.max(0, Math.min(2, Junc_type));
        setSpeedLv(lv_speed);
        setSpeedMv(mv_speed);
        setSpeedHgv(hgv_speed);
        setSpeedWav(wav_speed);
        setSpeedWbv(wbv_speed);
    }

    public void setSpeedLv(double speedLv) {
        this.speedLv = speedLv;
    }
    public void setSpeedMv(double speedMv) {
        this.speedMv = speedMv;
    }
    public void setSpeedHgv(double speedHgv) {
        this.speedHgv = speedHgv;
    }
    public void setSpeedWav(double speedWav) {
        this.speedWav = speedWav;
    }
    public void setSpeedWbv(double speedWbv) {
        this.speedWbv = speedWbv;
    }

    public double getLvPerHour() {
        return lvPerHour;
    }
    public double getMvPerHour() {
        return mvPerHour;
    }
    public double getHgvPerHour() {
        return hgvPerHour;
    }
    public double getWavPerHour() {
        return wavPerHour;
    }
    public double getWbvPerHour() {
        return wbvPerHour;
    }

    public double getSlopePercentage() {
        return slopePercentage;
    }

    public double getSpeedLv() {
        return speedLv;
    }
    public double getSpeedMv() {
        return speedMv;
    }
    public double getSpeedHgv() {
        return speedHgv;
    }
    public double getSpeedWav() {
        return speedWav;
    }
    public double getSpeedWbv() {
        return speedWbv;
    }

    public int getFreqParam() {
        return FreqParam;
    }

    public double getTemperature() { return Temperature;}

    public String getRoadSurface() {return roadSurface;}

    public double getTs_stud() {return Ts_stud;}

    public double getPm_stud() {return Pm_stud;}

    public double getJunc_dist() {return Junc_dist;}

    public int getJunc_type() {return Junc_type;}

}

