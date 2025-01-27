package org.noise_planet.noisemodelling.jdbc.input;

import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivityRecord;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.DiscreteDirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.jdbc.NoiseEmissionMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 *  Default implementation for initializing input propagation process data for noise map computation.
 */
public class DefaultTableLoader implements NoiseMapByReceiverMaker.PropagationProcessDataFactory {
    int srid = 0;
    NoiseMapDatabaseParameters noiseMapDatabaseParameters = new NoiseMapDatabaseParameters();


    public enum INPUT_MODE {
        /** Test */
        INPUT_MODE_TRAFFIC_FLOW,
        INPUT_MODE_LW,
        INPUT_MODE_ATTENUATION }

    /**
     * When fetching tables data, expect the columns according to this mode
     */
    public INPUT_MODE inputMode = INPUT_MODE.INPUT_MODE_ATTENUATION;

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();

    /**
     * Inserts directivity attributes for noise sources for trains into the directionAttributes map.
     */
    public void insertTrainDirectivity() {
        directionAttributes.clear();
        directionAttributes.put(0, new OmnidirectionalDirection());
        int i=1;
        for(String typeSource : RailWayCnossosParameters.sourceType) {
            directionAttributes.put(i, new RailwayCnossosDirectivitySphere(new LineSource(typeSource)));
            i++;
        }
    }

    /**
     * Initializes the NoiseMap parameters and attenuation data based on the input mode specified in the NoiseMap parameters.
     * @param connection   the database connection to be used for initialization.
     * @param noiseMapByReceiverMaker the noise map by receiver maker object associated with the computation process.
     * @throws SQLException
     */

