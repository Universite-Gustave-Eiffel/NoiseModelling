package org.noise_planet.noisemodelling.emission;

public interface DirectionAttributes {

    /**
     * @param frequency Frequency in Hertz
     * @param phi (0 2π) 0 is front
     * @param theta (-π/2 π/2) 0 is horizontal π is top
     * @return Attenuation in dB
     */
    public double getAttenuation(double frequency, double phi, double theta);
}
