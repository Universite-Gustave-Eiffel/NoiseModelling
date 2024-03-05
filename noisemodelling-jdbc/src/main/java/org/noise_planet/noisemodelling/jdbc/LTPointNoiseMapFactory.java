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

import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.emission.DirectionAttributes;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.jdbc.utils.StringPreparedStatements;
import org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.utils.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.GZIPOutputStream;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

/**
 *
 */
public class LTPointNoiseMapFactory implements PointNoiseMap.PropagationProcessDataFactory, PointNoiseMap.IComputeRaysOutFactory, ProfilerThread.Metric {
    LTConfig ltConfig;
    TableWriter tableWriter;
    Thread tableWriterThread;
    Connection connection;
    static final int BATCH_MAX_SIZE = 500;
    static final int WRITER_CACHE = 65536;
    LTComputeRaysOut.LtData ltData = new LTComputeRaysOut.LtData();
    int srid;

    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectionAttributes> directionAttributes = new HashMap<>();


    public LTPointNoiseMapFactory(Connection connection, LTConfig ltConfig) {
        this.ltConfig = ltConfig;
        this.connection = connection;
    }

    @Override
    public String[] getColumnNames() {
        return new String[] {"jdbc_stack"};
    }

    @Override
    public String[] getCurrentValues() {
        return new String[] {Long.toString(ltData.queueSize.get())};
    }

    @Override
    public void tick(long currentMillis) {

    }

    public LTComputeRaysOut.LtData getLtData() {
        return ltData;
    }

    public void insertTrainDirectivity() {
        directionAttributes.clear();
        directionAttributes.put(0, new LTPropagationProcessData.OmnidirectionalDirection());
        for(RailWayLW.TrainNoiseSource noiseSource : RailWayLW.TrainNoiseSource.values()) {
            directionAttributes.put(noiseSource.ordinal() + 1, new RailWayLW.TrainAttenuation(noiseSource));
        }
    }

