package org.noise_planet.noisemodelling.wpsTools

import groovy.sql.Sql
import org.cts.crs.CRSException
import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.emission.RSParametersDynamic
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.KMLDocument
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import javax.xml.stream.XMLStreamException
import java.sql.SQLException

class GeneralTools {

    /**
     * Spartan ProgressBar
     * @param newVal
     * @param currentVal
     * @return
     */
    static int ProgressBar(int newVal, int currentVal)
    {
        if(newVal != currentVal) {
            currentVal = newVal
            System.print( 10*currentVal + '% ... ')
        }
        return currentVal
    }


/**
 *
 * @param array1
 * @param array2
 * @return
 */
    static double[] sumLinearArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array")
        } else {
            double[] sum = new double[array1.length]

            for (int i = 0; i < array1.length; ++i) {
                sum[i] = array1[i] + array2[i]
            }

            return sum
        }
    }


    /**
     * Export scene to kml format
     * @param name
     * @param manager
     * @param result
     * @return
     * @throws IOException
     */
    def static exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
        try {
            FileOutputStream outData = new FileOutputStream(name)
            KMLDocument kmlDocument = new KMLDocument(outData)
            kmlDocument.setInputCRS("EPSG:2154")
            kmlDocument.writeHeader()
            if (manager != null) {
                kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices())
            }
            if (result != null) {
                kmlDocument.writeRays(result.getPropagationPaths())
            }
            if (manager != null && manager.isHasBuildingWithHeight()) {
                kmlDocument.writeBuildings(manager)
            }
            kmlDocument.writeFooter()
        } catch (XMLStreamException | CRSException ex) {
            throw new IOException(ex)
        }
    }


    /**
    *
    * @param db
    * @return
    */
    static double[] DBToDBA(double[] db) {
        double[] dbA = [-26.2, -16.1, -8.6, -3.2, 0, 1.2, 1.0, -1.1]
        for (int i = 0; i < db.length; ++i) {
            db[i] = db[i] + dbA[i]
        }
        return db

    }

    /**
     * Sum two Array "octave band by octave band"
     * @param array1
     * @param array2
     * @return sum of to array
     */
    double[] sumArraySR(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array")
        } else {
            double[] sum = new double[array1.length]

            for (int i = 0; i < array1.length; ++i) {
                sum[i] = (array1[i]) + (array2[i])
            }

            return sum
        }
    }

}



/**
 * Read source database and compute the sound emission spectrum of roads sources*/
class TrafficPropagationProcessData extends PropagationProcessData {
    // Lden values
    public List<double[]> wjSourcesD = new ArrayList<>();
    public Map<Long, Integer> SourcesPk = new HashMap<>();

    private String AAFD_FIELD_NAME = "AADF";
    // Annual Average Daily Flow (AADF) estimates
    private String ROAD_CATEGORY_FIELD_NAME = "CLAS_ADM";
    def lv_hourly_distribution = [0.56, 0.3, 0.21, 0.26, 0.69, 1.8, 4.29, 7.56, 7.09, 5.5, 4.96, 5.04,
                                  5.8, 6.08, 6.23, 6.67, 7.84, 8.01, 7.12, 5.44, 3.45, 2.26, 1.72, 1.12];
    def hv_hourly_distribution = [1.01, 0.97, 1.06, 1.39, 2.05, 3.18, 4.77, 6.33, 6.72, 7.32, 7.37, 7.4,
                                  6.16, 6.22, 6.84, 6.74, 6.23, 4.88, 3.79, 3.05, 2.36, 1.76, 1.34, 1.07];
    private static final int LDAY_START_HOUR = 6;
    private static final int LDAY_STOP_HOUR = 18;
    private static final double HV_PERCENTAGE = 0.1;

    TrafficPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder);
    }

    int idSource = 0

    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs);
        SourcesPk.put(pk, idSource++)

        // Read average 24h traffic
        double tmja = rs.getDouble(AAFD_FIELD_NAME);

        int road_cat = rs.getInt(ROAD_CATEGORY_FIELD_NAME);

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
            lvPerHour = tmja * (1 - HV_PERCENTAGE) * (lv_hourly_distribution[h] / 100.0);
            hgvPerHour = tmja * HV_PERCENTAGE * (hv_hourly_distribution[h] / 100.0);
            int idFreq = 0;
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(speedLv, speedMv, speedHgv, speedWav,
                        speedWbv, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, Temperature,
                        roadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                rsParametersCnossos.setSpeedFromRoadCaracteristics(speed_lv, speed_lv, false, speed_lv, roadType);
                ld[idFreq++] += ComputeRays.dbaToW(EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos));
            }
        }
        // Average
        for (int i = 0; i < ld.length; i++) {
            ld[i] = ld[i] / (LDAY_STOP_HOUR - LDAY_START_HOUR);
        }
        wjSourcesD.add(ld);

    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId);
    }
}




/**
 * Class to read sound sources
 */
class TrafficPropagationProcessDataDEN extends PropagationProcessData {

    public List<double[]> wjSourcesDEN = new ArrayList<>()
    public Map<Long, Integer> SourcesPk = new HashMap<>()


    TrafficPropagationProcessDataDEN(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    int idSource = 0

    /**
     * Read Sound sources table and add to wjSourcesDEN variable
     * @param pk
     * @param geom
     * @param rs
     * @throws SQLException
     */
    @Override
    void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs)
        SourcesPk.put(pk, idSource++)

