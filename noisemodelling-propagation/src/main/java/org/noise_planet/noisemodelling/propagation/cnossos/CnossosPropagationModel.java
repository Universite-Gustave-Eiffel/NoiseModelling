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
    public SceneWithAttenuation scene;

    public CnossosPropagationModel(SceneWithAttenuation scene) {
        this.scene = scene;
    }

    /**
     * Compute the paths for a given geometrical cross-section / cut profile
     *
     * @param cutProfile geometrical cross-section / cut profile
     * @return {List<CnossosPath>} Paths
     */
    public List<CnossosPath> computePaths(CutProfile cutProfile){
        double gs = scene.sourceGs.getOrDefault(cutProfile.getSource().sourcePk, SceneWithAttenuation.DEFAULT_GS);
        return CnossosPathBuilder.computeCnossosPathsFromCutProfile(cutProfile, scene.isBodyBarrier(),
                scene.profileBuilder.exactFrequencyArray, gs);
    }

    /**
     * Compute the attenuation for a given path
     *
     * @param attenuationParameters parameters of the computation
     * @param path path used for the attenuation computation
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    public double[] computeAttenuation(AttenuationParameters attenuationParameters, CnossosPath path, boolean isExportAttenuationMatrix) {
        return AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, path,
                scene, isExportAttenuationMatrix);
    }

    /**
     * Compute attenuation along direct path between source and receiver
     *
     * @param attenuationParameters parameters of the computation
     * @param source source point information
     * @param receiver receiver point information
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    public double[] computeDirectAttenuation(AttenuationParameters attenuationParameters, PathFinder.SourcePointInfo source,
                                      PathFinder.ReceiverPointInfo receiver, boolean isExportAttenuationMatrix){
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
