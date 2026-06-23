package org.noise_planet.noisemodelling.propagation.cnossos;
import org.noise_planet.noisemodelling.propagation.PropagationModel;
import org.noise_planet.noisemodelling.propagation.PropagationModelCreator;

/**
 * Declares the concrete factory method that returns CnossosPropagationModel objects
 * @author Martin Glesser
 */
public class CnossosPropagationModelCreator implements PropagationModelCreator {
    /**
     * Factory method that returns CnossosPropagationModel objects
     * @return PropagationModel object
     */
    public PropagationModel create(){
        return new CnossosPropagationModel();
    }
}
