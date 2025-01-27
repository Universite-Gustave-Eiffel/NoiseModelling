package org.noise_planet.noisemodelling.jdbc.input;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Add emission information for each source in the computation scene
 * This is input data, not thread safe, never update anything here during propagation
 */
public class SceneWithEmission extends SceneWithAttenuation {


    /**
     * Set of unique power spectrum. Key is the hash of the spectrum. Power spectrum in energetic e = pow(10, dbVal / 10.0)
     */
    public Map<Integer, double[]> powerSpectrum = new HashMap<>();

    //  For each source primary key give the map between period and source power spectrum hash value
    public Map<Long, ArrayList<PeriodEmission>> wjSources = new HashMap<>();

    public SceneWithEmission(ProfileBuilder profileBuilder) {
        super(profileBuilder);
    }

    public SceneWithEmission() {
    }

    public static byte[] convertDoubleArrayToByteArray(double[] doubleArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(doubleArray.length * Double.BYTES);
        for (double value : doubleArray) {
            byteBuffer.putDouble(value);
        }
        return byteBuffer.array();
    }

    int updatePowerSpectrumSet(double[] wj) {
        int hashCode = Arrays.hashCode(wj);
        powerSpectrum.putIfAbsent(hashCode, wj);
        return hashCode;
    }

    /**
     * Link a source with a period and a spectrum
     * @param sourcePrimaryKey
     * @param period
     * @param wj
     */
    public void addSourceEmission(Long sourcePrimaryKey, String period, double[] wj) {
        int powerSpectrumHash = updatePowerSpectrumSet(wj);
        ArrayList<PeriodEmission> sourceEmissions;
        if(wjSources.containsKey(sourcePrimaryKey)) {
            sourceEmissions = wjSources.get(sourcePrimaryKey);
        } else {
            sourceEmissions = new ArrayList<>();
            wjSources.put(sourcePrimaryKey, sourceEmissions);
        }
        sourceEmissions.add(new PeriodEmission(period, powerSpectrumHash));
    }

    public static class PeriodEmission {
        final String period;
        final int emissionHash;

        public PeriodEmission(String period, int emissionHash) {
            this.period = period;
            this.emissionHash = emissionHash;
        }
    }
}
