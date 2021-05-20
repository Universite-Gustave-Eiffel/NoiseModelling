/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.emission;
/**
 * Railway noise evaluation from Cnossos reference : COMMISSION DIRECTIVE (EU) 2015/996
 * of 19 May 2015 establishing common noise assessment methods according to Directive 2002/49/EC
 * of the European Parliament and of the Council
 *
 * amending, for the purposes of adapting to scientific and technical progress, Annex II to
 * Directive 2002/49/EC of the European Parliament and of the Council as regards
 * common noise assessment methods
 *
 * part 2.3. Railway noise
 *
 * Return the dB value corresponding to the parameters
 * @author Adrien Le Bellec - univ Gustave eiffel
 * @author Olivier Chiello, Univ Gustave Eiffel
 */

import static org.noise_planet.noisemodelling.emission.Utils.sumDbArray;

/**
 * Data result stockage
 */
public class RailWayLW {

    public enum TrainNoiseSource {
        ROLLING,
        TRACTIONA,
        TRACTIONB,
        AERODYNAMICA,
        AERODYNAMICB,
        BRIDGE
    }

    /**
     * H_INDEX of noise source type (TrainNoiseSource)
     * 0 - Low height (0.5 m) A
     * 1 - High height (4 m) B
     */
    public static final int[] TRAIN_NOISE_SOURCE_H_INDEX = new int[] {0, 0, 1, 0, 1, 0};

    private double[] lWRolling;
    private double[] lWTractionA;
    private double[] lWTractionB;
    private double[] lWAerodynamicA;
    private double[] lWAerodynamicB;
    private double[] lWBridge;


    public void setLWRolling(double[] LWRolling) {
        this.lWRolling = LWRolling;
    }
    public void setLWTractionA(double[] LWTractionA) {
        this.lWTractionA = LWTractionA;
    }
    public void setLWTractionB(double[] LWTractionB) {
        this.lWTractionB = LWTractionB;
    }
    public void setLWAerodynamicA(double[] LWAerodynamicA) {
        this.lWAerodynamicA = LWAerodynamicA;
    }
    public void setLWAerodynamicB(double[] LWAerodynamicB) {
        this.lWAerodynamicB = LWAerodynamicB;
    }
    public void setLWBridge(double[] LWBridge) {
        this.lWBridge = LWBridge;
    }

    public double[] getLWRolling() {
        return lWRolling;
    }
    public double[] getLWTractionA() {
        return lWTractionA;
    }
    public double[] getLWTractionB() {
        return lWTractionB;
    }
    public double[] getLWAerodynamicA() {
        return lWAerodynamicA;
    }
    public double[] getLWAerodynamicB() {
        return lWAerodynamicB;
    }
    public double[] getLWBridge() {
        return lWBridge;
    }

    public RailWayLW(){
    }

    public RailWayLW(double[] lWRolling, double[] lWTractionA, double[] lWTractionB, double[] lWAerodynamicA, double[] lWAerodynamicB, double[] lWBridge){

        setLWRolling(lWRolling);
        setLWTractionA(lWTractionA);
        setLWTractionB(lWTractionB);
        setLWAerodynamicA(lWAerodynamicA);
        setLWAerodynamicB(lWAerodynamicB);
        setLWBridge(lWBridge);

    }

    /**
     * Sum two train emission instances
     * @param railWayLW1 Emission 1
     * @param railWayLW2 Emission 2
     * @return Merged level
     */
    public static RailWayLW sumRailWayLW(RailWayLW railWayLW1, RailWayLW railWayLW2){
        RailWayLW railWayLW = new RailWayLW();

        railWayLW.setLWRolling(sumDbArray(railWayLW1.getLWRolling() ,railWayLW2.getLWRolling()) );
        railWayLW.setLWAerodynamicA(sumDbArray(railWayLW1.getLWAerodynamicA() ,railWayLW2.getLWAerodynamicA()) );
        railWayLW.setLWAerodynamicB(sumDbArray(railWayLW1.getLWAerodynamicB() ,railWayLW2.getLWAerodynamicB()) );
        railWayLW.setLWBridge(sumDbArray(railWayLW1.getLWBridge() ,railWayLW2.getLWBridge()) );
        railWayLW.setLWTractionA(sumDbArray(railWayLW1.getLWTractionA() ,railWayLW2.getLWTractionA()) );
        railWayLW.setLWTractionB(sumDbArray(railWayLW1.getLWTractionB() ,railWayLW2.getLWTractionB()) );

        return railWayLW;
    }


    /**
     * Compute the attenuation for the specified noise source and parameters
     * @param noiseSource Noise source category
     * @param phi (0 2π) 0 is front
     * @param theta (-π/2 π/2) 0 is horizontal π is top
     * @param frequency Emission frequency
     * @return Directional sound power
     */
    public static Double getDirectionAttenuation(TrainNoiseSource noiseSource, double phi, double theta, double frequency) {
        if(noiseSource == TrainNoiseSource.BRIDGE) {
            return 0.0;
        }
        int height_index = TRAIN_NOISE_SOURCE_H_INDEX[noiseSource.ordinal()];
        double attHorizontal = 10 * Math.log10(0.01 + 0.99 * Math.pow(Math.sin(phi), 2));
        double attVertical = 0;
        if(height_index == 1) {
            if(theta > 0 && theta <= Math.PI / 2.0) {
                attVertical = (40.0 / 3.0)
                        * (2.0 / 3.0 * Math.sin(2 * theta) - Math.sin(theta))
                        * Math.log10((frequency + 600.0) / 200.0);
            }
        } else if(height_index == 2){
            if(theta < 0) { // for aerodynamic effect only
                attVertical = 10 * Math.log10(Math.pow(Math.sin(theta), 2));
            }
        }
        return attHorizontal + attVertical;
    }

    public static final class TrainAttenuation implements DirectionAttributes {
        TrainNoiseSource noiseSource;

        public TrainAttenuation(TrainNoiseSource noiseSource) {
            this.noiseSource = noiseSource;
        }

        @Override
        public double getAttenuation(double frequency, double phi, double theta) {
            return RailWayLW.getDirectionAttenuation(noiseSource, phi, theta, frequency);
        }
    }
}
