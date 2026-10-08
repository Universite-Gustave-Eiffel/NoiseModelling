/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.cnossos;

import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointReceiver;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPointSource;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * CNOSSOS P2P propagation model
 * Note : the instances of the class are thread-safe.
 * @author Martin Glesser
 */
public class CnossosPropagationModel implements PropagationModel {
    List<CnossosPath> cnossosPaths = new ArrayList<>();

    /**
     * Constructor for CnossosPropagationModel objects
     */
    public CnossosPropagationModel(){}

    /**
     * Compute the attenuation for a list of paths
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in attenuationOutput for debugging purpose
     * @return List of AttenuationOutput objects [favorable, homogeneous]
     */
    public List<AttenuationOutput> computeAttenuation(SceneWithAttenuation scene, CutProfile cutProfile,
                                      AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix) {
        // Compute favorable and homogeneous propagation paths
        if (cnossosPaths.isEmpty()) {
            double gs = scene.getSourceGs(cutProfile.getSource().sourcePk);
            cnossosPaths = CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile,
                    scene.profileBuilder.exactFrequencyArray, gs);
        }
        // Compute attenuation for each path
        List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
        for (CnossosPath cnossosPath : cnossosPaths){
            CnossosAttenuationOutput attenuationOutput = new CnossosAttenuationOutput(cutProfile);
            attenuationOutput.propagationPath = cnossosPath;
            AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, scene, attenuationOutput,
                    isExportAttenuationMatrix);
            attenuationOutput.setLineString(cnossosPath.asGeom());
            if (cnossosPath.isFavourable()){
                attenuationOutput.setMeteoType(MeteoType.FAVOURABLE);
            } else{
                attenuationOutput.setMeteoType(MeteoType.HOMOGENEOUS);
            }
            attenuationOutputs.add(attenuationOutput);
        }
        return attenuationOutputs;
    }
}
