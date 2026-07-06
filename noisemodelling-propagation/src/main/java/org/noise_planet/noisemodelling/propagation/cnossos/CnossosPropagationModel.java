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
     * Compute the propagation paths for a given geometrical cross-section / cut profile
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @return List of Cnossos propagation paths
     */
    public List<CnossosPath> computePaths(SceneWithAttenuation scene, CutProfile cutProfile){
        double gs = scene.sourceGs.getOrDefault(cutProfile.getSource().sourcePk, SceneWithAttenuation.DEFAULT_GS);
        return CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, gs);
    }

    /**
     * Compute the attenuation for a list of paths
     *
     * @param scene Geometrical information about the propagation scene
     * @param cutProfile Geometrical cross-section
     * @param paths List of propagation paths (Cnossos specific)
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return Attenuation for the homogeneous and favourable path
     */
    public List<double[]> computeAttenuation(SceneWithAttenuation scene, CutProfile cutProfile, List<CnossosPath> paths,
                                      AttenuationParameters attenuationParameters,
                                      boolean isExportAttenuationMatrix) {
        List<double[]> attenuationList = new ArrayList<>();
        for (CnossosPath path : paths){
            attenuationList.add(AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, path,
                    scene, isExportAttenuationMatrix));
        }
        return attenuationList;
    }

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param source source point information
     * @param receiver receiver point information
     * @param scene Geometrical information about the propagation scene
     * @param attenuationParameters parameters of the computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return Attenuation
     */
    public double[] computeDirectAttenuation(PathFinder.SourcePointInfo source, PathFinder.ReceiverPointInfo receiver,
                                             SceneWithAttenuation scene, AttenuationParameters attenuationParameters,
                                             boolean isExportAttenuationMatrix){
        CutProfile cutProfile = new CutProfile(new CutPointSource(source), new CutPointReceiver(receiver));
        CnossosPath cnossosPath = new CnossosPath(cutProfile);
        cnossosPath.setFavourable(true);
        cnossosPath.setPointList(new ArrayList<>());
        List<Coordinate> pts2D = cutProfile.computePts2D();
        cnossosPath.setSRSegment(CnossosPathBuilder.computeSegment(pts2D.get(0), pts2D.get(1), new double[] {0, 0}));
        cnossosPath.getPointList().add(new PointPath(pts2D.get(0), 0, PointPath.POINT_TYPE.SRCE));
        cnossosPath.getPointList().add(new PointPath(pts2D.get(1), 0, PointPath.POINT_TYPE.RECV));
        return AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, cnossosPath, scene, isExportAttenuationMatrix);

    }
}
