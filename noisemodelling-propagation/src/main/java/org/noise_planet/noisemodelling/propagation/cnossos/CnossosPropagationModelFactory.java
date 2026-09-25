package org.noise_planet.noisemodelling.propagation.cnossos;
import org.noise_planet.noisemodelling.propagation.PropagationModel;
import org.noise_planet.noisemodelling.propagation.PropagationModelFactory;

/**
 * Declares the concrete factory method that returns CnossosPropagationModel objects
 * @author Martin Glesser
 */
public class CnossosPropagationModelFactory implements PropagationModelFactory {
    /**
     * Factory method that returns CnossosPropagationModel objects
     * @return PropagationModel object
     */
    public PropagationModel create(){
        return new CnossosPropagationModel();
    }
}
