/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.script;


import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Send Job Progression state to the database
 */
public class ProgressionTracker implements PropertyChangeListener {
    DataSource serverDataSource;
    int jobIdentifier;
    private final Logger logger;
    private String lastProg = "";
    private long lastProgressionUpdate = 0;
    private static final long TABLE_UPDATE_DELAY = 5000;

    public ProgressionTracker(DataSource serverDataSource, int jobIdentifier) {
        this.serverDataSource = serverDataSource;
        this.jobIdentifier = jobIdentifier;
        this.logger = LoggerFactory.getLogger(Job.getThreadName(jobIdentifier));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getNewValue() instanceof Double) {
            double progressValue = Math.round(((Double) evt.getNewValue()) * 10000.0) / 100.0;
            String newLogProgress = MessageFormat.format("{0,number,0.00}", progressValue);
            if(!lastProg.equals(newLogProgress)) {
                lastProg = newLogProgress;
                long t = System.currentTimeMillis();
                if(t - lastProgressionUpdate > TABLE_UPDATE_DELAY) {
                    lastProgressionUpdate = t;
                    try (Connection connection = serverDataSource.getConnection()) {
                        DatabaseManagement.setJobProgression(connection, jobIdentifier, progressValue);
                    } catch (SQLException ex) {
                        logger.error(ex.getLocalizedMessage(), ex);
                    }
                    logger.info("{} %", newLogProgress);
                }
            }
        }
    }
}