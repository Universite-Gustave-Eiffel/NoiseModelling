/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Create SQL Tables from a stream of noise levels
 */
public class NoiseMapMaker implements PropagationProcessDataFactory, IComputeRaysOutFactory, ProfilerThread.Metric {
    LdenNoiseMapParameters ldenNoiseMapParameters;
    NoiseMapWriter noiseMapWriter;
    Thread tableWriterThread;
    Connection connection;
    static final int BATCH_MAX_SIZE = 500;
    static final int WRITER_CACHE = 65536;
    AttenuatedPaths AttenuatedPaths = new AttenuatedPaths();
    int srid;




    public NoiseMapMaker(Connection connection, LdenNoiseMapParameters LdenNoiseMapParameters) {
        this.ldenNoiseMapParameters = LdenNoiseMapParameters;
        this.connection = connection;
    }

    @Override
    public String[] getColumnNames() {
        return new String[] {"jdbc_stack"};
    }

    @Override
    public String[] getCurrentValues() {
        return new String[] {Long.toString(AttenuatedPaths.queueSize.get())};
    }

    @Override
    public void tick(long currentMillis) {

    }

    public AttenuatedPaths getLdenData() {
        return AttenuatedPaths;
    }


    /**
     * Initializes the NoiseMap parameters and attenuation data based on the input mode specified in the NoiseMap parameters.
     * @param connection   the database connection to be used for initialization.
     * @param ldenNoiseMapLoader the noise map by receiver maker object associated with the computation process.
     * @throws SQLException
     */

    @Override
    public void initialize(Connection connection, LdenNoiseMapLoader ldenNoiseMapLoader) throws SQLException {
        if(JDBCUtilities.tableExists(connection, ldenNoiseMapLoader.getSourcesTableName())) {
            this.srid = GeometryTableUtilities.getSRID(connection, ldenNoiseMapLoader.getSourcesTableName());
        }
        ldenNoiseMapParameters.initialize(connection, ldenNoiseMapLoader);
    }

    /**
     * Start creating and filling database tables
     */
    public void start() {
        if(ldenNoiseMapParameters.getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD.DAY) == null) {
            throw new IllegalStateException("start() function must be called after NoiseMapByReceiverMaker initialization call");
        }
        noiseMapWriter = new NoiseMapWriter(connection, ldenNoiseMapParameters, AttenuatedPaths, srid);
        ldenNoiseMapParameters.exitWhenDone = false;
        tableWriterThread = new Thread(noiseMapWriter);
        tableWriterThread.start();
        while (!noiseMapWriter.started && !ldenNoiseMapParameters.aborted) {
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
        ldenNoiseMapParameters.exitWhenDone = true;
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
     * Creates a new instance of NoiseEmissionMaker using the provided ProfileBuilder and NoiseMapParameters.
     * @param builder the profile builder used to construct the scene.
     * @return A new instance of NoiseEmissionMaker initialized with the provided ProfileBuilder and NoiseMapParameters.
     */
    @Override
    public LdenScene create(ProfileBuilder builder) {
        LdenScene ldenScene = new LdenScene(builder, ldenNoiseMapParameters);
        ldenScene.setDirectionAttributes(directionAttributes);
        return ldenScene;
    }

    /**
     *  Creates a new instance of IComputePathsOut using the provided Scene data and AttenuationCnossosParameters for different time periods.
     * @param threadData       the scene data for the current computation thread.
     * @param pathDataDay      the attenuation parameters for daytime computation.
     * @param pathDataEvening  the attenuation parameters for evening computation.
     * @param pathDataNight    the attenuation parameters for nighttime computation.
     * @return A new instance of IComputePathsOut initialized with the provided parameters.
     */
    @Override
    public IComputePathsOut create(Scene threadData, AttenuationCnossosParameters pathDataDay,
                                   AttenuationCnossosParameters pathDataEvening, AttenuationCnossosParameters pathDataNight) {
        return new NoiseMap(pathDataDay, pathDataEvening, pathDataNight,
                (LdenScene)threadData, AttenuatedPaths, ldenNoiseMapParameters);
    }


}
