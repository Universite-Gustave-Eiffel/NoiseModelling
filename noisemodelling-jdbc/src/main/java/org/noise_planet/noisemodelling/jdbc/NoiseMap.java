/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.IComputePathsOut;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.noise_planet.noisemodelling.propagation.Attenuation;

public class NoiseMap extends Attenuation {
    AttenuatedPaths attenuatedPaths;
    LdenScene ldenScene;
    public AttenuationCnossosParameters dayPathData;
    public AttenuationCnossosParameters eveningPathData;
    public AttenuationCnossosParameters nightPathData;
    public LdenNoiseMapParameters ldenNoiseMapParameters;

    /**
     * Create NoiseMap constructor
     * @param dayPathData
     * @param eveningPathData
     * @param nightPathData
     * @param inputData
     * @param attenuatedPaths
     * @param ldenNoiseMapParameters
     */
    public NoiseMap(AttenuationCnossosParameters dayPathData, AttenuationCnossosParameters eveningPathData,
                    AttenuationCnossosParameters nightPathData, LdenScene inputData,
                    AttenuatedPaths attenuatedPaths, LdenNoiseMapParameters ldenNoiseMapParameters) {
        super(inputData.ldenNoiseMapParameters.exportRaysMethod != LdenNoiseMapParameters.ExportRaysMethods.NONE, null, inputData);
        this.exportAttenuationMatrix = inputData.ldenNoiseMapParameters.exportAttenuationMatrix;
        this.attenuatedPaths = attenuatedPaths;
        this.ldenScene = inputData;
        this.dayPathData = dayPathData;
        this.eveningPathData = eveningPathData;
        this.nightPathData = nightPathData;
        this.ldenNoiseMapParameters = ldenNoiseMapParameters;
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess() {
        return new LdenComputePathsOut(this);
    }





}
