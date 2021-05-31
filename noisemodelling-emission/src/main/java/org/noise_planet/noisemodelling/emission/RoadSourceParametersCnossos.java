/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 * <p>
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission;

import java.io.IOException;

/**
 * RoadSource parameters for CNOSSOS method
 *
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel - 03/05/2017 - Update 12/01/2021
 */

public class RoadSourceParametersCnossos {
    private final double lvPerHour; // Qm in 2015 directive - It shall be expressed as yearly average per hour, per time period (day-evening-night), per vehicle class and per source line. For all categories, input traffic flow data derived from traffic counting or from traffic models shall be used.
    private final double mvPerHour; // Qm in 2015 directive
    private final double hgvPerHour; // Qm in 2015 directive
    private final double wavPerHour; // Qm in 2015 directive
    private final double wbvPerHour; // Qm in 2015 directive
    private final int FreqParam; // Frequency in Hz
    private final double Temperature; // Temperature in °C
    private final String roadSurface; // Road surface identifier, see coefficients_cnossos2019.json for name list
    private final double tsStud; // Period (in months) where the average ratio of the total volume of light vehicles per hour equipped with studded tyres
    private final double qStudRatio; // Average ratio of the total volume of light vehicles per hour equipped with studded tyres during the period Ts_stud (in months)
    private final double Junc_dist; // Distance to junction
    private final int Junc_type; // Junction type (k=1 traffic lights, k=2 roundabout)

    private double slopePercentage = 0; // slope s (in %), In the case of a bi-directional traffic flow, it is necessary to split the flow into two components and correct half for uphill and half for downhill.
    private int way = 1; // 1 = direct, 2 = inverse, 3 = double

    private double speedLv; // cat 1 vehicle speed vm (in km/h)
    private double speedMv; // cat 2 vehicle speed  (in km/h)
    private double speedHgv; // cat 3 vehicle speed  (in km/h)
    private double speedWav; // cat 4a vehicle speed  (in km/h)
    private double speedWbv; // cat 4b vehicle speed  (in km/h)

    private int coeffVer = 2; // default coefficient version (1 = 2015, 2 = 2019)

