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
    public SceneWithAttenuation scene;
    public CutProfile cutProfile;
    public List<CnossosPath> paths = new ArrayList<>();

    public CnossosPropagationModel(){}

    public CnossosPropagationModel(SceneWithAttenuation scene, CutProfile cutProfile) {
        this.scene = scene;
        this.cutProfile = cutProfile;
    }

    /**
     * Compute the paths for a given geometrical cross-section / cut profile
     *
     * @return {List<CnossosPath>} Paths
     */
    public List<CnossosPath> computePaths(){
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
     * @param isExportAttenuationMatrix if true, store intermediate values in proPathParameters for debugging purpose
     * @return {double[]} Attenuation
     */
    public double[] computeDirectAttenuation(AttenuationParameters attenuationParameters, boolean isExportAttenuationMatrix){
        CnossosPath cnossosPath = new CnossosPath(cutProfile);
        cnossosPath.setFavourable(true);
        cnossosPath.setPointList(new ArrayList<>());
        List<Coordinate> pts2D = cutProfile.computePts2D();
        cnossosPath.setSRSegment(CnossosPathBuilder.computeSegment(pts2D.get(0), pts2D.get(1), new double[] {0, 0}));
        cnossosPath.getPointList().add(new PointPath(pts2D.get(0), 0, PointPath.POINT_TYPE.SRCE));
        cnossosPath.getPointList().add(new PointPath(pts2D.get(1), 0, PointPath.POINT_TYPE.RECV));
        return AttenuationCnossos.computeCnossosAttenuation(attenuationParameters, cnossosPath, scene, isExportAttenuationMatrix);

    }

    /**
     * Getter for scene attribute
     *
     * @return {SceneWithAttenuation} Global geometrical information
     */
    public SceneWithAttenuation getScene(){
        return scene;
    }

    /**
     * Setter for scene attribute
     *
     * @param scene Global geometrical information
     */
    public void setScene(SceneWithAttenuation scene){
        this.scene = scene;
    }

    /**
     * Getter for curProfile attribute
     *
     * @return {CutProfile} Geometrical cut profile
     */
    public CutProfile getCutProfile() {
        return cutProfile;
    }

    /**
     * Setter for cutProfile attribute
     *
     * @param cutProfile Geometrical cut profile
     */
    public void setCutProfile(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
    }

    /**
     * Setter for cutProfile attribute
     *
     * @param source source point information
     * @param receiver receiver point information
     */
    public void setCutProfile(PathFinder.SourcePointInfo source,
                              PathFinder.ReceiverPointInfo receiver) {

        this.cutProfile = new CutProfile(new CutPointSource(source), new CutPointReceiver(receiver));
    }

    /**
     * Getter for paths attribute
     *
     * @return {List<CnossosPath>} List of paths
     */
    public List<CnossosPath> getPaths(){
        if (paths.isEmpty())
            paths = computePaths();
        return paths;
    }
}
