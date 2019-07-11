package org.noise_planet.nmtutorial01;

import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos;
import org.noise_planet.noisemodelling.emission.RSParametersCnossos;
import org.noise_planet.noisemodelling.propagation.ComputeRays;
import org.noise_planet.noisemodelling.propagation.FastObstructionTest;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read source database and compute the sound emission spectrum of roads sources
 */

class TrafficPropagationProcessData extends PropagationProcessData {
    // Lden values
    protected List<double[]> wjSourcesD = new ArrayList<>();
    private List<double[]> wjSourcesE = new ArrayList<>();
    private List<double[]> wjSourcesN = new ArrayList<>();

    private String AAFD_FIELD_NAME = "AADF"; // Annual Average Daily Flow (AADF) estimates
    private String ROAD_CATEGORY_FIELD_NAME = "CLAS_ADM";
    private
    static double[] lv_hourly_distribution = new double[]{ 0.56, 0.3, 0.21, 0.26, 0.69, 1.8, 4.29, 7.56, 7.09, 5.5, 4.96, 5.04,
            5.8, 6.08, 6.23, 6.67, 7.84, 8.01, 7.12, 5.44, 3.45, 2.26, 1.72, 1.12};
    private
    static double[] hv_hourly_distribution = new double[] {1.01, 0.97, 1.06, 1.39, 2.05, 3.18, 4.77, 6.33, 6.72, 7.32, 7.37, 7.4,
            6.16, 6.22, 6.84, 6.74, 6.23, 4.88, 3.79, 3.05, 2.36, 1.76, 1.34, 1.07};
    private static final int LDAY_START_HOUR = 6;
    private static final int LDAY_STOP_HOUR = 18;
    private static final double HV_PERCENTAGE = 0.1;

    public TrafficPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder);
    }


    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs);

        // Read average 24h traffic
        double tmja = rs.getDouble(AAFD_FIELD_NAME);

        //130 km/h 1:Autoroute
        //80 km/h  2:Nationale
        //50 km/h  3:Départementale
        //50 km/h  4:Voirie CUN
        //50 km/h  5:Inconnu
        //50 km/h  6:Privée
        //50 km/h  7:Communale
        int road_cat = rs.getInt(ROAD_CATEGORY_FIELD_NAME);

        int roadType;
        if(road_cat == 1) {
            roadType = 10;
        } else if(road_cat == 2) {
            roadType = 42;
        } else {
            roadType = 62;
        }
        double speed_lv = 50;
        if(road_cat == 1) {
            speed_lv = 120;
        } else if(road_cat == 2) {
            speed_lv = 80;
        }

        /**
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
        // Compute day average level
        double[] ld = new double[PropagationProcessPathData.freq_lvl.size()];
        double lvPerHour = 0;
        double mvPerHour = 0;
        double hgvPerHour = 0;
        double wavPerHour = 0;
        double wbvPerHour = 0;
        double Temperature = 20.0d;
        String roadSurface = "FR_R2";
        double Ts_stud = 0.5;
        double Pm_stud = 4;
        double Junc_dist = 0;
        int Junc_type = 0;
        double slopePercentage = 0;
        double speedLv = speed_lv;
        double speedMv = speed_lv;
        double speedHgv = speed_lv;
        double speedWav = speed_lv;
        double speedWbv = speed_lv;
        for (int h = LDAY_START_HOUR; h < LDAY_STOP_HOUR; h++) {
            lvPerHour = tmja * (1- HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0);
            hgvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0);
            int idFreq = 0;
            for(int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                        speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                        roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType);
                ld[idFreq++] += ComputeRays.dbaToW(EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos));
            }
        }
        // Average
        for(int i=0; i<ld.length; i++) {
            ld[i] = ld[i] / (LDAY_STOP_HOUR - LDAY_START_HOUR);
        }
        wjSourcesD.add(ld);

    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId);
    }
}