    /**
     * set Coefficient version  (1 = 2015, 2 = 2019)
     *
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
     * Compute {@link RoadSourceParametersCnossos#speedHgv}
     * and {@link RoadSourceParametersCnossos#speedLv} from theses parameters
     *
     * @param speed_junction   Speed in the junction section
     * @param speed_max        Maximum speed authorized
     * @param copound_roadtype Road surface type.
     * @param is_queue         If true use speed_junction in speedLoad
     */
    public void setSpeedFromRoadCaracteristics(double speedLoad, double speed_junction, boolean is_queue, double speed_max, int copound_roadtype) {
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
     * Eq. 2.2.13
     *
     * @param slopePercentage Gradient percentage of road from -12 % to 12 %
     */
    public void setSlopePercentage(double slopePercentage) {
        this.slopePercentage = Math.min(12., Math.max(-12., slopePercentage));
    }

    /**
     * Set way of the road section
     *
     * @param way
     */
    public void setWay(int way) {
        this.way = way;
    }

    /**
     * Eq. 2.2.13
     *
     * @param slopePercentage Gradient percentage of road from -12 % to 12 %
     */
    public void setSlopePercentage_without_limit(double slopePercentage) {
        this.slopePercentage = slopePercentage;
    }


    /**
     * Simplest road noise evaluation
     * Vehicles category Table 2.2.a Directive 2015/Amendments 2019
     * lv : Passenger cars, delivery vans ≤ 3.5 tons, SUVs , MPVs including trailers and caravans
     * mv: Medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle
     * hgv: Heavy duty vehicles, touring cars, buses, with three or more axles
     * wav:  mopeds, tricycles or quads ≤ 50 cc
     * wbv:  motorcycles, tricycles or quads > 50 cc
     *
     * @param lv_speed    Average light vehicle speed
     * @param mv_speed    Average medium vehicle speed
     * @param hgv_speed   Average heavy goods vehicle speed
     * @param wav_speed   Average light 2 wheels vehicle speed
     * @param wbv_speed   Average heavy 2 wheels vehicle speed
     * @param lvPerHour   Average light vehicle per hour
     * @param mvPerHour   Average heavy vehicle per hour
     * @param hgvPerHour  Average heavy vehicle per hour
     * @param wavPerHour  Average heavy vehicle per hour
     * @param wbvPerHour  Average heavy vehicle per hour
     * @param FreqParam   Studied Frequency
     * @param Temperature Temperature (Celsius)
     * @param roadSurface roadSurface empty default, NL01 FR01 ..
     * @param Ts_stud     A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres and during .
     * @param Pm_stud     Average proportion of vehicles equipped with studded tyres
     * @param Junc_dist   Distance to junction
     * @param Junc_type   Type of junction ((k = 1 for a crossing with traffic lights ; k = 2 for a roundabout)
     */
    public RoadSourceParametersCnossos(double lv_speed, double mv_speed, double hgv_speed, double wav_speed, double wbv_speed, double lvPerHour, double mvPerHour, double hgvPerHour, double wavPerHour, double wbvPerHour, int FreqParam, double Temperature, String roadSurface, double Ts_stud, double Pm_stud, double Junc_dist, int Junc_type) {

        if (lvPerHour < 0)
            throw new IllegalArgumentException("The flow rate of light vehicles is less than zero on one section.");
        if (mvPerHour < 0)
            throw new IllegalArgumentException("The flow rate of medium vehicles is less than zero on one section.");
        if (hgvPerHour < 0)
            throw new IllegalArgumentException("The flow rate of heavy vehicles is less than zero on one section.");
        if (wavPerHour < 0)
            throw new IllegalArgumentException("The flow rate of 2W(a) vehicles is less than zero on one section.");
        if (wbvPerHour < 0)
            throw new IllegalArgumentException("The flow rate of 2W(b) vehicles is less than zero on one section.");
        if (lv_speed < 0)
            throw new IllegalArgumentException("The speed light vehicles is less than zero on one section.");
        if (mv_speed < 0)
            throw new IllegalArgumentException("The speed of medium vehicles is less than zero on one section.");
        if (hgv_speed < 0)
            throw new IllegalArgumentException("The speed of heavy vehicles is less than zero on one section.");
        if (wav_speed < 0)
            throw new IllegalArgumentException("The speed of 2W(a) vehicles is less than zero on one section.");
        if (wbv_speed < 0)
            throw new IllegalArgumentException("The speed of 2W(b) vehicles is less than zero on one section.");
        if (Ts_stud < 0 || Ts_stud > 12)
            throw new IllegalArgumentException("The number of months of snow tire use is impossible for a section (<0 or >12).");
        if (Junc_type < 0 || Junc_type > 2) throw new IllegalArgumentException("Unlnown Junction type for a section.");
        this.lvPerHour = Math.max(0, lvPerHour);
        this.mvPerHour = Math.max(0, mvPerHour);
        this.hgvPerHour = Math.max(0, hgvPerHour);
        this.wavPerHour = Math.max(0, wavPerHour);
        this.wbvPerHour = Math.max(0, wbvPerHour);
        this.FreqParam = Math.max(0, FreqParam);
        this.Temperature = Temperature;
        this.roadSurface = roadSurface;
        this.tsStud = Math.max(0, Math.min(12, Ts_stud));
        this.qStudRatio = Math.max(0, Math.min(1, Pm_stud));
        this.Junc_dist = Math.max(0, Junc_dist);
        this.Junc_type = Math.max(0, Math.min(2, Junc_type));
        this.speedLv = lv_speed;
        this.speedMv = mv_speed;
        this.speedHgv = hgv_speed;
        this.speedWav = wav_speed;
        this.speedWbv = wbv_speed;
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

    public double getWay() {
        return way;
    }


    /**
     * For speeds less than 20 km/h it shall have the same sound power level as defined by the formula for vm = 20 km/h.
     *
     * @return
     */
    public double getSpeedLv() throws IOException {
        if (speedLv < 20) {
            speedLv = 20;
        }
        return speedLv;
    }

    /**
     * For speeds less than 20 km/h it shall have the same sound power level as defined by the formula for vm = 20 km/h.
     *
     * @return
     */
    public double getSpeedMv() throws IOException {
        if (speedMv < 20) {
            speedMv = 20;
        }
        return speedMv;
    }

    /**
     * For speeds less than 20 km/h it shall have the same sound power level as defined by the formula for vm = 20 km/h.
     *
     * @return
     */
    public double getSpeedHgv() throws IOException {
        if (speedHgv < 20) {
            speedHgv = 20;
        }
        return speedHgv;
    }

    /**
     * For speeds less than 20 km/h it shall have the same sound power level as defined by the formula for vm = 20 km/h.
     *
     * @return
     */
    public double getSpeedWav() throws IOException {
        if (speedWav < 20) {
            speedWav = 20;
        }
        return speedWav;
    }

    /**
     * For speeds less than 20 km/h it shall have the same sound power level as defined by the formula for vm = 20 km/h.
     *
     * @return
     */
    public double getSpeedWbv() throws IOException {
        if (speedWbv < 20) {
            speedWbv = 20;
        }
        return speedWbv;
    }

    public int getFreqParam() {
        return FreqParam;
    }

    public double getTemperature() {
        return Temperature;
    }

    public String getRoadSurface() {
        return roadSurface;
    }

    public double getTsStud() {
        return tsStud;
    }

    public double getqStudRatio() {
        return qStudRatio;
    }

    public double getJunc_dist() {
        return Junc_dist;
    }

    public int getJunc_type() {
        return Junc_type;
    }

}