        // Read average 24h traffic
        double[] ld = [ComputeRays.dbaToW(rs.getDouble('LWD63')),
                       ComputeRays.dbaToW(rs.getDouble('LWD125')),
                       ComputeRays.dbaToW(rs.getDouble('LWD250')),
                       ComputeRays.dbaToW(rs.getDouble('LWD500')),
                       ComputeRays.dbaToW(rs.getDouble('LWD1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWD8000'))]

        double[] le = [ComputeRays.dbaToW(rs.getDouble('LWE63')),
                       ComputeRays.dbaToW(rs.getDouble('LWE125')),
                       ComputeRays.dbaToW(rs.getDouble('LWE250')),
                       ComputeRays.dbaToW(rs.getDouble('LWE500')),
                       ComputeRays.dbaToW(rs.getDouble('LWE1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWE8000'))]

        double[] ln = [ComputeRays.dbaToW(rs.getDouble('LWN63')),
                       ComputeRays.dbaToW(rs.getDouble('LWN125')),
                       ComputeRays.dbaToW(rs.getDouble('LWN250')),
                       ComputeRays.dbaToW(rs.getDouble('LWN500')),
                       ComputeRays.dbaToW(rs.getDouble('LWN1000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN2000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN4000')),
                       ComputeRays.dbaToW(rs.getDouble('LWN8000'))]

        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()]

        int idFreq = 0
        // Combine day evening night sound levels
        for (int freq : PropagationProcessPathData.freq_lvl) {
            lden[idFreq++] = (12 * ld[idFreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idFreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idFreq]) + 10)) / 24.0
        }
        wjSourcesDEN.add(lden)
    }

    @Override
    double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesDEN.get(sourceId)
    }
}

/**
 *
 */
class TrafficPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessData(freeFieldFinder);
    }
}

/**
 *
 */
class TrafficPropagationProcessDataDENFactory implements PointNoiseMap.PropagationProcessDataFactory {
    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new TrafficPropagationProcessDataDEN(freeFieldFinder)
    }
}




/**
 *
 */
class ProbabilisticProcessData {

    Map<Integer, Double> SPEED_LV = new HashMap<>()
    Map<Integer, Double> SPEED_HV = new HashMap<>()
    Map<Integer, Double> LV = new HashMap<>()
    Map<Integer, Double> HV = new HashMap<>()

    double[] getCarsLevel(int idSource) throws SQLException {
        double[] res_d = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        double[] res_LV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        double[] res_HV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
        // memes valeurs d e et n


        def random = Math.random()
        if (random < LV.get(idSource)) {
            int kk = 0
            for (f in list) {

                double speed = SPEED_LV.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                int RoadSurface = 0
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                int veh_type = 1
                int acc_type = 1
                double LwStd = 1
                int VehId = 10

                RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
                rsParameters.setSlopePercentage(0)

                res_LV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }

        }
        if (random < HV.get(idSource)) {
            int kk = 0
            for (f in list) {
                double speed = SPEED_HV.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                int RoadSurface = 0
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                int veh_type = 3
                int acc_type = 1
                double LwStd = 1
                int VehId = 10

                RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
                rsParameters.setSlopePercentage(0)

                res_HV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }
        }
        int kk = 0
        for (f in list) {
            res_d[kk] = 10 * Math.log10(
                    (1.0 / 2.0) *
                            (Math.pow(10, (10 * Math.log10(Math.pow(10, res_LV[kk] / 10))) / 10)
                                    + Math.pow(10, (10 * Math.log10(Math.pow(10, res_HV[kk] / 10))) / 10)
                            )
            )
            kk++
        }


        return res_d
    }

    void setProbaTable(String tablename, Sql sql) {
        //////////////////////
        // Import file text
        //////////////////////
        int i_read = 0;

        // Remplissage des variables avec le contenu du fichier plan d'exp
        sql.eachRow('SELECT PK,  SPEED, HV,LV FROM ' + tablename + ';') { row ->
            int pk = (int) row[0]

            SPEED_HV.put(pk, (double) row[1])
            SPEED_LV.put(pk, (double) row[1])
            HV.put(pk, (double) row[2])
            LV.put(pk, (double) row[3])

        }


    }

}


/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
class ProbabilisticPropagationProcessData extends PropagationProcessData {

    protected List<double[]> wjSourcesD = new ArrayList<>()

    public ProbabilisticPropagationProcessData(FastObstructionTest freeFieldFinder) {
        super(freeFieldFinder)
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {

        super.addSource(pk, geom, rs)

        double db_m63 = 90
        double db_m125 = 90
        double db_m250 = 90
        double db_m500 = 90
        double db_m1000 = 90
        double db_m2000 = 90
        double db_m4000 = 90
        double db_m8000 = 90

        double[] res_d = [db_m63, db_m125, db_m250, db_m500, db_m1000, db_m2000, db_m4000, db_m8000]
        wjSourcesD.add(ComputeRays.dbaToW(res_d))
    }

    @Override
    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId)
    }


}

/**
 *
 */
class ProbabilisticPropagationProcessDataFactory implements PointNoiseMap.PropagationProcessDataFactory {

    @Override
    PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new ProbabilisticPropagationProcessData(freeFieldFinder)
    }
}