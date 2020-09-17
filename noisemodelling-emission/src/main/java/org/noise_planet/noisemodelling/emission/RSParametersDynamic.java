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
 * @author Nicolas Fortin
 * @author Pierre Aumond - 03/05/2017 - 21/08/2018
 * @author Arnaud Can - 27/02/2018 - 21/08/2018
 */
public class RSParametersDynamic {
    private final double speed;
    private final double acceleration;
    private final int veh_type;
    private final int acc_type;
    private final int FreqParam;
    private final double Temperature;
    private final int RoadSurface;
    private final boolean Stud ;
    private final double Junc_dist;
    private final int Junc_type;
    private final double LwStd;
    private final int VehId;


    private int surfaceAge;
    private double slopePercentage;

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
     * @param slopePercentage Gradient percentage of road from -6 % to 6 %
     */
    public void setSlopePercentage(double slopePercentage) {
        this.slopePercentage = Math.min(6., Math.max(-6., slopePercentage));
    }

    /**
     * @param slopePercentage Gradient percentage of road from -6 % to 6 %
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
     * @param speed Vehicle speed
     * @param acceleration Vehicle acceleration
     * @param veh_type Vehicle type (CNOSSOS categories)
     * @param acc_type Acceleration mode (1 = Distance to Junction (CNOSSOS), 2= Correction from IMAGINE with bounds , 3 = Correction from IMAGINE without bounds)
     * @param FreqParam Studied Frequency
     * @param Temperature Temperature(Celsius)
     * @param RoadSurface Road surface between 0 and 14
     * @param Stud True = equipped with studded tyres
     * @param Junc_dist Distance to junction
     * @param Junc_type Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     * @param LwStd Standard Deviation of Lw
     * @param VehId Vehicle ID used as a seed for LwStd
     */
    public RSParametersDynamic(double speed, double acceleration, int veh_type, int acc_type, int FreqParam, double Temperature, int RoadSurface, boolean Stud, double Junc_dist, int Junc_type, double LwStd, int VehId) {

        if (Junc_type <0 || Junc_type>2 ) throw new IllegalArgumentException("Unlnown Junction type for a section.");
        this.speed = speed;
        this.acceleration = acceleration;
        this.veh_type = veh_type;
        this.acc_type = acc_type;
        this.FreqParam = Math.max(0, FreqParam);
        this.Temperature = Temperature;
        this.RoadSurface = RoadSurface;
        this.Stud = Stud;
        this.Junc_dist = Math.max(0, Junc_dist);
        this.Junc_type = Math.max(0, Math.min(2, Junc_type));
        this.LwStd = LwStd;
        this.VehId = VehId;

    }

    public double getLwStd() {
        return LwStd;
    }

    public int getVehId() {
        return VehId;
    }

    public double getSlopePercentage() {
        return slopePercentage;
    }

    public double getSpeed() {
        return speed;
    }

    public int getFreqParam() {
        return FreqParam;
    }

    public int getAcc_type() {
        return acc_type;
    }

    public int getVeh_type() {
        return veh_type;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getTemperature() { return Temperature;}

    public int getRoadSurface() {return RoadSurface;}

    public boolean getStud() {return Stud;}


    public double getJunc_dist() {return Junc_dist;}

    public int getJunc_type() {return Junc_type;}

}

