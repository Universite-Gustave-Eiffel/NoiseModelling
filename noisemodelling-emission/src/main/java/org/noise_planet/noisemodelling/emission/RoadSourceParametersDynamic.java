/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.emission;

/**
 * @author Nicolas Fortin
 * @author Pierre Aumond - 03/05/2017 - 21/08/2018
 * @author Arnaud Can - 27/02/2018 - 21/08/2018
 */

public class RoadSourceParametersDynamic {
    private final double speed;
    private final double acceleration;
    private final String veh_type;
    private final int acc_type;
    private final int FreqParam;
    private final double Temperature;
    private final String roadSurface;
    private final boolean Stud ;
    private final double Junc_dist;
    private final int Junc_type;
    private final double LwStd;
    private final int VehId;

    private int coeffVer = 2;


    private int surfaceAge;
    private double slopePercentage;

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
     * @param roadSurface Road surface between 0 and 14
     * @param Stud True = equipped with studded tyres
     * @param Junc_dist Distance to junction
     * @param Junc_type Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     * @param LwStd Standard Deviation of Lw
     * @param VehId Vehicle ID used as a seed for LwStd
     */
    public RoadSourceParametersDynamic(double speed, double acceleration, String veh_type, int acc_type, int FreqParam, double Temperature, String roadSurface, boolean Stud, double Junc_dist, int Junc_type, double LwStd, int VehId) {

        if (Junc_type <0 || Junc_type>2 ) throw new IllegalArgumentException("Unlnown Junction type for a section.");
        this.speed = speed;
        this.acceleration = acceleration;
        this.veh_type = veh_type;
        this.acc_type = acc_type;
        this.FreqParam = Math.max(0, FreqParam);
        this.Temperature = Temperature;
        this.roadSurface = roadSurface;
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

    public String getVeh_type() {
        return veh_type;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getTemperature() { return Temperature;}

    public String getRoadSurface() {return roadSurface;}

    public boolean getStud() {return Stud;}


    public double getJunc_dist() {return Junc_dist;}

    public int getJunc_type() {return Junc_type;}

}

