/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.jdbc.input.SceneWithEmission;
import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;

/**
 * This class is built on each new computation cell area. It will create for each thread (range of receivers) an instance
 * of AttenuationOutputSingleThread
 */
public class AttenuationOutputMultiThread extends AttenuationComputeOutput {
    public AttenuatedPaths attenuatedPaths;
    public SceneWithEmission sceneWithEmission;
    public NoiseMapDatabaseParameters noiseMapDatabaseParameters;

    /**
     * Create NoiseMap constructor
     * @param inputData
     * @param attenuatedPaths
     * @param noiseMapDatabaseParameters
     */
    public AttenuationOutputMultiThread(SceneWithEmission inputData,
                                        AttenuatedPaths attenuatedPaths, NoiseMapDatabaseParameters noiseMapDatabaseParameters) {
        super(noiseMapDatabaseParameters.exportRaysMethod != NoiseMapDatabaseParameters.ExportRaysMethods.NONE, null, inputData);
        this.exportAttenuationMatrix = noiseMapDatabaseParameters.exportAttenuationMatrix;
        this.attenuatedPaths = attenuatedPaths;
        this.sceneWithEmission = inputData;
        this.noiseMapDatabaseParameters = noiseMapDatabaseParameters;
    }

    /**
     * Create a collector of Vertical Cut that will be processed by a single thread (an interval of receivers points)
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess() {
        return new AttenuationOutputSingleThread(this);
    }

}