    @Override
    public void initialize(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker) throws SQLException {
        if(JDBCUtilities.tableExists(connection, noiseMapByReceiverMaker.getSourcesTableName())) {
            this.srid = GeometryTableUtilities.getSRID(connection, noiseMapByReceiverMaker.getSourcesTableName());
        }
        if(noiseMapDatabaseParameters.input_mode == NoiseMapDatabaseParameters.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Fetch source fields
            List<String> sourceField = JDBCUtilities.getColumnNames(connection, noiseMapByReceiverMaker.getSourcesTableName());
            List<Integer> frequencyValues = new ArrayList<>();
            List<Integer> allFrequencyValues = Arrays.asList(Scene.DEFAULT_FREQUENCIES_THIRD_OCTAVE);
            String period = "";
            if (noiseMapDatabaseParameters.computeLDay || noiseMapDatabaseParameters.computeLDEN) {
                period = "D";
            } else if (noiseMapDatabaseParameters.computeLEvening) {
                period = "E";
            } else if (noiseMapDatabaseParameters.computeLNight) {
                period = "N";
            }
            String freqField = noiseMapDatabaseParameters.lwFrequencyPrepend + period;
            if (!period.isEmpty()) {
                for (String fieldName : sourceField) {
                    if (fieldName.toUpperCase(Locale.ROOT).startsWith(freqField)) {
                        int freq = Integer.parseInt(fieldName.substring(freqField.length()));
                        int index = allFrequencyValues.indexOf(freq);
                        if (index >= 0) {
                            frequencyValues.add(freq);
                        }
                    }
                }
            }
            // Sort frequencies values
            Collections.sort(frequencyValues);
            // Get associated values for each frequency
            List<Double> exactFrequencies = new ArrayList<>();
            List<Double> aWeighting = new ArrayList<>();
            for (int freq : frequencyValues) {
                int index = allFrequencyValues.indexOf(freq);
                exactFrequencies.add(Scene.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE[index]);
                aWeighting.add(Scene.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE[index]);
            }
            if(frequencyValues.isEmpty()) {
                throw new SQLException("Source table "+ noiseMapByReceiverMaker.getSourcesTableName()+" does not contains any frequency bands");
            }
            // Instance of PropagationProcessPathData maybe already set
            for(NoiseMapDatabaseParameters.TIME_PERIOD timePeriod : NoiseMapDatabaseParameters.TIME_PERIOD.values()) {
                if (noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod) == null) {
                    AttenuationCnossosParameters attenuationCnossosParameters = new AttenuationCnossosParameters(frequencyValues, exactFrequencies, aWeighting);
                    noiseMapDatabaseParameters.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                    noiseMapByReceiverMaker.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                } else {
                    noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod).setFrequencies(frequencyValues);
                    noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod).setFrequenciesExact(exactFrequencies);
                    noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod).setFrequenciesAWeighting(aWeighting);
                    noiseMapDatabaseParameters.setPropagationProcessPathData(timePeriod, noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod));
                }
            }
        } else {
            for(NoiseMapDatabaseParameters.TIME_PERIOD timePeriod : NoiseMapDatabaseParameters.TIME_PERIOD.values()) {
                if (noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod) == null) {
                    // Traffic flow cnossos frequencies are octave bands from 63 to 8000 Hz
                    AttenuationCnossosParameters attenuationCnossosParameters = new AttenuationCnossosParameters(false);
                    noiseMapDatabaseParameters.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                    noiseMapByReceiverMaker.setPropagationProcessPathData(timePeriod, attenuationCnossosParameters);
                } else {
                    noiseMapDatabaseParameters.setPropagationProcessPathData(timePeriod, noiseMapByReceiverMaker.getPropagationProcessPathData(timePeriod));
                }
            }
        }
    }

    @Override
    public ProfileBuilder createProfileBuilder() {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.setFrequencyArray(fre);
    }

    /**
     * Creates a new instance of NoiseEmissionMaker using the provided ProfileBuilder and NoiseMapParameters.
     * @param builder the profile builder used to construct the scene.
     * @return A new instance of NoiseEmissionMaker initialized with the provided ProfileBuilder and NoiseMapParameters.
     */
    @Override
    public SceneWithAttenuation create(ProfileBuilder builder) {
        SceneWithAttenuation noiseEmissionMaker = new SceneWithAttenuation(builder);
        noiseEmissionMaker.setDirectionAttributes(directionAttributes);
        return noiseEmissionMaker;
    }


    /**
     * The table shall contain the following fields :
     * DIR_ID : identifier of the directivity sphere (INTEGER)
     * THETA : Horizontal angle in degree. 0° front and 90° right (0-360) (FLOAT)
     * PHI : Vertical angle in degree. 0° front and 90° top -90° bottom (-90 - 90) (FLOAT)
     * LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000 : attenuation levels in dB for each octave or third octave (FLOAT)
     * @param connection
     * @param tableName
     * @param defaultInterpolation
     * @return
     */
    public static Map<Integer, DiscreteDirectivitySphere> fetchDirectivity(Connection connection, String tableName, int defaultInterpolation) throws SQLException {
        Map<Integer, DiscreteDirectivitySphere> directionAttributes = new HashMap<>();
        List<String> fields = JDBCUtilities.getColumnNames(connection, tableName);
        // fetch provided frequencies
        List<String> frequenciesFields = new ArrayList<>();
        for(String field : fields) {
            if(field.toUpperCase(Locale.ROOT).startsWith("LW")) {
                try {
                    double frequency = Double.parseDouble(field.substring(2));
                    if (frequency > 0) {
                        frequenciesFields.add(field);
                    }
                } catch (NumberFormatException ex) {
                    //ignore column
                }
            }
        }
        if(frequenciesFields.isEmpty()) {
            return directionAttributes;
        }
        double[] frequencies = new double[frequenciesFields.size()];
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencies[idFrequency] = Double.parseDouble(frequenciesFields.get(idFrequency).substring(2));
        }
        StringBuilder sb = new StringBuilder("SELECT DIR_ID, THETA, PHI");
        for(String frequency : frequenciesFields) {
            sb.append(", ");
            sb.append(frequency);
        }
        sb.append(" FROM ");
        sb.append(tableName);
        sb.append(" ORDER BY DIR_ID");
        try(Statement st = connection.createStatement()) {
            try(ResultSet rs = st.executeQuery(sb.toString())) {
                List<DirectivityRecord> rows = new ArrayList<>();
                int lastDirId = Integer.MIN_VALUE;
                while (rs.next()) {
                    int dirId = rs.getInt(1);
                    if(lastDirId != dirId && !rows.isEmpty()) {
                        DiscreteDirectivitySphere attributes = new DiscreteDirectivitySphere(lastDirId, frequencies);
                        attributes.setInterpolationMethod(defaultInterpolation);
                        attributes.addDirectivityRecords(rows);
                        directionAttributes.put(lastDirId, attributes);
                        rows.clear();
                    }
                    lastDirId = dirId;
                    double theta = Math.toRadians(rs.getDouble(2));
                    double phi = Math.toRadians(rs.getDouble(3));
                    double[] att = new double[frequencies.length];
                    for(int freqColumn = 0; freqColumn < frequencies.length; freqColumn++) {
                        att[freqColumn] = rs.getDouble(freqColumn + 4);
                    }
                    DirectivityRecord r = new DirectivityRecord(theta, phi, att);
                    rows.add(r);
                }
                if(!rows.isEmpty()) {
                    DiscreteDirectivitySphere attributes = new DiscreteDirectivitySphere(lastDirId, frequencies);
                    attributes.setInterpolationMethod(defaultInterpolation);
                    attributes.addDirectivityRecords(rows);
                    directionAttributes.put(lastDirId, attributes);
                }
            }
        }
        return directionAttributes;
    }
}
