/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.jdbc.input;

import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumArray;

/**
 * Add emission information for each source in the computation scene
 * This is input data, not thread safe, never update anything here during propagation
 */
public class SceneWithEmission extends SceneWithAttenuation {
    /** Old style DEN columns traffic period  */
    Map<String, Integer> sourceEmissionFieldsCache = new HashMap<>();

    //  For each source primary key give the map between period and source power spectrum values
    public Map<Long, ArrayList<PeriodEmission>> wjSources = new HashMap<>();

    public SceneDatabaseInputSettings sceneDatabaseInputSettings = new SceneDatabaseInputSettings();

    public SceneWithEmission(ProfileBuilder profileBuilder, SceneDatabaseInputSettings sceneDatabaseInputSettings) {
        super(profileBuilder);
        this.sceneDatabaseInputSettings = sceneDatabaseInputSettings;
    }

    public SceneWithEmission(ProfileBuilder profileBuilder) {
        super(profileBuilder);
    }

    public SceneWithEmission() {
    }

    public void processTrafficFlowDEN(Long pk, SpatialResultSet rs) throws SQLException {
        // Source table PK, GEOM, LV_D, LV_E, LV_N ...
        double[][] lw = EmissionTableGenerator.computeLw(rs, sceneDatabaseInputSettings.coefficientVersion, sourceFieldNames);
        // Will generate D E N emission
        for (EmissionTableGenerator.STANDARD_PERIOD period : EmissionTableGenerator.STANDARD_PERIOD.values()) {
            addSourceEmission(pk, EmissionTableGenerator.STANDARD_PERIOD_VALUE[period.ordinal()], lw[period.ordinal()]);
        }
    }

    /**
     * @param pk Source primary key
     * @param rs Emission source table IDSOURCE, PERIOD, LV, HV ..
     * @throws SQLException
     */
    public void processTrafficFlow(Long pk, ResultSet rs) throws SQLException {
        String period = rs.getString("PERIOD");
        // Use geometry as default slope (if field slope is not provided
        double defaultSlope = 0;
        if(!sourceEmissionFieldsCache.containsKey("SLOPE")) {
            int sourceIndex = sourcesPk.indexOf(pk);
            if(sourceIndex >= 0) {
                defaultSlope = EmissionTableGenerator.getSlope(sourceGeometries.get(sourceIndex));
            }
        }
        double[] lw = AcousticIndicatorsFunctions.dBToW(
                EmissionTableGenerator.getEmissionFromTrafficTable(rs, "",
                        defaultSlope,
                        sceneDatabaseInputSettings.coefficientVersion, sourceEmissionFieldsCache));
        addSourceEmission(pk, period, lw);
    }

    /**
     * @param pk Source primary key
     * @param rs Emission source table IDSOURCE, PERIOD, LV, HV ..
     * @throws SQLException
     */
    public void processEmission(Long pk, ResultSet rs) throws SQLException {
        double[] lw = new double[profileBuilder.frequencyArray.size()];
        List<Integer> frequencyArray = profileBuilder.frequencyArray;
        for (int i = 0, frequencyArraySize = frequencyArray.size(); i < frequencyArraySize; i++) {
            Integer frequency = frequencyArray.get(i);
            lw[i] = AcousticIndicatorsFunctions.dBToW(rs.getDouble(sceneDatabaseInputSettings.frequencyFieldPrepend +frequency));
        }
        String period = rs.getString("PERIOD");
        addSourceEmission(pk, period, lw);
    }

    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs);
        switch (Objects.requireNonNull(sceneDatabaseInputSettings.inputMode)) {
            case INPUT_MODE_TRAFFIC_FLOW_DEN:
                processTrafficFlowDEN(pk, rs);
                break;
            case INPUT_MODE_LW_DEN:
                processEmissionDEN(pk, rs);
                break;
        }
    }

    private void processEmissionDEN(Long pk, SpatialResultSet rs) throws SQLException {
        List<Integer> frequencyArray = profileBuilder.frequencyArray;
        for (EmissionTableGenerator.STANDARD_PERIOD period : EmissionTableGenerator.STANDARD_PERIOD.values()) {
            double[] lw = new double[profileBuilder.frequencyArray.size()];
            boolean missingField = false;
            String periodFieldName = EmissionTableGenerator.STANDARD_PERIOD_VALUE[period.ordinal()];
            for (int i = 0, frequencyArraySize = frequencyArray.size(); i < frequencyArraySize; i++) {
                Integer frequency = frequencyArray.get(i);
                final String tableFieldName = sceneDatabaseInputSettings.frequencyFieldPrepend + periodFieldName + frequency;
                if(sourceFieldNames.containsKey(tableFieldName)) {
                    lw[i] = AcousticIndicatorsFunctions.dBToW(
                            rs.getDouble(tableFieldName));
                } else {
                    missingField = true;
                    break;
                }
            }
            if(!missingField) {
                addSourceEmission(pk, periodFieldName, lw);
            }
        }
    }

    public void addSourceEmission(Long pk, ResultSet rs) throws SQLException {
        switch (sceneDatabaseInputSettings.inputMode) {
            case INPUT_MODE_TRAFFIC_FLOW:
                processTrafficFlow(pk, rs);
                break;
            case INPUT_MODE_LW:
                processEmission(pk, rs);
                break;
        }
    }

    /**
     * Link a source with a period and a spectrum
     * @param sourcePrimaryKey
     * @param period
     * @param wj
     */
    public void addSourceEmission(Long sourcePrimaryKey, String period, double[] wj) {
        ArrayList<PeriodEmission> sourceEmissions;
        if(wjSources.containsKey(sourcePrimaryKey)) {
            sourceEmissions = wjSources.get(sourcePrimaryKey);
        } else {
            sourceEmissions = new ArrayList<>();
            wjSources.put(sourcePrimaryKey, sourceEmissions);
        }
        sourceEmissions.add(new PeriodEmission(period, wj));
        if(!period.isEmpty()) {
            periodSet.add(period);
        }
    }

    @Override
    public void clearSources() {
        super.clearSources();
        sourceEmissionFieldsCache.clear();
        wjSources.clear();
    }

    public static class PeriodEmission {
        public final String period;
        public final double[] emission;

        public PeriodEmission(String period, double[] emission) {
            this.period = period;
            this.emission = emission;
        }
    }

}
