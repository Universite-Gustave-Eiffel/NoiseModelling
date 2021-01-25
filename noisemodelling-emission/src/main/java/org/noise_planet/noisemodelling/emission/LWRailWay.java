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

/**
 * Data result stockage
 */
public class LWRailWay {

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

    public LWRailWay(double[] lWRolling,double[] lWTractionA,double[] lWTractionB,double[] lWAerodynamicA,double[] lWAerodynamicB,double[] lWBridge){

        setLWRolling(lWRolling);
        setLWTractionA(lWTractionA);
        setLWTractionB(lWTractionB);
        setLWAerodynamicA(lWAerodynamicA);
        setLWAerodynamicB(lWAerodynamicB);
        setLWBridge(lWBridge);

    }
}
