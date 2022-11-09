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

package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossos;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossosParameters;
import org.noise_planet.noisemodelling.emission.utils.Utils;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.dbaToW;

/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
public class LDENPropagationProcessData extends CnossosPropagationData {
    public Map<String, Integer> sourceFields = null;

    // Source value in energetic  e = pow(10, dbVal / 10.0)
    public List<double[]> wjSourcesD = new ArrayList<>();
    public List<double[]> wjSourcesE = new ArrayList<>();
    public List<double[]> wjSourcesN = new ArrayList<>();

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();

    LDENConfig ldenConfig;

    public LDENPropagationProcessData(ProfileBuilder builder, LDENConfig ldenConfig) {
        super(builder, ldenConfig.propagationProcessPathDataDay.freq_lvl);
        this.ldenConfig = ldenConfig;
    }

    public void setDirectionAttributes(Map<Integer, DirectivitySphere> directionAttributes) {
        this.directionAttributes = directionAttributes;
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException, IOException {
        super.addSource(pk, geom, rs);
        double[][] res = computeLw(rs);
        if(ldenConfig.computeLDay || ldenConfig.computeLDEN) {
            wjSourcesD.add(res[0]);
        }
        if(ldenConfig.computeLEvening || ldenConfig.computeLDEN) {
            wjSourcesE.add(res[1]);
        }
        if(ldenConfig.computeLNight || ldenConfig.computeLDEN) {
            wjSourcesN.add(res[2]);
        }
    }

    @Override
    public boolean isOmnidirectional(int srcIndex) {
        return sourcesPk.size() > srcIndex && !sourceDirection.containsKey(sourcesPk.get(srcIndex));
    }

    @Override
    public double[] getSourceAttenuation(int srcIndex, double[] frequencies, double phi, double theta) {
        int directivityIdentifier = sourceDirection.get(sourcesPk.get(srcIndex));
        if(directionAttributes.containsKey(directivityIdentifier)) {
            return directionAttributes.get(directivityIdentifier).getAttenuationArray(frequencies, phi, theta);
        } else {
            // This direction identifier has not been found
            return new double[frequencies.length];
        }
    }

    @Override
    public double getSourceGs(int srcIndex){
        return sourceGs.get(sourcesPk.get(srcIndex));
    }

    /**
     * @param rs result set of source
     * @param period D or E or N
     * @param slope Gradient percentage of road from -12 % to 12 %
     * @return Emission spectrum in dB
     */
    public double[] getEmissionFromResultSet(ResultSet rs, String period, double slope) throws SQLException, IOException {
        if (sourceFields == null) {
            sourceFields = new HashMap<>();
            int fieldId = 1;
            for (String fieldName : JDBCUtilities.getColumnNames(rs.getMetaData())) {
                sourceFields.put(fieldName.toUpperCase(), fieldId++);
            }
        }
        double[] lvl = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
        // Set default values
        double tv = 0; // old format "total vehicles"
        double hv = 0; // old format "heavy vehicles"
        double lv_speed = 0;
        double mv_speed = 0;
        double hgv_speed = 0;
        double wav_speed = 0;
        double wbv_speed = 0;
        double lvPerHour = 0;
        double mvPerHour = 0;
        double hgvPerHour = 0;
        double wavPerHour = 0;
        double wbvPerHour = 0;
        double temperature = 20.0;
        String roadSurface = "NL08";
        double tsStud = 0;
        double pmStud = 0;
        double junctionDistance = 100; // no acceleration of deceleration changes with dist >= 100
        int junctionType = 2;
        int way = 3; // default value 2-way road

        // Read fields
        if(sourceFields.containsKey("LV_SPD_"+period)) {
            lv_speed = rs.getDouble(sourceFields.get("LV_SPD_"+period));
        }
        if(sourceFields.containsKey("MV_SPD_"+period)) {
            mv_speed = rs.getDouble(sourceFields.get("MV_SPD_"+period));
        }
        if(sourceFields.containsKey("HGV_SPD_"+period)) {
            hgv_speed = rs.getDouble(sourceFields.get("HGV_SPD_"+period));
        }
        if(sourceFields.containsKey("WAV_SPD_"+period)) {
            wav_speed = rs.getDouble(sourceFields.get("WAV_SPD_"+period));
        }
        if(sourceFields.containsKey("WBV_SPD_"+period)) {
            wbv_speed = rs.getDouble(sourceFields.get("WBV_SPD_"+period));
        }
        if(sourceFields.containsKey("LV_"+period)) {
            lvPerHour = rs.getDouble(sourceFields.get("LV_"+period));
        }
        if(sourceFields.containsKey("MV_"+period)) {
            mvPerHour = rs.getDouble(sourceFields.get("MV_"+period));
        }
        if(sourceFields.containsKey("HGV_"+period)) {
            hgvPerHour = rs.getDouble(sourceFields.get("HGV_"+period));
        }
        if(sourceFields.containsKey("WAV_"+period)) {
            wavPerHour = rs.getDouble(sourceFields.get("WAV_"+period));
        }
        if(sourceFields.containsKey("WBV_"+period)) {
            wbvPerHour = rs.getDouble(sourceFields.get("WBV_"+period));
        }
        if(sourceFields.containsKey("PVMT")) {
            roadSurface= rs.getString(sourceFields.get("PVMT"));
        }
        if(sourceFields.containsKey("TEMP_"+period)) {
            temperature = rs.getDouble(sourceFields.get("TEMP_"+period));
        }
        if(sourceFields.containsKey("TS_STUD")) {
            tsStud = rs.getDouble(sourceFields.get("TS_STUD"));
        }
        if(sourceFields.containsKey("PM_STUD")) {
            pmStud = rs.getDouble(sourceFields.get("PM_STUD"));
        }
        if(sourceFields.containsKey("JUNC_DIST")) {
            junctionDistance = rs.getDouble(sourceFields.get("JUNC_DIST"));
        }
        if(sourceFields.containsKey("JUNC_TYPE")) {
            junctionType = rs.getInt(sourceFields.get("JUNC_TYPE"));
        }

        if(sourceFields.containsKey("WAY")) {
            way = rs.getInt(sourceFields.get("WAY"));
        }

        if(sourceFields.containsKey("SLOPE")) {
            slope = rs.getDouble(sourceFields.get("SLOPE"));
        }else{
            way = 3;
        }


        // old fields
        if(sourceFields.containsKey("TV_"+period)) {
            tv = rs.getDouble(sourceFields.get("TV_"+period));
        }
        if(sourceFields.containsKey("HV_"+period)) {
            hv = rs.getDouble(sourceFields.get("HV_"+period));
        }
        if(sourceFields.containsKey("HV_SPD_"+period)) {
            hgv_speed = rs.getDouble(sourceFields.get("HV_SPD_"+period));
        }

        if(tv > 0) {
            lvPerHour = tv - (hv + mvPerHour + hgvPerHour + wavPerHour + wbvPerHour);
        }
        if(hv > 0) {
            hgvPerHour = hv;
        }
        // Compute emission
        int idFreq = 0;
        for (int freq : ldenConfig.propagationProcessPathDataDay.freq_lvl) {
            RoadCnossosParameters rsParametersCnossos = new RoadCnossosParameters(lv_speed, mv_speed, hgv_speed, wav_speed,
                    wbv_speed,lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, temperature,
                    roadSurface, tsStud, pmStud, junctionDistance, junctionType);
            rsParametersCnossos.setSlopePercentage(slope);
            rsParametersCnossos.setWay(way);
            rsParametersCnossos.setFileVersion(ldenConfig.coefficientVersion);
            lvl[idFreq++] = RoadCnossos.evaluate(rsParametersCnossos);
        }
        return lvl;
    }

    public double[][] computeLw(SpatialResultSet rs) throws SQLException, IOException {

        // Compute day average level
        double[] ld = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
        double[] le = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];
        double[] ln = new double[ldenConfig.propagationProcessPathDataDay.freq_lvl.size()];

        if (ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_PROBA) {
            double val = dbaToW(90.0);
            for(int idfreq = 0; idfreq < ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                ld[idfreq] = dbaToW(val);
                le[idfreq] = dbaToW(val);
                ln[idfreq] = dbaToW(val);
            }
        } else if (ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Read average 24h traffic
            if(ldenConfig.computeLDay || ldenConfig.computeLDEN) {
                for (int idfreq = 0; idfreq < ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                    ld[idfreq] = dbaToW(rs.getDouble(ldenConfig.lwFrequencyPrepend + "D" + ldenConfig.propagationProcessPathDataDay.freq_lvl.get(idfreq)));
                }
            }
            if(ldenConfig.computeLEvening || ldenConfig.computeLDEN) {
                for (int idfreq = 0; idfreq < ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                    le[idfreq] = dbaToW(rs.getDouble(ldenConfig.lwFrequencyPrepend + "E" + ldenConfig.propagationProcessPathDataDay.freq_lvl.get(idfreq)));
                }
            }
            if(ldenConfig.computeLNight || ldenConfig.computeLDEN) {
                for (int idfreq = 0; idfreq < ldenConfig.propagationProcessPathDataDay.freq_lvl.size(); idfreq++) {
                    ln[idfreq] = dbaToW(rs.getDouble(ldenConfig.lwFrequencyPrepend + "N" + ldenConfig.propagationProcessPathDataDay.freq_lvl.get(idfreq)));
                }
            }
        } else if(ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW) {
            // Extract road slope
            double slope = 0;
            try {
                Geometry g = rs.getGeometry();
                if(profileBuilder!=null && g != null && !g.isEmpty()) {
                    Coordinate[] c = g.getCoordinates();
                    if(c.length >= 2) {
                        double z0 = profileBuilder.getZ(c[0]);
                        double z1 = profileBuilder.getZ(c[1]);
                        if(!Double.isNaN(z0) && !Double.isNaN(z1)) {
                            slope = Utils.computeSlope(z0, z1, g.getLength());
                        }
                    }
                }
            } catch (SQLException ex) {
                // ignore
            }
            // Day
            ld = dbaToW(getEmissionFromResultSet(rs, "D", slope));

            // Evening
            le = dbaToW(getEmissionFromResultSet(rs, "E", slope));

            // Night
            ln = dbaToW(getEmissionFromResultSet(rs, "N", slope));

        }
        return new double[][] {ld, le, ln};
    }

    public double[] getMaximalSourcePower(int sourceId) {
        if(ldenConfig.computeLDay && sourceId < wjSourcesD.size()) {
            return wjSourcesD.get(sourceId);
        } else if(ldenConfig.computeLEvening && sourceId < wjSourcesE.size()) {
            return wjSourcesE.get(sourceId);
        } else if(ldenConfig.computeLNight && sourceId < wjSourcesN.size()) {
            return wjSourcesN.get(sourceId);
        } else {
            return new double[0];
        }
    }

    public static class OmnidirectionalDirection implements DirectivitySphere {

        @Override
        public double getAttenuation(double frequency, double phi, double theta) {
            return 0;
        }

        @Override
        public double[] getAttenuationArray(double[] frequencies, double phi, double theta) {
            return new double[frequencies.length];
        }
    }
}
