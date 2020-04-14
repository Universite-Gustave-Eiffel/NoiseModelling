package org.noise_planet.noisemodelling.wpsTools

import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData

import java.sql.SQLException


/**
 *
 */
class WpsPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new WpsPropagationProcessDataCopy(freeFieldFinder)
    }
}



/**
 * Read source database and compute the sound emission spectrum of roads sources
 * */
class WpsPropagationProcessDataCopy extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesD = new ArrayList<>()
    public List<double[]> wjSourcesE = new ArrayList<>()
    public List<double[]> wjSourcesN = new ArrayList<>()
    public List<double[]> wjSourcesDEN = new ArrayList<>()

    public Map<Long, Integer> SourcesPk = new HashMap<>()

    public String inputFormat = new String()
    int idSource = 0

    WpsPropagationProcessDataCopy(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat
    }

    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)
        SourcesPk.put(pk, idSource++)


        def res = computeLw(inputFormat, rs)
        wjSourcesD.add(res[0])
        wjSourcesE.add(res[1])
        wjSourcesN.add(res[2])
        wjSourcesDEN.add(res[3])

    }

    double[][] computeLw(String Format, SpatialResultSet rs) throws SQLException {

        // Compute day average level
        double[] ld = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] le = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] ln = new double[PropagationProcessPathData.freq_lvl.size()]
        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]

        if (Format == 'Proba') {
            double val = ComputeRays.dbaToW((BigDecimal) 90.0)
            ld = [val,val,val,val,val,val,val,val]
            le = [val,val,val,val,val,val,val,val]
            ln = [val,val,val,val,val,val,val,val]
        }

        if (Format == 'EmissionDEN') {
            // Read average 24h traffic
            ld = [ComputeRays.dbaToW(rs.getDouble('LWD63')),
                  ComputeRays.dbaToW(rs.getDouble('LWD125')),
                  ComputeRays.dbaToW(rs.getDouble('LWD250')),
                  ComputeRays.dbaToW(rs.getDouble('LWD500')),
                  ComputeRays.dbaToW(rs.getDouble('LWD1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWD8000'))]

            le = [ComputeRays.dbaToW(rs.getDouble('LWE63')),
                  ComputeRays.dbaToW(rs.getDouble('LWE125')),
                  ComputeRays.dbaToW(rs.getDouble('LWE250')),
                  ComputeRays.dbaToW(rs.getDouble('LWE500')),
                  ComputeRays.dbaToW(rs.getDouble('LWE1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWE8000'))]

            ln = [ComputeRays.dbaToW(rs.getDouble('LWN63')),
                  ComputeRays.dbaToW(rs.getDouble('LWN125')),
                  ComputeRays.dbaToW(rs.getDouble('LWN250')),
                  ComputeRays.dbaToW(rs.getDouble('LWN500')),
                  ComputeRays.dbaToW(rs.getDouble('LWN1000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN2000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN4000')),
                  ComputeRays.dbaToW(rs.getDouble('LWN8000'))]
        }
        if (Format == 'Classic') {
            // Get input traffic data
            double tvD = rs.getDouble("TV_D")
            double tvE = rs.getDouble("TV_E")
            double tvN = rs.getDouble("TV_N")

            double hvD = rs.getDouble("HV_D")
            double hvE = rs.getDouble("HV_E")
            double hvN = rs.getDouble("HV_N")

            double lvSpeedD = rs.getDouble("LV_SPD_D")
            double lvSpeedE = rs.getDouble("LV_SPD_E")
            double lvSpeedN = rs.getDouble("LV_SPD_N")

            double hvSpeedD = rs.getDouble("HV_SPD_D")
            double hvSpeedE = rs.getDouble("HV_SPD_E")
            double hvSpeedN = rs.getDouble("HV_SPD_N")

            String pavement = rs.getString("PVMT")

            // this options can be activated if needed
            double Temperature = 20.0d
            double Ts_stud = 0
            double Pm_stud = 0
            double Junc_dist = 300
            int Junc_type = 0

            // Day
            int idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                        lvSpeedD, Math.max(0, tvD - hvD), 0, hvD, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }

            // Evening
            idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                        lvSpeedE, Math.max(0, tvE - hvE), 0, hvE, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }

            // Night
            idFreq = 0
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                        lvSpeedN, Math.max(0, tvN - hvN), 0, hvN, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type)
                ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
            }


        }

        if (Format == "AADF") {
            String AAFD_FIELD_NAME = "AADF"

            // Annual Average Daily Flow (AADF) estimates
            String ROAD_CATEGORY_FIELD_NAME = "CLAS_ADM"
            def lv_hourly_distribution = [0.56, 0.3, 0.21, 0.26, 0.69, 1.8, 4.29, 7.56, 7.09, 5.5, 4.96, 5.04,
                                          5.8, 6.08, 6.23, 6.67, 7.84, 8.01, 7.12, 5.44, 3.45, 2.26, 1.72, 1.12];
            def hv_hourly_distribution = [1.01, 0.97, 1.06, 1.39, 2.05, 3.18, 4.77, 6.33, 6.72, 7.32, 7.37, 7.4,
                                          6.16, 6.22, 6.84, 6.74, 6.23, 4.88, 3.79, 3.05, 2.36, 1.76, 1.34, 1.07];

            int LDAY_START_HOUR = 6
            int LDAY_STOP_HOUR = 18
            int LEVENING_STOP_HOUR = 22
            int[] nightHours = [22, 23, 0, 1, 2, 3, 4, 5]
            double HV_PERCENTAGE = 0.1

            int idSource = 0

            idSource = idSource + 1
            // Read average 24h traffic
            double tmja = rs.getDouble(AAFD_FIELD_NAME)

            //130 km/h 1:Autoroute
            //80 km/h  2:Nationale
            //50 km/h  3:Départementale
            //50 km/h  4:Voirie CUN
            //50 km/h  5:Inconnu
            //50 km/h  6:Privée
            //50 km/h  7:Communale
            int road_cat = rs.getInt(ROAD_CATEGORY_FIELD_NAME)

            int roadType;
            if (road_cat == 1) {
                roadType = 10;
            } else {
                if (road_cat == 2) {
                    roadType = 42;
                } else {
                    roadType = 62;
                }
            }
            double speed_lv = 50;
            if (road_cat == 1) {
                speed_lv = 120;
            } else {
                if (road_cat == 2) {
                    speed_lv = 80;
                }
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
                lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0);
                hgvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0);
                int idFreq = 0;
                for (int freq : PropagationProcessPathData.freq_lvl) {
                    RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                            speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                            roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                    rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType);
                    ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
                }
            }
            // Average
            for (int i = 0; i < ld.length; i++) {
                ld[i] = ld[i] / (LDAY_STOP_HOUR - LDAY_START_HOUR);
            }

            // Evening
            for (int h = LDAY_STOP_HOUR; h < LEVENING_STOP_HOUR; h++) {
                lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
                mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
                int idFreq = 0
                for (int freq : PropagationProcessPathData.freq_lvl) {
                    RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                            speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                            roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                    rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                    le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
                }
            }

            for (int i = 0; i < le.size(); i++) {
                le[i] = (le[i] / (LEVENING_STOP_HOUR - LDAY_STOP_HOUR))
            }

            // Night
            for (int h : nightHours) {
                lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0)
                mvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0)
                int idFreq = 0
                for (int freq : PropagationProcessPathData.freq_lvl) {
                    RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                            speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                            roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                    rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType)
                    ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos)
                }
            }
            for (int i = 0; i < ln.size(); i++) {
                ln[i] = (ln[i] / nightHours.length)
            }
        }

        int idFreq = 0
        // Combine day evening night sound levels
        for (int freq : PropagationProcessPathData.freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }

        return [ld, le, ln, lden]
    }


    @Override
    double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }
}