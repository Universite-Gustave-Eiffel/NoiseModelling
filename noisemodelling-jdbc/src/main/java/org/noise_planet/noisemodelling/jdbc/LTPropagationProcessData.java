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
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.emission.DirectionAttributes;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

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
public class LTPropagationProcessData extends CnossosPropagationData {
    public Map<String, Integer> sourceFields = null;

    // Source value in energetic  e = pow(10, dbVal / 10.0)
    public List<double[]> wjSources = new ArrayList<>();

    public HashMap<String, List<double[]>> wjSourcesT ;


    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectionAttributes> directionAttributes = new HashMap<>();

    LTConfig ltConfig;

    public LTPropagationProcessData(ProfileBuilder builder, LTConfig ltConfig, List<Integer> freq_lvl) {
        super(builder, freq_lvl);
        this.ltConfig = ltConfig;
    }

    public void setDirectionAttributes(Map<Integer, DirectionAttributes> directionAttributes) {
        this.directionAttributes = directionAttributes;
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException, IOException {
        super.addSource(pk, geom, rs);
        double[][] res = computeLw(rs);
        wjSources.add(res[0]);
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
     * @return Emission spectrum in dB
     */
    public double[] getEmissionFromResultSet(ResultSet rs, String period) throws SQLException, IOException {
        Map.Entry<String, PropagationProcessPathData> iterator = ltConfig.propagationProcessPathDataT.entrySet().iterator().next();
        List<Integer> freq_lvl = iterator.getValue().freq_lvl;
        if (sourceFields == null) {
            sourceFields = new HashMap<>();
            int fieldId = 1;
            for (String fieldName : JDBCUtilities.getColumnNames(rs.getMetaData())) {
                sourceFields.put(fieldName.toUpperCase(), fieldId++);
            }
        }
        double[] lvl = new double[freq_lvl.size()];
        // Set default values
        String timestep = ""; // old format "total vehicles"

        // Read fields
        if(sourceFields.containsKey("IT")) {
            timestep = rs.getString(sourceFields.get("IT").toString());
        }

        // Compute emission
        int idFreq = 0;
        for (int freq : freq_lvl) {
            lvl[idFreq++] = rs.getDouble(sourceFields.get("HZ" + freq));
        }
        return lvl;
    }

    public double[][] computeLw(SpatialResultSet rs) throws SQLException, IOException {

        // Compute day average level
        double[] lt = new double[freq_lvl.size()];

        for (int idfreq = 0; idfreq < freq_lvl.size(); idfreq++) {
            lt[idfreq] = dbaToW(rs.getDouble(ltConfig.lwFrequencyPrepend +  freq_lvl.get(idfreq)));
        }

        return new double[][] {lt};
    }

    public double[] getMaximalSourcePower(int sourceId) {
        if( sourceId < wjSources.size()) {
            return wjSources.get(sourceId);
        } else {
            return new double[0];
        }
    }

    public static class OmnidirectionalDirection implements DirectionAttributes {

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
