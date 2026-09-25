/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc.output;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters;
import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.PathFinderProcessor;
import org.noise_planet.noisemodelling.pathfinder.PathFinderProcessorManager;
import org.noise_planet.noisemodelling.propagation.PropagationModelFactory;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPropagationModelFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates, for each thread (range of receivers) an instance of AttenuationProcessor.
 * This class is built on each new computation cell area.
 */
public class AttenuationProcessorManager implements PathFinderProcessorManager {
    public ResultsCache resultsCache = new ResultsCache();
    public SceneWithEmission sceneWithEmission;
    public NoiseMapDatabaseParameters noiseMapDatabaseParameters = new NoiseMapDatabaseParameters();
    public AtomicBoolean exitWhenDone = new AtomicBoolean(false);
    public AtomicBoolean aborted = new AtomicBoolean(false);
    public AtomicInteger cutProfileCount = new AtomicInteger();
    public PropagationModelFactory propagationModelFactory;

    /**
     * Create NoiseMap constructor
     *
     * @param inputData Geometrical information about the propagation scene
     * @param resultsCache Results cache
     * @param noiseMapDatabaseParameters Propagation parameters
     */
    public AttenuationProcessorManager(SceneWithEmission inputData, PropagationModelFactory propagationModelFactory,
                                       ResultsCache resultsCache, NoiseMapDatabaseParameters noiseMapDatabaseParameters, AtomicBoolean exitWhenDone, AtomicBoolean aborted) {
        this.resultsCache = resultsCache;
        this.sceneWithEmission = inputData;
        this.noiseMapDatabaseParameters = noiseMapDatabaseParameters;
        this.exitWhenDone = exitWhenDone;
        this.aborted = aborted;
        this.propagationModelFactory = propagationModelFactory;
    }

    /**
     * Constructor for AttenuationProcessorManager with Cnossos propagation model
     * (for testing purpose).
     *
     * @param sceneWithEmission Geometrical information about the propagation scene
     */
    public AttenuationProcessorManager(SceneWithEmission sceneWithEmission) {
        this.sceneWithEmission = sceneWithEmission;
        this.propagationModelFactory = new CnossosPropagationModelFactory();
    }

    /**
     * Create a collector of Vertical Cut that will be processed by a single thread (an interval of receivers points)
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public PathFinderProcessor subProcess(ProgressVisitor visitor) {
        return new AttenuationProcessor(this, visitor);
    }

}
