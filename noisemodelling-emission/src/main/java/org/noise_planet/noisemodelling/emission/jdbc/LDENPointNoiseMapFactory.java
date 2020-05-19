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

package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2.jdbc.JdbcBatchUpdateException;
import org.h2gis.utilities.TableLocation;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
public class LDENPointNoiseMapFactory implements PointNoiseMap.PropagationProcessDataFactory, PointNoiseMap.IComputeRaysOutFactory {
    LDENConfig ldenConfig;
    TableWriter tableWriter;
    Thread tableWriterThread;
    static final int BATCH_MAX_SIZE = 500;
    public boolean keepRays = false;
    LDENComputeRaysOut.LdenData ldenData = new LDENComputeRaysOut.LdenData();


    public LDENPointNoiseMapFactory(Connection connection, LDENConfig ldenConfig) {
        tableWriter = new TableWriter(connection, ldenConfig, ldenData);
        this.ldenConfig = ldenConfig;
    }

    /**
     * @return Store propagation rays
     */
    public boolean isKeepRays() {
        return keepRays;
    }

    /**
     * @param keepRays true to store propagation rays
     */
    public void setKeepRays(boolean keepRays) {
        this.keepRays = keepRays;
    }

    /**
     * Start creating and filling database tables
     */
    public void start() {
        tableWriterThread = new Thread(tableWriter);
        tableWriterThread.start();
    }

    /**
     * Write the last results and stop the sql writing thread
     */
    public void stop() {
        ldenConfig.exitWhenDone = true;
        while (tableWriterThread.isAlive()) {
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
        return new LDENPropagationProcessData(freeFieldFinder, ldenConfig);
    }

    @Override
    public IComputeRaysOut create(PropagationProcessData threadData, PropagationProcessPathData pathData) {
        return new LDENComputeRaysOut(keepRays, pathData, (LDENPropagationProcessData)threadData, ldenData);
    }

    private static class TableWriter implements Runnable {
        Logger LOGGER = LoggerFactory.getLogger(TableWriter.class);
        private Connection connection;
        LDENConfig ldenConfig;
        LDENComputeRaysOut.LdenData ldenData;
        double[] a_weighting;

        public TableWriter(Connection connection, LDENConfig ldenConfig, LDENComputeRaysOut.LdenData ldenData) {
            this.connection = connection;
            this.ldenConfig = ldenConfig;
            this.ldenData = ldenData;
            a_weighting = new double[PropagationProcessPathData.freq_lvl_a_weighting.size()];
            for(int idfreq = 0; idfreq < a_weighting.length; idfreq++) {
                a_weighting[idfreq] = PropagationProcessPathData.freq_lvl_a_weighting.get(idfreq);
            }
        }

        /**
         * Pop values from stack and insert rows
         * @param tableName Table to feed
         * @param stack Stack to pop from
         * @throws SQLException Got an error
         */
        void processStack(String tableName, ConcurrentLinkedDeque<ComputeRaysOut.VerticeSL> stack) throws SQLException {
            StringBuilder query = new StringBuilder("INSERT INTO ");
            query.append(TableLocation.parse(tableName));
            query.append(" VALUES (? "); // ID_RECEIVER
            if(!ldenConfig.mergeSources) {
                query.append(", ?"); // ID_SOURCE
            }
            for(int idfreq=0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                query.append(", ?"); // freq value
            }
            query.append(", ?, ?);"); // laeq, leq
            PreparedStatement ps = connection.prepareStatement(query.toString());
            int batchSize = 0;
            while(!stack.isEmpty()) {
                ComputeRaysOut.VerticeSL row = stack.pop();
                ldenData.queueSize.decrementAndGet();
                int parameterIndex = 1;
                ps.setLong(parameterIndex++, row.receiverId);
                if(!ldenConfig.mergeSources) {
                    ps.setLong(parameterIndex++, row.sourceId);
                }
                for(int idfreq=0;idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
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
                sb.append(" (IDRECEIVER serial");
            }
            for (int idfreq = 0; idfreq < PropagationProcessPathData.freq_lvl.size(); idfreq++) {
                sb.append(", HZ");
                sb.append(PropagationProcessPathData.freq_lvl.get(idfreq));
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
                if(ldenConfig.computeLDay) {
                    sql.execute(String.format("DROP TABLE %s IF EXISTS", ldenConfig.lDayTable));
                    sql.execute(forgeCreateTable(ldenConfig.lDayTable));
                }
                if(ldenConfig.computeLEvening) {
                    sql.execute(String.format("DROP TABLE %s IF EXISTS", ldenConfig.lEveningTable));
                    sql.execute(forgeCreateTable(ldenConfig.lEveningTable));
                }
                if(ldenConfig.computeLNight) {
                    sql.execute(String.format("DROP TABLE %s IF EXISTS", ldenConfig.lNightTable));
                    sql.execute(forgeCreateTable(ldenConfig.lNightTable));
                }
                if(ldenConfig.computeLDEN) {
                    sql.execute(String.format("DROP TABLE %s IF EXISTS", ldenConfig.lDenTable));
                    sql.execute(forgeCreateTable(ldenConfig.lDenTable));
                }
                while (!ldenConfig.aborted) {
                    try {
                        if(!ldenData.lDayLevels.isEmpty()) {
                            processStack(ldenConfig.lDayTable, ldenData.lDayLevels);
                        } else if(!ldenData.lEveningLevels.isEmpty()) {
                            processStack(ldenConfig.lEveningTable, ldenData.lEveningLevels);
                        } else if(!ldenData.lNightLevels.isEmpty()) {
                            processStack(ldenConfig.lNightTable, ldenData.lNightLevels);
                        } else if(!ldenData.lDenLevels.isEmpty()) {
                            processStack(ldenConfig.lDenTable, ldenData.lDenLevels);
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
                ldenConfig.aborted = true;
            }
            LOGGER.info("Exit TableWriter");
        }
    }
}
