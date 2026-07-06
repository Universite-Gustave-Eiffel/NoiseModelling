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
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.PropagationModel;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import java.util.ArrayList;
import java.util.List;

/**
 * CNOSSOS P2P propagation model
 * @author Martin Glesser
 */
public class CnossosPropagationModel implements PropagationModel {
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
                                      AttenuationParameters attenuationParameters,
                                      boolean isExportAttenuationMatrix) {
        // Compute favorable and homogeneous propagation paths
        double gs = scene.sourceGs.getOrDefault(cutProfile.getSource().sourcePk, SceneWithAttenuation.DEFAULT_GS);
        List<CnossosPath> cnossosPaths = CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, gs);
        // Compute attenuation for each path
        List<AttenuationOutput> attenuationOutputs = new ArrayList<>();
        for (CnossosPath cnossosPath : cnossosPaths){
            AttenuationOutput attenuationOutput = new AttenuationOutput(cutProfile);
            attenuationOutput.propagationPath = cnossosPath;
            AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, scene, attenuationOutput,
                    isExportAttenuationMatrix);
            attenuationOutputs.add(attenuationOutput);
        }
        return attenuationOutputs;
    }

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param source source point information
     * @param receiver receiver point information
     * @param scene Geometrical information about the propagation scene
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in attenuationOutput for debugging purpose
     * @return Attenuation
     */
    public AttenuationOutput computeDirectAttenuation(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                             SceneWithAttenuation scene, AttenuationParameters attenuationParameters,
                                             boolean isExportAttenuationMatrix){
        CutProfile cutProfile = new CutProfile(new CutPointSource(source), new CutPointReceiver(receiver));
        CnossosPath propagationPath = new CnossosPath(cutProfile);
        propagationPath.setFavourable(true);
        propagationPath.setPointList(new ArrayList<>());
        List<Coordinate> pts2D = cutProfile.computePts2D();
        propagationPath.setSRSegment(CnossosPathBuilder.computeSegment(pts2D.get(0), pts2D.get(1), new double[] {0, 0}));
        propagationPath.getPointList().add(new PointPath(pts2D.get(0), 0, PointPath.POINT_TYPE.SRCE));
        propagationPath.getPointList().add(new PointPath(pts2D.get(1), 0, PointPath.POINT_TYPE.RECV));
        AttenuationOutput attenuationOutput = new AttenuationOutput(cutProfile);
        attenuationOutput.propagationPath = propagationPath;
        AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, scene, attenuationOutput,
                isExportAttenuationMatrix);
        return attenuationOutput;

    }
}
