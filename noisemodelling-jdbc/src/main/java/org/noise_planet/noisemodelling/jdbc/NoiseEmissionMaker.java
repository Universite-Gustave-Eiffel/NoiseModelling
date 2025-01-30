/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.apache.commons.codec.digest.DigestUtils;
import org.h2.security.SHA256;
import org.h2gis.functions.spatial.convert.ST_Force3D;
import org.h2gis.functions.spatial.edit.ST_UpdateZ;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.railway.RailWayParameters;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossos;
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossosParameters;
import org.noise_planet.noisemodelling.emission.utils.Utils;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWGeom;
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.dbaToW;

/**
 * Read source database and compute the sound emission spectrum of roads sources
 */
public class NoiseEmissionMaker {

    public static final double DAY_RATIO = 12. / 24.;
    public static final double EVENING_RATIO = 4. / 24. * dbaToW(5.0);
    public static final double NIGHT_RATIO = 8. / 24. * dbaToW(10.0);

    /**
     * Set of unique power spectrum. Key is the hash of the spectrum. Power spectrum in energetic e = pow(10, dbVal / 10.0)
     */
    public Map<String, double[]> powerSpectrum = new HashMap<>();

    //  For each source index give the map between period and source power spectrum hash value
    public List<HashMap<String, String>> wjSources = new ArrayList<>();

    public NoiseMapParameters noiseMapParameters;

    /**
     *  Create NoiseEmissionMaker constructor
     * @param builder
     * @param noiseMapParameters
     */
    public NoiseEmissionMaker(ProfileBuilder builder, NoiseMapParameters noiseMapParameters) {
        super(builder);
        this.noiseMapParameters = noiseMapParameters;
    }

    public static byte[] convertDoubleArrayToByteArray(double[] doubleArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(doubleArray.length * Double.BYTES);
        for (double value : doubleArray) {
            byteBuffer.putDouble(value);
        }
        return byteBuffer.array();
    }


    String updatePowerSpectrumSet(double[] wj) {
        String spectrumHash = new String(DigestUtils.getSha256Digest().digest(convertDoubleArrayToByteArray(wj)),
                StandardCharsets.UTF_8);
        powerSpectrum.putIfAbsent(spectrumHash, wj);
        return spectrumHash;
    }

    /**
     * Adds a noise source with its properties to the noise map.
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param rs Additional attributes fetched from database
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        super.addSource(pk, geom, rs);
        double[][] res = computeLw(rs);
        if(noiseMapParameters.computeLDay || noiseMapParameters.computeLDEN) {
            wjSourcesD.add(res[0]);
        }
        if(noiseMapParameters.computeLEvening || noiseMapParameters.computeLDEN) {
            wjSourcesE.add(res[1]);
        }
        if(noiseMapParameters.computeLNight || noiseMapParameters.computeLDEN) {
            wjSourcesN.add(res[2]);
        }
    }

    /**
     * Retrieves the ground speed of the noise source at the specified index.
     * @param srcIndex
     * @return the ground speed of the noise source at the specified index.
     */
    @Override
    public double getSourceGs(int srcIndex){
        return sourceGs.get(sourcesPk.get(srcIndex));
    }


    /**
     * Computes the sound levels (Lw) for different periods based on the provided spatial result set.
     * @param rs
     * @return a two-dimensional array containing the sound levels (Ld, Le, Ln) for each frequency level.
     * @throws SQLException
     * @throws IOException
     */
    public double[][] computeLw(SpatialResultSet rs) throws SQLException, IOException {

        // Compute day average level
        double[] ld = new double[noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size()];
        double[] le = new double[noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size()];
        double[] ln = new double[noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size()];

        if (noiseMapParameters.input_mode == NoiseMapParameters.INPUT_MODE.INPUT_MODE_PROBA) {
            double val = dbaToW(90.0);
            for(int idfreq = 0; idfreq < noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size(); idfreq++) {
                ld[idfreq] = dbaToW(val);
                le[idfreq] = dbaToW(val);
                ln[idfreq] = dbaToW(val);
            }
        } else if (noiseMapParameters.input_mode == NoiseMapParameters.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Read average 24h traffic
            if(noiseMapParameters.computeLDay || noiseMapParameters.computeLDEN) {
                for (int idfreq = 0; idfreq < noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size(); idfreq++) {
                    ld[idfreq] = dbaToW(rs.getDouble(noiseMapParameters.lwFrequencyPrepend + "D" + noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.get(idfreq)));
                }
            }
            if(noiseMapParameters.computeLEvening || noiseMapParameters.computeLDEN) {
                for (int idfreq = 0; idfreq < noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size(); idfreq++) {
                    le[idfreq] = dbaToW(rs.getDouble(noiseMapParameters.lwFrequencyPrepend + "E" + noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.get(idfreq)));
                }
            }
            if(noiseMapParameters.computeLNight || noiseMapParameters.computeLDEN) {
                for (int idfreq = 0; idfreq < noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.size(); idfreq++) {
                    ln[idfreq] = dbaToW(rs.getDouble(noiseMapParameters.lwFrequencyPrepend + "N" + noiseMapParameters.attenuationCnossosParametersDay.freq_lvl.get(idfreq)));
                }
            }
        } else if(noiseMapParameters.input_mode == NoiseMapParameters.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW) {
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

    /**
     * Retrieves the maximal source power for the specified source ID.
     * @param sourceId
     * @return an array containing the maximal source power values for the specified source ID.
     */
    public double[] getMaximalSourcePower(int sourceId) {
        if(noiseMapParameters.computeLDay && sourceId < wjSourcesD.size()) {
            return wjSourcesD.get(sourceId);
        } else if(noiseMapParameters.computeLEvening && sourceId < wjSourcesE.size()) {
            return wjSourcesE.get(sourceId);
        } else if(noiseMapParameters.computeLNight && sourceId < wjSourcesN.size()) {
            return wjSourcesN.get(sourceId);
        } else {
            return new double[0];
        }
    }

}
