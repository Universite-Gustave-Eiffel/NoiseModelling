/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.cnossos.RailwayCnossosDirectivitySphere;
import org.noise_planet.noisemodelling.emission.directivity.OmnidirectionalDirection;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.jdbc.output.NoiseMapWriter;
import org.noise_planet.noisemodelling.jdbc.output.ResultsCache;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.ProfilerThread;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

import java.sql.Connection;
import java.util.*;

/**
 * Create SQL Tables from a stream of noise levels
 */
public class NoiseMapMaker { //implements NoiseMapByReceiverMaker.PropagationProcessDataFactory, NoiseMapByReceiverMaker.IComputeRaysOutFactory, ProfilerThread.Metric {
//    NoiseMapDatabaseParameters noiseMapDatabaseParameters;
//    NoiseMapWriter noiseMapWriter;
//    Thread tableWriterThread;
//    Connection connection;
//
//    org.noise_planet.noisemodelling.jdbc.output.ResultsCache ResultsCache = new ResultsCache();
//    int srid;
//
//    // Environmental propagation conditions for each time period
//    public Map<String, AttenuationCnossosParameters> attenuationCnossosParametersMap = new HashMap<>();
//    /**
//     * If a time period do not specify the propagation meteorological conditions, use this default settings
//     */
//    public AttenuationCnossosParameters defaultAttenuationCnossosParameters = new AttenuationCnossosParameters();
//
//    /**
//     * Attenuation and other attributes relative to direction on sphere
//     */
//    public Map<Integer, DirectivitySphere> directionAttributes = new HashMap<>();
//
//
//    public NoiseMapMaker(Connection connection, NoiseMapDatabaseParameters NoiseMapDatabaseParameters) {
//        this.noiseMapDatabaseParameters = NoiseMapDatabaseParameters;
//        this.connection = connection;
//    }
//
//    @Override
//    public String[] getColumnNames() {
//        return new String[] {"jdbc_stack"};
//    }
//
//    @Override
//    public String[] getCurrentValues() {
//        return new String[] {Long.toString(ResultsCache.queueSize.get())};
//    }
//
//    @Override
//    public void tick(long currentMillis) {
//
//    }
//
//    /**
//     * @return Thread safe dequeue. It is a cache location of data waiting to be pushed into the database
//     */
//    public ResultsCache getOutputDataDeque() {
//        return ResultsCache;
//    }
//
//    /**
//     * Inserts directivity attributes for noise sources for trains into the directionAttributes map.
//     */
//    public void insertTrainDirectivity() {
//        directionAttributes.clear();
//        directionAttributes.put(0, new OmnidirectionalDirection());
//        int i=1;
//        for(String typeSource : RailWayCnossosParameters.sourceType) {
//            directionAttributes.put(i, new RailwayCnossosDirectivitySphere(new LineSource(typeSource)));
//            i++;
//        }
//    }
//
//
//    /**
//     * Start creating and filling database tables
//     */
//    public void start() {
//        if(noiseMapDatabaseParameters.getPropagationProcessPathData(NoiseMapDatabaseParameters.TIME_PERIOD.DAY) == null) {
//            throw new IllegalStateException("start() function must be called after NoiseMapByReceiverMaker initialization call");
//        }
//        noiseMapWriter = new NoiseMapWriter(connection, noiseMapDatabaseParameters, ResultsCache, srid);
//        noiseMapDatabaseParameters.exitWhenDone = false;
//        tableWriterThread = new Thread(noiseMapWriter);
//        tableWriterThread.start();
//        while (!noiseMapWriter.started && !noiseMapDatabaseParameters.aborted) {
//            try {
//                Thread.sleep(150);
//            } catch (InterruptedException e) {
//                // ignore
//                break;
//            }
//        }
//    }
//
//    /**
//     * Write the last results and stop the sql writing thread
//     */
//    public void stop() {
//        noiseMapDatabaseParameters.exitWhenDone = true;
//        while (tableWriterThread != null && tableWriterThread.isAlive()) {
//            try {
//                Thread.sleep(150);
//            } catch (InterruptedException e) {
//                // ignore
//                break;
//            }
//        }
//    }
//
//
//    /**
//     * Creates a new instance of NoiseEmissionMaker using the provided ProfileBuilder and NoiseMapParameters.
//     * @param builder the profile builder used to construct the scene.
//     * @return A new instance of NoiseEmissionMaker initialized with the provided ProfileBuilder and NoiseMapParameters.
//     */
//    @Override
//    public NoiseEmissionMaker create(ProfileBuilder builder) {
//        NoiseEmissionMaker noiseEmissionMaker = new NoiseEmissionMaker(builder, noiseMapDatabaseParameters);
//        noiseEmissionMaker.setDirectionAttributes(directionAttributes);
//        return noiseEmissionMaker;
//    }
//
//    /**
//     *  Creates a new instance of IComputePathsOut using the provided Scene data and AttenuationCnossosParameters for different time periods.
//     * @param threadData       the scene data for the current computation thread.
//     * @param pathDataDay      the attenuation parameters for daytime computation.
//     * @param pathDataEvening  the attenuation parameters for evening computation.
//     * @param pathDataNight    the attenuation parameters for nighttime computation.
//     * @return A new instance of IComputePathsOut initialized with the provided parameters.
//     */
//    @Override
//    public IComputePathsOut create(SceneWithEmission scene) {
//        return new AttenuationOutputMultiThread(scene, ResultsCache, noiseMapDatabaseParameters);
//    }


}
