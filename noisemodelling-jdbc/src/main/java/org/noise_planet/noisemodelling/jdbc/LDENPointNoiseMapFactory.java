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
import org.noise_planet.noisemodelling.emission.DirectionAttributes;
import org.noise_planet.noisemodelling.emission.RailWayLW;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
public class LDENPointNoiseMapFactory implements PointNoiseMap.PropagationProcessDataFactory, PointNoiseMap.IComputeRaysOutFactory {
    LDENConfig ldenConfig;
    TableWriter tableWriter;
    Thread tableWriterThread;
    Connection connection;
    static final int BATCH_MAX_SIZE = 500;
    LDENComputeRaysOut.LdenData ldenData = new LDENComputeRaysOut.LdenData();
    /**
     * Attenuation and other attributes relative to direction on sphere
     */
    public Map<Integer, DirectionAttributes> directionAttributes = new HashMap<>();


    public LDENPointNoiseMapFactory(Connection connection, LDENConfig ldenConfig) {
        this.ldenConfig = ldenConfig;
        this.connection = connection;
    }

    public void insertTrainDirectivity() {
        directionAttributes.clear();
        directionAttributes.put(0, new LDENPropagationProcessData.OmnidirectionalDirection());
        for(RailWayLW.TrainNoiseSource noiseSource : RailWayLW.TrainNoiseSource.values()) {
            directionAttributes.put(noiseSource.ordinal() + 1, new RailWayLW.TrainAttenuation(noiseSource));
        }
    }

    @Override
    public void initialize(Connection connection, PointNoiseMap pointNoiseMap) throws SQLException {
        if(ldenConfig.input_mode == LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN) {
            // Fetch source fields
            List<String> sourceField = JDBCUtilities.getFieldNames(connection.getMetaData(), pointNoiseMap.getSourcesTableName());
            List<Integer> frequencyValues = new ArrayList<>();
            List<Integer> allFrequencyValues = Arrays.asList(PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE);
            String period = "";
            if (ldenConfig.computeLDay || ldenConfig.computeLDEN) {
                period = "D";
            } else if (ldenConfig.computeLEvening) {
                period = "E";
            } else if (ldenConfig.computeLNight) {
                period = "N";
            }
            String freqField = ldenConfig.lwFrequencyPrepend + period;
            if (!period.isEmpty()) {
                for (String fieldName : sourceField) {
                    if (fieldName.startsWith(freqField)) {
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
                exactFrequencies.add(PropagationProcessPathData.DEFAULT_FREQUENCIES_EXACT_THIRD_OCTAVE[index]);
                aWeighting.add(PropagationProcessPathData.DEFAULT_FREQUENCIES_A_WEIGHTING_THIRD_OCTAVE[index]);
            }
            if(frequencyValues.isEmpty()) {
                throw new SQLException("Source table "+pointNoiseMap.getSourcesTableName()+" does not contains any frequency bands");
            }
            // Instance of PropagationProcessPathData maybe already set
            if(pointNoiseMap.getPropagationProcessPathData() == null) {
                ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData(frequencyValues, exactFrequencies, aWeighting));
                pointNoiseMap.setPropagationProcessPathData(ldenConfig.propagationProcessPathData);
            } else {
                pointNoiseMap.getPropagationProcessPathData().setFrequencies(frequencyValues);
                pointNoiseMap.getPropagationProcessPathData().setFrequenciesExact(exactFrequencies);
                pointNoiseMap.getPropagationProcessPathData().setFrequenciesAWeighting(aWeighting);
                ldenConfig.setPropagationProcessPathData(pointNoiseMap.getPropagationProcessPathData());
            }
        } else {
            if(pointNoiseMap.getPropagationProcessPathData() == null) {
                // Traffic flow cnossos frequencies are octave bands from 63 to 8000 Hz
                ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData(false));
                pointNoiseMap.setPropagationProcessPathData(ldenConfig.propagationProcessPathData);
            } else {
                ldenConfig.setPropagationProcessPathData(pointNoiseMap.getPropagationProcessPathData());
            }
        }
    }

