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
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitorFactory;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is built on each new computation cell area. It will create for each thread (range of receivers) an instance
 * of AttenuationOutputSingleThread
 */
public class AttenuationOutputMultiThread implements CutPlaneVisitorFactory {
    public ResultsCache resultsCache = new ResultsCache();
    public SceneWithEmission sceneWithEmission;
    public NoiseMapDatabaseParameters noiseMapDatabaseParameters = new NoiseMapDatabaseParameters();
    public AtomicBoolean exitWhenDone = new AtomicBoolean(false);
    public AtomicBoolean aborted = new AtomicBoolean(false);
    public AtomicLong cnossosPathCount = new AtomicLong();

    /**
     * Create NoiseMap constructor
     * @param inputData
     * @param resultsCache
     * @param noiseMapDatabaseParameters
     */
    public AttenuationOutputMultiThread(SceneWithEmission inputData,
                                        ResultsCache resultsCache, NoiseMapDatabaseParameters noiseMapDatabaseParameters, AtomicBoolean exitWhenDone, AtomicBoolean aborted) {
        this.resultsCache = resultsCache;
        this.sceneWithEmission = inputData;
        this.noiseMapDatabaseParameters = noiseMapDatabaseParameters;
        this.exitWhenDone = exitWhenDone;
        this.aborted = aborted;
    }

    public AttenuationOutputMultiThread(SceneWithEmission sceneWithEmission) {
        this.sceneWithEmission = sceneWithEmission;
    }

    /**
     * Create a collector of Vertical Cut that will be processed by a single thread (an interval of receivers points)
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public CutPlaneVisitor subProcess(ProgressVisitor visitor) {
        return new AttenuationOutputSingleThread(this, visitor);
    }

}
