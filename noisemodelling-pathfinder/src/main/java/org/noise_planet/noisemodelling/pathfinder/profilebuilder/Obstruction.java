package org.noise_planet.noisemodelling.pathfinder.profilebuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Obstruction object
 */
public class Obstruction {
    public static final double DEFAULT_G = 100000;

    /** Obstruction alpha value. */
    private List<Double> alphas = new ArrayList<>();
    /** Obstruction global alpha or impedance */
    private double g = DEFAULT_G;

    /**
     * Initialize alpha values for each frequency
     * @param frequencyExact Exact frequency values
     */
    public void initialize(List<Double> frequencyExact) {
        if(alphas.size() != frequencyExact.size()) {
            alphas = new ArrayList<>();
            for (double freq : frequencyExact) {
                alphas.add(WallAbsorption.getWallAlpha(g, freq));
            }
        }
    }

    public void copyAlphas(Obstruction obstructionSource) {
        if(!obstructionSource.alphas.isEmpty()) {
            this.alphas = new ArrayList<>(obstructionSource.alphas);
        } else {
            this.alphas.clear();
        }
        this.g = obstructionSource.g;
    }
    /**
     * Retrieve the absorption coefficients.
     * @return The absorption coefficients.
     */
    public List<Double> getAlphas() {
        return alphas;
    }

    /**
     * @return Obstruction global alpha or impedance
     */
    public double getG() {
        return g;
    }

    /**
     * @param g Obstruction global alpha or impedance
     */
    public void setG(double g) {
        this.g = g;
    }

    /**
     * Sets the wall alphas.
     * @param alphas Wall alphas.
     */
    public void setAlpha(List<Double> alphas) {
        this.alphas = alphas;
    }
}
