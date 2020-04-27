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

import org.noise_planet.noisemodelling.propagation.FastObstructionTest;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class LDENPointNoiseMapFactory implements PointNoiseMap.PropagationProcessDataFactory, PointNoiseMap.IComputeRaysOutFactory {
    LDENConfig ldenConfig;
    TableWriter tableWriter;


    public LDENPointNoiseMapFactory(Connection connection, LDENConfig ldenConfig) {
        tableWriter = new TableWriter(connection, ldenConfig);
    }

    /**
     * Start creating and filling database tables
     */
    void start() {

    }

    /**
     * Write the last results and stop the sql writing thread
     */
    void stop() {

    }

    /**
     * Abort writing results and kill the writing thread
     */
    void cancel() {

    }

    @Override
    public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
        return new LDENPropagationProcessData(freeFieldFinder, ldenConfig);
    }

    @Override
    public IComputeRaysOut create(PropagationProcessData threadData, PropagationProcessPathData pathData) {
        return null;
    }

    private static class TableWriter implements Runnable {
        Logger LOGGER = LoggerFactory.getLogger(TableWriter.class);
        private Connection connection;
        LDENConfig ldenConfig;

        public TableWriter(Connection connection, LDENConfig ldenConfig) {
            this.connection = connection;
            this.ldenConfig = ldenConfig;
        }

        @Override
        public void run() {
            // Drop and create tables
            try(Statement sql = connection.createStatement()) {
                while (!ldenConfig.aborted) {
                    if(ldenConfig.computeLDay) {
                        sql.execute(String.format("DROP TABLE %s IF EXISTS", ldenConfig.lDayTable));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("SQL Writer exception", e);
                ldenConfig.aborted = true;
            }
        }
    }
}
