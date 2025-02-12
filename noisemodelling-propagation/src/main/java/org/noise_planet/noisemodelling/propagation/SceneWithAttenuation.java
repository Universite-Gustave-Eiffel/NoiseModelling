package org.noise_planet.noisemodelling.propagation;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.sql.SQLException;
import java.util.*;

/**
 * Scene is used by ProfileBuilder to construct profiles with only taking account of geometry information.
 * This scene is adding information about attenuation and source power
 */
public class SceneWithAttenuation extends Scene {
    public static final double DEFAULT_GS = 0.0;

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();

    /**
     * Link between sources PK and DirectivitySphere specified in linked with directionAttributes
     */
    public Map<Long, Integer> sourceEmissionAttenuation = new HashMap<>();

    /**
     * Link between sources PK and gs ground factor of the source area
     */
    public Map<Long, Double> sourceGs = new HashMap<>();

    /**
     * Cached source table fields
     */
    public Map<String, Integer> sourceFieldNames = new HashMap<>();

    /**
     * If {@link #cnossosParametersPerPeriod} is empty, attenuation visitor will use this default settings and output
     * empty period
     */
    public AttenuationParameters defaultCnossosParameters = new AttenuationParameters();

    /**
     * Define attenuation settings to apply for each period
     */
    public Map<String, AttenuationParameters> cnossosParametersPerPeriod = new HashMap<>();

    /**
     * Keep a known set of all periods of the simulation
     * This set is used to output a default value when there is no sound source found for a specific period
     */
    public Set<String> periodSet = new HashSet<>();

    public SceneWithAttenuation(ProfileBuilder profileBuilder) {
        super(profileBuilder);
        defaultCnossosParameters.setFrequencies(profileBuilder.frequencyArray);
    }

    public SceneWithAttenuation() {
    }

    /**
     * Retrieves the ground speed of the noise source at the specified index.
     * @param srcIndex
     * @return the ground speed of the noise source at the specified index.
     */
    public double getSourceGs(int srcIndex){
        return sourceGs.get(sourcesPk.get(srcIndex));
    }

    /**
     * Add geometry with additional attributes
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param gs Additional attributes
     */

    public void addSource(Long pk, Geometry geom, Double gs) {
        addSource(pk, geom);
        sourceGs.put(pk, gs);
    }

    /**
     * Sets the direction attributes for the receiver.
     * @param directionAttributes
     */
    public void setDirectionAttributes(Map<Integer, DirectivitySphere> directionAttributes) {
        this.directionAttributes = directionAttributes;
        // Check if the directivities contain all required frequencies
        directionAttributes.forEach((integer, directivitySphere) -> {
            profileBuilder.frequencyArray.forEach(frequency->{
                if(!directivitySphere.coverFrequency(frequency)) {
                    throw new IllegalArgumentException(
                            String.format(Locale.ROOT,
                                    "The provided DirectivitySphere does not handle %d Hertz", frequency));
                }
            });
        });
    }

    /**
     * Add geometry with additional attributes
     * @param pk Unique source identifier
     * @param geom Source geometry
     * @param rs Additional attributes fetched from database
     */
    public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException {
        if(sourceFieldNames.isEmpty()) {
            List<String> fieldNames = JDBCUtilities.getColumnNames(rs.getMetaData());
            for(int idField = 0; idField < fieldNames.size(); idField++) {
                sourceFieldNames.put(fieldNames.get(idField).toUpperCase(Locale.ROOT), idField + 1);
            }
        }
        float yaw = 0;
        float pitch = 0;
        float roll = 0;
        boolean hasOrientation = false;
        if(sourceFieldNames.containsKey(YAW_DATABASE_FIELD)) {
            yaw = rs.getFloat(sourceFieldNames.get(YAW_DATABASE_FIELD));
            hasOrientation = true;
        }
        if(sourceFieldNames.containsKey(PITCH_DATABASE_FIELD)) {
            pitch = rs.getFloat(sourceFieldNames.get(PITCH_DATABASE_FIELD));
            hasOrientation = true;
        }
        if(sourceFieldNames.containsKey(ROLL_DATABASE_FIELD)) {
            roll = rs.getFloat(sourceFieldNames.get(ROLL_DATABASE_FIELD));
            hasOrientation = true;
        }
        int directivityField = JDBCUtilities.getFieldIndex(rs.getMetaData(), DIRECTIVITY_DATABASE_FIELD);
        if(sourceFieldNames.containsKey(DIRECTIVITY_DATABASE_FIELD)) {
            sourceEmissionAttenuation.put(pk, rs.getInt(directivityField));
        }
        if(hasOrientation) {
            addSource(pk, geom, new Orientation(yaw, pitch, roll));
        } else {
            addSource(pk, geom);
        }

        int gsField = JDBCUtilities.getFieldIndex(rs.getMetaData(), GS_DATABASE_FIELD);
        if(sourceFieldNames.containsKey(GS_DATABASE_FIELD)) {
            sourceGs.put(pk, rs.getDouble(gsField));
        }
    }

    /**
     * Checks if the noise source at the specified index is omnidirectional.
     * @param srcIndex Source index in the list sourceGeometries
     * @return True if the source is omnidirectional and so does not have orientation dependant attenuation, false otherwise.
     */
    public boolean isOmnidirectional(int srcIndex) {
        if (srcIndex < 0 || !(srcIndex < sourcesPk.size())) {
            return true;
        }
        long sourcePk = sourcesPk.get(srcIndex);
        if(!sourceEmissionAttenuation.containsKey(sourcePk)) {
            return true;
        }
        return directionAttributes.get(sourceEmissionAttenuation.get(sourcePk)) instanceof OmnidirectionalDirection;
    }

    /**
     *
     * @param srcIndex Source index in the list sourceGeometries
     * @param frequencies Frequency in Hertz
     * @param phi (0 2π) 0 is front
     * @param theta (-π/2 π/2) 0 is horizontal π is top
     * @return
     */
    public double[] getSourceAttenuation(int srcIndex, double[] frequencies, double phi, double theta) {
        int directivityIdentifier = sourceEmissionAttenuation.get(sourcesPk.get(srcIndex));
        if (directionAttributes.containsKey(directivityIdentifier)) {
            return directionAttributes.get(directivityIdentifier).getAttenuationArray(frequencies, phi, theta);
        } else {
            // This direction identifier has not been found
            return new double[frequencies.length];
        }
    }

    @Override
    public void clearSources() {
        super.clearSources();
        sourceEmissionAttenuation.clear();
        sourceFieldNames.clear();
        sourceGs.clear();
        directionAttributes.clear();
    }
}
