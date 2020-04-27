/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.emission.jdbc;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
public class LDENPropagationProcessData extends PropagationProcessData {
    String lwFrequencyPrepend = "LW";

    // Lden values
    public List<double[]> wjSourcesD = new ArrayList<>();
    public List<double[]> wjSourcesE = new ArrayList<>();
    public List<double[]> wjSourcesN = new ArrayList<>();
    public List<double[]> wjSourcesDEN = new ArrayList<>();

    public Map<Long, Integer> SourcesPk = new HashMap<>();

    int idSource = 0;

    LDENConfig ldenConfig;

    public LDENPropagationProcessData(FastObstructionTest freeFieldFinder, LDENConfig ldenConfig) {
        super(freeFieldFinder);
        this.ldenConfig = ldenConfig;
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs);
        SourcesPk.put(pk, idSource++);


        double[][] res = computeLw(rs);
        wjSourcesD.add(res[0]);
        wjSourcesE.add(res[1]);
        wjSourcesN.add(res[2]);
        wjSourcesDEN.add(res[3]);

    }

    double[][] computeLw(SpatialResultSet rs) throws SQLException {

        // Compute day average level
        double[] ld = new double[PropagationProcessPathData.freq_lvl.size()];
        double[] le = new double[PropagationProcessPathData.freq_lvl.size()];
        double[] ln = new double[PropagationProcessPathData.freq_lvl.size()];
        double[] lden = new double[PropagationProcessPathData.freq_lvl.size()];

        if (ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_PROBA) {
            double val = ComputeRays.dbaToW(90.0);
            for(int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                ld[idfreq] = val;
                le[idfreq] = val;
                ln[idfreq] = val;
            }
        } else if (ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Read average 24h traffic
            for (int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                ld[idfreq] = ComputeRays.dbaToW(rs.getDouble(lwFrequencyPrepend + "D" +
                        PropagationProcessPathData.freq_lvl.get(idfreq)));
            }
            for (int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                le[idfreq] = ComputeRays.dbaToW(rs.getDouble(lwFrequencyPrepend + "E" +
                        PropagationProcessPathData.freq_lvl.get(idfreq)));
            }
            for (int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                ln[idfreq] = ComputeRays.dbaToW(rs.getDouble(lwFrequencyPrepend + "N" +
                        PropagationProcessPathData.freq_lvl.get(idfreq)));
            }
        } else if(ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW) {
            // Get input traffic data
            double tvD = rs.getDouble("TV_D");
            double tvE = rs.getDouble("TV_E");
            double tvN = rs.getDouble("TV_N");

            double hvD = rs.getDouble("HV_D");
            double hvE = rs.getDouble("HV_E");
            double hvN = rs.getDouble("HV_N");

            double lvSpeedD = rs.getDouble("LV_SPD_D");
            double lvSpeedE = rs.getDouble("LV_SPD_E");
            double lvSpeedN = rs.getDouble("LV_SPD_N");

            double hvSpeedD = rs.getDouble("HV_SPD_D");
            double hvSpeedE = rs.getDouble("HV_SPD_E");
            double hvSpeedN = rs.getDouble("HV_SPD_N");

            String pavement = rs.getString("PVMT");

            // this options can be activated if needed
            double Temperature = 20.0d;
            double Ts_stud = 0;
            double Pm_stud = 0;
            double Junc_dist = 300;
            int Junc_type = 0;

            // Day
            int idFreq = 0;
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedD, hvSpeedD, hvSpeedD, lvSpeedD,
                        lvSpeedD, Math.max(0, tvD - hvD), 0, hvD, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                ld[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos);
            }

            // Evening
            idFreq = 0;
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedE, hvSpeedE, hvSpeedE, lvSpeedE,
                        lvSpeedE, Math.max(0, tvE - hvE), 0, hvE, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                le[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos);
            }

            // Night
            idFreq = 0;
            for (int freq : PropagationProcessPathData.freq_lvl) {
                RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(lvSpeedN, hvSpeedN, hvSpeedN, lvSpeedN,
                        lvSpeedN, Math.max(0, tvN - hvN), 0, hvN, 0, 0, freq, Temperature,
                        pavement, Ts_stud, Pm_stud, Junc_dist, Junc_type);
                ln[idFreq++] += EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos);
            }


        }

        // Combine day evening night sound levels
        for (int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
            lden[idfreq] = (12 * ld[idfreq] + 4 * ComputeRays.dbaToW(ComputeRays.wToDba(le[idfreq]) + 5) + 8 * ComputeRays.dbaToW(ComputeRays.wToDba(ln[idfreq]) + 10)) / 24.0;
        }

        return new double[][] {ld, le, ln, lden};
    }

    public double[] getMaximalSourcePower(int sourceId) {
        return wjSourcesD.get(sourceId);
    }
}
