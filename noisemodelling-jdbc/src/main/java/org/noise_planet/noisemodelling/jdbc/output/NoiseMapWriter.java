/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.locationtech.jts.geom.*;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.input.SceneDatabaseInputSettings;
import org.noise_planet.noisemodelling.jdbc.utils.StringPreparedStatements;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.CoordinateMixin;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.LineSegmentMixin;
import org.noise_planet.noisemodelling.propagation.ReceiverNoiseLevel;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.*;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.dBToW;

/**
 * Process that run SQL query to feed tables
 */
public class NoiseMapWriter implements Callable<Boolean> {
    static final int BATCH_MAX_SIZE = 500;
    static final int WRITER_CACHE = 65536;
    AtomicBoolean exitWhenDone;
    AtomicBoolean aborted;
    Logger LOGGER = LoggerFactory.getLogger(NoiseMapWriter.class);
    File sqlFilePath;
    private Connection connection;
    NoiseMapByReceiverMaker noiseMapByReceiverMaker;
    NoiseMapDatabaseParameters databaseParameters;
    ResultsCache resultsCache;
    Writer writer;
    ObjectWriter jsonWriter;
    int srid;
    public List<Integer> frequencyArray = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    public double[] aWeightingArray = Arrays.stream(
                    asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE)).
            mapToDouble(value -> value).toArray();

    /**
     * Constructs a new NoiseMapWriter object with the specified parameters.
     * @param connection        the database connection used for writing data
     * @param noiseMapByReceiverMaker the parameters defining the noise map computation
     * @param ResultsCache   the attenuated paths containing computed noise data
     */
    public NoiseMapWriter(Connection connection, NoiseMapByReceiverMaker noiseMapByReceiverMaker, ResultsCache ResultsCache,
                          AtomicBoolean exitWhenDone, AtomicBoolean aborted) {
        this.connection = connection;
        this.noiseMapByReceiverMaker = noiseMapByReceiverMaker;
        databaseParameters = noiseMapByReceiverMaker.getNoiseMapDatabaseParameters();
        this.resultsCache = ResultsCache;
        this.srid = noiseMapByReceiverMaker.getGeometryFactory().getSRID();
        if(noiseMapByReceiverMaker.getPropagationProcessDataFactory() instanceof DefaultTableLoader) {
            aWeightingArray = ((DefaultTableLoader)noiseMapByReceiverMaker.getPropagationProcessDataFactory()).
                    aWeightingArray.stream().mapToDouble(value -> value).toArray();
            frequencyArray = ((DefaultTableLoader)noiseMapByReceiverMaker.getPropagationProcessDataFactory()).frequencyArray;
        }
        this.exitWhenDone = exitWhenDone;
        this.aborted = aborted;
        if(databaseParameters.exportCnossosPathWithAttenuation) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.addMixIn(Coordinate.class, CoordinateMixin.class);
            mapper.addMixIn(LineSegment.class, LineSegmentMixin.class);
            jsonWriter = mapper.writer();
        }
    }

    public String propagationPathAsJSON(CnossosPath path) throws JsonProcessingException {
        return jsonWriter.writeValueAsString(path);
    }

    public static CnossosPath jsonToPropagationPath(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CnossosPath.class);
    }

    /**
     * Processes the stack of CnossosPath objects and inserts their data into the rays table.
     * @param stack the stack of CnossosPath objects containing the data to be inserted into the rays table
     * @throws SQLException if an SQL exception occurs while executing the INSERT query
     */
    void processRaysStack(ConcurrentLinkedDeque<CnossosPath> stack) throws SQLException {
        boolean exportPeriod = !noiseMapByReceiverMaker.getSceneInputSettings().getInputMode().
                equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        StringBuilder query = new StringBuilder("INSERT INTO " + databaseParameters.raysTable +
                "(the_geom , IDRECEIVER , IDSOURCE");
        if(databaseParameters.exportCnossosPathWithAttenuation) {
            query.append(", PATH");
        }
        if(databaseParameters.exportAttenuationMatrix) {
            query.append(", LEQ");
        }
        if(exportPeriod) {
            query.append(", PERIOD");
        }
        query.append(") VALUES (?, ?, ?");
        if(databaseParameters.exportCnossosPathWithAttenuation) {
            query.append(", ?");
        }
        if(databaseParameters.exportAttenuationMatrix) {
            query.append(", ?");
        }
        if(exportPeriod) {
            query.append(", ?");
        }
        query.append(");");
        // PK, GEOM, ID_RECEIVER, ID_SOURCE
        PreparedStatement ps;
        if(sqlFilePath == null) {
            ps = connection.prepareStatement(query.toString());
        } else {
            ps = new StringPreparedStatements(writer, query.toString());
        }
        int batchSize = 0;
        while(!stack.isEmpty()) {
            CnossosPath row = stack.pop();
            resultsCache.queueSize.decrementAndGet();
            int parameterIndex = 1;
            LineString lineString = row.asGeom();
            lineString.setSRID(srid);
            ps.setObject(parameterIndex++, lineString);
            ps.setLong(parameterIndex++, row.getCutProfile().getReceiver().receiverPk);
            ps.setLong(parameterIndex++, row.getCutProfile().getSource().sourcePk);
            if(databaseParameters.exportCnossosPathWithAttenuation) {
                String json = "";
                try {
                    json = propagationPathAsJSON(row);
                } catch (IOException ex) {
                    //ignore
                }
                ps.setString(parameterIndex++, json);
            }
            if(databaseParameters.exportAttenuationMatrix) {
                double globalValue = sumDbArray(row.aGlobal);
                ps.setDouble(parameterIndex++, globalValue);
            }
            if(exportPeriod) {
                ps.setString(parameterIndex++, row.getTimePeriod());
            }
            ps.addBatch();
            batchSize++;
            if (batchSize >= BATCH_MAX_SIZE) {
                ps.executeBatch();
                ps.clearBatch();
                batchSize = 0;
            }
        }
        if (batchSize > 0) {
            ps.executeBatch();
        }

    }

    /**
     * Pop values from stack and insert rows
     * @param tableName Table to feed
     * @param stack Stack to pop from
     * @throws SQLException Got an error
     */
    void processStack(String tableName, ConcurrentLinkedDeque<ReceiverNoiseLevel> stack) throws SQLException {
        if(stack.isEmpty()) {
            return;
        }
        // If we compute attenuation only there is no period field
        boolean exportPeriod = !noiseMapByReceiverMaker.getSceneInputSettings().getInputMode().
                        equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(tableName);
        query.append(" VALUES (? "); // ID_RECEIVER
        if(!databaseParameters.mergeSources) {
            query.append(", ?"); // ID_SOURCE
        }
        if(exportPeriod) {
            query.append(", ?"); // PERIOD
        }
        if(databaseParameters.exportReceiverPosition) {
            query.append(", ?"); // THE_GEOM
        }
        if (!databaseParameters.computeLAEQOnly) {
            query.append(", ?".repeat(aWeightingArray.length)); // freq value LWXX
            query.append(", ?, ?);"); // laeq, leq
        }else{
            query.append(", ?);"); // laeq, leq
        }
        PreparedStatement ps;
        if(sqlFilePath == null) {
            ps = connection.prepareStatement(query.toString());
        } else {
            ps = new StringPreparedStatements(writer, query.toString());
        }
        int batchSize = 0;
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), srid);
        while(!stack.isEmpty()) {
            ReceiverNoiseLevel row = stack.pop();
            resultsCache.queueSize.decrementAndGet();
            int parameterIndex = 1;
            ps.setLong(parameterIndex++, row.receiver.receiverPk);
            if(!databaseParameters.mergeSources) {
                ps.setLong(parameterIndex++, row.source.sourcePk);
            }
            if(exportPeriod) {
                ps.setString(parameterIndex++, row.period);
            }
            if(databaseParameters.exportReceiverPosition) {
                ps.setObject(parameterIndex++,  row.receiver.position != null ?
                        factory.createPoint(row.receiver.position):
                        factory.createPoint());
            }
            if (!databaseParameters.computeLAEQOnly){
                for(int idfreq = 0; idfreq < aWeightingArray.length; idfreq++) {
                    double value = row.levels[idfreq];
                    if(!Double.isFinite(value)) {
                        value = -99.0;
                        row.levels[idfreq] = value;
                    }
                    ps.setDouble(parameterIndex++, value);
                }
            }
            // laeq value
            double value = wToDb(sumArray(AcousticIndicatorsFunctions.dBToW(sumArray(row.levels, aWeightingArray))));
            if(!Double.isFinite(value)) {
                value = -99;
            }
            ps.setDouble(parameterIndex++, value);

            // leq value
            if (!databaseParameters.computeLAEQOnly) {
                ps.setDouble(parameterIndex++, wToDb(sumArray(AcousticIndicatorsFunctions.dBToW(row.levels))));
            }

            ps.addBatch();
            batchSize++;
            if (batchSize >= BATCH_MAX_SIZE) {
                ps.executeBatch();
                ps.clearBatch();
                batchSize = 0;
            }
        }
        if (batchSize > 0) {
            ps.executeBatch();
        }
    }

    /**
     * Generates the SQL statement for creating a table based on the specified table name and configuration parameters.
     * @param tableName the name of the table to create
     * @return the SQL statement for creating the table
     */
    private String forgeCreateTable(String tableName) {
        // If we compute attenuation only there is no period field
        boolean exportPeriod = !noiseMapByReceiverMaker.getSceneInputSettings().getInputMode().
                equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        StringBuilder sb = new StringBuilder("create table ");
        sb.append(tableName);
        if(!databaseParameters.mergeSources) {
            sb.append(" (IDRECEIVER bigint NOT NULL");
            sb.append(", IDSOURCE bigint NOT NULL");
        } else {
            sb.append(" (IDRECEIVER bigint NOT NULL");
        }
        if(exportPeriod) {
            sb.append(", PERIOD VARCHAR NOT NULL");
        }
        if(databaseParameters.exportReceiverPosition) {
            sb.append(", THE_GEOM GEOMETRY(POINTZ,");
            sb.append(srid);
            sb.append(")");
        }
        if (databaseParameters.computeLAEQOnly){
            sb.append(", LAEQ REAL");
            sb.append(");");
        } else {
            for (int idfreq = 0; idfreq < aWeightingArray.length; idfreq++) {
                sb.append(", " + noiseMapByReceiverMaker.getFrequencyFieldPrepend());
                sb.append(frequencyArray.get(idfreq));
                sb.append(" REAL");
            }
            sb.append(", LAEQ REAL, LEQ REAL");
            sb.append(");");
        }
        return sb.toString();
    }

    /**
     * Creates a primary key or index on the specified table depending on the configuration.
     * @param tableName
     * @return the SQL statement for creating the primary key or index     */
    private String forgePkTable(String tableName) {
        boolean exportPeriod = !noiseMapByReceiverMaker.getSceneInputSettings().getInputMode().
                equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
        if (databaseParameters.mergeSources) {
            if(!exportPeriod) {
                return "ALTER TABLE " + tableName + " ADD PRIMARY KEY(IDRECEIVER);";
            } else {
                return "ALTER TABLE " + tableName + " ADD PRIMARY KEY(IDRECEIVER, PERIOD);";
            }
        } else {
            if(!exportPeriod) {
                return "CREATE INDEX ON " + tableName + " (IDRECEIVER, IDSOURCE);";
            } else {
                return "CREATE INDEX ON " + tableName + " (IDRECEIVER, IDSOURCE, PERIOD);";
            }
        }
    }

    /**
     * Executes the specified SQL query.
     * @param query
     * @throws SQLException
     * @throws IOException
     */
    private void processQuery(String query) throws SQLException, IOException {
        if(sqlFilePath == null) {
            try(Statement sql = connection.createStatement()) {
                sql.execute(query);
            }
        } else {
            writer.write(query+"\n");
        }
    }

    /**
     * Initializes the noise map calculation by setting up required database tables based on the specified parameters.
     * @throws SQLException
     * @throws IOException
     */
    public void init() throws SQLException, IOException {
        if(databaseParameters.getExportRaysMethod() == NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE) {
            boolean exportPeriod = !noiseMapByReceiverMaker.getSceneInputSettings().getInputMode().
                    equals(SceneDatabaseInputSettings.INPUT_MODE.INPUT_MODE_ATTENUATION);
            if(databaseParameters.dropResultsTable) {
                String q = String.format("DROP TABLE IF EXISTS %s;", databaseParameters.raysTable);
                processQuery(q);
            }
            StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + databaseParameters.raysTable + "(pk bigint auto_increment, the_geom " +
                    "geometry(LINESTRING Z,");
            sb.append(srid);
            sb.append("), IDRECEIVER bigint NOT NULL, IDSOURCE bigint NOT NULL");
            if(databaseParameters.exportCnossosPathWithAttenuation) {
                sb.append(", PATH VARCHAR");
            }
            if(databaseParameters.exportAttenuationMatrix) {
                sb.append(", LEQ DOUBLE");
            }
            if(exportPeriod) {
                sb.append(", PERIOD VARCHAR");
            }
            sb.append(");");
            processQuery(sb.toString());
        }
        if(databaseParameters.dropResultsTable) {
            String q = String.format("DROP TABLE IF EXISTS %s;", databaseParameters.receiversLevelTable);
            processQuery(q);
        }
        String q = forgeCreateTable(databaseParameters.receiversLevelTable);
        processQuery(q);
    }

    /**
     * Main loop for processing attenuated paths and stacking results.
     * @throws SQLException
     * @throws IOException
     */
    void mainLoop() throws SQLException, IOException {
        while (!aborted.get()) {
            try {
                if(!resultsCache.receiverLevels.isEmpty()) {
                    processStack(databaseParameters.receiversLevelTable, resultsCache.receiverLevels);
                } else if(!resultsCache.cnossosPaths.isEmpty()) {
                    processRaysStack(resultsCache.cnossosPaths);
                } else {
                    if(exitWhenDone.get()) {
                        break;
                    } else {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException ex) {
                // ignore
                break;
            }
        }
    }

    /**
     * Creates primary keys for the computed noise level tables.
     * @throws SQLException
     * @throws IOException
     */
    void createKeys()  throws SQLException, IOException {
        // Set primary keys
        LOGGER.info("Write done, apply primary keys");
        processQuery(forgePkTable(databaseParameters.receiversLevelTable));
        LOGGER.info("Primary keys applied");
    }

    /**
     * Gets an OutputStreamWriter for writing data to a file stream.
     * @return an OutputStreamWriter for writing data to a file stream
     * @throws IOException if an I/O error occurs while creating the stream
     */
    OutputStreamWriter getStream() throws IOException {
        if(databaseParameters.sqlOutputFileCompression) {
            return new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(sqlFilePath), WRITER_CACHE));
        } else {
            return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(sqlFilePath), WRITER_CACHE));
        }
    }

    /**
     * Executes the SQL writing process.
     */
    @Override
    public Boolean call() throws Exception {
        // Drop and create tables
        if(sqlFilePath == null) {
            try {
                mainLoop();
                createKeys();
            } catch (Exception e) {
                aborted.set(true);
                throw e;
            }
        } else {
            try(OutputStreamWriter bw = getStream()) {
                writer = bw;
                mainLoop();
                createKeys();
            } catch (Exception e) {
                aborted.set(true);
                throw e;
            }
        }
        return true;
    }
}