/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : https://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPropagationModel;

/**
 * Factory class for propagation model creation
 * @author Martin Glesser
 */
public class PropagationModelFactory {
    /**
     * Create a CnossosPropagationModel object associated
     * to a specific scene.
     * @param scene global geometrical information
     * @return propagation model object
     */
    public static PropagationModel create(SceneWithAttenuation scene, CutProfile cutProfile){
        return new CnossosPropagationModel(scene, cutProfile);
    }

    /**
     * Create a CnossosPropagationModel object.
     *
     * @return propagation model object
     */
    public static PropagationModel create(){
        return new CnossosPropagationModel();
    }

    /**
     * Create a specific implementation of the PropagationModel interface.
     *
     * @param modelName Name of the propagation model
     * @return propagation model object
     */
    public static PropagationModel create(String modelName){
        switch (modelName.toUpperCase()) {
            case "CNOSSOS":
                return new CnossosPropagationModel();
            default:
                throw new IllegalArgumentException("Unknown type: " + modelName);
        }
    }

    /**
     * Create a specific implementation of the PropagationModel interface
     * associated to a specific scene.
     *
     * @param modelName Name of the propagation model
     * @param scene Global geometrical information
     * @return propagation model object
     */
    public static PropagationModel create(String modelName, SceneWithAttenuation scene, CutProfile cutProfile){
        PropagationModel propagationModel = create(modelName);
        propagationModel.setScene(scene);
        propagationModel.setCutProfile(cutProfile);
        return propagationModel;
    }
}