    /**
     * @return Store propagation rays
     */
    public boolean isKeepRays() {
        return ldenConfig.exportRays;
    }

    /**
     * @param keepRays true to store propagation rays
     */
    public void setKeepRays(boolean keepRays) {
        ldenConfig.setExportRays(keepRays);
    }

    /**
     * Start creating and filling database tables
     */
    public void start() {
        if(ldenConfig.propagationProcessPathData == null) {
            throw new IllegalStateException("start() function must be called after PointNoiseMap initialization call");
        }
        tableWriter = new TableWriter(connection, ldenConfig, ldenData);
        ldenConfig.exitWhenDone = false;
        tableWriterThread = new Thread(tableWriter);
        tableWriterThread.start();
        while (!tableWriter.started && !ldenConfig.aborted) {
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
        ldenConfig.exitWhenDone = true;
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
        ldenConfig.aborted = true;
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
    public LDENPropagationProcessData create(FastObstructionTest freeFieldFinder) {
        LDENPropagationProcessData ldenPropagationProcessData = new LDENPropagationProcessData(freeFieldFinder, ldenConfig);
        ldenPropagationProcessData.setDirectionAttributes(directionAttributes);
        return ldenPropagationProcessData;
    }

    @Override
    public IComputeRaysOut create(PropagationProcessData threadData, PropagationProcessPathData pathData) {
        return new LDENComputeRaysOut(pathData, (LDENPropagationProcessData)threadData, ldenData);
    }

    private static class TableWriter implements Runnable {
        Logger LOGGER = LoggerFactory.getLogger(TableWriter.class);
        private Connection connection;
        LDENConfig ldenConfig;
        LDENComputeRaysOut.LdenData ldenData;
        double[] a_weighting;
        boolean started = false;

        public TableWriter(Connection connection, LDENConfig ldenConfig, LDENComputeRaysOut.LdenData ldenData) {
            this.connection = connection;
            this.ldenConfig = ldenConfig;
            this.ldenData = ldenData;
            a_weighting = new double[ldenConfig.propagationProcessPathData.freq_lvl_a_weighting.size()];
            for(int idfreq = 0; idfreq < a_weighting.length; idfreq++) {
                a_weighting[idfreq] = ldenConfig.propagationProcessPathData.freq_lvl_a_weighting.get(idfreq);
            }
        }

        void processRaysStack(ConcurrentLinkedDeque<PropagationPath> stack) throws SQLException {
            String query = "INSERT INTO " + ldenConfig.raysTable + "(the_geom , IDRECEIVER , IDSOURCE ) VALUES (?, ?, ?)";
            // PK, GEOM, ID_RECEIVER, ID_SOURCE
            PreparedStatement ps = connection.prepareStatement(query);
            int batchSize = 0;
            while(!stack.isEmpty()) {
                PropagationPath row = stack.pop();
                ldenData.queueSize.decrementAndGet();
                int parameterIndex = 1;
                ps.setObject(parameterIndex++, row.asGeom());
                ps.setLong(parameterIndex++, row.getIdReceiver());
                ps.setLong(parameterIndex++, row.getIdSource());
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
            if(!ldenConfig.mergeSources) {
                query.append(", ?"); // ID_SOURCE
            }
            for(int idfreq=0; idfreq < ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                query.append(", ?"); // freq value
            }
            query.append(", ?, ?);"); // laeq, leq
            PreparedStatement ps = connection.prepareStatement(query.toString());
            int batchSize = 0;
            while(!stack.isEmpty()) {
                ComputeRaysOutAttenuation.VerticeSL row = stack.pop();
                ldenData.queueSize.decrementAndGet();
                int parameterIndex = 1;
                ps.setLong(parameterIndex++, row.receiverId);
                if(!ldenConfig.mergeSources) {
                    ps.setLong(parameterIndex++, row.sourceId);
                }
                for(int idfreq=0;idfreq < ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                    Double value = row.value[idfreq];
                    if(!Double.isFinite(value)) {
                        value = -99.0;
                        row.value[idfreq] = value;
                    }
                    ps.setDouble(parameterIndex++, value);
                }
                // laeq value
                ps.setDouble(parameterIndex++, ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(ComputeRays.sumArray(row.value, a_weighting)))));

                // leq value
                ps.setDouble(parameterIndex++, ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(row.value))));

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
            if(!ldenConfig.mergeSources) {
                sb.append(" (IDRECEIVER bigint NOT NULL");
                sb.append(", IDSOURCE bigint NOT NULL");
            } else {
                sb.append(" (IDRECEIVER SERIAL PRIMARY KEY");
            }
            for (int idfreq = 0; idfreq < ldenConfig.propagationProcessPathData.freq_lvl.size(); idfreq++) {
                sb.append(", HZ");
                sb.append(ldenConfig.propagationProcessPathData.freq_lvl.get(idfreq));
                sb.append(" numeric(5, 2)");
            }
            sb.append(", LAEQ numeric(5, 2), LEQ numeric(5, 2)");
            if(!ldenConfig.mergeSources) {
                sb.append(", PRIMARY KEY(IDRECEIVER, IDSOURCE)");
            }
            sb.append(")");
            return sb.toString();
        }

        @Override
        public void run() {
            // Drop and create tables
            try(Statement sql = connection.createStatement()) {
                if(ldenConfig.exportRays) {
                    sql.execute(String.format("DROP TABLE IF EXISTS %s", ldenConfig.raysTable));
                    sql.execute("CREATE TABLE "+ldenConfig.raysTable+"(pk serial primary key, the_geom geometry, IDRECEIVER bigint NOT NULL, IDSOURCE bigint NOT NULL)");
                }
                if(ldenConfig.computeLDay) {
                    sql.execute(String.format("DROP TABLE IF EXISTS %s", ldenConfig.lDayTable));
                    sql.execute(forgeCreateTable(ldenConfig.lDayTable));
                }
                if(ldenConfig.computeLEvening) {
                    sql.execute(String.format("DROP TABLE IF EXISTS %s", ldenConfig.lEveningTable));
                    sql.execute(forgeCreateTable(ldenConfig.lEveningTable));
                }
                if(ldenConfig.computeLNight) {
                    sql.execute(String.format("DROP TABLE IF EXISTS %s", ldenConfig.lNightTable));
                    sql.execute(forgeCreateTable(ldenConfig.lNightTable));
                }
                if(ldenConfig.computeLDEN) {
                    sql.execute(String.format("DROP TABLE IF EXISTS %s", ldenConfig.lDenTable));
                    sql.execute(forgeCreateTable(ldenConfig.lDenTable));
                }
                while (!ldenConfig.aborted) {
                    started = true;
                    try {
                        if(!ldenData.lDayLevels.isEmpty()) {
                            processStack(ldenConfig.lDayTable, ldenData.lDayLevels);
                        } else if(!ldenData.lEveningLevels.isEmpty()) {
                            processStack(ldenConfig.lEveningTable, ldenData.lEveningLevels);
                        } else if(!ldenData.lNightLevels.isEmpty()) {
                            processStack(ldenConfig.lNightTable, ldenData.lNightLevels);
                        } else if(!ldenData.lDenLevels.isEmpty()) {
                            processStack(ldenConfig.lDenTable, ldenData.lDenLevels);
                        } else if(!ldenData.rays.isEmpty()) {
                            processRaysStack(ldenData.rays);
                        } else {
                            if(ldenConfig.exitWhenDone) {
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
            } catch (SQLException e) {
                LOGGER.error("SQL Writer exception", e);
                LOGGER.error(e.getLocalizedMessage(), e.getNextException());
                ldenConfig.aborted = true;
            }
            // LOGGER.info("Exit TableWriter");
        }
    }
}