    @Override
    public void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException {

        // Fetch source fields
        List<String> sourceField = JDBCUtilities.getColumnNames(connection, pointNoiseMap.getSourcesTableName());
        this.srid = GeometryTableUtilities.getSRID(connection, pointNoiseMap.getSourcesTableName());
        List<Integer> frequencyValues = new ArrayList<>();
        List<Integer> allFrequencyValues = Arrays.asList(CnossosPropagationData.DEFAULT_FREQUENCIES_THIRD_OCTAVE);

        String freqField = ltConfig.lwFrequencyPrepend;

        for (String fieldName : sourceField) {
            if (fieldName.startsWith(freqField)) {
                int freq = Integer.parseInt(fieldName.substring(freqField.length()));
                int index = allFrequencyValues.indexOf(freq);
                if (index >= 0) {
                    frequencyValues.add(freq);
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
            exactFrequencies.add(CnossosPropagationData.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE[index]);
            aWeighting.add(CnossosPropagationData.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE[index]);
        }
        if(frequencyValues.isEmpty()) {
            throw new SQLException("Source table "+pointNoiseMap.getSourcesTableName()+" does not contains any frequency bands");
        }

        try {
            pointNoiseMap.createSourcesTable(connection,frequencyValues);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Integer> sourcesList = null;

        try {
            sourcesList = pointNoiseMap.fetchSourceNumber(connection);
            ltConfig.timesteps = pointNoiseMap.fetchTimeSteps(connection);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pointNoiseMap.setSourcesTableName("LW_SOURCES_130DB");

        // Instance of PropagationProcessPathData maybe already set
        for(String timestep : ltConfig.timesteps) {
            if (pointNoiseMap.getPropagationProcessPathData(timestep) == null) {
                PropagationProcessPathData propagationProcessPathData = new PropagationProcessPathData(frequencyValues, exactFrequencies, aWeighting);
                ltConfig.setPropagationProcessPathData(timestep, propagationProcessPathData);
                pointNoiseMap.setPropagationProcessPathData(timestep, propagationProcessPathData);
            } else {
                pointNoiseMap.getPropagationProcessPathData(timestep).setFrequencies(frequencyValues);
                pointNoiseMap.getPropagationProcessPathData(timestep).setFrequenciesExact(exactFrequencies);
                pointNoiseMap.getPropagationProcessPathData(timestep).setFrequenciesAWeighting(aWeighting);
                ltConfig.setPropagationProcessPathData(timestep, pointNoiseMap.getPropagationProcessPathData(timestep));
            }
        }
    }

    /**
     * Start creating and filling database tables
     */
    public void start() {
        if(ltConfig.getPropagationProcessPathData() == null) {
            throw new IllegalStateException("start() function must be called after PointNoiseMap initialization call");
        }
        tableWriter = new TableWriter(connection, ltConfig, ltData, srid);
        ltConfig.exitWhenDone = false;
        tableWriterThread = new Thread(tableWriter);
        tableWriterThread.start();
        while (!tableWriter.started && !ltConfig.aborted) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }

    /**
     * Write the last results and stop the sql writing thread
     */
    public void stop() {
        ltConfig.exitWhenDone = true;
        while (tableWriterThread != null && tableWriterThread.isAlive()) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }

    /**
     * Abort writing results and kill the writing thread
     */
    public void cancel() {
        ltConfig.aborted = true;
        while (tableWriterThread.isAlive()) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }

    @Override
    public LTPropagationProcessData create(ProfileBuilder builder) {
        LTPropagationProcessData ltPropagationProcessData = new LTPropagationProcessData(builder, ltConfig, ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue().freq_lvl);
        ltPropagationProcessData.setDirectionAttributes(directionAttributes);
        return ltPropagationProcessData;
    }

    @Override
    public IComputeRaysOut create(CnossosPropagationData threadData, PropagationProcessPathData pathDataT, PropagationProcessPathData pathDataEvening, PropagationProcessPathData pathDataNight) {
        return new LTComputeRaysOut(pathDataT,
                (LTPropagationProcessData)threadData, ltData, ltConfig);
    }

    private static class TableWriter implements Runnable {
        Logger LOGGER = LoggerFactory.getLogger(TableWriter.class);
        File sqlFilePath;
        private Connection connection;
        LTConfig ltConfig;
        LTComputeRaysOut.LtData ltData;
        double[] a_weighting;
        boolean started = false;
        Writer o;
        int srid;

        public TableWriter(Connection connection, LTConfig ltConfig, LTComputeRaysOut.LtData ltData, int srid) {
            this.connection = connection;
            this.sqlFilePath = ltConfig.sqlOutputFile;
            this.ltConfig = ltConfig;
            this.ltData = ltData;
            PropagationProcessPathData propagationProcessPathData = ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue();
            a_weighting = new double[propagationProcessPathData.freq_lvl_a_weighting.size()];
            for(int idfreq = 0; idfreq < a_weighting.length; idfreq++) {
                a_weighting[idfreq] = propagationProcessPathData.freq_lvl_a_weighting.get(idfreq);
            }
            this.srid = srid;
        }

        void processRaysStack(ConcurrentLinkedDeque<PropagationPath> stack) throws SQLException {
            StringBuilder query = new StringBuilder("INSERT INTO " + ltConfig.raysTable +
                    "(the_geom , IDRECEIVER , IDSOURCE");
            if(ltConfig.exportProfileInRays) {
                query.append(", GEOJSON");
            }
            if(ltConfig.keepAbsorption) {
                query.append(", LEQ, PERIOD");
            }
            query.append(") VALUES (?, ?, ?");
            if(ltConfig.exportProfileInRays) {
                query.append(", ?");
            }
            if(ltConfig.keepAbsorption) {
                query.append(", ?, ?");
            }
            query.append(");");
            // PK, GEOM, ID_RECEIVER, ID_SOURCE
            PreparedStatement ps;
            if(sqlFilePath == null) {
                ps = connection.prepareStatement(query.toString());
            } else {
                ps = new StringPreparedStatements(o, query.toString());
            }
            int batchSize = 0;
            while(!stack.isEmpty()) {
                PropagationPath row = stack.pop();
                ltData.queueSize.decrementAndGet();
                int parameterIndex = 1;
                LineString lineString = row.asGeom();
                lineString.setSRID(srid);
                ps.setObject(parameterIndex++, lineString);
                ps.setLong(parameterIndex++, row.getIdReceiver());
                ps.setLong(parameterIndex++, row.getIdSource());
                if(ltConfig.exportProfileInRays) {
                    String geojson = "";
                    try {
                        geojson = row.profileAsJSON(ltConfig.geojsonColumnSizeLimit);
                    } catch (IOException ex) {
                        //ignore
                    }
                    ps.setString(parameterIndex++, geojson);
                }
                if(ltConfig.keepAbsorption) {
                    double globalValue = sumDbArray(row.absorptionData.aGlobal);
                    ps.setDouble(parameterIndex++, globalValue);
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
        void processStack(String tableName, ConcurrentLinkedDeque<ComputeRaysOutAttenuation.VerticeSL> stack) throws SQLException {
            StringBuilder query = new StringBuilder("INSERT INTO ");
            query.append(tableName);
            query.append(" VALUES (? "); // ID_RECEIVER
            if(!ltConfig.mergeSources) {
                query.append(", ?"); // ID_SOURCE
            }
            if (!ltConfig.computeLAEQOnly) {

                query.append(", ?".repeat(ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue().freq_lvl.size())); // freq value
                query.append(", ?, ?);"); // laeq, leq
            }else{
                query.append(", ?);"); // laeq, leq
            }
            PreparedStatement ps;
            if(sqlFilePath == null) {
                ps = connection.prepareStatement(query.toString());
            } else {
                ps = new StringPreparedStatements(o, query.toString());
            }
            int batchSize = 0;
            while(!stack.isEmpty()) {
                ComputeRaysOutAttenuation.VerticeSL row = stack.pop();
                ltData.queueSize.decrementAndGet();
                int parameterIndex = 1;
                ps.setLong(parameterIndex++, row.receiverId);
                if(!ltConfig.mergeSources) {
                    ps.setLong(parameterIndex++, row.sourceId);
                }

                if (!ltConfig.computeLAEQOnly){
                    for(int idfreq=0;idfreq < ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue().freq_lvl.size(); idfreq++) {
                        double value = row.value[idfreq];
                        if(!Double.isFinite(value)) {
                            value = -99.0;
                            row.value[idfreq] = value;
                        }
                        ps.setDouble(parameterIndex++, value);
                    }

                }
                // laeq value
                double value = wToDba(sumArray(dbaToW(sumArray(row.value, a_weighting))));
                if(!Double.isFinite(value)) {
                    value = -99;
                }
                ps.setDouble(parameterIndex++, value);

                // leq value
                if (!ltConfig.computeLAEQOnly) {
                    ps.setDouble(parameterIndex++, wToDba(sumArray(dbaToW(row.value))));
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

        private String forgeCreateTable(String tableName) {
            StringBuilder sb = new StringBuilder("create table ");
            sb.append(tableName);
            if(!ltConfig.mergeSources) {
                sb.append(" (IDRECEIVER bigint NOT NULL");
                sb.append(", IDSOURCE bigint NOT NULL");
            } else {
                sb.append(" (IDRECEIVER bigint NOT NULL");
            }
            if (ltConfig.computeLAEQOnly){
                sb.append(", LAEQ numeric(5, 2)");
                sb.append(");");
            } else {
                for (int idfreq = 0; idfreq < ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue().freq_lvl.size(); idfreq++) {
                    sb.append(", HZ");
                    sb.append(ltConfig.propagationProcessPathDataT.entrySet().iterator().next().getValue().freq_lvl.get(idfreq));
                    sb.append(" numeric(5, 2)");
                }
                sb.append(", LAEQ numeric(5, 2), LEQ numeric(5, 2)");
                sb.append(");");
            }
            return sb.toString();
        }

        private String forgePkTable(String tableName) {
            if (ltConfig.mergeSources) {
                return "ALTER TABLE " + tableName + " ADD PRIMARY KEY(IDRECEIVER);";
            } else {
                return "CREATE INDEX ON " + tableName + " (IDRECEIVER);";
            }
        }

        private void processQuery(String query) throws SQLException, IOException {
            if(sqlFilePath == null) {
                try(Statement sql = connection.createStatement()) {
                    sql.execute(query);
                }
            } else {
                o.write(query+"\n");
            }
        }

        public void init() throws SQLException, IOException {
            if(ltConfig.getExportRaysMethod() == LTConfig.ExportRaysMethods.TO_RAYS_TABLE) {
                if(ltConfig.dropResultsTable) {
                    String q = String.format("DROP TABLE IF EXISTS %s;", ltConfig.raysTable);
                    processQuery(q);
                }
                StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + ltConfig.raysTable + "(pk bigint auto_increment, the_geom " +
                        "geometry(LINESTRING Z,");
                sb.append(srid);
                sb.append("), IDRECEIVER bigint NOT NULL, IDSOURCE bigint NOT NULL");
                if(ltConfig.exportProfileInRays) {
                    sb.append(", GEOJSON VARCHAR");
                }
                if(ltConfig.keepAbsorption) {
                    sb.append(", LEQ DOUBLE, PERIOD VARCHAR");
                }
                sb.append(");");
                processQuery(sb.toString());
            }



            if(ltConfig.dropResultsTable) {
                String q = String.format("DROP TABLE IF EXISTS %s;", ltConfig.lTTable);
                processQuery(q);
            }
            String q = forgeCreateTable(ltConfig.lTTable);
            processQuery(q);

        }

        void mainLoop() throws SQLException, IOException {
            while (!ltConfig.aborted) {
                started = true;
                try {
                    if(!ltData.lTLevels.isEmpty()) {
                        processStack(ltConfig.lTTable, ltData.lTLevels);
                    } else if(!ltData.rays.isEmpty()) {
                        processRaysStack(ltData.rays);
                    } else {
                        if(ltConfig.exitWhenDone) {
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

        void createKeys()  throws SQLException, IOException {
            // Set primary keys
            LOGGER.info("Write done, apply primary keys");
            processQuery(forgePkTable(ltConfig.lTTable));

        }

        OutputStreamWriter getStream() throws IOException {
            if(ltConfig.sqlOutputFileCompression) {
                return new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(sqlFilePath), WRITER_CACHE));
            } else {
                return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(sqlFilePath), WRITER_CACHE));
            }
        }

        @Override
        public void run() {
            // Drop and create tables
            if(sqlFilePath == null) {
                try {
                    init();
                    mainLoop();
                    createKeys();
                } catch (SQLException e) {
                    LOGGER.error("SQL Writer exception", e);
                    LOGGER.error(e.getLocalizedMessage(), e.getNextException());
                    ltConfig.aborted = true;
                } catch (Throwable e) {
                    LOGGER.error("Got exception on result writer, cancel calculation", e);
                    ltConfig.aborted = true;
                }
            } else {
                try(OutputStreamWriter bw = getStream()) {
                    o = bw;
                    init();
                    mainLoop();
                    createKeys();
                } catch (SQLException e) {
                    LOGGER.error("SQL Writer exception", e);
                    LOGGER.error(e.getLocalizedMessage(), e.getNextException());
                    ltConfig.aborted = true;
                } catch (Throwable e) {
                    LOGGER.error("Got exception on result writer, cancel calculation", e);
                    ltConfig.aborted = true;
                }
            }
            // LOGGER.info("Exit TableWriter");
        }
    }
}
